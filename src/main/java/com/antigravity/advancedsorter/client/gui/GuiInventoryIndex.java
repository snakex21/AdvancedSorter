package com.antigravity.advancedsorter.client.gui;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.container.ContainerInventoryIndex;
import com.antigravity.advancedsorter.tiles.TileInventoryIndex;
import com.antigravity.advancedsorter.tiles.ChestGroup;
import com.antigravity.advancedsorter.network.PacketInventoryIndexSync;
import com.antigravity.advancedsorter.network.PacketRequestItem;
import com.antigravity.advancedsorter.network.PacketHandler;
import com.antigravity.advancedsorter.network.PacketManageChestGroup;
import com.antigravity.advancedsorter.client.RenderPathOverlay;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class GuiInventoryIndex extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft",
            "textures/gui/container/generic_54.png");
    private final TileInventoryIndex tile;

    // Tabs: 0 = Navigate, 1 = Get Items, 2 = Manage
    private int selectedTab = 0;
    private GuiTextField searchField;

    private List<PacketInventoryIndexSync.IndexEntry> cachedIndex = Collections.emptyList();
    private List<PacketInventoryIndexSync.IndexEntry> filteredIndex = new ArrayList<>();
    private String lastSearchText = "";

    private int scrollOffset = 0;
    private static final int VISIBLE_ROWS = 6;
    private static final int ITEMS_PER_ROW = 1; // List view implies 1 item per row

    // Sorting: 0 = A-Z, 1 = Z-A, 2 = Most, 3 = Least
    private int sortMode = 0;

    // Tab buttons
    private GuiButton btnTabNav;
    private GuiButton btnTabGet;
    private GuiButton btnTabManage;

    // Sort buttons
    private GuiButton btnSortAZ;
    private GuiButton btnSortZA;
    private GuiButton btnSortMost;
    private GuiButton btnSortLeast;

    // Manage tab
    private List<BlockPos> linkedChestsList = new ArrayList<>();
    private int selectedChestIndex = -1;
    private int manageScrollOffset = 0;
    private static final int MANAGE_VISIBLE_ITEMS = 6;
    private GuiButton btnRemoveChest;

    public GuiInventoryIndex(ContainerInventoryIndex container, TileInventoryIndex tile) {
        super(container);
        this.tile = tile;
        this.xSize = 184;
        this.ySize = 256; // Increased height
    }

    /**
     * Checks if this GUI is for the specified indexer position.
     * Used to filter packets from other indexers.
     */
    public boolean isForIndexer(BlockPos indexerPos) {
        return tile != null && tile.getPos().equals(indexerPos);
    }

    @Override
    public void initGui() {
        super.initGui();
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;

        // Tab buttons (Top) - Y=10
        this.buttonList.add(btnTabNav = new GuiButton(1, i + 10, j + 10, 50, 14, "Nav"));
        this.buttonList.add(btnTabGet = new GuiButton(2, i + 65, j + 10, 50, 14, "Get"));
        this.buttonList.add(btnTabManage = new GuiButton(3, i + 120, j + 10, 54, 14, "Manage"));

        // Search field (Below Tabs) - Y=26
        this.searchField = new GuiTextField(0, this.fontRenderer, i + 45, j + 26, 130, 12);
        this.searchField.setMaxStringLength(50);
        this.searchField.setEnableBackgroundDrawing(true);
        this.searchField.setVisible(true);
        this.searchField.setTextColor(16777215);

        // Sort buttons (Below Search) - Y=40
        int sortY = j + 40;
        btnSortAZ = new GuiButton(20, i + 8, sortY, 35, 12, "A-Z");
        btnSortZA = new GuiButton(21, i + 45, sortY, 35, 12, "Z-A");
        btnSortMost = new GuiButton(22, i + 82, sortY, 45, 12, "Most");
        btnSortLeast = new GuiButton(23, i + 129, sortY, 45, 12, "Least");
        this.buttonList.add(btnSortAZ);
        this.buttonList.add(btnSortZA);
        this.buttonList.add(btnSortMost);
        this.buttonList.add(btnSortLeast);

        // Remove button for Manage tab - Y=26 (Same as search)
        btnRemoveChest = new GuiButton(10, i + 110, j + 26, 65, 12, "Remove");
        btnRemoveChest.visible = false;
        this.buttonList.add(btnRemoveChest);

        updateSlotsVisibility();
        refreshLinkedChests();
        updateSortButtonStates();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 1: // Navigate
                selectedTab = 0;
                scrollOffset = 0;
                updateSlotsVisibility();
                break;
            case 2: // Get Items
                selectedTab = 1;
                scrollOffset = 0;
                updateSlotsVisibility();
                break;
            case 3: // Manage
                selectedTab = 2;
                manageScrollOffset = 0;
                selectedChestIndex = -1;
                refreshLinkedChests();
                updateSlotsVisibility();
                break;
            case 20: // Sort A-Z
            case 21: // Sort Z-A
            case 22: // Sort Most
            case 23: // Sort Least
                sortMode = button.id - 20;
                sortAndFilterIndex();
                updateSortButtonStates();
                break;
            case 10: // Remove chest
                if (selectedChestIndex >= 0 && selectedChestIndex < linkedChestsList.size()) {
                    BlockPos chestPos = linkedChestsList.get(selectedChestIndex);
                    if (tile.isChestInAnyGroup(chestPos)) {
                        PacketHandler.INSTANCE.sendToServer(
                                PacketManageChestGroup.removeChestFromGroup(tile.getPos(), chestPos, false));
                    } else {
                        PacketHandler.INSTANCE.sendToServer(
                                PacketManageChestGroup.removeIndividualChest(tile.getPos(), chestPos));
                    }
                    linkedChestsList.remove(selectedChestIndex);
                    selectedChestIndex = -1;
                    this.mc.player.sendMessage(new TextComponentString("\u00a7cRemoved chest from Indexer."));
                    updateSlotsVisibility();
                }
                break;
        }
    }

    private void updateSlotsVisibility() {
        boolean isGetTab = (selectedTab == 1);
        boolean isManageTab = (selectedTab == 2);

        btnTabNav.enabled = (selectedTab != 0);
        btnTabGet.enabled = (selectedTab != 1);
        btnTabManage.enabled = (selectedTab != 2);

        searchField.setVisible(!isManageTab);
        searchField.setEnabled(!isManageTab);

        boolean showSort = !isManageTab;
        btnSortAZ.visible = showSort;
        btnSortZA.visible = showSort;
        btnSortMost.visible = showSort;
        btnSortLeast.visible = showSort;

        btnRemoveChest.visible = isManageTab;
        btnRemoveChest.enabled = isManageTab && selectedChestIndex != -1;

        // Handle Slot Positions in Container
        // Slots 0-8 are the Output Buffer
        for (int k = 0; k < 9; ++k) {
            Slot slot = this.inventorySlots.getSlot(k);
            if (isGetTab) {
                // Position slots in a 3x3 grid on the right side of the list area
                slot.xPos = 135 + (k % 3) * 18;
                slot.yPos = 75 + (k / 3) * 18;
            } else {
                // Move them off-screen when not in Get tab
                slot.xPos = -2000;
                slot.yPos = -2000;
            }
        }
    }

    private void updateSortButtonStates() {
        if (selectedTab == 2) return;
        btnSortAZ.enabled = (sortMode != 0);
        btnSortZA.enabled = (sortMode != 1);
        btnSortMost.enabled = (sortMode != 2);
        btnSortLeast.enabled = (sortMode != 3);
    }

    public void updateIndex(List<PacketInventoryIndexSync.IndexEntry> entries) {
        this.cachedIndex = entries;
        updateFilteredList();
    }

    public void refreshLinkedChests() {
        if (tile != null) {
            this.linkedChestsList = new ArrayList<>(tile.getLinkedChests());
        }
    }

    public void updateChestGroups(List<BlockPos> individualChests, List<ChestGroup> groups) {
        refreshLinkedChests();
    }

    private void updateFilteredList() {
        String search = searchField != null ? searchField.getText().toLowerCase(Locale.ROOT).trim() : "";
        filteredIndex.clear();

        if (search.isEmpty()) {
            filteredIndex.addAll(cachedIndex);
        } else {
            for (PacketInventoryIndexSync.IndexEntry entry : cachedIndex) {
                String itemName = entry.stack.getDisplayName().toLowerCase(Locale.ROOT);
                if (itemName.contains(search)) {
                    filteredIndex.add(entry);
                }
            }
        }
        sortAndFilterIndex();
    }

    private void sortAndFilterIndex() {
        switch (sortMode) {
            case 0: // A-Z
                Collections.sort(filteredIndex, Comparator.comparing((PacketInventoryIndexSync.IndexEntry entry) -> entry.stack.getDisplayName()));
                break;
            case 1: // Z-A
                Collections.sort(filteredIndex, Comparator.comparing((PacketInventoryIndexSync.IndexEntry entry) -> entry.stack.getDisplayName()).reversed());
                break;
            case 2: // Most items
                Collections.sort(filteredIndex, Comparator.comparingInt((PacketInventoryIndexSync.IndexEntry entry) -> entry.count).reversed());
                break;
            case 3: // Least items
                Collections.sort(filteredIndex, Comparator.comparingInt((PacketInventoryIndexSync.IndexEntry entry) -> entry.count));
                break;
        }
        // Don't reset scroll here so user keeps position, but clamp it
        clampScroll();
    }

    private void clampScroll() {
        int maxScroll = Math.max(0, (filteredIndex.size() - 1) / ITEMS_PER_ROW - VISIBLE_ROWS + 1);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawRect(0, 0, this.width, this.height, 0x88000000);

        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;

        // Main Background
        drawRect(i - 2, j - 2, i + this.xSize + 2, j + this.ySize + 2, 0xFF333333);
        drawRect(i, j, i + this.xSize, j + this.ySize, 0xFFC6C6C6);

        // Table Area Background (Starts after sort buttons at j+55)
        drawRect(i + 4, j + 55, i + this.xSize - 4, j + 165, 0xFF8B8B8B);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);

        // Output slots background (Get Tab)
        if (selectedTab == 1) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    this.drawTexturedModalRect(i + 134 + c * 18, j + 74 + r * 18, 0, 0, 18, 18);
                }
            }
        }

        // Draw Player Inventory slots at the bottom (Background starts at Y=170)
        this.mc.getTextureManager().bindTexture(TEXTURE);
        this.drawTexturedModalRect(i + 4, j + 170, 0, 126, this.xSize - 8, 76);

        if (selectedTab != 2) {
            this.searchField.drawTextBox();
            // Start table at j+68 (headers at j+58)
            drawTable(mouseX, mouseY, 68, ITEMS_PER_ROW, 18);
        } else {
            drawManageTab(mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title;
        if (selectedTab == 0) {
            title = "Index Navigation";
        } else if (selectedTab == 1) {
            title = "Remote Retrieval";
        } else {
            title = "Manage Chests";
        }

        // Keep titles small and top-aligned
        this.fontRenderer.drawString(title, 8, -10, 0xFFFFFF); // Moved outside main rect for clarity or just very top

        if (selectedTab != 2) {
            this.fontRenderer.drawString("Search:", 8, 28, 0x404040);
        }

        if (selectedTab == 1) {
            this.fontRenderer.drawString("Output:", 135, 64, 0x404040);
        }

        // Inventory label (Y=170 + padding)
        this.fontRenderer.drawString(I18n.format("container.inventory"), 12, 172, 0x404040);
    }

    private void drawTable(int mouseX, int mouseY, int startY, int itemsPerRow, int spacing) {
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;

        int x = 8;
        int y = startY;
        int displayedItems = 0;
        int startIndex = scrollOffset * itemsPerRow;

        // Headers (10px above startY)
        int headerY = guiTop + startY - 10;
        this.fontRenderer.drawString("Num", guiLeft + x, headerY, 0x333333);
        this.fontRenderer.drawString("Item", guiLeft + x + 18, headerY, 0x333333);
        this.fontRenderer.drawString("Name", guiLeft + x + 36, headerY, 0x333333);
        this.fontRenderer.drawString("Qty", guiLeft + x + ((selectedTab == 1) ? 95 : 130), headerY, 0x333333);

        RenderHelper.enableGUIStandardItemLighting();

        for (int i = startIndex; i < filteredIndex.size() && displayedItems < VISIBLE_ROWS; i++) {
            PacketInventoryIndexSync.IndexEntry entry = filteredIndex.get(i);
            int currentItemY = guiTop + y;

            int rowWidth = (selectedTab == 1) ? 120 : 155;
            int bgColor = (i % 2 == 0) ? 0x44000000 : 0x22000000;
            drawRect(guiLeft + x - 1, currentItemY - 1, guiLeft + x + rowWidth, currentItemY + 17, bgColor);

            this.fontRenderer.drawString(String.valueOf(i + 1), guiLeft + x, currentItemY + 4, 0xDDDDDD);

            int iconX = guiLeft + x + 18;
            this.itemRender.renderItemAndEffectIntoGUI(entry.stack, iconX, currentItemY);

            String itemName = entry.stack.getDisplayName();
            int nameLimit = (selectedTab == 1) ? 60 : 90;
            if (this.fontRenderer.getStringWidth(itemName) > nameLimit) {
                itemName = this.fontRenderer.trimStringToWidth(itemName, nameLimit - 5) + "..";
            }
            this.fontRenderer.drawString(itemName, guiLeft + x + 38, currentItemY + 4, 0xFFFFFF);

            String countStr = compactCount(entry.count);
            int countX = guiLeft + x + ((selectedTab == 1) ? 95 : 130);
            this.fontRenderer.drawString(countStr, countX, currentItemY + 4, 0xAAFFAA);

            y += spacing;
            displayedItems++;
        }
        RenderHelper.disableStandardItemLighting();

        // Scroll info
        if (filteredIndex.size() > VISIBLE_ROWS) {
            int maxScroll = (filteredIndex.size() - 1) / ITEMS_PER_ROW - VISIBLE_ROWS + 1;
            this.fontRenderer.drawString((scrollOffset + 1) + "/" + (maxScroll + 1), guiLeft + 155, guiTop + 42, 0x444444);
        }
    }

    private void drawManageTab(int mouseX, int mouseY) {
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;

        this.fontRenderer.drawString("Linked Chests (" + linkedChestsList.size() + "):", guiLeft + 8, guiTop + 54, 0x404040);
        drawRect(guiLeft + 7, guiTop + 65, guiLeft + 175, guiTop + 175, 0xFF555555);

        int y = guiTop + 67;
        for (int i = manageScrollOffset; i < linkedChestsList.size() && i < manageScrollOffset + 9; i++) { // More items visible
            BlockPos pos = linkedChestsList.get(i);
            if (i == selectedChestIndex) {
                drawRect(guiLeft + 8, y - 1, guiLeft + 174, y + 10, 0xFF3366AA);
            }
            String text = (i + 1) + ". " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            this.fontRenderer.drawString(text, guiLeft + 10, y + 1, i == selectedChestIndex ? 0xFFFFFF : 0xDDDDDD);
            y += 12;
        }

        if (linkedChestsList.isEmpty()) {
            this.fontRenderer.drawString("No chests linked.", guiLeft + 15, guiTop + 80, 0xAAAAAA);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // Check buttons FIRST
        for (int i = 0; i < this.buttonList.size(); ++i) {
            GuiButton guibutton = this.buttonList.get(i);
            if (guibutton.mousePressed(this.mc, mouseX, mouseY)) {
                this.actionPerformed(guibutton);
                return;
            }
        }

        if (this.searchField.getVisible()) {
            this.searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (selectedTab == 2) {
            handleManageTabClick(mouseX, mouseY);
        } else {
            handleTableClick(mouseX, mouseY);
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void handleTableClick(int mouseX, int mouseY) {
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;
        int startY = 68; // Match drawTable
        int spacing = 18;

        // Collision check for table area background (j+55 to j+165)
        if (mouseY < guiTop + 55 || mouseY > guiTop + 165) return;

        int displayedItems = 0;
        int startIndex = scrollOffset * ITEMS_PER_ROW;

        for (int idx = startIndex; idx < filteredIndex.size() && displayedItems < VISIBLE_ROWS; idx++) {
            PacketInventoryIndexSync.IndexEntry entry = filteredIndex.get(idx);
            int itemY = guiTop + startY + (displayedItems * spacing);

            int rowWidth = (selectedTab == 1) ? 120 : 155;

            if (mouseX >= guiLeft + 8 && mouseX < guiLeft + 8 + rowWidth && mouseY >= itemY && mouseY < itemY + 16) {
                if (selectedTab == 0) {
                    RenderPathOverlay.setTarget(entry.locations);
                    this.mc.player.closeScreen();
                    this.mc.player.sendMessage(new TextComponentString("\u00a7aNavigation set to " + entry.stack.getDisplayName()));
                } else if (selectedTab == 1) {
                    int amount = isShiftKeyDown() ? 64 : 1;
                    PacketHandler.INSTANCE.sendToServer(new PacketRequestItem(tile.getPos(), entry.stack, amount));
                }
                return;
            }
            displayedItems++;
        }
    }

    private void handleManageTabClick(int mouseX, int mouseY) {
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;
        int listY = guiTop + 65;

        if (mouseX >= guiLeft + 7 && mouseX < guiLeft + 175 && mouseY >= listY && mouseY < listY + 110) {
            int clickedIdx = (mouseY - listY) / 12 + manageScrollOffset;
            if (clickedIdx >= 0 && clickedIdx < linkedChestsList.size()) {
                selectedChestIndex = clickedIdx;
                updateSlotsVisibility();
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.searchField.textboxKeyTyped(typedChar, keyCode)) {
            if (!lastSearchText.equals(searchField.getText())) {
                lastSearchText = searchField.getText();
                updateFilteredList();
            }
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.searchField.updateCursorCounter();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            // Faster scrolling (5 lines per notch)
            int scrollAmount = 5;
            if (selectedTab == 2) {
                int maxScroll = Math.max(0, linkedChestsList.size() - 9);
                if (wheel > 0) manageScrollOffset = Math.max(0, manageScrollOffset - scrollAmount);
                else manageScrollOffset = Math.min(maxScroll, manageScrollOffset + scrollAmount);
            } else {
                int maxScroll = Math.max(0, (filteredIndex.size() - 1) / ITEMS_PER_ROW - VISIBLE_ROWS + 1);
                if (wheel > 0) scrollOffset = Math.max(0, scrollOffset - scrollAmount);
                else scrollOffset = Math.min(maxScroll, scrollOffset + scrollAmount);
            }
        }
    }

    private String compactCount(int count) {
        if (count >= 1000000) return (count / 1000000) + "M";
        if (count >= 1000) return (count / 1000) + "k";
        return String.valueOf(count);
    }
}

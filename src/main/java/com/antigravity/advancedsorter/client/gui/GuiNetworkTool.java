package com.antigravity.advancedsorter.client.gui;

import com.antigravity.advancedsorter.blocks.ItemNetworkTool;
import com.antigravity.advancedsorter.tiles.TileInventoryIndex;
import com.antigravity.advancedsorter.network.PacketHandler;
import com.antigravity.advancedsorter.network.NetworkToolClientCache;
import com.antigravity.advancedsorter.tiles.ChestGroup;
import com.antigravity.advancedsorter.network.PacketManageChestGroup;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI for Network Tool - manages chest linking to Indexer
 */
public class GuiNetworkTool extends GuiScreen {

    private final EntityPlayer player;
    private ItemStack toolStack;

    private List<BlockPos> markedChests;
    private BlockPos selectedIndexerPos;
    private TileInventoryIndex selectedIndexer;

    // Lists of chests already linked to the indexer
    private List<BlockPos> linkedChests;

    // Selection
    private int selectedMarkedIndex = -1;
    private int selectedLinkedIndex = -1;

    // Scroll
    private int markedScrollOffset = 0;
    private int linkedScrollOffset = 0;
    private static final int VISIBLE_ITEMS = 6;

    // Buttons
    private GuiButton btnAddSelected;
    private GuiButton btnAddAll;
    private GuiButton btnRemoveSelected;
    private GuiButton btnClearMarked;
    private GuiButton btnClose;

    private int panelX, panelY;
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 180;

    public GuiNetworkTool(EntityPlayer player) {
        this.player = player;
        refreshData();
    }

    private void refreshData() {
        this.toolStack = player.getHeldItemMainhand();

        // Load data from client cache (synced from server)
        this.markedChests = new ArrayList<>(NetworkToolClientCache.markedChests);
        this.selectedIndexerPos = NetworkToolClientCache.selectedIndexer;
        this.linkedChests = new ArrayList<>();

        // Get linked chests from indexer
        if (selectedIndexerPos != null && player.world != null) {
            TileEntity tile = player.world.getTileEntity(selectedIndexerPos);
            if (tile instanceof TileInventoryIndex) {
                selectedIndexer = (TileInventoryIndex) tile;
                linkedChests.addAll(selectedIndexer.getLinkedChests());
                // Also add chests from groups
                for (ChestGroup group : selectedIndexer.getChestGroups()) {
                    linkedChests.addAll(group.getChestPositions());
                }
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        panelX = (this.width - PANEL_WIDTH) / 2;
        panelY = (this.height - PANEL_HEIGHT) / 2;

        int btnY = panelY + PANEL_HEIGHT - 28;

        btnAddSelected = new GuiButton(1, panelX + 10, btnY, 60, 20, "Add");
        btnAddAll = new GuiButton(2, panelX + 75, btnY, 60, 20, "Add All");
        btnRemoveSelected = new GuiButton(3, panelX + 145, btnY, 60, 20, "Remove");
        btnClearMarked = new GuiButton(4, panelX + 210, btnY, 60, 20, "Clear");
        btnClose = new GuiButton(5, panelX + PANEL_WIDTH - 50, panelY + 5, 45, 14, "Close");

        this.buttonList.add(btnAddSelected);
        this.buttonList.add(btnAddAll);
        this.buttonList.add(btnRemoveSelected);
        this.buttonList.add(btnClearMarked);
        this.buttonList.add(btnClose);

        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasIndexer = selectedIndexer != null;
        boolean hasMarked = !markedChests.isEmpty();
        boolean hasMarkedSelection = selectedMarkedIndex >= 0 && selectedMarkedIndex < markedChests.size();
        boolean hasLinkedSelection = selectedLinkedIndex >= 0 && selectedLinkedIndex < linkedChests.size();

        btnAddSelected.enabled = hasIndexer && hasMarkedSelection;
        btnAddAll.enabled = hasIndexer && hasMarked;
        btnRemoveSelected.enabled = hasIndexer && hasLinkedSelection;
        btnClearMarked.enabled = hasMarked;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 1: // Add Selected
                if (selectedMarkedIndex >= 0 && selectedMarkedIndex < markedChests.size() && selectedIndexerPos != null) {
                    BlockPos pos = markedChests.get(selectedMarkedIndex);
                    PacketHandler.INSTANCE.sendToServer(
                            PacketManageChestGroup.addIndividualChest(selectedIndexerPos, pos));
                    markedChests.remove(selectedMarkedIndex);
                    // Update client cache
                    NetworkToolClientCache.markedChests = new ArrayList<>(markedChests);
                    linkedChests.add(pos);
                    selectedMarkedIndex = -1;
                }
                break;

            case 2: // Add All
                if (selectedIndexerPos != null) {
                    for (BlockPos pos : markedChests) {
                        PacketHandler.INSTANCE.sendToServer(
                                PacketManageChestGroup.addIndividualChest(selectedIndexerPos, pos));
                        linkedChests.add(pos);
                    }
                    markedChests.clear();
                    // Update client cache
                    NetworkToolClientCache.markedChests.clear();
                    selectedMarkedIndex = -1;
                }
                break;

            case 3: // Remove Selected
                if (selectedLinkedIndex >= 0 && selectedLinkedIndex < linkedChests.size() && selectedIndexerPos != null) {
                    BlockPos pos = linkedChests.get(selectedLinkedIndex);
                    // Check if in group or individual
                    if (selectedIndexer != null) {
                        if (selectedIndexer.isChestInAnyGroup(pos)) {
                            PacketHandler.INSTANCE.sendToServer(
                                    PacketManageChestGroup.removeChestFromGroup(selectedIndexerPos, pos, false));
                        } else {
                            PacketHandler.INSTANCE.sendToServer(
                                    PacketManageChestGroup.removeIndividualChest(selectedIndexerPos, pos));
                        }
                    }
                    linkedChests.remove(selectedLinkedIndex);
                    selectedLinkedIndex = -1;
                }
                break;

            case 4: // Clear Marked
                markedChests.clear();
                // Update client cache
                NetworkToolClientCache.markedChests.clear();
                selectedMarkedIndex = -1;
                break;

            case 5: // Close
                this.mc.displayGuiScreen(null);
                return;
        }
        updateButtonStates();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        // Panel background
        drawRect(panelX - 2, panelY - 2, panelX + PANEL_WIDTH + 2, panelY + PANEL_HEIGHT + 2, 0xFF222222);
        drawRect(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF444444);

        // Title
        String title = "Network Tool Manager";
        this.drawCenteredString(this.fontRenderer, title, panelX + PANEL_WIDTH / 2, panelY + 8, 0xFFFFFF);

        // Indexer info
        if (selectedIndexerPos != null) {
            String info = "Indexer: " + selectedIndexerPos.getX() + ", " + selectedIndexerPos.getY() + ", " + selectedIndexerPos.getZ();
            this.fontRenderer.drawString(info, panelX + 10, panelY + 22, 0x55FF55);
        } else {
            this.fontRenderer.drawString("No Indexer selected!", panelX + 10, panelY + 22, 0xFF5555);
        }

        // Left column - Marked Chests
        int leftX = panelX + 10;
        int listY = panelY + 38;
        this.fontRenderer.drawString("Marked (" + markedChests.size() + "):", leftX, listY, 0xFFFF55);

        drawRect(leftX, listY + 12, leftX + 125, listY + 12 + VISIBLE_ITEMS * 14 + 4, 0xFF333333);

        int idx = 0;
        for (int i = markedScrollOffset; i < markedChests.size() && idx < VISIBLE_ITEMS; i++) {
            BlockPos pos = markedChests.get(i);
            int itemY = listY + 14 + idx * 14;

            // Selection highlight
            if (i == selectedMarkedIndex) {
                drawRect(leftX + 1, itemY - 1, leftX + 124, itemY + 11, 0xFF666699);
            }

            String text = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            this.fontRenderer.drawString(text, leftX + 4, itemY + 1, 0xCCCCCC);
            idx++;
        }

        // Scroll indicators for marked
        if (markedScrollOffset > 0) {
            this.fontRenderer.drawString("^", leftX + 115, listY + 14, 0xAAAAAA);
        }
        if (markedScrollOffset + VISIBLE_ITEMS < markedChests.size()) {
            this.fontRenderer.drawString("v", leftX + 115, listY + 12 + VISIBLE_ITEMS * 14 - 8, 0xAAAAAA);
        }

        // Right column - Linked Chests
        int rightX = panelX + 145;
        this.fontRenderer.drawString("Linked (" + linkedChests.size() + "):", rightX, listY, 0x55FF55);

        drawRect(rightX, listY + 12, rightX + 125, listY + 12 + VISIBLE_ITEMS * 14 + 4, 0xFF333333);

        idx = 0;
        for (int i = linkedScrollOffset; i < linkedChests.size() && idx < VISIBLE_ITEMS; i++) {
            BlockPos pos = linkedChests.get(i);
            int itemY = listY + 14 + idx * 14;

            // Selection highlight
            if (i == selectedLinkedIndex) {
                drawRect(rightX + 1, itemY - 1, rightX + 124, itemY + 11, 0xFF669966);
            }

            String text = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            this.fontRenderer.drawString(text, rightX + 4, itemY + 1, 0xCCCCCC);
            idx++;
        }

        // Scroll indicators for linked
        if (linkedScrollOffset > 0) {
            this.fontRenderer.drawString("^", rightX + 115, listY + 14, 0xAAAAAA);
        }
        if (linkedScrollOffset + VISIBLE_ITEMS < linkedChests.size()) {
            this.fontRenderer.drawString("v", rightX + 115, listY + 12 + VISIBLE_ITEMS * 14 - 8, 0xAAAAAA);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        int leftX = panelX + 10;
        int rightX = panelX + 145;
        int listY = panelY + 38 + 12;
        int listHeight = VISIBLE_ITEMS * 14 + 4;

        // Click on marked list
        if (mouseX >= leftX && mouseX < leftX + 125 && mouseY >= listY && mouseY < listY + listHeight) {
            int clickedIdx = (mouseY - listY - 2) / 14 + markedScrollOffset;
            if (clickedIdx >= 0 && clickedIdx < markedChests.size()) {
                selectedMarkedIndex = clickedIdx;
                selectedLinkedIndex = -1; // Deselect other
            }
        }

        // Click on linked list
        if (mouseX >= rightX && mouseX < rightX + 125 && mouseY >= listY && mouseY < listY + listHeight) {
            int clickedIdx = (mouseY - listY - 2) / 14 + linkedScrollOffset;
            if (clickedIdx >= 0 && clickedIdx < linkedChests.size()) {
                selectedLinkedIndex = clickedIdx;
                selectedMarkedIndex = -1; // Deselect other
            }
        }

        updateButtonStates();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getDWheel();
        if (wheel != 0) {
            int mouseX = org.lwjgl.input.Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - org.lwjgl.input.Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

            int leftX = panelX + 10;
            int rightX = panelX + 145;
            int listY = panelY + 38 + 12;
            int listHeight = VISIBLE_ITEMS * 14 + 4;

            // Scroll marked list
            if (mouseX >= leftX && mouseX < leftX + 125 && mouseY >= listY && mouseY < listY + listHeight) {
                if (wheel > 0 && markedScrollOffset > 0) {
                    markedScrollOffset--;
                } else if (wheel < 0 && markedScrollOffset < markedChests.size() - VISIBLE_ITEMS) {
                    markedScrollOffset++;
                }
            }

            // Scroll linked list
            if (mouseX >= rightX && mouseX < rightX + 125 && mouseY >= listY && mouseY < listY + listHeight) {
                if (wheel > 0 && linkedScrollOffset > 0) {
                    linkedScrollOffset--;
                } else if (wheel < 0 && linkedScrollOffset < linkedChests.size() - VISIBLE_ITEMS) {
                    linkedScrollOffset++;
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_E) {
            this.mc.displayGuiScreen(null);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

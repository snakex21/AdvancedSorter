package com.antigravity.advancedsorter.client.gui;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuiItemSelector extends GuiScreen {

    private final GuiScreen parent;
    private final IItemSelectorCallback callback;
    private GuiTextField searchField;
    private List<ItemStack> allItems;
    private List<ItemStack> filteredItems;

    // Grid layout
    private int gridX, gridY, gridWidth, gridHeight;
    private int slotSize = 18;
    private int columns;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public GuiItemSelector(GuiScreen parent, IItemSelectorCallback callback) {
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        this.searchField = new GuiTextField(0, this.fontRenderer, this.width / 2 - 80, 20, 160, 20);
        this.searchField.setFocused(true);
        this.searchField.setCanLoseFocus(false);

        this.gridX = this.width / 2 - 100;
        this.gridY = 50;
        this.gridWidth = 200;
        this.gridHeight = this.height - 60;
        this.columns = this.gridWidth / slotSize;

        loadItems();
        updateFilter();
    }

    private void loadItems() {
        if (allItems == null) {
            allItems = new ArrayList<>();
            for (Item item : ForgeRegistries.ITEMS) {
                if (item == null)
                    continue;
                try {
                    NonNullList<ItemStack> subItems = NonNullList.create();
                    item.getSubItems(CreativeTabs.SEARCH, subItems);
                    if (subItems.isEmpty()) {
                        allItems.add(new ItemStack(item));
                    } else {
                        allItems.addAll(subItems);
                    }
                } catch (Exception e) {
                    // Ignore items that crash on getSubItems
                    AdvancedSorterMod.logger.error("Failed to load subitems for " + item.getRegistryName(), e);
                }
            }
        }
    }

    private void updateFilter() {
        String query = searchField.getText().toLowerCase(Locale.ROOT);
        filteredItems = new ArrayList<>();

        for (ItemStack stack : allItems) {
            if (query.isEmpty()) {
                filteredItems.add(stack);
                continue;
            }

            String name = stack.getDisplayName().toLowerCase(Locale.ROOT);
            String id = stack.getItem().getRegistryName().toString().toLowerCase(Locale.ROOT);

            if (name.contains(query) || id.contains(query)) {
                filteredItems.add(stack);
            }
        }

        // Recalculate scroll
        int totalRows = (int) Math.ceil((double) filteredItems.size() / columns);
        int visibleRows = gridHeight / slotSize;
        maxScroll = Math.max(0, totalRows - visibleRows);
        scrollOffset = Math.min(scrollOffset, maxScroll);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.searchField.drawTextBox();

        this.drawCenteredString(this.fontRenderer, "Select Item", this.width / 2, 5, 0xFFFFFF);

        // Draw Grid Background
        drawRect(gridX - 1, gridY - 1, gridX + gridWidth + 1, gridY + gridHeight + 1, 0xFF444444);
        drawRect(gridX, gridY, gridX + gridWidth, gridY + gridHeight, 0xFF000000);

        // Draw Items
        int startIndex = scrollOffset * columns;
        int endIndex = Math.min(startIndex + (columns * (gridHeight / slotSize)), filteredItems.size());

        RenderHelper.enableGUIStandardItemLighting();

        for (int i = startIndex; i < endIndex; i++) {
            ItemStack stack = filteredItems.get(i);
            int relIndex = i - startIndex;
            int col = relIndex % columns;
            int row = relIndex / columns;

            int x = gridX + col * slotSize + 1;
            int y = gridY + row * slotSize + 1;

            // Highlight hover
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                drawRect(x, y, x + 16, y + 16, 0x80FFFFFF);
            }

            this.itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        }

        RenderHelper.disableStandardItemLighting();

        // Tooltip
        for (int i = startIndex; i < endIndex; i++) {
            ItemStack stack = filteredItems.get(i);
            int relIndex = i - startIndex;
            int col = relIndex % columns;
            int row = relIndex / columns;

            int x = gridX + col * slotSize + 1;
            int y = gridY + row * slotSize + 1;

            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                this.renderToolTip(stack, mouseX, mouseY);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            if (dWheel > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else {
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.searchField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0 && mouseX >= gridX && mouseX < gridX + gridWidth && mouseY >= gridY
                && mouseY < gridY + gridHeight) {
            int col = (mouseX - gridX) / slotSize;
            int row = (mouseY - gridY) / slotSize;

            int index = (scrollOffset + row) * columns + col;
            if (index >= 0 && index < filteredItems.size()) {
                ItemStack selected = filteredItems.get(index);
                callback.onSelectionConfirmed(selected);
                this.mc.displayGuiScreen(parent);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.searchField.textboxKeyTyped(typedChar, keyCode)) {
            updateFilter();
        } else if (keyCode == 1) { // Escape
            this.mc.displayGuiScreen(parent);
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }
}

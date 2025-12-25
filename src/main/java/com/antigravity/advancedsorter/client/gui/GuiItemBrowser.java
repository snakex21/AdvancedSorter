package com.antigravity.advancedsorter.client.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI for browsing and searching ALL items in the game.
 * Allows adding items to a list without having them in inventory.
 */
public class GuiItemBrowser extends GuiScreen {
    private final GuiScreen parentScreen;
    private final IItemSelectorCallback callback;
    private final boolean multiSelectMode;
    private List<ItemStack> selectedItems; // Items already in exceptions (for visual feedback)

    private GuiTextField searchField;
    private List<ItemStack> allItems;
    private List<ItemStack> filteredItems;
    private int scrollOffset = 0;

    private static final int GRID_COLUMNS = 9;
    private static final int GRID_ROWS = 8;
    private static final int ITEMS_PER_PAGE = GRID_COLUMNS * GRID_ROWS;

    public GuiItemBrowser(GuiScreen parent, IItemSelectorCallback callback) {
        this.parentScreen = parent;
        this.callback = callback;
        this.multiSelectMode = false;
        this.selectedItems = new ArrayList<>();
        this.allItems = new ArrayList<>();
        this.filteredItems = new ArrayList<>();

        // Load all items from registry
        for (Item item : ForgeRegistries.ITEMS) {
            try {
                ItemStack stack = new ItemStack(item);
                if (!stack.isEmpty()) {
                    allItems.add(stack);
                }
            } catch (Exception e) {
                // Skip invalid items
            }
        }

        filteredItems.addAll(allItems);
    }

    /**
     * Constructor for Exceptions mode - shows only pre-filtered items.
     * Multi-select is enabled: browser stays open after each selection.
     */
    public GuiItemBrowser(GuiScreen parent, IItemSelectorCallback callback, List<ItemStack> preFilteredItems) {
        this.parentScreen = parent;
        this.callback = callback;
        this.multiSelectMode = true;
        this.selectedItems = new ArrayList<>(); // Will be populated when items are clicked
        this.allItems = new ArrayList<>(preFilteredItems);
        this.filteredItems = new ArrayList<>(preFilteredItems);
    }

    /**
     * Constructor for Exceptions mode with initial selections.
     * Items in alreadySelected will be shown with green highlight.
     */
    public GuiItemBrowser(GuiScreen parent, IItemSelectorCallback callback, List<ItemStack> preFilteredItems,
            List<ItemStack> alreadySelected) {
        this.parentScreen = parent;
        this.callback = callback;
        this.multiSelectMode = true;
        this.selectedItems = new ArrayList<>(alreadySelected);
        this.allItems = new ArrayList<>(preFilteredItems);
        this.filteredItems = new ArrayList<>(preFilteredItems);
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        int searchWidth = 200;
        int searchX = (this.width - searchWidth) / 2;
        int searchY = 20;

        searchField = new GuiTextField(0, this.fontRenderer, searchX, searchY, searchWidth, 20);
        searchField.setMaxStringLength(50);
        searchField.setFocused(true);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Dark background
        this.drawDefaultBackground();

        int panelWidth = GRID_COLUMNS * 18 + 20;
        int panelHeight = GRID_ROWS * 18 + 80;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 50;

        // Panel background
        drawRect(panelX - 2, panelY - 2, panelX + panelWidth + 2, panelY + panelHeight + 2, 0xFF444444);
        drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFFC6C6C6);

        // Title
        String title = "Item Browser (" + filteredItems.size() + " items)";
        this.drawCenteredString(this.fontRenderer, title, this.width / 2, panelY + 5, 0x404040);

        // Draw items grid
        int startIndex = scrollOffset * GRID_COLUMNS;
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                int index = startIndex + row * GRID_COLUMNS + col;
                int slotX = panelX + 10 + col * 18;
                int slotY = panelY + 25 + row * 18;

                if (index < filteredItems.size()) {
                    ItemStack stack = filteredItems.get(index);
                    // Slot background - green if selected, gray otherwise
                    boolean selected = isItemSelected(stack);
                    int bgColor = selected ? 0xFF44AA44 : 0xFF888888;
                    drawRect(slotX, slotY, slotX + 18, slotY + 18, bgColor);

                    RenderHelper.enableGUIStandardItemLighting();
                    this.itemRender.renderItemAndEffectIntoGUI(stack, slotX + 1, slotY + 1);
                    RenderHelper.disableStandardItemLighting();
                } else {
                    // Empty slot
                    drawRect(slotX, slotY, slotX + 18, slotY + 18, 0xFF888888);
                }
            }
        }

        // Close button
        int closeX = panelX + (panelWidth - 50) / 2;
        int closeY = panelY + panelHeight - 20;
        drawRect(closeX, closeY, closeX + 50, closeY + 15, 0xFF666699);
        this.drawCenteredString(this.fontRenderer, "Close", closeX + 25, closeY + 3, 0xFFFFFF);

        // Scroll info
        int maxScroll = Math.max(0, (filteredItems.size() - 1) / GRID_COLUMNS - GRID_ROWS + 1);
        if (maxScroll > 0) {
            String scrollText = "Page: " + (scrollOffset + 1) + "/" + (maxScroll + 1);
            this.fontRenderer.drawString(scrollText, panelX + 10, closeY + 3, 0x404040);
        }

        // Search field
        searchField.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        searchField.mouseClicked(mouseX, mouseY, mouseButton);

        int panelWidth = GRID_COLUMNS * 18 + 20;
        int panelHeight = GRID_ROWS * 18 + 80;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 50;

        // Close button
        int closeX = panelX + (panelWidth - 50) / 2;
        int closeY = panelY + panelHeight - 20;
        if (mouseX >= closeX && mouseX < closeX + 50 && mouseY >= closeY && mouseY < closeY + 15) {
            this.mc.displayGuiScreen(parentScreen);
            return;
        }

        // Item click
        int startIndex = scrollOffset * GRID_COLUMNS;
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                int index = startIndex + row * GRID_COLUMNS + col;
                int slotX = panelX + 10 + col * 18;
                int slotY = panelY + 25 + row * 18;

                if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                    if (index < filteredItems.size()) {
                        ItemStack clicked = filteredItems.get(index);

                        if (multiSelectMode) {
                            // Toggle selection
                            if (isItemSelected(clicked)) {
                                // Remove from selection
                                removeFromSelected(clicked);
                            } else {
                                // Add to selection
                                selectedItems.add(clicked.copy());
                                callback.onSelectionConfirmed(clicked.copy());
                            }
                        } else {
                            // Single select mode - just add and close
                            callback.onSelectionConfirmed(clicked.copy());
                            this.mc.displayGuiScreen(parentScreen);
                        }
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getDWheel();
        if (wheel != 0) {
            int maxScroll = Math.max(0, (filteredItems.size() - 1) / GRID_COLUMNS - GRID_ROWS + 1);
            if (wheel > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else {
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchField.textboxKeyTyped(typedChar, keyCode)) {
            filterItems();
        } else if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(parentScreen);
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    private void filterItems() {
        String search = searchField.getText().toLowerCase().trim();
        filteredItems.clear();

        if (search.isEmpty()) {
            filteredItems.addAll(allItems);
        } else {
            for (ItemStack stack : allItems) {
                String itemName = stack.getDisplayName().toLowerCase();
                if (itemName.contains(search)) {
                    filteredItems.add(stack);
                }
            }
        }

        scrollOffset = 0;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /**
     * Check if item is already selected (in exceptions list)
     */
    private boolean isItemSelected(ItemStack stack) {
        for (ItemStack s : selectedItems) {
            if (s.isItemEqual(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove item from selected list (for toggle deselect)
     */
    private void removeFromSelected(ItemStack stack) {
        selectedItems.removeIf(s -> s.isItemEqual(stack));
        // Also remove from actual exceptionItems via callback
        if (callback instanceof IItemDeselectionCallback) {
            ((IItemDeselectionCallback) callback).onItemDeselected(stack);
        }
    }

    /**
     * Optional interface for handling deselection
     */
    public interface IItemDeselectionCallback {
        void onItemDeselected(ItemStack stack);
    }
}

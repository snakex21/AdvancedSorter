package com.antigravity.advancedsorter.client.gui;

import com.antigravity.advancedsorter.AdvancedSorterMod;
// import com.antigravity.advancedsorter.util.RecipeTreeCalculator;
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
import java.util.Map;

public class GuiCraftingCalculator extends GuiScreen {

    private final GuiScreen parent;
    private GuiTextField searchField;
    private List<ItemStack> allItems;
    private List<ItemStack> filteredItems;
    private ItemStack selectedItem = ItemStack.EMPTY;
    // private RecipeTreeCalculator.MaterialCount materialCount;
    private Object materialCount; // Placeholder to avoid more errors if used elsewhere

    // Grid layout for search results
    private int gridX, gridY, gridWidth, gridHeight;
    private int slotSize = 18;
    private int columns;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    // Materials display
    private int matX, matY, matWidth, matHeight;
    private int matScrollOffset = 0;
    private int matMaxScroll = 0;

    public GuiCraftingCalculator(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        this.searchField = new GuiTextField(0, this.fontRenderer, 10, 20, 150, 20);
        this.searchField.setFocused(true);
        this.searchField.setCanLoseFocus(false);

        this.gridX = 10;
        this.gridY = 50;
        this.gridWidth = 150;
        this.gridHeight = this.height - 60;
        this.columns = gridWidth / slotSize;

        this.matX = 180;
        this.matY = 50;
        this.matWidth = this.width - 190;
        this.matHeight = this.height - 60;

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

        int totalRows = (int) Math.ceil((double) filteredItems.size() / columns);
        int visibleRows = gridHeight / slotSize;
        maxScroll = Math.max(0, totalRows - visibleRows);
        scrollOffset = Math.min(scrollOffset, maxScroll);
    }

    private void selectItem(ItemStack stack) {
        this.selectedItem = stack;
        if (!stack.isEmpty()) {
            // this.materialCount = RecipeTreeCalculator.calculateTotalMaterials(stack, 1);
            // Calculate matMaxScroll
            // int totalMats = materialCount.materials.size();
            // int visibleMats = matHeight / 20;
            // matMaxScroll = Math.max(0, totalMats - visibleMats);
            matMaxScroll = 0;
            matScrollOffset = 0;
        } else {
            this.materialCount = null;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.searchField.drawTextBox();

        this.drawCenteredString(this.fontRenderer, "Crafting Calculator", this.width / 2, 5, 0xFFFFFF);
        this.fontRenderer.drawString("Search Items:", 10, 10, 0xAAAAAA);

        // Draw Search Grid
        drawRect(gridX - 1, gridY - 1, gridX + gridWidth + 1, gridY + gridHeight + 1, 0xFF444444);
        drawRect(gridX, gridY, gridX + gridWidth, gridY + gridHeight, 0xFF000000);

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

            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                drawRect(x, y, x + 16, y + 16, 0x80FFFFFF);
            }

            if (selectedItem != null && ItemStack.areItemStacksEqual(selectedItem, stack)) {
                drawRect(x - 1, y - 1, x + 17, y + 17, 0x8000FF00);
            }

            this.itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        }
        RenderHelper.disableStandardItemLighting();

        // Draw Materials Panel
        drawRect(matX - 1, matY - 1, matX + matWidth + 1, matY + matHeight + 1, 0xFF444444);
        drawRect(matX, matY, matX + matWidth, matY + matHeight, 0xFF222222);

        if (selectedItem.isEmpty()) {
            this.drawCenteredString(this.fontRenderer, "Select an item to see materials", matX + matWidth / 2,
                    matY + matHeight / 2 - 5, 0x888888);
        } else {
            this.fontRenderer.drawString("Total Materials for: " + selectedItem.getDisplayName(), matX + 5, matY - 15,
                    0xFFFFFF);

            if (materialCount != null) {
                int yOffset = matY + 5;
                int count = 0;
                // List<Map.Entry<String, Integer>> entries = new
                // ArrayList<>(materialCount.materials.entrySet());
                List<Map.Entry<String, Integer>> entries = new ArrayList<>(); // Placeholder

                for (int i = matScrollOffset; i < entries.size() && count < (matHeight / 20); i++) {
                    Map.Entry<String, Integer> entry = entries.get(i);
                    String[] parts = entry.getKey().split(":");
                    Item item = Item.getByNameOrId(parts[0] + ":" + parts[1]);
                    int meta = Integer.parseInt(parts[2]);
                    ItemStack matStack = new ItemStack(item, 1, meta);

                    RenderHelper.enableGUIStandardItemLighting();
                    this.itemRender.renderItemAndEffectIntoGUI(matStack, matX + 5, yOffset);
                    RenderHelper.disableStandardItemLighting();

                    String text = entry.getValue() + "x " + matStack.getDisplayName();
                    this.fontRenderer.drawString(text, matX + 25, yOffset + 4, 0xFFFFFF);

                    yOffset += 20;
                    count++;
                }
            }
        }

        // Tooltips for search grid
        for (int i = startIndex; i < endIndex; i++) {
            ItemStack stack = filteredItems.get(i);
            int relIndex = i - startIndex;
            int col = relIndex % columns;
            int row = relIndex / columns;
            int x = gridX + col * slotSize + 1;
            int y = gridY + row * slotSize + 1;

            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
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
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

            if (mouseX >= gridX && mouseX < gridX + gridWidth) {
                if (dWheel > 0)
                    scrollOffset = Math.max(0, scrollOffset - 1);
                else
                    scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            } else if (mouseX >= matX && mouseX < matX + matWidth) {
                if (dWheel > 0)
                    matScrollOffset = Math.max(0, matScrollOffset - 1);
                else
                    matScrollOffset = Math.min(matMaxScroll, matScrollOffset + 1);
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
                selectItem(filteredItems.get(index));
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

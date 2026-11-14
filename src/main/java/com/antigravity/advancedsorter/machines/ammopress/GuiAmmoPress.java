package com.antigravity.advancedsorter.machines.ammopress;

import com.antigravity.advancedsorter.autocrafter.CraftingRecipe;
import com.antigravity.advancedsorter.network.PacketAmmoPressAction;
import com.antigravity.advancedsorter.network.PacketHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom Dark/Military Themed GUI for Ammo Press.
 */
public class GuiAmmoPress extends GuiContainer {

    private final TileAmmoPress tile;
    private final ContainerAmmoPress container;

    // Default tab = 1 (Craft)
    private int selectedTab = 1;

    // Search
    private GuiTextField searchField;
    private List<CraftingRecipe> filteredRecipes = new ArrayList<>();

    // Buttons
    private GuiButton btnTabRecipes;
    private GuiButton btnTabCraft;
    private GuiButton btnTabAuto;

    private GuiButton btnAddRecipe;
    private GuiButton btnDeleteRecipe;
    private GuiButton btnClearGrid;

    private GuiButton btnCraftOne;
    private GuiButton btnCraftStack;

    private GuiButton btnToggleAuto;
    private GuiButton btnAddToAuto;
    private GuiButton btnRemoveFromAuto;
    private GuiButton btnToggleMode;

    // Scroll and selection
    private int scrollOffset = 0;
    private int selectedRecipeIndex = -1;

    // Theme Colors
    private static final int COLOR_BG_MAIN = 0xFF252525;
    private static final int COLOR_BG_BORDER = 0xFF505050;
    private static final int COLOR_SLOT_BG = 0xFF151515;
    private static final int COLOR_SLOT_BORDER = 0xFF353535;
    private static final int COLOR_TEXT = 0xFFCCCCCC;
    private static final int COLOR_TEXT_HEADER = 0xFFFFFFFF;
    private static final int COLOR_ENERGY_BG = 0xFF101010;
    private static final int COLOR_ENERGY_START = 0xFF00AA00;
    private static final int COLOR_ENERGY_END = 0xFF00FF00;

    public GuiAmmoPress(InventoryPlayer playerInv, TileAmmoPress tile) {
        super(new ContainerAmmoPress(playerInv, tile));
        this.container = (ContainerAmmoPress) this.inventorySlots;
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 256;
    }

    @Override
    public void initGui() {
        super.initGui();
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;

        // Search Field
        searchField = new GuiTextField(100, fontRenderer, i + 90, j + 90, 78, 12);
        searchField.setTextColor(COLOR_TEXT_HEADER);
        searchField.setDisabledTextColour(0xFFAAAAAA);
        searchField.setEnableBackgroundDrawing(true);
        searchField.setMaxStringLength(32);
        updateFilteredRecipes();

        // Tabs
        btnTabRecipes = new GuiButton(0, i + 5, j + 18, 54, 14, "Recipes");
        btnTabCraft = new GuiButton(1, i + 61, j + 18, 54, 14, "Craft");
        btnTabAuto = new GuiButton(2, i + 117, j + 18, 54, 14, "Auto");

        buttonList.add(btnTabRecipes);
        buttonList.add(btnTabCraft);
        buttonList.add(btnTabAuto);

        // Recipe Tab
        btnAddRecipe = new GuiButton(10, i + 115, j + 50, 55, 16, "Add");
        btnDeleteRecipe = new GuiButton(11, i + 115, j + 68, 55, 16, "Remove");
        btnClearGrid = new GuiButton(12, i + 115, j + 86, 55, 16, "Clear");

        buttonList.add(btnAddRecipe);
        buttonList.add(btnDeleteRecipe);
        buttonList.add(btnClearGrid);

        // Craft Tab
        btnCraftOne = new GuiButton(20, i + 8, j + 142, 30, 14, "x1");
        btnCraftStack = new GuiButton(21, i + 40, j + 142, 30, 14, "x64");

        buttonList.add(btnCraftOne);
        buttonList.add(btnCraftStack);

        // Auto Tab
        btnToggleAuto = new GuiButton(30, i + 8, j + 142, 40, 14, "Auto");
        btnAddToAuto = new GuiButton(31, i + 50, j + 142, 18, 14, "+");
        btnRemoveFromAuto = new GuiButton(32, i + 70, j + 142, 18, 14, "-");
        btnToggleMode = new GuiButton(33, i + 90, j + 142, 78, 14, "Mode");

        buttonList.add(btnToggleAuto);
        buttonList.add(btnAddToAuto);
        buttonList.add(btnRemoveFromAuto);
        buttonList.add(btnToggleMode);

        updateTabVisibility();
        container.setVisibleTab(selectedTab);
        updateSearchFieldPosition();
    }

    private void updateSearchFieldPosition() {
        int i = guiLeft;
        int j = guiTop;
        if (selectedTab == 0) {
            // Recipes Tab: Split row (Energy Bar Left, Search Right)
            searchField.x = i + 90;
            searchField.y = j + 110;
            searchField.width = 78;
        } else {
            // Craft/Auto Tabs: Search lower
            searchField.x = i + 90;
            searchField.y = j + 100; // Moved down from 96
            searchField.width = 78;
        }
    }

    private void updateFilteredRecipes() {
        String query = searchField.getText().toLowerCase();
        List<CraftingRecipe> all = tile.getRecipes();

        if (query.isEmpty()) {
            filteredRecipes = new ArrayList<>(all);
        } else {
            filteredRecipes = all.stream()
                .filter(r -> r.getDisplayName().toLowerCase().contains(query))
                .collect(Collectors.toList());
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
        searchField.drawTextBox();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchField.textboxKeyTyped(typedChar, keyCode)) {
            updateFilteredRecipes();
            scrollOffset = 0;
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        searchField.mouseClicked(mouseX, mouseY, mouseButton);
        handleRecipeListClick(mouseX, mouseY);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void updateTabVisibility() {
        updateSearchFieldPosition();

        btnTabRecipes.enabled = (selectedTab != 0);
        btnTabCraft.enabled = (selectedTab != 1);
        btnTabAuto.enabled = (selectedTab != 2);

        // Recipes Tab
        boolean isRecipesTab = (selectedTab == 0);
        btnAddRecipe.visible = isRecipesTab;
        btnDeleteRecipe.visible = isRecipesTab;
        btnClearGrid.visible = isRecipesTab;
        btnDeleteRecipe.enabled = isRecipesTab && selectedRecipeIndex >= 0;

        // Craft Tab
        boolean isCraftTab = (selectedTab == 1);
        btnCraftOne.visible = isCraftTab;
        btnCraftStack.visible = isCraftTab;
        btnCraftOne.enabled = isCraftTab && selectedRecipeIndex >= 0;
        btnCraftStack.enabled = isCraftTab && selectedRecipeIndex >= 0;

        // Auto Tab
        boolean isAutoTab = (selectedTab == 2);
        btnToggleAuto.visible = isAutoTab;
        btnAddToAuto.visible = isAutoTab;
        btnRemoveFromAuto.visible = isAutoTab;
        btnToggleMode.visible = isAutoTab;
        btnAddToAuto.enabled = isAutoTab && selectedRecipeIndex >= 0;
        btnRemoveFromAuto.enabled = isAutoTab && selectedRecipeIndex >= 0;

        if (tile != null) {
            btnToggleAuto.displayString = tile.isAutomationEnabled() ? "ON" : "OFF";
            btnToggleMode.displayString = tile.isRoundRobinMode() ? "Mode: Cycle" : "Mode: Prio";
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0: // Recipes
                selectedTab = 0;
                scrollOffset = 0;
                container.setVisibleTab(0);
                updateFilteredRecipes();
                updateTabVisibility();
                break;
            case 1: // Craft
                selectedTab = 1;
                scrollOffset = 0;
                container.setVisibleTab(1);
                updateFilteredRecipes();
                updateTabVisibility();
                break;
            case 2: // Auto
                selectedTab = 2;
                scrollOffset = 0;
                container.setVisibleTab(2);
                updateFilteredRecipes();
                updateTabVisibility();
                break;

            case 10: // Add Recipe
                PacketHandler.INSTANCE.sendToServer(PacketAmmoPressAction.addRecipe(tile.getPos()));
                break;
            case 11: // Delete Recipe
                if (selectedRecipeIndex >= 0 && selectedRecipeIndex < filteredRecipes.size()) {
                    CraftingRecipe recipe = filteredRecipes.get(selectedRecipeIndex);
                    PacketHandler.INSTANCE.sendToServer(PacketAmmoPressAction.removeRecipe(tile.getPos(), recipe.getId()));
                    selectedRecipeIndex = -1;
                    updateTabVisibility();
                }
                break;
            case 12: // Clear Grid
                PacketHandler.INSTANCE.sendToServer(PacketAmmoPressAction.clearGrid(tile.getPos()));
                break;

            case 20: // Craft 1
                if (selectedRecipeIndex >= 0 && selectedRecipeIndex < filteredRecipes.size()) {
                    CraftingRecipe recipe = filteredRecipes.get(selectedRecipeIndex);
                    PacketHandler.INSTANCE.sendToServer(PacketAmmoPressAction.craftRecipe(tile.getPos(), recipe.getId(), 1));
                }
                break;
            case 21: // Craft 64
                if (selectedRecipeIndex >= 0 && selectedRecipeIndex < filteredRecipes.size()) {
                    CraftingRecipe recipe = filteredRecipes.get(selectedRecipeIndex);
                    PacketHandler.INSTANCE.sendToServer(PacketAmmoPressAction.craftRecipe(tile.getPos(), recipe.getId(), 64));
                }
                break;

            case 30: // Toggle Auto
                PacketHandler.INSTANCE.sendToServer(PacketAmmoPressAction.toggleAutomation(tile.getPos()));
                break;
            case 31: // Add to Auto
                if (selectedRecipeIndex >= 0 && selectedRecipeIndex < filteredRecipes.size()) {
                    CraftingRecipe recipe = filteredRecipes.get(selectedRecipeIndex);
                    PacketHandler.INSTANCE.sendToServer(PacketAmmoPressAction.addToAutomation(tile.getPos(), recipe.getId()));
                }
                break;
            case 32: // Remove from Auto
                if (selectedRecipeIndex >= 0 && selectedRecipeIndex < filteredRecipes.size()) {
                    CraftingRecipe recipe = filteredRecipes.get(selectedRecipeIndex);
                    PacketHandler.INSTANCE.sendToServer(PacketAmmoPressAction.removeFromAutomation(tile.getPos(), recipe.getId()));
                }
                break;
            case 33: // Toggle Mode
                PacketHandler.INSTANCE.sendToServer(PacketAmmoPressAction.toggleMode(tile.getPos()));
                break;
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawRect(0, 0, this.width, this.height, 0xBB000000); // Darker dimmed background

        int i = guiLeft;
        int j = guiTop;

        // Custom Main Background - Military Style
        drawRect(i - 2, j - 2, i + xSize + 2, j + ySize + 2, COLOR_BG_BORDER);
        drawRect(i, j, i + xSize, j + ySize, COLOR_BG_MAIN);

        // Header separator
        drawRect(i, j + 16, i + xSize, j + 17, COLOR_BG_BORDER);

        if (tile.getRecipes().size() != filteredRecipes.size() && searchField.getText().isEmpty()) {
             updateFilteredRecipes();
        }

        // Draw Energy Bar in Craft/Auto tabs
        if (selectedTab == 1 || selectedTab == 2) {
             drawEnergyBar(i + 8, j + 128); // Position under the info area
        } else if (selectedTab == 0) {
             drawEnergyBar(i + 8, j + 112); // Position next to search field
        }

        switch (selectedTab) {
            case 0:
                drawRecipesTabBackground(i, j);
                drawRecipesTab(mouseX, mouseY);
                break;
            case 1:
                drawCraftTabBackground(i, j);
                drawCraftTab(mouseX, mouseY);
                break;
            case 2:
                drawAutoTabBackground(i, j);
                drawAutoTab(mouseX, mouseY);
                break;
        }

        drawPlayerInventoryBackground(i, j);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawEnergyBar(int x, int y) {
        int width = 78; // Fits under Legend
        int height = 8;

        drawRect(x - 1, y - 1, x + width + 1, y + height + 1, COLOR_SLOT_BORDER);
        drawRect(x, y, x + width, y + height, COLOR_ENERGY_BG);

        int energy = tile.getEnergyStored();
        int max = tile.getMaxEnergyStored();
        if (max > 0) {
            int fill = (int)((float)energy / max * width);
            drawGradientRect(x, y, x + fill, y + height, COLOR_ENERGY_START, COLOR_ENERGY_END);
        }
    }

    private void drawSlotBox(int x, int y) {
        drawRect(x - 1, y - 1, x + 17, y + 17, COLOR_SLOT_BORDER);
        drawRect(x, y, x + 16, y + 16, COLOR_SLOT_BG);
    }

    private void drawRecipesTabBackground(int i, int j) {
        // 3x3 Grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlotBox(i + 8 + col * 18, j + 50 + row * 18);
            }
        }
        // Result
        drawSlotBox(i + 90, j + 68);
    }

    private void drawCraftTabBackground(int i, int j) {
        // Input Slots (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBox(i + 8 + col * 18, j + 42 + row * 18);
            }
        }
        // Output Slots (1x9)
        for (int col = 0; col < 9; col++) {
            drawSlotBox(i + 8 + col * 18, j + 158);
        }
    }

    private void drawAutoTabBackground(int i, int j) {
        drawCraftTabBackground(i, j); // Same layout for slots
    }

    private void drawRecipesTab(int mouseX, int mouseY) {
        int i = guiLeft;
        int j = guiTop;

        fontRenderer.drawString("Ingredients:", i + 8, j + 38, COLOR_TEXT);
        fontRenderer.drawString("->", i + 74, j + 72, COLOR_TEXT);
        fontRenderer.drawString("Result:", i + 90, j + 55, COLOR_TEXT); // Moved right to 90

        drawRecipeList(i + 8, j + 124, mouseX, mouseY, 3);
    }

    private void drawCraftTab(int mouseX, int mouseY) {
        int i = guiLeft;
        int j = guiTop;

        fontRenderer.drawString("Materials:", i + 8, j + 32, COLOR_TEXT);

        // Info Box / Legend
        drawRect(i + 7, j + 98, i + 87, j + 126, 0xFF303030); // Info background
        drawRect(i + 7, j + 98, i + 87, j + 99, COLOR_BG_BORDER);
        drawRect(i + 7, j + 126, i + 87, j + 127, COLOR_BG_BORDER);

        if (selectedRecipeIndex >= 0 && selectedRecipeIndex < filteredRecipes.size()) {
            CraftingRecipe recipe = filteredRecipes.get(selectedRecipeIndex);
            fontRenderer.drawString("Requirements:", i + 9, j + 100, COLOR_TEXT_HEADER);

            List<ItemStack> ingredients = recipe.getIngredients();
            RenderHelper.enableGUIStandardItemLighting();
            int ingY = j + 111;
            for (int idx = 0; idx < Math.min(2, ingredients.size()); idx++) {
                ItemStack ing = ingredients.get(idx);
                int canMake = countAvailableWithSubCrafts(ing);

                itemRender.renderItemAndEffectIntoGUI(ing, i + 9, ingY);
                String countStr = canMake + "/" + ing.getCount();
                int countColor = canMake >= ing.getCount() ? 0x55FF55 : 0xFF5555;
                fontRenderer.drawString(countStr, i + 27, ingY + 4, countColor);
                ingY += 16;
            }
            if (ingredients.size() > 2) {
                fontRenderer.drawString("+" + (ingredients.size() - 2) + " more", i + 9, ingY - 4, 0xAAAAAA);
            }
            RenderHelper.disableStandardItemLighting();
        } else {
            fontRenderer.drawString("Select a", i + 25, j + 106, 0x888888);
            fontRenderer.drawString("recipe", i + 28, j + 116, 0x888888);
        }

        drawRecipeListSmall(i + 90, j + 114, mouseX, mouseY, 2); // Moved down to 114

        fontRenderer.drawString("Output:", i + 8, j + 146, COLOR_TEXT);
    }

    private void drawAutoTab(int mouseX, int mouseY) {
        int i = guiLeft;
        int j = guiTop;

        String status = tile.isAutomationEnabled() ? "ACTIVE" : "IDLE";
        int statusColor = tile.isAutomationEnabled() ? 0x00FF00 : 0xFF0000;
        fontRenderer.drawString("Status: " + status, i + 8, j + 32, statusColor);

        fontRenderer.drawString("In Auto:", i + 8, j + 98, COLOR_TEXT);

        int y = j + 108;
        List<CraftingRecipe> autoRecipes = new ArrayList<>();
        for (CraftingRecipe recipe : tile.getRecipes()) {
            if (tile.isInAutomation(recipe.getId())) {
                autoRecipes.add(recipe);
            }
        }

        if (autoRecipes.isEmpty()) {
            fontRenderer.drawString("(None)", i + 10, y, 0x666666);
        } else {
            RenderHelper.enableGUIStandardItemLighting();
            for (int idx = 0; idx < Math.min(2, autoRecipes.size()); idx++) {
                CraftingRecipe recipe = autoRecipes.get(idx);
                int entryY = y + idx * 10;
                itemRender.renderItemAndEffectIntoGUI(recipe.getResult(), i + 9, entryY - 4);
                String name = fontRenderer.trimStringToWidth(recipe.getDisplayName(), 65);
                fontRenderer.drawString(name, i + 27, entryY, COLOR_TEXT);
            }
            RenderHelper.disableStandardItemLighting();
        }

        drawRecipeListSmall(i + 90, j + 114, mouseX, mouseY, 2); // Moved down to 114
    }

    private int countItem(List<ItemStack> list, ItemStack target) {
        int count = 0;
        for (ItemStack stack : list) {
            if (ItemStack.areItemsEqual(target, stack) && ItemStack.areItemStackTagsEqual(target, stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int countAvailableWithSubCrafts(ItemStack target) {
        return countAvailableWithSubCrafts(target, new java.util.HashSet<>());
    }

    private int countAvailableWithSubCrafts(ItemStack target, java.util.Set<Integer> visited) {
        List<ItemStack> available = tile.getAvailableItems();
        int directCount = countItem(available, target);

        CraftingRecipe subRecipe = tile.findRecipeForResult(target);
        if (subRecipe != null && !visited.contains(subRecipe.getId())) {
            visited.add(subRecipe.getId());

            int canCraftTimes = Integer.MAX_VALUE;
            for (ItemStack ing : subRecipe.getIngredients()) {
                int haveIng = countAvailableWithSubCrafts(ing, visited);
                int timesFromThisIng = haveIng / ing.getCount();
                canCraftTimes = Math.min(canCraftTimes, timesFromThisIng);
            }

            if (canCraftTimes == Integer.MAX_VALUE) {
                canCraftTimes = 0;
            }
            int craftableCount = canCraftTimes * subRecipe.getResult().getCount();
            directCount += craftableCount;
        }
        return directCount;
    }

    private void drawRecipeList(int x, int y, int mouseX, int mouseY, int maxVisible) {
        if (filteredRecipes.isEmpty()) {
            fontRenderer.drawString("(No recipes)", x + 5, y + 2, 0x666666);
            return;
        }

        RenderHelper.enableGUIStandardItemLighting();
        for (int idx = scrollOffset; idx < Math.min(scrollOffset + maxVisible, filteredRecipes.size()); idx++) {
            CraftingRecipe recipe = filteredRecipes.get(idx);
            int localIdx = idx - scrollOffset;
            int entryY = y + localIdx * 15;

            boolean selected = (idx == selectedRecipeIndex);
            boolean hovered = mouseX >= x && mouseX < x + 158 && mouseY >= entryY && mouseY < entryY + 14;
            int bgColor = selected ? 0xFF005500 : (hovered ? 0xFF333333 : 0xFF202020);

            drawRect(x, entryY, x + 158, entryY + 14, bgColor);
            // Border
            if (selected || hovered) {
                 drawRect(x, entryY, x+1, entryY+14, 0xFF00AA00);
                 drawRect(x+157, entryY, x+158, entryY+14, 0xFF00AA00);
            }

            itemRender.renderItemAndEffectIntoGUI(recipe.getResult(), x + 1, entryY - 1);

            String name = recipe.getDisplayName();
            if (fontRenderer.getStringWidth(name) > 110) {
                name = fontRenderer.trimStringToWidth(name, 105) + "..";
            }
            fontRenderer.drawString(name, x + 18, entryY + 3, selected ? 0xFFFFFF : COLOR_TEXT);

            if (tile.isInAutomation(recipe.getId())) {
                fontRenderer.drawString("[A]", x + 140, entryY + 3, 0x00FF00);
            }
        }
        RenderHelper.disableStandardItemLighting();

        if (filteredRecipes.size() > maxVisible) {
            int maxScroll = filteredRecipes.size() - maxVisible;
            fontRenderer.drawString((scrollOffset + 1) + "/" + (maxScroll + 1), x + 140, y - 10, 0x666666);
        }
    }

    private void drawRecipeListSmall(int x, int y, int mouseX, int mouseY, int maxVisible) {
        if (filteredRecipes.isEmpty()) {
            fontRenderer.drawString("(None)", x + 2, y + 2, 0x666666);
            return;
        }

        RenderHelper.enableGUIStandardItemLighting();
        for (int idx = scrollOffset; idx < Math.min(scrollOffset + maxVisible, filteredRecipes.size()); idx++) {
            CraftingRecipe recipe = filteredRecipes.get(idx);
            int localIdx = idx - scrollOffset;
            int entryY = y + localIdx * 15;

            boolean selected = (idx == selectedRecipeIndex);
            boolean hovered = mouseX >= x && mouseX < x + 78 && mouseY >= entryY && mouseY < entryY + 14;
            int bgColor = selected ? 0xFF005500 : (hovered ? 0xFF333333 : 0xFF202020);

            drawRect(x, entryY, x + 78, entryY + 14, bgColor);
            if (selected) {
                 drawRect(x, entryY, x + 2, entryY + 14, 0xFF00AA00);
            }

            itemRender.renderItemAndEffectIntoGUI(recipe.getResult(), x + 3, entryY - 1);

            String name = fontRenderer.trimStringToWidth(recipe.getDisplayName(), 50);
            fontRenderer.drawString(name, x + 20, entryY + 3, selected ? 0xFFFFFF : COLOR_TEXT);
        }
        RenderHelper.disableStandardItemLighting();
    }

    private void drawPlayerInventoryBackground(int i, int j) {
        // Separator
        drawRect(i + 4, j + 175, i + xSize - 4, j + 176, COLOR_BG_BORDER);
        fontRenderer.drawString("Inventory", i + 8, j + 167, COLOR_TEXT);

        // 3 rows
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                 drawSlotBox(i + 8 + col * 18, j + 179 + row * 18);
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
             drawSlotBox(i + 8 + col * 18, j + 237);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = "AMMO PRESS";
        // Military stencil font would be nice but standard font is all we have.
        // Draw centered
        int w = fontRenderer.getStringWidth(title);
        fontRenderer.drawString(title, (xSize - w) / 2, 6, COLOR_TEXT_HEADER);
    }

    private void handleRecipeListClick(int mouseX, int mouseY) {
        if (filteredRecipes.isEmpty()) return;

        int listX, listY, listWidth, maxVisible;

        if (selectedTab == 0) {
            listX = guiLeft + 8;
            listY = guiTop + 124;
            listWidth = 158;
            maxVisible = 3;
        } else {
            listX = guiLeft + 90;
            listY = guiTop + 114; // Moved down from 110
            listWidth = 78;
            maxVisible = 2;
        }

        for (int idx = scrollOffset; idx < Math.min(scrollOffset + maxVisible, filteredRecipes.size()); idx++) {
            int localIdx = idx - scrollOffset;
            int entryY = listY + localIdx * 15;

            if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= entryY && mouseY < entryY + 14) {
                selectedRecipeIndex = idx;
                updateTabVisibility();
                return;
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            int maxVisible = (selectedTab == 0) ? 3 : 2;
            int maxScroll = Math.max(0, filteredRecipes.size() - maxVisible);

            if (wheel > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else {
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        searchField.updateCursorCounter();
        updateTabVisibility();
    }
}

package com.antigravity.advancedsorter.autocrafter;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.network.PacketHandler;
import com.antigravity.advancedsorter.network.PacketAutoCrafterAction;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GUI dla Auto Craftera z trzema zakladkami:
 * 1. Receptury - definiowanie receptur (siatka 3x3 + slot wyniku)
 * 2. Craftowanie - magazyn materialow + wybor receptury do craftowania
 * 3. Automatyzacja - konfiguracja automatycznego craftowania
 */
public class GuiAutoCrafter extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AdvancedSorterMod.MODID,
            "textures/gui/auto_crafter.png");

    private final TileAutoCrafter tile;
    private final ContainerAutoCrafter container;

    // Zakladki: 0=Receptury, 1=Craftowanie, 2=Automatyzacja
    private int selectedTab = 0;

    // Wyszukiwarka
    private GuiTextField searchField;
    private List<CraftingRecipe> filteredRecipes = new ArrayList<>();

    // Przyciski zakladek
    private GuiButton btnTabRecipes;
    private GuiButton btnTabCraft;
    private GuiButton btnTabAuto;

    // Przyciski zakladki Receptury
    private GuiButton btnAddRecipe;
    private GuiButton btnDeleteRecipe;
    private GuiButton btnClearGrid;

    // Przyciski zakladki Craftowanie
    private GuiButton btnCraftOne;
    private GuiButton btnCraftStack;

    // Przyciski zakladki Automatyzacja
    private GuiButton btnToggleAuto;
    private GuiButton btnAddToAuto;
    private GuiButton btnRemoveFromAuto;
    private GuiButton btnToggleMode;

    // Scroll i selekcja
    private int scrollOffset = 0;
    private int selectedRecipeIndex = -1;

    public GuiAutoCrafter(ContainerAutoCrafter container, TileAutoCrafter tile) {
        super(container);
        this.container = container;
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 256;
    }

    @Override
    public void initGui() {
        super.initGui();
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;

        // Wyszukiwarka
        searchField = new GuiTextField(100, fontRenderer, i + 90, j + 90, 78, 12);
        searchField.setTextColor(-1);
        searchField.setDisabledTextColour(-1);
        searchField.setEnableBackgroundDrawing(true);
        searchField.setMaxStringLength(32);
        updateFilteredRecipes();

        // Zakladki - pod tytulem (y=18)
        btnTabRecipes = new GuiButton(0, i + 5, j + 18, 54, 14, "Receptury");
        btnTabCraft = new GuiButton(1, i + 61, j + 18, 54, 14, "Craft");
        btnTabAuto = new GuiButton(2, i + 117, j + 18, 54, 14, "Auto");

        buttonList.add(btnTabRecipes);
        buttonList.add(btnTabCraft);
        buttonList.add(btnTabAuto);

        // Zakladka Receptury - przyciski po prawej stronie
        btnAddRecipe = new GuiButton(10, i + 115, j + 50, 55, 16, "Dodaj");
        btnDeleteRecipe = new GuiButton(11, i + 115, j + 68, 55, 16, "Usun");
        btnClearGrid = new GuiButton(12, i + 115, j + 86, 55, 16, "Wyczysc");

        buttonList.add(btnAddRecipe);
        buttonList.add(btnDeleteRecipe);
        buttonList.add(btnClearGrid);

        // Zakladka Craftowanie - przyciski (przesunięte na y=142)
        btnCraftOne = new GuiButton(20, i + 8, j + 142, 30, 14, "x1");
        btnCraftStack = new GuiButton(21, i + 40, j + 142, 30, 14, "x64");

        buttonList.add(btnCraftOne);
        buttonList.add(btnCraftStack);

        // Zakladka Automatyzacja - przyciski (przesunięte na y=142)
        btnToggleAuto = new GuiButton(30, i + 8, j + 142, 40, 14, "Auto");
        btnAddToAuto = new GuiButton(31, i + 50, j + 142, 18, 14, "+");
        btnRemoveFromAuto = new GuiButton(32, i + 70, j + 142, 18, 14, "-");
        btnToggleMode = new GuiButton(33, i + 90, j + 142, 78, 14, "Tryb: Prio");

        buttonList.add(btnToggleAuto);
        buttonList.add(btnAddToAuto);
        buttonList.add(btnRemoveFromAuto);
        buttonList.add(btnToggleMode);

        updateTabVisibility();
        container.setVisibleTab(selectedTab);

        // Aktualizacja pozycji searchbara
        updateSearchFieldPosition();
    }

    private void updateSearchFieldPosition() {
        int i = guiLeft;
        int j = guiTop;
        if (selectedTab == 0) {
            // Zakladka 0: Wyszukiwarka na dole
            searchField.x = i + 8;
            searchField.y = j + 110;
            searchField.width = 158;
        } else {
            // Zakladka 1 i 2: Wyszukiwarka po prawej
            searchField.x = i + 90;
            searchField.y = j + 92;
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

        // Aktywnosc zakladek
        btnTabRecipes.enabled = (selectedTab != 0);
        btnTabCraft.enabled = (selectedTab != 1);
        btnTabAuto.enabled = (selectedTab != 2);

        // Zakladka Receptury
        boolean isRecipesTab = (selectedTab == 0);
        btnAddRecipe.visible = isRecipesTab;
        btnDeleteRecipe.visible = isRecipesTab;
        btnClearGrid.visible = isRecipesTab;
        btnDeleteRecipe.enabled = isRecipesTab && selectedRecipeIndex >= 0;

        // Zakladka Craftowanie
        boolean isCraftTab = (selectedTab == 1);
        btnCraftOne.visible = isCraftTab;
        btnCraftStack.visible = isCraftTab;
        btnCraftOne.enabled = isCraftTab && selectedRecipeIndex >= 0;
        btnCraftStack.enabled = isCraftTab && selectedRecipeIndex >= 0;

        // Zakladka Automatyzacja
        boolean isAutoTab = (selectedTab == 2);
        btnToggleAuto.visible = isAutoTab;
        btnAddToAuto.visible = isAutoTab;
        btnRemoveFromAuto.visible = isAutoTab;
        btnToggleMode.visible = isAutoTab;
        btnAddToAuto.enabled = isAutoTab && selectedRecipeIndex >= 0;
        btnRemoveFromAuto.enabled = isAutoTab && selectedRecipeIndex >= 0;

        if (tile != null) {
            btnToggleAuto.displayString = tile.isAutomationEnabled() ? "WL" : "WYL";
            btnToggleMode.displayString = tile.isRoundRobinMode() ? "Tryb: Cykl" : "Tryb: Prio";
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0: // Receptury
                selectedTab = 0;
                scrollOffset = 0;
                container.setVisibleTab(0);
                updateFilteredRecipes();
                updateTabVisibility();
                break;
            case 1: // Craftowanie
                selectedTab = 1;
                scrollOffset = 0;
                container.setVisibleTab(1);
                updateFilteredRecipes();
                updateTabVisibility();
                break;
            case 2: // Automatyzacja
                selectedTab = 2;
                scrollOffset = 0;
                container.setVisibleTab(2);
                updateFilteredRecipes();
                updateTabVisibility();
                break;

            case 10: // Dodaj recepture
                PacketHandler.INSTANCE.sendToServer(
                    PacketAutoCrafterAction.addRecipe(tile.getPos()));
                // Po dodaniu receptury odswiez liste
                tile.markDirty(); // Lokalne odswiezenie moze nie wystarczyc jesli pakiet idzie na serwer
                // Ale GUI i tak powinno sie odswiezyc
                break;

            case 11: // Usun recepture
                if (selectedRecipeIndex >= 0 && selectedRecipeIndex < filteredRecipes.size()) {
                    CraftingRecipe recipe = filteredRecipes.get(selectedRecipeIndex);
                    PacketHandler.INSTANCE.sendToServer(
                        PacketAutoCrafterAction.removeRecipe(tile.getPos(), recipe.getId()));
                    selectedRecipeIndex = -1;
                    updateTabVisibility();
                }
                break;

            case 12: // Wyczysc siatke
                PacketHandler.INSTANCE.sendToServer(
                    PacketAutoCrafterAction.clearGrid(tile.getPos()));
                break;

            case 20: // Craft x1
                if (selectedRecipeIndex >= 0 && selectedRecipeIndex < filteredRecipes.size()) {
                    CraftingRecipe recipe = filteredRecipes.get(selectedRecipeIndex);
                    PacketHandler.INSTANCE.sendToServer(
                        PacketAutoCrafterAction.craftRecipe(tile.getPos(), recipe.getId(), 1));
                }
                break;

            case 21: // Craft x64
                if (selectedRecipeIndex >= 0 && selectedRecipeIndex < filteredRecipes.size()) {
                    CraftingRecipe recipe = filteredRecipes.get(selectedRecipeIndex);
                    PacketHandler.INSTANCE.sendToServer(
                        PacketAutoCrafterAction.craftRecipe(tile.getPos(), recipe.getId(), 64));
                }
                break;

            case 30: // Toggle auto
                PacketHandler.INSTANCE.sendToServer(
                    PacketAutoCrafterAction.toggleAutomation(tile.getPos()));
                break;

            case 31: // Dodaj do auto
                if (selectedRecipeIndex >= 0 && selectedRecipeIndex < filteredRecipes.size()) {
                    CraftingRecipe recipe = filteredRecipes.get(selectedRecipeIndex);
                    PacketHandler.INSTANCE.sendToServer(
                        PacketAutoCrafterAction.addToAutomation(tile.getPos(), recipe.getId()));
                }
                break;

            case 32: // Usun z auto
                if (selectedRecipeIndex >= 0 && selectedRecipeIndex < filteredRecipes.size()) {
                    CraftingRecipe recipe = filteredRecipes.get(selectedRecipeIndex);
                    PacketHandler.INSTANCE.sendToServer(
                        PacketAutoCrafterAction.removeFromAutomation(tile.getPos(), recipe.getId()));
                }
                break;

            case 33: // Toggle mode
                PacketHandler.INSTANCE.sendToServer(
                    PacketAutoCrafterAction.toggleMode(tile.getPos()));
                break;
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawRect(0, 0, this.width, this.height, 0x88000000);

        int i = guiLeft;
        int j = guiTop;

        drawRect(i - 2, j - 2, i + xSize + 2, j + ySize + 2, 0xFF333333);
        drawRect(i, j, i + xSize, j + ySize, 0xFFC6C6C6);

        // Aktualizuj filtry co klatke (na wypadek zmian z serwera)
        // Optymalniej byloby to robic tylko przy zmianach, ale to proste GUI
        if (tile.getRecipes().size() != filteredRecipes.size() && searchField.getText().isEmpty()) {
             updateFilteredRecipes();
        }

        switch (selectedTab) {
            case 0:
                drawRecipesTab(mouseX, mouseY);
                break;
            case 1:
                drawCraftTab(mouseX, mouseY);
                break;
            case 2:
                drawAutoTab(mouseX, mouseY);
                break;
        }

        drawPlayerInventoryBackground(i, j);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawRecipesTab(int mouseX, int mouseY) {
        int i = guiLeft;
        int j = guiTop;

        fontRenderer.drawString("Skladniki:", i + 8, j + 38, 0x404040);

        drawRect(i + 7, j + 49, i + 63, j + 105, 0xFF555555);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawRect(i + 8 + col * 18, j + 50 + row * 18,
                         i + 8 + col * 18 + 16, j + 50 + row * 18 + 16, 0xFF8B8B8B);
            }
        }

        fontRenderer.drawString("->", i + 70, j + 70, 0x404040);
        fontRenderer.drawString("Wynik:", i + 70, j + 55, 0x404040);

        drawRect(i + 89, j + 67, i + 107, j + 85, 0xFF555555);
        drawRect(i + 90, j + 68, i + 106, j + 84, 0xFF8B8B8B);

        // Lista zapisanych receptur (teraz filtrowana)
        // Searchbar jest na y=110, wiec lista nizej (y=124)
        // fontRenderer.drawString("Zapisane:", i + 8, j + 110, 0x404040); // Searchbar tu jest
        drawRecipeList(i + 8, j + 124, mouseX, mouseY, 3);
    }

    private void drawCraftTab(int mouseX, int mouseY) {
        int i = guiLeft;
        int j = guiTop;

        // Etykieta Materialy przesunieta nizej (y=32)
        fontRenderer.drawString("Materialy:", i + 8, j + 32, 0x404040);

        // Tlo dla input inventory (3x9) - y=42
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = i + 8 + col * 18;
                int slotY = j + 42 + row * 18;
                drawRect(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF555555);
                drawRect(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B);
            }
        }

        // === LEWA STRONA: Legenda skladnikow ===
        // Pod inputem (y=96 do y=136)
        drawRect(i + 7, j + 98, i + 85, j + 138, 0xFF444444);

        if (selectedRecipeIndex >= 0 && selectedRecipeIndex < filteredRecipes.size()) {
            CraftingRecipe recipe = filteredRecipes.get(selectedRecipeIndex);
            fontRenderer.drawString("Potrzebne:", i + 9, j + 100, 0xFFFFFF);

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
                fontRenderer.drawString("+" + (ingredients.size() - 2) + " wiecej", i + 9, ingY, 0x888888);
            }
            RenderHelper.disableStandardItemLighting();

            // Wynik - obok legendy
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(recipe.getResult(), i + 65, j + 114);
            RenderHelper.disableStandardItemLighting();
        } else {
            fontRenderer.drawString("Wybierz", i + 20, j + 109, 0x888888);
            fontRenderer.drawString("recepture", i + 16, j + 119, 0x888888);
        }

        // === PRAWA STRONA: Lista receptur ===
        // Searchbar na y=92, lista od y=106
        drawRecipeListSmall(i + 90, j + 106, mouseX, mouseY, 2);

        // === Wyjscie (1x9) - y=158 ===
        fontRenderer.drawString("Wyjscie:", i + 90, j + 146, 0x404040);
        for (int col = 0; col < 9; col++) {
            int slotX = i + 8 + col * 18;
            int slotY = j + 158;
            drawRect(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF555555);
            drawRect(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B);
        }
    }

    private void drawAutoTab(int mouseX, int mouseY) {
        int i = guiLeft;
        int j = guiTop;

        String status = tile.isAutomationEnabled() ? "WLACZONA" : "WYLACZONA";
        int statusColor = tile.isAutomationEnabled() ? 0x00AA00 : 0xAA0000;
        fontRenderer.drawString("Automatyzacja: " + status, i + 8, j + 32, statusColor);

        // Tlo dla input inventory (3x9) - y=42
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = i + 8 + col * 18;
                int slotY = j + 42 + row * 18;
                drawRect(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF555555);
                drawRect(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B);
            }
        }

        // Lista receptur w automatyzacji (ta nie jest filtrowana przez searchbar, bo to status)
        fontRenderer.drawString("W auto:", i + 8, j + 98, 0x404040);

        int y = j + 110;
        List<CraftingRecipe> autoRecipes = new ArrayList<>();
        for (CraftingRecipe recipe : tile.getRecipes()) {
            if (tile.isInAutomation(recipe.getId())) {
                autoRecipes.add(recipe);
            }
        }

        if (autoRecipes.isEmpty()) {
            fontRenderer.drawString("(brak)", i + 10, y, 0x666666);
        } else {
            RenderHelper.enableGUIStandardItemLighting();
            for (int idx = 0; idx < Math.min(2, autoRecipes.size()); idx++) {
                CraftingRecipe recipe = autoRecipes.get(idx);
                int entryY = y + idx * 16;
                drawRect(i + 8, entryY, i + 85, entryY + 15, 0xFF3366AA);
                itemRender.renderItemAndEffectIntoGUI(recipe.getResult(), i + 9, entryY);
                String name = fontRenderer.trimStringToWidth(recipe.getDisplayName(), 55);
                fontRenderer.drawString(name, i + 27, entryY + 4, 0xFFFFFF);
            }
            RenderHelper.disableStandardItemLighting();
        }

        // Lista wszystkich receptur do wyboru (filtrowana)
        // Searchbar y=92, lista od y=106
        drawRecipeListSmall(i + 90, j + 106, mouseX, mouseY, 2);

        // === Wyjscie (1x9) - y=158 ===
        // fontRenderer.drawString("Wyjscie:", i + 90, j + 146, 0x404040); // Usuniete - kolizja z przyciskami
        for (int col = 0; col < 9; col++) {
            int slotX = i + 8 + col * 18;
            int slotY = j + 158;
            drawRect(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF555555);
            drawRect(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B);
        }
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
            fontRenderer.drawString("(brak receptur)", x + 5, y + 2, 0x666666);
            return;
        }

        RenderHelper.enableGUIStandardItemLighting();
        for (int idx = scrollOffset; idx < Math.min(scrollOffset + maxVisible, filteredRecipes.size()); idx++) {
            CraftingRecipe recipe = filteredRecipes.get(idx);
            int localIdx = idx - scrollOffset;
            int entryY = y + localIdx * 15;

            boolean selected = (idx == selectedRecipeIndex);
            boolean hovered = mouseX >= x && mouseX < x + 158 && mouseY >= entryY && mouseY < entryY + 14;
            int bgColor = selected ? 0xFF3366AA : (hovered ? 0xFF555588 : 0xFF555555);
            drawRect(x, entryY, x + 158, entryY + 14, bgColor);

            itemRender.renderItemAndEffectIntoGUI(recipe.getResult(), x + 1, entryY - 1);

            String name = recipe.getDisplayName();
            if (fontRenderer.getStringWidth(name) > 110) {
                name = fontRenderer.trimStringToWidth(name, 105) + "..";
            }
            fontRenderer.drawString(name, x + 18, entryY + 3, 0xFFFFFF);

            if (tile.isInAutomation(recipe.getId())) {
                fontRenderer.drawString("[A]", x + 140, entryY + 3, 0x55FF55);
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
            fontRenderer.drawString("(brak)", x + 2, y + 2, 0x666666);
            return;
        }

        RenderHelper.enableGUIStandardItemLighting();
        for (int idx = scrollOffset; idx < Math.min(scrollOffset + maxVisible, filteredRecipes.size()); idx++) {
            CraftingRecipe recipe = filteredRecipes.get(idx);
            int localIdx = idx - scrollOffset;
            int entryY = y + localIdx * 15;

            boolean selected = (idx == selectedRecipeIndex);
            boolean hovered = mouseX >= x && mouseX < x + 78 && mouseY >= entryY && mouseY < entryY + 14;
            int bgColor = selected ? 0xFF3366AA : (hovered ? 0xFF555588 : 0xFF555555);
            drawRect(x, entryY, x + 78, entryY + 14, bgColor);

            itemRender.renderItemAndEffectIntoGUI(recipe.getResult(), x + 1, entryY - 1);

            String name = fontRenderer.trimStringToWidth(recipe.getDisplayName(), 55);
            fontRenderer.drawString(name, x + 18, entryY + 3, 0xFFFFFF);
        }
        RenderHelper.disableStandardItemLighting();
    }

    private void drawPlayerInventoryBackground(int i, int j) {
        drawRect(i + 7, j + 179, i + 169, j + 235, 0xFF8B8B8B);
        drawRect(i + 7, j + 237, i + 169, j + 255, 0xFF8B8B8B);
        fontRenderer.drawString("Inwentarz", i + 8, j + 169, 0x404040);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = "Auto Crafter";
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 6, 0x404040);
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
            listY = guiTop + 106;
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

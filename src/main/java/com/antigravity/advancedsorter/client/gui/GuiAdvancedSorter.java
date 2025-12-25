package com.antigravity.advancedsorter.client.gui;

import com.antigravity.advancedsorter.container.ContainerAdvancedSorter;
import com.antigravity.advancedsorter.network.PacketHandler;
import com.antigravity.advancedsorter.network.PacketUpdateRules;
import com.antigravity.advancedsorter.tiles.TileAdvancedSorter;
import com.antigravity.advancedsorter.util.SortRule;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.NonNullList;

public class GuiAdvancedSorter extends GuiContainer
        implements IItemSelectorCallback, GuiItemBrowser.IItemDeselectionCallback {
    private final TileAdvancedSorter tile;
    private GuiTextField valueField;
    private int selectedRuleIndex = -1;
    private boolean editingExceptions = false;

    // Filter list dimensions (relative to GUI top-left)
    private static final int LIST_X = 8;
    private static final int LIST_Y = 10;
    private static final int LIST_WIDTH = 100;
    private static final int SLOT_HEIGHT = 28;
    private static final int MAX_VISIBLE_SLOTS = 3;
    private int scrollOffset = 0;

    // Animation timer for cycling preview icons (counts up each frame)
    private int animationTick = 0;
    private static final int ANIMATION_INTERVAL = 10; // 10 ticks = 0.5 seconds

    // Cache for matching items to prevent per-frame registry search (major
    // performance fix)
    private Map<Integer, List<ItemStack>> matchingItemsCache = new HashMap<>();
    private int lastRulesHashCode = 0;

    // Expanded items panel state
    private boolean showExpandedItemsPanel = false;
    private int expandedPanelScroll = 0;
    private static final int EXPANDED_PANEL_COLUMNS = 9;
    private static final int EXPANDED_PANEL_ROWS = 6;

    public GuiAdvancedSorter(ContainerAdvancedSorter inventorySlotsIn, TileAdvancedSorter tile) {
        super(inventorySlotsIn);
        this.tile = tile;
        this.xSize = 256;
        this.ySize = 220;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;

        // Buttons (Left Side - List Control)
        this.buttonList.add(new GuiButton(0, i + 8, j + 100, 45, 20, "New"));
        this.buttonList.add(new GuiButton(1, i + 58, j + 100, 50, 20, "Delete"));

        // Buttons (Right Side - Rule Editing) - moved up for Items visibility
        this.buttonList.add(new GuiButton(2, i + 120, j + 70, 50, 20, "Save"));
        this.buttonList.add(new GuiButton(3, i + 120, j + 20, 60, 20, "Type"));
        this.buttonList.add(new GuiButton(4, i + 190, j + 20, 60, 20, "Face"));
        this.buttonList.add(new GuiButton(5, i + 120, j + 45, 60, 20, "Mode"));
        this.buttonList.add(new GuiButton(6, i + 190, j + 45, 60, 20, "Search"));

        // Default Side button - for items that don't match any filter
        this.buttonList
                .add(new GuiButton(7, i + 8, j + 125, 100, 15, "Default: " + tile.defaultSide.getName().toUpperCase()));

        // Global Distribution Mode button
        String globalModeText = tile.distributionMode == SortRule.DistributionMode.ROUND_ROBIN ? "Mode: RR"
                : "Mode: 1st";
        this.buttonList.add(new GuiButton(8, i + 8, j + 145, 100, 15, globalModeText));

        valueField = new GuiTextField(8, this.fontRenderer, i + 120, j + -5, 120, 20);
        valueField.setMaxStringLength(32767);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        // Auto-save rules to server when closing GUI
        PacketHandler.INSTANCE.sendToServer(new PacketUpdateRules(tile.getPos(), tile.rules));
    }

    @Override
    public void onSelectionConfirmed(ItemStack stack) {
        if (selectedRuleIndex >= 0 && selectedRuleIndex < tile.rules.size()) {
            SortRule rule = tile.rules.get(selectedRuleIndex);

            // When editing exceptions for MOD_ID/NAME_CONTAINS, just add to exception list
            if (editingExceptions
                    && (rule.type == SortRule.MatchType.MOD_ID || rule.type == SortRule.MatchType.NAME_CONTAINS)) {
                // Add to exceptions without changing rule type or matchValue
                boolean exists = false;
                for (ItemStack s : rule.exceptionItems) {
                    if (s.isItemEqual(stack)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    rule.exceptionItems.add(stack.copy());
                }
                // DON'T call updateField() - it would clear matchValue!
                return;
            }

            // For Items mode - add to appropriate list
            List<ItemStack> targetList = editingExceptions ? rule.exceptionItems : rule.exactItems;
            boolean exists = false;
            for (ItemStack s : targetList) {
                if (s.isItemEqual(stack)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                targetList.add(stack.copy());
            }

            // Only set rule type to EXACT when:
            // 1. Not editing exceptions AND
            // 2. The rule type is not already MOD_ID or NAME_CONTAINS (preserve those
            // rules)
            if (!editingExceptions &&
                    rule.type != SortRule.MatchType.MOD_ID &&
                    rule.type != SortRule.MatchType.NAME_CONTAINS) {
                rule.type = SortRule.MatchType.EXACT;
            }
            // DON'T call updateField() - it clears the text field which is annoying
            // updateField();
        }
    }

    @Override
    public void onItemDeselected(ItemStack stack) {
        // Remove from exceptions when deselected in browser
        if (selectedRuleIndex >= 0 && selectedRuleIndex < tile.rules.size()) {
            SortRule rule = tile.rules.get(selectedRuleIndex);
            rule.exceptionItems.removeIf(s -> s.isItemEqual(stack));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) { // New
            tile.rules.add(new SortRule());
            selectedRuleIndex = tile.rules.size() - 1;
            // Auto-scroll to show new item
            if (selectedRuleIndex >= MAX_VISIBLE_SLOTS) {
                scrollOffset = selectedRuleIndex - MAX_VISIBLE_SLOTS + 1;
            }
            updateField();
        } else if (button.id == 1) { // Delete
            if (selectedRuleIndex >= 0 && selectedRuleIndex < tile.rules.size()) {
                tile.rules.remove(selectedRuleIndex);
                selectedRuleIndex = -1;
                valueField.setText("");
            }
        } else if (button.id == 2) { // Save
            PacketHandler.INSTANCE.sendToServer(new PacketUpdateRules(tile.getPos(), tile.rules));
        } else if (button.id == 3) { // Cycle Type
            if (selectedRuleIndex >= 0 && selectedRuleIndex < tile.rules.size()) {
                SortRule rule = tile.rules.get(selectedRuleIndex);
                int next = (rule.type.ordinal() + 1) % SortRule.MatchType.values().length;
                rule.type = SortRule.MatchType.values()[next];
            }
        } else if (button.id == 4) { // Cycle Face
            if (selectedRuleIndex >= 0 && selectedRuleIndex < tile.rules.size()) {
                SortRule rule = tile.rules.get(selectedRuleIndex);
                int next = (rule.outputFace.getIndex() + 1) % EnumFacing.VALUES.length;
                rule.outputFace = EnumFacing.getFront(next);
            }
        } else if (button.id == 5) { // Toggle Mode
            editingExceptions = !editingExceptions;
        } else if (button.id == 6) { // Search Item
            if (selectedRuleIndex >= 0) {
                this.mc.displayGuiScreen(new GuiItemSelector(this, this));
            }
        } else if (button.id == 7) { // Cycle Default Side
            int next = (tile.defaultSide.getIndex() + 1) % EnumFacing.VALUES.length;
            tile.defaultSide = EnumFacing.getFront(next);
            button.displayString = "Default: " + tile.defaultSide.getName().toUpperCase();
            // Send update to server
            PacketHandler.INSTANCE.sendToServer(new PacketUpdateRules(tile.getPos(), tile.rules, tile.defaultSide));
        } else if (button.id == 8) { // Toggle Global Distribution Mode
            if (tile.distributionMode == SortRule.DistributionMode.FIRST_MATCH) {
                tile.distributionMode = SortRule.DistributionMode.ROUND_ROBIN;
                button.displayString = "Mode: RR";
            } else {
                tile.distributionMode = SortRule.DistributionMode.FIRST_MATCH;
                button.displayString = "Mode: 1st";
            }
            PacketHandler.INSTANCE.sendToServer(new PacketUpdateRules(tile.getPos(), tile.rules, tile.defaultSide));
        }
    }

    private void updateField() {
        if (selectedRuleIndex >= 0 && selectedRuleIndex < tile.rules.size()) {
            valueField.setText(tile.rules.get(selectedRuleIndex).matchValue);
            valueField.setFocused(true);
        } else {
            valueField.setText("");
            valueField.setFocused(false);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // Expanded panel interactions
        if (showExpandedItemsPanel) {
            int guiLeft = (this.width - this.xSize) / 2;
            int guiTop = (this.height - this.ySize) / 2;

            // Panel dimensions
            int panelWidth = EXPANDED_PANEL_COLUMNS * 18 + 20;
            int panelHeight = EXPANDED_PANEL_ROWS * 18 + 50;
            int panelX = guiLeft - 75;
            int panelY = guiTop + 10;

            // Close button click
            int closeX = panelX + (panelWidth - 50) / 2;
            int closeY = panelY + panelHeight - 20;
            if (mouseX >= closeX && mouseX < closeX + 50 && mouseY >= closeY && mouseY < closeY + 15) {
                showExpandedItemsPanel = false;
                return;
            }

            // Search button click (opens item browser)
            int searchX = panelX + panelWidth - 60;
            int searchY = panelY + 5;
            if (mouseX >= searchX && mouseX < searchX + 55 && mouseY >= searchY && mouseY < searchY + 12) {
                SortRule rule = tile.rules.get(selectedRuleIndex);
                if (editingExceptions
                        && (rule.type == SortRule.MatchType.MOD_ID || rule.type == SortRule.MatchType.NAME_CONTAINS)) {
                    // Exceptions mode: show only matching items, with alreadySelected items
                    // highlighted
                    List<ItemStack> matchingItems = getMatchingItemsForRule(rule);
                    this.mc.displayGuiScreen(new GuiItemBrowser(this, this, matchingItems, rule.exceptionItems));
                } else {
                    // Normal mode: show all items
                    this.mc.displayGuiScreen(new GuiItemBrowser(this, this));
                }
                return;
            }

            // Check if click is inside panel area
            boolean clickInPanel = mouseX >= panelX && mouseX < panelX + panelWidth &&
                    mouseY >= panelY && mouseY < panelY + panelHeight;

            // Item slot click in expanded panel
            if (clickInPanel && selectedRuleIndex >= 0 && selectedRuleIndex < tile.rules.size()) {
                SortRule rule = tile.rules.get(selectedRuleIndex);
                List<ItemStack> targetList = editingExceptions ? rule.exceptionItems : rule.exactItems;

                int startIndex = expandedPanelScroll * EXPANDED_PANEL_COLUMNS;
                for (int row = 0; row < EXPANDED_PANEL_ROWS; row++) {
                    for (int col = 0; col < EXPANDED_PANEL_COLUMNS; col++) {
                        int index = startIndex + row * EXPANDED_PANEL_COLUMNS + col;
                        int slotX = panelX + 10 + col * 18;
                        int slotY = panelY + 20 + row * 18;

                        if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                            if (mouseButton == 1) {
                                if (index < targetList.size()) {
                                    targetList.remove(index);
                                }
                            } else {
                                ItemStack held = this.mc.player.inventory.getItemStack();
                                if (!held.isEmpty()) {
                                    targetList.add(held.copy());
                                }
                            }
                            return;
                        }
                    }
                }
                return;
            }

            // Click outside panel - allow inventory interaction
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
        valueField.mouseClicked(mouseX, mouseY, mouseButton);

        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;

        // Filter List Click Detection
        int listLeft = guiLeft + LIST_X;
        int listTop = guiTop + LIST_Y;

        if (mouseX >= listLeft && mouseX < listLeft + LIST_WIDTH &&
                mouseY >= listTop && mouseY < listTop + (MAX_VISIBLE_SLOTS * SLOT_HEIGHT)) {

            int clickedSlot = (mouseY - listTop) / SLOT_HEIGHT;
            int actualIndex = clickedSlot + scrollOffset;

            if (actualIndex >= 0 && actualIndex < tile.rules.size()) {
                selectedRuleIndex = actualIndex;
                updateField();
            }
        }

        // Ghost Slots Interaction
        if (selectedRuleIndex >= 0 && selectedRuleIndex < tile.rules.size()) {
            SortRule rule = tile.rules.get(selectedRuleIndex);
            List<ItemStack> targetList = editingExceptions ? rule.exceptionItems : rule.exactItems;

            for (int slot = 0; slot < 3; slot++) {
                int x = guiLeft + 175 + slot * 18;
                int y = guiTop + 85;
                if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                    if (mouseButton == 1) {
                        if (slot < targetList.size())
                            targetList.remove(slot);
                    } else {
                        ItemStack held = this.mc.player.inventory.getItemStack();
                        if (!held.isEmpty()) {
                            if (slot < targetList.size())
                                targetList.set(slot, held.copy());
                            else
                                targetList.add(held.copy());
                        }
                    }
                }
            }

            // "+" button click - toggle expanded panel
            if (targetList.size() >= 3) {
                int plusX = guiLeft + 175 + 3 * 18;
                int plusY = guiTop + 85;
                if (mouseX >= plusX && mouseX < plusX + 18 && mouseY >= plusY && mouseY < plusY + 18) {
                    showExpandedItemsPanel = !showExpandedItemsPanel;
                    expandedPanelScroll = 0;
                }
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getDWheel();
        if (wheel != 0) {
            if (showExpandedItemsPanel && selectedRuleIndex >= 0 && selectedRuleIndex < tile.rules.size()) {
                // Scroll expanded panel
                SortRule rule = tile.rules.get(selectedRuleIndex);
                List<ItemStack> targetList = editingExceptions ? rule.exceptionItems : rule.exactItems;
                int maxPanelScroll = Math.max(0,
                        (targetList.size() - 1) / EXPANDED_PANEL_COLUMNS - EXPANDED_PANEL_ROWS + 1);
                if (wheel > 0) {
                    expandedPanelScroll = Math.max(0, expandedPanelScroll - 1);
                } else {
                    expandedPanelScroll = Math.min(maxPanelScroll, expandedPanelScroll + 1);
                }
            } else {
                // Scroll filter list
                int maxScroll = Math.max(0, tile.rules.size() - MAX_VISIBLE_SLOTS);
                if (wheel > 0) {
                    scrollOffset = Math.max(0, scrollOffset - 1);
                } else {
                    scrollOffset = Math.min(maxScroll, scrollOffset + 1);
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (valueField.textboxKeyTyped(typedChar, keyCode)) {
            if (selectedRuleIndex >= 0 && selectedRuleIndex < tile.rules.size()) {
                tile.rules.get(selectedRuleIndex).matchValue = valueField.getText();
            }
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Hide/show buttons based on expanded panel state
        for (GuiButton button : this.buttonList) {
            button.visible = !showExpandedItemsPanel;
        }

        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;

        // Draw Inventory Backgrounds (always visible)
        for (int k = 0; k < 9; k++)
            drawRect(guiLeft + 119 + k * 18, guiTop + 114, guiLeft + 137 + k * 18, guiTop + 132, 0xFF8B8B8B);
        drawRect(guiLeft + 119, guiTop + 137, guiLeft + 281, guiTop + 213, 0xFF8B8B8B);

        // If expanded panel is open, only draw the panel (hide everything else)
        if (showExpandedItemsPanel && selectedRuleIndex >= 0 && selectedRuleIndex < tile.rules.size()) {
            SortRule rule = tile.rules.get(selectedRuleIndex);
            List<ItemStack> targetList = editingExceptions ? rule.exceptionItems : rule.exactItems;
            drawExpandedItemsPanel(targetList);
            return;
        }

        // Draw Left Panel Background (Filter List)
        drawRect(guiLeft + LIST_X - 2, guiTop + LIST_Y - 2,
                guiLeft + LIST_X + LIST_WIDTH + 2, guiTop + LIST_Y + (MAX_VISIBLE_SLOTS * SLOT_HEIGHT) + 2,
                0xFF444444);
        drawRect(guiLeft + LIST_X - 1, guiTop + LIST_Y - 1,
                guiLeft + LIST_X + LIST_WIDTH + 1, guiTop + LIST_Y + (MAX_VISIBLE_SLOTS * SLOT_HEIGHT) + 1,
                0xFF222222);

        // Draw Filter List Items
        for (int i = 0; i < MAX_VISIBLE_SLOTS; i++) {
            int ruleIndex = i + scrollOffset;
            if (ruleIndex >= tile.rules.size())
                break;

            SortRule rule = tile.rules.get(ruleIndex);
            int slotX = guiLeft + LIST_X;
            int slotY = guiTop + LIST_Y + (i * SLOT_HEIGHT);

            // Slot background (highlight if selected)
            int bgColor = (ruleIndex == selectedRuleIndex) ? 0xFF6666AA : 0xFF555555;
            drawRect(slotX, slotY, slotX + LIST_WIDTH, slotY + SLOT_HEIGHT - 2, bgColor);

            // Draw item icon with animation (cycles through matching items)
            ItemStack icon = ItemStack.EMPTY;
            String name = "Empty";
            List<ItemStack> matchingItems = getMatchingItemsForRule(rule);

            switch (rule.type) {
                case EXACT:
                    if (!rule.exactItems.isEmpty()) {
                        // Cycle through all exact items
                        int exactIndex = (animationTick / ANIMATION_INTERVAL) % rule.exactItems.size();
                        icon = rule.exactItems.get(exactIndex);
                        if (rule.exactItems.size() > 1) {
                            name = icon.getDisplayName() + " +" + (rule.exactItems.size() - 1);
                        } else {
                            name = icon.getDisplayName();
                        }
                    } else {
                        name = "Exact: (Empty)";
                    }
                    break;
                case MOD_ID:
                    // Filter out exception items from preview
                    List<ItemStack> modItemsFiltered = filterExceptionItems(matchingItems, rule.exceptionItems);
                    if (!modItemsFiltered.isEmpty()) {
                        int modIndex = (animationTick / ANIMATION_INTERVAL) % modItemsFiltered.size();
                        icon = modItemsFiltered.get(modIndex);
                    }
                    name = "Mod: " + rule.matchValue;
                    break;
                case ORE_DICT:
                    name = "Ore: " + rule.matchValue;
                    break;
                case NAME_CONTAINS:
                    // Filter out exception items from preview
                    List<ItemStack> nameItemsFiltered = filterExceptionItems(matchingItems, rule.exceptionItems);
                    if (!nameItemsFiltered.isEmpty()) {
                        int nameIndex = (animationTick / ANIMATION_INTERVAL) % nameItemsFiltered.size();
                        icon = nameItemsFiltered.get(nameIndex);
                    }
                    name = "Name: " + rule.matchValue;
                    break;
            }

            if (!icon.isEmpty()) {
                RenderHelper.enableGUIStandardItemLighting();
                this.itemRender.renderItemAndEffectIntoGUI(icon, slotX + 2, slotY + 5);
                RenderHelper.disableStandardItemLighting();
            }

            // Truncate name if too long
            String displayName = name;
            if (this.fontRenderer.getStringWidth(displayName) > LIST_WIDTH - 25) {
                while (this.fontRenderer.getStringWidth(displayName + "...") > LIST_WIDTH - 25
                        && displayName.length() > 0) {
                    displayName = displayName.substring(0, displayName.length() - 1);
                }
                displayName += "...";
            }

            this.fontRenderer.drawString(displayName, slotX + 22, slotY + 3, 0xFFFFFF);
            this.fontRenderer.drawString(rule.outputFace.getName().toUpperCase(), slotX + 22, slotY + 14, 0xAAAAAA);
        }

        // Draw Right Panel Background (extended up more to fit Type and Face on
        // separate lines)
        drawRect(guiLeft + 110, guiTop - 40, guiLeft + 250, guiTop + 110, 0xFF444444);
        drawRect(guiLeft + 111, guiTop - 39, guiLeft + 249, guiTop + 109, 0xFFC6C6C6);

        if (selectedRuleIndex >= 0 && selectedRuleIndex < tile.rules.size()) {
            SortRule rule = tile.rules.get(selectedRuleIndex);

            valueField.drawTextBox();

            // Type and Face on separate lines so long type names don't overlap
            this.fontRenderer.drawString("Type: " + rule.type.name(), guiLeft + 115, guiTop - 32, 0x404040);
            this.fontRenderer.drawString("Face: " + rule.outputFace.getName(), guiLeft + 115, guiTop - 22, 0x404040);

            List<ItemStack> targetList = editingExceptions ? rule.exceptionItems : rule.exactItems;
            this.fontRenderer.drawString(editingExceptions ? "Exceptions:" : "Items:", guiLeft + 175, guiTop + 75,
                    0x404040);

            // Draw 3 visible ghost slots
            for (int slot = 0; slot < 3; slot++) {
                int x = guiLeft + 175 + slot * 18;
                int y = guiTop + 85;
                drawRect(x, y, x + 18, y + 18, 0xFF888888);

                if (slot < targetList.size()) {
                    ItemStack stack = targetList.get(slot);
                    RenderHelper.enableGUIStandardItemLighting();
                    this.itemRender.renderItemAndEffectIntoGUI(stack, x + 1, y + 1);
                    this.itemRender.renderItemOverlays(this.fontRenderer, stack, x + 1, y + 1);
                    RenderHelper.disableStandardItemLighting();
                }
            }

            // Draw "+" button and "+X" counter (only when we have items)
            if (targetList.size() >= 3) {
                int plusX = guiLeft + 175 + 3 * 18;
                int plusY = guiTop + 85;
                int plusBgColor = 0xFF666699;

                // Draw button
                drawRect(plusX, plusY, plusX + 18, plusY + 18, plusBgColor);
                this.drawCenteredString(this.fontRenderer, "+", plusX + 9, plusY + 5, 0xFFFFFF);

                // Draw "+X" counter if more than 3 items
                if (targetList.size() > 3) {
                    String extraCount = "+" + (targetList.size() - 3);
                    this.fontRenderer.drawString(extraCount, plusX + 20, plusY + 5, 0xFFFFFF);
                }
            }
        } else {
            this.drawCenteredString(this.fontRenderer, "Select a Filter", guiLeft + 180, guiTop + 5, 0xFFFFFF);
        }

        // Draw Connection Status Panel (far right side, separate from buttons)
        int connX = guiLeft + 260;
        int connY = guiTop + 10;

        // Draw a small background panel for connections
        drawRect(connX - 3, connY - 3, connX + 52, connY + 62, 0xFF444444);
        drawRect(connX - 2, connY - 2, connX + 51, connY + 61, 0xFFC6C6C6);

        this.fontRenderer.drawString("Outputs", connX + 3, connY, 0x404040);
        connY += 12;

        // Check each face for inventory connection - vertical layout
        for (EnumFacing face : EnumFacing.VALUES) {
            boolean hasInventory = tile.hasInventoryOnSide(face);
            int color = hasInventory ? 0xFF00AA00 : 0xFF880000;
            String faceName = face.getName().substring(0, 1).toUpperCase();
            String status = hasInventory ? " OK" : " --";

            this.fontRenderer.drawString(faceName + ":" + status, connX + 3, connY, color);
            connY += 8;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        animationTick++;
        if (valueField != null) {
            valueField.updateCursorCounter();
        }
    }

    /**
     * Get a list of matching items for preview animation.
     * Uses cache to prevent per-frame registry search (major performance fix).
     * For MOD_ID: returns items from that mod
     * For NAME_CONTAINS: returns items whose name contains the search string
     */
    private List<ItemStack> getMatchingItemsForRule(SortRule rule) {
        // Check if we need to invalidate cache (rules changed)
        int currentRulesHash = tile.rules.hashCode();
        if (currentRulesHash != lastRulesHashCode) {
            matchingItemsCache.clear();
            lastRulesHashCode = currentRulesHash;
        }

        // Check cache first
        int ruleKey = System.identityHashCode(rule);
        if (matchingItemsCache.containsKey(ruleKey)) {
            return matchingItemsCache.get(ruleKey);
        }

        // Calculate matching items (expensive operation, only done once per rule)
        List<ItemStack> result = new ArrayList<>();

        if (rule.matchValue == null || rule.matchValue.isEmpty()) {
            matchingItemsCache.put(ruleKey, result);
            return result;
        }

        String searchValue = rule.matchValue.toLowerCase();
        int maxResults = 500; // Increased for exceptions browsing

        for (Item item : ForgeRegistries.ITEMS) {
            if (result.size() >= maxResults)
                break;

            // Get all subtypes of this item
            NonNullList<ItemStack> subItems = NonNullList.create();
            try {
                item.getSubItems(CreativeTabs.SEARCH, subItems);
            } catch (Exception e) {
                // Some items may not support this
                subItems.add(new ItemStack(item));
            }

            for (ItemStack stack : subItems) {
                if (result.size() >= maxResults)
                    break;

                boolean matches = false;

                switch (rule.type) {
                    case MOD_ID:
                        String modId = stack.getItem().getRegistryName().getResourceDomain();
                        matches = modId.equalsIgnoreCase(rule.matchValue);
                        break;
                    case NAME_CONTAINS:
                        String itemName = stack.getDisplayName().toLowerCase();
                        matches = itemName.contains(searchValue);
                        break;
                    default:
                        break;
                }

                if (matches) {
                    result.add(stack.copy());
                }
            }
        }

        // Store in cache
        matchingItemsCache.put(ruleKey, result);
        return result;
    }

    /**
     * Draw the expanded items panel on the LEFT side.
     * Player inventory remains on the RIGHT (standard position).
     */
    private void drawExpandedItemsPanel(List<ItemStack> items) {
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;

        // Panel dimensions - positioned on the LEFT side
        int panelWidth = EXPANDED_PANEL_COLUMNS * 18 + 20;
        int panelHeight = EXPANDED_PANEL_ROWS * 18 + 50;
        int panelX = guiLeft - 75; // Moved left by 55 more
        int panelY = guiTop + 10;

        // Panel background
        drawRect(panelX - 2, panelY - 2, panelX + panelWidth + 2, panelY + panelHeight + 2, 0xFF444444);
        drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFFC6C6C6);

        // Title
        this.drawCenteredString(this.fontRenderer, "All Items (" + items.size() + ")",
                panelX + panelWidth / 2, panelY + 5, 0x404040);

        // Search button (next to title)
        int searchX = panelX + panelWidth - 60;
        int searchY = panelY + 5;
        drawRect(searchX, searchY, searchX + 55, searchY + 12, 0xFF666699);
        this.fontRenderer.drawString("Search", searchX + 12, searchY + 2, 0xFFFFFF);

        // Draw items grid
        int startIndex = expandedPanelScroll * EXPANDED_PANEL_COLUMNS;
        for (int row = 0; row < EXPANDED_PANEL_ROWS; row++) {
            for (int col = 0; col < EXPANDED_PANEL_COLUMNS; col++) {
                int index = startIndex + row * EXPANDED_PANEL_COLUMNS + col;
                int slotX = panelX + 10 + col * 18;
                int slotY = panelY + 20 + row * 18;

                // Slot background
                drawRect(slotX, slotY, slotX + 18, slotY + 18, 0xFF888888);

                if (index < items.size()) {
                    ItemStack stack = items.get(index);
                    RenderHelper.enableGUIStandardItemLighting();
                    this.itemRender.renderItemAndEffectIntoGUI(stack, slotX + 1, slotY + 1);
                    RenderHelper.disableStandardItemLighting();
                }
            }
        }

        // Close button at bottom of panel
        int closeX = panelX + (panelWidth - 50) / 2;
        int closeY = panelY + panelHeight - 20;
        drawRect(closeX, closeY, closeX + 50, closeY + 15, 0xFF666699);
        this.drawCenteredString(this.fontRenderer, "Close", closeX + 25, closeY + 3, 0xFFFFFF);

        // Scroll info
        int maxScroll = Math.max(0, (items.size() - 1) / EXPANDED_PANEL_COLUMNS - EXPANDED_PANEL_ROWS + 1);
        if (maxScroll > 0) {
            String scrollText = "Scroll: " + (expandedPanelScroll + 1) + "/" + (maxScroll + 1);
            this.fontRenderer.drawString(scrollText, panelX + 10, closeY + 3, 0x404040);
        }
    }

    /**
     * Filter out items that are in the exception list from the source list.
     * Used to hide excepted items from animated preview.
     */
    private List<ItemStack> filterExceptionItems(List<ItemStack> source, List<ItemStack> exceptions) {
        if (exceptions == null || exceptions.isEmpty()) {
            return source;
        }
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack item : source) {
            boolean isException = false;
            for (ItemStack exception : exceptions) {
                if (item.isItemEqual(exception)) {
                    isException = true;
                    break;
                }
            }
            if (!isException) {
                result.add(item);
            }
        }
        return result;
    }
}

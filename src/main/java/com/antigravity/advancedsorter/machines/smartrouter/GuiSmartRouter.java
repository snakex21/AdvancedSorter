package com.antigravity.advancedsorter.machines.smartrouter;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.network.PacketHandler;
import com.antigravity.advancedsorter.network.PacketSmartRouterAction;
import com.antigravity.advancedsorter.network.PacketUpdateSmartRouterRules;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiSmartRouter extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AdvancedSorterMod.MODID,
            "textures/gui/smart_router.png");

    private final TileSmartRouter tile;
    private final ContainerSmartRouter container;
    private final EntityPlayer player;

    // UI State
    private boolean isRuleSelected = false;
    private int selectedIndex = -1;
    private int scrollOffset = 0;

    // Edit Fields
    private GuiTextField nameField;
    private GuiTextField freqField;
    private ItemStack filterStack = ItemStack.EMPTY;

    // Buttons
    private GuiButton btnAdd;
    private GuiButton btnDelete;
    private GuiButton btnBack;

    public GuiSmartRouter(EntityPlayer player, TileSmartRouter tile) {
        super(new ContainerSmartRouter(player, tile));
        this.container = (ContainerSmartRouter) this.inventorySlots;
        this.tile = tile;
        this.player = player;
        this.xSize = 176;
        this.ySize = 222;
    }

    @Override
    public void initGui() {
        super.initGui();
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;

        Keyboard.enableRepeatEvents(true);

        // Fields
        nameField = new GuiTextField(0, fontRenderer, i + 62, j + 20, 100, 12);
        nameField.setMaxStringLength(32);
        nameField.setFocused(true);

        freqField = new GuiTextField(1, fontRenderer, i + 62, j + 45, 50, 12);
        freqField.setMaxStringLength(5);
        freqField.setText("0");

        // Buttons
        btnAdd = new GuiButton(0, i + 120, j + 43, 42, 16, "Dodaj");
        btnDelete = new GuiButton(1, i + 120, j + 65, 42, 16, "Usun");
        btnBack = new GuiButton(2, i + 8, j + 115, 40, 16, "< Powrot");

        buttonList.add(btnAdd);
        buttonList.add(btnDelete);
        buttonList.add(btnBack);

        updateState();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    private void updateState() {
        selectedIndex = tile.getSelectedRuleIndex();
        isRuleSelected = (selectedIndex >= 0 && selectedIndex < tile.rules.size());

        nameField.setVisible(!isRuleSelected);
        freqField.setVisible(!isRuleSelected);
        btnAdd.visible = !isRuleSelected;

        // Delete button visible if selecting from list but not in manual mode?
        // Or maybe just separate delete mode?
        // For simplicity: Delete button is visible in "Add/List" mode if we have a selection in list (but not entered it)
        // Wait, "isRuleSelected" means we entered the manual mode.
        // Let's rely on click logic.

        btnDelete.visible = !isRuleSelected;
        btnBack.visible = isRuleSelected;

        container.setManualSlotsVisible(isRuleSelected);

        // Disable automation input slot if we are in manual mode?
        // Maybe not necessary, but good for clarity.
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) { // Add Rule
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = "Rule " + (tile.rules.size() + 1);

            int freq = 0;
            try {
                freq = Integer.parseInt(freqField.getText().trim());
            } catch (NumberFormatException e) {}

            SmartRouterRule newRule = new SmartRouterRule(name, filterStack, freq);

            // Send update to server
            List<SmartRouterRule> newRules = new ArrayList<>(tile.rules);
            newRules.add(newRule);
            PacketHandler.INSTANCE.sendToServer(new PacketUpdateSmartRouterRules(tile.getPos(), newRules));

            // Client side update for responsiveness
            tile.rules.add(newRule);

            // Clear inputs
            nameField.setText("");
            filterStack = ItemStack.EMPTY;
        }
        else if (button.id == 1) { // Delete Rule (logic handled in mouseClicked currently for selection)
            // Implementation: Select item in list then click delete?
            // Current "selectedIndex" reflects the ACTIVE rule on tile.
            // Let's add a separate local selection for list management.
        }
        else if (button.id == 2) { // Back
            PacketHandler.INSTANCE.sendToServer(PacketSmartRouterAction.selectRule(tile.getPos(), -1));
            tile.setSelectedRuleIndex(-1);
            updateState();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!isRuleSelected) {
            if (nameField.textboxKeyTyped(typedChar, keyCode)) return;
            if (freqField.textboxKeyTyped(typedChar, keyCode)) return;
        }

        if (keyCode == 1 || keyCode == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
            if (isRuleSelected) {
                // Escape from manual mode goes back to list
                actionPerformed(btnBack);
                return;
            }
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (!isRuleSelected) {
            nameField.mouseClicked(mouseX, mouseY, mouseButton);
            freqField.mouseClicked(mouseX, mouseY, mouseButton);

            // Handle Filter Slot Click (Ghost Slot)
            int i = guiLeft;
            int j = guiTop;
            if (isPointInRegion(62, 65, 16, 16, mouseX, mouseY)) {
                ItemStack held = player.inventory.getItemStack();
                filterStack = held.copy();
                if (!filterStack.isEmpty()) filterStack.setCount(1);
                // Don't take item from player, just copy (Ghost)
                return;
            }

            // Handle List Click
            int listX = i + 8;
            int listY = j + 20;
            for (int idx = scrollOffset; idx < Math.min(scrollOffset + 5, tile.rules.size()); idx++) {
                int y = listY + (idx - scrollOffset) * 18;
                if (mouseX >= listX && mouseX < listX + 50 && mouseY >= y && mouseY < y + 18) {
                    // Clicked on a rule -> Enter manual mode
                    PacketHandler.INSTANCE.sendToServer(PacketSmartRouterAction.selectRule(tile.getPos(), idx));
                    tile.setSelectedRuleIndex(idx);
                    updateState();
                    return;
                }

                // Delete button area (small X next to list item?)
                if (mouseX >= listX + 130 && mouseX < listX + 145 && mouseY >= y && mouseY < y + 18) {
                     List<SmartRouterRule> newRules = new ArrayList<>(tile.rules);
                     if (idx < newRules.size()) {
                        newRules.remove(idx);
                        PacketHandler.INSTANCE.sendToServer(new PacketUpdateSmartRouterRules(tile.getPos(), newRules));
                        tile.rules.remove(idx);
                     }
                     return;
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getDWheel();
        if (wheel != 0 && !isRuleSelected) {
             if (wheel > 0) scrollOffset = Math.max(0, scrollOffset - 1);
             else scrollOffset = Math.min(Math.max(0, tile.rules.size() - 5), scrollOffset + 1);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawRect(0, 0, width, height, 0x88000000);

        int i = guiLeft;
        int j = guiTop;

        // Background
        drawRect(i, j, i + xSize, j + ySize, 0xFFC6C6C6);
        drawRect(i + 4, j + 4, i + xSize - 4, j + ySize - 4, 0xFF333333); // Inner dark

        if (!isRuleSelected) {
            drawListMode(i, j, mouseX, mouseY);
        } else {
            drawManualMode(i, j);
        }
    }

    private void drawListMode(int i, int j, int mouseX, int mouseY) {
        // Form Area
        fontRenderer.drawString("Nowy Preset", i + 62, j + 10, 0xFFFFFF);
        nameField.drawTextBox();

        fontRenderer.drawString("Czest.", i + 62, j + 35, 0xAAAAAA);
        freqField.drawTextBox();

        // Filter Slot
        fontRenderer.drawString("Filtr", i + 62, j + 56, 0xAAAAAA); // Filter
        drawRect(i + 61, j + 64, i + 79, j + 82, 0xFF8B8B8B); // Border
        drawRect(i + 62, j + 65, i + 78, j + 81, 0xFF333333); // Slot

        if (!filterStack.isEmpty()) {
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(filterStack, i + 62, j + 65);
            RenderHelper.disableStandardItemLighting();
        }

        // List Area
        fontRenderer.drawString("Lista:", i + 8, j + 10, 0xFFFFFF);
        int listY = j + 20;

        for (int idx = scrollOffset; idx < Math.min(scrollOffset + 5, tile.rules.size()); idx++) {
            SmartRouterRule rule = tile.rules.get(idx);
            int y = listY + (idx - scrollOffset) * 18;

            boolean hovered = mouseX >= i + 8 && mouseX < i + 58 && mouseY >= y && mouseY < y + 18;
            int color = hovered ? 0xFF555588 : 0xFF444444;

            drawRect(i + 8, y, i + 58, y + 16, color);

            // Name
            String n = fontRenderer.trimStringToWidth(rule.name, 48);
            fontRenderer.drawString(n, i + 10, y + 4, 0xFFFFFF);

            // Delete X
            if (hovered) {
                // Draw delete indicator nearby?
                // Or just separate button implemented in mouseClicked
            }

            // Draw Delete Button (Text)
            drawRect(i + 130, y, i + 145, y + 16, 0xFFAA0000);
            fontRenderer.drawString("X", i + 135, y + 4, 0xFFFFFF);
        }
    }

    private void drawManualMode(int i, int j) {
        if (selectedIndex >= 0 && selectedIndex < tile.rules.size()) {
            SmartRouterRule rule = tile.rules.get(selectedIndex);
            fontRenderer.drawString("Preset: " + rule.name, i + 8, j + 10, 0x55FF55);
            fontRenderer.drawString("Czestotliwosc: " + rule.frequency, i + 8, j + 22, 0xAAAAAA);
            if (!rule.filterStack.isEmpty()) {
                 fontRenderer.drawString("Filtr: " + rule.filterStack.getDisplayName(), i + 8, j + 100, 0xAAAAAA);
            } else {
                 fontRenderer.drawString("Filtr: Brak (Wszystko)", i + 8, j + 100, 0xAAAAAA);
            }
        }

        fontRenderer.drawString("Wrzutnia (Szybki Teleport)", i + 60, j + 30, 0xFFAA00);

        // Grid background for manual inventory (3x3)
        // Coords match ContainerSmartRouter: startX=62, startY=40
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int x = i + 62 + col * 18;
                int y = j + 40 + row * 18;
                drawRect(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
                drawRect(x, y, x + 16, y + 16, 0xFF555555);
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
         // Tooltips logic here if needed
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (!isRuleSelected) {
            nameField.updateCursorCounter();
            freqField.updateCursorCounter();
        }

        // Sync state if changed externally
        if (tile.getSelectedRuleIndex() != selectedIndex) {
            updateState();
        }
    }
}

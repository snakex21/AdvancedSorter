package com.antigravity.advancedsorter.pump;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.network.PacketUpdatePumpController;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiPumpController extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AdvancedSorterMod.MODID,
            "textures/gui/pump_controller.png");
    private final TilePumpController tile;
    private List<TilePumpController.PumpPreset> localPresets;

    private enum EditMode {
        NONE, ADDING, EDITING
    }

    private EditMode mode = EditMode.NONE;
    private int editingIndex = -1;
    private GuiTextField editNameField;
    private GuiTextField editFreqField;

    public GuiPumpController(ContainerPumpController container) {
        super(container);
        this.tile = container.getTile();
        this.xSize = 176;
        this.ySize = 220;
        this.localPresets = new ArrayList<>();
        for (TilePumpController.PumpPreset preset : tile.getPresets()) {
            localPresets.add(new TilePumpController.PumpPreset(preset.name, preset.frequency, preset.enabled));
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        // Coordinates are absolute screen coordinates
        editNameField = new GuiTextField(0, fontRenderer, guiLeft + 40, guiTop + 80, 100, 12);
        editFreqField = new GuiTextField(1, fontRenderer, guiLeft + 40, guiTop + 110, 30, 12);
        editFreqField.setMaxStringLength(2);

        refreshControls();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        savePresets();
    }

    private void refreshControls() {
        buttonList.clear();

        if (mode == EditMode.NONE) {
            int startY = guiTop + 30;
            for (int i = 0; i < localPresets.size(); i++) {
                TilePumpController.PumpPreset preset = localPresets.get(i);

                // Toggle button
                buttonList.add(new GuiButton(i * 4, guiLeft + 120, startY + i * 25 - 2, 25, 16,
                        preset.enabled ? TextFormatting.GREEN + "ON" : TextFormatting.RED + "OFF"));
                // Edit button
                buttonList.add(new GuiButton(i * 4 + 1, guiLeft + 147, startY + i * 25 - 2, 20, 16, "Edit"));
                // Delete button
                buttonList.add(new GuiButton(i * 4 + 2, guiLeft + 10, startY + i * 25 - 2, 12, 16, "X"));
            }

            if (localPresets.size() < 6) {
                buttonList.add(new GuiButton(100, guiLeft + 10, guiTop + 180, 156, 20, "Add Preset"));
            }
        } else {
            buttonList.add(new GuiButton(200, guiLeft + 40, guiTop + 130, 45, 20, "Save"));
            buttonList.add(new GuiButton(201, guiLeft + 95, guiTop + 130, 45, 20, "Cancel"));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (mode == EditMode.NONE) {
            if (button.id == 100) {
                mode = EditMode.ADDING;
                editNameField.setText("New Preset");
                editFreqField.setText("0");
                refreshControls();
            } else if (button.id < 100) {
                int index = button.id / 4;
                int type = button.id % 4;

                if (type == 0) { // Toggle
                    localPresets.get(index).enabled = !localPresets.get(index).enabled;
                    refreshControls();
                } else if (type == 1) { // Edit
                    mode = EditMode.EDITING;
                    editingIndex = index;
                    editNameField.setText(localPresets.get(index).name);
                    editFreqField.setText(String.valueOf(localPresets.get(index).frequency));
                    refreshControls();
                } else if (type == 2) { // Delete
                    localPresets.remove(index);
                    refreshControls();
                }
            }
        } else {
            if (button.id == 200) { // Save
                String name = editNameField.getText();
                int freq = 0;
                try {
                    freq = Integer.parseInt(editFreqField.getText());
                } catch (NumberFormatException e) {
                }
                freq = Math.max(0, Math.min(99, freq));

                if (mode == EditMode.ADDING) {
                    localPresets.add(new TilePumpController.PumpPreset(name, freq, false));
                } else if (mode == EditMode.EDITING) {
                    localPresets.get(editingIndex).name = name;
                    localPresets.get(editingIndex).frequency = freq;
                }
                mode = EditMode.NONE;
                refreshControls();
            } else if (button.id == 201) { // Cancel
                mode = EditMode.NONE;
                refreshControls();
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (mode != EditMode.NONE) {
            if (editNameField.textboxKeyTyped(typedChar, keyCode))
                return;
            if (editFreqField.textboxKeyTyped(typedChar, keyCode))
                return;
            if (keyCode == Keyboard.KEY_ESCAPE) {
                mode = EditMode.NONE;
                refreshControls();
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN) {
                actionPerformed(buttonList.stream().filter(b -> b.id == 200).findFirst().orElse(null));
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mode != EditMode.NONE) {
            editNameField.mouseClicked(mouseX, mouseY, mouseButton);
            editFreqField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    private void savePresets() {
        AdvancedSorterMod.network.sendToServer(new PacketUpdatePumpController(tile.getPos(), localPresets));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (mode != EditMode.NONE) {
            editNameField.drawTextBox();
            editFreqField.drawTextBox();
        }
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);

        if (mode != EditMode.NONE) {
            // Draw overlay
            drawRect(guiLeft + 20, guiTop + 40, guiLeft + 156, guiTop + 160, 0xEE000000);
            drawRect(guiLeft + 21, guiTop + 41, guiLeft + 155, guiTop + 159, 0xEE444444);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String s = "Pump Controller";
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 6, 4210752);

        if (mode == EditMode.NONE) {
            this.fontRenderer.drawString("Presets", 10, 20, 4210752);
            int startY = 30;
            for (int i = 0; i < localPresets.size(); i++) {
                TilePumpController.PumpPreset preset = localPresets.get(i);
                String text = preset.name + " (F:" + preset.frequency + ")";
                this.fontRenderer.drawString(text, 25, startY + i * 25 + 4, 4210752);
            }
        } else {
            String title = mode == EditMode.ADDING ? "Add Preset" : "Edit Preset";
            this.fontRenderer.drawString(title, this.xSize / 2 - this.fontRenderer.getStringWidth(title) / 2, 50,
                    0xFFFFFF);

            this.fontRenderer.drawString("Name:", 40, 70, 0xAAAAAA);
            this.fontRenderer.drawString("Freq (0-99):", 40, 100, 0xAAAAAA);
        }
    }
}

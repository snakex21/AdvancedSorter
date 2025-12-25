package com.antigravity.advancedsorter.pipes.fluid.extraction;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.network.PacketHandler;
import com.antigravity.advancedsorter.network.PacketUpdateExtractionPipe;
import com.antigravity.advancedsorter.util.PumpRegistry;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

/**
 * GUI for extraction fluid pipe - shows frequency setting, pump status and
 * manual mode toggle.
 */
@SideOnly(Side.CLIENT)
public class GuiExtractionFluidPipe extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(
            AdvancedSorterMod.MODID, "textures/gui/extraction_fluid_pipe.png");

    private final TileExtractionFluidPipe tile;
    private GuiTextField frequencyField;
    private GuiButton manualButton;

    public GuiExtractionFluidPipe(ContainerExtractionFluidPipe container) {
        super(container);
        this.tile = container.getTile();
        this.xSize = 176;
        this.ySize = 100;
    }

    @Override
    public void initGui() {
        super.initGui();

        // Frequency input field
        frequencyField = new GuiTextField(0, fontRenderer, guiLeft + 88, guiTop + 20, 30, 14);
        frequencyField.setMaxStringLength(2);
        frequencyField.setText(String.valueOf(tile.getFrequency()));

        // -/+ buttons for frequency
        addButton(new GuiButton(1, guiLeft + 68, guiTop + 18, 18, 18, "-"));
        addButton(new GuiButton(2, guiLeft + 120, guiTop + 18, 18, 18, "+"));

        // Manual mode toggle button
        manualButton = addButton(new GuiButton(3, guiLeft + 38, guiTop + 45, 100, 20,
                tile.isManualMode() ? "\u00a72MANUAL ON" : "\u00a7cMANUAL OFF"));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        manualButton.displayString = tile.isManualMode() ? "\u00a72MANUAL ON" : "\u00a7cMANUAL OFF";
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // Title
        String title = "Extraction Pipe";
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 6, 0xFFFFFF);

        // Frequency label
        fontRenderer.drawString("Freq:", 40, 23, 0xFFFFFF);

        // Status - show if extracting (manual or pump controller)
        boolean extracting = tile.isManualMode();
        if (!extracting && tile.getWorld() != null) {
            extracting = PumpRegistry.get(tile.getWorld()).isPumpingEnabled(tile.getFrequency());
        }
        String status = extracting ? "\u00a72EXTRACTING" : "\u00a7cIDLE";
        fontRenderer.drawString("Status: " + status, 40, 72, 0xFFFFFF);

        // Instructions
        fontRenderer.drawString("Manual = always extract", 28, 88, 0xAAAAAA);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        frequencyField.drawTextBox();
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
            int freq = Math.max(0, tile.getFrequency() - 1);
            frequencyField.setText(String.valueOf(freq));
            sendUpdate(freq, tile.isManualMode());
        } else if (button.id == 2) {
            int freq = Math.min(99, tile.getFrequency() + 1);
            frequencyField.setText(String.valueOf(freq));
            sendUpdate(freq, tile.isManualMode());
        } else if (button.id == 3) {
            // Toggle manual mode
            sendUpdate(tile.getFrequency(), !tile.isManualMode());
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (frequencyField.isFocused()) {
            if (keyCode == 28) {
                parseAndSendFrequency();
                return;
            }
            if (Character.isDigit(typedChar) || keyCode == 14) {
                frequencyField.textboxKeyTyped(typedChar, keyCode);
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        frequencyField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void onGuiClosed() {
        parseAndSendFrequency();
        super.onGuiClosed();
    }

    private void parseAndSendFrequency() {
        try {
            int freq = Integer.parseInt(frequencyField.getText());
            sendUpdate(freq, tile.isManualMode());
        } catch (NumberFormatException e) {
        }
    }

    private void sendUpdate(int frequency, boolean manualMode) {
        PacketHandler.INSTANCE.sendToServer(new PacketUpdateExtractionPipe(tile.getPos(), frequency, manualMode));
    }
}

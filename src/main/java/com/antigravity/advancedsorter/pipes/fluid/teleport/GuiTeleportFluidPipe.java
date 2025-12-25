package com.antigravity.advancedsorter.pipes.fluid.teleport;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.network.PacketRequestTeleportInfo;
import com.antigravity.advancedsorter.network.PacketUpdateTeleportFluidPipe;
import com.antigravity.advancedsorter.util.TeleportRegistry;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import org.lwjgl.input.Keyboard;

import net.minecraft.util.math.BlockPos;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI for Teleport Fluid Pipe.
 */
public class GuiTeleportFluidPipe extends GuiContainer {

    private final TileTeleportFluidPipe tile;
    private GuiTextField freqField;
    private List<TeleportRegistry.TeleportLocation> connectedPipes = new ArrayList<>();

    public GuiTeleportFluidPipe(InventoryPlayer playerInv, TileTeleportFluidPipe tile) {
        super(new ContainerTeleportFluidPipe(playerInv, tile));
        this.tile = tile;
        this.xSize = 350;
        this.ySize = 100;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        // Panel 1 (Left): Settings
        freqField = new GuiTextField(0, fontRenderer, guiLeft + 20, guiTop + 35, 76, 16);
        freqField.setMaxStringLength(5);
        freqField.setText(String.valueOf(tile.getFrequency()));
        freqField.setFocused(true);
        freqField.setEnableBackgroundDrawing(true);

        addButton(new GuiButton(1, guiLeft + 20, guiTop + 65, 76, 20, getModeDisplayName(tile.getMode())));

        // Request connection info from server
        AdvancedSorterMod.network.sendToServer(new PacketRequestTeleportInfo(tile.getPos()));
    }

    private String getModeDisplayName(TileTeleportFluidPipe.TeleportMode mode) {
        switch (mode) {
            case SEND:
                return "Send";
            case RECEIVE:
                return "Receive";
            case BOTH:
                return "Both";
            default:
                return mode.name();
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
            TileTeleportFluidPipe.TeleportMode nextMode = TileTeleportFluidPipe.TeleportMode
                    .values()[(tile.getMode().ordinal() + 1) % TileTeleportFluidPipe.TeleportMode.values().length];
            tile.setMode(nextMode);
            button.displayString = getModeDisplayName(nextMode);
            sendUpdate();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (freqField.textboxKeyTyped(typedChar, keyCode)) {
            try {
                String text = freqField.getText().trim();
                if (!text.isEmpty()) {
                    int freq = Integer.parseInt(text);
                    tile.setFrequency(freq);
                    sendUpdate();
                }
            } catch (NumberFormatException ignored) {
            }
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        freqField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        freqField.updateCursorCounter();
    }

    private void sendUpdate() {
        AdvancedSorterMod.network
                .sendToServer(new PacketUpdateTeleportFluidPipe(tile.getPos(), tile.getFrequency(), tile.getMode()));
    }

    public void updateConnectionInfo(List<TeleportRegistry.TeleportLocation> locations) {
        this.connectedPipes = locations;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        freqField.drawTextBox();
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        // Draw main background
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFFC6C6C6);

        // Draw panel borders
        int panelWidth = xSize / 3;

        // Panel 1 background
        drawRect(guiLeft + 5, guiTop + 5, guiLeft + panelWidth - 5, guiTop + ySize - 5, 0xFF333333);
        // Panel 2 background
        drawRect(guiLeft + panelWidth + 5, guiTop + 5, guiLeft + panelWidth * 2 - 5, guiTop + ySize - 5, 0xFF333333);
        // Panel 3 background
        drawRect(guiLeft + panelWidth * 2 + 5, guiTop + 5, guiLeft + xSize - 5, guiTop + ySize - 5, 0xFF333333);
    }

    private String getDimensionName(int dimId) {
        switch (dimId) {
            case 0:
                return "Earth";
            case -1:
                return "Nether";
            case 1:
                return "End";
            default:
                return "Dim " + dimId;
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int panelWidth = xSize / 3;

        // Panel 1: Settings
        String title1 = "Settings";
        fontRenderer.drawString(title1, panelWidth / 2 - fontRenderer.getStringWidth(title1) / 2, 10, 0xFFFFFF);
        fontRenderer.drawString("Frequency:", 20, 25, 0xAAAAAA);
        fontRenderer.drawString("Mode:", 20, 55, 0xAAAAAA);

        // Show tank level
        int fluidAmount = tile.getTank().getFluidAmount();
        int capacity = tile.getTank().getCapacity();
        fontRenderer.drawString(fluidAmount + "/" + capacity + " mB", 20, 85, 0x55AAFF);

        // Panel 2: Targets
        String title2 = "Targets (Sending)";
        fontRenderer.drawString(title2, panelWidth + panelWidth / 2 - fontRenderer.getStringWidth(title2) / 2, 10,
                0x55FF55);

        int yOff = 25;
        int count = 0;
        for (TeleportRegistry.TeleportLocation loc : connectedPipes) {
            if (loc.dimension == tile.getWorld().provider.getDimension() && loc.pos.equals(tile.getPos()))
                continue;
            if (tile.getMode().canSend() && loc.canReceive) {
                String s = getDimensionName(loc.dimension) + ": " + loc.pos.getX() + "," + loc.pos.getY() + ","
                        + loc.pos.getZ();
                fontRenderer.drawString(s, panelWidth + 10, yOff, 0xCCCCCC);
                yOff += 10;
                count++;
                if (count >= 6)
                    break;
            }
        }
        if (count == 0)
            fontRenderer.drawString("None found", panelWidth + 10, 25, 0x777777);

        // Panel 3: Sources
        String title3 = "Sources (Receiving)";
        fontRenderer.drawString(title3, panelWidth * 2 + panelWidth / 2 - fontRenderer.getStringWidth(title3) / 2, 10,
                0x5555FF);

        yOff = 25;
        count = 0;
        for (TeleportRegistry.TeleportLocation loc : connectedPipes) {
            if (loc.dimension == tile.getWorld().provider.getDimension() && loc.pos.equals(tile.getPos()))
                continue;
            if (tile.getMode().canReceive() && loc.canSend) {
                String s = getDimensionName(loc.dimension) + ": " + loc.pos.getX() + "," + loc.pos.getY() + ","
                        + loc.pos.getZ();
                fontRenderer.drawString(s, panelWidth * 2 + 10, yOff, 0xCCCCCC);
                yOff += 10;
                count++;
                if (count >= 6)
                    break;
            }
        }
        if (count == 0)
            fontRenderer.drawString("None found", panelWidth * 2 + 10, 25, 0x777777);
    }
}

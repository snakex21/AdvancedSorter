package com.antigravity.advancedsorter.pump;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.network.PacketUpdateAdvancedPump;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fluids.FluidStack;

import java.io.IOException;

public class GuiAdvancedPump extends GuiContainer {

    private final TileAdvancedPump tile;
    private GuiButton btnRateMinus;
    private GuiButton btnRatePlus;
    private GuiButton btnRedstone;

    public GuiAdvancedPump(InventoryPlayer playerInventory, TileAdvancedPump tile) {
        super(new ContainerAdvancedPump(playerInventory, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 100;
    }

    @Override
    public void initGui() {
        super.initGui();

        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;

        btnRateMinus = addButton(new GuiButton(0, x + 10, y + 50, 20, 20, "-"));
        btnRatePlus = addButton(new GuiButton(1, x + 90, y + 50, 20, 20, "+"));
        btnRedstone = addButton(new GuiButton(2, x + 120, y + 50, 50, 20, getRedstoneModeText()));
    }

    private String getRedstoneModeText() {
        switch (tile.getRedstoneMode()) {
            case 0:
                return I18n.format("gui.advanced_pump.redstone.ignore");
            case 1:
                return I18n.format("gui.advanced_pump.redstone.signal");
            case 2:
                return I18n.format("gui.advanced_pump.redstone.no_signal");
            default:
                return "?";
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == btnRateMinus) {
            int newRate = Math.max(0, tile.getPumpRateLimit() - 100);
            AdvancedSorterMod.network
                    .sendToServer(new PacketUpdateAdvancedPump(tile.getPos(), newRate, tile.getRedstoneMode()));
        } else if (button == btnRatePlus) {
            int newRate = Math.min(10000, tile.getPumpRateLimit() + 100);
            AdvancedSorterMod.network
                    .sendToServer(new PacketUpdateAdvancedPump(tile.getPos(), newRate, tile.getRedstoneMode()));
        } else if (button == btnRedstone) {
            int newMode = (tile.getRedstoneMode() + 1) % 3;
            AdvancedSorterMod.network
                    .sendToServer(new PacketUpdateAdvancedPump(tile.getPos(), tile.getPumpRateLimit(), newMode));
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;

        drawRect(x, y, x + xSize, y + ySize, 0xFFC6C6C6);
        drawRect(x + 2, y + 2, x + xSize - 2, y + ySize - 2, 0xFF8B8B8B);

        drawRect(x + 10, y + 10, x + 30, y + 40, 0xFF3A3A3A);
        int energyHeight = (int) (28.0f * tile.getEnergyStored() / tile.getMaxEnergyStored());
        drawRect(x + 12, y + 38 - energyHeight, x + 28, y + 38, 0xFFFF0000);

        drawRect(x + 40, y + 10, x + 60, y + 40, 0xFF3A3A3A);
        FluidStack fluid = tile.getTank().getFluid();
        if (fluid != null && fluid.amount > 0) {
            int fluidHeight = (int) (28.0f * fluid.amount / tile.getTank().getCapacity());
            int fluidColor = fluid.getFluid().getColor(fluid);
            drawRect(x + 42, y + 38 - fluidHeight, x + 58, y + 38, fluidColor | 0xFF000000);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.advanced_pump.title"), 8, -10, 0x404040);

        int energy = tile.getEnergyStored();
        String energyText = energy < 1000 ? energy + " RF" : String.format("%.1fk RF", energy / 1000.0f);
        fontRenderer.drawString(energyText, 10, 42, 0xFFFFFF);

        FluidStack fluid = tile.getTank().getFluid();
        String fluidText = fluid != null ? (fluid.amount / 1000) + "B" : "0B";
        fontRenderer.drawString(fluidText, 42, 42, 0xFFFFFF);

        String limitText = I18n.format("gui.advanced_pump.rate", tile.getPumpRateLimit());
        fontRenderer.drawString(limitText, 35, 65, 0x404040);

        String currentRateText = "Aktualna: " + tile.getLastSecondRate() + " mB/s";
        fontRenderer.drawString(currentRateText, 35, 75, 0x2020FF);

        if (btnRedstone != null) {
            btnRedstone.displayString = getRedstoneModeText();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;

        if (mouseX >= x + 10 && mouseX <= x + 30 && mouseY >= y + 10 && mouseY <= y + 40) {
            drawHoveringText(
                    java.util.Arrays
                            .asList("Energia: " + tile.getEnergyStored() + " / " + tile.getMaxEnergyStored() + " RF"),
                    mouseX, mouseY);
        }

        renderHoveredToolTip(mouseX, mouseY);
    }
}

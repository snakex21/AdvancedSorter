package com.antigravity.advancedsorter.machines.deconstructor;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Proste GUI dla Deconstructor - bez tekstur PNG.
 * Deconstructor: Item -> Skladniki (3x3 grid)
 */
public class GuiDeconstructor extends GuiContainer {

    private final TileDeconstructor tile;

    public GuiDeconstructor(InventoryPlayer playerInv, TileDeconstructor tile) {
        super(new ContainerDeconstructor(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 204;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);

        if (isPointInRegion(8, 18, 14, 52, mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a7cEnergia");
            tooltip.add(formatEnergy(tile.getEnergyStored()) + " / " + formatEnergy(tile.getMaxEnergyStored()) + " RF");
            drawHoveringText(tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int i = guiLeft;
        int j = guiTop;

        drawRect(i - 2, j - 2, i + xSize + 2, j + ySize + 2, 0xFF333333);
        drawRect(i, j, i + xSize, j + ySize, 0xFFC6C6C6);

        // Pasek energii
        drawRect(i + 7, j + 17, i + 23, j + 71, 0xFF555555);
        drawRect(i + 8, j + 18, i + 22, j + 70, 0xFF222222);

        int energy = tile.getEnergyStored();
        int maxEnergy = tile.getMaxEnergyStored();
        if (maxEnergy > 0 && energy > 0) {
            int barHeight = (int) (50 * ((float) energy / maxEnergy));
            for (int h = 0; h < barHeight; h++) {
                int y = j + 69 - h;
                float ratio = (float) h / 50;
                int color = 0xFF000000 | (200 << 16) | ((int)(50 + ratio * 150) << 8) | 50;
                drawRect(i + 9, y, i + 21, y + 1, color);
            }
        }

        // Slot wejsciowy (80, 20)
        drawSlot(i + 79, j + 19);

        // Strzalka w dol
        fontRenderer.drawString("v", i + 84, j + 40, 0x404040);

        // Siatka wyjsciowa 3x3 (62, 52)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlot(i + 61 + col * 18, j + 51 + row * 18);
            }
        }

        // Postep
        int progress = tile.getProgress();
        if (progress > 0) {
            int barHeight2 = (int) (16 * ((float) progress / 40));
            drawRect(i + 100, j + 38, i + 104, j + 38 + barHeight2, 0xFF55FF55);
        }

        drawPlayerInventory(i, j, 122, 180);
    }

    private void drawSlot(int x, int y) {
        drawRect(x, y, x + 18, y + 18, 0xFF555555);
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        drawRect(x, y, x + 18, y + 1, 0xFF373737);
        drawRect(x, y, x + 1, y + 18, 0xFF373737);
        drawRect(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        drawRect(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
    }

    private void drawPlayerInventory(int i, int j, int invY, int hotbarY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawRect(i + 7 + col * 18, j + invY + row * 18, i + 25 + col * 18, j + invY + 18 + row * 18, 0xFF8B8B8B);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawRect(i + 7 + col * 18, j + hotbarY, i + 25 + col * 18, j + hotbarY + 18, 0xFF8B8B8B);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = "Deconstructor";
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 6, 0x404040);
        fontRenderer.drawString("Inwentarz", 8, 110, 0x404040);
    }

    private String formatEnergy(int energy) {
        if (energy >= 1000000) return String.format("%.1fM", energy / 1000000.0);
        else if (energy >= 1000) return String.format("%.1fk", energy / 1000.0);
        return String.valueOf(energy);
    }
}

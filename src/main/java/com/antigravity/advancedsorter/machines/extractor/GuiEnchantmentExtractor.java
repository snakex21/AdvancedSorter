package com.antigravity.advancedsorter.machines.extractor;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;

import java.util.ArrayList;
import java.util.List;

public class GuiEnchantmentExtractor extends GuiContainer {

    private final TileEnchantmentExtractor tile;

    // Colors
    private static final int COLOR_BG_MAIN = 0xFF252525;
    private static final int COLOR_BG_BORDER = 0xFF505050;
    private static final int COLOR_SLOT_BG = 0xFF151515;
    private static final int COLOR_SLOT_BORDER = 0xFF353535;
    private static final int COLOR_TEXT_HEADER = 0xFFFFFFFF;
    private static final int COLOR_TEXT = 0xFFCCCCCC;

    public GuiEnchantmentExtractor(InventoryPlayer playerInv, TileEnchantmentExtractor tile) {
        super(new ContainerEnchantmentExtractor(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);

        if (isPointInRegion(8, 20, 14, 50, mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a7eEnergy");
            tooltip.add(formatEnergy(tile.getEnergyStored()) + " / " + formatEnergy(tile.getMaxEnergyStored()) + " RF");
            drawHoveringText(tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawRect(0, 0, this.width, this.height, 0xBB000000);

        int i = guiLeft;
        int j = guiTop;

        // Main Background
        drawRect(i - 2, j - 2, i + xSize + 2, j + ySize + 2, COLOR_BG_BORDER);
        drawRect(i, j, i + xSize, j + ySize, COLOR_BG_MAIN);

        // Header separator
        drawRect(i, j + 16, i + xSize, j + 17, COLOR_BG_BORDER);

        // Energy Bar
        drawRect(i + 7, j + 19, i + 23, j + 71, COLOR_SLOT_BORDER);
        drawRect(i + 8, j + 20, i + 22, j + 70, COLOR_SLOT_BG);

        int energy = tile.getEnergyStored();
        int maxEnergy = tile.getMaxEnergyStored();
        if (maxEnergy > 0 && energy > 0) {
            int barHeight = (int) (50 * ((float) energy / maxEnergy));
            int y = j + 70 - barHeight;
            drawRect(i + 8, y, i + 22, j + 70, 0xFFFFAA00);
        }

        // Input Slots (44, 26) & (44, 52)
        drawSlotBox(i + 43, j + 25);
        drawSlotBox(i + 43, j + 51);

        // Output Slots (116, 26) & (116, 52)
        drawSlotBox(i + 115, j + 25);
        drawSlotBox(i + 115, j + 51);

        // Progress Arrow
        // Draw between inputs and outputs
        int progress = tile.getProgress();
        int maxProgress = tile.getMaxProgress();

        // Static arrow background
        drawRect(i + 70, j + 38, i + 100, j + 44, COLOR_SLOT_BG);

        if (progress > 0 && maxProgress > 0) {
            int arrowWidth = (int) (30 * ((float) progress / maxProgress));
            drawRect(i + 70, j + 39, i + 70 + arrowWidth, j + 43, 0xFF00AAAA); // Cyan progress for magic
        }

        fontRenderer.drawString("Extraction", i + 65, j + 28, 0x555555);

        // Player Inventory
        drawPlayerInventoryBackground(i, j);
    }

    private void drawSlotBox(int x, int y) {
        drawRect(x - 1, y - 1, x + 17, y + 17, COLOR_SLOT_BORDER);
        drawRect(x, y, x + 16, y + 16, COLOR_SLOT_BG);
    }

    private void drawPlayerInventoryBackground(int i, int j) {
        drawRect(i + 4, j + 82, i + xSize - 4, j + 83, COLOR_BG_BORDER);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBox(i + 7 + col * 18, j + 83 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBox(i + 7 + col * 18, j + 141);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = "EXTRACTOR";
        int w = fontRenderer.getStringWidth(title);
        fontRenderer.drawString(title, (xSize - w) / 2, 6, COLOR_TEXT_HEADER);
        fontRenderer.drawString("Inventory", 8, ySize - 96 + 2, COLOR_TEXT);
    }

    private String formatEnergy(int energy) {
        if (energy >= 1000000) return String.format("%.1fM", energy / 1000000.0);
        else if (energy >= 1000) return String.format("%.1fk", energy / 1000.0);
        return String.valueOf(energy);
    }
}

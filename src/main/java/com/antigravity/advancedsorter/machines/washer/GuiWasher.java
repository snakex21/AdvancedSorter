package com.antigravity.advancedsorter.machines.washer;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Proste GUI dla Washera - bez tekstur PNG, rysowane kodem.
 * Washer: Sand -> Clay, Concrete Powder -> Concrete
 * Wymaga: Energia RF + Woda
 */
public class GuiWasher extends GuiContainer {

    private final TileWasher tile;

    public GuiWasher(InventoryPlayer playerInv, TileWasher tile) {
        super(new ContainerWasher(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);

        // Energy tooltip
        if (isPointInRegion(8, 18, 14, 52, mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a7cEnergia");
            tooltip.add(formatEnergy(tile.getEnergyStored()) + " / " + formatEnergy(tile.getMaxEnergyStored()) + " RF");
            drawHoveringText(tooltip, mouseX, mouseY);
        }

        // Water tank tooltip
        if (isPointInRegion(152, 18, 16, 52, mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a79Woda");
            tooltip.add(tile.getWaterAmount() + " / " + tile.getWaterCapacity() + " mB");
            drawHoveringText(tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int i = guiLeft;
        int j = guiTop;

        // Tlo okna
        drawRect(i - 2, j - 2, i + xSize + 2, j + ySize + 2, 0xFF333333);
        drawRect(i, j, i + xSize, j + ySize, 0xFFC6C6C6);

        // === Pasek energii (lewa strona) ===
        drawRect(i + 7, j + 17, i + 23, j + 71, 0xFF555555); // Ramka
        drawRect(i + 8, j + 18, i + 22, j + 70, 0xFF222222); // Tlo

        int energy = tile.getEnergyStored();
        int maxEnergy = tile.getMaxEnergyStored();
        if (maxEnergy > 0 && energy > 0) {
            int barHeight = (int) (50 * ((float) energy / maxEnergy));
            // Gradient od czerwonego (gora) do zoltego (dol)
            for (int h = 0; h < barHeight; h++) {
                int y = j + 69 - h;
                float ratio = (float) h / 50;
                int r = 200;
                int g = (int) (50 + ratio * 150);
                int b = 50;
                int color = 0xFF000000 | (r << 16) | (g << 8) | b;
                drawRect(i + 9, y, i + 21, y + 1, color);
            }
        }

        // === Slot wejsciowy (56, 35) ===
        drawSlot(i + 55, j + 34);

        // === Strzalka postepu ===
        drawRect(i + 76, j + 36, i + 104, j + 52, 0xFF8B8B8B); // Tlo strzalki

        int progress = tile.getProgress();
        int maxProgress = 20;
        if (progress > 0) {
            int arrowWidth = (int) (26 * ((float) progress / maxProgress));
            drawRect(i + 77, j + 37, i + 77 + arrowWidth, j + 51, 0xFF55FF55);
        }

        // Trojkat strzalki
        fontRenderer.drawString(">", i + 97, j + 40, 0x404040);

        // === Slot wyjsciowy (116, 35) ===
        drawSlot(i + 115, j + 34);

        // === Zbiornik wody (prawa strona) ===
        drawRect(i + 151, j + 17, i + 169, j + 71, 0xFF555555); // Ramka
        drawRect(i + 152, j + 18, i + 168, j + 70, 0xFF222222); // Tlo

        int water = tile.getWaterAmount();
        int maxWater = tile.getWaterCapacity();
        if (maxWater > 0 && water > 0) {
            int fillHeight = (int) (50 * ((float) water / maxWater));
            drawRect(i + 153, j + 69 - fillHeight, i + 167, j + 69, 0xFF3366CC);
        }

        // === Inwentarz gracza ===
        drawPlayerInventory(i, j);
    }

    private void drawSlot(int x, int y) {
        // Ramka 3D
        drawRect(x, y, x + 18, y + 18, 0xFF555555);
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        // Cien 3D
        drawRect(x, y, x + 18, y + 1, 0xFF373737);
        drawRect(x, y, x + 1, y + 18, 0xFF373737);
        drawRect(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        drawRect(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
    }

    private void drawPlayerInventory(int i, int j) {
        // Tlo inwentarza (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = i + 7 + col * 18;
                int slotY = j + 83 + row * 18;
                drawRect(slotX, slotY, slotX + 18, slotY + 18, 0xFF8B8B8B);
            }
        }

        // Tlo hotbara (1x9)
        for (int col = 0; col < 9; col++) {
            int slotX = i + 7 + col * 18;
            int slotY = j + 141;
            drawRect(slotX, slotY, slotX + 18, slotY + 18, 0xFF8B8B8B);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = "Washer";
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 6, 0x404040);
        fontRenderer.drawString("Inwentarz", 8, ySize - 96 + 2, 0x404040);
    }

    private String formatEnergy(int energy) {
        if (energy >= 1000000) {
            return String.format("%.1fM", energy / 1000000.0);
        } else if (energy >= 1000) {
            return String.format("%.1fk", energy / 1000.0);
        }
        return String.valueOf(energy);
    }
}

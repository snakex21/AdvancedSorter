package com.antigravity.advancedsorter.machines.trashcan;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiTrashCan extends GuiContainer {

    private static final int COLOR_BG_MAIN = 0xFF252525;
    private static final int COLOR_BG_BORDER = 0xFF505050;
    private static final int COLOR_SLOT_BG = 0xFF151515;
    private static final int COLOR_SLOT_BORDER = 0xFF353535;
    private static final int COLOR_TEXT_HEADER = 0xFFFFFFFF;
    private static final int COLOR_TEXT = 0xFFCCCCCC;

    public GuiTrashCan(InventoryPlayer playerInv, TileTrashCan tile) {
        super(new ContainerTrashCan(playerInv, tile));
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
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

        // Trash Slot (Center)
        drawSlotBox(i + 79, j + 34); // Centered around 80, 35

        // Player Inventory
        drawPlayerInventoryBackground(i, j);
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

    private void drawSlotBox(int x, int y) {
        drawRect(x - 1, y - 1, x + 17, y + 17, COLOR_SLOT_BORDER);
        drawRect(x, y, x + 16, y + 16, COLOR_SLOT_BG);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = "TRASH CAN";
        int w = fontRenderer.getStringWidth(title);
        fontRenderer.drawString(title, (xSize - w) / 2, 6, COLOR_TEXT_HEADER);
        fontRenderer.drawString("Inventory", 8, ySize - 96 + 2, COLOR_TEXT);

        fontRenderer.drawString("Drop items to VOID", 45, 55, 0xFF5555);
    }
}

package com.antigravity.advancedsorter.pipes.directional;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.network.PacketCyclePipeMode;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

/**
 * GUI for configuring Directional Pipe sides.
 * Shows 6 buttons with colored indicators for direction.
 */
@SideOnly(Side.CLIENT)
public class GuiDirectionalPipe extends GuiScreen {

    private final TileDirectionalPipe tile;
    private GuiButton[] sideButtons = new GuiButton[6];

    // Direction colors (matching arm textures)
    private static final int COLOR_NORTH = 0xFFFF8C00; // Orange
    private static final int COLOR_SOUTH = 0xFF00FF00; // Green
    private static final int COLOR_EAST = 0xFFFF0000; // Red
    private static final int COLOR_WEST = 0xFF808080; // Gray
    private static final int COLOR_UP = 0xFF0088FF; // Blue
    private static final int COLOR_DOWN = 0xFFFFFF00; // Yellow

    private int xSize = 220;
    private int ySize = 120;
    private int guiLeft;
    private int guiTop;

    public GuiDirectionalPipe(TileDirectionalPipe tile) {
        this.tile = tile;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();

        guiLeft = (width - xSize) / 2;
        guiTop = (height - ySize) / 2;

        int centerX = guiLeft + xSize / 2 - 20;
        int btnSize = 28;

        int row1Y = guiTop + 18;
        int row2Y = guiTop + 48;
        int row3Y = guiTop + 78;

        // UP button (top center)
        sideButtons[EnumFacing.UP.getIndex()] = addButton(
                new GuiButton(EnumFacing.UP.getIndex(), centerX - btnSize / 2, row1Y, btnSize, 20, ""));

        // Middle row: WEST, NORTH, EAST, SOUTH - spread out more
        sideButtons[EnumFacing.WEST.getIndex()] = addButton(
                new GuiButton(EnumFacing.WEST.getIndex(), centerX - btnSize * 2 - 5, row2Y, btnSize, 20, ""));
        sideButtons[EnumFacing.NORTH.getIndex()] = addButton(
                new GuiButton(EnumFacing.NORTH.getIndex(), centerX - btnSize / 2, row2Y, btnSize, 20, ""));
        sideButtons[EnumFacing.EAST.getIndex()] = addButton(
                new GuiButton(EnumFacing.EAST.getIndex(), centerX + btnSize / 2 + 10, row2Y, btnSize, 20, ""));
        sideButtons[EnumFacing.SOUTH.getIndex()] = addButton(
                new GuiButton(EnumFacing.SOUTH.getIndex(), centerX + btnSize + btnSize / 2 + 20, row2Y, btnSize, 20,
                        ""));

        // DOWN button
        sideButtons[EnumFacing.DOWN.getIndex()] = addButton(
                new GuiButton(EnumFacing.DOWN.getIndex(), centerX - btnSize / 2, row3Y, btnSize, 20, ""));

        updateButtonLabels();
    }

    private int getDirectionColor(EnumFacing face) {
        switch (face) {
            case NORTH:
                return COLOR_NORTH;
            case SOUTH:
                return COLOR_SOUTH;
            case EAST:
                return COLOR_EAST;
            case WEST:
                return COLOR_WEST;
            case UP:
                return COLOR_UP;
            case DOWN:
                return COLOR_DOWN;
            default:
                return 0xFFFFFFFF;
        }
    }

    private void updateButtonLabels() {
        for (EnumFacing face : EnumFacing.VALUES) {
            GuiButton btn = sideButtons[face.getIndex()];
            if (btn != null) {
                TileDirectionalPipe.SideMode mode = tile.getSideMode(face);

                String modeText;
                switch (mode) {
                    case INPUT:
                        modeText = "\u00a7aIN"; // Green
                        break;
                    case OUTPUT:
                        modeText = "\u00a7cOUT"; // Red
                        break;
                    case DISABLED:
                    default:
                        modeText = "\u00a7fX"; // White
                        break;
                }
                btn.displayString = modeText;
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id >= 0 && button.id < 6) {
            AdvancedSorterMod.network.sendToServer(new PacketCyclePipeMode(tile.getPos(), button.id));
            tile.cycleMode(EnumFacing.VALUES[button.id]);
            updateButtonLabels();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Background
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF2D2D2D);
        drawRect(guiLeft + 2, guiTop + 2, guiLeft + xSize - 2, guiTop + ySize - 2, 0xFF4A4A4A);

        // Title
        String title = "Directional Pipe";
        fontRenderer.drawString(title, guiLeft + (xSize - fontRenderer.getStringWidth(title)) / 2, guiTop + 5,
                0xFFFFFF);

        // Draw colored direction indicators next to buttons
        for (EnumFacing face : EnumFacing.VALUES) {
            GuiButton btn = sideButtons[face.getIndex()];
            if (btn != null) {
                int color = getDirectionColor(face);
                boolean connected = tile.isSideConnected(face);

                int indicatorSize = 6;
                int ix = btn.x - indicatorSize - 3;
                int iy = btn.y + (btn.height - indicatorSize) / 2;

                // For SOUTH, draw on right side
                if (face == EnumFacing.SOUTH) {
                    ix = btn.x + btn.width + 3;
                }

                if (connected) {
                    // Filled square when connected
                    drawRect(ix, iy, ix + indicatorSize, iy + indicatorSize, color);
                } else {
                    // Outline only when not connected
                    drawRect(ix, iy, ix + indicatorSize, iy + 1, color); // top
                    drawRect(ix, iy + indicatorSize - 1, ix + indicatorSize, iy + indicatorSize, color); // bottom
                    drawRect(ix, iy, ix + 1, iy + indicatorSize, color); // left
                    drawRect(ix + indicatorSize - 1, iy, ix + indicatorSize, iy + indicatorSize, color); // right
                }
            }
        }

        // Legend at bottom
        int legendY = guiTop + ySize - 14;
        fontRenderer.drawString("\u00a7aIN", guiLeft + 15, legendY, 0x55FF55);
        fontRenderer.drawString("|", guiLeft + 37, legendY, 0xAAAAAA);
        fontRenderer.drawString("\u00a7cOUT", guiLeft + 47, legendY, 0xFF5555);
        fontRenderer.drawString("|", guiLeft + 77, legendY, 0xAAAAAA);
        fontRenderer.drawString("\u00a7fX=Off", guiLeft + 87, legendY, 0xFFFFFF);

        // Draw buttons
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        updateButtonLabels();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

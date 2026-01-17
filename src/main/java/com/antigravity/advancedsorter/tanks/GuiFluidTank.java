package com.antigravity.advancedsorter.tanks;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.network.PacketCyclePipeMode;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;

import java.io.IOException;

public class GuiFluidTank extends GuiContainer {

    private final TileFluidTank tile;
    private GuiButton[] sideButtons = new GuiButton[6];

    // Direction colors
    private static final int COLOR_NORTH = 0xFFFF8C00; // Orange
    private static final int COLOR_SOUTH = 0xFF00FF00; // Green
    private static final int COLOR_EAST = 0xFFFF0000; // Red
    private static final int COLOR_WEST = 0xFF808080; // Gray
    private static final int COLOR_UP = 0xFF0088FF; // Blue
    private static final int COLOR_DOWN = 0xFFFFFF00; // Yellow

    public GuiFluidTank(InventoryPlayer playerInv, TileFluidTank tile) {
        super(new ContainerFluidTank(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();

        int btnSize = 24;
        int startX = guiLeft + 100;
        int startY = guiTop + 55;

        // Layout for side buttons:
        //      [UP]
        // [W] [N] [E] [S]
        //      [DOWN]

        // UP button (top center)
        sideButtons[EnumFacing.UP.getIndex()] = addButton(
                new GuiButton(EnumFacing.UP.getIndex(), startX, startY, btnSize, 20, ""));

        // Middle row
        int row2Y = startY + 22;
        sideButtons[EnumFacing.WEST.getIndex()] = addButton(
                new GuiButton(EnumFacing.WEST.getIndex(), startX - btnSize - 2, row2Y, btnSize, 20, ""));
        sideButtons[EnumFacing.NORTH.getIndex()] = addButton(
                new GuiButton(EnumFacing.NORTH.getIndex(), startX, row2Y, btnSize, 20, ""));
        sideButtons[EnumFacing.EAST.getIndex()] = addButton(
                new GuiButton(EnumFacing.EAST.getIndex(), startX + btnSize + 2, row2Y, btnSize, 20, ""));
        sideButtons[EnumFacing.SOUTH.getIndex()] = addButton(
                new GuiButton(EnumFacing.SOUTH.getIndex(), startX + (btnSize + 2) * 2, row2Y, btnSize, 20, ""));

        // DOWN button
        sideButtons[EnumFacing.DOWN.getIndex()] = addButton(
                new GuiButton(EnumFacing.DOWN.getIndex(), startX, row2Y + 22, btnSize, 20, ""));

        updateButtonLabels();
    }

    private int getDirectionColor(EnumFacing face) {
        switch (face) {
            case NORTH: return COLOR_NORTH;
            case SOUTH: return COLOR_SOUTH;
            case EAST: return COLOR_EAST;
            case WEST: return COLOR_WEST;
            case UP: return COLOR_UP;
            case DOWN: return COLOR_DOWN;
            default: return 0xFFFFFFFF;
        }
    }

    private void updateButtonLabels() {
        for (EnumFacing face : EnumFacing.VALUES) {
            GuiButton btn = sideButtons[face.getIndex()];
            if (btn != null) {
                TileFluidTank.SideMode mode = tile.getSideMode(face);
                String modeText;
                switch (mode) {
                    case INPUT:
                        modeText = "\u00a7aIN";
                        break;
                    case OUTPUT:
                        modeText = "\u00a7cOUT";
                        break;
                    case DISABLED:
                    default:
                        modeText = "\u00a7fX";
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
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);

        // Tooltip for fluid gauge
        int gaugeX = guiLeft + 10;
        int gaugeY = guiTop + 20;
        int gaugeWidth = 20;
        int gaugeHeight = 100;

        if (mouseX >= gaugeX && mouseX <= gaugeX + gaugeWidth && mouseY >= gaugeY && mouseY <= gaugeY + gaugeHeight) {
            FluidStack fluid = tile.getTank().getFluid();
            String text;
            if (fluid != null && fluid.amount > 0) {
                text = fluid.getLocalizedName() + ": " + formatAmount(fluid.amount) + " / " + formatAmount(tile.getTank().getCapacity());
            } else {
                text = "Empty: 0 / " + formatAmount(tile.getTank().getCapacity());
            }
            drawHoveringText(java.util.Collections.singletonList(text), mouseX, mouseY);
        }
    }

    private String formatAmount(int amount) {
        if (amount >= 1000000) {
            return String.format("%.2fM mB", amount / 1000000.0);
        } else if (amount >= 1000) {
            return String.format("%.1fK mB", amount / 1000.0);
        }
        return amount + " mB";
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        // Background
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF2D2D2D);
        drawRect(guiLeft + 2, guiTop + 2, guiLeft + xSize - 2, guiTop + ySize - 2, 0xFF4A4A4A);

        // Fluid Gauge Background
        int gaugeX = guiLeft + 10;
        int gaugeY = guiTop + 20;
        int gaugeWidth = 20;
        int gaugeHeight = 100;

        drawRect(gaugeX, gaugeY, gaugeX + gaugeWidth, gaugeY + gaugeHeight, 0xFF1A1A1A);

        // Fluid level
        FluidStack fluid = tile.getTank().getFluid();
        if (fluid != null && fluid.amount > 0) {
            float fillLevel = (float) fluid.amount / tile.getTank().getCapacity();
            int fluidHeight = (int) (gaugeHeight * fillLevel);
            int color = fluid.getFluid().getColor(fluid) | 0xFF000000;
            drawRect(gaugeX + 1, gaugeY + gaugeHeight - fluidHeight, gaugeX + gaugeWidth - 1, gaugeY + gaugeHeight, color);
        }

        // Gauge border
        drawRect(gaugeX, gaugeY, gaugeX + 1, gaugeY + gaugeHeight, 0xFF808080);
        drawRect(gaugeX + gaugeWidth - 1, gaugeY, gaugeX + gaugeWidth, gaugeY + gaugeHeight, 0xFF808080);
        drawRect(gaugeX, gaugeY, gaugeX + gaugeWidth, gaugeY + 1, 0xFF808080);
        drawRect(gaugeX, gaugeY + gaugeHeight - 1, gaugeX + gaugeWidth, gaugeY + gaugeHeight, 0xFF808080);

        // Draw connection indicators around buttons (green border if pipe connected)
        for (EnumFacing face : EnumFacing.VALUES) {
            GuiButton btn = sideButtons[face.getIndex()];
            if (btn != null && tile.hasFluidConnection(face)) {
                int bx = btn.x;
                int by = btn.y;
                int bw = btn.width;
                int bh = btn.height;
                int connColor = 0xFF00FF00; // Green for connected
                // Draw thicker border (2px)
                drawRect(bx - 2, by - 2, bx + bw + 2, by, connColor); // Top
                drawRect(bx - 2, by + bh, bx + bw + 2, by + bh + 2, connColor); // Bottom
                drawRect(bx - 2, by, bx, by + bh, connColor); // Left
                drawRect(bx + bw, by, bx + bw + 2, by + bh, connColor); // Right
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // Title with tier
        String title = tile.getTier().getName().substring(0, 1).toUpperCase() + tile.getTier().getName().substring(1) + " Fluid Tank";
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 6, getTierColor());

        // Fluid info
        FluidStack fluid = tile.getTank().getFluid();
        String fluidName = fluid != null ? fluid.getLocalizedName() : "Empty";
        fontRenderer.drawString(fluidName, 35, 25, 0xFFFFFF);

        String amount = fluid != null ? formatAmount(fluid.amount) : "0 mB";
        fontRenderer.drawString(amount, 35, 35, 0xAAAAAA);

        // Capacity
        fontRenderer.drawString("/ " + formatAmount(tile.getTank().getCapacity()), 35, 45, 0x808080);

        // Side config label
        fontRenderer.drawString("Side Config:", 85, 35, 0xFFFFFF);

        // Legend
        int legendY = 130;
        fontRenderer.drawString("\u00a7aIN", 100, legendY, 0x55FF55);
        fontRenderer.drawString("= Input", 115, legendY, 0xAAAAAA);
        fontRenderer.drawString("\u00a7cOUT", 100, legendY + 10, 0xFF5555);
        fontRenderer.drawString("= Output", 125, legendY + 10, 0xAAAAAA);
        fontRenderer.drawString("\u00a7fX", 100, legendY + 20, 0xFFFFFF);
        fontRenderer.drawString("= Disabled", 110, legendY + 20, 0xAAAAAA);

        // Draw colored direction labels
        for (EnumFacing face : EnumFacing.VALUES) {
            GuiButton btn = sideButtons[face.getIndex()];
            if (btn != null) {
                int color = getDirectionColor(face);
                String dirLabel = face.name().substring(0, 1);
                int labelX = btn.x - guiLeft + btn.width / 2 - fontRenderer.getStringWidth(dirLabel) / 2;
                int labelY = btn.y - guiTop - 10;

                if (face == EnumFacing.DOWN) {
                    labelY = btn.y - guiTop + btn.height + 2;
                }

                fontRenderer.drawString(dirLabel, labelX, labelY, color);
            }
        }
    }

    private int getTierColor() {
        switch (tile.getTier()) {
            case BASIC: return 0xAAAAAA;
            case ADVANCED: return 0xFF5555;
            case ELITE: return 0x5555FF;
            case ULTIMATE: return 0x55FF55;
            default: return 0xFFFFFF;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        updateButtonLabels();
    }
}

package com.antigravity.advancedsorter.client.render;

import com.antigravity.advancedsorter.pipes.gas.teleport.TileTeleportGasPipe;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

/**
 * Renderer for teleport gas pipes that shows the gas inside with correct color.
 * Renders gas OUTSIDE the main pipe model so it's visible.
 * Uses client-side caching to prevent flickering during updates.
 */
public class TileTeleportGasPipeRenderer extends TileEntitySpecialRenderer<TileTeleportGasPipe> {

    private static final float PIPE_MIN = 0.25f;
    private static final float PIPE_MAX = 0.75f;

    // Client-side cache to prevent flickering
    // Stores the last known gas info per pipe position
    private static class GasCache {
        float r, g, b;
        float fillLevel;
        Gas gas;
        long lastUpdateTime;

        GasCache(Gas gas, float r, float g, float b, float fillLevel) {
            this.gas = gas;
            this.r = r;
            this.g = g;
            this.b = b;
            this.fillLevel = fillLevel;
            this.lastUpdateTime = System.currentTimeMillis();
        }
    }

    private static final Map<Long, GasCache> gasCache = new HashMap<>();

    @Override
    public void render(TileTeleportGasPipe te, double x, double y, double z, float partialTicks, int destroyStage,
            float alpha) {

        long posKey = te.getPos().toLong();
        // Access the tank directly - TileTeleportGasPipe has public getTank()
        GasStack gasStack = te.getTank().getGas();
        GasCache cache = gasCache.get(posKey);

        float r, g, b, a, fillLevel;
        Gas gas;

        // Update cache if we have new gas data
        if (gasStack != null && gasStack.amount > 0 && gasStack.getGas() != null) {
            gas = gasStack.getGas();
            fillLevel = (float) gasStack.amount / te.getTank().getMaxGas();
            fillLevel = Math.min(1.0f, Math.max(0.1f, fillLevel));

            int color = gas.getTint();
            r = ((color >> 16) & 0xFF) / 255.0f;
            g = ((color >> 8) & 0xFF) / 255.0f;
            b = (color & 0xFF) / 255.0f;

            // Update cache
            gasCache.put(posKey, new GasCache(gas, r, g, b, fillLevel));
        } else if (cache != null) {
            // No current gas data, but we have cache - use cached values
            // Only clear cache if gas is truly empty (amount == 0, not just null during sync)
            if (gasStack != null && gasStack.amount == 0) {
                // Gas is actually empty, remove from cache
                gasCache.remove(posKey);
                return;
            }
            // Use cached values during sync gaps
            r = cache.r;
            g = cache.g;
            b = cache.b;
            fillLevel = cache.fillLevel;
            gas = cache.gas;
        } else {
            // No gas and no cache - nothing to render
            return;
        }

        a = 0.75f; // Transparency

        // Get gas texture
        TextureAtlasSprite sprite = getGasSprite(gas);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        // Enable blending for transparency
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.color(r, g, b, a);

        // Bind texture atlas
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        // Draw gas in connections (arms going to edge of block)
        for (EnumFacing face : te.getConnections()) {
            drawConnectionGas(face, fillLevel, r, g, b, a, sprite);
        }

        // Draw gas indicators on the core (small patches on each visible face)
        drawCoreGasIndicators(te, fillLevel, r, g, b, a, sprite);

        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void drawCoreGasIndicators(TileTeleportGasPipe te, float fillLevel,
            float r, float g, float b, float a, TextureAtlasSprite sprite) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        float u1 = sprite.getMinU();
        float v1 = sprite.getMinV();
        float u2 = sprite.getMaxU();
        float v2 = sprite.getMaxV();

        // Scaled UV based on the small indicator size
        float uvScale = 0.4f;
        float u2s = u1 + (u2 - u1) * uvScale;
        float v2full = v1 + (v2 - v1) * uvScale;

        // Size of gas indicator patches
        float indicatorMin = 0.30f;
        float indicatorMax = 0.70f;
        float indicatorHeight = indicatorMax; // Always full square for indicators

        // Offset to render just outside the pipe model
        float offset = 0.001f;

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        // Draw on faces that don't have connections
        if (!te.getConnections().contains(EnumFacing.NORTH)) {
            // North face (Z = 0.25 - offset)
            float z1 = PIPE_MIN - offset;
            buffer.pos(indicatorMin, indicatorMin, z1).tex(u1, v1).color(r, g, b, a).endVertex();
            buffer.pos(indicatorMin, indicatorHeight, z1).tex(u1, v2full).color(r, g, b, a).endVertex();
            buffer.pos(indicatorMax, indicatorHeight, z1).tex(u2s, v2full).color(r, g, b, a).endVertex();
            buffer.pos(indicatorMax, indicatorMin, z1).tex(u2s, v1).color(r, g, b, a).endVertex();
        }

        if (!te.getConnections().contains(EnumFacing.SOUTH)) {
            // South face (Z = 0.75 + offset)
            float z1 = PIPE_MAX + offset;
            buffer.pos(indicatorMax, indicatorMin, z1).tex(u2s, v1).color(r, g, b, a).endVertex();
            buffer.pos(indicatorMax, indicatorHeight, z1).tex(u2s, v2full).color(r, g, b, a).endVertex();
            buffer.pos(indicatorMin, indicatorHeight, z1).tex(u1, v2full).color(r, g, b, a).endVertex();
            buffer.pos(indicatorMin, indicatorMin, z1).tex(u1, v1).color(r, g, b, a).endVertex();
        }

        if (!te.getConnections().contains(EnumFacing.WEST)) {
            // West face (X = 0.25 - offset)
            float x1 = PIPE_MIN - offset;
            buffer.pos(x1, indicatorMin, indicatorMax).tex(u2s, v1).color(r, g, b, a).endVertex();
            buffer.pos(x1, indicatorHeight, indicatorMax).tex(u2s, v2full).color(r, g, b, a).endVertex();
            buffer.pos(x1, indicatorHeight, indicatorMin).tex(u1, v2full).color(r, g, b, a).endVertex();
            buffer.pos(x1, indicatorMin, indicatorMin).tex(u1, v1).color(r, g, b, a).endVertex();
        }

        if (!te.getConnections().contains(EnumFacing.EAST)) {
            // East face (X = 0.75 + offset)
            float x1 = PIPE_MAX + offset;
            buffer.pos(x1, indicatorMin, indicatorMin).tex(u1, v1).color(r, g, b, a).endVertex();
            buffer.pos(x1, indicatorHeight, indicatorMin).tex(u1, v2full).color(r, g, b, a).endVertex();
            buffer.pos(x1, indicatorHeight, indicatorMax).tex(u2s, v2full).color(r, g, b, a).endVertex();
            buffer.pos(x1, indicatorMin, indicatorMax).tex(u2s, v1).color(r, g, b, a).endVertex();
        }

        if (!te.getConnections().contains(EnumFacing.UP)) {
            // Top face (Y = 0.75 + offset)
            float y1 = PIPE_MAX + offset;
            buffer.pos(indicatorMin, y1, indicatorMin).tex(u1, v1).color(r, g, b, a).endVertex();
            buffer.pos(indicatorMin, y1, indicatorMax).tex(u1, v2full).color(r, g, b, a).endVertex();
            buffer.pos(indicatorMax, y1, indicatorMax).tex(u2s, v2full).color(r, g, b, a).endVertex();
            buffer.pos(indicatorMax, y1, indicatorMin).tex(u2s, v1).color(r, g, b, a).endVertex();
        }

        if (!te.getConnections().contains(EnumFacing.DOWN)) {
            // Bottom face (Y = 0.25 - offset)
            float y1 = PIPE_MIN - offset;
            buffer.pos(indicatorMin, y1, indicatorMax).tex(u1, v2full).color(r, g, b, a).endVertex();
            buffer.pos(indicatorMin, y1, indicatorMin).tex(u1, v1).color(r, g, b, a).endVertex();
            buffer.pos(indicatorMax, y1, indicatorMin).tex(u2s, v1).color(r, g, b, a).endVertex();
            buffer.pos(indicatorMax, y1, indicatorMax).tex(u2s, v2full).color(r, g, b, a).endVertex();
        }

        tessellator.draw();
    }

    private void drawConnectionGas(EnumFacing face, float fillLevel,
            float r, float g, float b, float a, TextureAtlasSprite sprite) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        float u1 = sprite.getMinU();
        float v1 = sprite.getMinV();
        float u2 = sprite.getMaxU();
        float v2 = sprite.getMaxV();

        // Connection arm dimensions - slightly smaller than pipe arm
        float armMin = 0.30f;
        float armMax = 0.70f;
        float armHeight = armMin + (armMax - armMin) * fillLevel;

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        switch (face) {
            case DOWN:
                // Arm going down - draw bottom face
                buffer.pos(armMin, 0.001f, armMin).tex(u1, v1).color(r, g, b, a).endVertex();
                buffer.pos(armMin, 0.001f, armMax).tex(u1, v2).color(r, g, b, a).endVertex();
                buffer.pos(armMax, 0.001f, armMax).tex(u2, v2).color(r, g, b, a).endVertex();
                buffer.pos(armMax, 0.001f, armMin).tex(u2, v1).color(r, g, b, a).endVertex();
                // Side faces of arm
                drawArmSidesGeneric(buffer, armMin, armMax, 0, PIPE_MIN, PIPE_MIN, PIPE_MAX, r, g, b, a, u1, v1, u2, v2,
                        EnumFacing.Axis.Y);
                break;

            case UP:
                // Arm going up - draw top face
                buffer.pos(armMin, 0.999f, armMax).tex(u1, v2).color(r, g, b, a).endVertex();
                buffer.pos(armMin, 0.999f, armMin).tex(u1, v1).color(r, g, b, a).endVertex();
                buffer.pos(armMax, 0.999f, armMin).tex(u2, v1).color(r, g, b, a).endVertex();
                buffer.pos(armMax, 0.999f, armMax).tex(u2, v2).color(r, g, b, a).endVertex();
                // Side faces of arm
                drawArmSidesGeneric(buffer, armMin, armMax, PIPE_MAX, 1, PIPE_MIN, PIPE_MAX, r, g, b, a, u1, v1, u2, v2,
                        EnumFacing.Axis.Y);
                break;

            case NORTH:
                // Arm going north - draw end face
                buffer.pos(armMin, armMin, 0.001f).tex(u1, v1).color(r, g, b, a).endVertex();
                buffer.pos(armMax, armMin, 0.001f).tex(u2, v1).color(r, g, b, a).endVertex();
                buffer.pos(armMax, armHeight, 0.001f).tex(u2, v2).color(r, g, b, a).endVertex();
                buffer.pos(armMin, armHeight, 0.001f).tex(u1, v2).color(r, g, b, a).endVertex();
                // Side faces of arm
                drawArmSidesGeneric(buffer, armMin, armMax, armMin, armHeight, 0, PIPE_MIN, r, g, b, a, u1, v1, u2, v2,
                        EnumFacing.Axis.Z);
                break;

            case SOUTH:
                // Arm going south - draw end face
                buffer.pos(armMax, armMin, 0.999f).tex(u2, v1).color(r, g, b, a).endVertex();
                buffer.pos(armMin, armMin, 0.999f).tex(u1, v1).color(r, g, b, a).endVertex();
                buffer.pos(armMin, armHeight, 0.999f).tex(u1, v2).color(r, g, b, a).endVertex();
                buffer.pos(armMax, armHeight, 0.999f).tex(u2, v2).color(r, g, b, a).endVertex();
                // Side faces of arm
                drawArmSidesGeneric(buffer, armMin, armMax, armMin, armHeight, PIPE_MAX, 1, r, g, b, a, u1, v1, u2, v2,
                        EnumFacing.Axis.Z);
                break;

            case WEST:
                // Arm going west - draw end face
                buffer.pos(0.001f, armMin, armMin).tex(u1, v1).color(r, g, b, a).endVertex();
                buffer.pos(0.001f, armMin, armMax).tex(u2, v1).color(r, g, b, a).endVertex();
                buffer.pos(0.001f, armHeight, armMax).tex(u2, v2).color(r, g, b, a).endVertex();
                buffer.pos(0.001f, armHeight, armMin).tex(u1, v2).color(r, g, b, a).endVertex();
                // Side faces of arm
                drawArmSidesGeneric(buffer, armMin, armMax, armMin, armHeight, 0, PIPE_MIN, r, g, b, a, u1, v1, u2, v2,
                        EnumFacing.Axis.X);
                break;

            case EAST:
                // Arm going east - draw end face
                buffer.pos(0.999f, armMin, armMax).tex(u2, v1).color(r, g, b, a).endVertex();
                buffer.pos(0.999f, armMin, armMin).tex(u1, v1).color(r, g, b, a).endVertex();
                buffer.pos(0.999f, armHeight, armMin).tex(u1, v2).color(r, g, b, a).endVertex();
                buffer.pos(0.999f, armHeight, armMax).tex(u2, v2).color(r, g, b, a).endVertex();
                // Side faces of arm
                drawArmSidesGeneric(buffer, armMin, armMax, armMin, armHeight, PIPE_MAX, 1, r, g, b, a, u1, v1, u2, v2,
                        EnumFacing.Axis.X);
                break;
        }

        tessellator.draw();
    }

    private void drawArmSidesGeneric(BufferBuilder buffer, float min, float max, float h1, float h2, float d1, float d2,
            float r, float g, float b, float a, float u1, float v1, float u2, float v2, EnumFacing.Axis axis) {

        if (axis == EnumFacing.Axis.Y) {
            // Vertical arm (Y axis)
            // North side (Z = min)
            buffer.pos(min, h1, min).tex(u1, v1).color(r, g, b, a).endVertex();
            buffer.pos(min, h2, min).tex(u1, v2).color(r, g, b, a).endVertex();
            buffer.pos(max, h2, min).tex(u2, v2).color(r, g, b, a).endVertex();
            buffer.pos(max, h1, min).tex(u2, v1).color(r, g, b, a).endVertex();

            // South side (Z = max)
            buffer.pos(max, h1, max).tex(u2, v1).color(r, g, b, a).endVertex();
            buffer.pos(max, h2, max).tex(u2, v2).color(r, g, b, a).endVertex();
            buffer.pos(min, h2, max).tex(u1, v2).color(r, g, b, a).endVertex();
            buffer.pos(min, h1, max).tex(u1, v1).color(r, g, b, a).endVertex();

            // West side (X = min)
            buffer.pos(min, h1, max).tex(u2, v1).color(r, g, b, a).endVertex();
            buffer.pos(min, h2, max).tex(u2, v2).color(r, g, b, a).endVertex();
            buffer.pos(min, h2, min).tex(u1, v2).color(r, g, b, a).endVertex();
            buffer.pos(min, h1, min).tex(u1, v1).color(r, g, b, a).endVertex();

            // East side (X = max)
            buffer.pos(max, h1, min).tex(u1, v1).color(r, g, b, a).endVertex();
            buffer.pos(max, h2, min).tex(u1, v2).color(r, g, b, a).endVertex();
            buffer.pos(max, h2, max).tex(u2, v2).color(r, g, b, a).endVertex();
            buffer.pos(max, h1, max).tex(u2, v1).color(r, g, b, a).endVertex();
        } else if (axis == EnumFacing.Axis.Z) {
            // Horizontal arm (Z axis)
            // Top side (Y = h2)
            buffer.pos(min, h2, d1).tex(u1, v1).color(r, g, b, a).endVertex();
            buffer.pos(min, h2, d2).tex(u1, v2).color(r, g, b, a).endVertex();
            buffer.pos(max, h2, d2).tex(u2, v2).color(r, g, b, a).endVertex();
            buffer.pos(max, h2, d1).tex(u2, v1).color(r, g, b, a).endVertex();

            // Bottom side (Y = h1)
            buffer.pos(max, h1, d1).tex(u2, v1).color(r, g, b, a).endVertex();
            buffer.pos(max, h1, d2).tex(u2, v2).color(r, g, b, a).endVertex();
            buffer.pos(min, h1, d2).tex(u1, v2).color(r, g, b, a).endVertex();
            buffer.pos(min, h1, d1).tex(u1, v1).color(r, g, b, a).endVertex();

            // West side (X = min)
            buffer.pos(min, h1, d1).tex(u1, v1).color(r, g, b, a).endVertex();
            buffer.pos(min, h2, d1).tex(u1, v2).color(r, g, b, a).endVertex();
            buffer.pos(min, h2, d2).tex(u2, v2).color(r, g, b, a).endVertex();
            buffer.pos(min, h1, d2).tex(u2, v1).color(r, g, b, a).endVertex();

            // East side (X = max)
            buffer.pos(max, h1, d2).tex(u2, v1).color(r, g, b, a).endVertex();
            buffer.pos(max, h2, d2).tex(u2, v2).color(r, g, b, a).endVertex();
            buffer.pos(max, h2, d1).tex(u1, v2).color(r, g, b, a).endVertex();
            buffer.pos(max, h1, d1).tex(u1, v1).color(r, g, b, a).endVertex();
        } else if (axis == EnumFacing.Axis.X) {
            // Horizontal arm (X axis)
            // Top side (Y = h2)
            buffer.pos(d1, h2, min).tex(u1, v1).color(r, g, b, a).endVertex();
            buffer.pos(d2, h2, min).tex(u1, v2).color(r, g, b, a).endVertex();
            buffer.pos(d2, h2, max).tex(u2, v2).color(r, g, b, a).endVertex();
            buffer.pos(d1, h2, max).tex(u2, v1).color(r, g, b, a).endVertex();

            // Bottom side (Y = h1)
            buffer.pos(d1, h1, max).tex(u2, v1).color(r, g, b, a).endVertex();
            buffer.pos(d2, h1, max).tex(u2, v2).color(r, g, b, a).endVertex();
            buffer.pos(d2, h1, min).tex(u1, v2).color(r, g, b, a).endVertex();
            buffer.pos(d1, h1, min).tex(u1, v1).color(r, g, b, a).endVertex();

            // North side (Z = min)
            buffer.pos(d1, h1, min).tex(u1, v1).color(r, g, b, a).endVertex();
            buffer.pos(d2, h1, min).tex(u1, v2).color(r, g, b, a).endVertex();
            buffer.pos(d2, h2, min).tex(u2, v2).color(r, g, b, a).endVertex();
            buffer.pos(d1, h2, min).tex(u2, v1).color(r, g, b, a).endVertex();

            // South side (Z = max)
            buffer.pos(d1, h2, max).tex(u2, v1).color(r, g, b, a).endVertex();
            buffer.pos(d2, h2, max).tex(u2, v2).color(r, g, b, a).endVertex();
            buffer.pos(d2, h1, max).tex(u1, v2).color(r, g, b, a).endVertex();
            buffer.pos(d1, h1, max).tex(u1, v1).color(r, g, b, a).endVertex();
        }
    }

    private TextureAtlasSprite getGasSprite(Gas gas) {
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
                .getAtlasSprite(gas.getIcon().toString());
        if (sprite == null) {
            sprite = Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
        }
        return sprite;
    }

    /**
     * Clear the cache for a specific position (called when pipe is removed)
     */
    public static void clearCache(long posKey) {
        gasCache.remove(posKey);
    }
}

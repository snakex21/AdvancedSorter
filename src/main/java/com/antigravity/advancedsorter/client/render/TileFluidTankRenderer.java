package com.antigravity.advancedsorter.client.render;

import com.antigravity.advancedsorter.tanks.TileFluidTank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;

/**
 * Renderer for fluid tanks that shows the fluid level inside.
 * Renders a cube of fluid inside the tank frame.
 */
public class TileFluidTankRenderer extends TileEntitySpecialRenderer<TileFluidTank> {

    // Fluid is rendered inside the glass panels (glass is at 0-1 and 15-16)
    private static final float MIN_XZ = 1.01f / 16f;
    private static final float MAX_XZ = 14.99f / 16f;
    private static final float MIN_Y = 2.01f / 16f;
    private static final float MAX_Y = 13.99f / 16f;

    // Client-side cache to prevent flickering and redundant calculations
    private static class TankCache {
        float r, g, b;
        float fillLevel;
        Fluid fluid;
        TextureAtlasSprite sprite;
        long lastUpdateTime;

        TankCache(Fluid fluid, TextureAtlasSprite sprite, float r, float g, float b, float fillLevel) {
            this.fluid = fluid;
            this.sprite = sprite;
            this.r = r;
            this.g = g;
            this.b = b;
            this.fillLevel = fillLevel;
            this.lastUpdateTime = System.currentTimeMillis();
        }
    }

    private static final java.util.Map<Long, TankCache> tankCache = new java.util.HashMap<>();

    @Override
    public void render(TileFluidTank te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        long posKey = te.getPos().toLong();
        FluidStack fluidStack = te.getTank().getFluid();
        TankCache cache = tankCache.get(posKey);

        float r, g, b, a, fillLevel;
        Fluid fluid;
        TextureAtlasSprite stillSprite;

        // Update cache if we have new fluid data
        if (fluidStack != null && fluidStack.amount > 0 && fluidStack.getFluid() != null) {
            fluid = fluidStack.getFluid();
            fillLevel = (float) fluidStack.amount / te.getTank().getCapacity();
            fillLevel = Math.min(1.0f, Math.max(0.01f, fillLevel)); // At least 1% visible

            int color = fluid.getColor(fluidStack);
            r = ((color >> 16) & 0xFF) / 255.0f;
            g = ((color >> 8) & 0xFF) / 255.0f;
            b = (color & 0xFF) / 255.0f;

            stillSprite = getFluidStillSprite(fluid);

            // Update cache
            tankCache.put(posKey, new TankCache(fluid, stillSprite, r, g, b, fillLevel));
        } else if (cache != null) {
            // No current fluid data, but we have cache - use cached values
            // Only clear cache if fluid is truly empty
            if (fluidStack != null && fluidStack.amount == 0) {
                tankCache.remove(posKey);
                return;
            }
            r = cache.r;
            g = cache.g;
            b = cache.b;
            fillLevel = cache.fillLevel;
            fluid = cache.fluid;
            stillSprite = cache.sprite;
        } else {
            return;
        }

        a = 0.9f;
        float fluidTop = MIN_Y + (MAX_Y - MIN_Y) * fillLevel;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        // Enable blending for transparency
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();

        // Bind texture atlas
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        float u1 = stillSprite.getMinU();
        float v1 = stillSprite.getMinV();
        float u2 = stillSprite.getMaxU();
        float v2 = stillSprite.getMaxV();

        // Scale UV to match the visible area
        // Note: Using pre-calculated UVs or simpler scaling avoids dependent texture reads if possible,
        // but getInterpolated is fine here as it's just math.
        float u1Scaled = stillSprite.getInterpolatedU(2);
        float v1Scaled = stillSprite.getInterpolatedV(2);
        float u2Scaled = stillSprite.getInterpolatedU(14);
        float v2Scaled = stillSprite.getInterpolatedV(14);

        // Top face (Y = fluidTop) - main visible face
        buffer.pos(MIN_XZ, fluidTop, MIN_XZ).tex(u1Scaled, v1Scaled).color(r, g, b, a).endVertex();
        buffer.pos(MIN_XZ, fluidTop, MAX_XZ).tex(u1Scaled, v2Scaled).color(r, g, b, a).endVertex();
        buffer.pos(MAX_XZ, fluidTop, MAX_XZ).tex(u2Scaled, v2Scaled).color(r, g, b, a).endVertex();
        buffer.pos(MAX_XZ, fluidTop, MIN_XZ).tex(u2Scaled, v1Scaled).color(r, g, b, a).endVertex();

        // Bottom face (Y = MIN_Y)
        buffer.pos(MIN_XZ, MIN_Y, MAX_XZ).tex(u1Scaled, v2Scaled).color(r * 0.6f, g * 0.6f, b * 0.6f, a).endVertex();
        buffer.pos(MIN_XZ, MIN_Y, MIN_XZ).tex(u1Scaled, v1Scaled).color(r * 0.6f, g * 0.6f, b * 0.6f, a).endVertex();
        buffer.pos(MAX_XZ, MIN_Y, MIN_XZ).tex(u2Scaled, v1Scaled).color(r * 0.6f, g * 0.6f, b * 0.6f, a).endVertex();
        buffer.pos(MAX_XZ, MIN_Y, MAX_XZ).tex(u2Scaled, v2Scaled).color(r * 0.6f, g * 0.6f, b * 0.6f, a).endVertex();

        // Calculate UV for sides based on fill level
        float vTopSide = stillSprite.getInterpolatedV(2 + 12 * fillLevel);

        // North face (Z = MIN_XZ)
        buffer.pos(MIN_XZ, MIN_Y, MIN_XZ).tex(u1Scaled, v2Scaled).color(r * 0.8f, g * 0.8f, b * 0.8f, a).endVertex();
        buffer.pos(MIN_XZ, fluidTop, MIN_XZ).tex(u1Scaled, vTopSide).color(r * 0.8f, g * 0.8f, b * 0.8f, a).endVertex();
        buffer.pos(MAX_XZ, fluidTop, MIN_XZ).tex(u2Scaled, vTopSide).color(r * 0.8f, g * 0.8f, b * 0.8f, a).endVertex();
        buffer.pos(MAX_XZ, MIN_Y, MIN_XZ).tex(u2Scaled, v2Scaled).color(r * 0.8f, g * 0.8f, b * 0.8f, a).endVertex();

        // South face (Z = MAX_XZ)
        buffer.pos(MAX_XZ, MIN_Y, MAX_XZ).tex(u2Scaled, v2Scaled).color(r * 0.8f, g * 0.8f, b * 0.8f, a).endVertex();
        buffer.pos(MAX_XZ, fluidTop, MAX_XZ).tex(u2Scaled, vTopSide).color(r * 0.8f, g * 0.8f, b * 0.8f, a).endVertex();
        buffer.pos(MIN_XZ, fluidTop, MAX_XZ).tex(u1Scaled, vTopSide).color(r * 0.8f, g * 0.8f, b * 0.8f, a).endVertex();
        buffer.pos(MIN_XZ, MIN_Y, MAX_XZ).tex(u1Scaled, v2Scaled).color(r * 0.8f, g * 0.8f, b * 0.8f, a).endVertex();

        // West face (X = MIN_XZ)
        buffer.pos(MIN_XZ, MIN_Y, MAX_XZ).tex(u1Scaled, v2Scaled).color(r * 0.7f, g * 0.7f, b * 0.7f, a).endVertex();
        buffer.pos(MIN_XZ, fluidTop, MAX_XZ).tex(u1Scaled, vTopSide).color(r * 0.7f, g * 0.7f, b * 0.7f, a).endVertex();
        buffer.pos(MIN_XZ, fluidTop, MIN_XZ).tex(u2Scaled, vTopSide).color(r * 0.7f, g * 0.7f, b * 0.7f, a).endVertex();
        buffer.pos(MIN_XZ, MIN_Y, MIN_XZ).tex(u2Scaled, v2Scaled).color(r * 0.7f, g * 0.7f, b * 0.7f, a).endVertex();

        // East face (X = MAX_XZ)
        buffer.pos(MAX_XZ, MIN_Y, MIN_XZ).tex(u2Scaled, v2Scaled).color(r * 0.7f, g * 0.7f, b * 0.7f, a).endVertex();
        buffer.pos(MAX_XZ, fluidTop, MIN_XZ).tex(u2Scaled, vTopSide).color(r * 0.7f, g * 0.7f, b * 0.7f, a).endVertex();
        buffer.pos(MAX_XZ, fluidTop, MAX_XZ).tex(u1Scaled, vTopSide).color(r * 0.7f, g * 0.7f, b * 0.7f, a).endVertex();
        buffer.pos(MAX_XZ, MIN_Y, MAX_XZ).tex(u1Scaled, v2Scaled).color(r * 0.7f, g * 0.7f, b * 0.7f, a).endVertex();

        tessellator.draw();

        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * Clear the cache for a specific position (called when tank is broken)
     */
    public static void clearCache(long posKey) {
        tankCache.remove(posKey);
    }

    private TextureAtlasSprite getFluidStillSprite(Fluid fluid) {
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
                .getAtlasSprite(fluid.getStill().toString());
        if (sprite == null) {
            sprite = Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
        }
        return sprite;
    }
}

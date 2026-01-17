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

    @Override
    public void render(TileFluidTank te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        FluidStack fluidStack = te.getTank().getFluid();

        if (fluidStack == null || fluidStack.amount <= 0 || fluidStack.getFluid() == null) {
            return;
        }

        Fluid fluid = fluidStack.getFluid();
        float fillLevel = (float) fluidStack.amount / te.getTank().getCapacity();
        fillLevel = Math.min(1.0f, Math.max(0.01f, fillLevel)); // At least 1% visible

        int color = fluid.getColor(fluidStack);
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = 0.9f;

        // Calculate fluid height based on fill level
        float fluidTop = MIN_Y + (MAX_Y - MIN_Y) * fillLevel;

        // Get fluid texture
        TextureAtlasSprite stillSprite = getFluidStillSprite(fluid);

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
        float uvScale = (MAX_XZ - MIN_XZ);
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

    private TextureAtlasSprite getFluidStillSprite(Fluid fluid) {
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
                .getAtlasSprite(fluid.getStill().toString());
        if (sprite == null) {
            sprite = Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
        }
        return sprite;
    }
}

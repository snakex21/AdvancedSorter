package com.antigravity.advancedsorter.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class RenderPathOverlay {

    private static List<BlockPos> targets = Collections.emptyList();
    private static long showUntil = 0;

    public static void setTarget(List<BlockPos> posList) {
        targets = posList;
        showUntil = System.currentTimeMillis() + 15000; // Show for 15 seconds
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        if (System.currentTimeMillis() > showUntil || targets.isEmpty())
            return;

        EntityPlayer player = Minecraft.getMinecraft().player;
        double partialTicks = event.getPartialTicks();
        double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.translate(-px, -py, -pz);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // Draw Lines
        GlStateManager.glLineWidth(5.0F);
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        Vec3d start = new Vec3d(px, py + player.getEyeHeight(), pz);

        for (BlockPos pos : targets) {
            // Draw line from player to chest
            // We can do a simple straight line
            buffer.pos(start.x, start.y - 0.5, start.z).color(0.0F, 1.0F, 0.0F, 0.5F).endVertex();
            buffer.pos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5).color(0.0F, 1.0F, 0.0F, 0.8F).endVertex();

            // Draw box around chest
            drawBox(buffer, pos, 0.0F, 1.0F, 0.0F, 0.5F);
        }

        tessellator.draw();

        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static void drawBox(BufferBuilder buffer, BlockPos pos, float r, float g, float b, float a) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        // A simple wireframe specific logic or utilize existing bounding box utility?
        // We'll trust simple lines for now, or just rely on the beam.
        // Actually, let's keep it simple: just the beam is enough for now.
    }
}

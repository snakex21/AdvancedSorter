package com.antigravity.advancedsorter.client.render;

import com.antigravity.advancedsorter.pipes.TileItemPipe;
import com.antigravity.advancedsorter.pipes.TravellingItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;

public class TileItemPipeRenderer extends TileEntitySpecialRenderer<TileItemPipe> {

    @Override
    public void render(TileItemPipe te, double x, double y, double z, float partialTicks, int destroyStage,
            float alpha) {
        if (te.getTravellingItems().isEmpty()) {
            return;
        }

        GlStateManager.pushMatrix();

        // Move to block center
        GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);

        // Smart X-ray: Offset towards camera to clear pipe wall but not other blocks
        double camDist = Math.sqrt(x * x + y * y + z * z);
        if (camDist > 0.1) {
            double dirX = -x / camDist;
            double dirY = -y / camDist;
            double dirZ = -z / camDist;
            // Offset by 0.4 blocks towards camera. Pipe wall is at 0.25.
            double offset = 0.4;
            GlStateManager.translate(dirX * offset, dirY * offset, dirZ * offset);
        }

        // Scale down to fit inside the pipe
        GlStateManager.scale(0.25, 0.25, 0.25);

        RenderHelper.enableStandardItemLighting();

        for (TravellingItem item : te.getTravellingItems()) {
            if (item.stack.isEmpty())
                continue;

            GlStateManager.pushMatrix();

            // Calculate interpolated position
            float progress = item.progress + (1.0f / te.getSpeed()) * partialTicks;
            if (progress > 1.0f)
                progress = 1.0f;

            double offsetX = 0;
            double offsetY = 0;
            double offsetZ = 0;

            EnumFacing from = item.source;
            EnumFacing to = item.direction;

            // Progress 0.0 to 0.5: coming from 'from' side to center
            // Progress 0.5 to 1.0: moving from center to 'to' side
            if (progress < 0.5f) {
                float p = (0.5f - progress) * 2.0f; // 1.0 to 0.0
                if (from != null) {
                    // Multiply by 2.0 because we are scaled by 0.25, and we want to move 0.5 blocks
                    // (0.5 / 0.25 = 2.0)
                    offsetX = from.getFrontOffsetX() * p * 2.0;
                    offsetY = from.getFrontOffsetY() * p * 2.0;
                    offsetZ = from.getFrontOffsetZ() * p * 2.0;
                }
            } else {
                float p = (progress - 0.5f) * 2.0f; // 0.0 to 1.0
                if (to != null) {
                    offsetX = to.getFrontOffsetX() * p * 2.0;
                    offsetY = to.getFrontOffsetY() * p * 2.0;
                    offsetZ = to.getFrontOffsetZ() * p * 2.0;
                }
            }

            GlStateManager.translate(offsetX, offsetY, offsetZ);

            // Rotation
            long time = Minecraft.getSystemTime();
            float rotation = (time / 20.0f) % 360.0f;
            GlStateManager.rotate(rotation, 0, 1, 0);

            Minecraft.getMinecraft().getRenderItem().renderItem(item.stack, ItemCameraTransforms.TransformType.FIXED);

            GlStateManager.popMatrix();
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }
}

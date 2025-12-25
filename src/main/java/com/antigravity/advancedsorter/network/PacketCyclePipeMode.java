package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.pipes.directional.TileDirectionalPipe;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Packet to cycle directional pipe side mode.
 * Sent from client GUI to server.
 */
public class PacketCyclePipeMode implements IMessage {

    private BlockPos pos;
    private int faceIndex;

    public PacketCyclePipeMode() {
    }

    public PacketCyclePipeMode(BlockPos pos, int faceIndex) {
        this.pos = pos;
        this.faceIndex = faceIndex;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int x = buf.readInt();
        int y = buf.readInt();
        int z = buf.readInt();
        pos = new BlockPos(x, y, z);
        faceIndex = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeInt(faceIndex);
    }

    public static class Handler implements IMessageHandler<PacketCyclePipeMode, IMessage> {
        @Override
        public IMessage onMessage(PacketCyclePipeMode message, MessageContext ctx) {
            // Handle on server thread
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                World world = ctx.getServerHandler().player.world;
                TileEntity te = world.getTileEntity(message.pos);
                if (te instanceof TileDirectionalPipe) {
                    if (message.faceIndex >= 0 && message.faceIndex < EnumFacing.VALUES.length) {
                        ((TileDirectionalPipe) te).cycleMode(EnumFacing.VALUES[message.faceIndex]);
                    }
                } else if (te instanceof com.antigravity.advancedsorter.pipes.fluid.directional.TileDirectionalFluidPipe) {
                    if (message.faceIndex >= 0 && message.faceIndex < EnumFacing.VALUES.length) {
                        ((com.antigravity.advancedsorter.pipes.fluid.directional.TileDirectionalFluidPipe) te)
                                .cycleMode(EnumFacing.VALUES[message.faceIndex]);
                    }
                }
            });
            return null;
        }
    }
}

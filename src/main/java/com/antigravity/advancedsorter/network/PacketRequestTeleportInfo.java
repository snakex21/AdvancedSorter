package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.pipes.fluid.teleport.TileTeleportFluidPipe;
import com.antigravity.advancedsorter.pipes.teleport.TileTeleportPipe;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketRequestTeleportInfo implements IMessage {
    private BlockPos pos;

    public PacketRequestTeleportInfo() {
    }

    public PacketRequestTeleportInfo(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
    }

    public static class Handler implements IMessageHandler<PacketRequestTeleportInfo, IMessage> {
        @Override
        public IMessage onMessage(PacketRequestTeleportInfo message, MessageContext ctx) {
            WorldServer world = ctx.getServerHandler().player.getServerWorld();
            world.addScheduledTask(() -> {
                TileEntity te = world.getTileEntity(message.pos);
                if (te instanceof TileTeleportPipe) {
                    ((TileTeleportPipe) te).syncConnectionInfo();
                } else if (te instanceof TileTeleportFluidPipe) {
                    ((TileTeleportFluidPipe) te).syncConnectionInfo();
                }
            });
            return null;
        }
    }
}

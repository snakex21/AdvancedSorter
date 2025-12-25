package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.pipes.teleport.TileTeleportPipe;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketUpdateTeleportPipe implements IMessage {
    private BlockPos pos;
    private int frequency;
    private TileTeleportPipe.TeleportMode mode;

    public PacketUpdateTeleportPipe() {
    }

    public PacketUpdateTeleportPipe(BlockPos pos, int frequency, TileTeleportPipe.TeleportMode mode) {
        this.pos = pos;
        this.frequency = frequency;
        this.mode = mode;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        frequency = buf.readInt();
        mode = TileTeleportPipe.TeleportMode.values()[buf.readInt()];
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeInt(frequency);
        buf.writeInt(mode.ordinal());
    }

    public static class Handler implements IMessageHandler<PacketUpdateTeleportPipe, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdateTeleportPipe message, MessageContext ctx) {
            WorldServer world = ctx.getServerHandler().player.getServerWorld();
            world.addScheduledTask(() -> {
                TileEntity te = world.getTileEntity(message.pos);
                if (te instanceof TileTeleportPipe) {
                    TileTeleportPipe pipe = (TileTeleportPipe) te;
                    pipe.setFrequency(message.frequency);
                    pipe.setMode(message.mode);
                }
            });
            return null;
        }
    }
}

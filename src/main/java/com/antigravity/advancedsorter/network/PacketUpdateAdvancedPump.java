package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.pump.TileAdvancedPump;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketUpdateAdvancedPump implements IMessage {

    private BlockPos pos;
    private int pumpRateLimit;
    private int redstoneMode;

    public PacketUpdateAdvancedPump() {
    }

    public PacketUpdateAdvancedPump(BlockPos pos, int pumpRateLimit, int redstoneMode) {
        this.pos = pos;
        this.pumpRateLimit = pumpRateLimit;
        this.redstoneMode = redstoneMode;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        pumpRateLimit = buf.readInt();
        redstoneMode = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeInt(pumpRateLimit);
        buf.writeInt(redstoneMode);
    }

    public static class Handler implements IMessageHandler<PacketUpdateAdvancedPump, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdateAdvancedPump message, MessageContext ctx) {
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                World world = ctx.getServerHandler().player.world;
                TileEntity te = world.getTileEntity(message.pos);
                if (te instanceof TileAdvancedPump) {
                    TileAdvancedPump pump = (TileAdvancedPump) te;
                    pump.setPumpRateLimit(message.pumpRateLimit);
                    pump.setRedstoneMode(message.redstoneMode);
                }
            });
            return null;
        }
    }
}

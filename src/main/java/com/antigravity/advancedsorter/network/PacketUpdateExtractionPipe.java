package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.pipes.fluid.extraction.TileExtractionFluidPipe;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Packet to update extraction pipe frequency and manual mode.
 */
public class PacketUpdateExtractionPipe implements IMessage {

    private BlockPos pos;
    private int frequency;
    private boolean manualMode;

    public PacketUpdateExtractionPipe() {
    }

    public PacketUpdateExtractionPipe(BlockPos pos, int frequency, boolean manualMode) {
        this.pos = pos;
        this.frequency = frequency;
        this.manualMode = manualMode;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        frequency = buf.readInt();
        manualMode = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeInt(frequency);
        buf.writeBoolean(manualMode);
    }

    public static class Handler implements IMessageHandler<PacketUpdateExtractionPipe, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdateExtractionPipe message, MessageContext ctx) {
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                TileEntity te = ctx.getServerHandler().player.world.getTileEntity(message.pos);
                if (te instanceof TileExtractionFluidPipe) {
                    TileExtractionFluidPipe pipe = (TileExtractionFluidPipe) te;
                    pipe.setFrequency(message.frequency);
                    pipe.setManualMode(message.manualMode);
                }
            });
            return null;
        }
    }
}

package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.machines.crusher.TileCrusher;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketCrusherMode implements IMessage {
    private BlockPos pos;

    public PacketCrusherMode() {
    }

    public PacketCrusherMode(BlockPos pos) {
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

    public static class Handler implements IMessageHandler<PacketCrusherMode, IMessage> {
        @Override
        public IMessage onMessage(PacketCrusherMode message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (player.world.isBlockLoaded(message.pos)) {
                    TileEntity te = player.world.getTileEntity(message.pos);
                    if (te instanceof TileCrusher) {
                        TileCrusher tile = (TileCrusher) te;
                        // Toggle mode: 0 -> 1, 1 -> 0
                        tile.setMode(1 - tile.getMode());
                    }
                }
            });
            return null;
        }
    }
}

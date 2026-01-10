package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.tiles.TileInventoryIndex;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketRequestItem implements IMessage {

    private BlockPos pos;
    private ItemStack requestedTemplate;
    private int amount;

    public PacketRequestItem() {
    }

    public PacketRequestItem(BlockPos pos, ItemStack template, int amount) {
        this.pos = pos;
        this.requestedTemplate = template;
        this.amount = amount;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        requestedTemplate = ByteBufUtils.readItemStack(buf);
        amount = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        ByteBufUtils.writeItemStack(buf, requestedTemplate);
        buf.writeInt(amount);
    }

    public static class Handler implements IMessageHandler<PacketRequestItem, IMessage> {
        @Override
        public IMessage onMessage(PacketRequestItem message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            World world = player.world;

            player.getServerWorld().addScheduledTask(() -> {
                if (world.isBlockLoaded(message.pos)) {
                    TileEntity tile = world.getTileEntity(message.pos);
                    if (tile instanceof TileInventoryIndex) {
                        ((TileInventoryIndex) tile).requestItem(message.requestedTemplate, message.amount);
                    }
                }
            });
            return null;
        }
    }
}

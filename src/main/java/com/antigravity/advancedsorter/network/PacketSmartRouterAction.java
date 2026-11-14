package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.machines.smartrouter.TileSmartRouter;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSmartRouterAction implements IMessage {

    public static final int ACTION_SELECT_RULE = 0;

    private BlockPos pos;
    private int action;
    private int data;

    public PacketSmartRouterAction() {
    }

    public PacketSmartRouterAction(BlockPos pos, int action, int data) {
        this.pos = pos;
        this.action = action;
        this.data = data;
    }

    public static PacketSmartRouterAction selectRule(BlockPos pos, int ruleIndex) {
        return new PacketSmartRouterAction(pos, ACTION_SELECT_RULE, ruleIndex);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        action = buf.readInt();
        data = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeInt(action);
        buf.writeInt(data);
    }

    public static class Handler implements IMessageHandler<PacketSmartRouterAction, IMessage> {
        @Override
        public IMessage onMessage(PacketSmartRouterAction message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity te = player.world.getTileEntity(message.pos);
                if (te instanceof TileSmartRouter) {
                    TileSmartRouter tile = (TileSmartRouter) te;
                    switch (message.action) {
                        case ACTION_SELECT_RULE:
                            tile.setSelectedRuleIndex(message.data);
                            break;
                    }
                }
            });
            return null;
        }
    }
}

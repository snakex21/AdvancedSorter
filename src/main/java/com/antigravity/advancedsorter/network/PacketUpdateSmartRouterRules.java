package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.machines.smartrouter.SmartRouterRule;
import com.antigravity.advancedsorter.machines.smartrouter.TileSmartRouter;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class PacketUpdateSmartRouterRules implements IMessage {
    private BlockPos pos;
    private NBTTagList rulesTag;

    public PacketUpdateSmartRouterRules() {
    }

    public PacketUpdateSmartRouterRules(BlockPos pos, List<SmartRouterRule> rules) {
        this.pos = pos;
        this.rulesTag = new NBTTagList();
        for (SmartRouterRule rule : rules) {
            this.rulesTag.appendTag(rule.serializeNBT());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        NBTTagCompound tag = ByteBufUtils.readTag(buf);
        if (tag != null && tag.hasKey("Rules")) {
            rulesTag = tag.getTagList("Rules", Constants.NBT.TAG_COMPOUND);
        } else {
            rulesTag = new NBTTagList();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Rules", rulesTag);
        ByteBufUtils.writeTag(buf, tag);
    }

    public static class Handler implements IMessageHandler<PacketUpdateSmartRouterRules, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdateSmartRouterRules message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity te = player.world.getTileEntity(message.pos);
                if (te instanceof TileSmartRouter) {
                    TileSmartRouter tile = (TileSmartRouter) te;
                    tile.rules.clear();
                    for (int i = 0; i < message.rulesTag.tagCount(); i++) {
                        SmartRouterRule rule = new SmartRouterRule();
                        rule.deserializeNBT(message.rulesTag.getCompoundTagAt(i));
                        tile.rules.add(rule);
                    }
                    te.markDirty();
                }
            });
            return null;
        }
    }
}

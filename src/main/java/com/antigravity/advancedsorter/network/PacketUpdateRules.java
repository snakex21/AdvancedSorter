package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.tiles.TileAdvancedSorter;
import com.antigravity.advancedsorter.util.RuleSerializer;
import com.antigravity.advancedsorter.util.SortRule;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;

public class PacketUpdateRules implements IMessage {
    private BlockPos pos;
    private NBTTagList rulesTag;
    private int defaultSideIndex = -1; // -1 means don't update

    public PacketUpdateRules() {
    }

    public PacketUpdateRules(BlockPos pos, List<SortRule> rules) {
        this.pos = pos;
        this.rulesTag = RuleSerializer.serializeList(rules);
        this.defaultSideIndex = -1; // Don't change default side
    }

    public PacketUpdateRules(BlockPos pos, List<SortRule> rules, EnumFacing defaultSide) {
        this.pos = pos;
        this.rulesTag = RuleSerializer.serializeList(rules);
        this.defaultSideIndex = defaultSide.getIndex();
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
        defaultSideIndex = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Rules", rulesTag);
        ByteBufUtils.writeTag(buf, tag);
        buf.writeInt(defaultSideIndex);
    }

    public static class Handler implements IMessageHandler<PacketUpdateRules, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdateRules message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity te = player.world.getTileEntity(message.pos);
                if (te instanceof TileAdvancedSorter) {
                    TileAdvancedSorter tile = (TileAdvancedSorter) te;
                    tile.rules = RuleSerializer.deserializeList(message.rulesTag);
                    if (message.defaultSideIndex >= 0 && message.defaultSideIndex < 6) {
                        tile.defaultSide = EnumFacing.getFront(message.defaultSideIndex);
                    }
                    te.markDirty();
                }
            });
            return null;
        }
    }
}

package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.tiles.TileInventoryIndex;
import com.antigravity.advancedsorter.tiles.ChestGroup;
import com.antigravity.advancedsorter.client.gui.GuiInventoryIndex;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * Syncs chest groups and individual chests from server to client.
 */
public class PacketChestGroupSync implements IMessage {

    private BlockPos tilePos;
    private List<BlockPos> individualChests;
    private List<ChestGroup> chestGroups;

    public PacketChestGroupSync() {
        this.individualChests = new ArrayList<>();
        this.chestGroups = new ArrayList<>();
    }

    public PacketChestGroupSync(TileInventoryIndex tile) {
        this.tilePos = tile.getPos();
        this.individualChests = new ArrayList<>(tile.getLinkedChests());
        this.chestGroups = new ArrayList<>(tile.getChestGroups());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        tilePos = BlockPos.fromLong(buf.readLong());

        // Read individual chests
        int chestCount = buf.readInt();
        individualChests = new ArrayList<>();
        for (int i = 0; i < chestCount; i++) {
            individualChests.add(BlockPos.fromLong(buf.readLong()));
        }

        // Read groups
        int groupCount = buf.readInt();
        chestGroups = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            NBTTagCompound groupNBT = ByteBufUtils.readTag(buf);
            chestGroups.add(ChestGroup.readFromNBT(groupNBT));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(tilePos.toLong());

        // Write individual chests
        buf.writeInt(individualChests.size());
        for (BlockPos pos : individualChests) {
            buf.writeLong(pos.toLong());
        }

        // Write groups
        buf.writeInt(chestGroups.size());
        for (ChestGroup group : chestGroups) {
            ByteBufUtils.writeTag(buf, group.writeToNBT());
        }
    }

    public BlockPos getTilePos() {
        return tilePos;
    }

    public List<BlockPos> getIndividualChests() {
        return individualChests;
    }

    public List<ChestGroup> getChestGroups() {
        return chestGroups;
    }

    public static class Handler implements IMessageHandler<PacketChestGroupSync, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketChestGroupSync message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                // Update GUI if open
                if (Minecraft.getMinecraft().currentScreen instanceof GuiInventoryIndex) {
                    GuiInventoryIndex gui = (GuiInventoryIndex) Minecraft.getMinecraft().currentScreen;
                    gui.updateChestGroups(message.getIndividualChests(), message.getChestGroups());
                }
            });
            return null;
        }
    }
}

package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.tiles.TileInventoryIndex;
import com.antigravity.advancedsorter.tiles.ChestGroup;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet for managing chest groups in TileInventoryIndex.
 * Handles creating groups, adding/removing chests from groups, etc.
 */
public class PacketManageChestGroup implements IMessage {

    // Action types
    public static final int ACTION_CREATE_GROUP = 0;
    public static final int ACTION_DELETE_GROUP = 1;
    public static final int ACTION_ADD_CHEST_TO_GROUP = 2;
    public static final int ACTION_REMOVE_CHEST_FROM_GROUP = 3;
    public static final int ACTION_RENAME_GROUP = 4;
    public static final int ACTION_REMOVE_INDIVIDUAL_CHEST = 5;
    public static final int ACTION_ADD_INDIVIDUAL_CHEST = 6;
    public static final int ACTION_CREATE_GROUP_WITH_CHESTS = 7;

    private BlockPos tilePos;
    private int action;
    private String groupId;
    private String groupName;
    private BlockPos chestPos;
    private boolean keepChests;
    private List<BlockPos> chestPositions; // For bulk operations

    public PacketManageChestGroup() {
        this.chestPositions = new ArrayList<>();
    }

    // Create group
    public static PacketManageChestGroup createGroup(BlockPos tilePos, String groupName) {
        PacketManageChestGroup packet = new PacketManageChestGroup();
        packet.tilePos = tilePos;
        packet.action = ACTION_CREATE_GROUP;
        packet.groupName = groupName;
        packet.groupId = "";
        packet.chestPos = BlockPos.ORIGIN;
        packet.keepChests = false;
        return packet;
    }

    // Create group with chests in one operation
    public static PacketManageChestGroup createGroupWithChests(BlockPos tilePos, String groupName, List<BlockPos> chests) {
        PacketManageChestGroup packet = new PacketManageChestGroup();
        packet.tilePos = tilePos;
        packet.action = ACTION_CREATE_GROUP_WITH_CHESTS;
        packet.groupName = groupName;
        packet.groupId = "";
        packet.chestPos = BlockPos.ORIGIN;
        packet.keepChests = false;
        packet.chestPositions = new ArrayList<>(chests);
        return packet;
    }

    // Delete group
    public static PacketManageChestGroup deleteGroup(BlockPos tilePos, String groupId, boolean keepChests) {
        PacketManageChestGroup packet = new PacketManageChestGroup();
        packet.tilePos = tilePos;
        packet.action = ACTION_DELETE_GROUP;
        packet.groupId = groupId;
        packet.groupName = "";
        packet.chestPos = BlockPos.ORIGIN;
        packet.keepChests = keepChests;
        return packet;
    }

    // Add chest to group
    public static PacketManageChestGroup addChestToGroup(BlockPos tilePos, BlockPos chestPos, String groupId) {
        PacketManageChestGroup packet = new PacketManageChestGroup();
        packet.tilePos = tilePos;
        packet.action = ACTION_ADD_CHEST_TO_GROUP;
        packet.groupId = groupId;
        packet.groupName = "";
        packet.chestPos = chestPos;
        packet.keepChests = false;
        return packet;
    }

    // Remove chest from group
    public static PacketManageChestGroup removeChestFromGroup(BlockPos tilePos, BlockPos chestPos, boolean addToIndividual) {
        PacketManageChestGroup packet = new PacketManageChestGroup();
        packet.tilePos = tilePos;
        packet.action = ACTION_REMOVE_CHEST_FROM_GROUP;
        packet.groupId = "";
        packet.groupName = "";
        packet.chestPos = chestPos;
        packet.keepChests = addToIndividual;
        return packet;
    }

    // Rename group
    public static PacketManageChestGroup renameGroup(BlockPos tilePos, String groupId, String newName) {
        PacketManageChestGroup packet = new PacketManageChestGroup();
        packet.tilePos = tilePos;
        packet.action = ACTION_RENAME_GROUP;
        packet.groupId = groupId;
        packet.groupName = newName;
        packet.chestPos = BlockPos.ORIGIN;
        packet.keepChests = false;
        return packet;
    }

    // Remove individual chest
    public static PacketManageChestGroup removeIndividualChest(BlockPos tilePos, BlockPos chestPos) {
        PacketManageChestGroup packet = new PacketManageChestGroup();
        packet.tilePos = tilePos;
        packet.action = ACTION_REMOVE_INDIVIDUAL_CHEST;
        packet.groupId = "";
        packet.groupName = "";
        packet.chestPos = chestPos;
        packet.keepChests = false;
        return packet;
    }

    // Add individual chest
    public static PacketManageChestGroup addIndividualChest(BlockPos tilePos, BlockPos chestPos) {
        PacketManageChestGroup packet = new PacketManageChestGroup();
        packet.tilePos = tilePos;
        packet.action = ACTION_ADD_INDIVIDUAL_CHEST;
        packet.groupId = "";
        packet.groupName = "";
        packet.chestPos = chestPos;
        packet.keepChests = false;
        return packet;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        tilePos = BlockPos.fromLong(buf.readLong());
        action = buf.readInt();
        groupId = ByteBufUtils.readUTF8String(buf);
        groupName = ByteBufUtils.readUTF8String(buf);
        chestPos = BlockPos.fromLong(buf.readLong());
        keepChests = buf.readBoolean();

        // Read chest positions list
        int count = buf.readInt();
        chestPositions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            chestPositions.add(BlockPos.fromLong(buf.readLong()));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(tilePos.toLong());
        buf.writeInt(action);
        ByteBufUtils.writeUTF8String(buf, groupId != null ? groupId : "");
        ByteBufUtils.writeUTF8String(buf, groupName != null ? groupName : "");
        buf.writeLong(chestPos != null ? chestPos.toLong() : 0);
        buf.writeBoolean(keepChests);

        // Write chest positions list
        buf.writeInt(chestPositions.size());
        for (BlockPos pos : chestPositions) {
            buf.writeLong(pos.toLong());
        }
    }

    public static class Handler implements IMessageHandler<PacketManageChestGroup, IMessage> {
        @Override
        public IMessage onMessage(PacketManageChestGroup message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            World world = player.world;

            player.getServerWorld().addScheduledTask(() -> {
                if (world.isBlockLoaded(message.tilePos)) {
                    TileEntity tile = world.getTileEntity(message.tilePos);
                    if (tile instanceof TileInventoryIndex) {
                        TileInventoryIndex indexTile = (TileInventoryIndex) tile;

                        switch (message.action) {
                            case ACTION_CREATE_GROUP:
                                indexTile.createGroup(message.groupName);
                                break;

                            case ACTION_CREATE_GROUP_WITH_CHESTS:
                                ChestGroup newGroup = indexTile.createGroup(message.groupName);
                                for (BlockPos chestPos : message.chestPositions) {
                                    indexTile.addChestToGroup(chestPos, newGroup.getGroupId());
                                }
                                break;

                            case ACTION_DELETE_GROUP:
                                indexTile.removeGroup(message.groupId, message.keepChests);
                                break;

                            case ACTION_ADD_CHEST_TO_GROUP:
                                indexTile.addChestToGroup(message.chestPos, message.groupId);
                                break;

                            case ACTION_REMOVE_CHEST_FROM_GROUP:
                                indexTile.removeChestFromGroup(message.chestPos, message.keepChests);
                                break;

                            case ACTION_RENAME_GROUP:
                                ChestGroup group = indexTile.getGroupById(message.groupId);
                                if (group != null) {
                                    group.setGroupName(message.groupName);
                                    indexTile.markDirty();
                                }
                                break;

                            case ACTION_REMOVE_INDIVIDUAL_CHEST:
                                indexTile.removeLinkedChest(message.chestPos);
                                break;

                            case ACTION_ADD_INDIVIDUAL_CHEST:
                                indexTile.addLinkedChest(message.chestPos);
                                break;
                        }

                        // Sync to client
                        PacketHandler.INSTANCE.sendTo(new PacketChestGroupSync(indexTile), player);
                    }
                }
            });
            return null;
        }
    }
}

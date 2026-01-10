package com.antigravity.advancedsorter.tiles;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a group of chests that are treated as one logical storage unit.
 * Multiple chests in a group are displayed together in the index.
 */
public class ChestGroup {

    private String groupId;
    private String groupName;
    private List<BlockPos> chestPositions;
    private int colorIndex; // For visual differentiation in GUI

    public ChestGroup() {
        this.groupId = UUID.randomUUID().toString().substring(0, 8);
        this.groupName = "Group " + groupId.substring(0, 4);
        this.chestPositions = new ArrayList<>();
        this.colorIndex = 0;
    }

    public ChestGroup(String name) {
        this.groupId = UUID.randomUUID().toString().substring(0, 8);
        this.groupName = name;
        this.chestPositions = new ArrayList<>();
        this.colorIndex = 0;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String name) {
        this.groupName = name;
    }

    public List<BlockPos> getChestPositions() {
        return chestPositions;
    }

    public void addChest(BlockPos pos) {
        if (!chestPositions.contains(pos)) {
            chestPositions.add(pos);
        }
    }

    public void removeChest(BlockPos pos) {
        chestPositions.remove(pos);
    }

    public boolean containsChest(BlockPos pos) {
        return chestPositions.contains(pos);
    }

    public int getColorIndex() {
        return colorIndex;
    }

    public void setColorIndex(int colorIndex) {
        this.colorIndex = colorIndex % 16; // Limit to 16 colors
    }

    public boolean isEmpty() {
        return chestPositions.isEmpty();
    }

    public int size() {
        return chestPositions.size();
    }

    // NBT Serialization
    public NBTTagCompound writeToNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setString("GroupId", groupId);
        compound.setString("GroupName", groupName);
        compound.setInteger("ColorIndex", colorIndex);

        NBTTagList posList = new NBTTagList();
        for (BlockPos pos : chestPositions) {
            posList.appendTag(NBTUtil.createPosTag(pos));
        }
        compound.setTag("ChestPositions", posList);

        return compound;
    }

    public static ChestGroup readFromNBT(NBTTagCompound compound) {
        ChestGroup group = new ChestGroup();
        group.groupId = compound.getString("GroupId");
        group.groupName = compound.getString("GroupName");
        group.colorIndex = compound.getInteger("ColorIndex");

        group.chestPositions.clear();
        NBTTagList posList = compound.getTagList("ChestPositions", 10);
        for (int i = 0; i < posList.tagCount(); i++) {
            group.chestPositions.add(NBTUtil.getPosFromTag(posList.getCompoundTagAt(i)));
        }

        return group;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ChestGroup) {
            return this.groupId.equals(((ChestGroup) obj).groupId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return groupId.hashCode();
    }
}

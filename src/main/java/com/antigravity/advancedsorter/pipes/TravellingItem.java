package com.antigravity.advancedsorter.pipes;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

/**
 * Represents an item travelling through a pipe.
 * Tracks position, direction, and source to prevent backtracking.
 */
public class TravellingItem {

    public ItemStack stack;
    public float progress; // 0.0 = entered pipe, 1.0 = exiting pipe
    public EnumFacing direction; // Where the item is heading
    public EnumFacing source; // Where the item came from (to prevent backtracking)
    public int ticksInPipe; // How long in current pipe segment
    public boolean teleported; // Flag to prevent teleport loops

    public TravellingItem(ItemStack stack, EnumFacing source) {
        this.stack = stack.copy();
        this.progress = 0.0f;
        this.source = source;
        this.direction = null; // Will be calculated by pipe
        this.ticksInPipe = 0;
        this.teleported = false;
    }

    public TravellingItem(NBTTagCompound tag) {
        this.stack = new ItemStack(tag.getCompoundTag("Stack"));
        this.progress = tag.getFloat("Progress");
        this.ticksInPipe = tag.getInteger("TicksInPipe");

        if (tag.hasKey("Direction")) {
            this.direction = EnumFacing.getFront(tag.getInteger("Direction"));
        }
        if (tag.hasKey("Source")) {
            this.source = EnumFacing.getFront(tag.getInteger("Source"));
        }
        this.teleported = tag.getBoolean("Teleported");
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Stack", stack.serializeNBT());
        tag.setFloat("Progress", progress);
        tag.setInteger("TicksInPipe", ticksInPipe);

        if (direction != null) {
            tag.setInteger("Direction", direction.getIndex());
        }
        if (source != null) {
            tag.setInteger("Source", source.getIndex());
        }
        tag.setBoolean("Teleported", teleported);

        return tag;
    }

    /**
     * Update item position based on speed.
     * 
     * @param speed Ticks per block (lower = faster)
     * @return true if item reached end of pipe (progress >= 1.0)
     */
    public boolean update(float speed) {
        ticksInPipe++;
        progress += 1.0f / speed;
        return progress >= 1.0f;
    }

    /**
     * Check if item is at center of pipe (for rendering/direction change).
     */
    public boolean isAtCenter() {
        return progress >= 0.5f && progress < 0.5f + (1.0f / 20.0f);
    }
}

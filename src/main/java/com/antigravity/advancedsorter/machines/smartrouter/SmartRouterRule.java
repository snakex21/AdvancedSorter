package com.antigravity.advancedsorter.machines.smartrouter;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class SmartRouterRule {
    public String name = "";
    public ItemStack filterStack = ItemStack.EMPTY;
    public int frequency = 0;

    public SmartRouterRule() {
    }

    public SmartRouterRule(String name, ItemStack stack, int freq) {
        this.name = name;
        this.filterStack = stack.copy();
        if (!this.filterStack.isEmpty()) {
            this.filterStack.setCount(1);
        }
        this.frequency = freq;
    }

    public boolean matches(ItemStack stack) {
        // If filter is empty, it matches everything (optional filter)
        if (filterStack.isEmpty()) return true;
        return stack.getItem() == filterStack.getItem() &&
               (filterStack.getMetadata() == 32767 || stack.getMetadata() == filterStack.getMetadata());
    }

    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Name", name);
        if (!filterStack.isEmpty()) {
            tag.setTag("Filter", filterStack.serializeNBT());
        }
        tag.setInteger("Frequency", frequency);
        return tag;
    }

    public void deserializeNBT(NBTTagCompound tag) {
        name = tag.getString("Name");
        if (tag.hasKey("Filter")) {
            filterStack = new ItemStack(tag.getCompoundTag("Filter"));
        } else {
            filterStack = ItemStack.EMPTY;
        }
        frequency = tag.getInteger("Frequency");
    }
}

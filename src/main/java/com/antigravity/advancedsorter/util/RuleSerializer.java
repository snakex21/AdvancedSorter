package com.antigravity.advancedsorter.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class RuleSerializer {

    public static NBTTagCompound serialize(SortRule rule) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Type", rule.type.ordinal());
        tag.setString("Value", rule.matchValue);
        tag.setInteger("Output", rule.outputFace.getIndex());
        tag.setBoolean("Enabled", rule.enabled);

        NBTTagList exactList = new NBTTagList();
        for (ItemStack stack : rule.exactItems) {
            exactList.appendTag(stack.serializeNBT());
        }
        tag.setTag("ExactItems", exactList);

        NBTTagList exceptionList = new NBTTagList();
        for (ItemStack stack : rule.exceptionItems) {
            exceptionList.appendTag(stack.serializeNBT());
        }
        tag.setTag("ExceptionItems", exceptionList);

        // Save distribution override
        tag.setInteger("DistOverride", rule.distributionOverride.ordinal());

        return tag;
    }

    public static SortRule deserialize(NBTTagCompound tag) {
        SortRule rule = new SortRule();
        if (tag.hasKey("Type"))
            rule.type = SortRule.MatchType.values()[tag.getInteger("Type")];
        if (tag.hasKey("Value"))
            rule.matchValue = tag.getString("Value");
        if (tag.hasKey("Output"))
            rule.outputFace = EnumFacing.getFront(tag.getInteger("Output"));
        if (tag.hasKey("Enabled"))
            rule.enabled = tag.getBoolean("Enabled");

        if (tag.hasKey("ExactItems")) {
            NBTTagList list = tag.getTagList("ExactItems", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                rule.exactItems.add(new ItemStack(list.getCompoundTagAt(i)));
            }
        }

        if (tag.hasKey("ExceptionItems")) {
            NBTTagList list = tag.getTagList("ExceptionItems", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                rule.exceptionItems.add(new ItemStack(list.getCompoundTagAt(i)));
            }
        }

        // Load distribution override
        if (tag.hasKey("DistOverride")) {
            int override = tag.getInteger("DistOverride");
            SortRule.DistributionOverride[] values = SortRule.DistributionOverride.values();
            rule.distributionOverride = values[override % values.length];
        }

        return rule;
    }

    public static NBTTagList serializeList(List<SortRule> rules) {
        NBTTagList list = new NBTTagList();
        for (SortRule rule : rules) {
            list.appendTag(serialize(rule));
        }
        return list;
    }

    public static List<SortRule> deserializeList(NBTTagList list) {
        List<SortRule> rules = new ArrayList<>();
        for (int i = 0; i < list.tagCount(); i++) {
            rules.add(deserialize(list.getCompoundTagAt(i)));
        }
        return rules;
    }
}

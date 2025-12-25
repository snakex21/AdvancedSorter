package com.antigravity.advancedsorter.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

public class SortRule {
    public enum MatchType {
        MOD_ID,
        NAME_CONTAINS,
        ORE_DICT,
        EXACT
    }

    /**
     * Distribution mode for the sorter.
     * FIRST_MATCH: Items go to the first matching rule (default).
     * ROUND_ROBIN: Items are distributed cyclically between matching rules.
     */
    public enum DistributionMode {
        FIRST_MATCH,
        ROUND_ROBIN
    }

    /**
     * Per-rule distribution override.
     * USE_GLOBAL: Use the sorter's global distribution mode (default).
     * FORCE_FIRST_MATCH: Always use first match for this rule, ignoring global.
     * FORCE_ROUND_ROBIN: Always use round robin for this rule, ignoring global.
     */
    public enum DistributionOverride {
        USE_GLOBAL,
        FORCE_FIRST_MATCH,
        FORCE_ROUND_ROBIN
    }

    public MatchType type = MatchType.EXACT;
    public String matchValue = "";
    public List<ItemStack> exactItems = new ArrayList<>();
    public List<ItemStack> exceptionItems = new ArrayList<>();
    public EnumFacing outputFace = EnumFacing.DOWN;
    public boolean enabled = true;
    public DistributionOverride distributionOverride = DistributionOverride.USE_GLOBAL;

    public boolean matches(ItemStack stack) {
        if (!enabled || stack.isEmpty())
            return false;

        // Check exceptions first
        for (ItemStack exception : exceptionItems) {
            if (isSameItem(stack, exception))
                return false;
        }

        switch (type) {
            case MOD_ID:
                String modId = stack.getItem().getRegistryName().getResourceDomain();
                return modId.equalsIgnoreCase(matchValue);
            case NAME_CONTAINS:
                return stack.getDisplayName().toLowerCase().contains(matchValue.toLowerCase());
            case ORE_DICT:
                int[] ids = OreDictionary.getOreIDs(stack);
                for (int id : ids) {
                    String oreName = OreDictionary.getOreName(id);
                    if (oreName.startsWith(matchValue))
                        return true;
                }
                return false;
            case EXACT:
                for (ItemStack exact : exactItems) {
                    if (isSameItem(stack, exact))
                        return true;
                }
                return false;
            default:
                return false;
        }
    }

    private boolean isSameItem(ItemStack a, ItemStack b) {
        // Ignore NBT and Damage for matching, unless it's strictly required?
        // User said: "Ignoruj durability i NBT podczas dopasowania: dopasowanie typu
        // item = stack.getItem()"
        return a.getItem() == b.getItem();
    }
}

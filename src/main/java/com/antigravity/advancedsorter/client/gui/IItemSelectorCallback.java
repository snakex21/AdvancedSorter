package com.antigravity.advancedsorter.client.gui;

import net.minecraft.item.ItemStack;

public interface IItemSelectorCallback {
    void onSelectionConfirmed(ItemStack stack);
}

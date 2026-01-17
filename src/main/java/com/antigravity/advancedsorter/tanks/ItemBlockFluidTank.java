package com.antigravity.advancedsorter.tanks;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class ItemBlockFluidTank extends ItemBlock {

    public ItemBlockFluidTank(Block block) {
        super(block);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        TankTier tier = TankTier.fromMeta(stack.getMetadata());
        return "tile.advancedsorter.fluid_tank_" + tier.getName();
    }
}

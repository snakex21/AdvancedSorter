package com.antigravity.advancedsorter.pipes;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.pipes.fluid.TileFluidPipe;
import com.antigravity.advancedsorter.pipes.gas.teleport.TileTeleportGasPipe;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Wrench tool for configuring pipe connections.
 * Right-click on pipe face to toggle connection.
 */
public class ItemWrench extends Item {

    public ItemWrench() {
        setUnlocalizedName("item_wrench");
        setRegistryName(AdvancedSorterMod.MODID, "item_wrench");
        setMaxStackSize(1);
        setCreativeTab(AdvancedSorterMod.CREATIVE_TAB);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileItemPipe) {
                ((TileItemPipe) te).toggleConnection(facing);
                return EnumActionResult.SUCCESS;
            } else if (te instanceof TileFluidPipe) {
                ((TileFluidPipe) te).toggleConnection(facing);
                return EnumActionResult.SUCCESS;
            } else if (te instanceof TileTeleportGasPipe) {
                ((TileTeleportGasPipe) te).toggleConnection(facing);
                return EnumActionResult.SUCCESS;
            }
        }
        return EnumActionResult.PASS;
    }
}

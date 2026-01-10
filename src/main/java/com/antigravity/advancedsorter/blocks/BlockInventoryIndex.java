package com.antigravity.advancedsorter.blocks;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.tiles.TileInventoryIndex;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockInventoryIndex extends Block implements ITileEntityProvider {

    public BlockInventoryIndex() {
        super(Material.IRON);
        setUnlocalizedName("inventory_index");
        setRegistryName("inventory_index");
        setCreativeTab(AdvancedSorterMod.CREATIVE_TAB);
        setHardness(3.0f);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            playerIn.openGui(AdvancedSorterMod.instance, AdvancedSorterMod.GUI_INVENTORY_INDEX, worldIn, pos.getX(),
                    pos.getY(), pos.getZ());
        }
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileInventoryIndex();
    }
}

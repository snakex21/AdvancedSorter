package com.antigravity.advancedsorter.pipes.fluid.directional;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.pipes.fluid.BlockFluidPipe;
import com.antigravity.advancedsorter.pipes.fluid.FluidPipeTier;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Directional Fluid Pipe block - controls fluid flow direction and filtering.
 */
public class BlockDirectionalFluidPipe extends BlockFluidPipe {

    public BlockDirectionalFluidPipe() {
        super("directional_fluid_pipe", FluidPipeTier.DIAMOND);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileDirectionalFluidPipe();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (playerIn.isSneaking())
            return false;

        // Check for wrench first
        if (super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ)) {
            return true;
        }

        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileDirectionalFluidPipe) {
                // Open GUI
                playerIn.openGui(AdvancedSorterMod.instance, 3, worldIn, pos.getX(), pos.getY(), pos.getZ());
                return true;
            }
        }
        return true;
    }
}

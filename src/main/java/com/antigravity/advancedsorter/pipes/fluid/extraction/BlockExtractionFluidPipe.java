package com.antigravity.advancedsorter.pipes.fluid.extraction;

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
 * Extraction fluid pipe block that actively pulls fluids from adjacent tanks.
 */
public class BlockExtractionFluidPipe extends BlockFluidPipe {

    public BlockExtractionFluidPipe() {
        super("extraction_fluid_pipe", FluidPipeTier.IRON);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileExtractionFluidPipe();
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
            if (te instanceof TileExtractionFluidPipe) {
                playerIn.openGui(AdvancedSorterMod.instance, AdvancedSorterMod.GUI_EXTRACTION_FLUID_PIPE,
                        worldIn, pos.getX(), pos.getY(), pos.getZ());
                return true;
            }
        }
        return true;
    }
}

package com.antigravity.advancedsorter.pipes.teleport;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.pipes.BlockItemPipe;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockTeleportPipe extends BlockItemPipe {

    public BlockTeleportPipe() {
        super("teleport_pipe");
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileTeleportPipe();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        // Open GUI if not sneaking and not holding a wrench
        if (!playerIn.isSneaking()) {
            if (!worldIn.isRemote) {
                playerIn.openGui(AdvancedSorterMod.instance, 2, worldIn, pos.getX(), pos.getY(), pos.getZ());
            }
            return true;
        }
        return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
    }
}

package com.antigravity.advancedsorter.pipes.transport;

import com.antigravity.advancedsorter.pipes.BlockItemPipe;
import com.antigravity.advancedsorter.pipes.PipeTier;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Stone tier transport pipe block.
 */
public class BlockStonePipe extends BlockItemPipe {

    public BlockStonePipe() {
        super("stone_pipe");
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileStonePipe();
    }

    @Override
    public PipeTier getTier() {
        return PipeTier.STONE;
    }
}

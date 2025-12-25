package com.antigravity.advancedsorter.pipes.transport;

import com.antigravity.advancedsorter.pipes.BlockItemPipe;
import com.antigravity.advancedsorter.pipes.PipeTier;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Diamond tier transport pipe block.
 */
public class BlockDiamondPipe extends BlockItemPipe {

    public BlockDiamondPipe() {
        super("diamond_pipe");
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileDiamondPipe();
    }

    @Override
    public PipeTier getTier() {
        return PipeTier.DIAMOND;
    }
}

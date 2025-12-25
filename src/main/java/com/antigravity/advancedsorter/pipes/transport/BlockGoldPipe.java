package com.antigravity.advancedsorter.pipes.transport;

import com.antigravity.advancedsorter.pipes.BlockItemPipe;
import com.antigravity.advancedsorter.pipes.PipeTier;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Gold tier transport pipe block.
 */
public class BlockGoldPipe extends BlockItemPipe {

    public BlockGoldPipe() {
        super("gold_pipe");
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileGoldPipe();
    }

    @Override
    public PipeTier getTier() {
        return PipeTier.GOLD;
    }
}

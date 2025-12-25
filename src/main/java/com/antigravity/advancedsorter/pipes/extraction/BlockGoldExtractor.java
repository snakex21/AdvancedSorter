package com.antigravity.advancedsorter.pipes.extraction;

import com.antigravity.advancedsorter.pipes.BlockExtractionPipe;
import com.antigravity.advancedsorter.pipes.PipeTier;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Gold tier extraction pipe block - extracts 32 items per tick
 */
public class BlockGoldExtractor extends BlockExtractionPipe {

    public BlockGoldExtractor() {
        super("gold_extractor");
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileGoldExtractor();
    }

    @Override
    public PipeTier getTier() {
        return PipeTier.GOLD;
    }
}

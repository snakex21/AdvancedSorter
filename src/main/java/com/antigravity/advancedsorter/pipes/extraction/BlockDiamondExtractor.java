package com.antigravity.advancedsorter.pipes.extraction;

import com.antigravity.advancedsorter.pipes.BlockExtractionPipe;
import com.antigravity.advancedsorter.pipes.PipeTier;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Diamond tier extraction pipe block - extracts 64 items (full stack) per tick
 */
public class BlockDiamondExtractor extends BlockExtractionPipe {

    public BlockDiamondExtractor() {
        super("diamond_extractor");
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileDiamondExtractor();
    }

    @Override
    public PipeTier getTier() {
        return PipeTier.DIAMOND;
    }
}

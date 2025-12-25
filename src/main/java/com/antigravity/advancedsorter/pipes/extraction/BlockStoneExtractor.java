package com.antigravity.advancedsorter.pipes.extraction;

import com.antigravity.advancedsorter.pipes.BlockExtractionPipe;
import com.antigravity.advancedsorter.pipes.PipeTier;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Stone tier extraction pipe block - extracts 1 item per tick
 */
public class BlockStoneExtractor extends BlockExtractionPipe {

    public BlockStoneExtractor() {
        super("stone_extractor");
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileStoneExtractor();
    }

    @Override
    public PipeTier getTier() {
        return PipeTier.STONE;
    }
}

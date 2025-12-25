package com.antigravity.advancedsorter.pipes;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Extraction pipe block - pulls items from adjacent inventories automatically.
 * Visual difference from regular pipe (gold/red ring).
 */
public class BlockExtractionPipe extends BlockItemPipe {

    public BlockExtractionPipe() {
        super("iron_extractor");
    }

    protected BlockExtractionPipe(String name) {
        super(name);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileExtractionPipe();
    }
}

package com.antigravity.advancedsorter.pipes.extraction;

import com.antigravity.advancedsorter.pipes.PipeTier;
import com.antigravity.advancedsorter.pipes.TileExtractionPipe;

/**
 * Diamond tier extraction pipe - extracts 64 items (full stack) per tick
 */
public class TileDiamondExtractor extends TileExtractionPipe {
    public TileDiamondExtractor() {
        super(PipeTier.DIAMOND);
    }
}

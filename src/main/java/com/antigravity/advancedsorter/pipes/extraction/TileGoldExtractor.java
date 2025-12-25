package com.antigravity.advancedsorter.pipes.extraction;

import com.antigravity.advancedsorter.pipes.PipeTier;
import com.antigravity.advancedsorter.pipes.TileExtractionPipe;

/**
 * Gold tier extraction pipe - extracts 32 items per tick
 */
public class TileGoldExtractor extends TileExtractionPipe {
    public TileGoldExtractor() {
        super(PipeTier.GOLD);
    }
}

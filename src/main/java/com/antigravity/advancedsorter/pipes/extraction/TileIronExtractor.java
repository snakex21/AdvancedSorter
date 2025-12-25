package com.antigravity.advancedsorter.pipes.extraction;

import com.antigravity.advancedsorter.pipes.PipeTier;
import com.antigravity.advancedsorter.pipes.TileExtractionPipe;

/**
 * Iron tier extraction pipe - extracts 8 items per tick
 */
public class TileIronExtractor extends TileExtractionPipe {
    public TileIronExtractor() {
        super(PipeTier.IRON);
    }
}

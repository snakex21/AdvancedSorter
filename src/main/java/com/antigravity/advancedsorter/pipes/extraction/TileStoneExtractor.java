package com.antigravity.advancedsorter.pipes.extraction;

import com.antigravity.advancedsorter.pipes.PipeTier;
import com.antigravity.advancedsorter.pipes.TileExtractionPipe;

/**
 * Stone tier extraction pipe - extracts 1 item per tick
 */
public class TileStoneExtractor extends TileExtractionPipe {
    public TileStoneExtractor() {
        super(PipeTier.STONE);
    }
}

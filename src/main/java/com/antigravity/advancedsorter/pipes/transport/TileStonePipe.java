package com.antigravity.advancedsorter.pipes.transport;

import com.antigravity.advancedsorter.pipes.PipeTier;
import com.antigravity.advancedsorter.pipes.TileItemPipe;

/**
 * Stone tier transport pipe - slowest.
 */
public class TileStonePipe extends TileItemPipe {
    public TileStonePipe() {
        super(PipeTier.STONE);
    }
}

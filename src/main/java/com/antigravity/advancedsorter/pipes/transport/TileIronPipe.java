package com.antigravity.advancedsorter.pipes.transport;

import com.antigravity.advancedsorter.pipes.PipeTier;
import com.antigravity.advancedsorter.pipes.TileItemPipe;

/**
 * Iron tier transport pipe - medium speed.
 */
public class TileIronPipe extends TileItemPipe {
    public TileIronPipe() {
        super(PipeTier.IRON);
    }
}

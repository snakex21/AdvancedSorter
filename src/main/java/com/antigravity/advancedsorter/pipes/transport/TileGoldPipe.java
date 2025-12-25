package com.antigravity.advancedsorter.pipes.transport;

import com.antigravity.advancedsorter.pipes.PipeTier;
import com.antigravity.advancedsorter.pipes.TileItemPipe;

/**
 * Gold tier transport pipe - fast.
 */
public class TileGoldPipe extends TileItemPipe {
    public TileGoldPipe() {
        super(PipeTier.GOLD);
    }
}

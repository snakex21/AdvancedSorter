package com.antigravity.advancedsorter.pipes;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * Extraction pipe - pulls items from adjacent inventories.
 * No engine needed - automatically extracts on a timer.
 * Extraction amount is determined by tier.
 */
public class TileExtractionPipe extends TileItemPipe {

    private int extractionCooldown = 0;
    private int extractionRate = 20; // Ticks between extraction attempts

    public TileExtractionPipe() {
        super(PipeTier.IRON); // Default to iron tier
    }

    public TileExtractionPipe(PipeTier tier) {
        super(tier);
    }

    @Override
    public void update() {
        super.update();

        if (world.isRemote)
            return;

        // Extraction logic
        extractionCooldown--;
        if (extractionCooldown <= 0) {
            extractionCooldown = extractionRate;
            tryExtract();
        }
    }

    /**
     * Try to extract items from all connected inventories.
     * Amount extracted depends on tier.
     */
    private void tryExtract() {
        int maxExtract = tier.getExtractAmount(); // Get amount from tier (1/8/32/64)

        for (EnumFacing face : connections) {
            TileEntity neighbor = world.getTileEntity(pos.offset(face));

            // Skip other pipes
            if (neighbor instanceof TileItemPipe)
                continue;

            if (neighbor == null)
                continue;

            // Get item handler
            IItemHandler handler = null;
            EnumFacing into = face.getOpposite();

            if (neighbor.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, into)) {
                handler = neighbor.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, into);
            } else if (neighbor.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                handler = neighbor.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            }

            if (handler != null) {
                // Try to extract from any slot
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack inSlot = handler.getStackInSlot(slot);
                    if (!inSlot.isEmpty()) {
                        // Extract up to maxExtract items (tier-based)
                        ItemStack extracted = handler.extractItem(slot, maxExtract, false);
                        if (!extracted.isEmpty()) {
                            // Send into pipe network (full stack travels together)
                            receiveItem(extracted, face);
                            return; // One extraction per cycle
                        }
                    }
                }
            }
        }
    }

    public void setExtractionRate(int rate) {
        this.extractionRate = Math.max(1, rate);
    }

    public int getExtractionRate() {
        return extractionRate;
    }
}

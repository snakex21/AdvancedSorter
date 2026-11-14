package com.antigravity.advancedsorter.pipes;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Extraction pipe - pulls items from adjacent inventories.
 * No engine needed - automatically extracts on a timer.
 * Extraction amount is determined by tier.
 */
public class TileExtractionPipe extends TileItemPipe {

    private int extractionCooldown = 0;
    private int extractionRate = 20; // Ticks between extraction attempts

    // OPTYMALIZACJA: Cache IItemHandler dla ekstrakcji
    private final Map<EnumFacing, IItemHandler> extractionHandlerCache = new EnumMap<>(EnumFacing.class);
    // OPTYMALIZACJA: Cache listy ścianek do iteracji (unikamy alokacji ArrayList w update)
    private final List<EnumFacing> cachedExtractionFaces = new java.util.ArrayList<>();
    private boolean extractionCacheDirty = true;

    // OPTYMALIZACJA: Round-Robin dla slotów i ścianek (sprawiedliwe i szybsze opróżnianie)
    private final Map<EnumFacing, Integer> slotCursors = new EnumMap<>(EnumFacing.class);
    private int faceCursor = 0;

    public TileExtractionPipe() {
        super(PipeTier.IRON); // Default to iron tier
    }

    public TileExtractionPipe(PipeTier tier) {
        super(tier);
    }

    @Override
    public void updateConnections() {
        super.updateConnections();
        // Invalidate extraction cache when connections change
        extractionCacheDirty = true;
    }

    private void updateExtractionCache() {
        if (!extractionCacheDirty) return;
        extractionCacheDirty = false;
        extractionHandlerCache.clear();
        cachedExtractionFaces.clear();

        for (EnumFacing face : connections) {
            TileEntity neighbor = world.getTileEntity(pos.offset(face));

            // Skip other pipes
            if (neighbor instanceof TileItemPipe)
                continue;

            if (neighbor == null)
                continue;

            // Get item handler and cache it
            EnumFacing into = face.getOpposite();
            IItemHandler handler = null;

            if (neighbor.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, into)) {
                handler = neighbor.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, into);
            } else if (neighbor.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                handler = neighbor.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            }

            if (handler != null) {
                extractionHandlerCache.put(face, handler);
                cachedExtractionFaces.add(face);
            }
        }
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
        // OPTYMALIZACJA: Aktualizuj cache jeśli potrzeba
        updateExtractionCache();

        if (extractionHandlerCache.isEmpty()) return;

        int maxExtract = tier.getExtractAmount(); // Get amount from tier (1/8/32/64)

        // Konwersja na listę dla dostępu po indeksie (Round-Robin ścianek)
        // Cache this list if iteration is frequent, but extractionRate is usually 20 ticks, so it's fine.
        List<EnumFacing> faces = cachedExtractionFaces;
        if (faces.isEmpty()) return;

        int startFace = faceCursor % faces.size();
        int facesChecked = 0;

        while (facesChecked < faces.size()) {
            int idx = (startFace + facesChecked) % faces.size();
            EnumFacing face = faces.get(idx);
            IItemHandler handler = extractionHandlerCache.get(face);

            if (handler != null && extractFromHandler(handler, face, maxExtract)) {
                // Sukces - przesuń kursor na następną ściankę dla następnego cyklu
                faceCursor = (idx + 1) % faces.size();
                return; // One extraction per cycle
            }
            facesChecked++;
        }
        // Jeśli przeszliśmy wszystko i nic, kursor zostaje tam gdzie skończyliśmy (lub +1)
        faceCursor = (startFace + 1) % faces.size();
    }

    private boolean extractFromHandler(IItemHandler handler, EnumFacing face, int maxExtract) {
        int slots = handler.getSlots();
        if (slots == 0) return false;

        // Round-Robin dla slotów
        int startSlot = slotCursors.getOrDefault(face, 0);
        int slotsChecked = 0;

        while (slotsChecked < slots) {
            int slot = (startSlot + slotsChecked) % slots;
            ItemStack inSlot = handler.getStackInSlot(slot);

            if (!inSlot.isEmpty()) {
                // Extract up to maxExtract items (tier-based)
                ItemStack extracted = handler.extractItem(slot, maxExtract, false);
                if (!extracted.isEmpty()) {
                    // Send into pipe network (full stack travels together)
                    receiveItem(extracted, face);

                    // Update cursor to next slot for next time
                    slotCursors.put(face, (slot + 1) % slots);
                    return true;
                }
            }
            slotsChecked++;
        }

        // Reset cursor if looped through all without success
        slotCursors.put(face, 0);
        return false;
    }

    public void setExtractionRate(int rate) {
        this.extractionRate = Math.max(1, rate);
    }

    public int getExtractionRate() {
        return extractionRate;
    }
}

package com.antigravity.advancedsorter.machines.converter;

import com.antigravity.advancedsorter.tiles.TileBasicEnergy;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Optional;

import ic2.api.energy.tile.IEnergyEmitter;

import net.minecraft.util.ITickable;
import java.util.EnumMap;
import java.util.Map;

@Optional.InterfaceList({
    @Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "ic2"),
    @Optional.Interface(iface = "ic2.api.energy.tile.IEnergySource", modid = "ic2")
})
public class TileEnergyConverter extends TileBasicEnergy implements ITickable {

    // OPTYMALIZACJA: Cache sąsiadów
    private final Map<EnumFacing, IEnergyStorage> neighborEnergyCache = new EnumMap<>(EnumFacing.class);
    private boolean cacheDirty = true;

    // OPTYMALIZACJA: Tryb uśpienia gdy brak transferu energii
    private int idleTicks = 0;
    private static final int IDLE_THRESHOLD = 20; // Po 1 sekundzie bezczynności
    private static final int SLEEP_INTERVAL = 5; // Sprawdzaj co 5 ticków
    private int sleepTimer = 0;

    public TileEnergyConverter() {
        super(10000000, 100000, 100000);
        this.sinkTier = 4; // EV (Tier 4) - Accepts High Voltage
        this.sourceTier = 1; // LV (Tier 1) - Outputs Low Voltage (Safe)
    }

    public void onNeighborChanged() {
        cacheDirty = true;
        idleTicks = 0; // Wake up on neighbor update
        sleepTimer = 0;
    }

    private void updateCache() {
        if (cacheDirty) {
            neighborEnergyCache.clear();
            for (EnumFacing facing : EnumFacing.VALUES) {
                TileEntity neighbor = world.getTileEntity(pos.offset(facing));
                if (neighbor != null && neighbor.hasCapability(CapabilityEnergy.ENERGY, facing.getOpposite())) {
                    IEnergyStorage handler = neighbor.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
                    if (handler != null) {
                        neighborEnergyCache.put(facing, handler);
                    }
                }
            }
            cacheDirty = false;
        }
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        updateCache();

        // Jeśli jesteśmy w trybie idle, czekamy
        if (idleTicks > IDLE_THRESHOLD) {
            if (sleepTimer > 0) {
                sleepTimer--;
                return;
            }
            sleepTimer = SLEEP_INTERVAL;
        }

        boolean active = false;

        // 1. Pull energy from neighbors (RF/FE)
        if (pullEnergy()) active = true;

        // 2. Push energy to neighbors (RF/FE)
        if (pushEnergy()) active = true;

        if (active) {
            idleTicks = 0; // Reset idle timer if active
            sleepTimer = 0;
        } else {
            if (idleTicks <= IDLE_THRESHOLD) {
                idleTicks++;
            }
        }
    }

    private boolean pullEnergy() {
        if (storage.getEnergyStored() >= storage.getMaxEnergyStored()) return false;

        boolean transferred = false;
        for (Map.Entry<EnumFacing, IEnergyStorage> entry : neighborEnergyCache.entrySet()) {
            IEnergyStorage neighborStorage = entry.getValue();
            // Verify validity (tile might have been removed but neighborChanged didn't fire perfectly or capability became invalid)
            // But usually neighborChanged handles it.
            if (neighborStorage.canExtract()) {
                int space = storage.getMaxEnergyStored() - storage.getEnergyStored();
                int maxReceive = Math.min(100000, space);
                int extracted = neighborStorage.extractEnergy(maxReceive, true);
                if (extracted > 0) {
                    int accepted = storage.receiveEnergy(extracted, false);
                    neighborStorage.extractEnergy(accepted, false);
                    transferred = true;
                }
            }
        }
        return transferred;
    }

    private boolean pushEnergy() {
        if (storage.getEnergyStored() <= 0) return false;

        boolean transferred = false;
        for (Map.Entry<EnumFacing, IEnergyStorage> entry : neighborEnergyCache.entrySet()) {
            IEnergyStorage neighborStorage = entry.getValue();
            if (neighborStorage.canReceive()) {
                int maxExtract = Math.min(100000, storage.getEnergyStored());
                int accepted = neighborStorage.receiveEnergy(maxExtract, false);
                if (accepted > 0) {
                    storage.extractEnergy(accepted, false);
                    transferred = true;
                }
            }
        }
        return transferred;
    }

    // --- IC2 Overrides ---

    @Override
    @Optional.Method(modid = "ic2")
    public double getOfferedEnergy() {
        // Converter specific: Convert RF to EU for output
        // Limit to LV (32 EU/t) to prevent explosions, unless upgraded (but upgrades not impl yet)
        return Math.min(32, storage.getEnergyStored() / (double) RF_PER_EU);
    }
}

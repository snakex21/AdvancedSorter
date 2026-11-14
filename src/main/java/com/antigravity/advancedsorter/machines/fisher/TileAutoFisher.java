package com.antigravity.advancedsorter.machines.fisher;

import com.antigravity.advancedsorter.tiles.TileBasicEnergy;
import net.minecraft.block.material.Material;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;

import javax.annotation.Nullable;
import java.util.List;

public class TileAutoFisher extends TileBasicEnergy implements ITickable {

    private final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    private int timer = 0;
    private static final int OPERATION_COST = 400;
    private static final int SPEED = 100; // 5 seconds (slower than others)

    // OPTYMALIZACJA: Cache dla sprawdzania wody (sprawdzaj co 20 ticków zamiast co tick)
    private int waterCheckTimer = 0;
    private boolean hasWater = false;
    private static final int WATER_CHECK_INTERVAL = 20;

    // OPTYMALIZACJA: Sleep timer dla braku energii/miejsca
    private int sleepTimer = 0;

    public TileAutoFisher() {
        super(20000, 200);
    }

    @Override
    public void update() {
        if (world.isRemote) return;

        if (sleepTimer > 0) {
            sleepTimer--;
            return;
        }

        // OPTYMALIZACJA: Sprawdzaj wodę co 20 ticków zamiast co tick
        waterCheckTimer++;
        if (waterCheckTimer >= WATER_CHECK_INTERVAL) {
            waterCheckTimer = 0;
            EnumFacing facing = world.getBlockState(pos).getValue(BlockAutoFisher.FACING);
            BlockPos waterPos = pos.offset(facing);
            hasWater = world.getBlockState(waterPos).getMaterial() == Material.WATER;
        }

        if (!hasWater) {
            sleepTimer = 20; // Sleep and check later
            return; // No water, no fish
        }

        if (storage.getEnergyStored() >= OPERATION_COST) {
            timer++;
            if (timer >= SPEED) {
                timer = 0;
                if (!performFish()) {
                    sleepTimer = 20; // Sleep if inventory full
                }
            }
        } else {
            sleepTimer = 20; // Not enough energy
        }
    }

    /**
     * @return true if fished successfully
     */
    private boolean performFish() {
        if (world instanceof WorldServer) {
            // Use GAMEPLAY_FISHING_FISH to catch only fish, not junk or treasures
            LootTable loottable = world.getLootTableManager().getLootTableFromLocation(LootTableList.GAMEPLAY_FISHING_FISH);
            LootContext.Builder builder = new LootContext.Builder((WorldServer) world);
            List<ItemStack> result = loottable.generateLootForPools(world.rand, builder.build());

            // OPTYMALIZACJA: Sprawdź czy drop się zmieści PRZED pobraniem energii i wstawieniem.
            // Zapobiega wyrzucaniu przedmiotów na ziemię (lagi).
            if (canFitResult(result)) {
                storage.extractEnergy(OPERATION_COST, false);
                for (ItemStack stack : result) {
                    insertItem(stack);
                }
                return true;
            }
        }
        return false;
    }

    private boolean canFitResult(List<ItemStack> drops) {
        ItemStackHandler simInventory = new ItemStackHandler(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            simInventory.setStackInSlot(i, inventory.getStackInSlot(i).copy());
        }

        for (ItemStack drop : drops) {
            ItemStack remaining = drop.copy();
            for (int i = 0; i < simInventory.getSlots(); i++) {
                remaining = simInventory.insertItem(i, remaining, false);
                if (remaining.isEmpty()) break;
            }
            if (!remaining.isEmpty()) return false;
        }
        return true;
    }

    private void insertItem(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < inventory.getSlots(); i++) {
            remaining = inventory.insertItem(i, remaining, false);
            if (remaining.isEmpty()) break;
        }
        // Nie wyrzucaj na ziemię - canFitResult gwarantuje, że się zmieści.
        // Jeśli jednak coś zostanie (błąd logiczny), lepiej to zniszczyć niż lagować serwer.
    }

    public void dropInventory() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        return compound;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }
}

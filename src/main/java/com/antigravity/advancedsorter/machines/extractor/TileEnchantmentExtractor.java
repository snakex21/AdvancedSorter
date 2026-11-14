package com.antigravity.advancedsorter.machines.extractor;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Map;

public class TileEnchantmentExtractor extends TileEntity implements ITickable {

    // OPTYMALIZACJA: Cache dla pola energii
    private static final Field ENERGY_FIELD;
    static {
        Field f = null;
        try {
            f = EnergyStorage.class.getDeclaredField("energy");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        ENERGY_FIELD = f;
    }

    private final EnergyStorage energyStorage = new EnergyStorage(50000, 1000, 0);
    private static final int ENERGY_PER_OPERATION = 2000;
    private static final int TICKS_PER_OPERATION = 100; // 5 seconds

    // Slot 0: Input Item, Slot 1: Input Book
    // Slot 2: Output Item, Slot 3: Output Book
    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                // Must have enchantments
                return stack.isItemEnchanted() || stack.getItem() == Items.ENCHANTED_BOOK;
            }
            if (slot == 1) {
                return stack.getItem() == Items.BOOK;
            }
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == 0 || slot == 1) sleepTimer = 0; // Wake up
            markDirty();
        }
    };

    private int progress = 0;
    private int sleepTimer = 0;

    @Override
    public void update() {
        if (world.isRemote) return;

        if (sleepTimer > 0) {
            sleepTimer--;
            return;
        }

        ItemStack inputItem = inventory.getStackInSlot(0);
        ItemStack inputBook = inventory.getStackInSlot(1);

        if (!inputItem.isEmpty() && !inputBook.isEmpty()) {
            if (canProcess(inputItem, inputBook)) {
                if (energyStorage.getEnergyStored() >= ENERGY_PER_OPERATION) {
                    progress++;
                    if (progress >= TICKS_PER_OPERATION) {
                        processItem(inputItem, inputBook);
                        progress = 0;
                    }
                } else {
                    sleepTimer = 20; // No energy
                }
            } else {
                progress = 0;
                sleepTimer = 20; // Cannot process (invalid items or full)
            }
        } else {
            progress = 0;
            sleepTimer = 20; // Empty input
        }
    }

    private boolean canProcess(ItemStack item, ItemStack book) {
        if (item.getItem() == Items.ENCHANTED_BOOK) return false; // Don't extract from books to books (pointless)
        if (book.getItem() != Items.BOOK) return false;

        // OPTYMALIZACJA: Fail fast - sprawdź czy przedmiot w ogóle ma tagi enchantów
        // zanim spróbujesz parsować całą listę do HashMapy
        if (!item.isItemEnchanted()) return false;

        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(item);
        if (enchants.isEmpty()) return false;

        // Check output slots
        ItemStack outItem = inventory.getStackInSlot(2);
        ItemStack outBook = inventory.getStackInSlot(3);

        if (!outItem.isEmpty() && !outItem.isItemEqual(item)) return false; // Output blocked
        if (!outBook.isEmpty()) return false; // Output book slot must be empty for simplicity

        return true;
    }

    private void processItem(ItemStack inputItem, ItemStack inputBook) {
        // 1. Create Output Item (Clean)
        ItemStack resultItem = inputItem.copy();

        // Remove enchantments
        if (resultItem.hasTagCompound()) {
            resultItem.getTagCompound().removeTag("ench");
        }
        resultItem.setCount(1);

        // 2. Create Output Book (Enchanted)
        ItemStack resultBook = new ItemStack(Items.ENCHANTED_BOOK);
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(inputItem);

        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            ItemEnchantedBook.addEnchantment(resultBook, new net.minecraft.enchantment.EnchantmentData(entry.getKey(), entry.getValue()));
        }

        // 3. Consume Inputs
        inventory.extractItem(0, 1, false);
        inventory.extractItem(1, 1, false);
        energyStorage.extractEnergy(ENERGY_PER_OPERATION, false);

        // 4. Insert Outputs
        inventory.insertItem(2, resultItem, false);
        inventory.insertItem(3, resultBook, false);

        markDirty();
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return TICKS_PER_OPERATION;
    }

    public void setEnergyStored(int energy) {
        if (ENERGY_FIELD != null) {
            try {
                ENERGY_FIELD.setInt(energyStorage, energy);
            } catch (Exception e) {}
        }
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    // Capability
    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY || capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(energyStorage);
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }

    // NBT
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setInteger("Energy", energyStorage.getEnergyStored());
        compound.setInteger("Progress", progress);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Inventory")) inventory.deserializeNBT(compound.getCompoundTag("Inventory"));

        if (compound.hasKey("Energy")) {
            int energy = compound.getInteger("Energy");
            if (ENERGY_FIELD != null) {
                 try {
                     ENERGY_FIELD.setInt(energyStorage, energy);
                } catch (Exception e) {}
            }
        }
        progress = compound.getInteger("Progress");
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

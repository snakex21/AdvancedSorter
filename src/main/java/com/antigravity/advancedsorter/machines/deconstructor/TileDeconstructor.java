package com.antigravity.advancedsorter.machines.deconstructor;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TileDeconstructor extends TileEntity implements ITickable {

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

    private final EnergyStorage energyStorage = new EnergyStorage(100000, 2000, 0);
    private static final int ENERGY_COST = 2000; // Cost per operation
    private static final int TICKS_PER_OPERATION = 40; // 2 seconds

    // OPTYMALIZACJA: Cache receptur - Item -> składniki (lub pusty list jeśli brak receptury)
    private static final Map<net.minecraft.item.Item, CachedRecipe> recipeCache = new HashMap<>();

    private static class CachedRecipe {
        final List<ItemStack> ingredients;
        final int requiredInputCount;

        CachedRecipe(List<ItemStack> ingredients, int requiredInputCount) {
            this.ingredients = ingredients;
            this.requiredInputCount = requiredInputCount;
        }
    }

    // Slot 0: Input
    // Slots 1-9: Output (max 9 ingredients from 3x3 crafting)
    private final ItemStackHandler inventory = new ItemStackHandler(10) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0; // Only insert into input
        }
    };

    private int progress = 0;
    // OPTYMALIZACJA: Sleep timer dla dekonstruktora
    private int sleepTimer = 0;

    // Cache dla aktualnego inputu
    private CachedRecipe currentRecipeCache = null;
    private net.minecraft.item.Item lastInputItem = null;

    @Override
    public void update() {
        if (world.isRemote) return;

        if (sleepTimer > 0) {
            sleepTimer--;
            return;
        }

        ItemStack input = inventory.getStackInSlot(0);

        if (!input.isEmpty()) {
            // OPTYMALIZACJA: Sprawdź czy input się zmienił
            if (lastInputItem != input.getItem()) {
                lastInputItem = input.getItem();
                currentRecipeCache = getCachedRecipe(input);
            }

            // Optimization: Only process if we have energy and cached recipe
            if (currentRecipeCache != null) {

                if (energyStorage.getEnergyStored() >= ENERGY_COST) {
                    List<ItemStack> ingredients = currentRecipeCache.ingredients;
                    int requiredCount = currentRecipeCache.requiredInputCount;

                    if (!ingredients.isEmpty() && input.getCount() >= requiredCount) {
                        // Check if outputs fit
                        if (canFitOutputs(ingredients)) {

                            progress++;
                            if (progress >= TICKS_PER_OPERATION) {
                                progress = 0;

                                // Consume input (proper amount based on recipe)
                                inventory.extractItem(0, requiredCount, false);
                                energyStorage.extractEnergy(ENERGY_COST, false);

                                // Produce outputs
                                for (ItemStack ing : ingredients) {
                                    insertOutput(ing.copy());
                                }

                                markDirty();
                            }
                        } else {
                            // Output full
                            sleepTimer = 20;
                        }
                    } else {
                        progress = 0; // Recipe requires more items than we have
                        sleepTimer = 20;
                    }
                } else {
                    // Not enough energy
                    sleepTimer = 20;
                }
            } else {
                progress = 0; // No recipe
                sleepTimer = 20;
            }
        } else {
            progress = 0;
            lastInputItem = null;
            currentRecipeCache = null;
            sleepTimer = 20; // Empty input
        }
    }

    private CachedRecipe getCachedRecipe(ItemStack input) {
        net.minecraft.item.Item item = input.getItem();

        // Sprawdź cache
        if (recipeCache.containsKey(item)) {
            return recipeCache.get(item);
        }

        // Szukaj receptury i zapisz w cache
        CachedRecipe result = findAndCacheRecipe(input);
        recipeCache.put(item, result);
        return result;
    }

    private CachedRecipe findAndCacheRecipe(ItemStack input) {
        // Iterate all registered recipes
        for (IRecipe recipe : CraftingManager.REGISTRY) {
            ItemStack output = recipe.getRecipeOutput();

            // Check if output matches input
            if (!output.isEmpty() && output.isItemEqual(input)) {
                // Found a recipe!
                // Get ingredients
                NonNullList<Ingredient> ings = recipe.getIngredients();
                List<ItemStack> result = new ArrayList<>();

                for (Ingredient ing : ings) {
                    if (ing != Ingredient.EMPTY) {
                        ItemStack[] matching = ing.getMatchingStacks();
                        if (matching.length > 0) {
                            result.add(matching[0].copy());
                        }
                    }
                }

                if (!result.isEmpty()) {
                    return new CachedRecipe(result, output.getCount());
                }
            }
        }
        return null; // No recipe found
    }

    private boolean canFitOutputs(List<ItemStack> ingredients) {
        // Simulate insertion into slots 1-9
        // This is complex to check perfectly without cloning inventory
        // Simple check: do we have empty slots?
        int emptySlots = 0;
        for (int i = 1; i < 10; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) emptySlots++;
        }
        return emptySlots >= ingredients.size(); // Rough check
    }

    private void insertOutput(ItemStack stack) {
        for (int i = 1; i < 10; i++) {
            stack = inventory.insertItem(i, stack, false);
            if (stack.isEmpty()) return;
        }
    }

    public int getEnergyStored() { return energyStorage.getEnergyStored(); }
    public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
    public int getProgress() { return progress; }
    public ItemStackHandler getInternalInventory() { return inventory; }

    public void setEnergyStored(int e) {
        if (ENERGY_FIELD != null) {
            try {
                ENERGY_FIELD.setInt(energyStorage, e);
            } catch (Exception ex) {}
        }
    }
    public void setProgress(int p) { this.progress = p; }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return true;
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
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
        setEnergyStored(compound.getInteger("Energy"));
        progress = compound.getInteger("Progress");
    }
}

package com.antigravity.advancedsorter.machines.ammopress;

import com.antigravity.advancedsorter.autocrafter.CraftingRecipe;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;

public class TileAmmoPress extends TileEntity implements ITickable {

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
    private static final int ENERGY_PER_OPERATION = 500;
    private static final int CRAFT_INTERVAL = 10;
    private int tickCounter = 0;

    // OPTYMALIZACJA: Timer do zatrzymywania automatyzacji gdy nic nie można wytworzyć
    private int automationStuckTimer = 0;
    private static final int AUTOMATION_RETRY_DELAY = 40; // 2 seconds

    // Sloty wejściowe (27 slotów)
    private final ItemStackHandler inputInventory = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            automationStuckTimer = 0; // Reset timer
            markDirty();
            sendUpdate();
        }
    };

    // Sloty wyjściowe (9 slotów)
    private final ItemStackHandler outputInventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            automationStuckTimer = 0; // Reset timer
            markDirty();
            sendUpdate();
        }
    };

    // Siatka definiowania receptury (3x3 = 9 slotów)
    private final ItemStackHandler recipeGrid = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            sendUpdate();
        }
    };

    // Slot na wynik receptury
    private final ItemStackHandler recipeResultSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            sendUpdate();
        }
    };

    private ItemStack recipeResultPreview = ItemStack.EMPTY;
    private final List<CraftingRecipe> recipes = new ArrayList<>();
    private int nextRecipeId = 1;

    private final List<Integer> craftingQueue = new ArrayList<>();
    private boolean automationEnabled = false;
    private boolean roundRobinMode = false;
    private int currentRoundRobinIndex = 0;
    private final List<Integer> automationRecipes = new ArrayList<>();

    public TileAmmoPress() {
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        // OPTYMALIZACJA: Logic for stuck automation
        if (automationStuckTimer > 0) {
            automationStuckTimer--;
        }

        tickCounter++;
        if (tickCounter >= CRAFT_INTERVAL) {
            tickCounter = 0;
            processCraftingQueue();
            if (automationEnabled && automationStuckTimer <= 0) {
                processAutomation();
            }
        }
    }

    private void processCraftingQueue() {
        if (craftingQueue.isEmpty()) return;

        int recipeId = craftingQueue.get(0);
        CraftingRecipe recipe = getRecipeById(recipeId);

        if (recipe == null) {
            craftingQueue.remove(0);
            return;
        }

        if (tryCraft(recipe)) {
            craftingQueue.remove(0);
            markDirty();
            sendUpdate();
        }
    }

    private void processAutomation() {
        if (automationRecipes.isEmpty()) return;
        List<ItemStack> available = getAvailableItems();
        boolean craftedAny = false;

        if (roundRobinMode) {
            int size = automationRecipes.size();
            for (int i = 0; i < size; i++) {
                int checkIndex = (currentRoundRobinIndex + i) % size;
                int recipeId = automationRecipes.get(checkIndex);
                CraftingRecipe recipe = getRecipeById(recipeId);
                if (recipe != null && recipe.canCraft(available)) {
                    if (tryCraft(recipe)) {
                        currentRoundRobinIndex = (checkIndex + 1) % size;
                        craftedAny = true;
                        break;
                    }
                }
            }
        } else {
            for (int recipeId : automationRecipes) {
                CraftingRecipe recipe = getRecipeById(recipeId);
                if (recipe != null && recipe.canCraft(available)) {
                    if (tryCraft(recipe)) {
                        craftedAny = true;
                        break;
                    }
                }
            }
        }

        if (!craftedAny) {
            automationStuckTimer = AUTOMATION_RETRY_DELAY;
        }
    }

    public boolean tryCraft(CraftingRecipe recipe) {
        if (energyStorage.getEnergyStored() < ENERGY_PER_OPERATION) return false;
        return tryCraft(recipe, new HashSet<>(), false);
    }

    private boolean tryCraft(CraftingRecipe recipe, Set<Integer> inProgress, boolean isSubCraft) {
        if (inProgress.contains(recipe.getId())) return false;
        inProgress.add(recipe.getId());

        List<ItemStack> available = getAvailableItems();
        List<ItemStack> missing = recipe.getMissingIngredients(available);

        if (!missing.isEmpty()) {
            for (ItemStack missingItem : missing) {
                CraftingRecipe subRecipe = findRecipeForResult(missingItem);
                if (subRecipe != null) {
                    // Check if we have energy for subcrafts too
                    if (energyStorage.getEnergyStored() < ENERGY_PER_OPERATION) return false;

                    int timesNeeded = (int) Math.ceil((double) missingItem.getCount() / subRecipe.getResult().getCount());
                    for (int i = 0; i < timesNeeded; i++) {
                        if (!tryCraft(subRecipe, inProgress, true)) {
                            return false;
                        }
                    }
                } else {
                    return false;
                }
            }

            available = getAvailableItems();
            if (!recipe.canCraft(available)) return false;
        }

        return executeCraft(recipe, isSubCraft);
    }

    private boolean executeCraft(CraftingRecipe recipe, boolean toInput) {
        if (energyStorage.getEnergyStored() < ENERGY_PER_OPERATION) return false;

        ItemStack resultCopy = recipe.getResult().copy();

        if (!toInput) {
            if (isIngredientForAnyAutoRecipe(resultCopy)) {
                toInput = true;
            }
        }

        ItemStack remaining;
        if (toInput) {
            remaining = insertIntoInput(resultCopy, true);
            if (!remaining.isEmpty()) {
                ItemStack remOutput = insertIntoOutput(resultCopy, true);
                if (remOutput.isEmpty()) {
                    toInput = false;
                    remaining = ItemStack.EMPTY;
                }
            }
        } else {
            remaining = insertIntoOutput(resultCopy, true);
        }

        if (!remaining.isEmpty()) return false;

        // Consume ingredients
        for (ItemStack ingredient : recipe.getIngredients()) {
            int toRemove = ingredient.getCount();
            for (int i = 0; i < inputInventory.getSlots() && toRemove > 0; i++) {
                ItemStack slot = inputInventory.getStackInSlot(i);
                if (ItemStack.areItemsEqual(ingredient, slot) && ItemStack.areItemStackTagsEqual(ingredient, slot)) {
                    int remove = Math.min(toRemove, slot.getCount());
                    inputInventory.extractItem(i, remove, false);
                    toRemove -= remove;
                }
            }
        }

        // Consume Energy
        energyStorage.extractEnergy(ENERGY_PER_OPERATION, false);

        // Add result
        if (toInput) {
            insertIntoInput(recipe.getResult().copy(), false);
        } else {
            insertIntoOutput(recipe.getResult().copy(), false);
        }

        markDirty();
        sendUpdate();
        return true;
    }

    private boolean isIngredientForAnyAutoRecipe(ItemStack stack) {
        for (int id : automationRecipes) {
            CraftingRecipe r = getRecipeById(id);
            if (r != null) {
                for (ItemStack ing : r.getIngredients()) {
                    if (ItemStack.areItemsEqual(ing, stack)) return true;
                }
            }
        }
        return false;
    }

    private boolean isResultOfAnyAutoRecipe(ItemStack stack) {
        for (int id : automationRecipes) {
            CraftingRecipe r = getRecipeById(id);
            if (r != null && ItemStack.areItemsEqual(r.getResult(), stack)) return true;
        }
        return false;
    }

    private boolean shouldRejectFromPipes(ItemStack stack) {
        return isResultOfAnyAutoRecipe(stack) && !isIngredientForAnyAutoRecipe(stack);
    }

    private ItemStack insertIntoInput(ItemStack stack, boolean simulate) {
        return ItemHandlerHelper.insertItemStacked(inputInventory, stack, simulate);
    }

    private ItemStack insertIntoOutput(ItemStack stack, boolean simulate) {
        return ItemHandlerHelper.insertItemStacked(outputInventory, stack, simulate);
    }

    public CraftingRecipe findRecipeForResult(ItemStack result) {
        for (CraftingRecipe recipe : recipes) {
            if (recipe.matchesResult(result)) return recipe;
        }
        return null;
    }

    public CraftingRecipe getRecipeById(int id) {
        for (CraftingRecipe recipe : recipes) {
            if (recipe.getId() == id) return recipe;
        }
        return null;
    }

    public List<ItemStack> getAvailableItems() {
        List<ItemStack> available = new ArrayList<>();
        for (int i = 0; i < inputInventory.getSlots(); i++) {
            ItemStack stack = inputInventory.getStackInSlot(i);
            if (!stack.isEmpty()) available.add(stack.copy());
        }
        return available;
    }

    public CraftingRecipe addRecipeFromGridAndSlot() {
        ItemStack result = recipeResultSlot.getStackInSlot(0);
        if (result.isEmpty()) return null;

        List<ItemStack> ingredients = new ArrayList<>();
        for (int i = 0; i < recipeGrid.getSlots(); i++) {
            ItemStack stack = recipeGrid.getStackInSlot(i);
            if (!stack.isEmpty()) {
                boolean found = false;
                for (ItemStack existing : ingredients) {
                    if (ItemStack.areItemsEqual(existing, stack) && ItemStack.areItemStackTagsEqual(existing, stack)) {
                        existing.grow(stack.getCount());
                        found = true;
                        break;
                    }
                }
                if (!found) ingredients.add(stack.copy());
            }
        }

        if (ingredients.isEmpty()) return null;

        CraftingRecipe recipe = new CraftingRecipe(ingredients, result.copy());
        recipe.setId(nextRecipeId++);
        recipes.add(recipe);
        markDirty();
        sendUpdate();
        return recipe;
    }

    public boolean removeRecipe(int id) {
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).getId() == id) {
                recipes.remove(i);
                craftingQueue.removeIf(queueId -> queueId == id);
                automationRecipes.remove((Integer) id);
                markDirty();
                sendUpdate();
                return true;
            }
        }
        return false;
    }

    public void addToAutomation(int recipeId) {
        if (!automationRecipes.contains(recipeId)) {
            automationRecipes.add(recipeId);
            markDirty();
            sendUpdate();
        }
    }

    public void removeFromAutomation(int recipeId) {
        automationRecipes.remove((Integer) recipeId);
        markDirty();
        sendUpdate();
    }

    public void updateRecipePreview() {
        List<ItemStack> gridItems = new ArrayList<>();
        for (int i = 0; i < recipeGrid.getSlots(); i++) {
            ItemStack stack = recipeGrid.getStackInSlot(i);
            if (!stack.isEmpty()) {
                boolean found = false;
                for (ItemStack existing : gridItems) {
                    if (ItemStack.areItemsEqual(existing, stack) && ItemStack.areItemStackTagsEqual(existing, stack)) {
                        existing.grow(stack.getCount());
                        found = true;
                        break;
                    }
                }
                if (!found) gridItems.add(stack.copy());
            }
        }

        recipeResultPreview = ItemStack.EMPTY;
        for (CraftingRecipe recipe : recipes) {
            if (ingredientsMatch(recipe.getIngredients(), gridItems)) {
                recipeResultPreview = recipe.getResult().copy();
                break;
            }
        }
        markDirty();
        sendUpdate();
    }

    public int copyRecipesFrom(TileAmmoPress other) {
        int added = 0;
        for (CraftingRecipe otherRecipe : other.recipes) {
            boolean exists = false;
            for (CraftingRecipe myRecipe : recipes) {
                if (isSameRecipe(myRecipe, otherRecipe)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                CraftingRecipe copy = new CraftingRecipe(otherRecipe.writeToNBT());
                copy.setId(nextRecipeId++);
                recipes.add(copy);
                added++;
            }
        }

        if (added > 0) {
            markDirty();
            sendUpdate();
        }
        return added;
    }

    private boolean isSameRecipe(CraftingRecipe a, CraftingRecipe b) {
        if (!ItemStack.areItemsEqual(a.getResult(), b.getResult()) ||
            !ItemStack.areItemStackTagsEqual(a.getResult(), b.getResult()) ||
            a.getResult().getCount() != b.getResult().getCount()) {
            return false;
        }
        return ingredientsMatch(a.getIngredients(), b.getIngredients());
    }

    private boolean ingredientsMatch(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;
        List<ItemStack> aCopy = new ArrayList<>();
        for (ItemStack s : a) aCopy.add(s.copy());

        for (ItemStack bItem : b) {
            boolean found = false;
            for (int i = 0; i < aCopy.size(); i++) {
                ItemStack aItem = aCopy.get(i);
                if (ItemStack.areItemsEqual(aItem, bItem) && ItemStack.areItemStackTagsEqual(aItem, bItem) && aItem.getCount() == bItem.getCount()) {
                    aCopy.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return aCopy.isEmpty();
    }

    // Getters and Setters
    public List<CraftingRecipe> getRecipes() { return Collections.unmodifiableList(recipes); }
    public List<Integer> getCraftingQueue() { return Collections.unmodifiableList(craftingQueue); }
    public boolean isAutomationEnabled() { return automationEnabled; }
    public void setAutomationEnabled(boolean enabled) { this.automationEnabled = enabled; markDirty(); sendUpdate(); }
    public boolean isRoundRobinMode() { return roundRobinMode; }
    public void setRoundRobinMode(boolean mode) { this.roundRobinMode = mode; this.currentRoundRobinIndex = 0; markDirty(); sendUpdate(); }
    public boolean isInAutomation(int id) { return automationRecipes.contains(id); }
    public ItemStack getRecipeResultPreview() { return recipeResultPreview; }
    public IItemHandler getInputHandler() { return inputInventory; }
    public IItemHandler getOutputHandler() { return outputInventory; }
    public IItemHandler getRecipeGridHandler() { return recipeGrid; }
    public IItemHandler getRecipeResultSlotHandler() { return recipeResultSlot; }
    public int getEnergyStored() { return energyStorage.getEnergyStored(); }
    public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
    public void setEnergyStored(int e) {
        if (ENERGY_FIELD != null) {
            try {
                ENERGY_FIELD.setInt(energyStorage, e);
            } catch (Exception ex) {}
        }
    }

    // NBT and Sync
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Input", inputInventory.serializeNBT());
        compound.setTag("Output", outputInventory.serializeNBT());
        compound.setTag("RecipeGrid", recipeGrid.serializeNBT());
        compound.setTag("RecipeResultSlot", recipeResultSlot.serializeNBT());
        compound.setInteger("Energy", energyStorage.getEnergyStored());

        NBTTagList recipeList = new NBTTagList();
        for (CraftingRecipe recipe : recipes) recipeList.appendTag(recipe.writeToNBT());
        compound.setTag("Recipes", recipeList);
        compound.setInteger("NextRecipeId", nextRecipeId);

        int[] queueArray = new int[craftingQueue.size()];
        for (int i = 0; i < craftingQueue.size(); i++) queueArray[i] = craftingQueue.get(i);
        compound.setIntArray("Queue", queueArray);

        int[] autoArray = new int[automationRecipes.size()];
        for(int i=0; i<automationRecipes.size(); i++) autoArray[i] = automationRecipes.get(i);
        compound.setIntArray("AutomationRecipes", autoArray);

        compound.setBoolean("AutomationEnabled", automationEnabled);
        compound.setBoolean("RoundRobinMode", roundRobinMode);
        compound.setInteger("RoundRobinIndex", currentRoundRobinIndex);

        if (!recipeResultPreview.isEmpty()) compound.setTag("ResultPreview", recipeResultPreview.writeToNBT(new NBTTagCompound()));

        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Input")) inputInventory.deserializeNBT(compound.getCompoundTag("Input"));
        if (compound.hasKey("Output")) outputInventory.deserializeNBT(compound.getCompoundTag("Output"));
        if (compound.hasKey("RecipeGrid")) recipeGrid.deserializeNBT(compound.getCompoundTag("RecipeGrid"));
        if (compound.hasKey("RecipeResultSlot")) recipeResultSlot.deserializeNBT(compound.getCompoundTag("RecipeResultSlot"));
        setEnergyStored(compound.getInteger("Energy"));

        recipes.clear();
        if (compound.hasKey("Recipes")) {
            NBTTagList recipeList = compound.getTagList("Recipes", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < recipeList.tagCount(); i++) recipes.add(new CraftingRecipe(recipeList.getCompoundTagAt(i)));
        }
        nextRecipeId = compound.getInteger("NextRecipeId");
        if (nextRecipeId == 0) nextRecipeId = 1;

        craftingQueue.clear();
        if (compound.hasKey("Queue")) {
            int[] queueArray = compound.getIntArray("Queue");
            for (int id : queueArray) craftingQueue.add(id);
        }

        automationRecipes.clear();
        if (compound.hasKey("AutomationRecipes")) {
            int[] autoArray = compound.getIntArray("AutomationRecipes");
            for (int id : autoArray) automationRecipes.add(id);
        }

        automationEnabled = compound.getBoolean("AutomationEnabled");
        roundRobinMode = compound.getBoolean("RoundRobinMode");
        currentRoundRobinIndex = compound.getInteger("RoundRobinIndex");

        if (compound.hasKey("ResultPreview")) recipeResultPreview = new ItemStack(compound.getCompoundTag("ResultPreview"));
    }

    protected void sendUpdate() {
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
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

    // Capabilities
    private final IItemHandler combinedHandler = new IItemHandler() {
        @Override
        public int getSlots() { return inputInventory.getSlots() + outputInventory.getSlots(); }
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < inputInventory.getSlots()) return ItemStack.EMPTY;
            return outputInventory.getStackInSlot(slot - inputInventory.getSlots());
        }
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (shouldRejectFromPipes(stack)) return stack;
            return ItemHandlerHelper.insertItemStacked(inputInventory, stack, simulate);
        }
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot >= inputInventory.getSlots()) {
                return outputInventory.extractItem(slot - inputInventory.getSlots(), amount, simulate);
            }
            return ItemStack.EMPTY;
        }
        @Override
        public int getSlotLimit(int slot) {
            if (slot < inputInventory.getSlots()) return inputInventory.getSlotLimit(slot);
            return outputInventory.getSlotLimit(slot - inputInventory.getSlots());
        }
    };

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return true;
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return CapabilityEnergy.ENERGY.cast(energyStorage);
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return (T) combinedHandler;
        return super.getCapability(capability, facing);
    }
}

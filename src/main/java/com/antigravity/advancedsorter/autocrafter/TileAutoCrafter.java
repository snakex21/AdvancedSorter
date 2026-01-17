package com.antigravity.advancedsorter.autocrafter;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Tile Entity dla Auto Craftera.
 *
 * Funkcje:
 * - Przechowuje liste wlasnych receptur
 * - Lancuchowe craftowanie (jesli brakuje skladnika, sprawdza czy moze go zrobic)
 * - Automatyzacja z rurami (przedmioty wchodza, craftowane wychodza)
 * - Kolejka craftowania (jakie przedmioty maja byc robione)
 */
public class TileAutoCrafter extends TileEntity implements ITickable {

    // Sloty wejściowe (27 slotów - jak skrzynia)
    private final ItemStackHandler inputInventory = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            sendUpdate();
        }
    };

    // Sloty wyjściowe (9 slotów)
    private final ItemStackHandler outputInventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
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

    // Slot na wynik receptury (gdzie uzytkownik kladzie co ma powstac)
    private final ItemStackHandler recipeResultSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            sendUpdate();
        }
    };

    // Slot wyniku receptury (do pokazania co powstanie)
    private ItemStack recipeResultPreview = ItemStack.EMPTY;

    // Lista wszystkich zdefiniowanych receptur
    private final List<CraftingRecipe> recipes = new ArrayList<>();
    private int nextRecipeId = 1;

    // Kolejka przedmiotów do zrobienia (ID receptury)
    private final List<Integer> craftingQueue = new ArrayList<>();

    // Tryb automatyzacji: czy automatycznie craftować gdy przyjdą przedmioty
    private boolean automationEnabled = false;

    // Tryb cykliczny (Round-Robin): false = Priorytet (zawsze od poczatku), true = Cykliczny
    private boolean roundRobinMode = false;
    private int currentRoundRobinIndex = 0;

    // Lista receptur do automatyzacji (ID receptur które mają być robione automatycznie)
    // Zmieniono z Set na List aby zachowac kolejnosc (1 -> 2 -> 3)
    private final List<Integer> automationRecipes = new ArrayList<>();

    // Tick counter dla przetwarzania
    private int tickCounter = 0;
    private static final int CRAFT_INTERVAL = 10; // Co ile ticków próbować craftować

    public TileAutoCrafter() {
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        tickCounter++;
        if (tickCounter >= CRAFT_INTERVAL) {
            tickCounter = 0;

            // Proces craftowania z kolejki
            processCraftingQueue();

            // Automatyzacja - sprawdź czy można coś zrobić
            if (automationEnabled) {
                processAutomation();
            }
        }
    }

    /**
     * Przetwarza kolejkę craftowania.
     */
    private void processCraftingQueue() {
        if (craftingQueue.isEmpty()) return;

        int recipeId = craftingQueue.get(0);
        CraftingRecipe recipe = getRecipeById(recipeId);

        if (recipe == null) {
            craftingQueue.remove(0);
            return;
        }

        // Próbuj wykonać craft
        if (tryCraft(recipe)) {
            craftingQueue.remove(0);
            markDirty();
            sendUpdate(); // Odswiez stan kolejki u gracza
        }
    }

    /**
     * Automatyzacja - próbuje wykonać receptury z listy automatyzacji.
     */
    private void processAutomation() {
        if (automationRecipes.isEmpty()) return;

        List<ItemStack> available = getAvailableItems();

        if (roundRobinMode) {
            // Tryb Cykliczny: Zaczynamy od ostatniego indeksu
            int size = automationRecipes.size();
            boolean crafted = false;

            // Sprawdzamy wszystkie receptury zaczynajac od aktualnego indeksu
            for (int i = 0; i < size; i++) {
                int checkIndex = (currentRoundRobinIndex + i) % size;
                int recipeId = automationRecipes.get(checkIndex);

                CraftingRecipe recipe = getRecipeById(recipeId);
                if (recipe != null && recipe.canCraft(available)) {
                    if (tryCraft(recipe)) {
                        // Udalo sie - przesuwamy wskaznik na nastepna recepture
                        currentRoundRobinIndex = (checkIndex + 1) % size;
                        crafted = true;
                        break; // Jeden craft na tick
                    }
                }
            }

            // Jesli nic nie udalo sie zrobic, nie przesuwamy indeksu (czekamy na surowce)
            // Lub opcjonalnie: mozna przesunac, zeby sprawdzac inne next tick.
            // W tym wariancie trzymamy sie "Round Robin" przy sukcesie.
        } else {
            // Tryb Priorytetowy: Zawsze od poczatku listy
            for (int recipeId : automationRecipes) {
                CraftingRecipe recipe = getRecipeById(recipeId);
                if (recipe != null && recipe.canCraft(available)) {
                    tryCraft(recipe);
                    break; // Jeden craft na tick
                }
            }
        }
    }

    /**
     * Próbuje wykonać craft receptury.
     * Jeśli brakuje składników, próbuje najpierw je zrobić (łańcuchowe craftowanie).
     *
     * @return true jeśli craft się udał
     */
    public boolean tryCraft(CraftingRecipe recipe) {
        return tryCraft(recipe, new HashSet<>(), false);
    }

    /**
     * Wersja z ochroną przed rekurencją (żeby nie wpaść w pętlę).
     * @param isSubCraft true jeśli to jest sub-craft (wynik trafia do input)
     */
    private boolean tryCraft(CraftingRecipe recipe, Set<Integer> inProgress, boolean isSubCraft) {
        if (inProgress.contains(recipe.getId())) {
            // Zapobieganie nieskończonej rekurencji
            return false;
        }
        inProgress.add(recipe.getId());

        List<ItemStack> available = getAvailableItems();

        // Sprawdź brakujące składniki
        List<ItemStack> missing = recipe.getMissingIngredients(available);

        if (!missing.isEmpty()) {
            // Spróbuj zrobić brakujące składniki
            for (ItemStack missingItem : missing) {
                CraftingRecipe subRecipe = findRecipeForResult(missingItem);
                if (subRecipe != null) {
                    // Ile razy musimy wykonać sub-recepturę
                    int timesNeeded = (int) Math.ceil((double) missingItem.getCount() / subRecipe.getResult().getCount());
                    for (int i = 0; i < timesNeeded; i++) {
                        // Sub-craft - wynik trafia do INPUT żeby był dostępny dla następnego craftu
                        if (!tryCraft(subRecipe, inProgress, true)) {
                            return false;
                        }
                    }
                } else {
                    // Nie mamy receptury na brakujący składnik
                    return false;
                }
            }

            // Sprawdź ponownie po sub-craftach
            available = getAvailableItems();
            if (!recipe.canCraft(available)) {
                return false;
            }
        }

        // Wykonaj craft - jeśli to sub-craft, wynik trafia do input
        return executeCraft(recipe, isSubCraft);
    }

    /**
     * Faktycznie wykonuje craft - zabiera składniki i dodaje wynik.
     * @param toInput jeśli true, wynik trafia do inputInventory (dla łańcuchowego craftowania)
     */
    private boolean executeCraft(CraftingRecipe recipe, boolean toInput) {
        ItemStack resultCopy = recipe.getResult().copy();

        // SMART OUTPUT:
        // Jeśli przedmiot NIE miał trafić do input (czyli normalny craft),
        // sprawdzamy, czy nie jest potrzebny jako składnik innej receptury w automatyzacji.
        // Jeśli tak -> wymuszamy trafienie do INPUT (chyba że Input jest pełny, wtedy Output).
        if (!toInput) {
            if (isIngredientForAnyAutoRecipe(resultCopy)) {
                toInput = true;
            }
        }

        // Sprawdź czy jest miejsce na wynik
        ItemStack remaining;

        if (toInput) {
            remaining = insertIntoInput(resultCopy, true); // Simulate

            // Jeśli nie mieści się w Input (np. jest pełny), spróbujmy jednak Output
            if (!remaining.isEmpty()) {
                ItemStack remOutput = insertIntoOutput(resultCopy, true);
                if (remOutput.isEmpty()) {
                    toInput = false; // Zmiana decyzji - idzie do outputu bo w input nie ma miejsca
                    remaining = ItemStack.EMPTY; // Znaleźliśmy miejsce
                }
            }
        } else {
            remaining = insertIntoOutput(resultCopy, true); // Simulate
        }

        if (!remaining.isEmpty()) {
            return false; // Brak miejsca
        }

        // Zabierz składniki
        for (ItemStack ingredient : recipe.getIngredients()) {
            int toRemove = ingredient.getCount();
            for (int i = 0; i < inputInventory.getSlots() && toRemove > 0; i++) {
                ItemStack slot = inputInventory.getStackInSlot(i);
                if (ItemStack.areItemsEqual(ingredient, slot) &&
                    ItemStack.areItemStackTagsEqual(ingredient, slot)) {
                    int remove = Math.min(toRemove, slot.getCount());
                    inputInventory.extractItem(i, remove, false);
                    toRemove -= remove;
                }
            }
        }

        // Dodaj wynik
        if (toInput) {
            insertIntoInput(recipe.getResult().copy(), false);
        } else {
            insertIntoOutput(recipe.getResult().copy(), false);
        }
        markDirty();
        sendUpdate(); // Odswiez inwentarze u gracza
        return true;
    }

    /**
     * Sprawdza, czy dany przedmiot jest potrzebny jako składnik w innej aktywnej recepturze automatycznej.
     */
    private boolean isIngredientForAnyAutoRecipe(ItemStack stack) {
        for (int id : automationRecipes) {
            CraftingRecipe r = getRecipeById(id);
            if (r != null) {
                for (ItemStack ing : r.getIngredients()) {
                    if (ItemStack.areItemsEqual(ing, stack)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Sprawdza, czy dany przedmiot jest wynikiem (produktem końcowym) jakiejkolwiek receptury w automatyzacji.
     */
    private boolean isResultOfAnyAutoRecipe(ItemStack stack) {
        for (int id : automationRecipes) {
            CraftingRecipe r = getRecipeById(id);
            if (r != null) {
                if (ItemStack.areItemsEqual(r.getResult(), stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Sprawdza czy przedmiot powinien być odrzucony przez rury (jest końcowym produktem, nie składnikiem).
     */
    private boolean shouldRejectFromPipes(ItemStack stack) {
        // Odrzuć jeśli przedmiot jest wynikiem receptury ALE nie jest składnikiem żadnej innej
        return isResultOfAnyAutoRecipe(stack) && !isIngredientForAnyAutoRecipe(stack);
    }

    /**
     * Wstawia przedmiot do slotów wejściowych (dla łańcuchowego craftowania).
     *
     * @return to co się nie zmieściło
     */
    private ItemStack insertIntoInput(ItemStack stack, boolean simulate) {
        return ItemHandlerHelper.insertItemStacked(inputInventory, stack, simulate);
    }

    /**
     * Wstawia przedmiot do slotów wyjściowych.
     *
     * @return to co się nie zmieściło
     */
    private ItemStack insertIntoOutput(ItemStack stack, boolean simulate) {
        return ItemHandlerHelper.insertItemStacked(outputInventory, stack, simulate);
    }

    /**
     * Znajduje recepturę która tworzy dany przedmiot.
     */
    public CraftingRecipe findRecipeForResult(ItemStack result) {
        for (CraftingRecipe recipe : recipes) {
            if (recipe.matchesResult(result)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * Zwraca recepturę po ID.
     */
    public CraftingRecipe getRecipeById(int id) {
        for (CraftingRecipe recipe : recipes) {
            if (recipe.getId() == id) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * Zwraca listę dostępnych przedmiotów (z input inventory).
     */
    public List<ItemStack> getAvailableItems() {
        List<ItemStack> available = new ArrayList<>();
        for (int i = 0; i < inputInventory.getSlots(); i++) {
            ItemStack stack = inputInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                available.add(stack.copy());
            }
        }
        return available;
    }

    // ========== Zarządzanie recepturami ==========

    /**
     * Dodaje nowa recepture z aktualnej siatki i slotu wyniku.
     * @return Nowo utworzona receptura lub null jesli siatka lub wynik jest pusty
     */
    public CraftingRecipe addRecipeFromGridAndSlot() {
        ItemStack result = recipeResultSlot.getStackInSlot(0);
        if (result.isEmpty()) return null;

        List<ItemStack> ingredients = new ArrayList<>();
        for (int i = 0; i < recipeGrid.getSlots(); i++) {
            ItemStack stack = recipeGrid.getStackInSlot(i);
            if (!stack.isEmpty()) {
                // Szukaj czy juz mamy taki skladnik
                boolean found = false;
                for (ItemStack existing : ingredients) {
                    if (ItemStack.areItemsEqual(existing, stack) &&
                        ItemStack.areItemStackTagsEqual(existing, stack)) {
                        existing.grow(stack.getCount());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    ingredients.add(stack.copy());
                }
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

    /**
     * Dodaje nową recepturę z aktualnej siatki.
     *
     * @param result Wynik receptury
     * @return Nowo utworzona receptura lub null jeśli siatka jest pusta
     */
    public CraftingRecipe addRecipeFromGrid(ItemStack result) {
        if (result.isEmpty()) return null;

        List<ItemStack> ingredients = new ArrayList<>();
        for (int i = 0; i < recipeGrid.getSlots(); i++) {
            ItemStack stack = recipeGrid.getStackInSlot(i);
            if (!stack.isEmpty()) {
                // Szukaj czy już mamy taki składnik
                boolean found = false;
                for (ItemStack existing : ingredients) {
                    if (ItemStack.areItemsEqual(existing, stack) &&
                        ItemStack.areItemStackTagsEqual(existing, stack)) {
                        existing.grow(stack.getCount());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    ingredients.add(stack.copy());
                }
            }
        }

        if (ingredients.isEmpty()) return null;

        CraftingRecipe recipe = new CraftingRecipe(ingredients, result);
        recipe.setId(nextRecipeId++);
        recipes.add(recipe);
        markDirty();
        sendUpdate();
        return recipe;
    }

    /**
     * Usuwa recepturę po ID.
     */
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

    /**
     * Dodaje recepturę do kolejki craftowania.
     */
    public void addToQueue(int recipeId) {
        craftingQueue.add(recipeId);
        markDirty();
        sendUpdate();
    }

    /**
     * Dodaje recepturę do automatyzacji.
     */
    public void addToAutomation(int recipeId) {
        if (!automationRecipes.contains(recipeId)) {
            automationRecipes.add(recipeId);
            markDirty();
            sendUpdate();
        }
    }

    /**
     * Usuwa recepturę z automatyzacji.
     */
    public void removeFromAutomation(int recipeId) {
        automationRecipes.remove((Integer) recipeId);
        markDirty();
        sendUpdate();
    }

    /**
     * Sprawdza czy receptura jest w automatyzacji.
     */
    public boolean isInAutomation(int recipeId) {
        return automationRecipes.contains(recipeId);
    }

    /**
     * Aktualizuje podgląd wyniku na podstawie siatki.
     * Wyszukuje czy składniki na siatce pasują do jakiejś istniejącej receptury.
     */
    public void updateRecipePreview() {
        // Zbierz składniki z siatki
        List<ItemStack> gridItems = new ArrayList<>();
        for (int i = 0; i < recipeGrid.getSlots(); i++) {
            ItemStack stack = recipeGrid.getStackInSlot(i);
            if (!stack.isEmpty()) {
                // Agreguj
                boolean found = false;
                for (ItemStack existing : gridItems) {
                    if (ItemStack.areItemsEqual(existing, stack) &&
                        ItemStack.areItemStackTagsEqual(existing, stack)) {
                        existing.grow(stack.getCount());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    gridItems.add(stack.copy());
                }
            }
        }

        // Szukaj pasującej receptury
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

    /**
     * Sprawdza czy dwie listy składników są takie same.
     */
    private boolean ingredientsMatch(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;

        // Kopiujemy listy żeby móc je modyfikować
        List<ItemStack> aCopy = new ArrayList<>();
        for (ItemStack s : a) aCopy.add(s.copy());

        for (ItemStack bItem : b) {
            boolean found = false;
            for (int i = 0; i < aCopy.size(); i++) {
                ItemStack aItem = aCopy.get(i);
                if (ItemStack.areItemsEqual(aItem, bItem) &&
                    ItemStack.areItemStackTagsEqual(aItem, bItem) &&
                    aItem.getCount() == bItem.getCount()) {
                    aCopy.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return aCopy.isEmpty();
    }

    // ========== Gettery ==========

    public List<CraftingRecipe> getRecipes() {
        return Collections.unmodifiableList(recipes);
    }

    public List<Integer> getCraftingQueue() {
        return Collections.unmodifiableList(craftingQueue);
    }

    public List<Integer> getAutomationRecipes() {
        return Collections.unmodifiableList(automationRecipes);
    }

    public boolean isAutomationEnabled() {
        return automationEnabled;
    }

    public void setAutomationEnabled(boolean enabled) {
        this.automationEnabled = enabled;
        markDirty();
        sendUpdate();
    }

    public boolean isRoundRobinMode() {
        return roundRobinMode;
    }

    public void setRoundRobinMode(boolean roundRobinMode) {
        this.roundRobinMode = roundRobinMode;
        this.currentRoundRobinIndex = 0; // Reset index on change
        markDirty();
        sendUpdate();
    }

    public ItemStack getRecipeResultPreview() {
        return recipeResultPreview;
    }

    public IItemHandler getInputHandler() {
        return inputInventory;
    }

    public IItemHandler getOutputHandler() {
        return outputInventory;
    }

    public IItemHandler getRecipeGridHandler() {
        return recipeGrid;
    }

    public IItemHandler getRecipeResultSlotHandler() {
        return recipeResultSlot;
    }

    /**
     * Kopiuje unikalne receptury z innego craftera.
     * @return Liczba dodanych receptur
     */
    public int copyRecipesFrom(TileAutoCrafter other) {
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

    /**
     * Sprawdza czy dwie receptury są identyczne (ten sam wynik i składniki).
     */
    private boolean isSameRecipe(CraftingRecipe a, CraftingRecipe b) {
        // Sprawdź wynik
        if (!ItemStack.areItemsEqual(a.getResult(), b.getResult()) ||
            !ItemStack.areItemStackTagsEqual(a.getResult(), b.getResult()) ||
            a.getResult().getCount() != b.getResult().getCount()) {
            return false;
        }

        // Sprawdź składniki
        return ingredientsMatch(a.getIngredients(), b.getIngredients());
    }

    // ========== NBT ==========

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        if (compound.hasKey("Input")) {
            inputInventory.deserializeNBT(compound.getCompoundTag("Input"));
        }
        if (compound.hasKey("Output")) {
            outputInventory.deserializeNBT(compound.getCompoundTag("Output"));
        }
        if (compound.hasKey("RecipeGrid")) {
            recipeGrid.deserializeNBT(compound.getCompoundTag("RecipeGrid"));
        }
        if (compound.hasKey("RecipeResultSlot")) {
            recipeResultSlot.deserializeNBT(compound.getCompoundTag("RecipeResultSlot"));
        }

        recipes.clear();
        if (compound.hasKey("Recipes")) {
            NBTTagList recipeList = compound.getTagList("Recipes", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < recipeList.tagCount(); i++) {
                recipes.add(new CraftingRecipe(recipeList.getCompoundTagAt(i)));
            }
        }
        nextRecipeId = compound.getInteger("NextRecipeId");
        if (nextRecipeId == 0) nextRecipeId = 1;

        craftingQueue.clear();
        if (compound.hasKey("Queue")) {
            int[] queueArray = compound.getIntArray("Queue");
            for (int id : queueArray) {
                craftingQueue.add(id);
            }
        }

        automationRecipes.clear();
        if (compound.hasKey("AutomationRecipes")) {
            int[] autoArray = compound.getIntArray("AutomationRecipes");
            for (int id : autoArray) {
                automationRecipes.add(id);
            }
        }

        automationEnabled = compound.getBoolean("AutomationEnabled");
        roundRobinMode = compound.getBoolean("RoundRobinMode");
        currentRoundRobinIndex = compound.getInteger("RoundRobinIndex");

        if (compound.hasKey("ResultPreview")) {
            recipeResultPreview = new ItemStack(compound.getCompoundTag("ResultPreview"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        compound.setTag("Input", inputInventory.serializeNBT());
        compound.setTag("Output", outputInventory.serializeNBT());
        compound.setTag("RecipeGrid", recipeGrid.serializeNBT());
        compound.setTag("RecipeResultSlot", recipeResultSlot.serializeNBT());

        NBTTagList recipeList = new NBTTagList();
        for (CraftingRecipe recipe : recipes) {
            recipeList.appendTag(recipe.writeToNBT());
        }
        compound.setTag("Recipes", recipeList);
        compound.setInteger("NextRecipeId", nextRecipeId);

        int[] queueArray = new int[craftingQueue.size()];
        for (int i = 0; i < craftingQueue.size(); i++) {
            queueArray[i] = craftingQueue.get(i);
        }
        compound.setIntArray("Queue", queueArray);

        int[] autoArray = new int[automationRecipes.size()];
        int idx = 0;
        for (int id : automationRecipes) {
            autoArray[idx++] = id;
        }
        compound.setIntArray("AutomationRecipes", autoArray);

        compound.setBoolean("AutomationEnabled", automationEnabled);
        compound.setBoolean("RoundRobinMode", roundRobinMode);
        compound.setInteger("RoundRobinIndex", currentRoundRobinIndex);

        if (!recipeResultPreview.isEmpty()) {
            compound.setTag("ResultPreview", recipeResultPreview.writeToNBT(new NBTTagCompound()));
        }

        return compound;
    }

    // ========== Sync ==========

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

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }

    // ========== Capabilities ==========

    // Handler dla rur - wkładanie do input, wyjmowanie z output
    private final IItemHandler combinedHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return inputInventory.getSlots() + outputInventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            // Rury widzą tylko output - input jest ukryty
            if (slot < inputInventory.getSlots()) {
                return ItemStack.EMPTY; // Ukryj input od rur
            }
            return outputInventory.getStackInSlot(slot - inputInventory.getSlots());
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            // Odrzuć końcowe produkty - nie pozwól rurom wkładać ich z powrotem
            if (shouldRejectFromPipes(stack)) {
                return stack; // Zwróć stack - nie przyjmujemy
            }
            // Wkładanie ZAWSZE trafia do input
            return ItemHandlerHelper.insertItemStacked(inputInventory, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // Wyjmowanie tylko z output
            if (slot >= inputInventory.getSlots()) {
                return outputInventory.extractItem(slot - inputInventory.getSlots(), amount, simulate);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot < inputInventory.getSlots()) {
                return inputInventory.getSlotLimit(slot);
            }
            return outputInventory.getSlotLimit(slot - inputInventory.getSlots());
        }
    };

    // Handler tylko dla wkładania (input)
    private final IItemHandler inputOnlyHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return inputInventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inputInventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return inputInventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY; // Nie można wyjmować z input przez rury
        }

        @Override
        public int getSlotLimit(int slot) {
            return inputInventory.getSlotLimit(slot);
        }
    };

    // Handler tylko dla wyjmowania (output)
    private final IItemHandler outputOnlyHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return outputInventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return outputInventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack; // Nie można wkładać do output przez rury
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return outputInventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return outputInventory.getSlotLimit(slot);
        }
    };

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            // Wszystkie strony - wkładanie do input, wyciąganie z output
            return (T) combinedHandler;
        }
        return super.getCapability(capability, facing);
    }
}

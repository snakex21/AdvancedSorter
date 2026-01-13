package com.antigravity.advancedsorter.autocrafter;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Reprezentuje pojedynczą recepturę w Auto Crafterze.
 *
 * Receptura składa się z:
 * - Listy składników (ItemStack z ilością)
 * - Wyniku (ItemStack)
 * - Opcjonalnego ID dla łatwej identyfikacji
 */
public class CraftingRecipe {

    private final List<ItemStack> ingredients;
    private final ItemStack result;
    private int id;

    public CraftingRecipe(List<ItemStack> ingredients, ItemStack result) {
        this.ingredients = new ArrayList<>(ingredients);
        this.result = result.copy();
        this.id = 0;
    }

    public CraftingRecipe(NBTTagCompound nbt) {
        this.ingredients = new ArrayList<>();
        this.result = new ItemStack(nbt.getCompoundTag("Result"));
        this.id = nbt.getInteger("Id");

        NBTTagList ingredientList = nbt.getTagList("Ingredients", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < ingredientList.tagCount(); i++) {
            ItemStack stack = new ItemStack(ingredientList.getCompoundTagAt(i));
            if (!stack.isEmpty()) {
                ingredients.add(stack);
            }
        }
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("Id", id);
        nbt.setTag("Result", result.writeToNBT(new NBTTagCompound()));

        NBTTagList ingredientList = new NBTTagList();
        for (ItemStack ingredient : ingredients) {
            ingredientList.appendTag(ingredient.writeToNBT(new NBTTagCompound()));
        }
        nbt.setTag("Ingredients", ingredientList);

        return nbt;
    }

    public List<ItemStack> getIngredients() {
        return ingredients;
    }

    public ItemStack getResult() {
        return result;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Sprawdza czy dany ItemStack pasuje do wyniku tej receptury.
     */
    public boolean matchesResult(ItemStack stack) {
        return ItemStack.areItemsEqual(result, stack) &&
               ItemStack.areItemStackTagsEqual(result, stack);
    }

    /**
     * Sprawdza czy mamy wszystkie składniki w podanym inwentarzu.
     *
     * @param available Lista dostępnych przedmiotów
     * @return true jeśli mamy wszystkie składniki
     */
    public boolean canCraft(List<ItemStack> available) {
        // Tworzymy kopię listy dostępnych przedmiotów
        List<ItemStack> availableCopy = new ArrayList<>();
        for (ItemStack stack : available) {
            if (!stack.isEmpty()) {
                availableCopy.add(stack.copy());
            }
        }

        // Sprawdzamy każdy składnik
        for (ItemStack ingredient : ingredients) {
            int needed = ingredient.getCount();

            for (ItemStack avail : availableCopy) {
                if (ItemStack.areItemsEqual(ingredient, avail) &&
                    ItemStack.areItemStackTagsEqual(ingredient, avail)) {
                    int take = Math.min(needed, avail.getCount());
                    needed -= take;
                    avail.shrink(take);
                    if (needed <= 0) break;
                }
            }

            if (needed > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Zwraca listę brakujących składników.
     *
     * @param available Lista dostępnych przedmiotów
     * @return Lista brakujących składników
     */
    public List<ItemStack> getMissingIngredients(List<ItemStack> available) {
        List<ItemStack> missing = new ArrayList<>();

        // Tworzymy kopię listy dostępnych przedmiotów
        List<ItemStack> availableCopy = new ArrayList<>();
        for (ItemStack stack : available) {
            if (!stack.isEmpty()) {
                availableCopy.add(stack.copy());
            }
        }

        // Sprawdzamy każdy składnik
        for (ItemStack ingredient : ingredients) {
            int needed = ingredient.getCount();

            for (ItemStack avail : availableCopy) {
                if (ItemStack.areItemsEqual(ingredient, avail) &&
                    ItemStack.areItemStackTagsEqual(ingredient, avail)) {
                    int take = Math.min(needed, avail.getCount());
                    needed -= take;
                    avail.shrink(take);
                    if (needed <= 0) break;
                }
            }

            if (needed > 0) {
                ItemStack missingStack = ingredient.copy();
                missingStack.setCount(needed);
                missing.add(missingStack);
            }
        }
        return missing;
    }

    /**
     * Zwraca opis receptury do wyświetlenia.
     */
    public String getDisplayName() {
        return result.getDisplayName();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Recipe #").append(id).append(": ");
        for (int i = 0; i < ingredients.size(); i++) {
            if (i > 0) sb.append(" + ");
            ItemStack ing = ingredients.get(i);
            sb.append(ing.getCount()).append("x ").append(ing.getDisplayName());
        }
        sb.append(" -> ").append(result.getCount()).append("x ").append(result.getDisplayName());
        return sb.toString();
    }
}

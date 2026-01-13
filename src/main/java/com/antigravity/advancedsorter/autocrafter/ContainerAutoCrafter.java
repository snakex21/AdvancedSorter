package com.antigravity.advancedsorter.autocrafter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Container dla Auto Craftera.
 *
 * Sloty:
 * - 0-26: Input inventory (27 slotow) - materialy do craftowania
 * - 27-35: Output inventory (9 slotow) - wyniki craftowania
 * - 36-44: Recipe grid (9 slotow) - definiowanie receptury
 * - 45: Recipe result slot (1 slot) - wynik receptury
 * - 46-72: Player inventory (27 slotow)
 * - 73-81: Player hotbar (9 slotow)
 */
public class ContainerAutoCrafter extends Container {

    private final TileAutoCrafter tile;
    private final EntityPlayer player;

    // Pozycje slotow
    private static final int INPUT_START = 0;
    private static final int INPUT_END = 27;
    private static final int OUTPUT_START = 27;
    private static final int OUTPUT_END = 36;
    private static final int GRID_START = 36;
    private static final int GRID_END = 45;
    private static final int RESULT_SLOT = 45;
    private static final int PLAYER_INV_START = 46;
    private static final int PLAYER_INV_END = 73;
    private static final int HOTBAR_START = 73;
    private static final int HOTBAR_END = 82;

    public ContainerAutoCrafter(InventoryPlayer playerInv, TileAutoCrafter tile) {
        this.tile = tile;
        this.player = playerInv.player;

        IItemHandler inputHandler = tile.getInputHandler();
        IItemHandler outputHandler = tile.getOutputHandler();
        IItemHandler gridHandler = tile.getRecipeGridHandler();
        IItemHandler resultHandler = tile.getRecipeResultSlotHandler();

        // Input inventory (3 rows x 9 cols) - domyslnie ukryte
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new SlotItemHandler(inputHandler, col + row * 9, -1000, -1000));
            }
        }

        // Output inventory (3x3) - domyslnie ukryte
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlotToContainer(new SlotItemHandler(outputHandler, col + row * 3, -1000, -1000));
            }
        }

        // Recipe grid (3x3) - domyslnie ukryte
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlotToContainer(new SlotItemHandler(gridHandler, col + row * 3, -1000, -1000));
            }
        }

        // Recipe result slot (1 slot) - domyslnie ukryty
        addSlotToContainer(new SlotItemHandler(resultHandler, 0, -1000, -1000));

        // Player inventory - pozycja stala na dole GUI
        // Przesunieto z y=174 na y=180 (+6px)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 180 + row * 18));
            }
        }

        // Player hotbar
        // Przesunieto z y=232 na y=238 (+6px)
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 238));
        }
    }

    /**
     * Ustawia widocznosc slotow w zaleznosci od zakladki.
     *
     * @param tab 0=Receptury, 1=Craftowanie, 2=Automatyzacja
     */
    public void setVisibleTab(int tab) {
        switch (tab) {
            case 0: // Receptury - pokazuje grid (3x3) i result slot
                // Ukryj input i output
                hideSlots(INPUT_START, INPUT_END);
                hideSlots(OUTPUT_START, OUTPUT_END);

                // Recipe grid (3x3) - lewa strona pod zakladkami
                for (int i = GRID_START; i < GRID_END; i++) {
                    Slot slot = getSlot(i);
                    int idx = i - GRID_START;
                    slot.xPos = 8 + (idx % 3) * 18;
                    slot.yPos = 50 + (idx / 3) * 18;
                }

                // Result slot - po prawej od grida
                getSlot(RESULT_SLOT).xPos = 90;
                getSlot(RESULT_SLOT).yPos = 68;
                break;

            case 1: // Craftowanie - pokazuje input (materialy) i output (wyniki)
                // Ukryj grid i result
                hideSlots(GRID_START, GRID_END);
                getSlot(RESULT_SLOT).xPos = -1000;
                getSlot(RESULT_SLOT).yPos = -1000;

                // Input inventory (3x9) - gora GUI (y=42)
                for (int i = INPUT_START; i < INPUT_END; i++) {
                    Slot slot = getSlot(i);
                    int idx = i - INPUT_START;
                    slot.xPos = 8 + (idx % 9) * 18;
                    slot.yPos = 42 + (idx / 9) * 18;
                }

                // Output (1x9) - nad player inventory (y=158)
                for (int i = OUTPUT_START; i < OUTPUT_END; i++) {
                    Slot slot = getSlot(i);
                    int idx = i - OUTPUT_START;
                    slot.xPos = 8 + idx * 18;
                    slot.yPos = 158;
                }
                break;

            case 2: // Automatyzacja - pokazuje input i output
                // Ukryj grid i result
                hideSlots(GRID_START, GRID_END);
                getSlot(RESULT_SLOT).xPos = -1000;
                getSlot(RESULT_SLOT).yPos = -1000;

                // Input inventory (3x9) - gora GUI (y=42)
                for (int i = INPUT_START; i < INPUT_END; i++) {
                    Slot slot = getSlot(i);
                    int idx = i - INPUT_START;
                    slot.xPos = 8 + (idx % 9) * 18;
                    slot.yPos = 42 + (idx / 9) * 18;
                }

                // Output (1x9) - nad player inventory (y=158)
                for (int i = OUTPUT_START; i < OUTPUT_END; i++) {
                    Slot slot = getSlot(i);
                    int idx = i - OUTPUT_START;
                    slot.xPos = 8 + idx * 18;
                    slot.yPos = 158;
                }
                break;
        }
    }

    private void hideSlots(int start, int end) {
        for (int i = start; i < end; i++) {
            Slot slot = getSlot(i);
            slot.xPos = -1000;
            slot.yPos = -1000;
        }
    }

    public TileAutoCrafter getTile() {
        return tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return !playerIn.isSpectator() &&
               playerIn.getDistanceSq(tile.getPos().getX() + 0.5, tile.getPos().getY() + 0.5, tile.getPos().getZ() + 0.5) <= 64;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack slotStack = slot.getStack();
            result = slotStack.copy();

            // From Input/Output/Grid/Result -> Player
            if (index < PLAYER_INV_START) {
                if (!mergeItemStack(slotStack, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From Player -> Input or Grid
            else {
                if (!mergeItemStack(slotStack, INPUT_START, INPUT_END, false)) {
                    // Try grid if input is full
                    if (!mergeItemStack(slotStack, GRID_START, GRID_END, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (slotStack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (slotStack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, slotStack);
        }

        return result;
    }
}

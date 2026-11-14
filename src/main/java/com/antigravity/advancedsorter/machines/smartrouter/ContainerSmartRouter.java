package com.antigravity.advancedsorter.machines.smartrouter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerSmartRouter extends Container {

    private final TileSmartRouter tile;
    private boolean manualSlotsActive = false;

    public ContainerSmartRouter(EntityPlayer player, TileSmartRouter tile) {
        this.tile = tile;
        IItemHandler inventory = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        IItemHandler manualInv = tile.getManualInventory();

        // Input Slot (0) - For automation/pipes
        this.addSlotToContainer(new SlotItemHandler(inventory, 0, 80, 20) {
             // Optional: prevent manual interaction if we want to separate modes?
             // For now keep it accessible.
        });

        // Manual Inventory (9 slots: 3x3) - Initially hidden (off-screen)
        // GUI will move them when a preset is selected
        // Sloty blokują wkładanie gdy nie ma aktywnego presetu
        ContainerSmartRouter container = this;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                final int slotIndex = col + row * 3;
                this.addSlotToContainer(new SlotItemHandler(manualInv, slotIndex, -1000, -1000) {
                    @Override
                    public boolean isItemValid(ItemStack stack) {
                        // Blokuj wkładanie jeśli preset nie jest wybrany
                        if (!container.manualSlotsActive) {
                            return false;
                        }
                        // Blokuj wkładanie jeśli slot nie jest pusty (max 1 stack na slot)
                        if (!this.getStack().isEmpty()) {
                            return false;
                        }
                        return super.isItemValid(stack);
                    }
                });
            }
        }

        // Player Inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(player.inventory, j + i * 9 + 9, 8 + j * 18, 140 + i * 18));
            }
        }

        // Player Hotbar
        for (int k = 0; k < 9; ++k) {
            this.addSlotToContainer(new Slot(player.inventory, k, 8 + k * 18, 198));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return !playerIn.isDead;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            // Slots:
            // 0: Automation Input
            // 1-9: Manual Input (9 slots)
            // 10-36: Player Inventory
            // 37-45: Hotbar

            if (index < 10) { // From Tile (Auto or Manual) to Player
                if (!this.mergeItemStack(itemstack1, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
            } else { // From Player to Tile
                // Tylko przenoś do Manual slots jeśli są aktywne
                if (manualSlotsActive) {
                    // Sprawdź czy jest wolny slot manualny
                    boolean transferred = false;
                    for (int i = 1; i <= 9; i++) {
                        Slot manualSlot = this.inventorySlots.get(i);
                        if (!manualSlot.getHasStack() && manualSlot.isItemValid(itemstack1)) {
                            ItemStack toInsert = itemstack1.copy();
                            manualSlot.putStack(toInsert);
                            itemstack1.setCount(0);
                            transferred = true;
                            break;
                        }
                    }
                    if (!transferred) {
                        // Wszystkie sloty zajęte - nie rób nic
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Manual sloty nieaktywne - tylko Automation slot
                    if (!this.mergeItemStack(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
        }

        return itemstack;
    }

    // Helper to move slots dynamically
    public void setManualSlotsVisible(boolean visible) {
        // Ustaw flagę aktywności - blokuje/odblokowuje wkładanie przedmiotów
        this.manualSlotsActive = visible;

        int startX = 62; // Centered-ish
        int startY = 40;

        for (int i = 1; i <= 9; i++) {
             Slot slot = this.inventorySlots.get(i);
             if (visible) {
                 int idx = i - 1;
                 slot.xPos = startX + (idx % 3) * 18;
                 slot.yPos = startY + (idx / 3) * 18;
             } else {
                 slot.xPos = -1000;
                 slot.yPos = -1000;
             }
        }
    }

    public boolean areManualSlotsActive() {
        return manualSlotsActive;
    }
}

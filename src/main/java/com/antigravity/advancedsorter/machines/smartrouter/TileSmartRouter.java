package com.antigravity.advancedsorter.machines.smartrouter;

import com.antigravity.advancedsorter.pipes.teleport.TileTeleportPipe;
import com.antigravity.advancedsorter.util.TeleportRegistry;
import com.antigravity.advancedsorter.util.PendingTeleportRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TileSmartRouter extends TileEntity implements ITickable {

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    public List<SmartRouterRule> rules = new ArrayList<>();
    private int cooldown = 0;

    // OPTYMALIZACJA: Timer do ponawiania prób manualnego wysyłania (zamiast co tick)
    private int manualRetryTimer = 0;
    private static final int RETRY_INTERVAL = 20;

    // Manual Sending Inventory (3x3)
    // Items placed here are immediately teleported to the selected rule's frequency
    private final ItemStackHandler manualInventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            if (world != null && !world.isRemote) {
                // Natychmiast próbuj wysłać przedmiot
                tryManualSend(slot);
                // Reset timer to ensure update loop checks again soon if needed
                manualRetryTimer = 0;
            }
            markDirty();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            // Blokuj wkładanie jeśli nie ma wybranego presetu
            if (selectedRuleIndex < 0 || selectedRuleIndex >= rules.size()) {
                return stack; // Zwróć stack - nie można włożyć
            }
            // Blokuj wkładanie jeśli slot nie jest pusty
            if (!getStackInSlot(slot).isEmpty()) {
                return stack; // Zwróć stack - slot zajęty
            }
            return super.insertItem(slot, stack, simulate);
        }
    };

    private int selectedRuleIndex = -1;

    // Round Robin tracking per frequency
    // Simplified: we will just iterate for now, or implement a map <freq, index> if needed later.
    // For now, standard list iteration is fine as TeleportRegistry returns list.

    // OPTYMALIZACJA: Cache dla ostatnio dopasowanej reguły
    private int cachedRuleIndex = -1;
    private ItemStack cachedStack = ItemStack.EMPTY;

    @Override
    public void update() {
        if (world.isRemote) return;

        // OPTYMALIZACJA: Manual sending - retry logic
        if (manualRetryTimer > 0) {
            manualRetryTimer--;
        } else {
            boolean hasItems = false;
            boolean success = false;
            for (int i = 0; i < manualInventory.getSlots(); i++) {
                if (!manualInventory.getStackInSlot(i).isEmpty()) {
                    hasItems = true;
                    if (tryManualSend(i)) {
                        success = true;
                    }
                }
            }
            // Jeśli mamy przedmioty, ale nic nie udało się wysłać (np. brak odbiornika), czekamy
            if (hasItems && !success) {
                manualRetryTimer = RETRY_INTERVAL;
            } else if (!hasItems) {
                // Jeśli nie ma przedmiotów, również czekamy (nie skanuj pustego EQ co tick)
                // onContentsChanged obudzi nas jeśli coś wpadnie
                manualRetryTimer = RETRY_INTERVAL;
            }
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        ItemStack stack = inventory.getStackInSlot(0);
        if (!stack.isEmpty()) {

            // Sprawdź cache
            if (cachedRuleIndex >= 0 && cachedRuleIndex < rules.size() && !cachedStack.isEmpty() &&
                ItemStack.areItemsEqual(stack, cachedStack) && ItemStack.areItemStackTagsEqual(stack, cachedStack)) {

                SmartRouterRule rule = rules.get(cachedRuleIndex);
                // Upewnij się że reguła nadal pasuje (mogła zostać zmieniona w GUI)
                if (rule.matches(stack)) {
                    if (tryTeleport(stack, rule.frequency)) {
                        inventory.setStackInSlot(0, ItemStack.EMPTY);
                        cooldown = 10;
                        return;
                    }
                    // Jeśli teleport się nie udał (np. brak odbiornika), to i tak była dobra reguła.
                    // Czekamy na cooldown (ustawiony niżej)
                } else {
                    // Reguła przestała pasować - inwaliduj cache
                    cachedRuleIndex = -1;
                    cachedStack = ItemStack.EMPTY;
                }
            } else {
                // Item się zmienił lub brak cache
                cachedRuleIndex = -1;
                cachedStack = ItemStack.EMPTY;
            }

            // Jeśli cache nie zadziałał, szukaj normalnie
            if (cachedRuleIndex == -1) {
                for (int i = 0; i < rules.size(); i++) {
                    SmartRouterRule rule = rules.get(i);
                    if (rule.matches(stack)) {
                        // Znaleziono nową regułę - zapisz do cache
                        cachedRuleIndex = i;
                        cachedStack = stack.copy(); // Kopia do porównań

                        if (tryTeleport(stack, rule.frequency)) {
                            inventory.setStackInSlot(0, ItemStack.EMPTY);
                            cooldown = 10;
                            return;
                        }
                        break; // Znaleziono pasującą, ale teleport nieudany. Nie szukaj dalej.
                    }
                }
            }

            // OPTYMALIZACJA: Jeśli nie znaleziono reguły lub nie udało się wysłać, czekaj dłużej
            // Unika spamowania sprawdzaniem reguł/teleportacji co tick dla zablokowanego przedmiotu
            cooldown = 20;
        } else {
            // Slot pusty - wyczyść cache
            if (cachedRuleIndex != -1) {
                cachedRuleIndex = -1;
                cachedStack = ItemStack.EMPTY;
            }
        }
    }

    private boolean tryManualSend(int slot) {
        if (selectedRuleIndex < 0 || selectedRuleIndex >= rules.size()) return false;

        ItemStack stack = manualInventory.getStackInSlot(slot);
        if (stack.isEmpty()) return false;

        SmartRouterRule rule = rules.get(selectedRuleIndex);

        if (tryTeleport(stack, rule.frequency)) {
             manualInventory.setStackInSlot(slot, ItemStack.EMPTY);
             return true;
        }
        return false;
    }

    private boolean tryTeleport(ItemStack stack, int frequency) {
        TeleportRegistry registry = TeleportRegistry.get(world);
        List<TeleportRegistry.TeleportLocation> receivers = registry.getReceivers(frequency);

        if (receivers.isEmpty()) return false;

        // Try to send to first available receiver (could be improved with RR later)
        for (TeleportRegistry.TeleportLocation loc : receivers) {
            // Don't send to self (though unlikely as this is a router not a pipe)
            if (loc.dimension == world.provider.getDimension() && loc.pos.equals(pos)) continue;

            // Pobierz świat docelowy
            WorldServer targetWorld = DimensionManager.getWorld(loc.dimension);

            // Jeśli wymiar nie jest załadowany - kolejkuj przedmiot
            if (targetWorld == null) {
                PendingTeleportRegistry.get(world).queueItem(loc.dimension, loc.pos, stack.copy());
                return true; // Przedmiot "wysłany" (do kolejki)
            }

            // Jeśli chunk nie jest załadowany - kolejkuj przedmiot
            if (!targetWorld.isBlockLoaded(loc.pos)) {
                PendingTeleportRegistry.get(world).queueItem(loc.dimension, loc.pos, stack.copy());
                return true; // Przedmiot "wysłany" (do kolejki)
            }

            // Chunk jest załadowany - teleportuj bezpośrednio
            TileEntity te = targetWorld.getTileEntity(loc.pos);
            if (te instanceof TileTeleportPipe) {
                TileTeleportPipe targetPipe = (TileTeleportPipe) te;
                if (targetPipe.getFrequency() == frequency) {
                    targetPipe.receiveItem(stack.copy(), null, true);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        manualInventory.deserializeNBT(compound.getCompoundTag("ManualInventory"));

        rules.clear();
        if (compound.hasKey("Rules")) {
            NBTTagList list = compound.getTagList("Rules", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                SmartRouterRule rule = new SmartRouterRule();
                rule.deserializeNBT(list.getCompoundTagAt(i));
                rules.add(rule);
            }
        }
        selectedRuleIndex = compound.getInteger("SelectedRule");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setTag("ManualInventory", manualInventory.serializeNBT());

        NBTTagList list = new NBTTagList();
        for (SmartRouterRule rule : rules) {
            list.appendTag(rule.serializeNBT());
        }
        compound.setTag("Rules", list);
        compound.setInteger("SelectedRule", selectedRuleIndex);
        return compound;
    }

    public void setSelectedRuleIndex(int index) {
        this.selectedRuleIndex = index;
        markDirty();
        // Trigger manual send check immediately if we switched to a valid rule and have items
        if (index >= 0) {
            for (int i = 0; i < manualInventory.getSlots(); i++) {
                if (!manualInventory.getStackInSlot(i).isEmpty()) {
                    tryManualSend(i);
                }
            }
        }
    }

    public int getSelectedRuleIndex() {
        return selectedRuleIndex;
    }

    public ItemStackHandler getManualInventory() {
        return manualInventory;
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
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }
}

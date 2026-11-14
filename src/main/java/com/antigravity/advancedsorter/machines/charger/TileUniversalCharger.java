package com.antigravity.advancedsorter.machines.charger;

import mekanism.api.energy.IStrictEnergyAcceptor;
import mekanism.api.energy.IStrictEnergyStorage;
import mekanism.api.gas.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Optional.InterfaceList({
    @Optional.Interface(iface = "mekanism.api.energy.IStrictEnergyAcceptor", modid = "mekanism"),
    @Optional.Interface(iface = "mekanism.api.energy.IStrictEnergyStorage", modid = "mekanism")
})
public class TileUniversalCharger extends TileEntity implements IStrictEnergyAcceptor, IStrictEnergyStorage {

    // OPTYMALIZACJA: Cache dla refleksji
    private static Field ENERGY_FIELD;

    // IC2 Cache
    private static Method ic2GetInstance;
    private static Method ic2AddTile;
    private static Method ic2RemoveTile;
    private static Object ic2EnergyNetInstance; // Cache instance too if singleton? usually accessed via instance()
    private static Object ic2ManagerInstance;
    private static Method ic2ChargeMethod;

    // Mekanism Cache
    private static Class<?> mekEnergizedItemClass;
    private static Method mekGetEnergy;
    private static Method mekGetMaxEnergy;
    private static Method mekSetEnergy;

    // Mekanism Gas Capability
    @CapabilityInject(IGasHandler.class)
    public static Capability<IGasHandler> GAS_HANDLER_CAPABILITY = null;

    // Huge buffer for fast charging - allow both receive and extract
    private final EnergyStorage energyStorage = new EnergyStorage(1000000, 50000, 50000); // 1M RF, 50k IO in/out
    private final FluidTank fluidTank = new FluidTank(32000); // 32 Buckets buffer

    // Gas tank for Mekanism gases (Oxygen, Hydrogen, etc.)
    private final GasTank gasTank = new GasTank(32000);

    // Config
    private static final int CHARGE_RATE = 10000; // RF/tick per item
    private static final int FLUID_RATE = 1000; // mB/tick per item
    private static final int GAS_RATE = 1000; // mB/tick per item

    // Conversion rate: 1 RF = 2.5 Joules (Mekanism standard)
    private static final double RF_TO_JOULES = 2.5;
    // Conversion rate: 1 EU = 4 RF (common conversion)
    private static final double EU_TO_RF = 4.0;

    // IC2 integration
    private static boolean ic2Available = false;
    private boolean ic2Registered = false;

    static {
        // Energy Storage Field
        try {
            ENERGY_FIELD = EnergyStorage.class.getDeclaredField("energy");
            ENERGY_FIELD.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // IC2
        try {
            Class.forName("ic2.api.energy.tile.IEnergySink");
            ic2Available = true;

            Class<?> energyNetClass = Class.forName("ic2.api.energy.EnergyNet");
            ic2GetInstance = energyNetClass.getMethod("instance");
            // Invoke instance once if possible, but it might be per-world or similar. Safest to invoke method each time or check.
            // Actually EnergyNet.instance is static singleton usually.
            // But let's cache the methods at least.
            ic2AddTile = energyNetClass.getMethod("addTile", TileEntity.class);
            ic2RemoveTile = energyNetClass.getMethod("removeTile", TileEntity.class);

            Class<?> electricItemClass = Class.forName("ic2.api.item.ElectricItem");
            Field managerField = electricItemClass.getField("manager");
            ic2ManagerInstance = managerField.get(null);
            if (ic2ManagerInstance != null) {
                ic2ChargeMethod = ic2ManagerInstance.getClass().getMethod("charge", ItemStack.class, double.class, int.class, boolean.class, boolean.class);
            }

        } catch (Exception ignored) {
            ic2Available = false;
        }

        // Mekanism
        try {
            mekEnergizedItemClass = Class.forName("mekanism.api.energy.IEnergizedItem");
            mekGetEnergy = mekEnergizedItemClass.getMethod("getEnergy", ItemStack.class);
            mekGetMaxEnergy = mekEnergizedItemClass.getMethod("getMaxEnergy", ItemStack.class);
            mekSetEnergy = mekEnergizedItemClass.getMethod("setEnergy", ItemStack.class, double.class);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!world.isRemote && ic2Available && !ic2Registered) {
            registerIC2Tile();
            ic2Registered = true;
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (!world.isRemote && ic2Available && ic2Registered) {
            unregisterIC2Tile();
            ic2Registered = false;
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (!world.isRemote && ic2Available && ic2Registered) {
            unregisterIC2Tile();
            ic2Registered = false;
        }
    }

    private void registerIC2Tile() {
        if (ic2GetInstance != null && ic2AddTile != null) {
            try {
                Object energyNet = ic2GetInstance.invoke(null);
                ic2AddTile.invoke(energyNet, this);
            } catch (Exception ignored) {}
        }
    }

    private void unregisterIC2Tile() {
        if (ic2GetInstance != null && ic2RemoveTile != null) {
            try {
                Object energyNet = ic2GetInstance.invoke(null);
                ic2RemoveTile.invoke(energyNet, this);
            } catch (Exception ignored) {}
        }
    }

    // ========== IC2 IEnergySink methods (called via reflection by IC2) ==========

    public double getDemandedEnergy() {
        int space = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
        return space / EU_TO_RF;
    }

    public int getSinkTier() {
        return 4; // Accept up to EV (512 EU/t)
    }

    public double injectEnergy(EnumFacing directionFrom, double amount, double voltage) {
        int rfAmount = (int) (amount * EU_TO_RF);
        int space = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
        int toAccept = Math.min(rfAmount, space);

        if (ENERGY_FIELD != null) {
            try {
                ENERGY_FIELD.setInt(energyStorage, energyStorage.getEnergyStored() + toAccept);
            } catch (Exception ignored) {}
        }
        markDirty();

        // Return leftover EU
        int leftoverRF = rfAmount - toAccept;
        return leftoverRF / EU_TO_RF;
    }

    public boolean acceptsEnergyFrom(TileEntity emitter, EnumFacing direction) {
        return true;
    }

    public void chargePlayer(EntityPlayer player) {
        // Charge Energy (RF/FE, Mekanism Joules, IC2 EU)
        if (energyStorage.getEnergyStored() > 0) {
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    chargeItemEnergy(stack);
                    chargeItemMekanismEnergy(stack);
                    if (ic2Available) chargeItemIC2Energy(stack);
                }
            }
        }

        // Fill Fluids
        if (fluidTank.getFluidAmount() > 0) {
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    fillItemFluid(stack);
                }
            }
        }

        // Fill Gases (Mekanism)
        if (gasTank.getStored() > 0) {
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    fillItemGas(stack);
                }
            }
        }
    }

    private void chargeItemEnergy(ItemStack stack) {
        if (stack.hasCapability(CapabilityEnergy.ENERGY, null)) {
            IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY, null);
            if (storage != null && storage.canReceive()) {
                int maxExtract = Math.min(CHARGE_RATE, energyStorage.getEnergyStored());
                int received = storage.receiveEnergy(maxExtract, false);
                energyStorage.extractEnergy(received, false);
            }
        }
    }

    /**
     * Charge items that use Mekanism's energy system (Joules).
     * Uses reflection to support Mekanism items without hard dependency.
     */
    private void chargeItemMekanismEnergy(ItemStack stack) {
        try {
            if (mekEnergizedItemClass != null && mekEnergizedItemClass.isInstance(stack.getItem())) {
                if (mekGetEnergy != null && mekGetMaxEnergy != null && mekSetEnergy != null) {
                    double currentEnergy = (Double) mekGetEnergy.invoke(stack.getItem(), stack);
                    double maxEnergy = (Double) mekGetMaxEnergy.invoke(stack.getItem(), stack);
                    double space = maxEnergy - currentEnergy;

                    if (space > 0) {
                        double joulesAvailable = energyStorage.getEnergyStored() * RF_TO_JOULES;
                        double maxJoules = Math.min(CHARGE_RATE * RF_TO_JOULES, joulesAvailable);
                        double toTransfer = Math.min(maxJoules, space);

                        mekSetEnergy.invoke(stack.getItem(), stack, currentEnergy + toTransfer);

                        int rfUsed = (int) Math.ceil(toTransfer / RF_TO_JOULES);
                        energyStorage.extractEnergy(rfUsed, false);
                    }
                }
            }
        } catch (Exception ignored) {
            // Mekanism not loaded or incompatible version - silently ignore
        }
    }

    /**
     * Charge items that use IC2's energy system (EU).
     * Uses reflection to interact with IC2 API.
     */
    private void chargeItemIC2Energy(ItemStack stack) {
        try {
            if (ic2ManagerInstance != null && ic2ChargeMethod != null) {
                double euAvailable = energyStorage.getEnergyStored() / EU_TO_RF;
                double maxChargeEU = Math.min(CHARGE_RATE / EU_TO_RF, euAvailable);

                // Tier 4 (EV)
                // double charge(ItemStack itemStack, double amount, int tier, boolean ignoreTransferLimit, boolean simulate)
                double charged = (Double) ic2ChargeMethod.invoke(ic2ManagerInstance, stack, maxChargeEU, 4, false, false);

                if (charged > 0) {
                    int rfUsed = (int) Math.ceil(charged * EU_TO_RF);
                    energyStorage.extractEnergy(rfUsed, false);
                }
            }
        } catch (Exception ignored) {
            // IC2 API mismatch or not loaded
        }
    }

    private void fillItemFluid(ItemStack stack) {
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(stack);
        if (handler != null) {
            FluidStack resource = fluidTank.getFluid();
            if (resource != null) {
                // Try to fill the item with the fluid we have
                // We simulate first to see how much it takes
                int filled = handler.fill(resource, false);
                if (filled > 0) {
                    // Limit by rate
                    int toFill = Math.min(filled, FLUID_RATE);
                    // Limit by available
                    toFill = Math.min(toFill, fluidTank.getFluidAmount());

                    if (toFill > 0) {
                        FluidStack drainResource = new FluidStack(resource.getFluid(), toFill);
                        int actuallyFilled = handler.fill(drainResource, true);
                        if (actuallyFilled > 0) {
                            fluidTank.drain(actuallyFilled, true);
                        }
                    }
                }
            }
        }
    }

    /**
     * Fill items that can store Mekanism gases (like Jetpacks, Scuba Tanks, etc.)
     */
    private void fillItemGas(ItemStack stack) {
        if (GAS_HANDLER_CAPABILITY == null) return;

        try {
            // Check if item has gas handler capability
            if (stack.hasCapability(GAS_HANDLER_CAPABILITY, null)) {
                IGasHandler handler = stack.getCapability(GAS_HANDLER_CAPABILITY, null);
                if (handler != null && gasTank.getGas() != null) {
                    Gas gasType = gasTank.getGasType();
                    if (handler.canReceiveGas(null, gasType)) {
                        int maxTransfer = Math.min(GAS_RATE, gasTank.getStored());
                        GasStack toTransfer = new GasStack(gasType, maxTransfer);

                        int received = handler.receiveGas(null, toTransfer, true);
                        if (received > 0) {
                            gasTank.draw(received, true);
                        }
                    }
                }
            }

            // Also check for IGasItem interface (direct item implementation)
            if (stack.getItem() instanceof IGasItem) {
                IGasItem gasItem = (IGasItem) stack.getItem();
                if (gasTank.getGas() != null) {
                    Gas gasType = gasTank.getGasType();
                    if (gasItem.canReceiveGas(stack, gasType)) {
                        int maxTransfer = Math.min(GAS_RATE, gasTank.getStored());

                        // IGasItem.addGas returns remaining gas, not received amount
                        GasStack toAdd = new GasStack(gasType, maxTransfer);
                        GasStack currentGas = gasItem.getGas(stack);
                        int maxGas = gasItem.getMaxGas(stack);
                        int currentAmount = currentGas != null ? currentGas.amount : 0;
                        int space = maxGas - currentAmount;

                        if (space > 0) {
                            int toTransfer = Math.min(maxTransfer, space);
                            gasItem.setGas(stack, new GasStack(gasType, currentAmount + toTransfer));
                            gasTank.draw(toTransfer, true);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Mekanism not loaded or incompatible version - silently ignore
        }
    }

    // ========== Mekanism Energy (IStrictEnergyStorage) ==========

    @Override
    public double getEnergy() {
        return energyStorage.getEnergyStored() * RF_TO_JOULES;
    }

    @Override
    public void setEnergy(double energy) {
        int rfAmount = (int) (energy / RF_TO_JOULES);
        if (ENERGY_FIELD != null) {
            try {
                ENERGY_FIELD.setInt(energyStorage, Math.min(rfAmount, energyStorage.getMaxEnergyStored()));
            } catch (Exception ignored) {}
        }
        markDirty();
    }

    @Override
    public double getMaxEnergy() {
        return energyStorage.getMaxEnergyStored() * RF_TO_JOULES;
    }

    // ========== Mekanism Energy Acceptor ==========

    @Override
    public double acceptEnergy(EnumFacing side, double amount, boolean simulate) {
        int rfAmount = (int) (amount / RF_TO_JOULES);
        int space = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
        int toAccept = Math.min(rfAmount, Math.min(space, 50000)); // 50k RF/tick transfer rate

        if (!simulate) {
            if (ENERGY_FIELD != null) {
                try {
                    ENERGY_FIELD.setInt(energyStorage, energyStorage.getEnergyStored() + toAccept);
                } catch (Exception ignored) {}
            }
            markDirty();
        }

        return toAccept * RF_TO_JOULES;
    }

    @Override
    public boolean canReceiveEnergy(EnumFacing side) {
        return true;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return true;
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return true;

        // Mekanism capabilities - check safely
        if (isMekanismLoaded()) {
            try {
                if (GAS_HANDLER_CAPABILITY != null && capability == GAS_HANDLER_CAPABILITY) return true;
            } catch (Throwable t) {
                // Ignore errors
            }
        }

        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(energyStorage);
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluidTank);
        }

        // Mekanism capabilities - check safely
        if (isMekanismLoaded()) {
            try {
                if (GAS_HANDLER_CAPABILITY != null && capability == GAS_HANDLER_CAPABILITY) {
                    // Wrap gas tank as IGasHandler
                    return GAS_HANDLER_CAPABILITY.cast(new IGasHandler() {
                        @Override
                        public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
                            return gasTank.receive(stack, doTransfer);
                        }

                        @Override
                        public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
                            return gasTank.draw(amount, doTransfer);
                        }

                        @Override
                        public boolean canReceiveGas(EnumFacing side, Gas type) {
                            return gasTank.canReceive(type);
                        }

                        @Override
                        public boolean canDrawGas(EnumFacing side, Gas type) {
                            return gasTank.canDraw(type);
                        }

                        @Override
                        public GasTankInfo[] getTankInfo() {
                            return new GasTankInfo[]{gasTank};
                        }
                    });
                }
            } catch (Throwable t) {
                // Ignore errors
            }
        }

        return super.getCapability(capability, facing);
    }

    private static Boolean mekanismLoaded = null;
    private boolean isMekanismLoaded() {
        if (mekanismLoaded == null) {
            try {
                Class.forName("mekanism.common.capabilities.Capabilities");
                mekanismLoaded = true;
            } catch (Throwable t) {
                mekanismLoaded = false;
            }
        }
        return mekanismLoaded;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Energy", energyStorage.getEnergyStored());
        compound.setTag("Fluid", fluidTank.writeToNBT(new NBTTagCompound()));
        compound.setTag("Gas", gasTank.write(new NBTTagCompound()));
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        int energy = compound.getInteger("Energy");
        if (ENERGY_FIELD != null) {
            try {
                ENERGY_FIELD.setInt(energyStorage, energy);
            } catch (Exception e) {}
        }

        if (compound.hasKey("Fluid")) {
            fluidTank.readFromNBT(compound.getCompoundTag("Fluid"));
        }

        if (compound.hasKey("Gas")) {
            gasTank.read(compound.getCompoundTag("Gas"));
        }
    }
}

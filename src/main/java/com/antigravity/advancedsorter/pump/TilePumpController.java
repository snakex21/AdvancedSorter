package com.antigravity.advancedsorter.pump;

import com.antigravity.advancedsorter.util.PumpRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Tile entity for pump controller - controls extraction pipes remotely by
 * frequency.
 */
public class TilePumpController extends TileEntity implements ITickable {

    public static class PumpPreset {
        public String name;
        public int frequency;
        public boolean enabled;

        public PumpPreset(String name, int frequency, boolean enabled) {
            this.name = name;
            this.frequency = frequency;
            this.enabled = enabled;
        }

        public NBTTagCompound writeToNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("name", name);
            tag.setInteger("frequency", frequency);
            tag.setBoolean("enabled", enabled);
            return tag;
        }

        public static PumpPreset readFromNBT(NBTTagCompound tag) {
            return new PumpPreset(
                    tag.getString("name"),
                    tag.getInteger("frequency"),
                    tag.getBoolean("enabled"));
        }
    }

    private List<PumpPreset> presets = new ArrayList<>();

    public TilePumpController() {
        // Default preset will be added in readFromNBT if missing
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }

        // Update registry with our pumping state for each preset
        PumpRegistry registry = PumpRegistry.get(world);
        for (PumpPreset preset : presets) {
            registry.setPumpingEnabled(preset.frequency, preset.enabled);
        }
    }

    public List<PumpPreset> getPresets() {
        return presets;
    }

    public void setPresets(List<PumpPreset> presets) {
        // Disable pumping for old frequencies before updating
        if (world != null && !world.isRemote) {
            PumpRegistry registry = PumpRegistry.get(world);
            for (PumpPreset preset : this.presets) {
                registry.setPumpingEnabled(preset.frequency, false);
            }
        }

        this.presets = presets;
        markDirty();
        sendUpdate();
    }

    public void addPreset(String name, int frequency) {
        presets.add(new PumpPreset(name, Math.max(0, Math.min(99, frequency)), false));
        markDirty();
        sendUpdate();
    }

    public void removePreset(int index) {
        if (index >= 0 && index < presets.size()) {
            PumpPreset removed = presets.remove(index);
            if (world != null && !world.isRemote) {
                PumpRegistry.get(world).setPumpingEnabled(removed.frequency, false);
            }
            markDirty();
            sendUpdate();
        }
    }

    public void togglePreset(int index) {
        if (index >= 0 && index < presets.size()) {
            presets.get(index).enabled = !presets.get(index).enabled;
            markDirty();
            sendUpdate();
        }
    }

    public int getConnectedPipeCount(int frequency) {
        if (world != null && !world.isRemote) {
            return PumpRegistry.get(world).getPipeCount(frequency);
        }
        return 0;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        // Turn off pumping for all presets when controller is removed
        if (world != null && !world.isRemote) {
            PumpRegistry registry = PumpRegistry.get(world);
            for (PumpPreset preset : presets) {
                registry.setPumpingEnabled(preset.frequency, false);
            }
        }
    }

    private void sendUpdate() {
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        presets.clear();
        if (compound.hasKey("presets", Constants.NBT.TAG_LIST)) {
            NBTTagList list = compound.getTagList("presets", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                presets.add(PumpPreset.readFromNBT(list.getCompoundTagAt(i)));
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        NBTTagList list = new NBTTagList();
        for (PumpPreset preset : presets) {
            list.appendTag(preset.writeToNBT());
        }
        compound.setTag("presets", list);
        return compound;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(super.getUpdateTag());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }

    @Override
    public net.minecraft.network.play.server.SPacketUpdateTileEntity getUpdatePacket() {
        return new net.minecraft.network.play.server.SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(net.minecraft.network.NetworkManager net,
            net.minecraft.network.play.server.SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }
}

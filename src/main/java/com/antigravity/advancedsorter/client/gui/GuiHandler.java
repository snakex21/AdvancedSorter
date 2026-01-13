package com.antigravity.advancedsorter.client.gui;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.container.ContainerAdvancedSorter;
import com.antigravity.advancedsorter.pipes.directional.ContainerDirectionalPipe;
import com.antigravity.advancedsorter.pipes.directional.GuiDirectionalPipe;
import com.antigravity.advancedsorter.pipes.directional.TileDirectionalPipe;
import com.antigravity.advancedsorter.pipes.teleport.ContainerTeleportPipe;
import com.antigravity.advancedsorter.pipes.teleport.GuiTeleportPipe;
import com.antigravity.advancedsorter.pipes.teleport.TileTeleportPipe;
import com.antigravity.advancedsorter.pipes.fluid.directional.ContainerDirectionalFluidPipe;
import com.antigravity.advancedsorter.pipes.fluid.directional.GuiDirectionalFluidPipe;
import com.antigravity.advancedsorter.pipes.fluid.directional.TileDirectionalFluidPipe;
import com.antigravity.advancedsorter.pipes.fluid.teleport.ContainerTeleportFluidPipe;
import com.antigravity.advancedsorter.pipes.fluid.teleport.GuiTeleportFluidPipe;
import com.antigravity.advancedsorter.pipes.fluid.teleport.TileTeleportFluidPipe;
import com.antigravity.advancedsorter.pipes.gas.teleport.ContainerTeleportGasPipe;
import com.antigravity.advancedsorter.pipes.gas.teleport.GuiTeleportGasPipe;
import com.antigravity.advancedsorter.pipes.gas.teleport.TileTeleportGasPipe;
import com.antigravity.advancedsorter.pipes.fluid.extraction.ContainerExtractionFluidPipe;
import com.antigravity.advancedsorter.pipes.fluid.extraction.GuiExtractionFluidPipe;
import com.antigravity.advancedsorter.pipes.fluid.extraction.TileExtractionFluidPipe;
import com.antigravity.advancedsorter.pump.ContainerPumpController;
import com.antigravity.advancedsorter.pump.GuiPumpController;
import com.antigravity.advancedsorter.pump.TilePumpController;
import com.antigravity.advancedsorter.pump.ContainerAdvancedPump;
import com.antigravity.advancedsorter.pump.GuiAdvancedPump;
import com.antigravity.advancedsorter.pump.TileAdvancedPump;
import com.antigravity.advancedsorter.pump.TileAdvancedPump;
import com.antigravity.advancedsorter.container.ContainerInventoryIndex;
import com.antigravity.advancedsorter.client.gui.GuiInventoryIndex;
import com.antigravity.advancedsorter.tiles.TileInventoryIndex;
import com.antigravity.advancedsorter.tiles.TileAdvancedSorter;
import com.antigravity.advancedsorter.autocrafter.ContainerAutoCrafter;
import com.antigravity.advancedsorter.autocrafter.GuiAutoCrafter;
import com.antigravity.advancedsorter.autocrafter.TileAutoCrafter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class GuiHandler implements IGuiHandler {
    public static final int GUI_ID = 1; // Legacy ID for Advanced Sorter

    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));

        if (ID == GUI_ID || ID == AdvancedSorterMod.GUI_ADVANCED_SORTER) {
            if (tile instanceof TileAdvancedSorter) {
                return new ContainerAdvancedSorter(player, (TileAdvancedSorter) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_DIRECTIONAL_PIPE) {
            if (tile instanceof TileDirectionalPipe) {
                return new ContainerDirectionalPipe(player, (TileDirectionalPipe) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_TELEPORT_PIPE) {
            if (tile instanceof TileTeleportPipe) {
                return new ContainerTeleportPipe(player.inventory, (TileTeleportPipe) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_DIRECTIONAL_FLUID_PIPE) {
            if (tile instanceof TileDirectionalFluidPipe) {
                return new ContainerDirectionalFluidPipe(player.inventory, (TileDirectionalFluidPipe) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_TELEPORT_FLUID_PIPE) {
            if (tile instanceof TileTeleportFluidPipe) {
                return new ContainerTeleportFluidPipe(player.inventory, (TileTeleportFluidPipe) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_EXTRACTION_FLUID_PIPE) {
            if (tile instanceof TileExtractionFluidPipe) {
                return new ContainerExtractionFluidPipe((TileExtractionFluidPipe) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_PUMP_CONTROLLER) {
            if (tile instanceof TilePumpController) {
                return new ContainerPumpController((TilePumpController) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_ADVANCED_PUMP) {
            if (tile instanceof TileAdvancedPump) {
                return new ContainerAdvancedPump(player.inventory, (TileAdvancedPump) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_INVENTORY_INDEX) {
            if (tile instanceof TileInventoryIndex) {
                return new ContainerInventoryIndex(player.inventory, (TileInventoryIndex) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_TELEPORT_GAS_PIPE) {
            if (tile instanceof TileTeleportGasPipe) {
                return new ContainerTeleportGasPipe(player.inventory, (TileTeleportGasPipe) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_AUTO_CRAFTER) {
            if (tile instanceof TileAutoCrafter) {
                return new ContainerAutoCrafter(player.inventory, (TileAutoCrafter) tile);
            }
        }

        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));

        if (ID == GUI_ID || ID == AdvancedSorterMod.GUI_ADVANCED_SORTER) {
            if (tile instanceof TileAdvancedSorter) {
                return new GuiAdvancedSorter(new ContainerAdvancedSorter(player, (TileAdvancedSorter) tile),
                        (TileAdvancedSorter) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_DIRECTIONAL_PIPE) {
            if (tile instanceof TileDirectionalPipe) {
                return new GuiDirectionalPipe((TileDirectionalPipe) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_TELEPORT_PIPE) {
            if (tile instanceof TileTeleportPipe) {
                return new GuiTeleportPipe(player.inventory, (TileTeleportPipe) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_DIRECTIONAL_FLUID_PIPE) {
            if (tile instanceof TileDirectionalFluidPipe) {
                return new GuiDirectionalFluidPipe(player.inventory, (TileDirectionalFluidPipe) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_TELEPORT_FLUID_PIPE) {
            if (tile instanceof TileTeleportFluidPipe) {
                return new GuiTeleportFluidPipe(player.inventory, (TileTeleportFluidPipe) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_EXTRACTION_FLUID_PIPE) {
            if (tile instanceof TileExtractionFluidPipe) {
                return new GuiExtractionFluidPipe(new ContainerExtractionFluidPipe((TileExtractionFluidPipe) tile));
            }
        }

        if (ID == AdvancedSorterMod.GUI_PUMP_CONTROLLER) {
            if (tile instanceof TilePumpController) {
                return new GuiPumpController(new ContainerPumpController((TilePumpController) tile));
            }
        }

        if (ID == AdvancedSorterMod.GUI_ADVANCED_PUMP) {
            if (tile instanceof TileAdvancedPump) {
                return new GuiAdvancedPump(player.inventory, (TileAdvancedPump) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_INVENTORY_INDEX) {
            if (tile instanceof TileInventoryIndex) {
                return new GuiInventoryIndex(new ContainerInventoryIndex(player.inventory, (TileInventoryIndex) tile),
                        (TileInventoryIndex) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_CRAFTING_CALCULATOR) {
            return new com.antigravity.advancedsorter.client.gui.GuiCraftingCalculator(null);
        }

        if (ID == AdvancedSorterMod.GUI_NETWORK_TOOL) {
            return new GuiNetworkTool(player);
        }

        if (ID == AdvancedSorterMod.GUI_TELEPORT_GAS_PIPE) {
            if (tile instanceof TileTeleportGasPipe) {
                return new GuiTeleportGasPipe(player.inventory, (TileTeleportGasPipe) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_AUTO_CRAFTER) {
            if (tile instanceof TileAutoCrafter) {
                return new GuiAutoCrafter(new ContainerAutoCrafter(player.inventory, (TileAutoCrafter) tile),
                        (TileAutoCrafter) tile);
            }
        }

        return null;
    }
}

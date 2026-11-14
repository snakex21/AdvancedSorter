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
import com.antigravity.advancedsorter.container.ContainerInventoryIndex;
import com.antigravity.advancedsorter.client.gui.GuiInventoryIndex;
import com.antigravity.advancedsorter.tiles.TileInventoryIndex;
import com.antigravity.advancedsorter.tiles.TileAdvancedSorter;
import com.antigravity.advancedsorter.autocrafter.ContainerAutoCrafter;
import com.antigravity.advancedsorter.autocrafter.GuiAutoCrafter;
import com.antigravity.advancedsorter.autocrafter.TileAutoCrafter;
import com.antigravity.advancedsorter.tanks.ContainerFluidTank;
import com.antigravity.advancedsorter.tanks.GuiFluidTank;
import com.antigravity.advancedsorter.tanks.TileFluidTank;
import com.antigravity.advancedsorter.machines.cobblegen.*;
import com.antigravity.advancedsorter.machines.crucible.*;
import com.antigravity.advancedsorter.machines.crusher.*;
import com.antigravity.advancedsorter.machines.washer.*;
import com.antigravity.advancedsorter.machines.obsidiangen.*;
import com.antigravity.advancedsorter.machines.deconstructor.*;
import com.antigravity.advancedsorter.machines.repairer.*;
import com.antigravity.advancedsorter.machines.ammopress.*;
import com.antigravity.advancedsorter.machines.stabilizer.*;
import com.antigravity.advancedsorter.machines.flintmaker.*;
import com.antigravity.advancedsorter.machines.extractor.*;
import com.antigravity.advancedsorter.machines.xpvacuum.*;
import com.antigravity.advancedsorter.machines.breaker.*;
import com.antigravity.advancedsorter.machines.placer.*;
import com.antigravity.advancedsorter.machines.grinder.*;
import com.antigravity.advancedsorter.machines.trashcan.*;
import com.antigravity.advancedsorter.machines.fisher.*;
import com.antigravity.advancedsorter.machines.voidminer.*;
import com.antigravity.advancedsorter.machines.smartrouter.*;
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

        if (ID == AdvancedSorterMod.GUI_ADVANCED_TANK) {
            if (tile instanceof TileFluidTank) {
                return new ContainerFluidTank(player.inventory, (TileFluidTank) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_COBBLE_GENERATOR) {
            if (tile instanceof TileCobbleGenerator) {
                return new ContainerCobbleGenerator(player.inventory, (TileCobbleGenerator) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_CRUCIBLE) {
            if (tile instanceof TileCrucible) {
                return new ContainerCrucible(player.inventory, (TileCrucible) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_CRUSHER) {
            if (tile instanceof TileCrusher) {
                return new ContainerCrusher(player.inventory, (TileCrusher) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_WASHER) {
            if (tile instanceof TileWasher) {
                return new ContainerWasher(player.inventory, (TileWasher) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_OBSIDIAN_GENERATOR) {
            if (tile instanceof TileObsidianGenerator) {
                return new ContainerObsidianGenerator(player.inventory, (TileObsidianGenerator) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_DECONSTRUCTOR) {
            if (tile instanceof TileDeconstructor) {
                return new ContainerDeconstructor(player.inventory, (TileDeconstructor) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_AUTO_REPAIRER) {
            if (tile instanceof TileAutoRepairer) {
                return new ContainerAutoRepairer(player.inventory, (TileAutoRepairer) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_AMMO_PRESS) {
            if (tile instanceof TileAmmoPress) {
                return new ContainerAmmoPress(player.inventory, (TileAmmoPress) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_ATMOSPHERIC_STABILIZER) {
            if (tile instanceof TileAtmosphericStabilizer) {
                return new ContainerAtmosphericStabilizer(player.inventory, (TileAtmosphericStabilizer) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_FLINT_MAKER) {
            if (tile instanceof TileFlintMaker) {
                return new ContainerFlintMaker(player.inventory, (TileFlintMaker) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_ENCHANTMENT_EXTRACTOR) {
            if (tile instanceof TileEnchantmentExtractor) {
                return new ContainerEnchantmentExtractor(player.inventory, (TileEnchantmentExtractor) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_XP_VACUUM) {
            if (tile instanceof TileXPVacuum) {
                return new ContainerXPVacuum(player.inventory, (TileXPVacuum) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_BLOCK_BREAKER) {
            if (tile instanceof TileBlockBreaker) {
                return new ContainerBlockBreaker(player.inventory, (TileBlockBreaker) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_BLOCK_PLACER) {
            if (tile instanceof TileBlockPlacer) {
                return new ContainerBlockPlacer(player.inventory, (TileBlockPlacer) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_MOB_GRINDER) {
            if (tile instanceof TileMobGrinder) {
                return new ContainerMobGrinder(player.inventory, (TileMobGrinder) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_TRASH_CAN) {
            if (tile instanceof TileTrashCan) {
                return new ContainerTrashCan(player.inventory, (TileTrashCan) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_AUTO_FISHER) {
            if (tile instanceof TileAutoFisher) {
                return new ContainerAutoFisher(player.inventory, (TileAutoFisher) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_VOID_MINER) {
            if (tile instanceof TileVoidMiner) {
                return new ContainerVoidMiner(player.inventory, (TileVoidMiner) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_SMART_ROUTER) {
            if (tile instanceof TileSmartRouter) {
                return new ContainerSmartRouter(player, (TileSmartRouter) tile);
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

        if (ID == AdvancedSorterMod.GUI_ADVANCED_TANK) {
            if (tile instanceof TileFluidTank) {
                return new GuiFluidTank(player.inventory, (TileFluidTank) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_COBBLE_GENERATOR) {
            if (tile instanceof TileCobbleGenerator) {
                return new GuiCobbleGenerator(player.inventory, (TileCobbleGenerator) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_CRUCIBLE) {
            if (tile instanceof TileCrucible) {
                return new GuiCrucible(player.inventory, (TileCrucible) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_CRUSHER) {
            if (tile instanceof TileCrusher) {
                return new GuiCrusher(player.inventory, (TileCrusher) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_WASHER) {
            if (tile instanceof TileWasher) {
                return new GuiWasher(player.inventory, (TileWasher) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_OBSIDIAN_GENERATOR) {
            if (tile instanceof TileObsidianGenerator) {
                return new GuiObsidianGenerator(player.inventory, (TileObsidianGenerator) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_DECONSTRUCTOR) {
            if (tile instanceof TileDeconstructor) {
                return new GuiDeconstructor(player.inventory, (TileDeconstructor) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_AUTO_REPAIRER) {
            if (tile instanceof TileAutoRepairer) {
                return new GuiAutoRepairer(player.inventory, (TileAutoRepairer) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_AMMO_PRESS) {
            if (tile instanceof TileAmmoPress) {
                return new GuiAmmoPress(player.inventory, (TileAmmoPress) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_ATMOSPHERIC_STABILIZER) {
            if (tile instanceof TileAtmosphericStabilizer) {
                return new GuiAtmosphericStabilizer(player.inventory, (TileAtmosphericStabilizer) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_FLINT_MAKER) {
            if (tile instanceof TileFlintMaker) {
                return new GuiFlintMaker(player.inventory, (TileFlintMaker) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_ENCHANTMENT_EXTRACTOR) {
            if (tile instanceof TileEnchantmentExtractor) {
                return new GuiEnchantmentExtractor(player.inventory, (TileEnchantmentExtractor) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_XP_VACUUM) {
            if (tile instanceof TileXPVacuum) {
                return new GuiXPVacuum(player.inventory, (TileXPVacuum) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_BLOCK_BREAKER) {
            if (tile instanceof TileBlockBreaker) {
                return new GuiBlockBreaker(player.inventory, (TileBlockBreaker) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_BLOCK_PLACER) {
            if (tile instanceof TileBlockPlacer) {
                return new GuiBlockPlacer(player.inventory, (TileBlockPlacer) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_MOB_GRINDER) {
            if (tile instanceof TileMobGrinder) {
                return new GuiMobGrinder(player.inventory, (TileMobGrinder) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_TRASH_CAN) {
            if (tile instanceof TileTrashCan) {
                return new GuiTrashCan(player.inventory, (TileTrashCan) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_AUTO_FISHER) {
            if (tile instanceof TileAutoFisher) {
                return new GuiAutoFisher(player.inventory, (TileAutoFisher) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_VOID_MINER) {
            if (tile instanceof TileVoidMiner) {
                return new GuiVoidMiner(player.inventory, (TileVoidMiner) tile);
            }
        }

        if (ID == AdvancedSorterMod.GUI_SMART_ROUTER) {
            if (tile instanceof TileSmartRouter) {
                return new GuiSmartRouter(player, (TileSmartRouter) tile);
            }
        }

        return null;
    }
}

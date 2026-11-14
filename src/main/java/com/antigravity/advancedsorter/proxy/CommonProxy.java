package com.antigravity.advancedsorter.proxy;

import com.antigravity.advancedsorter.blocks.BlockAdvancedSorter;
import com.antigravity.advancedsorter.tiles.TileAdvancedSorter;
import com.antigravity.advancedsorter.pipes.BlockItemPipe;
import com.antigravity.advancedsorter.pipes.BlockExtractionPipe;
import com.antigravity.advancedsorter.pipes.TileItemPipe;
import com.antigravity.advancedsorter.pipes.TileExtractionPipe;
import com.antigravity.advancedsorter.pipes.ItemWrench;
import com.antigravity.advancedsorter.pipes.directional.BlockDirectionalPipe;
import com.antigravity.advancedsorter.pipes.directional.TileDirectionalPipe;
import com.antigravity.advancedsorter.pipes.transport.*;
import com.antigravity.advancedsorter.pipes.extraction.*;
import com.antigravity.advancedsorter.pipes.teleport.BlockTeleportPipe;
import com.antigravity.advancedsorter.pipes.teleport.TileTeleportPipe;
import com.antigravity.advancedsorter.pipes.fluid.TileFluidPipe;
import com.antigravity.advancedsorter.pipes.fluid.transport.*;
import com.antigravity.advancedsorter.pipes.fluid.directional.BlockDirectionalFluidPipe;
import com.antigravity.advancedsorter.pipes.fluid.directional.TileDirectionalFluidPipe;
import com.antigravity.advancedsorter.pipes.fluid.teleport.BlockTeleportFluidPipe;
import com.antigravity.advancedsorter.pipes.fluid.teleport.TileTeleportFluidPipe;
import com.antigravity.advancedsorter.pipes.fluid.extraction.BlockExtractionFluidPipe;
import com.antigravity.advancedsorter.pipes.fluid.extraction.TileExtractionFluidPipe;
import com.antigravity.advancedsorter.pipes.gas.teleport.BlockTeleportGasPipe;
import com.antigravity.advancedsorter.pipes.gas.teleport.TileTeleportGasPipe;
import com.antigravity.advancedsorter.pump.BlockPumpController;
import com.antigravity.advancedsorter.pump.TilePumpController;
import com.antigravity.advancedsorter.pump.BlockAdvancedPump;
import com.antigravity.advancedsorter.pump.TileAdvancedPump;
import com.antigravity.advancedsorter.pipes.fluid.BlockFluidOutlet;
import com.antigravity.advancedsorter.pipes.fluid.TileFluidOutlet;
import com.antigravity.advancedsorter.blocks.BlockInventoryIndex;
import com.antigravity.advancedsorter.tiles.TileInventoryIndex;
import com.antigravity.advancedsorter.blocks.ItemNetworkTool;
import com.antigravity.advancedsorter.autocrafter.BlockAutoCrafter;
import com.antigravity.advancedsorter.autocrafter.TileAutoCrafter;
import com.antigravity.advancedsorter.machines.cobblegen.BlockCobbleGenerator;
import com.antigravity.advancedsorter.machines.cobblegen.TileCobbleGenerator;
import com.antigravity.advancedsorter.machines.crucible.BlockCrucible;
import com.antigravity.advancedsorter.machines.crucible.TileCrucible;
import com.antigravity.advancedsorter.machines.crusher.BlockCrusher;
import com.antigravity.advancedsorter.machines.crusher.TileCrusher;
import com.antigravity.advancedsorter.machines.washer.BlockWasher;
import com.antigravity.advancedsorter.machines.washer.TileWasher;
import com.antigravity.advancedsorter.machines.obsidiangen.BlockObsidianGenerator;
import com.antigravity.advancedsorter.machines.obsidiangen.TileObsidianGenerator;
import com.antigravity.advancedsorter.machines.charger.BlockUniversalCharger;
import com.antigravity.advancedsorter.machines.charger.TileUniversalCharger;
import com.antigravity.advancedsorter.machines.deconstructor.BlockDeconstructor;
import com.antigravity.advancedsorter.machines.deconstructor.TileDeconstructor;
import com.antigravity.advancedsorter.machines.repairer.BlockAutoRepairer;
import com.antigravity.advancedsorter.machines.repairer.TileAutoRepairer;
import com.antigravity.advancedsorter.machines.ammopress.BlockAmmoPress;
import com.antigravity.advancedsorter.machines.ammopress.TileAmmoPress;
import com.antigravity.advancedsorter.machines.stabilizer.BlockAtmosphericStabilizer;
import com.antigravity.advancedsorter.machines.stabilizer.TileAtmosphericStabilizer;
import com.antigravity.advancedsorter.machines.converter.BlockEnergyConverter;
import com.antigravity.advancedsorter.machines.converter.TileEnergyConverter;
import com.antigravity.advancedsorter.machines.flintmaker.BlockFlintMaker;
import com.antigravity.advancedsorter.machines.flintmaker.TileFlintMaker;
import com.antigravity.advancedsorter.machines.extractor.BlockEnchantmentExtractor;
import com.antigravity.advancedsorter.machines.extractor.TileEnchantmentExtractor;
import com.antigravity.advancedsorter.machines.xpvacuum.BlockXPVacuum;
import com.antigravity.advancedsorter.machines.xpvacuum.TileXPVacuum;
import com.antigravity.advancedsorter.machines.breaker.BlockBlockBreaker;
import com.antigravity.advancedsorter.machines.breaker.TileBlockBreaker;
import com.antigravity.advancedsorter.machines.placer.BlockBlockPlacer;
import com.antigravity.advancedsorter.machines.placer.TileBlockPlacer;
import com.antigravity.advancedsorter.machines.grinder.BlockMobGrinder;
import com.antigravity.advancedsorter.machines.grinder.TileMobGrinder;
import com.antigravity.advancedsorter.machines.trashcan.BlockTrashCan;
import com.antigravity.advancedsorter.machines.trashcan.TileTrashCan;
import com.antigravity.advancedsorter.machines.fisher.BlockAutoFisher;
import com.antigravity.advancedsorter.machines.fisher.TileAutoFisher;
import com.antigravity.advancedsorter.machines.voidminer.BlockVoidMiner;
import com.antigravity.advancedsorter.machines.voidminer.TileVoidMiner;
import com.antigravity.advancedsorter.machines.smartrouter.BlockSmartRouter;
import com.antigravity.advancedsorter.machines.smartrouter.TileSmartRouter;
import com.antigravity.advancedsorter.tanks.BlockFluidTank;
import com.antigravity.advancedsorter.tanks.TileFluidTank;
import com.antigravity.advancedsorter.tanks.ItemBlockFluidTank;
import com.antigravity.advancedsorter.fluids.ModFluids;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import com.antigravity.advancedsorter.AdvancedSorterMod;

@Mod.EventBusSubscriber
public class CommonProxy {
        // Blocks
        public static BlockAdvancedSorter ADVANCED_SORTER;

        // Transport pipes
        public static BlockStonePipe STONE_PIPE;
        public static BlockItemPipe IRON_PIPE;
        public static BlockGoldPipe GOLD_PIPE;
        public static BlockDiamondPipe DIAMOND_PIPE;

        // Extraction pipes
        public static BlockStoneExtractor STONE_EXTRACTOR;
        public static BlockExtractionPipe IRON_EXTRACTOR;
        public static BlockGoldExtractor GOLD_EXTRACTOR;
        public static BlockDiamondExtractor DIAMOND_EXTRACTOR;

        public static BlockDirectionalPipe DIRECTIONAL_PIPE;
        public static BlockTeleportPipe TELEPORT_PIPE;
        public static ItemWrench WRENCH;

        // Fluid Transport Pipes
        public static BlockStoneFluidPipe STONE_FLUID_PIPE;
        public static BlockIronFluidPipe IRON_FLUID_PIPE;
        public static BlockGoldFluidPipe GOLD_FLUID_PIPE;
        public static BlockDiamondFluidPipe DIAMOND_FLUID_PIPE;
        public static BlockDirectionalFluidPipe DIRECTIONAL_FLUID_PIPE;
        public static BlockTeleportFluidPipe TELEPORT_FLUID_PIPE;
        public static BlockExtractionFluidPipe EXTRACTION_FLUID_PIPE;
        public static BlockTeleportGasPipe TELEPORT_GAS_PIPE;
        public static BlockAdvancedPump ADVANCED_PUMP;
        public static BlockFluidOutlet FLUID_OUTLET;

        // Pump Controller
        public static BlockPumpController PUMP_CONTROLLER;

        // Inventory Indexer
        public static BlockInventoryIndex INVENTORY_INDEX;
        public static ItemNetworkTool NETWORK_TOOL;

        // Auto Crafter
        public static BlockAutoCrafter AUTO_CRAFTER;

        // Machines
        public static BlockCobbleGenerator COBBLE_GENERATOR;
        public static BlockCrucible CRUCIBLE;
        public static BlockCrusher CRUSHER;
        public static BlockWasher WASHER;
        public static BlockObsidianGenerator OBSIDIAN_GENERATOR;
        public static BlockUniversalCharger UNIVERSAL_CHARGER;
        public static BlockDeconstructor DECONSTRUCTOR;
        public static BlockAutoRepairer AUTO_REPAIRER;
        public static BlockAmmoPress AMMO_PRESS;
        public static BlockAtmosphericStabilizer ATMOSPHERIC_STABILIZER;

        // Energy Converter
        public static BlockEnergyConverter ENERGY_CONVERTER;

        // Flint Maker
        public static BlockFlintMaker FLINT_MAKER;

        // Enchantment Extractor
        public static BlockEnchantmentExtractor ENCHANTMENT_EXTRACTOR;

        // XP Vacuum
        public static BlockXPVacuum XP_VACUUM;

        // Block Breaker
        public static BlockBlockBreaker BLOCK_BREAKER;

        // Block Placer
        public static BlockBlockPlacer BLOCK_PLACER;

        // Mob Grinder
        public static BlockMobGrinder MOB_GRINDER;

        // Trash Can
        public static BlockTrashCan TRASH_CAN;

        // Auto Fisher
        public static BlockAutoFisher AUTO_FISHER;

        // Void Miner
        public static BlockVoidMiner VOID_MINER;

        // Smart Router
        public static BlockSmartRouter SMART_ROUTER;

        // Fluid Tanks
        public static BlockFluidTank FLUID_TANK;

        public void preInit(FMLPreInitializationEvent event) {
                ModFluids.registerFluids();
        }

        public void init(FMLInitializationEvent event) {
        }

        @SubscribeEvent
        public static void registerBlocks(RegistryEvent.Register<Block> event) {
                // Advanced Sorter
                ADVANCED_SORTER = new BlockAdvancedSorter();
                event.getRegistry().register(ADVANCED_SORTER);
                GameRegistry.registerTileEntity(TileAdvancedSorter.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "advanced_sorter"));

                // Transport Pipes
                STONE_PIPE = new BlockStonePipe();
                event.getRegistry().register(STONE_PIPE);
                GameRegistry.registerTileEntity(TileStonePipe.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "stone_pipe"));

                IRON_PIPE = new BlockItemPipe();
                event.getRegistry().register(IRON_PIPE);
                GameRegistry.registerTileEntity(TileItemPipe.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_item_pipe"));

                GOLD_PIPE = new BlockGoldPipe();
                event.getRegistry().register(GOLD_PIPE);
                GameRegistry.registerTileEntity(TileGoldPipe.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "gold_pipe"));

                DIAMOND_PIPE = new BlockDiamondPipe();
                event.getRegistry().register(DIAMOND_PIPE);
                GameRegistry.registerTileEntity(TileDiamondPipe.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "diamond_pipe"));

                // Extraction Pipes
                STONE_EXTRACTOR = new BlockStoneExtractor();
                event.getRegistry().register(STONE_EXTRACTOR);
                GameRegistry.registerTileEntity(TileStoneExtractor.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "stone_extractor"));

                IRON_EXTRACTOR = new BlockExtractionPipe();
                event.getRegistry().register(IRON_EXTRACTOR);
                GameRegistry.registerTileEntity(TileExtractionPipe.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "iron_extractor"));

                GOLD_EXTRACTOR = new BlockGoldExtractor();
                event.getRegistry().register(GOLD_EXTRACTOR);
                GameRegistry.registerTileEntity(TileGoldExtractor.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "gold_extractor"));

                DIAMOND_EXTRACTOR = new BlockDiamondExtractor();
                event.getRegistry().register(DIAMOND_EXTRACTOR);
                GameRegistry.registerTileEntity(TileDiamondExtractor.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "diamond_extractor"));

                // Directional Pipes
                DIRECTIONAL_PIPE = new BlockDirectionalPipe();
                event.getRegistry().register(DIRECTIONAL_PIPE);
                GameRegistry.registerTileEntity(TileDirectionalPipe.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "directional_pipe"));

                // Teleport Pipes
                TELEPORT_PIPE = new BlockTeleportPipe();
                event.getRegistry().register(TELEPORT_PIPE);
                GameRegistry.registerTileEntity(TileTeleportPipe.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "teleport_pipe"));

                // Fluid Transport Pipes
                STONE_FLUID_PIPE = new BlockStoneFluidPipe();
                event.getRegistry().register(STONE_FLUID_PIPE);

                IRON_FLUID_PIPE = new BlockIronFluidPipe();
                event.getRegistry().register(IRON_FLUID_PIPE);

                GOLD_FLUID_PIPE = new BlockGoldFluidPipe();
                event.getRegistry().register(GOLD_FLUID_PIPE);

                DIAMOND_FLUID_PIPE = new BlockDiamondFluidPipe();
                event.getRegistry().register(DIAMOND_FLUID_PIPE);

                GameRegistry.registerTileEntity(TileFluidPipe.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_fluid_pipe"));

                // Directional Fluid Pipe
                DIRECTIONAL_FLUID_PIPE = new BlockDirectionalFluidPipe();
                event.getRegistry().register(DIRECTIONAL_FLUID_PIPE);
                GameRegistry.registerTileEntity(TileDirectionalFluidPipe.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_directional_fluid_pipe"));

                // Teleport Fluid Pipe
                TELEPORT_FLUID_PIPE = new BlockTeleportFluidPipe();
                event.getRegistry().register(TELEPORT_FLUID_PIPE);
                GameRegistry.registerTileEntity(TileTeleportFluidPipe.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_teleport_fluid_pipe"));

                // Extraction Fluid Pipe
                EXTRACTION_FLUID_PIPE = new BlockExtractionFluidPipe();
                event.getRegistry().register(EXTRACTION_FLUID_PIPE);
                GameRegistry.registerTileEntity(TileExtractionFluidPipe.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_extraction_fluid_pipe"));

                // Teleport Gas Pipe (Mekanism)
                TELEPORT_GAS_PIPE = new BlockTeleportGasPipe();
                event.getRegistry().register(TELEPORT_GAS_PIPE);
                GameRegistry.registerTileEntity(TileTeleportGasPipe.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_teleport_gas_pipe"));

                // Pump Controller
                PUMP_CONTROLLER = new BlockPumpController();
                event.getRegistry().register(PUMP_CONTROLLER);
                GameRegistry.registerTileEntity(TilePumpController.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_pump_controller"));

                // Advanced Pump
                ADVANCED_PUMP = new BlockAdvancedPump();
                event.getRegistry().register(ADVANCED_PUMP);
                GameRegistry.registerTileEntity(TileAdvancedPump.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_advanced_pump"));

                // Fluid Outlet
                FLUID_OUTLET = new BlockFluidOutlet();
                event.getRegistry().register(FLUID_OUTLET);
                GameRegistry.registerTileEntity(TileFluidOutlet.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_fluid_outlet"));

                // Inventory Indexer
                INVENTORY_INDEX = new BlockInventoryIndex();
                event.getRegistry().register(INVENTORY_INDEX);
                GameRegistry.registerTileEntity(TileInventoryIndex.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_inventory_index"));

                // Auto Crafter
                AUTO_CRAFTER = new BlockAutoCrafter();
                event.getRegistry().register(AUTO_CRAFTER);
                GameRegistry.registerTileEntity(TileAutoCrafter.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_auto_crafter"));

                // Cobble Generator
                COBBLE_GENERATOR = new BlockCobbleGenerator();
                event.getRegistry().register(COBBLE_GENERATOR);
                GameRegistry.registerTileEntity(TileCobbleGenerator.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_cobble_generator"));

                // Crucible
                CRUCIBLE = new BlockCrucible();
                event.getRegistry().register(CRUCIBLE);
                GameRegistry.registerTileEntity(TileCrucible.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_crucible"));

                // Crusher
                CRUSHER = new BlockCrusher();
                event.getRegistry().register(CRUSHER);
                GameRegistry.registerTileEntity(TileCrusher.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_crusher"));

                // Washer
                WASHER = new BlockWasher();
                event.getRegistry().register(WASHER);
                GameRegistry.registerTileEntity(TileWasher.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_washer"));

                // Obsidian Generator
                OBSIDIAN_GENERATOR = new BlockObsidianGenerator();
                event.getRegistry().register(OBSIDIAN_GENERATOR);
                GameRegistry.registerTileEntity(TileObsidianGenerator.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_obsidian_generator"));

                // Universal Charger
                UNIVERSAL_CHARGER = new BlockUniversalCharger();
                event.getRegistry().register(UNIVERSAL_CHARGER);
                GameRegistry.registerTileEntity(TileUniversalCharger.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_universal_charger"));

                // Deconstructor
                DECONSTRUCTOR = new BlockDeconstructor();
                event.getRegistry().register(DECONSTRUCTOR);
                GameRegistry.registerTileEntity(TileDeconstructor.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_deconstructor"));

                // Auto Repairer
                AUTO_REPAIRER = new BlockAutoRepairer();
                event.getRegistry().register(AUTO_REPAIRER);
                GameRegistry.registerTileEntity(TileAutoRepairer.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_auto_repairer"));

                // Ammo Press
                AMMO_PRESS = new BlockAmmoPress();
                event.getRegistry().register(AMMO_PRESS);
                GameRegistry.registerTileEntity(TileAmmoPress.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_ammo_press"));

                // Atmospheric Stabilizer
                ATMOSPHERIC_STABILIZER = new BlockAtmosphericStabilizer();
                event.getRegistry().register(ATMOSPHERIC_STABILIZER);
                GameRegistry.registerTileEntity(TileAtmosphericStabilizer.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_atmospheric_stabilizer"));

                // Energy Converter
                ENERGY_CONVERTER = new BlockEnergyConverter();
                event.getRegistry().register(ENERGY_CONVERTER);
                GameRegistry.registerTileEntity(TileEnergyConverter.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_energy_converter"));

                // Flint Maker
                FLINT_MAKER = new BlockFlintMaker();
                event.getRegistry().register(FLINT_MAKER);
                GameRegistry.registerTileEntity(TileFlintMaker.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_flint_maker"));

                // Enchantment Extractor
                ENCHANTMENT_EXTRACTOR = new BlockEnchantmentExtractor();
                event.getRegistry().register(ENCHANTMENT_EXTRACTOR);
                GameRegistry.registerTileEntity(TileEnchantmentExtractor.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_enchantment_extractor"));

                // XP Vacuum
                XP_VACUUM = new BlockXPVacuum();
                event.getRegistry().register(XP_VACUUM);
                GameRegistry.registerTileEntity(TileXPVacuum.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_xp_vacuum"));

                // Block Breaker
                BLOCK_BREAKER = new BlockBlockBreaker();
                event.getRegistry().register(BLOCK_BREAKER);
                GameRegistry.registerTileEntity(TileBlockBreaker.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_block_breaker"));

                // Block Placer
                BLOCK_PLACER = new BlockBlockPlacer();
                event.getRegistry().register(BLOCK_PLACER);
                GameRegistry.registerTileEntity(TileBlockPlacer.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_block_placer"));

                // Mob Grinder
                MOB_GRINDER = new BlockMobGrinder();
                event.getRegistry().register(MOB_GRINDER);
                GameRegistry.registerTileEntity(TileMobGrinder.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_mob_grinder"));

                // Trash Can
                TRASH_CAN = new BlockTrashCan();
                event.getRegistry().register(TRASH_CAN);
                GameRegistry.registerTileEntity(TileTrashCan.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_trash_can"));

                // Auto Fisher
                AUTO_FISHER = new BlockAutoFisher();
                event.getRegistry().register(AUTO_FISHER);
                GameRegistry.registerTileEntity(TileAutoFisher.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_auto_fisher"));

                // Void Miner
                VOID_MINER = new BlockVoidMiner();
                event.getRegistry().register(VOID_MINER);
                GameRegistry.registerTileEntity(TileVoidMiner.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_void_miner"));

                // Smart Router
                SMART_ROUTER = new BlockSmartRouter();
                event.getRegistry().register(SMART_ROUTER);
                GameRegistry.registerTileEntity(TileSmartRouter.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_smart_router"));

                // Fluid Tank (all tiers)
                FLUID_TANK = new BlockFluidTank();
                event.getRegistry().register(FLUID_TANK);
                GameRegistry.registerTileEntity(TileFluidTank.class,
                                new ResourceLocation(AdvancedSorterMod.MODID, "tile_fluid_tank"));
        }

        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> event) {
                event.getRegistry().register(new ItemBlock(ADVANCED_SORTER)
                                .setRegistryName(ADVANCED_SORTER.getRegistryName()));

                event.getRegistry().register(new ItemBlock(STONE_PIPE).setRegistryName(STONE_PIPE.getRegistryName()));
                event.getRegistry().register(new ItemBlock(IRON_PIPE).setRegistryName(IRON_PIPE.getRegistryName()));
                event.getRegistry().register(new ItemBlock(GOLD_PIPE).setRegistryName(GOLD_PIPE.getRegistryName()));
                event.getRegistry()
                                .register(new ItemBlock(DIAMOND_PIPE).setRegistryName(DIAMOND_PIPE.getRegistryName()));

                event.getRegistry().register(
                                new ItemBlock(STONE_EXTRACTOR).setRegistryName(STONE_EXTRACTOR.getRegistryName()));
                event.getRegistry().register(
                                new ItemBlock(IRON_EXTRACTOR).setRegistryName(IRON_EXTRACTOR.getRegistryName()));
                event.getRegistry().register(
                                new ItemBlock(GOLD_EXTRACTOR).setRegistryName(GOLD_EXTRACTOR.getRegistryName()));
                event.getRegistry()
                                .register(new ItemBlock(DIAMOND_EXTRACTOR)
                                                .setRegistryName(DIAMOND_EXTRACTOR.getRegistryName()));

                event.getRegistry()
                                .register(new ItemBlock(DIRECTIONAL_PIPE)
                                                .setRegistryName(DIRECTIONAL_PIPE.getRegistryName()));
                event.getRegistry().register(
                                new ItemBlock(TELEPORT_PIPE).setRegistryName(TELEPORT_PIPE.getRegistryName()));

                WRENCH = new ItemWrench();
                event.getRegistry().register(WRENCH);

                // Fluid Pipes
                event.getRegistry().register(
                                new ItemBlock(STONE_FLUID_PIPE).setRegistryName(STONE_FLUID_PIPE.getRegistryName()));
                event.getRegistry().register(
                                new ItemBlock(IRON_FLUID_PIPE).setRegistryName(IRON_FLUID_PIPE.getRegistryName()));
                event.getRegistry().register(
                                new ItemBlock(GOLD_FLUID_PIPE).setRegistryName(GOLD_FLUID_PIPE.getRegistryName()));
                event.getRegistry().register(new ItemBlock(DIAMOND_FLUID_PIPE)
                                .setRegistryName(DIAMOND_FLUID_PIPE.getRegistryName()));
                event.getRegistry().register(new ItemBlock(DIRECTIONAL_FLUID_PIPE)
                                .setRegistryName(DIRECTIONAL_FLUID_PIPE.getRegistryName()));
                event.getRegistry().register(new ItemBlock(TELEPORT_FLUID_PIPE)
                                .setRegistryName(TELEPORT_FLUID_PIPE.getRegistryName()));
                event.getRegistry().register(new ItemBlock(EXTRACTION_FLUID_PIPE)
                                .setRegistryName(EXTRACTION_FLUID_PIPE.getRegistryName()));
                event.getRegistry().register(new ItemBlock(TELEPORT_GAS_PIPE)
                                .setRegistryName(TELEPORT_GAS_PIPE.getRegistryName()));
                event.getRegistry().register(new ItemBlock(PUMP_CONTROLLER)
                                .setRegistryName(PUMP_CONTROLLER.getRegistryName()));
                event.getRegistry().register(new ItemBlock(ADVANCED_PUMP)
                                .setRegistryName(ADVANCED_PUMP.getRegistryName()));
                event.getRegistry().register(new ItemBlock(FLUID_OUTLET)
                                .setRegistryName(FLUID_OUTLET.getRegistryName()));

                // Indexer Items
                event.getRegistry().register(new ItemBlock(INVENTORY_INDEX)
                                .setRegistryName(INVENTORY_INDEX.getRegistryName()));

                NETWORK_TOOL = new ItemNetworkTool();
                event.getRegistry().register(NETWORK_TOOL);

                // Auto Crafter
                event.getRegistry().register(new ItemBlock(AUTO_CRAFTER)
                                .setRegistryName(AUTO_CRAFTER.getRegistryName()));

                // Machines
                event.getRegistry().register(new ItemBlock(COBBLE_GENERATOR)
                                .setRegistryName(COBBLE_GENERATOR.getRegistryName()));
                event.getRegistry().register(new ItemBlock(CRUCIBLE)
                                .setRegistryName(CRUCIBLE.getRegistryName()));
                event.getRegistry().register(new ItemBlock(CRUSHER)
                                .setRegistryName(CRUSHER.getRegistryName()));
                event.getRegistry().register(new ItemBlock(WASHER)
                                .setRegistryName(WASHER.getRegistryName()));

                event.getRegistry().register(new ItemBlock(OBSIDIAN_GENERATOR)
                                .setRegistryName(OBSIDIAN_GENERATOR.getRegistryName()));
                event.getRegistry().register(new ItemBlock(UNIVERSAL_CHARGER)
                                .setRegistryName(UNIVERSAL_CHARGER.getRegistryName()));
                event.getRegistry().register(new ItemBlock(DECONSTRUCTOR)
                                .setRegistryName(DECONSTRUCTOR.getRegistryName()));
                event.getRegistry().register(new ItemBlock(AUTO_REPAIRER)
                                .setRegistryName(AUTO_REPAIRER.getRegistryName()));
                event.getRegistry().register(new ItemBlock(AMMO_PRESS)
                                .setRegistryName(AMMO_PRESS.getRegistryName()));
                event.getRegistry().register(new ItemBlock(ATMOSPHERIC_STABILIZER)
                                .setRegistryName(ATMOSPHERIC_STABILIZER.getRegistryName()));

                event.getRegistry().register(new ItemBlock(ENERGY_CONVERTER)
                                .setRegistryName(ENERGY_CONVERTER.getRegistryName()));

                event.getRegistry().register(new ItemBlock(FLINT_MAKER)
                                .setRegistryName(FLINT_MAKER.getRegistryName()));

                event.getRegistry().register(new ItemBlock(ENCHANTMENT_EXTRACTOR)
                                .setRegistryName(ENCHANTMENT_EXTRACTOR.getRegistryName()));

                event.getRegistry().register(new ItemBlock(XP_VACUUM)
                                .setRegistryName(XP_VACUUM.getRegistryName()));

                event.getRegistry().register(new ItemBlock(BLOCK_BREAKER)
                                .setRegistryName(BLOCK_BREAKER.getRegistryName()));
                event.getRegistry().register(new ItemBlock(BLOCK_PLACER)
                                .setRegistryName(BLOCK_PLACER.getRegistryName()));
                event.getRegistry().register(new ItemBlock(MOB_GRINDER)
                                .setRegistryName(MOB_GRINDER.getRegistryName()));
                event.getRegistry().register(new ItemBlock(TRASH_CAN)
                                .setRegistryName(TRASH_CAN.getRegistryName()));
                event.getRegistry().register(new ItemBlock(AUTO_FISHER)
                                .setRegistryName(AUTO_FISHER.getRegistryName()));
                event.getRegistry().register(new ItemBlock(VOID_MINER)
                                .setRegistryName(VOID_MINER.getRegistryName()));
                event.getRegistry().register(new ItemBlock(SMART_ROUTER)
                                .setRegistryName(SMART_ROUTER.getRegistryName()));

                // Fluid Tank (with subtypes for tiers)
                event.getRegistry().register(new ItemBlockFluidTank(FLUID_TANK)
                                .setRegistryName(FLUID_TANK.getRegistryName()));
        }
}

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
import com.antigravity.advancedsorter.pump.BlockPumpController;
import com.antigravity.advancedsorter.pump.TilePumpController;
import com.antigravity.advancedsorter.pump.BlockAdvancedPump;
import com.antigravity.advancedsorter.pump.TileAdvancedPump;
import com.antigravity.advancedsorter.pipes.fluid.BlockFluidOutlet;
import com.antigravity.advancedsorter.pipes.fluid.TileFluidOutlet;
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
        public static BlockAdvancedPump ADVANCED_PUMP;
        public static BlockFluidOutlet FLUID_OUTLET;

        // Pump Controller
        public static BlockPumpController PUMP_CONTROLLER;

        public void preInit(FMLPreInitializationEvent event) {
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
                event.getRegistry().register(new ItemBlock(PUMP_CONTROLLER)
                                .setRegistryName(PUMP_CONTROLLER.getRegistryName()));
                event.getRegistry().register(new ItemBlock(ADVANCED_PUMP)
                                .setRegistryName(ADVANCED_PUMP.getRegistryName()));
                event.getRegistry().register(new ItemBlock(FLUID_OUTLET)
                                .setRegistryName(FLUID_OUTLET.getRegistryName()));
        }
}

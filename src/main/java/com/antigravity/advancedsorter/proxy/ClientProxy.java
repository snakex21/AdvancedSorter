package com.antigravity.advancedsorter.proxy;

import com.antigravity.advancedsorter.blocks.BlockAdvancedSorter;
import com.antigravity.advancedsorter.client.render.TileItemPipeRenderer;
import com.antigravity.advancedsorter.pipes.TileExtractionPipe;
import com.antigravity.advancedsorter.pipes.TileItemPipe;
import com.antigravity.advancedsorter.pipes.directional.TileDirectionalPipe;
import com.antigravity.advancedsorter.pipes.extraction.TileDiamondExtractor;
import com.antigravity.advancedsorter.pipes.extraction.TileGoldExtractor;
import com.antigravity.advancedsorter.pipes.extraction.TileStoneExtractor;
import com.antigravity.advancedsorter.pipes.transport.TileDiamondPipe;
import com.antigravity.advancedsorter.pipes.transport.TileGoldPipe;
import com.antigravity.advancedsorter.pipes.transport.TileStonePipe;
import com.antigravity.advancedsorter.pipes.teleport.TileTeleportPipe;
import com.antigravity.advancedsorter.pipes.fluid.TileFluidPipe;
import com.antigravity.advancedsorter.client.render.TileFluidPipeRenderer;
import com.antigravity.advancedsorter.pipes.gas.teleport.TileTeleportGasPipe;
import com.antigravity.advancedsorter.client.render.TileTeleportGasPipeRenderer;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraft.item.Item;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import com.antigravity.advancedsorter.client.KeyInputHandler;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        // Register TileEntity Renderers
        ClientRegistry.bindTileEntitySpecialRenderer(TileItemPipe.class, new TileItemPipeRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileStonePipe.class, new TileItemPipeRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileGoldPipe.class, new TileItemPipeRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileDiamondPipe.class, new TileItemPipeRenderer());

        ClientRegistry.bindTileEntitySpecialRenderer(TileExtractionPipe.class, new TileItemPipeRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileStoneExtractor.class, new TileItemPipeRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileGoldExtractor.class, new TileItemPipeRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileDiamondExtractor.class, new TileItemPipeRenderer());

        ClientRegistry.bindTileEntitySpecialRenderer(TileDirectionalPipe.class, new TileItemPipeRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileTeleportPipe.class, new TileItemPipeRenderer());

        // Fluid pipe renderer - register for base class and all subclasses
        ClientRegistry.bindTileEntitySpecialRenderer(TileFluidPipe.class, new TileFluidPipeRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(
                com.antigravity.advancedsorter.pipes.fluid.directional.TileDirectionalFluidPipe.class,
                new TileFluidPipeRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(
                com.antigravity.advancedsorter.pipes.fluid.teleport.TileTeleportFluidPipe.class,
                new TileFluidPipeRenderer());

        // Gas pipe renderer
        ClientRegistry.bindTileEntitySpecialRenderer(TileTeleportGasPipe.class, new TileTeleportGasPipeRenderer());

        // Register Keybinds
        KeyInputHandler.register();
        MinecraftForge.EVENT_BUS.register(new KeyInputHandler());
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        // Register block models
        if (CommonProxy.ADVANCED_SORTER != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.ADVANCED_SORTER), 0,
                    new ModelResourceLocation(CommonProxy.ADVANCED_SORTER.getRegistryName(), "inventory"));
        }
        if (CommonProxy.INVENTORY_INDEX != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.INVENTORY_INDEX), 0,
                    new ModelResourceLocation(CommonProxy.INVENTORY_INDEX.getRegistryName(), "inventory"));
        }

        // Pipe and wrench models are registered via blockstate/model json inheritance
        // Transport pipes
        if (CommonProxy.STONE_PIPE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.STONE_PIPE), 0,
                    new ModelResourceLocation(CommonProxy.STONE_PIPE.getRegistryName(), "inventory"));
        }
        if (CommonProxy.IRON_PIPE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.IRON_PIPE), 0,
                    new ModelResourceLocation(CommonProxy.IRON_PIPE.getRegistryName(), "inventory"));
        }
        if (CommonProxy.GOLD_PIPE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.GOLD_PIPE), 0,
                    new ModelResourceLocation(CommonProxy.GOLD_PIPE.getRegistryName(), "inventory"));
        }
        if (CommonProxy.DIAMOND_PIPE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.DIAMOND_PIPE), 0,
                    new ModelResourceLocation(CommonProxy.DIAMOND_PIPE.getRegistryName(), "inventory"));
        }

        // Extraction pipes
        if (CommonProxy.STONE_EXTRACTOR != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.STONE_EXTRACTOR), 0,
                    new ModelResourceLocation(CommonProxy.STONE_EXTRACTOR.getRegistryName(), "inventory"));
        }
        if (CommonProxy.IRON_EXTRACTOR != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.IRON_EXTRACTOR), 0,
                    new ModelResourceLocation(CommonProxy.IRON_EXTRACTOR.getRegistryName(), "inventory"));
        }
        if (CommonProxy.GOLD_EXTRACTOR != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.GOLD_EXTRACTOR), 0,
                    new ModelResourceLocation(CommonProxy.GOLD_EXTRACTOR.getRegistryName(), "inventory"));
        }
        if (CommonProxy.DIAMOND_EXTRACTOR != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.DIAMOND_EXTRACTOR), 0,
                    new ModelResourceLocation(CommonProxy.DIAMOND_EXTRACTOR.getRegistryName(), "inventory"));
        }

        if (CommonProxy.WRENCH != null) {
            ModelLoader.setCustomModelResourceLocation(
                    CommonProxy.WRENCH, 0,
                    new ModelResourceLocation(CommonProxy.WRENCH.getRegistryName(), "inventory"));
        }
        if (CommonProxy.NETWORK_TOOL != null) {
            ModelLoader.setCustomModelResourceLocation(
                    CommonProxy.NETWORK_TOOL, 0,
                    new ModelResourceLocation(CommonProxy.NETWORK_TOOL.getRegistryName(), "inventory"));
        }

        if (CommonProxy.DIRECTIONAL_PIPE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.DIRECTIONAL_PIPE), 0,
                    new ModelResourceLocation(CommonProxy.DIRECTIONAL_PIPE.getRegistryName(), "inventory"));
        }
        if (CommonProxy.TELEPORT_PIPE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.TELEPORT_PIPE), 0,
                    new ModelResourceLocation(CommonProxy.TELEPORT_PIPE.getRegistryName(), "inventory"));
        }

        // Fluid pipes
        if (CommonProxy.STONE_FLUID_PIPE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.STONE_FLUID_PIPE), 0,
                    new ModelResourceLocation(CommonProxy.STONE_FLUID_PIPE.getRegistryName(), "inventory"));
        }
        if (CommonProxy.IRON_FLUID_PIPE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.IRON_FLUID_PIPE), 0,
                    new ModelResourceLocation(CommonProxy.IRON_FLUID_PIPE.getRegistryName(), "inventory"));
        }
        if (CommonProxy.GOLD_FLUID_PIPE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.GOLD_FLUID_PIPE), 0,
                    new ModelResourceLocation(CommonProxy.GOLD_FLUID_PIPE.getRegistryName(), "inventory"));
        }
        if (CommonProxy.DIAMOND_FLUID_PIPE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.DIAMOND_FLUID_PIPE), 0,
                    new ModelResourceLocation(CommonProxy.DIAMOND_FLUID_PIPE.getRegistryName(), "inventory"));
        }
        if (CommonProxy.DIRECTIONAL_FLUID_PIPE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.DIRECTIONAL_FLUID_PIPE), 0,
                    new ModelResourceLocation(CommonProxy.DIRECTIONAL_FLUID_PIPE.getRegistryName(), "inventory"));
        }
        if (CommonProxy.TELEPORT_FLUID_PIPE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.TELEPORT_FLUID_PIPE), 0,
                    new ModelResourceLocation(CommonProxy.TELEPORT_FLUID_PIPE.getRegistryName(), "inventory"));
        }
        if (CommonProxy.EXTRACTION_FLUID_PIPE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.EXTRACTION_FLUID_PIPE), 0,
                    new ModelResourceLocation(CommonProxy.EXTRACTION_FLUID_PIPE.getRegistryName(), "inventory"));
        }
        if (CommonProxy.PUMP_CONTROLLER != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.PUMP_CONTROLLER), 0,
                    new ModelResourceLocation(CommonProxy.PUMP_CONTROLLER.getRegistryName(), "inventory"));
        }
        if (CommonProxy.ADVANCED_PUMP != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.ADVANCED_PUMP), 0,
                    new ModelResourceLocation(CommonProxy.ADVANCED_PUMP.getRegistryName(), "inventory"));
        }
        if (CommonProxy.FLUID_OUTLET != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.FLUID_OUTLET), 0,
                    new ModelResourceLocation(CommonProxy.FLUID_OUTLET.getRegistryName(), "inventory"));
        }
        if (CommonProxy.TELEPORT_GAS_PIPE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.TELEPORT_GAS_PIPE), 0,
                    new ModelResourceLocation(CommonProxy.TELEPORT_GAS_PIPE.getRegistryName(), "inventory"));
        }
    }
}

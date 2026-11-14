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
import com.antigravity.advancedsorter.tanks.TileFluidTank;
import com.antigravity.advancedsorter.tanks.TankTier;
import com.antigravity.advancedsorter.client.render.TileFluidTankRenderer;
import net.minecraft.util.ResourceLocation;
import com.antigravity.advancedsorter.AdvancedSorterMod;
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

        // Fluid tank renderer (shows fluid inside)
        ClientRegistry.bindTileEntitySpecialRenderer(TileFluidTank.class, new TileFluidTankRenderer());

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
                    new ModelResourceLocation(new ResourceLocation(AdvancedSorterMod.MODID, "directional_pipe_inventory"), "inventory"));
        }
        if (CommonProxy.TELEPORT_PIPE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.TELEPORT_PIPE), 0,
                    new ModelResourceLocation(new ResourceLocation(AdvancedSorterMod.MODID, "teleport_pipe_inventory"), "inventory"));
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
                    new ModelResourceLocation(new ResourceLocation(AdvancedSorterMod.MODID, "directional_fluid_pipe_inventory"), "inventory"));
        }
        if (CommonProxy.TELEPORT_FLUID_PIPE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.TELEPORT_FLUID_PIPE), 0,
                    new ModelResourceLocation(new ResourceLocation(AdvancedSorterMod.MODID, "teleport_fluid_pipe_inventory"), "inventory"));
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
                    new ModelResourceLocation(new ResourceLocation(AdvancedSorterMod.MODID, "teleport_gas_pipe_inventory"), "inventory"));
        }

        // Auto Crafter
        if (CommonProxy.AUTO_CRAFTER != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.AUTO_CRAFTER), 0,
                    new ModelResourceLocation(CommonProxy.AUTO_CRAFTER.getRegistryName(), "inventory"));
        }

        // Machines
        if (CommonProxy.COBBLE_GENERATOR != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.COBBLE_GENERATOR), 0,
                    new ModelResourceLocation(CommonProxy.COBBLE_GENERATOR.getRegistryName(), "inventory"));
        }
        if (CommonProxy.FLINT_MAKER != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.FLINT_MAKER), 0,
                    new ModelResourceLocation(CommonProxy.FLINT_MAKER.getRegistryName(), "inventory"));
        }
        if (CommonProxy.CRUCIBLE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.CRUCIBLE), 0,
                    new ModelResourceLocation(CommonProxy.CRUCIBLE.getRegistryName(), "inventory"));
        }
        if (CommonProxy.CRUSHER != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.CRUSHER), 0,
                    new ModelResourceLocation(CommonProxy.CRUSHER.getRegistryName(), "inventory"));
        }
        if (CommonProxy.WASHER != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.WASHER), 0,
                    new ModelResourceLocation(CommonProxy.WASHER.getRegistryName(), "inventory"));
        }

        if (CommonProxy.OBSIDIAN_GENERATOR != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.OBSIDIAN_GENERATOR), 0,
                    new ModelResourceLocation(CommonProxy.OBSIDIAN_GENERATOR.getRegistryName(), "inventory"));
        }
        if (CommonProxy.UNIVERSAL_CHARGER != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.UNIVERSAL_CHARGER), 0,
                    new ModelResourceLocation(CommonProxy.UNIVERSAL_CHARGER.getRegistryName(), "inventory"));
        }
        if (CommonProxy.DECONSTRUCTOR != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.DECONSTRUCTOR), 0,
                    new ModelResourceLocation(CommonProxy.DECONSTRUCTOR.getRegistryName(), "inventory"));
        }
        if (CommonProxy.AUTO_REPAIRER != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.AUTO_REPAIRER), 0,
                    new ModelResourceLocation(CommonProxy.AUTO_REPAIRER.getRegistryName(), "inventory"));
        }
        if (CommonProxy.AMMO_PRESS != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.AMMO_PRESS), 0,
                    new ModelResourceLocation(CommonProxy.AMMO_PRESS.getRegistryName(), "inventory"));
        }
        if (CommonProxy.ATMOSPHERIC_STABILIZER != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.ATMOSPHERIC_STABILIZER), 0,
                    new ModelResourceLocation(CommonProxy.ATMOSPHERIC_STABILIZER.getRegistryName(), "inventory"));
        }
        if (CommonProxy.ENERGY_CONVERTER != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.ENERGY_CONVERTER), 0,
                    new ModelResourceLocation(CommonProxy.ENERGY_CONVERTER.getRegistryName(), "inventory"));
        }
        if (CommonProxy.ENCHANTMENT_EXTRACTOR != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.ENCHANTMENT_EXTRACTOR), 0,
                    new ModelResourceLocation(CommonProxy.ENCHANTMENT_EXTRACTOR.getRegistryName(), "inventory"));
        }
        if (CommonProxy.XP_VACUUM != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.XP_VACUUM), 0,
                    new ModelResourceLocation(CommonProxy.XP_VACUUM.getRegistryName(), "inventory"));
        }
        if (CommonProxy.BLOCK_BREAKER != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.BLOCK_BREAKER), 0,
                    new ModelResourceLocation(CommonProxy.BLOCK_BREAKER.getRegistryName(), "inventory"));
        }
        if (CommonProxy.BLOCK_PLACER != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.BLOCK_PLACER), 0,
                    new ModelResourceLocation(CommonProxy.BLOCK_PLACER.getRegistryName(), "inventory"));
        }
        if (CommonProxy.MOB_GRINDER != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.MOB_GRINDER), 0,
                    new ModelResourceLocation(CommonProxy.MOB_GRINDER.getRegistryName(), "inventory"));
        }
        if (CommonProxy.TRASH_CAN != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.TRASH_CAN), 0,
                    new ModelResourceLocation(CommonProxy.TRASH_CAN.getRegistryName(), "inventory"));
        }
        if (CommonProxy.AUTO_FISHER != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.AUTO_FISHER), 0,
                    new ModelResourceLocation(CommonProxy.AUTO_FISHER.getRegistryName(), "inventory"));
        }
        if (CommonProxy.VOID_MINER != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.VOID_MINER), 0,
                    new ModelResourceLocation(CommonProxy.VOID_MINER.getRegistryName(), "inventory"));
        }
        if (CommonProxy.SMART_ROUTER != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(CommonProxy.SMART_ROUTER), 0,
                    new ModelResourceLocation(CommonProxy.SMART_ROUTER.getRegistryName(), "inventory"));
        }

        // Fluid Tanks (all tiers)
        if (CommonProxy.FLUID_TANK != null) {
            for (TankTier tier : TankTier.values()) {
                ModelLoader.setCustomModelResourceLocation(
                        Item.getItemFromBlock(CommonProxy.FLUID_TANK), tier.getId(),
                        new ModelResourceLocation(CommonProxy.FLUID_TANK.getRegistryName(), "tier=" + tier.getName()));
            }
        }
    }
}

package com.antigravity.advancedsorter;

import com.antigravity.advancedsorter.proxy.CommonProxy;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

@Mod(modid = AdvancedSorterMod.MODID, name = AdvancedSorterMod.NAME, version = AdvancedSorterMod.VERSION, dependencies = "after:mekanism")
public class AdvancedSorterMod {
    public static final String MODID = "advancedsorter";
    public static final String NAME = "Advanced Sorter";
    public static final String VERSION = "2.1.0";

    // GUI IDs
    public static final int GUI_ADVANCED_SORTER = 0;
    public static final int GUI_DIRECTIONAL_PIPE = 1;
    public static final int GUI_TELEPORT_PIPE = 2;
    public static final int GUI_DIRECTIONAL_FLUID_PIPE = 3;
    public static final int GUI_TELEPORT_FLUID_PIPE = 4;
    public static final int GUI_EXTRACTION_FLUID_PIPE = 5;
    public static final int GUI_PUMP_CONTROLLER = 6;
    public static final int GUI_ADVANCED_PUMP = 7;
    public static final int GUI_CRAFTING_CALCULATOR = 8;
    public static final int GUI_INVENTORY_INDEX = 9;
    public static final int GUI_NETWORK_TOOL = 10;
    public static final int GUI_TELEPORT_GAS_PIPE = 11;
    public static final int GUI_AUTO_CRAFTER = 12;
    public static final int GUI_ADVANCED_TANK = 13;
    public static final int GUI_COBBLE_GENERATOR = 14;
    public static final int GUI_CRUCIBLE = 15;
    public static final int GUI_CRUSHER = 16;
    public static final int GUI_WASHER = 17;
    public static final int GUI_OBSIDIAN_GENERATOR = 18;
    public static final int GUI_DECONSTRUCTOR = 19;
    public static final int GUI_AUTO_REPAIRER = 20;
    public static final int GUI_AMMO_PRESS = 21;
    public static final int GUI_ATMOSPHERIC_STABILIZER = 22;
    public static final int GUI_FLINT_MAKER = 23;
    public static final int GUI_ENCHANTMENT_EXTRACTOR = 24;
    public static final int GUI_XP_VACUUM = 25;
    public static final int GUI_BLOCK_BREAKER = 26;
    public static final int GUI_BLOCK_PLACER = 27;
    public static final int GUI_MOB_GRINDER = 28;
    public static final int GUI_TRASH_CAN = 29;
    public static final int GUI_AUTO_FISHER = 30;
    public static final int GUI_VOID_MINER = 31;
    public static final int GUI_SMART_ROUTER = 32;

    // Creative Tab
    public static final CreativeTabs CREATIVE_TAB = new CreativeTabs(MODID) {
        @Override
        @SideOnly(Side.CLIENT)
        public ItemStack getTabIconItem() {
            return new ItemStack(com.antigravity.advancedsorter.proxy.CommonProxy.WRENCH);
        }
    };

    // Network
    public static SimpleNetworkWrapper network;

    @SidedProxy(clientSide = "com.antigravity.advancedsorter.proxy.ClientProxy", serverSide = "com.antigravity.advancedsorter.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.Instance
    public static AdvancedSorterMod instance;

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        net.minecraftforge.fml.common.network.NetworkRegistry.INSTANCE.registerGuiHandler(instance,
                new com.antigravity.advancedsorter.client.gui.GuiHandler());
        com.antigravity.advancedsorter.network.PacketHandler.registerMessages();
        com.antigravity.advancedsorter.util.ChunkLoadingHandler.register();
    }
}

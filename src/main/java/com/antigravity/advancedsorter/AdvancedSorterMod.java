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

@Mod(modid = AdvancedSorterMod.MODID, name = AdvancedSorterMod.NAME, version = AdvancedSorterMod.VERSION)
public class AdvancedSorterMod {
    public static final String MODID = "advancedsorter";
    public static final String NAME = "Advanced Sorter";
    public static final String VERSION = "1.0.0";

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

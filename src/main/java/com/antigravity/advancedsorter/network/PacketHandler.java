package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {
        public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE
                        .newSimpleChannel(AdvancedSorterMod.MODID);
        private static int packetId = 0;

        public static void registerMessages() {
                // Set the network reference in main mod for easy access
                AdvancedSorterMod.network = INSTANCE;

                INSTANCE.registerMessage(PacketUpdateRules.Handler.class, PacketUpdateRules.class, packetId++,
                                Side.SERVER);
                INSTANCE.registerMessage(PacketCyclePipeMode.Handler.class, PacketCyclePipeMode.class, packetId++,
                                Side.SERVER);
                INSTANCE.registerMessage(PacketUpdateTeleportPipe.Handler.class, PacketUpdateTeleportPipe.class,
                                packetId++,
                                Side.SERVER);
                INSTANCE.registerMessage(PacketUpdateTeleportFluidPipe.Handler.class,
                                PacketUpdateTeleportFluidPipe.class,
                                packetId++, Side.SERVER);
                INSTANCE.registerMessage(PacketUpdateTeleportGasPipe.Handler.class,
                                PacketUpdateTeleportGasPipe.class,
                                packetId++, Side.SERVER);
                INSTANCE.registerMessage(PacketTeleportInfo.Handler.class, PacketTeleportInfo.class, packetId++,
                                Side.CLIENT);
                INSTANCE.registerMessage(PacketRequestTeleportInfo.Handler.class, PacketRequestTeleportInfo.class,
                                packetId++,
                                Side.SERVER);
                INSTANCE.registerMessage(PacketUpdateExtractionPipe.Handler.class, PacketUpdateExtractionPipe.class,
                                packetId++, Side.SERVER);
                INSTANCE.registerMessage(PacketUpdatePumpController.Handler.class, PacketUpdatePumpController.class,
                                packetId++, Side.SERVER);
                INSTANCE.registerMessage(PacketUpdateAdvancedPump.Handler.class, PacketUpdateAdvancedPump.class,
                                packetId++, Side.SERVER);
                INSTANCE.registerMessage(PacketInventoryIndexSync.Handler.class, PacketInventoryIndexSync.class,
                                packetId++, Side.CLIENT);
                INSTANCE.registerMessage(PacketRequestItem.Handler.class, PacketRequestItem.class,
                                packetId++, Side.SERVER);
                INSTANCE.registerMessage(PacketManageChestGroup.Handler.class, PacketManageChestGroup.class,
                                packetId++, Side.SERVER);
                INSTANCE.registerMessage(PacketChestGroupSync.Handler.class, PacketChestGroupSync.class,
                                packetId++, Side.CLIENT);
                INSTANCE.registerMessage(PacketNetworkToolSync.Handler.class, PacketNetworkToolSync.class,
                                packetId++, Side.CLIENT);
        }
}

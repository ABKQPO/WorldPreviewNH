package com.hfstudio.preview.network;

import com.hfstudio.WorldPreviewNH;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

/**
 * Registers the mod's network channel and packet types.
 * Packet 0: SeedDataMessage (server → client) sends world seed info on login.
 */
public class NetworkHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(WorldPreviewNH.MODID);

    public static void init() {
        INSTANCE.registerMessage(SeedDataMessage.Handler.class, SeedDataMessage.class, 0, Side.CLIENT);
    }
}

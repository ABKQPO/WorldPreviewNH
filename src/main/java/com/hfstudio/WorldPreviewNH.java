package com.hfstudio;

import static com.hfstudio.WorldPreviewNH.MODID;
import static com.hfstudio.WorldPreviewNH.MODNAME;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = MODID,
    version = Tags.VERSION,
    name = MODNAME,
    acceptableRemoteVersions = "*",
    acceptedMinecraftVersions = "1.7.10")
public class WorldPreviewNH {

    @Mod.Instance(Tags.MODID)
    public static WorldPreviewNH instance;
    public static final String MODID = Tags.MODID;
    public static final String MODNAME = Tags.MODNAME;
    public static final String VERSION = Tags.VERSION;
    public static final String ARTHOR = "HFstudio";
    public static final Logger LOG = LogManager.getLogger(MODID);

    /** Debug mode: enables biome key export on first preview GUI open. */
    public static boolean debug = false;

    @SidedProxy(clientSide = "com.hfstudio.ClientProxy", serverSide = "com.hfstudio.CommonProxy")
    public static CommonProxy proxy;

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void completeInit(FMLLoadCompleteEvent event) {
        proxy.completeInit(event);
    }

    // register server commands in this event handler (Remove if not needed)
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {}

    @Mod.EventHandler
    public void onMissingMappings(FMLMissingMappingsEvent event) {}
}

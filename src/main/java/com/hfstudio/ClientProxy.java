package com.hfstudio;

import net.minecraftforge.common.MinecraftForge;

import com.hfstudio.preview.handler.CreateWorldHandler;
import com.hfstudio.preview.handler.SelectWorldHandler;
import com.hfstudio.preview.integration.createworldui.CreateWorldUiCompat;
import com.hfstudio.preview.network.ServerSeedData;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

public class ClientProxy extends CommonProxy {

    private final CreateWorldUiCompat createWorldUiCompat = new CreateWorldUiCompat();

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        createWorldUiCompat.registerPreviewTabIfAvailable();
        MinecraftForge.EVENT_BUS.register(new CreateWorldHandler());
        MinecraftForge.EVENT_BUS.register(new SelectWorldHandler());
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ServerSeedData.clear();
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    @Override
    public void completeInit(FMLLoadCompleteEvent event) {
        super.completeInit(event);
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }
}

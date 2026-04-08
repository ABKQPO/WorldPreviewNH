package com.hfstudio.preview.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

/**
 * Server-side event handler that sends the world seed to each player on login.
 * This allows remote clients to preview the world map without being in singleplayer.
 */
public class ServerSeedSender {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP player)) return;

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.worldServers == null || server.worldServers.length == 0) return;

        WorldServer overworld = server.worldServers[0];
        long seed = overworld.getSeed();
        WorldType worldType = overworld.getWorldInfo()
            .getTerrainType();
        String worldTypeName = worldType != null ? worldType.getWorldTypeName() : "DEFAULT";
        String genOptions = overworld.getWorldInfo()
            .getGeneratorOptions();

        NetworkHandler.INSTANCE.sendTo(new SeedDataMessage(seed, worldTypeName, genOptions), player);
    }
}

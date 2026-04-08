package com.hfstudio.preview.network;

import net.minecraft.world.WorldType;

/**
 * Client-side storage for world seed data received from the server.
 * When populated, the in-game menu preview button will be shown even in multiplayer.
 * Cleared on disconnect.
 */
public class ServerSeedData {

    private static volatile boolean available = false;
    private static volatile long seed;
    private static volatile String worldTypeName = "DEFAULT";
    private static volatile String generatorOptions = "";

    public static void set(long seed, String worldTypeName, String generatorOptions) {
        ServerSeedData.seed = seed;
        ServerSeedData.worldTypeName = worldTypeName != null ? worldTypeName : "DEFAULT";
        ServerSeedData.generatorOptions = generatorOptions != null ? generatorOptions : "";
        ServerSeedData.available = true;
    }

    public static void clear() {
        available = false;
        seed = 0;
        worldTypeName = "DEFAULT";
        generatorOptions = "";
    }

    public static boolean isAvailable() {
        return available;
    }

    public static long getSeed() {
        return seed;
    }

    public static WorldType getWorldType() {
        for (WorldType wt : WorldType.worldTypes) {
            if (wt != null && wt.getWorldTypeName()
                .equals(worldTypeName)) {
                return wt;
            }
        }
        return WorldType.DEFAULT;
    }

    public static String getGeneratorOptions() {
        return generatorOptions;
    }
}

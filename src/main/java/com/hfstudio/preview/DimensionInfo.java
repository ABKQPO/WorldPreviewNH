package com.hfstudio.preview;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.biome.WorldChunkManagerHell;
import net.minecraftforge.common.DimensionManager;

import com.github.bsideup.jabel.Desugar;

/**
 * Information about a dimension for the preview GUI.
 * Provides dimension name and the ability to create a WorldChunkManager for sampling.
 */
@Desugar
public record DimensionInfo(int dimensionId, String displayName) {

    /**
     * Collects all registered dimensions. Safe to call from the client thread.
     */
    public static List<DimensionInfo> getAllDimensions() {
        List<DimensionInfo> result = new ArrayList<>();
        try {
            Integer[] dimIds = DimensionManager.getStaticDimensionIDs();
            if (dimIds == null) return result;

            for (int dimId : dimIds) {
                try {
                    WorldProvider provider = DimensionManager.createProviderFor(dimId);
                    if (provider != null) {
                        String name = provider.getDimensionName();
                        if (name == null || name.isEmpty()) {
                            name = "Dimension " + dimId;
                        }
                        result.add(new DimensionInfo(dimId, name));
                    }
                } catch (Exception ignored) {
                    // Some mod dimensions may fail to instantiate without a world
                    result.add(new DimensionInfo(dimId, "Dimension " + dimId));
                }
            }
        } catch (Exception ignored) {}

        // Sort: overworld (0) first, then nether (-1), end (1), others ascending
        result.sort((a, b) -> {
            int pa = sortPriority(a.dimensionId);
            int pb = sortPriority(b.dimensionId);
            if (pa != pb) return Integer.compare(pa, pb);
            return Integer.compare(a.dimensionId, b.dimensionId);
        });

        return result;
    }

    private static int sortPriority(int dimId) {
        if (dimId == 0) return 0;
        if (dimId == -1) return 1;
        if (dimId == 1) return 2;
        return 3;
    }

    /**
     * Try to create a WorldChunkManager for this dimension.
     * First tries to get the actual WCM from the loaded server world (supports mod biomes).
     * Falls back to creating a new WCM based on dimension type.
     */
    public WorldChunkManager createChunkManager(long seed, net.minecraft.world.WorldType worldType) {
        // Try to get the WCM from the loaded server world (in-game preview with mod biomes)
        WorldChunkManager fromServer = getChunkManagerFromServer();
        if (fromServer != null) {
            return fromServer;
        }

        // For overworld, use the standard constructor
        if (dimensionId == 0) {
            return new WorldChunkManager(seed, worldType);
        }

        // For nether and end, use their fixed single-biome managers
        if (dimensionId == -1) {
            return new WorldChunkManagerHell(BiomeGenBase.hell, 0.0F);
        }
        if (dimensionId == 1) {
            return new WorldChunkManagerHell(BiomeGenBase.sky, 0.0F);
        }

        // For mod dimensions, try to create a provider and get its chunk manager type
        try {
            WorldProvider provider = DimensionManager.createProviderFor(dimensionId);
            if (provider != null) {
                // Check if it's a hell-type provider (single biome)
                if (provider instanceof net.minecraft.world.WorldProviderHell) {
                    return new WorldChunkManagerHell(BiomeGenBase.hell, 0.0F);
                }
                if (provider instanceof net.minecraft.world.WorldProviderEnd) {
                    return new WorldChunkManagerHell(BiomeGenBase.sky, 0.0F);
                }
            }
        } catch (Exception ignored) {}

        // Fallback: use overworld chunk manager
        return new WorldChunkManager(seed, worldType);
    }

    /**
     * Try to get the WorldChunkManager from a loaded server world.
     * This supports mod-added biomes (e.g. Biomes O' Plenty Nether biomes).
     */
    private WorldChunkManager getChunkManagerFromServer() {
        try {
            MinecraftServer server = MinecraftServer.getServer();
            if (server == null) return null;
            WorldServer world = DimensionManager.getWorld(dimensionId);
            if (world != null) {
                WorldChunkManager wcm = world.getWorldChunkManager();
                if (wcm != null) {
                    return wcm;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}

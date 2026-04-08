package com.hfstudio.preview;

import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Lightweight World subclass that provides just enough state
 * (seed, worldType, generatorOptions) for {@code WorldType.getChunkManager(World)}
 * to create the correct WorldChunkManager.
 * <p>
 * Uses the client-side World constructor which skips heavy initialization
 * (chunk loading, provider registration, etc.).
 */
@SideOnly(Side.CLIENT)
public class PreviewDummyWorld extends World {

    public PreviewDummyWorld(long seed, WorldType worldType, String generatorOptions) {
        super(
            null,
            "preview",
            new WorldProviderSurface(),
            createSettings(seed, worldType, generatorOptions),
            new Profiler());
    }

    private static WorldSettings createSettings(long seed, WorldType worldType, String generatorOptions) {
        WorldSettings settings = new WorldSettings(seed, WorldSettings.GameType.SURVIVAL, true, false, worldType);
        if (generatorOptions != null && !generatorOptions.isEmpty()) {
            settings.func_82750_a(generatorOptions);
        }
        return settings;
    }

    @Override
    protected IChunkProvider createChunkProvider() {
        return null;
    }

    @Override
    protected int func_152379_p() {
        return 0;
    }

    @Override
    public Entity getEntityByID(int id) {
        return null;
    }
}

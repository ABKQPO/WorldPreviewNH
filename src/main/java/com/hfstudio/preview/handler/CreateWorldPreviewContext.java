package com.hfstudio.preview.handler;

import net.minecraft.world.WorldType;

/**
 * Immutable preview input resolved from a create-world screen.
 */
public class CreateWorldPreviewContext {

    private final long seed;
    private final String displaySeed;
    private final WorldType worldType;
    private final String generatorOptions;

    public CreateWorldPreviewContext(long seed, String displaySeed, WorldType worldType, String generatorOptions) {
        this.seed = seed;
        this.displaySeed = displaySeed;
        this.worldType = worldType;
        this.generatorOptions = generatorOptions;
    }

    public long seed() {
        return seed;
    }

    public String displaySeed() {
        return displaySeed;
    }

    public WorldType worldType() {
        return worldType;
    }

    public String generatorOptions() {
        return generatorOptions;
    }
}

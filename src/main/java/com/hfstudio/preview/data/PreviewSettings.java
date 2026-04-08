package com.hfstudio.preview.data;

/**
 * Holds all preview settings. Shared between GUI screens.
 */
public class PreviewSettings {

    /** Blocks per pixel - zoom level. Powers of 2 from 4 to 64. */
    public int blocksPerPixel = 16;

    /** Whether to sample and display height map data. */
    public boolean heightSampling = false;

    /** Whether to show Y-layer intersection (block colors at current Y level). */
    public boolean yIntersection = false;

    /** Current Y level for height/intersection display. */
    public int currentY = 64;

    /** Show cave biomes with full color when biome highlighting is active. */
    public boolean showCaves = false;

    /** Which render mode is active. */
    public RenderMode renderMode = RenderMode.BIOMES;

    /** Minimum Y for height sampling. */
    public int minY = 0;

    /** Maximum Y for height sampling. */
    public int maxY = 256;

    /** Y scroll step size. */
    public static final int Y_BLOCK_STRIDE = 8;

    public void incrementY() {
        currentY = Math.min(maxY, currentY + Y_BLOCK_STRIDE);
    }

    public void decrementY() {
        currentY = Math.max(minY, currentY - Y_BLOCK_STRIDE);
    }

    public enum RenderMode {
        BIOMES,
        HEIGHTMAP,
        INTERSECTIONS
    }
}

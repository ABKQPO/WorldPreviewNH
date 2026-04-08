package com.hfstudio.preview.biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.biome.WorldChunkManagerHell;

import com.hfstudio.preview.data.DimensionInfo;

import lombok.Getter;

/**
 * Tile-based biome sampler with persistent cache and multi-threaded generation.
 * Each tile covers TILE_SIZE x TILE_SIZE pixels. Tiles are cached so previously
 * generated areas don't need regeneration when panning. During drag, already-cached
 * tiles are displayed immediately; missing tiles are queued with center-first priority.
 */
public class BiomeSampler {

    /** Tile size in pixels. Each tile is independently cached. */
    public static final int TILE_SIZE = 64;

    @Getter
    private WorldChunkManager chunkManager;
    @Getter
    private long currentSeed;
    @Getter
    private WorldType currentWorldType;
    @Getter
    private int currentDimensionId = 0;
    private String currentGeneratorOptions = "";
    private int currentBlocksPerPixel = -1;

    // Thread pool for parallel tile generation
    private ExecutorService executor;
    private static final int THREAD_COUNT = Math.max(
        1,
        Runtime.getRuntime()
            .availableProcessors() - 1);

    // Tile cache: key = "tileX,tileZ" at current zoom level
    private final Map<Long, int[]> tileCache = new ConcurrentHashMap<>();

    // Composite output written by composeTiles, read by GUI
    private volatile int[] compositeData;
    private volatile int compositeWidth;
    private volatile int compositeHeight;

    // Current viewport params (updated each requestViewport call)
    private volatile int vpStartBlockX, vpStartBlockZ;
    private volatile int vpPixelWidth, vpPixelHeight;
    private volatile int vpBlocksPerPixel;

    // Generation tracking
    private final AtomicBoolean generationActive = new AtomicBoolean(false);
    private final AtomicInteger pendingTiles = new AtomicInteger(0);
    private final AtomicInteger generationEpoch = new AtomicInteger(0);
    private final Map<Long, Future<?>> activeFutures = new ConcurrentHashMap<>();

    public void setup(long seed, WorldType worldType) {
        setup(seed, worldType, 0, "");
    }

    public void setup(long seed, WorldType worldType, int dimensionId) {
        setup(seed, worldType, dimensionId, "");
    }

    public void setup(long seed, WorldType worldType, int dimensionId, String generatorOptions) {
        String opts = generatorOptions != null ? generatorOptions : "";
        if (seed == currentSeed && worldType == currentWorldType
            && dimensionId == currentDimensionId
            && opts.equals(currentGeneratorOptions)
            && chunkManager != null) {
            return;
        }
        this.currentSeed = seed;
        this.currentWorldType = worldType;
        this.currentDimensionId = dimensionId;
        this.currentGeneratorOptions = opts;
        this.currentBlocksPerPixel = -1;
        DimensionInfo dimInfo = new DimensionInfo(dimensionId, "");
        this.chunkManager = dimInfo.createChunkManager(seed, worldType, opts);
        clearCache();
    }

    /**
     * Set up using an existing WorldChunkManager (for in-game preview).
     */
    public void setupFromExisting(WorldChunkManager wcm, long seed, WorldType worldType) {
        this.currentSeed = seed;
        this.currentWorldType = worldType;
        this.currentBlocksPerPixel = -1;
        this.chunkManager = wcm;
        clearCache();
    }

    /**
     * Clear tile cache (e.g. on seed change or zoom change).
     */
    public void clearCache() {
        cancelPending();
        tileCache.clear();
        compositeData = null;
    }

    /**
     * Request a viewport update. Composes cached tiles immediately into the output
     * buffer. Missing tiles are queued for async generation with center-first priority.
     * Tiles already being generated that overlap the new viewport are kept alive.
     *
     * @param onTileComplete called on the EDT when any new tile finishes (to trigger texture update)
     */
    public void requestViewport(int centerBlockX, int centerBlockZ, int pixelWidth, int pixelHeight, int blocksPerPixel,
        Runnable onTileComplete) {
        if (chunkManager == null) return;

        // If zoom changed, invalidate cache
        if (blocksPerPixel != currentBlocksPerPixel) {
            cancelPending();
            tileCache.clear();
            currentBlocksPerPixel = blocksPerPixel;
        }

        ensureExecutor();

        // Calculate which tiles cover the viewport
        int startBlockX = centerBlockX - (pixelWidth / 2) * blocksPerPixel;
        int startBlockZ = centerBlockZ - (pixelHeight / 2) * blocksPerPixel;

        // Store current viewport params for late-arriving tiles
        vpStartBlockX = startBlockX;
        vpStartBlockZ = startBlockZ;
        vpPixelWidth = pixelWidth;
        vpPixelHeight = pixelHeight;
        vpBlocksPerPixel = blocksPerPixel;

        // Tile coordinates: each tile covers TILE_SIZE * blocksPerPixel world blocks
        int tileBlockSize = TILE_SIZE * blocksPerPixel;
        int tileStartX = Math.floorDiv(startBlockX, tileBlockSize);
        int tileStartZ = Math.floorDiv(startBlockZ, tileBlockSize);
        int tileEndX = Math.floorDiv(startBlockX + pixelWidth * blocksPerPixel - 1, tileBlockSize);
        int tileEndZ = Math.floorDiv(startBlockZ + pixelHeight * blocksPerPixel - 1, tileBlockSize);

        // Cancel only out-of-viewport futures, keep in-viewport tiles alive
        cancelOutOfViewport(tileStartX, tileEndX, tileStartZ, tileEndZ);

        // Compose existing cached tiles into output
        int[] output = new int[pixelWidth * pixelHeight];
        List<long[]> missingTiles = new ArrayList<>(); // [tileKey, tileX, tileZ]

        for (int tz = tileStartZ; tz <= tileEndZ; tz++) {
            for (int tx = tileStartX; tx <= tileEndX; tx++) {
                long key = tileKey(tx, tz);
                int[] cached = tileCache.get(key);

                // Calculate where this tile maps to in the output
                int tileWorldX = tx * tileBlockSize;
                int tileWorldZ = tz * tileBlockSize;

                if (cached != null) {
                    blitTileToOutput(
                        cached,
                        tileWorldX,
                        tileWorldZ,
                        output,
                        startBlockX,
                        startBlockZ,
                        pixelWidth,
                        pixelHeight,
                        blocksPerPixel);
                } else if (!activeFutures.containsKey(key)) {
                    // Only queue tiles that aren't already being generated
                    missingTiles.add(new long[] { key, tx, tz });
                }
            }
        }

        compositeData = output;
        compositeWidth = pixelWidth;
        compositeHeight = pixelHeight;

        // Queue missing tiles sorted by distance from center (spiral outward)
        if (!missingTiles.isEmpty()) {
            int centerTileX = Math.floorDiv(centerBlockX, tileBlockSize);
            int centerTileZ = Math.floorDiv(centerBlockZ, tileBlockSize);

            missingTiles.sort((a, b) -> {
                long distA = (a[1] - centerTileX) * (a[1] - centerTileX) + (a[2] - centerTileZ) * (a[2] - centerTileZ);
                long distB = (b[1] - centerTileX) * (b[1] - centerTileX) + (b[2] - centerTileZ) * (b[2] - centerTileZ);
                return Long.compare(distA, distB);
            });

            generationActive.set(true);
            pendingTiles.addAndGet(missingTiles.size());

            final int fBlocksPerPixel = blocksPerPixel;

            for (long[] tile : missingTiles) {
                final long tKey = tile[0];
                final int tX = (int) tile[1];
                final int tZ = (int) tile[2];

                Future<?> f = executor.submit(() -> {
                    // Clear any stale interrupt flag from previous cancellation
                    Thread.interrupted();
                    try {
                        int[] tileData = generateTile(tX, tZ, fBlocksPerPixel);

                        // Only cache and blit if zoom hasn't changed since submission
                        if (currentBlocksPerPixel == fBlocksPerPixel) {
                            tileCache.put(tKey, tileData);
                            blitTileToComposite(tileData, tX, tZ, fBlocksPerPixel);

                            if (onTileComplete != null) {
                                onTileComplete.run();
                            }
                        }
                    } catch (Exception e) {
                        if (!(e instanceof InterruptedException)) {
                            e.printStackTrace();
                        }
                    } finally {
                        activeFutures.remove(tKey);
                        if (pendingTiles.decrementAndGet() <= 0) {
                            generationActive.set(false);
                        }
                    }
                });
                activeFutures.put(tKey, f);
            }
        }
    }

    /**
     * Generate a single tile's biome data.
     * Vanilla WCM: batch all rows in one call (GenLayer is fast, sync overhead dominates).
     * Modded WCMs (RWG etc): row-by-row to avoid massive over-sampling at block coords.
     */
    private int[] generateTile(int tileX, int tileZ, int blocksPerPixel) {
        int[] result = new int[TILE_SIZE * TILE_SIZE];
        int tileBlockSize = TILE_SIZE * blocksPerPixel;
        int worldX = tileX * tileBlockSize;
        int worldZ = tileZ * tileBlockSize;

        // Vanilla WorldChunkManager.getBiomesForGeneration() uses GenLayer coordinates
        // where 1 unit = 4 blocks (quarter-block resolution).
        // Modded WCMs (e.g. RWG's ChunkManagerRealistic) override getBiomesForGeneration
        // to use block coordinates (1 unit = 1 block).
        boolean useQuartCoords = chunkManager.getClass() == WorldChunkManager.class;
        int coordScale = useQuartCoords ? 4 : 1;
        int startX = worldX / coordScale;
        int startZ = worldZ / coordScale;
        int samplesPerRow = Math.max(1, TILE_SIZE * blocksPerPixel / coordScale);

        if (useQuartCoords) {
            // Vanilla: batch all rows in one call (GenLayer is fast, sync overhead dominates)
            int samplesPerCol = samplesPerRow;
            BiomeGenBase[] batchBuffer;
            synchronized (chunkManager) {
                batchBuffer = chunkManager.getBiomesForGeneration(null, startX, startZ, samplesPerRow, samplesPerCol);
            }
            for (int py = 0; py < TILE_SIZE; py++) {
                int sampleZ = py * blocksPerPixel / coordScale;
                for (int px = 0; px < TILE_SIZE; px++) {
                    int sampleX = px * blocksPerPixel / coordScale;
                    int sampleIdx = sampleZ * samplesPerRow + sampleX;
                    if (sampleIdx >= 0 && sampleIdx < batchBuffer.length && batchBuffer[sampleIdx] != null) {
                        result[py * TILE_SIZE + px] = batchBuffer[sampleIdx].biomeID;
                    }
                }
            }
        } else {
            // Modded WCMs (RWG etc): row-by-row to avoid O(n^2) over-sampling.
            // At blocksPerPixel=16, batch would request 1024x1024=1M samples
            // vs row-by-row requesting 64x1024=65K — a 16x difference.
            BiomeGenBase[] rowBuffer = null;
            for (int py = 0; py < TILE_SIZE; py++) {
                int rowZ = startZ + py * blocksPerPixel / coordScale;
                synchronized (chunkManager) {
                    rowBuffer = chunkManager.getBiomesForGeneration(rowBuffer, startX, rowZ, samplesPerRow, 1);
                }
                for (int px = 0; px < TILE_SIZE; px++) {
                    int sampleIdx = px * blocksPerPixel / coordScale;
                    if (sampleIdx < rowBuffer.length && rowBuffer[sampleIdx] != null) {
                        result[py * TILE_SIZE + px] = rowBuffer[sampleIdx].biomeID;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Blit a tile into the current composite using the latest viewport params.
     * Safe to call from background threads — reads volatile viewport fields.
     * Skips blit if the tile's zoom level doesn't match the current viewport
     * (the tile is still cached and will be picked up on next requestViewport).
     */
    private void blitTileToComposite(int[] tileData, int tileX, int tileZ, int blocksPerPixel) {
        int[] composite = compositeData;
        if (composite == null) return;
        int currentBpp = vpBlocksPerPixel;
        // Skip if tile was generated at a different zoom level
        if (blocksPerPixel != currentBpp) return;
        int pw = vpPixelWidth;
        int ph = vpPixelHeight;
        if (composite.length != pw * ph) return;
        int tileWorldX = tileX * TILE_SIZE * blocksPerPixel;
        int tileWorldZ = tileZ * TILE_SIZE * blocksPerPixel;
        blitTileToOutput(tileData, tileWorldX, tileWorldZ, composite, vpStartBlockX, vpStartBlockZ, pw, ph, currentBpp);
    }

    /**
     * Copy tile biome data into the correct position in the output buffer.
     * Uses System.arraycopy for row-level copying.
     */
    private void blitTileToOutput(int[] tileData, int tileWorldX, int tileWorldZ, int[] output, int viewStartBlockX,
        int viewStartBlockZ, int outWidth, int outHeight, int blocksPerPixel) {
        // Pixel offset of this tile in the output
        int outOffsetX = (tileWorldX - viewStartBlockX) / blocksPerPixel;
        int outOffsetZ = (tileWorldZ - viewStartBlockZ) / blocksPerPixel;

        // Calculate visible overlap region
        int srcStartX = Math.max(0, -outOffsetX);
        int srcStartZ = Math.max(0, -outOffsetZ);
        int srcEndX = Math.min(TILE_SIZE, outWidth - outOffsetX);
        int srcEndZ = Math.min(TILE_SIZE, outHeight - outOffsetZ);
        int copyLen = srcEndX - srcStartX;

        if (copyLen <= 0) return;

        for (int ty = srcStartZ; ty < srcEndZ; ty++) {
            int oy = outOffsetZ + ty;
            System.arraycopy(
                tileData,
                ty * TILE_SIZE + srcStartX,
                output,
                oy * outWidth + outOffsetX + srcStartX,
                copyLen);
        }
    }

    /**
     * Cancel futures for tiles outside the given viewport bounds.
     * Tiles inside the viewport keep running so they finish and populate the cache.
     */
    private void cancelOutOfViewport(int tileStartX, int tileEndX, int tileStartZ, int tileEndZ) {
        activeFutures.entrySet()
            .removeIf(entry -> {
                long key = entry.getKey();
                int tx = (int) (key >> 32);
                int tz = (int) key;
                if (tx < tileStartX || tx > tileEndX || tz < tileStartZ || tz > tileEndZ) {
                    entry.getValue()
                        .cancel(true);
                    pendingTiles.decrementAndGet();
                    return true;
                }
                return false;
            });
    }

    private void cancelPending() {
        generationEpoch.incrementAndGet();
        for (Future<?> f : activeFutures.values()) {
            f.cancel(true);
        }
        activeFutures.clear();
        pendingTiles.set(0);
        generationActive.set(false);
    }

    private void ensureExecutor() {
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newFixedThreadPool(THREAD_COUNT, r -> {
                Thread t = new Thread(r, "WorldPreview-Sampler");
                t.setDaemon(true);
                return t;
            });
        }
    }

    private static long tileKey(int tileX, int tileZ) {
        return ((long) tileX << 32) | (tileZ & 0xFFFFFFFFL);
    }

    // --- Public accessors ---

    public int[] getBiomeIds() {
        return compositeData;
    }

    public int getDataWidth() {
        return compositeWidth;
    }

    public int getDataHeight() {
        return compositeHeight;
    }

    public boolean isSampling() {
        return generationActive.get();
    }

    public boolean isSingleBiomeDimension() {
        return chunkManager instanceof WorldChunkManagerHell;
    }

    public int getCacheSize() {
        return tileCache.size();
    }

    public void shutdown() {
        cancelPending();
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        tileCache.clear();
    }
}

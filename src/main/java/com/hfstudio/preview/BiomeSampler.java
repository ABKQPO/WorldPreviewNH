package com.hfstudio.preview;

import java.util.ArrayList;
import java.util.Collections;
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

    // Generation tracking
    private final AtomicBoolean generationActive = new AtomicBoolean(false);
    private final AtomicInteger pendingTiles = new AtomicInteger(0);
    private volatile boolean cancelled = false;
    private final List<Future<?>> activeFutures = Collections.synchronizedList(new ArrayList<>());

    public void setup(long seed, WorldType worldType) {
        setup(seed, worldType, 0);
    }

    public void setup(long seed, WorldType worldType, int dimensionId) {
        if (seed == currentSeed && worldType == currentWorldType
            && dimensionId == currentDimensionId
            && chunkManager != null) {
            return;
        }
        this.currentSeed = seed;
        this.currentWorldType = worldType;
        this.currentDimensionId = dimensionId;
        this.currentBlocksPerPixel = -1;
        DimensionInfo dimInfo = new DimensionInfo(dimensionId, "");
        this.chunkManager = dimInfo.createChunkManager(seed, worldType);
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
     *
     * @param onTileComplete called on the EDT when any new tile finishes (to trigger texture update)
     */
    public void requestViewport(int centerBlockX, int centerBlockZ, int pixelWidth, int pixelHeight, int blocksPerPixel,
        Runnable onTileComplete) {
        if (chunkManager == null) return;

        // If zoom changed, invalidate cache
        if (blocksPerPixel != currentBlocksPerPixel) {
            tileCache.clear();
            currentBlocksPerPixel = blocksPerPixel;
        }

        ensureExecutor();
        cancelPending();

        // Calculate which tiles cover the viewport
        int startBlockX = centerBlockX - (pixelWidth / 2) * blocksPerPixel;
        int startBlockZ = centerBlockZ - (pixelHeight / 2) * blocksPerPixel;

        // Tile coordinates: each tile covers TILE_SIZE * blocksPerPixel world blocks
        int tileBlockSize = TILE_SIZE * blocksPerPixel;
        int tileStartX = Math.floorDiv(startBlockX, tileBlockSize);
        int tileStartZ = Math.floorDiv(startBlockZ, tileBlockSize);
        int tileEndX = Math.floorDiv(startBlockX + pixelWidth * blocksPerPixel - 1, tileBlockSize);
        int tileEndZ = Math.floorDiv(startBlockZ + pixelHeight * blocksPerPixel - 1, tileBlockSize);

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
                } else {
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
            cancelled = false;
            pendingTiles.set(missingTiles.size());

            // Capture viewport params for re-composition after each tile completes
            final int fStartBlockX = startBlockX;
            final int fStartBlockZ = startBlockZ;
            final int fPixelWidth = pixelWidth;
            final int fPixelHeight = pixelHeight;
            final int fBlocksPerPixel = blocksPerPixel;
            final int fTileStartX = tileStartX;
            final int fTileEndX = tileEndX;
            final int fTileStartZ = tileStartZ;
            final int fTileEndZ = tileEndZ;

            for (long[] tile : missingTiles) {
                final long tKey = tile[0];
                final int tX = (int) tile[1];
                final int tZ = (int) tile[2];

                Future<?> f = executor.submit(() -> {
                    // Clear any stale interrupt flag from previous cancellation
                    Thread.interrupted();
                    if (cancelled) return;
                    int maxRetries = 3;
                    for (int attempt = 1; attempt <= maxRetries; attempt++) {
                        if (cancelled) return;
                        try {
                            int[] tileData = generateTile(tX, tZ, fBlocksPerPixel);
                            if (!cancelled) {
                                tileCache.put(tKey, tileData);

                                // Recompose the full output from all relevant cached tiles
                                recompose(
                                    fStartBlockX,
                                    fStartBlockZ,
                                    fPixelWidth,
                                    fPixelHeight,
                                    fBlocksPerPixel,
                                    fTileStartX,
                                    fTileEndX,
                                    fTileStartZ,
                                    fTileEndZ);

                                if (onTileComplete != null) {
                                    onTileComplete.run();
                                }
                            }
                            break; // success, exit retry loop
                        } catch (Exception e) {
                            if (e instanceof InterruptedException || cancelled) {
                                break;
                            }
                            if (attempt < maxRetries) {
                                try {
                                    Thread.sleep(200L * attempt);
                                } catch (InterruptedException ie) {
                                    break;
                                }
                            } else {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (pendingTiles.decrementAndGet() <= 0) {
                        generationActive.set(false);
                    }
                });
                activeFutures.add(f);
            }
        }
    }

    /**
     * Generate a single tile's biome data.
     */
    private int[] generateTile(int tileX, int tileZ, int blocksPerPixel) {
        int[] result = new int[TILE_SIZE * TILE_SIZE];
        int tileBlockSize = TILE_SIZE * blocksPerPixel;
        int worldX = tileX * tileBlockSize;
        int worldZ = tileZ * tileBlockSize;

        int quartsPerPixel = Math.max(1, blocksPerPixel >> 2);
        int startQuartX = worldX >> 2;
        int startQuartZ = worldZ >> 2;
        int totalQuartW = TILE_SIZE * quartsPerPixel;

        BiomeGenBase[] rowBuffer = null;
        for (int py = 0; py < TILE_SIZE; py++) {
            if (cancelled || Thread.currentThread()
                .isInterrupted()) return result;

            int quartZ = startQuartZ + py * quartsPerPixel;
            // Synchronize on chunkManager since it's not thread-safe
            synchronized (chunkManager) {
                rowBuffer = chunkManager.getBiomesForGeneration(rowBuffer, startQuartX, quartZ, totalQuartW, 1);
            }

            for (int px = 0; px < TILE_SIZE; px++) {
                int quartIdx = px * quartsPerPixel;
                if (quartIdx < rowBuffer.length && rowBuffer[quartIdx] != null) {
                    result[py * TILE_SIZE + px] = rowBuffer[quartIdx].biomeID;
                }
            }
        }
        return result;
    }

    /**
     * Recompose all cached tiles into the composite output buffer.
     */
    private void recompose(int startBlockX, int startBlockZ, int pixelWidth, int pixelHeight, int blocksPerPixel,
        int tileStartX, int tileEndX, int tileStartZ, int tileEndZ) {
        int[] output = new int[pixelWidth * pixelHeight];
        int tileBlockSize = TILE_SIZE * blocksPerPixel;

        for (int tz = tileStartZ; tz <= tileEndZ; tz++) {
            for (int tx = tileStartX; tx <= tileEndX; tx++) {
                int[] cached = tileCache.get(tileKey(tx, tz));
                if (cached != null) {
                    int tileWorldX = tx * tileBlockSize;
                    int tileWorldZ = tz * tileBlockSize;
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
                }
            }
        }

        compositeData = output;
        compositeWidth = pixelWidth;
        compositeHeight = pixelHeight;
    }

    /**
     * Copy tile biome data into the correct position in the output buffer.
     */
    private void blitTileToOutput(int[] tileData, int tileWorldX, int tileWorldZ, int[] output, int viewStartBlockX,
        int viewStartBlockZ, int outWidth, int outHeight, int blocksPerPixel) {
        // Pixel offset of this tile in the output
        int outOffsetX = (tileWorldX - viewStartBlockX) / blocksPerPixel;
        int outOffsetZ = (tileWorldZ - viewStartBlockZ) / blocksPerPixel;

        for (int ty = 0; ty < TILE_SIZE; ty++) {
            int oy = outOffsetZ + ty;
            if (oy < 0 || oy >= outHeight) continue;

            for (int tx = 0; tx < TILE_SIZE; tx++) {
                int ox = outOffsetX + tx;
                if (ox < 0 || ox >= outWidth) continue;

                output[oy * outWidth + ox] = tileData[ty * TILE_SIZE + tx];
            }
        }
    }

    private void cancelPending() {
        cancelled = true;
        synchronized (activeFutures) {
            for (Future<?> f : activeFutures) {
                f.cancel(true);
            }
            activeFutures.clear();
        }
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

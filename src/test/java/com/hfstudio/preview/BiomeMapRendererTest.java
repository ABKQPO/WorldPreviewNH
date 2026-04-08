package com.hfstudio.preview;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;

import org.junit.jupiter.api.Test;

/**
 * Debug tool for rendering biome maps to PNG files.
 * Can be run as a JUnit test or as a standalone program with CLI arguments.
 *
 * <p>
 * CLI usage: {@code java BiomeMapRendererTest [options]}
 * <ul>
 * <li>{@code --seed <value>} - World seed (number or string, default: 0)</li>
 * <li>{@code --type <name>} - World type name (default, flat, largeBiomes, amplified, default: default)</li>
 * <li>{@code --centerX <value>} - Center block X coordinate (default: 0)</li>
 * <li>{@code --centerZ <value>} - Center block Z coordinate (default: 0)</li>
 * <li>{@code --zoom <value>} - Blocks per pixel (default: 16)</li>
 * <li>{@code --width <value>} - Image width in pixels (default: 1024)</li>
 * <li>{@code --height <value>} - Image height in pixels (default: 1024)</li>
 * <li>{@code --output <path>} - Output PNG file path (default: biome_map.png)</li>
 * <li>{@code --mode <biomes|heightmap|intersection>} - Render mode (default: biomes)</li>
 * <li>{@code --y <value>} - Y level for intersection mode (default: 64)</li>
 * <li>{@code --legend} - Draw biome legend on the image</li>
 * <li>{@code --grid} - Draw coordinate grid overlay</li>
 * </ul>
 */
public class BiomeMapRendererTest {

    // Hardcoded biome colors (same as BiomeColorMap but standalone, no MC dependency for color lookup)
    private static final Map<Integer, Integer> BIOME_COLORS = new HashMap<>();

    static {
        put(0, 0x000070); // ocean
        put(1, 0x8DB360); // plains
        put(2, 0xFA9418); // desert
        put(3, 0x606060); // extremeHills
        put(4, 0x056621); // forest
        put(5, 0x0B6659); // taiga
        put(6, 0x07F9B2); // swampland
        put(7, 0x0000FF); // river
        put(8, 0xFF0000); // hell
        put(9, 0x8080FF); // sky (end)
        put(10, 0x9090A0); // frozenOcean
        put(11, 0xA0A0FF); // frozenRiver
        put(12, 0xFFFFFF); // icePlains
        put(13, 0xA0A0A0); // iceMountains
        put(14, 0xFF00FF); // mushroomIsland
        put(15, 0xA000FF); // mushroomIslandShore
        put(16, 0xFADE55); // beach
        put(17, 0xD25F12); // desertHills
        put(18, 0x22551C); // forestHills
        put(19, 0x163933); // taigaHills
        put(20, 0x72789A); // extremeHillsEdge
        put(21, 0x537B09); // jungle
        put(22, 0x2C4205); // jungleHills
        put(23, 0x628B17); // jungleEdge
        put(24, 0x000030); // deepOcean
        put(25, 0xA2A284); // stoneBeach
        put(26, 0xFAF0C0); // coldBeach
        put(27, 0x307444); // birchForest
        put(28, 0x1F5F32); // birchForestHills
        put(29, 0x40511A); // roofedForest
        put(30, 0x31554A); // coldTaiga
        put(31, 0x243F36); // coldTaigaHills
        put(32, 0x596651); // megaTaiga
        put(33, 0x545F3E); // megaTaigaHills
        put(34, 0x507050); // extremeHillsPlus
        put(35, 0xBDB25F); // savanna
        put(36, 0xA79D64); // savannaPlateau
        put(37, 0xD94515); // mesa
        put(38, 0xB09765); // mesaPlateau_F
        put(39, 0xCA8C65); // mesaPlateau
        put(129, 0xB5DB88); // sunflowerPlains
        put(130, 0xFFBC40); // desertM
        put(131, 0x888888); // extremeHillsM
        put(132, 0x2D8E49); // flowerForest
        put(133, 0x338E81); // taigaM
        put(134, 0x2FFFDA); // swamplandM
        put(140, 0xB4DCE0); // icePlainsSpikes
        put(149, 0x7BA331); // jungleM
        put(151, 0x8AB33F); // jungleEdgeM
        put(155, 0x589C6C); // birchForestM
        put(156, 0x47875A); // birchForestHillsM
        put(157, 0x687942); // roofedForestM
        put(158, 0x597D72); // coldTaigaM
        put(160, 0x6B7D7B); // megaSpruceTaiga
        put(161, 0x6B6F5E); // megaSpruceTaigaHills
        put(162, 0x789878); // extremeHillsPlusM
        put(163, 0xE5DA87); // savannaM
        put(164, 0xCFC58C); // savannaPlateauM
        put(165, 0xFF6D3D); // mesaBryce
        put(166, 0xD8BF8D); // mesaPlateauFM
        put(167, 0xF2B48D); // mesaPlateauM
    }

    private static void put(int id, int color) {
        BIOME_COLORS.put(id, color);
    }

    /**
     * Default JUnit test: renders a 1024x1024 biome map with seed 0, centered at 0,0.
     */
    @Test
    void renderDefaultMap() throws Exception {
        renderMap(
            0L,
            WorldType.DEFAULT,
            0,
            0,
            16,
            1024,
            1024,
            "biomes",
            64,
            true,
            true,
            new File("run/test_output/biome_map_default.png"));
        System.out.println("Rendered: run/test_output/biome_map_default.png");
    }

    @Test
    void renderLargeBiomesMap() throws Exception {
        renderMap(
            12345L,
            WorldType.LARGE_BIOMES,
            0,
            0,
            32,
            1024,
            1024,
            "biomes",
            64,
            true,
            true,
            new File("run/test_output/biome_map_large.png"));
        System.out.println("Rendered: run/test_output/biome_map_large.png");
    }

    @Test
    void renderHeightmap() throws Exception {
        renderMap(
            0L,
            WorldType.DEFAULT,
            0,
            0,
            16,
            1024,
            1024,
            "heightmap",
            64,
            true,
            false,
            new File("run/test_output/biome_map_heightmap.png"));
        System.out.println("Rendered: run/test_output/biome_map_heightmap.png");
    }

    @Test
    void renderIntersection() throws Exception {
        renderMap(
            0L,
            WorldType.DEFAULT,
            0,
            0,
            16,
            1024,
            1024,
            "intersection",
            64,
            true,
            false,
            new File("run/test_output/biome_map_intersection_y64.png"));
        System.out.println("Rendered: run/test_output/biome_map_intersection_y64.png");
    }

    /**
     * CLI entry point for custom renders.
     */
    public static void main(String[] args) throws Exception {
        long seed = 0;
        String typeName = "default";
        int centerX = 0, centerZ = 0;
        int zoom = 16;
        int width = 1024, height = 1024;
        String output = "biome_map.png";
        String mode = "biomes";
        int yLevel = 64;
        boolean legend = false;
        boolean grid = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--seed":
                    String seedStr = args[++i];
                    try {
                        seed = Long.parseLong(seedStr);
                    } catch (NumberFormatException e) {
                        seed = seedStr.hashCode();
                    }
                    break;
                case "--type":
                    typeName = args[++i];
                    break;
                case "--centerX":
                    centerX = Integer.parseInt(args[++i]);
                    break;
                case "--centerZ":
                    centerZ = Integer.parseInt(args[++i]);
                    break;
                case "--zoom":
                    zoom = Integer.parseInt(args[++i]);
                    break;
                case "--width":
                    width = Integer.parseInt(args[++i]);
                    break;
                case "--height":
                    height = Integer.parseInt(args[++i]);
                    break;
                case "--output":
                    output = args[++i];
                    break;
                case "--mode":
                    mode = args[++i];
                    break;
                case "--y":
                    yLevel = Integer.parseInt(args[++i]);
                    break;
                case "--legend":
                    legend = true;
                    break;
                case "--grid":
                    grid = true;
                    break;
                default:
                    System.err.println("Unknown option: " + args[i]);
                    printUsage();
                    return;
            }
        }

        WorldType worldType = findWorldType(typeName);
        if (worldType == null) {
            System.err.println("Unknown world type: " + typeName);
            System.err.println("Available types:");
            for (WorldType wt : WorldType.worldTypes) {
                if (wt != null && wt.getCanBeCreated()) {
                    System.err.println("  " + wt.getWorldTypeName());
                }
            }
            return;
        }

        System.out.printf(
            "Rendering: seed=%d, type=%s, center=(%d,%d), zoom=%d, size=%dx%d, mode=%s%n",
            seed,
            worldType.getWorldTypeName(),
            centerX,
            centerZ,
            zoom,
            width,
            height,
            mode);

        File outputFile = new File(output);
        renderMap(seed, worldType, centerX, centerZ, zoom, width, height, mode, yLevel, legend, grid, outputFile);

        System.out.println("Output: " + outputFile.getAbsolutePath());
    }

    private static void printUsage() {
        System.err.println("Usage: BiomeMapRendererTest [options]");
        System.err.println("  --seed <value>     World seed (default: 0)");
        System.err.println("  --type <name>      World type (default: default)");
        System.err.println("  --centerX <value>  Center X (default: 0)");
        System.err.println("  --centerZ <value>  Center Z (default: 0)");
        System.err.println("  --zoom <value>     Blocks per pixel (default: 16)");
        System.err.println("  --width <value>    Image width (default: 1024)");
        System.err.println("  --height <value>   Image height (default: 1024)");
        System.err.println("  --output <path>    Output PNG path (default: biome_map.png)");
        System.err.println("  --mode <mode>      biomes|heightmap|intersection (default: biomes)");
        System.err.println("  --y <value>        Y level for intersection mode (default: 64)");
        System.err.println("  --legend           Draw biome color legend");
        System.err.println("  --grid             Draw coordinate grid");
    }

    private static WorldType findWorldType(String name) {
        for (WorldType wt : WorldType.worldTypes) {
            if (wt != null && wt.getWorldTypeName()
                .equalsIgnoreCase(name)) {
                return wt;
            }
        }
        return null;
    }

    /**
     * Core rendering. Samples biomes and writes a PNG.
     */
    private static void renderMap(long seed, WorldType worldType, int centerX, int centerZ, int blocksPerPixel,
        int imgWidth, int imgHeight, String mode, int yLevel, boolean drawLegend, boolean drawGrid, File outputFile)
        throws Exception {

        outputFile.getParentFile()
            .mkdirs();

        WorldChunkManager wcm = new WorldChunkManager(seed, worldType);

        // Sample biomes
        int startBlockX = centerX - (imgWidth / 2) * blocksPerPixel;
        int startBlockZ = centerZ - (imgHeight / 2) * blocksPerPixel;

        int quartsPerPixel = Math.max(1, blocksPerPixel >> 2);
        int startQuartX = startBlockX >> 2;
        int startQuartZ = startBlockZ >> 2;
        int totalQuartW = imgWidth * quartsPerPixel;

        int[] biomeIds = new int[imgWidth * imgHeight];
        BiomeGenBase[] rowBuffer = null;

        long t0 = System.currentTimeMillis();
        for (int py = 0; py < imgHeight; py++) {
            int quartZ = startQuartZ + py * quartsPerPixel;
            rowBuffer = wcm.getBiomesForGeneration(rowBuffer, startQuartX, quartZ, totalQuartW, 1);

            for (int px = 0; px < imgWidth; px++) {
                int quartIdx = px * quartsPerPixel;
                if (quartIdx < rowBuffer.length && rowBuffer[quartIdx] != null) {
                    biomeIds[py * imgWidth + px] = rowBuffer[quartIdx].biomeID;
                }
            }

            if (py % 256 == 0 && py > 0) {
                System.out.printf("  Sampled %d/%d rows...%n", py, imgHeight);
            }
        }
        long samplingMs = System.currentTimeMillis() - t0;
        System.out.printf("  Sampling done in %d ms%n", samplingMs);

        // Render to image
        BufferedImage image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
        BiomeGenBase[] allBiomes = BiomeGenBase.getBiomeGenArray();

        // Collect seen biomes for legend
        Map<Integer, String> seenBiomes = new HashMap<>();

        for (int i = 0; i < biomeIds.length; i++) {
            int biomeId = biomeIds[i];
            int color;

            switch (mode) {
                case "heightmap":
                    color = computeHeightmapColor(biomeId, allBiomes);
                    break;
                case "intersection":
                    color = computeIntersectionColor(biomeId, allBiomes, yLevel);
                    break;
                default:
                    color = getBiomeColor(biomeId, allBiomes);
                    break;
            }

            image.setRGB(i % imgWidth, i / imgWidth, color);

            if (drawLegend && !seenBiomes.containsKey(biomeId)) {
                BiomeGenBase biome = (biomeId >= 0 && biomeId < allBiomes.length) ? allBiomes[biomeId] : null;
                seenBiomes.put(biomeId, biome != null ? biome.biomeName : "ID:" + biomeId);
            }
        }

        Graphics2D g2d = image.createGraphics();

        // Draw grid
        if (drawGrid) {
            g2d.setColor(new Color(255, 255, 255, 60));
            // Grid lines every 512 blocks
            int gridSpacing = 512 / blocksPerPixel;
            if (gridSpacing < 8) gridSpacing = 1024 / blocksPerPixel;
            if (gridSpacing >= 8) {
                int offsetX = ((centerX - (imgWidth / 2) * blocksPerPixel) % (gridSpacing * blocksPerPixel));
                int startPx = -offsetX / blocksPerPixel;
                if (startPx < 0) startPx += gridSpacing;
                for (int px = startPx; px < imgWidth; px += gridSpacing) {
                    g2d.drawLine(px, 0, px, imgHeight - 1);
                }
                int offsetZ = ((centerZ - (imgHeight / 2) * blocksPerPixel) % (gridSpacing * blocksPerPixel));
                int startPz = -offsetZ / blocksPerPixel;
                if (startPz < 0) startPz += gridSpacing;
                for (int pz = startPz; pz < imgHeight; pz += gridSpacing) {
                    g2d.drawLine(0, pz, imgWidth - 1, pz);
                }
            }

            // Crosshair at center
            int cx = imgWidth / 2;
            int cy = imgHeight / 2;
            g2d.setColor(new Color(255, 255, 255, 180));
            g2d.drawLine(cx - 8, cy, cx + 8, cy);
            g2d.drawLine(cx, cy - 8, cx, cy + 8);
        }

        // Draw legend
        if (drawLegend && !seenBiomes.isEmpty()) {
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
            int legendX = 6;
            int legendY = 6;
            int entryHeight = 14;
            int maxEntries = Math.min(seenBiomes.size(), (imgHeight - 12) / entryHeight);

            // Sort by biome ID
            List<Map.Entry<Integer, String>> sortedBiomes = new ArrayList<>(seenBiomes.entrySet());
            sortedBiomes.sort(Comparator.comparingInt(Map.Entry::getKey));
            if (sortedBiomes.size() > maxEntries) {
                sortedBiomes = sortedBiomes.subList(0, maxEntries);
            }

            // Background
            int legendWidth = 160;
            int legendHeight = sortedBiomes.size() * entryHeight + 6;
            g2d.setColor(new Color(0, 0, 0, 160));
            g2d.fillRect(legendX - 2, legendY - 2, legendWidth, legendHeight);

            for (Map.Entry<Integer, String> entry : sortedBiomes) {
                int biomeId = entry.getKey();
                String name = entry.getValue();
                int color = getBiomeColor(biomeId, allBiomes);

                // Color swatch
                g2d.setColor(new Color(color));
                g2d.fillRect(legendX, legendY, 10, 10);
                g2d.setColor(new Color(80, 80, 80));
                g2d.drawRect(legendX, legendY, 10, 10);

                // Text
                g2d.setColor(Color.WHITE);
                g2d.drawString(String.format("%d: %s", biomeId, name), legendX + 14, legendY + 10);
                legendY += entryHeight;
            }
        }

        // Info bar at top-right
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        String info = String.format(
            "Seed:%d  Type:%s  Center:(%d,%d)  Zoom:%d blk/px",
            seed,
            worldType.getWorldTypeName(),
            centerX,
            centerZ,
            blocksPerPixel);
        if ("intersection".equals(mode)) {
            info += "  Y:" + yLevel;
        }
        int infoWidth = g2d.getFontMetrics()
            .stringWidth(info);
        g2d.setColor(new Color(0, 0, 0, 160));
        g2d.fillRect(imgWidth - infoWidth - 12, 0, infoWidth + 12, 20);
        g2d.setColor(Color.WHITE);
        g2d.drawString(info, imgWidth - infoWidth - 6, 14);

        g2d.dispose();

        ImageIO.write(image, "PNG", outputFile);
    }

    private static int getBiomeColor(int biomeId, BiomeGenBase[] allBiomes) {
        Integer color = BIOME_COLORS.get(biomeId);
        if (color != null) return color;

        BiomeGenBase biome = (biomeId >= 0 && biomeId < allBiomes.length) ? allBiomes[biomeId] : null;
        if (biome != null) {
            if (biome.color != 0) {
                BIOME_COLORS.put(biomeId, biome.color);
                return biome.color;
            }
            float temp = biome.temperature;
            float rain = biome.rainfall;
            float h = (1.0f - temp) * 0.7f;
            float s = 0.3f + rain * 0.5f;
            float b = 0.4f + rain * 0.3f + temp * 0.2f;
            int generated = Color.HSBtoRGB(h, Math.min(1f, s), Math.min(1f, b)) & 0xFFFFFF;
            BIOME_COLORS.put(biomeId, generated);
            return generated;
        }

        int hash = biomeId * 0x9E3779B9;
        int fallback = ((hash >> 16) & 0xFF) << 16 | ((hash >> 8) & 0xFF) << 8 | (hash & 0xFF);
        BIOME_COLORS.put(biomeId, fallback);
        return fallback;
    }

    private static int computeHeightmapColor(int biomeId, BiomeGenBase[] allBiomes) {
        BiomeGenBase biome = (biomeId >= 0 && biomeId < allBiomes.length) ? allBiomes[biomeId] : null;
        if (biome == null) return 0x000000;

        float surfaceHeight = 63.0f + biome.rootHeight * 17.0f;
        float t = Math.max(0.0f, Math.min(1.0f, surfaceHeight / 256.0f));

        int r, g, b;
        if (t < 0.25f) {
            float s = t / 0.25f;
            r = (int) (20 + s * 30);
            g = (int) (40 + s * 80);
            b = (int) (120 + s * 60);
        } else if (t < 0.5f) {
            float s = (t - 0.25f) / 0.25f;
            r = (int) (50 + s * 80);
            g = (int) (120 + s * 60);
            b = (int) (40 - s * 20);
        } else if (t < 0.75f) {
            float s = (t - 0.5f) / 0.25f;
            r = (int) (130 + s * 60);
            g = (int) (120 - s * 30);
            b = (int) (20 + s * 30);
        } else {
            float s = (t - 0.75f) / 0.25f;
            r = (int) (190 + s * 65);
            g = (int) (190 + s * 65);
            b = (int) (190 + s * 65);
        }

        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int computeIntersectionColor(int biomeId, BiomeGenBase[] allBiomes, int currentY) {
        BiomeGenBase biome = (biomeId >= 0 && biomeId < allBiomes.length) ? allBiomes[biomeId] : null;
        if (biome == null) return 0x000000;

        float surfaceHeight = 63.0f + biome.rootHeight * 17.0f;
        float variation = biome.heightVariation * 8.0f;

        int baseColor = getBiomeColor(biomeId, allBiomes);
        int cr = (baseColor >> 16) & 0xFF;
        int cg = (baseColor >> 8) & 0xFF;
        int cb = baseColor & 0xFF;

        if (currentY > surfaceHeight + variation) {
            cr = Math.min(255, cr + 100);
            cg = Math.min(255, cg + 100);
            cb = Math.min(255, cb + 100);
        } else if (currentY < surfaceHeight - variation) {
            cr = cr * 40 / 100;
            cg = cg * 40 / 100;
            cb = cb * 40 / 100;
        }

        return (cr << 16) | (cg << 8) | cb;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}

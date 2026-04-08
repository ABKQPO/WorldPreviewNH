package com.hfstudio.preview;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.biome.WorldChunkManagerHell;

import org.junit.jupiter.api.Test;

import com.hfstudio.preview.data.DimensionInfo;
import com.hfstudio.preview.data.PreviewDummyWorld;

/**
 * Tests that DimensionInfo correctly delegates to custom WorldType.getChunkManager(World),
 * ensuring modded world types (e.g., RWG) produce the correct WorldChunkManager
 * instead of falling back to vanilla GenLayer-based WCM.
 */
public class ModWorldTypeTest {

    private static final long TEST_SEED = 12346L;
    private static final int IMG_SIZE = 512;
    private static final int BLOCKS_PER_PIXEL = 16;

    // ---- Custom WorldType that overrides getChunkManager(World) ----

    /**
     * A test WorldType that returns a single-biome WCM (desert)
     * via getChunkManager(World), simulating how a mod like RWG
     * overrides this method to return a custom WCM.
     */
    static class TestCustomWorldType extends WorldType {

        TestCustomWorldType() {
            super("testcustom");
        }

        @Override
        public WorldChunkManager getChunkManager(World world) {
            // Simulate a mod that creates a custom WCM using the world's seed.
            // Return a single-biome WCM so we can easily verify it was used.
            long seed = world.getSeed();
            // Use seed to deterministically pick a biome
            BiomeGenBase biome = (seed % 2 == 0) ? BiomeGenBase.desert : BiomeGenBase.icePlains;
            return new WorldChunkManagerHell(biome, 0.5F);
        }

        @Override
        public boolean getCanBeCreated() {
            return false; // don't show in GUI
        }
    }

    /**
     * Verify that PreviewDummyWorld correctly provides seed, worldType, and generatorOptions.
     */
    @Test
    void testPreviewDummyWorldProperties() {
        long seed = 42L;
        WorldType worldType = WorldType.DEFAULT;
        String generatorOptions = "test_options";

        PreviewDummyWorld world = new PreviewDummyWorld(seed, worldType, generatorOptions);

        assertEquals(seed, world.getSeed(), "PreviewDummyWorld should return the correct seed");
        assertNotNull(world.getWorldInfo(), "PreviewDummyWorld should have non-null WorldInfo");
        assertEquals(
            seed,
            world.getWorldInfo()
                .getSeed(),
            "WorldInfo should have the correct seed");
        assertEquals(
            worldType,
            world.getWorldInfo()
                .getTerrainType(),
            "WorldInfo should have the correct terrain type");
        assertEquals(
            generatorOptions,
            world.getWorldInfo()
                .getGeneratorOptions(),
            "WorldInfo should have the correct generator options");

        System.out.println(
            "PreviewDummyWorld properties verified: seed=" + seed
                + ", type="
                + worldType.getWorldTypeName()
                + ", options="
                + generatorOptions);
    }

    /**
     * Verify that DimensionInfo.createChunkManager() uses worldType.getChunkManager(World)
     * instead of falling back to vanilla new WorldChunkManager(seed, worldType).
     */
    @Test
    void testCustomWorldTypeDelegation() {
        TestCustomWorldType customType = new TestCustomWorldType();
        DimensionInfo dimInfo = new DimensionInfo(0, "Overworld");

        // Even seed → desert biome
        WorldChunkManager wcm = dimInfo.createChunkManager(TEST_SEED, customType, "");
        assertNotNull(wcm, "WCM should not be null");
        assertTrue(
            wcm instanceof WorldChunkManagerHell,
            "DimensionInfo should have used custom WorldType's getChunkManager(), got: " + wcm.getClass()
                .getName());

        // Sample a biome to verify it's the expected one (desert for even seed)
        BiomeGenBase[] biomes = wcm.getBiomesForGeneration(null, 0, 0, 1, 1);
        assertNotNull(biomes);
        assertEquals(1, biomes.length);
        assertEquals(
            BiomeGenBase.desert.biomeID,
            biomes[0].biomeID,
            "Custom WCM should return desert biome for even seed");

        // Odd seed → icePlains biome
        WorldChunkManager wcm2 = dimInfo.createChunkManager(TEST_SEED + 1, customType, "");
        BiomeGenBase[] biomes2 = wcm2.getBiomesForGeneration(null, 0, 0, 1, 1);
        assertEquals(
            BiomeGenBase.icePlains.biomeID,
            biomes2[0].biomeID,
            "Custom WCM should return icePlains biome for odd seed");

        System.out.println("Custom WorldType delegation verified successfully");
    }

    /**
     * Verify that vanilla WorldTypes also work correctly through the PreviewDummyWorld path.
     */
    @Test
    void testVanillaWorldTypeThroughDummyWorld() {
        DimensionInfo dimInfo = new DimensionInfo(0, "Overworld");

        // Test DEFAULT world type
        WorldChunkManager wcm = dimInfo.createChunkManager(TEST_SEED, WorldType.DEFAULT, "");
        assertNotNull(wcm, "WCM for DEFAULT should not be null");
        BiomeGenBase[] biomes = wcm.getBiomesForGeneration(null, 0, 0, 4, 4);
        assertNotNull(biomes);
        assertEquals(16, biomes.length);
        for (BiomeGenBase b : biomes) {
            assertNotNull(b, "All biome entries should be non-null");
        }

        // Test FLAT world type
        WorldChunkManager wcmFlat = dimInfo.createChunkManager(TEST_SEED, WorldType.FLAT, "");
        assertNotNull(wcmFlat, "WCM for FLAT should not be null");
        assertTrue(wcmFlat instanceof WorldChunkManagerHell, "FLAT world type should return WorldChunkManagerHell");

        System.out.println("Vanilla WorldTypes through PreviewDummyWorld verified");
    }

    /**
     * Verify that the PreviewDummyWorld path produces the same biome data
     * as the vanilla path for the DEFAULT world type.
     */
    @Test
    void testVanillaBiomeConsistency() {
        long seed = 42L;

        // Old path: direct constructor
        WorldChunkManager wcmDirect = new WorldChunkManager(seed, WorldType.DEFAULT);

        // New path: via PreviewDummyWorld
        DimensionInfo dimInfo = new DimensionInfo(0, "Overworld");
        WorldChunkManager wcmDummy = dimInfo.createChunkManager(seed, WorldType.DEFAULT, "");

        // Sample a large area and compare
        int size = 64;
        BiomeGenBase[] directBiomes = wcmDirect.getBiomesForGeneration(null, -size / 2, -size / 2, size, size);
        BiomeGenBase[] dummyBiomes = wcmDummy.getBiomesForGeneration(null, -size / 2, -size / 2, size, size);

        assertEquals(directBiomes.length, dummyBiomes.length);
        int matches = 0;
        for (int i = 0; i < directBiomes.length; i++) {
            if (directBiomes[i].biomeID == dummyBiomes[i].biomeID) {
                matches++;
            }
        }
        assertEquals(
            directBiomes.length,
            matches,
            "All biomes should match between direct WCM and PreviewDummyWorld WCM");

        System.out.println("Vanilla biome consistency verified: " + matches + "/" + directBiomes.length + " match");
    }

    /**
     * Try to find and use the RWG WorldType at runtime (if available).
     * Renders comparison maps: vanilla WCM vs worldType.getChunkManager() WCM.
     */
    @Test
    void testRwgWorldTypeIfAvailable() throws Exception {
        // Try to find "RWG" in registered WorldTypes
        WorldType rwgType = findWorldType("RWG");
        if (rwgType == null) {
            // Not loaded in test env — try reflective class loading
            rwgType = tryLoadRwgWorldType();
        }

        if (rwgType == null) {
            System.out.println("RWG WorldType not available in test environment — skipping RWG-specific test");
            System.out.println("This test will pass when RWG is on the classpath (e.g., runClient)");
            return;
        }

        System.out.println(
            "Found RWG WorldType: " + rwgType.getWorldTypeName()
                + " (class: "
                + rwgType.getClass()
                    .getName()
                + ")");

        // Ensure RWG Support biome lists are initialized (normally done in FML postInit)
        initRwgSupport();

        File outputDir = new File("run/test_output");
        outputDir.mkdirs();

        // Render using vanilla WCM path (old broken approach for RWG)
        WorldChunkManager vanillaWcm = new WorldChunkManager(TEST_SEED, rwgType);
        renderBiomeMap(vanillaWcm, TEST_SEED, "RWG-vanilla-WCM", new File(outputDir, "rwg_vanilla_wcm.png"));

        // Render using worldType.getChunkManager() path (correct approach)
        DimensionInfo dimInfo = new DimensionInfo(0, "Overworld");
        WorldChunkManager modWcm = dimInfo.createChunkManager(TEST_SEED, rwgType, "");
        String modWcmClass = modWcm.getClass()
            .getName();
        renderBiomeMap(modWcm, TEST_SEED, "RWG-mod-WCM (" + modWcmClass + ")", new File(outputDir, "rwg_mod_wcm.png"));

        System.out.println(
            "RWG vanilla WCM class: " + vanillaWcm.getClass()
                .getName());
        System.out.println("RWG mod WCM class:     " + modWcmClass);

        // If RWG properly overrides getChunkManager, the mod WCM should be a different class
        if (!modWcmClass.equals(
            vanillaWcm.getClass()
                .getName())) {
            System.out.println("SUCCESS: RWG returned a custom WCM class — biomes should differ from vanilla");
        } else {
            System.out
                .println("WARNING: mod WCM is same class as vanilla — worldType.getChunkManager() may have failed");
        }
    }

    /**
     * Renders a comparison map for any registered modded WorldType that overrides getChunkManager.
     * Outputs both the vanilla-path and mod-path maps for visual comparison.
     */
    @Test
    void renderAllModdedWorldTypeMaps() throws Exception {
        File outputDir = new File("run/test_output");
        outputDir.mkdirs();
        int rendered = 0;

        for (WorldType wt : WorldType.worldTypes) {
            if (wt == null || !wt.getCanBeCreated()) continue;
            if (wt == WorldType.DEFAULT || wt == WorldType.FLAT
                || wt == WorldType.LARGE_BIOMES
                || wt == WorldType.AMPLIFIED) {
                continue; // skip vanilla types
            }

            String name = wt.getWorldTypeName();
            System.out.println(
                "Found modded WorldType: " + name
                    + " (class: "
                    + wt.getClass()
                        .getName()
                    + ")");

            try {
                // Vanilla WCM path
                WorldChunkManager vanillaWcm = new WorldChunkManager(TEST_SEED, wt);
                renderBiomeMap(
                    vanillaWcm,
                    TEST_SEED,
                    name + " (vanilla WCM)",
                    new File(outputDir, "modtype_" + name + "_vanilla.png"));

                // Mod WCM path via PreviewDummyWorld
                DimensionInfo dimInfo = new DimensionInfo(0, "Overworld");
                WorldChunkManager modWcm = dimInfo.createChunkManager(TEST_SEED, wt, "");
                renderBiomeMap(
                    modWcm,
                    TEST_SEED,
                    name + " (mod WCM: "
                        + modWcm.getClass()
                            .getSimpleName()
                        + ")",
                    new File(outputDir, "modtype_" + name + "_mod.png"));

                System.out.println("  Rendered comparison maps for " + name);
                rendered++;
            } catch (Throwable t) {
                System.err.println("  Failed to render " + name + ": " + t.getMessage());
                t.printStackTrace();
            }
        }

        System.out.println("Rendered " + rendered + " modded WorldType comparison map pairs");
    }

    // ---- Helper methods ----

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
     * Try to reflectively instantiate the RWG WorldType if the class is on the classpath
     * but wasn't registered via FML mod loading.
     */
    private static WorldType tryLoadRwgWorldType() {
        try {
            Class<?> rwgClass = Class.forName("rwg.world.WorldTypeRealistic");
            // Check if already registered
            for (WorldType wt : WorldType.worldTypes) {
                if (wt != null && rwgClass.isInstance(wt)) {
                    return wt;
                }
            }
            // Try to instantiate — this will register it in WorldType.worldTypes
            Constructor<?> ctor = rwgClass.getConstructor(String.class);
            return (WorldType) ctor.newInstance("RWG");
        } catch (Throwable t) {
            System.out.println("Could not load RWG WorldType: " + t.getMessage());
            return null;
        }
    }

    /**
     * Initialize RWG's Support biome lists if they haven't been initialized yet.
     * In the actual game this is done during FML postInit; in tests we must do it manually.
     */
    private static void initRwgSupport() {
        try {
            Class<?> supportClass = Class.forName("rwg.support.Support");
            java.lang.reflect.Field snowField = supportClass.getField("biomes_snow");
            if (snowField.get(null) == null) {
                System.out.println("  Initializing RWG Support biome lists...");
                // Also need BaseBiomes loaded first
                try {
                    Class<?> baseBiomesClass = Class.forName("rwg.biomes.base.BaseBiomes");
                    baseBiomesClass.getMethod("load")
                        .invoke(null);
                    System.out.println("  BaseBiomes.load() completed");
                } catch (Throwable t) {
                    System.out.println("  Could not call BaseBiomes.load(): " + t.getMessage());
                }
                supportClass.getMethod("init")
                    .invoke(null);
                System.out.println("  Support.init() completed");
            }
        } catch (Throwable t) {
            System.out.println("  Could not initialize RWG Support: " + t.getMessage());
        }
    }

    // ---- Biome map rendering ----

    private static final Map<Integer, Integer> BIOME_COLORS = new HashMap<>();
    static {
        put(0, 0x000070);
        put(1, 0x8DB360);
        put(2, 0xFA9418);
        put(3, 0x606060);
        put(4, 0x056621);
        put(5, 0x0B6659);
        put(6, 0x07F9B2);
        put(7, 0x0000FF);
        put(10, 0x9090A0);
        put(11, 0xA0A0FF);
        put(12, 0xFFFFFF);
        put(13, 0xA0A0A0);
        put(14, 0xFF00FF);
        put(16, 0xFADE55);
        put(17, 0xD25F12);
        put(18, 0x22551C);
        put(21, 0x537B09);
        put(24, 0x000030);
        put(27, 0x307444);
        put(29, 0x40511A);
        put(30, 0x31554A);
        put(32, 0x596651);
        put(34, 0x507050);
        put(35, 0xBDB25F);
        put(37, 0xD94515);
        put(38, 0xB09765);
        put(39, 0xCA8C65);
    }

    private static void put(int id, int color) {
        BIOME_COLORS.put(id, color);
    }

    private static void renderBiomeMap(WorldChunkManager wcm, long seed, String label, File outputFile)
        throws Exception {
        outputFile.getParentFile()
            .mkdirs();

        // Use the same coordinate logic as BiomeSampler.generateTile():
        // Vanilla WCM uses GenLayer coords (1 unit = 4 blocks),
        // modded WCMs use block coords (1 unit = 1 block).
        boolean useQuartCoords = wcm.getClass() == WorldChunkManager.class;
        int coordScale = useQuartCoords ? 4 : 1;
        int unitsPerPixel = Math.max(1, BLOCKS_PER_PIXEL / coordScale);
        int startX = -(IMG_SIZE / 2) * unitsPerPixel;
        int startZ = startX;
        int rowWidth = IMG_SIZE * unitsPerPixel;

        int[] biomeIds = new int[IMG_SIZE * IMG_SIZE];
        BiomeGenBase[] rowBuffer = null;

        for (int py = 0; py < IMG_SIZE; py++) {
            int rowZ = startZ + py * unitsPerPixel;
            rowBuffer = wcm.getBiomesForGeneration(rowBuffer, startX, rowZ, rowWidth, 1);
            for (int px = 0; px < IMG_SIZE; px++) {
                int sampleIdx = px * unitsPerPixel;
                if (sampleIdx < rowBuffer.length && rowBuffer[sampleIdx] != null) {
                    biomeIds[py * IMG_SIZE + px] = rowBuffer[sampleIdx].biomeID;
                }
            }
        }

        BufferedImage image = new BufferedImage(IMG_SIZE, IMG_SIZE, BufferedImage.TYPE_INT_RGB);
        BiomeGenBase[] allBiomes = BiomeGenBase.getBiomeGenArray();

        for (int i = 0; i < biomeIds.length; i++) {
            image.setRGB(i % IMG_SIZE, i / IMG_SIZE, getBiomeColor(biomeIds[i], allBiomes));
        }

        // Draw label
        Graphics2D g2d = image.createGraphics();
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        String info = String.format(
            "Seed:%d  %s  WCM:%s",
            seed,
            label,
            wcm.getClass()
                .getSimpleName());
        int infoWidth = g2d.getFontMetrics()
            .stringWidth(info);
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, infoWidth + 12, 20);
        g2d.setColor(Color.WHITE);
        g2d.drawString(info, 6, 14);
        g2d.dispose();

        ImageIO.write(image, "PNG", outputFile);
        System.out.println("  Written: " + outputFile.getPath());
    }

    private static int getBiomeColor(int biomeId, BiomeGenBase[] allBiomes) {
        Integer color = BIOME_COLORS.get(biomeId);
        if (color != null) return color;

        BiomeGenBase biome = (biomeId >= 0 && biomeId < allBiomes.length) ? allBiomes[biomeId] : null;
        if (biome != null) {
            float temp = biome.temperature;
            float rain = biome.rainfall;
            int generated = Color.HSBtoRGB((1.0f - temp) * 0.7f, 0.3f + rain * 0.5f, 0.4f + rain * 0.3f + temp * 0.2f)
                & 0xFFFFFF;
            BIOME_COLORS.put(biomeId, generated);
            return generated;
        }

        int hash = biomeId * 0x9E3779B9;
        int fallback = ((hash >> 16) & 0xFF) << 16 | ((hash >> 8) & 0xFF) << 8 | (hash & 0xFF);
        BIOME_COLORS.put(biomeId, fallback);
        return fallback;
    }
}

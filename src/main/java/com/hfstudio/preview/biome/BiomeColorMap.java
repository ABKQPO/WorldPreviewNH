package com.hfstudio.preview.biome;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.biome.BiomeGenBase;

public class BiomeColorMap {

    private static final Map<Integer, Integer> BIOME_COLORS = new HashMap<>();
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Vanilla biomes - standard map colors (AMIDST-style)
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

        // Mutated / variant biomes (128+)
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

    private static void put(int biomeId, int color) {
        BIOME_COLORS.put(biomeId, color);
    }

    public static int getColor(int biomeId) {
        Integer color = BIOME_COLORS.get(biomeId);
        if (color != null) {
            return color;
        }

        // For mod biomes, try to generate a sensible color
        BiomeGenBase[] biomeArray = BiomeGenBase.getBiomeGenArray();
        BiomeGenBase biome = (biomeId >= 0 && biomeId < biomeArray.length) ? biomeArray[biomeId] : null;

        if (biome != null) {
            // Use the biome's own color field if non-zero
            if (biome.color != 0) {
                BIOME_COLORS.put(biomeId, biome.color);
                return biome.color;
            }

            // Generate color from temperature and rainfall using HSB space
            float temp = biome.temperature;
            float rain = biome.rainfall;
            float h = (1.0f - temp) * 0.7f;
            float s = 0.3f + rain * 0.5f;
            float b = 0.4f + rain * 0.3f + temp * 0.2f;
            int generated = Color.HSBtoRGB(h, Math.min(1f, s), Math.min(1f, b)) & 0xFFFFFF;
            BIOME_COLORS.put(biomeId, generated);
            return generated;
        }

        // Fallback: deterministic hash-based color
        int hash = biomeId * 0x9E3779B9;
        int fallback = ((hash >> 16) & 0xFF) << 16 | ((hash >> 8) & 0xFF) << 8 | (hash & 0xFF);
        BIOME_COLORS.put(biomeId, fallback);
        return fallback;
    }

    public static int getArgb(int biomeId) {
        return 0xFF000000 | getColor(biomeId);
    }
}

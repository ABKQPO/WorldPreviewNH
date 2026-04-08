package com.hfstudio.preview.biome;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import net.minecraft.client.Minecraft;
import net.minecraft.world.biome.BiomeGenBase;

import com.hfstudio.WorldPreviewNH;

/**
 * Exports all registered biome translation keys to a file for localization use.
 */
public class BiomeKeyExporter {

    private BiomeKeyExporter() {}

    /**
     * Export all biome translation keys to {@code worldpreview_biome_keys.lang} in the game directory.
     */
    public static void export() {
        File outputFile = new File(Minecraft.getMinecraft().mcDataDir, "worldpreview_biome_keys.lang");
        BiomeGenBase[] allBiomes = BiomeGenBase.getBiomeGenArray();

        int count = 0;
        try (PrintWriter writer = new PrintWriter(
            new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {

            writer.println("# World Preview NH - Biome Translation Keys");
            writer.println("# Auto-generated. Total registered biomes listed below.");
            writer.println();

            for (BiomeGenBase biome : allBiomes) {
                if (biome == null) continue;

                String key = "biome.worldpreview." + biome.biomeName.replace(' ', '_')
                    .toLowerCase();
                writer.println(key + "=" + biome.biomeName);
                count++;
            }

            WorldPreviewNH.LOG
                .info("[WorldPreview] Exported {} biome translation keys to {}", count, outputFile.getAbsolutePath());
        } catch (Exception e) {
            WorldPreviewNH.LOG.error("[WorldPreview] Failed to export biome keys", e);
        }
    }
}

package com.hfstudio.preview;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

/**
 * Persistent seed storage - saves/loads seeds to a file in the minecraft config directory.
 */
public class SeedStorage {

    private static final String FILE_NAME = "worldpreview_seeds.txt";
    private static final int MAX_SEEDS = 50;

    private static List<String> savedSeeds = null;
    private static File storageFile;

    private static File getFile() {
        if (storageFile == null) {
            storageFile = new File(Minecraft.getMinecraft().mcDataDir, "config/" + FILE_NAME);
        }
        return storageFile;
    }

    public static List<String> getSavedSeeds() {
        if (savedSeeds == null) {
            load();
        }
        return new ArrayList<>(savedSeeds);
    }

    public static void addSeed(String seed) {
        if (seed == null || seed.trim()
            .isEmpty()) return;
        seed = seed.trim();
        if (savedSeeds == null) load();

        // Remove if already exists (move to top)
        savedSeeds.remove(seed);
        savedSeeds.add(0, seed);

        // Trim to max
        while (savedSeeds.size() > MAX_SEEDS) {
            savedSeeds.remove(savedSeeds.size() - 1);
        }
        save();
    }

    public static void removeSeed(String seed) {
        if (savedSeeds == null) load();
        savedSeeds.remove(seed);
        save();
    }

    private static void load() {
        savedSeeds = new ArrayList<>();
        File f = getFile();
        if (!f.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    savedSeeds.add(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void save() {
        File f = getFile();
        try {
            f.getParentFile()
                .mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(f))) {
                writer.write("# WorldPreview saved seeds");
                writer.newLine();
                for (String seed : savedSeeds) {
                    writer.write(seed);
                    writer.newLine();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package com.hfstudio.preview.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.StatCollector;
import net.minecraft.world.biome.BiomeGenBase;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.preview.biome.BiomeColorMap;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import lombok.Getter;

/**
 * Scrollable biome list panel showing colored squares and biome names.
 * Supports biome highlighting (click to select/deselect).
 * Tracks hovered entry for tooltip rendering in the parent GUI.
 */
public class BiomeListSlot extends GuiSlot {

    private final GuiWorldPreview parent;
    private final List<BiomeEntry> allEntries = new ArrayList<>();
    private final List<BiomeEntry> entries = new ArrayList<>();
    private String filterText = "";
    private int selectedIndex = -1;
    private int hoveredIndex = -1;
    @Getter
    private int lastMouseX;
    @Getter
    private int lastMouseY;

    /** Cache: package prefix -> mod name. Built once. */
    private static Map<String, String> packageToModName;

    public BiomeListSlot(GuiWorldPreview parent, int width, int height, int top, int bottom) {
        super(Minecraft.getMinecraft(), width, height, top, bottom, 16);
        this.parent = parent;
        this.setShowSelectionBox(false);
        rebuildEntries();
    }

    public void setBounds(int width, int height, int top, int bottom) {
        this.width = width;
        this.height = height;
        this.top = top;
        this.bottom = bottom;
        this.left = 0;
        this.right = width;
    }

    /**
     * Rebuild entry list based on visible biomes from the current sampled data.
     */
    public void rebuildEntries() {
        allEntries.clear();
        BiomeGenBase[] allBiomes = BiomeGenBase.getBiomeGenArray();
        for (int i = 0; i < allBiomes.length; i++) {
            if (allBiomes[i] != null) {
                allEntries.add(createEntry(i, allBiomes[i]));
            }
        }
        applyFilter();
    }

    /**
     * Update entries using only biomes that appear in the current preview data.
     */
    public void updateFromSampledData(int[] biomeIds) {
        if (biomeIds == null) return;

        // Count occurrences
        java.util.Map<Integer, Integer> counts = new java.util.LinkedHashMap<>();
        for (int id : biomeIds) {
            counts.merge(id, 1, Integer::sum);
        }

        allEntries.clear();
        BiomeGenBase[] allBiomes = BiomeGenBase.getBiomeGenArray();

        // Sort by frequency descending
        List<java.util.Map.Entry<Integer, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        for (java.util.Map.Entry<Integer, Integer> e : sorted) {
            int id = e.getKey();
            BiomeGenBase biome = (id >= 0 && id < allBiomes.length) ? allBiomes[id] : null;
            if (biome != null) {
                allEntries.add(createEntry(id, biome));
            } else {
                allEntries.add(new BiomeEntry(id, "ID:" + id, BiomeColorMap.getColor(id), "Unknown", "unknown", false));
            }
        }

        applyFilter();
    }

    private static BiomeEntry createEntry(int id, BiomeGenBase biome) {
        String translationKey = "biome.worldpreview." + biome.biomeName.replace(' ', '_')
            .toLowerCase();
        boolean hasTranslation = StatCollector.canTranslate(translationKey);
        String displayName = hasTranslation ? StatCollector.translateToLocal(translationKey) : biome.biomeName;
        String modSource = getModSourceForBiome(biome);
        String className = biome.getClass()
            .getName();
        return new BiomeEntry(id, displayName, BiomeColorMap.getColor(id), modSource, className, hasTranslation);
    }

    /**
     * Set the search filter text. Entries whose name or biome ID don't match will be hidden.
     */
    public void setFilter(String text) {
        String normalized = text == null ? ""
            : text.trim()
                .toLowerCase();
        if (normalized.equals(filterText)) return;
        filterText = normalized;
        applyFilter();
    }

    private void applyFilter() {
        int prevSelectedBiomeId = getSelectedBiomeId();
        entries.clear();
        if (filterText.isEmpty()) {
            entries.addAll(allEntries);
        } else {
            for (BiomeEntry e : allEntries) {
                if (e.name()
                    .toLowerCase()
                    .contains(filterText)
                    || String.valueOf(e.biomeId())
                        .contains(filterText)
                    || e.modSource()
                        .toLowerCase()
                        .contains(filterText)) {
                    entries.add(e);
                }
            }
        }
        // Try to preserve selection
        selectedIndex = -1;
        if (prevSelectedBiomeId >= 0) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i)
                    .biomeId() == prevSelectedBiomeId) {
                    selectedIndex = i;
                    break;
                }
            }
        }
    }

    @Override
    protected int getSize() {
        return entries.size();
    }

    @Override
    protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY) {
        if (selectedIndex == index) {
            selectedIndex = -1; // deselect
        } else {
            selectedIndex = index;
        }
        parent.onBiomeSelected(getSelectedBiomeId());
    }

    @Override
    protected boolean isSelected(int index) {
        return index == selectedIndex;
    }

    @Override
    protected void drawBackground() {
        // Don't draw any background
    }

    @Override
    protected void drawSlot(int index, int x, int y, int slotHeight, Tessellator tess, int mouseX, int mouseY) {
        if (index < 0 || index >= entries.size()) return;
        BiomeEntry entry = entries.get(index);
        Minecraft mc = Minecraft.getMinecraft();

        // Track hover state
        if (mouseX >= this.left && mouseX < this.right - 6
            && mouseY >= y
            && mouseY < y + slotHeight
            && mouseY >= this.top
            && mouseY < this.bottom) {
            hoveredIndex = index;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }

        // Draw colored square (10x10)
        int squareX = this.left + 3;
        int squareY = y + 1;
        int color = 0xFF000000 | entry.color;

        // Fill colored rectangle
        Gui.drawRect(squareX, squareY, squareX + 10, squareY + 10, color);

        // Draw border around the square
        int borderColor = (index == selectedIndex) ? 0xFFFFFFFF : 0xFF404040;
        drawHorizontalLine(squareX - 1, squareX + 10, squareY - 1, borderColor);
        drawHorizontalLine(squareX - 1, squareX + 10, squareY + 10, borderColor);
        drawVerticalLine(squareX - 1, squareY - 1, squareY + 10, borderColor);
        drawVerticalLine(squareX + 10, squareY - 1, squareY + 10, borderColor);

        // Draw biome name
        String name = entry.name;
        int maxNameWidth = this.right - this.left - 20;
        if (mc.fontRenderer.getStringWidth(name) > maxNameWidth) {
            name = mc.fontRenderer.trimStringToWidth(name, maxNameWidth - 6) + "...";
        }

        int textColor = (index == selectedIndex) ? 0xFFFFFF : 0xC0C0C0;
        mc.fontRenderer.drawStringWithShadow(name, squareX + 14, squareY + 1, textColor);
    }

    /**
     * Custom drawScreen override that supports rendering at an offset position.
     */
    public void drawScreenAt(int offsetX, int listWidth, int mouseX, int mouseY, float partialTicks) {
        this.left = offsetX;
        this.right = offsetX + listWidth;
        hoveredIndex = -1; // Reset; will be set in drawSlot if applicable
        this.drawScreen(mouseX, mouseY, partialTicks);
    }

    /**
     * @return the currently hovered BiomeEntry, or null if none
     */
    public BiomeEntry getHoveredEntry() {
        if (hoveredIndex >= 0 && hoveredIndex < entries.size()) {
            return entries.get(hoveredIndex);
        }
        return null;
    }

    /**
     * Handle mouse input for the list at the given offset.
     */
    public void handleMouseInputAt(int offsetX, int listWidth) {
        this.left = offsetX;
        this.right = offsetX + listWidth;
    }

    @Override
    protected int getScrollBarX() {
        return this.right - 6;
    }

    @Override
    public int getListWidth() {
        return this.right - this.left - 8;
    }

    /**
     * @return the biome ID of the selected entry, or -1 if nothing selected
     */
    public int getSelectedBiomeId() {
        if (selectedIndex >= 0 && selectedIndex < entries.size()) {
            return entries.get(selectedIndex).biomeId;
        }
        return -1;
    }

    public void clearSelection() {
        selectedIndex = -1;
    }

    private static void drawHorizontalLine(int x1, int x2, int y, int color) {
        if (x2 < x1) {
            int t = x1;
            x1 = x2;
            x2 = t;
        }
        Gui.drawRect(x1, y, x2 + 1, y + 1, color);
    }

    private static void drawVerticalLine(int x, int y1, int y2, int color) {
        if (y2 < y1) {
            int t = y1;
            y1 = y2;
            y2 = t;
        }
        Gui.drawRect(x, y1, x + 1, y2 + 1, color);
    }

    /**
     * Detect which mod registered a biome, based on its class package.
     */
    private static String getModSourceForBiome(BiomeGenBase biome) {
        String className = biome.getClass()
            .getName();

        // Vanilla biomes are in net.minecraft.world.biome
        if (className.startsWith("net.minecraft.")) {
            return "Minecraft";
        }

        if (packageToModName == null) {
            packageToModName = new HashMap<>();
            for (ModContainer mod : Loader.instance()
                .getModList()) {
                List<String> ownedPackages = mod.getOwnedPackages();
                if (ownedPackages != null) {
                    for (String pkg : ownedPackages) {
                        packageToModName.put(pkg, mod.getName());
                    }
                }
            }
        }

        // Find the biome's package and look up in the mod map
        String pkg = className.contains(".") ? className.substring(0, className.lastIndexOf('.')) : "";
        // Try exact package match, then walk up the hierarchy
        while (!pkg.isEmpty()) {
            String modName = packageToModName.get(pkg);
            if (modName != null) {
                return modName;
            }
            int lastDot = pkg.lastIndexOf('.');
            if (lastDot < 0) break;
            pkg = pkg.substring(0, lastDot);
        }

        return "Unknown";
    }

    @Desugar
    public record BiomeEntry(int biomeId, String name, int color, String modSource, String className,
        boolean hasTranslation) {

        /**
         * @return the translation key for this biome
         */
        public String translationKey() {
            // Reconstruct the key from the name (best-effort)
            BiomeGenBase[] allBiomes = BiomeGenBase.getBiomeGenArray();
            BiomeGenBase biome = (biomeId >= 0 && biomeId < allBiomes.length) ? allBiomes[biomeId] : null;
            if (biome != null) {
                return "biome.worldpreview." + biome.biomeName.replace(' ', '_')
                    .toLowerCase();
            }
            return "biome.worldpreview.unknown_" + biomeId;
        }
    }
}

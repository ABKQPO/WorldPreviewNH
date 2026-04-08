package com.hfstudio.preview.gui;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.hfstudio.WorldPreviewNH;
import com.hfstudio.preview.biome.BiomeColorMap;
import com.hfstudio.preview.biome.BiomeKeyExporter;
import com.hfstudio.preview.biome.BiomeSampler;
import com.hfstudio.preview.data.DimensionInfo;
import com.hfstudio.preview.data.PreviewSettings;
import com.hfstudio.preview.data.SeedStorage;

import lombok.Getter;

/**
 * Full-screen biome map preview GUI with left panel (toolbar, tab list, seed input)
 * and right panel (biome map display). Layout matches the world-preview mod design.
 */
public class GuiWorldPreview extends GuiScreen {

    private final GuiScreen parentScreen;
    private long seed;
    private WorldType worldType;
    private int worldTypeIndex;
    private String seedText;
    private String generatorOptions = "";

    private BiomeSampler sampler;
    private DynamicTexture mapTexture;
    private ResourceLocation mapTextureLocation;
    private final PreviewSettings settings = new PreviewSettings();

    // Layout constants
    private static final int LEFT_PANEL_WIDTH = 180;
    private static final int TOOLBAR_HEIGHT = 22;
    private static final int TAB_BAR_HEIGHT = 20;
    private static final int SEED_BAR_HEIGHT = 24;
    private static final int PADDING = 2;
    private static final int MIN_BLOCKS_PER_PIXEL = 1;
    private static final int MAX_BLOCKS_PER_PIXEL = 2048;

    // Map display area
    private int mapX, mapY, mapWidth, mapHeight;

    // Left panel area
    private int panelX, panelY, panelWidth, panelBottom;

    // Viewport state
    @Getter
    private int centerBlockX;
    @Getter
    private int centerBlockZ;

    // Drag state
    private boolean dragging = false;
    private int dragStartMouseX, dragStartMouseY;
    private int dragOriginCX, dragOriginCZ;
    private int totalDragPixels = 0;

    // Hover info
    private String hoveredBiomeName = "";
    private int hoveredBlockX, hoveredBlockZ;

    // Sampling state
    private volatile boolean textureNeedsUpdate = false;

    // Highlighted biome
    private int highlightedBiomeId = -1;

    // Coordinate copy message
    private String copiedMessage = null;
    private long copiedMessageTime = 0;

    // First-open flag for debug export
    private static boolean firstOpen = true;

    // Tab state: 0=Biomes, 1=Seeds
    private int activeTab = 0;

    // Widgets
    private GuiTextField seedField;
    private GuiTextField searchField;
    private BiomeListSlot biomeList;
    private SeedListSlot seedList;

    // Toolbar buttons (icons from buttons.png)
    // xTexStart values: 0=random, 20=save, 40=delete, 60=settings, 80=caves(toggle),
    // 120=home, 140=refresh, 200=heightmap(toggle), 240=intersections(toggle), 360=biomes(toggle)
    private WPImageButton btnSettings;
    private WPImageButton btnCaves;
    private WPImageButton btnHome;
    private WPImageButton btnHeightmap;
    private WPImageButton btnIntersections;
    private WPImageButton btnRefresh;
    private WPImageButton btnSaveSeed;
    private WPImageButton btnDeleteSeed;

    // Random seed button (placed next to seed field, not in toolbar)
    private WPImageButton btnRandomSeed;

    // Tab buttons
    private GuiButton btnTabBiomes;
    private GuiButton btnTabSeeds;

    // Bottom buttons
    private GuiButton btnBack;
    private GuiButton btnGoto;
    private GuiButton btnWorldType;
    private GuiButton btnDimension;

    // Dimension selection
    private List<DimensionInfo> dimensionList;
    private int currentDimensionIndex;
    private static final int DIM_POPUP_ENTRY_HEIGHT = 14;
    private static final int DIM_POPUP_MAX_VISIBLE = 8;
    private int dimPopupScrollOffset = 0;
    private boolean dimPopupVisible = false;

    // Toolbar button IDs
    private static final int ID_SETTINGS = 100;
    private static final int ID_CAVES = 101;
    private static final int ID_HOME = 102;
    private static final int ID_HEIGHTMAP = 103;
    private static final int ID_INTERSECTIONS = 104;
    private static final int ID_REFRESH = 105;
    private static final int ID_SAVE_SEED = 106;
    private static final int ID_DELETE_SEED = 107;
    private static final int ID_TAB_BIOMES = 108;
    private static final int ID_TAB_SEEDS = 109;
    private static final int ID_BACK = 110;
    private static final int ID_GOTO = 111;
    private static final int ID_RANDOM_SEED = 112;
    private static final int ID_WORLD_TYPE = 113;
    private static final int ID_DIMENSION = 114;

    public GuiWorldPreview(GuiScreen parent, long seed, WorldType worldType) {
        this(parent, seed, String.valueOf(seed), worldType, "", 0, 0, 0);
    }

    public GuiWorldPreview(GuiScreen parent, long seed, String seedStr, WorldType worldType) {
        this(parent, seed, seedStr, worldType, "", 0, 0, 0);
    }

    public GuiWorldPreview(GuiScreen parent, long seed, String seedStr, WorldType worldType, String generatorOptions) {
        this(parent, seed, seedStr, worldType, generatorOptions, 0, 0, 0);
    }

    public GuiWorldPreview(GuiScreen parent, long seed, WorldType worldType, int initialCenterX, int initialCenterZ,
        int initialDimensionId) {
        this(parent, seed, String.valueOf(seed), worldType, "", initialCenterX, initialCenterZ, initialDimensionId);
    }

    public GuiWorldPreview(GuiScreen parent, long seed, String seedStr, WorldType worldType, String generatorOptions,
        int initialCenterX, int initialCenterZ, int initialDimensionId) {
        this.parentScreen = parent;
        this.seed = seed;
        this.worldType = worldType;
        this.seedText = seedStr;
        this.generatorOptions = generatorOptions != null ? generatorOptions : "";
        this.worldTypeIndex = 0;
        for (int i = 0; i < WorldType.worldTypes.length; i++) {
            if (WorldType.worldTypes[i] == worldType) {
                this.worldTypeIndex = i;
                break;
            }
        }
        this.centerBlockX = initialCenterX;
        this.centerBlockZ = initialCenterZ;
        this.dimensionList = DimensionInfo.getAllDimensions();
        this.currentDimensionIndex = 0;
        for (int i = 0; i < dimensionList.size(); i++) {
            if (dimensionList.get(i)
                .dimensionId() == initialDimensionId) {
                this.currentDimensionIndex = i;
                break;
            }
        }
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        // Export biome keys on first open when debug is enabled
        if (firstOpen && WorldPreviewNH.debug) {
            firstOpen = false;
            BiomeKeyExporter.export();
        }

        // Left panel layout
        panelX = PADDING;
        panelY = PADDING;
        panelWidth = LEFT_PANEL_WIDTH;
        panelBottom = this.height - 26;

        // Map display area: right of left panel
        mapX = panelX + panelWidth + PADDING;
        mapY = PADDING;
        mapWidth = this.width - mapX - PADDING;
        mapHeight = this.height - PADDING * 2 - 24;

        if (mapWidth < 10) mapWidth = 10;
        if (mapHeight < 10) mapHeight = 10;

        // (Re)create map texture
        if (mapTextureLocation != null) {
            mc.getTextureManager()
                .deleteTexture(mapTextureLocation);
        }
        mapTexture = new DynamicTexture(mapWidth, mapHeight);
        mapTextureLocation = mc.getTextureManager()
            .getDynamicTextureLocation("worldpreview_map", mapTexture);

        int[] pixels = mapTexture.getTextureData();
        Arrays.fill(pixels, 0xFF000000);
        mapTexture.updateDynamicTexture();

        // Initialize sampler
        if (sampler == null) {
            sampler = new BiomeSampler();
        }
        sampler.setup(seed, worldType, getCurrentDimensionId(), generatorOptions);
        BiomeColorMap.init();

        // --- Build button list ---
        this.buttonList.clear();

        // Toolbar row (20x20 icon buttons)
        int toolX = panelX + 1;
        int toolY = panelY + 1;
        int toolSpacing = 22;

        btnSettings = new WPImageButton(ID_SETTINGS, toolX, toolY, 60, 0);
        btnSettings.setTooltip(StatCollector.translateToLocal("worldpreview.tooltip.settings"));
        toolX += toolSpacing;

        btnCaves = new WPImageButton(ID_CAVES, toolX, toolY, 80, 20);
        btnCaves.setTooltip(StatCollector.translateToLocal("worldpreview.tooltip.caves"));
        btnCaves.setToggled(settings.showCaves);
        toolX += toolSpacing;

        btnHome = new WPImageButton(ID_HOME, toolX, toolY, 120, 0);
        btnHome.setTooltip(StatCollector.translateToLocal("worldpreview.tooltip.home"));
        toolX += toolSpacing;

        btnHeightmap = new WPImageButton(ID_HEIGHTMAP, toolX, toolY, 200, 20);
        btnHeightmap.setTooltip(StatCollector.translateToLocal("worldpreview.tooltip.heightmap"));
        btnHeightmap.setToggled(settings.heightSampling);
        toolX += toolSpacing;

        btnIntersections = new WPImageButton(ID_INTERSECTIONS, toolX, toolY, 240, 20);
        btnIntersections.setTooltip(StatCollector.translateToLocal("worldpreview.tooltip.intersections"));
        btnIntersections.setToggled(settings.yIntersection);
        toolX += toolSpacing;

        // Refresh button (at old random seed position)
        btnRefresh = new WPImageButton(ID_REFRESH, toolX, toolY, 0, 0);
        btnRefresh.setTooltip(StatCollector.translateToLocal("worldpreview.tooltip.refresh"));
        toolX += toolSpacing;

        // Seed operations: save, delete
        btnSaveSeed = new WPImageButton(ID_SAVE_SEED, toolX, toolY, 20, 0);
        btnSaveSeed.setTooltip(StatCollector.translateToLocal("worldpreview.tooltip.save_seed"));
        toolX += toolSpacing;

        btnDeleteSeed = new WPImageButton(ID_DELETE_SEED, toolX, toolY, 40, 0);
        btnDeleteSeed.setTooltip(StatCollector.translateToLocal("worldpreview.tooltip.delete_seed"));

        this.buttonList.add(btnSettings);
        this.buttonList.add(btnCaves);
        this.buttonList.add(btnHome);
        this.buttonList.add(btnHeightmap);
        this.buttonList.add(btnIntersections);
        this.buttonList.add(btnRefresh);
        this.buttonList.add(btnSaveSeed);
        this.buttonList.add(btnDeleteSeed);

        // Tab buttons below toolbar
        int tabY = panelY + TOOLBAR_HEIGHT + 2;
        int tabW = (panelWidth - 4) / 2;
        btnTabBiomes = new GuiButton(
            ID_TAB_BIOMES,
            panelX + 1,
            tabY,
            tabW,
            TAB_BAR_HEIGHT,
            StatCollector.translateToLocal("worldpreview.tab.biomes"));
        btnTabSeeds = new GuiButton(
            ID_TAB_SEEDS,
            panelX + 1 + tabW + 2,
            tabY,
            tabW,
            TAB_BAR_HEIGHT,
            StatCollector.translateToLocal("worldpreview.tab.seeds"));
        this.buttonList.add(btnTabBiomes);
        this.buttonList.add(btnTabSeeds);

        // Seed text field at the bottom of the left panel (with random button to the right)
        int seedFieldY = panelBottom - SEED_BAR_HEIGHT;
        int randomBtnSize = 20;
        int seedFieldWidth = panelWidth - 4 - randomBtnSize - 2;
        seedField = new GuiTextField(fontRendererObj, panelX + 2, seedFieldY + 2, seedFieldWidth, SEED_BAR_HEIGHT - 4);
        seedField.setMaxStringLength(64);
        seedField.setText(seedText);

        // Random seed button right of seed field
        btnRandomSeed = new WPImageButton(ID_RANDOM_SEED, panelX + 2 + seedFieldWidth + 2, seedFieldY + 2, 0, 0);
        btnRandomSeed.setTooltip(StatCollector.translateToLocal("worldpreview.tooltip.random_seed"));
        this.buttonList.add(btnRandomSeed);

        // Biome list occupies the space between tabs and seed field
        int listTop = tabY + TAB_BAR_HEIGHT + 2;
        int listBottom = seedFieldY - 2;

        // Search field above biome list
        int searchFieldHeight = 16;
        int searchFieldY = listTop;
        searchField = new GuiTextField(fontRendererObj, panelX + 2, searchFieldY, panelWidth - 4, searchFieldHeight);
        searchField.setMaxStringLength(128);

        // Adjust biome list top to accommodate search field
        int biomeListTop = searchFieldY + searchFieldHeight + 2;
        biomeList = new BiomeListSlot(this, panelWidth, this.height, biomeListTop, listBottom);
        seedList = new SeedListSlot(this, panelWidth, this.height, listTop, listBottom);

        // Bottom buttons (centered as a group: 70 + 4 + 70 + 4 + 130 + 4 + 120 = 402)
        int bottomY = this.height - 24;
        int totalBottomWidth = 70 + 4 + 70 + 4 + 130 + 4 + 120;
        int bottomStartX = (this.width - totalBottomWidth) / 2;
        btnBack = new GuiButton(
            ID_BACK,
            bottomStartX,
            bottomY,
            70,
            20,
            StatCollector.translateToLocal("worldpreview.gui.back"));
        btnGoto = new GuiButton(
            ID_GOTO,
            bottomStartX + 74,
            bottomY,
            70,
            20,
            StatCollector.translateToLocal("worldpreview.goto.title"));
        btnWorldType = new GuiButton(ID_WORLD_TYPE, bottomStartX + 148, bottomY, 130, 20, getWorldTypeButtonText());
        btnDimension = new GuiButton(ID_DIMENSION, bottomStartX + 282, bottomY, 120, 20, getDimensionButtonText());
        this.buttonList.add(btnBack);
        this.buttonList.add(btnGoto);
        this.buttonList.add(btnWorldType);
        this.buttonList.add(btnDimension);

        requestViewportUpdate();
    }

    @Override
    public void updateScreen() {
        if (seedField != null) {
            seedField.updateCursorCounter();
        }
        if (searchField != null) {
            searchField.updateCursorCounter();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        // Update texture if new tile data is available
        if (textureNeedsUpdate) {
            textureNeedsUpdate = false;
            updateTexture();
            // Update biome list from sampled data
            if (activeTab == 0) {
                biomeList.updateFromSampledData(sampler.getBiomeIds());
            }
        }

        // --- Draw left panel background ---
        drawRect(panelX, panelY, panelX + panelWidth, panelBottom, 0xC0101010);

        // --- Draw map ---
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager()
            .bindTexture(mapTextureLocation);
        Gui.func_146110_a(mapX, mapY, 0, 0, mapWidth, mapHeight, mapWidth, mapHeight);

        // Map border
        int borderColor = 0xFF555555;
        drawHorizontalLine(mapX - 1, mapX + mapWidth, mapY - 1, borderColor);
        drawHorizontalLine(mapX - 1, mapX + mapWidth, mapY + mapHeight, borderColor);
        drawVerticalLine(mapX - 1, mapY - 1, mapY + mapHeight, borderColor);
        drawVerticalLine(mapX + mapWidth, mapY - 1, mapY + mapHeight, borderColor);

        // Crosshair
        int cx = mapX + mapWidth / 2;
        int cy = mapY + mapHeight / 2;
        drawHorizontalLine(cx - 4, cx + 4, cy, 0xAAFFFFFF);
        drawVerticalLine(cx, cy - 4, cy + 4, 0xAAFFFFFF);

        // --- Draw the list (biomes or seeds) ---
        if (activeTab == 0) {
            biomeList.drawScreenAt(panelX, panelWidth, mouseX, mouseY, partialTicks);
            searchField.drawTextBox();
            if (searchField.getText()
                .isEmpty() && !searchField.isFocused()) {
                fontRendererObj.drawStringWithShadow(
                    StatCollector.translateToLocal("worldpreview.gui.search_hint"),
                    searchField.xPosition + 2,
                    searchField.yPosition + (searchField.height - 8) / 2,
                    0x707070);
            }
        } else {
            seedList.drawScreenAt(panelX, panelWidth, mouseX, mouseY, partialTicks);
        }

        // Seed text field
        seedField.drawTextBox();

        // --- Draw map overlays ---
        updateHoverInfo(mouseX, mouseY);

        // Copied coordinates message
        if (copiedMessage != null && System.currentTimeMillis() - copiedMessageTime < 5000) {
            this.drawCenteredString(
                fontRendererObj,
                copiedMessage,
                mapX + mapWidth / 2,
                mapY + mapHeight - 14,
                0x55FF55);
        } else {
            copiedMessage = null;
        }

        // Loading indicator
        if (sampler.isSampling()) {
            this.drawCenteredString(
                fontRendererObj,
                StatCollector.translateToLocal("worldpreview.gui.loading"),
                mapX + mapWidth / 2,
                mapY + 4,
                0xFFFF00);
        }

        // Single-biome dimension overlay
        if (sampler != null && sampler.isSingleBiomeDimension() && !sampler.isSampling()) {
            DimensionInfo dim = dimensionList.get(currentDimensionIndex);
            String dimName = dim.displayName();
            String msg = StatCollector.translateToLocalFormatted("worldpreview.gui.single_biome", dimName);
            int msgW = fontRendererObj.getStringWidth(msg);
            int msgX = mapX + (mapWidth - msgW) / 2;
            int msgY = mapY + mapHeight / 2 - 5;
            drawRect(msgX - 4, msgY - 4, msgX + msgW + 4, msgY + 14, 0xC0000000);
            fontRendererObj.drawStringWithShadow(msg, msgX, msgY, 0xFFAAAA);
        }

        // Info bar at bottom of map
        String zoomStr = StatCollector
            .translateToLocalFormatted("worldpreview.gui.zoom", String.valueOf(settings.blocksPerPixel));
        String centerStr = String.format("X:%d Z:%d", centerBlockX, centerBlockZ);
        fontRendererObj.drawStringWithShadow(zoomStr, mapX + 2, mapY + mapHeight - 12, 0x888888);
        int cw = fontRendererObj.getStringWidth(centerStr);
        fontRendererObj.drawStringWithShadow(centerStr, mapX + mapWidth - cw - 2, mapY + mapHeight - 12, 0x888888);

        if (settings.heightSampling || settings.yIntersection) {
            String yStr = "Y:" + settings.currentY;
            int yw = fontRendererObj.getStringWidth(yStr);
            fontRendererObj.drawStringWithShadow(yStr, mapX + mapWidth / 2 - yw / 2, mapY + mapHeight - 12, 0x88AAFF);
        }

        // Draw all buttons
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Toolbar button tooltips
        drawToolbarTooltips(mouseX, mouseY);

        // Highlight active tab
        if (activeTab == 0) {
            btnTabBiomes.enabled = false;
            btnTabSeeds.enabled = true;
        } else {
            btnTabBiomes.enabled = true;
            btnTabSeeds.enabled = false;
        }

        // Dimension hover popup
        drawDimensionPopup(mouseX, mouseY);

        // Biome hover tooltip on map (drawn after buttons so it renders on top)
        if (isInMap(mouseX, mouseY) && !hoveredBiomeName.isEmpty()) {
            String line1 = hoveredBiomeName;
            String line2 = "X=" + hoveredBlockX + " Z=" + hoveredBlockZ;
            if (settings.heightSampling || settings.yIntersection) {
                line2 += " Y=" + settings.currentY;
            }
            int tw = Math.max(fontRendererObj.getStringWidth(line1), fontRendererObj.getStringWidth(line2));
            int tx = mouseX + 12;
            int ty = mouseY - 4;
            if (tx + tw + 4 > this.width) tx = mouseX - tw - 12;
            if (ty < 0) ty = 0;
            drawRect(tx - 2, ty - 2, tx + tw + 2, ty + 22, 0xD0000000);
            fontRendererObj.drawStringWithShadow(line1, tx, ty, 0xFFFFFF);
            fontRendererObj.drawStringWithShadow(line2, tx, ty + 11, 0xAAAAAA);
        }

        // Biome list tooltip (drawn last so it renders on top of everything)
        if (activeTab == 0) {
            drawBiomeListTooltip(mouseX, mouseY);
        }
    }

    private void drawBiomeListTooltip(int mouseX, int mouseY) {
        BiomeListSlot.BiomeEntry entry = biomeList.getHoveredEntry();
        if (entry == null) return;

        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        // Line 1: translation status
        if (!entry.hasTranslation()) {
            String key = entry.translationKey();
            lines.add(StatCollector.translateToLocal("worldpreview.tooltip.no_translation") + ": " + key);
            colors.add(0x888888);
        }

        // Line 2: biome display name + ID
        lines.add(entry.name() + " (ID: " + entry.biomeId() + ")");
        colors.add(0xFFFFFF);

        // Line 3: mod source
        lines.add(StatCollector.translateToLocal("worldpreview.tooltip.mod_source") + ": " + entry.modSource());
        colors.add(0x55FF55);

        // Line 4 (Alt held): class path
        boolean altHeld = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        if (altHeld) {
            lines.add(entry.className());
            colors.add(0xAAAAFF);
        } else {
            lines.add(StatCollector.translateToLocal("worldpreview.tooltip.alt_classpath"));
            colors.add(0x666666);
        }

        // Calculate tooltip dimensions
        int maxW = 0;
        for (String line : lines) {
            maxW = Math.max(maxW, fontRendererObj.getStringWidth(line));
        }
        int tooltipH = lines.size() * 11 + 4;

        int tx = mouseX + 12;
        int ty = mouseY - 4;
        if (tx + maxW + 6 > this.width) tx = mouseX - maxW - 12;
        if (ty + tooltipH > this.height) ty = this.height - tooltipH;
        if (ty < 0) ty = 0;

        // Draw background
        drawRect(tx - 3, ty - 3, tx + maxW + 3, ty + tooltipH, 0xE0000000);
        drawRect(tx - 3, ty - 3, tx + maxW + 3, ty - 2, 0xFF333366);
        drawRect(tx - 3, ty + tooltipH - 1, tx + maxW + 3, ty + tooltipH, 0xFF333366);

        // Draw lines
        for (int i = 0; i < lines.size(); i++) {
            fontRendererObj.drawStringWithShadow(lines.get(i), tx, ty + i * 11 + 1, colors.get(i));
        }
    }

    private void drawDimensionPopup(int mouseX, int mouseY) {
        if (btnDimension == null || dimensionList == null || dimensionList.isEmpty()) return;

        // Check if mouse is over the button or the popup area
        boolean overButton = mouseX >= btnDimension.xPosition && mouseX < btnDimension.xPosition + btnDimension.width
            && mouseY >= btnDimension.yPosition
            && mouseY < btnDimension.yPosition + btnDimension.height;

        int visibleCount = Math.min(dimensionList.size(), DIM_POPUP_MAX_VISIBLE);
        int popupHeight = visibleCount * DIM_POPUP_ENTRY_HEIGHT + 4;
        int popupWidth = btnDimension.width;
        int popupX = btnDimension.xPosition;
        int popupY = btnDimension.yPosition - popupHeight;

        boolean overPopup = dimPopupVisible && mouseX >= popupX
            && mouseX < popupX + popupWidth
            && mouseY >= popupY
            && mouseY < popupY + popupHeight;

        dimPopupVisible = overButton || overPopup;

        if (!dimPopupVisible) {
            dimPopupScrollOffset = 0;
            return;
        }

        // Clamp scroll
        int maxScroll = Math.max(0, dimensionList.size() - DIM_POPUP_MAX_VISIBLE);
        if (dimPopupScrollOffset > maxScroll) dimPopupScrollOffset = maxScroll;
        if (dimPopupScrollOffset < 0) dimPopupScrollOffset = 0;

        // Draw popup background
        drawRect(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xE0202020);
        drawRect(popupX, popupY, popupX + popupWidth, popupY + 1, 0xFF555555);
        drawRect(popupX, popupY, popupX + 1, popupY + popupHeight, 0xFF555555);
        drawRect(popupX + popupWidth - 1, popupY, popupX + popupWidth, popupY + popupHeight, 0xFF555555);

        // Draw entries
        for (int i = 0; i < visibleCount; i++) {
            int idx = i + dimPopupScrollOffset;
            if (idx >= dimensionList.size()) break;
            DimensionInfo dim = dimensionList.get(idx);

            int entryY = popupY + 2 + i * DIM_POPUP_ENTRY_HEIGHT;
            boolean hovered = mouseX >= popupX && mouseX < popupX + popupWidth
                && mouseY >= entryY
                && mouseY < entryY + DIM_POPUP_ENTRY_HEIGHT;
            boolean selected = idx == currentDimensionIndex;

            if (hovered) {
                drawRect(popupX + 1, entryY, popupX + popupWidth - 1, entryY + DIM_POPUP_ENTRY_HEIGHT, 0x40FFFFFF);
            }
            if (selected) {
                drawRect(popupX + 1, entryY, popupX + popupWidth - 1, entryY + DIM_POPUP_ENTRY_HEIGHT, 0x3044AAFF);
            }

            String text = dim.displayName();
            if (fontRendererObj.getStringWidth(text) > popupWidth - 8) {
                while (fontRendererObj.getStringWidth(text + "...") > popupWidth - 8 && text.length() > 1) {
                    text = text.substring(0, text.length() - 1);
                }
                text += "...";
            }
            int textColor = selected ? 0x55CCFF : 0xCCCCCC;
            fontRendererObj.drawStringWithShadow(text, popupX + 4, entryY + 3, textColor);
        }

        // Scrollbar if needed
        if (dimensionList.size() > DIM_POPUP_MAX_VISIBLE) {
            int scrollBarX = popupX + popupWidth - 4;
            int scrollTrackH = popupHeight - 4;
            float ratio = (float) DIM_POPUP_MAX_VISIBLE / dimensionList.size();
            int thumbH = Math.max(8, (int) (scrollTrackH * ratio));
            int thumbY = popupY + 2 + (int) ((scrollTrackH - thumbH) * ((float) dimPopupScrollOffset / maxScroll));
            drawRect(scrollBarX, popupY + 2, scrollBarX + 3, popupY + popupHeight - 2, 0x40FFFFFF);
            drawRect(scrollBarX, thumbY, scrollBarX + 3, thumbY + thumbH, 0xA0AAAAAA);
        }
    }

    private void drawToolbarTooltips(int mouseX, int mouseY) {
        for (Object obj : this.buttonList) {
            if (obj instanceof WPImageButton btn) {
                if (btn.isHovered(mouseX, mouseY) && !btn.getTooltipText()
                    .isEmpty()) {
                    int tx = mouseX + 8;
                    int ty = mouseY - 14;
                    String text = btn.getTooltipText();
                    int tw = fontRendererObj.getStringWidth(text);
                    if (tx + tw + 4 > this.width) tx = mouseX - tw - 8;
                    drawRect(tx - 2, ty - 1, tx + tw + 2, ty + 10, 0xE0000000);
                    fontRendererObj.drawStringWithShadow(text, tx, ty, 0xFFFFFF);
                }
            }
        }
    }

    private void updateTexture() {
        int[] biomeData = sampler.getBiomeIds();
        if (biomeData == null) return;

        int[] pixels = mapTexture.getTextureData();
        int len = Math.min(pixels.length, biomeData.length);

        boolean hasHighlight = highlightedBiomeId >= 0;
        BiomeGenBase[] allBiomes = BiomeGenBase.getBiomeGenArray();

        for (int i = 0; i < len; i++) {
            int biomeId = biomeData[i];
            int color = switch (settings.renderMode) {
                case HEIGHTMAP -> computeHeightmapColor(biomeId, allBiomes);
                case INTERSECTIONS -> computeIntersectionColor(biomeId, allBiomes, settings.currentY);
                default -> BiomeColorMap.getColor(biomeId);
            };

            if (hasHighlight && biomeId != highlightedBiomeId) {
                // Grayscale non-highlighted biomes
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                int gray = (r * 30 + g * 59 + b * 11) / 100;
                color = (gray << 16) | (gray << 8) | gray;
            }
            pixels[i] = 0xFF000000 | color;
        }

        mapTexture.updateDynamicTexture();
    }

    /**
     * Compute a heightmap color for a biome based on its rootHeight.
     * Maps estimated surface height to a green-brown-white gradient.
     */
    private int computeHeightmapColor(int biomeId, BiomeGenBase[] allBiomes) {
        BiomeGenBase biome = (biomeId >= 0 && biomeId < allBiomes.length) ? allBiomes[biomeId] : null;
        if (biome == null) return 0x000000;

        // Estimated surface height: sea level (63) + rootHeight scaled
        // rootHeight ranges roughly from -2.0 (deep ocean) to 1.5 (extreme hills)
        float surfaceHeight = 63.0f + biome.rootHeight * 17.0f;

        // Normalize to 0..1 range for coloring (Y=0 to Y=256)
        float t = Math.max(0.0f, Math.min(1.0f, surfaceHeight / 256.0f));

        // Color gradient: deep blue -> green -> brown -> white
        int r, g, b;
        if (t < 0.25f) {
            // Deep water -> shallow water (blue shades)
            float s = t / 0.25f;
            r = (int) (20 + s * 30);
            g = (int) (40 + s * 80);
            b = (int) (120 + s * 60);
        } else if (t < 0.5f) {
            // Low land -> mid land (green shades)
            float s = (t - 0.25f) / 0.25f;
            r = (int) (50 + s * 80);
            g = (int) (120 + s * 60);
            b = (int) (40 - s * 20);
        } else if (t < 0.75f) {
            // Mid land -> high land (brown/tan shades)
            float s = (t - 0.5f) / 0.25f;
            r = (int) (130 + s * 60);
            g = (int) (120 - s * 30);
            b = (int) (20 + s * 30);
        } else {
            // Mountains -> peaks (gray -> white)
            float s = (t - 0.75f) / 0.25f;
            r = (int) (190 + s * 65);
            g = (int) (190 + s * 65);
            b = (int) (190 + s * 65);
        }

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Compute Y-intersection color: shows approximate block at the given Y level.
     * Above estimated surface = lighter biome color (air/sky tint).
     * Below estimated surface = darker biome color (underground).
     * At surface = normal biome color.
     */
    private int computeIntersectionColor(int biomeId, BiomeGenBase[] allBiomes, int currentY) {
        BiomeGenBase biome = (biomeId >= 0 && biomeId < allBiomes.length) ? allBiomes[biomeId] : null;
        if (biome == null) return 0x000000;

        float surfaceHeight = 63.0f + biome.rootHeight * 17.0f;
        float variation = biome.heightVariation * 8.0f;

        int baseColor = BiomeColorMap.getColor(biomeId);
        int cr = (baseColor >> 16) & 0xFF;
        int cg = (baseColor >> 8) & 0xFF;
        int cb = baseColor & 0xFF;

        if (currentY > surfaceHeight + variation) {
            // Above surface: lighten significantly (sky-like)
            cr = Math.min(255, cr + 100);
            cg = Math.min(255, cg + 100);
            cb = Math.min(255, cb + 100);
        } else if (currentY < surfaceHeight - variation) {
            // Below surface: darken (underground)
            cr = cr * 40 / 100;
            cg = cg * 40 / 100;
            cb = cb * 40 / 100;
        }
        // At or near surface: use normal biome color (no modification)

        return (cr << 16) | (cg << 8) | cb;
    }

    private void updateHoverInfo(int mouseX, int mouseY) {
        if (isInMap(mouseX, mouseY)) {
            int pixelX = mouseX - mapX;
            int pixelZ = mouseY - mapY;

            hoveredBlockX = centerBlockX + (pixelX - mapWidth / 2) * settings.blocksPerPixel;
            hoveredBlockZ = centerBlockZ + (pixelZ - mapHeight / 2) * settings.blocksPerPixel;

            int[] biomeData = sampler.getBiomeIds();
            if (biomeData != null) {
                int idx = pixelZ * mapWidth + pixelX;
                if (idx >= 0 && idx < biomeData.length) {
                    int biomeId = biomeData[idx];
                    BiomeGenBase[] biomeArray = BiomeGenBase.getBiomeGenArray();
                    BiomeGenBase biome = (biomeId >= 0 && biomeId < biomeArray.length) ? biomeArray[biomeId] : null;
                    if (biome != null) {
                        String translationKey = "biome.worldpreview." + biome.biomeName.replace(' ', '_')
                            .toLowerCase();
                        hoveredBiomeName = StatCollector.canTranslate(translationKey)
                            ? StatCollector.translateToLocal(translationKey)
                            : biome.biomeName;
                    } else {
                        hoveredBiomeName = "ID:" + biomeId;
                    }
                } else {
                    hoveredBiomeName = "";
                }
            } else {
                hoveredBiomeName = "";
            }
        } else {
            hoveredBiomeName = "";
        }
    }

    // --- Input handling ---

    @Override
    protected void keyTyped(char c, int key) {
        if (searchField.isFocused()) {
            searchField.textboxKeyTyped(c, key);
            biomeList.setFilter(searchField.getText());
            if (key == Keyboard.KEY_ESCAPE) {
                searchField.setFocused(false);
            }
            return;
        }
        if (seedField.isFocused()) {
            seedField.textboxKeyTyped(c, key);
            seedText = seedField.getText();
            if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
                applySeed(seedText);
            }
            return;
        }
        if (key == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parentScreen);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // Handle dimension popup click (before super to intercept)
        if (dimPopupVisible && btnDimension != null) {
            int visibleCount = Math.min(dimensionList.size(), DIM_POPUP_MAX_VISIBLE);
            int popupHeight = visibleCount * DIM_POPUP_ENTRY_HEIGHT + 4;
            int popupX = btnDimension.xPosition;
            int popupY = btnDimension.yPosition - popupHeight;
            int popupWidth = btnDimension.width;

            if (mouseX >= popupX && mouseX < popupX + popupWidth && mouseY >= popupY && mouseY < popupY + popupHeight) {
                // Clicked inside popup - select entry
                int relY = mouseY - popupY - 2;
                int clickedIdx = relY / DIM_POPUP_ENTRY_HEIGHT + dimPopupScrollOffset;
                if (clickedIdx >= 0 && clickedIdx < dimensionList.size()) {
                    selectDimension(clickedIdx);
                }
                return;
            }
        }

        // Right-click on dimension button: previous dimension
        if (mouseButton == 1 && btnDimension != null
            && mouseX >= btnDimension.xPosition
            && mouseX < btnDimension.xPosition + btnDimension.width
            && mouseY >= btnDimension.yPosition
            && mouseY < btnDimension.yPosition + btnDimension.height) {
            cycleDimensionBackward();
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);

        // Search field & Seed field
        searchField.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 1 && mouseX >= searchField.xPosition
            && mouseX < searchField.xPosition + searchField.width
            && mouseY >= searchField.yPosition
            && mouseY < searchField.yPosition + searchField.height) {
            searchField.setText("");
            biomeList.setFilter("");
        }
        seedField.mouseClicked(mouseX, mouseY, mouseButton);

        // Right-click on map: copy coordinates
        if (mouseButton == 1 && isInMap(mouseX, mouseY)) {
            int pixelX = mouseX - mapX;
            int pixelZ = mouseY - mapY;
            int bx = centerBlockX + (pixelX - mapWidth / 2) * settings.blocksPerPixel;
            int bz = centerBlockZ + (pixelZ - mapHeight / 2) * settings.blocksPerPixel;
            String coords = bx + " " + settings.currentY + " " + bz;
            setClipboardString(coords);
            copiedMessage = StatCollector.translateToLocalFormatted("worldpreview.gui.copied", coords);
            copiedMessageTime = System.currentTimeMillis();
            return;
        }

        // Left-click on map: start drag or select biome
        if (mouseButton == 0 && isInMap(mouseX, mouseY)) {
            dragging = true;
            dragStartMouseX = mouseX;
            dragStartMouseY = mouseY;
            dragOriginCX = centerBlockX;
            dragOriginCZ = centerBlockZ;
            totalDragPixels = 0;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (dragging && clickedMouseButton == 0) {
            int dx = mouseX - dragStartMouseX;
            int dz = mouseY - dragStartMouseY;
            totalDragPixels = Math.abs(dx) + Math.abs(dz);
            centerBlockX = dragOriginCX - dx * settings.blocksPerPixel;
            centerBlockZ = dragOriginCZ - dz * settings.blocksPerPixel;

            // Request viewport: cached tiles render immediately, missing tiles queued in background
            requestViewportUpdate();
        }
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        if (state == 0 && dragging) {
            dragging = false;
            int dx = mouseX - dragStartMouseX;
            int dz = mouseY - dragStartMouseY;
            centerBlockX = dragOriginCX - dx * settings.blocksPerPixel;
            centerBlockZ = dragOriginCZ - dz * settings.blocksPerPixel;

            // If minimal drag (< 4 pixels), treat as biome-select click
            if (totalDragPixels <= 4 && isInMap(mouseX, mouseY)) {
                int[] biomeData = sampler.getBiomeIds();
                if (biomeData != null) {
                    int px = mouseX - mapX;
                    int pz = mouseY - mapY;
                    int idx = pz * mapWidth + px;
                    if (idx >= 0 && idx < biomeData.length) {
                        int clickedBiome = biomeData[idx];
                        if (highlightedBiomeId == clickedBiome) {
                            highlightedBiomeId = -1;
                            biomeList.clearSelection();
                        } else {
                            highlightedBiomeId = clickedBiome;
                        }
                        // Force texture update with highlighting
                        updateTexture();
                    }
                }
                // Restore center since this was a click not a drag
                centerBlockX = dragOriginCX;
                centerBlockZ = dragOriginCZ;
            } else {
                requestViewportUpdate();
            }
        }
        super.mouseMovedOrUp(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();

        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int scroll = Mouse.getEventDWheel();

        if (scroll != 0) {
            // Dimension popup scroll
            if (dimPopupVisible && btnDimension != null) {
                int visibleCount = Math.min(dimensionList.size(), DIM_POPUP_MAX_VISIBLE);
                int popupHeight = visibleCount * DIM_POPUP_ENTRY_HEIGHT + 4;
                int popupX = btnDimension.xPosition;
                int popupY = btnDimension.yPosition - popupHeight;
                int popupWidth = btnDimension.width;

                if (mouseX >= popupX && mouseX < popupX + popupWidth
                    && mouseY >= popupY
                    && mouseY < popupY + popupHeight) {
                    if (scroll < 0) {
                        dimPopupScrollOffset++;
                    } else {
                        dimPopupScrollOffset--;
                    }
                    int maxScroll = Math.max(0, dimensionList.size() - DIM_POPUP_MAX_VISIBLE);
                    dimPopupScrollOffset = Math.max(0, Math.min(dimPopupScrollOffset, maxScroll));
                    return;
                }
            }

            if (isInMap(mouseX, mouseY)) {
                if (settings.heightSampling || settings.yIntersection) {
                    // Scroll on map changes Y level
                    if (scroll > 0) {
                        settings.incrementY();
                    } else {
                        settings.decrementY();
                    }
                    // Y change re-interprets existing biome data, no resample needed
                    textureNeedsUpdate = true;
                } else {
                    // Scroll zooms
                    if (scroll > 0) {
                        settings.blocksPerPixel = Math.max(MIN_BLOCKS_PER_PIXEL, settings.blocksPerPixel / 2);
                    } else {
                        settings.blocksPerPixel = Math.min(MAX_BLOCKS_PER_PIXEL, settings.blocksPerPixel * 2);
                    }
                    requestViewportUpdate();
                }
            }
            // Let list handle scroll if mouse is in left panel
            if (mouseX >= panelX && mouseX < panelX + panelWidth) {
                if (activeTab == 0) {
                    biomeList.handleMouseInputAt(panelX, panelWidth);
                } else {
                    seedList.handleMouseInputAt(panelX, panelWidth);
                }
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case ID_BACK:
                mc.displayGuiScreen(parentScreen);
                break;

            case ID_GOTO:
                mc.displayGuiScreen(new GuiCoordinateGoto(this));
                break;

            case ID_SETTINGS:
                mc.displayGuiScreen(new GuiPreviewSettings(this, settings));
                break;

            case ID_HOME:
                centerBlockX = 0;
                centerBlockZ = 0;
                settings.currentY = 64;
                requestViewportUpdate();
                break;

            case ID_CAVES:
                btnCaves.toggle();
                settings.showCaves = btnCaves.isToggled();
                break;

            case ID_HEIGHTMAP:
                btnHeightmap.toggle();
                settings.heightSampling = btnHeightmap.isToggled();
                if (settings.heightSampling) {
                    settings.renderMode = PreviewSettings.RenderMode.HEIGHTMAP;
                    // Disable intersections when enabling heightmap
                    settings.yIntersection = false;
                    btnIntersections.setToggled(false);
                } else {
                    settings.renderMode = PreviewSettings.RenderMode.BIOMES;
                }
                // Force texture update with new render mode (same biome data, different coloring)
                textureNeedsUpdate = true;
                break;

            case ID_INTERSECTIONS:
                btnIntersections.toggle();
                settings.yIntersection = btnIntersections.isToggled();
                if (settings.yIntersection) {
                    settings.renderMode = PreviewSettings.RenderMode.INTERSECTIONS;
                    // Disable heightmap when enabling intersections
                    settings.heightSampling = false;
                    btnHeightmap.setToggled(false);
                } else {
                    settings.renderMode = PreviewSettings.RenderMode.BIOMES;
                }
                // Force texture update with new render mode
                textureNeedsUpdate = true;
                break;

            case ID_REFRESH:
                sampler.clearCache();
                requestViewportUpdate();
                break;

            case ID_RANDOM_SEED:
                randomizeSeed();
                break;

            case ID_SAVE_SEED:
                SeedStorage.addSeed(seedText);
                seedList.refresh();
                break;

            case ID_DELETE_SEED:
                if (activeTab == 1) {
                    seedList.deleteSelected();
                }
                break;

            case ID_TAB_BIOMES:
                activeTab = 0;
                break;

            case ID_TAB_SEEDS:
                activeTab = 1;
                seedList.refresh();
                break;

            case ID_WORLD_TYPE:
                cycleWorldType();
                break;

            case ID_DIMENSION:
                cycleDimensionForward();
                break;
        }
    }

    // --- Public API for sub-screens ---

    public void jumpToCoordinates(int x, int z) {
        centerBlockX = x;
        centerBlockZ = z;
        requestViewportUpdate();
    }

    public void onBiomeSelected(int biomeId) {
        highlightedBiomeId = biomeId;
        updateTexture();
    }

    public void loadSeed(String seedStr) {
        seedText = seedStr;
        if (seedField != null) {
            seedField.setText(seedStr);
        }
        applySeed(seedStr);
    }

    private void applySeed(String seedStr) {
        long newSeed;
        if (seedStr == null || seedStr.trim()
            .isEmpty()) {
            return;
        }
        try {
            newSeed = Long.parseLong(seedStr.trim());
        } catch (NumberFormatException e) {
            newSeed = seedStr.trim()
                .hashCode();
        }
        if (newSeed != this.seed) {
            this.seed = newSeed;
            sampler.setup(seed, worldType, getCurrentDimensionId(), generatorOptions);
            centerBlockX = 0;
            centerBlockZ = 0;
            highlightedBiomeId = -1;
            biomeList.clearSelection();
            requestViewportUpdate();
        }
    }

    private void randomizeSeed() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.allocate(Long.BYTES * 2);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        // Use base64-encoded UUID as the seed string (like the original mod)
        String newSeedStr = Base64.getEncoder()
            .encodeToString(bb.array())
            .substring(0, 16);
        seedText = newSeedStr;
        if (seedField != null) {
            seedField.setText(newSeedStr);
        }
        applySeed(newSeedStr);
    }

    private String getWorldTypeButtonText() {
        String typeName = StatCollector.translateToLocal("generator." + worldType.getWorldTypeName());
        return StatCollector.translateToLocalFormatted("worldpreview.gui.worldtype", typeName);
    }

    private void cycleWorldType() {
        int startIndex = worldTypeIndex;
        do {
            worldTypeIndex = (worldTypeIndex + 1) % WorldType.worldTypes.length;
            if (worldTypeIndex == startIndex) break;
        } while (WorldType.worldTypes[worldTypeIndex] == null
            || !WorldType.worldTypes[worldTypeIndex].getCanBeCreated());

        worldType = WorldType.worldTypes[worldTypeIndex];
        btnWorldType.displayString = getWorldTypeButtonText();
        sampler.setup(seed, worldType, getCurrentDimensionId(), generatorOptions);
        sampler.clearCache();
        requestViewportUpdate();
    }

    private int getCurrentDimensionId() {
        if (dimensionList == null || dimensionList.isEmpty()) return 0;
        return dimensionList.get(currentDimensionIndex)
            .dimensionId();
    }

    private String getDimensionButtonText() {
        if (dimensionList == null || dimensionList.isEmpty()) {
            return StatCollector.translateToLocalFormatted("worldpreview.gui.dimension", "Overworld");
        }
        return StatCollector.translateToLocalFormatted(
            "worldpreview.gui.dimension",
            dimensionList.get(currentDimensionIndex)
                .displayName());
    }

    private void selectDimension(int index) {
        if (index < 0 || index >= dimensionList.size()) return;
        currentDimensionIndex = index;
        applyDimension();
    }

    private void cycleDimensionForward() {
        if (dimensionList == null || dimensionList.isEmpty()) return;
        currentDimensionIndex = (currentDimensionIndex + 1) % dimensionList.size();
        applyDimension();
    }

    private void cycleDimensionBackward() {
        if (dimensionList == null || dimensionList.isEmpty()) return;
        currentDimensionIndex = (currentDimensionIndex - 1 + dimensionList.size()) % dimensionList.size();
        applyDimension();
    }

    private void applyDimension() {
        DimensionInfo dim = dimensionList.get(currentDimensionIndex);
        btnDimension.displayString = getDimensionButtonText();
        sampler.setup(seed, worldType, dim.dimensionId(), generatorOptions);
        sampler.clearCache();
        requestViewportUpdate();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (mapTextureLocation != null) {
            mc.getTextureManager()
                .deleteTexture(mapTextureLocation);
            mapTextureLocation = null;
        }
        if (sampler != null) {
            sampler.shutdown();
            sampler = null;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    private boolean isInMap(int mouseX, int mouseY) {
        return mouseX >= mapX && mouseX < mapX + mapWidth && mouseY >= mapY && mouseY < mapY + mapHeight;
    }

    /**
     * Request the sampler to provide viewport data. Cached tiles are composited immediately;
     * missing tiles are queued for async generation with center-first priority.
     */
    private void requestViewportUpdate() {
        if (sampler == null || mapWidth <= 0 || mapHeight <= 0) return;
        sampler.requestViewport(
            centerBlockX,
            centerBlockZ,
            mapWidth,
            mapHeight,
            settings.blocksPerPixel,
            () -> textureNeedsUpdate = true);
        // Immediately update texture from whatever cached data is available
        textureNeedsUpdate = true;
    }
}

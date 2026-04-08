package com.hfstudio.preview.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

import com.hfstudio.preview.data.PreviewSettings;

/**
 * Settings screen for the world preview.
 * Controls: height sampling toggle, Y-intersection toggle, zoom level,
 * thread count, and other sampling parameters.
 */
public class GuiPreviewSettings extends GuiScreen {

    private final GuiWorldPreview parent;
    private final PreviewSettings settings;

    // Button IDs
    private static final int BTN_DONE = 0;
    private static final int BTN_HEIGHT_SAMPLING = 1;
    private static final int BTN_Y_INTERSECTION = 2;
    private static final int BTN_ZOOM_MINUS = 3;
    private static final int BTN_ZOOM_PLUS = 4;
    private static final int BTN_SHOW_CAVES = 5;

    public GuiPreviewSettings(GuiWorldPreview parent, PreviewSettings settings) {
        this.parent = parent;
        this.settings = settings;
    }

    @Override
    public void initGui() {
        int centerX = this.width / 2;
        int y = this.height / 4;
        int btnW = 200;

        this.buttonList.clear();

        // Height sampling toggle
        this.buttonList
            .add(new GuiButton(BTN_HEIGHT_SAMPLING, centerX - btnW / 2, y, btnW, 20, getHeightSamplingLabel()));
        y += 24;

        // Y-intersection toggle
        this.buttonList
            .add(new GuiButton(BTN_Y_INTERSECTION, centerX - btnW / 2, y, btnW, 20, getYIntersectionLabel()));
        y += 24;

        // Show caves toggle
        this.buttonList.add(new GuiButton(BTN_SHOW_CAVES, centerX - btnW / 2, y, btnW, 20, getCavesLabel()));
        y += 24;

        // Zoom level
        this.buttonList.add(new GuiButton(BTN_ZOOM_MINUS, centerX - btnW / 2, y, 40, 20, "-"));
        this.buttonList.add(new GuiButton(BTN_ZOOM_PLUS, centerX + btnW / 2 - 40, y, 40, 20, "+"));
        y += 32;

        // Done button
        this.buttonList.add(
            new GuiButton(
                BTN_DONE,
                centerX - 100,
                this.height - 28,
                200,
                20,
                StatCollector.translateToLocal("worldpreview.settings.done")));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        this.drawCenteredString(
            this.fontRendererObj,
            StatCollector.translateToLocal("worldpreview.settings.title"),
            this.width / 2,
            this.height / 4 - 20,
            0xFFFFFF);

        // Draw zoom level label between - and + buttons
        String zoomLabel = StatCollector
            .translateToLocalFormatted("worldpreview.settings.zoom", String.valueOf(settings.blocksPerPixel));
        int zoomY = this.height / 4 + 72;
        this.drawCenteredString(this.fontRendererObj, zoomLabel, this.width / 2, zoomY + 6, 0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case BTN_DONE:
                mc.displayGuiScreen(parent);
                break;
            case BTN_HEIGHT_SAMPLING:
                settings.heightSampling = !settings.heightSampling;
                button.displayString = getHeightSamplingLabel();
                break;
            case BTN_Y_INTERSECTION:
                settings.yIntersection = !settings.yIntersection;
                button.displayString = getYIntersectionLabel();
                break;
            case BTN_SHOW_CAVES:
                settings.showCaves = !settings.showCaves;
                button.displayString = getCavesLabel();
                break;
            case BTN_ZOOM_MINUS:
                settings.blocksPerPixel = Math.min(64, settings.blocksPerPixel * 2);
                break;
            case BTN_ZOOM_PLUS:
                settings.blocksPerPixel = Math.max(4, settings.blocksPerPixel / 2);
                break;
        }
    }

    private String getHeightSamplingLabel() {
        return StatCollector.translateToLocal("worldpreview.settings.height_sampling") + ": "
            + getOnOff(settings.heightSampling);
    }

    private String getYIntersectionLabel() {
        return StatCollector.translateToLocal("worldpreview.settings.y_intersection") + ": "
            + getOnOff(settings.yIntersection);
    }

    private String getCavesLabel() {
        return StatCollector.translateToLocal("worldpreview.settings.show_caves") + ": " + getOnOff(settings.showCaves);
    }

    private String getOnOff(boolean value) {
        return StatCollector.translateToLocal(value ? "worldpreview.settings.on" : "worldpreview.settings.off");
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}

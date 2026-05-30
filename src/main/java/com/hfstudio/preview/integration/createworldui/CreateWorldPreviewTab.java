package com.hfstudio.preview.integration.createworldui;

import java.util.List;
import java.util.Random;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.WorldType;

import com.hfstudio.preview.data.DimensionInfo;
import com.hfstudio.preview.gui.EmbeddedWorldPreview;

import decok.dfcdvadstf.createworldui.api.tab.AbstractScreenTab;
import decok.dfcdvadstf.createworldui.api.tab.TabManager;

/**
 * Embedded preview tab for CreateWorldUI.
 */
public class CreateWorldPreviewTab extends AbstractScreenTab {

    private static final int ID_GOTO = 2000;
    private static final int ID_WORLD_TYPE = 2001;
    private static final int ID_DIMENSION = 2002;
    private static final int LEFT_PANEL_WIDTH = 180;
    private static final int PANEL_TOP = 24;
    private static final int CONTROL_BOTTOM_MARGIN = 28;
    private static final int CONTROL_BUTTON_HEIGHT = 20;
    private static final int CONTROL_SPACING = 4;
    private static final int CONTROL_TOP_MARGIN = 6;
    private static final int MIN_DIMENSION_BUTTON_WIDTH = 104;
    private static final int MIN_WORLD_TYPE_BUTTON_WIDTH = 84;
    private static final int EMBEDDED_CONTROL_VERTICAL_OFFSET = 7;
    private static final int PREFERRED_WORLD_TYPE_BUTTON_WIDTH = 100;
    private static final int PREFERRED_DIMENSION_BUTTON_WIDTH = 108;
    private static final int DIM_POPUP_ENTRY_HEIGHT = 14;
    private static final int DIM_POPUP_MAX_VISIBLE = 8;

    private EmbeddedWorldPreview embeddedPreview;
    private final List<DimensionInfo> dimensionList = DimensionInfo.getAllDimensions();
    private String lastSeedText = "";
    private int lastWorldTypeIndex = Integer.MIN_VALUE;
    private GuiButton gotoButton;
    private GuiButton worldTypeButton;
    private GuiButton dimensionButton;
    private boolean dimensionPopupVisible = false;
    private int dimensionPopupScrollOffset = 0;

    public CreateWorldPreviewTab() {
        super(103, "worldpreview.tab.preview");
    }

    @Override
    public void initGui(TabManager tabManager, int width, int height) {
        super.initGui(tabManager, width, height);
        initControlButtons(width, height);
        rebuildPreview();
        setVisible(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            return;
        }
        refreshPreviewIfNeeded();
        if (embeddedPreview != null) {
            embeddedPreview.update();
            embeddedPreview.draw(mouseX, mouseY, partialTicks);
        }
        drawDimensionPopup(mouseX, mouseY);
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (button == gotoButton) {
            openGotoScreen();
            return;
        }
        if (button == worldTypeButton) {
            int nextWorldTypeIndex = findNextWorldTypeIndex(lastWorldTypeIndex);
            tabManager.setWorldTypeIndex(nextWorldTypeIndex);
            updatePreviewContext(resolveSeedText(), nextWorldTypeIndex);
            return;
        }
        if (button == dimensionButton) {
            if (embeddedPreview != null) {
                embeddedPreview.cycleDimensionNext();
                updateControlLabels();
            }
            return;
        }
        if (embeddedPreview != null) {
            embeddedPreview.actionPerformed(button);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && handleDimensionPopupClick(mouseX, mouseY)) {
            return;
        }
        if (mouseButton == 1) {
            if (isOverButton(worldTypeButton, mouseX, mouseY)) {
                int previousWorldTypeIndex = findPreviousWorldTypeIndex(lastWorldTypeIndex);
                tabManager.setWorldTypeIndex(previousWorldTypeIndex);
                updatePreviewContext(resolveSeedText(), previousWorldTypeIndex);
                return;
            }
            if (isOverButton(dimensionButton, mouseX, mouseY) && embeddedPreview != null) {
                embeddedPreview.cycleDimensionPrevious();
                updateControlLabels();
                return;
            }
        }
        if (embeddedPreview != null) {
            embeddedPreview.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (embeddedPreview != null) {
            embeddedPreview.keyTyped(typedChar, keyCode);
        }
    }

    public void handleMouseInput() {
        if (embeddedPreview != null) {
            embeddedPreview.handleMouseInput();
        }
        handleDimensionPopupScroll();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        setControlsVisible(visible);
        if (visible) {
            refreshPreviewIfNeeded();
        }
    }

    private void refreshPreviewIfNeeded() {
        String currentSeedText = resolveSeedText();
        int currentWorldTypeIndex = getWorldTypeIndex();
        if (!currentSeedText.equals(lastSeedText) || currentWorldTypeIndex != lastWorldTypeIndex) {
            updatePreviewContext(currentSeedText, currentWorldTypeIndex);
        }
    }

    private void rebuildPreview() {
        if (embeddedPreview != null) {
            embeddedPreview.close();
        }
        String seedText = resolveSeedText();
        long seed = parseSeed(seedText);
        WorldType worldType = resolveWorldType(getWorldTypeIndex());
        embeddedPreview = new EmbeddedWorldPreview(tabManager.getParent(), seed, seedText, worldType, "");
        embeddedPreview.init(
            mc,
            tabManager.getParent().width,
            tabManager.getParent().height,
            0,
            PANEL_TOP,
            width(),
            height() - CONTROL_BOTTOM_MARGIN - CONTROL_BUTTON_HEIGHT - CONTROL_TOP_MARGIN);
        embeddedPreview.setEmbeddedReturnScreen(tabManager.getParent());
        embeddedPreview.setEmbeddedShowBottomControls(false);
        lastSeedText = seedText;
        lastWorldTypeIndex = getWorldTypeIndex();
        updateControlLabels();
    }

    private void updatePreviewContext(String seedText, int worldTypeIndex) {
        if (embeddedPreview == null) {
            rebuildPreview();
            return;
        }
        long seed = parseSeed(seedText);
        WorldType worldType = resolveWorldType(worldTypeIndex);
        embeddedPreview.applyContext(seed, seedText, worldType, "");
        lastSeedText = seedText;
        lastWorldTypeIndex = worldTypeIndex;
        updateControlLabels();
    }

    private void initControlButtons(int width, int height) {
        int bottomY = height - CONTROL_BOTTOM_MARGIN - CONTROL_BUTTON_HEIGHT - EMBEDDED_CONTROL_VERTICAL_OFFSET;
        int mapLeft = LEFT_PANEL_WIDTH + 6;
        int availableWidth = Math.max(0, width - mapLeft - 10);
        int gotoWidth = Math.min(70, Math.max(54, availableWidth / 4));
        int remainingWidth = Math.max(0, availableWidth - gotoWidth - CONTROL_SPACING * 2);
        int preferredWidth = PREFERRED_WORLD_TYPE_BUTTON_WIDTH + PREFERRED_DIMENSION_BUTTON_WIDTH;
        int worldTypeWidth = preferredWidth > 0 ? remainingWidth * PREFERRED_WORLD_TYPE_BUTTON_WIDTH / preferredWidth
            : 0;
        int dimensionWidth = Math.max(0, remainingWidth - worldTypeWidth);
        if (worldTypeWidth < MIN_WORLD_TYPE_BUTTON_WIDTH || dimensionWidth < MIN_DIMENSION_BUTTON_WIDTH) {
            int deficit = Math.max(0, MIN_WORLD_TYPE_BUTTON_WIDTH - worldTypeWidth)
                + Math.max(0, MIN_DIMENSION_BUTTON_WIDTH - dimensionWidth);
            int shrinkableGotoWidth = Math.max(0, gotoWidth - 40);
            int recoveredWidth = Math.min(deficit, shrinkableGotoWidth);
            gotoWidth -= recoveredWidth;
            remainingWidth += recoveredWidth;
            worldTypeWidth = remainingWidth / 2;
            dimensionWidth = remainingWidth - worldTypeWidth;
        }
        int totalWidth = gotoWidth + CONTROL_SPACING + worldTypeWidth + CONTROL_SPACING + dimensionWidth;
        int startX = mapLeft + Math.max(0, availableWidth - totalWidth);

        gotoButton = new GuiButton(
            ID_GOTO,
            startX,
            bottomY,
            gotoWidth,
            CONTROL_BUTTON_HEIGHT,
            StatCollector.translateToLocal("worldpreview.goto.title"));
        worldTypeButton = new GuiButton(
            ID_WORLD_TYPE,
            startX + gotoWidth + CONTROL_SPACING,
            bottomY,
            worldTypeWidth,
            CONTROL_BUTTON_HEIGHT,
            "");
        dimensionButton = new GuiButton(
            ID_DIMENSION,
            startX + gotoWidth + CONTROL_SPACING + worldTypeWidth + CONTROL_SPACING,
            bottomY,
            dimensionWidth,
            CONTROL_BUTTON_HEIGHT,
            "");

        addButton(gotoButton);
        addButton(worldTypeButton);
        addButton(dimensionButton);
    }

    private void updateControlLabels() {
        if (worldTypeButton != null) {
            worldTypeButton.displayString = getWorldTypeButtonText();
        }
        if (dimensionButton != null && embeddedPreview != null) {
            dimensionButton.displayString = embeddedPreview.getCurrentDimensionButtonText();
        }
    }

    private void setControlsVisible(boolean visible) {
        if (gotoButton != null) {
            gotoButton.visible = visible;
        }
        if (worldTypeButton != null) {
            worldTypeButton.visible = visible;
        }
        if (dimensionButton != null) {
            dimensionButton.visible = visible;
        }
    }

    private void openGotoScreen() {
        if (embeddedPreview == null) {
            return;
        }
        GuiCreateWorld parent = tabManager.getParent();
        mc.displayGuiScreen(
            new CreateWorldPreviewGotoScreen(
                parent,
                embeddedPreview,
                embeddedPreview.getCenterBlockX(),
                embeddedPreview.getCenterBlockZ()));
    }

    private String getWorldTypeButtonText() {
        WorldType worldType = resolveWorldType(lastWorldTypeIndex);
        String typeName = StatCollector.translateToLocal("generator." + worldType.getWorldTypeName());
        return StatCollector.translateToLocalFormatted("worldpreview.gui.worldtype", typeName);
    }

    private boolean isOverButton(GuiButton button, int mouseX, int mouseY) {
        return button != null && button.visible
            && mouseX >= button.xPosition
            && mouseX < button.xPosition + button.width
            && mouseY >= button.yPosition
            && mouseY < button.yPosition + button.height;
    }

    private void drawDimensionPopup(int mouseX, int mouseY) {
        if (dimensionButton == null || embeddedPreview == null || dimensionList.isEmpty()) {
            dimensionPopupVisible = false;
            return;
        }
        boolean overButton = isOverButton(dimensionButton, mouseX, mouseY);
        int popupX = dimensionButton.xPosition;
        int popupWidth = dimensionButton.width;
        int popupHeight = Math.min(dimensionList.size(), DIM_POPUP_MAX_VISIBLE) * DIM_POPUP_ENTRY_HEIGHT + 4;
        int popupY = dimensionButton.yPosition - popupHeight;
        boolean overPopup = dimensionPopupVisible && mouseX >= popupX
            && mouseX < popupX + popupWidth
            && mouseY >= popupY
            && mouseY < popupY + popupHeight;

        dimensionPopupVisible = overButton || overPopup;
        if (!dimensionPopupVisible) {
            dimensionPopupScrollOffset = 0;
            return;
        }

        int maxScroll = Math.max(0, dimensionList.size() - DIM_POPUP_MAX_VISIBLE);
        dimensionPopupScrollOffset = Math.max(0, Math.min(dimensionPopupScrollOffset, maxScroll));

        Gui.drawRect(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xE0202020);
        Gui.drawRect(popupX, popupY, popupX + popupWidth, popupY + 1, 0xFF555555);
        Gui.drawRect(popupX, popupY, popupX + 1, popupY + popupHeight, 0xFF555555);
        Gui.drawRect(popupX + popupWidth - 1, popupY, popupX + popupWidth, popupY + popupHeight, 0xFF555555);

        int currentDimensionId = embeddedPreview.getCurrentDimensionId();
        int visibleCount = Math.min(dimensionList.size(), DIM_POPUP_MAX_VISIBLE);
        for (int i = 0; i < visibleCount; i++) {
            int index = i + dimensionPopupScrollOffset;
            if (index >= dimensionList.size()) {
                break;
            }
            DimensionInfo dimensionInfo = dimensionList.get(index);
            int entryY = popupY + 2 + i * DIM_POPUP_ENTRY_HEIGHT;
            boolean hovered = mouseX >= popupX && mouseX < popupX + popupWidth
                && mouseY >= entryY
                && mouseY < entryY + DIM_POPUP_ENTRY_HEIGHT;
            boolean selected = dimensionInfo.dimensionId() == currentDimensionId;

            if (hovered) {
                Gui.drawRect(popupX + 1, entryY, popupX + popupWidth - 1, entryY + DIM_POPUP_ENTRY_HEIGHT, 0x40FFFFFF);
            }
            if (selected) {
                Gui.drawRect(popupX + 1, entryY, popupX + popupWidth - 1, entryY + DIM_POPUP_ENTRY_HEIGHT, 0x3044AAFF);
            }

            String text = trimDimensionText(dimensionInfo.displayName(), popupWidth - 8);
            int textColor = selected ? 0x55CCFF : 0xCCCCCC;
            mc.fontRenderer.drawStringWithShadow(text, popupX + 4, entryY + 3, textColor);
        }
    }

    private boolean handleDimensionPopupClick(int mouseX, int mouseY) {
        if (!dimensionPopupVisible || dimensionButton == null || embeddedPreview == null) {
            return false;
        }
        int popupHeight = Math.min(dimensionList.size(), DIM_POPUP_MAX_VISIBLE) * DIM_POPUP_ENTRY_HEIGHT + 4;
        int popupX = dimensionButton.xPosition;
        int popupY = dimensionButton.yPosition - popupHeight;
        int popupWidth = dimensionButton.width;
        if (mouseX < popupX || mouseX >= popupX + popupWidth || mouseY < popupY || mouseY >= popupY + popupHeight) {
            return false;
        }

        int relativeY = mouseY - popupY - 2;
        if (relativeY < 0) {
            return true;
        }
        int clickedIndex = relativeY / DIM_POPUP_ENTRY_HEIGHT + dimensionPopupScrollOffset;
        if (clickedIndex < 0 || clickedIndex >= dimensionList.size()) {
            return true;
        }

        int targetDimensionId = dimensionList.get(clickedIndex)
            .dimensionId();
        while (embeddedPreview.getCurrentDimensionId() != targetDimensionId) {
            embeddedPreview.cycleDimensionNext();
        }
        updateControlLabels();
        dimensionPopupVisible = false;
        return true;
    }

    private void handleDimensionPopupScroll() {
        if (!dimensionPopupVisible || dimensionList.size() <= DIM_POPUP_MAX_VISIBLE) {
            return;
        }
        int scroll = org.lwjgl.input.Mouse.getEventDWheel();
        if (scroll == 0) {
            return;
        }
        if (scroll < 0) {
            dimensionPopupScrollOffset++;
        } else {
            dimensionPopupScrollOffset--;
        }
        int maxScroll = Math.max(0, dimensionList.size() - DIM_POPUP_MAX_VISIBLE);
        dimensionPopupScrollOffset = Math.max(0, Math.min(dimensionPopupScrollOffset, maxScroll));
    }

    private String trimDimensionText(String text, int maxWidth) {
        if (mc.fontRenderer.getStringWidth(text) <= maxWidth) {
            return text;
        }
        String result = text;
        while (result.length() > 1 && mc.fontRenderer.getStringWidth(result + "...") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    private int width() {
        return tabManager.getParent().width;
    }

    private int height() {
        return tabManager.getParent().height;
    }

    private long parseSeed(String seedText) {
        if (MathHelper.stringNullOrLengthZero(seedText)) {
            return new Random().nextLong();
        }
        try {
            return Long.parseLong(seedText);
        } catch (NumberFormatException e) {
            return seedText.hashCode();
        }
    }

    private WorldType resolveWorldType(int worldTypeIndex) {
        if (worldTypeIndex >= 0 && worldTypeIndex < WorldType.worldTypes.length) {
            WorldType worldType = WorldType.worldTypes[worldTypeIndex];
            if (worldType != null) {
                return worldType;
            }
        }
        return WorldType.DEFAULT;
    }

    private int findNextWorldTypeIndex(int currentIndex) {
        if (WorldType.worldTypes.length == 0) {
            return 0;
        }
        int nextIndex = currentIndex;
        do {
            nextIndex = (nextIndex + 1) % WorldType.worldTypes.length;
            WorldType worldType = WorldType.worldTypes[nextIndex];
            if (worldType != null && worldType.getCanBeCreated()) {
                return nextIndex;
            }
        } while (nextIndex != currentIndex);
        return currentIndex;
    }

    private int findPreviousWorldTypeIndex(int currentIndex) {
        if (WorldType.worldTypes.length == 0) {
            return 0;
        }
        int previousIndex = currentIndex;
        do {
            previousIndex = (previousIndex - 1 + WorldType.worldTypes.length) % WorldType.worldTypes.length;
            WorldType worldType = WorldType.worldTypes[previousIndex];
            if (worldType != null && worldType.getCanBeCreated()) {
                return previousIndex;
            }
        } while (previousIndex != currentIndex);
        return currentIndex;
    }

    private String resolveSeedText() {
        String seedText = getSeed();
        if (!MathHelper.stringNullOrLengthZero(seedText)) {
            return seedText;
        }
        if (embeddedPreview != null) {
            String currentSeedText = embeddedPreview.getSeedText();
            if (!MathHelper.stringNullOrLengthZero(currentSeedText)) {
                return currentSeedText;
            }
        }
        return lastSeedText;
    }
}

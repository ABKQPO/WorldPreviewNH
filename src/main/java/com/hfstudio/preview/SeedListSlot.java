package com.hfstudio.preview;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;

/**
 * Scrollable list of saved seeds. Each entry shows the seed string and a delete button.
 */
public class SeedListSlot extends GuiSlot {

    private final GuiWorldPreview parent;
    private List<String> seeds = new ArrayList<>();
    private int selectedIndex = -1;

    public SeedListSlot(GuiWorldPreview parent, int width, int height, int top, int bottom) {
        super(Minecraft.getMinecraft(), width, height, top, bottom, 16);
        this.parent = parent;
        this.setShowSelectionBox(true);
        refresh();
    }

    public void setBounds(int width, int height, int top, int bottom) {
        this.width = width;
        this.height = height;
        this.top = top;
        this.bottom = bottom;
        this.left = 0;
        this.right = width;
    }

    public void refresh() {
        seeds = SeedStorage.getSavedSeeds();
        if (selectedIndex >= seeds.size()) {
            selectedIndex = -1;
        }
    }

    @Override
    protected int getSize() {
        return seeds.size();
    }

    @Override
    protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY) {
        selectedIndex = index;
        if (doubleClick && index >= 0 && index < seeds.size()) {
            parent.loadSeed(seeds.get(index));
        }
    }

    @Override
    protected boolean isSelected(int index) {
        return index == selectedIndex;
    }

    @Override
    protected void drawBackground() {}

    @Override
    protected void drawSlot(int index, int x, int y, int slotHeight, Tessellator tess, int mouseX, int mouseY) {
        if (index < 0 || index >= seeds.size()) return;
        Minecraft mc = Minecraft.getMinecraft();
        String seed = seeds.get(index);

        int maxWidth = this.right - this.left - 6;
        String display = seed;
        if (mc.fontRenderer.getStringWidth(display) > maxWidth) {
            display = mc.fontRenderer.trimStringToWidth(display, maxWidth - 6) + "...";
        }

        int textColor = (index == selectedIndex) ? 0xFFFFFF : 0xC0C0C0;
        mc.fontRenderer.drawStringWithShadow(display, this.left + 3, y + 2, textColor);
    }

    public void drawScreenAt(int offsetX, int listWidth, int mouseX, int mouseY, float partialTicks) {
        this.left = offsetX;
        this.right = offsetX + listWidth;
        this.drawScreen(mouseX, mouseY, partialTicks);
    }

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

    public String getSelectedSeed() {
        if (selectedIndex >= 0 && selectedIndex < seeds.size()) {
            return seeds.get(selectedIndex);
        }
        return null;
    }

    public void deleteSelected() {
        String seed = getSelectedSeed();
        if (seed != null) {
            SeedStorage.removeSeed(seed);
            refresh();
        }
    }
}

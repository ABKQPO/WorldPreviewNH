package com.hfstudio.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import lombok.Getter;
import lombok.Setter;

/**
 * Image button that renders icons from the world-preview buttons.png sprite sheet.
 * The sheet is 400x60 with 20x20 icons, 3 rows: normal(y=0), hover(y=20), disabled(y=40).
 */
public class WPImageButton extends GuiButton {

    private static final ResourceLocation BUTTONS_TEX = new ResourceLocation(
        "worldpreview",
        "textures/gui/buttons.png");
    private static final int TEX_WIDTH = 400;
    private static final int TEX_HEIGHT = 60;
    private static final int ICON_SIZE = 20;

    private final int xTexStart;
    @Setter
    @Getter
    private boolean toggled = false;
    private final int xDiff; // offset for toggled state (0 = not a toggle button)
    @Getter
    private String tooltipText = "";

    /**
     * @param id        button ID
     * @param x         screen x
     * @param y         screen y
     * @param xTexStart X offset in the sprite sheet for this icon
     * @param xDiff     X offset difference when toggled (0 for non-toggle buttons)
     */
    public WPImageButton(int id, int x, int y, int xTexStart, int xDiff) {
        super(id, x, y, ICON_SIZE, ICON_SIZE, "");
        this.xTexStart = xTexStart;
        this.xDiff = xDiff;
    }

    public WPImageButton setTooltip(String tooltip) {
        this.tooltipText = tooltip;
        return this;
    }

    public void toggle() {
        this.toggled = !this.toggled;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;

        mc.getTextureManager()
            .bindTexture(BUTTONS_TEX);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);

        boolean hovered = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;

        int texX = xTexStart + (!toggled && xDiff > 0 ? xDiff : 0);
        int texY;
        if (!this.enabled) {
            texY = 40; // disabled row
        } else if (hovered) {
            texY = 0; // hover row (slightly muted for feedback)
        } else {
            texY = 20; // normal active row (bright)
        }

        drawTexturedRect(this.xPosition, this.yPosition, texX, texY, ICON_SIZE, ICON_SIZE);
        GL11.glDisable(GL11.GL_BLEND);
    }

    /**
     * Draw a region from the buttons texture. Handles non-256x256 texture sheets.
     */
    private void drawTexturedRect(int x, int y, int u, int v, int w, int h) {
        float uScale = 1.0F / TEX_WIDTH;
        float vScale = 1.0F / TEX_HEIGHT;
        Gui.func_146110_a(x, y, u, v, w, h, TEX_WIDTH, TEX_HEIGHT);
    }

    /**
     * Check if mouse is hovering over this button.
     */
    public boolean isHovered(int mouseX, int mouseY) {
        return this.visible && this.enabled
            && mouseX >= this.xPosition
            && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;
    }
}

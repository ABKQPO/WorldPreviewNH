package com.hfstudio.preview.gui;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * A 20x20 button that displays a biome-view icon from the buttons.png sprite sheet.
 * Uses the "toggle biomes" icon (xTexStart=360) from the world-preview texture atlas.
 */
public class GuiPreviewButton extends GuiButton {

    private static final ResourceLocation BUTTONS_TEX = new ResourceLocation(
        "worldpreview",
        "textures/gui/buttons.png");
    private static final int TEX_WIDTH = 400;
    private static final int TEX_HEIGHT = 60;

    // The biomes toggle icon at xTexStart=360 in the sprite sheet
    private static final int ICON_X = 360;
    private static final int ICON_SIZE = 20;

    private boolean hovered;

    public GuiPreviewButton(int id, int x, int y) {
        super(id, x, y, 20, 20, "");
    }

    public boolean isHovered() {
        return hovered && visible;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;

        mc.getTextureManager()
            .bindTexture(BUTTONS_TEX);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);

        hovered = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;

        int texY = hovered ? 0 : 20;

        Gui.func_146110_a(this.xPosition, this.yPosition, ICON_X, texY, this.width, this.height, TEX_WIDTH, TEX_HEIGHT);

        GL11.glDisable(GL11.GL_BLEND);
    }

    /**
     * Draw a vanilla-style tooltip (dark background with purple gradient border).
     * Can be called from any context without needing GuiScreen subclass access.
     */
    public static void drawVanillaTooltip(FontRenderer font, String text, int mouseX, int mouseY, int screenWidth,
        int screenHeight) {
        drawVanillaTooltip(font, Arrays.asList(text), mouseX, mouseY, screenWidth, screenHeight);
    }

    /**
     * Draw a vanilla-style multi-line tooltip (dark background with purple gradient border).
     */
    public static void drawVanillaTooltip(FontRenderer font, List<String> lines, int mouseX, int mouseY,
        int screenWidth, int screenHeight) {
        if (lines.isEmpty()) return;

        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        int maxWidth = 0;
        for (String s : lines) {
            int w = font.getStringWidth(s);
            if (w > maxWidth) maxWidth = w;
        }

        int x = mouseX + 12;
        int y = mouseY - 12;
        int boxHeight = 8;
        if (lines.size() > 1) {
            boxHeight += 2 + (lines.size() - 1) * 10;
        }

        if (x + maxWidth > screenWidth) {
            x -= 28 + maxWidth;
        }
        if (y + boxHeight + 6 > screenHeight) {
            y = screenHeight - boxHeight - 6;
        }

        // Background
        int bg = 0xF0100010;
        drawGradient(x - 3, y - 4, x + maxWidth + 3, y - 3, bg, bg);
        drawGradient(x - 3, y + boxHeight + 3, x + maxWidth + 3, y + boxHeight + 4, bg, bg);
        drawGradient(x - 3, y - 3, x + maxWidth + 3, y + boxHeight + 3, bg, bg);
        drawGradient(x - 4, y - 3, x - 3, y + boxHeight + 3, bg, bg);
        drawGradient(x + maxWidth + 3, y - 3, x + maxWidth + 4, y + boxHeight + 3, bg, bg);

        // Border
        int borderTop = 0x505000FF;
        int borderBot = (borderTop & 0xFEFEFE) >> 1 | borderTop & 0xFF000000;
        drawGradient(x - 3, y - 3 + 1, x - 3 + 1, y + boxHeight + 3 - 1, borderTop, borderBot);
        drawGradient(x + maxWidth + 2, y - 3 + 1, x + maxWidth + 3, y + boxHeight + 3 - 1, borderTop, borderBot);
        drawGradient(x - 3, y - 3, x + maxWidth + 3, y - 3 + 1, borderTop, borderTop);
        drawGradient(x - 3, y + boxHeight + 2, x + maxWidth + 3, y + boxHeight + 3, borderBot, borderBot);

        // Text
        for (int i = 0; i < lines.size(); i++) {
            font.drawStringWithShadow(lines.get(i), x, y, -1);
            if (i == 0) y += 2;
            y += 10;
        }

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        RenderHelper.enableStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
    }

    private static void drawGradient(int left, int top, int right, int bottom, int colorTop, int colorBottom) {
        float aT = (float) (colorTop >> 24 & 255) / 255.0F;
        float rT = (float) (colorTop >> 16 & 255) / 255.0F;
        float gT = (float) (colorTop >> 8 & 255) / 255.0F;
        float bT = (float) (colorTop & 255) / 255.0F;
        float aB = (float) (colorBottom >> 24 & 255) / 255.0F;
        float rB = (float) (colorBottom >> 16 & 255) / 255.0F;
        float gB = (float) (colorBottom >> 8 & 255) / 255.0F;
        float bB = (float) (colorBottom & 255) / 255.0F;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        net.minecraft.client.renderer.Tessellator t = net.minecraft.client.renderer.Tessellator.instance;
        t.startDrawingQuads();
        t.setColorRGBA_F(rT, gT, bT, aT);
        t.addVertex(right, top, 300.0);
        t.addVertex(left, top, 300.0);
        t.setColorRGBA_F(rB, gB, bB, aB);
        t.addVertex(left, bottom, 300.0);
        t.addVertex(right, bottom, 300.0);
        t.draw();

        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }
}

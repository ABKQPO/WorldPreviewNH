package com.hfstudio.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

/**
 * A 16x16 button that displays a biome-view icon from the buttons.png sprite sheet.
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

    public GuiPreviewButton(int id, int x, int y) {
        super(id, x, y, 20, 20, "");
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

        int texY = hovered ? 0 : 20;

        // Scale the 20x20 icon to fit in the 16x16 button area
        // Use func_146110_a(x, y, u, v, w, h, texW, texH) for proper UV mapping
        Gui.func_146110_a(this.xPosition, this.yPosition, ICON_X, texY, this.width, this.height, TEX_WIDTH, TEX_HEIGHT);

        GL11.glDisable(GL11.GL_BLEND);
    }
}

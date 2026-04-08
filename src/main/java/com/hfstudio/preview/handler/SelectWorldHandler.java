package com.hfstudio.preview.handler;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.SaveFormatComparator;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.client.event.GuiScreenEvent;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.hfstudio.preview.gui.GuiPreviewButton;
import com.hfstudio.preview.gui.GuiWorldPreview;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Forge event handler that draws a small preview icon button on each world entry
 * in the single-player world selection screen (GuiSelectWorld).
 * Clicking the icon opens GuiWorldPreview for that world without entering it.
 */
public class SelectWorldHandler {

    private static final ResourceLocation BUTTONS_TEX = new ResourceLocation(
        "worldpreview",
        "textures/gui/buttons.png");
    private static final int TEX_WIDTH = 400;
    private static final int TEX_HEIGHT = 60;
    private static final int ICON_X = 360;
    private static final int ICON_SIZE = 20;

    /** Track mouse button state to detect click transitions */
    private boolean wasMouseDown = false;

    @SubscribeEvent
    public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiSelectWorld screen)) return;

        GuiSlot list = screen.field_146638_t;
        List<SaveFormatComparator> saveList = screen.field_146639_s;
        if (list == null || saveList == null || saveList.isEmpty()) return;

        int listTop = list.top;
        int listBottom = list.bottom;
        int slotHeight = list.slotHeight;
        int scrollOffset = list.getAmountScrolled();
        int listWidth = 220;
        int slotContentX = list.left + list.width / 2 - listWidth / 2 + 2;

        int mouseX = event.mouseX;
        int mouseY = event.mouseY;
        int hoveredSlot = -1;

        // Enable scissor to clip to the list area
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int scaleFactor = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            list.left * scaleFactor,
            mc.displayHeight - listBottom * scaleFactor,
            list.width * scaleFactor,
            (listBottom - listTop) * scaleFactor);

        for (int i = 0; i < saveList.size(); i++) {
            int slotY = listTop + 4 - scrollOffset + i * slotHeight;
            int slotVisibleHeight = slotHeight - 4;

            // Skip slots entirely outside visible area
            if (slotY + slotVisibleHeight < listTop || slotY > listBottom) continue;

            // Position the icon at the left edge of the slot content, vertically centered
            int iconX = slotContentX - ICON_SIZE - 4;
            int iconY = slotY + (slotVisibleHeight - ICON_SIZE) / 2;

            boolean hovered = mouseX >= iconX && mouseX < iconX + ICON_SIZE
                && mouseY >= iconY
                && mouseY < iconY + ICON_SIZE
                && mouseY >= listTop
                && mouseY < listBottom;

            if (hovered) {
                hoveredSlot = i;
            }

            // Draw the icon
            mc.getTextureManager()
                .bindTexture(BUTTONS_TEX);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(GL11.GL_BLEND);
            int texY = hovered ? 0 : 20;
            Gui.func_146110_a(iconX, iconY, ICON_X, texY, ICON_SIZE, ICON_SIZE, TEX_WIDTH, TEX_HEIGHT);
            GL11.glDisable(GL11.GL_BLEND);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Draw vanilla-style tooltip when hovering over any preview icon
        if (hoveredSlot >= 0) {
            String tooltip = StatCollector.translateToLocal("worldpreview.button.tooltip");
            GuiPreviewButton.drawVanillaTooltip(mc.fontRenderer, tooltip, mouseX, mouseY, screen.width, screen.height);
        }

        // Detect mouse click transition (pressed this frame, wasn't pressed last frame)
        boolean mouseDown = Mouse.isButtonDown(0);
        if (mouseDown && !wasMouseDown && hoveredSlot >= 0) {
            openPreview(screen, hoveredSlot);
        }
        wasMouseDown = mouseDown;
    }

    private void openPreview(GuiSelectWorld screen, int index) {
        List<SaveFormatComparator> saveList = screen.field_146639_s;
        if (saveList == null || index >= saveList.size()) return;

        SaveFormatComparator save = saveList.get(index);
        String fileName = save.getFileName();

        Minecraft mc = Minecraft.getMinecraft();
        ISaveHandler saveHandler = mc.getSaveLoader()
            .getSaveLoader(fileName, false);
        WorldInfo worldInfo = saveHandler.loadWorldInfo();
        saveHandler.flush();

        if (worldInfo == null) return;

        long seed = worldInfo.getSeed();
        WorldType worldType = worldInfo.getTerrainType();
        if (worldType == null) {
            worldType = WorldType.DEFAULT;
        }
        String generatorOptions = worldInfo.getGeneratorOptions();
        if (generatorOptions == null) {
            generatorOptions = "";
        }

        mc.displayGuiScreen(new GuiWorldPreview(screen, seed, String.valueOf(seed), worldType, generatorOptions));
    }
}

package com.hfstudio.preview.integration.createworldui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

import com.hfstudio.preview.gui.EmbeddedWorldPreview;

/**
 * Simple coordinate dialog that returns to the CreateWorldUI preview tab.
 */
public class CreateWorldPreviewGotoScreen extends GuiScreen {

    private final GuiScreen parentScreen;
    private final EmbeddedWorldPreview preview;
    private final int currentX;
    private final int currentZ;
    private GuiTextField fieldX;
    private GuiTextField fieldZ;

    public CreateWorldPreviewGotoScreen(GuiScreen parentScreen, EmbeddedWorldPreview preview, int currentX,
        int currentZ) {
        this.parentScreen = parentScreen;
        this.preview = preview;
        this.currentX = currentX;
        this.currentZ = currentZ;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        int centerX = width / 2;
        int centerY = height / 2;

        fieldX = new GuiTextField(fontRendererObj, centerX - 80, centerY - 30, 160, 20);
        fieldX.setMaxStringLength(12);
        fieldX.setText(String.valueOf(currentX));
        fieldX.setFocused(true);

        fieldZ = new GuiTextField(fontRendererObj, centerX - 80, centerY + 5, 160, 20);
        fieldZ.setMaxStringLength(12);
        fieldZ.setText(String.valueOf(currentZ));

        buttonList.clear();
        buttonList.add(
            new GuiButton(
                0,
                centerX - 80,
                centerY + 40,
                75,
                20,
                StatCollector.translateToLocal("worldpreview.goto.go")));
        buttonList.add(
            new GuiButton(
                1,
                centerX + 5,
                centerY + 40,
                75,
                20,
                StatCollector.translateToLocal("worldpreview.goto.cancel")));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(
            fontRendererObj,
            StatCollector.translateToLocal("worldpreview.goto.title"),
            width / 2,
            height / 2 - 60,
            0xFFFFFF);
        drawString(fontRendererObj, "X:", width / 2 - 95, height / 2 - 25, 0xA0A0A0);
        drawString(fontRendererObj, "Z:", width / 2 - 95, height / 2 + 10, 0xA0A0A0);
        fieldX.drawTextBox();
        fieldZ.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parentScreen);
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            applyCoordinates();
            return;
        }
        if (keyCode == Keyboard.KEY_TAB) {
            boolean focusX = fieldX.isFocused();
            fieldX.setFocused(!focusX);
            fieldZ.setFocused(focusX);
            return;
        }
        if (fieldX.isFocused()) {
            fieldX.textboxKeyTyped(typedChar, keyCode);
        }
        if (fieldZ.isFocused()) {
            fieldZ.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        fieldX.mouseClicked(mouseX, mouseY, mouseButton);
        fieldZ.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            applyCoordinates();
        } else if (button.id == 1) {
            mc.displayGuiScreen(parentScreen);
        }
    }

    @Override
    public void updateScreen() {
        fieldX.updateCursorCounter();
        fieldZ.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private void applyCoordinates() {
        try {
            int x = Integer.parseInt(
                fieldX.getText()
                    .trim());
            int z = Integer.parseInt(
                fieldZ.getText()
                    .trim());
            preview.jumpToCoordinates(x, z);
            mc.displayGuiScreen(parentScreen);
        } catch (NumberFormatException ignored) {}
    }
}

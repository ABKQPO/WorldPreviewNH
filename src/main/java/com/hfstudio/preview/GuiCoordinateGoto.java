package com.hfstudio.preview;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

/**
 * Simple dialog for entering X/Z coordinates to jump to on the preview map.
 */
public class GuiCoordinateGoto extends GuiScreen {

    private final GuiWorldPreview parent;
    private GuiTextField fieldX;
    private GuiTextField fieldZ;

    public GuiCoordinateGoto(GuiWorldPreview parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        fieldX = new GuiTextField(this.fontRendererObj, centerX - 80, centerY - 30, 160, 20);
        fieldX.setMaxStringLength(12);
        fieldX.setText(String.valueOf(parent.getCenterBlockX()));
        fieldX.setFocused(true);

        fieldZ = new GuiTextField(this.fontRendererObj, centerX - 80, centerY + 5, 160, 20);
        fieldZ.setMaxStringLength(12);
        fieldZ.setText(String.valueOf(parent.getCenterBlockZ()));

        this.buttonList.clear();
        this.buttonList.add(
            new GuiButton(
                0,
                centerX - 80,
                centerY + 40,
                75,
                20,
                StatCollector.translateToLocal("worldpreview.goto.go")));
        this.buttonList.add(
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
        this.drawDefaultBackground();

        this.drawCenteredString(
            this.fontRendererObj,
            StatCollector.translateToLocal("worldpreview.goto.title"),
            this.width / 2,
            this.height / 2 - 60,
            0xFFFFFF);

        this.drawString(this.fontRendererObj, "X:", this.width / 2 - 95, this.height / 2 - 25, 0xA0A0A0);
        this.drawString(this.fontRendererObj, "Z:", this.width / 2 - 95, this.height / 2 + 10, 0xA0A0A0);

        fieldX.drawTextBox();
        fieldZ.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            doGoto();
            return;
        }
        if (key == Keyboard.KEY_TAB) {
            if (fieldX.isFocused()) {
                fieldX.setFocused(false);
                fieldZ.setFocused(true);
            } else {
                fieldZ.setFocused(false);
                fieldX.setFocused(true);
            }
            return;
        }

        // Only allow digits, minus sign, and control keys
        if (fieldX.isFocused()) fieldX.textboxKeyTyped(c, key);
        if (fieldZ.isFocused()) fieldZ.textboxKeyTyped(c, key);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        fieldX.mouseClicked(mouseX, mouseY, button);
        fieldZ.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void updateScreen() {
        fieldX.updateCursorCounter();
        fieldZ.updateCursorCounter();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            doGoto();
        } else if (button.id == 1) {
            mc.displayGuiScreen(parent);
        }
    }

    private void doGoto() {
        try {
            int x = Integer.parseInt(
                fieldX.getText()
                    .trim());
            int z = Integer.parseInt(
                fieldZ.getText()
                    .trim());
            parent.jumpToCoordinates(x, z);
            mc.displayGuiScreen(parent);
        } catch (NumberFormatException e) {
            // Invalid input - just do nothing
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}

package com.hfstudio.preview.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.world.WorldType;

/**
 * Embeddable world-preview content host shared by standalone and tabbed UIs.
 */
public class EmbeddedWorldPreview {

    private final GuiWorldPreview delegate;

    public EmbeddedWorldPreview(GuiScreen parent, long seed, String seedText, WorldType worldType,
        String generatorOptions) {
        delegate = new GuiWorldPreview(parent, seed, seedText, worldType, generatorOptions);
    }

    public void init(Minecraft mc, int width, int height) {
        delegate.initializeEmbedded(mc, width, height);
    }

    public void init(Minecraft mc, int width, int height, int left, int top, int right, int bottom) {
        delegate.initializeEmbedded(mc, width, height, left, top, right, bottom);
    }

    public void setEmbeddedReturnScreen(GuiScreen returnScreen) {
        delegate.setEmbeddedReturnScreen(returnScreen);
    }

    public void setEmbeddedShowBottomControls(boolean showBottomControls) {
        delegate.setEmbeddedShowBottomControls(showBottomControls);
    }

    public void draw(int mouseX, int mouseY, float partialTicks) {
        delegate.drawScreen(mouseX, mouseY, partialTicks);
    }

    public void update() {
        delegate.updateScreen();
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        delegate.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public void mouseClickMove(int mouseX, int mouseY, int mouseButton, long dragTime) {
        delegate.mouseClickMove(mouseX, mouseY, mouseButton, dragTime);
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        delegate.mouseMovedOrUp(mouseX, mouseY, state);
    }

    public void keyTyped(char typedChar, int keyCode) {
        delegate.keyTyped(typedChar, keyCode);
    }

    public void handleMouseInput() {
        delegate.handleMouseInput();
    }

    public void actionPerformed(GuiButton button) {
        delegate.actionPerformed(button);
    }

    public void close() {
        delegate.onGuiClosed();
    }

    public GuiTextField getSeedField() {
        return delegate.getSeedField();
    }

    public void jumpToCoordinates(int x, int z) {
        delegate.jumpToCoordinates(x, z);
    }

    public void cycleWorldTypeForward() {
        delegate.cycleWorldTypeForward();
    }

    public void cycleDimensionNext() {
        delegate.cycleDimensionNext();
    }

    public void cycleDimensionPrevious() {
        delegate.cycleDimensionPrevious();
    }

    public int getCenterBlockX() {
        return delegate.getCenterBlockX();
    }

    public int getCenterBlockZ() {
        return delegate.getCenterBlockZ();
    }

    public int getCurrentDimensionId() {
        return delegate.getCurrentDimensionIdValue();
    }

    public String getCurrentDimensionButtonText() {
        return delegate.getCurrentDimensionButtonText();
    }

    public void applyContext(long seed, String seedText, WorldType worldType, String generatorOptions) {
        delegate.applyEmbeddedContext(seed, seedText, worldType, generatorOptions);
    }

    public String getSeedText() {
        return getSeedField() != null ? getSeedField().getText() : "";
    }
}

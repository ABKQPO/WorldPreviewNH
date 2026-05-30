package com.hfstudio.preview.handler;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;

/**
 * Adapts create-world screen variants to a common preview integration contract.
 */
public interface CreateWorldScreenAdapter {

    GuiButton findAnchorButton(GuiCreateWorld screen, Iterable<?> buttonList);

    void updatePreviewButton(GuiCreateWorld screen, GuiButton previewButton, Iterable<?> buttonList);

    CreateWorldPreviewContext createPreviewContext(GuiCreateWorld screen);
}

package com.hfstudio.preview.handler;

import java.util.Random;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldType;

/**
 * Default adapter for the vanilla create-world layout.
 */
public class VanillaCreateWorldScreenAdapter implements CreateWorldScreenAdapter {

    @Override
    public GuiButton findAnchorButton(GuiCreateWorld screen, Iterable<?> buttonList) {
        for (Object entry : buttonList) {
            if (entry instanceof GuiButton button && button.id == 5) {
                return button;
            }
        }
        return null;
    }

    @Override
    public void updatePreviewButton(GuiCreateWorld screen, GuiButton previewButton, Iterable<?> buttonList) {
        GuiButton anchorButton = findAnchorButton(screen, buttonList);
        if (anchorButton == null || previewButton == null) {
            if (previewButton != null) {
                previewButton.visible = false;
            }
            return;
        }

        previewButton.xPosition = anchorButton.xPosition + anchorButton.width + 4;
        previewButton.yPosition = anchorButton.yPosition + (anchorButton.height - previewButton.height) / 2;
        previewButton.visible = anchorButton.visible;
        previewButton.enabled = anchorButton.enabled;
    }

    @Override
    public CreateWorldPreviewContext createPreviewContext(GuiCreateWorld screen) {
        String seedText = screen.field_146335_h.getText();
        long seed = parseSeed(seedText);
        WorldType worldType = resolveWorldType(screen.field_146331_K);
        String displaySeed = MathHelper.stringNullOrLengthZero(seedText) ? String.valueOf(seed) : seedText;
        String generatorOptions = screen.field_146334_a != null ? screen.field_146334_a : "";
        return new CreateWorldPreviewContext(seed, displaySeed, worldType, generatorOptions);
    }

    protected long parseSeed(String seedText) {
        if (MathHelper.stringNullOrLengthZero(seedText)) {
            return new Random().nextLong();
        }
        try {
            return Long.parseLong(seedText);
        } catch (NumberFormatException e) {
            return seedText.hashCode();
        }
    }

    protected WorldType resolveWorldType(int worldTypeIndex) {
        if (worldTypeIndex >= 0 && worldTypeIndex < WorldType.worldTypes.length) {
            WorldType worldType = WorldType.worldTypes[worldTypeIndex];
            if (worldType != null) {
                return worldType;
            }
        }
        return WorldType.DEFAULT;
    }
}

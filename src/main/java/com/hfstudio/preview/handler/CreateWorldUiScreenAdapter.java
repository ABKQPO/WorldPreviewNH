package com.hfstudio.preview.handler;

import java.lang.reflect.Field;
import java.util.Random;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldType;

import cpw.mods.fml.common.Loader;

/**
 * Runtime adapter for CreateWorldUI's tabbed create-world screen.
 */
public class CreateWorldUiScreenAdapter implements CreateWorldScreenAdapter {

    private static final String MOD_ID = "createworldui";
    private static final int WORLD_TAB_ID = 101;
    private static final int WORLD_TYPE_BUTTON_ID = 5;
    private static final int WORLD_TAB_CONTENT_X_OFFSET = -154;
    private static final int WORLD_TYPE_ROW_Y_OFFSET = 10;
    private static final int PREVIEW_BUTTON_SPACING = 4;
    private static final int TAB_HEIGHT = 24;
    private static final String TAB_MANAGER_FIELD_NAME = "modernWorldCreatingUI$tabManager";
    private static final String CURRENT_TAB_ID_METHOD_NAME = "getCurrentTabId";
    private static final String ACCESSOR_CLASS_NAME = "decok.dfcdvadstf.createworldui.mixin.access.IGuiCreateWorldAccess";
    private static final String GET_SEED_METHOD_NAME = "modernWorldCreatingUI$getSeed";
    private static final String GET_WORLD_TYPE_INDEX_METHOD_NAME = "modernWorldCreatingUI$getWorldTypeIndex";

    @Override
    public GuiButton findAnchorButton(GuiCreateWorld screen, Iterable<?> buttonList) {
        if (!isAvailable() || !isWorldTabSelected(screen)) {
            return null;
        }
        for (Object entry : buttonList) {
            if (entry instanceof GuiButton button && button.id == WORLD_TYPE_BUTTON_ID) {
                return button;
            }
        }
        return createSyntheticAnchor(screen);
    }

    @Override
    public void updatePreviewButton(GuiCreateWorld screen, GuiButton previewButton, Iterable<?> buttonList) {
        if (previewButton == null) {
            return;
        }
        GuiButton anchorButton = findAnchorButton(screen, buttonList);
        if (anchorButton == null) {
            previewButton.visible = false;
            previewButton.enabled = false;
            return;
        }

        previewButton.xPosition = anchorButton.xPosition + anchorButton.width + PREVIEW_BUTTON_SPACING;
        previewButton.yPosition = anchorButton.yPosition;
        previewButton.visible = true;
        previewButton.enabled = true;
    }

    @Override
    public CreateWorldPreviewContext createPreviewContext(GuiCreateWorld screen) {
        String seedText = readSeed(screen);
        long seed = parseSeed(seedText);
        int worldTypeIndex = readWorldTypeIndex(screen);
        WorldType worldType = resolveWorldType(worldTypeIndex);
        String displaySeed = MathHelper.stringNullOrLengthZero(seedText) ? String.valueOf(seed) : seedText;
        String generatorOptions = screen.field_146334_a != null ? screen.field_146334_a : "";
        return new CreateWorldPreviewContext(seed, displaySeed, worldType, generatorOptions);
    }

    public boolean isAvailable() {
        return Loader.isModLoaded(MOD_ID);
    }

    public boolean shouldHandle(GuiCreateWorld screen) {
        return isAvailable() && readField(screen, TAB_MANAGER_FIELD_NAME) != null;
    }

    private boolean isWorldTabSelected(GuiCreateWorld screen) {
        Object tabManager = readField(screen, TAB_MANAGER_FIELD_NAME);
        if (tabManager == null) {
            return false;
        }
        Object currentTabId = invokeNoArgs(tabManager, CURRENT_TAB_ID_METHOD_NAME);
        return currentTabId instanceof Integer tabId && tabId == WORLD_TAB_ID;
    }

    private GuiButton createSyntheticAnchor(GuiCreateWorld screen) {
        int x = screen.width / 2 + WORLD_TAB_CONTENT_X_OFFSET;
        int y = screen.height / 8 + TAB_HEIGHT + WORLD_TYPE_ROW_Y_OFFSET;
        return new GuiButton(WORLD_TYPE_BUTTON_ID, x, y, 150, 20, "");
    }

    private String readSeed(GuiCreateWorld screen) {
        Object accessor = tryCastAccessor(screen);
        Object value = accessor == null ? null : invokeNoArgs(accessor, GET_SEED_METHOD_NAME);
        return value instanceof String seedText ? seedText : "";
    }

    private int readWorldTypeIndex(GuiCreateWorld screen) {
        Object accessor = tryCastAccessor(screen);
        Object value = accessor == null ? null : invokeNoArgs(accessor, GET_WORLD_TYPE_INDEX_METHOD_NAME);
        return value instanceof Integer worldTypeIndex ? worldTypeIndex : 0;
    }

    private Object tryCastAccessor(GuiCreateWorld screen) {
        try {
            Class<?> accessorClass = Class.forName(ACCESSOR_CLASS_NAME);
            return accessorClass.isInstance(screen) ? screen : null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Object readField(Object target, String fieldName) {
        Class<?> currentClass = target.getClass();
        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException ignored) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }

    private Object invokeNoArgs(Object target, String methodName) {
        try {
            return target.getClass()
                .getMethod(methodName)
                .invoke(target);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private long parseSeed(String seedText) {
        if (MathHelper.stringNullOrLengthZero(seedText)) {
            return new Random().nextLong();
        }
        try {
            return Long.parseLong(seedText);
        } catch (NumberFormatException e) {
            return seedText.hashCode();
        }
    }

    private WorldType resolveWorldType(int worldTypeIndex) {
        if (worldTypeIndex >= 0 && worldTypeIndex < WorldType.worldTypes.length) {
            WorldType worldType = WorldType.worldTypes[worldTypeIndex];
            if (worldType != null) {
                return worldType;
            }
        }
        return WorldType.DEFAULT;
    }
}

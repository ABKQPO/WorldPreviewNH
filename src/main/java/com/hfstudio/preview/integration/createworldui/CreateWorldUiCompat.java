package com.hfstudio.preview.integration.createworldui;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import decok.dfcdvadstf.catframe.ui.tab.TabRegistry;
import decok.dfcdvadstf.createworldui.tab.CreateWorldUITabBar;

/**
 * Optional CreateWorldUI integration entry point.
 */
public class CreateWorldUiCompat {

    public static final String MOD_ID = "createworldui";
    private static final int PREVIEW_TAB_ID = 103;

    public void registerPreviewTabIfAvailable() {
        if (!Loader.isModLoaded(MOD_ID)) {
            return;
        }
        registerPreviewTab();
    }

    @Optional.Method(modid = MOD_ID)
    protected void registerPreviewTab() {
        TabRegistry.registerTab(
            CreateWorldUITabBar.BAR_ID,
            CreateWorldPreviewTab::new,
            PREVIEW_TAB_ID,
            "worldpreview.tab.preview");
    }
}

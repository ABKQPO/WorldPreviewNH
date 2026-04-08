package com.hfstudio.preview.handler;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.GuiScreenEvent;

import com.hfstudio.preview.gui.GuiPreviewButton;
import com.hfstudio.preview.gui.GuiWorldPreview;
import com.hfstudio.preview.network.ServerSeedData;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Forge event handler that injects preview buttons into:
 * 1. GuiCreateWorld - 20x20 icon button next to "World Type"
 * 2. GuiIngameMenu - 20x20 icon button next to Statistics (singleplayer only)
 */
public class CreateWorldHandler {

    private static final int PREVIEW_BUTTON_ID = -161520;
    private static final int INGAME_PREVIEW_BUTTON_ID = -161521;

    private GuiButton worldTypeButton;
    private GuiPreviewButton previewButton;
    private GuiButton ingamePreviewButton;

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiCreateWorld) {
            initCreateWorldButton(event);
        } else if (event.gui instanceof GuiIngameMenu) {
            initIngameMenuButton(event);
        }
    }

    @SuppressWarnings("unchecked")
    private void initCreateWorldButton(GuiScreenEvent.InitGuiEvent.Post event) {
        worldTypeButton = null;
        previewButton = null;

        // Find the World Type button (ID 5)
        for (Object obj : event.buttonList) {
            GuiButton btn = (GuiButton) obj;
            if (btn.id == 5) {
                worldTypeButton = btn;
                break;
            }
        }

        if (worldTypeButton != null) {
            // Place 16x16 button to the right of World Type button, vertically centered
            int btnX = worldTypeButton.xPosition + worldTypeButton.width + 4;
            int btnY = worldTypeButton.yPosition + (worldTypeButton.height - 20) / 2;

            previewButton = new GuiPreviewButton(PREVIEW_BUTTON_ID, btnX, btnY);
            previewButton.visible = worldTypeButton.visible;
            event.buttonList.add(previewButton);
        }
    }

    @SuppressWarnings("unchecked")
    private void initIngameMenuButton(GuiScreenEvent.InitGuiEvent.Post event) {
        ingamePreviewButton = null;
        Minecraft mc = Minecraft.getMinecraft();

        // Only show in singleplayer or when server has the mod installed
        if (!mc.isSingleplayer() && !ServerSeedData.isAvailable()) return;

        // Find the Statistics button (ID 7)
        GuiButton statsButton = null;
        for (Object obj : event.buttonList) {
            GuiButton btn = (GuiButton) obj;
            if (btn.id == 7) {
                statsButton = btn;
                break;
            }
        }

        if (statsButton != null) {
            // Place icon button to the right of statistics button
            int btnX = statsButton.xPosition + statsButton.width + 4;
            int btnY = statsButton.yPosition + (statsButton.height - 20) / 2;
            ingamePreviewButton = new GuiPreviewButton(INGAME_PREVIEW_BUTTON_ID, btnX, btnY);
            event.buttonList.add(ingamePreviewButton);
        }
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!(event.gui instanceof GuiCreateWorld)) return;

        // Sync preview button visibility with World Type button (both hidden when "More World Options" is collapsed)
        if (worldTypeButton != null && previewButton != null) {
            previewButton.visible = worldTypeButton.visible;
        }
    }

    @SubscribeEvent
    public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        String tooltip = StatCollector.translateToLocal("worldpreview.button.tooltip");

        if (event.gui instanceof GuiCreateWorld) {
            if (previewButton != null && previewButton.isHovered()) {
                GuiPreviewButton.drawVanillaTooltip(
                    mc.fontRenderer,
                    tooltip,
                    event.mouseX,
                    event.mouseY,
                    event.gui.width,
                    event.gui.height);
            }
        } else if (event.gui instanceof GuiIngameMenu) {
            if (ingamePreviewButton instanceof GuiPreviewButton btn && btn.isHovered()) {
                GuiPreviewButton.drawVanillaTooltip(
                    mc.fontRenderer,
                    tooltip,
                    event.mouseX,
                    event.mouseY,
                    event.gui.width,
                    event.gui.height);
            }
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.gui instanceof GuiCreateWorld && event.button.id == PREVIEW_BUTTON_ID) {
            handleCreateWorldPreview((GuiCreateWorld) event.gui);
        } else if (event.gui instanceof GuiIngameMenu && event.button.id == INGAME_PREVIEW_BUTTON_ID) {
            handleIngamePreview(event.gui);
        }
    }

    private void handleCreateWorldPreview(GuiCreateWorld screen) {
        // Parse the seed from the text field (same logic as vanilla world creation)
        String seedStr = screen.field_146335_h.getText();
        long seed;
        if (!MathHelper.stringNullOrLengthZero(seedStr)) {
            try {
                seed = Long.parseLong(seedStr);
            } catch (NumberFormatException e) {
                seed = seedStr.hashCode();
            }
        } else {
            seed = new Random().nextLong();
        }

        // Read the selected world type
        int worldTypeIndex = screen.field_146331_K;
        WorldType worldType = WorldType.worldTypes[worldTypeIndex];
        if (worldType == null) {
            worldType = WorldType.DEFAULT;
        }

        // Open the preview screen with the create world screen as parent
        String displaySeedStr = MathHelper.stringNullOrLengthZero(seedStr) ? String.valueOf(seed) : seedStr;
        String generatorOptions = screen.field_146334_a != null ? screen.field_146334_a : "";
        Minecraft.getMinecraft()
            .displayGuiScreen(new GuiWorldPreview(screen, seed, displaySeedStr, worldType, generatorOptions));
    }

    private void handleIngamePreview(net.minecraft.client.gui.GuiScreen pauseMenu) {
        Minecraft mc = Minecraft.getMinecraft();

        long seed;
        WorldType worldType;
        String genOptions;

        if (mc.isSingleplayer()) {
            IntegratedServer server = mc.getIntegratedServer();
            if (server == null || server.worldServers == null || server.worldServers.length == 0) return;

            seed = server.worldServers[0].getSeed();
            worldType = server.worldServers[0].getWorldInfo()
                .getTerrainType();
            genOptions = server.worldServers[0].getWorldInfo()
                .getGeneratorOptions();
        } else if (ServerSeedData.isAvailable()) {
            seed = ServerSeedData.getSeed();
            worldType = ServerSeedData.getWorldType();
            genOptions = ServerSeedData.getGeneratorOptions();
        } else {
            return;
        }

        if (worldType == null) {
            worldType = WorldType.DEFAULT;
        }

        // Center on player position and match player dimension
        int playerX = MathHelper.floor_double(mc.thePlayer.posX);
        int playerZ = MathHelper.floor_double(mc.thePlayer.posZ);
        int playerDim = mc.thePlayer.dimension;

        mc.displayGuiScreen(
            new GuiWorldPreview(
                pauseMenu,
                seed,
                String.valueOf(seed),
                worldType,
                genOptions,
                playerX,
                playerZ,
                playerDim));
    }
}

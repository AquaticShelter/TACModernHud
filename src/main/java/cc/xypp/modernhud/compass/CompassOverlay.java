package cc.xypp.modernhud.compass;

import cc.xypp.modernhud.config.CompassConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** The compass overlay (a single reference-style renderer). */
public class CompassOverlay implements IGuiOverlay {
    private static final ModernCompassRenderer RENDERER = new ModernCompassRenderer();

    /** Resource reload: font metrics changed (resource pack / language). The renderer keeps no text-width cache. */
    public static void invalidateTextMetrics() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!CompassConfig.enabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || (mc.screen != null && !(mc.screen instanceof ChatScreen) && !cc.xypp.modernhud.client.gui.config.ConfigPreview.isActive())) {
            return;
        }
        RENDERER.render(graphics, partialTick, screenWidth, screenHeight);
    }
}

package cc.xypp.modernhud.client;

import cc.xypp.modernhud.ModernHud;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Small wrapper around the bar textures with a partial-fill blit helper.
 * Absorption bar textures are 21x6 px; health bar textures are 96x6 px.
 */
public final class BarTextures {

    // Absorption bar (21x6)
    public static final Texture EMPTY = new Texture("textures/gui/bar_empty.png", 21, 6);
    public static final Texture WHITE = new Texture("textures/gui/bar_fill_white.png", 21, 6);
    public static final Texture BLUE = new Texture("textures/gui/bar_fill_blue.png", 21, 6);
    public static final Texture PURPLE = new Texture("textures/gui/bar_fill_purple.png", 21, 6);
    public static final Texture RED = new Texture("textures/gui/bar_fill_red.png", 21, 6);

    // Health bar (96x6) - the player's own health, part of the personal HUD widget.
    public static final Texture HEALTH_EMPTY = new Texture("textures/gui/health_empty.png", 96, 6);
    public static final Texture HEALTH_FILL = new Texture("textures/gui/health_fill.png", 96, 6);

    // Feedback icons (256x256, square)
    public static final Texture ICON_BREAK = new Texture("textures/gui/icon_break.png", 256, 256);
    public static final Texture ICON_HIT = new Texture("textures/gui/icon_hit.png", 256, 256);

    private BarTextures() {
    }

    /** Picks the fill colour tier based on how many segments are currently filled. */
    public static Texture tierFor(int filledSegments) {
        if (filledSegments > 3) {
            return RED;
        }
        if (filledSegments > 2) {
            return PURPLE;
        }
        if (filledSegments > 1) {
            return BLUE;
        }
        return WHITE;
    }

    public static final class Texture {
        public final ResourceLocation location;
        public final int width;
        public final int height;

        Texture(String path, int width, int height) {
            this.location = new ResourceLocation(ModernHud.MOD_ID, path);
            this.width = width;
            this.height = height;
        }

        /**
         * Draws a horizontal partial-fill slice of this texture, left-aligned, stretched
         * (or cropped) to fit a slot of slotWidth x slotHeight screen pixels.
         * fillFraction is 0.0 - 1.0, how much of the slot is filled from the left.
         */
        public void blitPartial(GuiGraphics graphics, int x, int y, int slotWidth, int slotHeight, float fillFraction) {
            fillFraction = Math.max(0.0f, Math.min(1.0f, fillFraction));
            int drawWidth = Math.round(slotWidth * fillFraction);
            int srcWidth = Math.round(width * fillFraction);
            if (drawWidth <= 0 || srcWidth <= 0 || slotHeight <= 0) {
                return;
            }
            RenderSystem.enableBlend();
            graphics.blit(location, x, y, drawWidth, slotHeight, 0, 0, srcWidth, height, width, height);
            RenderSystem.disableBlend();
        }

        public void blitFull(GuiGraphics graphics, int x, int y, int slotWidth, int slotHeight) {
            blitPartial(graphics, x, y, slotWidth, slotHeight, 1.0f);
        }
    }
}

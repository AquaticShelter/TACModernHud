package cc.xypp.modernhud.client;

import cc.xypp.modernhud.config.HudConfig;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A soft glow halo around a rectangle. Built on {@link SoftDraw#soft}: one feathered mesh with per-vertex alpha
 * (a gentle tint over the rect, brightest right at its rim, fading smoothly to nothing outward), added to
 * whatever is behind it. This replaces the old technique of stacking 17 whole-pixel-offset copies of the rect,
 * whose steps were visible as blocky, pixelated bands - especially at high GUI scales, where one GUI pixel is
 * several screen pixels. Same call signature, so every existing glow (weapon switch, low ammo, ready slots,
 * team "down" pulse, ...) gets the smoother look automatically.
 */
public final class GlowEffect {

    private GlowEffect() {
    }

    /**
     * Draws a glow halo around a {@code w x h} rect at {@code (x, y)}.
     *
     * @param colorRgb glow colour, 0xRRGGBB
     * @param alpha    overall glow strength, 0-1 - already including any fade/pulse the caller wants
     * @param spread   how far (px) the glow reaches beyond the rect's edge (the visible falloff is smooth, so it
     *                 reads a little wider than the old stepped version at the same value)
     */
    public static void drawGlowRect(GuiGraphics g, int x, int y, int w, int h, int colorRgb, float alpha, float spread) {
        float k = HudConfig.glowIntensity / 100f; // global "Glow intensity" setting (0 = all glows off)
        alpha *= k;
        if (alpha <= 0.001f || spread <= 0.01f || w <= 0 || h <= 0) {
            return;
        }
        // Wider, gentler falloff than before: the halo is now a round-cornered smoothstep fade (see SoftDraw), so a
        // little more reach reads as light rather than as a hard box edge.
        SoftDraw.soft(g, x, y, w, h, colorRgb, 0.22f * alpha, 0.58f * alpha, Math.max(1.5f, spread * 2.4f), true);
    }

    /** Round bloom behind an icon / dot / piece of text: centre (cx, cy), reach {@code radius} px. Scaled by the
     *  global glow intensity; the colour is added on top of what is already drawn. */
    public static void drawBloom(GuiGraphics g, float cx, float cy, float radius, int colorRgb, float alpha) {
        alpha *= HudConfig.glowIntensity / 100f;
        if (alpha <= 0.001f || radius <= 0.5f) {
            return;
        }
        SoftDraw.radial(g, cx, cy, radius, colorRgb, 0.5f * alpha, true);
    }
}

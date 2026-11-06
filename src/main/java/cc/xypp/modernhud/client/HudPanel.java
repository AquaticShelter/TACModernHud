package cc.xypp.modernhud.client;

import cc.xypp.modernhud.config.ElementStyle;
import cc.xypp.modernhud.config.PanelBorderStyle;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The shared background panel drawn behind the player widget's and the weapon widget's individual
 * elements.
 *
 * <p>The fill itself was previously a directional "fade" (a gradient from opaque to fully
 * transparent). That's gone: per explicit feedback this should NOT fade at all — it's a single
 * flat colour at one caller-controlled opacity (0-100%), same everywhere across the panel, no
 * gradient. Simpler and does exactly what a "toggle-able shadowbox" implies: either a flat tinted
 * backdrop at whatever strength you set, or nothing.
 *
 * <p>{@link #drawBorder} layers an optional edge accent on top of that fill, in one of three
 * styles (see {@link PanelBorderStyle}): none at all, four short "L" corner marks (modelled on the
 * referenced MW4 HUD screenshot — edges between corners stay open, not a boxed outline), or a
 * complete thin 1px outline. Callers gate this behind their own "show panel" flag, since it's
 * opt-in on top of the fill being on, not a replacement for it.
 *
 * <p>Every fill here forces {@code RenderSystem}'s shader colour to opaque white immediately
 * beforehand, then restores whatever it was — because vanilla's GUI shaders multiply every flat
 * fill's colour by that global value, a panel drawn while it's left in some OTHER tinted/faded
 * state (an exception mid-render elsewhere in this mod, or in another mod entirely, that skipped
 * its own restore step) would silently render at the wrong opacity or not at all. This was the
 * actual root cause behind panels reported as "disappearing" around certain other mods: not an
 * incompatibility with THIS panel's own drawing code, but this code trusting ambient GL state it
 * had no guarantee was still sane. Never assume it is again.
 */
public final class HudPanel {

    private HudPanel() {
    }

    /**
     * Flat-fills a {@code w x h} rectangle at {@code (x, y)} with {@code colorRgb} at
     * {@code opacityPercent} (0-100, clamped). {@code opacityPercent <= 0} draws nothing at all
     * (equivalent to the panel being off, but callers should still gate on their own
     * {@code showPanel} flag first to skip the call entirely).
     */
    public static void draw(GuiGraphics g, int x, int y, int w, int h, int colorRgb, double opacityPercent) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int alpha = (int) Math.round(clamp(opacityPercent, 0.0, 100.0) * 2.55);
        if (alpha <= 0) {
            return;
        }
        int color = (alpha << 24) | (colorRgb & 0xFFFFFF);
        withSafeShaderColor(() -> g.fill(x, y, x + w, y + h, color));
    }

    /** Panel + border of an {@link ElementStyle}, both scaled by {@code alpha} (0-1, already including the
     *  element's own fade) and by the style's overall opacity. */
    public static void drawStyled(GuiGraphics g, int x, int y, int w, int h, ElementStyle st, float alpha) {
        float a = st.alpha(alpha);
        if (a <= 0.001f) {
            return;
        }
        if (st.panelEnabled) {
            draw(g, x, y, w, h, st.panelColor, st.panelOpacity * a);
        }
        if (st.borderStyle != PanelBorderStyle.NONE) {
            drawBorder(g, x, y, w, h, st.borderColor, st.borderOpacity * a, st.borderStyle, st.borderLength,
                    st.borderOutset, st.borderScale / 100.0);
        }
    }

    /** Dispatches to the right edge style — see {@link PanelBorderStyle}. {@code armLength} only
     *  matters for {@link PanelBorderStyle#CORNERS}. {@code outset} pushes CORNERS-style brackets
     *  outward from the box edge by that many px (0 = flush with the edge, the old behaviour) —
     *  ignored for FULL/NONE, since a full outline sitting flush with the fill IS the point of it. */
    public static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int colorRgb, double opacityPercent,
                                   PanelBorderStyle style, int armLength, int outset) {
        drawBorder(g, x, y, w, h, colorRgb, opacityPercent, style, armLength, outset, 1.0);
    }

    /** The size of a dot for a given {@code borderLength} (as before: length - 1, at least 2), times {@code scale}. */
    public static int dotSize(int armLength, double scale) {
        return Math.max(1, (int) Math.round(Math.max(2, armLength - 1) * scale));
    }

    /** Same, with {@code scale} (1.0 = unchanged) applied to the corner brackets' arm length and to the dot size. */
    public static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int colorRgb, double opacityPercent,
                                   PanelBorderStyle style, int armLength, int outset, double scale) {
        if (style == PanelBorderStyle.NONE) {
            return;
        }
        final int arm = Math.max(1, (int) Math.round(armLength * scale));
        final int dot = dotSize(armLength, scale);
        HudBatch.run(g, () -> {
            switch (style) {
                case CORNERS -> drawCornerBrackets(g, x, y, w, h, colorRgb, opacityPercent, arm, outset);
                case FULL -> drawFullBorder(g, x, y, w, h, colorRgb, opacityPercent);
                case DOTS -> drawCornerDots(g, x, y, w, h, colorRgb, opacityPercent, dot, outset);
                case BOTTOM_DOTS -> drawBottomDots(g, x, y, w, h, colorRgb, opacityPercent, dot);
                case FRAME -> {
                    drawFullBorder(g, x, y, w, h, colorRgb, opacityPercent * 0.35);
                    drawCornerDots(g, x, y, w, h, colorRgb, opacityPercent, dot, 0);
                }
                case NONE -> { /* nothing to draw */ }
            }
        });
    }

    /**
     * Four short "L" corner marks around a {@code w x h} rectangle at {@code (x, y)} — two 1px-thick
     * segments per corner. With {@code outset = 0} they're flush with that rectangle's edges; with
     * {@code outset > 0} they sit that many px OUTSIDE the box instead, framing it with a small gap
     * rather than sitting on its border — matching the reference HUD, where the brackets visibly
     * float outside the glow/box they frame rather than tracing its edge. Edges between corners are
     * always left open (no continuous border) either way.
     *
     * <p>{@code armLength} is a caller-configured MAXIMUM, not a fixed length: the actual arm drawn
     * is also capped to a fraction of the box's own smaller dimension (see {@code proportionalCap}
     * below). Without that second cap, a bracket sized right for a big icon box read as oversized
     * and "in the way" on the small text chips (mag/reserve/fire-mode) once every element started
     * getting its own individually-sized box — this keeps the same configured length from looking
     * chunky on the small ones while still reaching its full configured size on the bigger ones.
     */
    public static void drawCornerBrackets(GuiGraphics g, int x, int y, int w, int h, int colorRgb,
                                           double opacityPercent, int armLength, int outset) {
        if (w <= 0 || h <= 0 || armLength <= 0) {
            return;
        }
        int alpha = (int) Math.round(clamp(opacityPercent, 0.0, 100.0) * 2.55);
        if (alpha <= 0) {
            return;
        }
        int color = (alpha << 24) | (colorRgb & 0xFFFFFF);
        int proportionalCap = Math.max(1, Math.round(Math.min(w, h) * 0.3f));
        int arm = Math.min(armLength, proportionalCap);
        int t = 1; // fixed 1px thickness - crisp, pixel-HUD scale; not worth exposing as config

        // Expand the box outward by `outset` first - everything below then just draws flush corner
        // marks on THIS larger virtual box, same as before.
        int ox = x - outset;
        int oy = y - outset;
        int ow = w + outset * 2;
        int oh = h + outset * 2;

        withSafeShaderColor(() -> {
            // top-left
            g.fill(ox, oy, ox + arm, oy + t, color);
            g.fill(ox, oy, ox + t, oy + arm, color);
            // top-right
            g.fill(ox + ow - arm, oy, ox + ow, oy + t, color);
            g.fill(ox + ow - t, oy, ox + ow, oy + arm, color);
            // bottom-left
            g.fill(ox, oy + oh - t, ox + arm, oy + oh, color);
            g.fill(ox, oy + oh - arm, ox + t, oy + oh, color);
            // bottom-right
            g.fill(ox + ow - arm, oy + oh - t, ox + ow, oy + oh, color);
            g.fill(ox + ow - t, oy + oh - arm, ox + ow, oy + oh, color);
        });
    }

    /** A small solid square centred on each of the four corners of the rectangle (pushed {@code outset} px
     *  outward) - the "dot" border type. */
    public static void drawCornerDots(GuiGraphics g, int x, int y, int w, int h, int colorRgb,
                                       double opacityPercent, int dotSize, int outset) {
        if (w <= 0 || h <= 0 || dotSize <= 0) {
            return;
        }
        int alpha = (int) Math.round(clamp(opacityPercent, 0.0, 100.0) * 2.55);
        if (alpha <= 0) {
            return;
        }
        int color = (alpha << 24) | (colorRgb & 0xFFFFFF);
        int size = Math.min(dotSize, Math.max(1, Math.min(w, h) / 2));
        int half = size / 2;
        int x0 = x - outset - half;
        int y0 = y - outset - half;
        int x1 = x + w + outset - (size - half);
        int y1 = y + h + outset - (size - half);
        withSafeShaderColor(() -> {
            g.fill(x0, y0, x0 + size, y0 + size, color);
            g.fill(x1, y0, x1 + size, y0 + size, color);
            g.fill(x0, y1, x0 + size, y1 + size, color);
            g.fill(x1, y1, x1 + size, y1 + size, color);
        });
    }

    /** A complete thin (1px) outline around all four edges of a {@code w x h} rectangle at
     *  {@code (x, y)} — the {@link PanelBorderStyle#FULL} option. */
    public static void drawFullBorder(GuiGraphics g, int x, int y, int w, int h, int colorRgb, double opacityPercent) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int alpha = (int) Math.round(clamp(opacityPercent, 0.0, 100.0) * 2.55);
        if (alpha <= 0) {
            return;
        }
        int color = (alpha << 24) | (colorRgb & 0xFFFFFF);
        int t = 1;
        withSafeShaderColor(() -> {
            g.fill(x, y, x + w, y + t, color);             // top
            g.fill(x, y + h - t, x + w, y + h, color);      // bottom
            g.fill(x, y, x + t, y + h, color);              // left
            g.fill(x + w - t, y, x + w, y + h, color);       // right
        });
    }

    /**
     * Two small filled squares, one at each BOTTOM corner only — nothing at the top corners at all.
     * The secondary/neutral weapon box's own accent, distinct from the primary icon's full 4-corner
     * brackets, matching the reference HUD's more minimal treatment for a slot that isn't the one
     * actively being fired.
     */
    public static void drawBottomDots(GuiGraphics g, int x, int y, int w, int h, int colorRgb, double opacityPercent, int dotSize) {
        if (w <= 0 || h <= 0 || dotSize <= 0) {
            return;
        }
        int alpha = (int) Math.round(clamp(opacityPercent, 0.0, 100.0) * 2.55);
        if (alpha <= 0) {
            return;
        }
        int color = (alpha << 24) | (colorRgb & 0xFFFFFF);
        int size = Math.min(dotSize, Math.max(1, Math.min(w, h) / 3));
        withSafeShaderColor(() -> {
            g.fill(x, y + h - size, x + size, y + h, color);                     // bottom-left
            g.fill(x + w - size, y + h - size, x + w, y + h, color);             // bottom-right
        });
    }

    /** Forces shader colour to opaque white for the duration of {@code action}, then restores
     *  whatever it was before — GUARANTEED via try/finally, so an exception inside {@code action}
     *  can never leave shader colour corrupted for every draw call that follows it this frame. */
    private static void withSafeShaderColor(Runnable action) {
        // getShaderColor() returns the INTERNAL array: copy the values first, or "restoring" it restores nothing
        float[] cur = RenderSystem.getShaderColor();
        float pr = cur[0], pg = cur[1], pb = cur[2], pa = cur[3];
        try {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            action.run();
        } finally {
            RenderSystem.setShaderColor(pr, pg, pb, pa);
        }
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Inset padding a caller should leave between the panel edge and its own content. */
    public static int padding() {
        return 5;
    }
}

package cc.xypp.modernhud.client;

import cc.xypp.modernhud.client.render.SdfMath;
import cc.xypp.modernhud.client.render.SdfShapes;
import cc.xypp.modernhud.config.HudConfig;
import cc.xypp.modernhud.config.PlateFill;
import cc.xypp.modernhud.config.PlateShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Code-drawn HUD bars — the render-engine replacement for PNG bars, tuned to the Modern Warfare
 * 2022 / Warzone / DMZ look: thin, seamless RECTANGLES with an ultra-thin edge (no chunky border),
 * a faint top sheen, and armour "plates" as separated segments. Everything is {@link GuiGraphics#fill}
 * only — no textures, crisp at any scale.
 *
 * <p>Two behaviours borrowed from Damage Engine: bars glide toward their real value ({@link #smooth})
 * and are drawn in one place so measured height can't drift from what's rendered.
 */
public final class BarRender {

    private BarRender() {
    }

    /** Empty-track colour: a faint dark wash so the bar reads over any world without a hard border. */
    public static final int TRACK = 0x66000000;
    /** Ultra-thin edge line (MW "seamless" hairline). Very subtle. */
    public static final int EDGE = 0x33FFFFFF;

    // MW plate tiers, keyed by how many plates are FULLY filled. Progression per request:
    // gray (starting) → blue → purple → red. So 1 full plate is still gray, 2 = blue, 3 = purple,
    // 4+ = red — the colour only climbs as you stack more complete plates. Only used when
    // AbsorptionColorMode is TIERED; CUSTOM mode skips this entirely (see plates() below).
    public static final int TIER_GRAY = 0xFFB8BFC6;
    public static final int TIER_BLUE = 0xFF54ABE8;
    public static final int TIER_PURPLE = 0xFF9E6FE2;
    public static final int TIER_RED = 0xFFE85138;

    /** MW2022 health: clean white. */
    public static final int HEALTH = 0xFFF4F6F8;
    public static final int HEALTH_LOW = 0xFFE85138;

    /** Default "ocean blue" used when a caller wants a fixed absorption colour but supplies none. */
    public static final int OCEAN_BLUE = 0xFF1C7ED6;

    public static int tierColor(int filledSegments) {
        if (filledSegments >= 4) return TIER_RED;
        if (filledSegments == 3) return TIER_PURPLE;
        if (filledSegments == 2) return TIER_BLUE;
        return TIER_GRAY;
    }

    /** Seamless rectangular track: faint wash + a 1px top sheen line. No heavy border. */
    public static void track(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, TRACK);
        g.fill(x, y, x + w, y + 1, EDGE); // hairline top edge
    }

    /** Rectangular fill from the left, with a 1px brighter top line for the sleek MW sheen. */
    public static void fill(GuiGraphics g, int x, int y, int w, int h, float fraction, int color) {
        int filled = Math.round(w * clamp01(fraction));
        if (filled <= 0) return;
        g.fill(x, y, x + filled, y + h, color);
        if (h >= 3) {
            g.fill(x, y, x + filled, y + 1, lighten(color, 0.30f));
        }
    }

    /** Track + fill — the common thin rectangle bar. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, float fraction, int color) {
        track(g, x, y, w, h);
        fill(g, x, y, w, h, fraction, color);
    }

    /** Track + fill, but filled from the RIGHT edge growing LEFTWARD instead of the usual left-to-
     *  right — e.g. the weapon widget's heat bar, so it reads as "heat rising toward the icon"
     *  rather than mimicking an ammo/health bar's usual direction. */
    public static void barRTL(GuiGraphics g, int x, int y, int w, int h, float fraction, int color) {
        track(g, x, y, w, h);
        int filled = Math.round(w * clamp01(fraction));
        if (filled <= 0) return;
        int fx = x + w - filled;
        g.fill(fx, y, x + w, y + h, color);
        if (h >= 3) {
            g.fill(fx, y, x + w, y + 1, lighten(color, 0.30f));
        }
    }

    /**
     * MW2022 armour PLATES: {@code segments} separated rectangles with a visible gap between them.
     * Each plate fills proportionally so a half-charged plate reads correctly.
     *
     * @param useCustomColor when true, every plate uses {@code customColor} instead of the
     *                       gray/blue/purple/red tier progression (the "custom absorption colour"
     *                       config option — e.g. locking the whole bar to a single ocean blue).
     */
    public static void plates(GuiGraphics g, int x, int y, int w, int h,
                              float value, int segments, double perSegment,
                              boolean useCustomColor, int customColor) {
        if (segments <= 0) return;
        double barMax = segments * perSegment;
        int filledWhole = (int) Math.floor(Math.min(value, barMax) / perSegment);
        // useCustomColor's value comes straight from config as plain 0xRRGGBB (no alpha byte) -
        // force it opaque here so a custom absorption colour can never end up invisible.
        int color = useCustomColor ? (customColor | 0xFF000000) : tierColor(filledWhole);
        final int gap = Math.max(2, w / 40);                 // clear separation between plates
        final int cellW = Math.max(1, (w - gap * (segments - 1)) / segments);
        HudBatch.run(g, () -> {
            for (int i = 0; i < segments; i++) {
                int cx = x + i * (cellW + gap);
                g.fill(cx, y, cx + cellW, y + h, TRACK);         // plate slot
                double segValue = value - i * perSegment;
                if (segValue > 0) {
                    float frac = (float) Math.min(1.0, segValue / perSegment);
                    int fw = Math.round(cellW * frac);
                    g.fill(cx, y, cx + fw, y + h, color);
                    if (h >= 3) g.fill(cx, y, cx + fw, y + 1, lighten(color, 0.30f));
                }
            }
        });
    }

    /** How much absorption is hidden above what the bar can visually represent (never negative). */
    /** Smallest plate height that still shows a shape (a 3 pixel strip cannot have cut corners). */
    public static final int MIN_SHAPED_HEIGHT = 6;

    /** Height of the absorption row for the configured shape. */
    public static int plateHeight(int configured, PlateShape shape) {
        return plateHeight(configured, shape, 0);
    }

    /**
     * Same, but knows the width of the whole row. WARZONE is a long, slim plate (about 9.5 : 1 in the reference), so its
     * height is derived from the width of one plate in SCREEN pixels (the HUD's GUI scale is not a whole number and is
     * not Minecraft's), then rounded up to whole layout units so the row reserves enough room. The plate itself is drawn
     * with the exact pixel height and centred in that room (see {@link SdfShapes#plates}). {@code absorptionHeight} does
     * not thicken it: the slimness is the point of the shape.
     */
    public static int plateHeight(int configured, PlateShape shape, int rowWidth) {
        int h = Math.max(2, configured);
        if (shape == PlateShape.BAR) {
            return h;
        }
        boolean smooth = HudConfig.absorptionSmooth && SdfShapes.usable();
        if (shape == PlateShape.WARZONE && smooth && rowWidth > 0) {
            int seg = Math.max(1, HudConfig.segmentCount);
            int gap = Math.max(2, rowWidth / 40);
            double cell = Math.max(1.0, (rowWidth - gap * (seg - 1)) / (double) seg);
            double phys = 1.0;
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getWindow() != null) {
                phys = Math.max(0.5, HudScale.wanted(mc.getWindow()));
            }
            int hPx = SdfMath.warzoneHeightPx((int) Math.round(cell * phys));
            return Math.max(2, (int) Math.ceil(hPx / phys));
        }
        // smooth shapes are drawn at the screen's own resolution, so they look right from 4 units; the pixel fallback needs 6
        int min = smooth ? 4 : MIN_SHAPED_HEIGHT;
        if (shape == PlateShape.WARZONE) {
            min = 5;   // pixel fallback: 1 px cut-out under a round top
        }
        return Math.max(h, min);
    }

    /**
     * The extent of row {@code r} (0 = top) of a plate {@code w} by {@code h} pixels: {first x, one past the last x}.
     * Rows are whole pixels, so every shape is pixel exact.
     */
    static int[] span(PlateShape shape, int r, int w, int h) {
        int l = 0;
        int e = w;
        switch (shape) {
            case CHAMFER, PLATE, PILL -> {
                int c = Math.max(1, Math.min(h / 3, w / 3));
                int d = r < c ? c - r : (r >= h - c ? r - (h - c) + 1 : 0);
                l = d;
                e = w - d;
            }
            case HEX -> {
                int c = Math.max(1, Math.min(h / 2, w / 3));
                float mid = (h - 1) / 2f;
                float t = mid <= 0f ? 0f : Math.abs(r - mid) / mid; // 0 in the middle row, 1 at the top and bottom rows
                int d = Math.round(c * t);
                l = d;
                e = w - d;
            }
            case SLANT -> {
                int s = Math.max(1, Math.min(h / 2 + 1, w / 3));
                int denom = Math.max(1, h - 1);
                l = Math.round(s * (float) (h - 1 - r) / denom);
                e = w - Math.round(s * (float) r / denom);
            }
            default -> {
            }
        }
        return new int[]{l, Math.max(l + 1, e)};
    }

    /** Plates with a shape (see {@link PlateShape}); BAR is the old rectangle. */
    public static void platesShaped(GuiGraphics g, int x, int y, int w, int h,
                                    float value, int segments, double perSegment,
                                    boolean useCustomColor, int customColor,
                                    PlateShape shape, PlateFill fill, boolean outline) {
        if (shape == PlateShape.BAR) {
            plates(g, x, y, w, h, value, segments, perSegment, useCustomColor, customColor);
            return;
        }
        if (segments <= 0) return;
        double barMax = segments * perSegment;
        int filledWhole = (int) Math.floor(Math.min(value, barMax) / perSegment);
        int color = useCustomColor ? (customColor | 0xFF000000) : tierColor(filledWhole);

        // preferred: smooth, anti-aliased shapes (one draw call for the whole row)
        if (HudConfig.absorptionSmooth && SdfShapes.usable()) {
            int sgap = Math.max(2, w / 40) + (shape == PlateShape.SLANT ? 2 : 0);
            float scell = Math.max(3f, (w - sgap * (segments - 1)) / (float) segments);
            float[] fractions = new float[segments];
            for (int i = 0; i < segments; i++) {
                double segValue = value - i * perSegment;
                fractions[i] = segValue > 0 ? (float) Math.min(1.0, segValue / perSegment) : 0f;
            }
            // the texture carries a vertical shading ramp (bright top, darker bottom): start a little brighter
            // WARZONE is flat in the reference: no extra brightening, a slate slot (an empty slim plate stays visible
            // on dark maps) and a soft hairline
            boolean wz = shape == PlateShape.WARZONE;
            if (SdfShapes.plates(g, x, y, scell, h, sgap, segments, fractions,
                    (wz ? color : lighten(color, 0.14f)) | 0xFF000000,
                    wz ? 0x9A39414A : 0xA00A0F14, wz ? 0x50FFFFFF : 0x70FFFFFF, shape, fill, outline)) {
                return;
            }
        }
        final int gap = Math.max(2, w / 40) + (shape == PlateShape.SLANT ? 1 : 0);
        final int cellW = Math.max(3, (w - gap * (segments - 1)) / segments);
        final int hl = lighten(color, 0.30f);
        if (shape == PlateShape.WARZONE) {
            warzonePixels(g, x, y, cellW, h, gap, segments, value, perSegment, color, fill);
            return;
        }
        HudBatch.run(g, () -> {
            for (int i = 0; i < segments; i++) {
                int cx = x + i * (cellW + gap);
                double segValue = value - i * perSegment;
                float frac = segValue > 0 ? (float) Math.min(1.0, segValue / perSegment) : 0f;
                int fillW = Math.round(cellW * frac);
                int fillRows = frac > 0f ? Math.max(1, Math.round(h * frac)) : 0;
                int[] prev = null;
                for (int r = 0; r < h; r++) {
                    int[] s = span(shape, r, cellW, h);
                    int x0 = cx + s[0];
                    int x1 = cx + s[1];
                    g.fill(x0, y + r, x1, y + r + 1, TRACK);                         // plate slot
                    if (frac > 0f) {
                        if (fill == PlateFill.HORIZONTAL) {
                            int fx1 = Math.min(x1, cx + fillW);
                            if (fx1 > x0) {
                                g.fill(x0, y + r, fx1, y + r + 1, r == 0 && h >= 3 ? hl : color);
                            }
                        } else if (r >= h - fillRows) {
                            g.fill(x0, y + r, x1, y + r + 1, r == h - fillRows && h >= 3 ? hl : color);
                        }
                    }
                    if (outline) {
                        boolean edgeRow = r == 0 || r == h - 1;
                        if (edgeRow) {
                            g.fill(x0, y + r, x1, y + r + 1, PLATE_EDGE);          // top and bottom edge
                        } else {
                            int[] next = span(shape, r + 1, cellW, h);
                            int[] before = prev != null ? prev : span(shape, r - 1, cellW, h);
                            // left / right edge pixels, widened where the outline steps so it stays unbroken
                            int lw = Math.max(1, Math.max(Math.abs(s[0] - before[0]), Math.abs(s[0] - next[0])));
                            int rw = Math.max(1, Math.max(Math.abs(s[1] - before[1]), Math.abs(s[1] - next[1])));
                            g.fill(x0, y + r, Math.min(x1, x0 + lw), y + r + 1, PLATE_EDGE);
                            g.fill(Math.max(x0, x1 - rw), y + r, x1, y + r + 1, PLATE_EDGE);
                        }
                    }
                    prev = s;
                }
            }
        });
    }

    /**
     * Pixel-exact WARZONE plates for when the smooth path is off or failed. Per row: the outer span follows the round
     * top corners (a quarter circle in whole pixels); in the bottom rows the middle is cut out, leaving a foot at each end.
     */
    private static void warzonePixels(GuiGraphics g, int x, int y, int cellW, int h, int gap, int segments,
                                      float value, double perSegment, int color, PlateFill fill) {
        final int depth = Math.max(1, Math.round(0.22f * h));
        final int feet = Math.max(2, Math.min(Math.round(1.2f * h), Math.round(0.25f * cellW)));
        final int rad = Math.max(1, Math.min(Math.round(0.5f * h), cellW / 4));
        HudBatch.run(g, () -> {
            for (int i = 0; i < segments; i++) {
                int cx = x + i * (cellW + gap);
                double segValue = value - i * perSegment;
                float frac = segValue > 0 ? (float) Math.min(1.0, segValue / perSegment) : 0f;
                int fillW = Math.round(cellW * frac);
                int fillRows = frac > 0f ? Math.max(1, Math.round(h * frac)) : 0;
                for (int r = 0; r < h; r++) {
                    // inset of the round top corners in this row (circle of radius rad)
                    int inset = 0;
                    if (r < rad) {
                        double dy = rad - (r + 0.5);
                        inset = (int) Math.round(rad - Math.sqrt(Math.max(0.0, rad * (double) rad - dy * dy)));
                    }
                    int x0 = cx + inset;
                    int x1 = cx + cellW - inset;
                    boolean cut = r >= h - depth;
                    boolean on = fill == PlateFill.HORIZONTAL ? frac > 0f : r >= h - fillRows;
                    // the row is one span, or (bottom rows) two feet
                    int[][] spans = cut
                            ? new int[][]{{x0, cx + feet}, {cx + cellW - feet, x1}}
                            : new int[][]{{x0, x1}};
                    for (int[] sp : spans) {
                        if (sp[1] <= sp[0]) continue;
                        g.fill(sp[0], y + r, sp[1], y + r + 1, TRACK);
                        if (on) {
                            int e = fill == PlateFill.HORIZONTAL ? Math.min(sp[1], cx + fillW) : sp[1];
                            if (e > sp[0]) g.fill(sp[0], y + r, e, y + r + 1, color);
                        }
                    }
                }
            }
        });
    }

    /** Faint white outline of a plate, like the thin frame of the icons in the reference HUDs. */
    private static final int PLATE_EDGE = 0x55FFFFFF;

    public static float overflow(float value, int segments, double perSegment) {
        double barMax = segments * perSegment;
        return (float) Math.max(0.0, value - barMax);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    public static int lighten(int argb, float amount) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        r = (int) (r + (255 - r) * amount);
        g = (int) (g + (255 - g) * amount);
        b = (int) (b + (255 - b) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}

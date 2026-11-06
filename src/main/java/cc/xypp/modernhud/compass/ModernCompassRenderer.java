package cc.xypp.modernhud.compass;

import cc.xypp.modernhud.client.HudPanel;
import cc.xypp.modernhud.client.HudText;
import cc.xypp.modernhud.config.CompassConfig;
import cc.xypp.modernhud.config.LocationDisplayMode;
import cc.xypp.modernhud.config.PanelBorderStyle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import org.joml.Matrix4f;

import java.util.Locale;

/**
 * The Modern Warfare-style compass (style = MODERN). Every number below was measured from the reference
 * screenshots, in "label heights" (the label glyph is 8 units tall), so the proportions hold at any scale:
 *
 * <pre>
 *   ◁ tiny                       ▽  (hollow)                     tiny ▷      y = -6
 *   ·   ·   ·   ·   ·   ·   ·   ·|·   ·   ·   ·   ·   ·   ·   ·              top dots        y = 0
 *     30   ·   ·   NE   ·   ·   60 |75   ·   ·    E    ·   ·   105           labels + mid dots  y = 4..12
 *   ¦   ▪   ▪   ¦   ▪   ▪   ¦   ▪ |¦   ▪   ▪   ¦   ▪   ▪   ¦                 bottom marks    y = 16
 *                                 △  (hollow)                                  y = 18
 *                  ┌ E 74 ┐ │ Backlot │                                        box row  y = 25
 *                      LOC.OVERWORLD                                           tiny tag
 * </pre>
 *
 * A tick position every 5 degrees: at the 5 / 10 degree positions a column of THREE small dots (top, middle, bottom),
 * at the 15 degree positions the label sits in the middle with a short dot above and a short vertical dash below;
 * a faint line runs through the centre between the two triangles. The readout and the place name are two boxes of
 * the same height and text size, centred together as a pair. Text uses {@link HudText#text}, so the glyphs stay
 * pixel-perfect at any scale.
 */
public class ModernCompassRenderer {

    private static final String[] WIND = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    private static final Component[] WIND_BOLD = new Component[8];
    private static final String[] NUMBERS = new String[360];
    private static final float SPAN_DEGREES = 90f;

    // geometry in GUI units (label glyph = 8 units tall)
    private static final int LABEL_Y = 4;
    /** The three small vertical dashes of a tick column, measured from the reference: {start, end} in units. */
    private static final float DASH_TOP_A = -1.25f, DASH_TOP_B = -0.1f;      // short
    private static final float DASH_MID_A = 6.35f, DASH_MID_B = 7.5f;        // short
    private static final float DASH_BOT_A = 13.9f, DASH_BOT_B = 16.25f;      // long (about 2x the others)
    /** Hollow centre triangles: size in units (reference: 8 x 7 px next to a 12 px digit). */
    private static final float TRI_W = 4.67f, TRI_H = 4.1f;
    private static final float TRI_TOP_Y = -5.9f;
    private static final float TRI_BOTTOM_Y = 16.8f;
    private static final int BOX_Y = 25;
    /** Readout / place text is 1.33x the label text in the reference. */
    private static final float READOUT_TEXT_SCALE = 1.33f;
    private static final int PAIR_GAP = 2;

    static {
        for (int i = 0; i < NUMBERS.length; i++) {
            NUMBERS[i] = Integer.toString(i);
        }
        for (int i = 0; i < WIND.length; i++) {
            WIND_BOLD[i] = Component.literal(WIND[i]).withStyle(Style.EMPTY.withBold(true));
        }
    }

    // per-frame pose metrics so shapes can be snapped onto WHOLE screen pixels (see fillPx)
    private float mScaleX, mScaleY, mTransX, mTransY, guiScale = 1f, physPerUnit = 1f;
    private int lw = 1;   // line thickness in screen pixels

    private final CompassPingRenderer pingRenderer = CompassCompatLoader.loadPingRenderer();
    private final CompassTaczRenderer taczRenderer = CompassCompatLoader.loadTaczRenderer();

    // cached strings: nothing is formatted / allocated per frame unless the value actually changed
    private ResourceLocation cachedBiomeId;
    private String cachedBiomeText = "";
    private String cachedReadout = "";
    private Component cachedReadoutComponent = Component.empty();
    private String cachedPlace = "";
    private Component cachedPlaceComponent = Component.empty();
    private int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;
    private String sideX = "", sideZ = "", sideY = "";
    private ResourceLocation cachedDimension;
    private String cachedDimensionTag = "";
    private String cachedLocTag = "";

    public void render(GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        float yaw = Mth.rotLerp(partialTick, mc.player.yRotO, mc.player.getYRot());
        float heading = ((yaw + 180f) % 360f + 360f) % 360f; // 0 = north, clockwise

        float scale = (float) (double) CompassConfig.SCALE.get();
        PoseStack pose = g.pose();
        pose.pushPose();
        try {
            pose.translate(screenWidth / 2f, (float) (double) CompassConfig.OFFSET_Y.get(), 0f);
            pose.scale(scale, scale, 1f);
            final Minecraft fmc = mc;
            final float fh = heading;
            final float fp = partialTick;
            // one batched draw for the whole compass (every fill + string would otherwise be its own draw call)
            cc.xypp.modernhud.client.HudBatch.run(g, () -> drawContent(g, fmc, fp, fh));
        } finally {
            pose.popPose();
        }
    }

    private void drawContent(GuiGraphics g, Minecraft mc, float partialTick, float heading) {
        Font font = mc.font;
        updateMetrics(g, mc);
        boolean shadow = CompassConfig.TEXT_SHADOW.get();
        int half = (int) Math.round(CompassConfig.TAPE_LENGTH.get() / 2.0);
        double ppd = (half * 2) / (double) SPAN_DEGREES;
        int rgb = CompassConfig.argb(CompassConfig.COLOR.get(), 1.0) & 0xFFFFFF;
        float dim = CompassConfig.DIM_OPACITY.get().floatValue(); // 1 = reference brightness
        float k = Math.max(0.2f, dim / 0.6f);                            // the default 0.6 = the measured brightness
        int fadeLen = Math.max(1, Math.min(half, CompassConfig.FADE_LENGTH.get()));

        // ------------------------------------------------------------ tape
        int first = (int) Math.floor((heading - 50f) / 5f);
        int last = (int) Math.ceil((heading + 50f) / 5f);
        for (int i = first; i <= last; i++) {
            int ix = Math.round((float) ((i * 5f - heading) * ppd));
            int ax = Math.abs(ix);
            if (ax > half) {
                continue;
            }
            int world = ((i * 5) % 360 + 360) % 360;
            boolean letter = world % 45 == 0;
            boolean label = world % 15 == 0;
            float c = 1f - ax / (float) half;                                  // 1 at the centre, 0 at the ends
            float end = 0.35f + 0.65f * smooth(Math.min(1f, (half - ax) / (float) fadeLen));

            int aTop = alpha(0.50f * k * end);
            int aMid = alpha(0.34f * k * end);
            int aBottom = alpha(0.62f * k * end);
            if (!label) {
                // 5 / 10 degrees: THREE short vertical lines stacked in a column - top, MIDDLE, bottom
                dash(g, ix, DASH_TOP_A, DASH_TOP_B, aTop, rgb);
                dash(g, ix, DASH_MID_A, DASH_MID_B, aMid, rgb);
                dash(g, ix, DASH_BOT_A, DASH_BOT_B, aBottom, rgb);
                continue;
            }
            // 15 degrees: short dash above, the label, a long vertical dash below
            dash(g, ix, DASH_TOP_A, DASH_TOP_B, aTop, rgb);
            dash(g, ix, DASH_BOT_A, DASH_BOT_B, alpha(0.80f * k * end), rgb);
            float strength = letter ? 0.45f + 0.55f * Math.min(1f, c / 0.3f) : 0.40f + 0.60f * c;
            int tAlpha = alpha(strength);
            if (tAlpha >= 16) {
                int color = (tAlpha << 24) | rgb;
                if (letter) {
                    Component text = WIND_BOLD[world / 45];
                    HudText.text(g, font, text, ix - HudText.width(g, font, text) / 2f, LABEL_Y, color, shadow);
                } else {
                    String text = NUMBERS[world];
                    HudText.text(g, font, text, ix - HudText.width(g, font, text) / 2f, LABEL_Y, color, shadow);
                }
            }
        }

        // ------------------------------------------------------------ centre: faint line + hollow triangles
        int solid = 0xE6000000 | rgb;
        int triW = oddPx(TRI_W * physPerUnit), triH = Math.max(4, Math.round(TRI_H * physPerUnit));
        int topY = py(TRI_TOP_Y), botY = py(TRI_BOTTOM_Y);
        int cl = centerLeft(0f);                                            // left pixel of the centre line
        fillPx(g, cl, topY + triH - 1, cl + lw, botY + 1, (alpha(0.30f) << 24) | rgb);
        int triX = cl + (lw - triW) / 2;                                    // bounding box left, tip on the line
        hollowTriangle(g, DIR_DOWN, triX, topY, triW, triH, solid);
        hollowTriangle(g, DIR_UP, triX, botY, triW, triH, solid);

        // ------------------------------------------------------------ the two ends: hollow arrow + two tiny lines
        float tiny = HudText.snapScale(g, 0.5);
        int lineStep = Math.max(5, (int) Math.ceil(8 * tiny) + 1);
        if (CompassConfig.SHOW_SIDE_INFO.get()) {
            updateSideStrings(mc);
            int sideA = (alpha(0.65f) << 24) | rgb;
            int textA = (alpha(0.50f) << 24) | rgb;
            int edgeX = half + 14;
            int arrL = Math.max(4, Math.round(3.0f * physPerUnit)), arrS = oddPx(3.6f * physPerUnit);
            int arrY = py(2.5f) - arrS / 2;
            hollowTriangle(g, DIR_LEFT, px(-edgeX) - arrL, arrY, arrS, arrL, sideA);
            hollowTriangle(g, DIR_RIGHT, px(edgeX), arrY, arrS, arrL, sideA);
            tinyRight(g, font, tiny, sideX, -edgeX, 9, textA);
            tinyRight(g, font, tiny, sideZ, -edgeX, 9 + lineStep, textA);
            tinyLeft(g, font, tiny, sideY, edgeX, 9, textA);
            tinyLeft(g, font, tiny, dimensionTag(mc), edgeX, 9 + lineStep, textA);
        }

        // ------------------------------------------------------------ readout + place name: two equal boxes, centred as a pair
        boolean showReadout = CompassConfig.SHOW_READOUT.get();
        String place = locationText(mc, CompassConfig.LOCATION.get());
        boolean showPlace = !place.isEmpty();
        if (!showReadout && !showPlace) {
            return;
        }
        float ts = HudText.snapScale(g, READOUT_TEXT_SCALE);                // same text size in BOTH boxes
        int boxH = Math.max(15, Math.round(8 * ts * 1.4f));                 // same height for both
        float textTop = (boxH - 8 * ts) / 2f;

        int nearest = Math.round(heading / 45f) % 8;
        int degrees = Math.round(heading) % 360;
        Component readout = readoutComponent(WIND[nearest] + " " + degrees);
        int readoutW = Math.round(font.width(readout) * ts);
        int boxW = showReadout ? Math.max(57, readoutW + 18) : 0;
        Component placeC = showPlace ? placeComponent(place) : null;
        int placeTextW = showPlace ? Math.round(font.width(placeC) * ts) : 0;
        int panelW = showPlace ? placeTextW + 12 : 0;
        int gap = showReadout && showPlace ? PAIR_GAP : 0;
        int total = boxW + gap + panelW;
        int startX = -total / 2;                                            // the PAIR is centred, not the readout alone

        int solidText = 0xFA000000 | rgb;
        if (showReadout) {
            readoutBox(g, startX, BOX_Y, boxW, boxH, rgb);
            scaledText(g, font, readout, startX + (boxW - readoutW) / 2f, BOX_Y + textTop, ts, solidText, shadow);
        }
        if (showPlace) {
            int panelX = startX + boxW + gap;
            int pX0 = px(panelX), pX1 = px(panelX + panelW), pY0 = py(BOX_Y), pY1 = py(BOX_Y + boxH);
            fillPx(g, pX0, pY0, pX1, pY1, 0x59000000);                                   // dark panel
            fillPx(g, pX0, pY0, pX0 + lw, pY1, (alpha(0.60f) << 24) | rgb);              // thin bright edge on its left
            scaledText(g, font, placeC, panelX + 6, BOX_Y + textTop, ts, solidText, shadow);
        }
        if (CompassConfig.SHOW_SIDE_INFO.get()) {
            String tag = locTag(mc);
            tinyCentered(g, font, tiny, tag, 0, BOX_Y + boxH + 3, (alpha(0.40f) << 24) | rgb);
        }

        // ------------------------------------------------------------ enemy markers (gunfire / ping wheel)
        if (CompassConfig.SHOW_MARKERS.get()) {
            renderMarkers(g, mc, partialTick, heading, half, ppd);
        }
    }

    private void renderMarkers(GuiGraphics g, Minecraft mc, float partialTick, float heading, int half, double ppd) {
        float markerY = -3.5f; // just above the top dot row, as in the reference
        if (CompassConfig.PINGS_ENABLED.get()) {
            CompassPingStyle style = new CompassPingStyle(0.6, markerY, 1.0, 5.0, 3.0);
            pingRenderer.render(g, mc, partialTick, heading, 0, -half, half, ppd, 1f, style);
        }
        boolean on = CompassConfig.TACZ_GUNFIRE_ENABLED.get();
        CompassTaczStyle style = new CompassTaczStyle(on, 0.6, markerY,
                CompassConfig.argb(CompassConfig.TACZ_GUNFIRE_COLOR.get(), 1.0),
                CompassConfig.TACZ_GUNFIRE_MAX_SCALE_PERCENT.get(), CompassConfig.TACZ_GUNFIRE_MIN_SCALE_PERCENT.get(),
                CompassConfig.TACZ_GUNFIRE_MAX_SCALE_DISTANCE.get(), CompassConfig.TACZ_GUNFIRE_MIN_SCALE_DISTANCE.get(),
                CompassConfig.TACZ_GUNFIRE_POP_DURATION.get(), CompassConfig.TACZ_GUNFIRE_VISIBLE_DURATION.get(),
                CompassConfig.TACZ_GUNFIRE_FADE_DURATION.get(), 1.0, 5.0, 3.0);
        taczRenderer.render(g, mc, partialTick, heading, 0, -half, half, ppd, 1f, style);
    }

    // ---------------------------------------------------------------------------------------------- boxes + shapes

    /**
     * The reference readout box: a dark translucent fill, a faint 1-pixel outline all round and brighter L-shaped
     * corner brackets on top of it. {@code CORNERS} (the default) = this; any other style = that plain border.
     * Everything is snapped to whole screen pixels so the lines stay crisp and the corners complete.
     */
    private void readoutBox(GuiGraphics g, int x, int y, int w, int h, int rgb) {
        PanelBorderStyle style = CompassConfig.READOUT_BORDER.get();
        int x0 = px(x), y0 = py(y), x1 = px(x + w), y1 = py(y + h);
        fillPx(g, x0, y0, x1, y1, 0x40000000);
        if (style != PanelBorderStyle.CORNERS) {
            HudPanel.drawBorder(g, x, y, w, h, rgb, 90.0, style, 3, 0);
            return;
        }
        int outline = (alpha(0.30f) << 24) | rgb;
        fillPx(g, x0, y0, x1, y0 + lw, outline);
        fillPx(g, x0, y1 - lw, x1, y1, outline);
        fillPx(g, x0, y0 + lw, x0 + lw, y1 - lw, outline);
        fillPx(g, x1 - lw, y0 + lw, x1, y1 - lw, outline);
        int br = (alpha(0.95f) << 24) | rgb;
        int arm = Math.max(4, Math.round(2.9f * physPerUnit));
        fillPx(g, x0, y0, x0 + arm, y0 + lw, br);               // top-left
        fillPx(g, x0, y0 + lw, x0 + lw, y0 + arm, br);
        fillPx(g, x1 - arm, y0, x1, y0 + lw, br);               // top-right
        fillPx(g, x1 - lw, y0 + lw, x1, y0 + arm, br);
        fillPx(g, x0, y1 - lw, x0 + arm, y1, br);               // bottom-left
        fillPx(g, x0, y1 - arm, x0 + lw, y1 - lw, br);
        fillPx(g, x1 - arm, y1 - lw, x1, y1, br);               // bottom-right
        fillPx(g, x1 - lw, y1 - arm, x1, y1 - lw, br);
    }

    // ---------------------------------------------------------------------------------------------- pixel-snapped drawing

    private static final int DIR_DOWN = 0, DIR_UP = 1, DIR_LEFT = 2, DIR_RIGHT = 3;

    /** Reads the current pose so local units can be mapped onto the screen pixel grid. */
    private void updateMetrics(GuiGraphics g, Minecraft mc) {
        Matrix4f m = g.pose().last().pose();
        mScaleX = m.m00();
        mScaleY = m.m11();
        mTransX = m.m30();
        mTransY = m.m31();
        guiScale = (float) mc.getWindow().getGuiScale();
        physPerUnit = Math.max(0.01f, mScaleX * guiScale);
        lw = Math.max(1, Math.round(0.6f * physPerUnit));
    }

    /** Local x (units) -> screen pixel column. */
    private int px(float x) {
        return Math.round((mScaleX * x + mTransX) * guiScale);
    }

    /** Local y (units) -> screen pixel row. */
    private int py(float y) {
        return Math.round((mScaleY * y + mTransY) * guiScale);
    }

    /** Left pixel of a {@code lw}-wide vertical line centred on local x. */
    private int centerLeft(float x) {
        return Math.round((mScaleX * x + mTransX) * guiScale - lw / 2f);
    }

    private static int oddPx(float v) {
        int r = Math.max(3, Math.round(v));
        return (r & 1) == 0 ? r + 1 : r;
    }

    /** Fills a rectangle given in SCREEN pixels (immune to fractional scales: every shape keeps its pixels). */
    private void fillPx(GuiGraphics g, int x0, int y0, int x1, int y1, int argb) {
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        PoseStack ps = g.pose();
        ps.pushPose();
        Matrix4f pm = ps.last().pose();
        float z = pm.m32();
        pm.identity();
        pm.translate(0f, 0f, z);
        pm.scale(1f / guiScale, 1f / guiScale, 1f);
        g.fill(x0, y0, x1, y1, argb);
        ps.popPose();
    }

    /** One short vertical line of a tick column, centred on local x, from local y a to b (at least 2 pixels tall). */
    private void dash(GuiGraphics g, float x, float a, float b, int alpha, int rgb) {
        if (alpha < 16) {
            return;
        }
        int y0 = py(a);
        int h = Math.max(2, Math.round((b - a) * physPerUnit));
        int x0 = centerLeft(x);
        fillPx(g, x0, y0, x0 + lw, y0 + h, (alpha << 24) | rgb);
    }

    /**
     * A hollow (outline-only) triangle with edges that always connect, drawn pixel run by pixel run.
     * {@code (x0, y0)} is the top-left screen pixel of its bounding box; for DOWN / UP {@code size} is the base width
     * and {@code len} the height, for LEFT / RIGHT {@code size} is the base height and {@code len} the width.
     * DOWN = tip at the bottom, UP = tip at the top, LEFT = tip on the left, RIGHT = tip on the right.
     */
    private void hollowTriangle(GuiGraphics g, int dir, int x0, int y0, int size, int len, int argb) {
        boolean vertical = dir == DIR_DOWN || dir == DIR_UP;
        float c = (size - 1) / 2f;                       // centre along the base axis, in pixel indices
        int[] lo = new int[len];
        int[] hi = new int[len];                         // inclusive outer-edge pixel indices per step
        for (int i = 0; i < len; i++) {
            float t = len == 1 ? 1f : i / (float) (len - 1);   // 0 at the base, 1 at the tip
            float half = c * (1f - t);
            lo[i] = Math.round(c - half);
            hi[i] = Math.round(c + half);
        }
        for (int i = 0; i < len; i++) {
            int a = lo[i], b = hi[i];
            int step = (dir == DIR_DOWN || dir == DIR_RIGHT) ? i : len - 1 - i;
            if (i < lw || i == len - 1 || b - a < 2 * lw) {   // base line, or the tip: one solid run
                plotRun(g, vertical, x0, y0, step, a, b + 1, argb);
                continue;
            }
            // each edge extends to where the next row's edge starts, so the diagonals never break
            int leftEnd = Math.max(a + lw, lo[i + 1]);
            int rightStart = Math.min(b - lw + 1, hi[i + 1] + 1);
            plotRun(g, vertical, x0, y0, step, a, Math.min(leftEnd, b + 1), argb);
            plotRun(g, vertical, x0, y0, step, Math.max(rightStart, a), b + 1, argb);
        }
    }

    /** One run [from, to) across the base axis at {@code step} along the length axis. */
    private void plotRun(GuiGraphics g, boolean vertical, int x0, int y0, int step, int from, int to, int argb) {
        if (vertical) {
            fillPx(g, x0 + from, y0 + step, x0 + to, y0 + step + 1, argb);
        } else {
            fillPx(g, x0 + step, y0 + from, x0 + step + 1, y0 + to, argb);
        }
    }

    // ---------------------------------------------------------------------------------------------- text helpers

    private static void scaledText(GuiGraphics g, Font font, Component c, float x, float y, float scale, int argb,
                                   boolean shadow) {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x, y, 0f);
        ps.scale(scale, scale, 1f);
        HudText.text(g, font, c, 0, 0, argb, shadow);
        ps.popPose();
    }

    private static void tinyRight(GuiGraphics g, Font font, float scale, String s, int rightX, int y, int argb) {
        if (!s.isEmpty()) {
            tiny(g, font, scale, s, rightX - font.width(s) * scale, y, argb);
        }
    }

    private static void tinyLeft(GuiGraphics g, Font font, float scale, String s, int leftX, int y, int argb) {
        if (!s.isEmpty()) {
            tiny(g, font, scale, s, leftX, y, argb);
        }
    }

    private static void tinyCentered(GuiGraphics g, Font font, float scale, String s, int centerX, int y, int argb) {
        if (!s.isEmpty()) {
            tiny(g, font, scale, s, centerX - font.width(s) * scale / 2f, y, argb);
        }
    }

    private static void tiny(GuiGraphics g, Font font, float scale, String s, float x, float y, int argb) {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x, y, 0f);
        ps.scale(scale, scale, 1f);
        HudText.text(g, font, s, 0, 0, argb, false);
        ps.popPose();
    }

    // ---------------------------------------------------------------------------------------------- helpers

    private static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    private static int alpha(float v) {
        return Math.max(0, Math.min(255, Math.round(v * 255f)));
    }

    private Component readoutComponent(String s) {
        if (!s.equals(cachedReadout)) {
            cachedReadout = s;
            cachedReadoutComponent = Component.literal(s).withStyle(Style.EMPTY.withBold(true));
        }
        return cachedReadoutComponent;
    }

    private Component placeComponent(String s) {
        if (!s.equals(cachedPlace)) {
            cachedPlace = s;
            cachedPlaceComponent = Component.literal(s);
        }
        return cachedPlaceComponent;
    }

    private void updateSideStrings(Minecraft mc) {
        BlockPos pos = mc.player.blockPosition();
        if (pos.getX() != lastX) {
            lastX = pos.getX();
            sideX = "X " + lastX;
        }
        if (pos.getZ() != lastZ) {
            lastZ = pos.getZ();
            sideZ = "Z " + lastZ;
        }
        if (pos.getY() != lastY) {
            lastY = pos.getY();
            sideY = "Y " + lastY;
        }
    }

    private String dimensionTag(Minecraft mc) {
        if (mc.level == null) {
            return "";
        }
        ResourceLocation id = mc.level.dimension().location();
        if (!id.equals(cachedDimension)) {
            cachedDimension = id;
            String up = id.getPath().toUpperCase(Locale.ROOT);
            cachedDimensionTag = "DIM." + up;
            cachedLocTag = "LOC." + up;
        }
        return cachedDimensionTag;
    }

    private String locTag(Minecraft mc) {
        dimensionTag(mc);
        return cachedLocTag;
    }

    private String locationText(Minecraft mc, LocationDisplayMode mode) {
        if (mode == LocationDisplayMode.OFF || mc.player == null || mc.level == null) {
            return "";
        }
        BlockPos pos = mc.player.blockPosition();
        if (mode == LocationDisplayMode.COORDINATES) {
            return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        }
        Holder<Biome> holder = mc.level.getBiome(pos);
        ResourceKey<Biome> key = holder.unwrapKey().orElse(null);
        ResourceLocation id = key == null ? null : key.location();
        if (id == null) {
            return "Unknown";
        }
        if (!id.equals(cachedBiomeId)) {
            cachedBiomeId = id;
            String translation = "biome." + id.getNamespace() + "." + id.getPath();
            cachedBiomeText = I18n.exists(translation) ? I18n.get(translation) : id.getPath();
        }
        return cachedBiomeText;
    }
}

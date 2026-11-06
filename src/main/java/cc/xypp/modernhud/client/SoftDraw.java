package cc.xypp.modernhud.client;

import com.mojang.blaze3d.platform.GlStateManager;
import cc.xypp.modernhud.config.HudConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/**
 * Smooth 2D drawing for HUD elements: float coordinates and PER-VERTEX colours, so gradients and soft glows are
 * interpolated by the GPU instead of being faked with stacked integer-aligned rectangles (which is what made the
 * old glows look blocky/pixelated - every step was a whole GUI pixel, i.e. 2-4 screen pixels at common GUI scales).
 *
 * <p>Everything draws in the CURRENT pose-stack space, immediately (not through the batched GuiGraphics buffer), in
 * the same order calls are made, and restores the default blend function afterwards.
 */
public final class SoftDraw {

    private SoftDraw() {
    }

    /** Immediate-mode draws would otherwise land BEFORE fills still waiting in an open batch. */
    private static void sync(GuiGraphics g) {
        if (HudBatch.active()) {
            g.flush();
        }
    }

    /** Horizontal gradient: {@code argbLeft} at x0 to {@code argbRight} at x1. */
    public static void gradientH(GuiGraphics g, float x0, float y0, float x1, float y1, int argbLeft, int argbRight) {
        sync(g);
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        BufferBuilder bb = begin(false);
        Matrix4f m = g.pose().last().pose();
        vertex(bb, m, x0, y0, argbLeft);
        vertex(bb, m, x0, y1, argbLeft);
        vertex(bb, m, x1, y1, argbRight);
        vertex(bb, m, x1, y0, argbRight);
        end(bb, false);
    }

    /** Vertical gradient: {@code argbTop} at y0 to {@code argbBottom} at y1. */
    public static void gradientV(GuiGraphics g, float x0, float y0, float x1, float y1, int argbTop, int argbBottom) {
        sync(g);
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        BufferBuilder bb = begin(false);
        Matrix4f m = g.pose().last().pose();
        vertex(bb, m, x0, y0, argbTop);
        vertex(bb, m, x0, y1, argbBottom);
        vertex(bb, m, x1, y1, argbBottom);
        vertex(bb, m, x1, y0, argbTop);
        end(bb, false);
    }

    /** Flat rectangle with float coordinates (sub-pixel edges - e.g. a progress fill that grows smoothly). */
    public static void rect(GuiGraphics g, float x0, float y0, float x1, float y1, int argb) {
        gradientH(g, x0, y0, x1, y1, argb, argb);
    }

    /**
     * A soft, feathered rectangle: solid {@code alphaCore} over the rect itself, fading smoothly to 0 over
     * {@code spread} px outside it (three rings approximating a smooth falloff, interpolated per vertex, so there
     * are no visible steps). {@code additive} = light (glow); otherwise ordinary alpha (shadow).
     */
    public static void soft(GuiGraphics g, float x, float y, float w, float h, int rgb, float alphaCore,
                            float spread, boolean additive) {
        soft(g, x, y, w, h, rgb, alphaCore, alphaCore, spread, additive);
    }

    /** Rings (radial steps) and arc segments per corner of a soft rect: enough that the falloff reads as a smooth
     *  rounded halo rather than a few visible rectangular steps. */

    /**
     * Same, with the rect's INTERIOR and its RIM given separately: {@code alphaCore} over the rect itself,
     * {@code alphaEdge} where the falloff starts just outside it. Edge &gt; core gives a "rim light" glow that
     * hugs the box instead of washing its contents.
     *
     * <p>The halo is a rounded shape: straight strips along the four edges and fan-shaped arcs ({@link #ARC_SEGS}
     * segments) around the four corners, over {@link #RINGS} rings whose alpha follows a smoothstep falloff - so
     * there are no square corners and no visible bands, only a soft round-cornered fade.
     */
    public static void soft(GuiGraphics g, float x, float y, float w, float h, int rgb, float alphaCore,
                            float alphaEdge, float spread, boolean additive) {
        sync(g);
        if ((alphaCore <= 0.002f && alphaEdge <= 0.002f) || w <= 0 || h <= 0) {
            return;
        }
        float a0 = Math.min(1f, alphaCore);
        float ae = Math.min(1f, alphaEdge);
        BufferBuilder bb = begin(additive);
        Matrix4f m = g.pose().last().pose();
        int r = (rgb >> 16) & 0xFF;
        int gr = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        // core
        quad(bb, m, x, y, a0, x, y + h, a0, x + w, y + h, a0, x + w, y, a0, r, gr, b);
        if (spread > 0.01f && ae > 0.002f) {
            // a small glow needs far fewer rings / corner segments to look smooth: a 5 px fade on HIGH was ~290 quads
            final int RINGS = Math.min(HudConfig.glowQuality.rings(), Math.max(2, (int) Math.ceil(spread / 2f)));
            final int ARC_SEGS = Math.min(HudConfig.glowQuality.segs(), Math.max(2, (int) Math.ceil(spread / 1.5f)));
            float[] rad = RAD;
            float[] alp = ALP;
            float[] curve = fallCurve(RINGS);
            for (int i = 0; i <= RINGS; i++) {
                rad[i] = spread * (i / (float) RINGS);
                alp[i] = ae * curve[i];
            }
            alp[RINGS] = 0f;
            for (int i = 0; i < RINGS; i++) {
                float r0 = rad[i], r1 = rad[i + 1], p0 = alp[i], p1 = alp[i + 1];
                // edge strips: top, bottom, left, right
                quad(bb, m, x, y - r1, p1, x, y - r0, p0, x + w, y - r0, p0, x + w, y - r1, p1, r, gr, b);
                quad(bb, m, x, y + h + r0, p0, x, y + h + r1, p1, x + w, y + h + r1, p1, x + w, y + h + r0, p0, r, gr, b);
                quad(bb, m, x - r1, y, p1, x - r1, y + h, p1, x - r0, y + h, p0, x - r0, y, p0, r, gr, b);
                quad(bb, m, x + w + r0, y, p0, x + w + r0, y + h, p0, x + w + r1, y + h, p1, x + w + r1, y, p1, r, gr, b);
                // rounded corners: TL, TR, BR, BL (angles in screen space, y down)
                arc(bb, m, x, y, r0, p0, r1, p1, (float) Math.PI, ARC_SEGS, r, gr, b);
                arc(bb, m, x + w, y, r0, p0, r1, p1, (float) (Math.PI * 1.5), ARC_SEGS, r, gr, b);
                arc(bb, m, x + w, y + h, r0, p0, r1, p1, 0f, ARC_SEGS, r, gr, b);
                arc(bb, m, x, y + h, r0, p0, r1, p1, (float) (Math.PI * 0.5), ARC_SEGS, r, gr, b);
            }
        }
        end(bb, additive);
    }

    /**
     * A round soft glow ("bloom") centred on a point: brightest in the middle, smoothstep falloff to nothing at
     * {@code radius}. For icons, dots and text; the rectangular {@link #soft} is for boxes.
     */
    public static void radial(GuiGraphics g, float cx, float cy, float radius, int rgb, float alphaCenter,
                              boolean additive) {
        radial(g, cx, cy, radius, rgb, alphaCenter, additive, 0b1111);
    }

    /**
     * Same, but only the quarters in {@code quarterMask}: bit 0 = towards +x/+y (down-right, the inside of a TOP-LEFT
     * corner), bit 1 = down-left (top-right corner), bit 2 = up-left (bottom-right corner), bit 3 = up-right
     * (bottom-left corner). Lets a glow centred ON a box corner light only the inside of the box.
     */
    public static void radial(GuiGraphics g, float cx, float cy, float radius, int rgb, float alphaCenter,
                              boolean additive, int quarterMask) {
        if (alphaCenter <= 0.002f || radius <= 0.5f || (quarterMask & 0b1111) == 0) {
            return;
        }
        sync(g);
        BufferBuilder bb = begin(additive);
        appendRadial(bb, g.pose().last().pose(), cx, cy, radius, rgb, alphaCenter, quarterMask);
        end(bb, additive);
    }

    /** Writes the quads of one radial glow into {@code bb} (no state change, no draw call). */
    private static void appendRadial(BufferBuilder bb, Matrix4f m, float cx, float cy, float radius, int rgb,
                                     float alphaCenter, int quarterMask) {
        // fewer rings and arc segments for a small glow: it looks the same and costs a fraction of the quads
        int rings = Math.min(HudConfig.glowQuality.rings(), Math.max(3, (int) Math.ceil(radius / 5f)));
        int segs = Math.min(HudConfig.glowQuality.segs(), Math.max(3, (int) Math.ceil(radius / 5f))) * 4;
        int r = (rgb >> 16) & 0xFF;
        int gr = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        float[] ct = QUARTER_COS[Math.max(1, Math.min(MAX_SEGS, segs / 4))];
        float[] st = QUARTER_SIN[Math.max(1, Math.min(MAX_SEGS, segs / 4))];
        int qs = ct.length - 1;
        float[] curve = fallCurve(rings);
        for (int i = 0; i < rings; i++) {
            float r0 = radius * (i / (float) rings);
            float r1 = radius * ((i + 1) / (float) rings);
            float a0 = alphaCenter * curve[i];
            float a1 = i + 1 == rings ? 0f : alphaCenter * curve[i + 1];
            for (int quarter = 0; quarter < 4; quarter++) {
                if ((quarterMask & (1 << quarter)) == 0) {
                    continue;
                }
                for (int k = 0; k < qs; k++) {
                    float c0 = ct[k], s0 = st[k], c1 = ct[k + 1], s1 = st[k + 1];
                    float x0, y0, x1, y1;
                    switch (quarter) {
                        case 1 -> { x0 = -s0; y0 = c0; x1 = -s1; y1 = c1; }
                        case 2 -> { x0 = -c0; y0 = -s0; x1 = -c1; y1 = -s1; }
                        case 3 -> { x0 = s0; y0 = -c0; x1 = s1; y1 = -c1; }
                        default -> { x0 = c0; y0 = s0; x1 = c1; y1 = s1; }
                    }
                    quad(bb, m, cx + x0 * r0, cy + y0 * r0, a0, cx + x0 * r1, cy + y0 * r1, a1,
                            cx + x1 * r1, cy + y1 * r1, a1, cx + x1 * r0, cy + y1 * r0, a0, r, gr, b);
                }
            }
        }
    }

    /**
     * Several radial glows of the same blend mode, drawn with ONE draw call. They share the pose that is current when
     * the batch is opened, so open it where the glows are laid out (for example the four corners of one box). Each
     * separate {@link #radial} costs a flush of the open GUI batch, a shader / blend set-up and its own draw call; with a
     * shader pack's GUI hooks every one of those is more expensive.
     */
    public static final class RadialBatch {
        private final BufferBuilder bb;
        private final Matrix4f m;
        private final boolean additive;

        private RadialBatch(BufferBuilder bb, Matrix4f m, boolean additive) {
            this.bb = bb;
            this.m = m;
            this.additive = additive;
        }
    }

    public static RadialBatch radialBatch(GuiGraphics g, boolean additive) {
        sync(g);
        BufferBuilder bb = begin(additive);
        return new RadialBatch(bb, new Matrix4f(g.pose().last().pose()), additive);
    }

    /** Adds one glow to the batch; same meaning of the arguments as {@link #radial}. */
    public static void add(RadialBatch b, float cx, float cy, float radius, int rgb, float alphaCenter, int quarterMask) {
        if (alphaCenter <= 0.002f || radius <= 0.5f || (quarterMask & 0b1111) == 0) {
            return;
        }
        appendRadial(b.bb, b.m, cx, cy, radius, rgb, alphaCenter, quarterMask);
    }

    /** Draws everything added so far (nothing is drawn, and nothing breaks, if nothing was added). */
    public static void finish(RadialBatch b) {
        end(b.bb, b.additive);
    }

    /**
     * A soft ring segment ("arc") around (cx, cy): angles in radians measured CLOCKWISE from straight up, radii from
     * {@code rInner} to {@code rOuter} solid, with a soft glow of {@code glow} px on both sides and ends that taper
     * to nothing. Used by the damage-direction indicator.
     */
    public static void arc(GuiGraphics g, float cx, float cy, float rInner, float rOuter, float a0, float a1,
                           float glow, int rgb, float alpha, boolean additive) {
        if (alpha <= 0.002f || a1 <= a0) {
            return;
        }
        int segs = Math.max(8, HudConfig.glowQuality.segs() * 3);
        float[] rad = {rInner - glow, rInner, (rInner + rOuter) * 0.5f, rOuter, rOuter + glow};
        float[] alp = {0f, 0.85f, 1f, 0.85f, 0f};
        if (glow <= 0.01f) {
            rad = new float[]{rInner, (rInner + rOuter) * 0.5f, rOuter};
            alp = new float[]{0.9f, 1f, 0.9f};
        }
        BufferBuilder bb = begin(additive);
        Matrix4f m = g.pose().last().pose();
        int r = (rgb >> 16) & 0xFF;
        int gr = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        for (int k = 0; k < segs; k++) {
            float t0 = k / (float) segs;
            float t1 = (k + 1) / (float) segs;
            float f0 = taper(t0) * alpha;
            float f1 = taper(t1) * alpha;
            float an0 = a0 + (a1 - a0) * t0;
            float an1 = a0 + (a1 - a0) * t1;
            float s0 = (float) Math.sin(an0), c0 = (float) Math.cos(an0);
            float s1 = (float) Math.sin(an1), c1 = (float) Math.cos(an1);
            for (int j = 0; j + 1 < rad.length; j++) {
                float ra = rad[j], rb = rad[j + 1];
                float pa = alp[j], pb = alp[j + 1];
                quad(bb, m,
                        cx + s0 * ra, cy - c0 * ra, pa * f0,
                        cx + s0 * rb, cy - c0 * rb, pb * f0,
                        cx + s1 * rb, cy - c1 * rb, pb * f1,
                        cx + s1 * ra, cy - c1 * ra, pa * f1, r, gr, b);
            }
        }
        end(bb, additive);
    }

    /** Ends of the arc fade out smoothly: 0 at both tips, 1 across the middle. */
    private static float taper(float t) {
        float e = Math.min(t, 1f - t) / 0.28f;
        if (e >= 1f) {
            return 1f;
        }
        return e * e * (3f - 2f * e);
    }

    private static float falloff(float t) {
        float s = t * t * (3f - 2f * t);
        return (float) Math.pow(1f - s, 1.6);
    }

    // ------------------------------------------------------------------ internals

    /** One quarter-circle band between radii r0 and r1 around (cx, cy), starting at angle {@code start}
     *  (a multiple of a quarter turn); the sin/cos come from a cached table - no trig per vertex. */
    private static void arc(BufferBuilder bb, Matrix4f m, float cx, float cy, float r0, float a0, float r1, float a1,
                            float start, int segs, int r, int g, int b) {
        float[] ct = QUARTER_COS[segs];
        float[] st = QUARTER_SIN[segs];
        int q = Math.round(start / (float) (Math.PI * 0.5)) & 3;
        for (int k = 0; k < segs; k++) {
            float c0 = ct[k], s0 = st[k], c1 = ct[k + 1], s1 = st[k + 1];
            float x0, y0, x1, y1;
            switch (q) {
                case 1 -> { x0 = -s0; y0 = c0; x1 = -s1; y1 = c1; }
                case 2 -> { x0 = -c0; y0 = -s0; x1 = -c1; y1 = -s1; }
                case 3 -> { x0 = s0; y0 = -c0; x1 = s1; y1 = -c1; }
                default -> { x0 = c0; y0 = s0; x1 = c1; y1 = s1; }
            }
            quad(bb, m,
                    cx + x0 * r0, cy + y0 * r0, a0,
                    cx + x0 * r1, cy + y0 * r1, a1,
                    cx + x1 * r1, cy + y1 * r1, a1,
                    cx + x1 * r0, cy + y1 * r0, a0, r, g, b);
        }
    }

    // ---- cached trig / falloff tables (the glows are drawn many times a frame: no sin/cos/pow per vertex)
    private static final int MAX_SEGS = 64;
    private static final float[][] QUARTER_COS = new float[MAX_SEGS + 1][];
    private static final float[][] QUARTER_SIN = new float[MAX_SEGS + 1][];
    private static final float[][] FALL = new float[MAX_SEGS + 1][];
    private static final float[] RAD = new float[MAX_SEGS + 1];
    private static final float[] ALP = new float[MAX_SEGS + 1];

    static {
        for (int segs = 1; segs <= MAX_SEGS; segs++) {
            float[] c = new float[segs + 1];
            float[] sn = new float[segs + 1];
            for (int k = 0; k <= segs; k++) {
                double an = Math.PI * 0.5 * k / segs;
                c[k] = (float) Math.cos(an);
                sn[k] = (float) Math.sin(an);
            }
            QUARTER_COS[segs] = c;
            QUARTER_SIN[segs] = sn;
            float[] f = new float[segs + 1];
            for (int i = 0; i <= segs; i++) {
                f[i] = falloff(i / (float) segs);
            }
            FALL[segs] = f;
        }
    }

    private static float[] fallCurve(int rings) {
        return FALL[Math.max(1, Math.min(MAX_SEGS, rings))];
    }

    private static void quad(BufferBuilder bb, Matrix4f m,
                             float x1, float y1, float a1, float x2, float y2, float a2,
                             float x3, float y3, float a3, float x4, float y4, float a4,
                             int r, int g, int b) {
        bb.vertex(m, x1, y1, 0f).color(r, g, b, alpha(a1)).endVertex();
        bb.vertex(m, x2, y2, 0f).color(r, g, b, alpha(a2)).endVertex();
        bb.vertex(m, x3, y3, 0f).color(r, g, b, alpha(a3)).endVertex();
        bb.vertex(m, x4, y4, 0f).color(r, g, b, alpha(a4)).endVertex();
    }

    private static int alpha(float a) {
        return Math.max(0, Math.min(255, Math.round(a * 255f)));
    }

    private static void vertex(BufferBuilder bb, Matrix4f m, float x, float y, int argb) {
        bb.vertex(m, x, y, 0f).color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF).endVertex();
    }

    private static BufferBuilder begin(boolean additive) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.disableCull(); // strips/corners below are not all wound the same way; the GUI never needs culling
        if (additive) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        return bb;
    }

    private static void end(BufferBuilder bb, boolean additive) {
        BufferUploader.drawWithShader(bb.end());
        if (additive) {
            RenderSystem.defaultBlendFunc();
        }
    }
}

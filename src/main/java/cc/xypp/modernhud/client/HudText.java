package cc.xypp.modernhud.client;

import cc.xypp.modernhud.config.HudConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

/**
 * Pixel-perfect scaling for the pixel font (and the HUD's pixel-art in general).
 *
 * <p>Why scaled text looks wrong: Minecraft's font is a bitmap, drawn with nearest-neighbour sampling. At a
 * scale where one font pixel covers e.g. 1.8 screen pixels, some font pixels become 1 px wide and others 2 px
 * wide - uneven strokes and "broken" letters - and below 1 screen pixel per font pixel columns/rows are simply
 * dropped. The only artefact-free scales are WHOLE numbers of screen pixels per font pixel (1, 2, 3 ...).
 *
 * <p>So every scale that ends up on text is snapped, at the place it is chosen, so that
 * {@code scale * (screen pixels per unit at the current pose)} is a whole number (never below 1). Layout code
 * measures with the SNAPPED scale, so boxes and text always agree. Turn {@code pixelPerfectText} off to get the
 * exact fractional scales back (smoother sizing, uneven pixels).
 */
public final class HudText {

    private HudText() {
    }

    /** Screen pixels per local unit at the current pose (the pose's x scale times Minecraft's GUI scale). */
    public static float physPerUnit(GuiGraphics g) {
        Matrix4f m = g.pose().last().pose();
        float sx = (float) Math.sqrt(m.m00() * m.m00() + m.m01() * m.m01() + m.m02() * m.m02());
        return sx * (float) Minecraft.getInstance().getWindow().getGuiScale();
    }

    /** Snaps {@code requested} (a scale to apply from the CURRENT pose on) to whole screen pixels per font pixel. */
    public static float snapScale(GuiGraphics g, double requested) {
        return snapScale(g, requested, 1f);
    }

    /** Same, for a scale that will be applied on top of a parent scale that is not on the pose yet. */
    public static float snapScale(GuiGraphics g, double requested, float parentScale) {
        float r = (float) requested;
        if (!HudConfig.pixelPerfectText || r <= 0f) {
            return r;
        }
        float base = physPerUnit(g) * parentScale;
        if (base <= 0.01f) {
            return r;
        }
        float p = Math.max(1f, Math.round(r * base));
        return p / base;
    }

    /**
     * Element-level (whole widget) scale: snapped to whole screen pixels ONLY when {@code pixelPerfectElements} is on.
     * Off by default so the scale options always change the size smoothly; text sizes inside are still snapped.
     */
    public static float snapElement(GuiGraphics g, double requested) {
        return HudConfig.pixelPerfectElements ? snapScale(g, requested) : (float) requested;
    }

    /**
     * Snaps a pair of scales (big number + smaller companion) so each is pixel-perfect AND the smaller one stays
     * visibly smaller (when snapping would make them equal, the small one drops one whole pixel step).
     * @return {big, small}
     */
    public static float[] snapPair(GuiGraphics g, double big, double small) {
        float b = snapScale(g, big);
        float s = snapScale(g, small);
        if (HudConfig.pixelPerfectText && small < big) {
            float base = physPerUnit(g);
            if (base > 0.01f && s >= b - 0.0001f && Math.round(b * base) > 1) {
                s = (Math.round(b * base) - 1) / base;
            }
        }
        return new float[]{b, s};
    }

    /** Draws text with its origin snapped onto the screen pixel grid (no half-pixel blur / shimmer). */
    public static void drawSnapped(GuiGraphics g, net.minecraft.client.gui.Font font, String text, float x, float y,
                                   float scale, int color, boolean shadow) {
        PoseStack pose = g.pose();
        pose.pushPose();
        float tx = x;
        float ty = y;
        if (HudConfig.pixelPerfectText) {
            Matrix4f m = g.pose().last().pose();
            float gs = (float) Minecraft.getInstance().getWindow().getGuiScale();
            float sx = (float) Math.sqrt(m.m00() * m.m00() + m.m01() * m.m01() + m.m02() * m.m02()) * gs;
            if (sx > 0.01f) {
                float dx = (m.m00() * x + m.m10() * y + m.m30()) * gs;
                float dy = (m.m01() * x + m.m11() * y + m.m31()) * gs;
                tx += (Math.round(dx) - dx) / sx;
                ty += (Math.round(dy) - dy) / sx;
            }
        }
        pose.translate(tx, ty, 0f);
        pose.scale(scale, scale, 1f);
        g.drawString(font, text, 0, 0, color, shadow);
        pose.popPose();
    }

    // ------------------------------------------------------------------------------------------ crisp text

    /**
     * How much wider (or narrower) {@link #text} draws than the layout assumes. Glyphs are drawn at a whole number of
     * screen pixels per font pixel, so at {@code physPerUnit} = 2.5 they are drawn at 3 (1.2 times the layout width).
     * Multiply {@code font.width(...)} by this to get the width that is really drawn.
     */
    public static float layoutRatio(float physPerUnit) {
        if (!HudConfig.pixelPerfectText || physPerUnit <= 0.01f) {
            return 1f;
        }
        return Math.max(1f, Math.round(physPerUnit)) / physPerUnit;
    }

    /** Width in layout units that {@link #text} will really draw {@code s} with, under the CURRENT pose. */
    public static float width(GuiGraphics g, net.minecraft.client.gui.Font font, String s) {
        return font.width(s) * layoutRatio(currentPhys(g));
    }

    public static float width(GuiGraphics g, net.minecraft.client.gui.Font font, net.minecraft.network.chat.Component c) {
        return font.width(c) * layoutRatio(currentPhys(g));
    }

    private static float currentPhys(GuiGraphics g) {
        Matrix4f m = g.pose().last().pose();
        float sx = (float) Math.sqrt(m.m00() * m.m00() + m.m01() * m.m01() + m.m02() * m.m02());
        return sx * (float) Minecraft.getInstance().getWindow().getGuiScale();
    }

    /**
     * Draws text so the GLYPHS are always pixel-perfect while the LAYOUT stays smooth: the text's origin is mapped
     * through the current pose (so it sits exactly where a fractional widget scale puts it, snapped to a screen pixel),
     * but the glyphs themselves are drawn at a WHOLE number of screen pixels per font pixel (never below 1). That is
     * what removes the uneven / jagged strokes of a bitmap font at fractional or small scales, without having to
     * force the whole widget onto coarse scale steps. {@code pixelPerfectText} off = plain {@code drawString}.
     */
    public static void text(GuiGraphics g, net.minecraft.client.gui.Font font, String s, float x, float y, int color,
                            boolean shadow) {
        draw(g, font, null, s, x, y, color, shadow);
    }

    public static void text(GuiGraphics g, net.minecraft.client.gui.Font font, net.minecraft.network.chat.Component c,
                            float x, float y, int color, boolean shadow) {
        draw(g, font, c, null, x, y, color, shadow);
    }

    private static void draw(GuiGraphics g, net.minecraft.client.gui.Font font, net.minecraft.network.chat.Component c,
                             String s, float x, float y, int color, boolean shadow) {
        PoseStack ps = g.pose();
        Matrix4f m = ps.last().pose();
        float gs = (float) Minecraft.getInstance().getWindow().getGuiScale();
        float sx = (float) Math.sqrt(m.m00() * m.m00() + m.m01() * m.m01() + m.m02() * m.m02());
        float phys = sx * gs;
        if (!HudConfig.pixelPerfectText || phys <= 0.01f) {
            plain(g, font, c, s, x, y, color, shadow);
            return;
        }
        float p = Math.max(1f, Math.round(phys));
        float ox = m.m00() * x + m.m10() * y + m.m30();
        float oy = m.m01() * x + m.m11() * y + m.m31();
        float dx = Math.round(ox * gs) / gs;
        float dy = Math.round(oy * gs) / gs;
        float z = m.m32();
        ps.pushPose();
        Matrix4f pm = ps.last().pose();
        pm.identity();
        pm.translate(dx, dy, z);
        pm.scale(p / gs, p / gs, 1f);
        plain(g, font, c, s, 0f, 0f, color, shadow);
        ps.popPose();
    }

    private static void plain(GuiGraphics g, net.minecraft.client.gui.Font font, net.minecraft.network.chat.Component c,
                              String s, float x, float y, int color, boolean shadow) {
        if (c != null) {
            g.drawString(font, c, Math.round(x), Math.round(y), color, shadow);
        } else {
            g.drawString(font, s, x, y, color, shadow);
        }
    }
}

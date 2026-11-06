package cc.xypp.modernhud.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

/**
 * Crisp, texture-free pixel shapes for the HUD. Everything is mapped through the CURRENT pose onto the real screen
 * pixel grid and filled in WHOLE screen pixels, so a shape can never lose a row / column or get uneven strokes at a
 * fractional widget scale or any GUI scale (the failure of down-sampling big textures with nearest-neighbour).
 */
public final class PixelDraw {

    private PixelDraw() {
    }

    /** Screen pixels per local unit at the current pose. */
    public static float physPerUnit(GuiGraphics g) {
        return Math.max(0.01f, g.pose().last().pose().m00() * gui());
    }

    private static float gui() {
        return (float) Minecraft.getInstance().getWindow().getGuiScale();
    }

    /** > 0 while the pose is already set to screen pixels (inside {@link #scoped}), so fills need no pose change. */
    private static int pxDepth;

    /** Fills a rectangle given in SCREEN pixels, regardless of the current pose. */
    public static void fillPx(GuiGraphics g, int x0, int y0, int x1, int y1, int argb) {
        if (x1 <= x0 || y1 <= y0 || (argb >>> 24) == 0) {
            return;
        }
        if (pxDepth > 0) {
            g.fill(x0, y0, x1, y1, argb);
            return;
        }
        PoseStack ps = g.pose();
        ps.pushPose();
        setPixelPose(ps);
        g.fill(x0, y0, x1, y1, argb);
        ps.popPose();
    }

    private static void setPixelPose(PoseStack ps) {
        float gs = gui();
        Matrix4f pm = ps.last().pose();
        float z = pm.m32();
        pm.identity();
        pm.translate(0f, 0f, z);
        pm.scale(1f / gs, 1f / gs, 1f);
    }

    /**
     * Runs {@code body} (many {@link #fillPx} calls) as one draw batch with the pose switched to screen pixels once,
     * instead of a pose push, matrix rebuild and GPU flush for every rectangle.
     */
    private static void scoped(GuiGraphics g, Runnable body) {
        HudBatch.run(g, () -> {
            PoseStack ps = g.pose();
            ps.pushPose();
            setPixelPose(ps);
            pxDepth++;
            try {
                body.run();
            } finally {
                pxDepth--;
                ps.popPose();
            }
        });
    }

    /** Fills a rectangle given in local units, snapped to whole screen pixels (at least 1 px each way). */
    public static void fillUnits(GuiGraphics g, float x, float y, float w, float h, int argb) {
        Matrix4f m = g.pose().last().pose();
        float gs = gui();
        int x0 = Math.round((m.m00() * x + m.m30()) * gs);
        int y0 = Math.round((m.m11() * y + m.m31()) * gs);
        int x1 = Math.max(x0 + 1, Math.round((m.m00() * (x + w) + m.m30()) * gs));
        int y1 = Math.max(y0 + 1, Math.round((m.m11() * (y + h) + m.m31()) * gs));
        fillPx(g, x0, y0, x1, y1, argb);
    }

    public static boolean isFireIcon(String mode) {
        return "AUTO".equals(mode) || "SEMI".equals(mode) || "BURST".equals(mode);
    }

    /**
     * Ammo glyphs built from little pixel BULLETS, like the reference HUD, in a {@code size} x {@code size} box at
     * local (x, y): AUTO = three bullets stacked, BURST = three, SEMI = one (fire-mode cells; a bullet is a short
     * rounded cap, a gap, then a body ending in a pointed tip), RESERVE = four double-pointed rounds stacked (the icon
     * next to the reserve count). Everything is whole screen pixels with integer thickness / gaps, so the bullets stay
     * exact at any scale.
     */
    public static void fireIcon(GuiGraphics g, String mode, float x, float y, float size, int argb) {
        fireIcon(g, mode, x, y, size, size, argb);
    }

    /** Same, in a {@code w} x {@code hgt} box (local units): bullets are long, so a wide box suits them better than a square. */
    public static void fireIcon(GuiGraphics g, String mode, float x, float y, float w, float hgt, int argb) {
        if ((argb >>> 24) == 0) {
            return;
        }
        Matrix4f m = g.pose().last().pose();
        float gs = gui();
        final int nw = Math.max(5, Math.round(m.m00() * w * gs));       // box width in screen pixels
        final int nh = Math.max(5, Math.round(m.m00() * hgt * gs));     // box height in screen pixels
        int bx = Math.round((m.m00() * x + m.m30()) * gs);
        int by = Math.round((m.m11() * y + m.m31()) * gs);
        final boolean reserve = "RESERVE".equals(mode);
        int rows = reserve ? 4 : "AUTO".equals(mode) ? 3 : "BURST".equals(mode) ? 3 : 1;
        int h = Math.max(2, Math.round(nh * (reserve ? 0.17f : 0.20f)));      // bullet thickness
        int gap = Math.max(1, Math.round(nh * (reserve ? 0.08f : 0.10f)));    // gap between rows
        while (rows > 1 && rows * h + (rows - 1) * gap > nh) {
            if (gap > 1) {
                gap--;
            } else if (h > 2) {
                h--;
            } else {
                break;
            }
        }
        int[] fit = fitParity(rows, h, gap, nh);
        h = fit[0];
        gap = fit[1];
        int total = rows * h + (rows - 1) * gap;
        int top = by + (nh - total) / 2;
        final boolean burst = "BURST".equals(mode);
        final int bw = burst ? Math.max(5, Math.round(nw * 0.72f)) : nw;        // burst: shorter bullets in a cascade
        final int fh = h, fgap = gap, ftop = top, frows = rows, fbx = bx;
        scoped(g, () -> {
            for (int i = 0; i < frows; i++) {
                int ry = ftop + i * (fh + fgap);
                if (reserve) {
                    roundBullet(g, fbx, ry, nw, fh, argb);
                } else if (burst) {
                    int off = frows == 1 ? 0 : Math.round((nw - bw) * i / (float) (frows - 1));   // 0 .. nw-bw, left to right
                    fireBullet(g, fbx + off, ry, bw, fh, argb);
                } else {
                    fireBullet(g, fbx, ry, nw, fh, argb);
                }
            }
        });
    }

    /**
     * The fire mode icon for one cell of the fire mode control, worked out in screen pixels from the cell so it is always
     * centered with an even margin and never near the border. The reference icons are small: AUTO and BURST are three
     * short bars (about 38% of the cell width, 49% of its height), SEMI is one thin bullet (about 47% of the width).
     */
    public static void fireCellIcon(GuiGraphics g, String mode, float x, float y, float w, float hgt, int argb) {
        if ((argb >>> 24) == 0) {
            return;
        }
        Matrix4f m = g.pose().last().pose();
        float gs = gui();
        int cx = Math.round((m.m00() * x + m.m30()) * gs);
        int cy = Math.round((m.m11() * y + m.m31()) * gs);
        int cw = Math.round(m.m00() * w * gs);
        int ch = Math.round(m.m00() * hgt * gs);
        final boolean semi = "SEMI".equals(mode);
        final boolean burst = "BURST".equals(mode);
        final int rows = semi ? 1 : 3;
        // margin on the left / right is a whole number of pixels, so the icon width has the same parity as the cell
        final int margin = Math.max(2, Math.round(cw * (1f - (semi ? 0.47f : 0.38f)) / 2f));
        final int iw = Math.max(8, cw - 2 * margin);
        int t0;
        int gap0;
        if (semi) {
            t0 = Math.max(3, Math.round(ch * 0.21f));          // one thin bullet
            gap0 = 0;
        } else {
            int ih = Math.max(5, even(ch * 0.49f));
            t0 = Math.max(2, (int) Math.floor(ih / (rows + (rows - 1) * 0.5)));
            gap0 = Math.max(1, t0 / 2);
        }
        int[] fit = fitParity(rows, t0, gap0, ch);
        final int t = fit[0];
        final int gap = fit[1];
        final int total = rows * t + (rows - 1) * gap;
        final int ix = cx + margin;
        final int top = cy + (ch - total) / 2;
        final int bw = burst ? Math.max(6, Math.round(iw * 0.72f)) : iw;
        scoped(g, () -> {
            for (int i = 0; i < rows; i++) {
                int off = burst ? Math.round((iw - bw) * i / (float) (rows - 1)) : 0;   // burst: a cascade, left to right
                fireBullet(g, ix + off, top + i * (t + gap), burst ? bw : iw, t, argb);
            }
        });
    }

    private static int even(float v) {
        int r = Math.round(v);
        return (r & 1) == 0 ? r : r + 1;
    }

    /** Height of a tapered piece at column k of len (k = 0 is the full-height end, shrinking to 1 px at the point). */
    private static int taper(int h, int k, int len) {
        return Math.max(1, Math.round(h * (1f - (k + 0.5f) / len)));
    }

    /**
     * One bullet row pointing left: a short pointed piece (the tip), a gap, then the long piece (the casing) with a square
     * end. The tip grows two rows per column, so it is exactly centered at every thickness (rounding a taper to whole
     * rows skewed it by a pixel). A row too thin to have a point is drawn as two plain pieces.
     * {@code (x, y)} is the top-left corner, {@code w} x {@code h} pixels.
     */
    private static void bulletRow(GuiGraphics g, int x, int y, int w, int h, int argb) {
        int minTh = (h & 1) == 1 ? 1 : 2;
        int gap = Math.max(1, Math.round(w * 0.07f));
        if (h <= minTh) {
            int piece = Math.max(2, Math.round(w * 0.2f));
            fillPx(g, x, y, x + piece, y + h, argb);
            fillPx(g, x + piece + gap, y, x + w, y + h, argb);
            return;
        }
        int cols = (h - minTh) / 2 + 1;                       // tip heights minTh, minTh + 2, ... h
        int full = Math.max(1, Math.round(w * 0.06f));        // full-height columns right behind the point
        while (cols + full + gap + 2 > w && full > 0) {
            full--;
        }
        for (int k = 0; k < cols; k++) {
            int th = minTh + 2 * k;
            int yy = y + (h - th) / 2;                        // h - th is even: no skew
            fillPx(g, x + k, yy, x + k + 1, yy + th, argb);
        }
        for (int k = cols; k < cols + full; k++) {
            fillPx(g, x + k, y, x + k + 1, y + h, argb);
        }
        fillPx(g, x + cols + full + gap, y, x + w, y + h, argb);
    }

    private static void fireBullet(GuiGraphics g, int x, int y, int w, int h, int argb) {
        bulletRow(g, x, y, w, h, argb);
    }

    private static void roundBullet(GuiGraphics g, int x, int y, int w, int h, int argb) {
        bulletRow(g, x, y, w, h, argb);                       // reserve ammo icon: same bullet as the fire mode icons
    }

    /**
     * Adjusts bar thickness / gap so that {@code avail - total} is even. The group then centers with exactly the same
     * margin above and below (a one pixel difference looks like a crooked icon). Returns {thickness, gap}.
     */
    private static int[] fitParity(int rows, int t, int gap, int avail) {
        int total = rows * t + (rows - 1) * gap;
        if (((avail - total) & 1) == 0) {
            return new int[]{t, gap};
        }
        if ((rows & 1) == 1) {                                 // odd rows: thickness changes the total by an odd amount
            int nt = total + rows <= avail ? t + 1 : t - 1;
            return new int[]{Math.max(1, nt), gap};
        }
        int ng = total + (rows - 1) <= avail ? gap + 1 : gap - 1;   // even rows: the gap does
        return new int[]{t, Math.max(1, ng)};
    }

    // ------------------------------------------------------------------------------------------ marker shapes

    /** Screen-pixel centre and radius of a shape given in local units. {cx, cy, r}. */
    private static int[] frame(GuiGraphics g, float cx, float cy, float r) {
        Matrix4f m = g.pose().last().pose();
        float gs = gui();
        return new int[]{Math.round((m.m00() * cx + m.m30()) * gs), Math.round((m.m11() * cy + m.m31()) * gs),
                Math.max(2, Math.round(r * m.m00() * gs))};
    }

    private static int line(GuiGraphics g) {
        return Math.max(1, Math.round(0.6f * physPerUnit(g)));
    }

    /** Diamond, filled or outline, radius {@code r} units. */
    public static void diamond(GuiGraphics g, float cx, float cy, float r, boolean filled, int argb) {
        final int[] f = frame(g, cx, cy, r);
        final int t = line(g);
        scoped(g, () -> {
            for (int dy = -f[2]; dy <= f[2]; dy++) {
                int half = f[2] - Math.abs(dy);
                if (filled || half < t) {
                    fillPx(g, f[0] - half, f[1] + dy, f[0] + half + 1, f[1] + dy + 1, argb);
                } else {
                    fillPx(g, f[0] - half, f[1] + dy, f[0] - half + t, f[1] + dy + 1, argb);
                    fillPx(g, f[0] + half + 1 - t, f[1] + dy, f[0] + half + 1, f[1] + dy + 1, argb);
                }
            }
        });
    }

    /** Square, filled or outline, half-size {@code r} units. */
    public static void square(GuiGraphics g, float cx, float cy, float r, boolean filled, int argb) {
        final int[] f = frame(g, cx, cy, r);
        final int rp = Math.max(2, f[2] - 1);
        final int t = line(g);
        final int x0 = f[0] - rp, y0 = f[1] - rp, x1 = f[0] + rp + 1, y1 = f[1] + rp + 1;
        scoped(g, () -> {
            if (filled) {
                fillPx(g, x0, y0, x1, y1, argb);
                return;
            }
            fillPx(g, x0, y0, x1, y0 + t, argb);
            fillPx(g, x0, y1 - t, x1, y1, argb);
            fillPx(g, x0, y0 + t, x0 + t, y1 - t, argb);
            fillPx(g, x1 - t, y0 + t, x1, y1 - t, argb);
        });
    }

    /** Solid triangle pointing DOWN (a vehicle / "target here" marker), base width 2r+1 pixels. */
    public static void triangleDown(GuiGraphics g, float cx, float cy, float r, int argb) {
        final int[] f = frame(g, cx, cy, r);
        final int h = f[2] * 2 + 1;
        scoped(g, () -> {
            for (int i = 0; i < h; i++) {
                int half = Math.round(f[2] * (1f - i / (float) h));
                fillPx(g, f[0] - half, f[1] - f[2] + i, f[0] + half + 1, f[1] - f[2] + i + 1, argb);
            }
        });
    }

    /** Solid arrow head for the tape ends: {@code left} points left, otherwise right. Height 2r+1 pixels. */
    public static void arrowHead(GuiGraphics g, float cx, float cy, float r, boolean left, int argb) {
        final int[] f = frame(g, cx, cy, r);
        final int w = f[2] + 1;
        scoped(g, () -> {
            for (int i = 0; i < w; i++) {
                int half = Math.round(f[2] * (1f - i / (float) w));
                int x = left ? f[0] - i : f[0] + i;
                fillPx(g, x, f[1] - half, x + 1, f[1] + half + 1, argb);
            }
        });
    }
}

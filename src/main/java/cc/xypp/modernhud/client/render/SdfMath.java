package cc.xypp.modernhud.client.render;

import cc.xypp.modernhud.config.PlateShape;

/**
 * Procedural, anti-aliased shapes from signed distance fields (SDF). A shape is a function {@code d(x, y)} that is
 * negative inside, positive outside and 0 on the edge. Sampling it on a 4 x 4 grid inside every pixel gives the exact
 * coverage of that pixel, so the edges are smooth at any size, which a hand-placed row of rectangles can never be.
 *
 * <p>Pure maths (no Minecraft classes), so it can be tested on its own. The rasters are plain ARGB int arrays.
 */
public final class SdfMath {

    private static final int SS = 4; // 4 x 4 samples per pixel

    private SdfMath() {
    }

    // ------------------------------------------------------------------------------------------------ distance functions

    /** Rounded rectangle with a different radius per corner (screen coordinates: y grows downwards, origin = centre). */
    static double roundBox(double x, double y, double hw, double hh, double rTL, double rTR, double rBR, double rBL) {
        double r = x < 0 ? (y < 0 ? rTL : rBL) : (y < 0 ? rTR : rBR);
        double qx = Math.abs(x) - (hw - r);
        double qy = Math.abs(y) - (hh - r);
        return Math.min(Math.max(qx, qy), 0.0) + Math.hypot(Math.max(qx, 0.0), Math.max(qy, 0.0)) - r;
    }

    /** Signed distance to a convex polygon (any winding): negative inside. */
    static double polygon(double px, double py, double[] vx, double[] vy) {
        int n = vx.length;
        double best = Double.MAX_VALUE;
        boolean pos = false;
        boolean neg = false;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double ex = vx[j] - vx[i];
            double ey = vy[j] - vy[i];
            double wx = px - vx[i];
            double wy = py - vy[i];
            double t = Math.max(0.0, Math.min(1.0, (wx * ex + wy * ey) / (ex * ex + ey * ey)));
            double dx = wx - ex * t;
            double dy = wy - ey * t;
            best = Math.min(best, dx * dx + dy * dy);
            double cross = ex * wy - ey * wx;
            if (cross > 0) {
                pos = true;
            } else if (cross < 0) {
                neg = true;
            }
        }
        double d = Math.sqrt(best);
        return (pos && neg) ? d : -d;
    }

    /** Distance to a line segment, minus a half width: a stroke with round ends. */
    static double stroke(double px, double py, double ax, double ay, double bx, double by, double halfWidth) {
        double ex = bx - ax;
        double ey = by - ay;
        double wx = px - ax;
        double wy = py - ay;
        double t = Math.max(0.0, Math.min(1.0, (wx * ex + wy * ey) / (ex * ex + ey * ey)));
        return Math.hypot(wx - ex * t, wy - ey * t) - halfWidth;
    }

    /**
     * Whole-pixel layout of the WARZONE plate: {depth of the bottom cut-out, width of one foot}. Measured on the
     * reference: the feet stand about 22 % of the height below the bar and are about 1.2 x the height wide.
     */
    static int[] warzoneLayout(int w, int h) {
        int depth = Math.max(1, Math.round(0.22f * h));
        int feet = Math.max(2, Math.min(Math.round(1.2f * h), Math.round(0.25f * w)));
        return new int[]{depth, feet};
    }

    /** Height in screen pixels of a WARZONE plate {@code cellPx} wide: about 9.5 : 1 like the reference, 4 px at the least. */
    public static int warzoneHeightPx(int cellPx) {
        return Math.max(4, Math.round(cellPx / 9.5f));
    }

    /** Smooth intersection of two distance fields: like {@code max(a, b)} but with the convex corner rounded by r. */
    private static double roundedMax(double a, double b, double r) {
        double ux = Math.max(a + r, 0.0);
        double uy = Math.max(b + r, 0.0);
        return Math.min(-r, Math.max(a, b)) + Math.hypot(ux, uy);
    }

    /** Distance of the WARZONE plate (origin = centre of the {@code w} x {@code h} box). */
    private static double warzone(double x, double y, double w, double h) {
        int[] l = warzoneLayout((int) Math.round(w), (int) Math.round(h));
        double hw = w / 2.0;
        double hh = h / 2.0;
        double rTop = Math.min(0.5 * h, w / 4.0);          // big round top corners
        double body = roundBox(x, y, hw, hh, rTop, rTop, 0.5, 0.5);
        // the cut-out along the bottom: from the inner edge of one foot to the other, depth l[0], open at the bottom
        double nTop = hh - l[0];
        double nBottom = hh + 4.0;
        double notch = roundBox(x, y - (nTop + nBottom) / 2.0, hw - l[1], (nBottom - nTop) / 2.0, 0.8, 0.8, 0.8, 0.8);
        return roundedMax(body, -notch, Math.max(0.6, 0.09 * h));   // feet get softly rounded inner corners
    }

    /** Distance of the plate outline (origin at the centre of a {@code w} by {@code h} box). */
    static double plate(PlateShape shape, double x, double y, double w, double h) {
        double hw = w / 2.0;
        double hh = h / 2.0;
        switch (shape) {
            case WARZONE:
                return warzone(x, y, w, h);
            case CHAMFER: {
                double r = 0.10 * h;
                double ix = hw - r;
                double iy = hh - r;
                double c = Math.min(0.30 * h, Math.min(ix, iy * 0.85)); // never longer than the side it cuts
                return polygon(x, y,
                        new double[]{-ix + c, ix - c, ix, ix, ix - c, -ix + c, -ix, -ix},
                        new double[]{-iy, -iy, -iy + c, iy - c, iy, iy, iy - c, -iy + c}) - r;
            }
            case HEX: {
                double r = 0.10 * h;
                double ix = hw - r;
                double iy = hh - r;
                double c = Math.min(0.55 * h, ix * 0.9);
                return polygon(x, y,
                        new double[]{-ix + c, ix - c, ix, ix - c, -ix + c, -ix},
                        new double[]{-iy, -iy, 0, iy, iy, 0}) - r;
            }
            case SLANT: {
                double r = 0.14 * h;
                double ix = hw - r;
                double iy = hh - r;
                double s = Math.min(0.6 * h, ix);
                return polygon(x, y,
                        new double[]{-ix + s, ix, ix - s, -ix},
                        new double[]{-iy, -iy, iy, iy}) - r;
            }
            case PILL:
                return roundBox(x, y, hw, hh, 0.5 * h, 0.5 * h, 0.5 * h, 0.5 * h);
            default: // PLATE and anything else: the armor plate of the reference (small left corners, big round right end)
                return roundBox(x, y, hw, hh, 0.22 * h, 0.50 * h, 0.50 * h, 0.22 * h);
        }
    }

    // ------------------------------------------------------------------------------------------------ rasters

    /**
     * One plate as an atlas: the BODY on the left (white with a vertical shading ramp in the colour channels, coverage in
     * alpha) and the OUTLINE ring on the right (white, coverage in alpha), each with a 1 pixel transparent margin so
     * that bilinear sampling never reads its neighbour.
     *
     * @return ARGB pixels, row by row, of size {@code 2 * (w + 2)} by {@code h + 2}
     */
    public static int[] plateAtlas(PlateShape shape, int w, int h) {
        int tw = 2 * (w + 2);
        int th = h + 2;
        int[] out = new int[tw * th];
        boolean flat = shape == PlateShape.WARZONE;
        // flat design like the reference: a faint hairline so an empty plate stays visible, almost no shading
        double ring = flat ? (h >= 10 ? 1.0 : 0.0) : Math.max(1.0, 0.11 * h);
        double bevel = flat ? 0.05 : 0.30;
        for (int py = 0; py < h; py++) {
            double shade = 1.0 - bevel * ((py + 0.5) / h); // bright top, darker bottom: a soft bevel
            int g = (int) Math.round(255.0 * shade);
            for (int px = 0; px < w; px++) {
                int body = 0;
                int edge = 0;
                for (int sy = 0; sy < SS; sy++) {
                    for (int sx = 0; sx < SS; sx++) {
                        double x = px + (sx + 0.5) / SS - w / 2.0;
                        double y = py + (sy + 0.5) / SS - h / 2.0;
                        double d = plate(shape, x, y, w, h);
                        if (d <= 0.0) {
                            body++;
                            if (ring > 0.0 && d >= -ring) {
                                edge++;
                            }
                        }
                    }
                }
                int total = SS * SS;
                int ab = body * 255 / total;
                int ae = edge * 255 / total;
                out[(py + 1) * tw + (px + 1)] = (ab << 24) | (g << 16) | (g << 8) | g;
                out[(py + 1) * tw + (w + 2) + (px + 1)] = (ae << 24) | 0xFFFFFF;
            }
        }
        return out;
    }

    /**
     * The "lost" X: four thin arms pointing at the centre with the centre left EMPTY (they never cross), like a
     * hitmarker. Each arm is a flat-ended bar (no round blobs), so the tips stay sharp. Coverage is the exact area of
     * the bar in each pixel (8 x 8 samples), then lifted a little so a hairline does not look washed out.
     * White, coverage in alpha, 1 pixel transparent margin all round.
     *
     * @return ARGB pixels of size {@code (s + 2)} by {@code (s + 2)}
     */
    public static int[] crossRaster(int s) {
        final int n = 8;
        final double r2 = Math.sqrt(2.0);
        int tw = s + 2;
        int[] out = new int[tw * tw];
        double c = s / 2.0;
        double t0 = 0.17 * s * r2;                  // start of an arm along its diagonal (the empty centre)
        double t1 = 0.41 * s * r2;                  // end of an arm
        double tm = (t0 + t1) / 2.0;
        double hl = (t1 - t0) / 2.0;
        double half = Math.max(0.5, 0.036 * s);     // half the stroke width: about 1.15 px at 16, thin at any scale
        for (int py = 0; py < s; py++) {
            for (int px = 0; px < s; px++) {
                int cov = 0;
                for (int sy = 0; sy < n; sy++) {
                    for (int sx = 0; sx < n; sx++) {
                        double x = px + (sx + 0.5) / n - c;
                        double y = py + (sy + 0.5) / n - c;
                        double d = Double.MAX_VALUE;
                        for (int dx = -1; dx <= 1; dx += 2) {
                            for (int dy = -1; dy <= 1; dy += 2) {
                                double along = (x * dx + y * dy) / r2;
                                double across = (y * dx - x * dy) / r2;
                                double qx = Math.abs(along - tm) - hl;
                                double qy = Math.abs(across) - half;
                                d = Math.min(d, Math.min(Math.max(qx, qy), 0.0) + Math.hypot(Math.max(qx, 0.0), Math.max(qy, 0.0)));
                            }
                        }
                        if (d <= 0.0) {
                            cov++;
                        }
                    }
                }
                double a = Math.pow(cov / (double) (n * n), 0.80);
                out[(py + 1) * tw + (px + 1)] = ((int) Math.round(a * 255.0) << 24) | 0xFFFFFF;
            }
        }
        return out;
    }
}

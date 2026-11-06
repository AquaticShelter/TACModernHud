package cc.xypp.modernhud.client.render;

import cc.xypp.modernhud.client.HudBatch;
import cc.xypp.modernhud.client.HudText;
import cc.xypp.modernhud.config.PlateFill;
import cc.xypp.modernhud.config.PlateShape;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Smooth shapes for the HUD: armor plates and the "lost item" X.
 *
 * <p>Rows of rectangles cannot make a round or slanted edge look good. Instead each shape is computed once from a signed
 * distance field ({@link SdfMath}) at the exact size in SCREEN pixels it will be drawn at (so it is anti-aliased and still
 * 1:1 with the screen), kept in a small texture, and drawn as textured quads tinted with the colour wanted. A whole row of
 * plates, with track, fill and outline, goes out as ONE draw call. If anything fails, callers fall back to the old
 * pixel-row drawing, so a problem here can never break the HUD.
 */
public final class SdfShapes {

    private static final Logger LOGGER = LogManager.getLogger("ModernHud/Sdf");
    private static final int MAX_TEXTURES = 24;

    private static final class Tex {
        final DynamicTexture texture;
        final int width;   // whole atlas
        final int height;

        Tex(DynamicTexture texture, int width, int height) {
            this.texture = texture;
            this.width = width;
            this.height = height;
        }
    }

    /** Insertion ordered, so the oldest texture is the first to be freed. */
    private static final Map<String, Tex> CACHE = new LinkedHashMap<>();
    private static boolean broken;

    private SdfShapes() {
    }

    public static boolean usable() {
        return !broken;
    }

    // ------------------------------------------------------------------------------------------------ textures

    private static Tex texture(String key, int w, int h, int[] argb) {
        Tex t = CACHE.get(key);
        if (t != null) {
            return t;
        }
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, w, h, true);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = argb[y * w + x];
                // NativeImage wants ABGR
                int abgr = (p & 0xFF00FF00) | ((p >> 16) & 0xFF) | ((p & 0xFF) << 16);
                img.setPixelRGBA(x, y, abgr);
            }
        }
        DynamicTexture tex = new DynamicTexture(img);
        tex.setFilter(true, false); // bilinear: the shapes are 1:1, this only smooths a half pixel offset
        tex.upload();
        if (CACHE.size() >= MAX_TEXTURES) {
            String oldest = CACHE.keySet().iterator().next();
            CACHE.remove(oldest).texture.close();
        }
        t = new Tex(tex, w, h);
        CACHE.put(key, t);
        return t;
    }

    private static Tex plateTexture(PlateShape shape, int w, int h) {
        String key = "P|" + shape + "|" + w + "|" + h;
        Tex t = CACHE.get(key);
        return t != null ? t : texture(key, 2 * (w + 2), h + 2, SdfMath.plateAtlas(shape, w, h));
    }

    private static Tex crossTexture(int s) {
        String key = "X|" + s;
        Tex t = CACHE.get(key);
        return t != null ? t : texture(key, s + 2, s + 2, SdfMath.crossRaster(s));
    }

    // ------------------------------------------------------------------------------------------------ drawing helpers

    /** Moves a local coordinate so that its SCREEN position is a whole pixel (keeps a 1:1 texture exactly sharp). */
    private static float snap(float local, float scale, float offset, double gui) {
        if (scale <= 0f) {
            return local;
        }
        double screen = (scale * local + offset) * gui;
        return (float) (((Math.rint(screen) / gui) - offset) / scale);
    }

    private static void quad(BufferBuilder bb, Matrix4f m, float x0, float y0, float x1, float y1,
                             float u0, float v0, float u1, float v1, int argb) {
        int a = argb >>> 24;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        bb.vertex(m, x0, y1, 0f).color(r, g, b, a).uv(u0, v1).endVertex();
        bb.vertex(m, x1, y1, 0f).color(r, g, b, a).uv(u1, v1).endVertex();
        bb.vertex(m, x1, y0, 0f).color(r, g, b, a).uv(u1, v0).endVertex();
        bb.vertex(m, x0, y0, 0f).color(r, g, b, a).uv(u0, v0).endVertex();
    }

    private static void begin(GuiGraphics g, Tex t) {
        if (HudBatch.active()) {
            g.flush(); // what the open GUI batch holds must be drawn before this, to keep the order
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.setShaderTexture(0, t.texture.getId());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    // ------------------------------------------------------------------------------------------------ plates

    /**
     * A row of absorption plates: for each plate its slot (track), its fill and an outline, all in one draw call.
     *
     * @return false if the smooth path could not be used (the caller then draws the old way)
     */
    public static boolean plates(GuiGraphics g, float x, float y, float cellW, float h, float gap, int segments,
                                 float[] fractions, int fillArgb, int trackArgb, int edgeArgb,
                                 PlateShape shape, PlateFill fill, boolean outline) {
        if (broken || segments <= 0) {
            return false;
        }
        try {
            Matrix4f m = g.pose().last().pose();
            float scale = m.m00();
            float phys = HudText.physPerUnit(g);
            if (phys <= 0.01f || scale <= 0f) {
                return false;
            }
            double gui = Minecraft.getInstance().getWindow().getGuiScale();
            int wPx = Math.max(6, Math.round(cellW * phys));
            int hPx = Math.max(4, Math.round(h * phys));
            int hRow = hPx;                   // the room the layout reserved
            if (shape == PlateShape.WARZONE) {
                hPx = Math.max(4, Math.min(hRow, SdfMath.warzoneHeightPx(wPx)));   // slim: ~9.5 : 1, centred in the row
            }
            Tex t = plateTexture(shape, wPx, hPx);
            float uw = wPx / phys;  // size of one plate in local units: exactly wPx screen pixels
            float uh = hPx / phys;
            float oy = snap(y + ((hRow - hPx) / 2) / phys, m.m11(), m.m31(), gui);
            float tw = t.width;
            float th = t.height;
            float bodyU = 1f;                 // texel offsets of the two halves inside the atlas
            float ringU = wPx + 2 + 1f;

            begin(g, t);
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            for (int i = 0; i < segments; i++) {
                float ox = snap(x + i * (cellW + gap), scale, m.m30(), gui);
                // 1. the slot
                quad(bb, m, ox, oy, ox + uw, oy + uh, bodyU / tw, 1f / th, (bodyU + wPx) / tw, (1f + hPx) / th, trackArgb);
                // 2. the fill, cut at a whole pixel
                float f = fractions[i];
                if (f > 0.001f) {
                    if (fill == PlateFill.HORIZONTAL) {
                        int fw = Math.max(1, Math.min(wPx, Math.round(wPx * f)));
                        quad(bb, m, ox, oy, ox + fw / phys, oy + uh,
                                bodyU / tw, 1f / th, (bodyU + fw) / tw, (1f + hPx) / th, fillArgb);
                    } else {
                        int fh = Math.max(1, Math.min(hPx, Math.round(hPx * f)));
                        quad(bb, m, ox, oy + (hPx - fh) / phys, ox + uw, oy + uh,
                                bodyU / tw, (1f + hPx - fh) / th, (bodyU + wPx) / tw, (1f + hPx) / th, fillArgb);
                    }
                }
                // 3. the outline
                if (outline) {
                    quad(bb, m, ox, oy, ox + uw, oy + uh, ringU / tw, 1f / th, (ringU + wPx) / tw, (1f + hPx) / th, edgeArgb);
                }
            }
            BufferUploader.drawWithShader(bb.end());
            return true;
        } catch (RuntimeException | LinkageError e) {
            broken = true;
            LOGGER.warn("Smooth shapes switched off, using the pixel drawing instead: {}", e.toString());
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------------ the X

    /**
     * The "lost" X (four strokes, empty centre) in a square of {@code size} local units.
     *
     * @return false if the smooth path could not be used
     */
    public static boolean cross(GuiGraphics g, float x, float y, float size, int argb) {
        if (broken) {
            return false;
        }
        try {
            Matrix4f m = g.pose().last().pose();
            float scale = m.m00();
            float phys = HudText.physPerUnit(g);
            if (phys <= 0.01f || scale <= 0f) {
                return false;
            }
            double gui = Minecraft.getInstance().getWindow().getGuiScale();
            int s = Math.max(8, Math.round(size * phys));
            s += s & 1;                                   // even: the centre sits on a pixel corner, all 4 arms are mirror images
            Tex t = crossTexture(s);
            float u = s / phys;
            float ox = snap(x + (size - u) / 2f, scale, m.m30(), gui);   // keep the centre where the icon slot's centre is
            float oy = snap(y + (size - u) / 2f, m.m11(), m.m31(), gui);
            begin(g, t);
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            quad(bb, m, ox, oy, ox + u, oy + u, 1f / t.width, 1f / t.height, (1f + s) / t.width, (1f + s) / t.height, argb);
            BufferUploader.drawWithShader(bb.end());
            return true;
        } catch (RuntimeException | LinkageError e) {
            broken = true;
            LOGGER.warn("Smooth shapes switched off, using the pixel drawing instead: {}", e.toString());
            return false;
        }
    }
}

package cc.xypp.modernhud.client;

import cc.xypp.modernhud.client.motion.MotionHud;
import cc.xypp.modernhud.client.motion.MotionLayer;
import cc.xypp.modernhud.client.perf.HudProfiler;
import cc.xypp.modernhud.config.HudConfig;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * The HUD's own GUI scale, independent of Minecraft's "GUI Scale" video setting.
 *
 * <p>Every overlay of this mod lays itself out in "GUI units" (the {@code screenWidth/screenHeight} it is handed).
 * Minecraft hands out units of its own GUI scale; to use a different one this wrapper draws the overlay in a pose
 * scaled by {@code wanted / minecraft} and tells it the screen size in the WANTED units - so each overlay simply
 * believes the screen is that size, and nothing inside needs to know.
 */
public final class HudScale {

    private HudScale() {
    }

    /** The GUI scale (window pixels per GUI unit) the HUD wants right now. */
    public static double wanted(Window w) {
        switch (HudConfig.guiScaleMode) {
            case FIXED:
                return Math.max(0.25, HudConfig.hudGuiScale);
            case RESOLUTION:
            {
                    double v = HudConfig.hudGuiScale * w.getHeight() / (double) Math.max(1, HudConfig.guiScaleReferenceHeight);
                    if (HudConfig.guiScaleSnap) {
                        v = Math.max(1.0, Math.round(v));
                    }
                    return Math.max(0.25, v);
                }
            default:
                return w.getGuiScale();
        }
    }

    /** Wraps an overlay that never sways with the camera. */
    public static IGuiOverlay wrap(IGuiOverlay inner) {
        return wrap(inner, MotionLayer.NONE);
    }

    /** Wraps an overlay; {@code layer} decides how strongly the motion HUD moves it (see {@link MotionHud}). */
    public static IGuiOverlay wrap(IGuiOverlay inner, MotionLayer layer) {
        return (gui, graphics, partialTick, sw, sh) -> render(inner, gui, graphics, partialTick, sw, sh, layer);
    }

    public static void render(IGuiOverlay inner, ForgeGui gui, GuiGraphics graphics, float partialTick, int sw, int sh) {
        render(inner, gui, graphics, partialTick, sw, sh, MotionLayer.NONE);
    }

    public static void render(IGuiOverlay inner, ForgeGui gui, GuiGraphics graphics, float partialTick, int sw, int sh,
                              MotionLayer layer) {
        // the camera-motion offset is applied OUTSIDE the HUD scale: it is a whole-screen-pixel shift of everything below
        PoseStack outer = graphics.pose();
        boolean moved = MotionHud.push(outer, layer);
        long token = HudProfiler.begin(layer);
        try {
            renderScaled(inner, gui, graphics, partialTick, sw, sh);
        } finally {
            HudProfiler.end(layer, token);
            if (moved) {
                outer.popPose();
            }
        }
    }

    private static void renderScaled(IGuiOverlay inner, ForgeGui gui, GuiGraphics graphics, float partialTick, int sw, int sh) {
        Window w = Minecraft.getInstance().getWindow();
        double mc = w.getGuiScale();
        double want = wanted(w);
        if (Math.abs(want - mc) < 0.001) {
            inner.render(gui, graphics, partialTick, sw, sh);
            return;
        }
        float k = (float) (want / mc);
        int vw = (int) Math.ceil(w.getWidth() / want);
        int vh = (int) Math.ceil(w.getHeight() / want);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.scale(k, k, 1f);
        try {
            inner.render(gui, graphics, partialTick, vw, vh);
        } finally {
            pose.popPose();
        }
    }
}

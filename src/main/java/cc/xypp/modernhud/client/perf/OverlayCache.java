package cc.xypp.modernhud.client.perf;

import cc.xypp.modernhud.ModernHud;
import cc.xypp.modernhud.config.PerfConfig;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Overlay cache: any HUD overlay, from any mod, can be redrawn only N times a second. In between, the picture it produced
 * last time is shown from a texture, which costs almost nothing, so the overlay's whole CPU time is saved on those frames.
 *
 * <p>How it works, with nothing but the Pre / Post events Forge fires around every overlay:
 * <ul>
 *   <li><b>Redraw frame</b> (the overlay is due): in Pre the GUI batch is flushed and a full-screen framebuffer is bound
 *       and cleared; the overlay then draws as usual, into that framebuffer; in Post the batch is flushed again, the screen
 *       framebuffer is bound back and the picture is shown.</li>
 *   <li><b>Other frames</b>: Pre is cancelled (the overlay is not run at all) and the stored picture is shown instead.</li>
 * </ul>
 *
 * <p>Safety, because the overlays of other mods are not under our control:
 * <ul>
 *   <li>Nothing is cached unless the player asked for that overlay ({@code /modernhud optimize}).</li>
 *   <li>If an overlay binds a framebuffer of its own while it draws (a minimap that renders into a texture, for example),
 *       its picture cannot be captured: caching is switched off for it for the rest of the session and a message says so.</li>
 *   <li>If something goes wrong between Pre and Post, the screen framebuffer is restored at the end of the GUI pass.</li>
 *   <li>Any exception also switches the overlay's caching off; the overlay itself is never left cancelled.</li>
 * </ul>
 * One full-screen picture is kept per cached overlay (about 8 MB at 1080p), at most {@value #MAX_SLOTS} of them.
 */
@Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class OverlayCache {

    private static final Logger LOGGER = LogManager.getLogger("ModernHud/OverlayCache");
    private static final int MAX_SLOTS = 8;

    private static final class Slot {
        TextureTarget target;
        int width;
        int height;
        double guiScale;
        long lastCapture;
        boolean valid;
        boolean broken;
        String reason = "";
        boolean redrawThisFrame;
        long redraws;
        long replays;
    }

    private static final Map<String, Slot> SLOTS = new HashMap<>();
    private static String capturingId;
    private static Slot capturing;

    private OverlayCache() {
    }

    // ------------------------------------------------------------------------------------------------ events

    /** A new GUI pass: forget the marks of the last one. */
    @SubscribeEvent
    public static void onGuiStart(final RenderGuiEvent.Pre event) {
        for (Slot s : SLOTS.values()) {
            s.redrawThisFrame = false;
        }
    }

    /** Highest priority: decide whether this overlay is replayed from the stored picture (then it is cancelled). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPreDecide(final RenderGuiOverlayEvent.Pre event) {
        Map<String, Integer> hz = PerfConfig.hz;
        if (!PerfConfig.cacheEnabled || hz.isEmpty()) {
            return;
        }
        String id = event.getOverlay().id().toString();
        Integer rate = hz.get(id);
        if (rate == null) {
            return;
        }
        try {
            Slot s = SLOTS.computeIfAbsent(id, k -> new Slot());
            if (s.broken || (s.target == null && SLOTS.size() > MAX_SLOTS)) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            Window window = mc.getWindow();
            if (!prepare(s, window)) {
                return;
            }
            long now = System.nanoTime();
            boolean due = !s.valid || now - s.lastCapture >= 1_000_000_000L / rate;
            if (due) {
                s.redrawThisFrame = true; // captured in onPreCapture, after every other listener had its say
            } else {
                event.setCanceled(true);
                show(event.getGuiGraphics(), s, window);
                s.replays++;
            }
        } catch (RuntimeException | LinkageError e) {
            disable(id, "error: " + e);
        }
    }

    /** Lowest priority and not receiving cancelled events: only runs when the overlay really is going to draw. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPreCapture(final RenderGuiOverlayEvent.Pre event) {
        if (SLOTS.isEmpty()) {
            return;
        }
        String id = event.getOverlay().id().toString();
        Slot s = SLOTS.get(id);
        if (s == null || !s.redrawThisFrame || s.broken || capturing != null) {
            return;
        }
        try {
            GuiGraphics g = event.getGuiGraphics();
            g.flush(); // what was drawn before this overlay must reach the screen, not our picture
            s.target.setClearColor(0f, 0f, 0f, 0f);
            s.target.clear(Minecraft.ON_OSX);
            s.target.bindWrite(true);
            capturingId = id;
            capturing = s;
        } catch (RuntimeException | LinkageError e) {
            restoreScreen();
            disable(id, "error: " + e);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPost(final RenderGuiOverlayEvent.Post event) {
        if (capturing == null || !capturingId.equals(event.getOverlay().id().toString())) {
            return;
        }
        String id = capturingId;
        Slot s = capturing;
        capturing = null;
        capturingId = null;
        try {
            GuiGraphics g = event.getGuiGraphics();
            g.flush(); // everything the overlay drew goes into the picture
            int bound = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            boolean intact = bound == s.target.frameBufferId;
            restoreScreen();
            if (!intact) {
                disable(id, "it binds a framebuffer of its own while drawing, so its picture cannot be captured");
                return;
            }
            s.valid = true;
            s.lastCapture = System.nanoTime();
            s.redraws++;
            show(g, s, Minecraft.getInstance().getWindow());
        } catch (RuntimeException | LinkageError e) {
            restoreScreen();
            disable(id, "error: " + e);
        }
    }

    /** Safety net: an overlay that never reached Post (cancelled late, or an exception) must not leave our framebuffer bound. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGuiEnd(final RenderGuiEvent.Post event) {
        if (capturing != null) {
            String id = capturingId;
            capturing = null;
            capturingId = null;
            restoreScreen();
            disable(id, "it did not finish drawing (cancelled by another mod?)");
        }
    }

    // ------------------------------------------------------------------------------------------------ internals

    private static void restoreScreen() {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }

    /** Creates or resizes the picture; returns false when it cannot be used. */
    private static boolean prepare(Slot s, Window window) {
        int w = window.getWidth();
        int h = window.getHeight();
        double gui = window.getGuiScale();
        if (w <= 0 || h <= 0) {
            return false;
        }
        if (s.target == null) {
            s.target = new TextureTarget(w, h, true, Minecraft.ON_OSX);
            s.width = w;
            s.height = h;
            s.guiScale = gui;
            s.valid = false;
        } else if (s.width != w || s.height != h) {
            s.target.resize(w, h, Minecraft.ON_OSX);
            s.width = w;
            s.height = h;
            s.valid = false;
        }
        if (s.guiScale != gui) { // a different layout: the old picture is wrong
            s.guiScale = gui;
            s.valid = false;
        }
        return true;
    }

    /** Draws the stored picture over the whole screen. Both are the same size, so it is a 1:1 copy. */
    private static void show(GuiGraphics g, Slot s, Window window) {
        g.flush(); // keep the order of drawing
        float w = window.getGuiScaledWidth();
        float h = window.getGuiScaledHeight();
        Matrix4f m = g.pose().last().pose();
        RenderSystem.enableBlend();
        // the picture holds colours already multiplied by their alpha (it was drawn onto transparent black)
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, s.target.getColorTextureId());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.vertex(m, 0f, h, 0f).uv(0f, 0f).endVertex();
        bb.vertex(m, w, h, 0f).uv(1f, 0f).endVertex();
        bb.vertex(m, w, 0f, 0f).uv(1f, 1f).endVertex();
        bb.vertex(m, 0f, 0f, 0f).uv(0f, 1f).endVertex();
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.defaultBlendFunc();
    }

    private static void disable(String id, String reason) {
        Slot s = SLOTS.get(id);
        if (s != null) {
            s.broken = true;
            s.valid = false;
            s.reason = reason;
            if (s.target != null) {
                try {
                    s.target.destroyBuffers();
                } catch (RuntimeException ignored) {
                    // already gone
                }
                s.target = null;
            }
        }
        LOGGER.warn("Overlay cache switched off for {}: {}", id, reason);
        PerfConfig.block(id); // not picked again, in this or later sessions, until /modernhud optimize retry
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("[Modern HUD] Overlay cache off for " + id + ": " + reason), false);
        }
    }

    /** Lines for {@code /modernhud optimize list}. */
    public static java.util.List<String> status() {
        java.util.List<String> out = new java.util.ArrayList<>();
        out.add("Overlay cache is " + (PerfConfig.cacheEnabled ? "ON" : "OFF") + ", " + PerfConfig.hz.size() + " overlay(s) listed:");
        for (Map.Entry<String, Integer> e : PerfConfig.hz.entrySet()) {
            Slot s = SLOTS.get(e.getKey());
            String state = s == null ? "not drawn yet"
                    : s.broken ? "SWITCHED OFF: " + s.reason
                    : "redrawn " + s.redraws + " times, shown from the picture " + s.replays + " times";
            out.add("  " + e.getKey() + " = " + e.getValue() + " redraws/s  (" + state + ")");
        }
        return out;
    }

    /** Frees the pictures of overlays that are no longer listed. */
    public static void prune() {
        Iterator<Map.Entry<String, Slot>> it = SLOTS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Slot> e = it.next();
            if (!PerfConfig.hz.containsKey(e.getKey())) {
                if (e.getValue().target != null) {
                    e.getValue().target.destroyBuffers();
                }
                it.remove();
            }
        }
    }

    /** Lets a switched-off overlay try again (after the player changed something). */
    public static void retryAll() {
        PerfConfig.clearBlocked();
        for (Slot s : SLOTS.values()) {
            s.broken = false;
            s.reason = "";
        }
    }
}

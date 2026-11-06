package cc.xypp.modernhud.client;

import cc.xypp.modernhud.ModernHud;
import cc.xypp.modernhud.config.BossBackdrop;
import cc.xypp.modernhud.config.BossBarConfig;
import cc.xypp.modernhud.config.BossNameMode;
import cc.xypp.modernhud.config.BossNumberLayout;
import cc.xypp.modernhud.config.BossNumberMode;
import cc.xypp.modernhud.config.ElementStyle;
import cc.xypp.modernhud.config.HudConfig;
import cc.xypp.modernhud.config.HudPosition;
import cc.xypp.modernhud.config.PanelBorderStyle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Compact corner boss bars in the style of Modern Warfare's match-progress readout: a thin full-length TRACK
 * line with an end cap under a thicker, smoothly shaded, softly glowing FILL whose leading edge burns brighter;
 * the number above the first bar and below the last (or beside each - see {@code numberLayout}); a small label
 * right-aligned over the track end; each bar with its own shadow/box behind it.
 *
 * <p>Smoothness: all bar shapes go through {@link SoftDraw} - float coordinates and per-vertex colours, so the
 * gradient and the glow are interpolated by the GPU rather than built from whole-GUI-pixel rectangles.
 *
 * <p>Motion: every bar eases (ease-in-out) - sliding in from the screen edge while fading in when its boss
 * appears, sliding out and fading when it is gone (the bar is kept alive just long enough to play that out),
 * the fill easing from its previous value to the new one, and the stack gliding when a bar joins or leaves.
 *
 * <p>How it hooks in: vanilla draws every boss bar itself and fires
 * {@link CustomizeGuiOverlayEvent.BossEventProgress} for each one first. {@link Events} cancels that (vanilla then
 * draws neither the wide bar nor its name, and the spacing is zeroed) and remembers the boss event; this overlay,
 * registered above everything, draws them. Vanilla keeps handling screen darkening, boss music and fog itself.
 */
public class BossBarOverlay implements IGuiOverlay {

    /** Boss events vanilla has offered, in vanilla's order, each with the time it was last offered.
     *
     *  <p>This used to be a plain per-frame list that {@link #render} cleared after every call. That made a bar's
     *  existence depend on vanilla offering it in EVERY single frame: any frame where it did not (another mod
     *  cancelling the vanilla boss overlay for a moment, a hitch, a second render pass, F1 toggles...) the bar was
     *  declared gone, ease-OUT started, and on the next frame it was back and eased IN again - the bar kept
     *  fading/sliding away and back like an auto-hide. Now a bar only counts as gone once vanilla has not offered it
     *  for {@link #GRACE_MS}, so a normal frame-to-frame gap can never trigger the exit animation. */
    private static final Map<UUID, LerpingBossEvent> SEEN = new LinkedHashMap<>();
    private static final Map<UUID, Long> SEEN_AT = new HashMap<>();
    /** How long a boss event may go unoffered before its bar eases out. */
    private static final long GRACE_MS = 700L;

    private static long monoMs() {
        return System.nanoTime() / 1_000_000L;
    }

    private static final int NUMBER_GAP = 5;
    private static final int CAPTION_GAP = 2;
    private static final long RESOLVE_INTERVAL_MS = 1000L;

    /** Per-boss state: eased visibility / fill / vertical slot, plus the last data seen (kept so a bar whose boss is
     *  gone can still draw itself while it eases out). */
    private static final class Bar {
        UUID id;
        EasedAnimator vis;
        EasedAnimator fill;
        EasedAnimator y;
        boolean present;
        boolean yPlaced;
        // last seen data
        String number = "";
        String name = "";
        int color = 0xF2F2F2;
        int notches;
        float progress;
        float lastProgress;
        long trailHoldUntil;
        EasedAnimator trail; // the "damage trail": lingers behind the fill after the boss loses health
        // layout (computed while present)
        boolean numTop, numBottom, numSide, nameTop, nameBottom;
        int capTopH, capBottomH, mainH, entryH;
    }

    /** Boss entity matched to a boss event (for exact health), re-resolved at most once a second. */
    private static final class Match {
        LivingEntity entity;
        long nextScanMs;
    }

    private final Map<UUID, Bar> bars = new LinkedHashMap<>();
    private final Map<UUID, Match> matches = new HashMap<>();
    private final EasedAnimator blockHeight = new EasedAnimator(380f, EasedAnimator.Easing.CUBIC_IN_OUT);
    private boolean blockHeightSeeded;
    private float lastBlockW = 80f;

    @Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class Events {
        @SubscribeEvent
        public static void onBossBar(final CustomizeGuiOverlayEvent.BossEventProgress event) {
            if (!BossBarConfig.enabled) {
                return;
            }
            event.setCanceled(true);
            event.setIncrement(0);
            LerpingBossEvent ev = event.getBossEvent();
            if (SEEN.size() < 64 || SEEN.containsKey(ev.getId())) { // safety: never grow unbounded
                SEEN.put(ev.getId(), ev);
                SEEN_AT.put(ev.getId(), monoMs());
            }
        }
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        try {
            if (!BossBarConfig.enabled) {
                bars.clear();
                SEEN.clear();
                SEEN_AT.clear();
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            Font font = mc.font;
            long now = System.currentTimeMillis();
            boolean animate = BossBarConfig.animations;
            float ms = BossBarConfig.animationMs;

            // ---- sync bars with what vanilla offered this frame ----
            for (Bar b : bars.values()) {
                b.present = false;
            }
            int taken = 0;
            long mono = monoMs();
            Iterator<Map.Entry<UUID, LerpingBossEvent>> seenIt = SEEN.entrySet().iterator();
            while (seenIt.hasNext()) {
                Map.Entry<UUID, LerpingBossEvent> entry = seenIt.next();
                Long at = SEEN_AT.get(entry.getKey());
                if (at == null || mono - at > GRACE_MS) {
                    seenIt.remove(); // really gone: vanilla has not offered it for a while
                    SEEN_AT.remove(entry.getKey());
                    continue;
                }
                if (taken >= BossBarConfig.maxBars) {
                    continue; // keep scanning so expired entries are still pruned
                }
                taken++;
                LerpingBossEvent ev = entry.getValue();
                Bar b = bars.get(ev.getId());
                if (b == null) {
                    b = new Bar();
                    b.id = ev.getId();
                    b.vis = new EasedAnimator(ms, EasedAnimator.Easing.CUBIC_IN_OUT);
                    b.fill = new EasedAnimator(ms * 1.2f, EasedAnimator.Easing.CUBIC_IN_OUT);
                    b.y = new EasedAnimator(ms, EasedAnimator.Easing.CUBIC_IN_OUT);
                    b.vis.snapTo(animate ? 0f : 1f);
                    float first = Math.max(0f, Math.min(1f, ev.getProgress()));
                    b.fill.snapTo(animate ? 0f : first);
                    b.trail = new EasedAnimator(ms * 2f, EasedAnimator.Easing.CUBIC_OUT);
                    b.trail.snapTo(animate ? 0f : first);
                    b.lastProgress = first;
                    bars.put(b.id, b);
                }
                b.present = true;
                b.progress = Math.max(0f, Math.min(1f, ev.getProgress()));
                if (b.progress < b.lastProgress - 0.002f) {
                    b.trailHoldUntil = now + 320L; // let the loss register before the trail drains
                }
                b.lastProgress = b.progress;
                b.name = ev.getName().getString();
                b.color = BossBarConfig.useBossColor ? colorOf(ev.getColor().name()) : 0xF2F2F2;
                b.notches = notchCount(ev.getOverlay().name());
                b.number = numberText(ev, b.progress, mc, now);
            }
            if (bars.isEmpty()) {
                return;
            }

            // ---- layout of the bars that are currently present ----
            float elemScale = HudText.snapElement(graphics, BossBarConfig.scale);
            float numScale = HudText.snapScale(graphics, BossBarConfig.numberScale, elemScale);
            float nameScale = HudText.snapScale(graphics, BossBarConfig.nameScale, elemScale);
            int barW = BossBarConfig.barWidth;
            int barH = BossBarConfig.barHeight;
            int numH = (int) Math.ceil(font.lineHeight * numScale);
            int nameH = (int) Math.ceil(font.lineHeight * nameScale);

            List<Bar> active = new ArrayList<>();
            for (Bar b : bars.values()) {
                if (b.present) {
                    active.add(b);
                }
            }
            int m = active.size();
            BossNumberLayout layout = BossBarConfig.numberLayout;
            if (layout == BossNumberLayout.ABOVE_BELOW) {
                layout = m == 1 ? BossNumberLayout.ABOVE : (m > 2 ? BossNumberLayout.SIDE : layout);
            }
            boolean anySide = false;
            float widest = font.width("000") * numScale;
            for (int i = 0; i < m; i++) {
                Bar b = active.get(i);
                computeLayout(b, i, m, layout, numH, nameH, barH);
                anySide |= b.numSide;
                widest = Math.max(widest, font.width(b.number) * numScale);
            }
            int barX = anySide ? (int) Math.ceil(widest) + NUMBER_GAP : 0;
            float blockW = barX + barW + 1;
            lastBlockW = blockW;

            int spacing = BossBarConfig.barSpacing;
            int cursor = 0;
            for (Bar b : active) {
                setTarget(b.y, cursor, animate);
                b.yPlaced = true;
                cursor += b.entryH + spacing;
            }
            float totalH = Math.max(0, cursor - spacing);
            if (m > 0) {
                if (!blockHeightSeeded) {
                    blockHeight.snapTo(totalH);
                    blockHeightSeeded = true;
                }
                setTarget(blockHeight, totalH, animate);
            }
            for (Bar b : bars.values()) {
                b.vis.setDurationMs(b.present ? BossBarConfig.style.fadeInMs : BossBarConfig.style.fadeOutMs);
                setTarget(b.vis, b.present ? 1f : 0f, animate);
                if (b.present && Math.abs(b.fill.getTargetValue() - b.progress) > 0.0005f) {
                    setTarget(b.fill, b.progress, animate); // not every frame: only on a real change
                }
            }

            // ---- placement ----
            float scale = elemScale;
            float blockH = blockHeight.update();
            int w = Math.round(blockW * scale);
            int h = Math.round(blockH * scale);
            int ox = BossBarConfig.offsetX;
            int oy = BossBarConfig.offsetY;
            int left;
            int top;
            switch (BossBarConfig.position) {
                case TOP_LEFT -> { left = ox; top = oy; }
                case TOP_RIGHT -> { left = screenWidth - w - ox; top = oy; }
                case BOTTOM_LEFT -> { left = ox; top = screenHeight - h - oy; }
                default -> { left = screenWidth - w - ox; top = screenHeight - h - oy; }
            }
            boolean rightSide = BossBarConfig.position == HudPosition.TOP_RIGHT
                    || BossBarConfig.position == HudPosition.BOTTOM_RIGHT;

            PoseStack pose = graphics.pose();
            pose.pushPose();
            try {
                pose.translate(left, top, 0f);
                pose.scale(scale, scale, 1f);
                float slide = blockW * 0.55f + 20f;
                Iterator<Bar> it = bars.values().iterator();
                while (it.hasNext()) {
                    Bar b = it.next();
                    float vis = b.vis.update();
                    float fillAnim = b.fill.update();
                    float trailAnim = fillAnim;
                    if (BossBarConfig.damageTrail && animate) {
                        if (fillAnim >= b.trail.getCurrentValue()) {
                            b.trail.snapTo(fillAnim); // healing / growing: nothing to trail
                        } else if (now >= b.trailHoldUntil) {
                            b.trail.setTarget(fillAnim);
                        }
                        trailAnim = Math.max(fillAnim, b.trail.update());
                    }
                    float yy = b.y.update();
                    if (!b.present && vis < 0.015f) {
                        it.remove();
                        continue;
                    }
                    if (vis < 0.02f) {
                        continue;
                    }
                    float dx = animate ? (1f - vis) * slide * (rightSide ? 1f : -1f) : 0f;
                    pose.pushPose();
                    try {
                        pose.translate(dx, yy, 0f);
                        drawBar(graphics, font, b, barX, barW, barH, blockW, numScale, nameScale, numH, nameH,
                                vis, fillAnim, trailAnim, now);
                    } finally {
                        pose.popPose();
                    }
                }
                matches.keySet().retainAll(bars.keySet());
            } finally {
                pose.popPose();
            }
        } finally {
            // nothing to reset here: SEEN persists across frames on purpose (see GRACE_MS)
        }
    }

    private static void setTarget(EasedAnimator a, float target, boolean animate) {
        if (animate) {
            a.setTarget(target);
        } else {
            a.snapTo(target);
        }
    }

    /** Which caption rows (above / below the bar) this bar has and what's in them - see {@link BossNumberLayout}. */
    private static void computeLayout(Bar b, int idx, int m, BossNumberLayout layout, int numH, int nameH, int barH) {
        boolean showNumber = BossBarConfig.showNumber;
        BossNameMode nameMode = BossBarConfig.nameMode;
        b.numTop = showNumber && (layout == BossNumberLayout.ABOVE
                || (layout == BossNumberLayout.ABOVE_BELOW && idx == 0));
        b.numBottom = showNumber && (layout == BossNumberLayout.BELOW
                || (layout == BossNumberLayout.ABOVE_BELOW && idx == m - 1 && m > 1));
        b.numSide = showNumber && !b.numTop && !b.numBottom;
        boolean named = nameMode != BossNameMode.HIDDEN;
        b.nameTop = named && (b.numTop || (!b.numBottom && nameMode == BossNameMode.ABOVE));
        b.nameBottom = named && (b.numBottom || (!b.numTop && nameMode == BossNameMode.BELOW));
        b.capTopH = Math.max(b.numTop ? numH : 0, b.nameTop ? nameH : 0);
        b.capBottomH = Math.max(b.numBottom ? numH : 0, b.nameBottom ? nameH : 0);
        b.mainH = Math.max(barH + 4, b.numSide ? numH : 0);
        b.entryH = (b.capTopH > 0 ? b.capTopH + CAPTION_GAP : 0) + b.mainH
                + (b.capBottomH > 0 ? b.capBottomH + CAPTION_GAP : 0);
    }

    // ------------------------------------------------------------------ one bar

    private void drawBar(GuiGraphics g, Font font, Bar b, int barX, int barW, int barH, float blockW,
                         float numScale, float nameScale, int numH, int nameH, float vis, float fillAnim,
                         float trailAnim, long now) {
        float a = BossBarConfig.style.alpha(vis); // fade * the element's overall opacity
        boolean low = BossBarConfig.lowWarning && b.progress <= 0.2f && b.progress > 0f;
        // slow, soft breathing (not a hard blink): it reads as a warning without looking like flicker
        float pulse = low ? 0.5f + 0.5f * (float) Math.sin(now / 320.0) : 0f;
        int color = low ? mix(b.color, 0xFF3B3B, 0.40f + 0.45f * pulse) : b.color;

        // ---- this bar's own backdrop (never one shared box) ----
        drawBackdrop(g, blockW, b.entryH, a, vis);

        int capTopY = 0;
        int mainY = b.capTopH > 0 ? b.capTopH + CAPTION_GAP : 0;
        int capBottomY = mainY + b.mainH + CAPTION_GAP;
        int barY = mainY + (b.mainH - barH) / 2;
        int trackEnd = barX + barW;

        // ---- captions: number left, name right-aligned over the track end ----
        int numArgb = argb(low ? color : 0xFFFFFF, a);
        if (b.numTop) {
            drawText(g, font, b.number, 0, capTopY + (b.capTopH - numH) / 2, numScale, numArgb);
        }
        if (b.numBottom) {
            drawText(g, font, b.number, 0, capBottomY + (b.capBottomH - numH) / 2, numScale, numArgb);
        }
        if (b.numSide) {
            drawText(g, font, b.number, 0, mainY + (b.mainH - numH) / 2, numScale, numArgb);
        }
        if (b.nameTop) {
            drawName(g, font, b, trackEnd, capTopY + (b.capTopH - nameH) / 2, barW, numScale, nameScale, a, b.numTop);
        }
        if (b.nameBottom) {
            drawName(g, font, b, trackEnd, capBottomY + (b.capBottomH - nameH) / 2, barW, numScale, nameScale, a, b.numBottom);
        }

        // ---- track: thin full-length line + end cap = the empty part of the bar ----
        if (BossBarConfig.showTrack) {
            float cy = barY + barH / 2f;
            SoftDraw.rect(g, barX, cy - 0.5f, trackEnd, cy + 0.5f, argb(0xFFFFFF, 0.55f * a));
            SoftDraw.rect(g, trackEnd, barY - 2f, trackEnd + 1f, barY + barH + 2f, argb(0xFFFFFF, 0.8f * a));
        }

        // ---- damage trail: the part the boss just lost, lingering lighter behind the fill, then draining ----
        if (trailAnim > fillAnim + 0.002f) {
            float tx0 = barX + barW * Math.max(0f, Math.min(1f, fillAnim));
            float tx1 = barX + barW * Math.max(0f, Math.min(1f, trailAnim));
            SoftDraw.gradientV(g, tx0, barY, tx1, barY + barH,
                    argb(mix(color, 0xFFFFFF, 0.75f), 0.62f * a), argb(mix(color, 0xFFFFFF, 0.45f), 0.38f * a));
        }

        // ---- fill ----
        float fillW = barW * Math.max(0f, Math.min(1f, fillAnim));
        if (fillW > 0.4f) {
            float glow = BossBarConfig.glowIntensity / 100f * (HudConfig.glowIntensity / 100f);
            float x1 = barX + fillW;
            if (BossBarConfig.barGlow && glow > 0f) {
                SoftDraw.soft(g, barX, barY, fillW, barH, color, 0.32f * glow * a, 4.5f, true);
            }
            // body: deeper at the tail, true colour toward the head
            SoftDraw.gradientH(g, barX, barY, x1, barY + barH,
                    argb(mix(color, 0x000000, 0.42f), a), argb(color, a));
            // sheen along the top half
            SoftDraw.gradientV(g, barX, barY, x1, barY + barH * 0.55f,
                    argb(0xFFFFFF, 0.34f * a), argb(0xFFFFFF, 0f));
            // leading edge: burns toward white, with its own bloom
            float headLen = Math.min(fillW, 5f);
            SoftDraw.gradientH(g, x1 - headLen, barY, x1, barY + barH,
                    argb(0xFFFFFF, 0f), argb(0xFFFFFF, 0.95f * a));
            if (BossBarConfig.headGlow && glow > 0f) {
                SoftDraw.soft(g, x1 - 2f, barY - 0.5f, 2f, barH + 1f, mix(color, 0xFFFFFF, 0.55f),
                        0.85f * glow * a, 5.5f, true);
            }
        }

        // ---- notched boss bars: thin dark gaps ----
        for (int k = 1; k < b.notches; k++) {
            float nx = barX + barW * (k / (float) b.notches);
            SoftDraw.rect(g, nx - 0.45f, barY, nx + 0.45f, barY + barH, argb(0x000000, 0.7f * a));
        }
    }

    private void drawBackdrop(GuiGraphics g, float blockW, int entryH, float a, float vis) {
        BossBackdrop mode = BossBarConfig.backdrop;
        ElementStyle st = BossBarConfig.style;
        float opacity = BossBarConfig.backdropOpacity / 100f;
        float strength = st.panelOpacity / 100f;
        int w = Math.round(blockW) + 8;
        int h = entryH + 6;
        if (mode == BossBackdrop.SHADOW && opacity > 0f) {
            // soft feathered light white-grey drop-off (colour + strength from the style's panel settings)
            SoftDraw.soft(g, -3f, -2f, blockW + 6f, entryH + 4f, st.panelColor, strength * opacity * a, 7f, false);
        } else if (mode == BossBackdrop.BOX && opacity > 0f) {
            HudPanel.draw(g, -4, -3, w, h, st.panelColor, st.panelOpacity * opacity * a);
        }
        // the border type (dots / line / corners ...) applies in every mode except NONE
        if (mode != BossBackdrop.NONE && st.borderStyle != PanelBorderStyle.NONE) {
            HudPanel.drawBorder(g, -4, -3, w, h, st.borderColor, st.borderOpacity * a, st.borderStyle,
                    st.borderLength, st.borderOutset, st.borderScale / 100.0);
        }
    }

    private void drawName(GuiGraphics g, Font font, Bar b, int trackEnd, int y, int barW, float numScale,
                          float nameScale, float a, boolean sharesRowWithNumber) {
        String name = b.name.toUpperCase(Locale.ROOT);
        int numberW = sharesRowWithNumber ? Math.round(font.width(b.number) * numScale) + 6 : 0;
        int maxPx = Math.max(10, Math.round((trackEnd - numberW) / nameScale));
        name = font.plainSubstrByWidth(name, Math.min(maxPx, Math.round(barW / nameScale)));
        int textW = Math.round(font.width(name) * nameScale);
        drawText(g, font, name, trackEnd - textW, y, nameScale, argb(0xB8C0C8, a));
    }

    /** Text at {@code textScale}; the plain 1.0 case skips the pose scale entirely so the pixel font stays sharp.
     *  Skipped below ~6% alpha: vanilla treats a near-zero alpha byte as fully opaque. */
    private static void drawText(GuiGraphics g, Font font, String text, int x, int y, float textScale, int argb) {
        if (((argb >>> 24) & 0xFF) < 16) {
            return;
        }
        if (Math.abs(textScale - 1f) < 0.001f) {
            HudText.text(g, font, text, x, y, argb, true);
            return;
        }
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0f);
        pose.scale(textScale, textScale, 1f);
        HudText.text(g, font, text, 0, 0, argb, true);
        pose.popPose();
    }

    // ------------------------------------------------------------------ number

    private String numberText(LerpingBossEvent event, float progress, Minecraft mc, long now) {
        if (BossBarConfig.numberMode == BossNumberMode.HEALTH) {
            LivingEntity boss = findBoss(event, mc, now);
            if (boss != null) {
                float hp = boss.getHealth();
                return String.valueOf(hp <= 0f ? 0 : Math.max(1, (int) Math.ceil(hp)));
            }
        }
        int percent = progress <= 0f ? 0 : Math.max(1, Math.round(progress * 100f));
        return BossBarConfig.percentSign ? percent + "%" : String.valueOf(percent);
    }

    /** Matches a boss event to the boss entity by display name (the wither / dragon / most mod bosses set the bar's
     *  name from the entity's), nearest to the player first. Cached, and only rescanned once a second while unmatched. */
    private LivingEntity findBoss(LerpingBossEvent event, Minecraft mc, long now) {
        if (mc.level == null || mc.player == null) {
            return null;
        }
        Match match = matches.computeIfAbsent(event.getId(), k -> new Match());
        LivingEntity cached = match.entity;
        if (cached != null && !cached.isRemoved() && cached.isAlive()) {
            return cached;
        }
        match.entity = null;
        if (now < match.nextScanMs) {
            return null;
        }
        match.nextScanMs = now + RESOLVE_INTERVAL_MS;
        String name = event.getName().getString();
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof LivingEntity living && living.getDisplayName().getString().equals(name)) {
                double d = living.distanceToSqr(mc.player);
                if (d < bestDist) {
                    bestDist = d;
                    best = living;
                }
            }
        }
        match.entity = best;
        return best;
    }

    // ------------------------------------------------------------------ helpers

    private static int notchCount(String overlayName) {
        int idx = overlayName.lastIndexOf('_');
        if (idx < 0 || !overlayName.startsWith("NOTCHED")) {
            return 0;
        }
        try {
            return Integer.parseInt(overlayName.substring(idx + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Boss event colour (by enum name) -> a cleaner, modern-HUD tint. */
    private static int colorOf(String name) {
        return switch (name) {
            case "PINK" -> 0xFF6FA8;
            case "BLUE" -> 0x56AEFF;
            case "RED" -> 0xFF5252;
            case "GREEN" -> 0x7BE07B;
            case "YELLOW" -> 0xFFD84A;
            case "PURPLE" -> 0xB58CFF;
            default -> 0xF2F2F2;
        };
    }

    private static int argb(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static int mix(int from, int to, float t) {
        float c = Math.max(0f, Math.min(1f, t));
        int r = Math.round(((from >> 16) & 0xFF) * (1f - c) + ((to >> 16) & 0xFF) * c);
        int g = Math.round(((from >> 8) & 0xFF) * (1f - c) + ((to >> 8) & 0xFF) * c);
        int b = Math.round((from & 0xFF) * (1f - c) + (to & 0xFF) * c);
        return (r << 16) | (g << 8) | b;
    }
}

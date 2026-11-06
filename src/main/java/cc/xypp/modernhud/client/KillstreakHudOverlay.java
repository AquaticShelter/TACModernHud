package cc.xypp.modernhud.client;

import cc.xypp.modernhud.client.compat.ModernLoadoutCompat;
import cc.xypp.modernhud.client.compat.SuperbWarfareCompat;
import cc.xypp.modernhud.config.KillstreakConfig;
import cc.xypp.modernhud.config.PanelBorderStyle;
import cc.xypp.modernhud.config.WeaponConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Killstreak / Specialist tracker, Modern Warfare style, in the weapon widget's own look: NO text, only
 * shapes and light.
 *
 * <ul>
 *   <li><b>Queue column</b> - the rewards not yet earned, as SQUARE slots (weapon-widget panel + corner
 *       brackets) stacked above the weapon widget, the next one at the bottom.</li>
 *   <li><b>Kill bar</b> - a thin segmented bar beside the column: one segment per kill needed for the NEXT
 *       reward. Every kill fills one segment yellow from the bottom; the empty segments are the kills still
 *       to go. Reaching the reward empties it for the following one.</li>
 *   <li><b>Ready row</b> - when a reward is earned its slot glows green and slides out sideways into a
 *       horizontal row, while the slots above it drop down into the freed positions like a queue. Once every
 *       reward is earned the whole group sits in that row, glowing green together, until they are used up
 *       (a used reward fades out of the row; Specialist perks just stay lit while active).</li>
 *   <li><b>Selection display</b> - when the selected streaks/perks change (joining, or the loadout changing
 *       them) the chosen set blinks once above the tracker and fades away.</li>
 * </ul>
 *
 * Everything about the ladder (thresholds with Hardline applied, earned rewards, Specialist stages, partial
 * hostile-mob progress, resets) comes from ModernLoadout via {@link ModernLoadoutCompat}; this class only
 * decides how it looks. Hidden in Spectator and inside SBW vehicles, like the weapon widget.
 */
public class KillstreakHudOverlay implements IGuiOverlay {

    private static final int BAR_W = 4;
    private static final int BAR_GAP = 6;
    private static final long FLASH_MS = 700L;
    private static final long UNLOCK_GRACE_MS = 1500L;
    private static final long SELECTION_FADE_MS = 800L;
    private static final long SELECTION_BLINK_MS = 720L;
    private static final int MAX_SEGMENTS = 30;

    private enum State { LOCKED, NEXT, READY, ACTIVE, USED }

    /** Per-rung eased position (cluster space) and visibility, so slots slide into the row / drop down the
     *  queue instead of jumping. Rebuilt whenever the ladder itself changes. */
    private EasedAnimator[] ax = new EasedAnimator[0];
    private EasedAnimator[] ay = new EasedAnimator[0];
    private EasedAnimator[] av = new EasedAnimator[0];
    private State[] states = new State[0];
    private String animatedLadder = "";
    /** Auto-hide: 0 = pushed out / hidden, 1 = shown. Always 1 when auto-hide is off (item streaks only). */
    private final EasedAnimator groupVis = new EasedAnimator(460f, EasedAnimator.Easing.CUBIC_IN_OUT);

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        // Tell ModernLoadout whether we are the ones drawing (its own tiles/meter then stay off). Only
        // touches the API when the value changes, and does nothing at all without ModernLoadout.
        if (!ModernLoadoutCompat.loaded()) {
            return;
        }
        boolean wantsToDraw = KillstreakConfig.enabled;
        ModernLoadoutCompat.setExternalHud(wantsToDraw);
        if (!wantsToDraw) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator() || SuperbWarfareCompat.isInVehicle(player)) {
            return;
        }
        if (!ModernLoadoutCompat.refresh()) {
            return;
        }
        int n = ModernLoadoutCompat.rungCount();
        // Specialist: the final 8-kill stage is not drawn as a 4th slot (it only completes the set); the rungs are
        // ordered by kills so it is always the last one.
        if (ModernLoadoutCompat.isSpecialist() && KillstreakConfig.specialistHideFinalStage && n > 1
                && ModernLoadoutCompat.kills(n - 1) >= 8) {
            n--;
        }
        long now = System.currentTimeMillis();
        ensureAnimators(n, ModernLoadoutCompat.ladderKey());

        int slot = KillstreakConfig.slotSize;
        int gap = KillstreakConfig.slotGap;
        int columnH = n * slot + (n - 1) * gap;

        // Auto-hide (always on for Specialist: once equipped it only shows while progress is being made):
        // after autoHideSeconds with no change the tracker slides out and fades, then pushes back in on the next
        // change. Item rewards that are ready to use keep it on screen so you never lose track of them.
        boolean specialist = ModernLoadoutCompat.isSpecialist();
        float group = 1f;
        if (specialist || KillstreakConfig.autoHide) {
            long holdMs = KillstreakConfig.autoHideSeconds * 1000L;
            boolean showNow;
            boolean full = specialist && KillstreakConfig.specialistStopWhenFull && n > 0;
            long fullAt = 0L;
            if (full) {
                for (int i = 0; i < n; i++) {
                    if (!ModernLoadoutCompat.earned(i)) {
                        full = false;
                        break;
                    }
                    fullAt = Math.max(fullAt, ModernLoadoutCompat.earnedAtMs(i));
                }
            }
            if (full) {
                // set complete: show the completion once (from the moment the last perk landed), then stay away -
                // later kills no longer bring the tracker back
                showNow = fullAt > 0 && now - fullAt < holdMs;
            } else {
                showNow = now - ModernLoadoutCompat.lastChangeMs() < holdMs
                        || (!specialist && ModernLoadoutCompat.anyReady());
            }
            groupVis.setDurationMs(showNow ? KillstreakConfig.slotStyle.fadeInMs : KillstreakConfig.slotStyle.fadeOutMs);
            groupVis.setTarget(showNow ? 1f : 0f);
            group = groupVis.update();
        } else {
            groupVis.snapTo(1f);
        }
        float slidePx = (1f - group) * (slot + BAR_W + BAR_GAP);

        // ---- placement: pose origin = bottom-right corner of the cluster ----
        float scale;
        float originX;
        float originY;
        if (KillstreakConfig.attachToWeaponHud) {
            double weaponScale = WeaponHudOverlay.effectiveScale(graphics);
            scale = HudText.snapElement(graphics, weaponScale * KillstreakConfig.scaleFactor);
            int totalW = WeaponConfig.frameWidth + WeaponHudOverlay.gearWidth();
            int[] pos = WeaponHudOverlay.resolvePosition(screenWidth, screenHeight,
                    totalW, WeaponConfig.frameHeight, weaponScale);
            originX = (float) (pos[0] + totalW * weaponScale);
            originY = (float) (pos[1] - KillstreakConfig.groupGap * weaponScale);
        } else {
            scale = HudText.snapElement(graphics, KillstreakConfig.scaleFactor);
            int w = Math.round((BAR_W + BAR_GAP + n * slot + (n - 1) * gap) * scale);
            int h = Math.round(columnH * scale);
            int ox = KillstreakConfig.offsetX;
            int oy = KillstreakConfig.offsetY;
            int left;
            int top;
            switch (KillstreakConfig.position) {
                case TOP_LEFT -> { left = ox; top = oy; }
                case TOP_RIGHT -> { left = screenWidth - w - ox; top = oy; }
                case BOTTOM_LEFT -> { left = ox; top = screenHeight - h - oy; }
                default -> { left = screenWidth - w - ox; top = screenHeight - h - oy; }
            }
            originX = left + w;
            originY = top + h;
        }

        // ---- targets: locked rungs queue up the column (next at the bottom), earned ones line up in the row ----
        int next = -1;
        int locked = 0;
        long lastEarnedMs = 0;
        for (int i = 0; i < n; i++) {
            if (!ModernLoadoutCompat.earned(i)) {
                if (next < 0) {
                    next = i;
                }
                locked++;
            } else {
                lastEarnedMs = Math.max(lastEarnedMs, ModernLoadoutCompat.earnedAtMs(i));
            }
        }
        int colRight = -(BAR_W + BAR_GAP);
        int colLeft = colRight - slot;
        int rowRight = locked == 0 ? colRight : colLeft - gap; // the row slides over the emptied column
        int queuePos = 0;
        int rowPos = 0;
        float ams = KillstreakConfig.animationMs;
        for (int i = 0; i < n; i++) {
            ax[i].setDurationMs(ams);
            ay[i].setDurationMs(ams);
            av[i].setDurationMs(ams * 0.62f);
            State st = stateOf(i, i == next, now);
            states[i] = st;
            if (!ModernLoadoutCompat.earned(i)) {
                ax[i].setTarget(colLeft);
                ay[i].setTarget(-(queuePos + 1) * slot - queuePos * gap);
                av[i].setTarget(1f);
                queuePos++;
            } else if (st == State.USED) {
                av[i].setTarget(0f); // consumed: fades out where it stands, the rest of the row closes up
            } else {
                ax[i].setTarget(rowRight - (rowPos + 1) * slot - rowPos * gap);
                ay[i].setTarget(-slot);
                av[i].setTarget(1f);
                rowPos++;
            }
        }

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(originX, originY, 0f);
        pose.scale(scale, scale, 1f);
        try {
            pose.pushPose();
            try {
                pose.translate(slidePx, 0f, 0f); // push in / out: slide toward the screen edge while fading
                if (group >= 0.05f && KillstreakConfig.showProgressBar && next >= 0) {
                    drawBar(graphics, next, slot, group * opacityMul());
                }
                drawSlots(graphics, n, slot, group, locked == 0, lastEarnedMs, now);
            } finally {
                pose.popPose();
            }
            // In Specialist mode the perk toast already shows the selected perks - don't repeat them as a blink too.
            boolean toastCovers = specialist && KillstreakConfig.perkToast && ModernLoadoutCompat.perksAvailable();
            if (KillstreakConfig.showSelection && !toastCovers) {
                drawSelection(graphics, n, slot, gap, columnH, now);
            }
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            pose.popPose();
        }
    }

    private void drawSlots(GuiGraphics graphics, int n, int slot, float group, boolean allEarned, long lastEarnedMs, long now) {
        {
            for (int i = 0; i < n; i++) {
                float x = ax[i].update();
                float y = ay[i].update();
                float vis = av[i].update();
                float a = vis * group * opacityMul();
                if (a < 0.03f) {
                    continue;
                }
                long ownEarned = ModernLoadoutCompat.earnedAtMs(i);
                long flashStart = ownEarned;
                if (allEarned && lastEarnedMs > 0) {
                    flashStart = lastEarnedMs; // the whole group flares together when the last one lands
                }
                float flash = flashStart > 0 && now - flashStart < FLASH_MS ? 1f - (now - flashStart) / (float) FLASH_MS : 0f;
                drawSlot(graphics, i, states[i], Math.round(x), Math.round(y), slot, a, now, flash);
            }
        }
    }

    private void ensureAnimators(int n, String ladderKey) {
        if (n == ax.length && ladderKey.equals(animatedLadder)) {
            return;
        }
        animatedLadder = ladderKey;
        ax = new EasedAnimator[n];
        ay = new EasedAnimator[n];
        av = new EasedAnimator[n];
        states = new State[n];
        for (int i = 0; i < n; i++) {
            // ease-in-out: a slot leaves the queue and joins the ready row (and the queue drops) with a soft
            // start AND a soft landing, rather than snapping off the line and braking late
            ax[i] = new EasedAnimator(420f, EasedAnimator.Easing.CUBIC_IN_OUT);
            ay[i] = new EasedAnimator(420f, EasedAnimator.Easing.CUBIC_IN_OUT);
            av[i] = new EasedAnimator(260f, EasedAnimator.Easing.CUBIC_IN_OUT);
        }
    }

    private static State stateOf(int rung, boolean isNext, long now) {
        if (ModernLoadoutCompat.earned(rung)) {
            if (ModernLoadoutCompat.isSpecialist()) {
                return State.ACTIVE;
            }
            // The item lands in the inventory a moment after the packet that says "earned" - don't read that
            // gap as "already used".
            boolean justEarned = now - ModernLoadoutCompat.earnedAtMs(rung) < UNLOCK_GRACE_MS
                    && ModernLoadoutCompat.earnedAtMs(rung) > 0;
            return ModernLoadoutCompat.held(rung) || justEarned ? State.READY : State.USED;
        }
        return isNext ? State.NEXT : State.LOCKED;
    }

    // ------------------------------------------------------------------ kill bar

    /** Bare bar (no box, no border, no backing) beside the bottom slot: one segment per kill still needed for the
     *  NEXT reward ONLY - the gap from the previous reward's kill count, not the running total. It starts empty
     *  for each reward, fills a segment per kill, and refreshes empty for the following one once the reward is
     *  earned. As tall as one slot, bottom-aligned with it, at the cluster's right edge. */
    private static void drawBar(GuiGraphics g, int next, int slot, float fade) {
        int previous = next > 0 ? ModernLoadoutCompat.kills(next - 1) : 0;
        int needed = Math.max(1, ModernLoadoutCompat.kills(next) - previous);
        int segs = Math.min(needed, MAX_SEGMENTS);
        float gained = Math.max(0f, Math.min(needed, ModernLoadoutCompat.progress() - previous));
        float units = gained * segs / needed;
        int whole = (int) Math.floor(units + 1e-3f);
        float partial = units - whole;

        int gap = segs * 3 <= slot ? 2 : (segs * 2 <= slot ? 1 : 0);
        int segH = Math.max(1, (slot - (segs - 1) * gap) / segs);
        int color = KillstreakConfig.barColor;
        for (int j = 0; j < segs; j++) {
            int bottom = -j * (segH + gap);
            int top = bottom - segH;
            if (j < whole) {
                g.fill(-BAR_W, top, 0, bottom, argb(color, fade));
            } else {
                g.fill(-BAR_W, top, 0, bottom, argb(color, 0.28f * fade));
                if (j == whole && partial > 0.02f) {
                    int h = Math.max(1, Math.round(segH * partial)); // a partial mob point fills part of one segment
                    g.fill(-BAR_W, bottom - h, 0, bottom, argb(color, 0.85f * fade));
                }
            }
        }
    }

    // ------------------------------------------------------------------ slots

    private void drawSlot(GuiGraphics g, int rung, State st, int x, int y, int size, float a, long now, float flash) {
        int ready = KillstreakConfig.readyColor;
        int active = KillstreakConfig.activeColor;
        int accent = KillstreakConfig.barColor;
        float pulse = 0.5f + 0.5f * (float) Math.sin(now / 260.0);

        if (panelOn()) {
            HudPanel.draw(g, x, y, size, size, panelColor(), panelOpacity() * a);
        }
        if (st == State.READY) {
            HudPanel.draw(g, x, y, size, size, ready, (16 + 16 * pulse) * a);
        } else if (st == State.ACTIVE) {
            HudPanel.draw(g, x, y, size, size, active, 15 * a);
        }
        if (borderOn()) {
            int border = switch (st) {
                case READY -> ready;
                case ACTIVE -> active;
                case NEXT -> accent;
                default -> borderColor();
            };
            double opacity = switch (st) {
                case READY, ACTIVE -> 100.0;
                case NEXT -> Math.max(borderOpacity(), 80.0);
                case USED -> borderOpacity() * 0.5;
                default -> borderOpacity() * 0.8;
            };
            HudPanel.drawBorder(g, x, y, size, size, border, opacity * a,
                    borderStyle(), borderLength(), borderOutset(), borderScale());
        }

        float iconAlpha = switch (st) {
            case LOCKED -> 0.38f;
            case NEXT -> 0.78f;
            case USED -> 0.30f;
            default -> 1f;
        };
        drawIcon(g, rung, x, y, size, iconAlpha * a, st == State.LOCKED || st == State.NEXT || st == State.USED);

        if (st == State.READY) {
            GlowEffect.drawGlowRect(g, x, y, size, size, ready, (0.24f + 0.34f * pulse) * a, 3f);
        } else if (st == State.ACTIVE) {
            GlowEffect.drawGlowRect(g, x, y, size, size, active, 0.22f * a, 3f);
        }
        if (flash > 0.01f) {
            GlowEffect.drawGlowRect(g, x, y, size, size, 0xFFFFFF, 0.6f * flash * a, 4f);
        }
    }

    /** Icon centred in a square slot: a perk texture, an item model, or - for Specialist's final "all perks"
     *  stage, which has neither - four small squares. Item models ignore the shader colour's alpha, so a dimmed
     *  item gets a translucent cover instead. */
    private static void drawIcon(GuiGraphics g, int rung, int x, int y, int size, float alpha, boolean dimWithCover) {
        int icon = size - 6;
        int ix = x + (size - icon) / 2;
        int iy = y + (size - icon) / 2;
        ResourceLocation texture = ModernLoadoutCompat.texture(rung);
        ItemStack stack = ModernLoadoutCompat.icon(rung);
        if (texture != null) {
            float[] prev = RenderSystem.getShaderColor();
            float pr = prev[0], pg = prev[1], pb = prev[2], pa = prev[3];
            try {
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1f, 1f, 1f, Math.min(1f, alpha));
                g.blit(texture, ix, iy, icon, icon, 0, 0, 24, 24, 24, 24);
            } finally {
                RenderSystem.setShaderColor(pr, pg, pb, pa);
            }
        } else if (!stack.isEmpty()) {
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate(ix, iy, 0f);
            float s = icon / 16f;
            pose.scale(s, s, 1f);
            boolean dim = dimWithCover && alpha < 1f;
            if (dim && !KillstreakConfig.iconBackdrop) {
                // NO background of any kind: a dimmed item is simply drawn a little darker, the slot box behind it stays as it is
                float[] prev = RenderSystem.getShaderColor();
                float k = 1f - 0.65f * (1f - Math.min(1f, alpha));
                try {
                    RenderSystem.setShaderColor(k, k, k, 1f);
                    g.renderItem(stack, 0, 0);
                } finally {
                    RenderSystem.setShaderColor(prev[0], prev[1], prev[2], prev[3]);
                }
                pose.popPose();
            } else {
                g.renderItem(stack, 0, 0);
                pose.popPose();
                if (dim) { // old look: a translucent dark square over the item
                    int cover = Math.max(0, Math.min(255, Math.round((1f - Math.min(1f, alpha)) * 255)));
                    g.fill(ix, iy, ix + icon, iy + icon, (cover << 24) | 0x0A0D11);
                }
            }
        } else {
            int cell = Math.max(2, icon / 2 - 2);
            int space = Math.max(1, icon - 2 * cell);
            int base = argb(0xE6E9ED, Math.min(1f, Math.max(alpha, 0.35f)));
            for (int r = 0; r < 2; r++) {
                for (int c = 0; c < 2; c++) {
                    int cx = ix + c * (cell + space);
                    int cy = iy + r * (cell + space);
                    g.fill(cx, cy, cx + cell, cy + cell, base);
                }
            }
        }
    }

    // ------------------------------------------------------------------ selection display

    /** The chosen set, blinking in once above the tracker and fading away - shown when the selection (ladder)
     *  changes, never again until it does. */
    private static void drawSelection(GuiGraphics g, int n, int slot, int gap, int columnH, long now) {
        long start = ModernLoadoutCompat.selectionStartMs();
        if (start <= 0) {
            return;
        }
        long elapsed = now - start;
        long hold = KillstreakConfig.selectionSeconds * 1000L;
        if (elapsed >= hold + SELECTION_FADE_MS) {
            return;
        }
        float a = (elapsed < hold ? 1f : 1f - (elapsed - hold) / (float) SELECTION_FADE_MS) * opacityMul();
        float blink = elapsed < SELECTION_BLINK_MS ? ((elapsed / 120) % 2 == 0 ? 1f : 0.3f) : 1f;
        int accent = KillstreakConfig.barColor;
        int y = -columnH - 8 - slot;
        for (int i = 0; i < n; i++) {
            int x = -(n - i) * slot - (n - i - 1) * gap;
            float s = a * blink;
            if (panelOn()) {
                HudPanel.draw(g, x, y, slot, slot, panelColor(), panelOpacity() * a);
            }
            if (borderOn()) {
                HudPanel.drawBorder(g, x, y, slot, slot, accent, 100.0 * s,
                        borderStyle(), borderLength(), borderOutset(), borderScale());
            }
            drawIcon(g, i, x, y, slot, a, false);
            GlowEffect.drawGlowRect(g, x, y, slot, slot, accent, 0.45f * s, 3f);
        }
    }

    // ------------------------------------------------------------------ style (KillstreakConfig.slotStyle)

    private static float opacityMul() {
        return KillstreakConfig.slotStyle.opacity / 100f;
    }

    private static boolean panelOn() {
        return KillstreakConfig.slotStyle.panelEnabled;
    }

    private static boolean borderOn() {
        return true; // the border TYPE (NONE ... DOTS) decides
    }

    private static int panelColor() {
        return KillstreakConfig.slotStyle.panelColor;
    }

    private static double panelOpacity() {
        return KillstreakConfig.slotStyle.panelOpacity;
    }

    private static int borderColor() {
        return KillstreakConfig.slotStyle.borderColor;
    }

    private static double borderOpacity() {
        return KillstreakConfig.slotStyle.borderOpacity;
    }

    private static PanelBorderStyle borderStyle() {
        return KillstreakConfig.slotStyle.borderStyle;
    }

    private static int borderLength() {
        return KillstreakConfig.slotStyle.borderLength;
    }

    private static int borderOutset() {
        return KillstreakConfig.slotStyle.borderOutset;
    }

    private static double borderScale() {
        return KillstreakConfig.slotStyle.borderScale / 100.0;
    }

    // ------------------------------------------------------------------ colour helpers

    private static int argb(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}

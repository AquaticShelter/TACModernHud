package cc.xypp.modernhud.client;

import cc.xypp.modernhud.client.compat.LrTacticalCompat;
import cc.xypp.modernhud.client.render.SdfShapes;
import cc.xypp.modernhud.config.ElementStyle;
import cc.xypp.modernhud.config.PanelBorderStyle;
import cc.xypp.modernhud.config.SpentMarker;
import cc.xypp.modernhud.config.WeaponConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The extra "gear" column of the weapon widget, to the right of the primary weapon box (Modern Warfare layout):
 * on top ONE wide slot with the lrtactical MELEE item found in the hotbar, below it TWO small slots tracking
 * lrtactical THROWABLE / CONSUMABLE items found in the hotbar (at most two tracked items). A tracked slot keeps
 * its place when its item runs out: it shows an X (hitmarker style, empty centre) and the count reads 00, until a
 * different item in stock takes the slot over by itself.
 * Two layouts: tall next to the primary box while a gun (or any gear item) is in hand, compact beside the two idle
 * weapon strips otherwise. Outline = small square dots.
 */
final class LoadoutGear {

    /** Gap between the primary weapon box and the gear column. */
    static final int GAP = 2;
    /** Slightly wider than the primary box, as in the reference (so two squares + the gap fit exactly). */
    private static final int COLUMN_W = WeaponHudOverlay.ICON_BOX_W + 2;
    private static final int V_GAP = WeaponHudOverlay.SECONDARY_GAP;
    private static final int H_GAP = 2;
    /** Bottom edge shared with the secondary weapon strip (active layout). */
    private static final int BOTTOM = WeaponHudOverlay.ICON_BOX_H + WeaponHudOverlay.SECONDARY_GAP
            + WeaponHudOverlay.SECONDARY_BOX_H;
    /** The two lower slots are exactly SQUARE (side = half the column minus the gap: 32x32 with the defaults) ... */
    private static final int SQUARE = (COLUMN_W - H_GAP) / 2;
    /** ... and the melee strip takes what is left above them (16, i.e. about half the primary box, like the reference). */
    private static final int MELEE_H = BOTTOM - V_GAP - SQUARE;
    private static final int TL = 1, TR = 2, BL = 4, BR = 8;
    private static final int SCAN_MS = 200;
    private static final int NORMAL = 0xE8ECF0;

    private static final class Tracked {
        String key;
        ItemStack icon = ItemStack.EMPTY;
        int count;
    }

    private static final Tracked[] SLOTS = {new Tracked(), new Tracked()};
    private static ItemStack melee = ItemStack.EMPTY;
    /** Last melee item seen, kept so its slot can fade OUT after the item is gone. */
    private static ItemStack meleeShown = ItemStack.EMPTY;
    /** Per-slot fade (0 = hidden, 1 = shown): index 0 = melee, 1 / 2 = the two tracked slots. */
    private static final EasedAnimator[] SLOT_VIS = {
            new EasedAnimator(220f, EasedAnimator.Easing.CUBIC_IN_OUT),
            new EasedAnimator(220f, EasedAnimator.Easing.CUBIC_IN_OUT),
            new EasedAnimator(220f, EasedAnimator.Easing.CUBIC_IN_OUT)};
    private static long lastScan = -1000L;
    private static long lastDraw = -1000L;
    /** Eased width of the whole column incl. the gap (0 = nothing to show: the weapon box sits in the corner). */
    private static final EasedAnimator WIDTH = new EasedAnimator(220f, EasedAnimator.Easing.CUBIC_IN_OUT);
    private static long lastLayout = -1000L;
    /** 0 = hidden (idle: no gun in hand), 1 = shown. */
    private static final EasedAnimator VIS = new EasedAnimator(220f, EasedAnimator.Easing.CUBIC_IN_OUT);

    static {
        VIS.snapTo(0f);
        WIDTH.snapTo(0f);
        for (EasedAnimator a : SLOT_VIS) {
            a.snapTo(0f);
        }
    }

    private LoadoutGear() {
    }

    static boolean enabled() {
        return WeaponConfig.showGear && LrTacticalCompat.loaded();
    }

    /**
     * Extra width the gear column adds to the widget frame (0 when it is off / lrtactical is absent). With auto layout
     * this is the EASED width of just the slots in use, so the weapon box glides into the corner when nothing is carried.
     */
    static int extraWidth() {
        if (!enabled()) {
            return 0;
        }
        if (!WeaponConfig.gearAutoLayout) {
            return GAP + COLUMN_W;
        }
        float w = WIDTH.update();
        return w < 0.5f ? 0 : Math.round(w);
    }

    /** Number of throwable / consumable slots that count as shown (a spent one is hidden while idle). */
    private static int wantedLower(boolean idle) {
        int n = 0;
        for (Tracked t : SLOTS) {
            if (isWanted(t, idle)) {
                n++;
            }
        }
        return n;
    }

    private static boolean isWanted(Tracked t, boolean idle) {
        return t.key != null && !(idle && t.count <= 0);
    }

    /** Width of the column content right now: full for a melee strip, else just the throwable squares. */
    private static int contentWidth(boolean idle) {
        if (!melee.isEmpty()) {
            return COLUMN_W;
        }
        int n = wantedLower(idle);
        return n == 0 ? 0 : n == 1 ? (COLUMN_W - H_GAP) / 2 : COLUMN_W;
    }

    /**
     * Scans the hotbar and re-targets the column width. Call BEFORE the widget position is resolved (every frame the
     * widget is drawn) so the frame width already reflects which slots exist.
     */
    static void update(LocalPlayer player, boolean idle) {
        scan(player);
        long now = Util.getMillis();
        int content = contentWidth(idle);
        float target = content == 0 ? 0f : GAP + content;
        if (now - lastLayout > 300L) {
            WIDTH.snapTo(target); // widget just appeared: no slide from the old width
        }
        lastLayout = now;
        WIDTH.setDurationMs(WeaponConfig.gearStyle.fadeInMs);
        WIDTH.setTarget(target);
    }

    /** Re-scans the hotbar a few times a second. */
    private static void scan(LocalPlayer player) {
        long now = Util.getMillis();
        if (now - lastScan < SCAN_MS) {
            return;
        }
        lastScan = now;
        Inventory inv = player.getInventory();
        int limit = WeaponConfig.gearCountWholeInventory ? inv.items.size() : 9;
        ItemStack meleeFound = ItemStack.EMPTY;
        ItemStack shieldFound = ItemStack.EMPTY;
        Map<String, ItemStack> found = new LinkedHashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < limit; i++) {
            ItemStack s = inv.getItem(i);
            LrTacticalCompat.Kind kind = LrTacticalCompat.kind(s);
            if (kind == LrTacticalCompat.Kind.NONE) {
                continue;
            }
            if (kind == LrTacticalCompat.Kind.MELEE) {
                if (i < 9) {
                    if (LrTacticalCompat.isShield(s)) {
                        if (shieldFound.isEmpty()) {
                            shieldFound = s; // flash shield: melee slot, but a real melee weapon wins
                        }
                    } else if (meleeFound.isEmpty()) {
                        meleeFound = s;
                    }
                }
                continue;
            }
            String key = LrTacticalCompat.key(s);
            counts.merge(key, s.getCount(), Integer::sum);
            // every item in the scanned range can take over a spent / empty slot (hotbar first: it comes first in the list)
            found.putIfAbsent(key, s);
        }
        ItemStack chosen = meleeFound.isEmpty() ? shieldFound : meleeFound;
        melee = chosen.isEmpty() ? ItemStack.EMPTY : chosen.copy();

        for (Tracked t : SLOTS) {
            if (t.key == null) {
                continue;
            }
            ItemStack f = found.remove(t.key);
            if (f != null) {
                t.icon = f.copy();
            }
            t.count = counts.getOrDefault(t.key, 0);
        }
        for (Map.Entry<String, ItemStack> e : found.entrySet()) {
            Tracked target = null;
            for (Tracked t : SLOTS) {
                if (t.key == null) {
                    target = t;
                    break;
                }
            }
            if (target == null) {
                for (Tracked t : SLOTS) {
                    if (t.count <= 0) { // a spent slot gives way to a newly found item
                        target = t;
                        break;
                    }
                }
            }
            if (target == null) {
                break; // both slots hold something still in stock: max 2 tracked items
            }
            target.key = e.getKey();
            target.icon = e.getValue().copy();
            target.count = counts.getOrDefault(e.getKey(), e.getValue().getCount());
        }
    }

    /** True when {@code stack} is a lrtactical melee / throwable / consumable item (and the gear column is on). */
    static boolean isGearItem(ItemStack stack) {
        return enabled() && LrTacticalCompat.kind(stack) != LrTacticalCompat.Kind.NONE;
    }

    /**
     * @param x0   left edge of the column in widget-local units
     * @param idle false = a gun (or a gear item) is in hand: tall layout next to the primary box;
     *             true = idle: compact layout beside the two idle weapon strips
     */
    static void draw(GuiGraphics g, int x0, boolean idle) {
        ElementStyle st = WeaponConfig.gearStyle;
        long now = Util.getMillis();
        if (now - lastDraw > 300L) {
            VIS.snapTo(0f); // not drawn for a while: start hidden, so it fades in instead of popping
        }
        lastDraw = now;
        VIS.setDurationMs(st.fadeInMs);
        VIS.setTarget(1f);
        float vis = VIS.update();
        float a = st.alpha(vis);
        if (a <= 0.01f) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int secH = WeaponHudOverlay.SECONDARY_BOX_H;

        // vertical layout of the two rows
        int meleeY = idle ? (WeaponHudOverlay.ICON_BOX_H - secH) / 2 : 0;
        int meleeH = idle ? secH : MELEE_H;
        int lowerY = idle ? meleeY + secH + V_GAP : BOTTOM - SQUARE;
        int lowerH = idle ? secH : SQUARE;
        int w0 = (COLUMN_W - H_GAP) / 2;
        int w1 = COLUMN_W - H_GAP - w0;

        // each slot fades in when its item shows up and out when it is gone (idle: a spent slot fades away too;
        // with a gun in hand it stays as a dimmed 00)
        if (!melee.isEmpty()) {
            meleeShown = melee;
        }
        EasedAnimator mv = SLOT_VIS[0];
        mv.setDurationMs(melee.isEmpty() ? st.fadeOutMs : st.fadeInMs);
        mv.setTarget(melee.isEmpty() ? 0f : 1f);
        float meleeVis = mv.update();

        // ---- melee: one flat wide slot on top, dots only on its right side (top-right, bottom-right) ----
        if (meleeVis > 0.01f && !meleeShown.isEmpty()) {
            float av = vis * meleeVis;
            float aa = st.alpha(av);
            box(g, x0, meleeY, COLUMN_W, meleeH, TR | BR, st, av);
            int iconPx = 14;
            float ix = WeaponConfig.gearCenterIcons ? x0 + (COLUMN_W - iconPx) / 2f : x0 + COLUMN_W - iconPx - 4f;
            float iy = meleeY + (meleeH - iconPx) / 2f;
            if (WeaponConfig.gearGlow) {
                GlowEffect.drawBloom(g, ix + iconPx / 2f, iy + iconPx / 2f, 12f, NORMAL, 0.24f * aa);
            }
            drawItem(g, meleeShown, ix, iy, iconPx / 16f, aa);
        }

        // ---- throwable / consumable ----
        int packed = 0; // position among the slots in use: with auto layout a missing slot never leaves a hole
        for (int i = 0; i < SLOTS.length; i++) {
            Tracked t = SLOTS[i];
            boolean spent = t.count <= 0;
            boolean present = t.key != null && !(idle && spent);
            int pos = WeaponConfig.gearAutoLayout ? packed : i;
            if (present) {
                packed++;
            }
            EasedAnimator sv = SLOT_VIS[i + 1];
            sv.setDurationMs(present ? st.fadeInMs : st.fadeOutMs);
            sv.setTarget(present ? 1f : 0f);
            float slotVis = sv.update();
            if (t.key == null || slotVis <= 0.01f) {
                continue;
            }
            float av = vis * slotVis;
            float aa = st.alpha(av);
            int w = pos == 0 ? w0 : w1;
            int x = x0 + (pos == 0 ? 0 : w0 + H_GAP);
            // dots: both bottom corners plus ONE top dot, on opposite sides for the two slots
            int top = (pos == 0) != WeaponConfig.gearTopDotInner ? TL : TR;
            box(g, x, lowerY, w, lowerH, BL | BR | top, st, av);

            float scale = idle ? 0.75f : 1.0f;
            int iconPx = Math.round(16 * scale);
            boolean asX = spent && WeaponConfig.gearSpentMarker == SpentMarker.X;
            // the count text is needed first: in the compact row the icon and the count are centred together
            int shown = Math.min(99, Math.max(0, t.count));
            String text = shown < 10 ? "0" + shown : Integer.toString(shown);
            boolean centre = WeaponConfig.gearCenterIcons;
            float ix;
            float iy;
            if (idle) {
                if (centre) {
                    float groupW = iconPx + 2f + font.width(text);
                    ix = x + (w - groupW) / 2f;
                } else {
                    ix = x + 2f;                   // icon left, count right: one compact row
                }
                iy = lowerY + (lowerH - iconPx) / 2f;
            } else if (centre) {
                ix = x + (w - iconPx) / 2f;        // icon in the middle of the box, count bottom-left
                iy = lowerY + (lowerH - iconPx) / 2f;
            } else {
                ix = x + w - iconPx - 3f;          // icon top-right, count bottom-left
                iy = lowerY + 3f;
            }
            if (WeaponConfig.gearGlow && !spent) {
                GlowEffect.drawBloom(g, ix + iconPx / 2f, iy + iconPx / 2f, 12f, NORMAL, 0.24f * aa);
            }
            if (asX) {
                int xa = Math.max(0, Math.min(255, Math.round(aa * 0.9f * 255f)));
                int xargb = (xa << 24) | (WeaponConfig.gearSpentColor & 0xFFFFFF);
                if (xa >= 4 && !SdfShapes.cross(g, ix, iy, iconPx, xargb)) {
                    drawSpentX(g, Math.round(ix), Math.round(iy), iconPx, WeaponConfig.gearSpentColor, aa * 0.9f); // fallback
                }
            } else {
                drawItem(g, t.icon, ix, iy, scale, spent ? aa * 0.28f : aa); // spent: dimmed icon
            }

            // the count: same size / colour as the reserve counter of the secondary strip
            int ta = Math.max(1, Math.round(Math.min(1f, aa) * 255f));
            int color = (ta << 24) | (WeaponConfig.reserveColor & 0xFFFFFF);
            int tx;
            int ty;
            if (idle) {
                tx = Math.round(ix) + iconPx + 2;
                ty = lowerY + (lowerH - font.lineHeight) / 2;
            } else {
                tx = x + WeaponHudOverlay.CHIP_PAD_X;
                ty = lowerY + lowerH - font.lineHeight - 2;
            }
            HudText.text(g, font, text, tx, ty, color, true);
        }
    }

    /**
     * The "item lost" mark: four short diagonal strokes pointing at the centre, with the centre left EMPTY (the strokes
     * never cross), like a hitmarker. Pixel exact: every stroke is a run of whole-pixel squares.
     */
    private static void drawSpentX(GuiGraphics g, int x, int y, int size, int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        if (a < 4) {
            return;
        }
        final int c = (a << 24) | (rgb & 0xFFFFFF);
        final int s = Math.max(6, size);
        final int t = s >= 24 ? 2 : 1;                 // stroke thickness: a hairline up to 23 px
        final int gap = Math.max(2, s / 5);            // the empty centre
        final int arm = Math.max(gap + 2, s / 2 - 1);  // how far the strokes reach towards the corners
        final int cx = x + s / 2;
        final int cy = y + s / 2;
        HudBatch.run(g, () -> {
            for (int k = gap; k <= arm; k++) {
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sy = -1; sy <= 1; sy += 2) {
                        int px = cx + sx * k - (sx < 0 ? t - 1 : 0);
                        int py = cy + sy * k - (sy < 0 ? t - 1 : 0);
                        g.fill(px, py, px + t, py + t, c);
                    }
                }
            }
        });
    }

    /** A flat panel (like the secondary strip) with its square dots on the given corners. */
    private static void box(GuiGraphics g, int x, int y, int w, int h, int corners, ElementStyle st, float vis) {
        float a = st.alpha(vis);
        if (st.panelEnabled) {
            HudPanel.draw(g, x, y, w, h, st.panelColor, st.panelOpacity * a);
        }
        PanelBorderStyle bs = st.borderStyle;
        if (bs == PanelBorderStyle.NONE) {
            return;
        }
        if (bs == PanelBorderStyle.DOTS || bs == PanelBorderStyle.BOTTOM_DOTS || bs == PanelBorderStyle.FRAME) {
            if (bs == PanelBorderStyle.FRAME) {
                HudPanel.drawBorder(g, x, y, w, h, st.borderColor, st.borderOpacity * a * 0.35, PanelBorderStyle.FULL, 1, 0);
            }
            // same dots as the secondary weapon strip: solid squares tucked INSIDE the corners
            int size = Math.min(Math.max(1, (int) Math.round(st.borderLength * st.borderScale / 100.0)), Math.max(1, Math.min(w, h) / 3));
            int alpha = (int) Math.round(Math.min(100.0, st.borderOpacity * a) * 2.55);
            if (alpha <= 0) {
                return;
            }
            int c = (alpha << 24) | (st.borderColor & 0xFFFFFF);
            if ((corners & TL) != 0) {
                g.fill(x, y, x + size, y + size, c);
            }
            if ((corners & TR) != 0) {
                g.fill(x + w - size, y, x + w, y + size, c);
            }
            if ((corners & BL) != 0) {
                g.fill(x, y + h - size, x + size, y + h, c);
            }
            if ((corners & BR) != 0) {
                g.fill(x + w - size, y + h - size, x + w, y + h, c);
            }
        } else {
            HudPanel.drawBorder(g, x, y, w, h, st.borderColor, st.borderOpacity * a, bs, st.borderLength, st.borderOutset,
                    st.borderScale / 100.0);
        }
    }

    private static void drawItem(GuiGraphics g, ItemStack stack, float x, float y, float scale, float alpha) {
        float[] cur = RenderSystem.getShaderColor();
        float[] prev = {cur[0], cur[1], cur[2], cur[3]}; // copy: the getter returns the internal array
        PoseStack ps = g.pose();
        ps.pushPose();
        try {
            RenderSystem.setShaderColor(1f, 1f, 1f, Math.max(0f, Math.min(1f, alpha)));
            ps.translate(x, y, 0f);
            ps.scale(scale, scale, 1f);
            g.renderItem(stack, 0, 0);
        } finally {
            RenderSystem.setShaderColor(prev[0], prev[1], prev[2], prev[3]);
            ps.popPose();
        }
    }
}

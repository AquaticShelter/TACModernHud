package cc.xypp.modernhud.client;

import cc.xypp.modernhud.client.compat.ModernLoadoutCompat;
import cc.xypp.modernhud.config.HudConfig;
import cc.xypp.modernhud.config.HudPosition;
import cc.xypp.modernhud.config.ElementStyle;
import cc.xypp.modernhud.config.KillstreakConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.Locale;

/**
 * Selected-perks toast (Modern Warfare style): whenever the perks selected in the loadout change - on joining, when
 * the class / its perks are changed, or Specialist's chosen perks - ONE box slides in showing each perk as
 * "NAME  [icon]", holds a few seconds, then slides back out.
 *
 * <p>Look: the box is framed by a thin white outline with a small dot on all four corners over a faint soft shadow;
 * each perk's name sits on its own very light, thin highlight strip that fades in from the left. The rows enter one
 * after another (ease-in-out, short stagger, from the screen edge) and leave in the same order, instead of the whole
 * box popping as one block. Which perks are selected comes from ModernLoadout via {@link ModernLoadoutCompat}.
 */
public class PerkToastOverlay implements IGuiOverlay {

    private static final int PAD = 10;
    private static final int ROW_GAP = 4;
    /** Horizontal padding of each row's own box around its content. */
    private static final int ROW_PAD = 6;
    private static final int ICON_GAP = 6;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!ModernLoadoutCompat.loaded() || !KillstreakConfig.perkToast) {
            return;
        }
        if (Minecraft.getInstance().player == null || !ModernLoadoutCompat.refreshPerks()) {
            return;
        }
        int n = ModernLoadoutCompat.perkCount();
        long now = System.currentTimeMillis();
        long elapsed = now - ModernLoadoutCompat.perkSelectionStartMs();
        long hold = KillstreakConfig.perkHoldSeconds * 1000L;
        // global animation switch / speed apply here too (this overlay times itself instead of using EasedAnimator)
        float spd = HudConfig.animations ? (float) Math.max(0.1, HudConfig.animationSpeed) : 1000f;
        long slideIn = Math.max(1, Math.round(KillstreakConfig.perkStyle.fadeInMs / spd));
        long slideOut = Math.max(1, Math.round(KillstreakConfig.perkStyle.fadeOutMs / spd));
        long stagger = Math.round(KillstreakConfig.perkStaggerMs / spd);
        float op = KillstreakConfig.perkStyle.opacity / 100f;
        long enterTotal = slideIn + (n - 1) * stagger;
        long outStart = Math.max(hold, enterTotal);
        long outTotal = slideOut + (n - 1) * stagger;
        if (elapsed < 0 || elapsed > outStart + outTotal) {
            return; // not shown (yet / any more) until the selection changes again
        }

        Font font = Minecraft.getInstance().font;
        int icon = KillstreakConfig.perkIconSize;
        int rowH = Math.max(icon, font.lineHeight + 4);
        int nameW = 0;
        String[] names = new String[n];
        for (int i = 0; i < n; i++) {
            names[i] = ModernLoadoutCompat.perkLabel(i).toUpperCase(Locale.ROOT);
            nameW = Math.max(nameW, font.width(names[i]));
        }
        int contentW = nameW + ICON_GAP + icon;
        int boxW = contentW + PAD * 2;
        int boxH = n * rowH + (n - 1) * ROW_GAP + PAD * 2;

        // per-row visibility: eased in (staggered, first row first), eased out (staggered, same order)
        float[] vis = new float[n];
        float overall = 0f;
        for (int i = 0; i < n; i++) {
            float in = Ease.inOutCubic((elapsed - i * stagger) / (float) slideIn);
            float out = Ease.inOutCubic((elapsed - outStart - i * stagger) / (float) slideOut);
            vis[i] = in * (1f - out);
            overall = Math.max(overall, vis[i]);
        }
        if (overall < 0.01f) {
            return;
        }

        float scale = HudText.snapElement(graphics, KillstreakConfig.perkScale);
        int w = Math.round(boxW * scale);
        int h = Math.round(boxH * scale);
        int ox = KillstreakConfig.perkOffsetX;
        int oy = KillstreakConfig.perkOffsetY;
        HudPosition pos = KillstreakConfig.perkPosition;
        int left;
        int top;
        switch (pos) {
            case TOP_LEFT -> { left = ox; top = oy; }
            case TOP_RIGHT -> { left = screenWidth - w - ox; top = oy; }
            case BOTTOM_LEFT -> { left = ox; top = screenHeight - h - oy; }
            default -> { left = screenWidth - w - ox; top = screenHeight - h - oy; }
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(left, top, 0f);
            pose.scale(scale, scale, 1f);

            // Pure FADE (no sliding): the frame follows the overall (first-in, last-out) visibility ...
            if (KillstreakConfig.perkFrame) {
                // ... and closes the whole block with its border type: by default a dot on each of the four corners
                // (two at the very top, two at the very bottom). Per-row boxes are drawn separately below.
                HudPanel.drawBorder(graphics, 0, 0, boxW, boxH, KillstreakConfig.perkStyle.borderColor,
                        KillstreakConfig.perkStyle.borderOpacity * overall * op, KillstreakConfig.perkStyle.borderStyle,
                        KillstreakConfig.perkStyle.borderLength, KillstreakConfig.perkStyle.borderOutset,
                        KillstreakConfig.perkStyle.borderScale / 100.0);
            }

            // every perk is its OWN thin light box (soft light shadow + flat light panel), fading in turn
            for (int i = 0; i < n; i++) {
                float a = vis[i];
                if (a < 0.02f) {
                    continue;
                }
                int rowY = PAD + i * (rowH + ROW_GAP);
                pose.pushPose();
                try {
                    pose.translate(0f, rowY, 0f);
                    drawRowBox(graphics, boxW, rowH, contentW, a * op);
                    drawRow(graphics, font, i, names[i], boxW, rowH, icon, a * op);
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            pose.popPose();
        }
    }

    /** One perk's own thin box: a soft white-grey shadow around a flat light panel (colour / opacity from the style). */
    private static void drawRowBox(GuiGraphics g, int boxW, int rowH, int contentW, float a) {
        ElementStyle st = KillstreakConfig.perkStyle;
        if (!st.panelEnabled || a <= 0.01f) {
            return;
        }
        int bx = boxW - PAD - contentW - ROW_PAD;
        int bw = contentW + ROW_PAD * 2;
        float strength = st.panelOpacity / 100f;
        SoftDraw.soft(g, bx, 0, bw, rowH, st.panelColor, 0.55f * strength * a, 4f, false);
        if (KillstreakConfig.perkGlow) {
            GlowEffect.drawGlowRect(g, bx, 0, bw, rowH, 0xFFFFFF, 0.22f * a, 3f);
        }
        HudPanel.draw(g, bx, 0, bw, rowH, st.panelColor, st.panelOpacity * a);
    }

    /** One perk: faint highlight strip behind the name, name right-aligned, icon at the right edge. */
    private static void drawRow(GuiGraphics g, Font font, int idx, String name, int boxW, int rowH, int icon, float a) {
        int right = boxW - PAD;
        int iconX = right - icon;
        int iconY = (rowH - icon) / 2;
        int nameW = font.width(name);
        int textX = iconX - ICON_GAP - nameW;
        int textY = (rowH - font.lineHeight) / 2;

        if (KillstreakConfig.perkHighlight) {
            float hx0 = textX - 14f;
            float hx1 = iconX - 3f;
            float hy0 = textY - 1.5f;
            float hy1 = textY + font.lineHeight + 0.5f;
            // fades in from the left, a hair brighter toward the text - "ultra light" so it only lifts the text
            SoftDraw.gradientH(g, hx0, hy0, hx1, hy1, argb(0xFFFFFF, 0f), argb(0xFFFFFF, 0.13f * a));
            SoftDraw.rect(g, hx0 + 10f, hy1 - 0.6f, hx1, hy1, argb(0xFFFFFF, 0.10f * a));
        }
        if (((int) (a * 255f)) >= 16) {
            HudText.text(g, font, name, textX, textY, argb(0xFFFFFF, a), true);
        }

        if (KillstreakConfig.perkGlow) {
            GlowEffect.drawBloom(g, iconX + icon / 2f, iconY + icon / 2f, icon * 1.0f, 0xFFFFFF, 0.40f * a);
        }
        ResourceLocation texture = ModernLoadoutCompat.perkTexture(idx);
        if (texture != null) {
            float[] prev = RenderSystem.getShaderColor();
            float pr = prev[0], pg = prev[1], pb = prev[2], pa = prev[3];
            try {
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1f, 1f, 1f, Math.min(1f, a));
                g.blit(texture, iconX, iconY, icon, icon, 0, 0, 24, 24, 24, 24);
            } finally {
                RenderSystem.setShaderColor(pr, pg, pb, pa);
            }
        } else {
            // no icon texture for this perk: a small framed dot so the row still has its anchor
            SoftDraw.rect(g, iconX + icon * 0.3f, iconY + icon * 0.3f, iconX + icon * 0.7f, iconY + icon * 0.7f,
                    argb(0xFFFFFF, 0.8f * a));
        }
    }

    private static int argb(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}

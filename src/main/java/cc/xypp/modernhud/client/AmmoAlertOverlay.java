package cc.xypp.modernhud.client;

import cc.xypp.modernhud.client.compat.TaczCompat;
import cc.xypp.modernhud.client.compat.SuperbWarfareCompat;
import cc.xypp.modernhud.config.ElementStyle;
import cc.xypp.modernhud.config.WeaponConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Pulsing centre-screen "RELOAD" / "NO AMMO" / "LOW AMMO" text — the same alert convention
 * TaczPresence uses (down to the sine-wave pulse and roughly matching thresholds), so the two feel
 * consistent to anyone who's used both. Deliberately its own overlay rather than part of
 * {@link WeaponHudOverlay}: unlike the fixed-position ammo panel, this is meant to sit near screen
 * centre where you're actually looking, not tucked in a corner.
 *
 * <p>Skipped for guns that read ammo straight from the inventory (no separate mag to run dry) and
 * while overheat-locked, matching TaczPresence's own gate.
 */
public class AmmoAlertOverlay implements IGuiOverlay {

    /** What to show right now. */
    private record Alert(String text, int color, long periodMs) {
    }

    /** 0 = hidden, 1 = shown: fades in when an alert starts and out when it ends (times from the alert style),
     *  instead of the text popping on and off. Starts hidden. */
    private final EasedAnimator vis = new EasedAnimator(200f, EasedAnimator.Easing.CUBIC_IN_OUT);
    private String lastText = "";
    private int lastColor = 0xFFFFFF;
    private long lastPeriod = 600L;

    {
        vis.snapTo(0f);
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Alert alert = currentAlert();
        ElementStyle st = WeaponConfig.alertStyle;
        if (alert != null) {
            lastText = alert.text();
            lastColor = alert.color();
            lastPeriod = alert.periodMs();
        }
        vis.setDurationMs(alert != null ? st.fadeInMs : st.fadeOutMs);
        vis.setTarget(alert != null ? 1f : 0f);
        float v = vis.update();
        if (v < 0.01f || lastText.isEmpty()) {
            return;
        }

        float pulse = WeaponConfig.ammoAlertPulse
                ? 0.45f + 0.55f * 0.5f * (1.0f + (float) Math.sin(System.currentTimeMillis() / (double) lastPeriod))
                : 1.0f;
        float alpha = st.alpha(v * pulse);

        Font font = Minecraft.getInstance().font;
        float scale = HudText.snapElement(graphics, WeaponConfig.ammoAlertScale);
        int width = font.width(lastText);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.scale(scale, scale, 1.0f);
        float x = (screenWidth / 2.0f) / scale - (width / 2.0f);
        float y = (screenHeight / 2.0f) / scale + (WeaponConfig.ammoAlertYOffset / scale);
        int px = Math.round(x);
        int py = Math.round(y);
        // optional panel + border behind the text (own style: opacity, border type dot / line / corners ...)
        HudPanel.drawStyled(graphics, px - 6, py - 3, width + 12, font.lineHeight + 5, st, v);
        if (WeaponConfig.ammoAlertGlow) {
            GlowEffect.drawGlowRect(graphics, px - 3, py - 2, width + 6, font.lineHeight + 3, lastColor, 0.45f * alpha, 5f);
        }
        // vanilla treats a near-zero alpha byte as opaque, so skip the text below ~6%
        if (alpha * 255f >= 16f) {
            int argb = ((int) (alpha * 255f) << 24) | (lastColor & 0xFFFFFF);
            HudText.text(graphics, font, lastText, px, py, argb, true);
        }
        pose.popPose();
    }

    /** The alert that applies this frame, or null when there is none (same rules as before). */
    private static Alert currentAlert() {
        if (!WeaponConfig.enabled || !WeaponConfig.showAmmoAlerts || !TaczCompat.loaded()) {
            return null;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        if (Minecraft.getInstance().options.hideGui && !WeaponConfig.ignoreHideGui) {
            return null;
        }
        if (SuperbWarfareCompat.isInVehicle(player)) {
            return null; // same reasoning as PlayerHudOverlay/WeaponHudOverlay - SBW owns this screen area
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !TaczCompat.isGun(held) || TaczCompat.usesInventoryAmmo(held)) {
            return null;
        }

        int mag = TaczCompat.magAmmo(player, held);
        int cap = TaczCompat.magCapacity(held);
        int reserve = TaczCompat.reserveAmmo(player, held);

        if (mag <= 0) {
            if (reserve == TaczCompat.INFINITE || reserve > 0) {
                return new Alert("RELOAD", WeaponConfig.reloadAlertColor, 400L);
            }
            return new Alert("NO AMMO", WeaponConfig.noAmmoAlertColor, 400L);
        }
        if (cap > 0 && (100.0 * mag / cap) <= WeaponConfig.lowAmmoThresholdPercent) {
            return new Alert("LOW AMMO", WeaponConfig.lowAmmoAlertColor, 600L);
        }
        return null;
    }
}

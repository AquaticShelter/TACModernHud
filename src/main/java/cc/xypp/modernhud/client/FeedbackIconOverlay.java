package cc.xypp.modernhud.client;

import cc.xypp.modernhud.config.HudConfig;
import cc.xypp.modernhud.config.HudPosition;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Renders the "hit landed on active shield" and "shield fully broken" icons.
 * Positioned with the exact same anchor system as the main HUD widget
 * (screen corner + offset, or CUSTOM for an absolute position) - NOT locked
 * next to the crosshair, so it never has to get in the way of your aim.
 */
public class FeedbackIconOverlay implements IGuiOverlay {

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        boolean showBreak = HudConfig.showBreakIcon
                && withinDuration(ClientFeedback.millisSinceBreakTrigger(), HudConfig.breakIconDurationMs);
        boolean showHit = !showBreak && HudConfig.showHitIcon
                && withinDuration(ClientFeedback.millisSinceHitTrigger(), HudConfig.hitIconDurationMs);

        if (!showBreak && !showHit) {
            return;
        }

        BarTextures.Texture icon = showBreak ? BarTextures.ICON_BREAK : BarTextures.ICON_HIT;
        long elapsed = showBreak ? ClientFeedback.millisSinceBreakTrigger() : ClientFeedback.millisSinceHitTrigger();
        int duration = showBreak ? HudConfig.breakIconDurationMs : HudConfig.hitIconDurationMs;
        // holds at full strength, then fades over the last feedbackFadeMs of the display time
        float fadeLen = Math.max(1f, Math.min((float) HudConfig.feedbackFadeMs, (float) duration));
        float alpha = Math.max(0f, Math.min(1f, (duration - elapsed) / fadeLen));
        alpha = alpha * alpha * (3f - 2f * alpha) * (HudConfig.feedbackOpacity / 100f); // smoothstep, then opacity

        int width = HudConfig.feedbackIconWidth;
        int height = HudConfig.feedbackIconHeight;
        double scale = HudConfig.feedbackIconScale;

        int[] pos = resolvePosition(screenWidth, screenHeight, width, height, scale);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(pos[0], pos[1], 0);
        pose.scale((float) scale, (float) scale, 1.0f);

        RenderSystem.enableBlend();
        try {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            icon.blitFull(graphics, 0, 0, width, height);
        } finally {
            // Same reasoning as TaczCompat/WeaponHudOverlay - a bad icon texture throwing here must
            // never leave the fade alpha stuck globally for every OTHER draw call this frame.
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }

        pose.popPose();
    }

    private boolean withinDuration(long elapsed, int durationMs) {
        return elapsed >= 0 && elapsed < durationMs;
    }

    private int[] resolvePosition(int screenWidth, int screenHeight, int width, int height, double scale) {
        int scaledWidth = (int) Math.round(width * scale);
        int scaledHeight = (int) Math.round(height * scale);
        HudPosition position = HudConfig.feedbackPosition;
        int x;
        int y;
        switch (position) {
            case TOP_LEFT:
                x = HudConfig.feedbackOffsetX;
                y = HudConfig.feedbackOffsetY;
                break;
            case TOP_RIGHT:
                x = screenWidth - scaledWidth - HudConfig.feedbackOffsetX;
                y = HudConfig.feedbackOffsetY;
                break;
            case BOTTOM_RIGHT:
                x = screenWidth - scaledWidth - HudConfig.feedbackOffsetX;
                y = screenHeight - scaledHeight - HudConfig.feedbackOffsetY;
                break;
            case CUSTOM:
                x = HudConfig.feedbackOffsetX;
                y = HudConfig.feedbackOffsetY;
                break;
            case BOTTOM_LEFT:
            default:
                x = HudConfig.feedbackOffsetX;
                y = screenHeight - scaledHeight - HudConfig.feedbackOffsetY;
                break;
        }
        return new int[]{x, y};
    }
}

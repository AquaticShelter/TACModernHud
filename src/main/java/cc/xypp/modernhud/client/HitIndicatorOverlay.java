package cc.xypp.modernhud.client;

import cc.xypp.modernhud.ModernHud;
import cc.xypp.modernhud.config.HitIndicationConfig;
import cc.xypp.modernhud.config.HudConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Draws the Modern Warfare-style damage direction: a soft glowing arc around the crosshair pointing back at the
 * attacker, rotating smoothly with the (interpolated) view and fading by real time, plus the crit / kill marker.
 * Detection is server-side ({@code HitIndicationEvents}), bookkeeping is {@link ClientHitTracker}.
 */
public class HitIndicatorOverlay implements IGuiOverlay {

    private static final ResourceLocation[] MARKER_CRIT = {
            loc("textures/hit/marker_crit1.png"), loc("textures/hit/marker_crit2.png"),
            loc("textures/hit/marker_crit3.png"), loc("textures/hit/marker_crit4.png"),
    };
    private static final ResourceLocation[] MARKER_KILL = {
            loc("textures/hit/marker_kill1.png"), loc("textures/hit/marker_kill2.png"),
            loc("textures/hit/marker_kill3.png"), loc("textures/hit/marker_kill4.png"),
    };

    private static final int MARKER_SIZE = 20;

    private static ResourceLocation loc(String path) {
        return new ResourceLocation(ModernHud.MOD_ID, path);
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        if (!ClientHitTracker.activeIndicators.isEmpty()) {
            // interpolated view yaw / position: the arc rotates smoothly with the camera instead of stepping per tick
            Vec3 look = calculateViewVector(0.0f, player.getViewYRot(partialTick));
            Vec2 lookXZ = new Vec2((float) look.x, (float) look.z);
            Vec3 pos = player.getPosition(partialTick);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            for (HitIndicatorEntry entry : ClientHitTracker.activeIndicators) {
                drawArc(graphics, entry, centerX, centerY, pos, lookXZ);
            }
            RenderSystem.disableBlend();
        }
        if (ClientHitTracker.activeMarker != null) {
            drawHitMarker(graphics, ClientHitTracker.activeMarker, Math.round(centerX), Math.round(centerY));
        }
    }

    private static void drawArc(GuiGraphics graphics, HitIndicatorEntry entry, float cx, float cy, Vec3 playerPos,
                                Vec2 lookXZ) {
        float strength = entry.strength();
        float alpha = strength * HitIndicationConfig.opacity / 100f;
        if (alpha <= 0.01f) {
            return;
        }
        Vec3 source = entry.getLocation();
        Vec2 toSource = new Vec2((float) (source.x - playerPos.x), (float) (source.z - playerPos.z));
        // clockwise degrees from "straight ahead" = from straight up on the screen
        double angle = Math.toRadians(angleBetween(lookXZ, toSource));

        int rgb = HitIndicationConfig.absorptionBlueIndicator && entry.isShielded()
                ? HitIndicationConfig.absorptionIndicatorColor : HitIndicationConfig.hitIndicatorColor;
        float half = (float) Math.toRadians(HitIndicationConfig.arcSpanDegrees) / 2f;
        float thick = HitIndicationConfig.thickness;
        // the arc settles onto its radius with a small ease-out "pop" as it flashes in
        float pop = 1f - entry.popProgress();
        float radius = HitIndicationConfig.radius + pop * pop * 8f;
        float glow = HitIndicationConfig.glow ? Math.max(2f, thick * 2.2f) * (HudConfig.glowIntensity / 100f) : 0f;

        SoftDraw.arc(graphics, cx, cy, radius - thick / 2f, radius + thick / 2f,
                (float) angle - half, (float) angle + half, glow, rgb, alpha, false);
        if (HitIndicationConfig.glow && HudConfig.glowIntensity > 0) {
            // a lighter additive halo on top: reads as light, not just a coloured line
            SoftDraw.arc(graphics, cx, cy, radius - thick, radius + thick, (float) angle - half * 0.9f,
                    (float) angle + half * 0.9f, glow * 1.6f, rgb, alpha * 0.35f * (HudConfig.glowIntensity / 100f), true);
        }
    }

    private static void drawHitMarker(GuiGraphics graphics, HitMarkerEntry marker, int centerX, int centerY) {
        float alpha = (marker.getType() == HitMarkerType.CRIT ? 30.0f : 60.0f) / 100.0f;
        ResourceLocation texture = getMarkerTexture(marker.getType(), marker.getLifetime());
        int size = MARKER_SIZE;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        graphics.blit(texture, centerX - size / 2, centerY - size / 2, 0, 0, size, size, size, size);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private static ResourceLocation getMarkerTexture(HitMarkerType type, int lifetime) {
        ResourceLocation[] frames = type == HitMarkerType.KILL ? MARKER_KILL : MARKER_CRIT;
        // 9-tick lifetime counting down: the first couple of ticks play through frames 0-2 as a
        // quick "pop in" animation, then it holds on the last frame for the remaining fade-out.
        return lifetime > 6 ? frames[9 - lifetime] : frames[3];
    }

    /** Signed angle (degrees) from {@code from} to {@code to}, via the standard 2D
     *  cross/dot-product atan2 trick - positive = clockwise. */
    private static double angleBetween(Vec2 from, Vec2 to) {
        double dot = from.x * to.x + from.y * to.y;
        double cross = from.x * to.y - to.x * from.y;
        return Math.atan2(cross, dot) * 180.0 / Math.PI;
    }

    private static Vec3 calculateViewVector(float xRot, float yRot) {
        float xRad = xRot * ((float) Math.PI / 180);
        float yRad = -yRot * ((float) Math.PI / 180);
        float cosY = Mth.cos(yRad);
        float sinY = Mth.sin(yRad);
        float cosX = Mth.cos(xRad);
        float sinX = Mth.sin(xRad);
        return new Vec3(sinY * cosX, -sinX, cosY * cosX);
    }
}

package cc.xypp.modernhud.client.motion;

import cc.xypp.modernhud.config.MotionConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * Motion HUD: the HUD hangs on a spring and lags behind the camera.
 *
 * <p>How it works, once per frame:
 * <ol>
 *   <li>The camera's yaw / pitch change since the last frame is read. The camera is used (not the player) on purpose:
 *       it already contains view shakes, so gun recoil from other mods makes the HUD kick too.</li>
 *   <li>That change shifts the HUD the opposite way, as if the HUD stayed behind in the world.</li>
 *   <li>A damped spring (frame-rate independent, integrated in small steps) pulls the HUD back to its place. "Return
 *       speed" is the spring's natural frequency, "damping" its damping ratio, so a low value gives a soft overshoot.</li>
 *   <li>The result is limited softly (a tanh curve, so the HUD slows down near the limit instead of hitting a wall),
 *       scaled to the real resolution and rounded to whole SCREEN pixels, so text and thin lines stay sharp.</li>
 * </ol>
 * Every element has its own weight ({@link MotionLayer}), which gives a depth (parallax) effect.
 */
public final class MotionHud {

    /** Reference pixels (1080p) of sway per degree of camera turn at 100 % strength. */
    private static final double BASE_GAIN = 0.85;
    /** Largest integration step of the spring, in seconds. */
    private static final double MAX_STEP = 1.0 / 240.0;

    private static long frameCounter;
    private static long updatedFrame = -1;

    private static boolean primed;
    private static float lastYaw;
    private static float lastPitch;
    private static long lastNanos;

    /** Spring state, in reference pixels. */
    private static double x;
    private static double y;
    private static double vx;
    private static double vy;

    /** Result of the current frame, in real SCREEN pixels, before the per-layer weight. */
    private static double outX;
    private static double outY;

    private MotionHud() {
    }

    /** Called once per rendered frame (see {@code MotionEvents}). */
    public static void onRenderTick() {
        frameCounter++;
    }

    /** Forget everything (left the world, feature switched off, resolution change). */
    public static void reset() {
        primed = false;
        x = y = vx = vy = 0.0;
        outX = outY = 0.0;
    }

    private static void update() {
        if (updatedFrame == frameCounter) {
            return;
        }
        updatedFrame = frameCounter;

        Minecraft mc = Minecraft.getInstance();
        if (!MotionConfig.enabled || mc.player == null || mc.level == null) {
            reset();
            return;
        }
        Camera cam = mc.gameRenderer.getMainCamera();
        float yaw = cam.getYRot();
        float pitch = cam.getXRot();
        long now = System.nanoTime();
        if (!primed) {
            primed = true;
            lastYaw = yaw;
            lastPitch = pitch;
            lastNanos = now;
            return;
        }
        double dt = Mth.clamp((now - lastNanos) / 1.0e9, 1.0e-4, 0.05);
        lastNanos = now;

        float dYaw = Mth.wrapDegrees(yaw - lastYaw);
        float dPitch = pitch - lastPitch;
        lastYaw = yaw;
        lastPitch = pitch;
        // a jump of more than 90 degrees in one frame is a teleport / respawn / camera switch, not a turn
        if (Math.abs(dYaw) > 90f || Math.abs(dPitch) > 90f) {
            dYaw = 0f;
            dPitch = 0f;
        }
        float dead = MotionConfig.deadzone;
        if (Math.abs(dYaw) < dead) {
            dYaw = 0f;
        }
        if (Math.abs(dPitch) < dead) {
            dPitch = 0f;
        }

        double strength = MotionConfig.strength;
        // default: the HUD lags behind the camera (turn right -> HUD slides left, look down -> HUD slides up)
        double gx = BASE_GAIN * strength * MotionConfig.horizontal * (MotionConfig.invertH ? 1.0 : -1.0);
        double gy = BASE_GAIN * strength * MotionConfig.vertical * (MotionConfig.invertV ? 1.0 : -1.0);
        x += gx * dYaw;
        y += gy * dPitch;

        double max = Math.max(0.0, MotionConfig.maxOffset);
        double w = MotionConfig.returnSpeed;
        double z = MotionConfig.bounce;
        int steps = Math.max(1, (int) Math.ceil(dt / MAX_STEP));
        double h = dt / steps;
        for (int i = 0; i < steps; i++) {
            vx += (-w * w * x - 2.0 * z * w * vx) * h;
            vy += (-w * w * y - 2.0 * z * w * vy) * h;
            x += vx * h;
            y += vy * h;
        }
        double hard = Math.max(1.0, max * 3.0); // keeps a burst of flicks from storing a long tail
        x = Mth.clamp(x, -hard, hard);
        y = Mth.clamp(y, -hard, hard);
        if (Math.abs(x) < 0.01 && Math.abs(y) < 0.01 && Math.abs(vx) < 0.05 && Math.abs(vy) < 0.05
                && dYaw == 0f && dPitch == 0f) {
            x = y = vx = vy = 0.0; // at rest: no endless micro movement
        }

        double k = mc.getWindow().getHeight() / 1080.0;
        outX = max <= 0.0 ? 0.0 : max * Math.tanh(x / max) * k;
        outY = max <= 0.0 ? 0.0 : max * Math.tanh(y / max) * k;
    }

    /**
     * Pushes the pose and moves it by the offset of {@code layer}.
     *
     * @return true when a pose was pushed (the caller must pop it); false when nothing moved
     */
    public static boolean push(PoseStack pose, MotionLayer layer) {
        if (layer == MotionLayer.NONE || !MotionConfig.enabled) {
            return false;
        }
        return pushWeighted(pose, MotionConfig.LAYER_WEIGHT[layer.ordinal()]);
    }

    /** Same, for the vanilla HUD pieces (weight from the {@code vanilla} section). */
    public static boolean pushVanilla(PoseStack pose) {
        if (!MotionConfig.enabled || !MotionConfig.vanillaEnabled) {
            return false;
        }
        return pushWeighted(pose, MotionConfig.vanillaPercent);
    }

    private static boolean pushWeighted(PoseStack pose, float weight) {
        if (weight <= 0f) {
            return false;
        }
        update();
        if (outX == 0.0 && outY == 0.0) {
            return false;
        }
        double gui = Math.max(1.0, Minecraft.getInstance().getWindow().getGuiScale());
        double px = outX * weight;
        double py = outY * weight;
        if (MotionConfig.pixelSnap) {
            px = Math.rint(px);
            py = Math.rint(py);
        }
        if (px == 0.0 && py == 0.0) {
            return false;
        }
        pose.pushPose();
        pose.translate(px / gui, py / gui, 0.0);
        return true;
    }
}

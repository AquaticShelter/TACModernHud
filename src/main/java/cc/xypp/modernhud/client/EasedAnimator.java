package cc.xypp.modernhud.client;

import cc.xypp.modernhud.config.HudConfig;

/**
 * A time-based, duration + easing-curve value animator — glides from a start value to a target
 * over a fixed REAL-WORLD duration (not a fixed number of frames), so the animation takes the same
 * amount of time regardless of framerate, and settles with a deliberate easing curve rather than
 * an exponential "asymptotically-never-quite-arrives" decay.
 *
 * <p>Modelled on the "digital scroll" animator technique from a reference kill-feed mod studied for
 * this v3.0 rewrite (a self-contained, reusable eased-value animator is a common building block
 * behind "premium"-feeling HUD numbers/bars in commercial shooters) — reimplemented here from
 * scratch in this project's own style; nothing is copied, since the source project's architecture
 * (JSON-config-driven, per-element render pipeline) doesn't transfer to this codebase's fixed
 * hand-coded widgets.
 *
 * <p>Retargeting mid-animation (calling {@link #setTarget} again before the current animation
 * finishes) restarts the animation FROM THE CURRENT VALUE toward the new target, so rapid changes
 * (e.g. taking several hits in quick succession) never visually "jump" or restart from scratch —
 * it just smoothly redirects, exactly like the reference technique's "update target without
 * restarting" note for fast-changing values.
 */
public final class EasedAnimator {

    public enum Easing {
        LINEAR,
        CUBIC_OUT,
        QUINTIC_OUT,
        /** Slow start AND slow finish - for things that appear / leave / slide (ease in + ease out). */
        CUBIC_IN_OUT
    }

    private float durationMs;
    private final Easing easing;
    /** True when the current segment began while the previous one was still running: an in-out curve would then
     *  restart from a standstill (sluggish, "sticky" values that change every frame) so it eases OUT instead. */
    private boolean retargeted;

    private float startValue;
    private float targetValue;
    private float currentValue;
    private long startTimeMs;
    private boolean initialized;

    public EasedAnimator(float durationMs, Easing easing) {
        this.durationMs = Math.max(1f, durationMs);
        this.easing = easing;
    }

    /** Changes the duration used for animations started from now on (e.g. a longer fade-out than fade-in). */
    public void setDurationMs(float ms) {
        this.durationMs = Math.max(1f, ms);
    }

    private static long nowMs() {
        return System.nanoTime() / 1_000_000L; // monotonic: immune to system-clock jumps
    }

    /** Snaps immediately to {@code value} with no animation — use on the very first frame, so a
     *  bar doesn't glide up from zero the moment the HUD first appears. */
    public void snapTo(float value) {
        startValue = value;
        targetValue = value;
        currentValue = value;
        initialized = true;
    }

    /** Redirects the animation toward {@code target}. A no-op if {@code target} already equals the
     *  current target, so calling this every frame with an unchanged value doesn't restart anything. */
    public void setTarget(float target) {
        if (!initialized) {
            snapTo(target);
            return;
        }
        if (Math.abs(target - targetValue) < 1e-5f) {
            return;
        }
        retargeted = currentValue != targetValue;
        startValue = currentValue;
        targetValue = target;
        startTimeMs = nowMs();
    }

    /** Advances and returns the current animated value — call once per frame. */
    public float update() {
        if (!initialized) {
            return currentValue;
        }
        if (!HudConfig.animations) {
            currentValue = targetValue; // global animations switch: everything snaps
            return currentValue;
        }
        float dur = durationMs / (float) Math.max(0.1, HudConfig.animationSpeed);
        long elapsed = nowMs() - startTimeMs;
        if (elapsed >= (long) dur) {
            currentValue = targetValue;
            return currentValue;
        }
        float t = Math.max(0f, elapsed / dur);
        currentValue = startValue + (targetValue - startValue) * applyEasing(t);
        return currentValue;
    }

    public float getCurrentValue() {
        return currentValue;
    }

    public float getTargetValue() {
        return targetValue;
    }

    public boolean isAnimating() {
        return initialized && currentValue != targetValue;
    }

    private float applyEasing(float t) {
        Easing e = retargeted && easing == Easing.CUBIC_IN_OUT ? Easing.CUBIC_OUT : easing;
        switch (e) {
            case LINEAR:
                return t;
            case CUBIC_OUT: {
                float inv = 1f - t;
                return 1f - inv * inv * inv;
            }
            case CUBIC_IN_OUT:
                return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
            case QUINTIC_OUT:
            default: {
                float inv = 1f - t;
                return 1f - inv * inv * inv * inv * inv;
            }
        }
    }
}

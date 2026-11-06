package cc.xypp.modernhud.client;

/** Plain easing curves on t in [0,1] (clamped), for hand-driven animations that need more control than
 *  {@link EasedAnimator} gives (staggered rows, separate in/out phases). */
public final class Ease {

    private Ease() {
    }

    public static float clamp01(float t) {
        return t < 0f ? 0f : (t > 1f ? 1f : t);
    }

    /** Slow start and slow finish. */
    public static float inOutCubic(float t) {
        t = clamp01(t);
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }

    /** Fast start, slow finish. */
    public static float outCubic(float t) {
        t = clamp01(t);
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    /** Fast start, overshoots slightly past 1 and settles back - a soft "pop" for things that arrive. */
    public static float outBack(float t) {
        t = clamp01(t);
        float c1 = 1.2f;
        float c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }
}

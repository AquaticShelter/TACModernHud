package cc.xypp.modernhud.client.spec;

import cc.xypp.modernhud.config.SpecsConfig;

/**
 * Server TPS (ticks per second), measured from the server's OWN game clock.
 *
 * <p>Every server sends a time update (with its game time in ticks) once a second. Between two updates the game time
 * grows by 20 on a healthy server and by less when the server lags, while real time keeps running. So
 * {@code ticks passed / real seconds passed} is the TPS. It works on any server, vanilla or modded, with no mod needed
 * on the server, and it also shows lag that is not a network problem. An update that did not move the clock (paused
 * single player) is skipped.
 */
public final class TpsTracker {

    private static final int RING = 64;
    private static final long[] AT = new long[RING];
    private static final long[] TICKS = new long[RING];
    private static final long[] NANOS = new long[RING];
    private static int head;
    private static int count;
    private static long lastNanos;
    private static long lastGameTime = Long.MIN_VALUE;

    private TpsTracker() {
    }

    public static synchronized void reset() {
        head = 0;
        count = 0;
        lastNanos = 0L;
        lastGameTime = Long.MIN_VALUE;
    }

    /** A time update arrived (network thread). {@code paused}: the game is paused, so the clock stands still on purpose. */
    public static synchronized void onTime(long nowNanos, long gameTime, boolean paused) {
        if (paused || lastGameTime == Long.MIN_VALUE || gameTime < lastGameTime) {
            lastNanos = nowNanos;
            lastGameTime = gameTime;
            return;
        }
        long dt = nowNanos - lastNanos;
        long dg = gameTime - lastGameTime;
        lastNanos = nowNanos;
        lastGameTime = gameTime;
        if (dt < 200_000_000L) {
            return; // two updates in a burst: no information in a gap this short
        }
        AT[head] = nowNanos;
        TICKS[head] = dg;
        NANOS[head] = dt;
        head = (head + 1) % RING;
        count = Math.min(count + 1, RING);
    }

    /** TPS over the configured window (at most 20), or -1 when there is no data yet. */
    public static synchronized float tps(long nowNanos) {
        long window = SpecsConfig.tpsWindowSeconds * 1_000_000_000L;
        long ticks = 0L;
        long nanos = 0L;
        for (int i = 1; i <= count; i++) {
            int idx = Math.floorMod(head - i, RING);
            if (nowNanos - AT[idx] > window) {
                break;
            }
            ticks += TICKS[idx];
            nanos += NANOS[idx];
        }
        if (nanos < 500_000_000L) {
            return -1f;
        }
        return Math.min(20f, (float) (ticks * 1.0e9 / nanos));
    }
}

package cc.xypp.modernhud.client.spec;

/**
 * Estimates packet loss on a multiplayer connection.
 *
 * <p>Minecraft uses TCP, which re-sends lost packets, so a mod never sees a packet that was really lost. What a player
 * feels as "packet loss" is a stall: nothing arrives for a while, then everything arrives at once. The server sends a
 * time update every second (every 20 ticks). This class watches those: an update that arrives much later than one
 * second after the previous one means the updates in between "went missing" (they were lost, delayed or the server
 * lagged). The estimate is {@code missing / (received + missing)} over a sliding window. While a stall is going on the
 * missing ones are counted right away, so the value rises in real time instead of after the stall.
 *
 * <p>Written from the network thread, read from the render thread; every method is synchronized (once a second, so the
 * cost is nothing).
 */
public final class PacketLossTracker {

    private static final int BUCKETS = 128;
    private static final long SECOND = 1_000_000_000L;
    /** An update later than this after the previous one starts counting as missing. */
    private static final long LATE_NANOS = 1_500_000_000L;

    private static final int[] RECEIVED = new int[BUCKETS];
    private static final int[] MISSING = new int[BUCKETS];
    private static final long[] STAMP = new long[BUCKETS];
    private static long last;

    private PacketLossTracker() {
    }

    public static synchronized void reset() {
        for (int i = 0; i < BUCKETS; i++) {
            RECEIVED[i] = 0;
            MISSING[i] = 0;
            STAMP[i] = Long.MIN_VALUE;
        }
        last = 0L;
    }

    /** A time update packet arrived at {@code nowNanos} ({@code System.nanoTime()}). */
    public static synchronized void onTimePacket(long nowNanos) {
        long missing = 0L;
        if (last != 0L) {
            long gap = nowNanos - last;
            if (gap > LATE_NANOS) {
                missing = Math.max(0L, Math.round(gap / (double) SECOND) - 1L);
            }
        }
        last = nowNanos;
        long second = Math.floorDiv(nowNanos, SECOND);
        int b = (int) Math.floorMod(second, (long) BUCKETS);
        if (STAMP[b] != second) {
            STAMP[b] = second;
            RECEIVED[b] = 0;
            MISSING[b] = 0;
        }
        RECEIVED[b]++;
        MISSING[b] += (int) Math.min(missing, 1000L);
    }

    /** Estimated loss in percent (0 to 100) over the last {@code windowSeconds} seconds. */
    public static synchronized float percent(long nowNanos, int windowSeconds) {
        if (last == 0L) {
            return 0f;
        }
        int window = Math.max(1, Math.min(windowSeconds, BUCKETS - 2));
        long second = Math.floorDiv(nowNanos, SECOND);
        long received = 0L;
        long missing = 0L;
        for (int i = 0; i < BUCKETS; i++) {
            long age = second - STAMP[i];
            if (STAMP[i] != Long.MIN_VALUE && age >= 0L && age < window) {
                received += RECEIVED[i];
                missing += MISSING[i];
            }
        }
        long gap = nowNanos - last;
        if (gap > LATE_NANOS) {
            missing += Math.max(0L, Math.round(gap / (double) SECOND) - 1L); // a stall that is still going on
        }
        long total = received + missing;
        if (total <= 0L) {
            return 0f;
        }
        return Math.min(100f, 100f * missing / total);
    }

    static {
        reset();
    }
}

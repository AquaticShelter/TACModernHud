package cc.xypp.modernhud.client.spec;

import cc.xypp.modernhud.config.PingSource;
import cc.xypp.modernhud.config.SpecsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

/**
 * The live numbers behind the spec readout. FPS is counted here (one tick per rendered frame), so it is exactly what
 * you see and needs no access to Minecraft's private counter.
 */
public final class SpecStats {

    private static final int RING = 8192;

    private static long fpsWindowStart;
    private static int frames;
    private static int fps = -1;
    private static float frameMs = -1f;
    private static int low1 = -1;

    /** The newest frame durations (ms) with the time they ended, for the 1 % low. */
    private static final float[] FRAME_MS = new float[RING];
    private static final long[] FRAME_AT = new long[RING];
    private static final float[] SCRATCH = new float[RING];
    private static int ringHead;
    private static int ringCount;
    private static long lastFrame;

    // frame time graph: the slowest frame of each short slice of time
    private static final int GRAPH_RING = 256;
    private static final float[] GRAPH = new float[GRAPH_RING];
    private static int graphHead;
    private static int graphCount;
    private static long bucketStart;
    private static float bucketMax;

    private static volatile long sessionStart;

    private SpecStats() {
    }

    /** One call per rendered frame, from the render tick event. */
    public static void onFrame(long nowNanos) {
        if (lastFrame != 0L) {
            float ms = (nowNanos - lastFrame) / 1.0e6f;
            if (ms < 5000f) { // ignore a long pause (loading, alt-tab)
                FRAME_MS[ringHead] = ms;
                FRAME_AT[ringHead] = nowNanos;
                ringHead = (ringHead + 1) % RING;
                ringCount = Math.min(ringCount + 1, RING);
            }
        }
        if (lastFrame != 0L) {
            float ft = (nowNanos - lastFrame) / 1.0e6f;
            if (bucketStart == 0L) {
                bucketStart = nowNanos;
            }
            bucketMax = Math.max(bucketMax, Math.min(ft, 1000f));
            if (nowNanos - bucketStart >= SpecsConfig.graphBucketMs * 1_000_000L) {
                GRAPH[graphHead] = bucketMax;
                graphHead = (graphHead + 1) % GRAPH_RING;
                graphCount = Math.min(graphCount + 1, GRAPH_RING);
                bucketMax = 0f;
                bucketStart = nowNanos;
            }
        }
        lastFrame = nowNanos;
        if (fpsWindowStart == 0L) {
            fpsWindowStart = nowNanos;
            frames = 0;
            return;
        }
        frames++;
        long elapsed = nowNanos - fpsWindowStart;
        if (elapsed >= SpecsConfig.fpsUpdateMs * 1_000_000L) {
            fps = (int) Math.round(frames * 1.0e9 / elapsed);
            frameMs = (float) (elapsed / 1.0e6 / Math.max(1, frames));
            low1 = computeLow(nowNanos);
            frames = 0;
            fpsWindowStart = nowNanos;
        }
    }

    /**
     * The 1 % low, the way MSI Afterburner reports it: the average FPS of the slowest 1 % of the frames of the last
     * {@code lowWindowSeconds} seconds. -1 until enough frames were seen.
     */
    private static int computeLow(long nowNanos) {
        long window = SpecsConfig.lowWindowSeconds * 1_000_000_000L;
        int n = 0;
        for (int i = 1; i <= ringCount; i++) {
            int idx = Math.floorMod(ringHead - i, RING);
            if (nowNanos - FRAME_AT[idx] > window) {
                break;
            }
            SCRATCH[n++] = FRAME_MS[idx];
        }
        if (n < 20) {
            return -1;
        }
        java.util.Arrays.sort(SCRATCH, 0, n);
        int worst = Math.max(1, (int) Math.ceil(n * 0.01)); // the slowest 1 % of the frames
        float sum = 0f;
        for (int i = n - worst; i < n; i++) {
            sum += SCRATCH[i];
        }
        float avg = sum / worst;
        return avg <= 0f ? -1 : Math.round(1000f / avg);
    }

    /** Number of bars with data (at most the ring size). */
    public static int graphCount() {
        return graphCount;
    }

    /** Frame time (ms) of the bar {@code i} counted from the NEWEST (0 = newest); 0 when there is none. */
    public static float graphFromNewest(int i) {
        if (i >= graphCount) {
            return 0f;
        }
        return GRAPH[Math.floorMod(graphHead - 1 - i, GRAPH_RING)];
    }

    public static void onSessionStart() {
        sessionStart = System.nanoTime();
    }

    /** Seconds since joining this world / server; -1 when not in one. */
    public static long sessionSeconds() {
        long s = sessionStart;
        return s == 0L ? -1L : (System.nanoTime() - s) / 1_000_000_000L;
    }

    /** {incoming, outgoing} packets per second averaged by the connection itself; null when not connected. */
    public static float[] packetsPerSecond() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return null;
        }
        net.minecraft.network.Connection c = connection.getConnection();
        return new float[]{c.getAverageReceivedPackets(), c.getAverageSentPackets()};
    }

    /** Average frame time in milliseconds over the update interval; -1 until the first value is ready. */
    public static float frameMs() {
        return frameMs;
    }

    /** 1 % low FPS; -1 when not known yet. */
    public static int low1() {
        return low1;
    }

    /** Frames per second averaged over the update interval; -1 until the first value is ready. */
    public static int fps() {
        return fps;
    }

    /** Ping in milliseconds as the server reports it (the same number as the tab list); -1 when unknown. */
    public static int pingMs() {
        PingSource src = SpecsConfig.pingSource;
        if (src != PingSource.TAB_LIST) {
            int live = LatencyProbe.pingMs();
            if (live >= 0) {
                return live; // measured live with the mod's own probe
            }
            if (src == PingSource.PROBE && LatencyProbe.active()) {
                return -1; // waiting for the first answer
            }
        }
        return tabListPing();
    }

    private static int tabListPing() {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || mc.player == null) {
            return -1;
        }
        PlayerInfo info = connection.getPlayerInfo(mc.player.getUUID());
        return info == null ? -1 : Math.max(0, info.getLatency());
    }

    /** True while playing a single-player world (no network at all). */
    public static boolean isSingleplayer() {
        return Minecraft.getInstance().getSingleplayerServer() != null;
    }

    /** Estimated packet loss in percent, 0 in single player. See {@link PacketLossTracker}. */
    public static float lossPercent() {
        if (isSingleplayer()) {
            return 0f;
        }
        long now = System.nanoTime();
        if (LatencyProbe.active()) {
            return LatencyProbe.lossPercent(now, SpecsConfig.lossWindowSeconds, SpecsConfig.lossTimeoutMs);
        }
        return PacketLossTracker.percent(now, SpecsConfig.lossWindowSeconds);
    }
}

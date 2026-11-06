package cc.xypp.modernhud.client.spec;

import cc.xypp.modernhud.config.SpecsConfig;
import cc.xypp.modernhud.network.PingChannel;
import cc.xypp.modernhud.network.PingProbePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.Arrays;

/**
 * Real ping and packet loss, measured with the mod's own probe packets (when the server has Modern HUD too).
 *
 * <p>Why not just use the tab-list ping? That number comes from vanilla keep-alives: the server sends one every 15
 * seconds, so it is a slow, smoothed value that reacts to a lag spike half a minute late. (Tools such as spark read the
 * very same server-side value.) Here the client sends a tiny probe twice a second and the server answers it at once,
 * so the round trip is measured live, on the client's own clock:
 * <ul>
 *   <li><b>Ping</b> = median of the last few round trips (a single spike does not make the number jump).</li>
 *   <li><b>Packet loss</b> = the share of probes in the window that got no answer, or an answer later than the timeout.
 *       TCP never loses data for good, but a packet that is re-sent arrives late, and a probe that late is what a player
 *       feels as loss.</li>
 * </ul>
 * Without the mod on the server nothing is sent and the readout falls back to the tab-list ping and the time-update
 * estimate of {@link PacketLossTracker}.
 */
public final class LatencyProbe {

    private static final int RING = 512;

    private static final long[] SENT = new long[RING];
    private static final long[] RTT = new long[RING]; // nanoseconds, 0 = no answer yet
    private static final int[] SEQ = new int[RING];
    private static int nextSeq = 1;
    private static long lastSendNanos;
    private static volatile long lastEchoNanos;
    private static volatile boolean active;
    private static volatile long medianNanos = -1L;

    private LatencyProbe() {
    }

    public static synchronized void reset() {
        Arrays.fill(SENT, 0L);
        Arrays.fill(RTT, 0L);
        Arrays.fill(SEQ, 0);
        nextSeq = 1;
        lastSendNanos = 0L;
        lastEchoNanos = 0L;
        active = false;
        medianNanos = -1L;
    }

    /** One call per client tick. */
    public static void onClientTick() {
        long nowN = System.nanoTime();
        if (nowN - lastSendNanos < SpecsConfig.pingProbeMs * 1_000_000L - 10_000_000L) {
            return; // not due yet (the check runs every game tick, 50 ms)
        }
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || mc.player == null || mc.getSingleplayerServer() != null) {
            active = false;
            return;
        }
        if (!PingChannel.CHANNEL.isRemotePresent(connection.getConnection())) {
            active = false;
            return; // the server has no Modern HUD: nothing to measure with
        }
        lastSendNanos = nowN;
        send();
    }

    private static synchronized void send() {
        long now = System.nanoTime();
        int seq = nextSeq++;
        int slot = Math.floorMod(seq, RING);
        SEQ[slot] = seq;
        SENT[slot] = now;
        RTT[slot] = 0L;
        active = true;
        PingChannel.CHANNEL.sendToServer(new PingProbePacket(seq, now));
    }

    /** The answer arrived (network thread). {@code sentNanos} is the value the client put in the probe. */
    public static synchronized void onEcho(int seq, long sentNanos, long nowNanos) {
        int slot = Math.floorMod(seq, RING);
        if (SEQ[slot] != seq || SENT[slot] != sentNanos) {
            return; // an answer to a probe from a previous connection
        }
        long rtt = Math.max(1L, nowNanos - sentNanos);
        RTT[slot] = rtt;
        lastEchoNanos = nowNanos;
        medianNanos = median(seq, Math.max(1, SpecsConfig.pingSamples));
    }

    /** Median of the newest {@code count} answered probes up to {@code newestSeq}. */
    private static long median(int newestSeq, int count) {
        long[] v = new long[count];
        int n = 0;
        for (int s = newestSeq; s > 0 && s > newestSeq - RING + 1 && n < count; s--) {
            int slot = Math.floorMod(s, RING);
            if (SEQ[slot] == s && RTT[slot] > 0L) {
                v[n++] = RTT[slot];
            }
        }
        if (n == 0) {
            return -1L;
        }
        Arrays.sort(v, 0, n);
        return v[n / 2];
    }

    /** True while probes are being sent and the server is answering (or has not yet been given the chance to). */
    public static boolean active() {
        return active;
    }

    /** Round-trip time in milliseconds, or -1 when there is no answer yet. */
    public static int pingMs() {
        long m = medianNanos;
        if (!active || m < 0L) {
            return -1;
        }
        return (int) Math.max(0L, Math.round(m / 1.0e6));
    }

    /** Percent of probes in the last {@code windowSeconds} that were not answered within the timeout (0 to 100). */
    public static synchronized float lossPercent(long nowNanos, int windowSeconds, int timeoutMs) {
        long timeout = timeoutMs * 1_000_000L;
        long window = windowSeconds * 1_000_000_000L;
        int settled = 0;
        int lost = 0;
        int newest = nextSeq - 1;
        for (int s = newest; s > 0 && s > newest - RING + 1; s--) {
            int slot = Math.floorMod(s, RING);
            if (SEQ[slot] != s) {
                break;
            }
            long age = nowNanos - SENT[slot];
            if (age > window) {
                break;
            }
            long rtt = RTT[slot];
            if (rtt > 0L) {
                settled++;
                if (rtt > timeout) {
                    lost++; // it came, but too late to count
                }
            } else if (age >= timeout) {
                settled++;
                lost++; // still no answer after the timeout
            } // else: still within the timeout, not decided yet
        }
        return settled == 0 ? 0f : Math.min(100f, 100f * lost / settled);
    }
}

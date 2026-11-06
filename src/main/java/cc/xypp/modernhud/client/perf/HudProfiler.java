package cc.xypp.modernhud.client.perf;

import cc.xypp.modernhud.client.motion.MotionLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Measures what each Modern HUD element costs, so optimising is guided by numbers instead of guesses.
 *
 * <ul>
 *   <li><b>CPU time</b> per element (the time the game thread spends building its draw commands), always on: two clock
 *       reads per element and frame, which is nothing. Smoothed over about a second.</li>
 *   <li><b>GPU time</b> per element with an OpenGL timer query, only while switched on ({@code /modernhud profile gpu on}),
 *       because queries add a little work of their own. Needs OpenGL 3.3, which every card in use has.</li>
 * </ul>
 * The numbers cover the elements drawn through {@code HudScale}. Vanilla overlays (hotbar, chat) and other mods' HUDs are
 * not in them, so comparing the total with F1 shows how much of the HUD cost is Modern HUD and how much is not.
 */
public final class HudProfiler {

    private static final Logger LOGGER = LogManager.getLogger("ModernHud/Profiler");
    private static final int N = MotionLayer.values().length;
    private static final double SMOOTH = 0.03;

    private static final long[] cpuFrame = new long[N];
    private static final double[] cpuAvg = new double[N];
    private static final double[] cpuMax = new double[N];
    private static double totalAvg;

    // GPU timer queries
    private static final int POOL = 64;
    private static final int[] queries = new int[POOL];
    private static final int[] owner = new int[POOL];
    private static final boolean[] pending = new boolean[POOL];
    private static boolean queriesReady;
    private static int nextQuery;
    private static int activeQuery = -1;
    private static boolean gpuOn;
    private static boolean gpuBroken;
    private static final long[] gpuFrame = new long[N];
    private static final double[] gpuAvg = new double[N];
    private static double gpuTotalAvg;

    // named parts inside an element, and every overlay of every mod (see OverlayTiming)
    private static final java.util.Map<String, double[]> SECTIONS = new java.util.LinkedHashMap<>();
    private static final java.util.Map<String, double[]> ALL = new java.util.LinkedHashMap<>();
    private static boolean allOn;

    // the scan used by auto-optimize: true average cost per frame of every overlay over a window
    private static boolean scanning;
    private static long scanFrames;
    private static final java.util.Map<String, long[]> SCAN = new java.util.HashMap<>();

    private HudProfiler() {
    }

    /** Start of a measured part inside an element (see {@link #section}). */
    public static long mark() {
        return System.nanoTime();
    }

    /** Adds the time since {@code t0} to the part called {@code name} (shown under the element in the report). */
    public static void section(String name, long t0) {
        double[] a = SECTIONS.get(name);
        if (a == null) {
            a = new double[3];
            SECTIONS.put(name, a);
        }
        a[0] += System.nanoTime() - t0;
    }

    /** Every overlay seen by {@code profile all}, with its average CPU milliseconds per drawn frame, most expensive first. */
    public static List<java.util.Map.Entry<String, Double>> overlayCosts() {
        List<java.util.Map.Entry<String, Double>> out = new ArrayList<>();
        for (java.util.Map.Entry<String, double[]> e : ALL.entrySet()) {
            out.add(new java.util.AbstractMap.SimpleEntry<>(e.getKey(), e.getValue()[1]));
        }
        out.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return out;
    }

    /** Forget all numbers (start a clean measurement). */
    public static void clear() {
        java.util.Arrays.fill(cpuAvg, 0.0);
        java.util.Arrays.fill(cpuMax, 0.0);
        java.util.Arrays.fill(gpuAvg, 0.0);
        totalAvg = 0.0;
        gpuTotalAvg = 0.0;
        SECTIONS.clear();
        ALL.clear();
    }

    public static boolean allEnabled() {
        return allOn;
    }

    /** True while any measurement of overlays is running (the overlay timer must run). */
    public static boolean timingWanted() {
        return allOn || scanning;
    }

    public static void startScan() {
        SCAN.clear();
        scanFrames = 0L;
        scanning = true;
    }

    public static boolean isScanning() {
        return scanning;
    }

    /** Ends the scan; returns, per overlay id, the average milliseconds it cost per frame over the window. */
    public static java.util.Map<String, Double> finishScan() {
        scanning = false;
        java.util.Map<String, Double> out = new java.util.LinkedHashMap<>();
        double frames = Math.max(1L, scanFrames);
        for (java.util.Map.Entry<String, long[]> e : SCAN.entrySet()) {
            out.put(e.getKey(), e.getValue()[0] / 1.0e6 / frames);
        }
        SCAN.clear();
        return out;
    }

    public static String setAll(boolean on) {
        allOn = on;
        if (!on) {
            ALL.clear();
        }
        return on ? "Timing every HUD overlay (vanilla and other mods) is on. Play a bit, then run /modernhud profile."
                : "Timing of every overlay is off.";
    }

    /** One overlay of any mod finished drawing; {@code ns} is how long it took. */
    static void addOverlay(String id, long ns) {
        if (allOn) {
            double[] a = ALL.get(id);
            if (a == null) {
                a = new double[3];
                ALL.put(id, a);
            }
            a[0] += ns;
        }
        if (scanning) {
            long[] s = SCAN.get(id);
            if (s == null) {
                s = new long[1];
                SCAN.put(id, s);
            }
            s[0] += ns;
        }
    }

    private static void fold(java.util.Map<String, double[]> map) {
        for (double[] a : map.values()) {
            if (a[0] > 0.0) { // average only over the frames in which it was drawn: idle frames must not dilute it
                double ms = a[0] / 1.0e6;
                a[1] += (ms - a[1]) * SMOOTH;
                a[2] = Math.max(ms, a[2] * 0.995);
            }
            a[0] = 0.0;
        }
    }

    /** Starts timing one element; returns the token for {@link #end}. */
    public static long begin(MotionLayer layer) {
        if (gpuOn && !gpuBroken && activeQuery < 0) {
            startQuery(layer.ordinal());
        }
        return System.nanoTime();
    }

    public static void end(MotionLayer layer, long token) {
        cpuFrame[layer.ordinal()] += System.nanoTime() - token;
        if (activeQuery >= 0) {
            endQuery();
        }
    }

    /** Once per rendered frame: folds this frame's numbers into the averages. */
    public static void onFrame() {
        if (scanning) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && !mc.options.hideGui) {
                scanFrames++; // only frames in which a HUD is showing count
            }
        }
        double total = 0.0;
        for (int i = 0; i < N; i++) {
            double ms = cpuFrame[i] / 1.0e6;
            if (cpuFrame[i] > 0L) { // per element: only frames in which it was drawn
                cpuAvg[i] += (ms - cpuAvg[i]) * SMOOTH;
                cpuMax[i] = Math.max(ms, cpuMax[i] * 0.995);
            }
            total += ms;
            cpuFrame[i] = 0L;
        }
        totalAvg += (total - totalAvg) * SMOOTH;
        fold(SECTIONS);
        if (allOn) {
            fold(ALL);
        }
        if (gpuOn && !gpuBroken) {
            pollQueries();
            double gtotal = 0.0;
            for (int i = 0; i < N; i++) {
                double ms = gpuFrame[i] / 1.0e6;
                gpuAvg[i] += (ms - gpuAvg[i]) * SMOOTH;
                gtotal += ms;
                gpuFrame[i] = 0L;
            }
            gpuTotalAvg += (gtotal - gpuTotalAvg) * SMOOTH;
        }
    }

    /** CPU milliseconds per frame that Modern HUD uses to draw (smoothed). */
    public static float totalCpuMs() {
        return (float) totalAvg;
    }

    // ------------------------------------------------------------------------------------------------ GPU queries

    public static boolean gpuEnabled() {
        return gpuOn && !gpuBroken;
    }

    /** Switches GPU timing on or off; returns a sentence about the result. */
    public static String setGpu(boolean on) {
        if (!on) {
            gpuOn = false;
            return "GPU timing off.";
        }
        try {
            if (!GL.getCapabilities().OpenGL33) {
                return "GPU timing needs OpenGL 3.3, which this graphics context does not offer.";
            }
            if (!queriesReady) {
                for (int i = 0; i < POOL; i++) {
                    queries[i] = GL15.glGenQueries();
                }
                queriesReady = true;
            }
            gpuBroken = false;
            gpuOn = true;
            return "GPU timing on. Wait a few seconds, then run /modernhud profile.";
        } catch (RuntimeException | LinkageError e) {
            gpuBroken = true;
            LOGGER.warn("GPU timer queries unavailable: {}", e.toString());
            return "GPU timing is not available here: " + e;
        }
    }

    private static void startQuery(int layer) {
        try {
            for (int tries = 0; tries < POOL; tries++) {
                int q = nextQuery;
                nextQuery = (nextQuery + 1) % POOL;
                if (!pending[q]) {
                    GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, queries[q]);
                    owner[q] = layer;
                    pending[q] = true;
                    activeQuery = q;
                    return;
                }
            }
        } catch (RuntimeException | LinkageError e) {
            gpuBroken = true;
            activeQuery = -1;
        }
    }

    private static void endQuery() {
        try {
            GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
        } catch (RuntimeException | LinkageError e) {
            gpuBroken = true;
        }
        activeQuery = -1;
    }

    private static void pollQueries() {
        try {
            for (int q = 0; q < POOL; q++) {
                if (pending[q] && q != activeQuery
                        && GL15.glGetQueryObjecti(queries[q], GL15.GL_QUERY_RESULT_AVAILABLE) != 0) {
                    long ns = GL33.glGetQueryObjectui64(queries[q], GL15.GL_QUERY_RESULT);
                    gpuFrame[owner[q]] += ns;
                    pending[q] = false;
                }
            }
        } catch (RuntimeException | LinkageError e) {
            gpuBroken = true;
        }
    }

    // ------------------------------------------------------------------------------------------------ report

    /** Lines for {@code /modernhud profile}: elements sorted by CPU cost, then the total against the frame time. */
    public static List<String> report(float frameMs) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            if (cpuAvg[i] > 0.0005 || gpuAvg[i] > 0.0005) {
                idx.add(i);
            }
        }
        idx.sort((a, b) -> Double.compare(cpuAvg[b], cpuAvg[a]));
        List<String> out = new ArrayList<>();
        out.add("Modern HUD cost per frame in which each element is drawn (CPU = game thread building the draw commands" + (gpuEnabled() ? ", GPU = timer query" : "") + "):");
        for (int i : idx) {
            String name = i == MotionLayer.NONE.ordinal() ? "OTHER" : MotionLayer.values()[i].name();
            out.add(String.format(Locale.ROOT, "  %-14s cpu %.3f ms (peak %.2f)%s", name, cpuAvg[i], cpuMax[i],
                    gpuEnabled() ? String.format(Locale.ROOT, "   gpu %.3f ms", gpuAvg[i]) : ""));
        }
        if (idx.isEmpty()) {
            out.add("  nothing was drawn yet (is the HUD visible?)");
        }
        String share = frameMs > 0f ? String.format(Locale.ROOT, " = %.1f %% of a %.2f ms frame", totalAvg / frameMs * 100.0, frameMs) : "";
        out.add(String.format(Locale.ROOT, "  TOTAL (all elements, averaged over every frame) cpu %.3f ms%s%s", totalAvg, share,
                gpuEnabled() ? String.format(Locale.ROOT, "   gpu %.3f ms", gpuTotalAvg) : ""));
        if (!SECTIONS.isEmpty()) {
            out.add("Parts inside the elements (they overlap their element's line above):");
            List<java.util.Map.Entry<String, double[]>> parts = new ArrayList<>(SECTIONS.entrySet());
            parts.sort((x, y) -> Double.compare(y.getValue()[1], x.getValue()[1]));
            for (java.util.Map.Entry<String, double[]> e : parts) {
                if (e.getValue()[1] > 0.0005) {
                    out.add(String.format(Locale.ROOT, "    %-22s cpu %.3f ms (peak %.2f)", e.getKey(), e.getValue()[1], e.getValue()[2]));
                }
            }
        }
        if (allOn) {
            out.add("Every HUD overlay, vanilla and all mods (CPU time of each overlay's own drawing):");
            List<java.util.Map.Entry<String, double[]>> list = new ArrayList<>(ALL.entrySet());
            list.sort((x, y) -> Double.compare(y.getValue()[1], x.getValue()[1]));
            double sum = 0.0;
            int shown = 0;
            for (java.util.Map.Entry<String, double[]> e : list) {
                sum += e.getValue()[1];
                if (shown < 14 && e.getValue()[1] > 0.002) {
                    out.add(String.format(Locale.ROOT, "    %-34s cpu %.3f ms (peak %.2f)", e.getKey(), e.getValue()[1], e.getValue()[2]));
                    shown++;
                }
            }
            out.add(String.format(Locale.ROOT, "    ALL OVERLAYS                       cpu %.3f ms", sum));
        }
        out.add("Press F1 and compare the FPS: whatever is left over is the vanilla HUD and other mods' HUDs, not Modern HUD.");
        return out;
    }
}

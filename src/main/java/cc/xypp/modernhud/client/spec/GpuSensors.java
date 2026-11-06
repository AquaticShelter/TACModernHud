package cc.xypp.modernhud.client.spec;

import cc.xypp.modernhud.config.GpuSource;
import cc.xypp.modernhud.config.SpecsConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GPU temperature, usage, video memory, power and clock. A game cannot read these by itself, so a background thread
 * asks the graphics driver. The commands are fixed; nothing from the config file is ever run.
 *
 * <p>Sources, tried in this order by AUTO:
 * <ol>
 *   <li><b>NVIDIA</b>: one {@code nvidia-smi} process that stays running and prints a line every few seconds (far
 *       cheaper than starting the tool for every reading). It is stopped when the readout stops asking, and when the
 *       game exits.</li>
 *   <li><b>Linux kernel sensors</b> (amdgpu, nouveau, i915 ...): temperature, and for amdgpu also usage, video memory,
 *       power and clock. No tool needed.</li>
 *   <li><b>LibreHardwareMonitor / OpenHardwareMonitor</b> running on Windows: every value, for AMD and Intel too.</li>
 * </ol>
 */
public final class GpuSensors extends BackgroundSampler<GpuSensors.Reading> {

    /** One reading. A value the source cannot give is {@link #NA}. */
    public record Reading(int temp, int load, int vramUsedMb, int vramTotalMb, int powerW, int clockMhz) {
        public static final int NA = -1;
    }

    private enum Kind { NVIDIA, HWMON, MONITOR }

    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    private static final boolean LINUX = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
    private static final int NA = Reading.NA;

    public static final GpuSensors INSTANCE = new GpuSensors();

    /** When each source may be tried again (epoch ms). Only touched by the sampler thread. */
    private final long[] retryAt = new long[Kind.values().length];
    private Kind working;

    // the running nvidia-smi
    private static Process nvProc;
    private static volatile String nvLine;
    private static volatile long nvLineMs;
    private static int nvIndex = -1;
    private static int nvPoll = -1;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(GpuSensors::stopNvidia, "modernhud-gpu-stop"));
    }

    private GpuSensors() {
        super("modernhud-gpu");
    }

    /** The newest reading or null (render thread; call once per frame while a GPU box is shown). */
    public static Reading get() {
        return INSTANCE.request();
    }

    @Override
    protected long intervalMs() {
        long ms = Math.max(250L, SpecsConfig.gpuPollMs);
        return working == Kind.MONITOR ? Math.max(ms, 3_000L) : ms; // PowerShell is slow to start
    }

    @Override
    protected void onIdle() {
        stopNvidia();
    }

    @Override
    protected Reading sample(long now) throws InterruptedException {
        int index = SpecsConfig.gpuIndex;
        boolean fixed = SpecsConfig.gpuSource != GpuSource.AUTO;
        Kind[] kinds = switch (SpecsConfig.gpuSource) {
            case NVIDIA_SMI -> new Kind[]{Kind.NVIDIA};
            case LINUX_HWMON -> new Kind[]{Kind.HWMON};
            case HARDWARE_MONITOR -> new Kind[]{Kind.MONITOR};
            default -> new Kind[]{Kind.NVIDIA, Kind.HWMON, Kind.MONITOR};
        };
        if (!fixed && working != null) {
            Reading r = read(working, index);
            if (r != null) {
                return r;
            }
            retryAt[working.ordinal()] = now + 60_000L;
            working = null;
        }
        for (Kind k : kinds) {
            if (!fixed && now < retryAt[k.ordinal()]) {
                continue;
            }
            Reading r = read(k, index);
            if (r != null) {
                if (working != k) {
                    LOGGER.info("GPU sensor source: {}", k);
                }
                working = k;
                return r;
            }
            retryAt[k.ordinal()] = now + 300_000L; // not present: do not try again for five minutes
        }
        return null;
    }

    private Reading read(Kind kind, int index) throws InterruptedException {
        try {
            return switch (kind) {
                case NVIDIA -> readNvidia(index);
                case HWMON -> LINUX ? readHwmon(index) : null;
                case MONITOR -> WINDOWS ? readMonitor(index) : null;
            };
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** Human readable state of every GPU source, for {@code /modernhud diag}. Runs the sensors once, so call it off-thread. */
    public static java.util.List<String> diagnose() {
        java.util.List<String> out = new ArrayList<>();
        out.add("GPU: configured source " + SpecsConfig.gpuSource + ", card index " + SpecsConfig.gpuIndex
                + ", working source " + INSTANCE.working);
        String[] exes = WINDOWS ? new String[]{"nvidia-smi", "C:\\Windows\\System32\\nvidia-smi.exe",
                "C:\\Program Files\\NVIDIA Corporation\\NVSMI\\nvidia-smi.exe"} : new String[]{"nvidia-smi"};
        String nv = "not found (no NVIDIA driver tools on PATH)";
        for (String exe : exes) {
            try {
                Process p = new ProcessBuilder(exe, "--query-gpu=name,driver_version,temperature.gpu,utilization.gpu",
                        "--format=csv,noheader").redirectErrorStream(true).start();
                p.getOutputStream().close();
                if (p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    nv = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim().replace("\n", " | ");
                } else {
                    p.destroyForcibly();
                    nv = "timed out";
                }
                break;
            } catch (IOException notFound) {
                // try the next path
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        out.add("  nvidia-smi: " + nv);
        if (LINUX) {
            try {
                Reading r = readHwmon(Math.max(0, SpecsConfig.gpuIndex));
                out.add("  Linux sysfs: " + (r == null ? "no GPU hwmon sensor for this index" : r.toString()));
            } catch (IOException | RuntimeException e) {
                out.add("  Linux sysfs: error " + e);
            }
        }
        if (WINDOWS) {
            List<LibreMonitor.Sensor> all = LibreMonitor.sensors(0L);
            Reading r = all.isEmpty() ? null : fromSensors(all, Math.max(0, SpecsConfig.gpuIndex));
            out.add("  LibreHardwareMonitor: " + (all.isEmpty() ? "not running" : r == null ? all.size() + " sensors, no GPU among them" : r.toString()));
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------------ NVIDIA

    private static Reading readNvidia(int index) throws InterruptedException {
        int poll = Math.max(250, SpecsConfig.gpuPollMs);
        if (nvProc == null || !nvProc.isAlive() || nvIndex != index || nvPoll != poll) {
            stopNvidia();
            if (!startNvidia(index, poll)) {
                return null;
            }
        }
        String line = nvLine;
        if (line == null || System.currentTimeMillis() - nvLineMs > Math.max(5_000L, poll * 8L)) {
            return null;
        }
        return parseNvidia(line);
    }

    private static synchronized boolean startNvidia(int index, int poll) throws InterruptedException {
        List<String> candidates = new ArrayList<>();
        candidates.add("nvidia-smi");
        if (WINDOWS) {
            candidates.add("C:\\Windows\\System32\\nvidia-smi.exe");
            candidates.add("C:\\Program Files\\NVIDIA Corporation\\NVSMI\\nvidia-smi.exe");
        }
        for (String exe : candidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder(exe, "-i", Integer.toString(Math.max(0, index)),
                        "--query-gpu=temperature.gpu,utilization.gpu,memory.used,memory.total,power.draw,clocks.gr",
                        "--format=csv,noheader,nounits", "-lms", Integer.toString(poll));
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.getOutputStream().close();
                nvLine = null;
                nvProc = p;
                nvIndex = index;
                nvPoll = poll;
                Thread reader = new Thread(() -> pump(p), "modernhud-gpu-nvidia");
                reader.setDaemon(true);
                reader.start();
                for (int i = 0; i < 40 && nvLine == null && p.isAlive(); i++) {
                    Thread.sleep(50L); // wait for the first line so the box never flashes N/A
                }
                if (nvLine != null && parseNvidia(nvLine) != null) {
                    return true;
                }
                stopNvidia();
            } catch (IOException notFound) {
                // this path does not exist: try the next one
            }
        }
        return false;
    }

    private static void pump(Process p) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (p == nvProc) {
                    nvLine = line;
                    nvLineMs = System.currentTimeMillis();
                }
            }
        } catch (IOException ignored) {
            // the process ended
        }
    }

    static synchronized void stopNvidia() {
        if (nvProc != null) {
            nvProc.destroyForcibly();
            nvProc = null;
        }
        nvLine = null;
    }

    /** "61, 23, 1523, 8192, 87.45, 1920" (temp, usage %, MB used, MB total, W, MHz); a field may be [N/A]. */
    static Reading parseNvidia(String line) {
        if (line == null) {
            return null;
        }
        String[] t = line.split(",");
        if (t.length < 6) {
            return null;
        }
        int temp = num(t[0]);
        if (temp == NA) {
            return null; // an error text, not a reading
        }
        return new Reading(temp, num(t[1]), num(t[2]), num(t[3]), num(t[4]), num(t[5]));
    }

    private static int num(String s) {
        try {
            double v = Double.parseDouble(s.trim().replace(',', '.'));
            return v < 0 ? NA : (int) Math.round(v);
        } catch (NumberFormatException e) {
            return NA; // [N/A], [Not Supported]
        }
    }

    // ------------------------------------------------------------------------------------------------ Linux sysfs

    private static final Pattern MHZ = Pattern.compile("(\\d+)\\s*[Mm][Hh][Zz]");

    private static Reading readHwmon(int index) throws IOException {
        Path drm = Paths.get("/sys/class/drm");
        if (!Files.isDirectory(drm)) {
            return null;
        }
        List<Path> cards = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(drm, "card[0-9]*")) {
            for (Path card : ds) {
                if (!card.getFileName().toString().contains("-") && firstExisting(card.resolve("device/hwmon"), "hwmon*",
                        "temp1_input") != null) {
                    cards.add(card);
                }
            }
        }
        Collections.sort(cards);
        if (index >= cards.size()) {
            return null;
        }
        Path card = cards.get(index);
        Path dev = card.resolve("device");
        Path mon = firstExisting(dev.resolve("hwmon"), "hwmon*", "temp1_input");
        int temp = mon == null ? NA : (int) Math.round(readLong(mon.resolve("temp1_input"), -1000L) / 1000.0);
        int load = (int) readLong(dev.resolve("gpu_busy_percent"), NA);
        long used = readLong(dev.resolve("mem_info_vram_used"), -1L);
        long total = readLong(dev.resolve("mem_info_vram_total"), -1L);
        int power = NA;
        if (mon != null) {
            long uw = readLong(mon.resolve("power1_average"), readLong(mon.resolve("power1_input"), -1L));
            power = uw < 0 ? NA : (int) Math.round(uw / 1_000_000.0);
        }
        int clock = parseDpmClock(readText(dev.resolve("pp_dpm_sclk")));
        if (clock == NA) {
            clock = (int) readLong(card.resolve("gt_cur_freq_mhz"), NA); // Intel
        }
        if (temp <= -100) {
            temp = NA;
        }
        return new Reading(temp, load, used < 0 ? NA : (int) (used / 1_048_576L), total < 0 ? NA : (int) (total / 1_048_576L),
                power, clock);
    }

    /** The line of pp_dpm_sclk marked with '*' is the current clock: "1: 1380Mhz *". */
    static int parseDpmClock(String text) {
        if (text == null) {
            return NA;
        }
        for (String line : text.split("\\R")) {
            if (line.contains("*")) {
                Matcher m = MHZ.matcher(line);
                if (m.find()) {
                    return Integer.parseInt(m.group(1));
                }
            }
        }
        return NA;
    }

    static Path firstExisting(Path dir, String glob, String mustContain) throws IOException {
        if (!Files.isDirectory(dir)) {
            return null;
        }
        TreeSet<Path> sorted = new TreeSet<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, glob)) {
            for (Path p : ds) {
                sorted.add(p);
            }
        }
        for (Path p : sorted) {
            if (Files.isReadable(p.resolve(mustContain))) {
                return p;
            }
        }
        return null;
    }

    static long readLong(Path file, long fallback) {
        try {
            return Long.parseLong(new String(Files.readAllBytes(file), StandardCharsets.US_ASCII).trim());
        } catch (IOException | NumberFormatException e) {
            return fallback;
        }
    }

    private static String readText(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.US_ASCII);
        } catch (IOException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------------------------------------ LibreHardwareMonitor

    private static Reading readMonitor(int index) {
        List<LibreMonitor.Sensor> all = LibreMonitor.sensors(Math.max(3_000L, SpecsConfig.gpuPollMs - 250L));
        return fromSensors(all, index);
    }

    /** Picks the {@code index}-th GPU out of the LibreHardwareMonitor sensor list. */
    static Reading fromSensors(List<LibreMonitor.Sensor> all, int index) {
        TreeSet<String> gpus = new TreeSet<>();
        for (LibreMonitor.Sensor s : all) {
            String id = s.id().toLowerCase(Locale.ROOT);
            if (id.startsWith("/gpu")) {
                int second = id.indexOf('/', 1);
                int third = second < 0 ? -1 : id.indexOf('/', second + 1);
                gpus.add(third < 0 ? id : id.substring(0, third));
            }
        }
        List<String> list = new ArrayList<>(gpus);
        if (index >= list.size()) {
            return null;
        }
        String prefix = list.get(index);
        double temp = -1, load = -1, clock = -1, power = -1, used = -1, total = -1;
        for (LibreMonitor.Sensor s : all) {
            if (!s.id().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                continue;
            }
            String n = s.name().toLowerCase(Locale.ROOT);
            switch (s.type()) {
                case "Temperature" -> {
                    if (temp < 0 || n.equals("gpu core")) {
                        temp = s.value();
                    }
                }
                case "Load" -> {
                    if (n.equals("gpu core")) {
                        load = s.value();
                    }
                }
                case "Clock" -> {
                    if (n.equals("gpu core")) {
                        clock = s.value();
                    }
                }
                case "Power" -> {
                    if (power < 0 || n.equals("gpu package") || n.equals("gpu power")) {
                        power = s.value();
                    }
                }
                case "SmallData" -> {
                    if (n.equals("gpu memory used")) {
                        used = s.value();
                    } else if (n.equals("gpu memory total")) {
                        total = s.value();
                    }
                }
                default -> {
                }
            }
        }
        if (temp < 0 && load < 0 && used < 0) {
            return null;
        }
        return new Reading(round(temp), round(load), round(used), round(total), round(power), round(clock));
    }

    private static int round(double v) {
        return v < 0 ? NA : (int) Math.round(v);
    }
}

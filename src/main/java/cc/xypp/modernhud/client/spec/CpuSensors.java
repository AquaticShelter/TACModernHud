package cc.xypp.modernhud.client.spec;

import cc.xypp.modernhud.config.CpuTempSource;
import cc.xypp.modernhud.config.SpecsConfig;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * CPU usage, system memory and CPU temperature, read on a background thread.
 * <ul>
 *   <li><b>Usage</b>: the JVM's own view of the whole machine (the same number as the task manager) and of this game.</li>
 *   <li><b>Memory</b>: "memory in use" the way task managers count it (total minus what is available, caches
 *       included in available), from /proc/meminfo on Linux and the JVM elsewhere.</li>
 *   <li><b>Temperature</b>: Linux kernel sensors (coretemp, k10temp, zenpower ...) or LibreHardwareMonitor on Windows.
 *       Only read while a CPU temperature box is shown.</li>
 * </ul>
 */
public final class CpuSensors extends BackgroundSampler<CpuSensors.Reading> {

    /** One reading; a value that is not known is {@link #NA}. */
    public record Reading(int load, int processLoad, int temp, int ramUsedMb, int ramTotalMb) {
        public static final int NA = -1;
    }

    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    private static final boolean LINUX = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
    private static final int NA = Reading.NA;

    public static final CpuSensors INSTANCE = new CpuSensors();

    private static volatile long tempWantedMs;
    private final com.sun.management.OperatingSystemMXBean os;

    private CpuSensors() {
        super("modernhud-cpu");
        java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
        os = bean instanceof com.sun.management.OperatingSystemMXBean x ? x : null;
    }

    public static Reading get() {
        return INSTANCE.request();
    }

    /** Call every frame a CPU temperature box is shown, so the (slower) temperature read stays on. */
    public static void wantTemperature() {
        tempWantedMs = System.currentTimeMillis();
    }

    @Override
    protected long intervalMs() {
        return Math.max(250, SpecsConfig.hardwarePollMs);
    }

    @Override
    protected Reading sample(long now) {
        int load = NA;
        int proc = NA;
        int used = NA;
        int total = NA;
        if (os != null) {
            load = percent(os.getCpuLoad());
            proc = percent(os.getProcessCpuLoad());
        }
        long[] mem = LINUX ? linuxMemory() : null;
        if (mem != null) {
            total = (int) (mem[0] / 1024L);
            used = (int) ((mem[0] - mem[1]) / 1024L);
        } else if (os != null) {
            long t = os.getTotalMemorySize();
            long f = os.getFreeMemorySize();
            if (t > 0) {
                total = (int) (t / 1_048_576L);
                used = (int) ((t - f) / 1_048_576L);
            }
        }
        int temp = NA;
        if (now - tempWantedMs < 3_000L) {
            temp = readTemp();
        }
        return new Reading(load, proc, temp, used, total);
    }

    private static int percent(double v) {
        return Double.isNaN(v) || v < 0 ? NA : (int) Math.round(Math.min(1.0, v) * 100.0);
    }

    /** Human readable state of every CPU source, for {@code /modernhud diag}. Runs the sensors once, so call it off-thread. */
    public static List<String> diagnose() {
        List<String> out = new ArrayList<>();
        out.add("CPU: usage " + (INSTANCE.os == null ? "unavailable (no JVM bean)" : percent(INSTANCE.os.getCpuLoad()) + " %")
                + ", configured temperature source " + SpecsConfig.cpuTempSource);
        if (LINUX) {
            try {
                int t = linuxTemp();
                out.add("  Linux hwmon (coretemp / k10temp / zenpower): " + (t == NA ? "no CPU sensor found in /sys/class/hwmon" : t + " C"));
            } catch (IOException | RuntimeException e) {
                out.add("  Linux hwmon: error " + e);
            }
        }
        if (WINDOWS) {
            List<LibreMonitor.Sensor> all = LibreMonitor.sensors(0L);
            int lhm = monitorTemp(all);
            out.add("  LibreHardwareMonitor / OpenHardwareMonitor: " + (all.isEmpty()
                    ? "NOT running (no WMI sensors). Start LibreHardwareMonitor, as administrator, for the exact CPU temperature."
                    : all.size() + " sensors, CPU temperature " + (lhm == NA ? "not among them" : lhm + " C")));
            int tz = ThermalZones.celsius(0L);
            out.add("  ACPI thermal zone: " + (tz < 0 ? "no valid zone reported by this motherboard" : tz + " C")
                    + " (NOT used by AUTO: it is a board sensor that often sits near room temperature and does not follow the"
                    + " CPU. Set hardware.cpuTempSource = ACPI_THERMAL_ZONE to show it anyway.)");
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------------ memory (Linux)

    /** {MemTotal kB, MemAvailable kB} or null. */
    private static long[] linuxMemory() {
        try {
            return parseMeminfo(new String(Files.readAllBytes(Paths.get("/proc/meminfo")), StandardCharsets.US_ASCII));
        } catch (IOException e) {
            return null;
        }
    }

    static long[] parseMeminfo(String text) {
        long total = -1;
        long avail = -1;
        for (String line : text.split("\\R")) {
            if (line.startsWith("MemTotal:")) {
                total = kb(line);
            } else if (line.startsWith("MemAvailable:")) {
                avail = kb(line);
            }
        }
        return total > 0 && avail >= 0 ? new long[]{total, avail} : null;
    }

    private static long kb(String line) {
        String[] p = line.trim().split("\\s+");
        try {
            return Long.parseLong(p[1]);
        } catch (RuntimeException e) {
            return -1;
        }
    }

    // ------------------------------------------------------------------------------------------------ temperature

    private static int readTemp() {
        CpuTempSource src = SpecsConfig.cpuTempSource;
        try {
            if ((src == CpuTempSource.AUTO || src == CpuTempSource.LINUX_HWMON) && LINUX) {
                int t = linuxTemp();
                if (t != NA) {
                    return t;
                }
            }
            long maxAge = Math.max(3_000L, SpecsConfig.hardwarePollMs - 250L);
            if ((src == CpuTempSource.AUTO || src == CpuTempSource.HARDWARE_MONITOR) && WINDOWS) {
                int t = monitorTemp(LibreMonitor.sensors(maxAge));
                if (t != NA || src == CpuTempSource.HARDWARE_MONITOR) {
                    return t;
                }
            }
            if (src == CpuTempSource.ACPI_THERMAL_ZONE && WINDOWS) {
                return ThermalZones.celsius(maxAge);
            }
        } catch (IOException | RuntimeException e) {
            return NA;
        }
        return NA;
    }

    private static int priority(String hwmonName) {
        return switch (hwmonName) {
            case "coretemp", "k10temp", "zenpower" -> 1;
            case "cpu_thermal", "soc_thermal" -> 2;
            case "acpitz" -> 3;
            default -> 99;
        };
    }

    private static int linuxTemp() throws IOException {
        Path root = Paths.get("/sys/class/hwmon");
        if (!Files.isDirectory(root)) {
            return NA;
        }
        Path best = null;
        int bestPrio = 99;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(root, "hwmon*")) {
            for (Path mon : ds) {
                String name;
                try {
                    name = new String(Files.readAllBytes(mon.resolve("name")), StandardCharsets.US_ASCII).trim();
                } catch (IOException e) {
                    continue;
                }
                int pr = priority(name);
                if (pr < bestPrio || (pr == bestPrio && best != null && mon.compareTo(best) < 0)) {
                    best = mon;
                    bestPrio = pr;
                }
            }
        }
        if (best == null || bestPrio == 99) {
            return NA;
        }
        // prefer the package / Tctl / Tdie sensor, else temp1, else the hottest
        long fallback = -1;
        long max = -1;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(best, "temp*_input")) {
            List<Path> files = new ArrayList<>();
            ds.forEach(files::add);
            for (Path f : files) {
                long v = GpuSensors.readLong(f, -1L);
                if (v <= 0) {
                    continue;
                }
                max = Math.max(max, v);
                String base = f.getFileName().toString();
                String label = "";
                try {
                    label = new String(Files.readAllBytes(best.resolve(base.replace("_input", "_label"))),
                            StandardCharsets.US_ASCII).trim().toLowerCase(Locale.ROOT);
                } catch (IOException ignored) {
                    // no label
                }
                if (label.contains("package") || label.contains("tctl") || label.contains("tdie")) {
                    return (int) Math.round(v / 1000.0);
                }
                if (base.equals("temp1_input")) {
                    fallback = v;
                }
            }
        }
        long v = fallback > 0 ? fallback : max;
        return v > 0 ? (int) Math.round(v / 1000.0) : NA;
    }

    static int monitorTemp(List<LibreMonitor.Sensor> all) {
        double best = -1;
        double any = -1;
        int bestRank = 99;
        for (LibreMonitor.Sensor s : all) {
            String id = s.id().toLowerCase(Locale.ROOT);
            if (!s.type().equals("Temperature") || !(id.startsWith("/intelcpu") || id.startsWith("/amdcpu"))) {
                continue;
            }
            any = Math.max(any, s.value());
            String n = s.name().toLowerCase(Locale.ROOT);
            int rank = n.equals("cpu package") ? 0 : n.startsWith("core (tctl") || n.startsWith("cpu (tctl") ? 1
                    : n.equals("core max") ? 2 : 99;
            if (rank < bestRank) {
                bestRank = rank;
                best = s.value();
            }
        }
        double v = best >= 0 ? best : any;
        return v > 0 ? (int) Math.round(v) : NA;
    }
}

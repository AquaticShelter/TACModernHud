package cc.xypp.modernhud.client.spec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Sensors of LibreHardwareMonitor / OpenHardwareMonitor (Windows), read through PowerShell's WMI access. One query
 * returns every sensor (CPU and GPU), and the result is cached, so the CPU and the GPU readers share a single call.
 * The command is fixed; nothing from the config is ever run.
 */
final class LibreMonitor {

    record Sensor(String type, String name, String id, double value) {
    }

    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    private static final String SCRIPT = "$ErrorActionPreference='SilentlyContinue';"
            + "$s=Get-CimInstance -Namespace root/LibreHardwareMonitor -ClassName Sensor;"
            + "if(-not $s){$s=Get-CimInstance -Namespace root/OpenHardwareMonitor -ClassName Sensor};"
            + "$s|ForEach-Object{[string]::Format([cultureinfo]::InvariantCulture,'{0}|{1}|{2}|{3}',"
            + "$_.SensorType,$_.Name,$_.Identifier,[math]::Round([double]$_.Value,1))}";

    private static List<Sensor> cache = List.of();
    private static long cacheMs;
    private static long failedUntil;

    private LibreMonitor() {
    }

    /** All sensors, at most {@code maxAgeMs} old. Empty when the program is not running (or not on Windows). */
    static synchronized List<Sensor> sensors(long maxAgeMs) {
        if (!WINDOWS) {
            return List.of();
        }
        long now = System.currentTimeMillis();
        if (now - cacheMs < maxAgeMs) {
            return cache;
        }
        if (now < failedUntil) {
            return List.of();
        }
        try {
            String out = run();
            List<Sensor> parsed = parse(out);
            if (parsed.isEmpty()) {
                failedUntil = now + 60_000L; // not running: look again in a minute
                cache = List.of();
            } else {
                cache = parsed;
            }
            cacheMs = System.currentTimeMillis();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            failedUntil = now + 60_000L;
            cache = List.of();
        }
        return cache;
    }

    static List<Sensor> parse(String text) {
        List<Sensor> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        for (String line : text.split("\\R")) {
            String[] p = line.trim().split("\\|", 4);
            if (p.length < 4) {
                continue;
            }
            try {
                out.add(new Sensor(p[0].trim(), p[1].trim(), p[2].trim(), Double.parseDouble(p[3].trim())));
            } catch (NumberFormatException ignored) {
                // a sensor without a value
            }
        }
        return out;
    }

    private static String run() throws IOException, InterruptedException {
        return powershell(SCRIPT, 10);
    }

    /** Runs a FIXED PowerShell script (never anything from the config) and returns its output. */
    static String powershell(String script, int timeoutSeconds) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                "-Command", script);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try {
            p.getOutputStream().close();
            if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new IOException("timeout");
            }
            return new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            if (p.isAlive()) {
                p.destroyForcibly();
            }
        }
    }
}

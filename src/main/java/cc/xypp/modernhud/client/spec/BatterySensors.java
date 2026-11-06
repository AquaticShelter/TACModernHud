package cc.xypp.modernhud.client.spec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Laptop battery charge: Linux from /sys/class/power_supply, Windows from {@code Win32_Battery} (read through PowerShell
 * every 15 seconds, on a background thread). A desktop has no battery: {@link #get()} stays null and the box hides.
 */
public final class BatterySensors extends BackgroundSampler<BatterySensors.Reading> {

    public record Reading(int percent, boolean charging) {
    }

    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    private static final boolean LINUX = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
    private static final String SCRIPT = "$ErrorActionPreference='SilentlyContinue';"
            + "Get-CimInstance -ClassName Win32_Battery|Select-Object -First 1"
            + "|ForEach-Object{[string]::Format([cultureinfo]::InvariantCulture,'{0}|{1}',$_.EstimatedChargeRemaining,$_.BatteryStatus)}";

    public static final BatterySensors INSTANCE = new BatterySensors();

    private BatterySensors() {
        super("modernhud-battery");
    }

    public static Reading get() {
        return INSTANCE.request();
    }

    @Override
    protected long intervalMs() {
        return 15_000L;
    }

    @Override
    protected Reading sample(long now) throws InterruptedException {
        try {
            if (LINUX) {
                return linux();
            }
            if (WINDOWS) {
                return parseWindows(LibreMonitor.powershell(SCRIPT, 8));
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    private static Reading linux() throws IOException {
        Path root = Paths.get("/sys/class/power_supply");
        if (!Files.isDirectory(root)) {
            return null;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(root, "BAT*")) {
            for (Path bat : ds) {
                long cap = GpuSensors.readLong(bat.resolve("capacity"), -1L);
                if (cap < 0) {
                    continue;
                }
                String status = "";
                try {
                    status = new String(Files.readAllBytes(bat.resolve("status")), StandardCharsets.US_ASCII).trim();
                } catch (IOException ignored) {
                    // no status file
                }
                return new Reading((int) Math.min(100, cap), status.equalsIgnoreCase("Charging"));
            }
        }
        return null;
    }

    /** "87|2": charge percent and the Win32_Battery status (2 = on AC, 6 to 9 = charging, 1 = discharging). */
    static Reading parseWindows(String text) {
        if (text == null) {
            return null;
        }
        for (String line : text.split("\\R")) {
            String[] p = line.trim().split("\\|");
            if (p.length < 2) {
                continue;
            }
            try {
                int pct = Integer.parseInt(p[0].trim());
                int st = Integer.parseInt(p[1].trim());
                boolean charging = st == 2 || (st >= 6 && st <= 9);
                return new Reading(Math.max(0, Math.min(100, pct)), charging);
            } catch (NumberFormatException ignored) {
                // no battery: the query printed nothing useful
            }
        }
        return null;
    }
}

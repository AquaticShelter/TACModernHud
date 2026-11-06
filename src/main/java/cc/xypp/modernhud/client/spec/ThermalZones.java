package cc.xypp.modernhud.client.spec;

import java.io.IOException;
import java.util.Locale;

/**
 * Windows thermal zones: the temperatures the motherboard firmware (ACPI) reports. They need no extra program and no
 * administrator rights ({@code Win32_PerfFormattedData_Counters_ThermalZoneInformation}), with the admin-only
 * {@code MSAcpi_ThermalZoneTemperature} as a second try. A zone is the board's CPU area, not the die itself, so the value
 * is close but not as exact as LibreHardwareMonitor; on some desktop boards it never moves.
 */
final class ThermalZones {

    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    private static final String SCRIPT = "$ErrorActionPreference='SilentlyContinue';"
            + "Get-CimInstance -ClassName Win32_PerfFormattedData_Counters_ThermalZoneInformation"
            + "|ForEach-Object{[string]::Format([cultureinfo]::InvariantCulture,'K|{0}|{1}',$_.Name,$_.Temperature)};"
            + "Get-CimInstance -Namespace root/wmi -ClassName MSAcpi_ThermalZoneTemperature"
            + "|ForEach-Object{[string]::Format([cultureinfo]::InvariantCulture,'A|{0}|{1}',$_.InstanceName,"
            + "[math]::Round($_.CurrentTemperature/10.0,1))}";

    private static int cached = -1;
    private static long cachedMs;
    private static long failedUntil;

    private ThermalZones() {
    }

    /** Hottest valid zone in degrees Celsius, or -1. At most {@code maxAgeMs} old. */
    static synchronized int celsius(long maxAgeMs) {
        if (!WINDOWS) {
            return -1;
        }
        long now = System.currentTimeMillis();
        if (now - cachedMs < maxAgeMs) {
            return cached;
        }
        if (now < failedUntil) {
            return -1;
        }
        try {
            cached = parse(LibreMonitor.powershell(SCRIPT, 8));
            cachedMs = System.currentTimeMillis();
            if (cached < 0) {
                failedUntil = now + 60_000L;
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            cached = -1;
            failedUntil = now + 60_000L;
        }
        return cached;
    }

    /** Lines "K|name|kelvin" and "A|name|kelvin" (both in Kelvin); the hottest value between 10 and 120 degrees wins. */
    static int parse(String text) {
        double best = -1;
        if (text == null) {
            return -1;
        }
        for (String line : text.split("\\R")) {
            String[] p = line.trim().split("\\|", 3);
            if (p.length < 3) {
                continue;
            }
            try {
                double c = Double.parseDouble(p[2].trim()) - 273.15;
                if (c >= 10.0 && c <= 120.0) {
                    best = Math.max(best, c);
                }
            } catch (NumberFormatException ignored) {
                // no value
            }
        }
        return best < 0 ? -1 : (int) Math.round(best);
    }
}

package cc.xypp.modernhud.config;

/** Where the CPU temperature is read from. */
public enum CpuTempSource {
    /** Linux: kernel sensors. Windows: LibreHardwareMonitor / OpenHardwareMonitor when it is running (otherwise N/A). */
    AUTO,
    /** Linux kernel sensors (coretemp, k10temp, zenpower ...). */
    LINUX_HWMON,
    /** LibreHardwareMonitor or OpenHardwareMonitor running in the background (Windows). Exact CPU package temperature. */
    HARDWARE_MONITOR,
    /** Windows thermal zones (ACPI), only when chosen. Needs no program, but it is a board sensor: often near room temperature, not following the CPU. */
    ACPI_THERMAL_ZONE
}

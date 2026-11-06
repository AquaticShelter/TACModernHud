package cc.xypp.modernhud.config;

/** Where the GPU temperature is read from. */
public enum GpuSource {
    /** Try the sources below in order and keep the first one that answers. */
    AUTO,
    /** NVIDIA drivers: runs {@code nvidia-smi} (Windows and Linux). */
    NVIDIA_SMI,
    /** Linux kernel sensors (amdgpu, nouveau, i915, xe). Needs no extra tool. */
    LINUX_HWMON,
    /** LibreHardwareMonitor or OpenHardwareMonitor running in the background (Windows, works for AMD and Intel). */
    HARDWARE_MONITOR
}

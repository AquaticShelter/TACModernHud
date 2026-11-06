package cc.xypp.modernhud.client.spec;

import cc.xypp.modernhud.client.perf.HudProfiler;
import net.minecraftforge.fml.ModList;

import java.util.function.Consumer;

/**
 * The text behind {@code /modernhud diag}: why a box shows N/A, which optimisation mods are present and how Modern HUD
 * cooperates with them, and what the HUD costs. Blocking (it runs the sensors once): call it from a background thread.
 */
public final class Diagnostics {

    private Diagnostics() {
    }

    public static void run(Consumer<String> out) {
        out.accept("Modern HUD diagnostics");
        out.accept("System: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + ", Java "
                + System.getProperty("java.version") + ", " + Runtime.getRuntime().availableProcessors() + " threads");
        for (String line : CpuSensors.diagnose()) {
            out.accept(line);
        }
        for (String line : GpuSensors.diagnose()) {
            out.accept(line);
        }
        out.accept("Network: live ping " + (LatencyProbe.active() ? "ACTIVE (server has Modern HUD), " + LatencyProbe.pingMs() + " ms"
                : "off (single player, or the server does not have Modern HUD: tab-list ping is used)")
                + ", TPS " + String.format(java.util.Locale.ROOT, "%.1f", TpsTracker.tps(System.nanoTime())));
        out.accept("Other mods that matter for HUD speed:");
        String[][] mods = {
                {"immediatelyfast", "ImmediatelyFast", "batches GUI / HUD drawing: the best companion, nothing to configure"},
                {"embeddium", "Embeddium", "speeds up the world, does not touch the HUD: no conflict"},
                {"rubidium", "Rubidium", "speeds up the world, does not touch the HUD: no conflict"},
                {"oculus", "Oculus (shaders)", "the HUD is drawn after the shader pass: no conflict"},
                {"modernfix", "ModernFix", "memory / loading only: no conflict"},
                {"ferritecore", "FerriteCore", "memory only: no conflict"},
                {"entityculling", "Entity Culling", "world only: no conflict"},
                {"dynamic_fps", "Dynamic FPS", "lowers FPS when the window is in the background: harmless"},
        };
        boolean any = false;
        for (String[] m : mods) {
            if (ModList.get().isLoaded(m[0])) {
                out.accept("  " + m[1] + ": installed, " + m[2]);
                any = true;
            }
        }
        if (!ModList.get().isLoaded("immediatelyfast")) {
            out.accept("  ImmediatelyFast: NOT installed. Recommended: it makes the HUD cheaper for every mod's HUD, Modern HUD included.");
        }
        if (!any) {
            out.accept("  (none of the usual optimisation mods found)");
        }
        for (String line : HudProfiler.report(SpecStats.frameMs())) {
            out.accept(line);
        }
    }
}

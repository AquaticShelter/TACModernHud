package cc.xypp.modernhud.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Re-bakes every config class into its static fields right now (used by the in-game config screen after Apply / Done). */
public final class ConfigRefresh {

    private static final Logger LOGGER = LogManager.getLogger("ModernHud/Config");

    private ConfigRefresh() {
    }

    public static void refreshAll() {
        run("hud", HudConfig::refresh);
        run("weapon", WeaponConfig::refresh);
        run("killstreak", KillstreakConfig::refresh);
        run("bossbar", BossBarConfig::refresh);
        run("hitindication", HitIndicationConfig::refresh);
        run("common", CommonConfig::refresh);
        run("compass", CompassConfig::refresh);
        run("sounds", SoundsConfig::refresh);
        run("motion", MotionConfig::refresh);
        run("specs", SpecsConfig::refresh);
        run("perf", PerfConfig::refresh);
    }

    private static void run(String name, Runnable refresh) {
        try {
            refresh.run();
        } catch (RuntimeException e) {
            // a spec that is not loaded in this context (e.g. COMMON on a dedicated server) has nothing to refresh
            LOGGER.debug("Config '{}' not refreshed: {}", name, e.toString());
        }
    }
}

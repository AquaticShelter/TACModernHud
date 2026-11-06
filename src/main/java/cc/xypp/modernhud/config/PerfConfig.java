package cc.xypp.modernhud.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Performance settings (file {@code modernhud-perf.toml}). Today: the overlay cache, which lets ANY HUD overlay, from
 * Modern HUD or from another mod, be redrawn only N times a second and shown from a stored picture in between.
 *
 * <p>Entries look like {@code "xaerominimap:xaero=30"} (overlay id = redraws per second). Manage them in game with
 * {@code /modernhud optimize}; nothing is cached until you ask for it.
 */
@Mod.EventBusSubscriber(modid = "modernhud", bus = Mod.EventBusSubscriber.Bus.MOD)
public class PerfConfig {
    private static final Builder BUILDER = new Builder();
    public static final ForgeConfigSpec SPEC;

    public static final BooleanValue CACHE_ENABLED;
    public static final ConfigValue<List<? extends String>> ENTRIES;
    public static final ConfigValue<List<? extends String>> BLOCKED;

    public static final BooleanValue AUTO_ENABLED;
    public static final ForgeConfigSpec.IntValue AUTO_WARMUP;
    public static final ForgeConfigSpec.IntValue AUTO_SCAN;
    public static final ForgeConfigSpec.DoubleValue AUTO_MIN_COST;
    public static final ForgeConfigSpec.IntValue AUTO_HZ;
    public static final ForgeConfigSpec.IntValue AUTO_SELF_HZ;
    public static final ConfigValue<List<? extends String>> AUTO_EXCLUDE;
    public static final BooleanValue AUTO_NOTIFY;

    public static volatile boolean cacheEnabled = true;
    /** Overlay id to redraws per second. Empty means: no overlay is cached. */
    public static volatile Map<String, Integer> hz = Map.of();
    public static volatile java.util.Set<String> blocked = java.util.Set.of();
    public static volatile boolean autoEnabled;
    public static volatile int autoWarmupSeconds = 5;
    public static volatile int autoScanSeconds = 15;
    public static volatile double autoMinCostMs = 0.10;
    public static volatile int autoHz = 30;
    public static volatile int autoSelfHz = 60;
    public static volatile List<String> autoExclude = List.of("minecraft:");
    public static volatile boolean autoNotify = true;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            refresh();
        }
    }

    public static void refresh() {
        cacheEnabled = CACHE_ENABLED.get();
        Map<String, Integer> map = new HashMap<>();
        for (String raw : ENTRIES.get()) {
            int eq = raw.lastIndexOf('=');
            if (eq <= 0) {
                continue;
            }
            try {
                int v = Integer.parseInt(raw.substring(eq + 1).trim());
                if (v > 0) {
                    map.put(raw.substring(0, eq).trim().toLowerCase(Locale.ROOT), Math.min(v, 240));
                }
            } catch (NumberFormatException ignored) {
                // a broken entry is skipped
            }
        }
        hz = Map.copyOf(map);
        java.util.Set<String> block = new java.util.HashSet<>();
        for (String raw : BLOCKED.get()) {
            block.add(raw.trim().toLowerCase(Locale.ROOT));
        }
        blocked = java.util.Set.copyOf(block);
        autoEnabled = AUTO_ENABLED.get();
        autoWarmupSeconds = AUTO_WARMUP.get();
        autoScanSeconds = AUTO_SCAN.get();
        autoMinCostMs = AUTO_MIN_COST.get();
        autoHz = AUTO_HZ.get();
        autoSelfHz = AUTO_SELF_HZ.get();
        List<String> ex = new ArrayList<>();
        for (String raw : AUTO_EXCLUDE.get()) {
            ex.add(raw.trim().toLowerCase(Locale.ROOT));
        }
        autoExclude = List.copyOf(ex);
        autoNotify = AUTO_NOTIFY.get();
    }

    /** True when auto-optimize must leave this overlay alone (excluded prefix or id, or it proved incompatible). */
    public static boolean isExcluded(String id) {
        if (blocked.contains(id)) {
            return true;
        }
        for (String prefix : autoExclude) {
            if (!prefix.isEmpty() && id.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** An overlay proved incompatible: stop caching it and never pick it again (until /modernhud optimize retry). */
    public static void block(String overlayId) {
        String id = overlayId.toLowerCase(Locale.ROOT);
        List<String> list = new ArrayList<>(BLOCKED.get());
        if (!list.contains(id)) {
            list.add(id);
        }
        BLOCKED.set(list);
        setEntry(id, 0);
    }

    public static void clearBlocked() {
        BLOCKED.set(new ArrayList<String>());
        refresh();
        try {
            BLOCKED.save();
        } catch (RuntimeException ignored) {
            // applied in memory
        }
    }

    /** Sets (or, with {@code redrawsPerSecond <= 0}, removes) the cache entry of one overlay and saves. */
    public static void setEntry(String overlayId, int redrawsPerSecond) {
        String id = overlayId.toLowerCase(Locale.ROOT);
        List<String> list = new ArrayList<>();
        for (String raw : ENTRIES.get()) {
            int eq = raw.lastIndexOf('=');
            if (eq > 0 && raw.substring(0, eq).trim().equalsIgnoreCase(id)) {
                continue;
            }
            list.add(raw);
        }
        if (redrawsPerSecond > 0) {
            list.add(id + "=" + Math.min(redrawsPerSecond, 240));
        }
        ENTRIES.set(list);
        refresh();
        try {
            ENTRIES.save();
        } catch (RuntimeException ignored) {
            // applied in memory; saving is best effort
        }
    }

    public static void clearEntries() {
        ENTRIES.set(new ArrayList<String>());
        refresh();
        try {
            ENTRIES.save();
        } catch (RuntimeException ignored) {
            // applied in memory
        }
    }

    static {
        BUILDER.comment("Overlay cache: redraw a HUD overlay only N times a second and show a stored picture in between.",
                "It saves the CPU time of that overlay on every other frame. Nothing is cached until you add it:",
                "/modernhud optimize auto  or  /modernhud optimize set <overlay id> <redraws per second>.").push("overlayCache");
        CACHE_ENABLED = BUILDER.comment("Master switch. Off draws every overlay every frame, as without this feature.")
                .translation("modernhud.config.perf_cache_enabled")
                .define("enabled", true);
        ENTRIES = BUILDER.comment("List of \"overlay id=redraws per second\", for example \"xaerominimap:xaero=30\".",
                        "An overlay that turns out to be incompatible (it switches framebuffers itself) is dropped for the rest",
                        "of the session automatically.")
                .defineListAllowEmpty("entries", new ArrayList<String>(), o -> o instanceof String);
        BLOCKED = BUILDER.comment("Overlays that proved incompatible with caching. They are never cached or picked automatically.",
                        "Filled by the mod; /modernhud optimize retry empties it.")
                .defineListAllowEmpty("blocked", new ArrayList<String>(), o -> o instanceof String);
        BUILDER.pop();

        BUILDER.comment("Auto-optimize: after you join a world, the mod waits a few seconds, measures every HUD overlay for a",
                "while, and caches the expensive ones by itself (see overlayCache). Needs overlayCache.enabled.").push("autoOptimize");
        AUTO_ENABLED = BUILDER.comment("Turn automatic scanning and optimising on. Off by default. A one-off scan is also",
                        "available with /modernhud optimize scan.")
                .translation("modernhud.config.perf_auto_enabled")
                .define("enabled", false);
        AUTO_WARMUP = BUILDER.comment("Seconds to wait after joining before measuring (the first seconds are loading spikes).")
                .translation("modernhud.config.perf_auto_warmup")
                .defineInRange("warmupSeconds", 5, 0, 60);
        AUTO_SCAN = BUILDER.comment("Seconds of play (HUD showing, no menu open) to measure. Hold the things you use (a gun) so their",
                        "overlays are measured too.")
                .translation("modernhud.config.perf_auto_scan")
                .defineInRange("scanSeconds", 15, 5, 120);
        AUTO_MIN_COST = BUILDER.comment("An overlay is cached when it costs at least this many milliseconds per frame on average",
                        "(counting the frames where it draws nothing too, so a popup that is rarely shown is not picked).")
                .translation("modernhud.config.perf_auto_min_cost")
                .defineInRange("minCostMs", 0.10, 0.01, 5.0);
        AUTO_HZ = BUILDER.comment("Redraws per second for the overlays of other mods. 30 looks identical for a minimap or a counter.")
                .translation("modernhud.config.perf_auto_hz")
                .defineInRange("redrawsPerSecond", 30, 10, 240);
        AUTO_SELF_HZ = BUILDER.comment("Redraws per second for Modern HUD's own overlays (they can sway with the camera, so higher).")
                .translation("modernhud.config.perf_auto_self_hz")
                .defineInRange("modernHudRedrawsPerSecond", 60, 20, 240);
        AUTO_EXCLUDE = BUILDER.comment("Overlay ids, or id prefixes ending in a colon, that are never picked. The default keeps the vanilla",
                        "HUD (hotbar, health) drawn every frame.")
                .defineListAllowEmpty("exclude", new ArrayList<String>(java.util.List.of("minecraft:")), o -> o instanceof String);
        AUTO_NOTIFY = BUILDER.comment("Tell you in chat what was optimised.")
                .translation("modernhud.config.perf_auto_notify")
                .define("notify", true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}

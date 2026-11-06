package cc.xypp.modernhud.client.perf;

import cc.xypp.modernhud.ModernHud;
import cc.xypp.modernhud.client.spec.SpecStats;
import cc.xypp.modernhud.config.PerfConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Auto-optimize: scans and optimises the HUD by itself.
 *
 * <p>After joining a world (when {@code autoOptimize.enabled} is on) it waits a few seconds for the loading spikes to pass,
 * measures what every HUD overlay of every mod costs per frame for {@code scanSeconds} of real play, then caches the
 * expensive ones with the overlay cache ({@link OverlayCache}). Only ticks in which a HUD is showing count, so opening a menu
 * pauses the scan instead of ruining it. An overlay that later proves incompatible is blocked and never picked again.
 * It does the same once on demand with {@code /modernhud optimize scan}.
 */
@Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AutoOptimizer {

    private enum State { IDLE, WAIT, SCAN }

    private static State state = State.IDLE;
    private static int ticks;
    private static int waitTicks;
    private static boolean manual;

    private AutoOptimizer() {
    }

    @SubscribeEvent
    public static void onLogin(final ClientPlayerNetworkEvent.LoggingIn event) {
        if (HudProfiler.isScanning()) {
            HudProfiler.finishScan();
        }
        if (PerfConfig.autoEnabled && PerfConfig.cacheEnabled) {
            begin(PerfConfig.autoWarmupSeconds * 20, false);
        } else {
            state = State.IDLE;
        }
    }

    /** {@code /modernhud optimize scan}: one scan now, whatever the config says. */
    public static void scanNow() {
        begin(0, true);
    }

    private static void begin(int warmupTicks, boolean byCommand) {
        state = State.WAIT;
        ticks = 0;
        waitTicks = warmupTicks;
        manual = byCommand;
    }

    @SubscribeEvent
    public static void onTick(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || state == State.IDLE) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // only count ticks in which the HUD is really on screen
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null) {
            return;
        }
        ticks++;
        if (state == State.WAIT) {
            if (ticks >= waitTicks) {
                HudProfiler.startScan();
                state = State.SCAN;
                ticks = 0;
                if (manual) {
                    say(mc, "Scanning the HUD for " + PerfConfig.autoScanSeconds + " seconds. Keep playing, hold a gun.");
                }
            }
        } else if (ticks >= PerfConfig.autoScanSeconds * 20) {
            state = State.IDLE;
            apply(mc, HudProfiler.finishScan());
        }
    }

    private static void apply(Minecraft mc, Map<String, Double> costs) {
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(costs.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        double saved = 0.0;
        int fps = Math.max(1, SpecStats.fps());
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Double> e : sorted) {
            String id = e.getKey().toLowerCase(Locale.ROOT);
            if (e.getValue() < PerfConfig.autoMinCostMs || PerfConfig.isExcluded(id) || PerfConfig.hz.containsKey(id)) {
                continue;
            }
            int hz = id.startsWith("modernhud:") ? PerfConfig.autoSelfHz : PerfConfig.autoHz;
            PerfConfig.setEntry(id, hz);
            OverlayCache.prune();
            saved += e.getValue() * Math.max(0.0, 1.0 - (double) hz / fps);
            lines.add(String.format(Locale.ROOT, "  %s: %.2f ms per frame -> %d redraws/s", id, e.getValue(), hz));
        }
        if (PerfConfig.autoNotify || manual) {
            if (lines.isEmpty()) {
                say(mc, "Nothing to optimise: no overlay costs " + String.format(Locale.ROOT, "%.2f", PerfConfig.autoMinCostMs)
                        + " ms or more (or they are already cached).");
            } else {
                say(mc, "Auto-optimize cached " + lines.size() + " overlay(s), about "
                        + String.format(Locale.ROOT, "%.2f", saved) + " ms per frame saved:");
                for (String l : lines) {
                    say(mc, l);
                }
                say(mc, "/modernhud optimize list shows them; /modernhud optimize set <id> 0 undoes one.");
            }
        }
    }

    private static void say(Minecraft mc, String text) {
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("[Modern HUD] " + text));
        }
    }
}

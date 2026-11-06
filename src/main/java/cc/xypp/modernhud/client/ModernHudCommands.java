package cc.xypp.modernhud.client;

import cc.xypp.modernhud.ModernHud;
import cc.xypp.modernhud.client.gui.config.ClientConfigHook;
import cc.xypp.modernhud.config.BossBarConfig;
import cc.xypp.modernhud.config.CompassConfig;
import cc.xypp.modernhud.config.ConfigPaths;
import cc.xypp.modernhud.config.ConfigPresets;
import cc.xypp.modernhud.config.ConfigRefresh;
import cc.xypp.modernhud.config.HitIndicationConfig;
import cc.xypp.modernhud.config.HudConfig;
import cc.xypp.modernhud.config.KillstreakConfig;
import cc.xypp.modernhud.config.MotionConfig;
import cc.xypp.modernhud.config.PerfConfig;
import cc.xypp.modernhud.config.SpecsConfig;
import cc.xypp.modernhud.config.WeaponConfig;
import cc.xypp.modernhud.sound.CombatSoundType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client commands, no server needed:
 * <pre>
 * /modernhud config                      open the config screen
 * /modernhud toggle &lt;element&gt;            turn one element on or off (also motion and specs)
 * /modernhud preset &lt;default|performance|minimal&gt;
 * /modernhud sound &lt;cue&gt;                  play a sound cue with your current style, volume and pitch
 * /modernhud reload                      re-read all config values
 * /modernhud info                        versions and detected mods, for bug reports
 * </pre>
 */
@Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ModernHudCommands {

    /** Element name -> {spec, path of its on/off option}. */
    private static final Map<String, Object[]> TOGGLES = new LinkedHashMap<>();

    static {
        TOGGLES.put("player", new Object[]{HudConfig.SPEC, "general.enabled"});
        TOGGLES.put("team", new Object[]{HudConfig.SPEC, "teamHud.enabled"});
        TOGGLES.put("compass", new Object[]{CompassConfig.SPEC, "general.enabled"});
        TOGGLES.put("killstreak", new Object[]{KillstreakConfig.SPEC, "killstreak.enabled"});
        TOGGLES.put("perktoast", new Object[]{KillstreakConfig.SPEC, "perkToast.enabled"});
        TOGGLES.put("bossbar", new Object[]{BossBarConfig.SPEC, "bossBar.enabled"});
        TOGGLES.put("hitindicator", new Object[]{HitIndicationConfig.SPEC, "hitIndication.enableHitIndication"});
        TOGGLES.put("ammoglow", new Object[]{WeaponConfig.SPEC, "ammoGlow.enabled"});
        TOGGLES.put("motion", new Object[]{MotionConfig.SPEC, "general.enabled"});
        TOGGLES.put("specs", new Object[]{SpecsConfig.SPEC, "general.enabled"});
    }

    private static final String[] CUES = {"hit", "kill", "headshot", "shieldhit", "shieldbreak"};

    private ModernHudCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("modernhud")
                .then(Commands.literal("config").executes(c -> openConfig()))
                .then(Commands.literal("reload").executes(c -> {
                    ConfigRefresh.refreshAll();
                    return say(c, "Config values re-read.");
                }))
                .then(Commands.literal("info").executes(ModernHudCommands::info))
                .then(Commands.literal("diag").executes(ModernHudCommands::diag))
                .then(Commands.literal("profile").executes(ModernHudCommands::profile)
                        .then(Commands.literal("gpu").then(Commands.argument("state", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"on", "off"}, b))
                                .executes(ModernHudCommands::profileGpu)))
                        .then(Commands.literal("all").then(Commands.argument("state", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"on", "off"}, b))
                                .executes(ModernHudCommands::profileAll))))
                .then(Commands.literal("optimize")
                        .then(Commands.literal("list").executes(ModernHudCommands::optimizeList))
                        .then(Commands.literal("auto").executes(ModernHudCommands::optimizeAuto))
                        .then(Commands.literal("reset").executes(ModernHudCommands::optimizeReset))
                        .then(Commands.literal("scan").executes(c -> {
                            cc.xypp.modernhud.client.perf.AutoOptimizer.scanNow();
                            return say(c, "Scan starts now (it only counts while the HUD is showing and no menu is open).");
                        }))
                        .then(Commands.literal("retry").executes(c -> {
                            cc.xypp.modernhud.client.perf.OverlayCache.retryAll();
                            return say(c, "Switched-off overlays will be tried again.");
                        }))
                        .then(Commands.literal("set").then(Commands.argument("overlay",
                                        net.minecraft.commands.arguments.ResourceLocationArgument.id())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        cc.xypp.modernhud.client.perf.HudProfiler.overlayCosts().stream()
                                                .map(e -> e.getKey()).toList(), b))
                                .then(Commands.argument("redrawsPerSecond", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 240))
                                        .executes(ModernHudCommands::optimizeSet)))))
                .then(Commands.literal("specs").then(Commands.literal("preset")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        java.util.Arrays.stream(SpecsConfig.Preset.values()).map(p -> p.name().toLowerCase()).toList(), b))
                                .executes(ModernHudCommands::specsPreset))))
                .then(Commands.literal("toggle").then(Commands.argument("element", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(TOGGLES.keySet(), b))
                        .executes(ModernHudCommands::toggle)))
                .then(Commands.literal("preset").then(Commands.argument("name", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ConfigPresets.NAMES, b))
                        .executes(ModernHudCommands::preset)))
                .then(Commands.literal("sound").then(Commands.argument("cue", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(CUES, b))
                        .executes(ModernHudCommands::sound)));
        event.getDispatcher().register(root);
    }

    private static int say(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendFailure(Component.literal(text));
        return 0;
    }

    private static int openConfig() {
        Minecraft mc = Minecraft.getInstance();
        // the chat screen closes right after a command runs, so open the config on the next tick
        mc.tell(() -> mc.setScreen(ClientConfigHook.create(mc.screen)));
        return 1;
    }

    private static int toggle(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "element").toLowerCase();
        Object[] t = TOGGLES.get(name);
        if (t == null) {
            return fail(c, "Unknown element '" + name + "'. Use one of: " + String.join(", ", TOGGLES.keySet()));
        }
        ForgeConfigSpec spec = (ForgeConfigSpec) t[0];
        ForgeConfigSpec.ConfigValue<?> cv = ConfigPaths.find(spec, (String) t[1]);
        if (cv == null || !(cv.get() instanceof Boolean current)) {
            return fail(c, "That option is not available in this version.");
        }
        ConfigPaths.set(spec, (String) t[1], !current);
        ConfigPaths.save(spec);
        ConfigRefresh.refreshAll();
        return say(c, name + " is now " + (!current ? "on" : "off") + ".");
    }

    private static int preset(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "name").toLowerCase();
        int n = ConfigPresets.apply(name);
        if (n < 0) {
            return fail(c, "Unknown preset '" + name + "'. Use one of: " + String.join(", ", ConfigPresets.NAMES));
        }
        return say(c, "Preset '" + name + "' applied (" + n + " values changed).");
    }

    private static int sound(CommandContext<CommandSourceStack> c) {
        String cue = StringArgumentType.getString(c, "cue").toLowerCase();
        switch (cue) {
            case "hit" -> ClientCombatSounds.play(CombatSoundType.HIT);
            case "kill" -> ClientCombatSounds.play(CombatSoundType.KILL);
            case "headshot" -> ClientCombatSounds.play(CombatSoundType.HEADSHOT_KILL);
            case "shieldhit" -> ClientCombatSounds.playShield(false);
            case "shieldbreak" -> ClientCombatSounds.playShield(true);
            default -> {
                return fail(c, "Unknown cue '" + cue + "'. Use one of: " + String.join(", ", CUES));
            }
        }
        return 1;
    }

    /** Sensor / compat report. The sensors are slow (PowerShell, nvidia-smi), so it runs off the game thread. */
    private static int diag(CommandContext<CommandSourceStack> c) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        say(c, "Collecting sensor information, this takes a few seconds...");
        Thread t = new Thread(() -> cc.xypp.modernhud.client.spec.Diagnostics.run(line -> mc.execute(() -> {
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal(line));
            }
        })), "modernhud-diag");
        t.setDaemon(true);
        t.start();
        return 1;
    }

    private static int profile(CommandContext<CommandSourceStack> c) {
        for (String line : cc.xypp.modernhud.client.perf.HudProfiler.report(cc.xypp.modernhud.client.spec.SpecStats.frameMs())) {
            say(c, line);
        }
        return 1;
    }

    private static int profileGpu(CommandContext<CommandSourceStack> c) {
        String state = StringArgumentType.getString(c, "state").toLowerCase();
        if (!state.equals("on") && !state.equals("off")) {
            return fail(c, "Use /modernhud profile gpu on  or  off");
        }
        return say(c, cc.xypp.modernhud.client.perf.HudProfiler.setGpu(state.equals("on")));
    }

    private static int profileAll(CommandContext<CommandSourceStack> c) {
        String state = StringArgumentType.getString(c, "state").toLowerCase();
        if (!state.equals("on") && !state.equals("off")) {
            return fail(c, "Use /modernhud profile all on  or  off");
        }
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (state.equals("on")) {
            cc.xypp.modernhud.client.perf.HudProfiler.clear();
            say(c, cc.xypp.modernhud.client.perf.HudProfiler.setAll(true)
                    + " Keep playing with the HUD showing (hold a gun): the report prints by itself in 15 seconds.");
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(15_000L);
                } catch (InterruptedException e) {
                    return;
                }
                mc.execute(() -> {
                    if (mc.player != null && cc.xypp.modernhud.client.perf.HudProfiler.allEnabled()) {
                        for (String line : cc.xypp.modernhud.client.perf.HudProfiler.report(
                                cc.xypp.modernhud.client.spec.SpecStats.frameMs())) {
                            mc.player.sendSystemMessage(Component.literal(line));
                        }
                    }
                });
            }, "modernhud-profile-report");
            t.setDaemon(true);
            t.start();
            return 1;
        }
        // turning it off clears the numbers, so print them first
        for (String line : cc.xypp.modernhud.client.perf.HudProfiler.report(cc.xypp.modernhud.client.spec.SpecStats.frameMs())) {
            say(c, line);
        }
        return say(c, cc.xypp.modernhud.client.perf.HudProfiler.setAll(false));
    }

    private static int optimizeList(CommandContext<CommandSourceStack> c) {
        for (String line : cc.xypp.modernhud.client.perf.OverlayCache.status()) {
            say(c, line);
        }
        return 1;
    }

    private static int optimizeSet(CommandContext<CommandSourceStack> c) {
        String id = net.minecraft.commands.arguments.ResourceLocationArgument.getId(c, "overlay").toString();
        int hz = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "redrawsPerSecond");
        PerfConfig.setEntry(id, hz);
        cc.xypp.modernhud.client.perf.OverlayCache.prune();
        return say(c, hz > 0 ? id + " is now redrawn " + hz + " times a second."
                : id + " is drawn every frame again.");
    }

    /** Lists the overlays that cost at least 0.12 ms (needs a /modernhud profile all measurement) and caches them. */
    private static int optimizeAuto(CommandContext<CommandSourceStack> c) {
        int n = 0;
        for (java.util.Map.Entry<String, Double> e : cc.xypp.modernhud.client.perf.HudProfiler.overlayCosts()) {
            if (e.getValue() < 0.12 || e.getKey().startsWith("minecraft:")) {
                continue; // cheap, or the vanilla HUD (a hotbar that updates 30 times a second feels wrong)
            }
            int hz = e.getKey().startsWith("modernhud:") ? 60 : 30;
            PerfConfig.setEntry(e.getKey(), hz);
            say(c, "  " + e.getKey() + " (" + String.format(java.util.Locale.ROOT, "%.2f", e.getValue()) + " ms) -> " + hz + " redraws/s");
            n++;
        }
        if (n == 0) {
            return say(c, "Nothing to do. Run /modernhud profile all on first, hold a gun for 15 seconds, then run this again.");
        }
        say(c, "Done. Look at the HUD: if an overlay looks wrong, /modernhud optimize set <id> 0 undoes it; "
                + "/modernhud optimize list shows what is active.");
        return n;
    }

    private static int optimizeReset(CommandContext<CommandSourceStack> c) {
        PerfConfig.clearEntries();
        cc.xypp.modernhud.client.perf.OverlayCache.prune();
        return say(c, "The overlay cache list is empty: every overlay is drawn every frame.");
    }

    private static int specsPreset(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "name").toUpperCase();
        try {
            SpecsConfig.applyPreset(SpecsConfig.Preset.valueOf(name));
        } catch (IllegalArgumentException e) {
            return fail(c, "Unknown preset. Use one of: minimal, standard, gaming, full");
        }
        return say(c, "Spec readout preset '" + name.toLowerCase() + "' applied.");
    }

    private static int info(CommandContext<CommandSourceStack> c) {
        say(c, "Modern HUD " + version("modernhud") + ", weapon HUD source: " + WeaponConfig.preferredHudSource);
        String[][] mods = {{"TACZ", "tacz"}, {"ModernLoadout", "mwloadout"}, {"lrtactical", "lrtactical"},
                {"Modern Ping", "modernping"}, {"Ping Wheel", "pingwheel"}, {"SuperbWarfare", "superbwarfare"},
                {"YACL", "yet_another_config_lib_v3"}};
        for (String[] m : mods) {
            say(c, "  " + m[0] + ": " + version(m[1]));
        }
        return 1;
    }

    private static String version(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(mc -> mc.getModInfo().getVersion().toString()).orElse("not installed");
    }
}

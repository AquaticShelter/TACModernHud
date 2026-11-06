package cc.xypp.modernhud.client.gui.config;

import cc.xypp.modernhud.config.BossBarConfig;
import cc.xypp.modernhud.config.CommonConfig;
import cc.xypp.modernhud.config.CompassConfig;
import cc.xypp.modernhud.config.HitIndicationConfig;
import cc.xypp.modernhud.config.HudConfig;
import cc.xypp.modernhud.config.KillstreakConfig;
import cc.xypp.modernhud.config.MotionConfig;
import cc.xypp.modernhud.config.PerfConfig;
import cc.xypp.modernhud.config.SpecsConfig;
import cc.xypp.modernhud.config.SoundsConfig;
import cc.xypp.modernhud.config.WeaponConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The YACL config screen. The storage stays the mod's ForgeConfigSpec files; this class only decides how the options
 * are LAID OUT: a handful of tabs, each split into collapsible groups, ordered the way you would look for things.
 * Every spec entry that the layout does not mention still shows up (tab "Other"), so a new option can never be lost.
 */
public final class ModernHudConfigScreen {

    private ModernHudConfigScreen() {
    }

    // ---------------------------------------------------------------------------------------------------- layout

    private static final class Group {
        final String name;
        final String tip;
        final boolean collapsed;
        final List<Source> sources = new ArrayList<>();

        Group(String name, String tip, boolean collapsed) {
            this.name = name;
            this.tip = tip;
            this.collapsed = collapsed;
        }
    }

    /** A spec plus the paths it contributes; a trailing {@code .*} takes every not-yet-used path under that prefix. */
    private record Source(ForgeConfigSpec spec, String[] patterns) {
    }

    private record Tab(String name, String tip, List<Group> groups) {
    }

    private static Group g(String name, String tip, ForgeConfigSpec spec, String... patterns) {
        Group grp = new Group(name, tip, false);
        grp.sources.add(new Source(spec, patterns));
        return grp;
    }

    private static Group gc(String name, String tip, ForgeConfigSpec spec, String... patterns) {
        Group grp = new Group(name, tip, true);
        grp.sources.add(new Source(spec, patterns));
        return grp;
    }

    private static Tab tab(String name, String tip, Group... groups) {
        return new Tab(name, tip, List.of(groups));
    }

    private static List<Tab> layout() {
        ForgeConfigSpec hud = HudConfig.SPEC;
        ForgeConfigSpec wpn = WeaponConfig.SPEC;
        ForgeConfigSpec ks = KillstreakConfig.SPEC;
        ForgeConfigSpec boss = BossBarConfig.SPEC;
        ForgeConfigSpec hit = HitIndicationConfig.SPEC;
        ForgeConfigSpec common = CommonConfig.SPEC;
        ForgeConfigSpec cmp = CompassConfig.SPEC;
        ForgeConfigSpec snd = SoundsConfig.SPEC;
        ForgeConfigSpec mot = MotionConfig.SPEC;
        ForgeConfigSpec spc = SpecsConfig.SPEC;
        ForgeConfigSpec prf = PerfConfig.SPEC;

        return List.of(
                tab("General", "Scaling, animation and glow settings shared by every HUD element.",
                        g("Scaling", "How the HUD scales with the GUI scale and resolution.", hud,
                                "animation.guiScaleMode", "animation.hudGuiScale", "animation.guiScaleReferenceHeight",
                                "animation.guiScaleSnap", "animation.pixelPerfectText", "animation.pixelPerfectElements", "animation.numberInput"),
                        g("Glow & animation", "Global glow quality / strength and animation speed.", hud,
                                "animation.glowQuality", "animation.glowIntensity", "animation.animations",
                                "animation.animationSpeed", "animation.healthAnimMs", "animation.absorptionAnimMs")),

                tab("Player", "Your own status widget: head, name, health / absorption bars and armor.",
                        g("Widget", "Master switches and what the widget shows.", hud,
                                "general.*"),
                        g("Position", "Where the widget is anchored.", hud, "position.*"),
                        g("Bars", "Bar size and how absorption points map onto it.", hud, "appearance.*"),
                        g("HP text", "The numeric health readout.", hud, "hpText.*"),
                        g("Armor text", "The numeric armor readout.", hud, "armor.*"),
                        gc("Panel & border", "Background panel and border of the widget.", hud, "panel.*")),

                tab("Team", "The teammate list.",
                        g("Team HUD", "A compact row per scoreboard teammate.", hud, "teamHud.*")),

                tab("Weapon", "The TACZ weapon widget: ammo, icons, fire mode, secondary weapon, gear column.",
                        g("General", "Source, visibility and the basics.", wpn,
                                "general.hudSource", "general.ignoreHideGui", "general.showName", "general.showFireMode",
                                "general.showReserveAmmo", "appearance.zeroPadAmmo"),
                        g("Position & size", "Anchor, offset and the size of the icon / ammo block.", wpn,
                                "position.*", "appearance.iconScale", "appearance.ammoTextScale",
                                "appearance.frameWidth", "appearance.frameHeight"),
                        g("Info box: size & spacing", "The box around the ammo numbers, gun name and fire mode: its height and the gaps between its rows.", wpn,
                                "appearance.infoBoxRise", "infoBox.nameGap", "infoBox.rowGap", "infoBox.gapToWeapon",
                                "infoBox.contentRaise", "infoBox.contentScale"),
                        g("Info box: panel & border", "Background panel and border of the ammo / name / fire-mode box (its own, separate from the weapon icon boxes).", wpn,
                                "infoBox.style.*"),
                        g("Ammo colours", "Digit colours, low-ammo threshold and the reserve readout.", wpn, "colors.*"),
                        g("Glow colour", "ONE colour for the weapon-switch flash, the selected weapon's corner glow and the ammo glow.", wpn,
                                "panel.weaponGlowColor", "panel.glowEnabled"),
                        g("Ammo glow", "Light glow inside the ammo box border (mag and reserve together) plus a halo on the magazine digits.", wpn,
                                "ammoGlow.*"),
                        g("Selected weapon box", "Attachment dots and the switch animation of the main icon box.", wpn,
                                "panel.showAttachmentDots",
                                "panel.attachmentDotColor", "panel.showSwitchSlide", "panel.switchSlideDistance",
                                "panel.showSwitchBorderFlash", "panel.switchBorderFlashColor", "panel.switchBorderFlashMs"),
                        g("Selected weapon corner glow", "Glow in the corners inside the selected weapon box (shared glow colour); every corner has its own strength.", wpn,
                                "gear.selectedGlow", "gear.selectedGlowStrength",
                                "gear.selectedGlowReach", "gear.selectedGlowTopLeft", "gear.selectedGlowTopRight",
                                "gear.selectedGlowBottomRight", "gear.selectedGlowBottomLeft", "gear.selectedGlowPulse",
                                "gear.selectedGlowPulseMs", "gear.selectedGlowPulseDepth", "gear.selectedGlowSwitchBoost"),
                        g("Heat", "Overheat bar for guns that have one.", wpn,
                                "general.heatDisplayMode", "general.heatBarColor", "general.heatBarLockedColor",
                                "general.heatFlashColor"),
                        g("Ammo-type icon", "The bullet icon next to the reserve count.", wpn,
                                "panel.showAmmoTypeIcon", "panel.ammoTypeIconScale"),
                        g("Secondary weapon", "The small dim icon of your other gun.", wpn,
                                "secondary.*", "panel.secondaryDotSize"),
                        g("Fire-type flash", "The centre-screen flash when you change fire mode.", wpn, "fireTypeFlash.*"),
                        g("Ammo alerts", "Reload / low-ammo / no-ammo messages.", wpn,
                                "ammoAlerts.enabled", "ammoAlerts.reloadAlertColor", "ammoAlerts.noAmmoAlertColor",
                                "ammoAlerts.lowAmmoAlertColor", "ammoAlerts.ammoAlertScale", "ammoAlerts.ammoAlertYOffset",
                                "ammoAlerts.ammoAlertPulse", "ammoAlerts.ammoAlertGlow"),
                        gc("Ammo alerts: look", "Opacity, fades, panel and border of the alerts.", wpn, "ammoAlerts.style.*"),
                        g("Gear slots", "Melee / throwable / consumable column. Auto layout collapses empty slots.", wpn,
                                "gear.showGear", "gear.countWholeInventory", "gear.glow", "gear.topDotsOuter", "gear.autoLayout",
                                "gear.centerIcons", "gear.spentMarker", "gear.spentColor"),
                        gc("Gear slots: look", "Opacity, fades, panel and border of the gear column.", wpn, "gear.style.*"),
                        gc("Weapon boxes: panel & border", "Panel and border of the weapon icon boxes (main + secondary).", wpn,
                                "panel.enabled", "panel.panelColor", "panel.panelOpacity", "panel.borderStyle",
                                "panel.borderColor", "panel.borderOpacity", "panel.borderLength", "panel.borderScale", "panel.borderOutset")),

                tab("Killstreak", "Killstreak slots and the perk toast.",
                        g("Killstreak bar", "Placement, slots and behaviour.", ks, "killstreak.*"),
                        g("Perk toast", "The popup when a perk unlocks.", ks, "perkToast.*")),

                tab("Boss bar", "Replacement boss bars.",
                        g("Boss bar", "Everything about the boss bars.", boss, "bossBar.*")),

                tab("Compass", "The heading compass (one reference-style tape).",
                        g("General", "Master switch.", cmp, "general.*"),
                        g("Tape", "Size, position, colour and fading of the tape.", cmp, "tape.*"),
                        g("Readout", "The bracketed direction / degrees box and place name.", cmp, "readout.*"),
                        g("Markers", "Master switch for gunfire and ping markers on the tape.", cmp, "markers.*"),
                        g("Ping markers: what shows", "Which pings (Modern Ping) appear: enemy players, hostile mobs, vehicles, locations...", cmp,
                                "pings.enabled", "pings.enemyPlayers", "pings.hostileMobs", "pings.vehicles", "pings.emptyVehicles",
                                "pings.vehicleClassPrefixes", "pings.vehicleNamespaces", "pings.debugLog", "pings.locations", "pings.otherEntities", "pings.maxDistance"),
                        g("Ping markers: look", "Size, distance scaling, edge arrows and colours of each marker type.", cmp, "pings.*"),
                        gc("TACZ gunfire markers", "How the heard-gunshot markers look and fade.", cmp, "gunfire.*")),

                tab("Motion", "The HUD sways with the camera: it lags behind when you turn and settles back on a spring.",
                        g("Motion", "Master switch, overall strength and direction.", mot, "general.*"),
                        g("Spring", "How far the HUD moves and how it settles back.", mot, "physics.*"),
                        g("Per-element strength", "How strongly each element follows the camera. Different values give depth.", mot, "layers.*"),
                        g("Vanilla HUD", "Also move the vanilla hotbar and bars (never the crosshair).", mot, "vanilla.*"),
                        gc("Output", "Whole-pixel snapping.", mot, "output.*")),

                tab("Performance", "Draws HUD overlays (also other mods') less often to save CPU. Lists are in modernhud-perf.toml.",
                        g("Overlay cache", "Master switch. Entries are managed with /modernhud optimize.", prf, "overlayCache.enabled"),
                        g("Auto-optimize", "Scan the HUD after joining a world and cache the expensive overlays by itself.", prf,
                                "autoOptimize.enabled", "autoOptimize.warmupSeconds", "autoOptimize.scanSeconds",
                                "autoOptimize.minCostMs", "autoOptimize.redrawsPerSecond",
                                "autoOptimize.modernHudRedrawsPerSecond", "autoOptimize.notify")),

                tab("Specs", "FPS, ping, packet loss, GPU temperature and time in small boxes, plus optional CPU / GPU / memory monitoring.",
                        g("Readout", "Master switch and when it shows.", spc, "general.*"),
                        g("Items", "Which boxes show, their order and their words.", spc, "items.*"),
                        g("Position", "Anchor, offset and size.", spc, "position.*"),
                        g("Layout", "Row or column, box size, gaps and character width.", spc, "layout.*"),
                        g("Text", "Colour, capitals and shadow.", spc, "text.*"),
                        g("Style: panel & border", "Background, border and fade of the boxes.", spc, "style.*"),
                        g("Warning colours", "When a number turns yellow or red.", spc, "colors.*"),
                        g("FPS", "How often the FPS number refreshes and the 1 % low window.", spc, "fps.*"),
                        g("Ping", "Live ping probe, source and smoothing.", spc, "ping.*"),
                        g("Server TPS", "How the TPS is averaged.", spc, "tps.*"),
                        g("Frame time graph", "Bars, slice length and scale of the graph.", spc, "graph.*"),
                        g("Packet loss", "The estimate window and timeout.", spc, "loss.*"),
                        g("CPU, RAM & game memory", "How often they are read, memory format, CPU temperature source.", spc, "hardware.*"),
                        g("GPU sensors", "Where GPU temperature, usage, video memory, power and clock are read from.", spc, "gpu.*"),
                        g("Clock", "12 / 24 hour, seconds and language.", spc, "time.*")),

                tab("Sounds", "Hit / kill / headshot / shield cues: style 1-3, volume, pitch, and priority over TACZ / SuperbWarfare sounds.",
                        g("Style", "Pick the sound set (1 = classic, 2, 3) for everything, or per cue.", snd, "style.*"),
                        g("Hit", "Ordinary hit (no shield involved).", snd, "hit.*"),
                        g("Kill", "Kill cue.", snd, "kill.*"),
                        g("Headshot kill", "Headshot-kill cue (TACZ only).", snd, "headshotKill.*"),
                        g("Shield hit", "A hit on an active shield; plays INSTEAD of the ordinary hit cue.", snd, "shieldHit.*"),
                        g("Shield break", "The shield was fully broken.", snd, "shieldBreak.*"),
                        g("Override TACZ / SuperbWarfare", "Silence the hit / kill sounds of those mods so yours always win.", snd, "override.*"),
                        gc("Server rules", "WHEN a cue fires (server decides): toggles, shield-first priority, cooldowns.", common, "combatSounds.*")),

                tab("Feedback", "Hit indicators, hit / break feedback and sounds.",
                        g("Hit indicators", "The ring that shows where damage comes from.", hit, "hitIndication.*"),
                        g("Shield feedback", "Hit / break feedback for shields and absorption.", common, "shieldFeedback.*"),
                        g("Shield feedback: your side", "Whether you also get the cue + icon when YOU are the one hit.", hud, "sounds.*"),
                        g("Feedback icon", "The hit / break icon: position and size.", hud, "feedbackIcon.*"),
                        g("Feedback icon timing", "How long each icon stays.", hud, "feedbackIconTiming.*", "animation.feedbackOpacity",
                                "animation.feedbackFadeMs"))
        );
    }

    // ---------------------------------------------------------------------------------------------------- build

    /** Builds a fresh screen (YACL needs a new instance for every screen). */
    public static Screen create(Screen parent) {
        // entries per spec + which of them the layout has already used
        Map<ForgeConfigSpec, List<SpecOptions.Entry>> all = new LinkedHashMap<>();
        for (ForgeConfigSpec s : new ForgeConfigSpec[]{HudConfig.SPEC, WeaponConfig.SPEC, KillstreakConfig.SPEC,
                BossBarConfig.SPEC, HitIndicationConfig.SPEC, CommonConfig.SPEC, CompassConfig.SPEC, SoundsConfig.SPEC,
                MotionConfig.SPEC, SpecsConfig.SPEC, PerfConfig.SPEC}) {
            all.put(s, SpecOptions.entries(s));
        }
        Map<ForgeConfigSpec, Set<String>> used = new LinkedHashMap<>();
        all.keySet().forEach(s -> used.put(s, new LinkedHashSet<>()));
        Set<ForgeConfigSpec> touched = new LinkedHashSet<>();
        List<PreviewScreen.Pair> pairs = new ArrayList<>();
        PreviewScreen[] screen = new PreviewScreen[1];

        YetAnotherConfigLib.Builder root = YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Modern HUD"));

        for (Tab t : layout()) {
            ConfigCategory.Builder cat = ConfigCategory.createBuilder()
                    .name(Component.literal(t.name()))
                    .tooltip(Component.literal(t.tip()));
            for (Group grp : t.groups()) {
                List<Option<?>> options = new ArrayList<>();
                for (Source src : grp.sources) {
                    List<SpecOptions.Entry> entries = all.get(src.spec());
                    for (String pat : src.patterns()) {
                        for (SpecOptions.Entry en : entries) {
                            if (used.get(src.spec()).contains(en.path()) || !matches(pat, en.path())) {
                                continue;
                            }
                            Option<?> o = SpecOptions.create(en);
                            if (o != null) {
                                pairs.add(new PreviewScreen.Pair(en, o));
                                options.add(o);
                                used.get(src.spec()).add(en.path());
                                touched.add(src.spec());
                            }
                        }
                    }
                }
                if (options.isEmpty()) {
                    continue;
                }
                cat.group(OptionGroup.createBuilder()
                        .name(Component.literal(grp.name))
                        .description(OptionDescription.of(Component.literal(grp.tip)))
                        .options(options)
                        .collapsed(grp.collapsed)
                        .build());
            }
            root.category(cat.build());
        }

        // safety net: anything the layout did not place
        List<Option<?>> other = new ArrayList<>();
        for (Map.Entry<ForgeConfigSpec, List<SpecOptions.Entry>> e : all.entrySet()) {
            for (SpecOptions.Entry en : e.getValue()) {
                if (!used.get(e.getKey()).contains(en.path())) {
                    Option<?> o = SpecOptions.create(en);
                    if (o != null) {
                        pairs.add(new PreviewScreen.Pair(en, o));
                        other.add(o);
                        touched.add(e.getKey());
                    }
                }
            }
        }
        if (!other.isEmpty()) {
            root.category(ConfigCategory.createBuilder()
                    .name(Component.literal("Other"))
                    .tooltip(Component.literal("Options that are not in any tab above."))
                    .group(OptionGroup.createBuilder().name(Component.literal("Other")).options(other).build())
                    .build());
        }

        // write every spec's file once when the user confirms; Forge's config watcher then reloads the static fields
        root.save(() -> {
            for (ForgeConfigSpec s : all.keySet()) {
                try {
                    for (SpecOptions.Entry en : all.get(s)) {
                        en.value().save();
                        break; // ConfigValue#save() saves the whole spec file
                    }
                } catch (RuntimeException ignored) {
                    // spec not loaded (e.g. server-only COMMON config on a dedicated server): nothing to write
                }
            }
            // apply NOW (do not wait for Forge's file watcher): colours, toggles and sizes change the moment you press Apply / Done
            cc.xypp.modernhud.config.ConfigRefresh.refreshAll();
        });
        screen[0] = new PreviewScreen(root.build(), parent, pairs);
        return screen[0];
    }

    private static boolean matches(String pattern, String path) {
        if (pattern.endsWith(".*")) {
            return path.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return pattern.equals(path);
    }
}

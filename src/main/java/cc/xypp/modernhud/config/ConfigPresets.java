package cc.xypp.modernhud.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.LinkedHashSet;
import java.util.Set;

/** Ready-made setups, applied with {@code /modernhud preset <name>}. They only change values the player can also change by hand. */
public final class ConfigPresets {

    public static final String[] NAMES = {"default", "performance", "minimal"};

    private ConfigPresets() {
    }

    /** A value to set: {spec, path, value}. */
    private record Setting(ForgeConfigSpec spec, String path, Object value) {
    }

    private static Setting s(ForgeConfigSpec spec, String path, Object value) {
        return new Setting(spec, path, value);
    }

    private static Setting[] performance() {
        return new Setting[]{
                s(HudConfig.SPEC, "animation.glowQuality", "LOW"),
                s(HudConfig.SPEC, "hpText.glowEnabled", false),
                s(WeaponConfig.SPEC, "ammoGlow.haloRadius", 0),
                s(WeaponConfig.SPEC, "ammoGlow.pulse", false),
                s(WeaponConfig.SPEC, "ammoGlow.lowAmmoPulse", false),
                s(WeaponConfig.SPEC, "gear.selectedGlowPulse", false),
                s(HitIndicationConfig.SPEC, "hitIndication.glow", false),
                s(BossBarConfig.SPEC, "bossBar.barGlow", false),
                s(BossBarConfig.SPEC, "bossBar.headGlow", false),
                s(BossBarConfig.SPEC, "bossBar.damageTrail", false),
                s(KillstreakConfig.SPEC, "perkToast.glow", false),
        };
    }

    private static Setting[] minimal() {
        return new Setting[]{
                s(HudConfig.SPEC, "teamHud.enabled", false),
                s(KillstreakConfig.SPEC, "killstreak.enabled", false),
                s(KillstreakConfig.SPEC, "perkToast.enabled", false),
                s(BossBarConfig.SPEC, "bossBar.enabled", false),
                s(WeaponConfig.SPEC, "ammoGlow.enabled", false),
                s(WeaponConfig.SPEC, "gear.selectedGlow", false),
                s(MotionConfig.SPEC, "general.enabled", false),
        };
    }

    /** Applies a preset by name. Returns the number of values changed, or -1 for an unknown name. */
    public static int apply(String name) {
        Set<ForgeConfigSpec> touched = new LinkedHashSet<>();
        int changed = 0;
        switch (name) {
            case "default" -> {
                for (ForgeConfigSpec spec : all()) {
                    int n = ConfigPaths.resetAll(spec);
                    if (n > 0) {
                        touched.add(spec);
                        changed += n;
                    }
                }
            }
            case "performance", "minimal" -> {
                for (Setting st : name.equals("performance") ? performance() : minimal()) {
                    ForgeConfigSpec.ConfigValue<?> cv = ConfigPaths.find(st.spec(), st.path());
                    if (cv != null && ConfigPaths.set(st.spec(), st.path(), st.value())) {
                        touched.add(st.spec());
                        changed++;
                    }
                }
            }
            default -> {
                return -1;
            }
        }
        for (ForgeConfigSpec spec : touched) {
            ConfigPaths.save(spec);
        }
        ConfigRefresh.refreshAll();
        return changed;
    }

    /** Every spec of this mod that is editable on the client. */
    public static ForgeConfigSpec[] all() {
        return new ForgeConfigSpec[]{HudConfig.SPEC, WeaponConfig.SPEC, KillstreakConfig.SPEC, BossBarConfig.SPEC,
                HitIndicationConfig.SPEC, CompassConfig.SPEC, SoundsConfig.SPEC, MotionConfig.SPEC, SpecsConfig.SPEC};
    }
}

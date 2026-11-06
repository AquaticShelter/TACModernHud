package cc.xypp.modernhud.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Your personal combat-sound settings: sound STYLE (1 / 2 / 3), volume and pitch for every cue, and whether ModernHud's
 * cues override the hit / kill sounds of TACZ and SuperbWarfare. Pure client side: the server only says WHEN a cue
 * fires (see CommonConfig), each player decides how it sounds.
 */
@Mod.EventBusSubscriber(modid = "modernhud", bus = Mod.EventBusSubscriber.Bus.MOD)
public class SoundsConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.IntValue STYLE;
    private static final ForgeConfigSpec.IntValue HIT_STYLE;
    private static final ForgeConfigSpec.IntValue KILL_STYLE;
    private static final ForgeConfigSpec.IntValue HEADSHOT_STYLE;
    private static final ForgeConfigSpec.IntValue BREAK_STYLE;

    private static final ForgeConfigSpec.BooleanValue HIT_ON;
    private static final ForgeConfigSpec.DoubleValue HIT_VOL;
    private static final ForgeConfigSpec.DoubleValue HIT_PITCH;
    private static final ForgeConfigSpec.BooleanValue KILL_ON;
    private static final ForgeConfigSpec.DoubleValue KILL_VOL;
    private static final ForgeConfigSpec.DoubleValue KILL_PITCH;
    private static final ForgeConfigSpec.BooleanValue HEADSHOT_ON;
    private static final ForgeConfigSpec.DoubleValue HEADSHOT_VOL;
    private static final ForgeConfigSpec.DoubleValue HEADSHOT_PITCH;
    private static final ForgeConfigSpec.BooleanValue SHIELD_HIT_ON;
    private static final ForgeConfigSpec.DoubleValue SHIELD_HIT_VOL;
    private static final ForgeConfigSpec.DoubleValue SHIELD_HIT_PITCH;
    private static final ForgeConfigSpec.BooleanValue SHIELD_BREAK_ON;
    private static final ForgeConfigSpec.DoubleValue SHIELD_BREAK_VOL;
    private static final ForgeConfigSpec.DoubleValue SHIELD_BREAK_PITCH;

    private static final ForgeConfigSpec.BooleanValue OVERRIDE_OTHERS;
    private static final ForgeConfigSpec.ConfigValue<String> OVERRIDE_PATTERNS;
    private static final ForgeConfigSpec.BooleanValue DEBUG_LOG;

    public static int style, hitStyle, killStyle, headshotStyle, breakStyle;
    public static boolean hitOn, killOn, headshotOn, shieldHitOn, shieldBreakOn;
    public static float hitVolume, hitPitch, killVolume, killPitch, headshotVolume, headshotPitch;
    public static float shieldHitVolume, shieldHitPitch, shieldBreakVolume, shieldBreakPitch;
    public static boolean overrideOthers;
    public static boolean debugLog;
    private static volatile List<Pattern> overridePatterns = List.of();

    /** Style of a cue: its own override when set (1-3), otherwise the global style. */
    public static int styleFor(int override) {
        return override > 0 ? override : style;
    }

    /** True when ModernHud should silence this sound of another mod (see {@code override.patterns}). */
    public static boolean shouldOverride(String id) {
        for (Pattern p : overridePatterns) {
            if (p.matcher(id).matches()) {
                return true;
            }
        }
        return false;
    }

    private static List<Pattern> compile(String csv) {
        List<Pattern> out = new ArrayList<>();
        for (String part : csv.split(",")) {
            String glob = part.trim().toLowerCase();
            if (glob.isEmpty()) {
                continue;
            }
            StringBuilder re = new StringBuilder();
            for (char c : glob.toCharArray()) {
                if (c == '*') {
                    re.append(".*");
                } else {
                    re.append(Pattern.quote(String.valueOf(c)));
                }
            }
            out.add(Pattern.compile(re.toString()));
        }
        return out;
    }

    static {
        BUILDER.comment("Sound style used by the cues: 1 = classic, 2 = style 2, 3 = style 3. A cue's own style",
                        "overrides this unless it is 0.").push("style");
        STYLE = BUILDER.comment("Sound style for all cues: 1 = classic, 2 = style 2, 3 = style 3.")
                .translation("modernhud.config.sound_style")
                .defineInRange("style", 1, 1, 3);
        HIT_STYLE = BUILDER.comment("Style for the hit cue only. 0 uses the style above.")
                .translation("modernhud.config.sound_hit_style").defineInRange("hitStyle", 0, 0, 3);
        KILL_STYLE = BUILDER.comment("Style for the kill cue only. 0 uses the style above.")
                .translation("modernhud.config.sound_kill_style").defineInRange("killStyle", 0, 0, 3);
        HEADSHOT_STYLE = BUILDER.comment("Style for the headshot-kill cue only. 0 uses the style above.")
                .translation("modernhud.config.sound_headshot_style").defineInRange("headshotKillStyle", 0, 0, 3);
        BREAK_STYLE = BUILDER.comment("Style for the shield-break cue only. Styles 1 and 2 share one sound, 3 has its own. 0",
                        "uses the style above.")
                .translation("modernhud.config.sound_break_style").defineInRange("shieldBreakStyle", 0, 0, 3);
        BUILDER.pop();

        BUILDER.comment("Ordinary hit (no shield involved)").push("hit");
        HIT_ON = BUILDER.comment("Hear the hit cue.").translation("modernhud.config.sound_hit_on").define("enabled", true);
        HIT_VOL = BUILDER.comment("Volume, 0.0-2.0 (Minecraft clamps the final loudness at full).")
                .translation("modernhud.config.sound_hit_volume").defineInRange("volume", 1.0, 0.0, 2.0);
        HIT_PITCH = BUILDER.comment("Pitch, 0.5-2.0.")
                .translation("modernhud.config.sound_hit_pitch").defineInRange("pitch", 1.0, 0.5, 2.0);
        BUILDER.pop();

        BUILDER.comment("Kill").push("kill");
        KILL_ON = BUILDER.comment("Hear the kill cue.").translation("modernhud.config.sound_kill_on").define("enabled", true);
        KILL_VOL = BUILDER.comment("Volume, 0.0-2.0.")
                .translation("modernhud.config.sound_kill_volume").defineInRange("volume", 1.0, 0.0, 2.0);
        KILL_PITCH = BUILDER.comment("Pitch, 0.5-2.0.")
                .translation("modernhud.config.sound_kill_pitch").defineInRange("pitch", 1.0, 0.5, 2.0);
        BUILDER.pop();

        BUILDER.comment("Headshot kill").push("headshotKill");
        HEADSHOT_ON = BUILDER.comment("Hear the headshot-kill cue.").translation("modernhud.config.sound_headshot_on").define("enabled", true);
        HEADSHOT_VOL = BUILDER.comment("Volume, 0.0-2.0.")
                .translation("modernhud.config.sound_headshot_volume").defineInRange("volume", 1.0, 0.0, 2.0);
        HEADSHOT_PITCH = BUILDER.comment("Pitch, 0.5-2.0.")
                .translation("modernhud.config.sound_headshot_pitch").defineInRange("pitch", 1.0, 0.5, 2.0);
        BUILDER.pop();

        BUILDER.comment("A hit that lands on an active shield. Plays instead of the ordinary hit cue.").push("shieldHit");
        SHIELD_HIT_ON = BUILDER.comment("Hear the shield-hit cue.").translation("modernhud.config.sound_shield_hit_on").define("enabled", true);
        SHIELD_HIT_VOL = BUILDER.comment("Volume, 0.0-2.0.")
                .translation("modernhud.config.sound_shield_hit_volume").defineInRange("volume", 1.0, 0.0, 2.0);
        SHIELD_HIT_PITCH = BUILDER.comment("Pitch, 0.5-2.0.")
                .translation("modernhud.config.sound_shield_hit_pitch").defineInRange("pitch", 1.0, 0.5, 2.0);
        BUILDER.pop();

        BUILDER.comment("Shield fully broken").push("shieldBreak");
        SHIELD_BREAK_ON = BUILDER.comment("Hear the shield-break cue.").translation("modernhud.config.sound_shield_break_on").define("enabled", true);
        SHIELD_BREAK_VOL = BUILDER.comment("Volume, 0.0-2.0.")
                .translation("modernhud.config.sound_shield_break_volume").defineInRange("volume", 2.0, 0.0, 2.0);
        SHIELD_BREAK_PITCH = BUILDER.comment("Pitch, 0.5-2.0.")
                .translation("modernhud.config.sound_shield_break_pitch").defineInRange("pitch", 1.0, 0.5, 2.0);
        BUILDER.pop();

        BUILDER.comment("Silence the hit and kill sounds of other mods so only these cues are heard.").push("override");
        OVERRIDE_OTHERS = BUILDER
                .comment("Cancel sounds from TACZ and SuperbWarfare that match the patterns below, so their hit",
                        "and kill cues do not play over yours.")
                .translation("modernhud.config.sound_override")
                .define("enabled", true);
        OVERRIDE_PATTERNS = BUILDER
                .comment("Sound ids to silence, separated by commas. * is a wildcard, for example tacz:*kill*.",
                        "If a sound still plays, turn on debugLog, trigger it and copy its id from",
                        "logs/latest.log (lines starting with ModernHud sound:).")
                .translation("modernhud.config.sound_override_patterns")
                .define("patterns", "tacz:*kill*,tacz:*headshot*,tacz:*hit_mark*,tacz:*hitmarker*,"
                        + "superbwarfare:*indication*,superbwarfare:*headshot*,superbwarfare:*kill*,superbwarfare:*hitmarker*,superbwarfare:hit");
        DEBUG_LOG = BUILDER
                .comment("Write the id of every sound played by TACZ or SuperbWarfare to logs/latest.log, to",
                        "find out what to silence.")
                .translation("modernhud.config.sound_debug_log")
                .define("debugLog", false);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            refresh();
        }
    }

    /** Re-reads the spec (also called right after the in-game screen saves). */
    public static void refresh() {
        style = STYLE.get();
        hitStyle = HIT_STYLE.get();
        killStyle = KILL_STYLE.get();
        headshotStyle = HEADSHOT_STYLE.get();
        breakStyle = BREAK_STYLE.get();
        hitOn = HIT_ON.get();
        hitVolume = HIT_VOL.get().floatValue();
        hitPitch = HIT_PITCH.get().floatValue();
        killOn = KILL_ON.get();
        killVolume = KILL_VOL.get().floatValue();
        killPitch = KILL_PITCH.get().floatValue();
        headshotOn = HEADSHOT_ON.get();
        headshotVolume = HEADSHOT_VOL.get().floatValue();
        headshotPitch = HEADSHOT_PITCH.get().floatValue();
        shieldHitOn = SHIELD_HIT_ON.get();
        shieldHitVolume = SHIELD_HIT_VOL.get().floatValue();
        shieldHitPitch = SHIELD_HIT_PITCH.get().floatValue();
        shieldBreakOn = SHIELD_BREAK_ON.get();
        shieldBreakVolume = SHIELD_BREAK_VOL.get().floatValue();
        shieldBreakPitch = SHIELD_BREAK_PITCH.get().floatValue();
        overrideOthers = OVERRIDE_OTHERS.get();
        overridePatterns = compile(OVERRIDE_PATTERNS.get());
        debugLog = DEBUG_LOG.get();
    }
}

package cc.xypp.modernhud.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Common configuration - runs on the server (and the integrated singleplayer
 * server). Controls whether "hit landed on active Absorption" and
 * "Absorption fully depleted" events are detected/sent at all.
 *
 * Deliberately does NOT have a "broadcast to everyone nearby" option - by
 * design, only the attacker and the victim are ever notified, so there is
 * nothing here to configure about who else can hear it.
 */
@Mod.EventBusSubscriber(modid = "modernhud", bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLE_HIT_FEEDBACK;
    private static final ForgeConfigSpec.BooleanValue ENABLE_BREAK_FEEDBACK;
    private static final ForgeConfigSpec.BooleanValue CLEAR_ABSORPTION_ON_EFFECT_END;
    private static final ForgeConfigSpec.BooleanValue COMBAT_SOUNDS;
    private static final ForgeConfigSpec.BooleanValue HIT_SOUND;
    private static final ForgeConfigSpec.BooleanValue KILL_SOUND;
    private static final ForgeConfigSpec.BooleanValue HEADSHOT_KILL_SOUND;
    private static final ForgeConfigSpec.BooleanValue SHIELD_PRIORITY;
    private static final ForgeConfigSpec.BooleanValue RESTRICT_TO_TACZ;
    private static final ForgeConfigSpec.IntValue HIT_COOLDOWN;
    private static final ForgeConfigSpec.IntValue KILL_COOLDOWN;

    public static final ForgeConfigSpec SPEC;

    public static boolean enableHitFeedback;
    public static boolean enableBreakFeedback;
    public static boolean clearAbsorptionOnEffectEnd;
    public static boolean combatSoundsEnabled;
    public static boolean hitSoundEnabled;
    public static boolean killSoundEnabled;
    public static boolean headshotKillSoundEnabled;
    public static boolean shieldSoundPriority;
    public static boolean restrictCombatSoundsToTacz;
    public static int hitSoundCooldownMs;
    public static int killSoundCooldownMs;

    static {
        BUILDER.comment("Server-side detection of Absorption being hit or depleted. Only the attacker and the",
                        "victim are notified.")
                .push("shieldFeedback");

        ENABLE_HIT_FEEDBACK = BUILDER
                .comment("Notify the attacker and the victim (sound only) when a hit lands on Absorption without",
                        "depleting it.")
                .translation("modernhud.config.enable_hit_feedback")
                .define("enableHitFeedback", true);

        ENABLE_BREAK_FEEDBACK = BUILDER
                .comment("Notify the attacker (sound and icon) and the victim (sound only) when Absorption is",
                        "fully depleted.")
                .translation("modernhud.config.enable_break_feedback")
                .define("enableBreakFeedback", true);

        CLEAR_ABSORPTION_ON_EFFECT_END = BUILDER
                .comment("In vanilla, absorption hearts stay after the Absorption effect ends. With this on they",
                        "are cleared when the effect ends, so the HUD bar clears with it. Turn it off for",
                        "vanilla behavior.")
                .translation("modernhud.config.clear_absorption_on_effect_end")
                .define("clearAbsorptionOnEffectEnd", true);

        BUILDER.pop();

        BUILDER.comment("Hit, kill and headshot-kill cues for the attacker. The server decides when they play;",
                        "each player picks style, volume and pitch in their own Sounds settings. Shield cues",
                        "use the shieldFeedback options above.")
                .push("combatSounds");

        COMBAT_SOUNDS = BUILDER
                .comment("Master switch for the hit / kill / headshot-kill cues.")
                .translation("modernhud.config.combat_sounds")
                .define("enabled", true);
        HIT_SOUND = BUILDER
                .comment("Play a cue to the attacker on a non-fatal hit.")
                .translation("modernhud.config.combat_sound_hit")
                .define("hitSound", true);
        KILL_SOUND = BUILDER
                .comment("Play a cue to the attacker on a kill.")
                .translation("modernhud.config.combat_sound_kill")
                .define("killSound", true);
        HEADSHOT_KILL_SOUND = BUILDER
                .comment("Play a cue on a headshot kill. Only TACZ reports headshots. It replaces the kill cue.")
                .translation("modernhud.config.combat_sound_headshot")
                .define("headshotKillSound", true);
        SHIELD_PRIORITY = BUILDER
                .comment("While the victim still has Absorption, play the shield cue (shield hit or break)",
                        "instead of the hit cue. Once the shield is gone the hit cue plays. A killing blow",
                        "plays the kill cue and no shield cue.")
                .translation("modernhud.config.combat_sound_shield_priority")
                .define("shieldPriority", true);
        RESTRICT_TO_TACZ = BUILDER
                .comment("true: only TACZ damage triggers hit and kill cues. false: any damage a player deals",
                        "does (melee, bows, SuperbWarfare, other mods). Headshots always need TACZ.")
                .translation("modernhud.config.combat_sound_restrict_tacz")
                .define("restrictToTacz", false);
        HIT_COOLDOWN = BUILDER
                .comment("Minimum time between hit cues for one player in milliseconds. Prevents sound spam from",
                        "fast guns. 0 means none.")
                .translation("modernhud.config.combat_sound_hit_cooldown")
                .defineInRange("hitCooldownMs", 20, 0, 2000);
        KILL_COOLDOWN = BUILDER
                .comment("Minimum time (ms) between kill cues per player (softens multi-kills). 0 = none.")
                .translation("modernhud.config.combat_sound_kill_cooldown")
                .defineInRange("killCooldownMs", 0, 0, 2000);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        try {
            refresh();
        } catch (IllegalStateException notLoadedHere) {
            // this spec belongs to the other side (client configs on a dedicated server): nothing to read
        }
    }

    /**
     * Re-reads every value of this spec into the static fields the renderers use. Called by the config event AND right
     * after the in-game screen saves, so a change applies instantly instead of waiting for Forge's file watcher.
     */
    public static void refresh() {
        enableHitFeedback = ENABLE_HIT_FEEDBACK.get();
        enableBreakFeedback = ENABLE_BREAK_FEEDBACK.get();
        clearAbsorptionOnEffectEnd = CLEAR_ABSORPTION_ON_EFFECT_END.get();
        combatSoundsEnabled = COMBAT_SOUNDS.get();
        hitSoundEnabled = HIT_SOUND.get();
        killSoundEnabled = KILL_SOUND.get();
        headshotKillSoundEnabled = HEADSHOT_KILL_SOUND.get();
        shieldSoundPriority = SHIELD_PRIORITY.get();
        restrictCombatSoundsToTacz = RESTRICT_TO_TACZ.get();
        hitSoundCooldownMs = HIT_COOLDOWN.get();
        killSoundCooldownMs = KILL_COOLDOWN.get();
    }
}

package cc.xypp.modernhud.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Motion HUD: the HUD sways with the camera. When you turn, the HUD lags behind a little and then settles back on a
 * spring. Sections: {@code general}, {@code physics}, {@code output}, {@code layers} (how strongly each element
 * follows, so near elements can move more than far ones) and {@code vanilla} (the vanilla hotbar / bars too).
 *
 * <p>All distances are in "reference pixels": pixels of a 1080p screen. They are scaled to your real resolution, so the
 * HUD sways the same amount on 1080p, 1440p and 4K, and at every GUI scale.
 */
@Mod.EventBusSubscriber(modid = "modernhud", bus = Mod.EventBusSubscriber.Bus.MOD)
public class MotionConfig {
    private static final Builder BUILDER = new Builder();
    public static final ForgeConfigSpec SPEC;

    // ---- general
    public static final BooleanValue ENABLED;
    public static final IntValue STRENGTH;
    public static final IntValue HORIZONTAL;
    public static final IntValue VERTICAL;
    public static final BooleanValue INVERT_HORIZONTAL;
    public static final BooleanValue INVERT_VERTICAL;

    // ---- physics
    public static final DoubleValue MAX_OFFSET;
    public static final DoubleValue RETURN_SPEED;
    public static final DoubleValue BOUNCE;
    public static final DoubleValue DEADZONE;

    // ---- output
    public static final BooleanValue PIXEL_SNAP;

    // ---- layers
    public static final IntValue LAYER_PLAYER;
    public static final IntValue LAYER_TEAM;
    public static final IntValue LAYER_WEAPON;
    public static final IntValue LAYER_AMMO_ALERT;
    public static final IntValue LAYER_KILLSTREAK;
    public static final IntValue LAYER_PERK_TOAST;
    public static final IntValue LAYER_COMPASS;
    public static final IntValue LAYER_BOSS_BAR;
    public static final IntValue LAYER_SPECS;
    public static final IntValue LAYER_FEEDBACK;
    public static final IntValue LAYER_HIT_INDICATOR;

    // ---- vanilla
    public static final BooleanValue VANILLA_ENABLED;
    public static final IntValue VANILLA_PERCENT;

    // ---- baked values (read by the renderer every frame)
    public static volatile boolean enabled = true;
    public static volatile float strength = 1f;
    public static volatile float horizontal = 1f;
    public static volatile float vertical = 0.7f;
    public static volatile boolean invertH;
    public static volatile boolean invertV;
    public static volatile float maxOffset = 28f;
    public static volatile float returnSpeed = 16f;
    public static volatile float bounce = 0.75f;
    public static volatile float deadzone = 0.02f;
    public static volatile boolean pixelSnap = true;
    public static volatile boolean vanillaEnabled;
    public static volatile float vanillaPercent = 1f;
    /** Index = {@code MotionLayer.ordinal()}; filled by {@link #refresh()}. */
    public static final float[] LAYER_WEIGHT = new float[16];

    private static volatile long revision;

    public static long revision() {
        return revision;
    }

    public static void toggleEnabled() {
        boolean next = !ENABLED.get();
        ENABLED.set(next);
        enabled = next;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            refresh();
        }
    }

    /** Re-reads the spec (also called right after the in-game screen saves). */
    public static void refresh() {
        enabled = ENABLED.get();
        strength = STRENGTH.get() / 100f;
        horizontal = HORIZONTAL.get() / 100f;
        vertical = VERTICAL.get() / 100f;
        invertH = INVERT_HORIZONTAL.get();
        invertV = INVERT_VERTICAL.get();
        maxOffset = MAX_OFFSET.get().floatValue();
        returnSpeed = RETURN_SPEED.get().floatValue();
        bounce = BOUNCE.get().floatValue();
        deadzone = DEADZONE.get().floatValue();
        pixelSnap = PIXEL_SNAP.get();
        vanillaEnabled = VANILLA_ENABLED.get();
        vanillaPercent = VANILLA_PERCENT.get() / 100f;
        // order must match cc.xypp.modernhud.client.motion.MotionLayer
        LAYER_WEIGHT[1] = LAYER_PLAYER.get() / 100f;
        LAYER_WEIGHT[2] = LAYER_TEAM.get() / 100f;
        LAYER_WEIGHT[3] = LAYER_WEAPON.get() / 100f;
        LAYER_WEIGHT[4] = LAYER_AMMO_ALERT.get() / 100f;
        LAYER_WEIGHT[5] = LAYER_KILLSTREAK.get() / 100f;
        LAYER_WEIGHT[6] = LAYER_PERK_TOAST.get() / 100f;
        LAYER_WEIGHT[7] = LAYER_COMPASS.get() / 100f;
        LAYER_WEIGHT[8] = LAYER_BOSS_BAR.get() / 100f;
        LAYER_WEIGHT[9] = LAYER_SPECS.get() / 100f;
        LAYER_WEIGHT[10] = LAYER_FEEDBACK.get() / 100f;
        LAYER_WEIGHT[11] = LAYER_HIT_INDICATOR.get() / 100f;
        revision++;
    }

    private static IntValue layer(String key, String translation, int def, String what) {
        return BUILDER.comment("How strongly " + what + " follows the camera, in percent. 0 keeps it still,",
                        "100 is the normal amount, above 100 moves it more (a closer layer).")
                .translation("modernhud.config." + translation)
                .defineInRange(key, def, 0, 300);
    }

    static {
        BUILDER.comment("Master switch and strength").push("general");
        ENABLED = BUILDER.comment("Let the HUD sway with the camera. Toggle in-game with /modernhud toggle motion.")
                .translation("modernhud.config.motion_enabled")
                .define("enabled", true);
        STRENGTH = BUILDER.comment("Overall strength in percent. 100 is the default feel, 0 is the same as off.")
                .translation("modernhud.config.motion_strength")
                .defineInRange("strength", 100, 0, 400);
        HORIZONTAL = BUILDER.comment("Sideways sway (turning left / right) in percent of the overall strength.")
                .translation("modernhud.config.motion_horizontal")
                .defineInRange("horizontal", 100, 0, 300);
        VERTICAL = BUILDER.comment("Up / down sway (looking up / down) in percent of the overall strength.")
                .translation("modernhud.config.motion_vertical")
                .defineInRange("vertical", 70, 0, 300);
        INVERT_HORIZONTAL = BUILDER.comment("Move the HUD WITH your turn instead of lagging behind it (sideways).")
                .translation("modernhud.config.motion_invert_h")
                .define("invertHorizontal", false);
        INVERT_VERTICAL = BUILDER.comment("Move the HUD WITH your look instead of lagging behind it (up / down).")
                .translation("modernhud.config.motion_invert_v")
                .define("invertVertical", false);
        BUILDER.pop();

        BUILDER.comment("The spring that pulls the HUD back to its place").push("physics");
        MAX_OFFSET = BUILDER.comment("The furthest the HUD may move from its place, in reference pixels (a 1080p screen).",
                        "Real distance scales with your resolution.")
                .translation("modernhud.config.motion_max_offset")
                .defineInRange("maxOffset", 28.0, 0.0, 200.0);
        RETURN_SPEED = BUILDER.comment("How fast the HUD settles back. Higher is snappier and moves less, lower is floaty.",
                        "Around 10 is soft, 16 is balanced, 30 is tight.")
                .translation("modernhud.config.motion_return_speed")
                .defineInRange("returnSpeed", 16.0, 4.0, 60.0);
        BOUNCE = BUILDER.comment("How much the HUD overshoots when it settles. 1.0 settles without overshoot,",
                        "lower values bounce a little (0.75 is a soft single overshoot).")
                .translation("modernhud.config.motion_bounce")
                .defineInRange("damping", 0.75, 0.2, 1.5);
        DEADZONE = BUILDER.comment("Camera movement smaller than this (degrees per frame) is ignored, so slow aiming",
                        "or a shaky mouse does not make the HUD crawl.")
                .translation("modernhud.config.motion_deadzone")
                .defineInRange("deadzone", 0.02, 0.0, 2.0);
        BUILDER.pop();

        BUILDER.comment("How the offset is drawn").push("output");
        PIXEL_SNAP = BUILDER.comment("Move the HUD in whole screen pixels only. This keeps text and thin lines sharp while",
                        "the HUD moves. Turn off for the smoothest (but slightly blurrier) motion.")
                .translation("modernhud.config.motion_pixel_snap")
                .define("pixelSnap", true);
        BUILDER.pop();

        BUILDER.comment("Per-element strength. Different values give a depth (parallax) effect.").push("layers");
        LAYER_PLAYER = layer("player", "motion_layer_player", 100, "the player widget (head, health, armor)");
        LAYER_TEAM = layer("team", "motion_layer_team", 100, "the team list");
        LAYER_WEAPON = layer("weapon", "motion_layer_weapon", 100, "the weapon widget (ammo, fire mode, gear)");
        LAYER_AMMO_ALERT = layer("ammoAlert", "motion_layer_ammo_alert", 100, "the reload / low ammo alert");
        LAYER_KILLSTREAK = layer("killstreak", "motion_layer_killstreak", 100, "the killstreak bar");
        LAYER_PERK_TOAST = layer("perkToast", "motion_layer_perk_toast", 100, "the perk popup");
        LAYER_COMPASS = layer("compass", "motion_layer_compass", 0, "the compass");
        LAYER_BOSS_BAR = layer("bossBar", "motion_layer_boss_bar", 0, "the boss bars");
        LAYER_SPECS = layer("specs", "motion_layer_specs", 0, "the FPS / ping / time readout");
        LAYER_FEEDBACK = layer("feedbackIcon", "motion_layer_feedback", 0, "the hit / break feedback icon");
        LAYER_HIT_INDICATOR = layer("hitIndicator", "motion_layer_hit_indicator", 0,
                "the hit direction arcs (they are tied to the crosshair, so they stay put by default)");
        BUILDER.pop();

        BUILDER.comment("Vanilla HUD pieces (hotbar, vanilla health / food / armor / air / XP bars, item name)").push("vanilla");
        VANILLA_ENABLED = BUILDER.comment("Also sway the vanilla hotbar and bars. The crosshair, chat and debug screen",
                        "never move.")
                .translation("modernhud.config.motion_vanilla")
                .define("enabled", false);
        VANILLA_PERCENT = BUILDER.comment("Strength for the vanilla pieces, in percent.")
                .translation("modernhud.config.motion_vanilla_percent")
                .defineInRange("percent", 100, 0, 300);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}

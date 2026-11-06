package cc.xypp.modernhud.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Modern Warfare-style damage direction: a soft glowing ARC around the crosshair that points back at whoever just
 * hit you, rotates smoothly with your view and fades out; plus an optional crosshair hit marker (crit / kill).
 *
 * <p>Reworked from the old "Hit Indication" integration. Gone as useless for PvP: edge-of-screen mode, block
 * (shield) indicators, non-directional (fall / fire / starvation) indicators, negative-potion hits, damage-based
 * sizing and distance scaling. Detection stays server-side ({@code HitIndicationEvents}); tracking is
 * {@code ClientHitTracker}; drawing is {@code HitIndicatorOverlay}.
 */
@Mod.EventBusSubscriber(modid = "modernhud", bus = Mod.EventBusSubscriber.Bus.MOD)
public class HitIndicationConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ENABLE;
    private static final ForgeConfigSpec.BooleanValue IGNORE_HIDE_GUI;
    private static final ForgeConfigSpec.IntValue RADIUS;
    private static final ForgeConfigSpec.IntValue ARC_SPAN;
    private static final ForgeConfigSpec.IntValue THICKNESS;
    private static final ForgeConfigSpec.IntValue OPACITY;
    private static final ForgeConfigSpec.IntValue FADE_IN_MS;
    private static final ForgeConfigSpec.IntValue HOLD_MS;
    private static final ForgeConfigSpec.IntValue FADE_OUT_MS;
    private static final ForgeConfigSpec.IntValue MAX_COUNT;
    private static final ForgeConfigSpec.BooleanValue MERGE;
    private static final ForgeConfigSpec.BooleanValue GLOW;
    private static final ForgeConfigSpec.ConfigValue<String> HIT_COLOR;
    private static final ForgeConfigSpec.BooleanValue ABSORPTION_BLUE;
    private static final ForgeConfigSpec.ConfigValue<String> ABSORPTION_COLOR;
    private static final ForgeConfigSpec.BooleanValue ENABLE_HIT_MARKERS;

    public static boolean enableHitIndication;
    public static boolean ignoreHideGui;
    public static int radius;
    public static int arcSpanDegrees;
    public static int thickness;
    public static int opacity;
    public static int fadeInMs;
    public static int holdMs;
    public static int fadeOutMs;
    public static int maxIndicatorCount;
    public static boolean mergeSameDirection;
    public static boolean glow;
    public static int hitIndicatorColor;
    public static boolean absorptionBlueIndicator;
    public static int absorptionIndicatorColor;
    public static boolean enableHitMarkers;

    static {
        BUILDER.comment("Damage direction: an arc around the crosshair that points at whoever hit you and",
                        "rotates with your view.").push("hitIndication");

        ENABLE = BUILDER
                .comment("Master switch for the directional damage arc.")
                .translation("modernhud.config.hit_enable")
                .define("enableHitIndication", true);

        IGNORE_HIDE_GUI = BUILDER
                .comment("Keep the arc and hit markers visible when F1 (hide GUI) is on.")
                .translation("modernhud.config.hit_ignore_hide_gui")
                .define("ignoreHideGui", true);

        RADIUS = BUILDER
                .comment("Distance (px) of the arc from the crosshair.")
                .translation("modernhud.config.hit_radius")
                .defineInRange("radius", 120, 20, 400);

        ARC_SPAN = BUILDER
                .comment("How wide the arc is, in degrees of the circle around the crosshair.")
                .translation("modernhud.config.hit_arc_span")
                .defineInRange("arcSpanDegrees", 38, 8, 120);

        THICKNESS = BUILDER
                .comment("Thickness of the arc's solid core in pixels. The glow adds a soft edge.")
                .translation("modernhud.config.hit_thickness")
                .defineInRange("thickness", 4, 1, 16);

        OPACITY = BUILDER
                .comment("Peak opacity of the arc, 0-100.")
                .translation("modernhud.config.hit_opacity")
                .defineInRange("opacity", 90, 0, 100);

        FADE_IN_MS = BUILDER
                .comment("Time (ms) for the arc to flash in.")
                .translation("modernhud.config.hit_fade_in_ms")
                .defineInRange("fadeInMs", 60, 0, 1000);

        HOLD_MS = BUILDER
                .comment("Time (ms) the arc stays at full strength.")
                .translation("modernhud.config.hit_hold_ms")
                .defineInRange("holdMs", 350, 0, 5000);

        FADE_OUT_MS = BUILDER
                .comment("Time (ms) for the arc to fade away.")
                .translation("modernhud.config.hit_fade_out_ms")
                .defineInRange("fadeOutMs", 450, 0, 5000);

        MAX_COUNT = BUILDER
                .comment("Most arcs shown at once. 0 means unlimited. The oldest is dropped first.")
                .translation("modernhud.config.hit_max_count")
                .defineInRange("maxIndicatorCount", 0, 0, 32);

        MERGE = BUILDER
                .comment("Hits from nearly the same direction refresh the existing arc instead of adding a new",
                        "one.")
                .translation("modernhud.config.hit_merge")
                .define("mergeSameDirection", true);

        GLOW = BUILDER
                .comment("Soft glow around the arc. Quality and strength follow the global glow settings.")
                .translation("modernhud.config.hit_glow")
                .define("glow", false);

        HIT_COLOR = BUILDER
                .comment("Arc color as a hex code.")
                .translation("modernhud.config.hit_color")
                .define("hitIndicatorColor", ColorConfig.toHex(0xFF0000));

        ABSORPTION_BLUE = BUILDER
                .comment("Draw the arc in absorptionIndicatorColor while you still have Absorption left. It",
                        "turns red once the Absorption is gone.")
                .translation("modernhud.config.hit_absorption_blue")
                .define("absorptionBlueIndicator", true);

        ABSORPTION_COLOR = BUILDER
                .comment("Arc color while Absorption is still up, as a hex code.")
                .translation("modernhud.config.hit_absorption_color")
                .define("absorptionIndicatorColor", ColorConfig.toHex(0x2F8CFF));

        ENABLE_HIT_MARKERS = BUILDER
                .comment("Show a brief crosshair marker on landing a critical hit or a kill.")
                .translation("modernhud.config.hit_enable_hit_markers")
                .define("enableHitMarkers", false);

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
        enableHitIndication = ENABLE.get();
        ignoreHideGui = IGNORE_HIDE_GUI.get();
        radius = RADIUS.get();
        arcSpanDegrees = ARC_SPAN.get();
        thickness = THICKNESS.get();
        opacity = OPACITY.get();
        fadeInMs = FADE_IN_MS.get();
        holdMs = HOLD_MS.get();
        fadeOutMs = FADE_OUT_MS.get();
        maxIndicatorCount = MAX_COUNT.get();
        mergeSameDirection = MERGE.get();
        glow = GLOW.get();
        hitIndicatorColor = ColorConfig.fromHex(HIT_COLOR.get(), 0xFF3B30);
        absorptionBlueIndicator = ABSORPTION_BLUE.get();
        absorptionIndicatorColor = ColorConfig.fromHex(ABSORPTION_COLOR.get(), 0x2F8CFF);
        enableHitMarkers = ENABLE_HIT_MARKERS.get();
    }
}

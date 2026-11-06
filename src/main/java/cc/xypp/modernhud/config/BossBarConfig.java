package cc.xypp.modernhud.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Client config for the compact boss bar: vanilla's wide centre-top boss bars are replaced by small, short
 * bars tucked into a screen corner (Modern Warfare match-progress style) - a big number (health percent) on the
 * left, a thin bar next to it, the boss name small underneath. Boss bars come from vanilla's own boss-event
 * system, so anything that uses it (wither, dragon, raids, mod bosses, commands) is covered.
 */
@Mod.EventBusSubscriber(modid = "modernhud", bus = Mod.EventBusSubscriber.Bus.MOD)
public class BossBarConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.EnumValue<HudPosition> POSITION;
    private static final ForgeConfigSpec.IntValue OFFSET_X;
    private static final ForgeConfigSpec.IntValue OFFSET_Y;
    private static final ForgeConfigSpec.DoubleValue SCALE;
    private static final ForgeConfigSpec.IntValue BAR_WIDTH;
    private static final ForgeConfigSpec.IntValue BAR_HEIGHT;
    private static final ForgeConfigSpec.IntValue BAR_SPACING;
    private static final ForgeConfigSpec.IntValue MAX_BARS;
    private static final ForgeConfigSpec.BooleanValue SHOW_NUMBER;
    private static final ForgeConfigSpec.EnumValue<BossNumberMode> NUMBER_MODE;
    private static final ForgeConfigSpec.BooleanValue PERCENT_SIGN;
    private static final ForgeConfigSpec.DoubleValue NUMBER_SCALE;
    private static final ForgeConfigSpec.EnumValue<BossNameMode> NAME_MODE;
    private static final ForgeConfigSpec.DoubleValue NAME_SCALE;
    private static final ForgeConfigSpec.BooleanValue USE_BOSS_COLOR;
    private static final ForgeConfigSpec.BooleanValue SHOW_TRACK;
    private static final ForgeConfigSpec.BooleanValue BAR_GLOW;
    private static final ForgeConfigSpec.BooleanValue HEAD_GLOW;
    private static final ForgeConfigSpec.EnumValue<BossBackdrop> BACKDROP;
    private static final ForgeConfigSpec.IntValue BACKDROP_OPACITY;
    private static final ForgeConfigSpec.EnumValue<BossNumberLayout> NUMBER_LAYOUT;
    private static final ForgeConfigSpec.IntValue GLOW_INTENSITY;
    private static final ForgeConfigSpec.BooleanValue ANIMATIONS;
    private static final ForgeConfigSpec.IntValue ANIMATION_MS;
    private static final ForgeConfigSpec.BooleanValue LOW_WARNING;
    private static final ForgeConfigSpec.BooleanValue DAMAGE_TRAIL;

    /** Overall opacity, fade-in / fade-out, BOX panel and border type of the boss bars. */
    public static final ElementStyle style;

    public static boolean enabled;
    public static HudPosition position;
    public static int offsetX;
    public static int offsetY;
    public static double scale;
    public static int barWidth;
    public static int barHeight;
    public static int barSpacing;
    public static int maxBars;
    public static boolean showNumber;
    public static BossNumberMode numberMode;
    public static boolean percentSign;
    public static double numberScale;
    public static BossNameMode nameMode;
    public static double nameScale;
    public static boolean useBossColor;
    public static boolean showTrack;
    public static boolean barGlow;
    public static boolean headGlow;
    public static BossBackdrop backdrop;
    public static int backdropOpacity;
    public static BossNumberLayout numberLayout;
    public static int glowIntensity;
    public static boolean animations;
    public static int animationMs;
    public static boolean lowWarning;
    public static boolean damageTrail;

    static {
        BUILDER.comment("Compact boss bars in a screen corner, replacing vanilla's wide bars at the top of the",
                        "screen.").push("bossBar");

        ENABLED = BUILDER
                .comment("Replace vanilla's boss bars with the compact corner version. Off = vanilla look.")
                .translation("modernhud.config.boss_enabled")
                .define("enabled", true);

        POSITION = BUILDER
                .comment("Screen corner the boss bars are tucked into.")
                .translation("modernhud.config.boss_position")
                .defineEnum("position", HudPosition.TOP_LEFT);

        OFFSET_X = BUILDER
                .comment("Horizontal offset in px from that corner's edge.")
                .translation("modernhud.config.boss_offset_x")
                .defineInRange("offsetX", 8, -2000, 4000);

        OFFSET_Y = BUILDER
                .comment("Vertical offset in px from that corner's edge.")
                .translation("modernhud.config.boss_offset_y")
                .defineInRange("offsetY", 90, -2000, 4000);

        SCALE = BUILDER
                .comment("Scale of the whole block. Keep it at a whole number if the pixel font looks uneven.")
                .translation("modernhud.config.boss_scale")
                .defineInRange("scale", 1.0, 0.5, 4.0);

        BAR_WIDTH = BUILDER
                .comment("Bar length in pixels, before scaling.")
                .translation("modernhud.config.boss_bar_width")
                .defineInRange("barWidth", 70, 30, 300);

        BAR_HEIGHT = BUILDER
                .comment("Thickness of the filled part in pixels, before scaling. The empty track stays a thin",
                        "line.")
                .translation("modernhud.config.boss_bar_height")
                .defineInRange("barHeight", 3, 1, 12);

        BAR_SPACING = BUILDER
                .comment("Gap between boss bars in pixels, before scaling.")
                .translation("modernhud.config.boss_bar_spacing")
                .defineInRange("barSpacing", 1, 0, 40);

        MAX_BARS = BUILDER
                .comment("Most boss bars shown at once; extra ones are simply not drawn.")
                .translation("modernhud.config.boss_max_bars")
                .defineInRange("maxBars", 2, 1, 10);

        SHOW_NUMBER = BUILDER
                .comment("Show the number next to the bar (see numberMode).")
                .translation("modernhud.config.boss_show_number")
                .define("showNumber", true);

        NUMBER_MODE = BUILDER
                .comment("PERCENT shows progress from 0 to 100. HEALTH shows exact health when the boss entity",
                        "is loaded and matches the bar name, otherwise the percent.")
                .translation("modernhud.config.boss_number_mode")
                .defineEnum("numberMode", BossNumberMode.HEALTH);

        PERCENT_SIGN = BUILDER
                .comment("Append a % to percent numbers.")
                .translation("modernhud.config.boss_percent_sign")
                .define("percentSign", false);

        NUMBER_SCALE = BUILDER
                .comment("Number text scale. Whole numbers (1, 2) keep the font sharp.")
                .translation("modernhud.config.boss_number_scale")
                .defineInRange("numberScale", 1.0, 0.5, 3.0);

        NAME_MODE = BUILDER
                .comment("Boss name: HIDDEN, ABOVE (small, right-aligned over the bar) or BELOW (left-aligned",
                        "under the bar).")
                .translation("modernhud.config.boss_name_mode")
                .defineEnum("nameMode", BossNameMode.ABOVE);

        NAME_SCALE = BUILDER
                .comment("Size of the name label. 1.0 = the font's native size (sharpest).")
                .translation("modernhud.config.boss_name_scale")
                .defineInRange("nameScale", 1.0, 0.5, 3.0);

        USE_BOSS_COLOR = BUILDER
                .comment("Tint the bar with the boss event's color. Off keeps it white.")
                .translation("modernhud.config.boss_use_color")
                .define("useBossColor", true);

        SHOW_TRACK = BUILDER
                .comment("Draw a thin line along the whole bar so the empty part stays visible.")
                .translation("modernhud.config.boss_show_track")
                .define("showTrack", true);

        BAR_GLOW = BUILDER
                .comment("Soft glow outline around the filled part of the bar.")
                .translation("modernhud.config.boss_bar_glow")
                .define("barGlow", true);

        HEAD_GLOW = BUILDER
                .comment("Make the leading edge of the fill glow.")
                .translation("modernhud.config.boss_head_glow")
                .define("headGlow", true);

        BACKDROP = BUILDER
                .comment("What sits behind each bar: NONE, SHADOW (soft dark fade) or BOX (a panel).")
                .translation("modernhud.config.boss_backdrop")
                .defineEnum("backdrop", BossBackdrop.SHADOW);

        BACKDROP_OPACITY = BUILDER
                .comment("Strength of the per-bar shadow / box, 0-100.")
                .translation("modernhud.config.boss_backdrop_opacity")
                .defineInRange("backdropOpacity", 55, 0, 100);

        NUMBER_LAYOUT = BUILDER
                .comment("Where numbers go: SIDE (left of each bar), ABOVE_BELOW (first bar above, last bar",
                        "below, made for two bars), ABOVE or BELOW. The boss name shares the row when a number",
                        "is above or below.")
                .translation("modernhud.config.boss_number_layout")
                .defineEnum("numberLayout", BossNumberLayout.ABOVE_BELOW);

        GLOW_INTENSITY = BUILDER
                .comment("Strength of the bar glow and head glow, 0 to 100. Needs barGlow or headGlow on.")
                .translation("modernhud.config.boss_glow_intensity")
                .defineInRange("glowIntensity", 60, 0, 100);

        ANIMATIONS = BUILDER
                .comment("Bars slide and fade in and out, the fill eases to its value and the stack moves",
                        "smoothly when a bar appears or goes.")
                .translation("modernhud.config.boss_animations")
                .define("animations", true);

        ANIMATION_MS = BUILDER
                .comment("Length of those animations in milliseconds.")
                .translation("modernhud.config.boss_animation_ms")
                .defineInRange("animationMs", 380, 100, 2000);

        LOW_WARNING = BUILDER
                .comment("Bar and number pulse red when the bar drops to 20% or less.")
                .translation("modernhud.config.boss_low_warning")
                .define("lowWarning", true);

        DAMAGE_TRAIL = BUILDER
                .comment("When the boss loses health, the lost part stays as a lighter trail and then drains",
                        "away.")
                .translation("modernhud.config.boss_damage_trail")
                .define("damageTrail", true);

        // fade-in / fade-out of each bar; the BOX backdrop (backdrop = BOX) takes its panel + border from here
        style = new ElementStyle(BUILDER, "style", "boss", new ElementStyle.Defaults(
                true, 0xD9DEE3, 16, PanelBorderStyle.NONE, 3, 1, 380, 380));

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
        enabled = ENABLED.get();
        position = POSITION.get();
        offsetX = OFFSET_X.get();
        offsetY = OFFSET_Y.get();
        scale = SCALE.get();
        barWidth = BAR_WIDTH.get();
        barHeight = BAR_HEIGHT.get();
        barSpacing = BAR_SPACING.get();
        maxBars = MAX_BARS.get();
        showNumber = SHOW_NUMBER.get();
        numberMode = NUMBER_MODE.get();
        percentSign = PERCENT_SIGN.get();
        numberScale = NUMBER_SCALE.get();
        nameMode = NAME_MODE.get();
        nameScale = NAME_SCALE.get();
        useBossColor = USE_BOSS_COLOR.get();
        showTrack = SHOW_TRACK.get();
        barGlow = BAR_GLOW.get();
        headGlow = HEAD_GLOW.get();
        backdrop = BACKDROP.get();
        backdropOpacity = BACKDROP_OPACITY.get();
        numberLayout = NUMBER_LAYOUT.get();
        glowIntensity = GLOW_INTENSITY.get();
        animations = ANIMATIONS.get();
        animationMs = ANIMATION_MS.get();
        lowWarning = LOW_WARNING.get();
        damageTrail = DAMAGE_TRAIL.get();
        style.refresh();
    }
}

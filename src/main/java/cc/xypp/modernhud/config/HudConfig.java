package cc.xypp.modernhud.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Client-only configuration for Modern HUD's personal widget and hit/break
 * feedback icon - what they look like and where they sit on screen. Whether
 * the feedback events fire at all (and who they're sent to) is decided
 * server-side, in CommonConfig.
 */
@Mod.EventBusSubscriber(modid = "modernhud", bus = Mod.EventBusSubscriber.Bus.MOD)
public class HudConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // --- General widget ---
    // --- Global animation / glow, shared by every element ---
    private static final ForgeConfigSpec.EnumValue<HudGuiScaleMode> GUI_SCALE_MODE;
    private static final ForgeConfigSpec.DoubleValue HUD_GUI_SCALE;
    private static final ForgeConfigSpec.IntValue GUI_SCALE_REFERENCE_HEIGHT;
    private static final ForgeConfigSpec.EnumValue<GlowQuality> GLOW_QUALITY;
    private static final ForgeConfigSpec.BooleanValue PIXEL_PERFECT_TEXT;
    private static final ForgeConfigSpec.BooleanValue PIXEL_PERFECT_ELEMENTS;
    private static final ForgeConfigSpec.EnumValue<NumberInputMode> NUMBER_INPUT;
    private static final ForgeConfigSpec.BooleanValue GUI_SCALE_SNAP;
    private static final ForgeConfigSpec.BooleanValue ANIMATIONS;
    private static final ForgeConfigSpec.DoubleValue ANIMATION_SPEED;
    private static final ForgeConfigSpec.IntValue GLOW_INTENSITY;
    private static final ForgeConfigSpec.IntValue HEALTH_ANIM_MS;
    private static final ForgeConfigSpec.IntValue ABSORPTION_ANIM_MS;
    private static final ForgeConfigSpec.IntValue FEEDBACK_OPACITY;
    private static final ForgeConfigSpec.IntValue FEEDBACK_FADE_MS;

    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.BooleanValue SHOW_HEALTH_BAR;
    private static final ForgeConfigSpec.BooleanValue SHOW_NAME;
    private static final ForgeConfigSpec.BooleanValue PERSONAL_USE_TEAM_COLOR;
    private static final ForgeConfigSpec.BooleanValue SHOW_HEAD;

    // --- Team HUD: vanilla-scoreboard-team teammate health list, stacked above the widget above ---
    private static final ForgeConfigSpec.BooleanValue SHOW_TEAM_HUD;
    private static final ForgeConfigSpec.IntValue TEAM_HUD_MAX_PLAYERS;
    private static final ForgeConfigSpec.IntValue TEAM_HUD_BAR_WIDTH;
    private static final ForgeConfigSpec.DoubleValue TEAM_HUD_SCALE_FACTOR;
    private static final ForgeConfigSpec.IntValue TEAM_HUD_ROW_GAP;
    private static final ForgeConfigSpec.IntValue TEAM_HUD_GROUP_GAP;
    private static final ForgeConfigSpec.BooleanValue TEAM_HUD_SHOW_HEAD;
    private static final ForgeConfigSpec.BooleanValue SHOW_TEAM_ABSORPTION;
    private static final ForgeConfigSpec.BooleanValue TEAM_HUD_USE_TEAM_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> TEAM_HUD_NAME_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> TEAM_HUD_DOWN_GLOW_COLOR;

    // --- HP number display ---
    private static final ForgeConfigSpec.BooleanValue SHOW_HP_TEXT;
    private static final ForgeConfigSpec.EnumValue<HpTextFormat> HP_TEXT_FORMAT;
    private static final ForgeConfigSpec.EnumValue<BarTextPosition> HP_TEXT_POSITION;
    private static final ForgeConfigSpec.DoubleValue LOW_HEALTH_THRESHOLD_PERCENT;
    private static final ForgeConfigSpec.BooleanValue SHOW_VITALS_GLOW;
    private static final ForgeConfigSpec.ConfigValue<String> VITALS_GLOW_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> HP_TEXT_NORMAL_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> HP_TEXT_LOW_COLOR;

    // --- Armor readout (vanilla armor points, 0-20) — TEXT ONLY, no bar ---
    private static final ForgeConfigSpec.BooleanValue SHOW_ARMOR;
    private static final ForgeConfigSpec.EnumValue<HpTextFormat> ARMOR_TEXT_FORMAT;
    private static final ForgeConfigSpec.EnumValue<BarTextPosition> ARMOR_TEXT_POSITION;
    private static final ForgeConfigSpec.ConfigValue<String> ARMOR_TEXT_COLOR;

    // --- Widget position ---
    private static final ForgeConfigSpec.EnumValue<HudPosition> POSITION;
    private static final ForgeConfigSpec.IntValue OFFSET_X;
    private static final ForgeConfigSpec.IntValue OFFSET_Y;

    // --- Bar appearance ---
    private static final ForgeConfigSpec.IntValue BAR_WIDTH;
    private static final ForgeConfigSpec.IntValue BAR_HEIGHT;
    private static final ForgeConfigSpec.DoubleValue BAR_SCALE;
    private static final ForgeConfigSpec.IntValue SEGMENT_COUNT;
    private static final ForgeConfigSpec.DoubleValue POINTS_PER_SEGMENT;
    private static final ForgeConfigSpec.BooleanValue SHOW_OVERFLOW_TEXT;
    private static final ForgeConfigSpec.BooleanValue COLLAPSE_ABSORPTION_ROW;
    private static final ForgeConfigSpec.EnumValue<NamePosition> NAME_POSITION;
    private static final ForgeConfigSpec.IntValue ABSORPTION_HEIGHT;
    private static final ForgeConfigSpec.EnumValue<AbsorptionColorMode> ABSORPTION_COLOR_MODE;
    private static final ForgeConfigSpec.EnumValue<PlateShape> ABSORPTION_SHAPE;
    private static final ForgeConfigSpec.EnumValue<PlateFill> ABSORPTION_FILL;
    private static final ForgeConfigSpec.BooleanValue ABSORPTION_OUTLINE;
    private static final ForgeConfigSpec.BooleanValue ABSORPTION_SMOOTH;
    private static final ForgeConfigSpec.ConfigValue<String> ABSORPTION_CUSTOM_COLOR;

    // --- Background panel (flat colour, adjustable opacity, behind the widget) ---
    private static final ForgeConfigSpec.BooleanValue SHOW_PANEL;
    private static final ForgeConfigSpec.ConfigValue<String> PANEL_COLOR;
    private static final ForgeConfigSpec.IntValue PANEL_OPACITY;
    private static final ForgeConfigSpec.EnumValue<PanelBorderStyle> BORDER_STYLE;
    private static final ForgeConfigSpec.ConfigValue<String> BORDER_COLOR;
    private static final ForgeConfigSpec.IntValue BORDER_OPACITY;
    private static final ForgeConfigSpec.IntValue BORDER_LENGTH;
    private static final ForgeConfigSpec.IntValue BORDER_OUTSET;
    private static final ForgeConfigSpec.IntValue BORDER_SCALE;

    // --- Hit / break feedback sounds (played locally on this client only) ---
    private static final ForgeConfigSpec.BooleanValue SHOW_FEEDBACK_WHEN_VICTIM;

    // --- Hit / break feedback icon: position (same anchor system as the main widget) ---
    private static final ForgeConfigSpec.EnumValue<HudPosition> FEEDBACK_POSITION;
    private static final ForgeConfigSpec.IntValue FEEDBACK_OFFSET_X;
    private static final ForgeConfigSpec.IntValue FEEDBACK_OFFSET_Y;
    private static final ForgeConfigSpec.IntValue FEEDBACK_ICON_WIDTH;
    private static final ForgeConfigSpec.IntValue FEEDBACK_ICON_HEIGHT;
    private static final ForgeConfigSpec.DoubleValue FEEDBACK_ICON_SCALE;

    // --- Hit / break feedback icon: which ones show + how long ---
    private static final ForgeConfigSpec.BooleanValue SHOW_HIT_ICON;
    private static final ForgeConfigSpec.IntValue HIT_ICON_DURATION_MS;
    private static final ForgeConfigSpec.BooleanValue SHOW_BREAK_ICON;
    private static final ForgeConfigSpec.IntValue BREAK_ICON_DURATION_MS;

    public static final ForgeConfigSpec SPEC;

    // Plain fields used by the renderers
    public static boolean enabled;
    public static boolean showHealthBar;
    public static boolean showName;
    public static boolean personalUseTeamColor;
    public static boolean showHead;

    public static boolean showTeamHud;
    public static int teamHudMaxPlayers;
    public static int teamHudBarWidth;
    public static double teamHudScaleFactor;
    public static int teamHudRowGap;
    public static int teamHudGroupGap;
    public static boolean teamHudShowHead;
    public static boolean showTeamAbsorption;
    public static boolean teamHudUseTeamColor;
    public static int teamHudNameColor;
    public static int teamHudDownGlowColor;

    public static boolean showHpText;
    public static HpTextFormat hpTextFormat;
    public static BarTextPosition hpTextPosition;
    public static double lowHealthThresholdPercent;
    public static boolean showVitalsGlow;
    public static int vitalsGlowColor;
    public static int hpTextNormalColor;
    public static int hpTextLowColor;

    public static boolean showArmor;
    public static HpTextFormat armorTextFormat;
    public static BarTextPosition armorTextPosition;
    public static int armorTextColor;
    public static HudPosition position;
    public static int offsetX;
    public static int offsetY;
    public static int barWidth;
    public static int barHeight;
    public static double barScale;
    public static int segmentCount;
    public static double pointsPerSegment;
    public static boolean showOverflowText;
    public static boolean collapseAbsorptionRow;
    public static NamePosition namePosition;
    public static int absorptionHeight;
    public static AbsorptionColorMode absorptionColorMode;
    public static PlateShape absorptionShape = PlateShape.WARZONE;
    public static PlateFill absorptionFill = PlateFill.HORIZONTAL;
    public static boolean absorptionOutline = true;
    public static boolean absorptionSmooth = true;
    public static int absorptionCustomColor;

    public static boolean showPanel;
    public static int panelColor;
    public static double panelOpacity;
    public static PanelBorderStyle borderStyle;
    public static int borderColor;
    public static double borderOpacity;
    public static int borderLength;
    public static int borderOutset;
    public static int borderScale = 100;

    public static boolean showFeedbackWhenVictim;

    public static HudPosition feedbackPosition;
    public static int feedbackOffsetX;
    public static int feedbackOffsetY;
    public static int feedbackIconWidth;
    public static int feedbackIconHeight;
    public static double feedbackIconScale;

    public static boolean showHitIcon;
    public static int hitIconDurationMs;
    public static boolean showBreakIcon;
    public static int breakIconDurationMs;

    // global animation / glow (defaults match the config defaults so anything drawn before the first load is sane)
    public static HudGuiScaleMode guiScaleMode = HudGuiScaleMode.MINECRAFT;
    public static double hudGuiScale = 2.0;
    public static int guiScaleReferenceHeight = 1080;
    public static GlowQuality glowQuality = GlowQuality.HIGH;
    public static boolean pixelPerfectText = true;
    public static boolean pixelPerfectElements = false;
    public static NumberInputMode numberInput = NumberInputMode.FIELD;
    public static boolean guiScaleSnap = true;
    public static boolean animations = true;
    public static double animationSpeed = 1.0;
    public static int glowIntensity = 100;
    public static int healthAnimMs = 280;
    public static int absorptionAnimMs = 220;
    public static int feedbackOpacity = 100;
    public static int feedbackFadeMs = 250;

    static {
        BUILDER.comment("Your own status widget: head, name, health and absorption bars, HP text.").push("general");

        ENABLED = BUILDER
                .comment("Show the player widget.")
                .translation("modernhud.config.enabled")
                .define("enabled", true);

        SHOW_HEALTH_BAR = BUILDER
                .comment("Show your own health bar under the absorption bar.")
                .translation("modernhud.config.show_health_bar")
                .define("showHealthBar", true);

        SHOW_NAME = BUILDER
                .comment("Show your own username next to the head icon.")
                .translation("modernhud.config.show_name")
                .define("showName", true);

        PERSONAL_USE_TEAM_COLOR = BUILDER
                .comment("Color your name with your scoreboard team color. White if you have no team.")
                .translation("modernhud.config.personal_use_team_color")
                .define("useTeamColor", false);

        SHOW_HEAD = BUILDER
                .comment("Show a small 2D render of your own head/skin.")
                .translation("modernhud.config.show_head")
                .define("showHead", true);

        BUILDER.pop();

        BUILDER.comment("Numeric health readout.").push("hpText");

        SHOW_HP_TEXT = BUILDER
                .comment("Show your current HP as text somewhere on the widget.")
                .translation("modernhud.config.show_hp_text")
                .define("enabled", false);

        HP_TEXT_FORMAT = BUILDER
                .comment("CLASSIC = '20/20' (current/max HP points). PERCENT = '100%'.")
                .translation("modernhud.config.hp_text_format")
                .defineEnum("format", HpTextFormat.PERCENT);

        HP_TEXT_POSITION = BUILDER
                .comment("Where the HP text goes: BELOW_BAR, BELOW_NAME_HEAD or END_OF_BAR.")
                .translation("modernhud.config.hp_text_position")
                .defineEnum("position", BarTextPosition.BELOW_NAME_HEAD);

        LOW_HEALTH_THRESHOLD_PERCENT = BUILDER
                .comment("Health percentage below which the HP text uses the low-health color.")
                .translation("modernhud.config.low_health_threshold_percent")
                .defineInRange("lowHealthThresholdPercent", 30.0, 0.0, 100.0);

        SHOW_VITALS_GLOW = BUILDER
                .comment("Glow behind the health bar: a flash when you are hit and a slow pulse while health is",
                        "low.")
                .translation("modernhud.config.show_vitals_glow")
                .define("glowEnabled", true);

        VITALS_GLOW_COLOR = BUILDER
                .comment("Vitals glow color, as a hex code (e.g. \"FF3B30\" or \"#FF3B30\").")
                .translation("modernhud.config.vitals_glow_color")
                .define("vitalsGlowColor", ColorConfig.toHex(0xFF3B30));

        HP_TEXT_NORMAL_COLOR = BUILDER
                .comment("HP text color above the low-health threshold, as a hex code (e.g. \"FFFFFF\").")
                .translation("modernhud.config.hp_text_normal_color")
                .define("normalColor", ColorConfig.toHex(0xFFFFFF));

        HP_TEXT_LOW_COLOR = BUILDER
                .comment("HP text color at/below the low-health threshold, as a hex code.")
                .translation("modernhud.config.hp_text_low_color")
                .define("lowColor", ColorConfig.toHex(0xFF5555));

        BUILDER.pop();

        BUILDER.comment("Armor readout, shown as text (for example 14/20). Visible even at 0.")
                .push("armor");

        SHOW_ARMOR = BUILDER
                .comment("Show your current armor points as text.")
                .translation("modernhud.config.show_armor")
                .define("enabled", false);

        ARMOR_TEXT_FORMAT = BUILDER
                .comment("CLASSIC = '12/20' (current/max armor points). PERCENT = '60%'.")
                .translation("modernhud.config.armor_text_format")
                .defineEnum("textFormat", HpTextFormat.CLASSIC);

        ARMOR_TEXT_POSITION = BUILDER
                .comment("BELOW_NAME_HEAD puts armor on the info line after the HP text. END_OF_BAR and",
                        "BELOW_BAR put it on its own row under the health bar.")
                .translation("modernhud.config.armor_text_position")
                .defineEnum("textPosition", BarTextPosition.END_OF_BAR);

        ARMOR_TEXT_COLOR = BUILDER
                .comment("Armor text color, as a hex code (e.g. \"C0C0C0\" or \"#C0C0C0\").")
                .translation("modernhud.config.armor_text_color")
                .define("textColor", ColorConfig.toHex(0xC0C0C0));

        BUILDER.pop();

        BUILDER.comment("Where the widget is anchored on screen").push("position");

        POSITION = BUILDER
                .comment("Screen corner the widget is attached to. CUSTOM uses the offsets as absolute",
                        "coordinates.")
                .translation("modernhud.config.position")
                .defineEnum("anchor", HudPosition.BOTTOM_LEFT);

        OFFSET_X = BUILDER
                .comment("Horizontal distance from the anchor in pixels (absolute X when anchor is CUSTOM).")
                .translation("modernhud.config.offset_x")
                .defineInRange("offsetX", 12, -10000, 10000);

        OFFSET_Y = BUILDER
                .comment("Vertical distance from the anchor in pixels (absolute Y when anchor is CUSTOM).")
                .translation("modernhud.config.offset_y")
                .defineInRange("offsetY", 12, -10000, 10000);

        BUILDER.pop();

        BUILDER.comment("Bar size and how Absorption points map onto it").push("appearance");

        BAR_WIDTH = BUILDER
                .comment("Bar width in pixels, before scaling.")
                .translation("modernhud.config.bar_width")
                .defineInRange("barWidth", 70, 10, 1000);

        BAR_HEIGHT = BUILDER
                .comment("Bar height in pixels, before scaling.")
                .translation("modernhud.config.bar_height")
                .defineInRange("barHeight", 5, 2, 200);

        BAR_SCALE = BUILDER
                .comment("Overall scale multiplier applied to the whole widget.")
                .translation("modernhud.config.bar_scale")
                .defineInRange("barScale", 0.9, 0.1, 5.0);

        SEGMENT_COUNT = BUILDER
                .comment("Number of segments in the absorption bar. 4 matches the four color tiers and the four",
                        "vanilla Absorption levels.")
                .translation("modernhud.config.segment_count")
                .defineInRange("segmentCount", 3, 1, 20);

        POINTS_PER_SEGMENT = BUILDER
                .comment("Absorption points per segment. Vanilla gives 4 per level, so 4.0 means one segment per",
                        "level.")
                .translation("modernhud.config.points_per_segment")
                .defineInRange("pointsPerSegment", 4.0, 0.5, 100.0);

        SHOW_OVERFLOW_TEXT = BUILDER
                .comment("Show a +N label when absorption is higher than the bar can display.")
                .translation("modernhud.config.show_overflow_text")
                .define("showOverflowText", false);

        COLLAPSE_ABSORPTION_ROW = BUILDER
                .comment("Take the absorption bar's row out of the layout while you have no absorption, so the widget is",
                        "shorter and the name moves down into its place. The row slides back in when absorption returns.")
                .translation("modernhud.config.collapse_absorption_row")
                .define("collapseAbsorptionRow", true);

        NAME_POSITION = BUILDER
                .comment("Where your name shows relative to the bars: ABOVE (MW default) or BELOW.")
                .translation("modernhud.config.name_position")
                .defineEnum("namePosition", NamePosition.ABOVE);

        ABSORPTION_HEIGHT = BUILDER
                .comment("Height of the thin absorption strip above the health bar, in pixels.")
                .translation("modernhud.config.absorption_height")
                .defineInRange("absorptionHeight", 3, 1, 100);

        ABSORPTION_SHAPE = BUILDER
                .comment("Shape of each absorption plate: WARZONE (the Warzone armor plate: round top corners and a cut-out along",
                        "the bottom; its height follows the bar width, about 9:1, 4 units at the least), PLATE (the armor plate of the reference: small left corners, round right",
                        "end), PILL (capsule), CHAMFER (cut corners), HEX (hexagon), SLANT (leaning parallelogram, Modern Warfare",
                        "2019) or BAR (the old straight rectangle). Shapes need at least 4 units of height (6 without",
                        "absorptionSmooth); a smaller absorptionHeight is raised to that.")
                .translation("modernhud.config.absorption_shape")
                .defineEnum("absorptionShape", PlateShape.WARZONE);

        ABSORPTION_FILL = BUILDER
                .comment("How a plate fills: HORIZONTAL (left to right) or VERTICAL (from the bottom up, like a gauge).")
                .translation("modernhud.config.absorption_fill")
                .defineEnum("absorptionFill", PlateFill.HORIZONTAL);

        ABSORPTION_SMOOTH = BUILDER
                .comment("Draw the plate shapes smooth: each shape is computed with signed distance fields at the screen's own",
                        "resolution (anti-aliased round ends and slanted edges, soft shading) and drawn in one call. Off uses",
                        "rows of plain pixels (jagged edges, needs 6 units of height). It also falls back by itself if the",
                        "smooth drawing ever fails.")
                .translation("modernhud.config.absorption_smooth")
                .define("absorptionSmooth", true);

        ABSORPTION_OUTLINE = BUILDER
                .comment("A thin light outline around each plate shape (not used by BAR).")
                .translation("modernhud.config.absorption_outline")
                .define("absorptionOutline", true);

        ABSORPTION_COLOR_MODE = BUILDER
                .comment("TIERED colors the plates gray, blue, purple, red as more fill up. CUSTOM always uses",
                        "absorptionCustomColor.")
                .translation("modernhud.config.absorption_color_mode")
                .defineEnum("absorptionColorMode", AbsorptionColorMode.CUSTOM);

        ABSORPTION_CUSTOM_COLOR = BUILDER
                .comment("Plate color when absorptionColorMode is CUSTOM, as a hex code.")
                .translation("modernhud.config.absorption_custom_color")
                .define("absorptionCustomColor", ColorConfig.toHex(0x78BBD5));

        BUILDER.pop();

        BUILDER.comment("Background panel behind the widget: a flat fill with an optional corner or line",
                        "border.")
                .push("panel");

        SHOW_PANEL = BUILDER
                .comment("Draw the background panel behind the whole widget.")
                .translation("modernhud.config.show_panel")
                .define("enabled", false);

        PANEL_COLOR = BUILDER
                .comment("Panel background color, as a hex code (e.g. \"0A0D11\" or \"#0A0D11\").")
                .translation("modernhud.config.panel_color")
                .define("panelColor", ColorConfig.toHex(0xD9DEE3));

        PANEL_OPACITY = BUILDER
                .comment("Panel opacity, 0 to 100.")
                .translation("modernhud.config.panel_opacity")
                .defineInRange("panelOpacity", 14, 0, 100);

        BORDER_STYLE = BUILDER
                .comment("Border drawn over the panel. NONE, CORNERS (short L-shaped brackets), FULL (thin",
                        "outline), DOTS, BOTTOM_DOTS or FRAME (outline with corner dots).")
                .translation("modernhud.config.border_style")
                .defineEnum("borderStyle", PanelBorderStyle.CORNERS);

        BORDER_COLOR = BUILDER
                .comment("Border/corner-bracket color, as a hex code (e.g. \"E8ECF0\" or \"#E8ECF0\").")
                .translation("modernhud.config.border_color")
                .define("borderColor", ColorConfig.toHex(0xE8ECF0));

        BORDER_OPACITY = BUILDER
                .comment("Border opacity, 0 to 100.")
                .translation("modernhud.config.border_opacity")
                .defineInRange("borderOpacity", 90, 0, 100);

        BORDER_LENGTH = BUILDER
                .comment("Length of each bracket arm in pixels. Capped to a fraction of the panel size.")
                .translation("modernhud.config.border_length")
                .defineInRange("borderLength", 1, 1, 20);

        BORDER_SCALE = BUILDER
                .comment("Scale of the corner dots and corner brackets in percent. 100 is the size set by borderLength,",
                        "50 is half, 200 is double.")
                .translation("modernhud.config.border_scale")
                .defineInRange("borderScale", 100, 25, 800);

        BORDER_OUTSET = BUILDER
                .comment("How far brackets and dots sit outside the panel edge, in pixels. 0 puts them on the",
                        "edge.")
                .translation("modernhud.config.border_outset")
                .defineInRange("borderOutset", 1, 0, 20);

        BUILDER.pop();

        BUILDER.comment("Shield feedback. The sounds themselves (style, volume, pitch) are in the Sounds tab.",
                        "They only play for you.")
                .push("sounds");

        SHOW_FEEDBACK_WHEN_VICTIM = BUILDER
                .comment("Also play the hit and break cue and icon when you are the one being hit.")
                .translation("modernhud.config.show_feedback_when_victim")
                .define("showFeedbackWhenVictim", false);

        BUILDER.pop();

        BUILDER.comment("Position and size of the shield hit / break icon.")
                .push("feedbackIcon");

        FEEDBACK_POSITION = BUILDER
                .comment("Screen corner the icon is attached to. CUSTOM uses the offsets as absolute",
                        "coordinates.")
                .translation("modernhud.config.feedback_position")
                .defineEnum("anchor", HudPosition.CUSTOM);

        FEEDBACK_OFFSET_X = BUILDER
                .comment("Horizontal distance from the anchor in pixels (absolute X when anchor is CUSTOM).")
                .translation("modernhud.config.feedback_offset_x")
                .defineInRange("offsetX", 377, -10000, 10000);

        FEEDBACK_OFFSET_Y = BUILDER
                .comment("Vertical distance from the anchor in pixels (absolute Y when anchor is CUSTOM).")
                .translation("modernhud.config.feedback_offset_y")
                .defineInRange("offsetY", 132, -10000, 10000);

        FEEDBACK_ICON_WIDTH = BUILDER
                .comment("Icon width in pixels, before scaling.")
                .translation("modernhud.config.feedback_icon_width")
                .defineInRange("width", 28, 4, 500);

        FEEDBACK_ICON_HEIGHT = BUILDER
                .comment("Icon height in pixels, before scaling.")
                .translation("modernhud.config.feedback_icon_height")
                .defineInRange("height", 28, 4, 500);

        FEEDBACK_ICON_SCALE = BUILDER
                .comment("Extra scale multiplier for the icon.")
                .translation("modernhud.config.feedback_icon_scale")
                .defineInRange("scale", 1.0, 0.1, 10.0);

        BUILDER.pop();

        BUILDER.comment("Which icons appear and how long they stay, in milliseconds.")
                .push("feedbackIconTiming");

        SHOW_HIT_ICON = BUILDER
                .comment("Show an icon when a hit lands on a shield without breaking it.")
                .translation("modernhud.config.show_hit_icon")
                .define("showHitIcon", true);

        HIT_ICON_DURATION_MS = BUILDER
                .comment("How long the hit icon stays on screen, in milliseconds.")
                .translation("modernhud.config.hit_icon_duration_ms")
                .defineInRange("hitIconDurationMs", 300, 50, 10000);

        SHOW_BREAK_ICON = BUILDER
                .comment("Show an icon when you (the attacker) fully break someone's Absorption.")
                .translation("modernhud.config.show_break_icon")
                .define("showBreakIcon", true);

        BREAK_ICON_DURATION_MS = BUILDER
                .comment("How long the break icon stays on screen, in milliseconds.")
                .translation("modernhud.config.break_icon_duration_ms")
                .defineInRange("breakIconDurationMs", 700, 50, 10000);

        BUILDER.pop();

        BUILDER.comment("Teammate list for your scoreboard team: one compact row per teammate above your own",
                        "widget.")
                .push("teamHud");

        SHOW_TEAM_HUD = BUILDER
                .comment("Show the teammate list. Nothing is drawn if you have no team or no other tracked",
                        "members.")
                .translation("modernhud.config.show_team_hud")
                .define("enabled", true);

        TEAM_HUD_MAX_PLAYERS = BUILDER
                .comment("Most teammates shown at once. Extra ones are hidden, sorted by name.")
                .translation("modernhud.config.team_hud_max_players")
                .defineInRange("maxPlayers", 6, 1, 16);

        TEAM_HUD_SCALE_FACTOR = BUILDER
                .comment("Size of teammate rows relative to your own widget. 0.9 makes them 10% smaller.")
                .translation("modernhud.config.team_hud_scale_factor")
                .defineInRange("scaleFactor", 0.95, 0.1, 1.5);

        TEAM_HUD_BAR_WIDTH = BUILDER
                .comment("Width of each teammate health bar in pixels, before scaling. Long names can run past",
                        "it.")
                .translation("modernhud.config.team_hud_bar_width")
                .defineInRange("barWidth", 70, 16, 400);

        TEAM_HUD_ROW_GAP = BUILDER
                .comment("Vertical gap in px between teammate rows.")
                .translation("modernhud.config.team_hud_row_gap")
                .defineInRange("rowGap", 2, 0, 40);

        TEAM_HUD_GROUP_GAP = BUILDER
                .comment("Vertical gap in pixels between the team list and your widget.")
                .translation("modernhud.config.team_hud_group_gap")
                .defineInRange("groupGap", 4, 0, 100);

        TEAM_HUD_SHOW_HEAD = BUILDER
                .comment("Show each teammate's head next to their row.")
                .translation("modernhud.config.team_hud_show_head")
                .define("showHead", true);

        SHOW_TEAM_ABSORPTION = BUILDER
                .comment("Show each teammate's absorption bar above their health bar, using the same style as",
                        "your own.")
                .translation("modernhud.config.team_hud_show_absorption")
                .define("showAbsorption", true);

        TEAM_HUD_USE_TEAM_COLOR = BUILDER
                .comment("Color teammate names with their scoreboard team color. Falls back to nameColor if the",
                        "team has none.")
                .translation("modernhud.config.team_hud_use_team_color")
                .define("useTeamColor", false);

        TEAM_HUD_NAME_COLOR = BUILDER
                .comment("Teammate name color when useTeamColor is off or the team has no color, as a hex code.")
                .translation("modernhud.config.team_hud_name_color")
                .define("nameColor", ColorConfig.toHex(0xE6EEF5));

        TEAM_HUD_DOWN_GLOW_COLOR = BUILDER
                .comment("Color of the pulsing glow over a row while that teammate is dead or spectating.")
                .translation("modernhud.config.team_hud_down_glow_color")
                .define("downGlowColor", ColorConfig.toHex(0xE85138));

        BUILDER.pop();

        BUILDER.comment("Scaling, animation and glow settings shared by every HUD element. Fade times for each",
                        "element are in that element's own section.")
                .push("animation");

        GUI_SCALE_MODE = BUILDER
                .comment("Where the HUD's GUI scale comes from. MINECRAFT follows the game setting. FIXED uses",
                        "hudGuiScale. RESOLUTION scales hudGuiScale with window height.")
                .translation("modernhud.config.gui_scale_mode")
                .defineEnum("guiScaleMode", HudGuiScaleMode.MINECRAFT);

        HUD_GUI_SCALE = BUILDER
                .comment("GUI scale for FIXED mode, or the scale at the reference height for RESOLUTION mode.",
                        "2.0 equals Minecraft's GUI Scale 2.")
                .translation("modernhud.config.hud_gui_scale")
                .defineInRange("hudGuiScale", 2.0, 0.5, 8.0);

        GUI_SCALE_REFERENCE_HEIGHT = BUILDER
                .comment("RESOLUTION mode only: the window height in pixels where hudGuiScale applies unchanged.")
                .translation("modernhud.config.gui_scale_reference_height")
                .defineInRange("guiScaleReferenceHeight", 1080, 360, 4320);

        PIXEL_PERFECT_TEXT = BUILDER
                .comment("Snap text so each font pixel covers a whole number of screen pixels. Removes uneven",
                        "text at fractional scales.")
                .translation("modernhud.config.pixel_perfect_text")
                .define("pixelPerfectText", true);

        NUMBER_INPUT = BUILDER
                .comment("How number options are edited in the config screen. FIELD: click the number and type it (exact",
                        "values, any scale, points per segment, ...). SLIDER: drag a slider where the option has a bounded",
                        "range. Reopen the screen after changing this.")
                .translation("modernhud.config.number_input")
                .defineEnum("numberInput", NumberInputMode.FIELD);

        PIXEL_PERFECT_ELEMENTS = BUILDER
                .comment("Also snap whole widgets to whole screen pixels. Sharper, but scale options change in",
                        "coarse steps.")
                .translation("modernhud.config.pixel_perfect_elements")
                .define("pixelPerfectElements", false);

        GUI_SCALE_SNAP = BUILDER
                .comment("RESOLUTION mode only: round the resulting GUI scale to a whole number.")
                .translation("modernhud.config.gui_scale_snap")
                .define("guiScaleSnap", true);

        GLOW_QUALITY = BUILDER
                .comment("Smoothness of glows: LOW, MEDIUM or HIGH.")
                .translation("modernhud.config.glow_quality")
                .defineEnum("glowQuality", GlowQuality.LOW);

        ANIMATIONS = BUILDER
                .comment("Turn all HUD animations (gliding bars, sliding slots, fades) on or off. Off makes",
                        "values change instantly.")
                .translation("modernhud.config.animations")
                .define("animations", true);

        ANIMATION_SPEED = BUILDER
                .comment("Speed of all animations. 2.0 is twice as fast, 0.5 is half speed.")
                .translation("modernhud.config.animation_speed")
                .defineInRange("animationSpeed", 1.0, 0.25, 4.0);

        GLOW_INTENSITY = BUILDER
                .comment("Strength of every glow in percent. 0 turns all glows off, 100 is the default.")
                .translation("modernhud.config.glow_intensity")
                .defineInRange("glowIntensity", 100, 0, 200);

        HEALTH_ANIM_MS = BUILDER
                .comment("Time in milliseconds for health bars to move to a new value.")
                .translation("modernhud.config.health_anim_ms")
                .defineInRange("healthAnimMs", 280, 0, 3000);

        ABSORPTION_ANIM_MS = BUILDER
                .comment("Time in milliseconds for absorption bars to move to a new value.")
                .translation("modernhud.config.absorption_anim_ms")
                .defineInRange("absorptionAnimMs", 220, 0, 3000);

        FEEDBACK_OPACITY = BUILDER
                .comment("Opacity of the hit / Absorption-break feedback icon, 0-100.")
                .translation("modernhud.config.feedback_opacity")
                .defineInRange("feedbackOpacity", 100, 0, 100);

        FEEDBACK_FADE_MS = BUILDER
                .comment("Fade-out time in milliseconds at the end of the icon's display time.")
                .translation("modernhud.config.feedback_fade_ms")
                .defineInRange("feedbackFadeMs", 250, 0, 10000);

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
        showHealthBar = SHOW_HEALTH_BAR.get();
        showName = SHOW_NAME.get();
        personalUseTeamColor = PERSONAL_USE_TEAM_COLOR.get();
        showHead = SHOW_HEAD.get();

        showHpText = SHOW_HP_TEXT.get();
        hpTextFormat = HP_TEXT_FORMAT.get();
        hpTextPosition = HP_TEXT_POSITION.get();
        lowHealthThresholdPercent = LOW_HEALTH_THRESHOLD_PERCENT.get();
        showVitalsGlow = SHOW_VITALS_GLOW.get();
        vitalsGlowColor = ColorConfig.fromHex(VITALS_GLOW_COLOR.get(), 0xFF3B30);
        hpTextNormalColor = ColorConfig.fromHex(HP_TEXT_NORMAL_COLOR.get(), 0xFFFFFF);
        hpTextLowColor = ColorConfig.fromHex(HP_TEXT_LOW_COLOR.get(), 0xFF5555);

        showArmor = SHOW_ARMOR.get();
        armorTextFormat = ARMOR_TEXT_FORMAT.get();
        armorTextPosition = ARMOR_TEXT_POSITION.get();
        armorTextColor = ColorConfig.fromHex(ARMOR_TEXT_COLOR.get(), 0xC0C0C0);
        position = POSITION.get();
        offsetX = OFFSET_X.get();
        offsetY = OFFSET_Y.get();
        barWidth = BAR_WIDTH.get();
        barHeight = BAR_HEIGHT.get();
        barScale = BAR_SCALE.get();
        segmentCount = SEGMENT_COUNT.get();
        pointsPerSegment = POINTS_PER_SEGMENT.get();
        showOverflowText = SHOW_OVERFLOW_TEXT.get();
        collapseAbsorptionRow = COLLAPSE_ABSORPTION_ROW.get();
        namePosition = NAME_POSITION.get();
        absorptionHeight = ABSORPTION_HEIGHT.get();
        absorptionColorMode = ABSORPTION_COLOR_MODE.get();
        absorptionShape = ABSORPTION_SHAPE.get();
        absorptionFill = ABSORPTION_FILL.get();
        absorptionOutline = ABSORPTION_OUTLINE.get();
        absorptionSmooth = ABSORPTION_SMOOTH.get();
        absorptionCustomColor = ColorConfig.fromHex(ABSORPTION_CUSTOM_COLOR.get(), 0x1C7ED6);

        showPanel = SHOW_PANEL.get();
        panelColor = ColorConfig.fromHex(PANEL_COLOR.get(), 0xD9DEE3);
        panelOpacity = PANEL_OPACITY.get();
        borderStyle = BORDER_STYLE.get();
        borderColor = ColorConfig.fromHex(BORDER_COLOR.get(), 0xE8ECF0);
        borderOpacity = BORDER_OPACITY.get();
        borderLength = BORDER_LENGTH.get();
        borderOutset = BORDER_OUTSET.get();
        borderScale = BORDER_SCALE.get();

        showFeedbackWhenVictim = SHOW_FEEDBACK_WHEN_VICTIM.get();

        feedbackPosition = FEEDBACK_POSITION.get();
        feedbackOffsetX = FEEDBACK_OFFSET_X.get();
        feedbackOffsetY = FEEDBACK_OFFSET_Y.get();
        feedbackIconWidth = FEEDBACK_ICON_WIDTH.get();
        feedbackIconHeight = FEEDBACK_ICON_HEIGHT.get();
        feedbackIconScale = FEEDBACK_ICON_SCALE.get();

        showHitIcon = SHOW_HIT_ICON.get();
        hitIconDurationMs = HIT_ICON_DURATION_MS.get();
        showBreakIcon = SHOW_BREAK_ICON.get();
        breakIconDurationMs = BREAK_ICON_DURATION_MS.get();

        showTeamHud = SHOW_TEAM_HUD.get();
        teamHudMaxPlayers = TEAM_HUD_MAX_PLAYERS.get();
        teamHudBarWidth = TEAM_HUD_BAR_WIDTH.get();
        teamHudScaleFactor = TEAM_HUD_SCALE_FACTOR.get();
        teamHudRowGap = TEAM_HUD_ROW_GAP.get();
        teamHudGroupGap = TEAM_HUD_GROUP_GAP.get();
        teamHudShowHead = TEAM_HUD_SHOW_HEAD.get();
        showTeamAbsorption = SHOW_TEAM_ABSORPTION.get();
        teamHudUseTeamColor = TEAM_HUD_USE_TEAM_COLOR.get();
        teamHudNameColor = ColorConfig.fromHex(TEAM_HUD_NAME_COLOR.get(), 0xE6EEF5);
        teamHudDownGlowColor = ColorConfig.fromHex(TEAM_HUD_DOWN_GLOW_COLOR.get(), 0xE85138);

        ColorConfig.migrateDarkPanel(PANEL_COLOR, PANEL_OPACITY, 0xD9DEE3, 14);
        guiScaleMode = GUI_SCALE_MODE.get();
        hudGuiScale = HUD_GUI_SCALE.get();
        guiScaleReferenceHeight = GUI_SCALE_REFERENCE_HEIGHT.get();
        glowQuality = GLOW_QUALITY.get();
        pixelPerfectText = PIXEL_PERFECT_TEXT.get();
        pixelPerfectElements = PIXEL_PERFECT_ELEMENTS.get();
        numberInput = NUMBER_INPUT.get();
        guiScaleSnap = GUI_SCALE_SNAP.get();
        animations = ANIMATIONS.get();
        animationSpeed = ANIMATION_SPEED.get();
        glowIntensity = GLOW_INTENSITY.get();
        healthAnimMs = HEALTH_ANIM_MS.get();
        absorptionAnimMs = ABSORPTION_ANIM_MS.get();
        feedbackOpacity = FEEDBACK_OPACITY.get();
        feedbackFadeMs = FEEDBACK_FADE_MS.get();
    }

    // --- Runtime setter for the drag-to-place HUD editor (Hit/Break icon only - the player HUD's
    // position is corner-anchored and config-file-only now, same treatment as the weapon widget;
    // see HudEditorScreen's class javadoc for why). Updates the live field AND the backing config
    // value (persisted on save), and forces CUSTOM anchoring so the absolute offset is honoured. ---
    public static void setFeedbackPosition(int x, int y) {
        feedbackPosition = HudPosition.CUSTOM;
        feedbackOffsetX = x;
        feedbackOffsetY = y;
        FEEDBACK_POSITION.set(HudPosition.CUSTOM);
        FEEDBACK_OFFSET_X.set(x);
        FEEDBACK_OFFSET_Y.set(y);
    }

    public static void saveConfig() {
        SPEC.save();
    }
}

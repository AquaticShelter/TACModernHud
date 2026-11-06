package cc.xypp.modernhud.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Client-only configuration for the TACZ weapon widget (bottom-right by default): the held gun's
 * icon, name, fire mode, and mag/reserve ammo — drawn in the same clean, flat "fade box + corner
 * accent lines" chrome as the player widget (see {@link HudConfig}'s panel section and
 * {@code HudPanel}), with no drop shadow and no ammo gauge bar under the numbers — numbers only.
 * Entirely inert (never registers its overlay) when TACZ isn't installed.
 */
@Mod.EventBusSubscriber(modid = "modernhud", bus = Mod.EventBusSubscriber.Bus.MOD)
public class WeaponConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.EnumValue<WeaponHudSource> PREFERRED_HUD_SOURCE;
    private static final ForgeConfigSpec.BooleanValue IGNORE_HIDE_GUI;
    private static final ForgeConfigSpec.BooleanValue SHOW_NAME;
    private static final ForgeConfigSpec.BooleanValue SHOW_FIRE_MODE;
    private static final ForgeConfigSpec.EnumValue<HeatDisplayMode> HEAT_DISPLAY_MODE;
    private static final ForgeConfigSpec.ConfigValue<String> HEAT_BAR_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> HEAT_BAR_LOCKED_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> HEAT_FLASH_COLOR;
    private static final ForgeConfigSpec.BooleanValue SHOW_RESERVE_AMMO;

    private static final ForgeConfigSpec.EnumValue<HudPosition> POSITION;
    private static final ForgeConfigSpec.IntValue OFFSET_X;
    private static final ForgeConfigSpec.IntValue OFFSET_Y;
    private static final ForgeConfigSpec.DoubleValue SCALE;

    private static final ForgeConfigSpec.DoubleValue ICON_SCALE;
    private static final ForgeConfigSpec.DoubleValue AMMO_TEXT_SCALE;
    private static final ForgeConfigSpec.IntValue FRAME_WIDTH;
    private static final ForgeConfigSpec.IntValue FRAME_HEIGHT;
    private static final ForgeConfigSpec.BooleanValue ZERO_PAD_AMMO;

    private static final ForgeConfigSpec.BooleanValue SHOW_SECONDARY;
    private static final ForgeConfigSpec.DoubleValue SECONDARY_ICON_SCALE;
    private static final ForgeConfigSpec.DoubleValue SECONDARY_ALPHA;


    private static final ForgeConfigSpec.BooleanValue SHOW_PANEL;
    private static final ForgeConfigSpec.ConfigValue<String> PANEL_COLOR;
    private static final ForgeConfigSpec.IntValue PANEL_OPACITY;
    private static final ForgeConfigSpec.EnumValue<PanelBorderStyle> BORDER_STYLE;
    private static final ForgeConfigSpec.ConfigValue<String> BORDER_COLOR;
    private static final ForgeConfigSpec.IntValue BORDER_OPACITY;
    private static final ForgeConfigSpec.IntValue BORDER_LENGTH;
    private static final ForgeConfigSpec.IntValue BORDER_OUTSET;
    private static final ForgeConfigSpec.IntValue BORDER_SCALE;
    private static final ForgeConfigSpec.BooleanValue SHOW_WEAPON_GLOW;
    private static final ForgeConfigSpec.ConfigValue<String> WEAPON_GLOW_COLOR;
    private static final ForgeConfigSpec.BooleanValue SHOW_AMMO_TYPE_ICON;
    private static final ForgeConfigSpec.DoubleValue AMMO_TYPE_ICON_SCALE;
    private static final ForgeConfigSpec.BooleanValue SHOW_ATTACHMENT_DOTS;
    private static final ForgeConfigSpec.ConfigValue<String> ATTACHMENT_DOT_COLOR;
    private static final ForgeConfigSpec.IntValue SECONDARY_DOT_SIZE;
    private static final ForgeConfigSpec.BooleanValue SHOW_SWITCH_SLIDE;
    private static final ForgeConfigSpec.IntValue SWITCH_SLIDE_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue SHOW_SWITCH_BORDER_FLASH;
    private static final ForgeConfigSpec.ConfigValue<String> SWITCH_BORDER_FLASH_COLOR;
    private static final ForgeConfigSpec.IntValue SWITCH_BORDER_FLASH_MS;

    private static final ForgeConfigSpec.ConfigValue<String> AMMO_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> AMMO_LOW_COLOR;
    private static final ForgeConfigSpec.DoubleValue LOW_AMMO_THRESHOLD_PERCENT;
    private static final ForgeConfigSpec.ConfigValue<String> RESERVE_COLOR;
    private static final ForgeConfigSpec.DoubleValue RESERVE_SCALE_OFFSET;
    private static final ForgeConfigSpec.IntValue RESERVE_OPACITY;
    private static final ForgeConfigSpec.ConfigValue<String> NAME_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> FIRE_MODE_COLOR;

    private static final ForgeConfigSpec.BooleanValue SHOW_FIRE_TYPE_FLASH;
    private static final ForgeConfigSpec.ConfigValue<String> FIRE_TYPE_FLASH_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> FIRE_TYPE_HOLD_COLOR;
    private static final ForgeConfigSpec.IntValue FIRE_TYPE_FLASH_MS;
    private static final ForgeConfigSpec.IntValue FIRE_TYPE_HOLD_MS;
    private static final ForgeConfigSpec.IntValue FIRE_TYPE_FADE_MS;
    private static final ForgeConfigSpec.DoubleValue FIRE_TYPE_SCALE;
    private static final ForgeConfigSpec.IntValue FIRE_TYPE_X_OFFSET;
    private static final ForgeConfigSpec.IntValue FIRE_TYPE_Y_OFFSET;
    private static final ForgeConfigSpec.BooleanValue FIRE_TYPE_SHOW_LABEL;

    private static final ForgeConfigSpec.BooleanValue SHOW_AMMO_ALERTS;
    private static final ForgeConfigSpec.ConfigValue<String> RELOAD_ALERT_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> NO_AMMO_ALERT_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> LOW_AMMO_ALERT_COLOR;
    private static final ForgeConfigSpec.DoubleValue AMMO_ALERT_SCALE;
    private static final ForgeConfigSpec.IntValue AMMO_ALERT_Y_OFFSET;
    private static final ForgeConfigSpec.BooleanValue AMMO_ALERT_PULSE;
    private static final ForgeConfigSpec.BooleanValue AMMO_ALERT_GLOW;
    private static final ForgeConfigSpec.BooleanValue SHOW_GEAR;
    private static final ForgeConfigSpec.BooleanValue GEAR_COUNT_WHOLE_INVENTORY;
    private static final ForgeConfigSpec.BooleanValue GEAR_GLOW;
    private static final ForgeConfigSpec.BooleanValue GEAR_TOP_DOT_INNER;
    private static final ForgeConfigSpec.BooleanValue GEAR_AUTO_LAYOUT;
    private static final ForgeConfigSpec.EnumValue<SpentMarker> GEAR_SPENT_MARKER;
    private static final ForgeConfigSpec.BooleanValue GEAR_CENTER_ICONS;
    private static final ForgeConfigSpec.ConfigValue<String> GEAR_SPENT_COLOR;
    private static final ForgeConfigSpec.BooleanValue SEL_GLOW_ENABLED;
    private static final ForgeConfigSpec.IntValue SEL_GLOW_OPACITY;
    private static final ForgeConfigSpec.IntValue SEL_GLOW_RADIUS;
    private static final ForgeConfigSpec.IntValue SEL_GLOW_TL;
    private static final ForgeConfigSpec.IntValue SEL_GLOW_TR;
    private static final ForgeConfigSpec.IntValue SEL_GLOW_BR;
    private static final ForgeConfigSpec.IntValue SEL_GLOW_BL;
    private static final ForgeConfigSpec.BooleanValue AMMO_GLOW;
    private static final ForgeConfigSpec.IntValue AMMO_GLOW_OPACITY;
    private static final ForgeConfigSpec.IntValue AMMO_GLOW_SPREAD;
    private static final ForgeConfigSpec.IntValue AMMO_GLOW_BOX;
    private static final ForgeConfigSpec.IntValue AMMO_GLOW_HALO;
    private static final ForgeConfigSpec.BooleanValue AMMO_GLOW_FOLLOW_LOW;
    private static final ForgeConfigSpec.BooleanValue AMMO_GLOW_PULSE;
    private static final ForgeConfigSpec.BooleanValue AMMO_GLOW_LOW_PULSE;
    private static final ForgeConfigSpec.IntValue INFO_BOX_RISE;
    private static final ForgeConfigSpec.BooleanValue AMMO_GLOW_RESERVE_PANEL;
    private static final ForgeConfigSpec.IntValue AMMO_GLOW_RESERVE_PANEL_OPACITY;
    private static final ForgeConfigSpec.IntValue AMMO_GLOW_RESERVE_PANEL_HEIGHT;
    private static final ForgeConfigSpec.BooleanValue SEL_GLOW_PULSE;
    private static final ForgeConfigSpec.IntValue SEL_GLOW_PULSE_MS;
    private static final ForgeConfigSpec.IntValue SEL_GLOW_PULSE_DEPTH;
    private static final ForgeConfigSpec.IntValue SEL_GLOW_SWITCH_BOOST;

    /** Style of the melee / throwable / consumable gear slots (square-dot outline by default). */
    public static final ElementStyle gearStyle;
    /** Panel / border of the info box that holds the ammo, name and fire mode rows. */
    public static final ElementStyle infoBoxStyle;
    private static final ForgeConfigSpec.IntValue INFO_NAME_GAP;
    private static final ForgeConfigSpec.IntValue INFO_ROW_GAP;
    private static final ForgeConfigSpec.IntValue INFO_BOX_GAP;
    private static final ForgeConfigSpec.IntValue INFO_CONTENT_RAISE;
    private static final ForgeConfigSpec.IntValue INFO_CONTENT_SCALE;
    public static int infoNameGap;
    public static int infoRowGap;
    public static int infoBoxGap;
    public static int infoContentRaise;
    public static int infoContentScale;

    /** Ammo alert look: overall opacity, fade-in / fade-out, optional panel behind the text, border type. */
    public static final ElementStyle alertStyle;

    public static final ForgeConfigSpec SPEC;

    /** Derived from {@link #preferredHudSource} — true only when that's MODERNHUD. Kept as a plain
     *  field (not its own config value) so every existing "is our widget on" check elsewhere in the
     *  codebase keeps working unchanged. */
    public static boolean enabled;
    public static WeaponHudSource preferredHudSource;
    public static boolean ignoreHideGui;
    public static boolean showName;
    public static boolean showFireMode;
    public static HeatDisplayMode heatDisplayMode;
    public static int heatBarColor;
    public static int heatBarLockedColor;
    public static int heatFlashColor;
    public static boolean showReserveAmmo;

    public static HudPosition position;
    public static int offsetX;
    public static int offsetY;
    public static double scale;

    public static double iconScale;
    public static double ammoTextScale;
    public static int frameWidth;
    public static int frameHeight;
    public static boolean zeroPadAmmo;

    public static boolean showSecondary;
    public static double secondaryIconScale;
    public static double secondaryAlpha;


    public static boolean showPanel;
    public static int panelColor;
    public static double panelOpacity;
    public static PanelBorderStyle borderStyle;
    public static int borderColor;
    public static double borderOpacity;
    public static int borderLength;
    public static int borderOutset;
    public static int borderScale = 100;
    public static boolean showWeaponGlow;
    public static int weaponGlowColor;
    public static boolean showAmmoTypeIcon;
    public static double ammoTypeIconScale;
    public static boolean showAttachmentDots;
    public static int attachmentDotColor;
    public static int secondaryDotSize;
    public static boolean showSwitchSlide;
    public static int switchSlideDistance;
    public static boolean showSwitchBorderFlash;
    public static int switchBorderFlashColor;
    public static int switchBorderFlashMs;

    public static int ammoColor;
    public static int ammoLowColor;
    public static double lowAmmoThresholdPercent;
    public static int reserveColor;
    public static double reserveScaleOffset;
    public static double reserveOpacity;
    public static int nameColor;
    public static int fireModeColor;

    public static boolean showFireTypeFlash;
    public static int fireTypeFlashColor;
    public static int fireTypeHoldColor;
    public static int fireTypeFlashMs;
    public static int fireTypeHoldMs;
    public static int fireTypeFadeMs;
    public static double fireTypeScale;
    public static int fireTypeXOffset;
    public static int fireTypeYOffset;
    public static boolean fireTypeShowLabel;

    public static boolean showAmmoAlerts;
    public static int reloadAlertColor;
    public static int noAmmoAlertColor;
    public static int lowAmmoAlertColor;
    public static double ammoAlertScale;
    public static int ammoAlertYOffset;
    public static boolean ammoAlertPulse;
    public static boolean ammoAlertGlow;
    public static boolean showGear;
    public static boolean gearCountWholeInventory;
    public static boolean gearGlow;
    public static boolean gearTopDotInner;
    /** Gear column collapses / repacks itself around the items that are actually carried. */
    public static boolean gearAutoLayout;
    public static SpentMarker gearSpentMarker = SpentMarker.X;
    public static boolean gearCenterIcons = true;
    public static int gearSpentColor = 0xE8ECF0;
    /** Golden corner glow inside the selected weapon box (strength per corner). */
    public static boolean selGlowEnabled;
    public static int selGlowOpacity;
    public static int selGlowRadius;
    /** Strength 0-100 of the glow in each corner of the selected weapon box (0 = that corner stays dark). */
    public static int selGlowTopLeft, selGlowTopRight, selGlowBottomRight, selGlowBottomLeft;
    /** Soft mint glow behind the ammo readout (mag | reserve), like the reference HUD. */
    public static boolean ammoGlow;
    public static int ammoGlowBox;
    public static int ammoGlowHalo;
    public static int ammoGlowOpacity;
    public static int ammoGlowSpread;
    public static boolean ammoGlowFollowLow;
    public static boolean ammoGlowPulse;
    public static boolean ammoGlowLowPulse;
    public static int infoBoxRise;
    public static boolean ammoGlowReservePanel;
    public static int ammoGlowReservePanelOpacity;
    public static int ammoGlowReservePanelHeight;
    public static boolean selGlowPulse;
    public static int selGlowPulseMs;
    public static int selGlowPulseDepth;
    public static int selGlowSwitchBoost;

    static {
        BUILDER.comment("Weapon widget for TACZ guns. Only active when TACZ is installed.")
                .push("general");

        PREFERRED_HUD_SOURCE = BUILDER
                .comment("Which weapon HUD is shown: TACZ_HUD (TACZ's own), TACZ_P_HUD (TaczPresence, falls back",
                        "to TACZ_HUD if missing) or MODERNHUD (this mod). Only the chosen one is drawn.")
                .translation("modernhud.config.weapon_hud_source")
                .defineEnum("hudSource", WeaponHudSource.MODERNHUD);

        IGNORE_HIDE_GUI = BUILDER
                .comment("Keep the weapon widget and ammo alerts visible when F1 (hide GUI) is on. Everything",
                        "else stays hidden.")
                .translation("modernhud.config.weapon_ignore_hide_gui")
                .define("ignoreHideGui", false);

        SHOW_NAME = BUILDER
                .comment("Show the held gun's name under the ammo numbers.")
                .translation("modernhud.config.weapon_show_name")
                .define("showName", true);

        SHOW_FIRE_MODE = BUILDER
                .comment("Show the current fire mode (SEMI, AUTO or BURST).")
                .translation("modernhud.config.weapon_show_fire_mode")
                .define("showFireMode", true);

        HEAT_DISPLAY_MODE = BUILDER
                .comment("How overheat is shown for guns with TACZ's heat mechanic (such as the minigun). NONE",
                        "shows nothing, BAR adds a thin bar under the fire-mode row, FLASH makes the icon box",
                        "glow and pulse faster as it heats up. TACZ's own heat display is only available by",
                        "switching hudSource.")
                .translation("modernhud.config.weapon_heat_display_mode")
                .defineEnum("heatDisplayMode", HeatDisplayMode.FLASH);

        HEAT_BAR_COLOR = BUILDER
                .comment("BAR mode: bar color while below the overheat-locked threshold, as a hex code.")
                .translation("modernhud.config.weapon_heat_bar_color")
                .define("heatBarColor", ColorConfig.toHex(0x5FC8E8));

        HEAT_BAR_LOCKED_COLOR = BUILDER
                .comment("Color once the gun is overheated and locked. FLASH mode also uses it as the hot end of",
                        "the glow.")
                .translation("modernhud.config.weapon_heat_bar_locked_color")
                .define("heatBarLockedColor", ColorConfig.toHex(0xE85138));

        HEAT_FLASH_COLOR = BUILDER
                .comment("FLASH mode: glow color when the gun is cold. It fades to heatBarLockedColor as heat",
                        "rises.")
                .translation("modernhud.config.weapon_heat_flash_color")
                .define("heatFlashColor", ColorConfig.toHex(0xFFD866));

        SHOW_RESERVE_AMMO = BUILDER
                .comment("Show reserve ammo next to the magazine count. Hidden for guns that use inventory ammo",
                        "directly.")
                .translation("modernhud.config.weapon_show_reserve")
                .define("showReserveAmmo", true);

        BUILDER.pop();

        BUILDER.comment("Where the weapon widget is anchored on screen").push("position");

        POSITION = BUILDER
                .comment("Screen corner the widget is attached to. CUSTOM uses the offsets as absolute",
                        "coordinates.")
                .translation("modernhud.config.weapon_position")
                .defineEnum("anchor", HudPosition.BOTTOM_RIGHT);

        OFFSET_X = BUILDER
                .comment("Horizontal distance from the anchor in pixels (absolute X when anchor is CUSTOM).")
                .translation("modernhud.config.weapon_offset_x")
                .defineInRange("offsetX", 8, -10000, 10000);

        OFFSET_Y = BUILDER
                .comment("Vertical distance from the anchor in pixels (absolute Y when anchor is CUSTOM).")
                .translation("modernhud.config.weapon_offset_y")
                .defineInRange("offsetY", 16, -10000, 10000);

        SCALE = BUILDER
                .comment("Overall scale multiplier applied to the whole weapon widget.")
                .translation("modernhud.config.weapon_scale")
                .defineInRange("scale", 0.9, 0.1, 5.0);

        BUILDER.pop();

        BUILDER.comment("Icon scale and the fixed size of the widget. The size does not change from gun to gun.")
                .push("appearance");

        ICON_SCALE = BUILDER
                .comment("Scale of TACZ's fixed 39x13 icon canvas. 1.5 draws it at about 58x19.")
                .translation("modernhud.config.weapon_icon_scale")
                .defineInRange("iconScale", 1.1, 0.25, 6.0);

        AMMO_TEXT_SCALE = BUILDER
                .comment("Scale of the big mag-ammo number specifically (independent of the overall widget scale).")
                .translation("modernhud.config.weapon_ammo_text_scale")
                .defineInRange("ammoTextScale", 1.5, 0.5, 4.0);

        FRAME_WIDTH = BUILDER
                .comment("Width of the whole widget in pixels, before scaling. Used for screen-edge anchoring.")
                .translation("modernhud.config.weapon_panel_width")
                .defineInRange("frameWidth", 129, 32, 1000);

        FRAME_HEIGHT = BUILDER
                .comment("Height of the whole widget in pixels, before scaling. Must fit the icon plus the",
                        "secondary weapon slot under it.")
                .translation("modernhud.config.weapon_panel_height")
                .defineInRange("frameHeight", 40, 16, 1000);

        INFO_BOX_RISE = BUILDER
                .comment("Height of the info box (ammo, name, fire mode): how many pixels it rises above the top of the main",
                        "weapon box. Its bottom edge always lines up with the bottom of the secondary weapon box.",
                        "0 makes the tops flush, negative values make the box shorter than the weapon box.",
                        "The three rows stay tightly packed and centred; extra height only adds margin. Never smaller than the rows need.")
                .translation("modernhud.config.weapon_info_box_rise")
                .defineInRange("infoBoxRise", 5, -10, 40);
        ZERO_PAD_AMMO = BUILDER
                .comment("Pad ammo numbers to three digits (030 instead of 30).")
                .translation("modernhud.config.weapon_zero_pad_ammo")
                .define("zeroPadAmmo", true);

        BUILDER.pop();

        BUILDER.comment("Secondary weapon slot: a small, dim icon of the last different gun you held, if it is",
                        "still in your hotbar.")
                .push("secondary");

        SHOW_SECONDARY = BUILDER
                .comment("Show the secondary weapon thumbnail.")
                .translation("modernhud.config.weapon_show_secondary")
                .define("showSecondary", true);

        SECONDARY_ICON_SCALE = BUILDER
                .comment("Scale of the secondary weapon icon. Keep it below iconScale.")
                .translation("modernhud.config.weapon_secondary_icon_scale")
                .defineInRange("secondaryIconScale", 0.85, 0.15, 4.0);

        SECONDARY_ALPHA = BUILDER
                .comment("Opacity of the secondary weapon icon, 0 to 1.")
                .translation("modernhud.config.weapon_secondary_alpha")
                .defineInRange("secondaryAlpha", 0.8, 0.1, 1.0);

        BUILDER.pop();

        BUILDER.comment("Boxes behind the icons and ammo readout: fills, borders and the weapon-switch",
                        "animation.")
                .push("panel");

        SHOW_PANEL = BUILDER
                .comment("Draw a background box behind the icon boxes and the fire-mode row.")
                .translation("modernhud.config.weapon_show_panel")
                .define("enabled", true);

        PANEL_COLOR = BUILDER
                .comment("Panel background color, as a hex code (e.g. \"0A0D11\" or \"#0A0D11\").")
                .translation("modernhud.config.weapon_panel_color")
                .define("panelColor", ColorConfig.toHex(0xD9DEE3));

        PANEL_OPACITY = BUILDER
                .comment("Box opacity, 0 to 100. The secondary weapon box is also scaled by secondaryAlpha.")
                .translation("modernhud.config.weapon_panel_opacity")
                .defineInRange("panelOpacity", 10, 0, 100);

        BORDER_STYLE = BUILDER
                .comment("Border style: NONE, CORNERS (short L-shaped brackets), FULL (thin outline), DOTS,",
                        "BOTTOM_DOTS or FRAME.")
                .translation("modernhud.config.weapon_border_style")
                .defineEnum("borderStyle", PanelBorderStyle.CORNERS);

        BORDER_COLOR = BUILDER
                .comment("Border/corner-bracket color, as a hex code (e.g. \"E8ECF0\" or \"#E8ECF0\").")
                .translation("modernhud.config.weapon_border_color")
                .define("borderColor", ColorConfig.toHex(0xE8ECF0));

        BORDER_OPACITY = BUILDER
                .comment("Border opacity, 0 to 100.")
                .translation("modernhud.config.weapon_border_opacity")
                .defineInRange("borderOpacity", 90, 0, 100);

        BORDER_LENGTH = BUILDER
                .comment("Length of each bracket arm in pixels. Capped to a fraction of each box's size.")
                .translation("modernhud.config.weapon_border_length")
                .defineInRange("borderLength", 2, 1, 20);

        BORDER_SCALE = BUILDER
                .comment("Scale of the corner dots and corner brackets of the weapon boxes in percent (also the two dots of the",
                        "secondary box). 100 is the size set by borderLength / secondaryDotSize, 50 is half, 200 is double.")
                .translation("modernhud.config.weapon_border_scale")
                .defineInRange("borderScale", 100, 25, 800);

        BORDER_OUTSET = BUILDER
                .comment("How far brackets and dots sit outside the box edge, in pixels. 0 puts them on the",
                        "edge.")
                .translation("modernhud.config.weapon_border_outset")
                .defineInRange("borderOutset", 1, 0, 20);

        SHOW_WEAPON_GLOW = BUILDER
                .comment("Flash behind the main icon when you switch weapons, and a red glow around the ammo row",
                        "while the magazine is low.")
                .translation("modernhud.config.weapon_show_glow")
                .define("glowEnabled", true);

        WEAPON_GLOW_COLOR = BUILDER
                .comment("The one glow color of the weapon widget, as a hex code. Used by the switch flash, the",
                        "corner glow of the selected weapon box and the ammo glow. The low-ammo glow uses",
                        "ammoLowColor.")
                .translation("modernhud.config.weapon_glow_color")
                .define("weaponGlowColor", ColorConfig.toHex(0xF2D98D));

        SHOW_AMMO_TYPE_ICON = BUILDER
                .comment("Small bullet icon next to the reserve count, tinted like the reserve number.")
                .translation("modernhud.config.weapon_show_ammo_type_icon")
                .define("showAmmoTypeIcon", true);

        AMMO_TYPE_ICON_SCALE = BUILDER
                .comment("Size of that icon relative to the reserve number's height.")
                .translation("modernhud.config.weapon_ammo_type_icon_scale")
                .defineInRange("ammoTypeIconScale", 0.7, 0.1, 1.5);

        SHOW_ATTACHMENT_DOTS = BUILDER
                .comment("Row of small squares above the main icon, one per attachment slot the gun has. Filled",
                        "gold when equipped, dim when empty.")
                .translation("modernhud.config.weapon_show_attachment_dots")
                .define("showAttachmentDots", true);

        ATTACHMENT_DOT_COLOR = BUILDER
                .comment("Color of filled attachment dots, as a hex code. Empty slots draw nothing.")
                .translation("modernhud.config.weapon_attachment_dot_color")
                .define("attachmentDotColor", ColorConfig.toHex(0xFFC94A));

        SECONDARY_DOT_SIZE = BUILDER
                .comment("Size in pixels of the two corner squares on the secondary weapon box.")
                .translation("modernhud.config.weapon_secondary_dot_size")
                .defineInRange("secondaryDotSize", 2, 1, 12);

        SHOW_SWITCH_SLIDE = BUILDER
                .comment("Icon box slides up into place when you switch weapons.")
                .translation("modernhud.config.weapon_show_switch_slide")
                .define("showSwitchSlide", true);

        SWITCH_SLIDE_DISTANCE = BUILDER
                .comment("How far below its final position the icon starts on a switch, in pixels, before",
                        "scaling.")
                .translation("modernhud.config.weapon_switch_slide_distance")
                .defineInRange("switchSlideDistance", 8, 0, 100);

        SHOW_SWITCH_BORDER_FLASH = BUILDER
                .comment("Brief gold border flash around the main icon box on a weapon switch.")
                .translation("modernhud.config.weapon_show_switch_border_flash")
                .define("showSwitchBorderFlash", true);

        SWITCH_BORDER_FLASH_COLOR = BUILDER
                .comment("Switch border-flash color, as a hex code. Defaults to a warm gold.")
                .translation("modernhud.config.weapon_switch_border_flash_color")
                .define("switchBorderFlashColor", ColorConfig.toHex(0xFFC94A));

        SWITCH_BORDER_FLASH_MS = BUILDER
                .comment("Time in milliseconds for the flash to fade back to the normal border color.")
                .translation("modernhud.config.weapon_switch_border_flash_ms")
                .defineInRange("switchBorderFlashMs", 450, 50, 5000);

        BUILDER.pop();

        BUILDER.comment("Text colors, as hex codes with or without a leading #.")
                .push("colors");

        AMMO_COLOR = BUILDER
                .comment("Mag ammo number color above the low-ammo threshold.")
                .translation("modernhud.config.weapon_ammo_color")
                .define("ammoColor", ColorConfig.toHex(0xFFFFFF));

        AMMO_LOW_COLOR = BUILDER
                .comment("Mag ammo number color at/below the low-ammo threshold.")
                .translation("modernhud.config.weapon_ammo_low_color")
                .define("ammoLowColor", ColorConfig.toHex(0xE85138));

        LOW_AMMO_THRESHOLD_PERCENT = BUILDER
                .comment("Below this percentage of magazine size the number uses the low-ammo color and the LOW",
                        "AMMO alert appears.")
                .translation("modernhud.config.weapon_low_ammo_threshold")
                .defineInRange("lowAmmoThresholdPercent", 20.0, 0.0, 100.0);

        RESERVE_COLOR = BUILDER
                .comment("Reserve number color, before reserveOpacity.")
                .translation("modernhud.config.weapon_reserve_color")
                .define("reserveColor", ColorConfig.toHex(0xF2F6FA));

        RESERVE_SCALE_OFFSET = BUILDER
                .comment("How much smaller the reserve number is than the magazine number. 0 makes them the same size,",
                        "as in the reference. With pixel-perfect text any value above 0 drops the reserve by one whole",
                        "screen pixel per font pixel, which is a big step at small sizes.")
                .translation("modernhud.config.weapon_reserve_scale_offset")
                .defineInRange("reserveSizeDrop", 0.05, 0.0, 1.0);

        RESERVE_OPACITY = BUILDER
                .comment("Reserve number opacity, 0 to 100.")
                .translation("modernhud.config.weapon_reserve_opacity")
                .defineInRange("reserveOpacity", 70, 0, 100);

        NAME_COLOR = BUILDER
                .comment("Weapon name text color.")
                .translation("modernhud.config.weapon_name_color")
                .define("nameColor", ColorConfig.toHex(0xE6EEF5));

        FIRE_MODE_COLOR = BUILDER
                .comment("Fire mode text color.")
                .translation("modernhud.config.weapon_fire_mode_color")
                .define("fireModeColor", ColorConfig.toHex(0x9FD3F0));

        BUILDER.pop();

        BUILDER.comment("Fire mode announcement shown above the crosshair when you pull a gun: a flash color,",
                        "then a held color, then it fades out.")
                .push("fireTypeFlash");

        SHOW_FIRE_TYPE_FLASH = BUILDER
                .comment("Show the \"FIRE TYPE: ...\" flash announcement on weapon switch.")
                .translation("modernhud.config.weapon_show_fire_type_flash")
                .define("enabled", true);

        FIRE_TYPE_FLASH_COLOR = BUILDER
                .comment("Color during the initial flash phase, hex code.")
                .translation("modernhud.config.weapon_fire_type_flash_color")
                .define("flashColor", ColorConfig.toHex(0xFFD866));

        FIRE_TYPE_HOLD_COLOR = BUILDER
                .comment("Color while the text is held, as a hex code.")
                .translation("modernhud.config.weapon_fire_type_hold_color")
                .define("holdColor", ColorConfig.toHex(0xFFFFFF));

        FIRE_TYPE_FLASH_MS = BUILDER
                .comment("How long the flash color lasts in milliseconds.")
                .translation("modernhud.config.weapon_fire_type_flash_ms")
                .defineInRange("flashMs", 150, 0, 5000);

        FIRE_TYPE_HOLD_MS = BUILDER
                .comment("How long the held color stays fully visible before fading, in milliseconds.")
                .translation("modernhud.config.weapon_fire_type_hold_ms")
                .defineInRange("holdMs", 900, 0, 20000);

        FIRE_TYPE_FADE_MS = BUILDER
                .comment("How long (ms) the fade-out takes once the hold period ends.")
                .translation("modernhud.config.weapon_fire_type_fade_ms")
                .defineInRange("fadeMs", 400, 0, 20000);

        FIRE_TYPE_SCALE = BUILDER
                .comment("Text scale of the announcement.")
                .translation("modernhud.config.weapon_fire_type_scale")
                .defineInRange("scale", 1.0, 0.25, 4.0);

        FIRE_TYPE_X_OFFSET = BUILDER
                .comment("Horizontal offset in px from screen center (positive = further right).")
                .translation("modernhud.config.weapon_fire_type_x_offset")
                .defineInRange("xOffset", 0, -2000, 2000);

        FIRE_TYPE_Y_OFFSET = BUILDER
                .comment("Vertical offset from screen center in pixels. Positive moves it down, negative moves",
                        "it up.")
                .translation("modernhud.config.weapon_fire_type_y_offset")
                .defineInRange("yOffset", 70, -2000, 2000);

        FIRE_TYPE_SHOW_LABEL = BUILDER
                .comment("true shows FIRE TYPE: FULL AUTO, false shows just FULL AUTO.")
                .translation("modernhud.config.weapon_fire_type_show_label")
                .define("showLabel", true);

        BUILDER.pop();

        BUILDER.comment("Center-screen alerts for RELOAD, NO AMMO and LOW AMMO. Not shown for guns that use",
                        "inventory ammo or while overheated.")
                .push("ammoAlerts");

        SHOW_AMMO_ALERTS = BUILDER
                .comment("Show the RELOAD / NO AMMO / LOW AMMO pulsing alert text.")
                .translation("modernhud.config.weapon_show_ammo_alerts")
                .define("enabled", true);

        RELOAD_ALERT_COLOR = BUILDER
                .comment("\"RELOAD\" alert color (mag empty, reserve available), hex code.")
                .translation("modernhud.config.weapon_reload_alert_color")
                .define("reloadAlertColor", ColorConfig.toHex(0xFFD866));

        NO_AMMO_ALERT_COLOR = BUILDER
                .comment("\"NO AMMO\" alert color (mag AND reserve both empty), hex code.")
                .translation("modernhud.config.weapon_no_ammo_alert_color")
                .define("noAmmoAlertColor", ColorConfig.toHex(0xE85138));

        LOW_AMMO_ALERT_COLOR = BUILDER
                .comment("\"LOW AMMO\" alert color (mag below the low-ammo threshold above), hex code.")
                .translation("modernhud.config.weapon_low_ammo_alert_color")
                .define("lowAmmoAlertColor", ColorConfig.toHex(0xFFD866));

        AMMO_ALERT_SCALE = BUILDER
                .comment("Scale of the alert text.")
                .translation("modernhud.config.weapon_ammo_alert_scale")
                .defineInRange("ammoAlertScale", 1.0, 0.25, 4.0);

        AMMO_ALERT_Y_OFFSET = BUILDER
                .comment("Vertical offset in px from screen center (positive = further down).")
                .translation("modernhud.config.weapon_ammo_alert_y_offset")
                .defineInRange("ammoAlertYOffset", 55, -2000, 2000);

        AMMO_ALERT_PULSE = BUILDER
                .comment("The alert text pulses (breathes) while shown. Off = steady.")
                .translation("modernhud.config.weapon_ammo_alert_pulse")
                .define("ammoAlertPulse", false);

        AMMO_ALERT_GLOW = BUILDER
                .comment("Soft glow in the alert's color behind the text.")
                .translation("modernhud.config.weapon_ammo_alert_glow")
                .define("ammoAlertGlow", false);

        alertStyle = new ElementStyle(BUILDER, "style", "alert", new ElementStyle.Defaults(
                false, 0xD9DEE3, 16, PanelBorderStyle.NONE, 3, 1, 160, 260));

        BUILDER.pop();

        BUILDER.comment("Slots next to the weapon box (needs lrtactical): your melee item on top and up to two",
                        "throwables or consumables below with their counts. A used-up item shows a red X and 0.").push("gear");

        SHOW_GEAR = BUILDER
                .comment("Show the gear slots (melee on top, throwable / consumable below).")
                .translation("modernhud.config.weapon_show_gear")
                .define("showGear", true);

        GEAR_COUNT_WHOLE_INVENTORY = BUILDER
                .comment("false looks at hotbar items only. true counts AND picks up the whole inventory: a slot whose item",
                        "ran out then takes over any other item in stock, wherever it is in the inventory.")
                .translation("modernhud.config.weapon_gear_whole_inventory")
                .define("countWholeInventory", false);

        GEAR_GLOW = BUILDER
                .comment("Soft round glow behind the gear icons.")
                .translation("modernhud.config.weapon_gear_glow")
                .define("glow", false);

        GEAR_TOP_DOT_INNER = BUILDER
                .comment("Corner dots on the throwable slots. true puts the top dots on the outer corners, false",
                        "on the inner corners facing each other.")
                .translation("modernhud.config.weapon_gear_top_dot_inner")
                .define("topDotsOuter", false);

        GEAR_AUTO_LAYOUT = BUILDER
                .comment("Only reserve room for slots that are in use. With no gear the weapon box moves into",
                        "the corner, with only throwables the column shrinks to fit them.")
                .translation("modernhud.config.weapon_gear_auto_layout")
                .define("autoLayout", true);

        GEAR_CENTER_ICONS = BUILDER
                .comment("Put the item icon of each gear slot (throwable, consumable, melee) in the centre of its box instead of",
                        "against one side. In the compact row the icon and its count are centred together.")
                .translation("modernhud.config.weapon_gear_center_icons")
                .define("centerIcons", true);

        GEAR_SPENT_MARKER = BUILDER
                .comment("What a gear slot shows when its item is used up or lost: X (a hitmarker-style X with an empty",
                        "centre) or DIM_ICON (the old faded icon). A slot takes over another item in stock by itself.")
                .translation("modernhud.config.weapon_gear_spent_marker")
                .defineEnum("spentMarker", SpentMarker.X);

        GEAR_SPENT_COLOR = BUILDER
                .comment("Colour of the X, as a hex code.")
                .translation("modernhud.config.weapon_gear_spent_color")
                .define("spentColor", ColorConfig.toHex(0xE8ECF0));

        SEL_GLOW_ENABLED = BUILDER
                .comment("Glow in the inner corners of the selected weapon box, drawn behind the gun icon.")
                .translation("modernhud.config.weapon_sel_glow")
                .define("selectedGlow", true);

        SEL_GLOW_OPACITY = BUILDER
                .comment("Strength of the corner glow, 0-100 (also scaled by the global glow intensity).")
                .translation("modernhud.config.weapon_sel_glow_opacity")
                .defineInRange("selectedGlowStrength", 80, 0, 100);

        SEL_GLOW_RADIUS = BUILDER
                .comment("How far each corner glow reaches into the box, in pixels before scaling.")
                .translation("modernhud.config.weapon_sel_glow_radius")
                .defineInRange("selectedGlowReach", 30, 4, 64);

        SEL_GLOW_TL = BUILDER
                .comment("Glow strength in the top-left corner, 0 to 100. 0 turns that corner off.")
                .translation("modernhud.config.weapon_sel_glow_tl")
                .defineInRange("selectedGlowTopLeft", 100, 0, 100);
        SEL_GLOW_TR = BUILDER
                .comment("Glow strength in the top-right inner corner, 0 to 100.")
                .translation("modernhud.config.weapon_sel_glow_tr")
                .defineInRange("selectedGlowTopRight", 0, 0, 100);
        SEL_GLOW_BR = BUILDER
                .comment("Glow strength in the bottom-right corner, 0 to 100.")
                .translation("modernhud.config.weapon_sel_glow_br")
                .defineInRange("selectedGlowBottomRight", 100, 0, 100);
        SEL_GLOW_BL = BUILDER
                .comment("Glow strength in the bottom-left inner corner, 0 to 100.")
                .translation("modernhud.config.weapon_sel_glow_bl")
                .defineInRange("selectedGlowBottomLeft", 0, 0, 100);

        SEL_GLOW_PULSE = BUILDER
                .comment("Slowly breathe the glow in and out instead of keeping it steady.")
                .translation("modernhud.config.weapon_sel_glow_pulse")
                .define("selectedGlowPulse", false);

        SEL_GLOW_PULSE_MS = BUILDER
                .comment("Length of one pulse in milliseconds. Only used when selectedGlowPulse is on.")
                .translation("modernhud.config.weapon_sel_glow_pulse_ms")
                .defineInRange("selectedGlowPulseMs", 1800, 300, 6000);

        SEL_GLOW_PULSE_DEPTH = BUILDER
                .comment("How far the pulse dips, 0 to 100. 100 fades the glow out completely at the low point.")
                .translation("modernhud.config.weapon_sel_glow_pulse_depth")
                .defineInRange("selectedGlowPulseDepth", 50, 0, 100);

        SEL_GLOW_SWITCH_BOOST = BUILDER
                .comment("Extra brightness, 0 to 100, that flashes in when you switch weapons and fades out.")
                .translation("modernhud.config.weapon_sel_glow_switch_boost")
                .defineInRange("selectedGlowSwitchBoost", 40, 0, 100);

        gearStyle = new ElementStyle(BUILDER, "style", "gear", new ElementStyle.Defaults(
                true, 0xD9DEE3, 10, PanelBorderStyle.DOTS, 2, 0, 200, 200));

        BUILDER.pop();

        BUILDER.comment("The box around the ammo numbers, gun name and fire mode: spacing between its rows,",
                "and its own panel and border (separate from the weapon icon boxes). Its height is",
                "appearance.infoBoxRise.").push("infoBox");
        INFO_NAME_GAP = BUILDER
                .comment("Gap in pixels between the ammo numbers and the gun name (0 = touching).")
                .translation("modernhud.config.weapon_info_name_gap")
                .defineInRange("nameGap", 1, 0, 16);
        INFO_ROW_GAP = BUILDER
                .comment("Gap in pixels between the gun name and the fire mode row.")
                .translation("modernhud.config.weapon_info_row_gap")
                .defineInRange("rowGap", 2, 0, 16);
        INFO_BOX_GAP = BUILDER
                .comment("Gap in pixels between the info box and the weapon boxes. The box gets narrower by the same amount,",
                        "so raise appearance.frameWidth by that much to keep its width. Negative values pull it closer.")
                .translation("modernhud.config.weapon_info_box_gap")
                .defineInRange("gapToWeapon", 6, -6, 40);
        INFO_CONTENT_RAISE = BUILDER
                .comment("Moves everything inside the info box (ammo, name, fire mode) up by this many pixels. The box stays put.",
                        "Capped so the rows never leave the box. Negative values move them down.")
                .translation("modernhud.config.weapon_info_content_raise")
                .defineInRange("contentRaise", 4, -10, 30);
        INFO_CONTENT_SCALE = BUILDER
                .comment("Size of the ammo digits, the reserve icon and the fire mode control inside the info box, in percent.",
                        "With pixel-perfect text the digits change in whole-pixel steps, so small changes may not show.")
                .translation("modernhud.config.weapon_info_content_scale")
                .defineInRange("contentScale", 92, 50, 130);
        infoBoxStyle = new ElementStyle(BUILDER, "style", "infobox", new ElementStyle.Defaults(
                false, 0xD9DEE3, 10, PanelBorderStyle.CORNERS, 2, 1, 0, 0));
        BUILDER.pop();

        BUILDER.comment("Soft glow behind the ammo readout (mag | reserve)").push("ammoGlow");
        AMMO_GLOW = BUILDER
                .comment("Glow for the ammo box: a light fill inside the border plus a halo on the magazine",
                        "digits.")
                .translation("modernhud.config.weapon_ammo_glow")
                .define("enabled", true);
        AMMO_GLOW_OPACITY = BUILDER
                .comment("Strength of the halo around the magazine digits, 0 to 100.")
                .translation("modernhud.config.weapon_ammo_glow_opacity")
                .defineInRange("opacity", 35, 0, 100);
        AMMO_GLOW_HALO = BUILDER
                .comment("Reach of the digit halo in font pixels. 0 turns the halo off.")
                .translation("modernhud.config.weapon_ammo_glow_halo")
                .defineInRange("haloRadius", 2, 0, 4);
        AMMO_GLOW_BOX = BUILDER
                .comment("Strength of the soft haze behind the ammo digits, 0 to 100. 0 turns it off.")
                .translation("modernhud.config.weapon_ammo_glow_box")
                .defineInRange("strength", 45, 0, 100);
        AMMO_GLOW_SPREAD = BUILDER
                .comment("How far the fill fades out past the border, in pixels before scaling.")
                .translation("modernhud.config.weapon_ammo_glow_spread")
                .defineInRange("spread", 5, 0, 30);
        AMMO_GLOW_FOLLOW_LOW = BUILDER
                .comment("Color the digit halo with the low-ammo color while the magazine is low.")
                .translation("modernhud.config.weapon_ammo_glow_follow_low")
                .define("followLowAmmo", true);
        AMMO_GLOW_RESERVE_PANEL = BUILDER
                .comment("Small box in the glow color behind the reserve count. It replaces the divider line.")
                .translation("modernhud.config.weapon_ammo_glow_reserve_panel")
                .define("reservePanel", true);
        AMMO_GLOW_RESERVE_PANEL_OPACITY = BUILDER
                .comment("Opacity of the reserve box, 0 to 100.")
                .translation("modernhud.config.weapon_ammo_glow_reserve_panel_opacity")
                .defineInRange("reservePanelOpacity", 25, 0, 100);
        AMMO_GLOW_RESERVE_PANEL_HEIGHT = BUILDER
                .comment("Height of the reserve box as a percentage of the digit height. 100 matches the digits, 90 is slightly shorter.")
                .translation("modernhud.config.weapon_ammo_glow_reserve_panel_height")
                .defineInRange("reservePanelHeight", 90, 40, 160);
        AMMO_GLOW_LOW_PULSE = BUILDER
                .comment("While the magazine is low, the red glow around the ammo row pulses (true) or stays",
                        "steady (false).")
                .translation("modernhud.config.weapon_ammo_glow_low_pulse")
                .define("lowAmmoPulse", false);
        AMMO_GLOW_PULSE = BUILDER
                .comment("Slowly breathe the haze and the digit halo in and out.")
                .translation("modernhud.config.weapon_ammo_glow_pulse")
                .define("pulse", false);
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
        preferredHudSource = PREFERRED_HUD_SOURCE.get();
        ignoreHideGui = IGNORE_HIDE_GUI.get();
        showName = SHOW_NAME.get();
        showFireMode = SHOW_FIRE_MODE.get();
        heatDisplayMode = HEAT_DISPLAY_MODE.get();
        heatBarColor = ColorConfig.fromHex(HEAT_BAR_COLOR.get(), 0x5FC8E8);
        heatBarLockedColor = ColorConfig.fromHex(HEAT_BAR_LOCKED_COLOR.get(), 0xE85138);
        heatFlashColor = ColorConfig.fromHex(HEAT_FLASH_COLOR.get(), 0xFFD866);
        showReserveAmmo = SHOW_RESERVE_AMMO.get();

        position = POSITION.get();
        offsetX = OFFSET_X.get();
        offsetY = OFFSET_Y.get();
        scale = SCALE.get();

        iconScale = ICON_SCALE.get();
        ammoTextScale = AMMO_TEXT_SCALE.get();
        frameWidth = FRAME_WIDTH.get();
        frameHeight = FRAME_HEIGHT.get();
        zeroPadAmmo = ZERO_PAD_AMMO.get();

        showSecondary = SHOW_SECONDARY.get();
        secondaryIconScale = SECONDARY_ICON_SCALE.get();
        secondaryAlpha = SECONDARY_ALPHA.get();


        showPanel = SHOW_PANEL.get();
        panelColor = ColorConfig.fromHex(PANEL_COLOR.get(), 0xD9DEE3);
        panelOpacity = PANEL_OPACITY.get();
        borderStyle = BORDER_STYLE.get();
        borderColor = ColorConfig.fromHex(BORDER_COLOR.get(), 0xE8ECF0);
        borderOpacity = BORDER_OPACITY.get();
        borderLength = BORDER_LENGTH.get();
        borderOutset = BORDER_OUTSET.get();
        borderScale = BORDER_SCALE.get();
        showWeaponGlow = SHOW_WEAPON_GLOW.get();
        weaponGlowColor = ColorConfig.fromHex(WEAPON_GLOW_COLOR.get(), 0xF2D98D);
        showAmmoTypeIcon = SHOW_AMMO_TYPE_ICON.get();
        ammoTypeIconScale = AMMO_TYPE_ICON_SCALE.get();
        showAttachmentDots = SHOW_ATTACHMENT_DOTS.get();
        attachmentDotColor = ColorConfig.fromHex(ATTACHMENT_DOT_COLOR.get(), 0xFFC94A);
        secondaryDotSize = SECONDARY_DOT_SIZE.get();
        showSwitchSlide = SHOW_SWITCH_SLIDE.get();
        switchSlideDistance = SWITCH_SLIDE_DISTANCE.get();
        showSwitchBorderFlash = SHOW_SWITCH_BORDER_FLASH.get();
        switchBorderFlashColor = ColorConfig.fromHex(SWITCH_BORDER_FLASH_COLOR.get(), 0xFFC94A);
        switchBorderFlashMs = SWITCH_BORDER_FLASH_MS.get();

        ammoColor = ColorConfig.fromHex(AMMO_COLOR.get(), 0xFFFFFF);
        ammoLowColor = ColorConfig.fromHex(AMMO_LOW_COLOR.get(), 0xE85138);
        lowAmmoThresholdPercent = LOW_AMMO_THRESHOLD_PERCENT.get();
        reserveColor = ColorConfig.fromHex(RESERVE_COLOR.get(), 0xF2F6FA);
        reserveScaleOffset = RESERVE_SCALE_OFFSET.get();
        reserveOpacity = RESERVE_OPACITY.get();
        nameColor = ColorConfig.fromHex(NAME_COLOR.get(), 0xE6EEF5);
        fireModeColor = ColorConfig.fromHex(FIRE_MODE_COLOR.get(), 0x9FD3F0);

        showFireTypeFlash = SHOW_FIRE_TYPE_FLASH.get();
        fireTypeFlashColor = ColorConfig.fromHex(FIRE_TYPE_FLASH_COLOR.get(), 0xFFD866);
        fireTypeHoldColor = ColorConfig.fromHex(FIRE_TYPE_HOLD_COLOR.get(), 0xFFFFFF);
        fireTypeFlashMs = FIRE_TYPE_FLASH_MS.get();
        fireTypeHoldMs = FIRE_TYPE_HOLD_MS.get();
        fireTypeFadeMs = FIRE_TYPE_FADE_MS.get();
        fireTypeScale = FIRE_TYPE_SCALE.get();
        fireTypeXOffset = FIRE_TYPE_X_OFFSET.get();
        fireTypeYOffset = FIRE_TYPE_Y_OFFSET.get();
        fireTypeShowLabel = FIRE_TYPE_SHOW_LABEL.get();

        showAmmoAlerts = SHOW_AMMO_ALERTS.get();
        reloadAlertColor = ColorConfig.fromHex(RELOAD_ALERT_COLOR.get(), 0xFFD866);
        noAmmoAlertColor = ColorConfig.fromHex(NO_AMMO_ALERT_COLOR.get(), 0xE85138);
        lowAmmoAlertColor = ColorConfig.fromHex(LOW_AMMO_ALERT_COLOR.get(), 0xFFD866);
        ammoAlertScale = AMMO_ALERT_SCALE.get();
        ammoAlertYOffset = AMMO_ALERT_Y_OFFSET.get();
        ColorConfig.migrateDarkPanel(PANEL_COLOR, PANEL_OPACITY, 0xD9DEE3, 14);
        ammoAlertPulse = AMMO_ALERT_PULSE.get();
        ammoAlertGlow = AMMO_ALERT_GLOW.get();
        showGear = SHOW_GEAR.get();
        gearCountWholeInventory = GEAR_COUNT_WHOLE_INVENTORY.get();
        gearGlow = GEAR_GLOW.get();
        gearTopDotInner = !GEAR_TOP_DOT_INNER.get(); // the key is 'topDotsOuter'
        gearAutoLayout = GEAR_AUTO_LAYOUT.get();
        gearSpentMarker = GEAR_SPENT_MARKER.get();
        gearCenterIcons = GEAR_CENTER_ICONS.get();
        gearSpentColor = ColorConfig.fromHex(GEAR_SPENT_COLOR.get(), 0xE8ECF0);
        selGlowEnabled = SEL_GLOW_ENABLED.get();
        selGlowOpacity = SEL_GLOW_OPACITY.get();
        selGlowRadius = SEL_GLOW_RADIUS.get();
        selGlowTopLeft = SEL_GLOW_TL.get();
        selGlowTopRight = SEL_GLOW_TR.get();
        selGlowBottomRight = SEL_GLOW_BR.get();
        selGlowBottomLeft = SEL_GLOW_BL.get();
        ammoGlow = AMMO_GLOW.get();
        ammoGlowBox = AMMO_GLOW_BOX.get();
        ammoGlowHalo = AMMO_GLOW_HALO.get();
        ammoGlowOpacity = AMMO_GLOW_OPACITY.get();
        ammoGlowSpread = AMMO_GLOW_SPREAD.get();
        ammoGlowFollowLow = AMMO_GLOW_FOLLOW_LOW.get();
        ammoGlowPulse = AMMO_GLOW_PULSE.get();
        ammoGlowLowPulse = AMMO_GLOW_LOW_PULSE.get();
        infoBoxRise = INFO_BOX_RISE.get();
        infoNameGap = INFO_NAME_GAP.get();
        infoRowGap = INFO_ROW_GAP.get();
        infoBoxGap = INFO_BOX_GAP.get();
        infoContentRaise = INFO_CONTENT_RAISE.get();
        infoContentScale = INFO_CONTENT_SCALE.get();
        infoBoxStyle.refresh();
        ammoGlowReservePanel = AMMO_GLOW_RESERVE_PANEL.get();
        ammoGlowReservePanelOpacity = AMMO_GLOW_RESERVE_PANEL_OPACITY.get();
        ammoGlowReservePanelHeight = AMMO_GLOW_RESERVE_PANEL_HEIGHT.get();
        selGlowPulse = SEL_GLOW_PULSE.get();
        selGlowPulseMs = SEL_GLOW_PULSE_MS.get();
        selGlowPulseDepth = SEL_GLOW_PULSE_DEPTH.get();
        selGlowSwitchBoost = SEL_GLOW_SWITCH_BOOST.get();
        gearStyle.refresh();
        alertStyle.refresh();

        // Sync every side of the 3-way HUD selector every time this config (re)loads - including
        // from the in-game config screen, not just at startup. See TaczCompat's javadoc for why
        // this exists (multiple HUDs rendering at once is what caused the "icon lệch" bug).
        syncHudSource();
    }

    /** The actual 3-way switch: makes exactly one weapon HUD source active, everywhere. Public so
     *  {@code ClientEvents} can also re-assert it once at client setup, in case TaczPresence's own
     *  config finishes loading after ours (load-order safety net; harmless either way). */
    public static void syncHudSource() {
        enabled = preferredHudSource == WeaponHudSource.MODERNHUD;
        cc.xypp.modernhud.client.compat.TaczCompat.setVanillaGunHudEnabled(!enabled);
        if (preferredHudSource != WeaponHudSource.MODERNHUD) {
            cc.xypp.modernhud.client.compat.TaczCompat.setPresenceHudType(
                    preferredHudSource == WeaponHudSource.TACZ_P_HUD);
        }
    }

    // No runtime position setter here on purpose: unlike the player widget and the feedback icon,
    // the weapon widget's position is config-file-only (modernhud-weapon.toml), not draggable in
    // the in-game HUD editor - see the editor's class javadoc for why.

    /**
     * Advances the 3-way HUD selector for the toggle keybind: MODERNHUD -&gt; TACZ_HUD -&gt;
     * TACZ_P_HUD -&gt; back to MODERNHUD, persisting to disk. Skips TACZ_P_HUD in the cycle when
     * TaczPresence isn't actually installed, since picking it would be indistinguishable from
     * TACZ_HUD anyway.
     */
    public static void cycleHudSource() {
        WeaponHudSource next = switch (preferredHudSource) {
            case MODERNHUD -> WeaponHudSource.TACZ_HUD;
            case TACZ_HUD -> cc.xypp.modernhud.client.compat.TaczCompat.presenceLoaded()
                    ? WeaponHudSource.TACZ_P_HUD : WeaponHudSource.MODERNHUD;
            case TACZ_P_HUD -> WeaponHudSource.MODERNHUD;
        };
        preferredHudSource = next;
        PREFERRED_HUD_SOURCE.set(next);
        syncHudSource();
    }
}

package cc.xypp.modernhud.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Client config for the killstreak / Specialist tracker, drawn only when ModernLoadout (mod id
 * {@code mwloadout}) is installed - see {@code ModernLoadoutCompat} / {@code KillstreakHudOverlay}.
 * ModernLoadout stays the source of truth for the progress itself (kills, thresholds, earned rewards,
 * Hardline/Killchain/Specialist rules); this file only controls how ModernHud DRAWS that state.
 */
@Mod.EventBusSubscriber(modid = "modernhud", bus = Mod.EventBusSubscriber.Bus.MOD)
public class KillstreakConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.BooleanValue ATTACH_TO_WEAPON_HUD;
    private static final ForgeConfigSpec.EnumValue<HudPosition> POSITION;
    private static final ForgeConfigSpec.IntValue OFFSET_X;
    private static final ForgeConfigSpec.IntValue OFFSET_Y;
    private static final ForgeConfigSpec.DoubleValue SCALE_FACTOR;
    private static final ForgeConfigSpec.IntValue SLOT_SIZE;
    private static final ForgeConfigSpec.IntValue SLOT_GAP;
    private static final ForgeConfigSpec.IntValue GROUP_GAP;
    private static final ForgeConfigSpec.BooleanValue SHOW_PROGRESS_BAR;
    private static final ForgeConfigSpec.BooleanValue ICON_BACKDROP;
    private static final ForgeConfigSpec.BooleanValue SHOW_SELECTION;
    private static final ForgeConfigSpec.IntValue SELECTION_SECONDS;
    private static final ForgeConfigSpec.BooleanValue AUTO_HIDE;
    private static final ForgeConfigSpec.IntValue AUTO_HIDE_SECONDS;
    private static final ForgeConfigSpec.ConfigValue<String> READY_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> BAR_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> ACTIVE_COLOR;
    private static final ForgeConfigSpec.BooleanValue PERK_TOAST;
    private static final ForgeConfigSpec.EnumValue<HudPosition> PERK_POSITION;
    private static final ForgeConfigSpec.IntValue PERK_OFFSET_X;
    private static final ForgeConfigSpec.IntValue PERK_OFFSET_Y;
    private static final ForgeConfigSpec.DoubleValue PERK_SCALE;
    private static final ForgeConfigSpec.IntValue PERK_HOLD_SECONDS;
    private static final ForgeConfigSpec.IntValue PERK_ICON_SIZE;
    private static final ForgeConfigSpec.BooleanValue PERK_FRAME;
    private static final ForgeConfigSpec.BooleanValue PERK_HIGHLIGHT;
    private static final ForgeConfigSpec.IntValue ANIMATION_MS;
    private static final ForgeConfigSpec.BooleanValue SPECIALIST_HIDE_FINAL;
    private static final ForgeConfigSpec.BooleanValue SPECIALIST_STOP_WHEN_FULL;
    private static final ForgeConfigSpec.IntValue PERK_STAGGER_MS;
    private static final ForgeConfigSpec.BooleanValue PERK_GLOW;

    /** Slot look: overall opacity, fade-in/out (auto-hide slide), panel + border type. */
    public static final ElementStyle slotStyle;
    /** Perk toast look: overall opacity, slide in/out times (fadeInMs / fadeOutMs), panel, border type. */
    public static final ElementStyle perkStyle;

    public static boolean enabled;
    public static boolean attachToWeaponHud;
    public static HudPosition position;
    public static int offsetX;
    public static int offsetY;
    public static double scaleFactor;
    public static int slotSize;
    public static int slotGap;
    public static int groupGap;
    public static boolean showProgressBar;
    public static boolean iconBackdrop;
    public static boolean showSelection;
    public static int selectionSeconds;
    public static boolean autoHide;
    public static int autoHideSeconds;
    public static int readyColor;
    public static int barColor;
    public static int activeColor;
    public static boolean perkToast;
    public static HudPosition perkPosition;
    public static int perkOffsetX;
    public static int perkOffsetY;
    public static double perkScale;
    public static int perkHoldSeconds;
    public static int perkIconSize;
    public static boolean perkFrame;
    public static boolean perkHighlight;
    public static int animationMs;
    public static boolean specialistHideFinalStage;
    public static boolean specialistStopWhenFull;
    public static int perkStaggerMs;
    public static boolean perkGlow;

    static {
        BUILDER.comment("Killstreak and Specialist tracker: square slots and a segmented kill bar. Needs",
                        "ModernLoadout, which does the tracking; this only draws it.").push("killstreak");

        ENABLED = BUILDER
                .comment("Draw the tracker with ModernHUD. ModernLoadout's own vertical bar is hidden while this",
                        "is on.")
                .translation("modernhud.config.ks_enabled")
                .define("enabled", true);

        ATTACH_TO_WEAPON_HUD = BUILDER
                .comment("Stack the tracker above the weapon widget, on the same edge and at the same scale. Off",
                        "uses position and the offsets below.")
                .translation("modernhud.config.ks_attach")
                .define("attachToWeaponHud", false);

        POSITION = BUILDER
                .comment("Screen corner the tracker is anchored to when attachToWeaponHud is off.")
                .translation("modernhud.config.ks_position")
                .defineEnum("position", HudPosition.BOTTOM_RIGHT);

        OFFSET_X = BUILDER
                .comment("Horizontal distance from the anchor in pixels. Only used when attachToWeaponHud is",
                        "off.")
                .translation("modernhud.config.ks_offset_x")
                .defineInRange("offsetX", 8, -2000, 4000);

        OFFSET_Y = BUILDER
                .comment("Vertical distance from the anchor in pixels. Only used when attachToWeaponHud is off.")
                .translation("modernhud.config.ks_offset_y")
                .defineInRange("offsetY", 120, -2000, 4000);

        SCALE_FACTOR = BUILDER
                .comment("Scale relative to the weapon widget (1.0 is the same size). Absolute scale when",
                        "attachToWeaponHud is off.")
                .translation("modernhud.config.ks_scale")
                .defineInRange("scaleFactor", 1.0, 0.25, 3.0);

        SLOT_SIZE = BUILDER
                .comment("Side of one square slot in px.")
                .translation("modernhud.config.ks_slot_size")
                .defineInRange("slotSize", 25, 16, 48);

        SLOT_GAP = BUILDER
                .comment("Gap between slots in px.")
                .translation("modernhud.config.ks_slot_gap")
                .defineInRange("slotGap", 2, 0, 24);

        GROUP_GAP = BUILDER
                .comment("Gap in pixels between the tracker and the weapon widget. Only used when",
                        "attachToWeaponHud is on.")
                .translation("modernhud.config.ks_group_gap")
                .defineInRange("groupGap", 10, 0, 48);

        SHOW_PROGRESS_BAR = BUILDER
                .comment("Segmented bar next to the slots with one segment per kill still needed for the next",
                        "reward. It fills as kills come in and resets for the next slot.")
                .translation("modernhud.config.ks_show_bar")
                .define("showProgressBar", true);

        ICON_BACKDROP = BUILDER
                .comment("Dim locked / next / used item icons with a dark square laid over them (the old look). Off (default):",
                        "no background of any kind, the slot box alone is the background and a dimmed item is drawn a little darker.")
                .translation("modernhud.config.ks_icon_backdrop")
                .define("iconBackdrop", false);

        SHOW_SELECTION = BUILDER
                .comment("Flash the selected streaks or Specialist perks once when you join and whenever the",
                        "loadout changes.")
                .translation("modernhud.config.ks_show_selection")
                .define("showSelection", false);

        SELECTION_SECONDS = BUILDER
                .comment("How long the selection stays visible before fading, in seconds.")
                .translation("modernhud.config.ks_selection_seconds")
                .defineInRange("selectionSeconds", 4, 1, 30);

        AUTO_HIDE = BUILDER
                .comment("Slide the tracker away after autoHideSeconds without progress and bring it back on a",
                        "kill, a reward or a loadout change. Ready rewards keep it visible. Specialist always",
                        "works this way.")
                .translation("modernhud.config.ks_auto_hide_enabled")
                .define("autoHide", false);

        AUTO_HIDE_SECONDS = BUILDER
                .comment("Seconds without a change before the tracker slides out.")
                .translation("modernhud.config.ks_auto_hide")
                .defineInRange("autoHideSeconds", 5, 1, 120);

        READY_COLOR = BUILDER
                .comment("Glow and border color of a reward that is ready to use, as a hex code.")
                .translation("modernhud.config.ks_ready_color")
                .define("readyGlowColor", ColorConfig.toHex(0xA6F58C));

        BAR_COLOR = BUILDER
                .comment("Color of the kill bar segments and the next slot's accent, as a hex code.")
                .translation("modernhud.config.ks_bar_color")
                .define("killBarColor", ColorConfig.toHex(0xFFFFFF));

        ACTIVE_COLOR = BUILDER
                .comment("Color of Specialist perks that are already active, as a hex code.")
                .translation("modernhud.config.ks_active_color")
                .define("activeGlowColor", ColorConfig.toHex(0xA6F58C));

        SPECIALIST_HIDE_FINAL = BUILDER
                .comment("Specialist: hide the fourth (8 kill) slot. Only the three perk slots are shown.")
                .translation("modernhud.config.ks_specialist_hide_final")
                .define("specialistHideFinalStage", true);

        SPECIALIST_STOP_WHEN_FULL = BUILDER
                .comment("Specialist: once every shown perk is earned, play the completion once, slide away and",
                        "stop updating.")
                .translation("modernhud.config.ks_specialist_stop_when_full")
                .define("specialistStopWhenFull", true);

        ANIMATION_MS = BUILDER
                .comment("Time in milliseconds for slots to slide between the queue and the ready row, and to",
                        "fade.")
                .translation("modernhud.config.ks_animation_ms")
                .defineInRange("animationMs", 420, 0, 3000);

        // fadeInMs / fadeOutMs here are the auto-hide slide-in / slide-out times
        slotStyle = new ElementStyle(BUILDER, "style", "ks", new ElementStyle.Defaults(
                true, 0xD9DEE3, 16, PanelBorderStyle.BOTTOM_DOTS, 3, 1, 460, 460), 90);

        BUILDER.pop();

        BUILDER.comment("Popup listing the perks you selected when you join or when the loadout changes. It",
                        "slides in, holds, then slides out. Needs ModernLoadout.").push("perkToast");

        PERK_TOAST = BUILDER
                .comment("Show the perk popup. In Specialist mode it replaces the small blink of the selected",
                        "perk icons above the tracker.")
                .translation("modernhud.config.perk_toast")
                .define("enabled", true);

        PERK_POSITION = BUILDER
                .comment("Screen corner the toast slides in from / sits in.")
                .translation("modernhud.config.perk_position")
                .defineEnum("position", HudPosition.TOP_RIGHT);

        PERK_OFFSET_X = BUILDER
                .comment("Horizontal offset in px from that corner's edge.")
                .translation("modernhud.config.perk_offset_x")
                .defineInRange("offsetX", 4, -2000, 4000);

        PERK_OFFSET_Y = BUILDER
                .comment("Vertical offset in px from that corner's edge.")
                .translation("modernhud.config.perk_offset_y")
                .defineInRange("offsetY", 40, -2000, 4000);

        PERK_SCALE = BUILDER
                .comment("Overall scale of the toast.")
                .translation("modernhud.config.perk_scale")
                .defineInRange("scale", 1.0, 0.5, 3.0);

        PERK_HOLD_SECONDS = BUILDER
                .comment("How long the popup stays on screen before sliding out, in seconds.")
                .translation("modernhud.config.perk_hold")
                .defineInRange("holdSeconds", 4, 1, 20);

        PERK_ICON_SIZE = BUILDER
                .comment("Size of each perk icon in px.")
                .translation("modernhud.config.perk_icon_size")
                .defineInRange("iconSize", 18, 10, 40);

        PERK_FRAME = BUILDER
                .comment("Draw a thin outline around the popup with a dot on each corner.")
                .translation("modernhud.config.perk_frame")
                .define("frame", true);

        PERK_HIGHLIGHT = BUILDER
                .comment("A very light, thin highlight strip behind each perk's name.")
                .translation("modernhud.config.perk_highlight")
                .define("nameHighlight", true);

        PERK_GLOW = BUILDER
                .comment("Soft glow behind each perk icon and around each perk's box.")
                .translation("modernhud.config.perk_glow")
                .define("glow", true);

        PERK_STAGGER_MS = BUILDER
                .comment("Delay in milliseconds between one perk row sliding in or out and the next.")
                .translation("modernhud.config.perk_stagger_ms")
                .defineInRange("rowStaggerMs", 110, 0, 1000);

        // fadeInMs / fadeOutMs here are the slide-in / slide-out times of each row; the border type applies to the
        // toast's frame (frame = true above): DOTS = a dot on each of the 4 outer corners (default), FRAME = dots + thin line, FULL, CORNERS, NONE. panelEnabled/panelColor/panelOpacity style each perk's OWN thin box
        perkStyle = new ElementStyle(BUILDER, "style", "perk", new ElementStyle.Defaults(
                false, 0xD9DEE3, 14, PanelBorderStyle.CORNERS, 1, 0, 480, 420));

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
        attachToWeaponHud = ATTACH_TO_WEAPON_HUD.get();
        position = POSITION.get();
        offsetX = OFFSET_X.get();
        offsetY = OFFSET_Y.get();
        scaleFactor = SCALE_FACTOR.get();
        slotSize = SLOT_SIZE.get();
        slotGap = SLOT_GAP.get();
        groupGap = GROUP_GAP.get();
        showProgressBar = SHOW_PROGRESS_BAR.get();
        iconBackdrop = ICON_BACKDROP.get();
        showSelection = SHOW_SELECTION.get();
        selectionSeconds = SELECTION_SECONDS.get();
        autoHide = AUTO_HIDE.get();
        autoHideSeconds = AUTO_HIDE_SECONDS.get();
        readyColor = ColorConfig.fromHex(READY_COLOR.get(), 0xA6F58C);
        barColor = ColorConfig.fromHex(BAR_COLOR.get(), 0xFFFFFF);
        activeColor = ColorConfig.fromHex(ACTIVE_COLOR.get(), 0xA6F58C);
        perkToast = PERK_TOAST.get();
        perkPosition = PERK_POSITION.get();
        perkOffsetX = PERK_OFFSET_X.get();
        perkOffsetY = PERK_OFFSET_Y.get();
        perkScale = PERK_SCALE.get();
        perkHoldSeconds = PERK_HOLD_SECONDS.get();
        perkIconSize = PERK_ICON_SIZE.get();
        perkFrame = PERK_FRAME.get();
        perkHighlight = PERK_HIGHLIGHT.get();
        animationMs = ANIMATION_MS.get();
        specialistHideFinalStage = SPECIALIST_HIDE_FINAL.get();
        specialistStopWhenFull = SPECIALIST_STOP_WHEN_FULL.get();
        perkStaggerMs = PERK_STAGGER_MS.get();
        perkGlow = PERK_GLOW.get();
        slotStyle.refresh();
        perkStyle.refresh();
    }
}

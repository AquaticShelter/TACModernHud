package cc.xypp.modernhud.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * The compass. There is only ONE style now (the reference-style tape with the bracketed readout), so the old CLASSIC
 * renderer and its ~95 options are gone. What is left is grouped in sections: {@code general}, {@code tape},
 * {@code readout}, {@code markers}, {@code pings} and {@code gunfire}.
 */
@Mod.EventBusSubscriber(modid = "modernhud", bus = Mod.EventBusSubscriber.Bus.MOD)
public class CompassConfig {
    private static final Builder BUILDER = new Builder();
    public static final ForgeConfigSpec SPEC;

    // ---- general
    public static final BooleanValue ENABLED;

    // ---- tape
    public static final DoubleValue TAPE_LENGTH;
    public static final DoubleValue SCALE;
    public static final DoubleValue OFFSET_Y;
    public static final IntValue FADE_LENGTH;
    public static final ConfigValue<String> COLOR;
    public static final DoubleValue DIM_OPACITY;
    public static final BooleanValue TEXT_SHADOW;

    // ---- readout
    public static final BooleanValue SHOW_READOUT;
    public static final EnumValue<PanelBorderStyle> READOUT_BORDER;
    public static final EnumValue<LocationDisplayMode> LOCATION;
    public static final BooleanValue SHOW_SIDE_INFO;

    // ---- markers
    public static final BooleanValue SHOW_MARKERS;
    // ---- pings (Modern Ping / Ping Wheel)
    public static final BooleanValue PINGS_ENABLED;
    public static final BooleanValue PING_ENEMY_PLAYERS;
    public static final BooleanValue PING_HOSTILE_MOBS;
    public static final BooleanValue PING_VEHICLES;
    public static final BooleanValue PING_NEUTRAL_VEHICLES;
    public static final ConfigValue<String> PING_VEHICLE_PREFIXES;
    public static final ConfigValue<String> PING_VEHICLE_NAMESPACES;
    public static final BooleanValue PING_DEBUG;
    public static final BooleanValue PING_LOCATIONS;
    public static final BooleanValue PING_OTHER_ENTITIES;
    public static final IntValue PING_MAX_DISTANCE;
    public static final DoubleValue PING_MARKER_SCALE;
    public static final BooleanValue PING_DISTANCE_SCALING;
    public static final IntValue PING_FAR_DISTANCE;
    public static final IntValue PING_FAR_SCALE_PERCENT;
    public static final BooleanValue PING_EDGE_HINTS;
    public static final ConfigValue<String> PING_ENEMY_COLOR;
    public static final ConfigValue<String> PING_HOSTILE_COLOR;
    public static final ConfigValue<String> PING_VEHICLE_COLOR;
    public static final ConfigValue<String> PING_LOCATION_COLOR;
    public static final BooleanValue PING_LOCATION_TEAM_COLOR;
    public static final ConfigValue<String> PING_OTHER_COLOR;
    public static final BooleanValue TACZ_GUNFIRE_ENABLED;
    public static final ConfigValue<String> TACZ_GUNFIRE_COLOR;
    public static final DoubleValue TACZ_GUNFIRE_MAX_SCALE_PERCENT;
    public static final DoubleValue TACZ_GUNFIRE_MIN_SCALE_PERCENT;
    public static final DoubleValue TACZ_GUNFIRE_MAX_SCALE_DISTANCE;
    public static final DoubleValue TACZ_GUNFIRE_MIN_SCALE_DISTANCE;
    public static final DoubleValue TACZ_GUNFIRE_POP_DURATION;
    public static final DoubleValue TACZ_GUNFIRE_VISIBLE_DURATION;
    public static final DoubleValue TACZ_GUNFIRE_FADE_DURATION;

    public static volatile boolean enabled = true;
    private static volatile long revision;

    public static int argb(String hex, double opacity) {
        int alpha = (int) (opacity * 255.0);
        int rgb;
        try {
            int start = hex.startsWith("#") ? 1 : 0;
            rgb = Integer.parseInt(hex, start, hex.length(), 16);
        } catch (NumberFormatException e) {
            rgb = 0xFFFFFF;
        }
        return alpha << 24 | rgb & 0xFFFFFF;
    }

    /** Bumped on every (re)load: renderers that cache colours / metrics compare against it. */
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
        revision++;
    }

    static {
        BUILDER.comment("Master switch").push("general");
        ENABLED = BUILDER.comment("Show the compass. Also toggleable in-game with the keybind (unbound by default).")
                .translation("modernhud.config.compass_enabled")
                .define("enabled", true);
        BUILDER.pop();

        BUILDER.comment("The heading tape").push("tape");
        TAPE_LENGTH = BUILDER.comment("Length of the tape in GUI units. It always covers 90 degrees. 420 matches the",
                        "reference proportions.")
                .translation("modernhud.config.compass_modern_width")
                .defineInRange("length", 400.0, 120.0, 1200.0);
        SCALE = BUILDER.comment("Overall scale (text, ticks and readout together).")
                .translation("modernhud.config.compass_modern_scale")
                .defineInRange("scale", 0.6, 0.5, 3.0);
        OFFSET_Y = BUILDER.comment("Distance from the top of the screen, in GUI units.")
                .translation("modernhud.config.compass_modern_offset_y")
                .defineInRange("offsetY", 8.0, -200.0, 600.0);
        FADE_LENGTH = BUILDER.comment("How far (GUI units) the tape fades out at both ends.")
                .translation("modernhud.config.compass_modern_fade_length")
                .defineInRange("fadeLength", 80, 1, 600);
        COLOR = BUILDER.comment("Color of ticks, numbers and the readout.")
                .translation("modernhud.config.compass_modern_color")
                .define("color", "#E8ECF0");
        DIM_OPACITY = BUILDER.comment("Opacity of the small ticks, numbers and place name. Cardinal letters and the readout",
                        "stay fully opaque.")
                .translation("modernhud.config.compass_modern_dim_opacity")
                .defineInRange("dimOpacity", 0.51, 0.0, 1.0);
        TEXT_SHADOW = BUILDER.comment("Drop shadow under the compass text (the reference has none).")
                .translation("modernhud.config.compass_modern_text_shadow")
                .define("textShadow", false);
        BUILDER.pop();

        BUILDER.comment("The bracketed readout under the tape").push("readout");
        SHOW_READOUT = BUILDER.comment("Show the readout box (direction + degrees).")
                .translation("modernhud.config.compass_modern_show_readout")
                .define("show", true);
        READOUT_BORDER = BUILDER.comment("Border of the readout box. CORNERS is a faint outline with bright corner brackets, any",
                        "other value is that plain border style.")
                .translation("modernhud.config.compass_modern_readout_border")
                .defineEnum("border", PanelBorderStyle.CORNERS);
        LOCATION = BUILDER.comment("The place name next to the readout: BIOME, COORDINATES or OFF.")
                .translation("modernhud.config.compass_modern_location")
                .defineEnum("location", LocationDisplayMode.BIOME);
        SHOW_SIDE_INFO = BUILDER.comment("Small dim coordinate / altitude text at the two ends of the tape.")
                .translation("modernhud.config.compass_modern_show_side_info")
                .define("sideInfo", true);
        BUILDER.pop();

        BUILDER.comment("Markers drawn on the tape").push("markers");
        SHOW_MARKERS = BUILDER.comment("Draw gunfire and ping markers on the tape. Each type is set up in its own section.")
                .translation("modernhud.config.compass_modern_show_markers")
                .define("show", true);
        BUILDER.pop();

        BUILDER.comment("Ping markers on the tape (Modern Ping, or the original Ping Wheel). Modern Ping sends",
                        "each ping to your whole team, so a ping on an enemy shows on every teammate's compass.").push("pings");
        PINGS_ENABLED = BUILDER.comment("Show pings on the compass.")
                .translation("modernhud.config.ping_enabled")
                .define("enabled", true);
        PING_ENEMY_PLAYERS = BUILDER.comment("Pings that land on a player who is not on your team.")
                .translation("modernhud.config.ping_enemy_players")
                .define("enemyPlayers", true);
        PING_HOSTILE_MOBS = BUILDER.comment("Pings that land on a hostile mob (anything the game counts as an Enemy, such as",
                        "zombies or creepers).")
                .translation("modernhud.config.ping_hostile_mobs")
                .define("hostileMobs", true);
        PING_VEHICLES = BUILDER.comment("Pings that land on a vehicle that is not yours. A vehicle is yours if it is on your",
                        "team or a teammate is riding it.")
                .translation("modernhud.config.ping_vehicles")
                .define("vehicles", true);
        PING_NEUTRAL_VEHICLES = BUILDER.comment("Also show pinged vehicles with no team and no rider.")
                .translation("modernhud.config.ping_neutral_vehicles")
                .define("emptyVehicles", true);
        PING_VEHICLE_PREFIXES = BUILDER.comment("Class name prefixes, separated by commas, that identify a vehicle. The default covers",
                        "SuperbWarfare. Add another vehicle mod's package to include it.")
                .translation("modernhud.config.ping_vehicle_prefixes")
                .define("vehicleClassPrefixes", "com.atsuishio.superbwarfare.entity.vehicle");
        PING_VEHICLE_NAMESPACES = BUILDER.comment("Mod ids, separated by commas, whose entities (except projectiles and items) count as",
                        "vehicles, matched on the entity type id such as superbwarfare:bmp_2.")
                .translation("modernhud.config.ping_vehicle_namespaces")
                .define("vehicleNamespaces", "superbwarfare");
        PING_DEBUG = BUILDER.comment("Write one line per new ping to logs/latest.log (entity class, entity type and why it",
                        "was shown or hidden). Lines start with ModernHud ping:.")
                .translation("modernhud.config.ping_debug")
                .define("debugLog", false);
        PING_LOCATIONS = BUILDER.comment("Plain location pings (the ping did not land on any entity).")
                .translation("modernhud.config.ping_locations")
                .define("locations", false);
        PING_OTHER_ENTITIES = BUILDER.comment("Pings on any other entity (animals, items, villagers...).")
                .translation("modernhud.config.ping_other_entities")
                .define("otherEntities", false);
        PING_MAX_DISTANCE = BUILDER.comment("Hide pings farther away than this many blocks. 0 means no limit.")
                .translation("modernhud.config.ping_max_distance")
                .defineInRange("maxDistance", 0, 0, 4096);
        PING_MARKER_SCALE = BUILDER.comment("Size of the ping markers on the tape.")
                .translation("modernhud.config.ping_marker_scale")
                .defineInRange("markerScale", 1.0, 0.5, 3.0);
        PING_DISTANCE_SCALING = BUILDER.comment("Shrink far markers so near threats stand out.")
                .translation("modernhud.config.ping_distance_scaling")
                .define("distanceScaling", true);
        PING_FAR_DISTANCE = BUILDER.comment("Distance in blocks at which a marker has shrunk to its smallest size.")
                .translation("modernhud.config.ping_far_distance")
                .defineInRange("farDistance", 80, 8, 1024);
        PING_FAR_SCALE_PERCENT = BUILDER.comment("Marker size at that distance, as a percentage of the full size.")
                .translation("modernhud.config.ping_far_scale")
                .defineInRange("farScalePercent", 60, 20, 100);
        PING_EDGE_HINTS = BUILDER.comment("Show an arrow at the end of the tape for pings outside the 90 degree view.")
                .translation("modernhud.config.ping_edge_hints")
                .define("edgeHints", true);
        PING_ENEMY_COLOR = BUILDER.comment("Enemy player marker (a filled diamond).")
                .translation("modernhud.config.ping_enemy_color")
                .define("enemyColor", "#FF3B30");
        PING_HOSTILE_COLOR = BUILDER.comment("Hostile mob marker (a filled square).")
                .translation("modernhud.config.ping_hostile_color")
                .define("hostileColor", "#FF9F0A");
        PING_VEHICLE_COLOR = BUILDER.comment("Vehicle marker (a filled triangle).")
                .translation("modernhud.config.ping_vehicle_color")
                .define("vehicleColor", "#FFD60A");
        PING_LOCATION_COLOR = BUILDER.comment("Color of location pings (hollow diamond) when the pinger's team color is not used.")
                .translation("modernhud.config.ping_location_color")
                .define("locationColor", "#FFFFFF");
        PING_LOCATION_TEAM_COLOR = BUILDER.comment("Color location pings with the pinger's team color when they have one.")
                .translation("modernhud.config.ping_location_team_color")
                .define("locationTeamColor", true);
        PING_OTHER_COLOR = BUILDER.comment("Marker for other entities (a hollow square).")
                .translation("modernhud.config.ping_other_color")
                .define("otherColor", "#B8C4CC");
        BUILDER.pop();

        BUILDER.comment("TACZ gunfire compatibility").push("gunfire");
        TACZ_GUNFIRE_ENABLED = BUILDER.comment("Show enemy TACZ shooters heard by this client. Detection uses TACZ's own shot packets,",
                        "so attachments, silencers and sound distance are respected.")
                .translation("modernhud.config.tacz_gunfire_enabled")
                .define("enabled", true);
        TACZ_GUNFIRE_COLOR = BUILDER.comment("Marker color.")
                .translation("modernhud.config.tacz_gunfire_color")
                .define("color", "#FF3B30");
        TACZ_GUNFIRE_MAX_SCALE_PERCENT = BUILDER.comment("Marker size at close range, as a percentage of the icon scale.")
                .translation("modernhud.config.tacz_gunfire_max_scale_percent")
                .defineInRange("maxScalePercent", 100.0, 1.0, 1000.0);
        TACZ_GUNFIRE_MIN_SCALE_PERCENT = BUILDER.comment("Marker size at long range as a percentage of the icon scale. Raise it if far markers",
                        "are too small.")
                .translation("modernhud.config.tacz_gunfire_min_scale_percent")
                .defineInRange("minScalePercent", 50.0, 1.0, 1000.0);
        TACZ_GUNFIRE_MAX_SCALE_DISTANCE = BUILDER.comment("Distance in blocks that still uses the maximum marker scale.")
                .translation("modernhud.config.tacz_gunfire_max_scale_distance")
                .defineInRange("maxScaleDistance", 10.0, 0.0, 10000.0);
        TACZ_GUNFIRE_MIN_SCALE_DISTANCE = BUILDER.comment("Distance in blocks that reaches the minimum marker scale (linear in between).")
                .translation("modernhud.config.tacz_gunfire_min_scale_distance")
                .defineInRange("minScaleDistance", 50.0, 0.01, 10000.0);
        TACZ_GUNFIRE_POP_DURATION = BUILDER.comment("Icon pop duration after every shot, in seconds.")
                .translation("modernhud.config.tacz_gunfire_pop_duration")
                .defineInRange("popDuration", 0.15, 0.01, 5.0);
        TACZ_GUNFIRE_VISIBLE_DURATION = BUILDER.comment("Fully visible time after the most recent shot, in seconds.")
                .translation("modernhud.config.tacz_gunfire_visible_duration")
                .defineInRange("visibleDuration", 1.0, 0.0, 60.0);
        TACZ_GUNFIRE_FADE_DURATION = BUILDER.comment("Fade-out duration after the most recent shot, in seconds.")
                .translation("modernhud.config.tacz_gunfire_fade_duration")
                .defineInRange("fadeDuration", 1.0, 0.01, 10.0);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}

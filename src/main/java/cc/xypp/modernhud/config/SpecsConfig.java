package cc.xypp.modernhud.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Spec readout: a row of small boxes with FPS, ping, packet loss, GPU temperature and the real-world time.
 * Sections: {@code general}, {@code position}, {@code layout}, {@code items}, {@code text}, {@code colors}, {@code fps},
 * {@code ping}, {@code loss}, {@code gpu}, {@code time} and {@code style} (panel / border / fade).
 *
 * <p>Every size is counted in FONT PIXELS: one font pixel is drawn as a whole number of screen pixels, so the boxes
 * and the text always sit exactly on the screen pixel grid.
 */
@Mod.EventBusSubscriber(modid = "modernhud", bus = Mod.EventBusSubscriber.Bus.MOD)
public class SpecsConfig {
    private static final Builder BUILDER = new Builder();
    public static final ForgeConfigSpec SPEC;

    /**
     * Everything the readout can show. The order here is the default order. FPS, ping, packet loss, GPU temperature and
     * time are on by default; the rest is optional (the "mini MSI Afterburner" set).
     */
    public enum Item { FPS, LOW, FRAME, GRAPH, PING, LOSS, TPS, NET, CPU, CPU_TEMP, GPU_LOAD, GPU, VRAM, GPU_POWER, GPU_CLOCK, RAM, JVM, HUD, BATTERY, SESSION, TIME }

    // ---- general
    public static final BooleanValue ENABLED;
    public static final BooleanValue HIDE_WITH_DEBUG;
    public static final BooleanValue SHOW_IN_SCREENS;

    // ---- position
    public static final EnumValue<SpecAnchor> ANCHOR;
    public static final IntValue OFFSET_X;
    public static final IntValue OFFSET_Y;
    public static final IntValue SCALE;

    // ---- layout
    public static final EnumValue<SpecLayout> DIRECTION;
    public static final BooleanValue SEPARATE_BOXES;
    public static final IntValue GAP;
    public static final IntValue BOX_HEIGHT;
    public static final IntValue PAD_X;
    public static final BooleanValue MONOSPACE;
    public static final IntValue CELL_WIDTH;

    // ---- items
    public static final ConfigValue<String> ORDER;
    /** One on / off switch per {@link Item}, in enum order. */
    private static final BooleanValue[] SHOW_CFG = new BooleanValue[Item.values().length];
    /** One word per {@link Item} (the clock has none), in enum order. */
    private static final ConfigValue<String>[] LABEL_CFG = newLabelArray();
    public static final BooleanValue SHOW_LABELS;

    @SuppressWarnings("unchecked")
    private static ConfigValue<String>[] newLabelArray() {
        return (ConfigValue<String>[]) new ConfigValue[Item.values().length];
    }

    // ---- text
    public static final ConfigValue<String> TEXT_COLOR;
    public static final IntValue LABEL_OPACITY;
    public static final BooleanValue UPPERCASE;
    public static final BooleanValue TEXT_SHADOW;

    // ---- colors
    public static final BooleanValue THRESHOLD_COLORS;
    public static final ConfigValue<String> WARN_COLOR;
    public static final ConfigValue<String> CRIT_COLOR;
    public static final IntValue FPS_WARN;
    public static final IntValue FPS_CRIT;
    public static final IntValue PING_WARN;
    public static final IntValue PING_CRIT;
    public static final IntValue LOSS_WARN;
    public static final IntValue LOSS_CRIT;
    public static final IntValue GPU_WARN;
    public static final IntValue GPU_CRIT;
    public static final IntValue CPU_TEMP_WARN;
    public static final IntValue CPU_TEMP_CRIT;
    public static final IntValue MEM_WARN;
    public static final IntValue MEM_CRIT;

    // ---- fps / ping / loss
    public static final IntValue FPS_UPDATE_MS;
    public static final IntValue LOW_WINDOW_SECONDS;
    public static final IntValue HARDWARE_POLL_MS;
    public static final IntValue PING_PROBE_MS;
    public static final IntValue TPS_WINDOW_SECONDS;
    public static final IntValue TPS_WARN;
    public static final IntValue TPS_CRIT;
    public static final IntValue GRAPH_BARS;
    public static final IntValue GRAPH_BAR_WIDTH;
    public static final IntValue GRAPH_BUCKET_MS;
    public static final IntValue GRAPH_MAX_MS;
    public static final BooleanValue MEMORY_SHOW_TOTAL;
    public static final EnumValue<CpuTempSource> CPU_TEMP_SOURCE;
    public static final BooleanValue PING_SHOW_IN_SINGLEPLAYER;
    public static final EnumValue<PingSource> PING_SOURCE;
    public static final IntValue PING_SAMPLES;
    public static final IntValue LOSS_WINDOW_SECONDS;
    public static final IntValue LOSS_TIMEOUT_MS;

    // ---- gpu
    public static final EnumValue<GpuSource> GPU_SOURCE;
    public static final IntValue GPU_INDEX;
    public static final IntValue GPU_POLL_MS;
    public static final EnumValue<TempUnit> GPU_UNIT;
    public static final EnumValue<UnavailableMode> GPU_UNAVAILABLE;

    // ---- time
    public static final EnumValue<TimeFormatMode> TIME_FORMAT;
    public static final BooleanValue TIME_SECONDS;
    public static final BooleanValue TIME_LEADING_ZERO;
    public static final ConfigValue<String> TIME_LANGUAGE;

    // ---- style
    public static final ElementStyle style;

    // ---- baked values
    public static volatile boolean enabled = true;
    public static volatile boolean hideWithDebug = true;
    public static volatile boolean showInScreens;
    public static volatile SpecAnchor anchor = SpecAnchor.TOP_LEFT;
    public static volatile int offsetX;
    public static volatile int offsetY;
    public static volatile float scale = 1f;
    public static volatile SpecLayout direction = SpecLayout.HORIZONTAL;
    public static volatile boolean separateBoxes = true;
    public static volatile int gap = 2;
    public static volatile int boxHeight = 15;
    public static volatile int padX = 4;
    public static volatile boolean monospace = true;
    public static volatile int cellWidth = 6;
    public static volatile Item[] order = Item.values();
    public static final boolean[] SHOW = new boolean[Item.values().length];
    public static final String[] LABELS = new String[Item.values().length];
    public static volatile boolean showLabels = true;
    public static volatile int textColor = 0xFFFFFF;
    public static volatile float labelOpacity = 1f;
    public static volatile boolean uppercase = true;
    public static volatile boolean textShadow;
    public static volatile boolean thresholdColors = true;
    public static volatile int warnColor = 0xFFD24A;
    public static volatile int critColor = 0xFF5A4A;
    public static volatile int fpsWarn = 60;
    public static volatile int fpsCrit = 30;
    public static volatile int pingWarn = 100;
    public static volatile int pingCrit = 200;
    public static volatile int lossWarn = 2;
    public static volatile int lossCrit = 8;
    public static volatile int gpuWarn = 80;
    public static volatile int gpuCrit = 90;
    public static volatile int cpuTempWarn = 80;
    public static volatile int cpuTempCrit = 90;
    public static volatile int memWarn = 85;
    public static volatile int memCrit = 95;
    public static volatile int lowWindowSeconds = 10;
    public static volatile int hardwarePollMs = 500;
    public static volatile int pingProbeMs = 500;
    public static volatile int tpsWindowSeconds = 10;
    public static volatile int tpsWarn = 18;
    public static volatile int tpsCrit = 12;
    public static volatile int graphBars = 60;
    public static volatile int graphBarWidth = 1;
    public static volatile int graphBucketMs = 100;
    public static volatile int graphMaxMs = 25;
    public static volatile boolean memoryShowTotal = true;
    public static volatile CpuTempSource cpuTempSource = CpuTempSource.AUTO;
    public static volatile int fpsUpdateMs = 250;
    public static volatile boolean pingInSingleplayer = true;
    public static volatile int lossWindowSeconds = 30;
    public static volatile int lossTimeoutMs = 1000;
    public static volatile PingSource pingSource = PingSource.AUTO;
    public static volatile int pingSamples = 5;
    public static volatile GpuSource gpuSource = GpuSource.AUTO;
    public static volatile int gpuIndex;
    public static volatile int gpuPollMs = 1000;
    public static volatile TempUnit gpuUnit = TempUnit.CELSIUS;
    public static volatile UnavailableMode gpuUnavailable = UnavailableMode.SHOW_NA;
    public static volatile TimeFormatMode timeFormat = TimeFormatMode.H12;
    public static volatile boolean timeSeconds;
    public static volatile boolean timeLeadingZero;
    public static volatile String timeLanguage = "";

    private static volatile long revision;

    public static long revision() {
        return revision;
    }

    public static void toggleEnabled() {
        boolean next = !ENABLED.get();
        ENABLED.set(next);
        enabled = next;
    }

    /** Ready-made sets of boxes, switched with a key or {@code /modernhud specs preset <name>}. */
    public enum Preset { MINIMAL, STANDARD, GAMING, FULL }

    private static int presetIndex = 1;

    /** Turns exactly the boxes of {@code p} on (and the readout itself), saves, and re-reads the config. */
    public static void applyPreset(Preset p) {
        java.util.Set<Item> on = switch (p) {
            case MINIMAL -> java.util.EnumSet.of(Item.FPS);
            case STANDARD -> java.util.EnumSet.of(Item.FPS, Item.PING, Item.LOSS, Item.GPU, Item.TIME);
            case GAMING -> java.util.EnumSet.of(Item.FPS, Item.LOW, Item.PING, Item.LOSS, Item.TPS, Item.CPU,
                    Item.GPU_LOAD, Item.GPU, Item.VRAM, Item.RAM);
            case FULL -> java.util.EnumSet.allOf(Item.class);
        };
        for (Item it : Item.values()) {
            SHOW_CFG[it.ordinal()].set(on.contains(it));
        }
        ENABLED.set(true);
        presetIndex = p.ordinal();
        refresh();
        try {
            ENABLED.save();
        } catch (RuntimeException ignored) {
            // the values are applied in memory; saving is best effort
        }
    }

    /** Next preset in the order minimal, standard, gaming, full. */
    public static Preset cyclePreset() {
        Preset next = Preset.values()[(presetIndex + 1) % Preset.values().length];
        applyPreset(next);
        return next;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            refresh();
        }
    }

    private static Item word(String p) {
        return switch (p) {
            case "fps" -> Item.FPS;
            case "low", "1low", "low1", "1%low", "fpslow" -> Item.LOW;
            case "frame", "frametime", "ft", "ms" -> Item.FRAME;
            case "graph", "framegraph", "ftgraph" -> Item.GRAPH;
            case "tps" -> Item.TPS;
            case "net", "pkt", "packets", "network" -> Item.NET;
            case "hud", "hudcost" -> Item.HUD;
            case "battery", "bat" -> Item.BATTERY;
            case "session", "playtime", "uptime" -> Item.SESSION;
            case "ping", "latency" -> Item.PING;
            case "loss", "packetloss", "packet_loss" -> Item.LOSS;
            case "cpu", "cpuload", "cpuusage" -> Item.CPU;
            case "cputemp", "cpu_temp" -> Item.CPU_TEMP;
            case "gpuload", "gpuusage", "gpu_load", "gpuuse" -> Item.GPU_LOAD;
            case "gpu", "gputemp", "gpu_temp", "temp" -> Item.GPU;
            case "vram", "gpumem", "gpumemory" -> Item.VRAM;
            case "gpupower", "gpu_power", "power", "watt" -> Item.GPU_POWER;
            case "gpuclock", "gpu_clock", "clock" -> Item.GPU_CLOCK;
            case "ram", "memory", "mem" -> Item.RAM;
            case "jvm", "heap", "javaheap", "game" -> Item.JVM;
            case "time", "irl" -> Item.TIME;
            default -> null;
        };
    }

    /**
     * Parses {@code "fps,ping,loss,gpu,time"}. Unknown words are skipped. An item the list leaves out is put back next to
     * its neighbours in the default order (so enabling CPU later does not drop it after the clock).
     */
    static Item[] parseOrder(String raw) {
        List<Item> out = new ArrayList<>();
        if (raw != null) {
            for (String part : raw.split("[,; ]+")) {
                Item it = word(part.trim().toLowerCase(Locale.ROOT));
                if (it != null && !out.contains(it)) {
                    out.add(it);
                }
            }
        }
        Item[] all = Item.values();
        for (int i = 0; i < all.length; i++) {
            if (out.contains(all[i])) {
                continue;
            }
            int at = 0;
            for (int j = i - 1; j >= 0; j--) { // right after the closest earlier default neighbour that is present
                int idx = out.indexOf(all[j]);
                if (idx >= 0) {
                    at = idx + 1;
                    break;
                }
            }
            out.add(at, all[i]);
        }
        return out.toArray(new Item[0]);
    }

    private static String label(String raw, String fallback) {
        String s = raw == null ? "" : raw.trim();
        return s.isEmpty() ? fallback : s;
    }

    /** Re-reads the spec (also called right after the in-game screen saves). */
    public static void refresh() {
        style.refresh();
        enabled = ENABLED.get();
        hideWithDebug = HIDE_WITH_DEBUG.get();
        showInScreens = SHOW_IN_SCREENS.get();
        anchor = ANCHOR.get();
        offsetX = OFFSET_X.get();
        offsetY = OFFSET_Y.get();
        scale = SCALE.get() / 100f;
        direction = DIRECTION.get();
        separateBoxes = SEPARATE_BOXES.get();
        gap = GAP.get();
        boxHeight = BOX_HEIGHT.get();
        padX = PAD_X.get();
        monospace = MONOSPACE.get();
        cellWidth = CELL_WIDTH.get();
        order = parseOrder(ORDER.get());
        for (Item it : Item.values()) {
            int i = it.ordinal();
            SHOW[i] = SHOW_CFG[i].get();
            LABELS[i] = LABEL_CFG[i] == null ? "" : label(LABEL_CFG[i].get(), DEFAULT_LABEL[i]);
        }
        showLabels = SHOW_LABELS.get();
        textColor = ColorConfig.fromHex(TEXT_COLOR.get(), 0xFFFFFF);
        labelOpacity = LABEL_OPACITY.get() / 100f;
        uppercase = UPPERCASE.get();
        textShadow = TEXT_SHADOW.get();
        thresholdColors = THRESHOLD_COLORS.get();
        warnColor = ColorConfig.fromHex(WARN_COLOR.get(), 0xFFD24A);
        critColor = ColorConfig.fromHex(CRIT_COLOR.get(), 0xFF5A4A);
        fpsWarn = FPS_WARN.get();
        fpsCrit = FPS_CRIT.get();
        pingWarn = PING_WARN.get();
        pingCrit = PING_CRIT.get();
        lossWarn = LOSS_WARN.get();
        lossCrit = LOSS_CRIT.get();
        gpuWarn = GPU_WARN.get();
        gpuCrit = GPU_CRIT.get();
        cpuTempWarn = CPU_TEMP_WARN.get();
        cpuTempCrit = CPU_TEMP_CRIT.get();
        memWarn = MEM_WARN.get();
        memCrit = MEM_CRIT.get();
        lowWindowSeconds = LOW_WINDOW_SECONDS.get();
        hardwarePollMs = HARDWARE_POLL_MS.get();
        pingProbeMs = PING_PROBE_MS.get();
        tpsWindowSeconds = TPS_WINDOW_SECONDS.get();
        tpsWarn = TPS_WARN.get();
        tpsCrit = TPS_CRIT.get();
        graphBars = GRAPH_BARS.get();
        graphBarWidth = GRAPH_BAR_WIDTH.get();
        graphBucketMs = GRAPH_BUCKET_MS.get();
        graphMaxMs = GRAPH_MAX_MS.get();
        memoryShowTotal = MEMORY_SHOW_TOTAL.get();
        cpuTempSource = CPU_TEMP_SOURCE.get();
        fpsUpdateMs = FPS_UPDATE_MS.get();
        pingInSingleplayer = PING_SHOW_IN_SINGLEPLAYER.get();
        lossWindowSeconds = LOSS_WINDOW_SECONDS.get();
        lossTimeoutMs = LOSS_TIMEOUT_MS.get();
        pingSource = PING_SOURCE.get();
        pingSamples = PING_SAMPLES.get();
        gpuSource = GPU_SOURCE.get();
        gpuIndex = GPU_INDEX.get();
        gpuPollMs = GPU_POLL_MS.get();
        gpuUnit = GPU_UNIT.get();
        gpuUnavailable = GPU_UNAVAILABLE.get();
        timeFormat = TIME_FORMAT.get();
        timeSeconds = TIME_SECONDS.get();
        timeLeadingZero = TIME_LEADING_ZERO.get();
        timeLanguage = TIME_LANGUAGE.get().trim();
        revision++;
    }

    /** Default word per item, in enum order. */
    private static final String[] DEFAULT_LABEL = {
            "FPS", "1% LOW", "FRAME", "", "PING", "PACKET LOSS", "TPS", "PACKETS", "CPU", "CPU TEMP", "GPU LOAD", "GPU",
            "VRAM", "GPU POWER", "GPU CLOCK", "RAM", "JVM", "HUD", "BATTERY", "SESSION", ""};

    private static void item(Item it, String key, String keyTr, String what, boolean def, String labelKey) {
        SHOW_CFG[it.ordinal()] = BUILDER.comment("Show " + what + (def ? "." : ". Off by default (optional)."))
                .translation("modernhud.config." + keyTr).define(key, def);
        if (labelKey != null) {
            LABEL_CFG[it.ordinal()] = BUILDER.comment("Word before " + what + ".")
                    .translation("modernhud.config." + keyTr.replace("show_", "label_"))
                    .define(labelKey, DEFAULT_LABEL[it.ordinal()]);
        }
    }

    static {
        BUILDER.comment("Master switch").push("general");
        ENABLED = BUILDER.comment("Show the spec readout (FPS, ping, packet loss, GPU temperature, time).",
                        "Toggle in-game with /modernhud toggle specs.")
                .translation("modernhud.config.specs_enabled")
                .define("enabled", true);
        HIDE_WITH_DEBUG = BUILDER.comment("Hide the readout while the F3 debug screen is open (they share the top-left corner).")
                .translation("modernhud.config.specs_hide_debug")
                .define("hideWithDebug", true);
        SHOW_IN_SCREENS = BUILDER.comment("Keep showing the readout behind menus (inventory, pause menu). Chat always keeps it.")
                .translation("modernhud.config.specs_in_screens")
                .define("showInScreens", false);
        BUILDER.pop();

        BUILDER.comment("Where the readout sits").push("position");
        ANCHOR = BUILDER.comment("Screen corner or edge the readout is attached to.")
                .translation("modernhud.config.specs_anchor")
                .defineEnum("anchor", SpecAnchor.TOP_LEFT);
        OFFSET_X = BUILDER.comment("Distance from the anchored edge in font pixels, going inward. For the centred anchors a positive value moves right.")
                .translation("modernhud.config.specs_offset_x")
                .defineInRange("offsetX", 0, -2000, 2000);
        OFFSET_Y = BUILDER.comment("Distance from the anchored edge in font pixels, going inward (down from the top, up from the bottom).")
                .translation("modernhud.config.specs_offset_y")
                .defineInRange("offsetY", 0, -2000, 2000);
        SCALE = BUILDER.comment("Size in percent. One font pixel is drawn as a WHOLE number of screen pixels, so the size",
                        "changes in steps: 100 is one unit per font pixel (at GUI scale 3 that is 3 screen pixels).")
                .translation("modernhud.config.specs_scale")
                .defineInRange("scale", 100, 25, 600);
        BUILDER.pop();

        BUILDER.comment("Boxes and spacing (all in font pixels)").push("layout");
        DIRECTION = BUILDER.comment("HORIZONTAL puts the boxes in a row (like the reference), VERTICAL stacks them.")
                .translation("modernhud.config.specs_direction")
                .defineEnum("direction", SpecLayout.HORIZONTAL);
        SEPARATE_BOXES = BUILDER.comment("One box per item with a gap between them (reference look). Off draws one",
                        "continuous bar.")
                .translation("modernhud.config.specs_separate")
                .define("separateBoxes", true);
        GAP = BUILDER.comment("Gap between boxes.")
                .translation("modernhud.config.specs_gap")
                .defineInRange("gap", 2, 0, 40);
        BOX_HEIGHT = BUILDER.comment("Height of a box. The text is centred in it. The default 15 leaves 4 pixels above",
                        "and below the 7 pixel tall letters.")
                .translation("modernhud.config.specs_box_height")
                .defineInRange("boxHeight", 15, 9, 60);
        PAD_X = BUILDER.comment("Space left and right of the text inside a box.")
                .translation("modernhud.config.specs_pad_x")
                .defineInRange("padX", 4, 0, 40);
        MONOSPACE = BUILDER.comment("Give every character the same width (like the reference's monospace font), so",
                        "numbers do not jitter when they change.")
                .translation("modernhud.config.specs_monospace")
                .define("monospace", true);
        CELL_WIDTH = BUILDER.comment("Width of one character in monospace mode, in font pixels. Letters are 5 wide, so 6 leaves",
                        "a one pixel gap. 7 or 8 gives the wide, airy look.")
                .translation("modernhud.config.specs_cell_width")
                .defineInRange("cellWidth", 6, 5, 12);
        BUILDER.pop();

        BUILDER.comment("What the readout shows and in which order. The first five are the reference readout; the rest",
                        "are optional and off by default (a mini MSI Afterburner).").push("items");
        ORDER = BUILDER.comment("Order of the boxes, separated by commas. Words: fps, low, frame, graph, ping, loss, tps, net,",
                        "cpu, cputemp, gpuload, gpu, vram, gpupower, gpuclock, ram, jvm, hud, battery, session, time.",
                        "Example: \"time,fps,ping\". Items you leave out keep their usual place.")
                .translation("modernhud.config.specs_order")
                .define("order", "fps,low,frame,graph,ping,loss,tps,net,cpu,cputemp,gpuload,gpu,vram,gpupower,gpuclock,ram,jvm,hud,battery,session,time");
        item(Item.FPS, "fps", "specs_show_fps", "frames per second", true, "labelFps");
        item(Item.LOW, "low1Percent", "specs_show_low", "the 1 % low FPS (the speed of the slowest 1 % of frames)", false, "labelLow");
        item(Item.FRAME, "frameTime", "specs_show_frame", "the frame time in milliseconds", false, "labelFrame");
        item(Item.GRAPH, "frameGraph", "specs_show_graph", "a live frame time graph (one bar per short slice of time, tall = slow)", false, null);
        item(Item.PING, "ping", "specs_show_ping", "the ping (latency to the server)", true, "labelPing");
        item(Item.LOSS, "packetLoss", "specs_show_loss", "the estimated packet loss", true, "labelPacketLoss");
        item(Item.TPS, "tps", "specs_show_tps", "the server TPS (ticks per second, 20 is perfect), measured from the server's own clock", false, "labelTps");
        item(Item.NET, "packets", "specs_show_net", "the packets per second received / sent", false, "labelNet");
        item(Item.CPU, "cpuLoad", "specs_show_cpu", "the CPU usage in percent", false, "labelCpu");
        item(Item.CPU_TEMP, "cpuTemp", "specs_show_cpu_temp", "the CPU temperature", false, "labelCpuTemp");
        item(Item.GPU_LOAD, "gpuLoad", "specs_show_gpu_load", "the GPU usage in percent", false, "labelGpuLoad");
        item(Item.GPU, "gpuTemp", "specs_show_gpu", "the GPU temperature", true, "labelGpu");
        item(Item.VRAM, "vram", "specs_show_vram", "the video memory in use", false, "labelVram");
        item(Item.GPU_POWER, "gpuPower", "specs_show_gpu_power", "the GPU power draw in watts", false, "labelGpuPower");
        item(Item.GPU_CLOCK, "gpuClock", "specs_show_gpu_clock", "the GPU core clock", false, "labelGpuClock");
        item(Item.RAM, "ram", "specs_show_ram", "the system memory in use", false, "labelRam");
        item(Item.JVM, "jvmMemory", "specs_show_jvm", "the memory the game itself uses (Java heap)", false, "labelJvm");
        item(Item.HUD, "hudCost", "specs_show_hud", "how long Modern HUD takes to draw each frame (milliseconds)", false, "labelHud");
        item(Item.BATTERY, "battery", "specs_show_battery", "the battery charge of a laptop (hidden on a desktop)", false, "labelBattery");
        item(Item.SESSION, "session", "specs_show_session", "how long you have been in this world / server", false, "labelSession");
        item(Item.TIME, "time", "specs_show_time", "the real-world time", true, null);
        SHOW_LABELS = BUILDER.comment("Show the words (FPS:, PING:, ...) before the numbers.")
                .translation("modernhud.config.specs_show_labels")
                .define("showLabels", true);
        BUILDER.pop();

        BUILDER.comment("Text").push("text");
        TEXT_COLOR = BUILDER.comment("Text colour as a hex code, for example FFFFFF.")
                .translation("modernhud.config.specs_text_color")
                .define("color", "#FFFFFF");
        LABEL_OPACITY = BUILDER.comment("Opacity of the words before the numbers, 0 to 100. Numbers stay fully opaque.")
                .translation("modernhud.config.specs_label_opacity")
                .defineInRange("labelOpacity", 100, 0, 100);
        UPPERCASE = BUILDER.comment("Write words and units in capitals (MS instead of ms).")
                .translation("modernhud.config.specs_uppercase")
                .define("uppercase", true);
        TEXT_SHADOW = BUILDER.comment("Drop shadow under the text (the reference has none).")
                .translation("modernhud.config.specs_text_shadow")
                .define("textShadow", false);
        BUILDER.pop();

        BUILDER.comment("Warning colours: the number turns yellow, then red, when a value gets bad").push("colors");
        THRESHOLD_COLORS = BUILDER.comment("Colour the numbers by how good the value is.")
                .translation("modernhud.config.specs_threshold_colors")
                .define("thresholdColors", true);
        WARN_COLOR = BUILDER.comment("Warning colour (hex).")
                .translation("modernhud.config.specs_warn_color").define("warnColor", "#FFD24A");
        CRIT_COLOR = BUILDER.comment("Critical colour (hex).")
                .translation("modernhud.config.specs_crit_color").define("critColor", "#FF5A4A");
        FPS_WARN = BUILDER.comment("FPS below this turns yellow.")
                .translation("modernhud.config.specs_fps_warn").defineInRange("fpsWarnBelow", 60, 1, 1000);
        FPS_CRIT = BUILDER.comment("FPS below this turns red.")
                .translation("modernhud.config.specs_fps_crit").defineInRange("fpsCritBelow", 30, 1, 1000);
        PING_WARN = BUILDER.comment("Ping (ms) above this turns yellow.")
                .translation("modernhud.config.specs_ping_warn").defineInRange("pingWarnAbove", 100, 1, 5000);
        PING_CRIT = BUILDER.comment("Ping (ms) above this turns red.")
                .translation("modernhud.config.specs_ping_crit").defineInRange("pingCritAbove", 200, 1, 5000);
        LOSS_WARN = BUILDER.comment("Packet loss (%) above this turns yellow.")
                .translation("modernhud.config.specs_loss_warn").defineInRange("lossWarnAbove", 2, 0, 100);
        LOSS_CRIT = BUILDER.comment("Packet loss (%) above this turns red.")
                .translation("modernhud.config.specs_loss_crit").defineInRange("lossCritAbove", 8, 0, 100);
        GPU_WARN = BUILDER.comment("GPU temperature (in degrees Celsius, whatever unit is shown) above this turns yellow.")
                .translation("modernhud.config.specs_gpu_warn").defineInRange("gpuWarnAbove", 80, 20, 150);
        GPU_CRIT = BUILDER.comment("GPU temperature (in degrees Celsius) above this turns red.")
                .translation("modernhud.config.specs_gpu_crit").defineInRange("gpuCritAbove", 90, 20, 150);
        CPU_TEMP_WARN = BUILDER.comment("CPU temperature (in degrees Celsius) above this turns yellow.")
                .translation("modernhud.config.specs_cpu_temp_warn").defineInRange("cpuTempWarnAbove", 80, 20, 150);
        CPU_TEMP_CRIT = BUILDER.comment("CPU temperature (in degrees Celsius) above this turns red.")
                .translation("modernhud.config.specs_cpu_temp_crit").defineInRange("cpuTempCritAbove", 90, 20, 150);
        TPS_WARN = BUILDER.comment("TPS below this turns yellow.")
                .translation("modernhud.config.specs_tps_warn").defineInRange("tpsWarnBelow", 18, 1, 20);
        TPS_CRIT = BUILDER.comment("TPS below this turns red.")
                .translation("modernhud.config.specs_tps_crit").defineInRange("tpsCritBelow", 12, 1, 20);
        MEM_WARN = BUILDER.comment("Memory (RAM, VRAM, game heap) use in percent of the total above this turns yellow.")
                .translation("modernhud.config.specs_mem_warn").defineInRange("memoryWarnAbove", 85, 1, 100);
        MEM_CRIT = BUILDER.comment("Memory use in percent of the total above this turns red.")
                .translation("modernhud.config.specs_mem_crit").defineInRange("memoryCritAbove", 95, 1, 100);
        BUILDER.pop();

        BUILDER.comment("FPS counter").push("fps");
        FPS_UPDATE_MS = BUILDER.comment("How often the FPS number is refreshed, in milliseconds. It is the average of all",
                        "frames in that time. Shorter reacts faster, longer is calmer.")
                .translation("modernhud.config.specs_fps_update")
                .defineInRange("updateMs", 250, 50, 5000);
        LOW_WINDOW_SECONDS = BUILDER.comment("The 1 % low is measured over the frames of this many seconds.")
                .translation("modernhud.config.specs_low_window")
                .defineInRange("lowWindowSeconds", 10, 2, 60);
        BUILDER.pop();

        BUILDER.comment("Server TPS. Measured from the server's own game clock (a vanilla server sends it every second, any",
                        "server works): ticks that passed divided by real seconds that passed.").push("tps");
        TPS_WINDOW_SECONDS = BUILDER.comment("TPS is averaged over this many seconds. Short reacts faster, long is calmer.")
                .translation("modernhud.config.specs_tps_window")
                .defineInRange("windowSeconds", 10, 3, 60);
        BUILDER.pop();

        BUILDER.comment("Frame time graph: one bar per slice of time, as tall as the slowest frame in that slice.").push("graph");
        GRAPH_BARS = BUILDER.comment("Number of bars.")
                .translation("modernhud.config.specs_graph_bars").defineInRange("bars", 60, 20, 240);
        GRAPH_BAR_WIDTH = BUILDER.comment("Width of a bar in font pixels.")
                .translation("modernhud.config.specs_graph_bar_width").defineInRange("barWidth", 1, 1, 4);
        GRAPH_BUCKET_MS = BUILDER.comment("Time slice per bar in milliseconds (60 bars x 100 ms shows the last 6 seconds).")
                .translation("modernhud.config.specs_graph_bucket").defineInRange("bucketMs", 100, 20, 1000);
        GRAPH_MAX_MS = BUILDER.comment("Frame time that fills the whole graph height, in milliseconds (25 = 40 FPS).")
                .translation("modernhud.config.specs_graph_max").defineInRange("maxMs", 25, 5, 200);
        BUILDER.pop();

        BUILDER.comment("CPU, system memory and the game's memory. Read on a background thread, only while a box that needs",
                        "them is shown. CPU and RAM numbers work everywhere. The CPU temperature needs Linux (works out",
                        "of the box) or LibreHardwareMonitor running in the background on Windows.")
                .push("hardware");
        HARDWARE_POLL_MS = BUILDER.comment("How often CPU usage, RAM and CPU temperature are read, in milliseconds. Shorter is",
                        "closer to real time (250 is smooth, the cost is a few microseconds). A LibreHardwareMonitor / ACPI",
                        "query on Windows is slow, so the CPU temperature there is read at most every 3 seconds.")
                .translation("modernhud.config.specs_hardware_poll")
                .defineInRange("pollMs", 500, 250, 10000);
        MEMORY_SHOW_TOTAL = BUILDER.comment("Show memory as used / total (9.8/15.9 GB) instead of just used.")
                .translation("modernhud.config.specs_memory_total")
                .define("showTotal", true);
        CPU_TEMP_SOURCE = BUILDER.comment("AUTO: Linux sensors on Linux, LibreHardwareMonitor on Windows (N/A if it is not running).",
                        "ACPI_THERMAL_ZONE: a Windows board sensor that needs no program, but often does not follow the CPU.")
                .translation("modernhud.config.specs_cpu_temp_source")
                .defineEnum("cpuTempSource", CpuTempSource.AUTO);
        BUILDER.pop();

        BUILDER.comment("Ping. With Modern HUD on the server too, the client sends a tiny probe twice a second and the",
                        "server answers at once, so the ping is measured live. The vanilla tab-list ping is only updated",
                        "by the server every 15 seconds (spark and similar tools read that same value).").push("ping");
        PING_SOURCE = BUILDER.comment("AUTO: live probe when the server has Modern HUD, otherwise the tab-list ping.",
                        "PROBE: live probe only. TAB_LIST: always the vanilla value.")
                .translation("modernhud.config.specs_ping_source")
                .defineEnum("source", PingSource.AUTO);
        PING_PROBE_MS = BUILDER.comment("How often the live ping probe is sent, in milliseconds (server mod needed). 250 is the",
                        "fastest; the server ignores anything quicker.")
                .translation("modernhud.config.specs_ping_probe")
                .defineInRange("probeMs", 500, 250, 2000);
        PING_SAMPLES = BUILDER.comment("The live ping is the median of this many latest probes (a single spike is ignored).",
                        "1 shows every probe as it comes.")
                .translation("modernhud.config.specs_ping_samples")
                .defineInRange("samples", 5, 1, 20);
        PING_SHOW_IN_SINGLEPLAYER = BUILDER.comment("Show the ping in single player too (it reads 0 there).")
                .translation("modernhud.config.specs_ping_singleplayer")
                .define("showInSingleplayer", true);
        BUILDER.pop();

        BUILDER.comment("Packet loss. Minecraft talks over TCP, which re-sends lost packets, so real loss is never visible",
                        "to a mod. The value is an ESTIMATE of what a player feels: with Modern HUD on the server, the share of",
                        "live probes that got no answer in time. Without it, the server's one-per-second time updates that",
                        "arrive late or not at all. It also shows server lag and stalls.")
                .push("loss");
        LOSS_TIMEOUT_MS = BUILDER.comment("A probe answered later than this, or not at all, counts as lost.")
                .translation("modernhud.config.specs_loss_timeout")
                .defineInRange("timeoutMs", 1000, 200, 5000);
        LOSS_WINDOW_SECONDS = BUILDER.comment("The percentage is measured over this many seconds.")
                .translation("modernhud.config.specs_loss_window")
                .defineInRange("windowSeconds", 30, 10, 120);
        BUILDER.pop();

        BUILDER.comment("GPU temperature, usage, video memory, power and clock. A game cannot read these by itself, so it asks",
                        "the graphics driver. NVIDIA gives everything out of the box. Linux AMD gives most of it with no",
                        "tool. On Windows with AMD or Intel, start LibreHardwareMonitor in the background.")
                .push("gpu");
        GPU_SOURCE = BUILDER.comment("AUTO tries every source and keeps the first that answers.")
                .translation("modernhud.config.specs_gpu_source")
                .defineEnum("source", GpuSource.AUTO);
        GPU_INDEX = BUILDER.comment("Which graphics card to read when there are several (0 is the first).")
                .translation("modernhud.config.specs_gpu_index")
                .defineInRange("index", 0, 0, 7);
        GPU_POLL_MS = BUILDER.comment("How often the GPU values are read, in milliseconds. NVIDIA is read from one long-running",
                        "nvidia-smi, so 250 to 1000 is cheap. LibreHardwareMonitor on Windows is slow and is read at most every",
                        "3 seconds. Always on a background thread: it never costs frames.")
                .translation("modernhud.config.specs_gpu_poll")
                .defineInRange("pollMs", 1000, 250, 10000);
        GPU_UNIT = BUILDER.comment("CELSIUS or FAHRENHEIT (also used for the CPU temperature).")
                .translation("modernhud.config.specs_gpu_unit")
                .defineEnum("unit", TempUnit.CELSIUS);
        GPU_UNAVAILABLE = BUILDER.comment("What to do when no sensor answers: SHOW_NA or HIDE the box (every hardware box).")
                .translation("modernhud.config.specs_gpu_unavailable")
                .defineEnum("whenUnavailable", UnavailableMode.SHOW_NA);
        BUILDER.pop();

        BUILDER.comment("Clock").push("time");
        TIME_FORMAT = BUILDER.comment("H12 (12:27 AM), H24 (00:27) or AUTO (what your system prefers).")
                .translation("modernhud.config.specs_time_format")
                .defineEnum("format", TimeFormatMode.H12);
        TIME_SECONDS = BUILDER.comment("Show seconds.")
                .translation("modernhud.config.specs_time_seconds")
                .define("seconds", false);
        TIME_LEADING_ZERO = BUILDER.comment("Write the hour with two digits (09:05 instead of 9:05).")
                .translation("modernhud.config.specs_time_leading_zero")
                .define("leadingZero", false);
        TIME_LANGUAGE = BUILDER.comment("Language tag for the AM / PM marker, for example vi, en-US, de. Empty uses the",
                        "language of your system.")
                .translation("modernhud.config.specs_time_language")
                .define("language", "");
        BUILDER.pop();

        // dark flat boxes like the reference; borders off by default
        style = new ElementStyle(BUILDER, "style", "specs", new ElementStyle.Defaults(
                true, 0x0F0F0F, 85, PanelBorderStyle.NONE, 4, 0, 150, 150));
        SPEC = BUILDER.build();
    }
}

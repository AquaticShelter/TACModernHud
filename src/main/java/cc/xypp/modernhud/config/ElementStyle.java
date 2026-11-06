package cc.xypp.modernhud.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * One reusable group of "look" options for a HUD element, so every element offers the same set: overall
 * opacity, fade-in / fade-out time, an optional flat panel behind it (colour + opacity) and a border
 * (type: none / corner brackets / line / dots / dots+line, colour, opacity, size, outset).
 *
 * <p>Usage: construct it inside a config class's static initialiser (it adds a sub-section to that builder), then
 * call {@link #refresh()} from the config's load handler; the public fields hold the live values.
 */
public final class ElementStyle {

    /** Default values for a new style group. */
    public record Defaults(boolean panelEnabled, int panelColor, int panelOpacity, PanelBorderStyle borderStyle,
                           int borderLength, int borderOutset, int fadeInMs, int fadeOutMs) {
    }

    private final ForgeConfigSpec.IntValue opacityV;
    private final ForgeConfigSpec.IntValue fadeInV;
    private final ForgeConfigSpec.IntValue fadeOutV;
    private final ForgeConfigSpec.BooleanValue panelEnabledV;
    private final ForgeConfigSpec.ConfigValue<String> panelColorV;
    private final ForgeConfigSpec.IntValue panelOpacityV;
    private final ForgeConfigSpec.EnumValue<PanelBorderStyle> borderStyleV;
    private final ForgeConfigSpec.ConfigValue<String> borderColorV;
    private final ForgeConfigSpec.IntValue borderOpacityV;
    private final ForgeConfigSpec.IntValue borderLengthV;
    private final ForgeConfigSpec.IntValue borderOutsetV;
    private final ForgeConfigSpec.IntValue borderScaleV;
    private final Defaults d;
    private final int opacityDefault;

    /** Overall opacity of the whole element, 0-100. */
    public int opacity = 100;
    public int fadeInMs;
    public int fadeOutMs;
    public boolean panelEnabled;
    public int panelColor;
    public int panelOpacity;
    public PanelBorderStyle borderStyle;
    public int borderColor = 0xE8ECF0;
    public int borderOpacity = 90;
    public int borderLength;
    public int borderOutset;
    /** Dot / corner size multiplier in percent (100 = as drawn by borderLength). */
    public int borderScale = 100;

    /**
     * @param section   sub-section name inside the builder's current section (e.g. "style")
     * @param keyPrefix translation-key prefix, e.g. "boss" -> "modernhud.config.boss_style_opacity"
     */
    public ElementStyle(ForgeConfigSpec.Builder b, String section, String keyPrefix, Defaults d) {
        this(b, section, keyPrefix, d, 100);
    }

    /** Same, with a custom default for the element's overall opacity (0-100). */
    public ElementStyle(ForgeConfigSpec.Builder b, String section, String keyPrefix, Defaults d, int opacityDefault) {
        this.d = d;
        this.opacityDefault = opacityDefault;
        String t = "modernhud.config." + keyPrefix + "_style_";
        b.comment("Look of this element: opacity, fade times, panel and border.").push(section);

        opacityV = b.comment("Opacity of the whole element, 0 to 100.")
                .translation(t + "opacity").defineInRange("opacity", opacityDefault, 0, 100);
        fadeInV = b.comment("Fade-in time in milliseconds. 0 is instant.")
                .translation(t + "fade_in_ms").defineInRange("fadeInMs", d.fadeInMs(), 0, 5000);
        fadeOutV = b.comment("Fade-out time in milliseconds. 0 is instant.")
                .translation(t + "fade_out_ms").defineInRange("fadeOutMs", d.fadeOutMs(), 0, 5000);
        panelEnabledV = b.comment("Draw a flat background panel behind the element.")
                .translation(t + "panel").define("panelEnabled", d.panelEnabled());
        panelColorV = b.comment("Panel color as a hex code, for example D9DEE3.")
                .translation(t + "panel_color").define("panelColor", ColorConfig.toHex(d.panelColor()));
        panelOpacityV = b.comment("Panel opacity, 0 to 100.")
                .translation(t + "panel_opacity").defineInRange("panelOpacity", d.panelOpacity(), 0, 100);
        borderStyleV = b.comment("Border style: NONE, CORNERS (short L-shaped brackets), FULL (thin outline),",
                        "DOTS (one dot per corner), BOTTOM_DOTS (bottom corners only) or FRAME (outline with corner dots).")
                .translation(t + "border_style").defineEnum("borderStyle", d.borderStyle());
        borderColorV = b.comment("Border color as a hex code, for example E8ECF0.")
                .translation(t + "border_color").define("borderColor", ColorConfig.toHex(0xE8ECF0));
        borderOpacityV = b.comment("Border opacity, 0 to 100.")
                .translation(t + "border_opacity").defineInRange("borderOpacity", 90, 0, 100);
        borderLengthV = b.comment("Bracket arm length for CORNERS, or dot size plus 1 for DOTS and FRAME, in pixels.")
                .translation(t + "border_length").defineInRange("borderLength", d.borderLength(), 1, 20);
        borderScaleV = b.comment("Scale of the corner dots and corner brackets in percent. 100 is the size set by borderLength,",
                        "50 is half, 200 is double. Works for CORNERS, DOTS, BOTTOM_DOTS and FRAME.")
                .translation(t + "border_scale").defineInRange("borderScale", 100, 25, 800);
        borderOutsetV = b.comment("How far brackets and dots sit outside the element's edge, in pixels.")
                .translation(t + "border_outset").defineInRange("borderOutset", d.borderOutset(), 0, 20);

        b.pop();
        refresh();
    }

    /** Re-reads the live values; call from the config's load handler. Safe to call before the config loads. */
    public void refresh() {
        ColorConfig.migrateDarkPanel(panelColorV, panelOpacityV, d.panelColor(), d.panelOpacity());
        try {
            opacity = opacityV.get();
            fadeInMs = fadeInV.get();
            fadeOutMs = fadeOutV.get();
            panelEnabled = panelEnabledV.get();
            panelColor = ColorConfig.fromHex(panelColorV.get(), d.panelColor());
            panelOpacity = panelOpacityV.get();
            borderStyle = borderStyleV.get();
            borderColor = ColorConfig.fromHex(borderColorV.get(), 0xE8ECF0);
            borderOpacity = borderOpacityV.get();
            borderLength = borderLengthV.get();
            borderOutset = borderOutsetV.get();
            borderScale = borderScaleV.get();
        } catch (RuntimeException notLoadedYet) { // NPE before the spec is built, IllegalStateException before it is loaded
            // config not loaded yet: fall back to the declared defaults
            opacity = opacityDefault;
            fadeInMs = d.fadeInMs();
            fadeOutMs = d.fadeOutMs();
            panelEnabled = d.panelEnabled();
            panelColor = d.panelColor();
            panelOpacity = d.panelOpacity();
            borderStyle = d.borderStyle();
            borderColor = 0xE8ECF0;
            borderOpacity = 90;
            borderLength = d.borderLength();
            borderOutset = d.borderOutset();
            borderScale = 100;
        }
    }

    /** {@code alpha} (0-1) scaled by this element's overall opacity. */
    public float alpha(float alpha) {
        return alpha * (opacity / 100f);
    }
}

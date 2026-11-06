package cc.xypp.modernhud.config;

/**
 * Every colour setting is stored as a hex STRING ("0A0D11" or "#0A0D11", either is accepted on
 * read) rather than Forge's default decimal-integer input — a raw {@code IntValue} config shows up
 * in the config screen as a huge unreadable decimal number like "660425", which nobody can look at
 * and know is a dark red without doing the hex math themselves. A text field you can type/paste an
 * actual hex code into is what every other tool that lets you pick a colour uses.
 */
public final class ColorConfig {

    private ColorConfig() {
    }

    /** Formats an 0xRRGGBB int as the default value string to hand to {@code Builder#define}. */
    public static String toHex(int rgb) {
        return String.format("%06X", rgb & 0xFFFFFF);
    }

    /**
     * Parses a hex-string colour value, tolerating a leading "#" or "0x"/"0X" and mixed case.
     * Falls back to {@code fallbackRgb} — silently, no log spam — for anything that isn't 1-6 valid
     * hex digits, since this runs on every config load/reload and a typo shouldn't crash the mod.
     */
    public static int fromHex(String value, int fallbackRgb) {
        if (value == null) {
            return fallbackRgb;
        }
        String s = value.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        } else if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        if (s.isEmpty() || s.length() > 6 || !s.matches("[0-9a-fA-F]+")) {
            return fallbackRgb;
        }
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallbackRgb;
        }
    }

    /**
     * Older builds defaulted every panel to a near-black "0A0D11"; the HUD now uses a light white-grey. A stored
     * file keeps whatever it was generated with, so when the stored colour is STILL exactly the old default (i.e.
     * never customised) it is moved to the new colour and opacity; anything the user changed is left alone.
     * Safe to call before the config is loaded (does nothing then).
     */
    private static volatile boolean MIGRATING;

    public static void migrateDarkPanel(net.minecraftforge.common.ForgeConfigSpec.ConfigValue<String> color,
                                        net.minecraftforge.common.ForgeConfigSpec.IntValue opacity,
                                        int newRgb, int newOpacity) {
        if (MIGRATING) {
            return; // set() can re-trigger a config event: never recurse
        }
        MIGRATING = true;
        try {
            if ("0A0D11".equalsIgnoreCase(color.get().replace("#", "").trim())) {
                color.set(toHex(newRgb));
                opacity.set(newOpacity);
                color.save();
            }
        } catch (RuntimeException notLoadedYet) {
            // spec not built / config not loaded yet - nothing to migrate
        } finally {
            MIGRATING = false;
        }
    }
}

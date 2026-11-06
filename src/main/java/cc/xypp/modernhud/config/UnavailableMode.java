package cc.xypp.modernhud.config;

/** What to show when a value cannot be read (for example no GPU sensor was found). */
public enum UnavailableMode {
    /** Show the item with {@code N/A}. */
    SHOW_NA,
    /** Hide the item until a value is available. */
    HIDE
}

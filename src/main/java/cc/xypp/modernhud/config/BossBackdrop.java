package cc.xypp.modernhud.config;

/** What sits behind each INDIVIDUAL compact boss bar (one per bar, never one shared box). */
public enum BossBackdrop {
    NONE,
    /** A soft, feathered dark drop-off behind the bar - no hard edge. */
    SHADOW,
    /** A panel box in the weapon widget's style (fill + corner brackets). */
    BOX
}

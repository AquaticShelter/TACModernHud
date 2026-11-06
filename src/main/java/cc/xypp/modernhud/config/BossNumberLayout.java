package cc.xypp.modernhud.config;

/** Where each compact boss bar's number goes relative to its bar. */
public enum BossNumberLayout {
    /** Number to the left of every bar. */
    SIDE,
    /** The Modern Warfare match-progress layout: the FIRST bar's number sits above its bar and the LAST bar's number
     *  below its bar (a single bar: above; three or more bars: SIDE for everything, since there is no clean
     *  above/below for the middle ones). */
    ABOVE_BELOW,
    /** Every number above its bar. */
    ABOVE,
    /** Every number below its bar. */
    BELOW
}

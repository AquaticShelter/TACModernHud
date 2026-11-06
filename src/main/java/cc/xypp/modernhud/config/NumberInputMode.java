package cc.xypp.modernhud.config;

/** How a number option is edited in the config screen. */
public enum NumberInputMode {
    /** A box you click and type the exact number into (with the option's minimum and maximum enforced). */
    FIELD,
    /** A slider you drag (only for options with a bounded range; the rest are always boxes). */
    SLIDER
}

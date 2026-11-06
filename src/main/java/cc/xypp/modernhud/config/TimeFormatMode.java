package cc.xypp.modernhud.config;

/** Clock format of the spec readout. */
public enum TimeFormatMode {
    /** 12-hour clock with the AM / PM marker of the chosen language, for example {@code 12:27 AM}. */
    H12,
    /** 24-hour clock, for example {@code 00:27}. */
    H24,
    /** Whatever the system language prefers. */
    AUTO
}

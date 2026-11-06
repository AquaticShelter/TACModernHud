package cc.xypp.modernhud.client;

/** Crosshair marker flashed on landing a hit - see {@link HitMarkerEntry}/{@link HitIndicatorOverlay}.
 *  Network wire format is the enum's {@code ordinal()}, so the declaration order here must stay
 *  CRIT, KILL to match {@link #fromWire}. */
public enum HitMarkerType {
    /** A vanilla critical hit landed. */
    CRIT,
    /** The hit killed its target. */
    KILL;

    public static HitMarkerType fromWire(int ordinal) {
        HitMarkerType[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CRIT;
    }
}

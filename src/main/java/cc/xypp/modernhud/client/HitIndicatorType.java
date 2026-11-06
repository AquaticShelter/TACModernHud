package cc.xypp.modernhud.client;

/** What kind of hit a {@link HitIndicatorEntry} represents - decides which texture/colour it draws
 *  with (see {@link HitIndicatorOverlay}). Network wire format is the enum's {@code ordinal()},
 *  so the declaration order here must stay HIT, BLOCK, ND_HIT to match {@link #fromWire}. */
public enum HitIndicatorType {
    /** A normal directional hit - points back toward the attacker. */
    HIT,
    /** You blocked the hit with a shield. */
    BLOCK,
    /** Damage with no locatable source (fall, fire, starvation, etc) - non-directional. */
    ND_HIT;

    public static HitIndicatorType fromWire(int ordinal) {
        HitIndicatorType[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : HIT;
    }
}

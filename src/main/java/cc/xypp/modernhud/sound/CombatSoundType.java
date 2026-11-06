package cc.xypp.modernhud.sound;

/** The cues the server can ask a client to play. */
public enum CombatSoundType {
    /** A normal (non-fatal) hit with no shield involved. */
    HIT,
    /** A kill. */
    KILL,
    /** A kill by a headshot (only TACZ can report headshots). */
    HEADSHOT_KILL
}

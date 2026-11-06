package cc.xypp.modernhud.client.motion;

/**
 * The HUD elements that can sway with the camera. The order of the constants must match the weight table filled by
 * {@code MotionConfig#refresh} (the index is {@link #ordinal()}).
 */
public enum MotionLayer {
    /** Never moves (used by code that has no layer). */
    NONE,
    PLAYER,
    TEAM,
    WEAPON,
    AMMO_ALERT,
    KILLSTREAK,
    PERK_TOAST,
    COMPASS,
    BOSS_BAR,
    SPECS,
    FEEDBACK,
    HIT_INDICATOR
}

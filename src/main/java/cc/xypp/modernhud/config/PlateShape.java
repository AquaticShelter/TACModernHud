package cc.xypp.modernhud.config;

/** Outline of one absorption (armor plate) cell in the player and team HUD. */
public enum PlateShape {
    /**
     * The armor plate of Warzone (the blue plate of the reference): a flat bar with big rounded TOP corners and a
     * shallow cut-out along the bottom, which leaves a short "foot" under each end. Needs at least 8 units of height.
     */
    WARZONE,
    /** The old straight rectangle (pixel rows, no shape). */
    BAR,
    /** The armor plate of the reference: small rounded left corners, a big round right end, soft shading. */
    PLATE,
    /** A capsule: both ends fully round. */
    PILL,
    /** A plate with cut corners. */
    CHAMFER,
    /** A hexagon pointing left and right. */
    HEX,
    /** A parallelogram leaning right, like the segmented bars of Modern Warfare (2019). */
    SLANT
}

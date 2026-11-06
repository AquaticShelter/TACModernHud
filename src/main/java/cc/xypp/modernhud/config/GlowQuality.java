package cc.xypp.modernhud.config;

/** Smoothness of every soft glow: radial steps and arc segments (more = smoother, a bit more GPU work). */
public enum GlowQuality {
    LOW(3, 3), MEDIUM(5, 5), HIGH(8, 8);

    private final int rings;
    private final int segs;

    GlowQuality(int rings, int segs) {
        this.rings = rings;
        this.segs = segs;
    }

    public int rings() {
        return rings;
    }

    /** Segments per quarter circle. */
    public int segs() {
        return segs;
    }
}

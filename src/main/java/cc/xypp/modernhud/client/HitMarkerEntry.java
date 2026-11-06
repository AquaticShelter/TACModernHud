package cc.xypp.modernhud.client;

/** One active crosshair crit/kill marker flash - fixed 9-tick lifetime, used both to pick the
 *  right animation frame and to fade it out (see {@link HitIndicatorOverlay#getMarkerTexture}). */
public class HitMarkerEntry {

    private final HitMarkerType type;
    private int lifetime;

    public HitMarkerEntry(HitMarkerType type) {
        this.type = type;
        this.lifetime = 9;
    }

    public void tick() {
        lifetime--;
    }

    public boolean expired() {
        return lifetime <= 0;
    }

    public int getLifetime() {
        return lifetime;
    }

    public HitMarkerType getType() {
        return type;
    }
}

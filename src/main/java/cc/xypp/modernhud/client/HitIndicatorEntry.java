package cc.xypp.modernhud.client;

import cc.xypp.modernhud.config.HitIndicationConfig;
import net.minecraft.Util;
import net.minecraft.world.phys.Vec3;

/** One active damage-direction arc: where the damage came from (world space) and when it was (last) hit.
 *  Timing is in real milliseconds, not ticks, so the fade is smooth at any frame rate. */
public class HitIndicatorEntry {

    private Vec3 sourceLocation;
    private boolean shielded;
    private long createdMs;

    public HitIndicatorEntry(double x, double y, double z, boolean shielded) {
        this.sourceLocation = new Vec3(x, y, z);
        this.shielded = shielded;
        this.createdMs = Util.getMillis();
    }

    /** A new hit from the same direction: re-flash this arc instead of adding another. */
    public void refresh(double x, double y, double z, boolean shielded) {
        this.sourceLocation = new Vec3(x, y, z);
        this.shielded = shielded;
        this.createdMs = Util.getMillis();
    }

    public long ageMs() {
        return Util.getMillis() - createdMs;
    }

    public boolean expired() {
        return ageMs() >= HitIndicationConfig.fadeInMs + HitIndicationConfig.holdMs + HitIndicationConfig.fadeOutMs;
    }

    /** 0-1 strength now: quick ease-out flash in, hold, smooth fade out. */
    public float strength() {
        long age = ageMs();
        int in = HitIndicationConfig.fadeInMs;
        int hold = HitIndicationConfig.holdMs;
        int out = Math.max(1, HitIndicationConfig.fadeOutMs);
        if (age < in) {
            float t = in <= 0 ? 1f : age / (float) in;
            return 1f - (1f - t) * (1f - t);
        }
        if (age < in + hold) {
            return 1f;
        }
        float t = Math.min(1f, (age - in - hold) / (float) out);
        float s = t * t * (3f - 2f * t);
        return 1f - s;
    }

    /** 0-1 progress of the flash-in (drives the little "pop" of the arc settling onto its radius). */
    public float popProgress() {
        int in = Math.max(1, HitIndicationConfig.fadeInMs);
        return Math.min(1f, ageMs() / (float) (in * 2));
    }

    public Vec3 getLocation() {
        return sourceLocation;
    }

    public boolean isShielded() {
        return shielded;
    }
}

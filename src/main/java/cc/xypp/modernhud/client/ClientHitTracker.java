package cc.xypp.modernhud.client;

import cc.xypp.modernhud.config.HitIndicationConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.ArrayList;
import java.util.List;

/** Client-side holding area for active hit indicators + the current crosshair marker - populated
 *  by the two network packets (see {@code cc.xypp.modernhud.network}), ticked by
 *  {@link ClientHitIndicatorEvents}, drawn by {@link HitIndicatorOverlay}, and cleared on leaving
 *  a world (same class). All config gating (enable toggles, max count, block/ND filters) happens
 *  here at insert time, not at draw time, so a disabled indicator never even enters the list. */
public class ClientHitTracker {

    public static final List<HitIndicatorEntry> activeIndicators = new ArrayList<>();
    public static HitMarkerEntry activeMarker = null;

    private ClientHitTracker() {
    }

    public static void addIndicator(double x, double y, double z, HitIndicatorType type,
                                     int damagePercent, boolean fromNegativePotion, boolean shielded) {
        if (!HitIndicationConfig.enableHitIndication) {
            return;
        }
        // PvP only: shield blocks, non-directional damage (fall / fire / ...) and potion hits are ignored
        if (type != HitIndicatorType.HIT || fromNegativePotion) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (HitIndicationConfig.mergeSameDirection && player != null) {
            double bearing = bearing(player.getX(), player.getZ(), x, z);
            for (HitIndicatorEntry e : activeIndicators) {
                double other = bearing(player.getX(), player.getZ(), e.getLocation().x, e.getLocation().z);
                double diff = Math.abs(((bearing - other) % 360.0 + 540.0) % 360.0 - 180.0);
                if (diff <= 12.0) {
                    e.refresh(x, y, z, shielded);
                    return;
                }
            }
        }
        activeIndicators.add(new HitIndicatorEntry(x, y, z, shielded));

        int max = HitIndicationConfig.maxIndicatorCount;
        if (max > 0 && activeIndicators.size() > max) {
            activeIndicators.remove(0);
        }
    }

    private static double bearing(double fromX, double fromZ, double toX, double toZ) {
        return Math.toDegrees(Math.atan2(toX - fromX, toZ - fromZ));
    }

    public static void setMarker(HitMarkerType type) {
        if (!HitIndicationConfig.enableHitMarkers) {
            return;
        }
        activeMarker = new HitMarkerEntry(type);
    }

    public static void tick() {
        for (int i = activeIndicators.size() - 1; i >= 0; i--) {
            HitIndicatorEntry entry = activeIndicators.get(i);
            if (entry.expired()) {
                activeIndicators.remove(i);
            }
        }
        if (activeMarker != null) {
            activeMarker.tick();
            if (activeMarker.expired()) {
                activeMarker = null;
            }
        }
    }

    public static void clear() {
        activeIndicators.clear();
        activeMarker = null;
    }
}

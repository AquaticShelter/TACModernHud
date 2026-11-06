package cc.xypp.modernhud.client.compat;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

/**
 * Soft/reflective compat with "[SBW] Superb Warfare" (github.com/Mercurows/SuperbWarfare, modid
 * {@code superbwarfare}) — a gun/vehicle mod whose tanks, helicopters, boats, artillery, etc. all
 * ultimately extend one common base class,
 * {@code com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity} (confirmed directly against
 * SBW's own source — every concrete vehicle, and every intermediate base like
 * {@code GeoVehicleEntity}/{@code ArtilleryEntity}/{@code AutoAimableEntity}, chains back to it), so
 * that one class is all this needs to check for.
 *
 * <p>Reflection rather than a {@code compileOnly} Maven dependency (contrast {@link TaczCompat}, which
 * compiles directly against TACZ's API): the only thing needed here is a single
 * {@code instanceof}-style check, not a real API surface, so pulling in a whole extra compile
 * dependency for it isn't worth it — same reasoning as {@code TaczPresenceCompat}. Class-loading is
 * safe on an SBW-free install: like the rest of this package, this is only ever invoked after
 * {@link #loaded()} has been checked, and {@link #loaded()} itself never touches an
 * {@code com.atsuishio.*} type.
 */
public final class SuperbWarfareCompat {

    private static final boolean LOADED = ModList.get().isLoaded("superbwarfare");

    private static Class<?> vehicleEntityClass;
    private static boolean lookupAttempted;

    private SuperbWarfareCompat() {
    }

    public static boolean loaded() {
        return LOADED;
    }

    /**
     * True if {@code entity} is currently riding an SBW vehicle — any seat (driver, gunner,
     * passenger alike), not just the one actually steering. Used to hide ModernHUD's own widgets
     * so they don't overlap SBW's own vehicle/weapon HUD, which covers roughly the same screen
     * area for every seat, not just the driver's.
     */
    public static boolean isInVehicle(Entity entity) {
        if (!LOADED) {
            return false;
        }
        Entity vehicle = entity.getVehicle();
        if (vehicle == null) {
            return false;
        }
        Class<?> clazz = vehicleEntityClass();
        return clazz != null && clazz.isInstance(vehicle);
    }

    private static Class<?> vehicleEntityClass() {
        if (!lookupAttempted) {
            lookupAttempted = true;
            try {
                vehicleEntityClass = Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity");
            } catch (Throwable ignored) {
                // SBW's internal package changed shape, or isn't present under this name in the
                // installed version - harmless no-op, ModernHUD's widgets just won't hide for it,
                // same fallback posture as every other soft dependency in this mod.
                vehicleEntityClass = null;
            }
        }
        return vehicleEntityClass;
    }
}

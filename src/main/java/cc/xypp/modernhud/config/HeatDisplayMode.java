package cc.xypp.modernhud.config;

/**
 * How (if at all) THIS widget shows a gun's overheat progress for guns using TACZ's own opt-in heat
 * mechanic (see {@code TaczCompat#hasHeat}) — most guns don't have this at all, so none of these
 * modes show anything for them regardless of which is picked.
 */
public enum HeatDisplayMode {
    /** No heat indicator from this widget at all. The default — TACZ's own native heat display
     *  (if the installed TACZ version draws one) can still be used instead by switching
     *  {@code hudSource} to TACZ_HUD/TACZ_P_HUD, which hands the WHOLE weapon HUD - heat readout
     *  included - back to TACZ/TaczPresence rather than this widget. There's no way to show TACZ's
     *  native heat element while still using this widget for everything else: TACZ gates its icon,
     *  ammo readout, and heat display behind one single toggle, not separate ones per-element (see
     *  {@code TaczCompat}'s class javadoc). */
    NONE,
    /** A thin bar, filled from the right edge growing leftward (see WeaponConfig's frame comments). */
    BAR,
    /** The primary weapon icon box glows/pulses instead of a separate bar, fading from
     *  {@code heatFlashColor} (cold) to {@code heatBarLockedColor} (hot/locked) as heat rises, and
     *  pulsing faster the hotter the gun gets. */
    FLASH
}

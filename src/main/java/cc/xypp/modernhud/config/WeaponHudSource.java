package cc.xypp.modernhud.config;

/**
 * Which weapon HUD should actually be on screen when TACZ (and possibly TaczPresence) are
 * installed. Mirrors TaczPresence's own {@code PresenceConfig.HudType} selector
 * ({@code TACZ_HUD} / {@code TACZ_P_HUD}) with a third option added for this mod's own widget —
 * we can't literally add a value to TaczPresence's own enum (it's their compiled mod, not ours),
 * so this is ModernHUD's side of the same idea: whichever one is chosen here, ModernHUD makes sure
 * only that one actually renders (see {@code WeaponConfig}'s sync logic and
 * {@code TaczCompat#setVanillaGunHudEnabled}/{@code #setPresenceHudType}).
 */
public enum WeaponHudSource {
    /** TACZ's own built-in gun HUD. */
    TACZ_HUD,
    /** TaczPresence's HUD, if that mod is installed (falls back to TACZ_HUD if it isn't). */
    TACZ_P_HUD,
    /** This mod's own weapon widget (the default). */
    MODERNHUD
}

package cc.xypp.modernhud.client;

import cc.xypp.modernhud.ModernHud;
import cc.xypp.modernhud.client.motion.MotionLayer;
import cc.xypp.modernhud.config.HitIndicationConfig;
import cc.xypp.modernhud.config.WeaponConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Registers the client overlays:
 *  - the personal HUD widget (head/name/absorption bar/health bar/armor bar)
 *  - the team HUD (vanilla-scoreboard-team teammate health list) - registered right after the
 *    personal widget so it renders after it every frame, since it reads that widget's
 *    last-computed screen position/size to stack itself directly above it
 *  - the hit/break feedback icon
 *  - the directional hit indicator (crosshair-relative hit markers pointing at your attacker) -
 *    integrated from a standalone "Hit Indication" mod; detection lives server-side in
 *    HitIndicationEvents, same reasoning as the feedback icon above
 *  - the TACZ weapon widget (soft dependency - see WeaponHudOverlay/TaczCompat)
 *  - the TACZ ammo alert (RELOAD / NO AMMO / LOW AMMO), same soft dependency
 *  - the killstreak / Specialist tracker (soft dependency on ModernLoadout), stacked above the weapon widget
 *  - the selected-perks toast (soft dependency on ModernLoadout)
 *  - the compact corner boss bars (replace vanilla's wide top-centre ones)
 *
 * The actual "did this hit land on/break Absorption" detection lives
 * server-side in CommonEvents, since it needs to work for every entity,
 * not just you, and needs to know who the attacker was.
 *
 * <p>The weapon HUD, ammo alert, and hit indicator instances are kept in static fields (rather
 * than the fire-and-forget {@code new XyzOverlay()} the other overlays use) and exposed via the
 * package-private accessors below so that {@link IgnoreHideGuiRenderer} can re-invoke the SAME
 * instances — with all their internal state (switch-glow timers, active indicators, etc) intact —
 * when F1 is hiding the GUI and the person has asked for these specific overlays to ignore that
 * (see {@link WeaponConfig#ignoreHideGui}/{@link HitIndicationConfig#ignoreHideGui}). A second,
 * fresh instance would restart all of that state every single frame instead of animating normally.
 */
@Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    private static final WeaponHudOverlay WEAPON_HUD = new WeaponHudOverlay();
    private static final AmmoAlertOverlay AMMO_ALERT = new AmmoAlertOverlay();
    private static final HitIndicatorOverlay HIT_INDICATOR = new HitIndicatorOverlay();

    /** See {@link IgnoreHideGuiRenderer} - the same instance registered below, so its internal
     *  animation/switch-timer state carries over whether it's drawn from the normal pipeline or
     *  from the F1-bypass path. */
    static WeaponHudOverlay weaponHud() {
        return WEAPON_HUD;
    }

    static AmmoAlertOverlay ammoAlert() {
        return AMMO_ALERT;
    }

    static HitIndicatorOverlay hitIndicator() {
        return HIT_INDICATOR;
    }

    @SubscribeEvent
    public static void onRegisterOverlays(final RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("player_hud", HudScale.wrap(new PlayerHudOverlay(), MotionLayer.PLAYER));
        event.registerAboveAll("team_hud", HudScale.wrap(new TeamHudOverlay(), MotionLayer.TEAM));
        event.registerAboveAll("feedback_icon", HudScale.wrap(new FeedbackIconOverlay(), MotionLayer.FEEDBACK));
        event.registerAboveAll("hit_indicator", HudScale.wrap(HIT_INDICATOR, MotionLayer.HIT_INDICATOR));
        // Safe to always register: render() itself no-ops instantly (TaczCompat.loaded() check)
        // on a TACZ-free install, and its overlay signature references no TACZ types.
        event.registerAboveAll("weapon_hud", HudScale.wrap(WEAPON_HUD, MotionLayer.WEAPON));
        event.registerAboveAll("ammo_alert", HudScale.wrap(AMMO_ALERT, MotionLayer.AMMO_ALERT));
        // After weapon_hud on purpose: it stacks above the weapon widget. Draws nothing (and never touches
        // the ModernLoadout API) unless ModernLoadout is installed - see ModernLoadoutCompat.
        event.registerAboveAll("killstreak_hud", HudScale.wrap(new KillstreakHudOverlay(), MotionLayer.KILLSTREAK));
        // Selected-perks toast: needs ModernLoadout (draws nothing without it).
        event.registerAboveAll("perk_toast", HudScale.wrap(new PerkToastOverlay(), MotionLayer.PERK_TOAST));
        // Draws the compact corner boss bars (vanilla's own are cancelled in BossBarOverlay.Events).
        event.registerAboveAll("boss_bar", HudScale.wrap(new BossBarOverlay(), MotionLayer.BOSS_BAR));
        event.registerAboveAll("compass", HudScale.wrap(new cc.xypp.modernhud.compass.CompassOverlay(), MotionLayer.COMPASS));
        // FPS / ping / packet loss / GPU temperature / time readout (see client.spec)
        event.registerAboveAll("spec_readout", HudScale.wrap(new cc.xypp.modernhud.client.spec.SpecReadoutOverlay(), MotionLayer.SPECS));
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(WeaponConfig::syncHudSource);
    }

    /** Resource reload (pack / language change): the compass caches text widths, drop them. */
    @SubscribeEvent
    public static void onRegisterReloadListeners(final net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm ->
                cc.xypp.modernhud.compass.CompassOverlay.invalidateTextMetrics());
    }
}

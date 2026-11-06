package cc.xypp.modernhud.client;

import cc.xypp.modernhud.ModernHud;
import cc.xypp.modernhud.client.compat.TaczCompat;
import cc.xypp.modernhud.client.perf.HudProfiler;
import cc.xypp.modernhud.client.compat.SuperbWarfareCompat;
import cc.xypp.modernhud.config.HudConfig;
import cc.xypp.modernhud.config.WeaponConfig;
import cc.xypp.modernhud.config.WeaponHudSource;
import cc.xypp.modernhud.config.HeatDisplayMode;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * TACZ weapon widget, Call-of-Duty style (Modern Warfare's bottom-right ammo counter): mag and
 * reserve numbers side by side ("051 | 108" — reserve rendered smaller and slightly dimmer than
 * the mag number, each with its own leading-zero digit(s) rendered faint/dim relative to its own
 * significant digits when {@link WeaponConfig#zeroPadAmmo} is on — see {@link #drawZeroPaddedNumber}),
 * plus a small "ammo type" icon next to reserve (see {@link WeaponConfig#showAmmoTypeIcon}) — its
 * own independent element (see {@link #drawAmmoRow}), background a soft GLOW rather than a flat
 * shadowbox, with corner brackets sitting OUTSIDE its edge (see {@link WeaponConfig#borderOutset}).
 * The weapon's name sits directly underneath as its own plain floating text with NO background of
 * its own (see {@link #drawNameRow}) — the ammo row's glow does not extend down over it. Fire mode
 * is its own segmented control below that (see {@link #drawFireModeSegmented}): one shared box
 * divided into equal-width cells, one per supported mode, with the ACTIVE mode's cell getting an
 * extra highlight border on top of the shared box's own — all per close comparison against the
 * reference HUD, where these three are visually independent, not one shared box.
 *
 * <p>The gun's own TACZ HUD icon sits on the right in its own separate box, with a row of small
 * attachment-slot squares directly above it (see {@link #drawAttachmentDots},
 * {@link WeaponConfig#showAttachmentDots} — one square per slot this SPECIFIC gun actually has,
 * filled gold for equipped, dim for empty), and a small, dim secondary-weapon box tucked BELOW the
 * icon (same width as the primary box, icon right-aligned inside it, reserve ammo count
 * left-aligned) for the last different gun you were holding (Call of Duty's primary/secondary
 * weapon slots) — or, while carrying but not holding any gun, for up to two guns found in the
 * hotbar (see {@link #renderNeutral}). The secondary/neutral box's own accent is deliberately more
 * minimal than the primary's: two small dots at its BOTTOM corners only, nothing at the top (see
 * {@link HudPanel#drawBottomDots}, {@link WeaponConfig#secondaryDotSize}) — matching the reference
 * HUD's less prominent treatment for a slot that isn't the one actively being fired.
 *
 * <p>No gradient/fade on the fills, just a flat colour (or, for the ammo row specifically, a soft
 * glow) at an adjustable opacity, plus an optional edge accent on top — none, small corner
 * brackets (the default, modelled on the referenced MW4 HUD), or a full thin outline — see
 * {@link WeaponConfig#borderStyle}. The icon box(es) and fire-mode box keep a dark default fill
 * ({@link WeaponConfig#panelColor}). The ammo row has no fill of its own: it gets the border plus the light
 * glow from {@link #drawAmmoGlow}.
 *
 * <p>The "push up into place" switch animation applies ONLY to the icon box(es) (and the
 * attachment dots/secondary box that move with it) — the ammo/name/fire-mode elements are drawn in
 * a separate, un-translated pose push, so they never slide around on a weapon swap, only the icon
 * graphic does. Independently toggle-able
 * ({@link WeaponConfig#showSwitchSlide}) — the slide itself now eases with a quintic ease-out curve
 * (see {@link EasedAnimator}) for a snappier, tighter settle than this used to have. The same
 * switch event ALSO triggers, independently of each other and of the slide: a brief bright glow
 * flash behind the primary icon (see {@link GlowEffect}, {@link WeaponConfig#showWeaponGlow}) that
 * outlasts the slide slightly so the icon visibly "settles" rather than the animation just cutting
 * off, and a gold border flash around the primary icon's own border (see
 * {@link WeaponConfig#showSwitchBorderFlash}) modelled on vanilla Minecraft's own gold-flash accents
 * (a totem of undying's screen flash, for instance), fading back to the normal border colour. The
 * combined ammo chip separately gets a slow breathing glow pulse (same colour as the low-ammo mag
 * number) while critically low on ammo. Separately, a centre-screen "FIRE TYPE: FULL AUTO"
 * announcement (see {@link #drawFireTypeFlash}) flashes above the crosshair on every weapon switch
 * AND every time you cycle fire mode on the gun you're already holding, independent of the widget's
 * own screen position - matching Modern Warfare's own weapon-pull announcement.
 *
 * <p>Holding something other than a TACZ gun doesn't hide the widget outright: as long as at least
 * one gun is still sitting somewhere in the hotbar, up to two of them (ordered by hotbar slot, not
 * by switch history) keep showing as a pair of small, dim, equally-unhighlighted icons with their
 * reserve ammo counts — see {@link #renderNeutral} — with no name/mag readout, since neither is
 * actually being held/fired right now. Switch-history ({@link #currentGunId}/{@link #secondaryGunId})
 * is left untouched while in this state, so picking a real gun back up resumes primary/secondary
 * tracking right where it left off.
 *
 * <p><b>Layout is FIXED</b> — the panel is one constant size (see {@link WeaponConfig}), never
 * resized by the current gun's name length or icon art, and its screen position is config-file-only
 * (no in-game drag handle — see {@code HudEditorScreen}'s class javadoc for why). TACZ HUD-icon
 * textures and gun-name lengths vary wildly gun to gun, so sizing the frame off either one made the
 * whole widget balloon, overlap the crosshair, and run off the edge of the screen for long names.
 *
 * <p>Registered only when TACZ is loaded (see {@code ClientEvents}), and TACZ's own built-in gun
 * HUD is switched off in favor of this one (see {@link TaczCompat#setVanillaGunHudEnabled}) so the
 * two never render on top of each other. Even then this renders nothing unless the held item is
 * actually a gun.
 */
public class WeaponHudOverlay implements IGuiOverlay {

    private static final ResourceLocation FIRE_ICON_SEMI = new ResourceLocation(ModernHud.MOD_ID, "textures/hud/fire_mode_semi.png");
    private static final ResourceLocation FIRE_ICON_AUTO = new ResourceLocation(ModernHud.MOD_ID, "textures/hud/fire_mode_auto.png");
    private static final ResourceLocation FIRE_ICON_BURST = new ResourceLocation(ModernHud.MOD_ID, "textures/hud/fire_mode_burst.png");

    private static final int PAD = 6;
    /** Package-visible: {@link KillstreakHudOverlay} sizes its slots off the same box and stacks above this widget. */
    static final int ICON_BOX_W = 64;
    static final int ICON_BOX_H = 32;
    /** Gap between the primary icon box and the secondary box directly below it - tightened from
     *  the previous 4px per feedback that the widget's spacing read as too loose. */
    static final int SECONDARY_GAP = 2;
    static final int SECONDARY_BOX_H = 16;
    /** Height of the icon box + secondary strip stack: what frameHeight centres. */
    static final int CONTENT_H = ICON_BOX_H + SECONDARY_GAP + SECONDARY_BOX_H;
    /** Fire mode icon area: wider than tall (bullets are long), and each cell has padding around it. */
    private static final int FIRE_ICON_SIZE = 7;
    private static final int FIRE_ICON_W = 14;
    private static final int FIRE_CELL_W = 21;
    private static final int FIRE_CELL_H = 10;

    /** Scale of everything inside the info box (config: infoBox.contentScale). */
    private static float contentScale() {
        return WeaponConfig.infoContentScale / 100f;
    }

    private static int fireCellW() {
        return Math.max(10, Math.round(FIRE_CELL_W * contentScale()));
    }

    private static int fireCellH() {
        return Math.max(6, Math.round(FIRE_CELL_H * contentScale()));
    }
    /** The fire mode control sits this far in from the info box's right border (the reference keeps it clear of the corner brackets). */
    private static final int FIRE_RIGHT_INSET = 10;
    /** Fixed gap between the ammo row and the gun name. */
    /** Inner top / bottom padding of the info box. */
    private static final int INFO_PAD_Y = 3;
    /** Pixels the mag/reserve digits are lifted above their original position inside the ammo box. */
    private static final int AMMO_TEXT_RAISE = 3;
    /** Extra size on the reserve ammo icon (+5%) on top of ammoTypeIconScale. */
    private static final float RESERVE_ICON_BOOST = 1.05f;
    /** Real full size of the fire-mode icon source PNGs (see {@link #FIRE_ICON_AUTO} etc.) - unlike
     *  {@link #FIRE_ICON_SIZE}, this is the texture's TRUE pixel dimensions, needed for a correct UV
     *  crop (see {@link #drawAmmoTypeIcon}), not the small size those icons happen to be drawn at
     *  elsewhere on this widget. */
    private static final int FIRE_ICON_TEX_SIZE = 128;
    /** Tight crop (in real texture pixels) of the actual icon content within
     *  {@link #FIRE_ICON_AUTO}'s padded {@link #FIRE_ICON_TEX_SIZE}x{@code FIRE_ICON_TEX_SIZE}
     *  canvas — measured directly against the source PNG's alpha channel, plus a 1px safety margin.
     *  Sampling only this region (rather than the whole padded canvas, as the fire-mode row's own
     *  icons still do at {@link #FIRE_ICON_SIZE}) means the destination box is spent entirely on
     *  real icon detail instead of blank space, which is what read as mushy/warped at a small size -
     *  see {@link #drawAmmoTypeIcon}. */
    private static final int FIRE_ICON_CROP_U = 33;
    private static final int FIRE_ICON_CROP_V = 30;
    private static final int FIRE_ICON_CROP_W = 68;
    private static final int FIRE_ICON_CROP_H = 68;
    /** Small squares near the top of the primary icon box showing equipped attachments - see
     *  {@link WeaponConfig#showAttachmentDots}. */
    private static final int ATTACHMENT_DOT_SIZE = 4;
    private static final int ATTACHMENT_DOT_GAP = 3;

    /** Outer padding of the combined ammo/name/fire-mode chip, and of each icon box, around their
     *  content — kept tight on purpose for the "sleek, modern" look, not a big roomy box. */
    static final int CHIP_PAD_X = 4;
    private static final int CHIP_PAD_Y = 2;
    /** Gap between the combined chip's internal rows (mag|reserve, name, fire-mode) - tightened
     *  from the previous 3px inter-chip gap now that they share one box instead of three. */

    private ResourceLocation currentGunId;
    private ResourceLocation secondaryGunId;
    /** Shared timer for the "push up into place" animation, the fire-type flash announcement, and
     *  (implicitly, since it's drawn every frame while a gun is held) everything else that's timed
     *  off a weapon switch - they're all triggered by the same event, so one timer covers all of it. */
    private long switchAnimStartMs = Long.MIN_VALUE / 2; // safely "long ago", avoids overflow in elapsed math
    private static final long PUSH_ANIM_MS = 220L;
    /** Duration of the bright glow flash behind the primary icon on weapon switch - see
     *  {@link WeaponConfig#showWeaponGlow}. Longer than the push animation itself so the glow
     *  visibly outlasts the slide, reading as "the icon just settled" rather than cutting off
     *  the instant the slide finishes. */
    private static final long SWITCH_GLOW_MS = 400L;

    /** Fire mode of the currently-held gun as of last frame - lets {@link #render} tell "you cycled
     *  fire mode on the SAME gun" apart from "you switched to a different gun" (the latter already
     *  has its own trigger, {@link #switchAnimStartMs}). Reset to {@code null} on a weapon switch so
     *  the new gun's mode is never compared against the old gun's. */
    private String lastFireModeSeen;
    /** Timer for a fire-mode change on the currently-held gun - see {@link #lastFireModeSeen}. Only
     *  drives the "FIRE TYPE: ..." announcement ({@link #drawFireTypeFlash}); deliberately doesn't
     *  touch the icon push/glow/border-flash above, since those are specifically about a weapon
     *  swap, not a fire-mode cycle on the gun you're already holding. */
    private long fireModeChangeStartMs = Long.MIN_VALUE / 2;

    /** How often (ms) we re-poke TACZ's own "show built-in gun HUD" toggle off while our widget is
     *  actively drawing. Fixes the "HUD đôi lúc bị đè lên nhau" report: something else (TACZ
     *  reloading its client config on a gunpack switch, another addon touching the same flag for
     *  its own vehicle/turret integration, etc.) can flip {@code RenderConfig.GUN_HUD_ENABLE} back
     *  on after our one-time sync at config-load. Re-asserting it every second while we're the
     *  active weapon HUD is cheap (a handful of reflection calls) and makes TACZ's own HUD unable
     *  to "win back" the screen for more than about a second at a time. */
    private static final long VANILLA_HUD_REASSERT_MS = 1000L;
    private long lastVanillaHudReassertMs = Long.MIN_VALUE / 2;

    /** True while THIS widget has force-silenced TACZ's/TaczPresence's gun HUD because the player is
     *  a spectator or riding an SBW vehicle - see {@link #updateContextSuppression}. */
    private boolean contextSuppressed;
    private long lastContextSuppressMs = Long.MIN_VALUE / 2;

    /**
     * TACZ's own HUD and TaczPresence's HUD share one render gate ({@code GUN_HUD_ENABLE}). While
     * spectating or riding an SBW vehicle neither should be on screen, regardless of which HUD
     * source is preferred - so this forces that gate off for as long as either state lasts (also
     * when the preferred source is TACZ_HUD/TACZ_P_HUD, i.e. even when this widget itself is not
     * the active HUD), re-asserting it about once a second like {@link #reassertVanillaHudOff()},
     * and hands control back to the normal preference sync the moment the state ends.
     */
    private void updateContextSuppression(boolean want) {
        if (want) {
            long now = Util.getMillis();
            if (!contextSuppressed || now - lastContextSuppressMs >= VANILLA_HUD_REASSERT_MS) {
                contextSuppressed = true;
                lastContextSuppressMs = now;
                TaczCompat.setVanillaGunHudEnabled(false);
            }
        } else if (contextSuppressed) {
            contextSuppressed = false;
            WeaponConfig.syncHudSource();
        }
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!TaczCompat.loaded()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        boolean spectator = player.isSpectator();
        boolean inVehicle = SuperbWarfareCompat.isInVehicle(player);
        updateContextSuppression(spectator || inVehicle);
        if (!WeaponConfig.enabled) {
            return;
        }
        if (Minecraft.getInstance().options.hideGui && !WeaponConfig.ignoreHideGui) {
            return;
        }
        if (spectator) {
            // Spectator has no real held gun to show a HUD for - this widget draws nothing, and
            // TACZ's/TaczPresence's own HUD is held off by updateContextSuppression above (it must
            // NOT be restored here, see restoreVanillaHudIfIdle's javadoc).
            currentGunId = null;
            return;
        }
        if (inVehicle) {
            // Riding an SBW vehicle - it draws its own vehicle/weapon HUD over the same bottom-right
            // area, for every seat, so ours steps aside entirely rather than overlap it (TACZ's own
            // HUD likewise held off by updateContextSuppression).
            currentGunId = null;
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && !TaczCompat.isGun(held) && currentGunId != null && LoadoutGear.isGearItem(held)) {
            // holding a melee / throwable / consumable shown in this HUD: the gun HUD stays up (for the gun you
            // were just using) instead of dropping to the idle layout
            ItemStack keep = findInHotbar(player.getInventory(), currentGunId);
            if (keep != null) {
                held = keep;
            }
        }
        if (held.isEmpty() || !TaczCompat.isGun(held)) {
            currentGunId = null;
            renderNeutral(graphics, player, screenWidth, screenHeight);
            return;
        }
        Font font = Minecraft.getInstance().font;

        { long _pt = HudProfiler.mark(); trackWeaponSlots(player, held); HudProfiler.section("weapon.logic", _pt); }
        reassertVanillaHudOff();

        // Cycling fire mode on the SAME gun (e.g. TACZ's own fire-mode key) should replay the
        // "FIRE TYPE: ..." announcement too, not just switching to a different gun - see
        // fireModeChangeStartMs/lastFireModeSeen. Runs every frame a gun is held, independent of
        // trackWeaponSlots' own switch detection above.
        String heldFireMode = TaczCompat.fireMode(held);
        if (lastFireModeSeen != null && heldFireMode != null && !heldFireMode.equals(lastFireModeSeen)) {
            fireModeChangeStartMs = Util.getMillis();
        }
        lastFireModeSeen = heldFireMode;

        int frameW = WeaponConfig.frameWidth;
        int frameH = WeaponConfig.frameHeight;

        if (LoadoutGear.enabled()) {
            LoadoutGear.update(player, false); // before the position: the frame width depends on which gear slots exist
        }
        int[] pos = resolvePosition(screenWidth, screenHeight, frameW + LoadoutGear.extraWidth(), frameH, effectiveScale(graphics));
        int originX = pos[0];
        int originY = pos[1];

        int iconBoxX = frameW - ICON_BOX_W;
        int textRightEdge = iconBoxX - WeaponConfig.infoBoxGap;   // info box right edge (config: infoBox.gapToWeapon)

        // "Push up into place" on weapon switch: starts switchSlideDistance px lower and eases up
        // to its resting position over PUSH_ANIM_MS, using the same switch timer as the name toast.
        // Applies ONLY to the icon box graphic(s) below - the ammo numbers, reserve, and fire-mode
        // row stay put and never slide, so the readable numbers don't jump around on a swap.
        // Quintic ease-out (matches EasedAnimator's default curve) for a snappier "premium" settle
        // than the plain quad curve this used to use. Independently toggle-able (showSwitchSlide) -
        // per feedback that the slide alone felt "loose" for some tastes, this can run alongside,
        // or instead of, the border flash below.
        long switchElapsed = Util.getMillis() - switchAnimStartMs;
        int pushOffset = 0;
        if (WeaponConfig.showSwitchSlide) {
            float animT = switchElapsed >= PUSH_ANIM_MS ? 1f : Math.max(0f, (float) switchElapsed / PUSH_ANIM_MS);
            float invT = 1f - animT;
            float eased = 1f - invT * invT * invT * invT * invT;
            pushOffset = Math.round((1f - eased) * WeaponConfig.switchSlideDistance);
        }

        // Same switch event drives a brief glow flash behind the primary icon (see SWITCH_GLOW_MS) -
        // an independent, longer-running fade so the glow visibly outlasts the slide itself.
        float switchGlowAlpha = 0f;
        if (WeaponConfig.showWeaponGlow && switchElapsed >= 0 && switchElapsed < SWITCH_GLOW_MS) {
            float glowT = 1f - (float) switchElapsed / SWITCH_GLOW_MS;
            switchGlowAlpha = glowT * glowT; // squared for a punchier falloff than linear
        }

        // ...and a gold border flash on the primary icon's own border, fading back to the normal
        // border colour - modelled on vanilla Minecraft's own gold-flash accents (a totem of
        // undying's screen flash, for instance). Drawn as an EXTRA border pass layered on top of
        // the normal one at fading opacity, rather than colour-interpolated, so it naturally reads
        // as "gold fading back to normal" as less of it shows through.
        float switchBorderFlashAlpha = 0f;
        if (WeaponConfig.showSwitchBorderFlash && switchElapsed >= 0 && switchElapsed < WeaponConfig.switchBorderFlashMs) {
            float flashT = 1f - (float) switchElapsed / WeaponConfig.switchBorderFlashMs;
            switchBorderFlashAlpha = flashT * flashT;
        }

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(originX, originY, 0);
        float wScale = effectiveScale(graphics);
        pose.scale(wScale, wScale, 1.0f);
        pose.translate(0, (frameH - CONTENT_H) / 2, 0); // frameHeight: the content is centred in the frame

        { long _pt = HudProfiler.mark(); drawInfoBox(graphics, font, player, held, textRightEdge); HudProfiler.section("weapon.infoBox", _pt); }

        pose.pushPose();
        pose.translate(0, pushOffset, 0);
        { long _pt = HudProfiler.mark(); drawPrimaryIcon(graphics, held, iconBoxX, 0, ICON_BOX_W, ICON_BOX_H, switchGlowAlpha, switchBorderFlashAlpha); HudProfiler.section("weapon.primaryIcon", _pt); }
        { long _pt = HudProfiler.mark(); drawHeatFlash(graphics, held, iconBoxX, 0, ICON_BOX_W, ICON_BOX_H); HudProfiler.section("weapon.heatFlash", _pt); }
        if (WeaponConfig.showAttachmentDots) {
            // Drawn AFTER (so on top of) the icon, INSIDE the box's own bounds near the top edge -
            // not a separate row above it, which previously left the box's own frame with nothing
            // visibly connecting the dots to it.
            { long _pt = HudProfiler.mark(); drawAttachmentDots(graphics, held, iconBoxX, 0, ICON_BOX_W); HudProfiler.section("weapon.attachDots", _pt); }
        }
        if (WeaponConfig.showSecondary) {
            { long _pt = HudProfiler.mark(); drawSecondaryWeapon(graphics, player, iconBoxX, ICON_BOX_H + SECONDARY_GAP); HudProfiler.section("weapon.secondary", _pt); }
        }
        pose.popPose();

        if (LoadoutGear.enabled()) {
            { long _pt = HudProfiler.mark(); LoadoutGear.draw(graphics, frameW + LoadoutGear.GAP, false); HudProfiler.section("weapon.gear", _pt); }
        }

        pose.popPose();

        if (WeaponConfig.showFireTypeFlash) {
            // Drawn OUTSIDE the widget's own pose/scale on purpose - this is a centre-screen,
            // above-the-crosshair announcement, entirely independent of where the widget itself
            // sits on screen (see the class javadoc).
            { long _pt = HudProfiler.mark(); drawFireTypeFlash(graphics, font, held, screenWidth, screenHeight); HudProfiler.section("weapon.fireTypeFlash", _pt); }
        }
    }

    /** See {@link #VANILLA_HUD_REASSERT_MS}. Throttled so this is a once-a-second cost, not
     *  once-a-frame. */
    private void reassertVanillaHudOff() {
        long now = Util.getMillis();
        if (now - lastVanillaHudReassertMs < VANILLA_HUD_REASSERT_MS) {
            return;
        }
        lastVanillaHudReassertMs = now;
        WeaponConfig.syncHudSource();
    }

    /**
     * Counterpart to {@link #reassertVanillaHudOff()} — restores TACZ's own built-in gun HUD when
     * THIS widget genuinely has nothing of its own to draw AND nothing else is claiming that screen
     * space either: specifically, not carrying a single gun anywhere in the hotbar. Without this,
     * TACZ's (and TaczPresence's) own HUD stayed suppressed indefinitely in that situation — hiding
     * that other mod's HUD with nothing of ours shown in its place, since the suppression only ever
     * got REASSERTED while actively rendering, never released when we stopped.
     *
     * <p>Deliberately NOT called from the Spectator or SBW-vehicle early-returns above, even though
     * this widget draws nothing there either — TACZ's own HUD springing back on in either of those
     * would be its own visible regression (a spectator has no real held gun to show a HUD for; a
     * vehicle already has SBW's own HUD occupying that space), so both of those need TACZ's HUD to
     * stay suppressed for as long as they last, not just while this widget would have drawn
     * something.
     *
     * <p>Only touches it while ModernHud is the actively-preferred source
     * ({@code preferredHudSource == MODERNHUD}); if the player instead picked one of TACZ's own HUDs
     * via the cycle keybind, that toggle belongs to {@code TACZ_HUD}/{@code TACZ_P_HUD} to manage,
     * not something this widget should fight over. Not throttled like the reassert above — it's only
     * ever called from an early-return path that already isn't doing any other per-frame work.
     */
    private void restoreVanillaHudIfIdle() {
        if (WeaponConfig.preferredHudSource == WeaponHudSource.MODERNHUD) {
            TaczCompat.setVanillaGunHudEnabled(true);
        }
    }

    private static final long NEUTRAL_SCAN_INTERVAL_MS = 250L;
    private ResourceLocation[] cachedNeutralGuns = new ResourceLocation[0];
    private long lastNeutralScanMs = Long.MIN_VALUE / 2;

    /**
     * Not holding a TACZ gun right now (pickaxe, block, bare hands...) — but the widget doesn't
     * just vanish if you're still CARRYING one or two guns elsewhere in the hotbar. Shows up to two
     * of them, ordered by hotbar slot (earliest slot first), both at the same reduced/dim scale the
     * secondary slot normally uses — neither highlighted as "the" primary, since you aren't
     * actually holding either one. No name/mag readout (there's nothing to read when nothing's in
     * hand), but each DOES show its reserve ammo count, same as the normal secondary slot does.
     * {@link #currentGunId}/{@link #secondaryGunId} (the switch-history used while actually holding
     * a gun) are deliberately left untouched here, so picking a real gun back up resumes
     * primary/secondary tracking exactly where it left off.
     *
     * <p>The hotbar scan itself is throttled to once every {@link #NEUTRAL_SCAN_INTERVAL_MS} rather
     * than re-scanning (and re-allocating a list/set/array for) all 9 slots on every single frame —
     * hotbar contents don't change nearly that often, and this path can run continuously for as long
     * as a player simply isn't holding a gun, which is most of the time for a lot of playstyles.
     */
    private void renderNeutral(GuiGraphics graphics, LocalPlayer player, int screenWidth, int screenHeight) {
        if (!WeaponConfig.enabled || !TaczCompat.loaded()) {
            return;
        }
        if (Minecraft.getInstance().options.hideGui && !WeaponConfig.ignoreHideGui) {
            return;
        }
        long now = Util.getMillis();
        if (now - lastNeutralScanMs >= NEUTRAL_SCAN_INTERVAL_MS) {
            lastNeutralScanMs = now;
            cachedNeutralGuns = findGunsInHotbarOrdered(player.getInventory(), 2);
        }
        ResourceLocation[] guns = cachedNeutralGuns;
        if (guns.length == 0) {
            restoreVanillaHudIfIdle();
            return; // nothing carried right now - genuinely nothing to show
        }
        reassertVanillaHudOff();

        int frameW = WeaponConfig.frameWidth;
        if (LoadoutGear.enabled()) {
            LoadoutGear.update(player, true);
        }
        int[] pos = resolvePosition(screenWidth, screenHeight, frameW + LoadoutGear.extraWidth(), WeaponConfig.frameHeight, effectiveScale(graphics));
        int iconBoxX = frameW - ICON_BOX_W;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(pos[0], pos[1], 0);
        float wScale = effectiveScale(graphics);
        pose.scale(wScale, wScale, 1.0f);
        pose.translate(0, (WeaponConfig.frameHeight - CONTENT_H) / 2, 0);

        int boxX = iconBoxX; // secondary/neutral box width now matches the primary box exactly
        int firstY = (ICON_BOX_H - SECONDARY_BOX_H) / 2; // centred where the primary icon normally sits
        drawNeutralIcon(graphics, player, guns[0], boxX, firstY);
        if (guns.length > 1) {
            drawNeutralIcon(graphics, player, guns[1], boxX, firstY + SECONDARY_BOX_H + SECONDARY_GAP);
        }
        if (LoadoutGear.enabled()) {
            LoadoutGear.draw(graphics, frameW + LoadoutGear.GAP, true); // idle: compact gear slots beside the idle strips
        }

        pose.popPose();
    }

    /** Up to {@code max} DIFFERENT guns found in the hotbar (slots 0-8), in slot order — the
     *  discovery method for {@link #renderNeutral} (only actually called once every
     *  {@link #NEUTRAL_SCAN_INTERVAL_MS}, see there), deliberately independent of switch-history.
     *  Writes straight into a fixed-size array instead of a List+Set+toArray, since {@code max} is
     *  always small (2) and this already only runs a few times a second. */
    private ResourceLocation[] findGunsInHotbarOrdered(Inventory inventory, int max) {
        ResourceLocation[] found = new ResourceLocation[max];
        int count = 0;
        for (int i = 0; i < 9 && count < max; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = TaczCompat.gunId(stack);
            if (id == null) {
                continue;
            }
            boolean duplicate = false;
            for (int j = 0; j < count; j++) {
                if (found[j].equals(id)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                found[count++] = id;
            }
        }
        return count == max ? found : java.util.Arrays.copyOf(found, count);
    }

    /** Draws one individually-sized flat-colour background chip, gated on {@link WeaponConfig#showPanel},
     *  plus its optional border accent (see {@code HudPanel#drawBorder}), gated on
     *  {@link WeaponConfig#borderStyle} != NONE - only drawn when the fill above is also on, since it's
     *  layered on top of it, not a replacement. {@code extraAlphaMul} additionally scales BOTH the
     *  fill's and the border's opacity (0-1, e.g. {@link WeaponConfig#secondaryAlpha}) for chips that
     *  should recede further, like the secondary/neutral icon boxes — pass 1.0 for full strength.
     *  Every boxed element on this widget goes through here, so they all share one source of truth. */
    private void drawChipBox(GuiGraphics g, int x, int y, int w, int h, double extraAlphaMul) {
        if (WeaponConfig.showPanel) {
            HudPanel.draw(g, x, y, w, h, WeaponConfig.panelColor, WeaponConfig.panelOpacity * extraAlphaMul);
            HudPanel.drawBorder(g, x, y, w, h, WeaponConfig.borderColor,
                    WeaponConfig.borderOpacity * extraAlphaMul, WeaponConfig.borderStyle, WeaponConfig.borderLength, WeaponConfig.borderOutset,
                    WeaponConfig.borderScale / 100.0);
        }
    }

    /** Same fill as {@link #drawChipBox}, but a more minimal accent: two small dots at the BOTTOM
     *  corners only, nothing at the top corners at all — the secondary/neutral weapon box's own
     *  distinct treatment, matching the reference HUD's less prominent styling for a slot that
     *  isn't the one actively being fired. */
    private void drawSecondaryChipBox(GuiGraphics g, int x, int y, int w, int h, double extraAlphaMul) {
        if (WeaponConfig.showPanel) {
            HudPanel.draw(g, x, y, w, h, WeaponConfig.panelColor, WeaponConfig.panelOpacity * extraAlphaMul);
            HudPanel.drawBottomDots(g, x, y, w, h, WeaponConfig.borderColor,
                    WeaponConfig.borderOpacity * extraAlphaMul,
                    Math.max(1, (int) Math.round(WeaponConfig.secondaryDotSize * WeaponConfig.borderScale / 100.0)));
        }
    }

    /**
     * Keeps track of "current gun" vs. "the last different gun you held" (secondary), the same
     * primary/secondary relationship Call of Duty shows: whichever gun is in hand right now is
     * always primary; switching to a different gun demotes the old one to secondary (and picking
     * the old one back up promotes it again). Switching to something that ISN'T a gun (a
     * pickaxe, a block, your fists) is ignored entirely - it neither becomes primary nor bumps
     * the secondary slot, so the last two actual guns stay remembered across non-gun items.
     *
     * <p>Switch-history alone used to be the ONLY way the secondary slot got populated — meaning
     * if you spawned in (or picked up a second gun) already carrying two guns and never actually
     * scrolled between them yet, the secondary box stayed empty even though you were holding two
     * guns right then. Now, whenever switch-history hasn't given us a secondary (or the one it
     * remembered isn't in the hotbar any more), we fall back to scanning the hotbar for whatever
     * OTHER gun is sitting there and show that instead — so both slots populate immediately, no
     * scrolling back and forth required first.
     */
    private void trackWeaponSlots(LocalPlayer player, ItemStack held) {
        ResourceLocation gunId = TaczCompat.gunId(held);
        if (gunId == null) {
            return;
        }
        if (!gunId.equals(currentGunId)) {
            if (currentGunId != null && !currentGunId.equals(gunId)) {
                secondaryGunId = currentGunId;
            }
            currentGunId = gunId;
            switchAnimStartMs = Util.getMillis();
            // Don't let the new gun's fire mode be compared against the old gun's next frame -
            // that's not a "you cycled fire mode" event, and the switch flash above already covers
            // announcing the new gun's mode.
            lastFireModeSeen = null;
        }
        if (gunId.equals(secondaryGunId)) {
            // Picked the secondary weapon back up - it's primary now, and there's nothing to show
            // as secondary until the player switches to some OTHER gun (or the hotbar fallback
            // below finds one).
            secondaryGunId = null;
        }
        if (secondaryGunId == null || findInHotbar(player.getInventory(), secondaryGunId) == null) {
            secondaryGunId = findOtherGunInHotbar(player.getInventory(), gunId);
        }
    }

    /** First gun in the hotbar (slots 0-8) that ISN'T {@code excludeGunId} — the auto-detect
     *  fallback for {@link #trackWeaponSlots}. */
    private ResourceLocation findOtherGunInHotbar(Inventory inventory, ResourceLocation excludeGunId) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = TaczCompat.gunId(stack);
            if (id != null && !id.equals(excludeGunId)) {
                return id;
            }
        }
        return null;
    }

    /**
     * Mag "|" reserve — its OWN independent element now, not part of a shared box with name/
     * fire-mode (per closer inspection of the reference HUD: only the ammo row itself gets a
     * background, the framed area does NOT extend over the name/fire-mode below it). Background is
     * a soft GLOW rather than a flat shadowbox fill, matching the reference's radiant look around
     * the numbers, plus corner brackets sitting OUTSIDE the row (see {@link WeaponConfig#borderOutset}).
     * Reserve renders smaller ({@link WeaponConfig#reserveScaleOffset}) and dimmer
     * ({@link WeaponConfig#reserveOpacity}) than the mag number. When {@link WeaponConfig#zeroPadAmmo}
     * is on, each number's leading zero(s) render at reduced opacity relative to its own significant
     * digits — a "ghost placeholder" look, matching the reference's faint leading "0" in "030".
     * Returns the Y just below this row, for {@link #drawNameRow} to sit under.
     */
    /** Everything the ammo row needs, measured once so the frame can be sized before anything is drawn. */
    private record AmmoRow(String magText, int magColor, boolean low, float ammoScale, float reserveScale, String reserveText, boolean hasReserve, float magW, float sepW, float reserveW, int ammoRowH, int reserveRowH, int iconW, int iconGap, float ammoRowW, int reserveColor) {
    }

    private AmmoRow ammoRow(GuiGraphics g, Font font, LocalPlayer player, ItemStack gun) {
        int mag = TaczCompat.magAmmo(player, gun);
        int cap = TaczCompat.magCapacity(gun);
        String magText = ammoString(mag);
        boolean low = cap > 0 && (100.0 * mag / cap) <= WeaponConfig.lowAmmoThresholdPercent;
        int magColor = (low ? WeaponConfig.ammoLowColor : WeaponConfig.ammoColor) | 0xFF000000;

        double textScale = WeaponConfig.ammoTextScale * contentScale();
        float[] scales = HudText.snapPair(g, textScale, Math.max(0.3, textScale - WeaponConfig.reserveScaleOffset));
        float ammoScale = scales[0];
        float reserveScale = scales[1];

        String reserveText = WeaponConfig.showReserveAmmo ? reserveString(player, gun) : "";
        boolean hasReserve = !reserveText.isEmpty();
        String sep = " | ";

        float magW = font.width(magText) * ammoScale;
        float sepW = hasReserve ? font.width(sep) * reserveScale : 0f;
        float reserveW = hasReserve ? font.width(reserveText) * reserveScale : 0f;
        int ammoRowH = Math.round(font.lineHeight * ammoScale);

        int reserveRowH = Math.round(font.lineHeight * reserveScale);

        int iconW = 0;
        int iconGap = 0;
        if (hasReserve && WeaponConfig.showAmmoTypeIcon) {
            // Sized to match the RESERVE digits' own rendered height (reserveRowH), not the larger
            // mag digits' height (ammoRowH) - this icon sits directly next to the reserve number,
            // so it needs to track reserveScale to stay visually balanced with it. Using ammoRowH
            // here made the icon render oversized relative to the (smaller, dimmer) reserve text.
            // On top of that, matching reserveRowH 1:1 (scale 1.0) STILL reads oversized next to
            // the actual glyphs, since the digits' rendered ink is noticeably smaller than the
            // font's full line-height box - so ammoTypeIconScale (<1) shrinks it down further to
            // an actually-balanced size. Still tracks ammoTextScale/reserveScaleOffset instead of
            // a fixed pixel size - see drawAmmoTypeIcon for how it's cropped/scaled cleanly to any size.
            iconW = Math.max(1, Math.round(reserveRowH * (float) WeaponConfig.ammoTypeIconScale * RESERVE_ICON_BOOST));
            iconGap = 4;
        }
        float ammoRowW = magW + sepW + reserveW + iconGap + iconW;

        int reserveAlpha = Math.max(1, (int) Math.round(WeaponConfig.reserveOpacity * 2.55));
        int reserveColor = (reserveAlpha << 24) | (WeaponConfig.reserveColor & 0xFFFFFF);

        return new AmmoRow(magText, magColor, low, ammoScale, reserveScale, reserveText, hasReserve, magW, sepW, reserveW, ammoRowH, reserveRowH, iconW, iconGap, ammoRowW, reserveColor);
    }

    /** Draws the ammo row (and the frame's border and glow) inside the given frame; returns the bottom of the row. */
    private int drawAmmoRow(GuiGraphics g, Font font, AmmoRow r, int boxX, int boxY, int boxW, int boxH, int rightEdge,
                            int rowTop) {
        String magText = r.magText();
        int magColor = r.magColor();
        boolean low = r.low();
        float ammoScale = r.ammoScale();
        float reserveScale = r.reserveScale();
        String reserveText = r.reserveText();
        boolean hasReserve = r.hasReserve();
        float magW = r.magW();
        float sepW = r.sepW();
        float reserveW = r.reserveW();
        int ammoRowH = r.ammoRowH();
        int reserveRowH = r.reserveRowH();
        int iconW = r.iconW();
        int iconGap = r.iconGap();
        float ammoRowW = r.ammoRowW();
        int reserveColor = r.reserveColor();
        // the info box has its OWN panel + border (WeaponConfig.infoBoxStyle), independent of the weapon icon boxes
        HudPanel.drawStyled(g, boxX, boxY, boxW, boxH, WeaponConfig.infoBoxStyle, 1f);

        // mag + reserve (and the reserve icon) sit a few px higher than the row's slot; the box, glow and name row stay put
        int cursorY = rowTop + INFO_PAD_Y - AMMO_TEXT_RAISE;
        float rowRightX = rightEdge - CHIP_PAD_X;
        float cursorX = rowRightX;
        drawAmmoGlow(g, boxX, boxY, boxW, boxH, cursorY, ammoRowH, low);

        if (hasReserve && WeaponConfig.showAmmoTypeIcon) {
            cursorX -= iconW;
            int iconY = cursorY + (ammoRowH - iconW) / 2;
            drawAmmoTypeIcon(g, Math.round(cursorX), iconY, iconW, WeaponConfig.reserveColor,
                    (float) (WeaponConfig.reserveOpacity / 100.0));
            cursorX -= iconGap;
        }
        if (hasReserve) {
            int reserveTextH = Math.round(font.lineHeight * reserveScale);
            float reserveY = cursorY + (ammoRowH - reserveTextH) / 2f;
            cursorX -= reserveW;
            boolean panel = WeaponConfig.ammoGlowReservePanel && WeaponConfig.ammoGlowReservePanelOpacity > 0;
            if (panel) {
                // small box in the glow color behind the reserve digits: centered on them horizontally, sitting on their
                // foot, and a little shorter than the digits (reservePanelHeight percent of their height), like the reference
                float digitH = 7f * reserveScale;
                float padX = 2.5f * reserveScale;
                float ph = Math.max(2f, digitH * WeaponConfig.ammoGlowReservePanelHeight / 100f);
                float py0 = reserveY + digitH - ph;               // bottom edge on the foot of the digits
                int pa = Math.max(1, Math.min(255, Math.round(WeaponConfig.ammoGlowReservePanelOpacity * 2.55f)));
                PixelDraw.fillUnits(g, cursorX - padX, py0, reserveW + padX * 2f, ph,
                        (pa << 24) | (WeaponConfig.weaponGlowColor & 0xFFFFFF));
            }
            float magHalo = haloAlpha;
            haloAlpha = 0f;                      // only the magazine digits get a halo
            drawZeroPaddedNumber(g, font, reserveText, cursorX, reserveY, reserveScale, reserveColor);
            haloAlpha = magHalo;
            cursorX -= sepW;
            if (!panel) {
                // thin divider between mag and reserve (only when there is no reserve box) (the pixel font's own '|' glyph is split in the middle)
                int sepAlpha = Math.max(1, Math.round(((reserveColor >>> 24) & 0xFF) * 0.65f));
                PixelDraw.fillUnits(g, cursorX + (sepW - reserveScale) / 2f, reserveY + reserveScale * 0.5f,
                        reserveScale, reserveScale * 7f, (sepAlpha << 24) | (reserveColor & 0xFFFFFF));
            }
        }
        cursorX -= magW;
        drawZeroPaddedNumber(g, font, magText, cursorX, cursorY, ammoScale, magColor);

        return rowTop + INFO_PAD_Y + ammoRowH;   // bottom of the ammo row
    }

    /**
     * The info box: ammo (mag | reserve), gun name and fire mode share ONE framed box to the left of the weapon boxes.
     * Its bottom edge lines up with the bottom of the secondary weapon box and it rises {@code infoBoxRise} pixels above
     * the top of the primary box, so it reads as its own element next to the two weapon boxes. Ammo sits at the top,
     * name and fire mode are anchored to the bottom, and the box grows upward if the ammo digits need more room.
     */
    private void drawInfoBox(GuiGraphics g, Font font, LocalPlayer player, ItemStack gun, int rightEdge) {
        AmmoRow row = ammoRow(g, font, player, gun);
        boolean nameShown = WeaponConfig.showName;
        String[] modes = WeaponConfig.showFireMode ? TaczCompat.supportedFireModes(gun) : new String[0];
        boolean fireShown = modes.length > 1;
        boolean heatShown = WeaponConfig.heatDisplayMode == HeatDisplayMode.BAR && TaczCompat.hasHeat(gun);

        // The three rows (ammo, name, fire mode) are packed tight and centred as one block inside the box, so they sit
        // close together like the reference; a taller box (infoBoxRise) just adds even margin above and below.
        // The slot for each row is FIXED (same for every gun, with or without a fire mode control).
        int blockH = row.ammoRowH() + WeaponConfig.infoNameGap + font.lineHeight + WeaponConfig.infoRowGap + fireCellH();
        int boxH = Math.max(CONTENT_H + WeaponConfig.infoBoxRise, blockH + INFO_PAD_Y * 2);
        int boxBottom = CONTENT_H;
        int boxY = boxBottom - boxH;

        int nameW = nameShown ? (int) Math.ceil(HudText.width(g, font, gun.getHoverName().getString())) : 0;
        int fireW = fireShown ? fireCellW() * modes.length + FIRE_RIGHT_INSET - CHIP_PAD_X : 0;
        int contentW = Math.max(Math.round(row.ammoRowW()), Math.max(nameW, fireW));
        int boxW = Math.max(contentW + CHIP_PAD_X * 2, rightEdge);
        int boxX = rightEdge - boxW;

        int centered = boxY + Math.round((boxH - blockH) / 2f);
        // lift the whole block (ammo, name, fire mode) by contentRaise, but keep it inside the box
        int digitTop = Math.max(boxY + 1, Math.min(centered - WeaponConfig.infoContentRaise, boxBottom - blockH - 1));
        int nameY = digitTop + row.ammoRowH() + WeaponConfig.infoNameGap;
        int fireY = nameY + font.lineHeight + WeaponConfig.infoRowGap;
        int rowTop = digitTop + AMMO_TEXT_RAISE - INFO_PAD_Y;     // drawAmmoRow adds INFO_PAD_Y and lifts by AMMO_TEXT_RAISE
        drawAmmoRow(g, font, row, boxX, boxY, boxW, boxH, rightEdge, rowTop);

        if (nameShown) {
            drawNameRow(g, font, gun, rightEdge, nameY);
        }
        if (fireShown) {
            drawFireModeSegmented(g, font, gun, rightEdge, fireY);
        }
        if (heatShown) {
            // sits in the gap just above the fire mode control, or in its place when the gun has none
            drawHeatBar(g, gun, rightEdge, fireShown ? fireY - HEAT_BAR_H - 1 : fireY + (fireCellH() - HEAT_BAR_H) / 2);
        }
    }

    /** The weapon's name — plain floating text, no background of its own, matching the reference
     *  HUD (the ammo row's glow does not extend down over this). Always visible, no fade. */
    private int drawNameRow(GuiGraphics g, Font font, ItemStack gun, int rightEdge, int y) {
        if (!WeaponConfig.showName) {
            return y;
        }
        String name = gun.getHoverName().getString();
        float nameW = HudText.width(g, font, name);
        int nameColor = WeaponConfig.nameColor | 0xFF000000;
        HudText.text(g, font, name, Math.round(rightEdge - CHIP_PAD_X - nameW), y, nameColor, true);
        return y + font.lineHeight;
    }

    /**
     * Fire mode as a segmented control: ONE box divided into equal-width cells, one per supported
     * mode, each showing that mode's icon — matching the reference HUD's 2-cell split button rather
     * than a loose row of icons. The ACTIVE mode's cell gets an extra highlight border drawn around
     * just that cell ("whichever mode is active, THAT one gets framed"), on top of the shared outer
     * box/border every other chip uses. Hidden entirely for guns with one (or zero reported) modes -
     * nothing to indicate. Falls back to a single-letter label per cell if the icon-set lookup comes
     * back empty for some reason, so real multi-mode support never silently shows nothing.
     */
    private void drawFireModeSegmented(GuiGraphics g, Font font, ItemStack gun, int rightEdge, int y) {
        if (!WeaponConfig.showFireMode) {
            return;
        }
        String[] modes = TaczCompat.supportedFireModes(gun);
        if (modes.length <= 1) {
            return;
        }
        String activeMode = TaczCompat.fireMode(gun);

        // wide rectangular cells (icon width > height) with padding, so the bullets never touch a cell border
        int cellW = fireCellW();
        int cellH = fireCellH();
        int boxW = cellW * modes.length;
        int boxX = rightEdge - FIRE_RIGHT_INSET - boxW;   // clear of the info box border

        if (WeaponConfig.showPanel) {
            HudPanel.draw(g, boxX, y, boxW, cellH, WeaponConfig.panelColor, WeaponConfig.panelOpacity);
        }

        for (int i = 0; i < modes.length; i++) {
            String mode = modes[i];
            int cellX = boxX + i * cellW;
            boolean isActive = mode.equals(activeMode);

            if (i > 0 && WeaponConfig.showPanel) {
                // thin divider between cells
                int dividerAlpha = Math.max(1, (int) Math.round(WeaponConfig.borderOpacity * 0.6 * 2.55));
                int dividerColor = (dividerAlpha << 24) | (WeaponConfig.borderColor & 0xFFFFFF);
                g.fill(cellX, y, cellX + 1, y + cellH, dividerColor);
            }
            if (isActive) {
                g.fill(cellX, y, cellX + cellW, y + cellH, 0x26FFFFFF);     // selected cell: light fill
            }
            if (isActive && WeaponConfig.showPanel) {
                HudPanel.drawFullBorder(g, cellX, y, cellW, cellH, WeaponConfig.borderColor, WeaponConfig.borderOpacity);
            }

            if (PixelDraw.isFireIcon(mode)) {
                PixelDraw.fireCellIcon(g, mode, cellX, y, cellW, cellH, isActive ? 0xFFFFFFFF : 0x80B4B4B4);
            } else {
                String label = mode.isEmpty() ? "?" : mode.substring(0, 1);
                int fmColor = (isActive ? 0xFFFFFFFF : (WeaponConfig.fireModeColor | 0xFF000000));
                HudText.text(g, font, label, cellX + (cellW - HudText.width(g, font, label)) / 2f,
                        y + (cellH - font.lineHeight) / 2, fmColor, true);
            }
        }
    }

    private static final int HEAT_BAR_H = 3;
    /** BAR mode is shorter than the full text-column width, per feedback that the original
     *  full-width version read as too dominant a row for a secondary readout. */
    private static final float HEAT_BAR_WIDTH_FRACTION = 0.55f;

    /**
     * Thin overheat progress bar (BAR mode only — see {@link WeaponConfig#heatDisplayMode}), for
     * guns using TACZ's own opt-in heat mechanic (see {@link TaczCompat#hasHeat}) — most guns don't
     * have this at all, so the bar simply never appears for them. One of two ways to restore an
     * equivalent to TACZ's own overheat readout, which disabling TACZ's built-in gun HUD to avoid
     * the two overlapping (see {@link TaczCompat}'s class javadoc) took away too, since TACZ ties
     * icon, ammo, AND heat to that one single {@code GUN_HUD_ENABLE} toggle, not separate ones.
     *
     * <p>Filled from the RIGHT edge growing leftward (see {@link BarRender#barRTL}) rather than the
     * usual left-to-right of every other bar in this mod, and shorter than the full text-column
     * width ({@link #HEAT_BAR_WIDTH_FRACTION}) — a deliberate visual distinction from the mag/reserve
     * ammo readout above it, per feedback that a full-width, left-filling bar read as just another
     * ammo-style readout rather than a distinct "heat" indicator.
     *
     * <p>Stacked below the fire-mode row when that row actually drew something this frame,
     * otherwise directly below the name row instead — {@code fireModeRowY} is the SAME y the caller
     * already passed to {@link #drawFireModeSegmented}, so this recomputes that row's height with
     * the same condition rather than needing a return value threaded back through it.
     */
    private void drawHeatBar(GuiGraphics g, ItemStack gun, int rightEdge, int y) {
        if (WeaponConfig.heatDisplayMode != HeatDisplayMode.BAR || !TaczCompat.hasHeat(gun)) {
            return;
        }
        float fraction = TaczCompat.heatFraction(gun);
        boolean locked = TaczCompat.isOverheatLocked(gun);
        int color = (0xFF000000) | (locked ? WeaponConfig.heatBarLockedColor : WeaponConfig.heatBarColor);

        int barW = Math.max(1, Math.round(rightEdge * HEAT_BAR_WIDTH_FRACTION));
        int barX = rightEdge - CHIP_PAD_X - barW; // right-anchored, same right edge as every row of the info box

        BarRender.barRTL(g, barX, y, barW, HEAT_BAR_H, fraction, color);
    }

    /**
     * Alternative to {@link #drawHeatBar} (FLASH mode — see {@link WeaponConfig#heatDisplayMode}):
     * instead of a separate bar, the primary weapon icon box itself glows, fading from
     * {@link WeaponConfig#heatFlashColor} (cold) toward {@link WeaponConfig#heatBarLockedColor}
     * (hot/locked) as heat rises, pulsing faster the hotter the gun gets and settling into a fast
     * steady pulse once fully overheat-locked. Drawn AFTER (so on top of) the icon itself, same as
     * the attachment dots — see the call site in {@link #render}.
     */
    private void drawHeatFlash(GuiGraphics g, ItemStack gun, int x, int y, int w, int h) {
        if (WeaponConfig.heatDisplayMode != HeatDisplayMode.FLASH || !TaczCompat.hasHeat(gun)) {
            return;
        }
        float fraction = TaczCompat.heatFraction(gun);
        boolean locked = TaczCompat.isOverheatLocked(gun);
        if (fraction <= 0.001f && !locked) {
            return; // cold - nothing to flash yet
        }
        int color = lerpRgb(WeaponConfig.heatFlashColor, WeaponConfig.heatBarLockedColor, fraction);
        // Pulse period tightens from 700ms (barely warm) down to 200ms (nearly overheated), settling
        // into a fast, steady 260ms pulse once actually locked - matches the low-ammo alert's own
        // pulse speed elsewhere in this mod, so "locked" reads consistently urgent across the widget.
        long periodMs = locked ? 260L : Math.round(700L - 500L * fraction);
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / (double) periodMs);
        float alpha = (locked ? 0.55f : 0.20f + 0.30f * fraction) * (0.5f + 0.5f * pulse);
        GlowEffect.drawGlowRect(g, x, y, w, h, color, alpha, 4f);
    }

    /** Linear-interpolates two opaque 0xRRGGBB colours (alpha byte, if any, is ignored/dropped). */
    private static int lerpRgb(int from, int to, float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        int fr = (from >> 16) & 0xFF, fg = (from >> 8) & 0xFF, fb = from & 0xFF;
        int tr = (to >> 16) & 0xFF, tg = (to >> 8) & 0xFF, tb = to & 0xFF;
        int r = Math.round(fr + (tr - fr) * clamped);
        int gr = Math.round(fg + (tg - fg) * clamped);
        int b = Math.round(fb + (tb - fb) * clamped);
        return (r << 16) | (gr << 8) | b;
    }

    /** Splits a (possibly zero-padded) number into a dim leading-zero portion and a bright
     *  significant-digit portion, drawing each separately - e.g. "030" draws a faint "0" then a
     *  bright "30". A number with no leading zeros (or zero-padding turned off, so nothing to dim)
     *  just draws normally in one piece. An all-zero string ("000") treats everything but the LAST
     *  digit as the dim "leading" portion, since the final digit is still the real value shown. */
    private void drawZeroPaddedNumber(GuiGraphics g, Font font, String text, float x, float y, float scale, int color) {
        float halo = haloAlpha;
        if (!WeaponConfig.zeroPadAmmo || text.length() <= 1) {
            drawTextHalo(g, font, text, x, y, scale, haloRgb, halo, WeaponConfig.ammoGlowHalo);
            drawScaledText(g, font, text, x, y, scale, color);
            return;
        }
        int splitIndex = 0;
        while (splitIndex < text.length() - 1 && text.charAt(splitIndex) == '0') {
            splitIndex++;
        }
        if (splitIndex == 0) {
            drawTextHalo(g, font, text, x, y, scale, haloRgb, halo, WeaponConfig.ammoGlowHalo);
            drawScaledText(g, font, text, x, y, scale, color);
            return;
        }
        String dimPart = text.substring(0, splitIndex);
        String brightPart = text.substring(splitIndex);
        int alpha = (color >>> 24) & 0xFF;
        int dimAlpha = Math.max(1, Math.round(alpha * 0.35f));
        int dimColor = (dimAlpha << 24) | (color & 0xFFFFFF);
        drawScaledText(g, font, dimPart, x, y, scale, dimColor, false);
        float brightX = x + font.width(dimPart) * scale;
        drawTextHalo(g, font, brightPart, brightX, y, scale, haloRgb, halo, WeaponConfig.ammoGlowHalo);   // only the bright digits glow
        drawScaledText(g, font, brightPart, brightX, y, scale, color);
    }

    /** The "ammo type" icon next to reserve — reuses the same full-auto fire-mode texture already
     *  used elsewhere on this widget (see {@link WeaponConfig#showAmmoTypeIcon}), tinted to match
     *  the reserve number, rather than a crude programmatically-drawn 3-line mark. Sized to {@code
     *  size} (the caller matches this to the ammo digits' own rendered height - see
     *  {@link #drawAmmoRow}) rather than a fixed pixel size.
     *
     * <p>Unlike the fire-mode row's own icons (drawn at a fixed {@link #FIRE_ICON_SIZE} straight off
     * the source PNG with no real crop), this draws a tight, SQUARE crop of just the icon's actual
     * content ({@link #FIRE_ICON_CROP_U}/{@code _V}/{@code _W}/{@code _H}, measured against the real
     * {@link #FIRE_ICON_TEX_SIZE}x{@code FIRE_ICON_TEX_SIZE} source) at an independent destination
     * size, using the same crop-with-independent-scale blit already used for the player head render
     * (see {@link PlayerHudOverlay#renderHead}). The source PNG has a lot of transparent padding
     * around the actual icon - stretching that whole padded canvas down to a small box (the earlier
     * approach here) spent most of the destination pixels on blank space, which read as
     * mushy/warped once this icon was sized up to match the ammo digits. Cropping to just the real
     * content first, THEN scaling, keeps it clean at any size. The crop is deliberately square so
     * scaling it onto this method's square destination box never skews the aspect ratio either. */
    private void drawAmmoTypeIcon(GuiGraphics g, int x, int y, int size, int colorRgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        if (a <= 0 || size <= 0) {
            return;
        }
        PixelDraw.fireIcon(g, "RESERVE", x, y, size, (a << 24) | (colorRgb & 0xFFFFFF));
    }

    /** Draws {@code text} with its top-left corner at the given SCREEN-space position, at its own
     *  independent scale — used where two pieces of text on the same row need different scales
     *  (the mag number and the smaller reserve number), so each can be pushed/scaled/popped on its
     *  own without disturbing the other's coordinate space. */
    private void drawScaledText(GuiGraphics g, Font font, String text, float screenX, float screenY, float scale, int color) {
        drawScaledText(g, font, text, screenX, screenY, scale, color, true);
    }

    private void drawScaledText(GuiGraphics g, Font font, String text, float screenX, float screenY, float scale, int color,
                                boolean shadow) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(screenX, screenY, 0);
        pose.scale(scale, scale, 1.0f);
        HudText.text(g, font, text, 0, 0, color, shadow);
        pose.popPose();
    }

    /** Reserve count for a gun that's ACTIVELY held/fired — no zero-padding. Empty string means
     *  "nothing to show" (inventory-ammo guns, which have no separate reserve concept); the "|"
     *  separator itself is drawn by the caller, not included here, so an empty reserve cleanly
     *  collapses the whole "| reserve" portion rather than leaving a stray separator behind. */
    private String reserveString(LocalPlayer player, ItemStack gun) {
        int reserve = TaczCompat.reserveAmmo(player, gun);
        if (reserve == TaczCompat.INFINITE) {
            return "\u221E";
        }
        if (reserve == TaczCompat.NO_RESERVE) {
            return "";
        }
        return ammoString(reserve);
    }

    /** "000" to "999", built once: formatting a number is an allocation per frame otherwise. */
    private static final String[] PADDED = new String[1000];
    private static final String[] PLAIN = new String[1000];

    private String ammoString(int count) {
        if (count < 0 || count >= 1000) {
            return WeaponConfig.zeroPadAmmo ? String.format("%03d", count) : String.valueOf(count);
        }
        String[] cache = WeaponConfig.zeroPadAmmo ? PADDED : PLAIN;
        String s = cache[count];
        if (s == null) {
            s = WeaponConfig.zeroPadAmmo ? String.format("%03d", count) : String.valueOf(count);
            cache[count] = s;
        }
        return s;
    }

    private ResourceLocation iconFor(String mode) {
        return switch (mode) {
            case "SEMI" -> FIRE_ICON_SEMI;
            case "AUTO" -> FIRE_ICON_AUTO;
            case "BURST" -> FIRE_ICON_BURST;
            default -> null;
        };
    }

    /**
     * "FIRE TYPE: FULL AUTO" (or just "FULL AUTO" if {@link WeaponConfig#fireTypeShowLabel} is off)
     * — flashes centre-screen, near where the ammo readout/alert text sits by default (see
     * {@link WeaponConfig#fireTypeXOffset}/{@link WeaponConfig#fireTypeYOffset} — same "positive =
     * right/down" sign convention as {@code ammoAlertXOffset}/{@code ammoAlertYOffset}, so the two
     * can be lined up together): starts in the flash colour, switches to the hold colour once the
     * flash phase ends, then fades out completely. The mode name itself is underlined; the
     * "FIRE TYPE: " prefix (when shown) isn't. Replays on EITHER of two triggers — switching to a
     * gun ({@link #switchAnimStartMs}) or cycling fire mode on the gun you're already holding
     * ({@link #fireModeChangeStartMs}) — driven by whichever one most recently fired, so it plays
     * exactly once per event, not on every frame the gun stays out. Silently does nothing for a gun
     * with no fire-mode data at all.
     */
    private void drawFireTypeFlash(GuiGraphics g, Font font, ItemStack gun, int screenWidth, int screenHeight) {
        String rawMode = TaczCompat.fireMode(gun);
        if (rawMode == null || rawMode.isEmpty()) {
            return;
        }
        long flashMs = Math.max(0, WeaponConfig.fireTypeFlashMs);
        long holdMs = Math.max(0, WeaponConfig.fireTypeHoldMs);
        long fadeMs = Math.max(1, WeaponConfig.fireTypeFadeMs);
        long totalMs = flashMs + holdMs + fadeMs;
        long elapsed = Util.getMillis() - Math.max(switchAnimStartMs, fireModeChangeStartMs);
        if (elapsed < 0 || elapsed >= totalMs) {
            return;
        }

        int color;
        float alpha;
        if (elapsed < flashMs) {
            color = WeaponConfig.fireTypeFlashColor;
            alpha = 1.0f;
        } else if (elapsed < flashMs + holdMs) {
            color = WeaponConfig.fireTypeHoldColor;
            alpha = 1.0f;
        } else {
            color = WeaponConfig.fireTypeHoldColor;
            alpha = 1.0f - (float) (elapsed - flashMs - holdMs) / fadeMs;
        }
        int a = Math.max(1, Math.round(alpha * 255f));
        int argb = (a << 24) | (color & 0xFFFFFF);

        String modeLabel = fireTypeLabel(rawMode);
        String prefix = WeaponConfig.fireTypeShowLabel ? "FIRE TYPE: " : "";

        float scale = HudText.snapScale(g, WeaponConfig.fireTypeScale);
        int prefixW = font.width(prefix);
        int modeW = font.width(modeLabel);
        int totalW = Math.round((prefixW + modeW) * scale);
        int textH = Math.round(font.lineHeight * scale);

        // Same "positive = right/down from screen centre" convention as the ammo alert overlay's
        // offsets, and (unlike before) NEGATIVE is fully supported on both axes - this used to only
        // be able to move UP from centre, which meant it could never be tucked down near the ammo
        // readout/alert area at all.
        int x = screenWidth / 2 - totalW / 2 + WeaponConfig.fireTypeXOffset;
        int y = screenHeight / 2 - textH / 2 + WeaponConfig.fireTypeYOffset;

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1.0f);
        if (!prefix.isEmpty()) {
            HudText.text(g, font, prefix, 0, 0, argb, true);
        }
        HudText.text(g, font, modeLabel, prefixW, 0, argb, true);
        g.fill(prefixW, font.lineHeight, prefixW + modeW, font.lineHeight + 1, argb); // underline the mode only
        pose.popPose();
    }

    /** Raw TACZ fire-mode id -&gt; the label shown in the flash announcement. */
    private String fireTypeLabel(String rawMode) {
        return switch (rawMode) {
            case "SEMI" -> "SEMI AUTO";
            case "AUTO" -> "FULL AUTO";
            case "BURST" -> "BURST";
            default -> rawMode;
        };
    }

    /**
     * A row of small gold squares INSIDE the primary icon box's own top-RIGHT corner, one per
     * EQUIPPED attachment (via {@link TaczCompat#attachmentSlotCounts}) — an empty or unsupported
     * slot draws nothing at all, not even a dim placeholder, so the row's width simply reflects how
     * many attachments are actually on the gun right now, and a bare gun draws none. Drawn AFTER
     * (on top of) the icon box itself, inset from its edges, rather than as a separate row floating
     * above it.
     */
    private void drawAttachmentDots(GuiGraphics g, ItemStack gun, int boxX, int boxY, int boxW) {
        int[] counts = TaczCompat.attachmentSlotCounts(gun);
        int filled = counts[1];
        if (filled <= 0) {
            return;
        }
        int inset = 3;
        int dotsW = filled * ATTACHMENT_DOT_SIZE + (filled - 1) * ATTACHMENT_DOT_GAP;
        int maxW = boxW - inset * 2;
        if (dotsW > maxW) {
            dotsW = maxW; // clamp - shouldn't realistically happen (max 6 slots), but stay inside the box
        }
        int x = boxX + boxW - inset - dotsW; // right-aligned within the box, not centred
        int y = boxY + inset;
        int color = 0xFF000000 | (WeaponConfig.attachmentDotColor & 0xFFFFFF);
        for (int i = 0; i < filled; i++) {
            g.fill(x, y, x + ATTACHMENT_DOT_SIZE, y + ATTACHMENT_DOT_SIZE, color);
            x += ATTACHMENT_DOT_SIZE + ATTACHMENT_DOT_GAP;
        }
    }

    /** The primary weapon's icon, centred in its fixed box, with an optional flat background chip
     *  (+ corner brackets) behind it, drawn through {@link #drawChipBox} like every other element. */
    private void drawPrimaryIcon(GuiGraphics g, ItemStack gun, int boxX, int boxY, int boxW, int boxH,
                                  float switchGlowAlpha, float switchBorderFlashAlpha) {
        drawChipBox(g, boxX, boxY, boxW, boxH, 1.0);
        if (switchBorderFlashAlpha > 0.01f && WeaponConfig.showPanel) {
            // An EXTRA border pass in the flash colour, layered on top of the normal one at fading
            // opacity - as less of this shows through, the normal border underneath dominates again,
            // which reads as "gold fading back to normal" without needing to interpolate the colours.
            HudPanel.drawBorder(g, boxX, boxY, boxW, boxH, WeaponConfig.switchBorderFlashColor,
                    WeaponConfig.borderOpacity * switchBorderFlashAlpha, WeaponConfig.borderStyle, WeaponConfig.borderLength, WeaponConfig.borderOutset);
        }
        drawSelectedGlow(g, boxX, boxY, boxW, boxH, switchGlowAlpha);
        if (TaczCompat.hasHudTexture(gun)) {
            int[] size = TaczCompat.iconSize((float) WeaponConfig.iconScale);
            int ix = boxX + (boxW - size[0]) / 2;
            int iy = boxY + (boxH - size[1]) / 2;
            TaczCompat.renderHudTexture(g, gun, ix, iy, (float) WeaponConfig.iconScale);
        } else {
            int size = Math.min(boxW, boxH) - 8;
            g.renderItem(gun, boxX + (boxW - size) / 2, boxY + (boxH - size) / 2);
        }
        if (switchGlowAlpha > 0.01f) {
            // Drawn LAST (on top of the icon) so it reads as a bright flash the moment the icon
            // settles into place, not a glow occluded behind it.
            GlowEffect.drawGlowRect(g, boxX, boxY, boxW, boxH, WeaponConfig.weaponGlowColor, switchGlowAlpha, 3f);
        }
    }

    /**
     * Soft glow in the corners INSIDE the selected weapon box (drawn behind the gun icon, additive, clipped to the box
     * because each corner only lights its own inward quarter). Colour / strength / reach / which corners / optional
     * breathing / extra flash on weapon switch are all configurable ({@code selectedGlow*} in the weapon config).
     */
    private void drawSelectedGlow(GuiGraphics g, int x, int y, int w, int h, float switchGlowAlpha) {
        if (!WeaponConfig.selGlowEnabled || WeaponConfig.selGlowOpacity <= 0) {
            return;
        }
        float a = WeaponConfig.selGlowOpacity / 100f;
        if (WeaponConfig.selGlowPulse) {
            double phase = (System.currentTimeMillis() % WeaponConfig.selGlowPulseMs) / (double) WeaponConfig.selGlowPulseMs;
            float wave = 0.5f - 0.5f * (float) Math.cos(phase * Math.PI * 2.0);      // 0..1, starts at the low point
            a *= 1f - (WeaponConfig.selGlowPulseDepth / 100f) * (1f - wave);
        }
        a += (WeaponConfig.selGlowSwitchBoost / 100f) * Math.max(0f, switchGlowAlpha) * 0.5f;
        a = Math.min(1f, a * (HudConfig.glowIntensity / 100f));
        if (a <= 0.003f) {
            return;
        }
        float r = WeaponConfig.selGlowRadius;
        int c = WeaponConfig.weaponGlowColor;
        // each corner has its OWN strength (the reference glows most in the bottom-right); alpha 0.5 at the corner
        // itself matches the weight of the other blooms, and a weaker corner also reaches a little less far
        if (WeaponConfig.selGlowTopLeft <= 0 && WeaponConfig.selGlowTopRight <= 0
                && WeaponConfig.selGlowBottomRight <= 0 && WeaponConfig.selGlowBottomLeft <= 0) {
            return;
        }
        // the four corners go out as ONE draw call instead of four
        SoftDraw.RadialBatch batch = SoftDraw.radialBatch(g, true);
        glowCorner(batch, x, y, r, c, a, WeaponConfig.selGlowTopLeft, 0b0001);                 // top-left
        glowCorner(batch, x + w, y, r, c, a, WeaponConfig.selGlowTopRight, 0b0010);            // top-right
        glowCorner(batch, x + w, y + h, r, c, a, WeaponConfig.selGlowBottomRight, 0b0100);     // bottom-right
        glowCorner(batch, x, y + h, r, c, a, WeaponConfig.selGlowBottomLeft, 0b1000);          // bottom-left
        SoftDraw.finish(batch);
    }

    private static void glowCorner(SoftDraw.RadialBatch batch, float cx, float cy, float radius, int rgb, float alpha,
                                   int strength, int quarter) {
        if (strength <= 0) {
            return;
        }
        float k = strength / 100f;
        SoftDraw.add(batch, cx, cy, radius * (0.6f + 0.4f * k), rgb, 0.5f * alpha * k, quarter);
    }

    /** Halo colour / strength for the magazine digits being drawn right now (set by {@link #drawAmmoGlow}). */
    private int haloRgb;
    private float haloAlpha;

    /**
     * Glow of the ammo box: a light fill inside the border (the whole box, mag and reserve together) that fades out a few
     * pixels past it, plus a tight halo around the magazine digits. Uses the shared glow colour; the halo switches to the
     * low-ammo colour while the magazine is low. Normal alpha blending, so it tints a bright background instead of
     * washing it out.
     */
    private void drawAmmoGlow(GuiGraphics g, int x, int y, int w, int h, int rowY, int rowH, boolean low) {
        haloAlpha = 0f;
        if (!WeaponConfig.ammoGlow) {
            return;
        }
        float pulse = 1f;
        if (WeaponConfig.ammoGlowPulse) {
            float wave = 0.5f - 0.5f * (float) Math.cos(System.currentTimeMillis() / 700.0);
            pulse = 0.6f + 0.4f * wave;
        }
        int glow = WeaponConfig.weaponGlowColor;
        if (low) {
            // low ammo: only the ammo glow reacts. Pulsing (if enabled) swings its colour gold -> red and back,
            // otherwise it just stays red. The box itself no longer pulses.
            float wave = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 260.0);
            glow = WeaponConfig.ammoGlowLowPulse
                    ? lerpRgb(WeaponConfig.weaponGlowColor, WeaponConfig.ammoLowColor, wave)
                    : WeaponConfig.ammoLowColor;
            if (WeaponConfig.ammoGlowLowPulse) {
                pulse = 0.75f + 0.25f * wave;
            }
        }
        float k = HudConfig.glowIntensity / 100f;
        if (WeaponConfig.ammoGlowBox > 0) {
            // One soft elliptical haze centered on the ammo digits, like the reference: strongest around the magazine
            // number and fading out toward the frame edges. No rectangles, so nothing has a visible edge.
            float a = WeaponConfig.ammoGlowBox / 100f * pulse * k;
            float reach = Math.max(0f, WeaponConfig.ammoGlowSpread);
            float cx = x + w * 0.45f;
            float cy = rowY + rowH * 0.5f;
            float ry = h * 0.40f + reach;
            float rx = w * 0.78f + reach;
            PoseStack ps = g.pose();
            ps.pushPose();
            ps.translate(cx, cy, 0f);
            ps.scale(rx / ry, 1f, 1f);
            SoftDraw.radial(g, 0f, 0f, ry, glow, Math.min(0.9f, a * 0.90f), false);
            ps.popPose();
        }
        haloRgb = low ? glow : WeaponConfig.weaponGlowColor;
        haloAlpha = WeaponConfig.ammoGlowHalo > 0 ? WeaponConfig.ammoGlowOpacity / 100f * pulse * k : 0f;
    }

    /**
     * Halo that follows the glyph outlines: the same text drawn a little off-centre in 8 directions (two rings),
     * very transparent, no shadow, underneath the real text. {@code radiusPx} is in font pixels of this text.
     */
    private void drawTextHalo(GuiGraphics g, Font font, String text, float x, float y, float scale, int rgb, float alpha,
                              int radiusPx) {
        if (alpha <= 0.004f || radiusPx <= 0 || text.isEmpty()) {
            return;
        }
        // LOW = 4 offsets, MEDIUM = 8, HIGH = 8 on two rings. All the text goes out as one batch.
        final int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
        final int count = HudConfig.glowQuality == cc.xypp.modernhud.config.GlowQuality.LOW ? 4 : 8;
        final int rings = HudConfig.glowQuality == cc.xypp.modernhud.config.GlowQuality.HIGH ? 2 : 1;
        HudBatch.run(g, () -> {
            // one push / scale for the whole halo; each copy is just a different offset (in the scaled space)
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate(x, y, 0);
            pose.scale(scale, scale, 1.0f);
            for (int ring = 1; ring <= rings; ring++) {
                float r = radiusPx * (rings == 1 ? 0.75f : (ring == 1 ? 0.5f : 1.0f));
                float a = alpha * (rings == 1 ? 0.22f : (ring == 1 ? 0.30f : 0.16f));
                int argb = (Math.min(255, Math.round(a * 255f)) << 24) | (rgb & 0xFFFFFF);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                for (int i = 0; i < count; i++) {
                    int[] d = dirs[i];
                    float diag = (d[0] != 0 && d[1] != 0) ? 0.7071f : 1f;
                    HudText.text(g, font, text, d[0] * r * diag, d[1] * r * diag, argb, false);
                }
            }
            pose.popPose();
        });
    }

    /**
     * The secondary weapon: a small, dim icon directly BELOW the primary icon box - its own strip,
     * entirely clear of the ammo/name/fire-mode text column, so it can never overlap them regardless
     * of frame size. Looks the gun's ResourceLocation up as a LIVE ItemStack in the hotbar every
     * frame (rather than caching the ItemStack itself), so ammo/skin state is never stale and a gun
     * that's been dropped/consumed/moved out of the hotbar since simply stops showing instead of
     * leaving a ghost behind.
     */
    private void drawSecondaryWeapon(GuiGraphics g, LocalPlayer player, int iconBoxX, int stripY) {
        if (secondaryGunId == null) {
            return;
        }
        ItemStack secondaryStack = findInHotbar(player.getInventory(), secondaryGunId);
        if (secondaryStack == null) {
            secondaryGunId = null; // no longer carried - forget it rather than show a ghost
            return;
        }
        drawCarriedIcon(g, player, secondaryStack, iconBoxX, stripY, WeaponConfig.secondaryAlpha);
    }

    /** One entry of the neutral-state pair — identical treatment to the normal secondary slot (same
     *  box, same dim alpha, same icon scale, same reserve readout), just driven by hotbar order
     *  instead of switch-history. */
    private void drawNeutralIcon(GuiGraphics g, LocalPlayer player, ResourceLocation gunId, int boxX, int boxY) {
        ItemStack stack = findInHotbar(player.getInventory(), gunId);
        if (stack == null) {
            return;
        }
        drawCarriedIcon(g, player, stack, boxX, boxY, WeaponConfig.secondaryAlpha);
    }

    /**
     * Icon + reserve-ammo readout for a gun that ISN'T actively being held/fired right now — the
     * shared drawing behind BOTH {@link #drawSecondaryWeapon} and {@link #drawNeutralIcon}: same
     * box, same dim treatment, same reserve-count text, just a different gun-selection source (
     * switch-history vs. hotbar order). Mirrors the reference HUD's secondary-weapon slot exactly:
     * the box is the SAME WIDTH as the primary icon box (not its own smaller width), the icon sits
     * right-aligned/toward the inside of that box, and the reserve ammo count fills the space that
     * leaves on the left — one box holding both, not the icon alone with the count elsewhere.
     */
    private void drawCarriedIcon(GuiGraphics g, LocalPlayer player, ItemStack stack, int boxX, int boxY, double alphaMul) {
        int boxW = ICON_BOX_W;
        int boxH = SECONDARY_BOX_H;

        drawSecondaryChipBox(g, boxX, boxY, boxW, boxH, alphaMul);

        float[] curColor = RenderSystem.getShaderColor();
        float[] prevColor = {curColor[0], curColor[1], curColor[2], curColor[3]}; // copy (internal array)
        try {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, (float) alphaMul);
            if (TaczCompat.hasHudTexture(stack)) {
                int[] size = TaczCompat.iconSize((float) WeaponConfig.secondaryIconScale);
                int ix = boxX + boxW - size[0] - 2; // right-aligned/inward, small inset from the box edge
                int iy = boxY + (boxH - size[1]) / 2;
                TaczCompat.renderHudTexture(g, stack, ix, iy, (float) WeaponConfig.secondaryIconScale);
            } else {
                int size = Math.min(boxH - 4, 20);
                g.renderItem(stack, boxX + boxW - size - 2, boxY + (boxH - size) / 2);
            }
        } finally {
            // Same guaranteed-restore reasoning as the fire-mode icon loop above - a missing/corrupt
            // gun HUD icon texture throwing here must never leave the dimmed alpha stuck globally.
            RenderSystem.setShaderColor(prevColor[0], prevColor[1], prevColor[2], prevColor[3]);
        }

        if (WeaponConfig.showReserveAmmo) {
            int reserve = TaczCompat.reserveAmmo(player, stack);
            String text = reserve == TaczCompat.INFINITE ? "\u221E"
                    : reserve == TaczCompat.NO_RESERVE ? "" : String.valueOf(reserve);
            if (!text.isEmpty()) {
                Font font = Minecraft.getInstance().font;
                int tx = boxX + CHIP_PAD_X; // left-aligned - the space the right-aligned icon leaves free
                int ty = boxY + (boxH - font.lineHeight) / 2;
                int a = Math.max(1, (int) Math.round(alphaMul * 255));
                int color = (a << 24) | (WeaponConfig.reserveColor & 0xFFFFFF);
                HudText.text(g, font, text, tx, ty, color, true);
            }
        }
    }

    private ItemStack findInHotbar(Inventory inventory, ResourceLocation gunId) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && gunId.equals(TaczCompat.gunId(stack))) {
                return stack;
            }
        }
        return null;
    }

    /** The widget scale snapped to whole screen pixels per font pixel (see {@link HudText}); every placement
     *  calculation uses this, so the boxes and the text always agree. */
    static float effectiveScale(GuiGraphics g) {
        return HudText.snapElement(g, WeaponConfig.scale);
    }

    /** Width of the optional gear column (0 when off); package-visible for {@link KillstreakHudOverlay}. */
    static int gearWidth() {
        return LoadoutGear.extraWidth();
    }

    /** Top-left of the widget's frame in screen px. Pure function of config + screen size (package-visible and
     *  static so {@link KillstreakHudOverlay} can find the widget's spot even on frames it draws nothing). */
    static int[] resolvePosition(int screenWidth, int screenHeight, int width, int height, double scale) {
        int scaledWidth = (int) Math.round(width * scale);
        int scaledHeight = (int) Math.round(height * scale);
        int x;
        int y;
        switch (WeaponConfig.position) {
            case TOP_LEFT -> { x = WeaponConfig.offsetX; y = WeaponConfig.offsetY; }
            case TOP_RIGHT -> { x = screenWidth - scaledWidth - WeaponConfig.offsetX; y = WeaponConfig.offsetY; }
            case BOTTOM_LEFT -> { x = WeaponConfig.offsetX; y = screenHeight - scaledHeight - WeaponConfig.offsetY; }
            case CUSTOM -> { x = WeaponConfig.offsetX; y = WeaponConfig.offsetY; }
            default -> { x = screenWidth - scaledWidth - WeaponConfig.offsetX; y = screenHeight - scaledHeight - WeaponConfig.offsetY; }
        }
        return new int[]{x, y};
    }
}

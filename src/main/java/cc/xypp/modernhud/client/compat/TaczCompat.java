package cc.xypp.modernhud.client.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunHeatData;
import com.tacz.guns.resource.pojo.data.gun.GunReloadData;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/**
 * TACZ (Timeless and Classics Zero) soft dependency: everything the weapon widget needs to read
 * from a held gun — mag ammo, reserve ammo, fire mode, and the gun's own HUD icon texture (so the
 * rendered icon is exactly what TACZ ships for that weapon, not a generic item render).
 *
 * <p>Ported from the equivalent class in the separate "CustomHUD" mod so the same numbers/logic
 * are used, but rewritten against this project's Mojang ("official") mappings instead of SRG, and
 * trimmed to only what the weapon widget actually displays.
 *
 * <p>Class-loading is safe on a TACZ-free install: like the rest of this package, this class is
 * only ever invoked after {@link #loaded()} has been checked, and {@link #loaded()} itself never
 * touches a {@code com.tacz.*} type.
 */
public final class TaczCompat {

    private static final boolean LOADED = ModList.get().isLoaded("tacz");

    /** The fixed logical canvas every TACZ HUD-icon texture is authored on. Never probe the PNG's
     *  real pixel size — TACZ itself always samples this exact 39x13 region regardless of gun. */
    private static final int CANVAS_W = 39;
    private static final int CANVAS_H = 13;

    private static IGun iGunCache;
    private static ItemStack iGunCacheStack;
    private static long iGunCacheMs = -1000L;

    private static ClientGunIndex indexCache;
    private static ItemStack indexCacheStack;
    private static long indexCacheMs = -1000L;

    public static final int NO_RESERVE = Integer.MIN_VALUE;
    public static final int INFINITE = -1;

    private static int reserveResult;
    private static ItemStack reserveResultGun;
    private static long reserveResultMs = -1000L;

    private static int invTotalCache;
    private static ItemStack invTotalGun;
    private static long invTotalMs = -1000L;

    private TaczCompat() {
    }

    public static boolean loaded() {
        return LOADED;
    }

    /**
     * TACZ ships its own built-in weapon HUD ({@code com.tacz.guns.client.gui.overlay.GunHudOverlay})
     * which renders independently of ours. Left alone, BOTH render at once — TACZ's own (large,
     * differently-positioned) gun icon overlapping our fixed frame — which is exactly the "icon bị
     * lệch/méo" mess this was reported as. TaczPresence solves this with a Mixin that cancels TACZ's
     * render() outright; we take the lighter-weight route TACZ itself exposes for this: its own
     * client render config has a "show the built-in gun HUD" toggle
     * ({@code com.tacz.guns.config.client.RenderConfig#GUN_HUD_ENABLE}) that its real overlay checks
     * at its own render() head. Flipping that off gets the same result without needing a Mixin
     * Gradle/refmap setup. Looked up by reflection (not a compile-time import) since this is an
     * internal TACZ config class, not part of its stable public API — if TACZ ever renames or
     * removes it, this becomes a harmless no-op rather than a build- or run-time break.
     *
     * @param enabled what to set TACZ's own built-in gun HUD toggle to. We call this with
     *                {@code !WeaponConfig.enabled}: our widget on -&gt; TACZ's own off, and vice versa
     *                (so turning our widget off in the config restores TACZ's default HUD instead of
     *                leaving the player with no weapon HUD at all).
     */
    public static void setVanillaGunHudEnabled(boolean enabled) {
        if (!LOADED) {
            return;
        }
        try {
            Class<?> renderConfigClass = Class.forName("com.tacz.guns.config.client.RenderConfig");
            java.lang.reflect.Field field = renderConfigClass.getField("GUN_HUD_ENABLE");
            Object configValue = field.get(null);
            if (configValue instanceof Boolean) {
                // Some installed versions may expose this as a plain static boolean rather than a
                // wrapped ForgeConfigSpec value - write the field directly in that case.
                field.set(null, enabled);
                return;
            }
            java.lang.reflect.Method setMethod = configValue.getClass().getMethod("set", Object.class);
            setMethod.invoke(configValue, enabled);
        } catch (Throwable ignored) {
            // TACZ's internal render config changed shape, or isn't present under this name in the
            // installed version - harmless no-op. Worst case both HUDs render at once again, same
            // as before this existed; nothing here is allowed to crash the mod. WeaponHudOverlay
            // re-calls this every ~1s while active (see its VANILLA_HUD_REASSERT_MS), which also
            // covers the case where something ELSE keeps flipping this flag back on after we set it.
        }
    }

    private static Boolean presenceLoadedCache;

    /** True if the separate "TaczPresence" mod is installed. Cached after the first check. */
    public static boolean presenceLoaded() {
        if (presenceLoadedCache == null) {
            try {
                presenceLoadedCache = net.minecraftforge.fml.ModList.get().isLoaded("tacz_presence");
            } catch (Throwable t) {
                presenceLoadedCache = false;
            }
        }
        return presenceLoadedCache;
    }

    /**
     * Sets TaczPresence's own "which HUD style" selector ({@code PresenceConfig.GUN_HUD_TYPE}, an
     * enum with values {@code TACZ_HUD}/{@code TACZ_P_HUD}) — the other half of making ModernHUD's
     * {@link cc.xypp.modernhud.config.WeaponHudSource} selector actually take effect: forcing
     * {@link #setVanillaGunHudEnabled} off silences both TACZ's own HUD AND TaczPresence's (they
     * share that one flag as their render gate), but only THIS also tells TaczPresence which of
     * its two styles to fall back to once ModernHUD's own widget isn't the one showing. A no-op if
     * TaczPresence isn't installed, or if its config shape doesn't match what's expected here.
     *
     * @param taczPHud true for TaczPresence's own HUD style, false for the plain TACZ_HUD style.
     */
    public static void setPresenceHudType(boolean taczPHud) {
        if (!presenceLoaded()) {
            return;
        }
        try {
            Class<?> presenceConfigClass = Class.forName("com.tacz.presence.config.PresenceConfig");
            Class<?> hudTypeClass = Class.forName("com.tacz.presence.config.PresenceConfig$HudType");
            Object targetEnumConstant = null;
            for (Object constant : hudTypeClass.getEnumConstants()) {
                String name = ((Enum<?>) constant).name();
                if (taczPHud ? name.equals("TACZ_P_HUD") : name.equals("TACZ_HUD")) {
                    targetEnumConstant = constant;
                    break;
                }
            }
            if (targetEnumConstant == null) {
                return;
            }
            java.lang.reflect.Field field = presenceConfigClass.getField("GUN_HUD_TYPE");
            Object configValue = field.get(null);
            java.lang.reflect.Method setMethod = configValue.getClass().getMethod("set", Object.class);
            setMethod.invoke(configValue, targetEnumConstant);
        } catch (Throwable ignored) {
            // Same defensive stance as setVanillaGunHudEnabled - TaczPresence changing its config
            // shape should never be able to crash this mod, just silently stop syncing with it.
        }
    }

    public static boolean isGun(ItemStack stack) {
        return LOADED && iGun(stack) != null;
    }

    private static IGun iGun(ItemStack gun) {
        long now = Util.getMillis();
        if (iGunCacheStack == gun && now - iGunCacheMs < 20L) {
            return iGunCache;
        }
        IGun g = IGun.getIGunOrNull(gun);
        iGunCache = g;
        iGunCacheStack = gun;
        iGunCacheMs = now;
        return g;
    }

    public static ResourceLocation gunId(ItemStack gun) {
        IGun iGun = iGun(gun);
        return iGun == null ? null : iGun.getGunId(gun);
    }

    /** True if this gun reads ammo straight from the inventory (no separate mag/reserve split). */
    public static boolean usesInventoryAmmo(ItemStack gun) {
        IGun iGun = iGun(gun);
        return iGun != null && iGun.useInventoryAmmo(gun);
    }

    private static ClientGunIndex clientIndex(ItemStack gun) {
        long now = Util.getMillis();
        if (indexCacheStack == gun && now - indexCacheMs < 20L) {
            return indexCache;
        }
        IGun iGun = iGun(gun);
        ClientGunIndex idx = iGun == null ? null
                : TimelessAPI.getClientGunIndex(iGun.getGunId(gun)).orElse(null);
        indexCache = idx;
        indexCacheStack = gun;
        indexCacheMs = now;
        return idx;
    }

    /**
     * Ammo shown in the mag right now — includes a chambered round, like TACZ's own HUD.
     *
     * <p>For guns that read ammo straight from the inventory ({@code useInventoryAmmo}), this IS
     * the inventory total (that's the whole point of that mode — no separate mag). BUT that total
     * has two different sources depending on {@code useDummyAmmo}, and matching TACZ's own
     * precedence here (see its {@code GunHudOverlay.handleCacheCount}) is what this method got
     * wrong: a gun can be BOTH {@code useInventoryAmmo} AND {@code useDummyAmmo} at once — a
     * virtual/NBT-tracked ammo count with no real backing item in the player's inventory at all
     * (this is exactly what let the M134 Minigun's vehicle-fed ammo show as a real "158" in TACZ's
     * own HUD, coloured cyan — TACZ's own {@code useInventoryAmmo && useDummyAmmo} colour code —
     * while this method was unconditionally re-scanning the player's personal inventory instead,
     * finding zero real ammo items there and showing "000"). {@code useDummyAmmo} must be checked
     * FIRST, before ever falling back to a real inventory scan.
     */
    public static int magAmmo(Player p, ItemStack gun) {
        IGun iGun = iGun(gun);
        if (iGun == null) {
            return 0;
        }
        if (iGun.useInventoryAmmo(gun)) {
            int total;
            if (!needCheckAmmo(p)) {
                total = 9999;
            } else if (iGun.useDummyAmmo(gun)) {
                total = iGun.getDummyAmmoAmount(gun);
            } else {
                total = inventoryAmmoTotal(p, gun);
            }
            return Math.min(9999, total + barrelBonus(gun));
        }
        return iGun.getCurrentAmmoCount(gun) + barrelBonus(gun);
    }

    private static int barrelBonus(ItemStack gun) {
        IGun iGun = iGun(gun);
        if (iGun == null || !iGun.hasBulletInBarrel(gun)) {
            return 0;
        }
        ClientGunIndex index = clientIndex(gun);
        if (index == null || index.getGunData() == null) {
            return 1;
        }
        return index.getGunData().getBolt() == Bolt.OPEN_BOLT ? 0 : 1;
    }

    /**
     * Whether THIS gun even has TACZ's opt-in heat/overheat mechanic at all (most guns don't - it's
     * set per-gun in the gun pack's own data file, e.g. the Minigun). Widget code should gate a heat
     * bar on this, not just on {@link #heatFraction} being non-zero, since 0 is also the ordinary
     * "not heated up yet" reading for a gun WITH the mechanic.
     */
    public static boolean hasHeat(ItemStack gun) {
        ClientGunIndex index = clientIndex(gun);
        return index != null && index.getGunData() != null && index.getGunData().hasHeatData();
    }

    /**
     * Overheat progress as a 0..1 fraction, for guns with {@link #hasHeat}. TACZ's own raw
     * {@link IGun#getHeatAmount} is 0..heatMax rather than already 0..1 — heatMax defaults to 100
     * but is configurable per-gun in its data file (see {@link GunHeatData#getHeatMax()}), so this
     * normalises against the ACTUAL configured max rather than assuming the default. Returns 0 for
     * a gun with no heat data at all, so callers don't need a separate null-check before reading it.
     */
    public static float heatFraction(ItemStack gun) {
        IGun iGun = iGun(gun);
        ClientGunIndex index = clientIndex(gun);
        if (iGun == null || index == null || index.getGunData() == null) {
            return 0f;
        }
        GunData data = index.getGunData();
        GunHeatData heatData = data.getHeatData();
        if (!data.hasHeatData() || heatData == null || heatData.getHeatMax() <= 0f) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, iGun.getHeatAmount(gun) / heatData.getHeatMax()));
    }

    /** True while the gun is fully overheated and locked from firing until it cools back down. */
    public static boolean isOverheatLocked(ItemStack gun) {
        IGun iGun = iGun(gun);
        return iGun != null && iGun.isOverheatLocked(gun);
    }

    public static int magCapacity(ItemStack gun) {
        ClientGunIndex index = clientIndex(gun);
        if (index == null) {
            return -1;
        }
        GunData data = index.getGunData();
        if (data == null) {
            return -1;
        }
        return AttachmentDataUtils.getAmmoCountWithAttachment(gun, data);
    }

    /**
     * {@code {totalSlots, filledSlots}} for this gun's attachment slots — iterates every real
     * {@link AttachmentType} (SCOPE/MUZZLE/STOCK/GRIP/LASER/EXTENDED_MAG, skipping the sentinel
     * {@code NONE}) this specific gun ALLOWS at all ({@code IGun#allowAttachmentType}, since not
     * every gun has every slot type), and of those, how many currently have something installed —
     * checked via {@link DefaultAssets#isEmptyAttachmentId}, NOT a null check: {@code getAttachmentId}
     * is {@code @Nonnull} and returns {@code DefaultAssets.EMPTY_ATTACHMENT_ID} for an empty slot
     * rather than null. All public TACZ API ({@code com.tacz.guns.api.*}), same stability guarantee
     * as the rest of this class's calls.
     */
    public static int[] attachmentSlotCounts(ItemStack gun) {
        IGun iGun = iGun(gun);
        if (iGun == null) {
            return new int[]{0, 0};
        }
        int total = 0;
        int filled = 0;
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) {
                continue;
            }
            if (!iGun.allowAttachmentType(gun, type)) {
                continue;
            }
            total++;
            // getAttachmentId is @Nonnull in TACZ's own API - it NEVER returns null, it returns the
            // sentinel DefaultAssets.EMPTY_ATTACHMENT_ID for an empty slot instead (confirmed
            // directly against TACZ's own source, where every internal "is this slot actually
            // empty" check uses this exact comparison, e.g. FirstPersonRenderGunEvent's scope
            // check). A plain `!= null` check here was always true regardless of whether anything
            // was really equipped, which is why every slot a gun merely ALLOWED was showing up
            // filled even on a completely bare gun.
            if (!DefaultAssets.isEmptyAttachmentId(iGun.getAttachmentId(gun, type))) {
                filled++;
            }
        }
        return new int[]{total, filled};
    }

    /** Reserve ammo count, {@link #INFINITE} for unlimited, or {@link #NO_RESERVE} when not applicable. */
    public static int reserveAmmo(Player p, ItemStack gun) {
        IGun iGun = iGun(gun);
        if (iGun == null) {
            return 0;
        }
        long now = Util.getMillis();
        if (reserveResultGun == gun && now - reserveResultMs < 50L) {
            return reserveResult;
        }
        reserveResultGun = gun;
        reserveResultMs = now;
        reserveResult = computeReserve(p, gun, iGun);
        return reserveResult;
    }

    private static int computeReserve(Player p, ItemStack gun, IGun iGun) {
        if (iGun.useInventoryAmmo(gun)) {
            return NO_RESERVE;
        }
        if (isReloadInfinite(gun)) {
            return INFINITE;
        }
        if (iGun.useDummyAmmo(gun)) {
            return iGun.getDummyAmmoAmount(gun);
        }
        if (!needCheckAmmo(p)) {
            return 9999;
        }
        return inventoryAmmoTotal(p, gun);
    }

    private static boolean isReloadInfinite(ItemStack gun) {
        ClientGunIndex index = clientIndex(gun);
        if (index == null || index.getGunData() == null) {
            return false;
        }
        GunReloadData rd = index.getGunData().getReloadData();
        return rd != null && rd.isInfinite();
    }

    private static boolean needCheckAmmo(Player p) {
        try {
            IGunOperator op = IGunOperator.fromLivingEntity((LivingEntity) p);
            return op == null || op.needCheckAmmo();
        } catch (Throwable t) {
            return true;
        }
    }

    /**
     * Total loose/boxed ammo compatible with this gun, found anywhere in the player's full
     * inventory (not just the hotbar — matches TaczPresence's own scan range). The bug this fixes:
     * an earlier version compared each ammo item's OWN {@code getAmmoId()} against the gun's
     * expected ammo id directly with {@code equals()} — but that's not the contract TACZ guns
     * actually use to decide compatibility (caliber aliasing, etc. can make that comparison miss
     * real matches). {@link IAmmo#isAmmoOfGun} is TACZ's own compatibility check, the same one
     * TaczPresence's {@code AmmoCacheCounter} uses — matching it fixed guns reading "000" ammo
     * despite a full inventory of the right ammo.
     */
    private static int inventoryAmmoTotal(Player p, ItemStack gun) {
        long now = Util.getMillis();
        if (invTotalGun == gun && now - invTotalMs < 50L) {
            return invTotalCache;
        }
        int n = 0;
        int size = p.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            Item item = s.getItem();
            if (item instanceof IAmmoBox box && box.isAmmoBoxOfGun(gun, s)) {
                if (box.isAllTypeCreative(s) || box.isCreative(s)) {
                    invTotalCache = 9999;
                    invTotalGun = gun;
                    invTotalMs = now;
                    return 9999;
                }
                n += box.getAmmoCount(s);
                continue;
            }
            if (item instanceof IAmmo ammo && ammo.isAmmoOfGun(gun, s)) {
                n += s.getCount();
            }
        }
        invTotalCache = n;
        invTotalGun = gun;
        invTotalMs = now;
        return n;
    }

    public static String fireMode(ItemStack gun) {
        IGun iGun = iGun(gun);
        if (iGun == null) {
            return "";
        }
        FireMode mode = iGun.getFireMode(gun);
        return mode == null ? "" : mode.name();
    }

    /**
     * Every fire mode this specific gun supports (as TACZ reports them, e.g. "SEMI", "AUTO"),
     * in TACZ's own order — empty if that data isn't available. Used to decide whether to show
     * the fire-mode row at all: a gun with only one supported mode (or none reported) shouldn't
     * show a redundant "this is your only option" readout.
     *
     * <p>Filters out any null entry (e.g. a mode name in the gun's data JSON that failed to parse
     * to a real {@link FireMode} constant) and deduplicates by name, both while preserving TACZ's
     * own relative order — a null/duplicate placeholder must never itself count as a second,
     * genuinely selectable mode, or a gun with exactly one real mode would wrongly show the
     * fire-mode row as if there were a real choice to make.
     */
    public static String[] supportedFireModes(ItemStack gun) {
        ClientGunIndex index = clientIndex(gun);
        if (index == null || index.getGunData() == null) {
            return new String[0];
        }
        java.util.List<FireMode> modes = index.getGunData().getFireModeSet();
        if (modes == null || modes.isEmpty()) {
            return new String[0];
        }
        java.util.LinkedHashSet<String> distinct = new java.util.LinkedHashSet<>();
        for (FireMode m : modes) {
            if (m != null) {
                distinct.add(m.name());
            }
        }
        return distinct.toArray(new String[0]);
    }

    /** True if TACZ has a HUD icon texture registered for this gun (no drawing, no GL calls). */
    public static boolean hasHudTexture(ItemStack gun) {
        ClientGunIndex index = clientIndex(gun);
        if (index == null) {
            return false;
        }
        GunDisplayInstance display = index.getDefaultDisplay();
        return display != null && display.getHUDTexture() != null;
    }

    /** The {@code {width, height}} the icon would be drawn at for a given scale — pure arithmetic
     *  on the fixed 39x13 canvas, no lookups, safe to call before deciding where to draw it. */
    public static int[] iconSize(float scale) {
        return new int[]{Math.round(CANVAS_W * scale), Math.round(CANVAS_H * scale)};
    }

    /**
     * Draws TACZ's own HUD icon texture for this gun at the given top-left position. TACZ HUD-icon
     * art is always authored on a fixed 39x13 logical canvas (confirmed against TACZ's own vanilla
     * HUD renderer and the TaczPresence mod's {@code ImmersiveGunHudOverlay}, both of which blit
     * HUD icons at a literal {@code 39, 13, 39, 13} — never derived from the PNG file's real pixel
     * dimensions). Reading the file's actual size and scaling from that (an earlier version of this
     * method did) samples the texture with the wrong UV rectangle and comes out warped/stretched —
     * this fixed-canvas approach is the one that actually matches what TACZ ships, for every gun,
     * regardless of that gun's icon art being wide (rifles) or narrow/tall (launchers like the
     * Javelin). Call {@link #hasHudTexture} / {@link #iconSize} first to lay out around it; this
     * method only draws and assumes the caller already knows it has something to draw.
     */
    public static void renderHudTexture(GuiGraphics g, ItemStack gun, int x, int y, float scale) {
        ClientGunIndex index = clientIndex(gun);
        if (index == null) {
            return;
        }
        GunDisplayInstance display = index.getDefaultDisplay();
        if (display == null) {
            return;
        }
        ResourceLocation tex = display.getHUDTexture();
        if (tex == null) {
            return;
        }
        RenderSystem.enableBlend();
        try {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            PoseStack pose = g.pose();
            pose.pushPose();
            try {
                pose.translate(x, y, 0);
                pose.scale(scale, scale, 1.0f);
                g.blit(tex, 0, 0, 0, 0, CANVAS_W, CANVAS_H, CANVAS_W, CANVAS_H);
            } finally {
                // GUARANTEED pop even if blit() throws (missing/corrupt HUD texture) - an unpopped
                // pose otherwise misaligns every OTHER draw call for the rest of the frame, which
                // reads as far more broken than just this one icon failing to show.
                pose.popPose();
            }
        } finally {
            RenderSystem.disableBlend();
        }
    }
}

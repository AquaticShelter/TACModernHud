package cc.xypp.modernhud.client.compat;

import cc.xypp.modernhud.ModernHud;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.lang.reflect.Method;

/**
 * Soft/reflective bridge to ModernLoadout's killstreak tracker (mod id {@code mwloadout}). ModernLoadout
 * stays the single source of truth - it already tracks kills, Hardline/Killchain thresholds, hostile-mob
 * fractions, Specialist stages and what is earned, and pushes all of it to the client in one packet. This
 * class only reads that state through its small public client API, {@code dev.szo9k4.mwloadout.api.StreakApi}
 * (plain primitives/arrays/String/ItemStack), so ModernHud needs no compile-time dependency on it, and a
 * ModernLoadout build without that class simply leaves everything disabled (its own bar keeps working).
 *
 * <p>Polling model: {@link #refresh()} is called once per frame; it only does reflective calls when
 * {@code StreakApi.version()} changed (a progress packet arrived) - plus a cheap, throttled re-read of the
 * "reward still carried" flags, which follow the inventory rather than the packet.
 */
public final class ModernLoadoutCompat {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean LOADED = ModList.get().isLoaded("mwloadout");
    private static final long HELD_POLL_MS = 250L;

    private static boolean bindAttempted;
    private static boolean bound;
    private static Method mVersion, mActive, mSpecialist, mProgress, mKills, mIds, mEarned, mHeld,
            mIcon, mTexture, mLabel, mSetExternal;

    private static long lastVersion = Long.MIN_VALUE;
    private static long lastHeldPollMs;
    private static boolean externalApplied;

    // ---- current snapshot (read-only for callers; arrays are replaced, never mutated in place) ----
    private static boolean active;
    private static boolean specialist;
    private static float progress;
    private static int[] kills = new int[0];
    private static String[] ids = new String[0];
    private static boolean[] earned = new boolean[0];
    private static boolean[] held = new boolean[0];
    private static ItemStack[] icons = new ItemStack[0];
    private static ResourceLocation[] textures = new ResourceLocation[0];
    private static String[] labels = new String[0];
    /** Wall-clock ms at which each rung became earned (0 = not recently) - drives the "just unlocked" flash. */
    private static long[] earnedAtMs = new long[0];
    private static long lastChangeMs;
    /** Identity of the current ladder (mode + thresholds + reward/perk ids): changes only when the LOADOUT changes
     *  it, not on kills or deaths. {@link #selectionStartMs()} is when it last changed (the "selected" display). */
    private static String ladderKey = "";
    private static long selectionStartMs;

    private ModernLoadoutCompat() {
    }

    public static boolean loaded() {
        return LOADED;
    }

    /** True when ModernLoadout is installed AND exposes the streak API this bridge needs. */
    public static boolean available() {
        if (!LOADED) {
            return false;
        }
        if (!bindAttempted) {
            bindAttempted = true;
            try {
                Class<?> api = Class.forName("dev.szo9k4.mwloadout.api.StreakApi");
                mVersion = api.getMethod("version");
                mActive = api.getMethod("active");
                mSpecialist = api.getMethod("specialist");
                mProgress = api.getMethod("progress");
                mKills = api.getMethod("kills");
                mIds = api.getMethod("ids");
                mEarned = api.getMethod("earned");
                mHeld = api.getMethod("held");
                mIcon = api.getMethod("icon", int.class);
                mTexture = api.getMethod("texture", int.class);
                mLabel = api.getMethod("label", int.class);
                mSetExternal = api.getMethod("setExternalHud", boolean.class);
                bound = true;
            } catch (Throwable t) {
                LOGGER.warn("[{}] ModernLoadout is installed but has no streak API (needs a newer build) - "
                        + "killstreak HUD compat disabled: {}", ModernHud.MOD_ID, t.toString());
                bound = false;
            }
        }
        return bound;
    }

    /** Tell ModernLoadout whether ModernHud is drawing the tracker (its own bar then stays hidden). */
    public static void setExternalHud(boolean value) {
        if (!available() || externalApplied == value) {
            return;
        }
        try {
            mSetExternal.invoke(null, value);
            externalApplied = value;
        } catch (Throwable t) {
            bound = false; // API misbehaving - stop touching it
            LOGGER.warn("[{}] ModernLoadout streak API call failed, disabling compat: {}", ModernHud.MOD_ID, t.toString());
        }
    }

    /**
     * Re-reads the tracker state if ModernLoadout reported a change since the last call.
     * @return false when there is nothing to draw (not installed / API missing / killstreaks off)
     */
    public static boolean refresh() {
        if (!available()) {
            return false;
        }
        try {
            long now = System.currentTimeMillis();
            long version = (Long) mVersion.invoke(null);
            if (version != lastVersion) {
                lastVersion = version;
                readSnapshot(now);
            } else if (active && !specialist && now - lastHeldPollMs >= HELD_POLL_MS) {
                lastHeldPollMs = now;
                held = (boolean[]) mHeld.invoke(null);
            }
            return active && kills.length > 0;
        } catch (Throwable t) {
            bound = false;
            LOGGER.warn("[{}] ModernLoadout streak API read failed, disabling compat: {}", ModernHud.MOD_ID, t.toString());
            return false;
        }
    }

    private static void readSnapshot(long now) throws ReflectiveOperationException {
        boolean wasSpecialist = specialist;
        boolean[] oldEarned = earned;
        float oldProgress = progress;

        active = (Boolean) mActive.invoke(null);
        specialist = (Boolean) mSpecialist.invoke(null);
        progress = (Float) mProgress.invoke(null);
        kills = ((int[]) mKills.invoke(null)).clone();
        ids = ((String[]) mIds.invoke(null)).clone();
        earned = ((boolean[]) mEarned.invoke(null)).clone();
        held = specialist ? new boolean[kills.length] : (boolean[]) mHeld.invoke(null);
        lastHeldPollMs = now;

        int n = kills.length;
        icons = new ItemStack[n];
        textures = new ResourceLocation[n];
        labels = new String[n];
        long[] oldEarnedAt = earnedAtMs;
        earnedAtMs = new long[n];
        for (int i = 0; i < n; i++) {
            icons[i] = (ItemStack) mIcon.invoke(null, i);
            String tex = (String) mTexture.invoke(null, i);
            textures[i] = tex == null || tex.isEmpty() ? null : ResourceLocation.tryParse(tex);
            labels[i] = (String) mLabel.invoke(null, i);
            boolean sameLadder = wasSpecialist == specialist && oldEarned.length == n;
            if (earned[i] && sameLadder && !oldEarned[i]) {
                earnedAtMs[i] = now; // newly unlocked on this update
            } else if (sameLadder && i < oldEarnedAt.length) {
                earnedAtMs[i] = oldEarnedAt[i];
            }
        }
        if (active && n > 0) {
            String key = specialist + "|" + java.util.Arrays.toString(kills) + "|" + String.join(",", ids);
            if (!key.equals(ladderKey)) {
                ladderKey = key;
                selectionStartMs = now;
            }
        }
        if (Math.abs(progress - oldProgress) > 0.01f || oldEarned.length != n) {
            lastChangeMs = now;
        }
        for (int i = 0; i < Math.min(oldEarned.length, n); i++) {
            if (earned[i] != oldEarned[i]) {
                lastChangeMs = now;
            }
        }
    }

    // ================================================================ selected perks (toast)

    private static final long PERK_POLL_MS = 250L;
    private static boolean perkBindAttempted;
    private static boolean perkBound;
    private static Method mPerkVersion, mPerkIds, mPerkLabels, mPerkTextures;
    private static long lastPerkVersion = Long.MIN_VALUE;
    private static long lastPerkPollMs;
    private static String[] perkIds = new String[0];
    private static String[] perkLabels = new String[0];
    private static ResourceLocation[] perkTextures = new ResourceLocation[0];
    private static long perkSelectionStartMs;

    /** True when ModernLoadout exposes the selected-perks part of its API (newer than the streak part). */
    public static boolean perksAvailable() {
        if (!LOADED) {
            return false;
        }
        if (!perkBindAttempted) {
            perkBindAttempted = true;
            try {
                Class<?> api = Class.forName("dev.szo9k4.mwloadout.api.StreakApi");
                mPerkVersion = api.getMethod("selectedPerksVersion");
                mPerkIds = api.getMethod("selectedPerkIds");
                mPerkLabels = api.getMethod("selectedPerkLabels");
                mPerkTextures = api.getMethod("selectedPerkTextures");
                perkBound = true;
            } catch (Throwable t) {
                LOGGER.warn("[{}] ModernLoadout has no selected-perks API (needs a newer build) - perk toast disabled: {}",
                        ModernHud.MOD_ID, t.toString());
                perkBound = false;
            }
        }
        return perkBound;
    }

    /**
     * Re-reads the player's selected perks when ModernLoadout reports they changed (polled a few times a second).
     * {@link #perkSelectionStartMs()} is when that last happened - the toast's start. Returns whether there is
     * anything selected to show.
     */
    public static boolean refreshPerks() {
        if (!perksAvailable()) {
            return false;
        }
        try {
            long now = System.currentTimeMillis();
            if (now - lastPerkPollMs >= PERK_POLL_MS) {
                lastPerkPollMs = now;
                long version = (Long) mPerkVersion.invoke(null);
                if (version != lastPerkVersion) {
                    lastPerkVersion = version;
                    perkIds = ((String[]) mPerkIds.invoke(null)).clone();
                    perkLabels = ((String[]) mPerkLabels.invoke(null)).clone();
                    String[] tex = (String[]) mPerkTextures.invoke(null);
                    perkTextures = new ResourceLocation[tex.length];
                    for (int i = 0; i < tex.length; i++) {
                        perkTextures[i] = tex[i] == null || tex[i].isEmpty() ? null : ResourceLocation.tryParse(tex[i]);
                    }
                    if (perkIds.length > 0) {
                        perkSelectionStartMs = now;
                    }
                }
            }
            return perkIds.length > 0;
        } catch (Throwable t) {
            perkBound = false;
            LOGGER.warn("[{}] ModernLoadout perk API read failed, disabling perk toast: {}", ModernHud.MOD_ID, t.toString());
            return false;
        }
    }

    public static int perkCount() { return perkIds.length; }
    public static String perkLabel(int i) { return perkLabels[i] == null ? "" : perkLabels[i]; }
    public static ResourceLocation perkTexture(int i) { return perkTextures[i]; }
    public static long perkSelectionStartMs() { return perkSelectionStartMs; }

    // ---- snapshot accessors ----
    public static boolean isSpecialist() { return specialist; }
    public static float progress() { return progress; }
    public static int rungCount() { return kills.length; }
    public static int kills(int i) { return kills[i]; }
    public static boolean earned(int i) { return earned[i]; }
    public static boolean held(int i) { return i < held.length && held[i]; }
    public static ItemStack icon(int i) { return icons[i] == null ? ItemStack.EMPTY : icons[i]; }
    public static ResourceLocation texture(int i) { return textures[i]; }
    public static String label(int i) { return labels[i] == null ? "" : labels[i]; }
    public static long earnedAtMs(int i) { return earnedAtMs[i]; }
    public static long lastChangeMs() { return lastChangeMs; }
    public static String ladderKey() { return ladderKey; }
    public static long selectionStartMs() { return selectionStartMs; }
    public static boolean anyReady() {
        if (specialist) {
            return false;
        }
        for (int i = 0; i < kills.length; i++) {
            if (earned[i] && held(i)) {
                return true;
            }
        }
        return false;
    }
}

package cc.xypp.modernhud.sound;

import cc.xypp.modernhud.config.CommonConfig;
import cc.xypp.modernhud.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side decision for the attacker's hit / kill / headshot cues (the shield cues are decided in
 * {@link cc.xypp.modernhud.CommonEvents}). Priority, per hit:
 * <ol>
 *   <li>a kill: the kill / headshot-kill cue (the shield cue of the same hit is muted),</li>
 *   <li>the victim still had a shield: the SHIELD cue (held = shield hit, drained = shield break) replaces the
 *       ordinary hit cue,</li>
 *   <li>no shield (never had one, or already gone): the ordinary hit cue.</li>
 * </ol>
 */
public final class CombatSounds {

    /** What the shield did on the most recent hit the attacker landed on this victim. */
    private record ShieldNote(long tick, boolean broken) {
    }

    private static final Map<LivingEntity, ShieldNote> SHIELD = new WeakHashMap<>();
    private static final Map<UUID, Long> LAST_HIT_MS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_KILL_MS = new ConcurrentHashMap<>();

    private CombatSounds() {
    }

    /** Called by the shield detection when a hit consumed (part of) the victim's absorption and a shield cue was sent. */
    public static void noteShield(LivingEntity victim, boolean broken) {
        synchronized (SHIELD) {
            SHIELD.put(victim, new ShieldNote(victim.level().getGameTime(), broken));
        }
    }

    /** A non-fatal hit by {@code attacker} on {@code victim}. */
    public static void onHit(ServerPlayer attacker, Entity victim) {
        if (!CommonConfig.combatSoundsEnabled || !CommonConfig.hitSoundEnabled) {
            return;
        }
        if (CommonConfig.shieldSoundPriority && victim instanceof LivingEntity living) {
            ShieldNote note;
            synchronized (SHIELD) {
                note = SHIELD.get(living);
            }
            if (note != null && note.tick() == living.level().getGameTime()) {
                return; // the shield cue already covers this hit; the ordinary hit cue only counts once the shield is gone
            }
        }
        if (onCooldown(LAST_HIT_MS, attacker.getUUID(), CommonConfig.hitSoundCooldownMs)) {
            return;
        }
        send(attacker, CombatSoundType.HIT);
    }

    /** A kill by {@code attacker}. */
    public static void onKill(ServerPlayer attacker, boolean headshot) {
        if (!CommonConfig.combatSoundsEnabled) {
            return;
        }
        if (headshot ? !CommonConfig.headshotKillSoundEnabled : !CommonConfig.killSoundEnabled) {
            return;
        }
        if (onCooldown(LAST_KILL_MS, attacker.getUUID(), CommonConfig.killSoundCooldownMs)) {
            return;
        }
        send(attacker, headshot ? CombatSoundType.HEADSHOT_KILL : CombatSoundType.KILL);
    }

    private static boolean onCooldown(Map<UUID, Long> last, UUID id, int cooldownMs) {
        if (cooldownMs <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long prev = last.get(id);
        if (prev != null && now - prev < cooldownMs) {
            return true;
        }
        last.put(id, now);
        return false;
    }

    private static void send(ServerPlayer player, CombatSoundType type) {
        ModNetworking.sendTo(player, new CombatSoundPacket(type));
    }

    /** Player logged out: drop their cooldown entries. */
    public static void forgetPlayer(UUID id) {
        LAST_HIT_MS.remove(id);
        LAST_KILL_MS.remove(id);
    }
}

package cc.xypp.modernhud.sound;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

/**
 * Hooks TACZ's own gun events (EntityHurtByGunEvent.Post = non-fatal hit, EntityKillByGunEvent = kill, which also
 * carries the headshot flag) through reflection, so there is no compile-time dependency. If TACZ is missing or renames
 * these methods this just logs once and stays silent - it never crashes the game.
 */
public final class TaczCombatBridge {

    private static final Logger LOGGER = LogManager.getLogger("ModernHud/CombatSounds");
    private static final String KILL_EVENT = "com.tacz.guns.api.event.common.EntityKillByGunEvent";
    private static final String HURT_POST_EVENT = "com.tacz.guns.api.event.common.EntityHurtByGunEvent$Post";

    private static Method killGetKilled;
    private static Method killGetAttacker;
    private static Method killIsHeadShot;
    private static Method hurtGetHurt;
    private static Method hurtGetAttacker;
    private static boolean active;

    private TaczCombatBridge() {
    }

    public static void init() {
        if (!ModList.get().isLoaded("tacz")) {
            return;
        }
        try {
            Class<?> killClass = Class.forName(KILL_EVENT);
            Class<?> hurtClass = Class.forName(HURT_POST_EVENT);
            killGetKilled = killClass.getMethod("getKilledEntity");
            killGetAttacker = killClass.getMethod("getAttacker");
            killIsHeadShot = killClass.getMethod("isHeadShot");
            hurtGetHurt = hurtClass.getMethod("getHurtEntity");
            hurtGetAttacker = hurtClass.getMethod("getAttacker");

            @SuppressWarnings("unchecked")
            Class<? extends Event> killType = (Class<? extends Event>) killClass;
            @SuppressWarnings("unchecked")
            Class<? extends Event> hurtType = (Class<? extends Event>) hurtClass;
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, killType, TaczCombatBridge::onGunKill);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, hurtType, TaczCombatBridge::onGunHurt);
            active = true;
            LOGGER.info("TACZ detected - combat sounds hooked into its gun hit/kill events.");
        } catch (Throwable t) {
            LOGGER.warn("TACZ is installed but its event API did not match; TACZ hit/kill/headshot sounds are disabled. Cause: {}", t.toString());
        }
    }

    public static boolean isActive() {
        return active;
    }

    private static void onGunKill(Event event) {
        try {
            Object killed = killGetKilled.invoke(event);
            Object attacker = killGetAttacker.invoke(event);
            boolean headshot = Boolean.TRUE.equals(killIsHeadShot.invoke(event));
            if (!(killed instanceof LivingEntity victim) || !(attacker instanceof ServerPlayer shooter)) {
                return;
            }
            if (victim.level().isClientSide || victim == shooter) {
                return;
            }
            CombatSounds.onKill(shooter, headshot);
        } catch (Exception e) {
            LOGGER.debug("Failed to read TACZ kill event", e);
        }
    }

    private static void onGunHurt(Event event) {
        try {
            Object hurt = hurtGetHurt.invoke(event);
            Object attacker = hurtGetAttacker.invoke(event);
            if (!(hurt instanceof Entity victim) || !(attacker instanceof ServerPlayer shooter)) {
                return;
            }
            if (victim.level().isClientSide || victim == shooter) {
                return;
            }
            CombatSounds.onHit(shooter, victim);
        } catch (Exception e) {
            LOGGER.debug("Failed to read TACZ hurt event", e);
        }
    }
}

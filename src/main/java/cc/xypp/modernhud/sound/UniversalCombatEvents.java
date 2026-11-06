package cc.xypp.modernhud.sound;

import cc.xypp.modernhud.ModernHud;
import cc.xypp.modernhud.config.CommonConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hit / kill cues for ANY damage a player deals (melee, bows, SuperbWarfare, other weapon mods...). TACZ damage is skipped
 * here because {@link TaczCombatBridge} reports it with the headshot flag. Runs at LOW priority so the shield detection in
 * {@link cc.xypp.modernhud.CommonEvents} has already noted what the shield did on this very hit.
 */
@Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class UniversalCombatEvents {

    private UniversalCombatEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamage(LivingDamageEvent event) {
        if (CommonConfig.restrictCombatSoundsToTacz) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || event.getAmount() <= 0) {
            return;
        }
        DamageSource source = event.getSource();
        if (isTaczDamage(source) || event.getAmount() >= victim.getHealth()) {
            return; // TACZ path handles TACZ; the killing blow is handled by onDeath
        }
        ServerPlayer attacker = attacker(source);
        if (attacker != null && attacker != victim) {
            CombatSounds.onHit(attacker, victim);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent event) {
        if (CommonConfig.restrictCombatSoundsToTacz) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        DamageSource source = event.getSource();
        if (isTaczDamage(source)) {
            return;
        }
        ServerPlayer attacker = attacker(source);
        if (attacker != null && attacker != victim) {
            CombatSounds.onKill(attacker, false); // headshots can only come from TACZ
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CombatSounds.forgetPlayer(event.getEntity().getUUID());
    }

    private static boolean isTaczDamage(DamageSource source) {
        return isTacz(source.getDirectEntity()) || isTacz(source.getEntity());
    }

    private static boolean isTacz(Entity e) {
        return e != null && e.getClass().getName().startsWith("com.tacz.");
    }

    private static ServerPlayer attacker(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer p) {
            return p;
        }
        if (source.getDirectEntity() instanceof Projectile proj && proj.getOwner() instanceof ServerPlayer p) {
            return p;
        }
        return null;
    }
}

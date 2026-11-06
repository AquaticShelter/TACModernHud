package cc.xypp.modernhud;

import cc.xypp.modernhud.client.HitIndicatorType;
import cc.xypp.modernhud.client.HitMarkerType;
import cc.xypp.modernhud.network.AddHitIndicatorPacket;
import cc.xypp.modernhud.network.ModNetworking;
import cc.xypp.modernhud.network.SetHitMarkerPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

/**
 * Server-authoritative detection for directional hit indication - integrated from a standalone
 * "Hit Indication" mod (rosymaple). Runs alongside {@link CommonEvents} (separate concern: this
 * is "where did that hit come from", CommonEvents is "did it break your Absorption") on the same
 * FORGE event bus - both subscribe to {@code LivingDamageEvent} independently with no conflict,
 * same as any two Forge listeners for the same event type.
 *
 * <p>Deobfuscated/ported from the original mod's compiled jar (no source was available) - the
 * detection logic itself (which events fire an indicator, the magic-absorb damage math for potion
 * splash) is unchanged from the original.
 */
@Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HitIndicationEvents {

    @SubscribeEvent
    public static void onAttack(final LivingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof ThrownPotion) {
            return; // potion splash damage is handled separately, in onPotion below
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attackerEntity)
                || attackerEntity.getUUID().equals(event.getEntity().getUUID())) {
            // No living attacker (or you damaged yourself) - only a non-directional indicator makes
            // sense here, and only for the victim if it's a player.
            if (!(event.getEntity() instanceof ServerPlayer victim)) {
                return;
            }
            int damagePercent = (int) Math.floor(event.getAmount() / victim.getMaxHealth() * 100.0f);
            sendIndicator(victim, null, HitIndicatorType.ND_HIT, damagePercent, false);
            return;
        }

        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && event.getSource().getDirectEntity() instanceof Projectile) {
            sendMarker(attacker, HitMarkerType.CRIT);
        }
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        int damagePercent = (int) Math.floor(event.getAmount() / victim.getMaxHealth() * 100.0f);
        sendIndicator(victim, attackerEntity, HitIndicatorType.HIT, damagePercent, false);
    }

    @SubscribeEvent
    public static void onCriticalHit(final CriticalHitEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.isVanillaCritical()) {
            return;
        }
        sendMarker(player, HitMarkerType.CRIT);
    }

    @SubscribeEvent
    public static void onKill(final LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        if (killer.getUUID().equals(event.getEntity().getUUID())) {
            return; // don't flash a kill marker for your own death
        }
        sendMarker(killer, HitMarkerType.KILL);
    }

    @SubscribeEvent
    public static void onBlock(final LivingAttackEvent event) {
        if (event.getSource().getDirectEntity() instanceof ThrownPotion) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity)) {
            return;
        }

        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            LivingEntity target = event.getEntity();
            boolean targetIsBlocking = target.isDamageSourceBlocked(event.getSource());
            boolean shieldAboutToBreak = attacker.getMainHandItem().getItem()
                    .canDisableShield(attacker.getMainHandItem(), target.getMainHandItem(), target, attacker);
            if (targetIsBlocking && shieldAboutToBreak) {
                sendMarker(attacker, HitMarkerType.CRIT);
            }
        }

        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        LivingEntity attackerEntity = (LivingEntity) event.getSource().getEntity();
        boolean victimIsBlocking = victim.isDamageSourceBlocked(event.getSource());
        boolean shieldAboutToBreak = attackerEntity.getMainHandItem().getItem().canDisableShield(
                attackerEntity.getMainHandItem(), victim.getMainHandItem(), victim, attackerEntity);
        if (victimIsBlocking) {
            sendIndicator(victim, attackerEntity, HitIndicatorType.BLOCK, shieldAboutToBreak ? 125 : 0, false);
        }
    }

    @SubscribeEvent
    public static void onPotion(final ProjectileImpactEvent event) {
        if (!(event.getProjectile().getOwner() instanceof LivingEntity owner)
                || !(owner.level() instanceof ServerLevel)
                || !(event.getProjectile() instanceof ThrownPotion potion)) {
            return;
        }
        AABB area = event.getProjectile().getBoundingBox().inflate(4.0, 2.0, 4.0);
        List<ServerPlayer> nearby = event.getProjectile().level().getEntitiesOfClass(ServerPlayer.class, area);

        List<MobEffectInstance> effects = PotionUtils.getMobEffects(potion.getItem());
        boolean hasNegativeEffects = effects.stream().anyMatch(e -> !e.getEffect().isBeneficial());
        boolean damagingPotion = effects.stream().anyMatch(e ->
                e.getEffect() == MobEffects.HARM || e.getEffect() == MobEffects.POISON || e.getEffect() == MobEffects.WITHER);
        Optional<MobEffectInstance> instantDamage = effects.stream()
                .filter(e -> e.getEffect() == MobEffects.HARM)
                .findFirst();

        for (ServerPlayer player : nearby) {
            if (!player.isAlive() || player.getUUID().equals(owner.getUUID())
                    || (!damagingPotion && !hasNegativeEffects)) {
                continue;
            }
            int damagePercent = 0;
            if (instantDamage.isPresent()) {
                float rawDamage = 3 * (2 << instantDamage.get().getAmplifier());
                float afterAbsorb = getDamageAfterMagicAbsorb(player, player.level().damageSources().magic(), rawDamage);
                damagePercent = (int) Math.floor(afterAbsorb / player.getMaxHealth() * 100.0f);
            }
            sendIndicator(player, owner, HitIndicatorType.HIT, damagePercent, hasNegativeEffects && !damagingPotion);
        }
    }

    private static float getDamageAfterMagicAbsorb(ServerPlayer player, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_EFFECTS)) {
            return amount;
        }
        if (player.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            int resistanceLevel = (player.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
            int remainingPercent = 25 - resistanceLevel;
            amount = Math.max(amount * remainingPercent / 25.0f, 0.0f);
        }
        if (amount <= 0.0f) {
            return 0.0f;
        }
        if (source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            return amount;
        }
        int protection = EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source);
        if (protection > 0) {
            amount = CombatRules.getDamageAfterMagicAbsorb(amount, protection);
        }
        return amount;
    }

    private static void sendIndicator(ServerPlayer recipient, LivingEntity damageSource,
                                       HitIndicatorType type, int damagePercent, boolean fromNegativePotion) {
        double x = damageSource != null ? damageSource.getX() : 0.0;
        double y = damageSource != null ? damageSource.getY() : 0.0;
        double z = damageSource != null ? damageSource.getZ() : 0.0;
        // By the time LivingDamageEvent fires vanilla has already taken this hit out of Absorption, so "still has
        // Absorption" means the hit did NOT break through; it only turns red once the shield is gone.
        boolean shielded = recipient.getAbsorptionAmount() > 0.0f;
        ModNetworking.sendTo(recipient, new AddHitIndicatorPacket(x, y, z, type, damagePercent, fromNegativePotion, shielded));
    }

    private static void sendMarker(ServerPlayer recipient, HitMarkerType type) {
        ModNetworking.sendTo(recipient, new SetHitMarkerPacket(type));
    }
}

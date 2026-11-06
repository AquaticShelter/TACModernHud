package cc.xypp.modernhud;

import cc.xypp.modernhud.config.CommonConfig;
import cc.xypp.modernhud.network.ModNetworking;
import cc.xypp.modernhud.network.ShieldFeedbackPacket;
import cc.xypp.modernhud.sound.CombatSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Detects Absorption being hit or fully depleted, and notifies ONLY the two
 * people involved (attacker + victim) - never a server-wide broadcast.
 * Runs only on the authoritative side (server, or the integrated server for
 * singleplayer); remote/rendered copies of other entities on a client don't
 * carry a trustworthy Absorption value, so we ignore those entirely.
 */
@Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonEvents {

    // Absorption value at the start of the current hurt() call, per entity - lets us
    // compare "before this specific hit" vs "after it" for that one hit.
    private static final Map<LivingEntity, Float> pendingHurtAbsorption = new WeakHashMap<>();

    // Absorption value seen last tick, per entity - drives the natural-expiry fallback.
    private static final Map<LivingEntity, Float> lastTickAbsorption = new WeakHashMap<>();

    // Game tick at which we last sent a "fully broken" notification for an entity via
    // the hit-based path, so the tick-based fallback doesn't also fire for the same event.
    private static final Map<LivingEntity, Long> lastBreakHandledTick = new WeakHashMap<>();

    @SubscribeEvent
    public static void onLivingHurt(final LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        pendingHurtAbsorption.put(entity, entity.getAbsorptionAmount());
    }

    @SubscribeEvent
    public static void onLivingDamage(final LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        Float before = pendingHurtAbsorption.remove(entity);
        if (before == null || before <= 0.0f) {
            return;
        }

        float after = entity.getAbsorptionAmount();
        if (after >= before) {
            // This hit didn't actually consume any Absorption (e.g. it was fully
            // blocked/cancelled by something else) - nothing to report.
            return;
        }
        boolean fullyBroken = after <= 0.0f;

        if (fullyBroken && !CommonConfig.enableBreakFeedback) {
            return;
        }
        if (!fullyBroken && !CommonConfig.enableHitFeedback) {
            return;
        }

        Entity sourceEntity = event.getSource().getEntity();
        ServerPlayer attacker = (sourceEntity instanceof ServerPlayer sp) ? sp : null;
        ServerPlayer victim = (entity instanceof ServerPlayer vp) ? vp : null;

        // A lethal hit: the kill cue takes over, the shield cue of this very hit stays silent (the icon still shows).
        boolean lethal = event.getAmount() >= entity.getHealth();
        if (attacker != null) {
            if (!lethal) {
                // shield-first priority: tells the hit-cue logic this hit was already covered by a shield cue
                CombatSounds.noteShield(entity, fullyBroken);
            }
            send(attacker, fullyBroken, true, lethal);
        }
        // Don't double-notify if you damaged yourself.
        if (victim != null && victim != attacker) {
            send(victim, fullyBroken, false, lethal);
        }

        if (fullyBroken) {
            lastBreakHandledTick.put(entity, entity.level().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(final MobEffectEvent.Remove event) {
        if (!CommonConfig.clearAbsorptionOnEffectEnd) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || instance.getEffect() != MobEffects.ABSORPTION) {
            return;
        }

        // Vanilla quirk: ending the Absorption effect (naturally or via /effect
        // clear, a milk bucket, etc.) does NOT reset the entity's absorption
        // points - they silently persist until damage consumes them. That's
        // what makes the HUD (and the real shield) look "stuck". Fix it here.
        if (entity.getAbsorptionAmount() <= 0.0f) {
            return;
        }
        entity.setAbsorptionAmount(0.0f);

        // Keep the tick-based fallback's cache in sync so it doesn't also try
        // to fire its own notification for this same transition next tick.
        lastTickAbsorption.put(entity, 0.0f);
        long currentTick = entity.level().getGameTime();
        lastBreakHandledTick.put(entity, currentTick);

        if (CommonConfig.enableBreakFeedback && entity instanceof ServerPlayer serverPlayer) {
            send(serverPlayer, true, false, false);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(final LivingEvent.LivingTickEvent event) {
        if (!CommonConfig.enableBreakFeedback) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        // Only players get a self-only "your shield expired" notice; mobs have
        // no player to notify beyond whoever might've just hit them (handled above).
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return;
        }

        float current = entity.getAbsorptionAmount();
        Float previous = lastTickAbsorption.put(entity, current);
        if (previous == null || previous <= 0.0f || current > 0.0f) {
            return;
        }

        // If a hit already handled this exact break this tick, don't double-fire.
        Long handledTick = lastBreakHandledTick.get(entity);
        long currentTick = entity.level().getGameTime();
        if (handledTick != null && handledTick == currentTick) {
            return;
        }

        send(serverPlayer, true, false, false);
    }

    private static void send(ServerPlayer recipient, boolean fullyBroken, boolean recipientIsAttacker, boolean silent) {
        ModNetworking.sendTo(recipient, new ShieldFeedbackPacket(fullyBroken, recipientIsAttacker, silent));
    }
}

package cc.xypp.modernhud.client;

import cc.xypp.modernhud.config.HudConfig;

/**
 * Client-side reaction to a {@link cc.xypp.modernhud.network.ShieldFeedbackPacket}.
 * Plays the sound locally (so only this one client hears it - never broadcast),
 * and records when to show which feedback icon.
 */
public final class ClientFeedback {

    private static long lastHitTriggerMillis = -1;
    private static long lastBreakTriggerMillis = -1;

    private ClientFeedback() {
    }

    public static void handle(boolean fullyBroken, boolean recipientIsAttacker, boolean silent) {
        if (!recipientIsAttacker && !HudConfig.showFeedbackWhenVictim) {
            // This client is the victim, and they've opted out of seeing/hearing
            // their own hit/break feedback. The attacker's side is never gated
            // by this - only the victim's own client decides this for themselves.
            return;
        }

        if (!silent) {
            ClientCombatSounds.playShield(fullyBroken); // style / volume / pitch come from the Sounds config
        }
        if (fullyBroken) {
            if (recipientIsAttacker) {
                lastBreakTriggerMillis = System.currentTimeMillis();
            }
        } else {
            lastHitTriggerMillis = System.currentTimeMillis();
        }
    }

    /** Milliseconds since the last "you broke it" trigger, or -1 if none yet. */
    public static long millisSinceBreakTrigger() {
        return lastBreakTriggerMillis < 0 ? -1 : System.currentTimeMillis() - lastBreakTriggerMillis;
    }

    /** Milliseconds since the last "hit landed on active shield" trigger, or -1 if none yet. */
    public static long millisSinceHitTrigger() {
        return lastHitTriggerMillis < 0 ? -1 : System.currentTimeMillis() - lastHitTriggerMillis;
    }
}

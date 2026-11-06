package cc.xypp.modernhud.client;

import cc.xypp.modernhud.SoundRegistry;
import cc.xypp.modernhud.config.SoundsConfig;
import cc.xypp.modernhud.sound.CombatSoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;

/** Plays the cues the server asked for, with this player's own style / volume / pitch. Local only. */
public final class ClientCombatSounds {

    private ClientCombatSounds() {
    }

    public static void play(CombatSoundType type) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        SoundEvent sound;
        float volume;
        float pitch;
        switch (type) {
            case HIT -> {
                if (!SoundsConfig.hitOn) {
                    return;
                }
                sound = SoundRegistry.combat(type, SoundsConfig.styleFor(SoundsConfig.hitStyle));
                volume = SoundsConfig.hitVolume;
                pitch = SoundsConfig.hitPitch;
            }
            case KILL -> {
                if (!SoundsConfig.killOn) {
                    return;
                }
                sound = SoundRegistry.combat(type, SoundsConfig.styleFor(SoundsConfig.killStyle));
                volume = SoundsConfig.killVolume;
                pitch = SoundsConfig.killPitch;
            }
            case HEADSHOT_KILL -> {
                if (!SoundsConfig.headshotOn) {
                    return;
                }
                sound = SoundRegistry.combat(type, SoundsConfig.styleFor(SoundsConfig.headshotStyle));
                volume = SoundsConfig.headshotVolume;
                pitch = SoundsConfig.headshotPitch;
            }
            default -> {
                return;
            }
        }
        player.playSound(sound, volume, pitch);
    }

    /** Shield cues (called from {@link ClientFeedback}). */
    public static void playShield(boolean broken) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (broken) {
            if (SoundsConfig.shieldBreakOn) {
                player.playSound(SoundRegistry.shieldBreak(SoundsConfig.styleFor(SoundsConfig.breakStyle)),
                        SoundsConfig.shieldBreakVolume, SoundsConfig.shieldBreakPitch);
            }
        } else if (SoundsConfig.shieldHitOn) {
            player.playSound(SoundRegistry.ARMOR_HIT.get(), SoundsConfig.shieldHitVolume, SoundsConfig.shieldHitPitch);
        }
    }
}

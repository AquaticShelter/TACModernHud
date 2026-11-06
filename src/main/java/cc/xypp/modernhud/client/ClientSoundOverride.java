package cc.xypp.modernhud.client;

import cc.xypp.modernhud.ModernHud;
import cc.xypp.modernhud.config.SoundsConfig;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Makes ModernHud's cues win over TACZ / SuperbWarfare: any sound of another mod that matches the configured patterns
 * (their hit / kill / headshot / hit-marker cues) is cancelled before it reaches the sound engine.
 */
@Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientSoundOverride {

    private static final Logger LOGGER = LogManager.getLogger("ModernHud/SoundOverride");

    private ClientSoundOverride() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlay(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) {
            return;
        }
        ResourceLocation id = sound.getLocation();
        String ns = id.getNamespace();
        if (ModernHud.MOD_ID.equals(ns)) {
            return;
        }
        boolean watched = "tacz".equals(ns) || "superbwarfare".equals(ns);
        String full = id.toString().toLowerCase();
        if (SoundsConfig.overrideOthers && SoundsConfig.shouldOverride(full)) {
            if (SoundsConfig.debugLog) {
                LOGGER.info("ModernHud sound: SILENCED {}", full);
            }
            event.setSound(null);
        } else if (SoundsConfig.debugLog && watched) {
            LOGGER.info("ModernHud sound: played {}", full);
        }
    }
}

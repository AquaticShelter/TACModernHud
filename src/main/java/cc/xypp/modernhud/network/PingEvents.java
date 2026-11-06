package cc.xypp.modernhud.network;

import cc.xypp.modernhud.ModernHud;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server side: forget the probe rate limit of a player who leaves. */
@Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PingEvents {

    private PingEvents() {
    }

    @SubscribeEvent
    public static void onLogout(final PlayerEvent.PlayerLoggedOutEvent event) {
        PingProbePacket.forget(event.getEntity().getUUID());
    }
}

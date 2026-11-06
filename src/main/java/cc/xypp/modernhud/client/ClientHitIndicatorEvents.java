package cc.xypp.modernhud.client;

import cc.xypp.modernhud.ModernHud;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Ages/fades active hit indicators and markers once per client tick, and drops all of them the
 *  moment you leave a world - otherwise a leftover indicator from the world you just left could
 *  flash on screen pointing at nothing in the new one. */
@Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientHitIndicatorEvents {

    @SubscribeEvent
    public static void onClientTick(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ClientHitTracker.tick();
    }

    @SubscribeEvent
    public static void onLevelUnload(final LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ClientLevel)) {
            return;
        }
        ClientHitTracker.clear();
    }
}

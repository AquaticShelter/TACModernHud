package cc.xypp.modernhud.client.spec;

import cc.xypp.modernhud.ModernHud;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge-bus hooks of the spec readout: the per-frame FPS counter and the packet probe on join / leave. */
@Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SpecEvents {

    private SpecEvents() {
    }

    @SubscribeEvent
    public static void onRenderTick(final TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            SpecStats.onFrame(System.nanoTime());
            cc.xypp.modernhud.client.perf.HudProfiler.onFrame();
        }
    }

    @SubscribeEvent
    public static void onClientTick(final TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            LatencyProbe.onClientTick();
        }
    }

    @SubscribeEvent
    public static void onLogin(final ClientPlayerNetworkEvent.LoggingIn event) {
        PacketLossTracker.reset();
        LatencyProbe.reset();
        TpsTracker.reset();
        SpecStats.onSessionStart();
        if (event.getConnection() != null) {
            NetProbe.install(event.getConnection()); // also in single player: the TPS needs the time updates
        }
    }

    @SubscribeEvent
    public static void onLogout(final ClientPlayerNetworkEvent.LoggingOut event) {
        PacketLossTracker.reset();
        LatencyProbe.reset();
        TpsTracker.reset();
    }
}

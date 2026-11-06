package cc.xypp.modernhud.client.perf;

import cc.xypp.modernhud.ModernHud;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Times every HUD overlay of every mod ({@code /modernhud profile all on}), using the Pre / Post events Forge fires around
 * each one. Does nothing, not even a clock read, while switched off.
 */
@Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class OverlayTiming {

    private static ResourceLocation current;
    private static long startNs;

    private OverlayTiming() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onPre(final RenderGuiOverlayEvent.Pre event) {
        if (!HudProfiler.timingWanted()) {
            return;
        }
        current = event.getOverlay().id();
        startNs = System.nanoTime();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onPost(final RenderGuiOverlayEvent.Post event) {
        if (!HudProfiler.timingWanted() || current == null || !current.equals(event.getOverlay().id())) {
            return;
        }
        HudProfiler.addOverlay(current.toString(), System.nanoTime() - startNs);
        current = null;
    }
}

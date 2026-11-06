package cc.xypp.modernhud.client.motion;

import cc.xypp.modernhud.ModernHud;
import cc.xypp.modernhud.config.MotionConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

/**
 * Forge-bus hooks of the motion HUD: the per-frame counter, and the optional swaying of the vanilla hotbar and bars.
 * No mixin is needed: Forge fires a Pre / Post event around every vanilla overlay.
 */
@Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MotionEvents {

    /** Vanilla overlays that move. The crosshair, chat, debug text and the tab list never do. */
    private static final Set<ResourceLocation> VANILLA = Set.of(
            VanillaGuiOverlay.HOTBAR.id(),
            VanillaGuiOverlay.PLAYER_HEALTH.id(),
            VanillaGuiOverlay.ARMOR_LEVEL.id(),
            VanillaGuiOverlay.FOOD_LEVEL.id(),
            VanillaGuiOverlay.AIR_LEVEL.id(),
            VanillaGuiOverlay.MOUNT_HEALTH.id(),
            VanillaGuiOverlay.JUMP_BAR.id(),
            VanillaGuiOverlay.EXPERIENCE_BAR.id(),
            VanillaGuiOverlay.ITEM_NAME.id());

    /** The overlay whose pose this class pushed and has not popped yet (null = none). */
    private static ResourceLocation pushedId;
    private static PoseStack pushedPose;

    private MotionEvents() {
    }

    @SubscribeEvent
    public static void onRenderTick(final TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            MotionHud.onRenderTick();
        }
    }

    /** A new GUI pass starts with a fresh pose stack: forget anything left over. */
    @SubscribeEvent
    public static void onGuiStart(final RenderGuiEvent.Pre event) {
        pushedId = null;
        pushedPose = null;
    }

    /**
     * LOWEST priority and not receiving cancelled events: this runs after every other listener, so when it pushes, the
     * overlay really is going to render and a matching Post event will follow.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onOverlayPre(final RenderGuiOverlayEvent.Pre event) {
        if (!MotionConfig.enabled || !MotionConfig.vanillaEnabled || pushedId != null) {
            return;
        }
        ResourceLocation id = event.getOverlay().id();
        if (!VANILLA.contains(id)) {
            return;
        }
        PoseStack pose = event.getGuiGraphics().pose();
        if (MotionHud.pushVanilla(pose)) {
            pushedId = id;
            pushedPose = pose;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onOverlayPost(final RenderGuiOverlayEvent.Post event) {
        if (pushedId == null || !pushedId.equals(event.getOverlay().id())) {
            return;
        }
        PoseStack pose = event.getGuiGraphics().pose();
        if (pose == pushedPose) {
            pose.popPose();
        }
        pushedId = null;
        pushedPose = null;
    }
}

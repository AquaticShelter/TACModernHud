package cc.xypp.modernhud.client;

import cc.xypp.modernhud.ModernHud;
import cc.xypp.modernhud.client.motion.MotionLayer;
import cc.xypp.modernhud.config.HitIndicationConfig;
import cc.xypp.modernhud.config.WeaponConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Makes the weapon widget/ammo alert ({@link WeaponConfig#ignoreHideGui}) and/or the hit
 * indicator ({@link HitIndicationConfig#ignoreHideGui}) keep showing even while the vanilla F1
 * "hide GUI" toggle is on.
 *
 * <p>F1 isn't something any individual overlay controls: Forge's normal overlay pipeline
 * ({@code RegisterGuiOverlaysEvent}, which is how every other overlay in this mod - and these
 * three normally - get drawn) only runs its registered layers at all when
 * {@code Minecraft.options.hideGui} is false, so simply "not checking hideGui" inside an overlay's
 * own {@code render()} has no effect - Forge never calls it in the first place. {@link RenderGuiEvent.Post}
 * is a separate hook that fires unconditionally regardless of hideGui (it wraps the ENTIRE GUI
 * render pass, gate and all), so THIS class re-invokes the exact same overlay instances
 * ({@link ClientEvents}' static fields, not fresh ones - reusing them keeps their internal
 * animation/switch-timer state intact) from there, but only for the specific frames where the
 * normal pipeline just skipped them (hideGui is on) and the person has actually opted in.
 */
@Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class IgnoreHideGuiRenderer {

    @SubscribeEvent
    public static void onRenderGuiPost(final RenderGuiEvent.Post event) {
        if (!Minecraft.getInstance().options.hideGui) {
            return; // normal pipeline already drew everything this frame - nothing to add here
        }
        boolean wantsWeapon = WeaponConfig.ignoreHideGui;
        boolean wantsHit = HitIndicationConfig.ignoreHideGui;
        if (!wantsWeapon && !wantsHit) {
            return;
        }

        ForgeGui gui = (ForgeGui) Minecraft.getInstance().gui;
        GuiGraphics graphics = event.getGuiGraphics();
        float partialTick = event.getPartialTick();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        if (wantsWeapon) {
            HudScale.render(ClientEvents.weaponHud(), gui, graphics, partialTick, width, height, MotionLayer.WEAPON);
            HudScale.render(ClientEvents.ammoAlert(), gui, graphics, partialTick, width, height, MotionLayer.AMMO_ALERT);
        }
        if (wantsHit) {
            HudScale.render(ClientEvents.hitIndicator(), gui, graphics, partialTick, width, height, MotionLayer.HIT_INDICATOR);
        }
    }
}

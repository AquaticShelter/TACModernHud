package cc.xypp.modernhud.client;

import cc.xypp.modernhud.ModernHud;
import cc.xypp.modernhud.client.compat.TaczCompat;
import cc.xypp.modernhud.config.WeaponConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keybinds. Both unbound by default (per-user preference, set them in Controls > Modern HUD).
 */
public class ModKeybinds {

    public static final String CATEGORY = "key.categories.modernhud";

    /** Opens the drag-to-place HUD editor. Unbound by default. */
    public static final KeyMapping OPEN_HUD_EDITOR = new KeyMapping(
            "key.modernhud.open_editor",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    /**
     * Cycles the 3-way weapon HUD selector ({@link cc.xypp.modernhud.config.WeaponHudSource}):
     * ModernHUD's own widget -&gt; TACZ's own built-in HUD -&gt; TaczPresence's HUD (if installed)
     * -&gt; back to ModernHUD. A live way to switch which one is showing, rather than editing
     * {@code modernhud-weapon.toml} and reloading. Unbound by default.
     */
    public static final KeyMapping TOGGLE_WEAPON_HUD = new KeyMapping(
            "key.modernhud.toggle_weapon_hud",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    /** Shows / hides the compass. Unbound by default. */
    public static final KeyMapping TOGGLE_COMPASS = new KeyMapping(
            "key.modernhud.toggle_compass",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    /** Shows / hides the spec readout (FPS, ping, ...). Unbound by default. */
    public static final KeyMapping TOGGLE_SPECS = new KeyMapping(
            "key.modernhud.toggle_specs",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    /** Cycles the spec readout presets: minimal, standard, gaming, full. Unbound by default. */
    public static final KeyMapping CYCLE_SPECS = new KeyMapping(
            "key.modernhud.cycle_specs",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    @Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterKeyMappings(final RegisterKeyMappingsEvent event) {
            event.register(OPEN_HUD_EDITOR);
            event.register(TOGGLE_WEAPON_HUD);
            event.register(TOGGLE_COMPASS);
            event.register(TOGGLE_SPECS);
            event.register(CYCLE_SPECS);
        }
    }

    @Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBus {
        @SubscribeEvent
        public static void onClientTick(final TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            while (OPEN_HUD_EDITOR.consumeClick()) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.screen == null) {
                    mc.setScreen(new cc.xypp.modernhud.client.gui.HudEditorScreen());
                }
            }
            while (TOGGLE_COMPASS.consumeClick()) {
                cc.xypp.modernhud.config.CompassConfig.toggleEnabled();
            }
            while (TOGGLE_SPECS.consumeClick()) {
                cc.xypp.modernhud.config.SpecsConfig.toggleEnabled();
            }
            while (CYCLE_SPECS.consumeClick()) {
                cc.xypp.modernhud.config.SpecsConfig.Preset next = cc.xypp.modernhud.config.SpecsConfig.cyclePreset();
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "Spec readout: " + next.name().toLowerCase()), true);
                }
            }
            while (TOGGLE_WEAPON_HUD.consumeClick()) {
                if (!TaczCompat.loaded()) {
                    continue;
                }
                WeaponConfig.cycleHudSource();
            }
        }
    }
}

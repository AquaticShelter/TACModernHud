package cc.xypp.modernhud;

import cc.xypp.modernhud.config.BossBarConfig;
import cc.xypp.modernhud.config.CommonConfig;
import cc.xypp.modernhud.config.HitIndicationConfig;
import cc.xypp.modernhud.config.HudConfig;
import cc.xypp.modernhud.config.KillstreakConfig;
import cc.xypp.modernhud.config.WeaponConfig;
import cc.xypp.modernhud.network.ModNetworking;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Modern HUD: a military-shooter style HUD for Forge 1.20.1.
 *
 * <ul>
 *   <li>Client: player and team widgets, TACZ weapon widget, compass, killstreak tracker, boss bars, hit direction arcs,
 *       config screen (YACL) and client commands.</li>
 *   <li>Common: shield (absorption) hit / break detection and hit / kill sound cues. The server only decides WHEN a cue
 *       plays and sends a small packet to the attacker and victim; nothing here changes gameplay except the optional
 *       clearing of leftover absorption when the effect ends.</li>
 * </ul>
 * Every integration (TACZ, ModernLoadout, Modern Ping, Ping Wheel, SuperbWarfare) is optional and loaded by reflection.
 */
@Mod(ModernHud.MOD_ID)
public class ModernHud {

    public static final String MOD_ID = "modernhud";

    public ModernHud() {
        SoundRegistry.SOUNDS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModNetworking.register();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(
                (net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e) ->
                        e.enqueueWork(cc.xypp.modernhud.sound.TaczCombatBridge::init));

        // Common config (shield-break detection) is relevant on both sides.
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);

        // Client-only configs and keybinds. Each needs its own explicit file
        // name - otherwise Forge derives "<modid>-<type>.toml" for both,
        // which collide since they're both CLIENT type under the same modid.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, HudConfig.SPEC, "modernhud-hud.toml");
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, WeaponConfig.SPEC, "modernhud-weapon.toml");
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, HitIndicationConfig.SPEC, "modernhud-hitindication.toml");
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, KillstreakConfig.SPEC, "modernhud-killstreak.toml");
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, BossBarConfig.SPEC, "modernhud-bossbar.toml");
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, cc.xypp.modernhud.config.CompassConfig.SPEC, "modernhud-compass.toml");
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, cc.xypp.modernhud.config.SoundsConfig.SPEC, "modernhud-sounds.toml");
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, cc.xypp.modernhud.config.MotionConfig.SPEC, "modernhud-motion.toml");
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, cc.xypp.modernhud.config.SpecsConfig.SPEC, "modernhud-specs.toml");
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, cc.xypp.modernhud.config.PerfConfig.SPEC, "modernhud-perf.toml");
            // the in-game config screen (YACL) behind the mod list's Config button
            cc.xypp.modernhud.client.gui.config.ClientConfigHook.register();
        }
    }
}

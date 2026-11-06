package cc.xypp.modernhud.client.gui.config;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Registers the YACL screen as this mod's "Config" button in the Forge mod list. Kept in its own class so the main
 * mod class never touches a YACL type (the dependency is client-only).
 */
public final class ClientConfigHook {

    private ClientConfigHook() {
    }

    /** A fresh config screen (used by /modernhud config). */
    public static net.minecraft.client.gui.screens.Screen create(net.minecraft.client.gui.screens.Screen parent) {
        return ModernHudConfigScreen.create(parent);
    }

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> ModernHudConfigScreen.create(parent)));
    }
}

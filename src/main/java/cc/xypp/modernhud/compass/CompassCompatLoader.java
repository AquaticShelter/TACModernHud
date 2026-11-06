package cc.xypp.modernhud.compass;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/** Loads the optional compass integrations (Modern Ping / Ping Wheel, TACZ gunfire) only when their mods are present. */
public final class CompassCompatLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TACZ_RENDERER = "cc.xypp.modernhud.compass.tacz.TaczGunfireCompassRenderer";

    private CompassCompatLoader() {
    }

    public static CompassPingRenderer loadPingRenderer() {
        if (ModList.get().isLoaded("modernping")) {
            try {
                CompassPingRenderer renderer = new ModernPingCompassRenderer();
                LOGGER.info("ModernHUD compass: Modern Ping markers enabled");
                return renderer;
            } catch (LinkageError | RuntimeException | ReflectiveOperationException e) {
                LOGGER.error("ModernHUD compass: could not enable Modern Ping compatibility", e);
                return CompassPingRenderer.NONE;
            }
        }
        if (!ModList.get().isLoaded("pingwheel")) {
            return CompassPingRenderer.NONE;
        }
        try {
            CompassPingRenderer renderer = new PingWheelCompassRenderer();
            LOGGER.info("ModernHUD compass: Ping Wheel markers enabled");
            return renderer;
        } catch (LinkageError | RuntimeException | ReflectiveOperationException e) {
            LOGGER.error("ModernHUD compass: could not enable Ping Wheel compatibility", e);
            return CompassPingRenderer.NONE;
        }
    }

    public static CompassTaczRenderer loadTaczRenderer() {
        if (!ModList.get().isLoaded("tacz")) {
            return CompassTaczRenderer.NONE;
        }
        try {
            Class<?> rendererClass = Class.forName(TACZ_RENDERER);
            CompassTaczRenderer renderer = (CompassTaczRenderer) rendererClass.getConstructor().newInstance();
            LOGGER.info("ModernHUD compass: TACZ gunfire markers enabled");
            return renderer;
        } catch (LinkageError | ReflectiveOperationException e) {
            LOGGER.error("ModernHUD compass: could not enable TACZ compatibility", e);
            return CompassTaczRenderer.NONE;
        }
    }
}

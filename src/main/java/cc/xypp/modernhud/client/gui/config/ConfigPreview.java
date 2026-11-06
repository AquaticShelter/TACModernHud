package cc.xypp.modernhud.client.gui.config;

/** True while the config screen is open, so overlays that normally hide behind screens keep drawing as a live preview. */
public final class ConfigPreview {

    private static volatile boolean active;

    private ConfigPreview() {
    }

    public static boolean isActive() {
        return active;
    }

    static void setActive(boolean value) {
        active = value;
    }
}

package cc.xypp.modernhud.config;

/** Where the ping number comes from. */
public enum PingSource {
    /** Live probe when the server has Modern HUD, otherwise the tab-list ping. */
    AUTO,
    /** Live probe only (shows -- on a server without the mod). */
    PROBE,
    /** The vanilla tab-list ping (updated by the server only every 15 seconds). */
    TAB_LIST
}

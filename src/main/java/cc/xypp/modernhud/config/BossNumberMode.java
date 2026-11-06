package cc.xypp.modernhud.config;

/** What the number next to a compact boss bar shows. */
public enum BossNumberMode {
    /** The bar's progress as a percent, 0-100 - works for every boss event. */
    PERCENT,
    /** The boss's exact health points. Only known when the boss entity is loaded on the client and can be matched
     *  to the bar by name (wither, ender dragon, most mod bosses); anything else (raids, /bossbar, a boss out of
     *  range) falls back to the percent. */
    HEALTH
}

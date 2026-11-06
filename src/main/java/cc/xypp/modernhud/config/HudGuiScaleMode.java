package cc.xypp.modernhud.config;

/** Where the HUD's GUI scale comes from. */
public enum HudGuiScaleMode {
    /** Follow Minecraft's own "GUI Scale" video setting (the old behaviour). */
    MINECRAFT,
    /** A fixed scale from this mod's config ({@code hudGuiScale}), whatever Minecraft's setting is. */
    FIXED,
    /** Scale with the window height: {@code hudGuiScale * windowHeight / referenceHeight}, so the HUD keeps the
     *  same proportion of the screen at any resolution / window size. */
    RESOLUTION
}

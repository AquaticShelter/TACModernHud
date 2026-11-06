package cc.xypp.modernhud.config;

/** How the Absorption plate row picks its fill colour. */
public enum AbsorptionColorMode {
    /** Gray -&gt; blue -&gt; purple -&gt; red as more whole plates fill (the original MW look). */
    TIERED,
    /** Always {@code absorptionCustomColor}, regardless of how many plates are filled. */
    CUSTOM
}

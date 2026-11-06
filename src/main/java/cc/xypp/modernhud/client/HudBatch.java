package cc.xypp.modernhud.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws a run of plain fills and text as ONE batch instead of one GPU flush per call. Nesting is safe: an inner call
 * while a batch is already open just runs its body. Only put fills and text inside a batch. Texture blits and the
 * immediate-mode {@link SoftDraw} glows draw straight away, so {@link SoftDraw} flushes the open batch first to keep
 * the draw order.
 */
public final class HudBatch {

    private static int depth;

    private HudBatch() {
    }

    public static boolean active() {
        return depth > 0;
    }

    public static void run(GuiGraphics g, Runnable body) {
        if (depth > 0) {
            body.run();
            return;
        }
        depth++;
        try {
            g.drawManaged(body);
        } finally {
            depth--;
        }
    }
}

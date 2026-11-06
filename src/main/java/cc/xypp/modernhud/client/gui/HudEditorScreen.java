package cc.xypp.modernhud.client.gui;

import cc.xypp.modernhud.client.compat.TaczCompat;
import cc.xypp.modernhud.config.HudConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Drag-to-place HUD editor for the Hit/Break feedback icon — the "move it with the mouse instead
 * of typing numbers" tool, in the spirit of Call of Duty's HUD layout editor. Open it with the
 * keybind (Controls &gt; Modern HUD), drag the icon to where you want it, and it saves on close.
 * Dragging converts it to a free (CUSTOM) absolute position.
 *
 * <p>Everything is code-drawn; no textures. The handle is a simple labelled box sized to roughly
 * match what the icon renders as in-world, so you can line it up by eye.
 *
 * <p>Neither the player HUD (health/name/armor widget) NOR the TACZ weapon widget live here: both
 * are corner-anchored and config-file-only (see {@code HudConfig}/{@code WeaponConfig}'s "position"
 * sections), not mouse-draggable. Dragging is imprecise in a way that interacts badly with
 * different players' screen resolutions/aspect ratios for a fixed-size element that's meant to sit
 * in exactly the same spot every game, on every screen — the player HUD used to have a drag handle
 * here too, but per feedback that made it too easy to accidentally end up somewhere odd; a
 * TOP_LEFT/TOP_RIGHT/BOTTOM_LEFT/BOTTOM_RIGHT anchor plus a numeric offset is both more precise and
 * more predictable across setups.
 */
public final class HudEditorScreen extends Screen {

    private final List<Handle> handles = new ArrayList<>();
    private Handle dragging;
    private int dragDX;
    private int dragDY;

    public HudEditorScreen() {
        super(Component.translatable("screen.modernhud.editor"));
    }

    @Override
    protected void init() {
        handles.clear();
        // Hit / break feedback icon - the ONLY element this editor still handles (see class javadoc).
        handles.add(new Handle("Hit / Break Icon",
                Math.max(16, HudConfig.feedbackIconWidth), Math.max(16, HudConfig.feedbackIconHeight),
                HudConfig.feedbackOffsetX, resolveY(HudConfig.feedbackOffsetY, HudConfig.feedbackIconHeight),
                (x, y) -> HudConfig.setFeedbackPosition(x, screenToStoredY(y, HudConfig.feedbackIconHeight))));
    }

    /** Stored offsets are from the bottom for BOTTOM_* anchors; in the editor we work top-left, so
     *  we just treat the stored value as a top-left Y once and let dragging own it thereafter. */
    private int resolveY(int storedY, int h) {
        return storedY;
    }

    private int screenToStoredY(int y, int h) {
        return y;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = handles.size() - 1; i >= 0; i--) {
                Handle h = handles.get(i);
                if (h.contains(mouseX, mouseY)) {
                    dragging = h;
                    dragDX = (int) mouseX - h.x;
                    dragDY = (int) mouseY - h.y;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging != null) {
            dragging.x = Math.max(0, Math.min(width - dragging.w, (int) mouseX - dragDX));
            dragging.y = Math.max(0, Math.min(height - dragging.h, (int) mouseY - dragDY));
            dragging.apply.accept(dragging.x, dragging.y);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        // Faint grid to help alignment (COD-style).
        int grid = 0x18FFFFFF;
        for (int x = 0; x < width; x += 20) g.fill(x, 0, x + 1, height, grid);
        for (int y = 0; y < height; y += 20) g.fill(0, y, width, y + 1, grid);

        g.drawCenteredString(font, Component.translatable("screen.modernhud.editor").getString(),
                width / 2, 8, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable("screen.modernhud.editor_hint").getString(),
                width / 2, 20, 0xFFB0B6BC);
        g.drawCenteredString(font, Component.translatable("screen.modernhud.editor_player_hint").getString(),
                width / 2, 31, 0xFF6E7580);
        if (TaczCompat.loaded()) {
            g.drawCenteredString(font, Component.translatable("screen.modernhud.editor_weapon_hint").getString(),
                    width / 2, 42, 0xFF6E7580);
        }

        for (Handle h : handles) {
            boolean hover = h.contains(mouseX, mouseY) || h == dragging;
            int border = hover ? 0xFF54ABE8 : 0x88FFFFFF;
            int fillC = hover ? 0x3354ABE8 : 0x22000000;
            g.fill(h.x, h.y, h.x + h.w, h.y + h.h, fillC);
            g.fill(h.x, h.y, h.x + h.w, h.y + 1, border);
            g.fill(h.x, h.y + h.h - 1, h.x + h.w, h.y + h.h, border);
            g.fill(h.x, h.y, h.x + 1, h.y + h.h, border);
            g.fill(h.x + h.w - 1, h.y, h.x + h.w, h.y + h.h, border);
            g.drawString(font, h.label, h.x + 3, h.y + 3, 0xFFFFFFFF, true);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        HudConfig.saveConfig();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class Handle {
        final String label;
        final int w;
        final int h;
        int x;
        int y;
        final java.util.function.BiConsumer<Integer, Integer> apply;

        Handle(String label, int w, int h, int x, int y, java.util.function.BiConsumer<Integer, Integer> apply) {
            this.label = label;
            this.w = w;
            this.h = h;
            this.x = x;
            this.y = y;
            this.apply = apply;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }
}

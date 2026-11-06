package cc.xypp.modernhud.client.gui.config;

import cc.xypp.modernhud.config.ConfigRefresh;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Objects;

/**
 * The config screen with a live preview, the way Halo HUD does it: every value you change is applied to the HUD right
 * away (the HUD keeps drawing behind the dimmed screen) and nothing is written to disk until Apply / Done. Cancel puts
 * every value back.
 */
final class PreviewScreen extends YACLScreen {

    /** One option and the spec entry it edits. */
    record Pair(SpecOptions.Entry entry, Option<?> option) {
    }

    private final List<Pair> pairs;

    PreviewScreen(YetAnotherConfigLib config, Screen parent, List<Pair> pairs) {
        super(config, parent);
        this.pairs = pairs;
    }

    @Override
    protected void init() {
        super.init();
        ConfigPreview.setActive(true);
    }

    @Override
    public void tick() {
        super.tick();
        pushPending();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void pushPending() {
        boolean changed = false;
        for (Pair p : pairs) {
            ForgeConfigSpec.ConfigValue cv = p.entry().value();
            Object pending = SpecOptions.pendingSpecValue(p.option());
            if (pending != null && !Objects.equals(pending, cv.get())) {
                cv.set(pending);
                changed = true;
            }
        }
        if (changed) {
            ConfigRefresh.refreshAll();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void removed() {
        super.removed();
        ConfigPreview.setActive(false);
        boolean changed = false;
        for (Pair p : pairs) {
            ForgeConfigSpec.ConfigValue cv = p.entry().value();
            Object original = SpecOptions.storedValue(cv);
            if (original != null && !Objects.equals(original, cv.get())) {
                cv.set(original);
                changed = true;
            }
        }
        if (changed) {
            ConfigRefresh.refreshAll();
        }
    }

    /** In a world the HUD is the preview, so dim the background less than YACL normally does. */
    @Override
    public void renderBackground(GuiGraphics graphics) {
        if (minecraft != null && minecraft.level != null) {
            graphics.fillGradient(0, 0, width, height, 0x66101010, 0x80101010);
        } else {
            super.renderBackground(graphics);
        }
    }
}

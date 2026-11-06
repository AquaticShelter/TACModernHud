package cc.xypp.modernhud.client.spec;

import cc.xypp.modernhud.client.HudBatch;
import cc.xypp.modernhud.client.HudPanel;
import cc.xypp.modernhud.client.HudText;
import cc.xypp.modernhud.client.gui.config.ConfigPreview;
import cc.xypp.modernhud.client.perf.HudProfiler;
import cc.xypp.modernhud.config.ElementStyle;
import cc.xypp.modernhud.config.SpecLayout;
import cc.xypp.modernhud.config.SpecsConfig;
import cc.xypp.modernhud.config.SpecsConfig.Item;
import cc.xypp.modernhud.config.TempUnit;
import cc.xypp.modernhud.config.TimeFormatMode;
import cc.xypp.modernhud.config.UnavailableMode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * The spec readout: FPS, ping, packet loss, GPU temperature and the real-world time, in small flat boxes.
 *
 * <p>Pixel-perfect by construction: every size is counted in FONT pixels, and one font pixel is drawn as a whole number
 * {@code p} of screen pixels. The origin is snapped to a screen pixel, so the boxes have crisp edges, equal gaps and
 * the letters are centred in them at every GUI scale and HUD scale. The text is laid out on a fixed character grid
 * (monospace) so changing digits never make the boxes jitter.
 */
public class SpecReadoutOverlay implements IGuiOverlay {

    /** Height of the capital letters / digits of Minecraft's font, in font pixels. */
    private static final int CAP_HEIGHT = 7;
    /** Ink width of a letter of Minecraft's font (the advance is 6: ink plus one pixel of spacing). */
    private static final int INK_WIDTH = 5;

    private static final class Entry {
        String label = "";
        String value = "";
        int valueRgb;
        boolean visible;
        long key = Long.MIN_VALUE;
        long rev = -1L;
        int labelCells;
        String full = "";
        boolean graph;
    }

    private final Entry[] entries = new Entry[Item.values().length];
    private final Entry[] shown = new Entry[Item.values().length];
    private final int[] widths = new int[Item.values().length];
    private final String[] charStrings = new String[256];

    private float vis;
    private long lastNanos;

    private DateTimeFormatter timeFmt;
    private String timeFmtKey = "";
    private Locale timeLocale = Locale.getDefault();

    public SpecReadoutOverlay() {
        for (int i = 0; i < entries.length; i++) {
            entries[i] = new Entry();
        }
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        long nowN = System.nanoTime();
        float dt = lastNanos == 0L ? 0f : Mth.clamp((nowN - lastNanos) / 1.0e9f, 0f, 0.25f);
        lastNanos = nowN;

        ElementStyle st = SpecsConfig.style;
        boolean show = shouldShow(mc);
        float target = show ? 1f : 0f;
        float ms = show ? st.fadeInMs : st.fadeOutMs;
        if (ms <= 0f) {
            vis = target;
        } else {
            float step = dt * 1000f / ms;
            vis = target > vis ? Math.min(target, vis + step) : Math.max(target, vis - step);
        }
        if (vis <= 0.003f) {
            return;
        }

        int n = collect(mc);
        if (n == 0) {
            return;
        }

        Font font = mc.font;
        boolean mono = SpecsConfig.monospace;
        int cell = SpecsConfig.cellWidth;
        int padX = SpecsConfig.padX;
        int boxH = SpecsConfig.boxHeight;
        boolean separate = SpecsConfig.separateBoxes;
        int gap = separate ? SpecsConfig.gap : 0;
        boolean vertical = SpecsConfig.direction == SpecLayout.VERTICAL;

        int maxW = 0;
        int sumW = 0;
        for (int i = 0; i < n; i++) {
            Entry e = shown[i];
            int textW = e.graph
                    ? SpecsConfig.graphBars * SpecsConfig.graphBarWidth
                    : mono
                    ? Math.max(0, e.full.length() * cell - (cell - INK_WIDTH))
                    : Math.max(0, font.width(e.full) - 1);
            widths[i] = textW + padX * 2;
            maxW = Math.max(maxW, widths[i]);
            sumW += widths[i];
        }
        int totalW = vertical ? maxW : sumW + gap * (n - 1);
        int totalH = vertical ? n * boxH + gap * (n - 1) : boxH;

        // one font pixel = p whole screen pixels
        float phys = HudText.physPerUnit(g);
        if (phys <= 0.01f) {
            return;
        }
        int p = Math.max(1, Math.round(SpecsConfig.scale * phys));
        float u = p / phys;
        float screenW = mc.getWindow().getWidth() / phys;
        float screenH = mc.getWindow().getHeight() / phys;
        float wU = totalW * u;
        float hU = totalH * u;
        float offX = SpecsConfig.offsetX * u;
        float offY = SpecsConfig.offsetY * u;
        float x;
        float y;
        switch (SpecsConfig.anchor) {
            case TOP_CENTER:
                x = (screenW - wU) / 2f + offX;
                y = offY;
                break;
            case TOP_RIGHT:
                x = screenW - wU - offX;
                y = offY;
                break;
            case BOTTOM_LEFT:
                x = offX;
                y = screenH - hU - offY;
                break;
            case BOTTOM_CENTER:
                x = (screenW - wU) / 2f + offX;
                y = screenH - hU - offY;
                break;
            case BOTTOM_RIGHT:
                x = screenW - wU - offX;
                y = screenH - hU - offY;
                break;
            default:
                x = offX;
                y = offY;
                break;
        }
        float ox = Math.round(x * phys) / phys; // snap the origin to a screen pixel
        float oy = Math.round(y * phys) / phys;

        PoseStack pose = g.pose();
        pose.pushPose();
        try {
            pose.translate(ox, oy, 0f);
            pose.scale(u, u, 1f);
            final int fn = n;
            final int fTotalW = totalW;
            final int fTotalH = totalH;
            HudBatch.run(g, () -> drawAll(g, font, fn, fTotalW, fTotalH, gap, boxH, padX, cell, vertical, separate, mono));
        } finally {
            pose.popPose();
        }
    }

    // ------------------------------------------------------------------------------------------------ drawing (font pixel space)

    private void drawAll(GuiGraphics g, Font font, int n, int totalW, int totalH, int gap, int boxH, int padX, int cell,
                         boolean vertical, boolean separate, boolean mono) {
        ElementStyle st = SpecsConfig.style;
        if (separate) {
            int pos = 0;
            for (int i = 0; i < n; i++) {
                int bx = vertical ? 0 : pos;
                int by = vertical ? pos : 0;
                int bw = vertical ? totalW : widths[i];
                HudPanel.drawStyled(g, bx, by, bw, boxH, st, vis);
                pos += (vertical ? boxH : widths[i]) + gap;
            }
        } else {
            HudPanel.drawStyled(g, 0, 0, totalW, totalH, st, vis);
        }

        float a = st.alpha(vis);
        int alpha = Mth.clamp(Math.round(a * 255f), 0, 255);
        int labelAlpha = Mth.clamp(Math.round(a * SpecsConfig.labelOpacity * 255f), 0, 255);
        int labelRgb = SpecsConfig.textColor;
        boolean shadow = SpecsConfig.textShadow;
        int textY = Math.max(0, (boxH - CAP_HEIGHT) / 2);
        int leftGap = mono ? Math.max(0, (cell - (INK_WIDTH + 1)) / 2) : 0;

        int pos = 0;
        for (int i = 0; i < n; i++) {
            Entry e = shown[i];
            int bx = vertical ? 0 : pos;
            int by = vertical ? pos : 0;
            if (e.graph) {
                drawGraph(g, bx + padX, by, boxH, alpha);
                pos += (vertical ? boxH : widths[i]) + gap;
                continue;
            }
            int tx = bx + padX - leftGap;
            int ty = by + textY;
            int labelColor = (labelAlpha << 24) | (labelRgb & 0xFFFFFF);
            int valueColor = (alpha << 24) | (e.valueRgb & 0xFFFFFF);
            if (mono) {
                String s = e.full;
                for (int c = 0; c < s.length(); c++) {
                    char ch = s.charAt(c);
                    if (ch == ' ') {
                        continue;
                    }
                    int color = c < e.labelCells ? labelColor : valueColor;
                    if ((color >>> 24) < 4) {
                        continue;
                    }
                    String cs = charString(ch);
                    int off = Math.max(0, (cell - font.width(cs)) / 2);
                    HudText.text(g, font, cs, tx + c * cell + off, ty, color, shadow);
                }
            } else {
                int labelW = e.labelCells > 0 ? font.width(e.label + " ") : 0;
                if (e.labelCells > 0 && (labelColor >>> 24) >= 4) {
                    HudText.text(g, font, e.label, tx, ty, labelColor, shadow);
                }
                if ((valueColor >>> 24) >= 4) {
                    HudText.text(g, font, e.value, tx + labelW, ty, valueColor, shadow);
                }
            }
            pos += (vertical ? boxH : widths[i]) + gap;
        }
    }

    /** The frame time graph: bars from the oldest (left) to the newest (right), as tall as the slowest frame of their slice. */
    private void drawGraph(GuiGraphics g, int x, int y, int boxH, int alpha) {
        int bars = SpecsConfig.graphBars;
        int bw = SpecsConfig.graphBarWidth;
        int pad = Math.min(3, Math.max(0, (boxH - 3) / 2));
        int gh = Math.max(3, boxH - 2 * pad);
        float maxMs = Math.max(1f, SpecsConfig.graphMaxMs);
        float warnMs = 1000f / Math.max(1, SpecsConfig.fpsWarn);
        float critMs = 1000f / Math.max(1, SpecsConfig.fpsCrit);
        int normal = (alpha << 24) | (SpecsConfig.textColor & 0xFFFFFF);
        int warn = (alpha << 24) | (SpecsConfig.warnColor & 0xFFFFFF);
        int crit = (alpha << 24) | (SpecsConfig.critColor & 0xFFFFFF);
        int baseY = y + pad + gh;
        for (int k = 0; k < bars; k++) {
            float ft = SpecStats.graphFromNewest(bars - 1 - k);
            if (ft <= 0f) {
                continue;
            }
            int h = Math.max(1, Math.min(gh, Math.round(ft / maxMs * gh)));
            int color = !SpecsConfig.thresholdColors ? normal : ft > critMs ? crit : ft > warnMs ? warn : normal;
            int bx = x + k * bw;
            g.fill(bx, baseY - h, bx + bw, baseY, color);
        }
    }

    private String charString(char c) {
        if (c < charStrings.length) {
            String s = charStrings[c];
            if (s == null) {
                s = String.valueOf(c);
                charStrings[c] = s;
            }
            return s;
        }
        return String.valueOf(c);
    }

    // ------------------------------------------------------------------------------------------------ data

    private static boolean shouldShow(Minecraft mc) {
        if (!SpecsConfig.enabled || mc.player == null) {
            return false;
        }
        if (SpecsConfig.hideWithDebug && mc.options.renderDebug) {
            return false;
        }
        if (mc.screen != null && !(mc.screen instanceof ChatScreen) && !SpecsConfig.showInScreens
                && !ConfigPreview.isActive()) {
            return false;
        }
        return true;
    }

    /** Refreshes every enabled item (only rebuilds a text when its number changed) and returns how many are visible. */
    private int collect(Minecraft mc) {
        int n = 0;
        long rev = SpecsConfig.revision();
        for (Item item : SpecsConfig.order) {
            if (!SpecsConfig.SHOW[item.ordinal()]) {
                continue;
            }
            Entry e = entries[item.ordinal()];
            refresh(e, item, rev, mc);
            if (e.visible) {
                shown[n++] = e;
            }
        }
        return n;
    }

    private void refresh(Entry e, Item item, long rev, Minecraft mc) {
        String label = SpecsConfig.LABELS[item.ordinal()];
        switch (item) {
            case FPS: {
                int fps = SpecStats.fps();
                if (changed(e, fps, rev)) {
                    e.visible = true;
                    e.valueRgb = fps < 0 ? SpecsConfig.textColor : colorLow(fps, SpecsConfig.fpsWarn, SpecsConfig.fpsCrit);
                    build(e, label, fps < 0 ? "--" : Integer.toString(fps));
                }
                break;
            }
            case LOW: {
                int low = SpecStats.low1();
                if (changed(e, low, rev)) {
                    e.visible = true;
                    e.valueRgb = low < 0 ? SpecsConfig.textColor : colorLow(low, SpecsConfig.fpsWarn, SpecsConfig.fpsCrit);
                    build(e, label, low < 0 ? "--" : Integer.toString(low));
                }
                break;
            }
            case FRAME: {
                float ms = SpecStats.frameMs();
                if (changed(e, Math.round(ms * 10f), rev)) {
                    e.visible = true;
                    e.valueRgb = ms <= 0f ? SpecsConfig.textColor
                            : colorLow(Math.round(1000f / ms), SpecsConfig.fpsWarn, SpecsConfig.fpsCrit);
                    build(e, label, ms < 0f ? "--" : String.format(Locale.ROOT, "%.1f", ms) + " " + unit("MS", "ms"));
                }
                break;
            }
            case PING: {
                boolean sp = SpecStats.isSingleplayer();
                int ping = SpecStats.pingMs();
                if (ping < 0 && sp) {
                    ping = 0;
                }
                long key = ping + (sp ? 100_000L : 0L);
                if (changed(e, key, rev)) {
                    e.visible = !(sp && !SpecsConfig.pingInSingleplayer);
                    e.valueRgb = ping < 0 ? SpecsConfig.textColor : colorHigh(ping, SpecsConfig.pingWarn, SpecsConfig.pingCrit);
                    build(e, label, ping < 0 ? "--" : ping + " " + unit("MS", "ms"));
                }
                break;
            }
            case LOSS: {
                int loss = Math.round(SpecStats.lossPercent());
                if (changed(e, loss, rev)) {
                    e.visible = true;
                    e.valueRgb = colorHigh(loss, SpecsConfig.lossWarn, SpecsConfig.lossCrit);
                    build(e, label, loss + " %");
                }
                break;
            }
            case CPU: {
                CpuSensors.Reading r = CpuSensors.get();
                int v = r == null ? -1 : r.load();
                if (changed(e, v, rev)) {
                    plain(e, label, v < 0 ? null : v + " %");
                }
                break;
            }
            case CPU_TEMP: {
                CpuSensors.wantTemperature();
                CpuSensors.Reading r = CpuSensors.get();
                int t = r == null ? -1 : r.temp();
                if (changed(e, t * 4L + SpecsConfig.gpuUnit.ordinal(), rev)) {
                    temperature(e, label, t, SpecsConfig.cpuTempWarn, SpecsConfig.cpuTempCrit);
                }
                break;
            }
            case GPU_LOAD: {
                GpuSensors.Reading g = GpuSensors.get();
                int v = g == null ? -1 : g.load();
                if (changed(e, v, rev)) {
                    plain(e, label, v < 0 ? null : v + " %");
                }
                break;
            }
            case GPU: {
                GpuSensors.Reading g = GpuSensors.get();
                int t = g == null ? -1 : g.temp();
                if (changed(e, t * 4L + SpecsConfig.gpuUnit.ordinal(), rev)) {
                    temperature(e, label, t, SpecsConfig.gpuWarn, SpecsConfig.gpuCrit);
                }
                break;
            }
            case VRAM: {
                GpuSensors.Reading g = GpuSensors.get();
                int used = g == null ? -1 : g.vramUsedMb();
                int total = g == null ? -1 : g.vramTotalMb();
                if (changed(e, memKey(used, total), rev)) {
                    memory(e, label, used, total);
                }
                break;
            }
            case GPU_POWER: {
                GpuSensors.Reading g = GpuSensors.get();
                int v = g == null ? -1 : g.powerW();
                if (changed(e, v, rev)) {
                    plain(e, label, v < 0 ? null : v + " " + unit("W", "W"));
                }
                break;
            }
            case GPU_CLOCK: {
                GpuSensors.Reading g = GpuSensors.get();
                int v = g == null ? -1 : g.clockMhz();
                if (changed(e, v, rev)) {
                    plain(e, label, v < 0 ? null : v + " " + unit("MHZ", "MHz"));
                }
                break;
            }
            case RAM: {
                CpuSensors.Reading r = CpuSensors.get();
                int used = r == null ? -1 : r.ramUsedMb();
                int total = r == null ? -1 : r.ramTotalMb();
                if (changed(e, memKey(used, total), rev)) {
                    memory(e, label, used, total);
                }
                break;
            }
            case JVM: {
                Runtime rt = Runtime.getRuntime();
                int used = (int) ((rt.totalMemory() - rt.freeMemory()) / 1_048_576L);
                long max = rt.maxMemory();
                int total = max > 0 && max < (1L << 50) ? (int) (max / 1_048_576L) : -1;
                if (changed(e, memKey(used, total), rev)) {
                    memory(e, label, used, total);
                }
                break;
            }
            case GRAPH: {
                e.visible = true;
                e.graph = true;
                e.full = "";
                e.labelCells = 0;
                e.key = rev;
                break;
            }
            case TPS: {
                float tps = TpsTracker.tps(System.nanoTime());
                if (changed(e, Math.round(tps * 10f), rev)) {
                    e.visible = true;
                    e.valueRgb = tps < 0f ? SpecsConfig.textColor
                            : colorLow((int) Math.floor(tps), SpecsConfig.tpsWarn, SpecsConfig.tpsCrit);
                    build(e, label, tps < 0f ? "--" : String.format(Locale.ROOT, "%.1f", tps));
                }
                break;
            }
            case NET: {
                float[] pk = SpecStats.packetsPerSecond();
                long key = pk == null ? -1L : Math.round(pk[0]) * 100_000L + Math.round(pk[1]);
                if (changed(e, key, rev)) {
                    plain(e, label, pk == null ? null : Math.round(pk[0]) + "/" + Math.round(pk[1]));
                }
                break;
            }
            case HUD: {
                float ms = HudProfiler.totalCpuMs();
                if (changed(e, Math.round(ms * 100f), rev)) {
                    plain(e, label, String.format(Locale.ROOT, "%.2f", ms) + " " + unit("MS", "ms"));
                }
                break;
            }
            case BATTERY: {
                BatterySensors.Reading b = BatterySensors.get();
                long key = b == null ? -1L : b.percent() * 2L + (b.charging() ? 1L : 0L);
                if (changed(e, key, rev)) {
                    if (b == null) {
                        e.visible = false; // a desktop has no battery
                    } else {
                        e.visible = true;
                        e.valueRgb = b.charging() ? SpecsConfig.textColor : colorLow(b.percent(), 20, 10);
                        build(e, label, b.percent() + " %" + (b.charging() ? " +" : ""));
                    }
                }
                break;
            }
            case SESSION: {
                long secs = SpecStats.sessionSeconds();
                if (changed(e, secs, rev)) {
                    e.visible = true;
                    e.valueRgb = SpecsConfig.textColor;
                    build(e, label, secs < 0 ? "--" : duration(secs));
                }
                break;
            }
            case TIME: {
                long second = System.currentTimeMillis() / 1000L;
                if (changed(e, second, rev)) {
                    e.visible = true;
                    e.valueRgb = SpecsConfig.textColor;
                    build(e, "", timeText());
                }
                break;
            }
            default:
                e.visible = false;
                break;
        }
    }

    /** A plain value in the normal text colour, or the "not available" state when {@code text} is null. */
    private static void plain(Entry e, String label, String text) {
        if (text == null) {
            unavailable(e, label);
            return;
        }
        e.visible = true;
        e.valueRgb = SpecsConfig.textColor;
        build(e, label, text);
    }

    private static void unavailable(Entry e, String label) {
        e.visible = SpecsConfig.gpuUnavailable != UnavailableMode.HIDE;
        e.valueRgb = SpecsConfig.textColor;
        build(e, label, "N/A");
    }

    private static void temperature(Entry e, String label, int celsius, int warn, int crit) {
        if (celsius < 0) {
            unavailable(e, label);
            return;
        }
        e.visible = true;
        e.valueRgb = colorHigh(celsius, warn, crit);
        String v = SpecsConfig.gpuUnit == TempUnit.FAHRENHEIT
                ? Math.round(celsius * 9f / 5f + 32f) + " \u00B0F"
                : celsius + " \u00B0";
        build(e, label, v);
    }

    private static void memory(Entry e, String label, int usedMb, int totalMb) {
        if (usedMb < 0) {
            unavailable(e, label);
            return;
        }
        e.visible = true;
        int pct = totalMb > 0 ? Math.round(usedMb * 100f / totalMb) : 0;
        e.valueRgb = totalMb > 0 ? colorHigh(pct, SpecsConfig.memWarn, SpecsConfig.memCrit) : SpecsConfig.textColor;
        build(e, label, memText(usedMb, totalMb));
    }

    /** Memory change key at the precision that is shown (0.1 GB, or 10 MB under 1 GB), so the text is not rebuilt per frame. */
    private static long memKey(int usedMb, int totalMb) {
        long q = usedMb < 0 ? -1L : usedMb >= 1024 ? usedMb * 10L / 1024L : 100_000L + usedMb / 10L;
        return q * 1_000_003L + totalMb;
    }

    static String memText(int usedMb, int totalMb) {
        boolean withTotal = SpecsConfig.memoryShowTotal && totalMb > 0;
        boolean mb = withTotal ? totalMb < 1024 : usedMb < 1024;
        if (mb) {
            return withTotal ? usedMb + "/" + totalMb + " MB" : usedMb + " MB";
        }
        String used = String.format(Locale.ROOT, "%.1f", usedMb / 1024.0);
        return withTotal ? used + "/" + String.format(Locale.ROOT, "%.1f", totalMb / 1024.0) + " GB" : used + " GB";
    }

    private static String duration(long s) {
        long h = s / 3600;
        long m = s / 60 % 60;
        long sec = s % 60;
        return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec) : String.format(Locale.ROOT, "%d:%02d", m, sec);
    }

    private static boolean changed(Entry e, long key, long rev) {
        if (e.key == key && e.rev == rev) {
            return false;
        }
        e.key = key;
        e.rev = rev;
        return true;
    }

    private static String unit(String upper, String lower) {
        return SpecsConfig.uppercase ? upper : lower;
    }

    private static void build(Entry e, String label, String value) {
        boolean has = SpecsConfig.showLabels && !label.isEmpty();
        String l = has ? (SpecsConfig.uppercase ? label.toUpperCase(Locale.ROOT) : label) + ":" : "";
        e.label = l;
        e.value = value;
        e.labelCells = has ? l.length() + 1 : 0;
        e.full = has ? l + " " + value : value;
    }

    /** Lower is worse (FPS). */
    private static int colorLow(int v, int warn, int crit) {
        if (!SpecsConfig.thresholdColors) {
            return SpecsConfig.textColor;
        }
        return v < crit ? SpecsConfig.critColor : v < warn ? SpecsConfig.warnColor : SpecsConfig.textColor;
    }

    /** Higher is worse (ping, loss, temperature). */
    private static int colorHigh(int v, int warn, int crit) {
        if (!SpecsConfig.thresholdColors) {
            return SpecsConfig.textColor;
        }
        return v > crit ? SpecsConfig.critColor : v > warn ? SpecsConfig.warnColor : SpecsConfig.textColor;
    }

    private String timeText() {
        TimeFormatMode mode = SpecsConfig.timeFormat;
        boolean sec = SpecsConfig.timeSeconds;
        boolean lz = SpecsConfig.timeLeadingZero;
        String lang = SpecsConfig.timeLanguage;
        String key = mode + "|" + sec + "|" + lz + "|" + lang;
        if (timeFmt == null || !key.equals(timeFmtKey)) {
            timeFmtKey = key;
            try {
                timeLocale = lang.isEmpty() ? Locale.getDefault() : Locale.forLanguageTag(lang);
                switch (mode) {
                    case H24:
                        timeFmt = DateTimeFormatter.ofPattern((lz ? "HH" : "H") + ":mm" + (sec ? ":ss" : ""), timeLocale);
                        break;
                    case AUTO:
                        timeFmt = DateTimeFormatter.ofLocalizedTime(sec ? FormatStyle.MEDIUM : FormatStyle.SHORT)
                                .withLocale(timeLocale);
                        break;
                    default:
                        timeFmt = DateTimeFormatter.ofPattern((lz ? "hh" : "h") + ":mm" + (sec ? ":ss" : "") + " a", timeLocale);
                        break;
                }
            } catch (RuntimeException bad) {
                timeLocale = Locale.ROOT;
                timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
            }
        }
        // newer Java puts a narrow no-break space before AM / PM; Minecraft's font has no glyph for it
        String s = LocalTime.now().format(timeFmt).replace('\u202F', ' ').replace('\u00A0', ' ');
        return SpecsConfig.uppercase ? s.toUpperCase(timeLocale) : s;
    }
}

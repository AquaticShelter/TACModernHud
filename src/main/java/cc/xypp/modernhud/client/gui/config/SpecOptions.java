package cc.xypp.modernhud.client.gui.config;

import cc.xypp.modernhud.config.HudConfig;
import cc.xypp.modernhud.config.NumberInputMode;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reads a {@link ForgeConfigSpec} (the mod's storage layer: the .toml files and every static field the renderers read
 * stay exactly as they were) and turns each entry into a YACL {@link Option} with the right controller:
 * tick box, number field (click and type; or a slider for bounded numbers when animation.numberInput = SLIDER), enum cycler,
 * colour picker (hex strings) or text field.
 * Names come from the existing translation keys, tooltips from the spec comments.
 */
final class SpecOptions {

    private static final Pattern HEX = Pattern.compile("^#?[0-9a-fA-F]{6}$");

    private SpecOptions() {
    }

    /** One spec entry: its dotted path, the spec (metadata) and the live value holder. */
    record Entry(ForgeConfigSpec owner, String path, ForgeConfigSpec.ValueSpec spec, ForgeConfigSpec.ConfigValue<?> value) {
    }

    /** All entries of {@code spec} in declaration order. */
    static List<Entry> entries(ForgeConfigSpec spec) {
        List<Entry> out = new ArrayList<>();
        walk(spec, spec.getSpec(), new ArrayList<>(), out);
        return out;
    }

    private static void walk(ForgeConfigSpec owner, UnmodifiableConfig cfg, List<String> prefix, List<Entry> out) {
        for (UnmodifiableConfig.Entry e : cfg.entrySet()) {
            List<String> path = new ArrayList<>(prefix);
            path.add(e.getKey());
            Object v = e.getValue();
            if (v instanceof UnmodifiableConfig sub) {
                walk(owner, sub, path, out);
            } else if (v instanceof ForgeConfigSpec.ValueSpec vs) {
                Object cv = owner.getValues().get(path);
                if (cv instanceof ForgeConfigSpec.ConfigValue<?> c) {
                    out.add(new Entry(owner, String.join(".", path), vs, c));
                }
            }
        }
    }

    /** Builds the YACL option for one entry, or {@code null} for a type this screen does not edit. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Option<?> create(Entry en) {
        Object def = en.spec().getDefault();
        BASELINE.put(en.value(), en.value().get());   // what is stored right now (the screen may change the live value)
        Component name = nameOf(en);
        OptionDescription desc = descOf(en);
        ForgeConfigSpec.ConfigValue cv = en.value();

        if (def instanceof Boolean b) {
            return Option.<Boolean>createBuilder().name(name).description(desc)
                    .binding(b, () -> (Boolean) base(cv), v -> commit(cv, v))
                    .controller(TickBoxControllerBuilder::create).build();
        }
        if (def instanceof Integer i) {
            Number[] r = range(en.spec());
            int min = r[0] == null ? Integer.MIN_VALUE : r[0].intValue();
            int max = r[1] == null ? Integer.MAX_VALUE : r[1].intValue();
            boolean slider = HudConfig.numberInput == NumberInputMode.SLIDER && r[0] != null && r[1] != null && (long) max - min <= 1000;
            return Option.<Integer>createBuilder().name(name).description(desc)
                    .binding(i, () -> ((Number) base(cv)).intValue(), v -> commit(cv, v))
                    .controller(opt -> slider
                            ? IntegerSliderControllerBuilder.create(opt).range(min, max).step(1)
                            : IntegerFieldControllerBuilder.create(opt).min(min).max(max))
                    .build();
        }
        if (def instanceof Double d) {
            Number[] r = range(en.spec());
            double min = r[0] == null ? -1.0e9 : r[0].doubleValue();
            double max = r[1] == null ? 1.0e9 : r[1].doubleValue();
            boolean slider = HudConfig.numberInput == NumberInputMode.SLIDER && r[0] != null && r[1] != null && max - min <= 1000.0;
            double step = step(max - min);
            return Option.<Double>createBuilder().name(name).description(desc)
                    .binding(d, () -> ((Number) base(cv)).doubleValue(), v -> commit(cv, v))
                    .controller(opt -> slider
                            ? DoubleSliderControllerBuilder.create(opt).range(min, max).step(step)
                            : DoubleFieldControllerBuilder.create(opt).min(min).max(max))
                    .build();
        }
        if (def instanceof Enum<?> e) {
            return enumOption((Class) e.getDeclaringClass(), cv, e, name, desc);
        }
        if (def instanceof String s) {
            if (HEX.matcher(s).matches()) {
                return Option.<Color>createBuilder().name(name).description(desc)
                        .binding(parseColor(s), () -> parseColor(String.valueOf(base(cv))),
                                v -> commit(cv, String.format(Locale.ROOT, "#%06X", v.getRGB() & 0xFFFFFF)))
                        .controller(ColorControllerBuilder::create).build();
            }
            return Option.<String>createBuilder().name(name).description(desc)
                    .binding(s, () -> String.valueOf(base(cv)), v -> commit(cv, v))
                    .controller(StringControllerBuilder::create).build();
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <E extends Enum<E>> Option<E> enumOption(Class<E> cls, ForgeConfigSpec.ConfigValue cv, Enum<?> def,
                                                            Component name, OptionDescription desc) {
        return Option.<E>createBuilder().name(name).description(desc)
                .binding(cls.cast(def), () -> cls.cast(base(cv)), v -> commit(cv, v))
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(cls)
                        .valueFormatter(v -> Component.literal(pretty(v.name()))))
                .build();
    }

    // ------------------------------------------------------------------------------------------------ helpers

    /**
     * What YACL compares your edits against: the value that is actually stored. The live preview changes the spec's value
     * while you edit, so YACL must not read that one, or it would think nothing changed and skip saving.
     */
    private static final java.util.Map<Object, Object> BASELINE = new java.util.IdentityHashMap<>();

    private static Object base(ForgeConfigSpec.ConfigValue<?> cv) {
        Object b = BASELINE.get(cv);
        return b != null ? b : cv.get();
    }

    /** Apply / Done: the edit becomes the stored value. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void commit(ForgeConfigSpec.ConfigValue cv, Object value) {
        cv.set(value);
        BASELINE.put(cv, value);
    }

    /** The stored value of an entry (what Cancel puts back), or null if the screen never saw it. */
    static Object storedValue(ForgeConfigSpec.ConfigValue<?> cv) {
        return BASELINE.get(cv);
    }

    /** The value an option currently holds in the screen (not yet saved), converted to what the spec stores. */
    static Object pendingSpecValue(Option<?> option) {
        Object v = option.pendingValue();
        if (v instanceof Color c) {
            return String.format(Locale.ROOT, "#%06X", c.getRGB() & 0xFFFFFF);
        }
        return v;
    }

    private static Component nameOf(Entry en) {
        String key = en.spec().getTranslationKey();
        if (key != null && !key.isEmpty()) {
            return Component.translatable(key);
        }
        String leaf = en.path().substring(en.path().lastIndexOf('.') + 1);
        return Component.literal(pretty(leaf));
    }

    private static OptionDescription descOf(Entry en) {
        String c = en.spec().getComment();
        if (c == null || c.isBlank()) {
            return OptionDescription.EMPTY;
        }
        return OptionDescription.of(Component.literal(c.trim()));
    }

    /** {min, max} of a bounded numeric spec, via reflection (ForgeConfigSpec's Range type is not public); nulls if unbounded. */
    private static Number[] range(ForgeConfigSpec.ValueSpec spec) {
        Number[] out = new Number[2];
        try {
            Method gr = ForgeConfigSpec.ValueSpec.class.getMethod("getRange");
            Object range = gr.invoke(spec);
            if (range != null) {
                Method gmin = range.getClass().getDeclaredMethod("getMin");
                Method gmax = range.getClass().getDeclaredMethod("getMax");
                gmin.setAccessible(true);
                gmax.setAccessible(true);
                Object lo = gmin.invoke(range);
                Object hi = gmax.invoke(range);
                out[0] = lo instanceof Number n ? n : null;
                out[1] = hi instanceof Number n ? n : null;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // unbounded: the caller falls back to a plain number field
        }
        return out;
    }

    private static double step(double span) {
        if (span <= 2.0) {
            return 0.01;
        }
        if (span <= 20.0) {
            return 0.05;
        }
        if (span <= 100.0) {
            return 0.1;
        }
        return 1.0;
    }

    private static Color parseColor(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            return new Color(Integer.parseInt(h, 16));
        } catch (NumberFormatException e) {
            return Color.WHITE;
        }
    }

    /** "TOP_RIGHT" -> "Top right", "hitSoundVolume" -> "Hit sound volume". */
    static String pretty(String raw) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '_') {
                sb.append(' ');
            } else if (Character.isUpperCase(c) && i > 0 && Character.isLowerCase(raw.charAt(i - 1))) {
                sb.append(' ').append(Character.toLowerCase(c));
            } else {
                sb.append(sb.length() == 0 ? Character.toUpperCase(c) : Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}

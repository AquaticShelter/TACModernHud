package cc.xypp.modernhud.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

/** Reads and writes config values by their dotted path (for example {@code ammoGlow.enabled}). */
public final class ConfigPaths {

    private ConfigPaths() {
    }

    public static ForgeConfigSpec.ConfigValue<?> find(ForgeConfigSpec spec, String path) {
        Object o = spec.getValues().get(Arrays.asList(path.split("\\.")));
        return o instanceof ForgeConfigSpec.ConfigValue<?> cv ? cv : null;
    }

    /** Sets a value; a String is converted when the option is an enum. Returns false if the path does not exist. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean set(ForgeConfigSpec spec, String path, Object value) {
        ForgeConfigSpec.ConfigValue cv = find(spec, path);
        if (cv == null) {
            return false;
        }
        Object v = value;
        if (cv.get() instanceof Enum<?> current && value instanceof String name) {
            v = Enum.valueOf((Class) current.getDeclaringClass(), name);
        }
        if (!java.util.Objects.equals(cv.get(), v)) {
            cv.set(v);
        }
        return true;
    }

    /** Puts every option of the spec back to its default. Returns how many values changed. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static int resetAll(ForgeConfigSpec spec) {
        int[] changed = {0};
        walk(spec, spec.getSpec(), new ArrayList<>(), (cv, vs) -> {
            Object def = vs.getDefault();
            if (!java.util.Objects.equals(((ForgeConfigSpec.ConfigValue) cv).get(), def)) {
                ((ForgeConfigSpec.ConfigValue) cv).set(def);
                changed[0]++;
            }
        });
        return changed[0];
    }

    private static void walk(ForgeConfigSpec owner, UnmodifiableConfig cfg, List<String> prefix,
                             BiConsumer<ForgeConfigSpec.ConfigValue<?>, ForgeConfigSpec.ValueSpec> out) {
        for (UnmodifiableConfig.Entry e : cfg.entrySet()) {
            List<String> path = new ArrayList<>(prefix);
            path.add(e.getKey());
            Object v = e.getValue();
            if (v instanceof UnmodifiableConfig sub) {
                walk(owner, sub, path, out);
            } else if (v instanceof ForgeConfigSpec.ValueSpec vs
                    && owner.getValues().get(path) instanceof ForgeConfigSpec.ConfigValue<?> cv) {
                out.accept(cv, vs);
            }
        }
    }

    /** Writes the spec's file; ignores a spec that is not loaded in this context. */
    public static void save(ForgeConfigSpec spec) {
        try {
            spec.save();
        } catch (RuntimeException ignored) {
            // not loaded here
        }
    }
}

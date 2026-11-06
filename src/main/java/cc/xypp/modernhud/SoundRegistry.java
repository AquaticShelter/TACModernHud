package cc.xypp.modernhud;

import cc.xypp.modernhud.sound.CombatSoundType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SoundRegistry {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ModernHud.MOD_ID);

    /** Number of sound styles for the hit / kill / headshot-kill / shield-break cues. */
    public static final int STYLE_COUNT = 3;

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ModernHud.MOD_ID, name)));
    }

    /** Shield fully depleted: styles 1 and 2 share the original sound, style 3 has its own. */
    public static final RegistryObject<SoundEvent> ABSORPTION_BREAK = reg("absorption_break");
    public static final RegistryObject<SoundEvent> ABSORPTION_BREAK_3 = reg("absorption_break_3");

    /** A hit landed on active Absorption that did not break it (one sound for every style). */
    public static final RegistryObject<SoundEvent> ARMOR_HIT = reg("armor_hit");

    private static final RegistryObject<SoundEvent>[] HIT = set("hit");
    private static final RegistryObject<SoundEvent>[] KILL = set("kill");
    private static final RegistryObject<SoundEvent>[] HEADSHOT_KILL = set("headshot_kill");

    @SuppressWarnings("unchecked")
    private static RegistryObject<SoundEvent>[] set(String base) {
        RegistryObject<SoundEvent>[] out = new RegistryObject[STYLE_COUNT];
        for (int style = 1; style <= STYLE_COUNT; style++) {
            out[style - 1] = reg(style == 1 ? base : base + "_" + style);
        }
        return out;
    }

    private static int idx(int style) {
        return Math.max(1, Math.min(STYLE_COUNT, style)) - 1;
    }

    public static SoundEvent combat(CombatSoundType type, int style) {
        return switch (type) {
            case HIT -> HIT[idx(style)].get();
            case KILL -> KILL[idx(style)].get();
            case HEADSHOT_KILL -> HEADSHOT_KILL[idx(style)].get();
        };
    }

    public static SoundEvent shieldBreak(int style) {
        return style >= 3 ? ABSORPTION_BREAK_3.get() : ABSORPTION_BREAK.get();
    }

    private SoundRegistry() {
    }
}

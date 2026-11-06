package cc.xypp.modernhud.client.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

/**
 * LesRaisins Tactical Equipments ("lrtactical") soft dependency: only classifies stacks as melee / throwable /
 * consumable. lrtactical registers GENERIC items (e.g. {@code lrtactical:throwable}, {@code lrtactical:consumable})
 * and the pack-defined variant lives in the stack's NBT (e.g. {@code ThrowableId}), so detection is by registry
 * name (with a class-name fallback) and never touches an lrtactical type - no compile-time dependency.
 */
public final class LrTacticalCompat {

    public enum Kind { NONE, MELEE, THROWABLE, CONSUMABLE }

    private static final boolean LOADED = ModList.get().isLoaded("lrtactical");

    private LrTacticalCompat() {
    }

    public static boolean loaded() {
        return LOADED;
    }

    public static Kind kind(ItemStack stack) {
        if (!LOADED || stack.isEmpty()) {
            return Kind.NONE;
        }
        Item item = stack.getItem();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        boolean ours = id != null && "lrtactical".equals(id.getNamespace());
        if (ours) {
            Kind k = fromWord(id.getPath());
            if (k != Kind.NONE) {
                return k;
            }
        }
        for (Class<?> c = item.getClass(); c != null && c != Item.class && c != Object.class; c = c.getSuperclass()) {
            if (ours || c.getName().toLowerCase(Locale.ROOT).contains("lrtactical")) {
                Kind k = fromWord(c.getSimpleName().toLowerCase(Locale.ROOT));
                if (k != Kind.NONE) {
                    return k;
                }
            }
        }
        return Kind.NONE;
    }

    private static Kind fromWord(String w) {
        if (w.contains("melee") || w.contains("shield")) { // flash_shield shows in the melee slot
            return Kind.MELEE;
        }
        if (w.contains("throw") || w.contains("grenade")) {
            return Kind.THROWABLE;
        }
        if (w.contains("consum")) {
            return Kind.CONSUMABLE;
        }
        return Kind.NONE;
    }

    /** The flash shield: classified as MELEE (shares that slot) but a real melee weapon takes precedence. */
    public static boolean isShield(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && "lrtactical".equals(id.getNamespace()) && id.getPath().contains("shield");
    }

    /** Identity of an item TYPE for counting: registry name plus the pack's id tag (e.g. which grenade). */
    public static String key(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String base = id == null ? stack.getItem().getClass().getName() : id.toString();
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            for (String k : tag.getAllKeys()) {
                if (k.toLowerCase(Locale.ROOT).endsWith("id") && tag.contains(k, 8)) {
                    return base + "|" + tag.getString(k);
                }
            }
        }
        return base;
    }
}

package cc.xypp.modernhud.network;

import cc.xypp.modernhud.client.ClientHitTracker;
import cc.xypp.modernhud.client.HitIndicatorType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sent server -> client, individually, to the ONE player who should see this indicator - never
 *  broadcast. Carries the damage source's last known world position (or the origin, for a
 *  non-directional hit with no real source - see {@code HitIndicationEvents#sendIndicator}), the
 *  indicator's type, the damage as a percent of the recipient's max health (only used by
 *  {@code sizeDependsOnDamage}), and whether it came from a non-damaging negative-effect potion. */
public class AddHitIndicatorPacket {

    private final double x;
    private final double y;
    private final double z;
    private final HitIndicatorType type;
    private final int damagePercent;
    private final boolean fromNegativePotion;
    /** The recipient still had Absorption left after this hit (so it did not break through to health yet). */
    private final boolean shielded;

    public AddHitIndicatorPacket(double x, double y, double z, HitIndicatorType type,
                                  int damagePercent, boolean fromNegativePotion, boolean shielded) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.damagePercent = damagePercent;
        this.fromNegativePotion = fromNegativePotion;
        this.shielded = shielded;
    }

    public static void encode(AddHitIndicatorPacket packet, FriendlyByteBuf buf) {
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
        buf.writeVarInt(packet.type.ordinal());
        buf.writeVarInt(packet.damagePercent);
        buf.writeBoolean(packet.fromNegativePotion);
        buf.writeBoolean(packet.shielded);
    }

    public static AddHitIndicatorPacket decode(FriendlyByteBuf buf) {
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        HitIndicatorType type = HitIndicatorType.fromWire(buf.readVarInt());
        int damagePercent = buf.readVarInt();
        boolean fromNegativePotion = buf.readBoolean();
        boolean shielded = buf.readBoolean();
        return new AddHitIndicatorPacket(x, y, z, type, damagePercent, fromNegativePotion, shielded);
    }

    public static void handle(AddHitIndicatorPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientHitTracker.addIndicator(packet.x, packet.y, packet.z, packet.type,
                        packet.damagePercent, packet.fromNegativePotion, packet.shielded)));
        ctx.setPacketHandled(true);
    }
}

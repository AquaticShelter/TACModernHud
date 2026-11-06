package cc.xypp.modernhud.network;

import cc.xypp.modernhud.client.ClientHitTracker;
import cc.xypp.modernhud.client.HitMarkerType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sent server -> client, individually, to the ONE player who should flash this crit/kill marker. */
public class SetHitMarkerPacket {

    private final HitMarkerType type;

    public SetHitMarkerPacket(HitMarkerType type) {
        this.type = type;
    }

    public static void encode(SetHitMarkerPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.type.ordinal());
    }

    public static SetHitMarkerPacket decode(FriendlyByteBuf buf) {
        return new SetHitMarkerPacket(HitMarkerType.fromWire(buf.readVarInt()));
    }

    public static void handle(SetHitMarkerPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientHitTracker.setMarker(packet.type)));
        ctx.setPacketHandled(true);
    }
}

package cc.xypp.modernhud.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent server -> client, individually, to exactly the two people involved in
 * an Absorption hit: the attacker and the victim. Never broadcast to anyone
 * else - by design, only these two should hear/see anything.
 */
public class ShieldFeedbackPacket {

    private final boolean fullyBroken;
    private final boolean recipientIsAttacker;
    /** True when the hit was lethal: the kill cue takes over, so the shield cue stays silent (the icon still shows). */
    private final boolean silent;

    public ShieldFeedbackPacket(boolean fullyBroken, boolean recipientIsAttacker, boolean silent) {
        this.fullyBroken = fullyBroken;
        this.recipientIsAttacker = recipientIsAttacker;
        this.silent = silent;
    }

    public static void encode(ShieldFeedbackPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.fullyBroken);
        buf.writeBoolean(packet.recipientIsAttacker);
        buf.writeBoolean(packet.silent);
    }

    public static ShieldFeedbackPacket decode(FriendlyByteBuf buf) {
        boolean fullyBroken = buf.readBoolean();
        boolean recipientIsAttacker = buf.readBoolean();
        boolean silent = buf.readBoolean();
        return new ShieldFeedbackPacket(fullyBroken, recipientIsAttacker, silent);
    }

    public static void handle(ShieldFeedbackPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        cc.xypp.modernhud.client.ClientFeedback.handle(packet.fullyBroken, packet.recipientIsAttacker, packet.silent))
        );
        ctx.setPacketHandled(true);
    }
}

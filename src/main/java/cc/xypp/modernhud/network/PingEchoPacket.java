package cc.xypp.modernhud.network;

import cc.xypp.modernhud.client.spec.LatencyProbe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: the probe, sent back unchanged. */
public class PingEchoPacket {

    private final int seq;
    private final long clientNanos;

    public PingEchoPacket(int seq, long clientNanos) {
        this.seq = seq;
        this.clientNanos = clientNanos;
    }

    public static void encode(PingEchoPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.seq);
        buf.writeLong(p.clientNanos);
    }

    public static PingEchoPacket decode(FriendlyByteBuf buf) {
        return new PingEchoPacket(buf.readVarInt(), buf.readLong());
    }

    public static void handle(PingEchoPacket p, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        // measured right here on the network thread: waiting for the game thread would add up to a frame of error
        long now = System.nanoTime();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> LatencyProbe.onEcho(p.seq, p.clientNanos, now));
        ctx.setPacketHandled(true);
    }
}

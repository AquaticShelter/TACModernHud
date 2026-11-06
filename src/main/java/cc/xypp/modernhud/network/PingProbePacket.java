package cc.xypp.modernhud.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Client -> server: "answer me right now". The client's own clock value travels along and comes back untouched, so no
 * clock has to be synchronised. The server answers on the network thread, WITHOUT waiting for the next game tick, so
 * the measured time is the network round trip and not up to 50 ms of tick waiting.
 */
public class PingProbePacket {

    /** The server ignores a player who sends faster than this (10 per second is far more than the client ever sends). */
    private static final long MIN_GAP_NANOS = 100_000_000L;
    private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();

    private final int seq;
    private final long clientNanos;

    public PingProbePacket(int seq, long clientNanos) {
        this.seq = seq;
        this.clientNanos = clientNanos;
    }

    public static void encode(PingProbePacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.seq);
        buf.writeLong(p.clientNanos);
    }

    public static PingProbePacket decode(FriendlyByteBuf buf) {
        return new PingProbePacket(buf.readVarInt(), buf.readLong());
    }

    public static void handle(PingProbePacket p, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ServerPlayer player = ctx.getSender();
        if (player != null) {
            long now = System.nanoTime();
            Long prev = LAST.put(player.getUUID(), now);
            if (prev == null || now - prev >= MIN_GAP_NANOS) {
                PingChannel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PingEchoPacket(p.seq, p.clientNanos));
            }
        }
        ctx.setPacketHandled(true);
    }

    /** Forget a player who left. */
    public static void forget(UUID id) {
        LAST.remove(id);
    }
}

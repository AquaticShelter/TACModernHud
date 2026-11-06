package cc.xypp.modernhud.network;

import cc.xypp.modernhud.ModernHud;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * A tiny channel of its own for measuring the real round-trip time to the server (see {@code LatencyProbe}).
 *
 * <p>It is separate from {@link ModNetworking} on purpose: it has its own protocol version, and a client or server that
 * does not have it (an older build, or no mod at all) is simply skipped: the readout then falls back to the tab-list
 * ping. Nothing here can block a join.
 */
public final class PingChannel {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ModernHud.MOD_ID, "ping"),
            () -> PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
    );

    private PingChannel() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, PingProbePacket.class,
                PingProbePacket::encode, PingProbePacket::decode, PingProbePacket::handle);
        CHANNEL.registerMessage(1, PingEchoPacket.class,
                PingEchoPacket::encode, PingEchoPacket::decode, PingEchoPacket::handle);
    }
}

package cc.xypp.modernhud.client.spec;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * A pass-through handler on the connection's network pipeline. It only looks at the time-update packet the server sends
 * every second (see {@link PacketLossTracker}) and forwards every packet unchanged, so it cannot change the game.
 * No mixin is needed: the handler is added when you join a server.
 */
final class NetProbe extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LogManager.getLogger("ModernHud/Specs");
    private static final String NAME = "modernhud_netprobe";

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ClientboundSetTimePacket time) {
            long now = System.nanoTime();
            PacketLossTracker.onTimePacket(now);
            TpsTracker.onTime(now, time.getGameTime(), Minecraft.getInstance().isPaused());
        }
        ctx.fireChannelRead(msg); // always pass the packet on, untouched
    }

    /** Adds the probe in front of the vanilla packet handler. Never throws. */
    static void install(Connection connection) {
        try {
            Channel channel = channelOf(connection);
            if (channel == null || channel.pipeline().get(NAME) != null) {
                return;
            }
            if (channel.pipeline().get("packet_handler") != null) {
                channel.pipeline().addBefore("packet_handler", NAME, new NetProbe());
            } else {
                channel.pipeline().addLast(NAME, new NetProbe());
            }
        } catch (RuntimeException | ReflectiveOperationException e) {
            LOGGER.warn("Packet loss estimate unavailable: {}", e.toString());
        }
    }

    /** Finds the connection's netty channel by TYPE, so it works with every field name (mapped or obfuscated). */
    private static Channel channelOf(Connection connection) throws ReflectiveOperationException {
        for (Field f : Connection.class.getDeclaredFields()) {
            if (Channel.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return (Channel) f.get(connection);
            }
        }
        return null;
    }
}

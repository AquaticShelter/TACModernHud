package cc.xypp.modernhud.network;

import cc.xypp.modernhud.ModernHud;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetworking {

    private static final String PROTOCOL_VERSION = "3";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ModernHud.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            // the mod is optional on either side: a player without it can join a server that has it, and the other way round
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
    );

    private static int nextId = 0;

    /** Sends to one player, and only if that player's client actually has this mod (otherwise it is skipped silently). */
    public static void sendTo(ServerPlayer player, Object message) {
        if (player != null && player.connection != null && CHANNEL.isRemotePresent(player.connection.connection)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }

    private ModNetworking() {
    }

    public static void register() {
        PingChannel.register(); // separate channel for the live ping probe
        CHANNEL.registerMessage(nextId++, ShieldFeedbackPacket.class,
                ShieldFeedbackPacket::encode,
                ShieldFeedbackPacket::decode,
                ShieldFeedbackPacket::handle);
        CHANNEL.registerMessage(nextId++, AddHitIndicatorPacket.class,
                AddHitIndicatorPacket::encode,
                AddHitIndicatorPacket::decode,
                AddHitIndicatorPacket::handle);
        CHANNEL.registerMessage(nextId++, SetHitMarkerPacket.class,
                SetHitMarkerPacket::encode,
                SetHitMarkerPacket::decode,
                SetHitMarkerPacket::handle);
        CHANNEL.registerMessage(nextId++, cc.xypp.modernhud.sound.CombatSoundPacket.class,
                cc.xypp.modernhud.sound.CombatSoundPacket::encode,
                cc.xypp.modernhud.sound.CombatSoundPacket::decode,
                cc.xypp.modernhud.sound.CombatSoundPacket::handle);
    }
}

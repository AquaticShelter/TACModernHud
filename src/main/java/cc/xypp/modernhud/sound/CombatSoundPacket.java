package cc.xypp.modernhud.sound;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> exactly one client (the attacker): which combat cue to play. Played locally only. */
public class CombatSoundPacket {

    private final CombatSoundType type;

    public CombatSoundPacket(CombatSoundType type) {
        this.type = type;
    }

    public static void encode(CombatSoundPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.type);
    }

    public static CombatSoundPacket decode(FriendlyByteBuf buf) {
        return new CombatSoundPacket(buf.readEnum(CombatSoundType.class));
    }

    public static void handle(CombatSoundPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> cc.xypp.modernhud.client.ClientCombatSounds.play(msg.type)));
        ctx.setPacketHandled(true);
    }
}

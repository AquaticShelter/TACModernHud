package cc.xypp.modernhud.mixin;

import cc.xypp.modernhud.compass.tacz.TaczGunfireBridge;
import com.tacz.guns.network.message.ServerMessageSound;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent.Context;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
   targets = {"com.tacz.guns.network.message.ServerMessageSound"},
   remap = false
)
public abstract class TaczServerMessageSoundMixin {
   @Inject(
      method = {"handle"},
      at = {@At("HEAD")},
      remap = false
   )
   private static void modernhud$compassOnHandle(ServerMessageSound message, Supplier<Context> contextSupplier, CallbackInfo callbackInfo) {
      if (contextSupplier.get().getDirection().getReceptionSide().isClient()) {
         TaczGunfireBridge.onSoundPacket(message.getEntityId(), message.getSoundName());
      }
   }
}

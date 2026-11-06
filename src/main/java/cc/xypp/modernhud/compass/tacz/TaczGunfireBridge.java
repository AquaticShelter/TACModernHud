package cc.xypp.modernhud.compass.tacz;

import java.util.concurrent.ConcurrentLinkedQueue;

public final class TaczGunfireBridge {
   private static final String SHOOT_SOUND = "shoot_3p";
   private static final String SILENCED_SOUND = "silence_3p";
   private static final ConcurrentLinkedQueue<TaczGunfireBridge.GunfireNotice> NOTICES = new ConcurrentLinkedQueue<>();

   private TaczGunfireBridge() {
   }

   public static void onSoundPacket(int shooterEntityId, String soundName) {
      if ("shoot_3p".equals(soundName) || "silence_3p".equals(soundName)) {
         NOTICES.add(new TaczGunfireBridge.GunfireNotice(shooterEntityId, System.nanoTime()));
      }
   }

   static TaczGunfireBridge.GunfireNotice poll() {
      return NOTICES.poll();
   }

   static void clear() {
      NOTICES.clear();
   }

   static record GunfireNotice(int shooterEntityId, long receivedNanos) {
   }
}

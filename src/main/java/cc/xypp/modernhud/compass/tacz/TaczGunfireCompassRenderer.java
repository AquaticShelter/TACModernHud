package cc.xypp.modernhud.compass.tacz;

import cc.xypp.modernhud.compass.CompassTaczRenderer;
import cc.xypp.modernhud.compass.CompassTaczStyle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class TaczGunfireCompassRenderer implements CompassTaczRenderer {
   private static final float HALF_COMPASS_SPAN_DEGREES = 45.0F;
   private static final int MIN_SAFE_FONT_ALPHA = 4;
   private final Map<UUID, TaczGunfireCompassRenderer.GunfireMarker> markers = new HashMap<>();
   private ClientLevel trackedLevel;

   @Override
   public void render(
      GuiGraphics gui,
      Minecraft mc,
      float partialTick,
      float compassAngle,
      int centerX,
      int scissorLeft,
      int scissorRight,
      double pixelsPerDegree,
      float screenScale,
      CompassTaczStyle style
   ) {
      if (!style.enabled()) {
         this.markers.clear();
         TaczGunfireBridge.clear();
      } else if (mc.level != null && mc.player != null) {
         if (this.trackedLevel != mc.level) {
            this.trackedLevel = mc.level;
            this.markers.clear();
         }

         long visibleNanos = secondsToNanos(style.visibleDurationSeconds());
         long fadeNanos = secondsToNanos(style.fadeDurationSeconds());
         long totalNanos = saturatingAdd(visibleNanos, fadeNanos);
         this.drainNotices(mc, partialTick);
         long now = System.nanoTime();
         Vec3 playerPosition = mc.player.getPosition(partialTick);
         Iterator<TaczGunfireCompassRenderer.GunfireMarker> iterator = this.markers.values().iterator();

         while (iterator.hasNext()) {
            TaczGunfireCompassRenderer.GunfireMarker marker = iterator.next();
            long sinceShot = Math.max(0L, now - marker.lastShotNanos);
            if (sinceShot > totalNanos) {
               iterator.remove();
            } else {
               Entity entity = mc.level.getEntity(marker.entityId);
               if (entity instanceof LivingEntity) {
                  LivingEntity shooter = (LivingEntity)entity;
                  if (marker.shooterId.equals(shooter.getUUID()) && !isEnemy(mc.player, shooter)) {
                     iterator.remove();
                     continue;
                  }
               }

               double deltaX = marker.x - playerPosition.x;
               double deltaY = marker.y - playerPosition.y;
               double deltaZ = marker.z - playerPosition.z;
               if (!(deltaX * deltaX + deltaZ * deltaZ < 1.0E-6)) {
                  float opacity = fadeOpacity(sinceShot, visibleNanos, fadeNanos);
                  float intro = Mth.clamp((float)((double)(now - marker.popStartedNanos) / (double)secondsToNanos(style.popDurationSeconds())), 0.0F, 1.0F);
                  int color = multiplyAlpha(style.color(), opacity);
                  if (ARGB32.alpha(color) >= 4) {
                     float targetBearing = (float)Math.toDegrees(Math.atan2(deltaX, -deltaZ));
                     float bearingDelta = Mth.wrapDegrees(targetBearing - compassAngle);
                     float distanceScale = distanceScale(Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ), style);
                     if (Math.abs(bearingDelta) <= 45.0F) {
                        float markerX = (float)centerX + (float)((double)bearingDelta * pixelsPerDegree);
                        renderEnemyIcon(
                           gui,
                           markerX,
                           (float)(style.iconY() * (double)screenScale),
                           (float)(style.iconScale() * (double)screenScale) * distanceScale,
                           color,
                           intro
                        );
                     } else {
                        renderEdgeIndicator(gui, mc, color, bearingDelta, scissorLeft, scissorRight, screenScale, style);
                     }
                  }
               }
            }
         }
      } else {
         this.trackedLevel = null;
         this.markers.clear();
         TaczGunfireBridge.clear();
      }
   }

   private void drainNotices(Minecraft mc, float partialTick) {
      TaczGunfireBridge.GunfireNotice notice;
      while ((notice = TaczGunfireBridge.poll()) != null) {
         Entity position = mc.level.getEntity(notice.shooterEntityId());
         if (position instanceof LivingEntity) {
            LivingEntity shooter = (LivingEntity)position;
            if (isEnemy(mc.player, shooter)) {
               Vec3 positionx = shooter.getPosition(partialTick);
               long handledAt = System.nanoTime();
               this.markers
                  .put(
                     shooter.getUUID(),
                     new TaczGunfireCompassRenderer.GunfireMarker(
                        shooter.getUUID(),
                        notice.shooterEntityId(),
                        notice.receivedNanos(),
                        handledAt,
                        positionx.x,
                        positionx.y,
                        positionx.z
                     )
                  );
            }
         }
      }
   }

   private static boolean isEnemy(Player localPlayer, LivingEntity shooter) {
      return shooter != localPlayer && !localPlayer.isAlliedTo(shooter) ? shooter instanceof Player || shooter instanceof Enemy : false;
   }

   private static void renderEnemyIcon(GuiGraphics gui, float x, float y, float baseScale, int color, float intro) {
      float popScale = 1.0F + 0.5F * (1.0F - smoothStep(intro));
      // pixel-exact diamond (the old rotated 5x5 quad lost pixels at fractional scales)
      cc.xypp.modernhud.client.PixelDraw.diamond(gui, x, y, 3.5F * baseScale * popScale, true, color);
   }

   private static void renderEdgeIndicator(
      GuiGraphics gui, Minecraft mc, int color, float bearingDelta, int scissorLeft, int scissorRight, float screenScale, CompassTaczStyle style
   ) {
      if (ARGB32.alpha(color) >= 4) {
         float gap = (float)(style.edgeGap() * (double)screenScale);
         float x = bearingDelta < 0.0F ? (float)scissorLeft - gap : (float)scissorRight + gap;
         float y = (float)(style.iconY() * (double)screenScale);
         cc.xypp.modernhud.client.PixelDraw.arrowHead(gui, x, y, 2.2F, bearingDelta < 0.0F, color);
      }
   }

   private static float fadeOpacity(long sinceShot, long visibleNanos, long fadeNanos) {
      return sinceShot <= visibleNanos ? 1.0F : 1.0F - Mth.clamp((float)((double)(sinceShot - visibleNanos) / (double)fadeNanos), 0.0F, 1.0F);
   }

   private static float smoothStep(float value) {
      float clamped = Mth.clamp(value, 0.0F, 1.0F);
      return clamped * clamped * (3.0F - 2.0F * clamped);
   }

   private static float distanceScale(double distance, CompassTaczStyle style) {
      double maxScaleDistance = Math.max(0.0, style.maxScaleDistance());
      double minScaleDistance = Math.max(maxScaleDistance + 1.0E-6, style.minScaleDistance());
      float progress = Mth.clamp((float)((distance - maxScaleDistance) / (minScaleDistance - maxScaleDistance)), 0.0F, 1.0F);
      float maxScale = (float)(style.maxScalePercent() / 100.0);
      float minScale = (float)(style.minScalePercent() / 100.0);
      return Mth.lerp(progress, maxScale, minScale);
   }

   private static int multiplyAlpha(int color, float multiplier) {
      int alpha = Mth.clamp((int)((float)ARGB32.alpha(color) * multiplier), 0, 255);
      return color & 16777215 | alpha << 24;
   }

   private static long secondsToNanos(double seconds) {
      return Math.max(1L, (long)(seconds * 1.0E9));
   }

   private static long saturatingAdd(long left, long right) {
      long result = left + right;
      return result < 0L ? Long.MAX_VALUE : result;
   }

   private static final class GunfireMarker {
      private final UUID shooterId;
      private final int entityId;
      private final long lastShotNanos;
      private final long popStartedNanos;
      private final double x;
      private final double y;
      private final double z;

      private GunfireMarker(UUID shooterId, int entityId, long shotNanos, long handledAtNanos, double x, double y, double z) {
         this.shooterId = shooterId;
         this.entityId = entityId;
         this.popStartedNanos = handledAtNanos;
         this.lastShotNanos = shotNanos;
         this.x = x;
         this.y = y;
         this.z = z;
      }
   }
}

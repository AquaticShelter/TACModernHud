package cc.xypp.modernhud.compass;

public record CompassTaczStyle(
   boolean enabled,
   double iconScale,
   double iconY,
   int color,
   double maxScalePercent,
   double minScalePercent,
   double maxScaleDistance,
   double minScaleDistance,
   double popDurationSeconds,
   double visibleDurationSeconds,
   double fadeDurationSeconds,
   double edgeTextScale,
   double edgeY,
   double edgeGap
) {
}

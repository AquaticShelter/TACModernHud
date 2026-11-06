package cc.xypp.modernhud.compass;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

@FunctionalInterface
public interface CompassTaczRenderer {
   CompassTaczRenderer NONE = (gui, mc, partialTick, compassAngle, centerX, scissorLeft, scissorRight, pixelsPerDegree, screenScale, style) -> {
   };

   void render(GuiGraphics var1, Minecraft var2, float var3, float var4, int var5, int var6, int var7, double var8, float var10, CompassTaczStyle var11);
}

package cc.xypp.modernhud.compass;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Ping Wheel enemy markers on the compass. Ping Wheel is a soft dependency that is NOT compiled against: every member is
 * looked up by name through reflection (the names are exactly the ones the standalone Modern Compass used), so a
 * missing / changed Ping Wheel simply disables the markers. Markers are drawn as a small diamond in the ping's own colour
 * (Ping Wheel's animated item icon needs its private draw context), with the same "<<" / ">>" edge hints as before.
 */
public final class PingWheelCompassRenderer implements CompassPingRenderer {
    private static final float HALF_SPAN = 45.0F;

    private final Object pingRepo;
    private final Method isEnemyMarker;
    private final Field dimension;
    private final Field entityId;
    private final Method getPosition;
    private final Method getIntroProgress;
    private final Method applyMarkerStyle;
    private final Method getTeamColor;
    private final Method gcGetDimension;
    private final Method gcGetEntity;
    private final Object clientConfig;
    private final Method getTeamColorMode;

    public PingWheelCompassRenderer() throws ReflectiveOperationException {
        Class<?> pingManager = Class.forName("nx.pingwheel.common.core.PingManager");
        this.pingRepo = pingManager.getField("PING_REPO").get(null);
        Class<?> pingView = Class.forName("nx.pingwheel.common.core.PingView");
        this.isEnemyMarker = pingView.getMethod("isEnemyMarker");
        this.dimension = pingView.getField("dimension");
        this.entityId = pingView.getField("entityId");
        this.getPosition = pingView.getMethod("getPosition");
        this.getIntroProgress = pingView.getMethod("getIntroProgress", float.class);
        this.applyMarkerStyle = pingView.getMethod("applyMarkerStyle", int.class, float.class);
        this.getTeamColor = pingView.getMethod("getTeamColor");
        Class<?> gameContext = Class.forName("nx.pingwheel.common.core.GameContext");
        this.gcGetDimension = gameContext.getMethod("getDimension");
        Method found = null;
        for (Method m : gameContext.getMethods()) {
            if (m.getName().equals("getEntity") && m.getParameterCount() == 1) {
                found = m;
                break;
            }
        }
        if (found == null) {
            throw new NoSuchMethodException("GameContext.getEntity");
        }
        this.gcGetEntity = found;
        Class<?> clientConfigClass = Class.forName("nx.pingwheel.common.config.ClientConfig");
        Object handler = clientConfigClass.getField("HANDLER").get(null);
        this.clientConfig = handler.getClass().getMethod("getConfig").invoke(handler);
        this.getTeamColorMode = clientConfig.getClass().getMethod("getTeamColorMode");
    }

    @Override
    public void render(GuiGraphics gui, Minecraft mc, float partialTick, float compassAngle, int centerX, int scissorLeft,
                       int scissorRight, double pixelsPerDegree, float screenScale, CompassPingStyle style) {
        if (mc.player == null || !(pingRepo instanceof Iterable<?> repo)) {
            return;
        }
        try {
            Object currentDimension = gcGetDimension.invoke(null);
            Vec3 playerPosition = mc.player.getPosition(partialTick);
            for (Object ping : repo) {
                if (!(Boolean) isEnemyMarker.invoke(ping) || !currentDimension.equals(dimension.get(ping))) {
                    continue;
                }
                Object id = entityId.get(ping);
                Entity target = id == null ? null : (Entity) gcGetEntity.invoke(null, id);
                Vec3 targetPosition = target == null ? (Vec3) getPosition.invoke(ping) : target.getPosition(partialTick);
                double dx = targetPosition.x - playerPosition.x;
                double dz = targetPosition.z - playerPosition.z;
                if (dx * dx + dz * dz < 1.0E-6) {
                    continue;
                }
                float bearing = (float) Math.toDegrees(Math.atan2(dx, -dz));
                float delta = Mth.wrapDegrees(bearing - compassAngle);
                int color = markerColor(ping, partialTick);
                if (ARGB32.alpha(color) < 4) {
                    continue;
                }
                if (Math.abs(delta) <= HALF_SPAN) {
                    float x = centerX + (float) (delta * pixelsPerDegree);
                    renderDiamond(gui, x, (float) (style.iconY() * screenScale), (float) (style.iconScale() * screenScale), color);
                } else {
                    float intro = (Float) getIntroProgress.invoke(ping, partialTick);
                    renderEdge(gui, mc, multiplyAlpha(color, intro), delta, scissorLeft, scissorRight, screenScale, style);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Ping Wheel changed under us: skip this frame's markers rather than crash the HUD
        }
    }

    private int markerColor(Object ping, float partialTick) throws ReflectiveOperationException {
        Object mode = getTeamColorMode.invoke(clientConfig);
        String name = mode == null ? "" : mode.toString();
        boolean team = name.equals("FULL") || name.equals("PING_ONLY");
        int base = team ? (Integer) getTeamColor.invoke(ping) : -1;
        return (Integer) applyMarkerStyle.invoke(ping, base, partialTick);
    }

    private static void renderDiamond(GuiGraphics gui, float x, float y, float scale, int color) {
        gui.pose().pushPose();
        gui.pose().translate(x, y, 200.0F);
        gui.pose().scale(scale, scale, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gui.pose().mulPose(Axis.ZP.rotationDegrees(45.0F));
        gui.pose().translate(-2.5F, -2.5F, 0.0F);
        gui.fill(0, 0, 5, 5, color);
        RenderSystem.disableBlend();
        gui.pose().popPose();
    }

    private static void renderEdge(GuiGraphics gui, Minecraft mc, int color, float delta, int left, int right,
                                   float screenScale, CompassPingStyle style) {
        if (ARGB32.alpha(color) < 4) {
            return;
        }
        Component indicator = Component.literal(delta < 0.0F ? "<<" : ">>")
                .withStyle(s -> s.withBold(true).withColor(TextColor.fromRgb(color & 0xFFFFFF)));
        int width = mc.font.width(indicator);
        float scale = (float) (style.edgeTextScale() * screenScale);
        float gap = (float) (style.edgeGap() * screenScale);
        float x = delta < 0.0F ? left - gap : right + gap;
        float y = (float) (style.edgeY() * screenScale);
        gui.pose().pushPose();
        gui.pose().translate(x, y, 200.0F);
        gui.pose().scale(scale, scale, 1.0F);
        gui.drawString(mc.font, indicator, delta < 0.0F ? -width : 0, 0, color, false);
        gui.pose().popPose();
    }

    private static int multiplyAlpha(int color, float m) {
        int a = Mth.clamp((int) (ARGB32.alpha(color) * m), 0, 255);
        return (color & 0xFFFFFF) | (a << 24);
    }
}

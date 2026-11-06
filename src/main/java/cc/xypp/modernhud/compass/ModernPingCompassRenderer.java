package cc.xypp.modernhud.compass;

import cc.xypp.modernhud.client.PixelDraw;
import cc.xypp.modernhud.config.CompassConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Modern Ping (dev.szo9k4.modernping) on the compass. Modern Ping is NOT compiled against: its ping list is read by
 * reflection, so a missing or changed Modern Ping just turns the markers off. Because Modern Ping delivers every ping to
 * the whole team / channel, everything shown here appears on every teammate's compass (they need ModernHud too).
 *
 * <p>Pings are sorted into categories, each with its own switch, colour and shape (see the {@code pings} config):
 * <ul>
 *   <li>ENEMY PLAYER - filled diamond. A ping that landed on a player who is not on your team (Modern Ping flags it itself).</li>
 *   <li>HOSTILE MOB - filled square. Any {@link Enemy} that is not allied to you.</li>
 *   <li>VEHICLE - filled triangle. An entity whose class matches {@code vehicleClassPrefixes} (SuperbWarfare by default)
 *       and that is not yours: yours = same scoreboard team, or a teammate is riding it.</li>
 *   <li>LOCATION - hollow diamond. A ping that hit no entity.</li>
 *   <li>OTHER - hollow square. Any other entity.</li>
 * </ul>
 */
public final class ModernPingCompassRenderer implements CompassPingRenderer {
    private static final Logger LOGGER = LogManager.getLogger("ModernHud/Ping");
    private final Map<Object, Boolean> logged = new java.util.WeakHashMap<>();
    private static final float HALF_SPAN = 45.0F;
    private static final int PLAYER = 0, HOSTILE = 1, VEHICLE = 2, LOCATION = 3, OTHER = 4;
    private static final int FRIENDLY = 0, ENEMY = 1, NEUTRAL = 2;

    private final Object pingRepo;
    private final Method isEnemyMarker;
    private final Field entityId;
    private final Field dimension;
    private final Method getPosition;
    private final Method getIntroProgress;
    private final Method applyMarkerStyle;
    private final Method getTeamColor;
    private final Field age;                    // private; optional
    private final Method gcGetDimension;
    private final Method gcGetEntity;
    private final Method isTeammate;            // TacticalMarkerManager.isTeammate(Player); optional
    private final Object clientConfig;          // optional
    private final Method getPingDuration;       // optional

    /** ping -> {entity, refreshTick}: resolving an entity by UUID walks the entity list, so do it a few times a second. */
    private final Map<Object, Object[]> entityCache = new IdentityHashMap<>();
    private final String[] vehiclePrefixCache = new String[1];
    private String[] vehiclePrefixes = new String[0];
    private final String[] vehicleNsCache = new String[1];
    private String[] vehicleNamespaces = new String[0];

    public ModernPingCompassRenderer() throws ReflectiveOperationException {
        String root = "dev.szo9k4.modernping.common.";
        Class<?> pingManager = Class.forName(root + "core.PingManager");
        this.pingRepo = pingManager.getField("PING_REPO").get(null);
        Class<?> pingView = Class.forName(root + "core.PingView");
        this.isEnemyMarker = pingView.getMethod("isEnemyMarker");
        this.entityId = pingView.getField("entityId");
        this.dimension = pingView.getField("dimension");
        this.getPosition = pingView.getMethod("getPosition");
        this.getIntroProgress = pingView.getMethod("getIntroProgress", float.class);
        this.applyMarkerStyle = pingView.getMethod("applyMarkerStyle", int.class, float.class);
        this.getTeamColor = pingView.getMethod("getTeamColor");
        Field ageField = null;
        try {
            ageField = pingView.getDeclaredField("age");
            ageField.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // no fade-out timing: pings simply vanish when Modern Ping removes them
        }
        this.age = ageField;
        Class<?> gameContext = Class.forName(root + "core.GameContext");
        this.gcGetDimension = gameContext.getMethod("getDimension");
        this.gcGetEntity = gameContext.getMethod("getEntity", UUID.class);
        Method team = null;
        try {
            team = Class.forName(root + "core.TacticalMarkerManager").getMethod("isTeammate", Player.class);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // falls back to the vanilla scoreboard team
        }
        this.isTeammate = team;
        Object cfg = null;
        Method duration = null;
        try {
            Class<?> cc = Class.forName(root + "config.ClientConfig");
            Object handler = cc.getField("HANDLER").get(null);
            cfg = handler.getClass().getMethod("getConfig").invoke(handler);
            duration = cfg.getClass().getMethod("getPingDuration");
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // optional
        }
        this.clientConfig = cfg;
        this.getPingDuration = duration;
    }

    @Override
    public void render(GuiGraphics gui, Minecraft mc, float partialTick, float compassAngle, int centerX, int scissorLeft,
                       int scissorRight, double pixelsPerDegree, float screenScale, CompassPingStyle style) {
        if (mc.player == null || mc.level == null || !(pingRepo instanceof Collection<?> repo) || repo.isEmpty()) {
            entityCache.clear();
            return;
        }
        try {
            refreshPrefixes();
            Object dim = gcGetDimension.invoke(null);
            Vec3 me = mc.player.getPosition(partialTick);
            long now = mc.level.getGameTime();
            double maxDist = CompassConfig.PING_MAX_DISTANCE.get();
            float markerScale = CompassConfig.PING_MARKER_SCALE.get().floatValue();
            boolean edge = CompassConfig.PING_EDGE_HINTS.get();
            float y = (float) style.iconY();
            int[] edgeCount = new int[2];
            List<Object> snapshot = new ArrayList<>(repo);
            entityCache.keySet().retainAll(snapshot);
            for (Object ping : snapshot) {
                if (!dim.equals(dimension.get(ping))) {
                    continue;
                }
                Vec3 pos = (Vec3) getPosition.invoke(ping);
                double dx = pos.x - me.x;
                double dz = pos.z - me.z;
                double dist = Math.sqrt(dx * dx + dz * dz + (pos.y - me.y) * (pos.y - me.y));
                if (dx * dx + dz * dz < 1.0E-6 || (maxDist > 0 && dist > maxDist)) {
                    continue;
                }
                int category = classify(mc, ping, now);
                if (category < 0) {
                    continue;
                }
                int base = baseColor(category, ping);
                float alpha = fadeAlpha(ping, partialTick);
                if (alpha <= 0.02F) {
                    continue;
                }
                int color = multiplyAlpha(base, alpha);
                float bearing = (float) Math.toDegrees(Math.atan2(dx, -dz));
                float delta = Mth.wrapDegrees(bearing - compassAngle);
                float intro = (Float) getIntroProgress.invoke(ping, partialTick);
                float pop = 1.0F + 0.5F * (1.0F - smooth(intro));
                float size = 3.4F * markerScale * distanceScale(dist) * pop;
                if (Math.abs(delta) <= HALF_SPAN) {
                    float x = centerX + (float) (delta * pixelsPerDegree);
                    drawMarker(gui, category, x, y, size, color);
                } else if (edge) {
                    int side = delta < 0 ? 0 : 1;
                    float step = 6.0F * Math.min(edgeCount[side]++, 3);
                    float x = delta < 0 ? scissorLeft - 4.0F - step : scissorRight + 4.0F + step;
                    PixelDraw.arrowHead(gui, x, y, Math.max(2.2F, size * 0.7F), delta < 0, multiplyAlpha(color, intro));
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Modern Ping changed under us: skip this frame's markers rather than crash the HUD
        }
    }

    // ------------------------------------------------------------------------------------------------ classification

    /** Category of a ping, or -1 when it should not be shown (switch off, teammate, ...). */
    private int classify(Minecraft mc, Object ping, long now) throws ReflectiveOperationException {
        UUID id = (UUID) entityId.get(ping);
        boolean flaggedEnemy = (Boolean) isEnemyMarker.invoke(ping);
        if (id == null) {
            int c = CompassConfig.PING_LOCATIONS.get() ? LOCATION : -1;
            debug(ping, null, c, c < 0 ? "location ping, 'locations' is off" : "location ping");
            return c;
        }
        Entity e = entity(ping, id, now);
        if (flaggedEnemy || e instanceof Player p && p != mc.player && !teammate(mc, p)) {
            int c = CompassConfig.PING_ENEMY_PLAYERS.get() ? PLAYER : -1;
            debug(ping, e, c, c < 0 ? "enemy player, 'enemyPlayers' is off" : "enemy player");
            return c;
        }
        if (e == null) {
            int c = CompassConfig.PING_OTHER_ENTITIES.get() ? OTHER : -1;
            debug(ping, null, c, "target entity not loaded on this client yet (too far / unloaded)");
            return c;
        }
        if (e instanceof Player) {
            debug(ping, e, -1, "yourself or a teammate");
            return -1;
        }
        if (isVehicle(e)) {
            if (!CompassConfig.PING_VEHICLES.get()) {
                debug(ping, e, -1, "vehicle, 'vehicles' is off");
                return -1;
            }
            int rel = vehicleRelation(mc, e);
            boolean show = rel == ENEMY || rel == NEUTRAL && CompassConfig.PING_NEUTRAL_VEHICLES.get();
            debug(ping, e, show ? VEHICLE : -1, "vehicle, " + (rel == FRIENDLY ? "yours (your team / teammate riding)"
                    : rel == ENEMY ? "other team" : "no team and no rider" + (show ? "" : ", 'emptyVehicles' is off")));
            return show ? VEHICLE : -1;
        }
        if (e instanceof Enemy && !mc.player.isAlliedTo(e)) {
            int c = CompassConfig.PING_HOSTILE_MOBS.get() ? HOSTILE : -1;
            debug(ping, e, c, c < 0 ? "hostile mob, 'hostileMobs' is off" : "hostile mob");
            return c;
        }
        int c = CompassConfig.PING_OTHER_ENTITIES.get() ? OTHER : -1;
        debug(ping, e, c, c < 0 ? "other entity, 'otherEntities' is off (not hostile, not a vehicle)" : "other entity");
        return c;
    }

    /** One log line per new ping when {@code pings.debugLog} is on. */
    private void debug(Object ping, Entity e, int category, String why) {
        if (!CompassConfig.PING_DEBUG.get() || logged.put(ping, Boolean.TRUE) != null) {
            return;
        }
        String type = e == null ? "-" : String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(e.getType()));
        LOGGER.info("ModernHud ping: class={} type={} -> {} ({})", e == null ? "-" : e.getClass().getName(), type,
                category < 0 ? "HIDDEN" : "shown", why);
    }

    private Entity entity(Object ping, UUID id, long now) throws ReflectiveOperationException {
        Object[] c = entityCache.get(ping);
        if (c != null && now - (Long) c[1] < 5L && (c[0] == null || !((Entity) c[0]).isRemoved())) {
            return (Entity) c[0];
        }
        Entity e = (Entity) gcGetEntity.invoke(null, id);
        entityCache.put(ping, new Object[]{e, now});
        return e;
    }

    private boolean teammate(Minecraft mc, Player p) {
        if (isTeammate != null) {
            try {
                return (Boolean) isTeammate.invoke(null, p);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // fall through
            }
        }
        return mc.player.isAlliedTo(p);
    }

    private void refreshPrefixes() {
        String cfg = CompassConfig.PING_VEHICLE_PREFIXES.get();
        if (!cfg.equals(vehiclePrefixCache[0])) {
            vehiclePrefixCache[0] = cfg;
            vehiclePrefixes = split(cfg, false);
        }
        String ns = CompassConfig.PING_VEHICLE_NAMESPACES.get();
        if (!ns.equals(vehicleNsCache[0])) {
            vehicleNsCache[0] = ns;
            vehicleNamespaces = split(ns, true);
        }
    }

    private static String[] split(String csv, boolean lower) {
        List<String> out = new ArrayList<>();
        for (String part : csv.split(",")) {
            if (!part.isBlank()) {
                out.add(lower ? part.trim().toLowerCase() : part.trim());
            }
        }
        return out.toArray(new String[0]);
    }

    /**
     * A vehicle if its class name starts with a configured prefix, OR its entity type belongs to a configured mod id
     * (SuperbWarfare by default) and it is not a projectile / item / player. Two independent checks, so a package move
     * inside the vehicle mod does not silently break the marker.
     */
    private boolean isVehicle(Entity e) {
        String name = e.getClass().getName();
        for (String prefix : vehiclePrefixes) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        if (vehicleNamespaces.length > 0 && !(e instanceof Projectile) && !(e instanceof ItemEntity) && !(e instanceof Player)) {
            net.minecraft.resources.ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
            if (key != null) {
                for (String ns : vehicleNamespaces) {
                    if (ns.equals(key.getNamespace())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** FRIENDLY = same scoreboard team or a teammate rides it; ENEMY = other team or an enemy rides it; else NEUTRAL. */
    private int vehicleRelation(Minecraft mc, Entity vehicle) {
        if (vehicle.getTeam() != null) {
            return mc.player.isAlliedTo(vehicle) ? FRIENDLY : ENEMY;
        }
        boolean friend = false;
        boolean foe = false;
        for (Entity rider : vehicle.getPassengers()) {
            if (rider instanceof Player p) {
                if (p == mc.player || teammate(mc, p)) {
                    friend = true;
                } else {
                    foe = true;
                }
            }
        }
        return friend ? FRIENDLY : foe ? ENEMY : NEUTRAL;
    }

    // ------------------------------------------------------------------------------------------------ look

    private int baseColor(int category, Object ping) throws ReflectiveOperationException {
        switch (category) {
            case PLAYER:
                return CompassConfig.argb(CompassConfig.PING_ENEMY_COLOR.get(), 1.0);
            case HOSTILE:
                return CompassConfig.argb(CompassConfig.PING_HOSTILE_COLOR.get(), 1.0);
            case VEHICLE:
                return CompassConfig.argb(CompassConfig.PING_VEHICLE_COLOR.get(), 1.0);
            case LOCATION:
                if (CompassConfig.PING_LOCATION_TEAM_COLOR.get()) {
                    int team = (Integer) getTeamColor.invoke(ping);
                    if (team != -1) {
                        return 0xFF000000 | (team & 0xFFFFFF);
                    }
                }
                return CompassConfig.argb(CompassConfig.PING_LOCATION_COLOR.get(), 1.0);
            default:
                return CompassConfig.argb(CompassConfig.PING_OTHER_COLOR.get(), 1.0);
        }
    }

    /** Opacity: Modern Ping's own fade for enemy pings, plus a 1 second fade before any other ping expires. */
    private float fadeAlpha(Object ping, float partialTick) throws ReflectiveOperationException {
        float a = ((Integer) applyMarkerStyle.invoke(ping, -1, partialTick) >>> 24) / 255.0F;
        if (age != null && getPingDuration != null && clientConfig != null) {
            int duration = (Integer) getPingDuration.invoke(clientConfig);
            if (duration > 0 && duration < 60) {
                float remaining = duration * 20.0F - (age.getInt(ping) + partialTick);
                a *= Mth.clamp(remaining / 20.0F, 0.0F, 1.0F);
            }
        }
        return a;
    }

    private static float distanceScale(double dist) {
        if (!CompassConfig.PING_DISTANCE_SCALING.get()) {
            return 1.0F;
        }
        float far = CompassConfig.PING_FAR_SCALE_PERCENT.get() / 100.0F;
        float t = Mth.clamp((float) (dist / CompassConfig.PING_FAR_DISTANCE.get()), 0.0F, 1.0F);
        return Mth.lerp(t, 1.0F, far);
    }

    private static void drawMarker(GuiGraphics gui, int category, float x, float y, float size, int color) {
        switch (category) {
            case PLAYER -> PixelDraw.diamond(gui, x, y, size, true, color);
            case HOSTILE -> PixelDraw.square(gui, x, y, size * 0.8F, true, color);
            case VEHICLE -> PixelDraw.triangleDown(gui, x, y, size, color);
            case LOCATION -> {
                PixelDraw.diamond(gui, x, y, size, false, color);
                PixelDraw.fillUnits(gui, x - 0.4F, y - 0.4F, 0.8F, 0.8F, color);
            }
            default -> PixelDraw.square(gui, x, y, size * 0.8F, false, color);
        }
    }

    private static float smooth(float v) {
        float c = Mth.clamp(v, 0.0F, 1.0F);
        return c * c * (3.0F - 2.0F * c);
    }

    private static int multiplyAlpha(int color, float m) {
        int a = Mth.clamp((int) (ARGB32.alpha(color) * m), 0, 255);
        return (color & 0xFFFFFF) | (a << 24);
    }
}

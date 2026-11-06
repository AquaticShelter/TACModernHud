package cc.xypp.modernhud.client;

import com.mojang.authlib.GameProfile;
import cc.xypp.modernhud.config.AbsorptionColorMode;
import cc.xypp.modernhud.config.HudConfig;
import cc.xypp.modernhud.client.compat.SuperbWarfareCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Teammate health list (vanilla scoreboard team), Warzone-style: one compact row per teammate —
 * head icon, name (coloured by their vanilla team colour by default), and a smoothly-animated
 * health bar — stacked directly above the player's own widget ({@link PlayerHudOverlay}), sharing
 * ONE background chip/border of its own, separate from the main widget's own panel. Capped at a
 * configurable row count ({@link HudConfig#teamHudMaxPlayers}, default 6); extras beyond that are
 * simply hidden, sorted alphabetically by name so the cutoff is at least stable/predictable rather
 * than shuffling every frame.
 *
 * <p>Reads {@link LocalPlayer#getTeam()} and matches it against every player on the tab list
 * ({@link PlayerInfo}) — the SAME vanilla scoreboard team system {@code /team} itself manages
 * (join/leave/kick, team colour, all of it), not a separate team system of its own. Live health and
 * Absorption need the teammate's entity to be loaded/tracked client-side (the same range vanilla
 * already shows other players' names and health at), so an ALIVE teammate out of that range simply
 * isn't shown until they're back in range. A DEAD or SPECTATING teammate is different: they are
 * always listed (empty bars + red pulse, see below) even when their entity isn't tracked at all,
 * since the tab list still knows their gamemode and a death seen while tracked is remembered.
 *
 * <p>Positioned off {@link PlayerHudOverlay}'s own last-computed screen bounds (its package-private
 * {@code lastFrameX}/{@code lastFrameY} fields) so it always sits flush above it — which means this
 * MUST render AFTER {@link PlayerHudOverlay} each frame (see the registration order in
 * {@code ClientEvents}); its left edge lines up with the main widget's left edge, which matches the
 * default BOTTOM_LEFT anchor cleanly but won't right-align against a right-anchored main widget of
 * a different width — a reasonable trade-off to keep this first version simple.
 *
 * <p>When {@link HudConfig#showTeamAbsorption} is on, each row also gets its own Absorption
 * (shield) plate bar directly above its health bar — same segmented-plate look, tiered/custom
 * colour mode, and segment/points-per-segment settings as the player's own widget, so the two read
 * as one consistent style (see {@link #drawRow}). A teammate who is currently dead or in Spectator
 * mode stays in the roster — their health/Absorption bars glide to empty and the whole row pulses
 * with a red glow ({@link HudConfig#teamHudDownGlowColor}), the same "something's wrong here"
 * visual language as the low-ammo/low-health pulse elsewhere in the mod, rather than the row simply
 * disappearing.
 *
 * <p>Rendered at {@link HudConfig#barScale} times an extra {@link HudConfig#teamHudScaleFactor}
 * (10% smaller by default) rather than the player's own widget's exact scale, so a teammate's row
 * reads as visually distinct from your own vitals at a glance instead of the two looking identical.
 */
public class TeamHudOverlay implements IGuiOverlay {

    private static final int HEAD_SIZE = 14;
    private static final int BAR_H = 3;
    private static final int PLATE_GAP = 1;

    /** Per-teammate eased health-glide animator, keyed by UUID, so each teammate's bar glides
     *  independently instead of sharing one smoothing state — same time-based quintic-ease-out
     *  technique as the player's own health bar (see {@link EasedAnimator}), not the mod's older
     *  frame-rate-dependent exponential decay. Entries for players no longer on the team are pruned
     *  periodically rather than every frame (see {@link #pruneStaleAnimatorsIfDue}) - team rosters
     *  are small, so this is about avoiding slow unbounded growth over a long session, not an
     *  every-frame cost concern. */
    private final Map<UUID, EasedAnimator> healthAnimators = new HashMap<>();

    /** Same idea as {@link #healthAnimators}, one independent glide per teammate, for their
     *  Absorption plate row. */
    private final Map<UUID, EasedAnimator> absorptionAnimators = new HashMap<>();

    /** One roster row's data for this frame. Instances are kept per-UUID in {@link #entries} and
     *  refilled every frame rather than reallocated (this runs hundreds of times a second). The
     *  roster comes from the tab list ({@link PlayerInfo}), NOT from the loaded-entity list, so a
     *  teammate who is dead, spectating, or otherwise not currently a tracked entity still has a
     *  row - {@link #entity} is simply null for those and the row is then always "down". */
    private static final class Entry {
        UUID id;
        String name;
        ResourceLocation skin;
        Team team;
        AbstractClientPlayer entity; // null when not currently tracked client-side
        boolean down;
    }

    private static final Comparator<Entry> BY_NAME =
            Comparator.comparing(e -> e.name, String.CASE_INSENSITIVE_ORDER);

    private final Map<UUID, Entry> entries = new HashMap<>();
    private final List<Entry> teammatesScratch = new ArrayList<>();
    private final Set<UUID> teamIdsScratch = new HashSet<>();

    /** Teammates last SEEN dead (entity present with health <= 0 / dying). Vanilla removes a dead
     *  player's entity soon after, so without this their row would vanish the moment the entity
     *  does; they stay "down" until their entity is seen alive again, they go Spectator -> back to
     *  another mode while tracked, or they leave the tab list. */
    private final Set<UUID> knownDown = new HashSet<>();

    private static final long PRUNE_INTERVAL_MS = 5000L;
    private long lastPruneMs = Long.MIN_VALUE / 2;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!HudConfig.enabled || !HudConfig.showTeamHud) {
            return;
        }
        LocalPlayer self = Minecraft.getInstance().player;
        if (self == null || self.level() == null) {
            return;
        }
        if (SuperbWarfareCompat.isInVehicle(self)) {
            // Same reasoning as PlayerHudOverlay: it stacks directly off that widget's position,
            // which just went stale/hidden this frame, so there's nothing sane to stack above.
            return;
        }
        Team myTeam = self.getTeam();
        if (myTeam == null) {
            return; // not on a vanilla team - nothing to show
        }
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return;
        }

        Scoreboard scoreboard = self.level().getScoreboard();
        List<Entry> teammates = teammatesScratch;
        teammates.clear();
        teamIdsScratch.clear();
        for (PlayerInfo info : connection.getOnlinePlayers()) {
            GameProfile profile = info.getProfile();
            UUID id = profile.getId();
            if (id.equals(self.getUUID())) {
                continue;
            }
            PlayerTeam theirTeam = scoreboard.getPlayersTeam(profile.getName());
            if (theirTeam == null || !myTeam.equals(theirTeam)) {
                continue;
            }
            teamIdsScratch.add(id);

            Player tracked = self.level().getPlayerByUUID(id);
            AbstractClientPlayer entity = tracked instanceof AbstractClientPlayer acp ? acp : null;
            if (entity != null) {
                if (entity.isDeadOrDying() || entity.getHealth() <= 0f) {
                    knownDown.add(id);
                } else {
                    knownDown.remove(id);
                }
            }
            boolean spectator = info.getGameMode() == GameType.SPECTATOR;
            boolean down = spectator || knownDown.contains(id);
            if (entity == null && !down) {
                continue; // alive but out of tracking range - no live data to show, as before
            }

            Entry e = entries.computeIfAbsent(id, k -> new Entry());
            e.id = id;
            e.name = profile.getName();
            e.skin = info.getSkinLocation();
            e.team = theirTeam;
            e.entity = entity;
            e.down = down;
            teammates.add(e);
        }
        if (teammates.isEmpty()) {
            return;
        }
        teammates.sort(BY_NAME);
        pruneStaleIfDue();
        int count = Math.min(Math.max(1, HudConfig.teamHudMaxPlayers), teammates.size());

        Font font = Minecraft.getInstance().font;
        int lineH = font.lineHeight;
        int headW = HudConfig.teamHudShowHead ? HEAD_SIZE + 5 : 0;
        int barW = HudConfig.teamHudBarWidth;
        int contentW = headW + Math.max(barW, 1);
        int plateH = HudConfig.showTeamAbsorption ? BarRender.plateHeight(HudConfig.absorptionHeight, HudConfig.absorptionShape, barW) : 0;
        int plateBlockH = plateH > 0 ? plateH + PLATE_GAP : 0;
        int rowContentH = Math.max(HudConfig.teamHudShowHead ? HEAD_SIZE : 0, lineH + 1 + plateBlockH + BAR_H);
        int rowGap = HudConfig.teamHudRowGap;
        int rowStride = rowContentH + rowGap;

        int panelPad = HudConfig.showPanel ? HudPanel.padding() : 0;
        int boxW = contentW + panelPad * 2;
        int boxH = count * rowContentH + (count - 1) * rowGap + panelPad * 2;

        double scale = HudText.snapElement(graphics, HudConfig.barScale * HudConfig.teamHudScaleFactor);
        int scaledBoxH = (int) Math.round(boxH * scale);

        // Stacks directly above the player's own widget - same left edge, its own bottom edge
        // sitting teamHudGroupGap px above the main widget's top edge.
        int x = PlayerHudOverlay.lastFrameX;
        int y = PlayerHudOverlay.lastFrameY - scaledBoxH - HudConfig.teamHudGroupGap;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale((float) scale, (float) scale, 1.0f);

        if (HudConfig.showPanel) {
            HudPanel.draw(graphics, 0, 0, boxW, boxH, HudConfig.panelColor, HudConfig.panelOpacity);
            HudPanel.drawBorder(graphics, 0, 0, boxW, boxH,
                    HudConfig.borderColor, HudConfig.borderOpacity, HudConfig.borderStyle, HudConfig.borderLength, HudConfig.borderOutset,
                    HudConfig.borderScale / 100.0);
        }

        int cursorY = panelPad;
        for (int i = 0; i < count; i++) {
            drawRow(graphics, font, teammates.get(i), panelPad, cursorY, headW, barW, plateH, lineH, rowContentH);
            cursorY += rowStride;
        }

        pose.popPose();
    }

    /** Sweeps the per-teammate maps/sets for players no longer on the team's tab-list roster, at
     *  most once every {@link #PRUNE_INTERVAL_MS} - only matters over minutes (long sessions, many
     *  people joining/leaving), so there's no reason to pay even a small cost every frame. */
    private void pruneStaleIfDue() {
        long now = System.currentTimeMillis();
        if (now - lastPruneMs < PRUNE_INTERVAL_MS) {
            return;
        }
        lastPruneMs = now;
        healthAnimators.keySet().retainAll(teamIdsScratch);
        absorptionAnimators.keySet().retainAll(teamIdsScratch);
        entries.keySet().retainAll(teamIdsScratch);
        knownDown.retainAll(teamIdsScratch);
    }

    private void drawRow(GuiGraphics g, Font font, Entry p, int x, int y,
                          int headW, int barW, int plateH, int lineH, int rowH) {
        // "Down" = dead or spectating - bars glide to empty and the whole row gets a pulsing red
        // glow, same visual language as the low-ammo/low-health pulse elsewhere in the mod, so it
        // reads as a clear alert rather than a silently-broken empty row.
        boolean down = p.down;
        AbstractClientPlayer ent = p.entity;
        if (down) {
            float pulseAlpha = 0.18f + 0.22f * 0.5f * (1f + (float) Math.sin(System.currentTimeMillis() / 260.0));
            GlowEffect.drawGlowRect(g, x, y, headW + Math.max(barW, 1), rowH,
                    HudConfig.teamHudDownGlowColor, pulseAlpha, 3f);
        }

        if (HudConfig.teamHudShowHead) {
            int headY = y + (rowH - HEAD_SIZE) / 2;
            if (ent != null) {
                PlayerHudOverlay.renderHead(g, ent, x, headY, HEAD_SIZE);
            } else {
                PlayerHudOverlay.renderHead(g, p.skin, x, headY, HEAD_SIZE);
            }
        }

        int textX = x + headW;
        int plateBlockH = plateH > 0 ? plateH + PLATE_GAP : 0;
        int textBlockH = lineH + 1 + plateBlockH + BAR_H;
        int textY = y + (rowH - textBlockH) / 2;
        UUID id = p.id;

        HudText.text(g, font, p.name, textX, textY, resolveNameColor(p.team), true);

        int cursorY = textY + lineH + 1;

        if (plateH > 0) {
            // Down (dead or spectating) never shows a real (possibly stale/leftover) Absorption
            // reading - the glide target is forced to zero so the bar eases smoothly down to empty
            // exactly like any other Absorption loss, instead of freezing mid-value or showing a
            // leftover number the server hasn't cleared yet.
            float absorptionRaw = (down || ent == null) ? 0f : Math.max(0f, ent.getAbsorptionAmount());
            EasedAnimator absorbAnimator = absorptionAnimators.computeIfAbsent(id,
                    k -> new EasedAnimator(220f, EasedAnimator.Easing.QUINTIC_OUT));
            absorbAnimator.setDurationMs(HudConfig.absorptionAnimMs);
            absorbAnimator.setTarget(absorptionRaw);
            float animatedAbsorption = absorbAnimator.update();
            if (animatedAbsorption > 0.01f) {
                boolean customColor = HudConfig.absorptionColorMode == AbsorptionColorMode.CUSTOM;
                BarRender.platesShaped(g, textX, cursorY, barW, plateH, animatedAbsorption,
                        HudConfig.segmentCount, HudConfig.pointsPerSegment, customColor, HudConfig.absorptionCustomColor,
                        HudConfig.absorptionShape, HudConfig.absorptionFill, HudConfig.absorptionOutline);
            }
            cursorY += plateBlockH;
        }

        // Same reasoning as Absorption above - Spectator mode doesn't reset a player's actual health
        // value server-side (they simply aren't "dead"), so the empty bar here is forced by "down"
        // rather than trusting getHealth() directly, which would otherwise just freeze at whatever
        // it last was.
        float target = 0f;
        if (!down && ent != null) {
            float maxHealth = Math.max(1.0f, ent.getMaxHealth());
            target = Math.max(0.0f, Math.min(1.0f, ent.getHealth() / maxHealth));
        }
        EasedAnimator animator = healthAnimators.computeIfAbsent(id,
                k -> new EasedAnimator(280f, EasedAnimator.Easing.QUINTIC_OUT));
        animator.setDurationMs(HudConfig.healthAnimMs);
        animator.setTarget(target);
        float current = animator.update();

        double percent = 100.0 * target;
        int barColor = percent <= HudConfig.lowHealthThresholdPercent ? BarRender.HEALTH_LOW : BarRender.HEALTH;
        BarRender.bar(g, textX, cursorY, barW, BAR_H, current, barColor);
    }

    private int resolveNameColor(Team team) {
        if (HudConfig.teamHudUseTeamColor) {
            ChatFormatting fmt = team != null ? team.getColor() : null;
            if (fmt != null && fmt.getColor() != null) {
                return 0xFF000000 | fmt.getColor();
            }
        }
        return HudConfig.teamHudNameColor | 0xFF000000;
    }
}

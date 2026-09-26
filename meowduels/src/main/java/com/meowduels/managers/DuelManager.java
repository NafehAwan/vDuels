/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.minimessage.MiniMessage
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.api.chat.ClickEvent
 *  net.md_5.bungee.api.chat.ClickEvent$Action
 *  net.md_5.bungee.api.chat.ComponentBuilder
 *  net.md_5.bungee.api.chat.HoverEvent
 *  net.md_5.bungee.api.chat.HoverEvent$Action
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.World
 *  org.bukkit.WorldBorder
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.ExperienceOrb
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.entity.TNTPrimed
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.util.Vector
 */
package com.meowduels.managers;

import com.meowduels.MeowDuels;
import com.meowduels.gui.DuelConfirmMenu;
import com.meowduels.gui.KitPickMenu;
import com.meowduels.gui.MatchSummaryMenu;
import com.meowduels.managers.StatsManager;
import com.meowduels.model.ActiveDuel;
import com.meowduels.model.Arena;
import com.meowduels.model.DuelRequest;
import com.meowduels.model.Kit;
import com.meowduels.model.PlayerSnapshot;
import com.meowduels.model.StartEffect;
import com.meowduels.util.Cooldowns;
import com.meowduels.util.AntiCheatBypass;
import com.meowduels.util.Colors;
import com.meowduels.util.GameModeGuard;
import com.meowduels.util.Sounds;
import com.meowduels.util.SpawnItems;
import com.meowduels.util.Text;
import com.meowduels.util.Trims;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class DuelManager {
    private final MeowDuels plugin;
    private final long requestTtlMs;
    private final int countdownSeconds;
    private final long transitionTicks;
    private final long matchEndTicks;
    private final long deathSpectateTicks;
    private final long winEndTicks;
    private final long matchFoundTicks;
    private final Map<UUID, Map<UUID, DuelRequest>> requests = new HashMap<UUID, Map<UUID, DuelRequest>>();
    // Concurrent because TAB resolves placeholders on its own thread: it calls
    // isInDuel/getDuel while the main thread is starting and ending matches, and
    // a plain HashMap read during a resize can spin forever.
    private final Map<UUID, ActiveDuel> playerDuels = new java.util.concurrent.ConcurrentHashMap<UUID, ActiveDuel>();
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<UUID, PlayerSnapshot>();
    private final Set<String> arenasInUse = new HashSet<String>();
    private final Map<UUID, UUID> lastOpponent = new HashMap<UUID, UUID>();
    private static final String REQUEST_COOLDOWN = "duel-request";
    private int gameCounter = 0;

    public DuelManager(MeowDuels plugin) {
        this.plugin = plugin;
        this.requestTtlMs = (long)Math.max(1, plugin.getConfig().getInt("duel.request-expiry-seconds", 60)) * 1000L;
        this.countdownSeconds = Math.max(1, plugin.getConfig().getInt("rounds.countdown-seconds", 5));
        this.transitionTicks = (long)Math.max(0.0, plugin.getConfig().getDouble("rounds.transition-seconds", 3.5) * 20.0);
        this.matchEndTicks = (long)Math.max(0.0, plugin.getConfig().getDouble("rounds.end-seconds", 3.5) * 20.0);
        this.deathSpectateTicks = (long)Math.max(0.0, plugin.getConfig().getDouble("rounds.death-spectate-seconds", 2.0) * 20.0);
        this.winEndTicks = (long)Math.max(0.0, plugin.getConfig().getDouble("rounds.win-teleport-seconds", 2.0) * 20.0);
        this.matchFoundTicks = (long)Math.max(0.0, plugin.getConfig().getDouble("rounds.match-found-seconds", 4.0) * 20.0);
    }

    public void sendRequest(Player sender, Player target, String kit, int rounds, String arena) {
        if (sender.equals((Object)target)) {
            sender.sendMessage(this.msg("duel.cannot-duel-self", new String[0]));
            return;
        }
        if (this.isInDuel(sender.getUniqueId())) {
            sender.sendMessage(this.msg("duel.already-in-duel", new String[0]));
            return;
        }
        if (this.isInDuel(target.getUniqueId())) {
            sender.sendMessage(this.msg("duel.target-in-duel", "target", target.getName()));
            return;
        }
        if (this.plugin.getEventManager().isInvolved(sender.getUniqueId())) {
            sender.sendMessage(Text.prefixed("&cYou can't duel while you're in the event."));
            return;
        }
        if (this.plugin.getEventManager().isInvolved(target.getUniqueId())) {
            sender.sendMessage(Text.prefixed("&c" + target.getName() + " is in the event right now."));
            return;
        }
        if (!this.plugin.getPlayerSettings().isDuelRequests(target.getUniqueId())) {
            sender.sendMessage(this.msg("duel.requests-off", "target", target.getName()));
            return;
        }
        if (this.plugin.getPartyManager().inParty(sender.getUniqueId())) {
            sender.sendMessage(Text.prefixed("&cYou're in a party - leave it first, or start a party match."));
            return;
        }
        if (this.plugin.getPartyManager().inParty(target.getUniqueId())) {
            sender.sendMessage(Text.prefixed("&c" + target.getName() + " is in a party right now."));
            return;
        }
        // Spam control. A duel request is a message in somebody else's chat
        // that they did not ask for, so it is rate limited twice: once on the
        // sender, and once per target so a pending request cannot be re-sent on
        // a loop to the same person.
        Map<UUID, DuelRequest> pending = this.requests.get(target.getUniqueId());
        DuelRequest existing = pending == null ? null : pending.get(sender.getUniqueId());
        if (existing != null && !existing.isExpired(this.requestTtlMs)) {
            sender.sendMessage(Text.prefixed("&c" + target.getName()
                    + " already has your challenge - give them a moment."));
            Sounds.deny(sender);
            return;
        }
        long left = Cooldowns.remaining(sender, REQUEST_COOLDOWN);
        if (left > 0L) {
            sender.sendMessage(Text.prefixed("&cWait &f" + Cooldowns.seconds(left)
                    + "s&c before sending another challenge."));
            Sounds.deny(sender);
            return;
        }
        Cooldowns.start(sender, REQUEST_COOLDOWN, null,
                (long)(this.plugin.getConfig().getDouble("duel.request-cooldown-seconds", 5.0) * 1000.0));
        if (this.plugin.getKitManager().get(kit) == null) {
            sender.sendMessage(this.msg("duel.kit-gone", new String[0]));
            return;
        }
        if (!this.hasFreeArenaFor(kit)) {
            sender.sendMessage(this.msg("duel.no-arenas", new String[0]));
            this.explainNoArena(sender, kit);
            return;
        }
        DuelRequest request = new DuelRequest(sender.getUniqueId(), target.getUniqueId(), kit, rounds, arena);
        this.requests.computeIfAbsent(target.getUniqueId(), k -> new HashMap()).put(sender.getUniqueId(), request);
        sender.sendMessage(this.msg("duel.sent", "target", target.getName(), "kit", kit, "rounds", String.valueOf(rounds)));
        this.sendRequestCard(target, sender, kit, rounds);
    }

    private void sendRequestCard(Player target, Player sender, String kit, int rounds) {
        String kitLabel = kit.replace('_', ' ').toUpperCase(Locale.ROOT);
        target.sendMessage("");
        target.sendMessage(this.msg("request.header", "sender", sender.getName()));
        target.sendMessage(this.msg("request.kit", "kit", kitLabel));
        target.sendMessage(this.msg("request.rounds", "rounds", String.valueOf(rounds)));
        target.sendMessage("");
        TextComponent click = new TextComponent(this.msg("request.click", new String[0]));
        click.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel accept " + sender.getName()));
        click.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(this.msg("request.click-hover", "sender", sender.getName())).create()));
        target.spigot().sendMessage(new BaseComponent[]{click});
        target.sendMessage("");
        Sounds.request(target);
    }

    private String msg(String key, String ... placeholders) {
        return this.plugin.messages().get(key, placeholders);
    }

    public void acceptRequest(Player target, UUID senderId) {
        DuelRequest request;
        Map<UUID, DuelRequest> targeted = this.requests.get(target.getUniqueId());
        DuelRequest duelRequest = request = targeted == null ? null : targeted.get(senderId);
        if (request == null || request.isExpired(this.requestTtlMs)) {
            target.sendMessage(this.msg("accept.expired", new String[0]));
            if (targeted != null) {
                targeted.remove(senderId);
            }
            return;
        }
        Player sender = Bukkit.getPlayer((UUID)senderId);
        if (sender == null) {
            target.sendMessage(this.msg("accept.sender-offline", new String[0]));
            targeted.remove(senderId);
            return;
        }
        if (this.isInDuel(sender.getUniqueId()) || this.isInDuel(target.getUniqueId())) {
            target.sendMessage(this.msg("accept.one-in-duel", new String[0]));
            return;
        }
        if (this.plugin.getEventManager().isInvolved(sender.getUniqueId()) || this.plugin.getEventManager().isInvolved(target.getUniqueId())) {
            target.sendMessage(Text.prefixed("&cThat duel can't start - someone is in the event."));
            return;
        }
        Arena arena = this.resolveArena(request);
        if (arena == null) {
            target.sendMessage(this.msg("accept.no-arena", new String[0]));
            sender.sendMessage(this.msg("accept.no-arena", new String[0]));
            return;
        }
        targeted.remove(senderId);
        Sounds.accept(sender);
        Sounds.accept(target);
        this.startDuel(sender, target, arena, request.getKit(), request.getRounds());
    }

    private Arena resolveArena(DuelRequest request) {
        Arena chosen;
        if (request.getArena() != null && (chosen = this.plugin.getArenaManager().get(request.getArena())) != null && chosen.isConfigured() && chosen.isEnabled() && chosen.supportsKit(request.getKit()) && !this.arenasInUse.contains(chosen.getName().toLowerCase())) {
            return chosen;
        }
        return this.plugin.getArenaManager().findFreeArena(a -> this.arenasInUse.contains(a.getName().toLowerCase()) || !a.supportsKit(request.getKit()));
    }

    public DuelRequest getMostRecentRequest(Player target) {
        Map<UUID, DuelRequest> targeted = this.requests.get(target.getUniqueId());
        if (targeted == null || targeted.isEmpty()) {
            return null;
        }
        DuelRequest latest = null;
        for (DuelRequest r : targeted.values()) {
            if (r.isExpired(this.requestTtlMs) || latest != null && r.getCreatedAt() <= latest.getCreatedAt()) continue;
            latest = r;
        }
        return latest;
    }


    private void startDuel(Player p1, Player p2, Arena arena, String kit, int rounds) {
        if (this.isArenaBusy(arena)) {
            String booked = this.msg("duel.arena-booked", new String[0]);
            p1.sendMessage(booked);
            p2.sendMessage(booked);
            this.plugin.getQueueManager().remove(p1.getUniqueId());
            this.plugin.getQueueManager().remove(p2.getUniqueId());
            return;
        }
        this.plugin.getQueueManager().remove(p1.getUniqueId());
        this.plugin.getQueueManager().remove(p2.getUniqueId());
        this.markArenaInUse(arena.getName());
        this.snapshots.put(p1.getUniqueId(), PlayerSnapshot.capture(p1));
        this.snapshots.put(p2.getUniqueId(), PlayerSnapshot.capture(p2));
        ActiveDuel duel = new ActiveDuel(p1.getUniqueId(), p2.getUniqueId(), arena, kit, rounds);
        duel.setGameNumber(++this.gameCounter);
        this.playerDuels.put(p1.getUniqueId(), duel);
        this.playerDuels.put(p2.getUniqueId(), duel);
        this.plugin.getScoreboardService().attach(p1, duel);
        this.plugin.getScoreboardService().attach(p2, duel);
        this.plugin.getTabService().attach(duel);
        this.sendStartCard(p1, p2.getName(), duel);
        this.sendStartCard(p2, p1.getName(), duel);
        if (arena.isGlowingOpponent()) {
            this.applyGlow(p1, p2, true);
        }
        this.matchFound(duel);
    }

    private void matchFound(ActiveDuel duel) {
        duel.setState(ActiveDuel.State.STARTING);
        Player p1 = Bukkit.getPlayer((UUID)duel.getPlayer1());
        Player p2 = Bukkit.getPlayer((UUID)duel.getPlayer2());
        if (p1 == null || p2 == null) {
            this.handleDisconnect(p1 == null ? duel.getPlayer1() : duel.getPlayer2());
            return;
        }
        this.playMatchFound(p1);
        this.playMatchFound(p2);
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (this.isStill(duel)) {
                this.startRound(duel);
            }
        }, this.matchFoundTicks);
    }

    private void playMatchFound(Player player) {
        String title = this.msg("titles.match-found.title", new String[0]);
        Sounds.matchFound(player);
        long effTicks = Math.max(20L, this.matchFoundTicks);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)effTicks, 3));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int)effTicks, 0));
        String[] frames = new String[]{Text.color("&c\u25cf &8\u25cf &8\u25cf"), Text.color("&c\u25cf &c\u25cf &8\u25cf"), Text.color("&c\u25cf &c\u25cf &c\u25cf"), Text.color("&8\u25cf &c\u25cf &c\u25cf")};
        this.sendTitle(player, title, frames[0], 6, 20, 0);
        UUID id = player.getUniqueId();
        for (int i = 1; i < frames.length; ++i) {
            String frame = frames[i];
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                Player p = Bukkit.getPlayer((UUID)id);
                if (p != null && this.playerDuels.containsKey(id)) {
                    this.sendTitle(p, title, frame, 0, 20, 0);
                }
            }, (long)i * 10L);
        }
    }

    private void applyGlow(Player p1, Player p2, boolean glow) {
        try {
            p1.setGlowing(glow);
            p2.setGlowing(glow);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void sendStartCard(Player player, String opponentName, ActiveDuel duel) {
        String kitLabel = this.kitLabel(duel.getKit());
        String rounds = String.valueOf(duel.getRoundsToWin());
        player.sendMessage(this.msg("duel.start.header", new String[0]));
        player.sendMessage(this.msg("duel.start.opponent", "opponent", opponentName));
        player.sendMessage(this.msg("duel.start.kit", "kit", kitLabel));
        player.sendMessage(this.msg("duel.start.rounds", "rounds", rounds));
        player.sendMessage("");
        player.sendMessage(this.msg("duel.start.leave", new String[0]));
    }

    private String kitLabel(String kitId) {
        Kit kit = this.plugin.getKitManager().get(kitId);
        if (kit != null && kit.getDisplayName() != null && !kit.getDisplayName().isEmpty()) {
            return kit.getDisplayName().replaceAll("<[^>]*>", "");
        }
        return kitId;
    }

    private void startRound(ActiveDuel duel) {
        duel.setState(ActiveDuel.State.STARTING);
        Player p1 = Bukkit.getPlayer((UUID)duel.getPlayer1());
        Player p2 = Bukkit.getPlayer((UUID)duel.getPlayer2());
        if (p1 == null || p2 == null) {
            this.handleDisconnect(p1 == null ? duel.getPlayer1() : duel.getPlayer2());
            return;
        }
        Kit kit = this.plugin.getKitManager().get(duel.getKit());
        duel.setArenaEntered(true);
        this.prepare(p1, duel.getArena().getSpawn1(), kit);
        this.prepare(p2, duel.getArena().getSpawn2(), kit);
        this.verifyArrival(p1, duel.getArena().getSpawn1());
        this.verifyArrival(p2, duel.getArena().getSpawn2());
        String round = String.valueOf(duel.getCurrentRound());
        String toWin = String.valueOf(duel.getRoundsToWin());
        p1.sendMessage(this.msg("duel.round", "round", round, "roundsToWin", toWin));
        p2.sendMessage(this.msg("duel.round", "round", round, "roundsToWin", toWin));
        duel.clearReady();
        this.plugin.getSpectateManager().followRoundStart(duel.getPlayer1(), duel.getPlayer2());
        int countdown = duel.getArena().getCountdownOverride();
        int totalTicks = (countdown > 0 ? countdown : this.countdownSeconds) * 20;
        duel.startCountdown((long)totalTicks * 50L);
        this.countdownTick(duel, totalTicks);
    }

    private void verifyArrival(Player player, Location spawn) {
        if (player == null || spawn == null || spawn.getWorld() == null) {
            return;
        }
        Location at = player.getLocation();
        boolean sameWorld = at.getWorld() != null && at.getWorld().equals((Object)spawn.getWorld());
        double dx = at.getX() - spawn.getX();
        double dy = at.getY() - spawn.getY();
        double dz = at.getZ() - spawn.getZ();
        if (!sameWorld || dx * dx + dy * dy + dz * dz > 9.0) {
            this.plugin.getLogger().warning("Teleport into the arena did not take effect for " + player.getName() + " - they are at " + DuelManager.describe(at) + " instead of " + DuelManager.describe(spawn) + ". Something else is blocking or moving them.");
        }
    }

    private static String describe(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return "unknown";
        }
        return loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private void prepare(Player player, Location spawn, Kit kit) {
        String arenaWorld = spawn != null && spawn.getWorld() != null ? spawn.getWorld().getName() : null;
        AntiCheatBypass.grant(this.plugin, player, AntiCheatBypass.worldNodes(this.plugin, arenaWorld));
        try {
            player.addScoreboardTag("meowduel");
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        DuelManager.ground(player);
        player.teleport(spawn);
        player.setVelocity(new Vector(0.0, 0.0, 0.0));
        player.setFallDistance(0.0f);
        GameModeGuard.pin(player, GameMode.SURVIVAL);
        try {
            player.setHealth(player.getMaxHealth());
        }
        catch (Throwable t) {
            player.setHealth(20.0);
        }
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setExhaustion(0.0f);
        player.setFireTicks(0);
        player.setFallDistance(0.0f);
        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(potionEffect.getType());
        }
        try {
            player.setAbsorptionAmount(0.0);
        }
        catch (Throwable t) {
            // empty catch block
        }
        if (kit != null) {
            // The player's own arrangement of this kit if they saved one,
            // otherwise the server default.
            this.plugin.getKitLayouts().applyTo(player, kit);
            this.applyPlayerTrims(player, kit.getName());
            // Start effects are NOT applied here - see Kit.applyStartEffects.
            // They are handed out when the countdown finishes, so the whole
            // duration belongs to the fight.
            Kit fixed = kit;
            UUID uUID = player.getUniqueId();
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                Player p = Bukkit.getPlayer((UUID)uUID);
                if (p != null) {
                    this.plugin.getKitLayouts().applyOffhand(p, fixed);
                }
            }, 1L);
        }
    }

    /** The auto pots, at the go signal - including when the countdown is cut
     *  short by both players readying up, which is the same moment. */
    private void giveStartEffects(ActiveDuel duel, Player p1, Player p2) {
        Kit kit = this.plugin.getKitManager().get(duel.getKit());
        if (kit == null) {
            return;
        }
        kit.applyStartEffects(p1);
        kit.applyStartEffects(p2);
    }

    private void countdownTick(ActiveDuel duel, int remainingTicks) {
        if (duel.getState() != ActiveDuel.State.STARTING) {
            return;
        }
        Player p1 = Bukkit.getPlayer((UUID)duel.getPlayer1());
        Player p2 = Bukkit.getPlayer((UUID)duel.getPlayer2());
        if (p1 == null || p2 == null) {
            return;
        }
        if (duel.getReadyCount() >= 2 || remainingTicks <= 0) {
            boolean forced = duel.getReadyCount() >= 2 && remainingTicks > 0;
            this.finishCountdown(duel, forced);
            return;
        }
        Component bar = this.readyBar(duel);
        p1.sendActionBar(bar);
        p2.sendActionBar(bar);
        if (remainingTicks % 20 == 0) {
            int secondsLeft = remainingTicks / 20;
            this.showCountdownNumber(p1, secondsLeft);
            this.showCountdownNumber(p2, secondsLeft);
            Sounds.countdown(p1);
            Sounds.countdown(p2);
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.countdownTick(duel, remainingTicks - 10), 10L);
    }

    /**
     * One number a second, fading out as the next one lands.
     *
     * <p>The three times add up to exactly one second on purpose: the digit
     * appears on the beat with no fade-in, holds while it is the answer, then
     * spends its last eight ticks dissolving under the next one. A hard cut
     * every second flickers, and a title that outlives its second leaves two
     * numbers on screen at once.
     */
    private void showCountdownNumber(Player player, int seconds) {
        this.sendTitle(player, this.msg("titles.countdown.title", "seconds", String.valueOf(seconds)), this.msg("titles.countdown.subtitle", "seconds", String.valueOf(seconds)), 0, 12, 8);
    }

    private void finishCountdown(ActiveDuel duel, boolean forced) {
        if (duel.getState() != ActiveDuel.State.STARTING) {
            return;
        }
        Player p1 = Bukkit.getPlayer((UUID)duel.getPlayer1());
        Player p2 = Bukkit.getPlayer((UUID)duel.getPlayer2());
        if (p1 == null || p2 == null) {
            return;
        }
        duel.setState(ActiveDuel.State.FIGHTING);
        duel.markFightStart();
        this.giveStartEffects(duel, p1, p2);
        // Deliberately no teleport here. startRound already put both players on
        // their spawns when the countdown began; doing it again at FIGHT yanked
        // them back from wherever they had walked, which made the countdown feel
        // like a cage rather than a moment to get set. You can walk, jump,
        // sprint and eat during it - you just cannot hit or drink.
        Component empty = DuelManager.mm("");
        p1.sendActionBar(empty);
        p2.sendActionBar(empty);
        if (forced) {
            String msg = this.msg("countdown.forced", new String[0]);
            p1.sendMessage(msg);
            p2.sendMessage(msg);
        }
        this.sendTitle(p1, this.msg("titles.fight.title", new String[0]), this.msg("titles.fight.subtitle", new String[0]), 0, 40, 10);
        this.sendTitle(p2, this.msg("titles.fight.title", new String[0]), this.msg("titles.fight.subtitle", new String[0]), 0, 40, 10);
        Sounds.fight(p1);
        Sounds.fight(p2);
    }

    public void markReady(UUID playerId) {
        ActiveDuel duel = this.playerDuels.get(playerId);
        if (duel == null || duel.getState() != ActiveDuel.State.STARTING || !duel.isArenaEntered()) {
            return;
        }
        if (!duel.addReady(playerId)) {
            return;
        }
        Player who = Bukkit.getPlayer((UUID)playerId);
        String name = who != null ? who.getName() : "A player";
        String readyMsg = this.msg("countdown.player-ready", "player", name);
        Component bar = this.readyBar(duel);
        Player p1 = Bukkit.getPlayer((UUID)duel.getPlayer1());
        Player p2 = Bukkit.getPlayer((UUID)duel.getPlayer2());
        if (p1 != null) {
            p1.sendMessage(readyMsg);
            p1.sendActionBar(bar);
            Sounds.ready(p1);
        }
        if (p2 != null) {
            p2.sendMessage(readyMsg);
            p2.sendActionBar(bar);
            Sounds.ready(p2);
        }
        if (duel.getReadyCount() >= 2) {
            this.finishCountdown(duel, true);
        }
    }

    /**
     * The one line the action bar carries during a duel countdown.
     *
     * <p>It says what you can do and how many of you have done it. A block
     * meter lived here for a while and was wrong: the countdown is already on
     * screen in numbers a metre tall, so the bar only added a second thing
     * moving in the corner of your eye.
     */
    private Component readyBar(ActiveDuel duel) {
        return DuelManager.mm("<gray>Sneak to get Ready <green>\u2714 <gray>(" + duel.getReadyCount() + "/2)");
    }

    private static Component mm(String miniMessage) {
        return MiniMessage.miniMessage().deserialize((Object)miniMessage);
    }

    public void handleRoundLoss(UUID loserId, Location deathLoc) {
        ActiveDuel duel = this.playerDuels.get(loserId);
        if (duel == null || duel.getState() != ActiveDuel.State.FIGHTING) {
            return;
        }
        duel.setState(ActiveDuel.State.ENDING);
        UUID winnerId = duel.getOpponent(loserId);
        Player loser = Bukkit.getPlayer((UUID)loserId);
        Player winner = Bukkit.getPlayer((UUID)winnerId);
        boolean matchOver = duel.awardRound(winnerId);
        this.plugin.getStatsManager().addWin(winnerId);
        this.plugin.getStatsManager().resetStreak(loserId);
        this.plugin.getStatsManager().save();
        String killerName = winner != null ? winner.getName() : "A player";
        String victimName = loser != null ? loser.getName() : "A player";
        int deathPick = 1 + ThreadLocalRandom.current().nextInt(5);
        this.broadcast(this.msg("death.duel." + deathPick, "killer", killerName, "victim", victimName, "killer_score", String.valueOf(duel.getScoreFor(winnerId)), "victim_score", String.valueOf(duel.getScoreFor(loserId))));
        if (loser != null && deathLoc != null) {
            GameModeGuard.pin(loser, GameMode.SPECTATOR);
            loser.teleport(deathLoc);
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                Player l = Bukkit.getPlayer((UUID)loserId);
                if (l != null && this.playerDuels.get(loserId) == duel && duel.getState() == ActiveDuel.State.ENDING) {
                    GameModeGuard.pin(l, GameMode.SPECTATOR);
                    l.teleport(deathLoc);
                }
            }, 3L);
        }
        if (matchOver) {
            if (winner != null) {
                this.sendTitle(winner, this.msg("titles.victory.title", new String[0]), this.msg("titles.victory.subtitle", "yourScore", String.valueOf(duel.getScoreFor(winnerId)), "theirScore", String.valueOf(duel.getScoreAgainst(winnerId))));
                Sounds.victory(winner);
            }
            if (loser != null) {
                this.sendTitle(loser, this.msg("titles.defeat.title", new String[0]), this.msg("titles.defeat.subtitle", "yourScore", String.valueOf(duel.getScoreFor(loserId)), "theirScore", String.valueOf(duel.getScoreAgainst(loserId))));
                Sounds.defeat(loser);
            }
            if (winner != null) {
                winner.getInventory().clear();
            }
            UUID endWinner = winnerId;
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                if (this.isStill(duel)) {
                    this.endMatch(duel, endWinner, EndReason.ROUND_WIN);
                }
            }, this.winEndTicks);
        } else {
            String roundNo = String.valueOf(duel.getCurrentRound());
            if (winner != null) {
                this.sendTitle(winner, this.msg("titles.round-won.title", new String[0]), this.msg("titles.round-won.subtitle", "yourScore", String.valueOf(duel.getScoreFor(winnerId)), "theirScore", String.valueOf(duel.getScoreAgainst(winnerId))));
                winner.sendMessage(this.msg("round.won-chat", "round", roundNo, "score", String.valueOf(duel.getScoreFor(winnerId)), "opponent_score", String.valueOf(duel.getScoreAgainst(winnerId))));
                Sounds.roundWon(winner);
            }
            if (loser != null) {
                this.sendTitle(loser, this.msg("titles.round-lost.title", new String[0]), this.msg("titles.round-lost.subtitle", "yourScore", String.valueOf(duel.getScoreFor(loserId)), "theirScore", String.valueOf(duel.getScoreAgainst(loserId))));
                loser.sendMessage(this.msg("round.lost-chat", "round", roundNo, "score", String.valueOf(duel.getScoreFor(loserId)), "opponent_score", String.valueOf(duel.getScoreAgainst(loserId))));
                Sounds.roundLost(loser);
            }
            duel.nextRound();
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.regenArena(duel), 2L);
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                if (this.isStill(duel)) {
                    this.startRound(duel);
                }
            }, this.deathSpectateTicks);
        }
    }

    private void regenArena(ActiveDuel duel) {
        Arena arena = duel.getArena();
        if (arena == null || !arena.isAutoRegenerate()) {
            return;
        }
        if (arena.getRegenDelayTicks() > 0) {
            try {
                Bukkit.getScheduler().runTaskLater((Plugin)this.plugin,
                        () -> this.regenNow(duel), (long)arena.getRegenDelayTicks());
                return;
            }
            catch (Throwable t) {
                // The scheduler refuses new tasks once the plugin is disabling.
                // A deferred regen would be silently dropped, so take the delay
                // away rather than the regen.
            }
        }
        this.regenNow(duel);
    }

    /** The regen itself, with no scheduling in it, so it can be called from
     *  shutdown where nothing deferred would ever run. */
    private void regenNow(ActiveDuel duel) {
        Arena arena = duel.getArena();
        if (arena == null || !arena.isAutoRegenerate()) {
            return;
        }
        int written = this.plugin.getArenaManager().regenArena(arena);
        if (written < 0 && !duel.getChangedBlocks().isEmpty()) {
            this.plugin.getArenaManager().restoreBlocks(duel.getChangedBlocks());
        }
        duel.getChangedBlocks().clear();
        this.clearArenaEntities(arena);
        this.plugin.getArenaManager().clearDirty(arena.getName());
    }

    private void clearArenaEntities(Arena arena) {
        World world = arena.getWorld();
        if (world == null || arena.getMin() == null || arena.getMax() == null) {
            return;
        }
        Location min = arena.getMin();
        Location max = arena.getMax();
        Location center = new Location(world, (double)(min.getBlockX() + max.getBlockX()) / 2.0 + 0.5, (double)(min.getBlockY() + max.getBlockY()) / 2.0 + 0.5, (double)(min.getBlockZ() + max.getBlockZ()) / 2.0 + 0.5);
        double dx = (double)(max.getBlockX() - min.getBlockX()) / 2.0 + 2.0;
        double dy = (double)(max.getBlockY() - min.getBlockY()) / 2.0 + 2.0;
        double dz = (double)(max.getBlockZ() - min.getBlockZ()) / 2.0 + 2.0;
        for (Entity entity : world.getNearbyEntities(center, dx, dy, dz)) {
            if (!(entity instanceof Projectile) && !(entity instanceof Item) && !(entity instanceof TNTPrimed) && !(entity instanceof ExperienceOrb)) continue;
            entity.remove();
        }
    }

    /** True while {@code duel} is still the live duel for its players - guards
     *  every delayed callback, so a timer from a finished match can never touch
     *  the new match those players have already started. */
    private boolean isStill(ActiveDuel duel) {
        return !duel.isFinished()
                && (this.playerDuels.get(duel.getPlayer1()) == duel
                 || this.playerDuels.get(duel.getPlayer2()) == duel);
    }

    private void endMatch(ActiveDuel duel, UUID winnerId, EndReason reason) {
        String winnerName;
        if (duel.isFinished()) {
            return;
        }
        duel.setFinished(true);
        duel.setState(ActiveDuel.State.ENDING);
        this.regenArena(duel);
        this.restorePlayer(duel.getPlayer1(), true);
        this.restorePlayer(duel.getPlayer2(), true);
        this.giveSpawnItemsTo(duel.getPlayer1());
        this.giveSpawnItemsTo(duel.getPlayer2());
        this.lastOpponent.put(duel.getPlayer1(), duel.getPlayer2());
        this.lastOpponent.put(duel.getPlayer2(), duel.getPlayer1());
        this.addRematchItem(duel.getPlayer1());
        this.addRematchItem(duel.getPlayer2());
        UUID sumWinner = winnerId;
        ActiveDuel sumDuel = duel;
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            this.openSummary(sumDuel, sumWinner, sumDuel.getPlayer1());
            this.openSummary(sumDuel, sumWinner, sumDuel.getPlayer2());
        }, 2L);
        this.plugin.getTabService().detach(duel.getPlayer1());
        this.plugin.getTabService().detach(duel.getPlayer2());
        this.plugin.getScoreboardService().detach(duel.getPlayer1());
        this.plugin.getScoreboardService().detach(duel.getPlayer2());
        if (duel.getArena().isGlowingOpponent()) {
            Player p1 = Bukkit.getPlayer((UUID)duel.getPlayer1());
            Player p2 = Bukkit.getPlayer((UUID)duel.getPlayer2());
            if (p1 != null) {
                try {
                    p1.setGlowing(false);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
            if (p2 != null) {
                try {
                    p2.setGlowing(false);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
        }
        this.arenasInUse.remove(duel.getArena().getName().toLowerCase());
        this.playerDuels.remove(duel.getPlayer1());
        this.playerDuels.remove(duel.getPlayer2());
        UUID loserId = duel.getOpponent(winnerId);
        this.plugin.getStatsManager().addDuelWin(winnerId);
        this.plugin.getStatsManager().addDuelLoss(loserId);
        this.plugin.getStatsManager().save();
        Player winner = Bukkit.getPlayer((UUID)winnerId);
        Player loser = Bukkit.getPlayer((UUID)loserId);
        String string = winnerName = winner != null ? winner.getName() : "A player";
        this.plugin.getStatsManager().addPlayed(winnerId);
        this.plugin.getStatsManager().addPlayed(loserId);
        switch (reason.ordinal()) {
            case 1: {
                if (winner != null) {
                    winner.sendMessage(this.msg("duel.victory-opponent-left", new String[0]));
                    this.sendTitle(winner, this.msg("titles.victory.title", new String[0]), this.msg("titles.victory.subtitle", new String[0]));
                    Sounds.victory(winner);
                }
                if (loser != null) {
                    loser.sendMessage(this.msg("duel.left-confirm", new String[0]));
                }
                String rage = this.msg("duel.forfeit-broadcast", new String[0]);
                if (winner != null) {
                    winner.sendMessage(rage);
                }
                if (loser == null) break;
                loser.sendMessage(rage);
                break;
            }
            case 2: {
                if (winner == null) break;
                winner.sendMessage(this.msg("duel.victory-opponent-disconnected", new String[0]));
                this.sendTitle(winner, this.msg("titles.victory.title", new String[0]), this.msg("titles.victory.subtitle", new String[0]));
                Sounds.victory(winner);
                break;
            }
            default: {
                if (winner != null) {
                    winner.sendMessage(this.msg("duel.victory", "yourScore", String.valueOf(duel.getScoreFor(winnerId)), "theirScore", String.valueOf(duel.getScoreAgainst(winnerId))));
                }
                if (loser != null) {
                    loser.sendMessage(this.msg("duel.defeat", "winner", winnerName));
                }
                String loserName = loser != null ? loser.getName() : "A player";
                int pick = 1 + ThreadLocalRandom.current().nextInt(10);
                this.broadcast(this.msg("match.toxic." + pick, "winner", winnerName, "loser", loserName, "winner_score", String.valueOf(duel.getScoreFor(winnerId)), "loser_score", String.valueOf(duel.getScoreAgainst(winnerId))));
            }
        }
    }

    private static void ground(Player player) {
        if (player == null) {
            return;
        }
        try {
            if (player.isGliding()) {
                player.setGliding(false);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        player.setFallDistance(0.0f);
    }

    private void broadcast(String message) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(message);
        }
    }

    private void restorePlayer(UUID id, boolean teleport) {
        GameModeGuard.release(id);
        Player player = Bukkit.getPlayer((UUID)id);
        if (player != null) {
            AntiCheatBypass.release(this.plugin, player);
            try {
                player.removeScoreboardTag("meowduel");
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                player.setWorldBorder(null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        PlayerSnapshot snapshot = this.snapshots.remove(id);
        if (player != null && snapshot != null) {
            snapshot.restore(player);
            DuelManager.ground(player);
            if (teleport) {
                Location destination = this.plugin.getDuelSpawn() != null ? this.plugin.getDuelSpawn() : snapshot.getLocation();
                player.teleport(destination);
            }
        }
    }

    public void handleDisconnect(UUID quitterId) {
        ActiveDuel duel = this.playerDuels.get(quitterId);
        if (duel == null) {
            return;
        }
        UUID decided = duel.getMatchWinner();
        UUID winnerId = decided != null ? decided : duel.getOpponent(quitterId);
        this.restorePlayer(quitterId, true);
        this.plugin.getScoreboardService().detach(quitterId);
        this.awardRemainingRounds(duel, winnerId);
        this.endMatch(duel, winnerId, EndReason.DISCONNECT);
    }

    public boolean isInDuel(UUID id) {
        return this.playerDuels.containsKey(id);
    }

    public boolean isArenaInUse(String name) {
        return this.arenasInUse.contains(name.toLowerCase());
    }

    public void markArenaInUse(String name) {
        this.arenasInUse.add(name.toLowerCase());
        // Journalled here rather than at each call site, so duels, events and
        // party matches all get the same protection: a crash that runs no
        // shutdown code still leaves a note for the next startup to regenerate
        // this arena.
        this.plugin.getArenaManager().markDirty(name);
    }

    public void freeArena(String name) {
        this.arenasInUse.remove(name.toLowerCase());
    }

    /** Releases arenas that nothing is actually using any more.
     *
     *  <p>arenasInUse is bookkeeping: a name goes in when a match or event claims
     *  an arena and comes out when it ends. Any path that ends a match without
     *  reaching the release - an exception mid-teardown, a reload, an event torn
     *  down oddly - strands the name in the set, and that arena is unusable until
     *  the next restart. That is the "no free arenas" report with arenas plainly
     *  sitting there empty.
     *
     *  <p>So rather than trust the bookkeeping, this rebuilds it once a second
     *  from what is really happening: the arenas of live duels, plus the event's
     *  arena. Anything else in the set is stale and gets released. Self-healing,
     *  and it covers leak paths that don't exist yet. */
    public void tickArenaReservations() {
        this.tickAbandonedDuels();
        if (this.arenasInUse.isEmpty()) {
            return;
        }
        Set<String> claimed = new HashSet<String>();
        for (ActiveDuel duel : this.playerDuels.values()) {
            if (duel != null && !duel.isFinished() && duel.getArena() != null) {
                claimed.add(duel.getArena().getName().toLowerCase());
            }
        }
        Arena eventArena = this.plugin.getEventManager().getArena();
        if (eventArena != null) {
            claimed.add(eventArena.getName().toLowerCase());
        }
        // Party matches claim arenas too. Without this the sweep would decide a
        // party's arena was stale within a second and hand it to a duel, putting
        // two fights in the same box.
        claimed.addAll(this.plugin.getPartyManager().claimedArenas());
        java.util.Iterator<String> it = this.arenasInUse.iterator();
        while (it.hasNext()) {
            String name = it.next();
            if (!claimed.contains(name)) {
                it.remove();
                this.plugin.getLogger().info("Released arena '" + name
                        + "' - it was still reserved but no duel or event is using it.");
            }
        }
    }

    /** Tells an admin WHY there was no arena - the plain message can't say
     *  whether they are all busy, all unconfigured, or none support the kit. */
    public void explainNoArena(Player who, String kit) {
        if (who != null && who.hasPermission("meowduels.admin")) {
            who.sendMessage(Text.prefixed("&8" + this.arenaAvailability(kit)));
        }
    }

    /** Why no arena can host this kit, for the admin who has to fix it. */
    public String arenaAvailability(String kit) {
        int total = 0;
        int configured = 0;
        int enabled = 0;
        int supporting = 0;
        int free = 0;
        for (Arena arena : this.plugin.getArenaManager().all()) {
            total++;
            if (!arena.isConfigured()) {
                continue;
            }
            configured++;
            if (!arena.isEnabled()) {
                continue;
            }
            enabled++;
            if (!arena.supportsKit(kit)) {
                continue;
            }
            supporting++;
            if (!this.arenasInUse.contains(arena.getName().toLowerCase())) {
                free++;
            }
        }
        return total + " arena(s): " + configured + " configured, " + enabled
                + " enabled, " + supporting + " support this kit, " + free + " free.";
    }

    /** Live counts, refreshed on the main thread by {@link #tickCounts()}.
     *
     *  <p>Volatile and cached on purpose: TAB resolves placeholders on its own
     *  async thread, so counting these on demand would read - and, when pruning,
     *  WRITE - the duel map off the main thread. */
    private volatile int fightingPlayers = 0;
    private volatile int fightingMatches = 0;

    /** Recounts who is actually fighting and drops stale entries. Main thread,
     *  once a second.
     *
     *  <p>An entry that outlives its match - the player went offline, or the
     *  match was already paid out - would otherwise keep inflating the figure
     *  until a restart, which is how "In Duels" crept up toward the online
     *  count. */
    public void tickCounts() {
        if (this.playerDuels.isEmpty()) {
            this.fightingPlayers = 0;
            this.fightingMatches = 0;
            return;
        }
        Set<ActiveDuel> seen = Collections.newSetFromMap(new IdentityHashMap<ActiveDuel, Boolean>());
        java.util.Iterator<Map.Entry<UUID, ActiveDuel>> it = this.playerDuels.entrySet().iterator();
        int players = 0;
        while (it.hasNext()) {
            Map.Entry<UUID, ActiveDuel> e = it.next();
            ActiveDuel duel = e.getValue();
            if (duel == null || duel.isFinished() || Bukkit.getPlayer((UUID) e.getKey()) == null) {
                it.remove();
                continue;
            }
            players++;
            seen.add(duel);
        }
        this.fightingPlayers = players;
        this.fightingMatches = seen.size();
    }

    /** How many players are fighting right now (two per match). */
    public int playersInDuels() {
        return this.fightingPlayers;
    }

    /** How many matches are in progress. */
    public int duelsInProgress() {
        return this.fightingMatches;
    }

    public ActiveDuel getDuel(UUID id) {
        return this.playerDuels.get(id);
    }

    public boolean isArenaBusy(Arena arena) {
        if (arena == null) {
            return true;
        }
        if (this.arenasInUse.contains(arena.getName().toLowerCase())) {
            return true;
        }
        for (ActiveDuel d : this.playerDuels.values()) {
            if (d.getArena() == null || d.getArena() == arena || !arena.overlaps(d.getArena())) continue;
            return true;
        }
        Arena ev = this.plugin.getEventManager().getArena();
        return ev != null && this.plugin.getEventManager().isRunning() && arena.overlaps(ev);
    }

    private void giveSpawnItemsTo(UUID id) {
        Player p = Bukkit.getPlayer((UUID)id);
        if (p != null) {
            this.plugin.giveSpawnItems(p);
        }
    }

    // Elo, ranks, placement matches and the promote/demote titles used to
    // live here. All of it is gone: a duel is a duel, and the only numbers
    // kept about one are the wins, losses and streak the summary shows.

    private static String mmc(String miniMessage) {
        return Colors.toSection(miniMessage);
    }

    private void addRematchItem(UUID id) {
        Player p = Bukkit.getPlayer((UUID)id);
        UUID opp = this.lastOpponent.get(id);
        if (p != null && opp != null) {
            SpawnItems.addRematch(this.plugin, p, opp, this.rematchName(opp));
        }
    }

    private String rematchName(UUID opp) {
        Player o = Bukkit.getPlayer((UUID)opp);
        if (o != null) {
            return o.getName();
        }
        OfflinePlayer off = Bukkit.getOfflinePlayer((UUID)opp);
        return off.getName() == null ? "Opponent" : off.getName();
    }

    private void openSummary(ActiveDuel duel, UUID winnerId, UUID viewerId) {
        Player p = Bukkit.getPlayer((UUID)viewerId);
        if (p != null) {
            try {
                new MatchSummaryMenu(this.plugin, duel, winnerId, viewerId).open(p);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
    }

    public void requestRematch(Player player) {
        UUID opp = this.lastOpponent.get(player.getUniqueId());
        if (opp == null) {
            player.sendMessage(Text.prefixed("&cNo recent opponent to rematch."));
            return;
        }
        Player opponent = Bukkit.getPlayer((UUID)opp);
        if (opponent == null || !opponent.isOnline()) {
            player.sendMessage(Text.prefixed("&cThat player is no longer online."));
            return;
        }
        if (this.isInDuel(opponent.getUniqueId())) {
            player.sendMessage(Text.prefixed("&cThey're already in a duel."));
            return;
        }
        // The clicker, not just the opponent. The summary window outlives the
        // match that opened it, so you can be queued into a new fight and
        // still have Rematch on screen - and every other door into the duel
        // menus is guarded but this one was not, so it opened a kit picker
        // mid-fight that could only ever fail at the end.
        if (this.isInDuel(player.getUniqueId())) {
            player.sendMessage(this.msg("duel.already-in-duel", new String[0]));
            return;
        }
        if (this.plugin.getPartyManager().inParty(player.getUniqueId())) {
            player.sendMessage(Text.prefixed("&cYou're in a party - leave it first, or start a party match."));
            return;
        }
        if (this.plugin.getEventManager().isInvolved(player.getUniqueId())) {
            player.sendMessage(Text.prefixed("&cYou can't duel while you're in the event."));
            return;
        }
        // Through the kit picker, same as /duel: Round Selection has no kit
        // button, so opening it directly would leave the request kitless.
        new KitPickMenu(this.plugin, new DuelConfirmMenu(this.plugin, player, opponent)).open(player);
    }

    private void applyDuelBorder(Player player, Arena arena) {
        if (player == null) {
            return;
        }
        if (arena == null || arena.getMin() == null || arena.getMax() == null) {
            return;
        }
        try {
            Location min = arena.getMin();
            Location max = arena.getMax();
            double cx = (double)(min.getBlockX() + max.getBlockX()) / 2.0 + 0.5;
            double cz = (double)(min.getBlockZ() + max.getBlockZ()) / 2.0 + 0.5;
            double size = Math.max(max.getBlockX() - min.getBlockX() + 1, max.getBlockZ() - min.getBlockZ() + 1);
            WorldBorder wb = Bukkit.createWorldBorder();
            wb.setCenter(cx, cz);
            wb.setWarningDistance(0);
            wb.setWarningTime(0);
            wb.setSize(Math.max(1.0, size));
            player.setWorldBorder(wb);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public boolean hasFreeArenaFor(String kit) {
        return this.plugin.getArenaManager().findFreeArena(a -> this.arenasInUse.contains(a.getName().toLowerCase()) || !a.supportsKit(kit)) != null;
    }

    public boolean hasUsableArena(String kit) {
        return this.plugin.getArenaManager().findFreeArena(a -> !a.supportsKit(kit)) != null;
    }

    public void tickArenaSafety() {
        if (this.playerDuels.isEmpty()) {
            return;
        }
        ArrayList<ActiveDuel> duels = new ArrayList<ActiveDuel>();
        Set seen = Collections.newSetFromMap(new IdentityHashMap());
        for (ActiveDuel d : this.playerDuels.values()) {
            if (!seen.add(d)) continue;
            duels.add(d);
        }
        Arena ev = this.plugin.getEventManager().getArena();
        boolean eventRunning = ev != null && this.plugin.getEventManager().isRunning();
        for (int i = 0; i < duels.size(); ++i) {
            ActiveDuel a = (ActiveDuel)duels.get(i);
            if (!this.playerDuels.containsKey(a.getPlayer1())) continue;
            if (eventRunning && a.getArena() != null && a.getArena().overlaps(ev)) {
                this.forceCancelBooked(a);
                continue;
            }
            for (int j = i + 1; j < duels.size(); ++j) {
                ActiveDuel b = (ActiveDuel)duels.get(j);
                if (!this.playerDuels.containsKey(b.getPlayer1()) || a.getArena() == null || b.getArena() == null || a.getArena() != b.getArena() && !a.getArena().overlaps(b.getArena())) continue;
                this.forceCancelBooked(b.getGameNumber() >= a.getGameNumber() ? b : a);
            }
        }
    }

    private void forceCancelBooked(ActiveDuel duel) {
        duel.setFinished(true);
        duel.setState(ActiveDuel.State.ENDING);
        String booked = this.msg("duel.arena-booked", new String[0]);
        Player p1 = Bukkit.getPlayer((UUID)duel.getPlayer1());
        Player p2 = Bukkit.getPlayer((UUID)duel.getPlayer2());
        this.restorePlayer(duel.getPlayer1(), true);
        this.restorePlayer(duel.getPlayer2(), true);
        this.giveSpawnItemsTo(duel.getPlayer1());
        this.giveSpawnItemsTo(duel.getPlayer2());
        this.plugin.getTabService().detach(duel.getPlayer1());
        this.plugin.getTabService().detach(duel.getPlayer2());
        this.plugin.getScoreboardService().detach(duel.getPlayer1());
        this.plugin.getScoreboardService().detach(duel.getPlayer2());
        this.arenasInUse.remove(duel.getArena().getName().toLowerCase());
        this.playerDuels.remove(duel.getPlayer1());
        this.playerDuels.remove(duel.getPlayer2());
        if (p1 != null) {
            p1.sendMessage(booked);
        }
        if (p2 != null) {
            p2.sendMessage(booked);
        }
    }

    public void tickWorldLocks() {
        if (this.playerDuels.isEmpty()) {
            return;
        }
        Set seen = Collections.newSetFromMap(new IdentityHashMap());
        for (ActiveDuel duel : this.playerDuels.values()) {
            if (duel.getArena() == null || !seen.add(duel.getArena())) continue;
            DuelManager.applyWorldLocks(duel.getArena());
        }
    }

    public static void applyWorldLocks(Arena arena) {
        if (arena == null || arena.getWorld() == null) {
            return;
        }
        World w = arena.getWorld();
        try {
            if (arena.isLockClearWeather()) {
                w.setStorm(false);
                w.setThundering(false);
                w.setWeatherDuration(Integer.MAX_VALUE);
            }
            if (arena.isLockDayTime()) {
                w.setTime(6000L);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void applyPlayerTrims(Player player, String kitName) {
        ItemStack[] armor;
        for (ItemStack piece : armor = player.getInventory().getArmorContents()) {
            Trims.clear(piece);
        }
        this.plugin.getTrimPreferences().applyToArmor(player.getUniqueId(), kitName, armor);
        player.getInventory().setArmorContents(armor);
        player.updateInventory();
    }

    public int fightsWithKit(String kit) {
        Set seen = Collections.newSetFromMap(new IdentityHashMap());
        int fights = 0;
        for (ActiveDuel duel : this.playerDuels.values()) {
            if (!seen.add(duel) || !duel.getKit().equalsIgnoreCase(kit)) continue;
            ++fights;
        }
        return fights;
    }

    public boolean startQueuedDuel(Player p1, Player p2, String kit) {
        if (this.isInDuel(p1.getUniqueId()) || this.isInDuel(p2.getUniqueId())) {
            return false;
        }
        Arena arena = this.plugin.getArenaManager().findFreeArena(a -> this.arenasInUse.contains(a.getName().toLowerCase()) || !a.supportsKit(kit));
        if (arena == null) {
            return false;
        }
        this.startDuel(p1, p2, arena, kit, 1);
        return true;
    }

    /**
     * Server stopping with fights in progress.
     *
     * <p>This used to restore the players and clear the maps - and leave every
     * arena exactly as the fight left it. Stopping the server mid-duel is not a
     * rare event (it is what "both players left" usually means in practice), and
     * the damage was permanent: nothing regenerates an arena that no longer has
     * a duel attached to it.
     *
     * <p>Regen runs inline here. The scheduler is already refusing new tasks by
     * the time onDisable is called, so anything deferred would simply never run.
     */
    public void shutdown() {
        for (ActiveDuel duel : new HashSet<ActiveDuel>(this.playerDuels.values())) {
            if (duel == null || duel.getArena() == null) {
                continue;
            }
            try {
                this.regenNow(duel);
            }
            catch (Throwable t) {
                this.plugin.getLogger().warning("Failed to regenerate arena '"
                        + duel.getArena().getName() + "' during shutdown: " + t);
            }
        }
        for (UUID id : new HashSet<UUID>(this.snapshots.keySet())) {
            this.restorePlayer(id, true);
            this.plugin.getScoreboardService().detach(id);
        }
        this.playerDuels.clear();
        this.arenasInUse.clear();
        this.requests.clear();
    }

    /**
     * Ends any duel whose players are all offline.
     *
     * <p>A quit normally tears its own duel down, but only if that path actually
     * completes - one throw anywhere in endMatch and the duel is left behind with
     * its arena dirty and reserved. Both players being gone is the unambiguous
     * signal that nothing is going to finish it, so this does.
     */
    private void tickAbandonedDuels() {
        if (this.playerDuels.isEmpty()) {
            return;
        }
        for (ActiveDuel duel : new HashSet<ActiveDuel>(this.playerDuels.values())) {
            if (duel == null || duel.isFinished()) {
                continue;
            }
            if (Bukkit.getPlayer((UUID)duel.getPlayer1()) != null
                    || Bukkit.getPlayer((UUID)duel.getPlayer2()) != null) {
                continue;
            }
            this.plugin.getLogger().info("Ending duel in arena '"
                    + (duel.getArena() == null ? "?" : duel.getArena().getName())
                    + "' - both players are offline.");
            try {
                this.endMatch(duel, duel.getPlayer1(), EndReason.DISCONNECT);
            }
            catch (Throwable t) {
                // endMatch does a lot of player-facing work that is meaningless
                // with nobody online. The arena is what matters here.
                this.plugin.getLogger().warning("Abandoned-duel teardown threw: " + t);
                duel.setFinished(true);
                this.playerDuels.remove(duel.getPlayer1());
                this.playerDuels.remove(duel.getPlayer2());
                if (duel.getArena() != null) {
                    this.arenasInUse.remove(duel.getArena().getName().toLowerCase());
                    this.regenNow(duel);
                }
            }
        }
    }

    private void sendTitle(Player player, String title, String subtitle) {
        this.sendTitle(player, title, subtitle, 5, 30, 10);
    }

    private void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    public void leave(Player player) {
        UUID id = player.getUniqueId();
        ActiveDuel duel = this.playerDuels.get(id);
        if (duel == null) {
            player.sendMessage(this.msg("leave.not-in-duel", new String[0]));
            return;
        }
        UUID decided = duel.getMatchWinner();
        UUID winnerId = decided != null ? decided : duel.getOpponent(id);
        duel.setState(ActiveDuel.State.ENDING);
        this.awardRemainingRounds(duel, winnerId);
        this.endMatch(duel, winnerId, decided != null ? EndReason.ROUND_WIN : EndReason.LEAVE);
    }

    private void awardRemainingRounds(ActiveDuel duel, UUID winnerId) {
        for (int guard = 0; guard < 64 && duel.getMatchWinner() == null; ++guard) {
            if (!duel.awardRound(winnerId)) continue;
            return;
        }
    }

    private static enum EndReason {
        ROUND_WIN,
        LEAVE,
        DISCONNECT;

    }
}


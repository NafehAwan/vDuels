package com.vduels.managers;

import com.vduels.VDuels;
import com.vduels.model.ActiveDuel;
import com.vduels.model.Arena;
import com.vduels.model.DuelRequest;
import com.vduels.model.Kit;
import com.vduels.model.PlayerSnapshot;
import com.vduels.util.Sounds;
import com.vduels.util.Text;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The gameplay engine: challenge requests, starting/running/ending duels, round
 * scoring, player state save/restore and arena regeneration hooks.
 */
public class DuelManager {

    private static final long REQUEST_TTL = 60_000L; // 60s
    private static final int COUNTDOWN = 5;

    private final VDuels plugin;

    // target -> pending requests aimed at them
    private final Map<UUID, Map<UUID, DuelRequest>> requests = new HashMap<>();
    private final Map<UUID, ActiveDuel> playerDuels = new HashMap<>();
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();
    private final Set<String> arenasInUse = new HashSet<>();

    public DuelManager(VDuels plugin) {
        this.plugin = plugin;
    }

    // --- requests ---------------------------------------------------------

    public void sendRequest(Player sender, Player target, String kit, int rounds, String arena) {
        if (sender.equals(target)) {
            sender.sendMessage(msg("duel.cannot-duel-self"));
            return;
        }
        if (isInDuel(sender.getUniqueId())) {
            sender.sendMessage(msg("duel.already-in-duel"));
            return;
        }
        if (isInDuel(target.getUniqueId())) {
            sender.sendMessage(msg("duel.target-in-duel", "target", target.getName()));
            return;
        }
        if (plugin.getKitManager().get(kit) == null) {
            sender.sendMessage(msg("duel.kit-gone"));
            return;
        }

        DuelRequest request = new DuelRequest(sender.getUniqueId(), target.getUniqueId(), kit, rounds, arena);
        requests.computeIfAbsent(target.getUniqueId(), k -> new HashMap<>())
                .put(sender.getUniqueId(), request);

        sender.sendMessage(msg("duel.sent", "target", target.getName(), "kit", kit, "rounds", String.valueOf(rounds)));
        sendRequestCard(target, sender, kit, rounds);
    }

    /** Renders the duel-request card in chat, matching the requested style. */
    private void sendRequestCard(Player target, Player sender, String kit, int rounds) {
        String kitLabel = kit.replace('_', ' ').toUpperCase(java.util.Locale.ROOT);
        target.sendMessage("");
        target.sendMessage(msg("request.header", "sender", sender.getName()));
        target.sendMessage(msg("request.kit", "kit", kitLabel));
        target.sendMessage(msg("request.rounds", "rounds", String.valueOf(rounds)));
        target.sendMessage(msg("request.ranked"));
        target.sendMessage("");

        TextComponent click = new TextComponent(msg("request.click"));
        click.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel accept " + sender.getName()));
        click.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(msg("request.click-hover", "sender", sender.getName())).create()));
        target.spigot().sendMessage(click);
        target.sendMessage("");
        Sounds.request(target);
    }

    private String msg(String key, String... placeholders) {
        return plugin.messages().get(key, placeholders);
    }

    public void acceptRequest(Player target, UUID senderId) {
        Map<UUID, DuelRequest> targeted = requests.get(target.getUniqueId());
        DuelRequest request = targeted == null ? null : targeted.get(senderId);
        if (request == null || request.isExpired(REQUEST_TTL)) {
            target.sendMessage(msg("accept.expired"));
            if (targeted != null) {
                targeted.remove(senderId);
            }
            return;
        }
        Player sender = Bukkit.getPlayer(senderId);
        if (sender == null) {
            target.sendMessage(msg("accept.sender-offline"));
            targeted.remove(senderId);
            return;
        }
        if (isInDuel(sender.getUniqueId()) || isInDuel(target.getUniqueId())) {
            target.sendMessage(msg("accept.one-in-duel"));
            return;
        }

        Arena arena = resolveArena(request);
        if (arena == null) {
            target.sendMessage(msg("accept.no-arena"));
            sender.sendMessage(msg("accept.no-arena"));
            return;
        }

        targeted.remove(senderId);
        startDuel(sender, target, arena, request.getKit(), request.getRounds());
    }

    /**
     * Picks the arena for a request: the challenger's chosen arena if it is
     * configured, supports the kit and is free; otherwise any free compatible
     * arena.
     */
    private Arena resolveArena(DuelRequest request) {
        if (request.getArena() != null) {
            Arena chosen = plugin.getArenaManager().get(request.getArena());
            if (chosen != null && chosen.isConfigured()
                    && chosen.supportsKit(request.getKit())
                    && !arenasInUse.contains(chosen.getName().toLowerCase())) {
                return chosen;
            }
        }
        return plugin.getArenaManager().findFreeArena(
                a -> arenasInUse.contains(a.getName().toLowerCase()) || !a.supportsKit(request.getKit()));
    }

    public DuelRequest getMostRecentRequest(Player target) {
        Map<UUID, DuelRequest> targeted = requests.get(target.getUniqueId());
        if (targeted == null || targeted.isEmpty()) {
            return null;
        }
        DuelRequest latest = null;
        for (DuelRequest r : targeted.values()) {
            if (!r.isExpired(REQUEST_TTL) && (latest == null || r.getCreatedAt() > latest.getCreatedAt())) {
                latest = r;
            }
        }
        return latest;
    }

    // --- duel lifecycle ---------------------------------------------------

    private void startDuel(Player p1, Player p2, Arena arena, String kit, int rounds) {
        // A duel supersedes any pending queue membership.
        plugin.getQueueManager().remove(p1.getUniqueId());
        plugin.getQueueManager().remove(p2.getUniqueId());
        arenasInUse.add(arena.getName().toLowerCase());
        snapshots.put(p1.getUniqueId(), PlayerSnapshot.capture(p1));
        snapshots.put(p2.getUniqueId(), PlayerSnapshot.capture(p2));

        ActiveDuel duel = new ActiveDuel(p1.getUniqueId(), p2.getUniqueId(), arena, kit, rounds);
        playerDuels.put(p1.getUniqueId(), duel);
        playerDuels.put(p2.getUniqueId(), duel);

        plugin.getScoreboardService().attach(p1, duel);
        plugin.getScoreboardService().attach(p2, duel);

        sendStartCard(p1, p2.getName(), duel);
        sendStartCard(p2, p1.getName(), duel);
        startRound(duel);
    }

    /** The multi-line "Duel:" card shown to each player when the match begins. */
    private void sendStartCard(Player player, String opponentName, ActiveDuel duel) {
        String kitLabel = kitLabel(duel.getKit());
        String rounds = String.valueOf(duel.getRoundsToWin());
        player.sendMessage(msg("duel.start.header"));
        player.sendMessage(msg("duel.start.opponent", "opponent", opponentName));
        player.sendMessage(msg("duel.start.kit", "kit", kitLabel));
        player.sendMessage(msg("duel.start.rounds", "rounds", rounds));
        player.sendMessage(msg("duel.start.ranked", "ranked", "No"));
        player.sendMessage("");
        player.sendMessage(msg("duel.start.leave"));
    }

    /**
     * The kit name to show in chat: its display name's plain text (MiniMessage
     * tags stripped) when set, otherwise the kit id.
     */
    private String kitLabel(String kitId) {
        Kit kit = plugin.getKitManager().get(kitId);
        if (kit != null && kit.getDisplayName() != null && !kit.getDisplayName().isEmpty()) {
            return kit.getDisplayName().replaceAll("<[^>]*>", "");
        }
        return kitId;
    }

    private void startRound(ActiveDuel duel) {
        duel.setState(ActiveDuel.State.STARTING);
        Player p1 = Bukkit.getPlayer(duel.getPlayer1());
        Player p2 = Bukkit.getPlayer(duel.getPlayer2());
        if (p1 == null || p2 == null) {
            handleDisconnect(p1 == null ? duel.getPlayer1() : duel.getPlayer2());
            return;
        }

        Kit kit = plugin.getKitManager().get(duel.getKit());
        prepare(p1, duel.getArena().getSpawn1(), kit);
        prepare(p2, duel.getArena().getSpawn2(), kit);

        String round = String.valueOf(duel.getCurrentRound());
        String toWin = String.valueOf(duel.getRoundsToWin());
        p1.sendMessage(msg("duel.round", "round", round, "roundsToWin", toWin));
        p2.sendMessage(msg("duel.round", "round", round, "roundsToWin", toWin));

        runCountdown(duel, COUNTDOWN);
    }

    private void prepare(Player player, Location spawn, Kit kit) {
        player.teleport(spawn);
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(10f);
        player.setFireTicks(0);
        player.setFallDistance(0f);
        for (var effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        if (kit != null) {
            kit.applyTo(player);
        }
    }

    private void runCountdown(ActiveDuel duel, int secondsLeft) {
        Player p1 = Bukkit.getPlayer(duel.getPlayer1());
        Player p2 = Bukkit.getPlayer(duel.getPlayer2());
        if (p1 == null || p2 == null) {
            return;
        }
        if (secondsLeft <= 0) {
            duel.setState(ActiveDuel.State.FIGHTING);
            // FIGHT holds a touch longer; no fade-in so it snaps in after "1".
            sendTitle(p1, msg("titles.fight.title"), msg("titles.fight.subtitle"), 0, 40, 10);
            sendTitle(p2, msg("titles.fight.title"), msg("titles.fight.subtitle"), 0, 40, 10);
            Sounds.fight(p1);
            Sounds.fight(p2);
            return;
        }
        String secs = String.valueOf(secondsLeft);
        String ctTitle = msg("titles.countdown.title", "seconds", secs);
        String ctSub = msg("titles.countdown.subtitle", "seconds", secs);
        // No fade in/out and a >1s hold so each number cleanly replaces the last.
        sendTitle(p1, ctTitle, ctSub, 0, 22, 2);
        sendTitle(p2, ctTitle, ctSub, 0, 22, 2);
        Sounds.countdown(p1);
        Sounds.countdown(p2);
        // Movement is frozen by DuelListener during STARTING; no re-teleport
        // needed (which would also reset where players are aiming).
        Bukkit.getScheduler().runTaskLater(plugin, () -> runCountdown(duel, secondsLeft - 1), 20L);
    }

    /**
     * Called when {@code loser} would die during a duel. Ends the round (and the
     * match if the winner has reached the target score).
     */
    public void handleRoundLoss(UUID loserId) {
        ActiveDuel duel = playerDuels.get(loserId);
        if (duel == null || duel.getState() != ActiveDuel.State.FIGHTING) {
            return;
        }
        duel.setState(ActiveDuel.State.ENDING);
        UUID winnerId = duel.getOpponent(loserId);

        // The loser has already died and respawned (see DuelListener); no need
        // to touch their health here.
        Player loser = Bukkit.getPlayer(loserId);
        Player winner = Bukkit.getPlayer(winnerId);

        boolean matchOver = duel.awardRound(winnerId);

        // Regenerate anything changed this round before the next one begins.
        if (duel.getArena().isAutoRegenerate() && !duel.getChangedBlocks().isEmpty()) {
            plugin.getArenaManager().restoreBlocks(duel.getChangedBlocks());
            duel.getChangedBlocks().clear();
        }

        if (winner != null) {
            sendTitle(winner, msg("titles.round-won.title"),
                    msg("titles.round-won.subtitle",
                            "yourScore", String.valueOf(duel.getScoreFor(winnerId)),
                            "theirScore", String.valueOf(duel.getScoreAgainst(winnerId))));
            Sounds.roundWon(winner);
        }
        if (loser != null) {
            sendTitle(loser, msg("titles.round-lost.title"),
                    msg("titles.round-lost.subtitle",
                            "yourScore", String.valueOf(duel.getScoreFor(loserId)),
                            "theirScore", String.valueOf(duel.getScoreAgainst(loserId))));
            Sounds.roundLost(loser);
        }

        if (matchOver) {
            endMatch(duel, winnerId, false);
        } else {
            duel.nextRound();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (playerDuels.containsKey(duel.getPlayer1())) {
                    startRound(duel);
                }
            }, 60L);
        }
    }

    private void endMatch(ActiveDuel duel, UUID winnerId, boolean silent) {
        duel.setState(ActiveDuel.State.ENDING);

        // Final regen sweep.
        if (duel.getArena().isAutoRegenerate() && !duel.getChangedBlocks().isEmpty()) {
            plugin.getArenaManager().restoreBlocks(duel.getChangedBlocks());
            duel.getChangedBlocks().clear();
        }

        restorePlayer(duel.getPlayer1(), true);
        restorePlayer(duel.getPlayer2(), true);

        plugin.getScoreboardService().detach(duel.getPlayer1());
        plugin.getScoreboardService().detach(duel.getPlayer2());

        arenasInUse.remove(duel.getArena().getName().toLowerCase());
        playerDuels.remove(duel.getPlayer1());
        playerDuels.remove(duel.getPlayer2());

        if (silent) {
            return;
        }
        UUID loserId = duel.getOpponent(winnerId);
        Player winner = Bukkit.getPlayer(winnerId);
        Player loser = Bukkit.getPlayer(loserId);
        String winnerName = winner != null ? winner.getName() : "A player";
        if (winner != null) {
            winner.sendMessage(msg("duel.victory",
                    "yourScore", String.valueOf(duel.getScoreFor(winnerId)),
                    "theirScore", String.valueOf(duel.getScoreAgainst(winnerId))));
            sendTitle(winner, msg("titles.victory.title"), msg("titles.victory.subtitle"));
            Sounds.victory(winner);
        }
        if (loser != null) {
            loser.sendMessage(msg("duel.defeat", "winner", winnerName));
            sendTitle(loser, msg("titles.defeat.title"), msg("titles.defeat.subtitle"));
            Sounds.defeat(loser);
        }
    }

    private void restorePlayer(UUID id, boolean teleport) {
        Player player = Bukkit.getPlayer(id);
        PlayerSnapshot snapshot = snapshots.remove(id);
        if (player != null && snapshot != null) {
            snapshot.restore(player);
            if (teleport) {
                player.teleport(snapshot.getLocation());
            }
        }
    }

    /** A duel participant disconnected: award the opponent and end the match. */
    public void handleDisconnect(UUID quitterId) {
        ActiveDuel duel = playerDuels.get(quitterId);
        if (duel == null) {
            return;
        }
        UUID winnerId = duel.getOpponent(quitterId);
        // Restore the quitter's real inventory now (they are still valid during
        // the quit event) so they don't keep the kit on rejoin. No teleport -
        // teleporting a leaving player is rejected by the server.
        restorePlayer(quitterId, false);
        plugin.getScoreboardService().detach(quitterId);
        // Ensure the winner reaches the target score for messaging.
        while (duel.getMatchWinner() == null) {
            if (duel.awardRound(winnerId)) {
                break;
            }
        }
        endMatch(duel, winnerId, false);
    }

    public boolean isInDuel(UUID id) {
        return playerDuels.containsKey(id);
    }

    public ActiveDuel getDuel(UUID id) {
        return playerDuels.get(id);
    }

    /**
     * The number of ongoing fights (duels) using the given kit. Each duel counts
     * once even though both players are its participants.
     */
    public int fightsWithKit(String kit) {
        Set<ActiveDuel> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        int fights = 0;
        for (ActiveDuel duel : playerDuels.values()) {
            if (seen.add(duel) && duel.getKit().equalsIgnoreCase(kit)) {
                fights++;
            }
        }
        return fights;
    }

    /**
     * Starts a duel between two queued players on a free arena that supports the
     * kit. Returns false (leaving them queued) when no arena is available.
     */
    public boolean startQueuedDuel(Player p1, Player p2, String kit) {
        if (isInDuel(p1.getUniqueId()) || isInDuel(p2.getUniqueId())) {
            return false;
        }
        Arena arena = plugin.getArenaManager().findFreeArena(
                a -> arenasInUse.contains(a.getName().toLowerCase()) || !a.supportsKit(kit));
        if (arena == null) {
            return false;
        }
        startDuel(p1, p2, arena, kit, 1);
        return true;
    }

    /** Restores everyone and cleans up; used on plugin disable. */
    public void shutdown() {
        for (UUID id : new HashSet<>(snapshots.keySet())) {
            restorePlayer(id, true);
            plugin.getScoreboardService().detach(id);
        }
        playerDuels.clear();
        arenasInUse.clear();
        requests.clear();
    }

    private void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, 5, 30, 10);
    }

    private void sendTitle(Player player, String title, String subtitle,
                           int fadeIn, int stay, int fadeOut) {
        // title/subtitle are already coloured by the message manager.
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    /** A player forfeits their current duel with {@code /leave}. */
    public void leave(Player player) {
        UUID id = player.getUniqueId();
        ActiveDuel duel = playerDuels.get(id);
        if (duel == null) {
            player.sendMessage(msg("leave.not-in-duel"));
            return;
        }
        UUID winnerId = duel.getOpponent(id);
        duel.setState(ActiveDuel.State.ENDING);
        // Award the remaining rounds to the opponent so the match ends cleanly.
        while (duel.getMatchWinner() == null) {
            if (duel.awardRound(winnerId)) {
                break;
            }
        }
        endMatch(duel, winnerId, false);
    }
}

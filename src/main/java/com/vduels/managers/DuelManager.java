package com.vduels.managers;

import com.vduels.VDuels;
import com.vduels.model.ActiveDuel;
import com.vduels.model.Arena;
import com.vduels.model.DuelRequest;
import com.vduels.model.Kit;
import com.vduels.model.PlayerSnapshot;
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
    private static final int COUNTDOWN = 3;

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
            sender.sendMessage(Text.prefixed("&cYou cannot duel yourself."));
            return;
        }
        if (isInDuel(sender.getUniqueId())) {
            sender.sendMessage(Text.prefixed("&cYou are already in a duel."));
            return;
        }
        if (isInDuel(target.getUniqueId())) {
            sender.sendMessage(Text.prefixed("&c" + target.getName() + " is already in a duel."));
            return;
        }
        if (plugin.getKitManager().get(kit) == null) {
            sender.sendMessage(Text.prefixed("&cThat kit no longer exists."));
            return;
        }

        DuelRequest request = new DuelRequest(sender.getUniqueId(), target.getUniqueId(), kit, rounds, arena);
        requests.computeIfAbsent(target.getUniqueId(), k -> new HashMap<>())
                .put(sender.getUniqueId(), request);

        sender.sendMessage(Text.prefixed("&aChallenge sent to &e" + target.getName()
                + "&a (&f" + kit + "&a, first to &f" + rounds + "&a)."));

        sendRequestCard(target, sender, kit, rounds);
    }

    /** Renders the duel-request card in chat, matching the requested style. */
    private void sendRequestCard(Player target, Player sender, String kit, int rounds) {
        String kitLabel = kit.replace('_', ' ').toUpperCase(java.util.Locale.ROOT);
        target.sendMessage("");
        target.sendMessage(Text.color("&6DUEL REQUEST FROM &e&l" + sender.getName()));
        target.sendMessage(Text.color("&eKit: &e&l" + kitLabel));
        target.sendMessage(Text.color("&eRounds: &f" + rounds));
        target.sendMessage(Text.color("&eRanked: &c&lDISABLED"));
        target.sendMessage("");

        TextComponent click = new TextComponent(Text.color("&6&l[CLICK HERE]"));
        click.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel accept " + sender.getName()));
        click.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Text.color("&aClick to accept the duel from &f" + sender.getName())).create()));
        target.spigot().sendMessage(click);
        target.sendMessage("");
    }

    public void acceptRequest(Player target, UUID senderId) {
        Map<UUID, DuelRequest> targeted = requests.get(target.getUniqueId());
        DuelRequest request = targeted == null ? null : targeted.get(senderId);
        if (request == null || request.isExpired(REQUEST_TTL)) {
            target.sendMessage(Text.prefixed("&cThat duel request has expired."));
            if (targeted != null) {
                targeted.remove(senderId);
            }
            return;
        }
        Player sender = Bukkit.getPlayer(senderId);
        if (sender == null) {
            target.sendMessage(Text.prefixed("&cThat player is no longer online."));
            targeted.remove(senderId);
            return;
        }
        if (isInDuel(sender.getUniqueId()) || isInDuel(target.getUniqueId())) {
            target.sendMessage(Text.prefixed("&cOne of you is already in a duel."));
            return;
        }

        Arena arena = resolveArena(request);
        if (arena == null) {
            target.sendMessage(Text.prefixed("&cNo free arena is available for that kit right now."));
            sender.sendMessage(Text.prefixed("&cNo free arena is available for that kit right now."));
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
        arenasInUse.add(arena.getName().toLowerCase());
        snapshots.put(p1.getUniqueId(), PlayerSnapshot.capture(p1));
        snapshots.put(p2.getUniqueId(), PlayerSnapshot.capture(p2));

        ActiveDuel duel = new ActiveDuel(p1.getUniqueId(), p2.getUniqueId(), arena, kit, rounds);
        playerDuels.put(p1.getUniqueId(), duel);
        playerDuels.put(p2.getUniqueId(), duel);

        plugin.getScoreboardService().attach(p1, duel);
        plugin.getScoreboardService().attach(p2, duel);

        p1.sendMessage(Text.prefixed("&aDuel starting against &e" + p2.getName() + "&a!"));
        p2.sendMessage(Text.prefixed("&aDuel starting against &e" + p1.getName() + "&a!"));
        startRound(duel);
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

        String roundLabel = "&fRound &b" + duel.getCurrentRound();
        p1.sendMessage(Text.prefixed(roundLabel + " &7- first to " + duel.getRoundsToWin()));
        p2.sendMessage(Text.prefixed(roundLabel + " &7- first to " + duel.getRoundsToWin()));

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
            sendTitle(p1, "&a&lFIGHT!", "");
            sendTitle(p2, "&a&lFIGHT!", "");
            return;
        }
        sendTitle(p1, "&e" + secondsLeft, "&7Get ready...");
        sendTitle(p2, "&e" + secondsLeft, "&7Get ready...");
        // Keep players in place during the countdown.
        p1.teleport(duel.getArena().getSpawn1());
        p2.teleport(duel.getArena().getSpawn2());
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

        Player loser = Bukkit.getPlayer(loserId);
        Player winner = Bukkit.getPlayer(winnerId);
        if (loser != null) {
            loser.setHealth(Math.min(20.0, loser.getHealth() <= 0 ? 20.0 : loser.getHealth()));
            loser.setFireTicks(0);
        }

        boolean matchOver = duel.awardRound(winnerId);

        // Regenerate anything changed this round before the next one begins.
        if (duel.getArena().isAutoRegenerate() && !duel.getChangedBlocks().isEmpty()) {
            plugin.getArenaManager().restoreBlocks(duel.getChangedBlocks());
            duel.getChangedBlocks().clear();
        }

        String score = "&b" + duel.getScore1() + " &7- &b" + duel.getScore2();
        if (winner != null) {
            sendTitle(winner, "&aRound won!", score);
        }
        if (loser != null) {
            sendTitle(loser, "&cRound lost", score);
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
        Player winner = Bukkit.getPlayer(winnerId);
        Player loser = Bukkit.getPlayer(duel.getOpponent(winnerId));
        String winnerName = winner != null ? winner.getName() : "A player";
        if (winner != null) {
            winner.sendMessage(Text.prefixed("&a&lVICTORY! &7You won the duel "
                    + "(" + duel.getScore1() + " - " + duel.getScore2() + ")."));
            sendTitle(winner, "&a&lVICTORY", "");
        }
        if (loser != null) {
            loser.sendMessage(Text.prefixed("&c&lDEFEAT. &7" + winnerName + " won the duel."));
            sendTitle(loser, "&c&lDEFEAT", "");
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
        player.sendTitle(Text.color(title), Text.color(subtitle), 5, 30, 10);
    }
}

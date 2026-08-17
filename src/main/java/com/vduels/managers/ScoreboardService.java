package com.vduels.managers;

import com.vduels.VDuels;
import com.vduels.model.ActiveDuel;
import com.vduels.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player duel sidebar scoreboard. Each duellist gets their own board showing
 * their score/team/ping/time and a configurable server-IP footer. Only players
 * currently in a duel have a board. Lines are backed by team prefixes so live
 * updates (ping, time) don't flicker.
 */
public class ScoreboardService {

    // One unique, (visually empty) entry string per line.
    private static final ChatColor[] LINE_KEYS = ChatColor.values();
    private static final int LINES = 8;

    // Monochrome unicode glyphs (coloured by legacy codes) matching the design.
    private static final String ICON_SCORE = "ⓘ"; // circled i
    private static final String ICON_TEAM = "⚑";  // flag
    private static final String ICON_PING = "★";  // star
    private static final String ICON_TIME = "⌚";  // watch

    private final VDuels plugin;
    private final Map<UUID, DuelBoard> boards = new HashMap<>();

    public ScoreboardService(VDuels plugin) {
        this.plugin = plugin;
    }

    public void attach(Player player, ActiveDuel duel) {
        DuelBoard board = new DuelBoard();
        boards.put(player.getUniqueId(), board);
        player.setScoreboard(board.scoreboard);
        update(player, duel, board);
    }

    public void detach(UUID id) {
        boards.remove(id);
        Player player = Bukkit.getPlayer(id);
        if (player != null && Bukkit.getScoreboardManager() != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    /** Refreshes every active board; called on a repeating task. */
    public void tick() {
        if (boards.isEmpty()) {
            return;
        }
        for (UUID id : new HashMap<>(boards).keySet()) {
            Player player = Bukkit.getPlayer(id);
            ActiveDuel duel = plugin.getDuelManager().getDuel(id);
            DuelBoard board = boards.get(id);
            if (player == null || duel == null || board == null) {
                detach(id);
                continue;
            }
            update(player, duel, board);
        }
    }

    private void update(Player player, ActiveDuel duel, DuelBoard board) {
        UUID id = player.getUniqueId();
        boolean blue = duel.isBlue(id);
        String teamColor = blue ? "&9" : "&c";
        String teamName = blue ? "BLUE" : "RED";
        long seconds = Math.max(0, (System.currentTimeMillis() - duel.getStartedAt()) / 1000L);
        String time = String.format("%02d:%02d", seconds / 60, seconds % 60);

        board.setLine(0, "");
        board.setLine(1, "&7" + ICON_SCORE + " &fScore: &b" + duel.getScoreFor(id)
                + " &7- &c" + duel.getScoreAgainst(id));
        board.setLine(2, "");
        board.setLine(3, teamColor + ICON_TEAM + " &fTeam: " + teamColor + teamName);
        board.setLine(4, "&a" + ICON_PING + " &fPing: &a" + player.getPing() + "ms");
        board.setLine(5, "&6" + ICON_TIME + " &fTime: &f" + time);
        board.setLine(6, "");
        board.setLine(7, "&7" + ICON_SCORE + " &b" + plugin.getScoreboardIp());
    }

    /** Holds one player's scoreboard and its per-line teams. */
    private static final class DuelBoard {
        private final Scoreboard scoreboard;
        private final Team[] teams = new Team[LINES];
        private final String[] entries = new String[LINES];

        private DuelBoard() {
            this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective objective = scoreboard.registerNewObjective("vduels", Criteria.DUMMY,
                    Text.color("&e&lDUELS"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            // Hide the red side-numbers Minecraft draws for each sidebar line.
            objective.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());
            for (int i = 0; i < LINES; i++) {
                String entry = LINE_KEYS[i].toString();
                entries[i] = entry;
                Team team = scoreboard.registerNewTeam("line" + i);
                team.addEntry(entry);
                objective.getScore(entry).setScore(LINES - i);
                teams[i] = team;
            }
        }

        private void setLine(int index, String text) {
            if (index < 0 || index >= LINES) {
                return;
            }
            teams[index].setPrefix(Text.color(text));
        }
    }
}

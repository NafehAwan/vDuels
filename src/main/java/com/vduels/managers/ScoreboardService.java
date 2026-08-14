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
    private static final int LINES = 7;

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
        String team = duel.isBlue(id) ? "&9&lBLUE" : "&c&lRED";
        String score = "&f" + duel.getScoreFor(id) + " &7- &f" + duel.getScoreAgainst(id);
        long seconds = Math.max(0, (System.currentTimeMillis() - duel.getStartedAt()) / 1000L);
        String time = String.format("%02d:%02d", seconds / 60, seconds % 60);

        board.setLine(0, "&7&m                    ");
        board.setLine(1, "&aScore: " + score);
        board.setLine(2, "&bTeam: " + team);
        board.setLine(3, "&aPing: &f" + player.getPing() + "ms");
        board.setLine(4, "&eTime: &f" + time);
        board.setLine(5, "&7&m                    ");
        board.setLine(6, "&e" + plugin.getScoreboardIp());
    }

    /** Holds one player's scoreboard and its per-line teams. */
    private static final class DuelBoard {
        private final Scoreboard scoreboard;
        private final Team[] teams = new Team[LINES];
        private final String[] entries = new String[LINES];

        private DuelBoard() {
            this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective objective = scoreboard.registerNewObjective("vduels", Criteria.DUMMY,
                    Text.color("&b&lDUELS"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
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

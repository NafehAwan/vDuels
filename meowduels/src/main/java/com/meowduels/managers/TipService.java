package com.meowduels.managers;

import com.meowduels.MeowDuels;
import com.meowduels.util.Colors;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * The occasional line in chat that says how something works.
 *
 * <p>Two lists, because the useful thing to say depends on where you are
 * standing. Someone in the lobby wants to know the commands; someone mid-fight
 * wants to know why the lobby went quiet and how to get one line out of the
 * arena. Sending the lobby list to a fighter is how a tip becomes spam.
 *
 * <p>Each list walks its own cursor, so both are seen in order rather than at
 * random, and a server with one tip in a list does not repeat a second one it
 * does not have.
 */
public class TipService {
    private final MeowDuels plugin;
    private int secondsLeft;
    private int globalIndex = 0;
    private int matchIndex = 0;

    public TipService(MeowDuels plugin) {
        this.plugin = plugin;
        this.secondsLeft = this.intervalSeconds();
    }

    private boolean enabled() {
        return this.plugin.getConfig().getBoolean("tips.enabled", true);
    }

    private int intervalSeconds() {
        return Math.max(30, this.plugin.getConfig().getInt("tips.interval-seconds", 180));
    }

    /** Called once a second. */
    public void tick() {
        if (!this.enabled()) {
            return;
        }
        if (--this.secondsLeft > 0) {
            return;
        }
        this.secondsLeft = this.intervalSeconds();
        this.send();
    }

    /** Re-read the interval after a reload, rather than on the next fire. */
    public void reload() {
        this.secondsLeft = this.intervalSeconds();
    }

    private void send() {
        List<String> global = this.plugin.getConfig().getStringList("tips.global");
        List<String> match = this.plugin.getConfig().getStringList("tips.match");
        String globalLine = TipService.pick(global, this.globalIndex);
        String matchLine = TipService.pick(match, this.matchIndex);
        if (globalLine != null) {
            this.globalIndex = (this.globalIndex + 1) % global.size();
        }
        if (matchLine != null) {
            this.matchIndex = (this.matchIndex + 1) % match.size();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            String line = this.inMatch(player) ? matchLine : globalLine;
            if (line == null || line.isEmpty()) continue;
            player.sendMessage(Colors.toSection(line));
        }
    }

    /**
     * In a duel, in a party match, or watching one.
     *
     * <p>Spectators count as in the match on purpose: they are reading the
     * match's chat, so the match's tips are the ones that explain what they
     * are looking at.
     */
    private boolean inMatch(Player player) {
        java.util.UUID id = player.getUniqueId();
        return this.plugin.getDuelManager().isInDuel(id)
                || this.plugin.getPartyManager().inPartyMatch(id)
                || this.plugin.getSpectateManager().getWatchedTarget(id) != null;
    }

    private static String pick(List<String> list, int index) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(index % list.size());
    }
}

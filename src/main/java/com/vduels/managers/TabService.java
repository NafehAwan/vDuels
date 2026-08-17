package com.vduels.managers;

import com.vduels.VDuels;
import com.vduels.model.ActiveDuel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Gives each fight its own tab list: the two fighters see only each other
 * (everyone else is hidden), each name carries a team-coloured "&#10209;" flag,
 * and a configurable header/footer (with a gradient-capable title) shows live
 * player counts. Everything is restored when the duel ends.
 */
public class TabService {

    private static final String BLUE_TEAM = "vdblue";
    private static final String RED_TEAM = "vdred";

    private final VDuels plugin;
    private final Set<UUID> fighters = new HashSet<>();

    public TabService(VDuels plugin) {
        this.plugin = plugin;
    }

    /** Put both fighters onto their isolated per-fight tab. */
    public void attach(ActiveDuel duel) {
        Player p1 = Bukkit.getPlayer(duel.getPlayer1());
        Player p2 = Bukkit.getPlayer(duel.getPlayer2());
        if (p1 == null || p2 == null) {
            return;
        }
        setup(p1, p2, duel);
        setup(p2, p1, duel);
    }

    private void setup(Player viewer, Player other, ActiveDuel duel) {
        fighters.add(viewer.getUniqueId());
        // Hide everyone who isn't part of this fight.
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(viewer.getUniqueId())
                    && !online.getUniqueId().equals(other.getUniqueId())) {
                viewer.hidePlayer(plugin, online);
            }
        }
        applyTeams(viewer, duel);
        sendHeaderFooter(viewer);
    }

    /** Colour both fighters' names (and flags) on the viewer's scoreboard. */
    private void applyTeams(Player viewer, ActiveDuel duel) {
        Scoreboard sb = viewer.getScoreboard();
        if (sb == null) {
            return;
        }
        Team blue = team(sb, BLUE_TEAM, ChatColor.BLUE, "§9⚑ ");
        Team red = team(sb, RED_TEAM, ChatColor.RED, "§c⚑ ");
        for (UUID id : new UUID[]{duel.getPlayer1(), duel.getPlayer2()}) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                (duel.isBlue(id) ? blue : red).addEntry(p.getName());
            }
        }
    }

    private Team team(Scoreboard sb, String name, ChatColor color, String prefix) {
        Team t = sb.getTeam(name);
        if (t == null) {
            t = sb.registerNewTeam(name);
        }
        t.setColor(color);
        t.setPrefix(prefix);
        return t;
    }

    private void sendHeaderFooter(Player viewer) {
        int online = Bukkit.getOnlinePlayers().size();
        int fighting = plugin.getDuelManager().playersInDuels();
        int global = online; // single server: global == online

        String header = plugin.getTabTitle()
                + "\n<gray>Global Players: <white>" + global
                + "\n \n<gray>Online: <green>" + online
                + " <dark_gray>• <gray>Fighting: <red>" + fighting;
        String footer = " \n<aqua>" + plugin.getTabDiscord()
                + "\n<yellow>" + plugin.getTabStore();

        viewer.sendPlayerListHeaderAndFooter(mm(header), mm(footer));
    }

    /** Refreshes the header/footer counts for everyone in a fight (per second). */
    public void tick() {
        if (fighters.isEmpty()) {
            return;
        }
        for (UUID id : new HashSet<>(fighters)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) {
                fighters.remove(id);
            } else {
                sendHeaderFooter(p);
            }
        }
    }

    /** A newcomer must stay hidden from anyone currently in a fight. */
    public void onPlayerJoin(Player joiner) {
        for (UUID id : fighters) {
            Player fighter = Bukkit.getPlayer(id);
            if (fighter != null && !fighter.getUniqueId().equals(joiner.getUniqueId())) {
                fighter.hidePlayer(plugin, joiner);
            }
        }
    }

    /** Restore a fighter's normal tab (show everyone, clear header/footer). */
    public void detach(UUID id) {
        if (!fighters.remove(id)) {
            return;
        }
        Player viewer = Bukkit.getPlayer(id);
        if (viewer == null) {
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(id)) {
                viewer.showPlayer(plugin, online);
            }
        }
        viewer.sendPlayerListHeaderAndFooter(mm(""), mm(""));
    }

    private static Component mm(String miniMessage) {
        return MiniMessage.miniMessage().deserialize(miniMessage);
    }
}

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.minimessage.MiniMessage
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scoreboard.Scoreboard
 *  org.bukkit.scoreboard.Team
 */
package com.meowduels.managers;

import com.meowduels.MeowDuels;
import com.meowduels.model.ActiveDuel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TabService {
    private static final String AQUA_TEAM = "vdaqua";
    private static final String RED_TEAM = "vdred";
    private final MeowDuels plugin;
    private final boolean external;
    private final Map<UUID, ActiveDuel> memberDuel = new HashMap<UUID, ActiveDuel>();

    public TabService(MeowDuels plugin) {
        this.plugin = plugin;
        this.external = plugin.getConfig().getBoolean("external-tab", false);
    }

    public boolean isExternal() {
        return this.external;
    }

    public void attach(ActiveDuel duel) {
        Player p1 = Bukkit.getPlayer((UUID)duel.getPlayer1());
        Player p2 = Bukkit.getPlayer((UUID)duel.getPlayer2());
        if (p1 == null || p2 == null) {
            return;
        }
        this.memberDuel.put(duel.getPlayer1(), duel);
        this.memberDuel.put(duel.getPlayer2(), duel);
        this.applyTeams(p1, duel);
        this.applyTeams(p2, duel);
        this.sendHeaderFooter(p1);
        this.sendHeaderFooter(p2);
        this.reconcileAll();
    }

    public void attachSpectator(Player spectator, ActiveDuel duel) {
        this.memberDuel.put(spectator.getUniqueId(), duel);
        this.applyTeams(spectator, duel);
        this.sendHeaderFooter(spectator);
        this.reconcileAll();
    }

    public void detach(UUID id) {
        this.leave(id);
    }

    public void detachSpectator(UUID id) {
        this.leave(id);
    }

    private void leave(UUID id) {
        if (this.memberDuel.remove(id) == null) {
            return;
        }
        Player viewer = Bukkit.getPlayer((UUID)id);
        if (viewer != null) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getUniqueId().equals(id)) continue;
                viewer.showPlayer((Plugin)this.plugin, online);
                online.showPlayer((Plugin)this.plugin, viewer);
            }
        }
        this.reconcileAll();
    }

    public void onPlayerJoin(Player joiner) {
        this.sendHeaderFooter(joiner);
        this.reconcileAll();
    }

    private void reconcileAll() {
        HashSet<ActiveDuel> duels = new HashSet<ActiveDuel>(this.memberDuel.values());
        for (ActiveDuel duel : duels) {
            this.reconcile(duel);
        }
    }

    private void reconcile(ActiveDuel duel) {
        HashSet<UUID> bubble = new HashSet<UUID>();
        for (Map.Entry<UUID, ActiveDuel> e : this.memberDuel.entrySet()) {
            if (e.getValue() != duel) continue;
            bubble.add(e.getKey());
        }
        // Each fighter/spectator sees only the others in their own match.
        for (UUID mid : bubble) {
            Player member = Bukkit.getPlayer((UUID)mid);
            if (member == null) continue;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getUniqueId().equals(mid)) continue;
                if (bubble.contains(online.getUniqueId())) {
                    member.showPlayer((Plugin)this.plugin, online);
                    continue;
                }
                member.hidePlayer((Plugin)this.plugin, online);
            }
        }
        // Deliberately one-directional. Fighters don't see anyone outside their
        // match, so their tab list is just the fight - but the rest of the server
        // still sees THEM, because they are online and should look it. Hiding
        // both ways made duellists vanish from everyone's tab list mid-match.
    }

    private void applyTeams(Player viewer, ActiveDuel duel) {
        if (this.external) {
            return;
        }
        Scoreboard sb = viewer.getScoreboard();
        if (sb == null) {
            return;
        }
        Team aqua = this.team(sb, AQUA_TEAM, ChatColor.AQUA, "\u00a7b\u26a1 ");
        Team red = this.team(sb, RED_TEAM, ChatColor.RED, "\u00a7c\u26a1 ");
        for (UUID id : new UUID[]{duel.getPlayer1(), duel.getPlayer2()}) {
            Player p = Bukkit.getPlayer((UUID)id);
            if (p == null) continue;
            (duel.isAqua(id) ? aqua : red).addEntry(p.getName());
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
        if (this.external) {
            return;
        }
        int online = Bukkit.getOnlinePlayers().size();
        int fighting = this.plugin.getDuelManager().playersInDuels();
        String header = this.plugin.getTabTitle() + "\n<gray>Global Players: <white>" + online + "\n \n<gray>Online: <green>" + online + " <dark_gray>\u2022 <gray>Fighting: <red>" + fighting;
        String footer = " \n" + this.plugin.getTabDiscord() + "\n" + this.plugin.getTabStore();
        viewer.sendPlayerListHeaderAndFooter(TabService.mm(header), TabService.mm(footer));
    }

    public void tick() {
        if (this.external) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            this.sendHeaderFooter(p);
        }
    }

    private static Component mm(String miniMessage) {
        return MiniMessage.miniMessage().deserialize((Object)miniMessage);
    }
}


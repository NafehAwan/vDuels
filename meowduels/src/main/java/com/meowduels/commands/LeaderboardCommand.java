/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package com.meowduels.commands;

import com.meowduels.MeowDuels;
import com.meowduels.managers.StatsManager;
import com.meowduels.util.Colors;
import com.meowduels.util.Ranks;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaderboardCommand
implements CommandExecutor {
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 25;
    private final MeowDuels plugin;

    public LeaderboardCommand(MeowDuels plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        StatsManager stats = this.plugin.getStatsManager();
        int size = 10;
        if (args.length > 0) {
            try {
                size = Math.max(1, Math.min(25, Integer.parseInt(args[0])));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        List<Map.Entry<UUID, Integer>> top = stats.topElo(size);
        sender.sendMessage("");
        sender.sendMessage(LeaderboardCommand.mmc("  <gradient:#FF2E55:#FF7FC4>\u1d07\u029f\u1d0f \u029f\u1d07\u1d00\u1d05\u1d07\u0280\u0299\u1d0f\u1d00\u0280\u1d05</gradient>"));
        sender.sendMessage("");
        if (top.isEmpty()) {
            sender.sendMessage(LeaderboardCommand.mmc("  <#8E959D>\u0274\u1d0f \u0280\u1d00\u0274\u1d0b\u1d07\u1d05 \u1d18\u029f\u1d00\u028f\u1d07\u0280\ua731 \u028f\u1d07\u1d1b <dark_gray>- <#8E959D>finish your placement matches to appear here."));
            sender.sendMessage("");
            return true;
        }
        for (int i = 0; i < top.size(); ++i) {
            Map.Entry<UUID, Integer> entry = top.get(i);
            sender.sendMessage(LeaderboardCommand.mmc("  " + this.position(i + 1) + " <#E6E8EB>" + this.nameOf(entry.getKey()) + " <dark_gray>\u00b7 " + Ranks.mini(stats, entry.getKey()) + " <dark_gray>\u00b7 <#FF8A93>" + String.valueOf(entry.getValue())));
        }
        this.appendViewer(sender, stats, top);
        sender.sendMessage("");
        return true;
    }

    private void appendViewer(CommandSender sender, StatsManager stats, List<Map.Entry<UUID, Integer>> shown) {
        if (!(sender instanceof Player)) {
            return;
        }
        UUID id = ((Player)sender).getUniqueId();
        for (Map.Entry<UUID, Integer> e : shown) {
            if (!e.getKey().equals(id)) continue;
            return;
        }
        sender.sendMessage("");
        if (!stats.isPlaced(id)) {
            int left = stats.placementsLeft(id);
            sender.sendMessage(LeaderboardCommand.mmc("  <#8E959D>\u028f\u1d0f\u1d1c <dark_gray>\u00b7 <#E6E8EB>" + left + " <#8E959D>placement " + (left == 1 ? "match" : "matches") + " left"));
            return;
        }
        int rank = this.rankOf(stats, id);
        sender.sendMessage(LeaderboardCommand.mmc("  <#8E959D>\u028f\u1d0f\u1d1c <dark_gray>\u00b7 <#E6E8EB>#" + rank + " <dark_gray>\u00b7 " + Ranks.mini(stats, id) + " <dark_gray>\u00b7 <#FF8A93>" + stats.getElo(id)));
    }

    private int rankOf(StatsManager stats, UUID id) {
        List<Map.Entry<UUID, Integer>> all = stats.topElo(Integer.MAX_VALUE);
        for (int i = 0; i < all.size(); ++i) {
            if (!all.get(i).getKey().equals(id)) continue;
            return i + 1;
        }
        return all.size() + 1;
    }

    private String position(int place) {
        switch (place) {
            case 1: {
                return "<#FFD65C>#1";
            }
            case 2: {
                return "<#D8DEE6>#2";
            }
            case 3: {
                return "<#E0954A>#3";
            }
        }
        return "<#6B7079>#" + place;
    }

    private String nameOf(UUID id) {
        Player online = Bukkit.getPlayer((UUID)id);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer off = Bukkit.getOfflinePlayer((UUID)id);
        return off.getName() == null ? "Unknown" : off.getName();
    }

    private static String mmc(String miniMessage) {
        return Colors.toSection(miniMessage);
    }
}


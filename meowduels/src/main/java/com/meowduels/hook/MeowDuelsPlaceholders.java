/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.PlaceholderAPI
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 */
package com.meowduels.hook;

import com.meowduels.MeowDuels;
import com.meowduels.model.ActiveDuel;
import com.meowduels.model.Kit;
import com.meowduels.util.Colors;
import com.meowduels.util.Ranks;
import java.util.Locale;
import java.util.UUID;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class MeowDuelsPlaceholders
extends PlaceholderExpansion {
    private final MeowDuels plugin;

    public MeowDuelsPlaceholders(MeowDuels plugin) {
        this.plugin = plugin;
    }

    public String getIdentifier() {
        return "meowduels";
    }

    public String getAuthor() {
        return "nafehawan";
    }

    public String getVersion() {
        return "1.0.0";
    }

    public boolean persist() {
        return true;
    }

    public String onRequest(OfflinePlayer player, String params) {
        UUID watched;
        String key;
        if (params == null) {
            return "";
        }
        switch (key = params.toLowerCase()) {
            case "players_in_duels": {
                return String.valueOf(this.plugin.getDuelManager().playersInDuels());
            }
            case "server_ip": {
                return this.plugin.getScoreboardIp();
            }
            case "tab_title": {
                return this.plugin.getTabTitle();
            }
            case "tab_discord": {
                return this.plugin.getTabDiscord();
            }
            case "tab_store": {
                return this.plugin.getTabStore();
            }
        }
        if (player == null) {
            return "";
        }
        UUID id = player.getUniqueId();
        boolean spectating = this.plugin.getSpectateManager().isSpectating(id);
        UUID subject = id;
        if (spectating && (watched = this.plugin.getSpectateManager().getWatchedTarget(id)) != null) {
            subject = watched;
        }
        ActiveDuel duel = this.plugin.getDuelManager().getDuel(subject);
        switch (key) {
            case "elo": {
                return String.valueOf(this.plugin.getStatsManager().getElo(id));
            }
            case "rank": {
                return Ranks.tab(this.plugin.getStatsManager(), id);
            }
            case "rank_mini": {
                return Ranks.mini(this.plugin.getStatsManager(), id);
            }
            case "tabprefix": {
                ActiveDuel own = this.plugin.getDuelManager().getDuel(id);
                if (own != null) {
                    return own.isAqua(id) ? "\u00a7b\u26a1 \u00a7b" : "\u00a7c\u26a1 \u00a7c";
                }
                return this.lpPrefix(player);
            }
            case "lpprefix": {
                return this.lpPrefix(player);
            }
            case "tagorrank": {
                if (this.plugin.getDuelManager().isInDuel(id)) {
                    return "";
                }
                String suffix = this.papi(player, "%luckperms_suffix%");
                if (!suffix.trim().isEmpty()) {
                    return Colors.toSection(suffix);
                }
                return Ranks.tab(this.plugin.getStatsManager(), id);
            }
            case "tag": {
                if (this.plugin.getDuelManager().isInDuel(id)) {
                    return "";
                }
                return Colors.toSection(this.papi(player, "%luckperms_suffix%"));
            }
            case "placements_left": {
                return String.valueOf(this.plugin.getStatsManager().placementsLeft(id));
            }
            case "rank_name": {
                return "[" + Ranks.tier(this.plugin.getStatsManager(), id)[0] + "]";
            }
            case "health": {
                Player online = Bukkit.getPlayer((UUID)id);
                if (online == null) {
                    return "";
                }
                double hp = online.getHealth();
                String n = hp < 5.0 ? String.format(Locale.US, "%.2f", hp) : String.valueOf((int)Math.round(hp));
                return "\u00a7c" + n + " \u2764";
            }
            case "in_fight": {
                return this.plugin.getDuelManager().isInDuel(id) ? "true" : "false";
            }
            case "role": {
                if (spectating) {
                    return "SPECTATOR";
                }
                return this.plugin.getDuelManager().isInDuel(id) ? "PLAYER" : "NONE";
            }
            case "status": {
                if (spectating) {
                    return "Spectating";
                }
                return this.plugin.getDuelManager().isInDuel(id) ? "Fighting" : "Idle";
            }
        }
        if (duel == null) {
            return "";
        }
        UUID self = spectating ? subject : id;
        UUID opponent = duel.getOpponent(self);
        boolean aqua = duel.isAqua(self);
        switch (key) {
            case "team": {
                return aqua ? "AQUA" : "RED";
            }
            case "team_color": {
                return aqua ? "&b" : "&c";
            }
            case "team_prefix": {
                return aqua ? "\u00a7b\u26a1 \u00a7b" : "\u00a7c\u26a1 \u00a7c";
            }
            case "state": {
                return duel.getState().name();
            }
            case "arena": {
                return duel.getArena() == null ? "" : duel.getArena().getName();
            }
            case "kit": {
                return this.kitLabel(duel.getKit());
            }
            case "round": {
                return String.valueOf(duel.getCurrentRound());
            }
            case "rounds_to_win": {
                return String.valueOf(duel.getRoundsToWin());
            }
            case "score": {
                return String.valueOf(duel.getScoreFor(self));
            }
            case "opponent_score": {
                return String.valueOf(duel.getScoreAgainst(self));
            }
            case "opponent": {
                return this.nameOf(opponent);
            }
            case "player1": {
                return this.nameOf(duel.getPlayer1());
            }
            case "player2": {
                return this.nameOf(duel.getPlayer2());
            }
            case "time": {
                long seconds = Math.max(0L, (System.currentTimeMillis() - duel.getStartedAt()) / 1000L);
                return String.format("%02d:%02d", seconds / 60L, seconds % 60L);
            }
        }
        return "";
    }

    private String lpPrefix(OfflinePlayer player) {
        return Colors.toSection(this.papi(player, "%luckperms_prefix%"));
    }

    private String papi(OfflinePlayer player, String placeholder) {
        try {
            String value = PlaceholderAPI.setPlaceholders((OfflinePlayer)player, (String)placeholder);
            if (value == null || value.equals(placeholder)) {
                return "";
            }
            return value;
        }
        catch (Throwable ignored) {
            return "";
        }
    }

    private String nameOf(UUID id) {
        if (id == null) {
            return "";
        }
        Player online = Bukkit.getPlayer((UUID)id);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer off = Bukkit.getOfflinePlayer((UUID)id);
        return off.getName() == null ? "" : off.getName();
    }

    private String kitLabel(String kitId) {
        Kit kit = this.plugin.getKitManager().get(kitId);
        if (kit != null && kit.getDisplayName() != null && !kit.getDisplayName().isEmpty()) {
            return kit.getDisplayName().replaceAll("<[^>]*>", "");
        }
        return kitId;
    }
}


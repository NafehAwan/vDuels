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
import com.meowduels.util.Health;
import com.meowduels.util.Ranks;
import com.meowduels.model.Party;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class MeowDuelsPlaceholders
extends PlaceholderExpansion
implements Relational {
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
        // Tab header/footer lines. These come first because they are asked for
        // by line number - tab_header_3 and so on - and would otherwise fall
        // through the whole switch below on every refresh.
        if (params.regionMatches(true, 0, "tab_header_", 0, 11)) {
            return this.tabLine(player, "header", params.substring(11));
        }
        if (params.regionMatches(true, 0, "tab_footer_", 0, 11)) {
            return this.tabLine(player, "footer", params.substring(11));
        }
        switch (key = params.toLowerCase()) {
            case "players_in_duels": {
                return String.valueOf(this.plugin.getDuelManager().playersInDuels());
            }
            case "duels": {
                // Number of MATCHES, where players_in_duels counts people (two
                // per match) - "In Duels: 6" with six online is six players in
                // three fights, not everyone stuck in a duel.
                return String.valueOf(this.plugin.getDuelManager().duelsInProgress());
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
                // The party fight marker used to be prepended here as well.
                // It is a suffix now, so both places agree and a party member
                // never shows the icon on one side and their rank on the other.
                //
                // Viewer-independent answer: their real rank, with its real
                // colours. Replacing it with the team bolt here was showing the
                // whole server a fighter in aqua/red instead of their rank -
                // the bolt belongs only in the fighters' own tab list, which is
                // what %rel_meowduels_tabprefix% handles.
                return this.lpPrefix(player);
            }
            case "tagprefix": {
                // Viewer-independent fallback, for a TAB build that does not
                // resolve relational placeholders in tagprefix.
                if (this.plugin.getEventManager().isPlaying(id)) {
                    return "\u00a7e\u26a1 \u00a7e";
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
            case "tabsuffix": {
                return this.suffixFor(player, id, true);
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
                return "\u00a7c" + Health.text(online.getHealth()) + " \u2764";
            }
            case "in_fight": {
                return this.plugin.getDuelManager().isInDuel(id) ? "true" : "false";
            }
            // Party placeholders sit above the duel block on purpose: everything
            // below it returns "" for anyone not in a duel, and a party member
            // usually isn't in one.
            case "in_party": {
                return this.plugin.getPartyManager().inParty(id) ? "true" : "false";
            }
            case "in_party_match": {
                return this.plugin.getPartyManager().inPartyMatch(id) ? "true" : "false";
            }
            case "party_leader": {
                Party p = this.plugin.getPartyManager().partyOf(id);
                return p == null ? "" : this.nameOf(p.getLeader());
            }
            case "party_size": {
                Party p = this.plugin.getPartyManager().partyOf(id);
                return p == null ? "0" : String.valueOf(p.size());
            }
            case "party_kit": {
                Party p = this.plugin.getPartyManager().partyOf(id);
                return p == null || p.getKit() == null ? "" : this.kitLabel(p.getKit());
            }
            case "party_status": {
                Party p = this.plugin.getPartyManager().partyOf(id);
                return p == null ? "" : (p.isFighting() ? "Fighting" : "Waiting");
            }
            case "party_alive": {
                Party p = this.plugin.getPartyManager().partyOf(id);
                return p == null ? "0" : String.valueOf(p.getAlive().size());
            }
            case "party_sort": {
                // TAB sorts the whole list by this, one order for everyone, so
                // it cannot be "my party first" - that would have to be
                // relational and sorting is not. What it CAN do is keep each
                // party's members next to each other and above everyone else,
                // which is what makes a party read as a block in the list.
                Party p = this.plugin.getPartyManager().partyOf(id);
                return p == null ? "ZZZZ" : "A" + this.nameOf(p.getLeader()).toLowerCase(Locale.ROOT);
            }
            case "party_members": {
                Party p = this.plugin.getPartyManager().partyOf(id);
                if (p == null) {
                    return "";
                }
                // Capped. A full party's names on one footer line wraps into
                // an unreadable block, and the roster is a glance, not a census
                // - the tab list itself is the full list.
                StringBuilder out = new StringBuilder();
                int shown = 0;
                for (UUID member : p.getMembers()) {
                    if (shown == 5) {
                        out.append("\u00a78 +").append(p.size() - shown);
                        break;
                    }
                    if (shown > 0) {
                        out.append("\u00a78, ");
                    }
                    out.append(this.memberTag(p, member));
                    ++shown;
                }
                return out.toString();
            }
            case "party_role": {
                Party p = this.plugin.getPartyManager().partyOf(id);
                return p == null ? "" : (p.isLeader(id) ? "Leader" : "Member");
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

    /**
     * One line of the tab header or footer, chosen for who is looking.
     *
     * <p>The layouts and their tokens live in TabService, which owns the tab
     * list and uses the same three layouts for the built-in header/footer. This
     * is only the doorway PlaceholderAPI comes in through, so it is where the
     * conversion to section codes belongs: a value handed through PlaceholderAPI
     * is not parsed again by whatever receives it, so MiniMessage would reach
     * TAB as literal text.
     */
    private String tabLine(OfflinePlayer player, String part, String indexText) {
        int index;
        try {
            index = Integer.parseInt(indexText.trim());
        }
        catch (NumberFormatException e) {
            return "";
        }
        UUID id = player == null ? null : player.getUniqueId();
        return Colors.toSection(this.plugin.getTabService().layoutLine(id, part, index));
    }

    /**
     * Viewer-aware placeholders: {@code %rel_meowduels_...%}.
     *
     * <p>PlaceholderAPI hands us both the player looking at the tab list and the
     * player being described, which a normal expansion never sees - it only ever
     * knows the target. That is the whole reason this exists: the duel marker is
     * for everyone OUTSIDE the match. The two fighters already know they are
     * fighting, and their tab list holds nothing but each other, so a dagger on
     * both rows is just noise.
     */
    public String onPlaceholderRequest(Player viewer, Player target, String params) {
        if (params == null || target == null) {
            return "";
        }
        String key = params.toLowerCase();
        UUID id = target.getUniqueId();
        // Is the person LOOKING part of a match - fighting it or watching it?
        // Everything in their tab list is that match, so it gets the fight
        // styling. Everyone else is "global" and sees normal ranks.
        boolean watchingAFight = viewer != null
                && (this.plugin.getDuelManager().isInDuel(viewer.getUniqueId())
                    || this.plugin.getSpectateManager().isSpectating(viewer.getUniqueId()));
        if ("tabprefix".equals(key)) {
            ActiveDuel fight = this.plugin.getDuelManager().getDuel(id);
            if (watchingAFight && fight != null) {
                return fight.isAqua(id) ? "\u00a7b\u26a1 \u00a7b" : "\u00a7c\u26a1 \u00a7c";
            }
            String party = this.partyPrefix(viewer, target);
            if (party != null) {
                return party;
            }
            // Outside the party, a party member keeps their ordinary rank here.
            // The "in a fight" marker is a SUFFIX, exactly like the duel one -
            // after the name, a space, then the icon - so the two read the same
            // way in the list instead of one leading and one trailing.
            return this.lpPrefix(target);
        }
        if ("tagprefix".equals(key)) {
            // The name ABOVE THE HEAD, per viewer.
            //
            // This exists so TAB never has to switch its scoreboard-teams off
            // for a fighter. It used to, via disable-condition, so that
            // MeowDuels could draw the duel nametag itself - and TAB implements
            // tab-list SORTING with those same teams, so a fighter lost their
            // sort team and the client floated them to the top of the list.
            // Drawing the nametag through TAB instead costs nothing and keeps
            // one owner for teams.
            ActiveDuel fight = this.plugin.getDuelManager().getDuel(id);
            if (watchingAFight && fight != null) {
                return fight.isAqua(id) ? "\u00a7b\u26a1 \u00a7b" : "\u00a7c\u26a1 \u00a7c";
            }
            if (this.plugin.getEventManager().isPlaying(id)) {
                return "\u00a7e\u26a1 \u00a7e";
            }
            String partyTag = this.partyPrefix(viewer, target);
            if (partyTag != null) {
                return partyTag;
            }
            return this.lpPrefix(target);
        }
        if ("tabsuffix".equals(key)) {
            // The marker is for people outside the match; inside it, everyone
            // already knows who is fighting. Same inside a party - the prefix
            // already says what everyone there is doing.
            boolean sameParty = viewer != null
                    && this.plugin.getPartyManager().partyOf(viewer.getUniqueId()) != null
                    && this.plugin.getPartyManager().partyOf(viewer.getUniqueId()).has(id);
            return this.suffixFor(target, id, !watchingAFight && !sameParty);
        }
        // Anything else falls through to the ordinary, viewer-independent form.
        return this.onRequest((OfflinePlayer) target, params);
    }

    /** One name in the party roster line, marked by what they are. */
    private String memberTag(Party party, UUID id) {
        String name = this.nameOf(id);
        if (party.isFighting()) {
            if (party.getAlive().contains(id)) {
                return "\u00a7a" + name;
            }
            if (party.getWatching().contains(id)) {
                return "\u00a78\u2620 " + name;
            }
        }
        return party.isLeader(id) ? "\u00a76\u2605 " + name : "\u00a7f" + name;
    }

    /**
     * How a party member appears in their own party's tab list.
     *
     * <p>Only inside the bubble. Everywhere else a party member is an ordinary
     * player with their ordinary rank - the same rule the duel bolt follows, and
     * for the same reason: the marker is information for the people it concerns,
     * not a badge worn at the whole server.
     *
     * <p>What it shows depends on what the party is doing. Idle, it is who runs
     * it; mid-match, it is who is still in it, because that is the only thing
     * anyone is looking at the list for.
     *
     * @return null when the two players are not in the same party
     */
    private String partyPrefix(Player viewer, Player target) {
        if (viewer == null) {
            return null;
        }
        Party party = this.plugin.getPartyManager().partyOf(viewer.getUniqueId());
        if (party == null || !party.has(target.getUniqueId())) {
            return null;
        }
        UUID id = target.getUniqueId();
        if (party.isFighting()) {
            // Split colours by side rather than by alive/out: in a team match
            // which side someone is on is the thing you need at a glance, and
            // the aqua/red pair is the one the whole plugin already uses.
            if (party.isTeamMode() && party.teamOf(id) != null) {
                boolean out = party.getWatching().contains(id);
                boolean aqua = party.teamOf(id) == Party.Team.AQUA;
                if (out) {
                    return "\u00a78\u2620 \u00a78";
                }
                return aqua ? "\u00a7b\u26a1 \u00a7b" : "\u00a7c\u26a1 \u00a7c";
            }
            if (party.getAlive().contains(id)) {
                return "\u00a7a\u26a1 \u00a7a";
            }
            if (party.getWatching().contains(id)) {
                return "\u00a78\u2620 \u00a78";
            }
        }
        return party.isLeader(id) ? "\u00a76\u2605 \u00a76" : "\u00a7d\u25c6 \u00a7d";
    }

    /**
     * What goes after a player's name in the tab list: the duel marker while
     * they fight, otherwise their MeowTags tag - one field deciding between the
     * two, so they can never collide.
     *
     * <p>Always opens with a reset so neither the rank's colour nor the tag's
     * bleeds into it, and always starts with a space so it never runs into the
     * name.
     *
     * @param showMarker false to leave a fighter's row bare (the viewer is in a
     *                   duel themselves, so the marker tells them nothing)
     */
    private String suffixFor(OfflinePlayer player, UUID id, boolean showMarker) {
        if (this.plugin.getDuelManager().isInDuel(id)) {
            return this.marker(this.plugin.getDuelMarker(), showMarker);
        }
        // A party in a match gets the same treatment in the same place - only
        // while it is actually fighting, so an idle party in the lobby is not
        // wearing a fight badge.
        if (this.plugin.getPartyManager().inPartyMatch(id)) {
            return this.marker(this.plugin.getPartyMarker(), showMarker);
        }
        String tag = Colors.toSection(this.papi(player, "%luckperms_suffix%"));
        return tag.trim().isEmpty() ? "" : " \u00a7r" + tag;
    }

    /** One shape for every after-the-name marker: space, reset, dark grey, icon. */
    private String marker(String icon, boolean show) {
        if (!show || icon == null || icon.isEmpty()) {
            return "";
        }
        return " \u00a7r\u00a78" + icon;
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


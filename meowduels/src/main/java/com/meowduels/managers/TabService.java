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
import org.bukkit.OfflinePlayer;
import java.util.List;
import com.meowduels.util.Ranks;
import com.meowduels.model.Kit;
import com.meowduels.model.Party;
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
    /**
     * Who shares a tab list with whom.
     *
     * <p>The value is whatever the players are grouped BY - an ActiveDuel or a
     * Party - and is only ever compared by identity. Generalising it is what
     * lets parties reuse the bubble: "you see the people in your own group and
     * nobody else" is one rule, and writing it twice would be two places for it
     * to drift.
     */
    private final Map<UUID, Object> memberGroup = new HashMap<UUID, Object>();

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
        this.memberGroup.put(duel.getPlayer1(), duel);
        this.memberGroup.put(duel.getPlayer2(), duel);
        this.applyTeams(p1, duel);
        this.applyTeams(p2, duel);
        this.sendHeaderFooter(p1);
        this.sendHeaderFooter(p2);
        this.reconcileAll();
    }

    public void attachSpectator(Player spectator, ActiveDuel duel) {
        this.memberGroup.put(spectator.getUniqueId(), duel);
        this.applyTeams(spectator, duel);
        this.sendHeaderFooter(spectator);
        this.reconcileAll();
    }

    /**
     * Gives a party its own tab list.
     *
     * <p>Exactly the duel arrangement, and one-directional for exactly the same
     * reason: the party sees the party, and the rest of the server still sees
     * them, with their real rank, because they are online and should look it.
     * Hiding both ways is what makes people vanish from everyone's list.
     *
     * <p>Rebuilt wholesale on every membership change rather than patched. A
     * party is small, and the patched version is how someone ends up hidden
     * from a list they are no longer in, with nothing to tell them why.
     */
    public void attachParty(Party party) {
        if (party == null) {
            return;
        }
        // One switch for the whole question of whether a party gets its own tab
        // list or just sits in the shared one. Turning it off releases anyone
        // already bubbled rather than leaving them stuck in a list nothing
        // maintains any more.
        if (!this.plugin.getConfig().getBoolean("party.separate-tab", true)) {
            this.detachParty(party);
            return;
        }
        for (Map.Entry<UUID, Object> e : new HashMap<UUID, Object>(this.memberGroup).entrySet()) {
            if (e.getValue() == party) {
                this.release(e.getKey());
            }
        }
        // From the first member, not the second: the party list is meant to be
        // the party from the moment it exists, so a leader on their own sees a
        // list of one rather than the whole server until somebody joins.
        for (UUID id : party.getMembers()) {
            if (Bukkit.getPlayer((UUID)id) != null) {
                this.memberGroup.put(id, party);
            }
        }
        this.reconcileAll();
    }

    /** Takes a whole party out of its bubble - disband, or the last member
     *  leaving a pair. */
    public void detachParty(Party party) {
        if (party == null) {
            return;
        }
        for (Map.Entry<UUID, Object> e : new HashMap<UUID, Object>(this.memberGroup).entrySet()) {
            if (e.getValue() == party) {
                this.release(e.getKey());
            }
        }
        this.reconcileAll();
    }

    public void detach(UUID id) {
        this.leave(id);
    }

    public void detachSpectator(UUID id) {
        this.leave(id);
    }

    private void leave(UUID id) {
        this.release(id);
        this.reconcileAll();
    }

    /** Out of whatever bubble they were in, and able to see everyone again.
     *  Split out of {@link #leave} so a rebuild can release several players
     *  before reconciling once, instead of reconciling per player. */
    private void release(UUID id) {
        Object group = this.memberGroup.remove(id);
        if (group == null) {
            return;
        }
        Player viewer = Bukkit.getPlayer((UUID)id);
        if (viewer == null) {
            return;
        }
        // Undo whichever kind of hiding this group applied. Calling the wrong
        // one leaves them released from a bubble they are still missing people
        // from - unlistPlayer is not undone by showPlayer.
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(id)) continue;
            this.setVisible(viewer, online, true, true);
            // Undo the old world-hiding too, for anyone carrying it from a
            // build where the bubble still used hidePlayer. Harmless when there
            // is nothing to undo, and the alternative is a player stuck
            // invisible until they relog.
            this.setVisible(viewer, online, true, false);
        }
    }

    public void onPlayerJoin(Player joiner) {
        this.sendHeaderFooter(joiner);
        this.reconcileAll();
    }

    private void reconcileAll() {
        HashSet<Object> groups = new HashSet<Object>(this.memberGroup.values());
        for (Object group : groups) {
            this.reconcile(group);
        }
    }

    private void reconcile(Object group) {
        HashSet<UUID> bubble = new HashSet<UUID>();
        for (Map.Entry<UUID, Object> e : this.memberGroup.entrySet()) {
            if (e.getValue() != group) continue;
            bubble.add(e.getKey());
        }
        // Tab-list only unless the fallback is switched on - see hidesWorld.
        boolean tabOnly = !this.hidesWorld();
        for (UUID mid : bubble) {
            Player member = Bukkit.getPlayer((UUID)mid);
            if (member == null) continue;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getUniqueId().equals(mid)) continue;
                this.setVisible(member, online, bubble.contains(online.getUniqueId()), tabOnly);
            }
        }
        // Deliberately one-directional. Fighters don't see anyone outside their
        // match, so their tab list is just the fight - but the rest of the server
        // still sees THEM, because they are online and should look it. Hiding
        // both ways made duellists vanish from everyone's tab list mid-match.
    }

    /**
     * Whether {@code member} can see {@code other}, and in what sense.
     *
     * <p>Two different kinds of hiding, because the two bubbles want different
     * things. hidePlayer takes the player out of the WORLD as well as the tab
     * list - right for a duel, where the fighters are alone in an arena and
     * anyone else rendering there would be a distraction that isn't really
     * there. Wrong for a party, which stands in the same lobby as everybody
     * else: it made the lobby look empty.
     *
     * <p>unlistPlayer is Paper's tab-list-only version, which is what a party
     * actually asked for. It throws if the player is not visible to begin with,
     * hence the guard - the two systems can disagree for a tick when a duel and
     * a party release the same player in the same moment.
     */
    private void setVisible(Player member, Player other, boolean visible, boolean tabOnly) {
        try {
            if (tabOnly) {
                if (visible) {
                    member.listPlayer(other);
                } else {
                    member.unlistPlayer(other);
                }
                return;
            }
            if (visible) {
                member.showPlayer((Plugin)this.plugin, other);
            } else {
                member.hidePlayer((Plugin)this.plugin, other);
            }
        }
        catch (Throwable t) {
            // listPlayer refuses a player this one cannot see. Nothing to do
            // about it here, and it corrects itself on the next reconcile.
        }
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

    /**
     * One line of the tab header or footer, chosen for who is looking.
     *
     * <p>Returned as raw MiniMessage with its tokens filled. The two callers
     * need different final forms - PlaceholderAPI wants section codes, the
     * built-in header/footer wants a Component - so neither conversion is done
     * here.
     *
     * <p>Three layouts in one place, config.yml under `tab:`. Whether TAB draws
     * the tab list or MeowDuels does, the wording comes from the same lines, so
     * the two cannot drift apart.
     *
     * <p>An out-of-range line is empty rather than missing, so shortening a
     * layout doesn't leave the placeholder text itself showing in the tab list.
     */
    public String layoutLine(UUID id, String part, int index) {
        String context = this.layoutContext(id);
        List<String> lines = this.plugin.getConfig().getStringList("tab." + context + "." + part);
        if (lines == null || index < 1 || index > lines.size()) {
            return "";
        }
        return this.fillTokens(lines.get(index - 1), id);
    }

    /** Which of the three tab layouts applies to this viewer. */
    private String layoutContext(UUID id) {
        if (id == null) {
            return "global";
        }
        if (this.plugin.getDuelManager().isInDuel(id)
                || this.plugin.getSpectateManager().isSpectating(id)) {
            return "duel";
        }
        if (this.plugin.getPartyManager().inParty(id)) {
            return "party";
        }
        return "global";
    }

    /**
     * Substitutes the {tokens} a tab layout may use.
     *
     * <p>Every token is filled for every context, so a line moved between
     * layouts keeps working. Tokens with nothing behind them come out empty
     * rather than as their own name.
     */
    private String fillTokens(String raw, UUID id) {
        if (raw == null || raw.indexOf(123) < 0) {
            return raw == null ? "" : raw;
        }
        String out = raw;
        out = out.replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        out = out.replace("{in_duels}", String.valueOf(this.plugin.getDuelManager().duelsInProgress()));
        out = out.replace("{server_ip}", this.plugin.getScoreboardIp());
        out = out.replace("{server_name}", this.plugin.getServerName());
        out = out.replace("{discord}", this.plugin.getTabDiscord());
        out = out.replace("{store}", this.plugin.getTabStore());
        if (id == null) {
            return out;
        }
        Player self = Bukkit.getPlayer((UUID)id);
        out = out.replace("{player}", self == null ? "" : self.getName());
        out = out.replace("{ping}", self == null ? "0" : String.valueOf(self.getPing()));
        out = out.replace("{rank}", Ranks.tab(this.plugin.getStatsManager(), id));
        out = out.replace("{elo}", String.valueOf(this.plugin.getStatsManager().getElo(id)));
        out = this.fillDuelTokens(out, id);
        out = this.fillPartyTokens(out, id);
        return out;
    }

    private String fillDuelTokens(String out, UUID id) {
        UUID subject = id;
        if (this.plugin.getSpectateManager().isSpectating(id)) {
            UUID watched = this.plugin.getSpectateManager().getWatchedTarget(id);
            if (watched != null) {
                subject = watched;
            }
        }
        ActiveDuel duel = this.plugin.getDuelManager().getDuel(subject);
        if (duel == null) {
            return out.replace("{opponent}", "").replace("{score}", "")
                      .replace("{opponent_score}", "").replace("{kit}", "")
                      .replace("{arena}", "").replace("{round}", "")
                      .replace("{rounds_to_win}", "").replace("{time}", "")
                      .replace("{team_color}", "<gray>");
        }
        long seconds = Math.max(0L, (System.currentTimeMillis() - duel.getStartedAt()) / 1000L);
        return out.replace("{opponent}", this.nameOf(duel.getOpponent(subject)))
                  .replace("{score}", String.valueOf(duel.getScoreFor(subject)))
                  .replace("{opponent_score}", String.valueOf(duel.getScoreAgainst(subject)))
                  .replace("{kit}", this.kitLabel(duel.getKit()))
                  .replace("{arena}", duel.getArena() == null ? "" : duel.getArena().getName())
                  .replace("{round}", String.valueOf(duel.getCurrentRound()))
                  .replace("{rounds_to_win}", String.valueOf(duel.getRoundsToWin()))
                  .replace("{time}", String.format("%02d:%02d", seconds / 60L, seconds % 60L))
                  .replace("{team_color}", duel.isAqua(subject) ? "<aqua>" : "<red>");
    }

    private String fillPartyTokens(String out, UUID id) {
        Party party = this.plugin.getPartyManager().partyOf(id);
        if (party == null) {
            return out.replace("{party_leader}", "").replace("{party_size}", "0")
                      .replace("{party_kit}", "").replace("{party_status}", "")
                      .replace("{party_alive}", "0").replace("{party_role}", "");
        }
        return out.replace("{party_leader}", this.nameOf(party.getLeader()))
                  .replace("{party_size}", String.valueOf(party.size()))
                  .replace("{party_kit}", party.getKit() == null ? "" : this.kitLabel(party.getKit()))
                  .replace("{party_status}", party.isFighting() ? "Fighting" : "Waiting")
                  .replace("{party_alive}", String.valueOf(party.getAlive().size()))
                  .replace("{party_role}", party.isLeader(id) ? "Leader" : "Member");
    }


    private String nameOf(UUID id) {
        if (id == null) {
            return "";
        }
        Player online = Bukkit.getPlayer((UUID)id);
        if (online != null) {
            return online.getName();
        }
        String name = Bukkit.getOfflinePlayer((UUID)id).getName();
        return name == null ? "" : name;
    }

    /** A kit's display name with its formatting tags stripped: the tab layout
     *  supplies the colour, and a kit called "<red>Sumo" would fight it. */
    private String kitLabel(String kitId) {
        if (kitId == null || kitId.isEmpty()) {
            return "";
        }
        Kit kit = this.plugin.getKitManager().get(kitId);
        if (kit != null && kit.getDisplayName() != null && !kit.getDisplayName().isEmpty()) {
            return kit.getDisplayName().replaceAll("<[^>]*>", "");
        }
        return kitId;
    }

    /**
     * The built-in header/footer, for a server not running TAB.
     *
     * <p>Reads the same three layouts TAB's placeholders read, so turning
     * external-tab off changes who draws the tab list and nothing about what it
     * says. This used to be a hardcoded string, which is exactly how the two
     * ended up able to disagree.
     */
    private void sendHeaderFooter(Player viewer) {
        if (this.external) {
            return;
        }
        UUID id = viewer.getUniqueId();
        viewer.sendPlayerListHeaderAndFooter(
                TabService.mm(this.joinLayout(id, "header")),
                TabService.mm(this.joinLayout(id, "footer")));
    }

    private String joinLayout(UUID id, String part) {
        List<String> lines = this.plugin.getConfig().getStringList(
                "tab." + this.layoutContext(id) + "." + part);
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.size(); ++i) {
            if (i > 0) {
                out.append('\n');
            }
            // An empty config line is a spacer. Minecraft collapses a truly
            // empty line away, so it has to carry a space to survive.
            String line = this.fillTokens(lines.get(i), id);
            out.append(line.isEmpty() ? " " : line);
        }
        return out.toString();
    }

    public void tick() {
        // Before the external check on purpose: the bubble is ours whether or
        // not TAB is drawing the rest of the list, and with TAB installed it is
        // the case that needs this most.
        this.reassertBubbles();
        if (this.external) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            this.sendHeaderFooter(p);
        }
    }

    /**
     * Puts back the tab entries TAB keeps re-adding.
     *
     * <p>unlistPlayer tells the client to drop a row. TAB then sends its own
     * player-info updates for everyone - names, prefixes, sorting - and an
     * update for a UUID the client no longer has makes the client create the row
     * again. So the bubble came apart within a second of being applied, which is
     * exactly what "we can see everyone in tab during a duel" was.
     *
     * <p>It is a list-then-unlist rather than a plain unlist, because Paper
     * tracks who it thinks is already unlisted and a repeat call can be a no-op
     * - and a no-op is the one thing that cannot help here, since the client's
     * state has changed without Paper knowing. Both packets land in the same
     * tick, so nothing flickers.
     *
     * <p>Costs nothing while nobody is in a duel or a party, which is the state
     * a server is in most of the time.
     */
    private void reassertBubbles() {
        if (this.memberGroup.isEmpty() || this.hidesWorld()) {
            // hidePlayer is not undone by TAB, so that mode needs no upkeep.
            return;
        }
        for (Map.Entry<UUID, Object> entry : this.memberGroup.entrySet()) {
            Player member = Bukkit.getPlayer((UUID)entry.getKey());
            if (member == null) {
                continue;
            }
            Object group = entry.getValue();
            for (Player online : Bukkit.getOnlinePlayers()) {
                UUID id = online.getUniqueId();
                if (id.equals(entry.getKey()) || this.memberGroup.get(id) == group) {
                    continue;
                }
                try {
                    member.listPlayer(online);
                    member.unlistPlayer(online);
                }
                catch (Throwable t) {
                    // not visible to this player; nothing to unlist
                }
            }
        }
    }

    /**
     * Whether the bubble should hide players from the WORLD as well as the tab
     * list.
     *
     * <p>Off by default, which is what was asked for: people you are not
     * fighting stay visible around you. On, the bubble uses hidePlayer instead -
     * cruder, but nothing else can undo it, so it is the fallback if TAB ever
     * wins the argument above.
     */
    private boolean hidesWorld() {
        return this.plugin.getConfig().getBoolean("tab.bubble-hides-world", false);
    }

    private static Component mm(String miniMessage) {
        return MiniMessage.miniMessage().deserialize((Object)miniMessage);
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.scoreboard.numbers.NumberFormat
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.ComponentLike
 *  net.kyori.adventure.text.minimessage.MiniMessage
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Statistic
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Player
 *  org.bukkit.scoreboard.Criteria
 *  org.bukkit.scoreboard.DisplaySlot
 *  org.bukkit.scoreboard.Objective
 *  org.bukkit.scoreboard.Score
 *  org.bukkit.scoreboard.Scoreboard
 *  org.bukkit.scoreboard.Team
 */
package com.meowduels.managers;

import com.meowduels.MeowDuels;
import com.meowduels.managers.EventManager;
import com.meowduels.managers.QueueManager;
import com.meowduels.model.ActiveDuel;
import com.meowduels.model.Kit;
import com.meowduels.model.Party;
import com.meowduels.util.Ranks;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class ScoreboardService {
    private static final int MAX_LINES = 15;
    private static final ChatColor[] LINE_KEYS = ChatColor.values();
    private final MeowDuels plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, Board> boards = new HashMap<UUID, Board>();
    private final Map<UUID, UUID> spectatorOf = new HashMap<UUID, UUID>();
    private final Set<UUID> tabOverridden = new HashSet<UUID>();
    private final Set<UUID> tabReleased = new HashSet<UUID>();
    private final Map<UUID, String> lastRank = new HashMap<UUID, String>();
    private final Set<UUID> queueBarShown = new HashSet<UUID>();
    private static final long SCORE_BAR_MS = 10000L;
    private boolean masterEnabled = true;
    private Layout global = new Layout();
    private Layout ffa = new Layout();
    private final Map<UUID, Integer> rankTeamIndex = new HashMap<UUID, Integer>();
    private int rankTeamCounter = 0;
    private boolean rankNametags = false;
    private boolean externalNametags = false;
    private boolean rankBelowName = false;
    private static final char[] SMALL = new char[]{'\u1d00', '\u0299', '\u1d04', '\u1d05', '\u1d07', '\ua730', '\u0262', '\u029c', '\u026a', '\u1d0a', '\u1d0b', '\u029f', '\u1d0d', '\u0274', '\u1d0f', '\u1d18', '\ua7af', '\u0280', '\ua731', '\u1d1b', '\u1d1c', '\u1d20', '\u1d21', 'x', '\u028f', '\u1d22'};

    public ScoreboardService(MeowDuels plugin) {
        this.plugin = plugin;
        this.reload();
        if (Bukkit.getScoreboardManager() != null) {
            this.setupRankBelowName(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void reload() {
        ConfigurationSection sb = this.plugin.getConfig().getConfigurationSection("scoreboard");
        if (sb == null) {
            this.masterEnabled = false;
            this.global = new Layout();
            this.ffa = new Layout();
            return;
        }
        this.masterEnabled = sb.getBoolean("enabled", true);
        this.rankNametags = sb.getBoolean("rank-above-head", false);
        this.externalNametags = sb.getBoolean("external-nametags", false);
        // Deliberately NOT read from config any more. The ELO rank under the
        // nametag is gone, and a server that already has rank-below-name: true
        // in its config would otherwise keep it - existing values are never
        // overwritten, so flipping the default would have changed nothing where
        // it mattered. The objective is still actively torn off any board that
        // carries one, so removing it takes effect the moment this loads.
        this.rankBelowName = false;
        // Re-run against the main scoreboard: it is never rebuilt, so turning
        // rank-below-name off in config and reloading would otherwise leave the
        // objective drawing there forever.
        if (Bukkit.getScoreboardManager() != null) {
            this.setupRankBelowName(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        this.global = this.loadLayout(sb.getConfigurationSection("global"));
        this.ffa = this.loadLayout(sb.getConfigurationSection("ffa"));
    }

    private Layout loadLayout(ConfigurationSection section) {
        Layout layout = new Layout();
        if (section == null) {
            return layout;
        }
        layout.enabled = section.getBoolean("enabled", true);
        layout.title = section.getString("title", "");
        ArrayList<String> lines = new ArrayList<String>(section.getStringList("lines"));
        while (lines.size() > 15) {
            lines.remove(lines.size() - 1);
        }
        layout.lines = lines;
        layout.titleStatic = layout.title.indexOf(123) < 0 ? this.deserialize(layout.title) : null;
        layout.lineStatic = new Component[lines.size()];
        for (int i = 0; i < lines.size(); ++i) {
            layout.lineStatic[i] = ((String)lines.get(i)).indexOf(123) < 0 ? this.deserialize((String)lines.get(i)) : null;
        }
        return layout;
    }

    private Component deserialize(String s) {
        try {
            return this.mm.deserialize((Object)s);
        }
        catch (Exception e) {
            return this.mm.deserialize((Object)"");
        }
    }

    public void attach(Player player, ActiveDuel duel) {
        this.refresh(player);
    }

    public void attachSpectator(Player spectator, ActiveDuel duel, UUID targetId) {
        this.spectatorOf.put(spectator.getUniqueId(), targetId);
        this.refresh(spectator);
    }

    public void detach(UUID id) {
        this.spectatorOf.remove(id);
        Player player = Bukkit.getPlayer((UUID)id);
        if (player != null) {
            this.refresh(player);
        } else {
            this.removeBoard(id, null);
        }
    }

    public void handleJoin(Player player) {
        this.lastRank.clear();
        this.refresh(player);
    }

    public void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.refresh(player);
        }
        if (!this.boards.isEmpty()) {
            for (UUID id : new ArrayList<UUID>(this.boards.keySet())) {
                if (Bukkit.getPlayer((UUID)id) != null) continue;
                this.boards.remove(id);
            }
        }
        if (this.rankNametags) {
            this.applyNametagTeams();
        }
    }

    private void applyNametagTeams() {
        try {
            ArrayList<Scoreboard> targets = new ArrayList<Scoreboard>();
            ArrayList<Set<String>> skips = new ArrayList<Set<String>>();
            if (Bukkit.getScoreboardManager() != null) {
                targets.add(Bukkit.getScoreboardManager().getMainScoreboard());
                skips.add(Collections.<String>emptySet());
            }
            for (Board b : this.boards.values()) {
                targets.add(b.scoreboard);
                HashSet<String> skip = new HashSet<String>();
                if (b.hasNametags) {
                    if (b.allyEntry != null) {
                        skip.add(b.allyEntry);
                    }
                    if (b.enemyEntry != null) {
                        skip.add(b.enemyEntry);
                    }
                }
                skips.add(skip);
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                String teamName = "mdr" + this.rankTeamFor(p.getUniqueId());
                Component prefix = this.deserialize(Ranks.mini(this.plugin.getStatsManager(), p.getUniqueId()) + " ");
                for (int i = 0; i < targets.size(); ++i) {
                    if (((Set)skips.get(i)).contains(name)) continue;
                    Scoreboard sb = (Scoreboard)targets.get(i);
                    Team t = sb.getTeam(teamName);
                    if (t == null) {
                        t = sb.registerNewTeam(teamName);
                    }
                    t.addEntry(name);
                    t.prefix(prefix);
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private int rankTeamFor(UUID id) {
        Integer i = this.rankTeamIndex.get(id);
        if (i == null) {
            i = this.rankTeamCounter++;
            this.rankTeamIndex.put(id, i);
        }
        return i;
    }

    private void refresh(Player player) {
        boolean nametags;
        UUID id = player.getUniqueId();
        ActiveDuel context = this.plugin.getDuelManager().getDuel(id);
        UUID self = id;
        boolean spectator = false;
        if (context == null) {
            ActiveDuel watchedDuel;
            UUID watched = this.spectatorOf.get(id);
            ActiveDuel activeDuel = watchedDuel = watched == null ? null : this.plugin.getDuelManager().getDuel(watched);
            if (watchedDuel != null) {
                context = watchedDuel;
                self = watched;
                spectator = true;
            } else if (watched != null) {
                this.spectatorOf.remove(id);
            }
        }
        boolean inFight = context != null;
        boolean active = inFight && context.isArenaEntered();
        Layout sidebar = null;
        boolean boardOn = this.plugin.getPlayerSettings().isScoreboard(id);
        boolean inEvent = this.plugin.getEventManager().isInvolved(id);
        boolean inParty = this.plugin.getPartyManager().inParty(id);
        if (this.masterEnabled && boardOn && !inParty) {
            // A party gets no sidebar at all - not even the global one. Everyone
            // else falls through to global, INCLUDING duellists: there is no
            // duel-specific board any more, and the !inFight guard that used to
            // keep the global one off them has gone with it.
            //
            // scoreboard.duel is not read. Defaulting it to false would have
            // changed nothing on a server that already has it true, because
            // existing config values are never overwritten - and it is the
            // server that already has it true where this needed to take effect.
            if (inEvent && this.ffa.enabled) {
                sidebar = this.ffa;
            } else if (this.global.enabled) {
                sidebar = this.global;
            }
        }
        int lineCount = sidebar == null ? 0 : sidebar.lines.size();
        boolean bl = nametags = active && !this.externalNametags;
        // FFA is one team, not two sides: everyone in the event shares a yellow
        // nametag. Only when they are NOT also in a duel - a duel's own two-team
        // colouring is more specific and wins.
        boolean ffaTags = !active && inEvent && !this.externalNametags;
        if (sidebar == null && !nametags && !ffaTags) {
            this.removeBoard(id, player);
            return;
        }
        Board board = this.boards.get(id);
        if (board == null || board.sidebarLines != lineCount || board.hasNametags != nametags
                || board.hasFfaTeam != ffaTags) {
            board = new Board(lineCount, nametags, ffaTags);
            this.boards.put(id, board);
            player.setScoreboard(board.scoreboard);
            this.lastRank.clear();
        }
        if (sidebar != null) {
            Map<String, String> tokens = this.tokens(player, context, self, spectator);
            board.setTitle(this.render(sidebar.titleStatic, sidebar.title, tokens));
            for (int i = 0; i < lineCount; ++i) {
                board.setLine(i, this.render(sidebar.lineStatic[i], sidebar.lines.get(i), tokens));
            }
        }
        if (nametags) {
            UUID enemy = context.getOpponent(self);
            board.setNametags(self, enemy, context.isAqua(self));
        }
        if (ffaTags) {
            board.setFfaMembers(this.plugin.getEventManager().involved());
        }
        if (active && context.getState() == ActiveDuel.State.FIGHTING && context.sinceFightStart() < 10000L) {
            this.sendActionBar(player, context, self);
        } else if (!inFight) {
            this.sendQueueBar(player);
        }
    }

    private void setupRankBelowName(Scoreboard board) {
        if (this.externalNametags || !this.rankBelowName) {
            // Not enough to skip creating it. A board that already carries the
            // objective keeps drawing it until something takes it away, so
            // turning the setting off would do nothing until a restart - and on
            // the MAIN scoreboard, which is never rebuilt, nothing at all.
            ScoreboardService.removeRankBelowName(board);
            return;
        }
        try {
            Objective o = board.registerNewObjective("mdrank", Criteria.DUMMY, "");
            o.displayName(this.deserialize(""));
            o.setDisplaySlot(DisplaySlot.BELOW_NAME);
            try {
                o.numberFormat(NumberFormat.blank());
            }
            catch (Throwable throwable) {}
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private static void removeRankBelowName(Scoreboard board) {
        try {
            Objective existing = board.getObjective("mdrank");
            if (existing != null) {
                existing.unregister();
            }
        }
        catch (Throwable throwable) {
            // already gone, or a board that never had one
        }
    }

    public void updateRankBelowName() {
        if (this.externalNametags || !this.rankBelowName) {
            return;
        }
        try {
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID id = p.getUniqueId();
                String mini = Ranks.mini(this.plugin.getStatsManager(), id);
                if (mini.equals(this.lastRank.get(id))) continue;
                this.lastRank.put(id, mini);
                NumberFormat fmt = NumberFormat.fixed((ComponentLike)this.deserialize(mini));
                this.applyBelowRank(p.getName(), fmt, this.plugin.getStatsManager().getElo(id));
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void applyBelowRank(String entry, NumberFormat fmt, int score) {
        if (Bukkit.getScoreboardManager() != null) {
            this.setBelowRank(Bukkit.getScoreboardManager().getMainScoreboard(), entry, fmt, score);
        }
        for (Board b : this.boards.values()) {
            this.setBelowRank(b.scoreboard, entry, fmt, score);
        }
    }

    private void setBelowRank(Scoreboard board, String entry, NumberFormat fmt, int score) {
        try {
            Objective o = board.getObjective("mdrank");
            if (o == null) {
                return;
            }
            Score sc = o.getScore(entry);
            sc.numberFormat(fmt);
            sc.setScore(score);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public void updateDuelHealthTags() {
        if (this.externalNametags) {
            return;
        }
        try {
            for (Board b : this.boards.values()) {
                if (!b.hasNametags) continue;
                b.refreshHealth();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void sendQueueBar(Player player) {
        UUID id = player.getUniqueId();
        QueueManager queue = this.plugin.getQueueManager();
        int kits = queue.queuedKits(id).size();
        if (kits <= 0) {
            if (this.queueBarShown.remove(id)) {
                player.sendActionBar(this.deserialize(""));
            }
            return;
        }
        this.queueBarShown.add(id);
        long seconds = Math.max(0L, queue.waitingSeconds(id));
        String time = String.format("%d:%02d", seconds / 60L, seconds % 60L);
        player.sendActionBar(this.deserialize("<gradient:#FF2E55:#FF7FC4>\u01eb\u1d1c\u1d07\u1d1c\u1d07\u1d05 \ua730\u1d0f\u0280 " + kits + " " + (kits == 1 ? "\u1d0b\u026a\u1d1b" : "\u1d0b\u026a\u1d1b\ua731") + "</gradient> <#6B7079>\u2503 <#FF7FC4>\u231a <#E6E8EB>" + time));
    }

    private void sendActionBar(Player player, ActiveDuel duel, UUID self) {
        String raw = this.plugin.getConfig().getString("scoreboard.action-bar", "{score} <gray>-</gray> {opponent_score}");
        raw = raw.replace("{score}", "<green>" + duel.getScoreFor(self) + "</green>").replace("{opponent_score}", "<red>" + duel.getScoreAgainst(self) + "</red>");
        player.sendActionBar(this.deserialize(raw));
    }

    private void removeBoard(UUID id, Player player) {
        if (this.boards.remove(id) != null && player != null && Bukkit.getScoreboardManager() != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private Component render(Component cached, String raw, Map<String, String> tokens) {
        if (cached != null) {
            return cached;
        }
        return this.deserialize(this.replace(raw, tokens));
    }

    private String replace(String s, Map<String, String> tokens) {
        if (s.indexOf(123) < 0) {
            return s;
        }
        for (Map.Entry<String, String> e : tokens.entrySet()) {
            s = s.replace("{" + e.getKey() + "}", e.getValue());
        }
        return s;
    }

    /** mm:ss, the same shape the duel board's {time} uses. */
    private static String clock(long seconds) {
        return String.format("%02d:%02d", seconds / 60L, seconds % 60L);
    }

    private Map<String, String> tokens(Player player, ActiveDuel ctx, UUID self, boolean spectator) {
        HashMap<String, String> t = new HashMap<String, String>();
        UUID id = player.getUniqueId();
        t.put("player", spectator ? this.nameOf(self) : player.getName());
        t.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));
        t.put("in_duels", String.valueOf(this.plugin.getDuelManager().playersInDuels()));
        t.put("server_ip", this.plugin.getScoreboardIp());
        t.put("ip_color", this.plugin.getConfig().getString("scoreboard-ip-color", "<dark_red>"));
        t.put("ping", String.valueOf(player.getPing()));
        t.put("kills", String.valueOf(player.getStatistic(Statistic.PLAYER_KILLS)));
        t.put("deaths", String.valueOf(player.getStatistic(Statistic.DEATHS)));
        t.put("playtime", this.playtime(player));
        t.put("kdr", this.kdr(player));
        t.put("streak", String.valueOf(this.plugin.getStatsManager().getStreak(id)));
        UUID recordId = spectator ? self : id;
        t.put("wins", String.valueOf(this.plugin.getStatsManager().getWins(recordId)));
        t.put("losses", String.valueOf(this.plugin.getStatsManager().getLosses(recordId)));
        t.put("elo", String.valueOf(this.plugin.getStatsManager().getElo(recordId)));
        t.put("rank", Ranks.mini(this.plugin.getStatsManager(), recordId));
        t.put("server_name", this.plugin.getServerName());
        t.put("date", this.date());
        t.put("server_ip_sc", ScoreboardService.smallCaps(this.plugin.getScoreboardIp()));
        // FFA/event tokens. Always present, so the ffa board never renders a
        // literal "{alive}" at the moment an event is winding down and the
        // manager has already cleared its state.
        EventManager ev = this.plugin.getEventManager();
        t.put("alive", String.valueOf(ev.aliveCount()));
        t.put("ffa_players", String.valueOf(ev.playerCount()));
        t.put("ffa_spectators", String.valueOf(ev.spectatorCount()));
        t.put("ffa_kills", String.valueOf(ev.killsOf(recordId)));
        t.put("ffa_kit", this.kitLabel(ev.getKit()));
        t.put("ffa_time", ScoreboardService.clock(ev.runningSeconds()));
        Party party = this.plugin.getPartyManager().partyOf(id);
        t.put("party_leader", party == null ? "" : this.nameOf(party.getLeader()));
        t.put("party_size", party == null ? "0" : String.valueOf(party.size()));
        t.put("party_kit", party == null || party.getKit() == null ? "" : this.kitLabel(party.getKit()));
        t.put("party_status", party == null ? "" : (party.isFighting() ? "Fighting" : "Waiting"));
        t.put("party_alive", party == null ? "0" : String.valueOf(party.getAlive().size()));
        t.put("party_role", party == null ? "" : (party.isLeader(id) ? "Leader" : "Member"));
        t.put("party_kills", party == null ? "0" : String.valueOf(party.killsOf(id)));
        t.put("party_time", ScoreboardService.clock(party == null || party.getStartedAt() == 0L
                ? 0L : Math.max(0L, (System.currentTimeMillis() - party.getStartedAt()) / 1000L)));
        String[] duelKeys = new String[]{"score", "opponent_score", "opponent", "kit", "arena", "round", "rounds_to_win", "time", "team", "team_color", "opponent_color", "game", "spectators"};
        if (ctx == null) {
            for (String k : duelKeys) {
                t.put(k, "");
            }
            return t;
        }
        t.put("spectators", String.valueOf(this.plugin.getSpectateManager().countWatchers(ctx.getPlayer1(), ctx.getPlayer2())));
        boolean aqua = ctx.isAqua(self);
        UUID opponent = ctx.getOpponent(self);
        t.put("score", String.valueOf(ctx.getScoreFor(self)));
        t.put("opponent_score", String.valueOf(ctx.getScoreAgainst(self)));
        t.put("opponent", this.nameOf(opponent));
        t.put("kit", this.kitLabel(ctx.getKit()));
        t.put("arena", ctx.getArena() == null ? "" : ctx.getArena().getName());
        t.put("round", String.valueOf(ctx.getCurrentRound()));
        t.put("rounds_to_win", String.valueOf(ctx.getRoundsToWin()));
        t.put("game", String.format("%04d", ctx.getGameNumber()));
        long seconds = Math.max(0L, (System.currentTimeMillis() - ctx.getStartedAt()) / 1000L);
        t.put("time", String.format("%02d:%02d", seconds / 60L, seconds % 60L));
        t.put("team", spectator ? "SPEC" : (aqua ? "AQUA" : "RED"));
        t.put("team_color", spectator ? "<gray>" : (aqua ? "<aqua>" : "<red>"));
        t.put("opponent_color", spectator ? "<gray>" : (aqua ? "<red>" : "<aqua>"));
        return t;
    }

    private static String smallCaps(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);
            if (c >= 'a' && c <= 'z') {
                sb.append(SMALL[c - 97]);
                continue;
            }
            if (c >= 'A' && c <= 'Z') {
                sb.append(SMALL[c - 65]);
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private String date() {
        String fmt = this.plugin.getConfig().getString("scoreboard.date-format", "MMM d, yyyy");
        try {
            return LocalDate.now().format(DateTimeFormatter.ofPattern(fmt, Locale.ENGLISH));
        }
        catch (Exception e) {
            return LocalDate.now().toString();
        }
    }

    private String playtime(Player player) {
        long minutes = (long)player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L / 60L;
        long h = minutes / 60L;
        long m = minutes % 60L;
        return h > 0L ? h + "h " + m + "m" : m + "m";
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

    private String kdr(Player player) {
        int kills = player.getStatistic(Statistic.PLAYER_KILLS);
        int deaths = player.getStatistic(Statistic.DEATHS);
        double ratio = deaths <= 0 ? (double)kills : (double)kills / (double)deaths;
        return String.format(Locale.US, "%.2f", ratio);
    }

    private String hpText(double hp) {
        if (hp < 0.0) {
            hp = 0.0;
        }
        if (hp < 5.0) {
            return String.format(Locale.US, "%.2f", hp);
        }
        return String.valueOf((int)Math.round(hp));
    }

    private String hpTextOf(UUID id) {
        Player p = id == null ? null : Bukkit.getPlayer((UUID)id);
        return p == null ? "0" : this.hpText(p.getHealth());
    }

    private String kitLabel(String kitId) {
        Kit kit = this.plugin.getKitManager().get(kitId);
        if (kit != null && kit.getDisplayName() != null && !kit.getDisplayName().isEmpty()) {
            return kit.getDisplayName();
        }
        return kitId;
    }

    private static final class Layout {
        private boolean enabled = false;
        private String title = "";
        private List<String> lines = new ArrayList<String>();
        private Component titleStatic;
        private Component[] lineStatic = new Component[0];

        private Layout() {
        }
    }

    private final class Board {
        private final Scoreboard scoreboard;
        private final Objective sidebarObjective;
        private final Team[] teams;
        private final int sidebarLines;
        private final boolean hasNametags;
        private final Team allyTeam;
        private final Team enemyTeam;
        private UUID allyId;
        private UUID enemyId;
        private String allyHp;
        private String enemyHp;
        private String allyEntry;
        private String enemyEntry;
        private final boolean hasFfaTeam;
        private Team ffaTeam;
        private final Set<String> ffaEntries = new HashSet<String>();

        private Board(int sidebarLines, boolean nametags, boolean ffaTeam) {
            this.hasFfaTeam = ffaTeam;
            this.sidebarLines = sidebarLines;
            this.hasNametags = nametags;
            this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            this.teams = new Team[sidebarLines];
            if (sidebarLines > 0) {
                Objective sidebar = this.scoreboard.registerNewObjective("mdside", Criteria.DUMMY, "");
                sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
                // Hiding the red score column is cosmetic, so it must not be
                // able to take the sidebar down with it. It already did once:
                // a mis-shaped NumberFormat stub threw IncompatibleClassChangeError
                // here, and because this line sits in the constructor the whole
                // board failed to build - every tick, for every player.
                try {
                    sidebar.numberFormat(NumberFormat.blank());
                }
                catch (Throwable t) {
                    // older/forked server without the API: the numbers just show
                }
                for (int i = 0; i < sidebarLines; ++i) {
                    String entry = LINE_KEYS[i % LINE_KEYS.length].toString();
                    Team team = this.scoreboard.registerNewTeam("line" + i);
                    team.addEntry(entry);
                    sidebar.getScore(entry).setScore(sidebarLines - i);
                    this.teams[i] = team;
                }
                this.sidebarObjective = sidebar;
            } else {
                this.sidebarObjective = null;
            }
            if (nametags) {
                this.allyTeam = this.scoreboard.registerNewTeam("md_ally");
                this.enemyTeam = this.scoreboard.registerNewTeam("md_enemy");
                this.allyTeam.setColor(ChatColor.AQUA);
                this.enemyTeam.setColor(ChatColor.RED);
            } else {
                this.allyTeam = null;
                this.enemyTeam = null;
            }
            if (ffaTeam) {
                this.ffaTeam = this.scoreboard.registerNewTeam("md_ffa");
                this.ffaTeam.setColor(ChatColor.YELLOW);
                this.ffaTeam.prefix(ScoreboardService.this.deserialize("<yellow>\u26a1 "));
            } else {
                this.ffaTeam = null;
            }
            ScoreboardService.this.setupRankBelowName(this.scoreboard);
        }

        private void setTitle(Component c) {
            if (this.sidebarObjective != null) {
                this.sidebarObjective.displayName(c);
            }
        }

        private void setLine(int i, Component c) {
            if (i >= 0 && i < this.teams.length && this.teams[i] != null) {
                this.teams[i].prefix(c);
            }
        }

        /**
         * Puts everyone in the event on one yellow team, shown as "\u26a1 Name".
         *
         * <p>Only the difference is applied. Re-adding an entry every tick
         * resends the team packet to everyone on the board for no reason, and
         * with a full FFA lobby that is a lot of packets a second.
         */
        private void setFfaMembers(Set<UUID> members) {
            if (this.ffaTeam == null) {
                return;
            }
            HashSet<String> want = new HashSet<String>();
            for (UUID id : members) {
                String name = ScoreboardService.this.nameOf(id);
                if (!name.isEmpty()) {
                    want.add(name);
                }
            }
            for (String name : want) {
                if (this.ffaEntries.add(name)) {
                    this.ffaTeam.addEntry(name);
                }
            }
            java.util.Iterator<String> it = this.ffaEntries.iterator();
            while (it.hasNext()) {
                String name = it.next();
                if (!want.contains(name)) {
                    try {
                        this.ffaTeam.removeEntry(name);
                    }
                    catch (Throwable throwable) {
                        // entry already gone with the player
                    }
                    it.remove();
                }
            }
        }

        private void setNametags(UUID ally, UUID enemy, boolean allyAqua) {
            if (this.allyTeam == null) {
                return;
            }
            this.allyId = ally;
            this.enemyId = enemy;
            String allyName = ScoreboardService.this.nameOf(ally);
            String enemyName = ScoreboardService.this.nameOf(enemy);
            if (!allyName.equals(this.allyEntry)) {
                this.allyTeam.addEntry(allyName);
                this.allyEntry = allyName;
            }
            if (!enemyName.equals(this.enemyEntry)) {
                this.enemyTeam.addEntry(enemyName);
                this.enemyEntry = enemyName;
            }
            this.allyTeam.setColor(allyAqua ? ChatColor.AQUA : ChatColor.RED);
            this.enemyTeam.setColor(allyAqua ? ChatColor.RED : ChatColor.AQUA);
            this.allyTeam.prefix(ScoreboardService.this.deserialize(allyAqua ? "<aqua>\u26a1 " : "<red>\u26a1 "));
            this.enemyTeam.prefix(ScoreboardService.this.deserialize(allyAqua ? "<red>\u26a1 " : "<aqua>\u26a1 "));
            this.refreshHealth();
        }

        private void refreshHealth() {
            if (this.allyTeam == null) {
                return;
            }
            this.allyHp = this.applyHealth(this.allyTeam, this.allyId, this.allyHp);
            this.enemyHp = this.applyHealth(this.enemyTeam, this.enemyId, this.enemyHp);
        }

        private String applyHealth(Team team, UUID id, String shown) {
            Player p;
            Player player = p = id == null ? null : Bukkit.getPlayer((UUID)id);
            if (p == null) {
                return shown;
            }
            String txt = ScoreboardService.this.hpText(p.getHealth());
            if (txt.equals(shown)) {
                return shown;
            }
            team.suffix(ScoreboardService.this.deserialize(" <#FF5C5C>\u2764 " + txt));
            return txt;
        }
    }
}


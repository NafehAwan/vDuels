package com.meowduels.model;

import com.meowduels.model.PartyMode;
import com.meowduels.model.PlayerSnapshot;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A group of players who queue, fight and spectate together.
 *
 * <p>Deliberately a plain holder: every rule about what a party may do lives in
 * PartyManager, so there is one place to look when a party ends up in a state it
 * should not be in. The one thing the model does enforce is that the leader is
 * always a member - a leaderless party is the shape most of the bugs would take.
 *
 * <p>A party match is a private free-for-all: everyone spawns at the arena's
 * event spawn, the last one standing wins, and anyone knocked out watches the
 * rest of it rather than being dumped back at spawn on their own.
 */
public class Party {
    public enum State { IDLE, FIGHTING }

    /** Which side of a Split match someone is on. Meaningless in FFA, where the
     *  map is simply left empty. */
    public enum Team { AQUA, RED }

    private final UUID leader;
    /** Insertion-ordered, so the member list in the GUI doesn't reshuffle
     *  itself every time it is opened. */
    private final Set<UUID> members = new LinkedHashSet<UUID>();
    private final Set<UUID> invited = new HashSet<UUID>();
    private final Set<UUID> alive = new HashSet<UUID>();
    private final Set<UUID> watching = new HashSet<UUID>();
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<UUID, PlayerSnapshot>();
    private final Map<UUID, Integer> kills = new HashMap<UUID, Integer>();
    /** Blocks this match changed, with what was there before - the fallback for
     *  restoring an arena that has no saved snapshot. Same contract as
     *  ActiveDuel's. */
    private final Map<Location, BlockData> changedBlocks = new HashMap<Location, BlockData>();
    /**
     * Team membership for a Split match.
     *
     * <p>Kept separate from {@link #alive}: a member who is knocked out stops
     * being alive but stays on their team, which is what lets the board and the
     * nametags keep colouring them correctly while they watch.
     */
    private final Map<UUID, Team> teams = new HashMap<UUID, Team>();
    private PartyMode mode = PartyMode.FFA;
    /** For Party Duels: the LEADER of the party we are about to fight, carried
     *  across the opponent picker, the kit picker and the confirm screen.
     *  A leader id rather than a Party reference, so a party that disbands
     *  between two menus resolves to nothing instead of to a ghost. */
    private UUID duelTarget;
    private int duelRounds = 3;
    /**
     * The other party in a Party Duels match.
     *
     * <p>Party Duels is Split played across two parties instead of inside one,
     * so the two sides need to see one match. Rather than invent a shared match
     * object, the match state - arena, alive, watching, teams, timings - is
     * MIRRORED into both parties, and this is the link that keeps them in step.
     * Every guard in the plugin starts from partyOf(someone) and finds a party
     * that already knows the whole fight, which is why nothing else had to
     * learn about two-party matches.
     *
     * <p>Only the host carries the snapshots and the changed blocks, because
     * those are restored exactly once.
     */
    private Party opponent;
    private boolean matchHost;
    /** Party Duels is played to a number of rounds, like a duel. Mirrored into
     *  both sides with everything else. */
    private int roundsToWin = 1;
    private int round = 1;
    private int scoreAqua;
    private int scoreRed;
    private State state = State.IDLE;
    private String kit;
    private Arena arena;
    private long startedAt;
    /** Wall-clock moment the fighting actually opens. Between the teleport and
     *  this, everyone is in the arena and nobody can hurt anyone - a countdown
     *  is a fight that has not started yet, not a fight with a delay on it. */
    private long fightStartsAt;
    private boolean friendlyFire = true;
    private boolean openToAll = false;
    private boolean finished = false;

    public Party(UUID leader) {
        this.leader = leader;
        this.members.add(leader);
    }

    public UUID getLeader() {
        return this.leader;
    }

    public boolean isLeader(UUID id) {
        return this.leader.equals(id);
    }

    public Set<UUID> getMembers() {
        return this.members;
    }

    public int size() {
        return this.members.size();
    }

    public boolean has(UUID id) {
        return this.members.contains(id);
    }

    public void add(UUID id) {
        this.members.add(id);
        this.invited.remove(id);
    }

    /** Removing the leader is not allowed here - PartyManager disbands instead,
     *  which is the only sane answer and keeps this class from having to invent
     *  a succession rule nobody asked for. */
    public void remove(UUID id) {
        this.members.remove(id);
        this.teams.remove(id);
        this.invited.remove(id);
        this.alive.remove(id);
        this.watching.remove(id);
        this.snapshots.remove(id);
        this.kills.remove(id);
    }

    public Set<UUID> getInvited() {
        return this.invited;
    }

    public void invite(UUID id) {
        this.invited.add(id);
    }

    public boolean isInvited(UUID id) {
        return this.invited.contains(id);
    }

    public State getState() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isFighting() {
        return this.state == State.FIGHTING;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public String getKit() {
        return this.kit;
    }

    public void setKit(String kit) {
        this.kit = kit;
    }

    public Arena getArena() {
        return this.arena;
    }

    public void setArena(Arena arena) {
        this.arena = arena;
    }

    public long getStartedAt() {
        return this.startedAt;
    }

    public long getFightStartsAt() {
        return this.fightStartsAt;
    }

    public void setFightStartsAt(long at) {
        this.fightStartsAt = at;
    }

    /** True while the countdown is still running. */
    public boolean isCountingDown() {
        return this.state == State.FIGHTING && System.currentTimeMillis() < this.fightStartsAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public Set<UUID> getAlive() {
        return this.alive;
    }

    /** Members who have been knocked out and are watching the rest of it. */
    public Set<UUID> getWatching() {
        return this.watching;
    }

    public Map<UUID, PlayerSnapshot> getSnapshots() {
        return this.snapshots;
    }

    public int killsOf(UUID id) {
        Integer v = this.kills.get(id);
        return v == null ? 0 : v;
    }

    public void addKill(UUID id) {
        if (this.members.contains(id)) {
            this.kills.merge(id, 1, Integer::sum);
        }
    }

    public boolean isFriendlyFire() {
        return this.friendlyFire;
    }

    public void setFriendlyFire(boolean v) {
        this.friendlyFire = v;
    }

    public boolean isOpenToAll() {
        return this.openToAll;
    }

    public void setOpenToAll(boolean v) {
        this.openToAll = v;
    }

    /** Everyone the match concerns: still fighting, plus knocked out and
     *  watching. Drives the nametag team and the spectate button. */
    public Set<UUID> involved() {
        HashSet<UUID> out = new HashSet<UUID>(this.alive);
        out.addAll(this.watching);
        return out;
    }

    public void recordChange(Location loc, BlockData original) {
        Location key = new Location(loc.getWorld(), (double)loc.getBlockX(), (double)loc.getBlockY(), (double)loc.getBlockZ());
        this.changedBlocks.putIfAbsent(key, original);
    }

    public Map<Location, BlockData> getChangedBlocks() {
        return this.changedBlocks;
    }

    public UUID getDuelTarget() {
        return this.duelTarget;
    }

    public void setDuelTarget(UUID leaderId) {
        this.duelTarget = leaderId;
    }

    public int getDuelRounds() {
        return this.duelRounds;
    }

    public void setDuelRounds(int rounds) {
        this.duelRounds = Math.max(1, Math.min(9, rounds));
    }

    public PartyMode getMode() {
        return this.mode;
    }

    public void setMode(PartyMode mode) {
        this.mode = mode == null ? PartyMode.FFA : mode;
    }

    public Party getOpponent() {
        return this.opponent;
    }

    public void setOpponent(Party other) {
        this.opponent = other;
    }

    public int getRoundsToWin() {
        return this.roundsToWin;
    }

    public void setRoundsToWin(int rounds) {
        this.roundsToWin = Math.max(1, Math.min(9, rounds));
    }

    public int getRound() {
        return this.round;
    }

    public void setRound(int round) {
        this.round = Math.max(1, round);
    }

    public int scoreOf(Team team) {
        return team == Team.AQUA ? this.scoreAqua : this.scoreRed;
    }

    public void addScore(Team team) {
        if (team == Team.AQUA) {
            ++this.scoreAqua;
        } else {
            ++this.scoreRed;
        }
    }

    public boolean isMatchHost() {
        return this.matchHost;
    }

    public void setMatchHost(boolean host) {
        this.matchHost = host;
    }

    /** Split and Party Duels are both two-sided; FFA is not. Anything that asks
     *  "are there teams here" wants this, not isSplit. */
    public boolean isTeamMode() {
        return this.mode == PartyMode.SPLIT || this.mode == PartyMode.DUELS;
    }

    public boolean isSplit() {
        return this.mode == PartyMode.SPLIT;
    }

    public Map<UUID, Team> getTeams() {
        return this.teams;
    }

    public Team teamOf(UUID id) {
        return this.teams.get(id);
    }

    public void setTeam(UUID id, Team team) {
        if (this.members.contains(id)) {
            this.teams.put(id, team);
        }
    }

    /** Everyone on a side, in member order so the picker does not reshuffle
     *  itself every time it is drawn. */
    public java.util.List<UUID> teamMembers(Team team) {
        java.util.ArrayList<UUID> out = new java.util.ArrayList<UUID>();
        for (UUID id : this.members) {
            if (this.teams.get(id) == team) {
                out.add(id);
            }
        }
        return out;
    }

    /**
     * Splits the party down the middle at random.
     *
     * <p>An odd member goes to AQUA rather than to whichever side the loop
     * happened to reach first - the imbalance is unavoidable, so it may as well
     * be predictable.
     */
    public void shuffleTeams() {
        java.util.ArrayList<UUID> order = new java.util.ArrayList<UUID>(this.members);
        java.util.Collections.shuffle(order);
        this.teams.clear();
        int half = (order.size() + 1) / 2;
        for (int i = 0; i < order.size(); ++i) {
            this.teams.put(order.get(i), i < half ? Team.AQUA : Team.RED);
        }
    }

    /** How many of a side are still in the fight. */
    public int aliveOn(Team team) {
        int count = 0;
        for (UUID id : this.alive) {
            if (this.teams.get(id) == team) {
                ++count;
            }
        }
        return count;
    }

    /**
     * How many this side has in play: the alive count once a match is running,
     * the membership count before it starts.
     *
     * <p>The team picker needs "does each side have someone" before anyone is
     * alive, and the same question during a match means "is this side still in
     * it". One method, because they are the same question at different times.
     */
    public int aliveOrMembers(Team team) {
        return this.state == State.FIGHTING ? this.aliveOn(team) : this.teamMembers(team).size();
    }

    public void resetMatch() {
        this.state = State.IDLE;
        this.finished = false;
        this.alive.clear();
        this.watching.clear();
        this.snapshots.clear();
        this.kills.clear();
        this.arena = null;
        this.startedAt = 0L;
        this.fightStartsAt = 0L;
        this.changedBlocks.clear();
        this.teams.clear();
        this.mode = PartyMode.FFA;
        this.duelTarget = null;
        this.opponent = null;
        this.matchHost = false;
        this.roundsToWin = 1;
        this.round = 1;
        this.scoreAqua = 0;
        this.scoreRed = 0;
    }
}

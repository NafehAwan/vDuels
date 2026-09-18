package com.meowduels.model;

import com.meowduels.model.PlayerSnapshot;
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

    private final UUID leader;
    /** Insertion-ordered, so the member list in the GUI doesn't reshuffle
     *  itself every time it is opened. */
    private final Set<UUID> members = new LinkedHashSet<UUID>();
    private final Set<UUID> invited = new HashSet<UUID>();
    private final Set<UUID> alive = new HashSet<UUID>();
    private final Set<UUID> watching = new HashSet<UUID>();
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<UUID, PlayerSnapshot>();
    private final Map<UUID, Integer> kills = new HashMap<UUID, Integer>();
    private State state = State.IDLE;
    private String kit;
    private Arena arena;
    private long startedAt;
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

    public void resetMatch() {
        this.state = State.IDLE;
        this.finished = false;
        this.alive.clear();
        this.watching.clear();
        this.snapshots.clear();
        this.kills.clear();
        this.arena = null;
        this.startedAt = 0L;
    }
}

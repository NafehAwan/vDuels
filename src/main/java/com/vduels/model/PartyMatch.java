package com.vduels.model;

import com.vduels.model.Arena;
import com.vduels.model.Party;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.scoreboard.Scoreboard;

public class PartyMatch {
    private final Party party;
    private final Arena arena;
    private final String kit;
    private final int roundsToWin;
    private final boolean healthIndicator;
    private final boolean allowDrops;
    private final Set<UUID> participants;
    private final Set<UUID> alive = new HashSet<UUID>();
    private final Map<UUID, Integer> wins = new HashMap<UUID, Integer>();
    private State state = State.STARTING;
    private int currentRound = 1;
    private Scoreboard healthScoreboard;

    public PartyMatch(Party party, Arena arena, String kit, int roundsToWin, boolean healthIndicator, boolean allowDrops, Set<UUID> participants) {
        this.party = party;
        this.arena = arena;
        this.kit = kit;
        this.roundsToWin = roundsToWin;
        this.healthIndicator = healthIndicator;
        this.allowDrops = allowDrops;
        this.participants = new LinkedHashSet<UUID>(participants);
        for (UUID id : participants) {
            this.wins.put(id, 0);
        }
    }

    public Party getParty() {
        return this.party;
    }

    public Arena getArena() {
        return this.arena;
    }

    public String getKit() {
        return this.kit;
    }

    public int getRoundsToWin() {
        return this.roundsToWin;
    }

    public boolean isHealthIndicator() {
        return this.healthIndicator;
    }

    public boolean isAllowDrops() {
        return this.allowDrops;
    }

    public Set<UUID> getParticipants() {
        return this.participants;
    }

    public Set<UUID> getAlive() {
        return this.alive;
    }

    public void resetAlive() {
        this.alive.clear();
        this.alive.addAll(this.participants);
    }

    public void eliminate(UUID id) {
        this.alive.remove(id);
    }

    public boolean isRoundOver() {
        return this.alive.size() <= 1;
    }

    public UUID getRoundWinner() {
        return this.alive.size() == 1 ? this.alive.iterator().next() : null;
    }

    public int awardRoundWin(UUID winner) {
        return this.wins.merge(winner, 1, Integer::sum);
    }

    public int getWins(UUID id) {
        return this.wins.getOrDefault(id, 0);
    }

    public UUID getMatchWinner() {
        for (Map.Entry<UUID, Integer> entry : this.wins.entrySet()) {
            if (entry.getValue() < this.roundsToWin) continue;
            return entry.getKey();
        }
        return null;
    }

    public int getCurrentRound() {
        return this.currentRound;
    }

    public void nextRound() {
        ++this.currentRound;
    }

    public State getState() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Scoreboard getHealthScoreboard() {
        return this.healthScoreboard;
    }

    public void setHealthScoreboard(Scoreboard scoreboard) {
        this.healthScoreboard = scoreboard;
    }

    public static enum State {
        STARTING,
        FIGHTING,
        ENDING;

    }
}


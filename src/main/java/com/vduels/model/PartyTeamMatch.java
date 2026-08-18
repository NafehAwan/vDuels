package com.vduels.model;

import com.vduels.model.Arena;
import com.vduels.model.Party;
import com.vduels.model.Team;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.scoreboard.Scoreboard;

public class PartyTeamMatch {
    private final Arena arena;
    private final String kit;
    private final int roundsToWin;
    private final boolean healthIndicator;
    private final boolean allowDrops;
    private final Map<UUID, Team> teamOf;
    private final Set<UUID> alive = new HashSet<UUID>();
    private final Map<Team, Integer> roundWins = new HashMap<Team, Integer>();
    private final Party redParty;
    private final Party blueParty;
    private State state = State.STARTING;
    private int currentRound = 1;
    private Scoreboard healthScoreboard;

    public PartyTeamMatch(Arena arena, String kit, int roundsToWin, boolean healthIndicator, boolean allowDrops, Map<UUID, Team> teamOf, Party redParty, Party blueParty) {
        this.arena = arena;
        this.kit = kit;
        this.roundsToWin = roundsToWin;
        this.healthIndicator = healthIndicator;
        this.allowDrops = allowDrops;
        this.teamOf = new LinkedHashMap<UUID, Team>(teamOf);
        this.redParty = redParty;
        this.blueParty = blueParty;
        this.roundWins.put(Team.RED, 0);
        this.roundWins.put(Team.BLUE, 0);
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

    public Map<UUID, Team> getTeamOf() {
        return this.teamOf;
    }

    public Team getTeam(UUID id) {
        return this.teamOf.getOrDefault(id, Team.NONE);
    }

    public Set<UUID> getParticipants() {
        return this.teamOf.keySet();
    }

    public Set<UUID> getAlive() {
        return this.alive;
    }

    public Party getRedParty() {
        return this.redParty;
    }

    public Party getBlueParty() {
        return this.blueParty;
    }

    public void resetAlive() {
        this.alive.clear();
        this.alive.addAll(this.teamOf.keySet());
    }

    public void eliminate(UUID id) {
        this.alive.remove(id);
    }

    private Set<Team> aliveTeams() {
        HashSet<Team> teams = new HashSet<Team>();
        for (UUID id : this.alive) {
            teams.add(this.teamOf.get(id));
        }
        return teams;
    }

    public boolean isRoundOver() {
        return this.aliveTeams().size() <= 1;
    }

    public Team getWinningTeam() {
        Set<Team> teams = this.aliveTeams();
        return teams.size() == 1 ? teams.iterator().next() : null;
    }

    public int awardRoundWin(Team team) {
        return this.roundWins.merge(team, 1, Integer::sum);
    }

    public int getWins(Team team) {
        return this.roundWins.getOrDefault((Object)team, 0);
    }

    public Team getMatchWinner() {
        for (Map.Entry<Team, Integer> entry : this.roundWins.entrySet()) {
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


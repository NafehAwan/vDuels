package com.vduels.model;

import com.vduels.model.PartyMatch;
import com.vduels.model.Team;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Party {
    private UUID leader;
    private final Set<UUID> members = new LinkedHashSet<UUID>();
    private final Map<UUID, Team> splitTeams = new LinkedHashMap<UUID, Team>();
    private String selectedKit;
    private int rounds = 1;
    private boolean healthIndicator = true;
    private boolean allowDrops = false;
    private boolean isPublic = false;
    private PartyMatch activeMatch;
    private boolean inTeamMatch = false;
    private UUID pendingDuelTarget;

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

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public Set<UUID> getMembers() {
        return this.members;
    }

    public int size() {
        return this.members.size();
    }

    public String getSelectedKit() {
        return this.selectedKit;
    }

    public void setSelectedKit(String kit) {
        this.selectedKit = kit;
    }

    public int getRounds() {
        return this.rounds;
    }

    public void setRounds(int rounds) {
        this.rounds = Math.max(1, rounds);
    }

    public boolean isHealthIndicator() {
        return this.healthIndicator;
    }

    public void setHealthIndicator(boolean healthIndicator) {
        this.healthIndicator = healthIndicator;
    }

    public boolean isAllowDrops() {
        return this.allowDrops;
    }

    public void setAllowDrops(boolean allowDrops) {
        this.allowDrops = allowDrops;
    }

    public boolean isPublic() {
        return this.isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public PartyMatch getActiveMatch() {
        return this.activeMatch;
    }

    public void setActiveMatch(PartyMatch match) {
        this.activeMatch = match;
    }

    public boolean isInTeamMatch() {
        return this.inTeamMatch;
    }

    public void setInTeamMatch(boolean inTeamMatch) {
        this.inTeamMatch = inTeamMatch;
    }

    public boolean isBusy() {
        return this.activeMatch != null || this.inTeamMatch;
    }

    public UUID getPendingDuelTarget() {
        return this.pendingDuelTarget;
    }

    public void setPendingDuelTarget(UUID target) {
        this.pendingDuelTarget = target;
    }

    public Team getTeam(UUID id) {
        return this.splitTeams.getOrDefault(id, Team.NONE);
    }

    public void setTeam(UUID id, Team team) {
        if (team == Team.NONE) {
            this.splitTeams.remove(id);
        } else {
            this.splitTeams.put(id, team);
        }
    }

    public Team cycleTeam(UUID id) {
        Team current = this.getTeam(id);
        Team next = current == Team.NONE ? Team.RED : (current == Team.RED ? Team.BLUE : Team.NONE);
        this.setTeam(id, next);
        return next;
    }

    public void shuffleTeams() {
        ArrayList<UUID> online = new ArrayList<UUID>(this.members);
        Collections.shuffle(online);
        this.splitTeams.clear();
        for (int i = 0; i < online.size(); ++i) {
            this.splitTeams.put((UUID)online.get(i), i % 2 == 0 ? Team.RED : Team.BLUE);
        }
    }

    public Map<UUID, Team> getSplitTeams() {
        return this.splitTeams;
    }
}


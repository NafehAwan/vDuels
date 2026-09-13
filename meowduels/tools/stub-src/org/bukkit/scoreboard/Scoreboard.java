package org.bukkit.scoreboard;

public interface Scoreboard {
    org.bukkit.scoreboard.Objective getObjective(java.lang.String a0);
    org.bukkit.scoreboard.Team getTeam(java.lang.String a0);
    org.bukkit.scoreboard.Objective registerNewObjective(java.lang.String a0, org.bukkit.scoreboard.Criteria a1, java.lang.String a2);
    org.bukkit.scoreboard.Team registerNewTeam(java.lang.String a0);
}

package org.bukkit.scoreboard;

public interface Objective {
    void displayName(net.kyori.adventure.text.Component a0);
    org.bukkit.scoreboard.Score getScore(java.lang.String a0);
    void numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat a0);
    void setDisplaySlot(org.bukkit.scoreboard.DisplaySlot a0);
}

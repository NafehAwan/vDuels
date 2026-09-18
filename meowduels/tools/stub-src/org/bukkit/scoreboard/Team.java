package org.bukkit.scoreboard;

public interface Team {
    void addEntry(java.lang.String a0);
    boolean removeEntry(java.lang.String a0);
    void prefix(net.kyori.adventure.text.Component a0);
    void setColor(org.bukkit.ChatColor a0);
    void setPrefix(java.lang.String a0);
    void suffix(net.kyori.adventure.text.Component a0);
    java.util.Set<String> getEntries();
}

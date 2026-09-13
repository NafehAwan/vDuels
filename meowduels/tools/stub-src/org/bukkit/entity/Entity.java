package org.bukkit.entity;

public interface Entity {
    org.bukkit.Location getLocation();
    void remove();
    java.util.Set<String> getScoreboardTags();
}

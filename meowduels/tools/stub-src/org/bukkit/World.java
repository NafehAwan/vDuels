package org.bukkit;

public interface World {
    boolean equals(java.lang.Object a0);
    org.bukkit.block.Block getBlockAt(int a0, int a1, int a2);
    int getMaxHeight();
    int getMinHeight();
    java.lang.String getName();
    void setStorm(boolean a0);
    void setThundering(boolean a0);
    void setTime(long a0);
    void setWeatherDuration(int a0);
    java.util.Collection<org.bukkit.entity.Entity> getNearbyEntities(org.bukkit.Location a0, double a1, double a2, double a3);
    java.util.List<org.bukkit.entity.Player> getPlayers();
}

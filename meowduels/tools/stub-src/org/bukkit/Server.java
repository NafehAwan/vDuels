package org.bukkit;

public interface Server {
    org.bukkit.entity.Player getPlayer(java.util.UUID a0);
    org.bukkit.entity.Player getPlayerExact(java.lang.String a0);
    org.bukkit.plugin.PluginManager getPluginManager();
    org.bukkit.scheduler.BukkitScheduler getScheduler();
    java.util.Collection<? extends org.bukkit.entity.Player> getOnlinePlayers();
}

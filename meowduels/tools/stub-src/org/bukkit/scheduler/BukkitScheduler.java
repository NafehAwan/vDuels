package org.bukkit.scheduler;

public interface BukkitScheduler {
    org.bukkit.scheduler.BukkitTask runTask(org.bukkit.plugin.Plugin a0, java.lang.Runnable a1);
    org.bukkit.scheduler.BukkitTask runTaskLater(org.bukkit.plugin.Plugin a0, java.lang.Runnable a1, long a2);
    org.bukkit.scheduler.BukkitTask runTaskTimer(org.bukkit.plugin.Plugin a0, java.lang.Runnable a1, long a2, long a3);
}

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 */
package com.meowduels.listeners;

import com.meowduels.MeowDuels;
import com.meowduels.model.Arena;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class EventListener
implements Listener {
    private final MeowDuels plugin;

    public EventListener(MeowDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled=true)
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        if (!this.plugin.getEventManager().isPlaying(player.getUniqueId())) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
            Player pl = player;
            Arena arena = this.plugin.getEventManager().getArena();
            if (arena != null) {
                Location spawn;
                Location location = spawn = arena.getSpawn1() != null ? arena.getSpawn1() : arena.getSpawn2();
                if (spawn != null) {
                    pl.setFallDistance(0.0f);
                    pl.teleport(spawn);
                }
            }
            return;
        }
        if (!this.plugin.getEventManager().isRunning()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        UUID id = victim.getUniqueId();
        if (!this.plugin.getEventManager().isPlaying(id) || !this.plugin.getEventManager().isRunning()) {
            return;
        }
        event.setKeepInventory(false);
        event.setDeathMessage(null);
        Player killer = victim.getKiller();
        UUID killerId = killer != null ? killer.getUniqueId() : null;
        this.plugin.getEventManager().announceKill(id, killerId);
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            victim.spigot().respawn();
            this.plugin.getEventManager().onDeath(id);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.plugin.getEventManager().handleQuit(event.getPlayer().getUniqueId());
    }
}


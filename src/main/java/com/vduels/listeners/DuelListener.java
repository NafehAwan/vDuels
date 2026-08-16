package com.vduels.listeners;

import com.vduels.VDuels;
import com.vduels.model.ActiveDuel;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

/**
 * Duel combat rules. Players actually die (real death animation, totems pop
 * naturally); the death is what ends the round. Outside the FIGHTING phase they
 * are invulnerable, and quitting mid-duel forfeits.
 */
public class DuelListener implements Listener {

    private final VDuels plugin;

    public DuelListener(VDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ActiveDuel duel = plugin.getDuelManager().getDuel(player.getUniqueId());
        if (duel == null) {
            return;
        }
        // Only invulnerable before the fight and while a round is wrapping up.
        // During FIGHTING damage is left alone so lethal hits actually kill.
        if (duel.getState() != ActiveDuel.State.FIGHTING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        ActiveDuel duel = plugin.getDuelManager().getDuel(victim.getUniqueId());
        if (duel == null) {
            return;
        }
        // Keep the kit on the body, drop nothing, no death spam.
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);

        UUID loserId = victim.getUniqueId();
        // Respawn immediately (skip the death screen), then award the round.
        Bukkit.getScheduler().runTask(plugin, () -> {
            victim.spigot().respawn();
            plugin.getDuelManager().handleRoundLoss(loserId);
        });
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        ActiveDuel duel = plugin.getDuelManager().getDuel(player.getUniqueId());
        if (duel == null) {
            return;
        }
        Location spawn = player.getUniqueId().equals(duel.getPlayer1())
                ? duel.getArena().getSpawn1() : duel.getArena().getSpawn2();
        if (spawn != null) {
            event.setRespawnLocation(spawn);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        ActiveDuel duel = plugin.getDuelManager().getDuel(event.getPlayer().getUniqueId());
        if (duel == null || duel.getState() != ActiveDuel.State.STARTING) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        // Frozen body during the countdown, but the head can still look around.
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            Location frozen = from.clone();
            frozen.setYaw(to.getYaw());
            frozen.setPitch(to.getPitch());
            event.setTo(frozen);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player
                && plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.getDuelManager().isInDuel(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getSetupManager().cancel(player);
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            plugin.getDuelManager().handleDisconnect(player.getUniqueId());
        }
    }
}

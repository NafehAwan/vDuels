package com.vduels.listeners;

import com.vduels.VDuels;
import com.vduels.model.ActiveDuel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Duel combat rules: converts lethal damage into a round loss, keeps players
 * invulnerable outside the FIGHTING phase, and cleans up on disconnect.
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
        if (duel.getState() != ActiveDuel.State.FIGHTING) {
            event.setCancelled(true);
            return;
        }
        double finalDamage = event.getFinalDamage();
        if (player.getHealth() - finalDamage <= 0) {
            event.setCancelled(true);
            plugin.getDuelManager().handleRoundLoss(player.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player
                && plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            // Keep hunger stable during matches.
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

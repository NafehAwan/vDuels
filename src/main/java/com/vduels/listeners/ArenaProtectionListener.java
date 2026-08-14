package com.vduels.listeners;

import com.vduels.VDuels;
import com.vduels.model.ActiveDuel;
import com.vduels.model.Arena;
import com.vduels.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Enforces each arena's break/place rules during duels, records changed blocks
 * for regeneration, and stops non-admins from editing configured arenas when no
 * duel is in progress.
 */
public class ArenaProtectionListener implements Listener {

    private final VDuels plugin;

    public ArenaProtectionListener(VDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        ActiveDuel duel = plugin.getDuelManager().getDuel(player.getUniqueId());

        if (duel == null) {
            protectIdleArena(player, loc, event::setCancelled);
            return;
        }

        Arena arena = duel.getArena();
        if (!arena.contains(loc)) {
            event.setCancelled(true);
            return;
        }

        boolean placedThisMatch = wasPlacedThisMatch(duel, loc);
        if (arena.isAllowBreak() || (placedThisMatch && arena.isAllowRemoveAdded())) {
            duel.recordChange(loc, event.getBlock().getBlockData());
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        ActiveDuel duel = plugin.getDuelManager().getDuel(player.getUniqueId());

        if (duel == null) {
            protectIdleArena(player, loc, event::setCancelled);
            return;
        }

        Arena arena = duel.getArena();
        if (!arena.contains(loc)) {
            event.setCancelled(true);
            return;
        }

        if (arena.isAllowPlace()) {
            duel.recordChange(loc, event.getBlockReplacedState().getBlockData());
        } else {
            event.setCancelled(true);
        }
    }

    private boolean wasPlacedThisMatch(ActiveDuel duel, Location loc) {
        Location key = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        BlockData original = duel.getChangedBlocks().get(key);
        return original != null && original.getMaterial() == Material.AIR;
    }

    private void protectIdleArena(Player player, Location loc, java.util.function.Consumer<Boolean> cancel) {
        if (player.hasPermission("vduels.admin")) {
            return; // admins can build/edit arenas freely
        }
        Arena arena = plugin.getArenaManager().findArenaAt(loc);
        if (arena != null) {
            cancel.accept(true);
            player.sendMessage(Text.prefixed("&cYou cannot edit an arena here."));
        }
    }
}

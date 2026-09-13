/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.entity.AbstractWindCharge
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockBurnEvent
 *  org.bukkit.event.block.BlockFromToEvent
 *  org.bukkit.event.block.BlockIgniteEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.entity.CreatureSpawnEvent
 *  org.bukkit.event.entity.CreatureSpawnEvent$SpawnReason
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.bukkit.event.entity.EntityExplodeEvent
 *  org.bukkit.event.player.PlayerBucketEmptyEvent
 */
package com.meowduels.listeners;

import com.meowduels.MeowDuels;
import com.meowduels.managers.EventManager;
import com.meowduels.model.ActiveDuel;
import com.meowduels.model.Arena;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractWindCharge;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

public class ArenaProtectionListener
implements Listener {
    private final MeowDuels plugin;

    public ArenaProtectionListener(MeowDuels plugin) {
        this.plugin = plugin;
    }

    private boolean eventArenaAllow(Location loc, BlockData before) {
        EventManager em = this.plugin.getEventManager();
        if (loc != null && em.isRunning() && em.getArena() != null && em.getArena().contains(loc)) {
            em.recordChange(loc, before);
            return true;
        }
        return false;
    }

    @EventHandler(ignoreCancelled=true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        if (this.eventArenaAllow(loc, event.getBlock().getBlockData())) {
            return;
        }
        ActiveDuel duel = this.plugin.getDuelManager().getDuel(player.getUniqueId());
        if (duel == null) {
            this.protectIdleArena(player, loc, arg_0 -> event.setCancelled(arg_0.booleanValue()));
            return;
        }
        Arena arena = duel.getArena();
        if (!arena.contains(loc)) {
            event.setCancelled(true);
            return;
        }
        boolean placedThisMatch = this.wasPlacedThisMatch(duel, loc);
        if (arena.canBreak(loc) || placedThisMatch && arena.isAllowRemoveAdded()) {
            duel.recordChange(loc, event.getBlock().getBlockData());
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        if (this.eventArenaAllow(loc, event.getBlockReplacedState().getBlockData())) {
            return;
        }
        ActiveDuel duel = this.plugin.getDuelManager().getDuel(player.getUniqueId());
        if (duel == null) {
            this.protectIdleArena(player, loc, arg_0 -> event.setCancelled(arg_0.booleanValue()));
            return;
        }
        Arena arena = duel.getArena();
        if (!arena.contains(loc)) {
            event.setCancelled(true);
            return;
        }
        if (arena.canPlace(loc)) {
            duel.recordChange(loc, event.getBlockReplacedState().getBlockData());
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof AbstractWindCharge) {
            return;
        }
        Location center = event.getLocation();
        if (!this.plugin.getArenaManager().hasArenaInWorld(center.getWorld())) {
            return;
        }
        Arena arena = this.plugin.getArenaManager().findArenaAt(center);
        if (arena == null) {
            return;
        }
        EventManager em = this.plugin.getEventManager();
        if (em.isRunning() && em.getArena() != null && em.getArena().getName().equalsIgnoreCase(arena.getName())) {
            for (Block b : event.blockList()) {
                em.recordChange(b.getLocation(), b.getBlockData());
            }
            return;
        }
        ActiveDuel duel = this.activeDuelFor(arena);
        if (duel == null || !arena.isAllowExplosions()) {
            event.blockList().clear();
            event.setCancelled(true);
            return;
        }
        for (Block b : event.blockList()) {
            duel.recordChange(b.getLocation(), b.getBlockData());
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onBurn(BlockBurnEvent event) {
        if (!this.plugin.getArenaManager().hasArenaInWorld(event.getBlock().getLocation().getWorld())) {
            return;
        }
        Arena arena = this.plugin.getArenaManager().findArenaAt(event.getBlock().getLocation());
        if (arena == null) {
            return;
        }
        ActiveDuel duel = this.activeDuelFor(arena);
        if (duel == null || !arena.isAllowFireSpread()) {
            event.setCancelled(true);
            return;
        }
        duel.recordChange(event.getBlock().getLocation(), event.getBlock().getBlockData());
    }

    @EventHandler(ignoreCancelled=true)
    public void onIgnite(BlockIgniteEvent event) {
        if (!this.plugin.getArenaManager().hasArenaInWorld(event.getBlock().getLocation().getWorld())) {
            return;
        }
        Arena arena = this.plugin.getArenaManager().findArenaAt(event.getBlock().getLocation());
        if (arena == null) {
            return;
        }
        ActiveDuel duel = this.activeDuelFor(arena);
        if (duel == null || !arena.isAllowFireSpread()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onLiquidFlow(BlockFromToEvent event) {
        if (!this.plugin.getArenaManager().hasArenaInWorld(event.getToBlock().getLocation().getWorld())) {
            return;
        }
        Location toLoc = event.getToBlock().getLocation();
        Arena arena = this.plugin.getArenaManager().findArenaAt(toLoc);
        if (arena == null) {
            return;
        }
        if (this.eventArenaAllow(toLoc, event.getToBlock().getBlockData())) {
            return;
        }
        ActiveDuel duel = this.activeDuelFor(arena);
        if (duel == null || !arena.isAllowLiquidFlow()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }
        if (!this.plugin.getArenaManager().hasArenaInWorld(event.getLocation().getWorld())) {
            return;
        }
        Arena arena = this.plugin.getArenaManager().findArenaAt(event.getLocation());
        if (arena == null) {
            return;
        }
        if (!arena.isAllowMobSpawns()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Block block = event.getBlock();
        if (block == null) {
            return;
        }
        Location loc = block.getLocation();
        if (!this.plugin.getArenaManager().hasArenaInWorld(loc.getWorld())) {
            return;
        }
        if (this.eventArenaAllow(loc, block.getBlockData())) {
            return;
        }
        Player player = event.getPlayer();
        ActiveDuel duel = this.plugin.getDuelManager().getDuel(player.getUniqueId());
        if (duel == null) {
            this.protectIdleArena(player, loc, arg_0 -> event.setCancelled(arg_0.booleanValue()));
            return;
        }
        Arena arena = duel.getArena();
        if (!arena.contains(loc)) {
            event.setCancelled(true);
            return;
        }
        if (arena.canPlace(loc)) {
            duel.recordChange(loc, block.getBlockData());
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onEntityBurn(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Item) && !(entity instanceof Projectile)) {
            return;
        }
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.FIRE && cause != EntityDamageEvent.DamageCause.FIRE_TICK && cause != EntityDamageEvent.DamageCause.LAVA) {
            return;
        }
        Location loc = entity.getLocation();
        if (!this.plugin.getArenaManager().hasArenaInWorld(loc.getWorld())) {
            return;
        }
        if (this.plugin.getArenaManager().findArenaAt(loc) != null) {
            event.setCancelled(true);
        }
    }

    private ActiveDuel activeDuelFor(Arena arena) {
        for (Player online : this.plugin.getServer().getOnlinePlayers()) {
            ActiveDuel duel = this.plugin.getDuelManager().getDuel(online.getUniqueId());
            if (duel == null || duel.getArena() != arena) continue;
            return duel;
        }
        return null;
    }

    private boolean wasPlacedThisMatch(ActiveDuel duel, Location loc) {
        Location key = new Location(loc.getWorld(), (double)loc.getBlockX(), (double)loc.getBlockY(), (double)loc.getBlockZ());
        BlockData original = duel.getChangedBlocks().get(key);
        return original != null && original.getMaterial() == Material.AIR;
    }

    private void protectIdleArena(Player player, Location loc, Consumer<Boolean> cancel) {
        if (player.hasPermission("meowduels.admin")) {
            return;
        }
        if (!this.plugin.getArenaManager().hasArenaInWorld(loc.getWorld())) {
            return;
        }
        Arena arena = this.plugin.getArenaManager().findArenaAt(loc);
        if (arena != null) {
            cancel.accept(true);
            player.sendMessage(this.plugin.messages().get("arena.cannot-edit", new String[0]));
        }
    }
}


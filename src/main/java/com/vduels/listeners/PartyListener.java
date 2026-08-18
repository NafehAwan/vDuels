package com.vduels.listeners;

import com.vduels.VDuels;
import com.vduels.gui.PartyInfoMenu;
import com.vduels.gui.PartyMatchMenu;
import com.vduels.model.Party;
import com.vduels.model.PartyMatch;
import com.vduels.model.PartyTeamMatch;
import com.vduels.util.Items;
import com.vduels.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class PartyListener
implements Listener {
    private final VDuels plugin;

    public PartyListener(VDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        String tag = Items.readTag(item, this.plugin.keyButton());
        if (tag == null || !tag.startsWith("party-")) {
            return;
        }
        event.setCancelled(true);
        Party party = this.plugin.getPartyManager().getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(Text.color("&cYou're not in a party anymore - use /party create."));
            return;
        }
        switch (tag) {
            case "party-leave": {
                this.plugin.getPartyManager().leave(player);
                break;
            }
            case "party-settings": {
                new PartyInfoMenu(this.plugin, party, player).open(player);
                break;
            }
            case "party-gamemodes": {
                new PartyMatchMenu(this.plugin, party).open(player);
            }
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        PartyMatch match = this.plugin.getPartyManager().getMatch(player.getUniqueId());
        if (match != null) {
            if (match.getState() != PartyMatch.State.FIGHTING) {
                event.setCancelled(true);
            }
            return;
        }
        PartyTeamMatch teamMatch = this.plugin.getPartyManager().getTeamMatch(player.getUniqueId());
        if (teamMatch != null && teamMatch.getState() != PartyTeamMatch.State.FIGHTING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        PartyMatch match = this.plugin.getPartyManager().getMatch(victim.getUniqueId());
        if (match != null) {
            event.setKeepInventory(true);
            if (!match.isAllowDrops()) {
                event.getDrops().clear();
            }
            event.setDroppedExp(0);
            event.setDeathMessage(null);
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                victim.spigot().respawn();
                this.plugin.getPartyManager().handleElimination(victim.getUniqueId(), false);
            });
            return;
        }
        PartyTeamMatch teamMatch = this.plugin.getPartyManager().getTeamMatch(victim.getUniqueId());
        if (teamMatch != null) {
            event.setKeepInventory(true);
            if (!teamMatch.isAllowDrops()) {
                event.getDrops().clear();
            }
            event.setDroppedExp(0);
            event.setDeathMessage(null);
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                victim.spigot().respawn();
                this.plugin.getPartyManager().handleTeamElimination(victim.getUniqueId(), false);
            });
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PartyMatch match = this.plugin.getPartyManager().getMatch(player.getUniqueId());
        if (match != null && match.getArena().getSpawn1() != null) {
            event.setRespawnLocation(match.getArena().getSpawn1().clone().add(0.0, 3.0, 0.0));
            return;
        }
        PartyTeamMatch teamMatch = this.plugin.getPartyManager().getTeamMatch(player.getUniqueId());
        if (teamMatch != null && teamMatch.getArena().getSpawn1() != null) {
            event.setRespawnLocation(teamMatch.getArena().getSpawn1().clone().add(0.0, 3.0, 0.0));
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onMove(PlayerMoveEvent event) {
        boolean freeze;
        Player player = event.getPlayer();
        PartyMatch match = this.plugin.getPartyManager().getMatch(player.getUniqueId());
        boolean bl = freeze = match != null && match.getState() == PartyMatch.State.STARTING;
        if (!freeze) {
            PartyTeamMatch teamMatch = this.plugin.getPartyManager().getTeamMatch(player.getUniqueId());
            boolean bl2 = freeze = teamMatch != null && teamMatch.getState() == PartyTeamMatch.State.STARTING;
        }
        if (!freeze) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            Location frozen = from.clone();
            frozen.setYaw(to.getYaw());
            frozen.setPitch(to.getPitch());
            event.setTo(frozen);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onHunger(FoodLevelChangeEvent event) {
        HumanEntity humanEntity = event.getEntity();
        if (humanEntity instanceof Player) {
            Player player = (Player)humanEntity;
            if (this.plugin.getPartyManager().getMatch(player.getUniqueId()) != null || this.plugin.getPartyManager().getTeamMatch(player.getUniqueId()) != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        PartyMatch match = this.plugin.getPartyManager().getMatch(player.getUniqueId());
        if (match != null) {
            if (!match.isAllowDrops()) {
                event.setCancelled(true);
            }
            return;
        }
        PartyTeamMatch teamMatch = this.plugin.getPartyManager().getTeamMatch(player.getUniqueId());
        if (teamMatch != null && !teamMatch.isAllowDrops()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.plugin.getPartyManager().handleQuit(event.getPlayer().getUniqueId());
    }
}


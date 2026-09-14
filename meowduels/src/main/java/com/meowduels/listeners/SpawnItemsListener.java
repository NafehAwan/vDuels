/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.ItemStack
 */
package com.meowduels.listeners;

import com.meowduels.MeowDuels;
import com.meowduels.gui.KitEditorMenu;
import com.meowduels.gui.QueuePickMenu;
import com.meowduels.gui.SettingsMenu;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SpawnItemsListener
implements Listener {
    private final MeowDuels plugin;

    public SpawnItemsListener(MeowDuels plugin) {
        this.plugin = plugin;
    }

    private boolean isSpawnItem(ItemStack stack) {
        String btn = Items.readTag(stack, this.plugin.keyButton());
        return "spawn-queue".equals(btn) || "spawn-quick".equals(btn) || "spawn-leavequeue".equals(btn) || "spawn-kiteditor".equals(btn) || "spawn-settings".equals(btn) || "spawn-rematch".equals(btn);
    }

    @EventHandler(ignoreCancelled=true)
    public void onDrop(PlayerDropItemEvent event) {
        if (event.getItemDrop() != null && this.isSpawnItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (this.isSpawnItem(event.getCurrentItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        boolean left;
        Action action = event.getAction();
        boolean right = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        boolean bl = left = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        if (!right && !left) {
            return;
        }
        if (event.getItem() == null) {
            return;
        }
        String btn = Items.readTag(event.getItem(), this.plugin.keyButton());
        if (btn == null) {
            return;
        }
        Player player = event.getPlayer();
        if ("spawn-leavequeue".equals(btn)) {
            event.setCancelled(true);
            this.plugin.getQueueManager().leaveAll(player);
            return;
        }
        if (!right) {
            return;
        }
        if ("spawn-queue".equals(btn)) {
            event.setCancelled(true);
            if (this.busy(player)) {
                return;
            }
            new QueuePickMenu(this.plugin).open(player);
        } else if ("spawn-quick".equals(btn)) {
            event.setCancelled(true);
            if (this.busy(player)) {
                return;
            }
            this.plugin.getQueueManager().quickJoin(player);
        } else if ("spawn-kiteditor".equals(btn)) {
            event.setCancelled(true);
            if (this.busy(player)) {
                return;
            }
            // Everyone can arrange their own layouts. /kiteditor stays the
            // admin door to editing the kits themselves.
            new KitEditorMenu(this.plugin, true).open(player);
        } else if ("spawn-settings".equals(btn)) {
            event.setCancelled(true);
            new SettingsMenu(this.plugin, player).open(player);
        } else if ("spawn-rematch".equals(btn)) {
            event.setCancelled(true);
            if (this.busy(player)) {
                return;
            }
            this.plugin.getDuelManager().requestRematch(player);
        }
    }

    private boolean busy(Player p) {
        UUID id = p.getUniqueId();
        if (this.plugin.getDuelManager().isInDuel(id)) {
            p.sendMessage(Text.prefixed("&cYou're already in a duel."));
            return true;
        }
        if (this.plugin.getSpectateManager().isSpectating(id)) {
            p.sendMessage(Text.prefixed("&cYou're spectating - type &f/leave&c first."));
            return true;
        }
        if (this.plugin.getEventManager().isInvolved(id)) {
            p.sendMessage(Text.prefixed("&cYou're in an event - type &f/leave&c first."));
            return true;
        }
        return false;
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.AsyncPlayerChatEvent
 *  org.bukkit.plugin.Plugin
 */
package com.meowduels.listeners;

import com.meowduels.MeowDuels;
import com.meowduels.gui.ArenaMenu;
import com.meowduels.model.Arena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

public class SetupChatListener
implements Listener {
    private final MeowDuels plugin;

    public SetupChatListener(MeowDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (this.plugin.getSpectateManager().isSpectating(player.getUniqueId()) && message.equalsIgnoreCase(".leave")) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.plugin.getSpectateManager().stop(player));
            return;
        }
        if (this.plugin.getTabEditManager().isEditing(player.getUniqueId())) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.plugin.getTabEditManager().handleInput(player, message));
            return;
        }
        if (this.plugin.getSetupManager().inEventSpawn(player.getUniqueId())) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                Arena arena = this.plugin.getSetupManager().handleEventSpawnInput(player, message);
                if (arena != null) {
                    new ArenaMenu(this.plugin, arena).open(player);
                }
            });
            return;
        }
        if (this.plugin.getSetupManager().inBorder(player.getUniqueId())) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                Arena arena = this.plugin.getSetupManager().handleBorderInput(player, message);
                if (arena != null) {
                    new ArenaMenu(this.plugin, arena).open(player);
                }
            });
            return;
        }
        if (!this.plugin.getSetupManager().inSetup(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            Arena finished = this.plugin.getSetupManager().handleInput(player, message);
            if (finished != null) {
                new ArenaMenu(this.plugin, finished).open(player);
            }
        });
    }
}


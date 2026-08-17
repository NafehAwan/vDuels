package com.vduels.listeners;

import com.vduels.VDuels;
import com.vduels.gui.ArenaMenu;
import com.vduels.model.Arena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Feeds chat lines from players in the arena setup wizard to the SetupManager.
 * Chat fires async, so all game logic is bounced back onto the main thread.
 */
public class SetupChatListener implements Listener {

    private final VDuels plugin;

    public SetupChatListener(VDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Spectators leave with ".leave" in chat.
        if (plugin.getSpectateManager().isSpectating(player.getUniqueId())
                && message.equalsIgnoreCase(".leave")) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getSpectateManager().stop(player));
            return;
        }

        if (plugin.getTabEditManager().isEditing(player.getUniqueId())) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin,
                    () -> plugin.getTabEditManager().handleInput(player, message));
            return;
        }

        if (!plugin.getSetupManager().inSetup(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> {
            Arena finished = plugin.getSetupManager().handleInput(player, message);
            if (finished != null) {
                new ArenaMenu(plugin, finished).open(player);
            }
        });
    }
}

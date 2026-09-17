/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.InventoryHolder
 */
package com.meowtags;

import com.meowtags.Gradient;
import com.meowtags.MeowTags;
import com.meowtags.Tag;
import com.meowtags.TagMenuHolder;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class MenuListener
implements Listener {
    private final MeowTags plugin;

    public MenuListener(MeowTags plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof TagMenuHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getWhoClicked();
        int raw = event.getRawSlot();
        if (raw < 0 || raw >= 54) {
            return;
        }
        if (raw == 49) {
            if (this.plugin.tags().isInDuel(player)) {
                player.sendMessage(this.plugin.prefixed("<red>You can't change your tag during a duel."));
                return;
            }
            this.plugin.tags().clear(player);
            player.sendMessage(this.plugin.prefixed("<gray>Your tag has been removed."));
            player.closeInventory();
            return;
        }
        // Same list the menu was built from, or the slot index would point at a
        // different tag than the one the player clicked.
        List<Tag> tags = this.plugin.tags().available(player);
        if (raw >= tags.size() || raw >= 45) {
            return;
        }
        Tag tag = tags.get(raw);
        if (this.plugin.tags().isInDuel(player)) {
            player.sendMessage(this.plugin.prefixed("<red>You can't change your tag during a duel."));
            return;
        }
        if (!player.hasPermission(tag.permission())) {
            player.sendMessage(this.plugin.prefixed("<red>You don't have access to that tag."));
            return;
        }
        this.plugin.tags().apply(player, tag.id);
        player.sendMessage(this.plugin.prefixed("<gray>Tag set to </gray><bold><gradient:#" + MenuListener.strip(tag.hex1) + ":#" + MenuListener.strip(tag.hex2) + ">" + Gradient.smallCaps(tag.display) + "</gradient></bold><gray>.</gray>"));
        player.closeInventory();
    }

    private static String strip(String hex) {
        String h = hex.trim();
        return h.startsWith("#") ? h.substring(1) : h;
    }
}


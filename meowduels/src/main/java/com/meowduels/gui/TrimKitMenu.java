/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.ItemStack
 */
package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.gui.TrimMenu;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class TrimKitMenu
extends Menu {
    private final MeowDuels plugin;

    public TrimKitMenu(MeowDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public void build() {
        this.create(6, "&d&lTrims &7- Pick a kit");
        ItemStack filler = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; ++i) {
            this.inventory.setItem(i, filler);
        }
        int slot = 0;
        for (Kit kit : this.plugin.getKitManager().all()) {
            if (slot >= 54) break;
            Items icon = Items.of(kit.getIcon()).tag(this.plugin.keyKit(), kit.getName());
            if (kit.getDisplayName() != null && !kit.getDisplayName().isEmpty()) {
                icon.miniName(kit.getDisplayName());
            } else {
                icon.name("&e" + kit.getName());
            }
            icon.lore("&7Click to edit this kit's armor trims.");
            this.inventory.setItem(slot++, icon.build());
        }
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String kitName = Items.readTag(event.getCurrentItem(), this.plugin.keyKit());
        if (kitName != null && this.plugin.getKitManager().exists(kitName)) {
            new TrimMenu(this.plugin, kitName, player.getUniqueId()).open(player);
        }
    }
}


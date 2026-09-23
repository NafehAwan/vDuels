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
import com.meowduels.managers.TabEditManager;
import com.meowduels.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class TabConfigMenu
extends Menu {
    private final MeowDuels plugin;

    public TabConfigMenu(MeowDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public void build() {
        this.create(3, "&7&lTab Settings");
        ItemStack filler = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; ++i) {
            this.inventory.setItem(i, filler);
        }
        this.inventory.setItem(11, Items.of(Material.BOOK).name("&eTitle").lore("&7Current:", "&f" + this.plugin.getTabTitle(), "", "&aClick to change &7(MiniMessage / gradients)").tag(this.plugin.keyButton(), "title").build());
        this.inventory.setItem(13, Items.of(Material.PAPER).name("&dDiscord").lore("&7Current: &f" + this.plugin.getTabDiscord(), "", "&aClick to change").tag(this.plugin.keyButton(), "discord").build());
        this.inventory.setItem(15, Items.of(Material.GOLDEN_APPLE).name("&6Store").lore("&7Current: &f" + this.plugin.getTabStore(), "", "&aClick to change").tag(this.plugin.keyButton(), "store").build());
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        TabEditManager.Field field;
        String button = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if (button == null) {
            return;
        }
        // The decompiler split this switch into a shadow variable that was never
        // read; each branch must assign the `field` the code below actually uses.
        switch (button) {
            case "title": {
                field = TabEditManager.Field.TITLE;
                break;
            }
            case "discord": {
                field = TabEditManager.Field.DISCORD;
                break;
            }
            case "store": {
                field = TabEditManager.Field.STORE;
                break;
            }
            default: {
                field = null;
            }
        }
        if (field != null) {
            player.closeInventory();
            this.plugin.getTabEditManager().begin(player, field);
        }
    }
}


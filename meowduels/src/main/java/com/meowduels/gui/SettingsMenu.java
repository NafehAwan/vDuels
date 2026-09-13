/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.ItemStack
 */
package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.gui.TrimKitMenu;
import com.meowduels.util.Items;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class SettingsMenu
extends Menu {
    private static final String DUEL = "set-duelreq";
    private static final String BOARD = "set-scoreboard";
    private static final String TRIMS = "set-trims";
    private final MeowDuels plugin;
    private final UUID owner;

    public SettingsMenu(MeowDuels plugin, Player player) {
        this.plugin = plugin;
        this.owner = player.getUniqueId();
    }

    @Override
    public void build() {
        this.create(1, "&b&lSettings");
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; ++i) {
            this.inventory.setItem(i, filler);
        }
        boolean requests = this.plugin.getPlayerSettings().isDuelRequests(this.owner);
        boolean board = this.plugin.getPlayerSettings().isScoreboard(this.owner);
        this.inventory.setItem(2, Items.of(Material.IRON_SWORD).miniName("<b><gradient:#A0E9FF:#4C7DF0>\u1d05\u1d1c\u1d07\u029f \u0280\u1d07\u01eb\u1d1c\u1d07\u0455\u1d1b\u0455</gradient></b>").lore(requests ? "&aEnabled &7- players can challenge you" : "&cDisabled &7- challenges are blocked", "", "&eClick to toggle.").glow(requests).hideTooltip().tag(this.plugin.keyButton(), DUEL).build());
        this.inventory.setItem(4, Items.of(Material.PAPER).miniName("<b><gradient:#B5F5C8:#3FBF6F>\u0455\u1d04\u1d0f\u0280\u1d07\u0299\u1d0f\u1d00\u0280\u1d05</gradient></b>").lore(board ? "&aShown" : "&cHidden", "", "&eClick to toggle the sidebar.").glow(board).hideTooltip().tag(this.plugin.keyButton(), BOARD).build());
        Player p = Bukkit.getPlayer((UUID)this.owner);
        if (p != null && p.hasPermission("meowduels.trims")) {
            this.inventory.setItem(6, Items.of(Material.LEATHER_CHESTPLATE).miniName("<b><gradient:#FFD98E:#E0A13B>\u1d0b\u026a\u1d1b \u1d1b\u0280\u026a\u1d0d\u0455</gradient></b>").lore("&7Set your armor trims per kit.", "", "&eClick to open.").hideTooltip().tag(this.plugin.keyButton(), TRIMS).build());
        }
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String btn = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if (btn == null) {
            return;
        }
        if (DUEL.equals(btn)) {
            this.plugin.getPlayerSettings().toggleDuelRequests(this.owner);
            this.reopen(player);
        } else if (BOARD.equals(btn)) {
            this.plugin.getPlayerSettings().toggleScoreboard(this.owner);
            this.reopen(player);
        } else if (TRIMS.equals(btn)) {
            if (!player.hasPermission("meowduels.trims")) {
                return;
            }
            new TrimKitMenu(this.plugin).open(player);
        }
    }

    private void reopen(Player player) {
        this.build();
        player.openInventory(this.inventory);
    }
}


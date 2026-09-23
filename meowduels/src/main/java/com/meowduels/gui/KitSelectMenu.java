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
import com.meowduels.gui.ArenaMenu;
import com.meowduels.gui.Menu;
import com.meowduels.model.Arena;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class KitSelectMenu
extends Menu {
    private final MeowDuels plugin;
    private final Arena arena;
    private List<Kit> kits;

    public KitSelectMenu(MeowDuels plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    @Override
    public void build() {
        this.create(6, "&8Compatible Kits: &d" + this.arena.getName());
        this.kits = this.plugin.getKitManager().all();
        if (this.kits.isEmpty()) {
            this.inventory.setItem(22, Items.of(Material.BARRIER).name("&cNo kits created yet").lore("&7Use &e/kitcreate <name>&7 first.").build());
        }
        for (int i = 0; i < this.kits.size() && i < 45; ++i) {
            Kit kit = this.kits.get(i);
            boolean enabled = this.arena.supportsKitExplicit(kit.getName());
            this.inventory.setItem(i, Items.of(kit.getIcon()).name((enabled ? "&a" : "&7") + kit.getName()).lore("", enabled ? "&aENABLED &8(click to disable)" : "&cDISABLED &8(click to enable)").glow(enabled).build());
        }
        ItemStack filler = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 45; i < 54; ++i) {
            this.inventory.setItem(i, filler);
        }
        this.inventory.setItem(49, Items.of(Material.ARROW).name("&eBack").lore("&7Return to the arena menu.").build());
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 49) {
            new ArenaMenu(this.plugin, this.arena).open(player);
            return;
        }
        if (slot < 0 || slot >= 45 || this.kits == null || slot >= this.kits.size()) {
            return;
        }
        Kit kit = this.kits.get(slot);
        if (this.arena.supportsKitExplicit(kit.getName())) {
            this.arena.getKits().removeIf(k -> k.equalsIgnoreCase(kit.getName()));
        } else {
            this.arena.getKits().add(kit.getName());
        }
        this.plugin.getArenaManager().save();
        this.build();
        player.openInventory(this.inventory);
    }
}


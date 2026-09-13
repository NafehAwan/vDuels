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
import com.meowduels.gui.DuelConfirmMenu;
import com.meowduels.gui.Menu;
import com.meowduels.model.Arena;
import com.meowduels.util.Items;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class MapSelectMenu
extends Menu {
    private final MeowDuels plugin;
    private final DuelConfirmMenu confirm;

    public MapSelectMenu(MeowDuels plugin, DuelConfirmMenu confirm) {
        this.plugin = plugin;
        this.confirm = confirm;
    }

    @Override
    public void build() {
        this.create(4, "&8DUEL MAP: &d" + this.confirm.getTarget().getName());
        String kit = this.confirm.getSelectedKit();
        if (this.plugin.getGuiLayoutManager().has("mapselect")) {
            for (Map.Entry<Integer, ItemStack> e : this.plugin.getGuiLayoutManager().get("mapselect").entrySet()) {
                if (e.getKey() >= 36) continue;
                if ("random".equals(Items.readTag(e.getValue(), this.plugin.keyButton()))) {
                    this.inventory.setItem(e.getKey().intValue(), this.randomButton());
                    continue;
                }
                String arenaName = Items.readTag(e.getValue(), this.plugin.keyArena());
                if (arenaName != null) {
                    ItemStack icon = this.liveArenaIcon(arenaName, kit);
                    if (icon == null) continue;
                    this.inventory.setItem(e.getKey().intValue(), icon);
                    continue;
                }
                this.inventory.setItem(e.getKey().intValue(), e.getValue());
            }
        } else {
            ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
            for (int i = 0; i < 36; ++i) {
                this.inventory.setItem(i, filler);
            }
            int slot = 0;
            for (Arena arena : this.plugin.getArenaManager().all()) {
                if (slot >= 35) break;
                if (!arena.isConfigured() || kit != null && !arena.supportsKit(kit)) continue;
                this.inventory.setItem(slot++, this.liveArenaIcon(arena.getName(), kit));
            }
            this.inventory.setItem(35, this.randomButton());
        }
    }

    private ItemStack randomButton() {
        boolean sel = this.confirm.getSelectedArena() == null;
        return Items.of(Material.ENDER_PEARL).name((sel ? "&a" : "&e") + "Random").lore("", sel ? "&aSelected" : "&fPick any free compatible arena").glow(sel).tag(this.plugin.keyButton(), "random").build();
    }

    private ItemStack liveArenaIcon(String name, String kit) {
        Arena arena = this.plugin.getArenaManager().get(name);
        if (arena == null || !arena.isConfigured()) {
            return null;
        }
        boolean sel = name.equalsIgnoreCase(this.confirm.getSelectedArena());
        boolean compatible = kit == null || arena.supportsKit(kit);
        return Items.of(Material.FILLED_MAP).name((sel ? "&a" : (compatible ? "&e" : "&c")) + arena.getName()).lore("", sel ? "&aSelected" : (compatible ? "&fClick to pick this map" : "&cIncompatible with this kit")).glow(sel).tag(this.plugin.keyArena(), arena.getName()).build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if ("random".equals(Items.readTag(clicked, this.plugin.keyButton()))) {
            this.confirm.setSelectedArena(null);
            this.confirm.reopen(player);
            return;
        }
        String arenaName = Items.readTag(clicked, this.plugin.keyArena());
        if (arenaName == null) {
            return;
        }
        Arena arena = this.plugin.getArenaManager().get(arenaName);
        String kit = this.confirm.getSelectedKit();
        if (arena == null || !arena.isConfigured()) {
            return;
        }
        if (kit != null && !arena.supportsKit(kit)) {
            player.sendMessage(this.plugin.messages().get("menu.arena-incompatible", new String[0]));
            return;
        }
        this.confirm.setSelectedArena(arena.getName());
        this.confirm.reopen(player);
    }
}


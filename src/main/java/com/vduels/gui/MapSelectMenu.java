package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.managers.GuiLayoutManager;
import com.vduels.model.Arena;
import com.vduels.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * The arena picker (DUEL MAP, small chest) opened from DUEL CONFIRM. Clicking an
 * arena (or Random) selects it and returns to DUEL CONFIRM. Layout is editable
 * with {@code /editgui mapselect}.
 */
public class MapSelectMenu extends Menu {

    private final VDuels plugin;
    private final DuelConfirmMenu confirm;

    public MapSelectMenu(VDuels plugin, DuelConfirmMenu confirm) {
        this.plugin = plugin;
        this.confirm = confirm;
    }

    @Override
    public void build() {
        create(4, "&8DUEL MAP: &b" + confirm.getTarget().getName());
        String kit = confirm.getSelectedKit();

        if (plugin.getGuiLayoutManager().has(GuiLayoutManager.MAP_SELECT)) {
            for (Map.Entry<Integer, ItemStack> e : plugin.getGuiLayoutManager().get(GuiLayoutManager.MAP_SELECT).entrySet()) {
                if (e.getKey() >= 36) {
                    continue;
                }
                if ("random".equals(Items.readTag(e.getValue(), plugin.keyButton()))) {
                    inventory.setItem(e.getKey(), randomButton());
                    continue;
                }
                String arenaName = Items.readTag(e.getValue(), plugin.keyArena());
                if (arenaName != null) {
                    ItemStack icon = liveArenaIcon(arenaName, kit);
                    if (icon != null) {
                        inventory.setItem(e.getKey(), icon);
                    }
                } else {
                    inventory.setItem(e.getKey(), e.getValue());
                }
            }
        } else {
            int slot = 0;
            for (Arena arena : plugin.getArenaManager().all()) {
                if (slot >= 35) {
                    break;
                }
                if (arena.isConfigured() && (kit == null || arena.supportsKit(kit))) {
                    inventory.setItem(slot++, liveArenaIcon(arena.getName(), kit));
                }
            }
            inventory.setItem(35, randomButton());
        }
    }

    private ItemStack randomButton() {
        boolean sel = confirm.getSelectedArena() == null;
        return Items.of(Material.ENDER_PEARL)
                .name((sel ? "&a" : "&e") + "Random")
                .lore("", sel ? "&aSelected" : "&fPick any free compatible arena")
                .glow(sel)
                .tag(plugin.keyButton(), "random")
                .build();
    }

    private ItemStack liveArenaIcon(String name, String kit) {
        Arena arena = plugin.getArenaManager().get(name);
        if (arena == null || !arena.isConfigured()) {
            return null;
        }
        boolean sel = name.equalsIgnoreCase(confirm.getSelectedArena());
        boolean compatible = kit == null || arena.supportsKit(kit);
        return Items.of(Material.FILLED_MAP)
                .name((sel ? "&a" : compatible ? "&e" : "&c") + arena.getName())
                .lore("", sel ? "&aSelected" : compatible ? "&fClick to pick this map" : "&cIncompatible with this kit")
                .glow(sel)
                .tag(plugin.keyArena(), arena.getName())
                .build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if ("random".equals(Items.readTag(clicked, plugin.keyButton()))) {
            confirm.setSelectedArena(null);
            confirm.reopen(player);
            return;
        }
        String arenaName = Items.readTag(clicked, plugin.keyArena());
        if (arenaName == null) {
            return;
        }
        Arena arena = plugin.getArenaManager().get(arenaName);
        String kit = confirm.getSelectedKit();
        if (arena == null || !arena.isConfigured()) {
            return;
        }
        if (kit != null && !arena.supportsKit(kit)) {
            player.sendMessage(plugin.messages().get("menu.arena-incompatible"));
            return;
        }
        confirm.setSelectedArena(arena.getName());
        confirm.reopen(player);
    }
}

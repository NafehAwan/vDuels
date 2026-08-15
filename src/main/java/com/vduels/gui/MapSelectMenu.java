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
 * The arena picker (DUEL MAP, small chest). Clicking an arena (or Random)
 * selects it; the admin-placed &quot;next&quot; arrow returns to DUEL CONFIRM.
 * Layout is editable with {@code /editgui mapselect}.
 */
public class MapSelectMenu extends Menu {

    private final VDuels plugin;
    private final DuelConfirmMenu confirm;

    public MapSelectMenu(VDuels plugin, DuelConfirmMenu confirm) {
        this.plugin = plugin;
        this.confirm = confirm;
    }

    public void reopen(Player player) {
        build();
        player.openInventory(inventory);
    }

    @Override
    public void build() {
        create(3, "&8DUEL MAP: &b" + confirm.getTarget().getName());
        String kit = confirm.getSelectedKit();

        if (plugin.getGuiLayoutManager().has(GuiLayoutManager.MAP_SELECT)) {
            for (Map.Entry<Integer, ItemStack> e : plugin.getGuiLayoutManager().get(GuiLayoutManager.MAP_SELECT).entrySet()) {
                if (e.getKey() >= 27) {
                    continue;
                }
                String button = Items.readTag(e.getValue(), plugin.keyButton());
                if (button != null) {
                    inventory.setItem(e.getKey(), renderButton(button));
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
                if (slot >= 25) {
                    break;
                }
                if (arena.isConfigured() && (kit == null || arena.supportsKit(kit))) {
                    inventory.setItem(slot++, liveArenaIcon(arena.getName(), kit));
                }
            }
            inventory.setItem(25, renderButton("random"));
            inventory.setItem(26, renderButton("next"));
        }
    }

    private ItemStack renderButton(String id) {
        if (id.equals("next")) {
            return Items.of(Material.ARROW)
                    .name("&eNext")
                    .lore("&fBack to the duel menu.")
                    .tag(plugin.keyButton(), "next")
                    .build();
        }
        if (id.equals("random")) {
            boolean sel = confirm.getSelectedArena() == null;
            return Items.of(Material.ENDER_PEARL)
                    .name((sel ? "&a" : "&e") + "Random")
                    .lore("", sel ? "&aSelected" : "&fPick any free compatible arena")
                    .glow(sel)
                    .tag(plugin.keyButton(), "random")
                    .build();
        }
        return Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
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
        String button = Items.readTag(clicked, plugin.keyButton());
        if ("next".equals(button)) {
            confirm.reopen(player);
            return;
        }
        if ("random".equals(button)) {
            confirm.setSelectedArena(null);
            reopen(player);
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
        reopen(player);
    }
}

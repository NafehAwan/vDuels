package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.managers.GuiLayoutManager;
import com.vduels.model.Arena;
import com.vduels.util.Items;
import com.vduels.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * The arena picker (DUEL MAP) opened from DUEL CONFIRM. Arena icons and
 * decoration can be arranged with {@code /editgui mapselect}; the Random and
 * Back buttons stay on the fixed bottom row.
 */
public class MapSelectMenu extends Menu {

    private static final int SLOT_RANDOM = 47;
    private static final int SLOT_BACK = 49;

    private final VDuels plugin;
    private final DuelConfirmMenu confirm;

    public MapSelectMenu(VDuels plugin, DuelConfirmMenu confirm) {
        this.plugin = plugin;
        this.confirm = confirm;
    }

    @Override
    public void build() {
        create(6, "&8DUEL MAP: &b" + confirm.getTarget().getName());
        String kit = confirm.getSelectedKit();

        if (plugin.getGuiLayoutManager().has(GuiLayoutManager.MAP_SELECT)) {
            for (Map.Entry<Integer, ItemStack> e : plugin.getGuiLayoutManager().get(GuiLayoutManager.MAP_SELECT).entrySet()) {
                if (e.getKey() >= 45) {
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
                if (slot >= 45) {
                    break;
                }
                if (arena.isConfigured() && (kit == null || arena.supportsKit(kit))) {
                    inventory.setItem(slot++, liveArenaIcon(arena.getName(), kit));
                }
            }
        }

        ItemStack filler = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }
        boolean randomSel = confirm.getSelectedArena() == null;
        inventory.setItem(SLOT_RANDOM, Items.of(Material.ENDER_PEARL)
                .name((randomSel ? "&a" : "&e") + "Random")
                .lore("", randomSel ? "&aSelected" : "&7Pick any free compatible arena")
                .glow(randomSel)
                .build());
        inventory.setItem(SLOT_BACK, Items.of(Material.ARROW)
                .name("&eBack").lore("&7Return to the duel menu.").build());
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
                .lore("", sel ? "&aSelected" : compatible ? "&7Click to pick this map" : "&cIncompatible with this kit")
                .glow(sel)
                .tag(plugin.keyArena(), arena.getName())
                .build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == SLOT_BACK) {
            confirm.reopen(player);
            return;
        }
        if (slot == SLOT_RANDOM) {
            confirm.setSelectedArena(null);
            confirm.reopen(player);
            return;
        }
        String arenaName = Items.readTag(event.getCurrentItem(), plugin.keyArena());
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

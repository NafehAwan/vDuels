package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.model.Arena;
import com.vduels.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The arena picker opened from the DUEL CONFIRM menu (DUEL MAP). Lists arenas
 * that are configured and compatible with the chosen kit, plus a "Random"
 * option. Clicking one selects it and returns to the confirm menu.
 */
public class MapSelectMenu extends Menu {

    private static final int SLOT_RANDOM = 47;
    private static final int SLOT_BACK = 49;

    private final VDuels plugin;
    private final DuelConfirmMenu confirm;
    private List<Arena> shown = new ArrayList<>();

    public MapSelectMenu(VDuels plugin, DuelConfirmMenu confirm) {
        this.plugin = plugin;
        this.confirm = confirm;
    }

    @Override
    public void build() {
        create(6, "&8DUEL MAP: &b" + confirm.getTarget().getName());

        String kit = confirm.getSelectedKit();
        shown = new ArrayList<>();
        for (Arena arena : plugin.getArenaManager().all()) {
            if (arena.isConfigured() && (kit == null || arena.supportsKit(kit))) {
                shown.add(arena);
            }
        }

        for (int i = 0; i < shown.size() && i < 45; i++) {
            Arena arena = shown.get(i);
            boolean sel = arena.getName().equalsIgnoreCase(confirm.getSelectedArena());
            inventory.setItem(i, Items.of(Material.FILLED_MAP)
                    .name((sel ? "&a" : "&e") + arena.getName())
                    .lore("", sel ? "&aSelected" : "&7Click to pick this map")
                    .glow(sel)
                    .build());
        }
        if (shown.isEmpty()) {
            inventory.setItem(22, Items.of(Material.BARRIER)
                    .name("&cNo compatible arenas")
                    .lore("&7No configured arena supports this kit.")
                    .build());
        }

        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
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
                .name("&eBack")
                .lore("&7Return to the duel menu.")
                .build());
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
        if (slot >= 0 && slot < shown.size() && slot < 45) {
            confirm.setSelectedArena(shown.get(slot).getName());
            confirm.reopen(player);
        }
    }
}

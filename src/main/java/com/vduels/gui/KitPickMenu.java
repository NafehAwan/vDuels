package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.managers.MenuLayoutManager;
import com.vduels.model.Kit;
import com.vduels.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * The kit picker opened from the DUEL CONFIRM menu. Uses the admin-customised
 * layout (see {@code /adminduel}) if one exists, otherwise a default grid.
 * Clicking a kit selects it and returns to the confirm menu.
 */
public class KitPickMenu extends Menu {

    private final VDuels plugin;
    private final DuelConfirmMenu confirm;

    public KitPickMenu(VDuels plugin, DuelConfirmMenu confirm) {
        this.plugin = plugin;
        this.confirm = confirm;
    }

    @Override
    public void build() {
        create(6, "&8Select a Kit");

        if (plugin.getMenuLayoutManager().hasLayout()) {
            Map<Integer, ItemStack> layout = plugin.getMenuLayoutManager().getLayout();
            for (Map.Entry<Integer, ItemStack> entry : layout.entrySet()) {
                inventory.setItem(entry.getKey(), entry.getValue());
            }
        } else {
            int slot = 0;
            for (Kit kit : plugin.getKitManager().all()) {
                if (slot >= MenuLayoutManager.EDITABLE_SLOTS) {
                    break;
                }
                inventory.setItem(slot++, Items.of(kit.getIcon())
                        .name("&e" + kit.getName())
                        .lore("", "&7Click to select")
                        .tag(plugin.keyKit(), kit.getName())
                        .build());
            }
        }

        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(49, Items.of(Material.ARROW)
                .name("&eBack")
                .lore("&7Return to the duel menu.")
                .build());
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 49) {
            confirm.reopen(player);
            return;
        }
        if (slot < 0 || slot >= MenuLayoutManager.EDITABLE_SLOTS) {
            return;
        }
        String kitName = Items.readTag(event.getCurrentItem(), plugin.keyKit());
        if (kitName != null && plugin.getKitManager().exists(kitName)) {
            confirm.setSelectedKit(kitName);
            confirm.reopen(player);
        }
    }
}

package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.model.Kit;
import com.vduels.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * {@code /editkit} with no kit named: lists every kit so an admin can pick one
 * to edit its starting effects.
 */
public class EditKitListMenu extends Menu {

    // Inner slots of a 6-row chest (rows 2-5, columns 2-8): up to 28 kits.
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final VDuels plugin;

    public EditKitListMenu(VDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public void build() {
        create(6, "&7&lEdit Kit");
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }
        List<Kit> kits = plugin.getKitManager().all();
        for (int i = 0; i < SLOTS.length && i < kits.size(); i++) {
            inventory.setItem(SLOTS[i], icon(kits.get(i)));
        }
    }

    private ItemStack icon(Kit kit) {
        int effects = kit.getStartEffects().size();
        Items item = Items.of(kit.getIcon())
                .lore("", "&7Start effects: &f" + effects, "&aClick to edit.")
                .hideTooltip()
                .tag(plugin.keyKit(), kit.getName());
        if (kit.getDisplayName() != null && !kit.getDisplayName().isEmpty()) {
            item.miniName(kit.getDisplayName());
        } else {
            item.name("&e" + kit.getName());
        }
        return item.build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String kitName = Items.readTag(event.getCurrentItem(), plugin.keyKit());
        if (kitName == null) {
            return;
        }
        Kit kit = plugin.getKitManager().get(kitName);
        if (kit != null) {
            new EditKitEffectsMenu(plugin, kit).open(player);
        }
    }
}

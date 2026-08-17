package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.managers.CategoryManager;
import com.vduels.model.Kit;
import com.vduels.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The kit picker (small chest, 4 rows) opened first by {@code /duel}. Kits flow
 * live into the middle two rows (inner columns), so adding a kit to a category
 * shows up immediately with no editing. The rest is gray glass, with the
 * category arrow pinned to the bottom-right when more than one category exists.
 */
public class KitPickMenu extends Menu {

    private static final int ARROW_SLOT = 35;
    // Rows 2 and 3, columns 2-8 (inner slots) - where kits are shown.
    private static final int[] KIT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    private final VDuels plugin;
    private final DuelConfirmMenu confirm;
    private int categoryIndex = 0;

    public KitPickMenu(VDuels plugin, DuelConfirmMenu confirm) {
        this.plugin = plugin;
        this.confirm = confirm;
    }

    @Override
    public void build() {
        List<CategoryManager.Category> categories = plugin.getCategoryManager().all();

        List<String> kitNames;
        boolean multi = false;
        if (categories.isEmpty()) {
            // Bold gray "Duel Request", no category to append.
            create(4, "&7&lDuel Request");
            kitNames = new ArrayList<>();
            for (Kit kit : plugin.getKitManager().all()) {
                kitNames.add(kit.getName());
            }
        } else {
            if (categoryIndex >= categories.size()) {
                categoryIndex = 0;
            }
            CategoryManager.Category category = categories.get(categoryIndex);
            multi = categories.size() > 1;
            // Bold gray "Duel Request", then the category name in gray, not bold.
            create(4, "&7&lDuel Request&r &7" + category.getHeader());
            kitNames = plugin.getCategoryManager().kitsFor(category);
        }

        // Gray glass background everywhere.
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }

        // Kits flow into the fixed inner slots, in list order.
        for (int i = 0; i < KIT_SLOTS.length && i < kitNames.size(); i++) {
            ItemStack icon = kitIcon(kitNames.get(i));
            if (icon != null) {
                inventory.setItem(KIT_SLOTS[i], icon);
            }
        }

        if (multi) {
            inventory.setItem(ARROW_SLOT, arrowItem());
        }
    }

    private ItemStack kitIcon(String name) {
        Kit kit = plugin.getKitManager().get(name);
        if (kit == null) {
            return null;
        }
        // Hover shows nothing but the kit's name (its display name when set) -
        // no lore, no item stats.
        Items item = Items.of(kit.getIcon())
                .hideTooltip()
                .tag(plugin.keyKit(), kit.getName());
        if (kit.getDisplayName() != null && !kit.getDisplayName().isEmpty()) {
            item.miniName(kit.getDisplayName());
        } else {
            item.name("&e" + kit.getName());
        }
        return item.build();
    }

    private ItemStack arrowItem() {
        return Items.of(Material.ARROW)
                .name("&eNext Category")
                .lore("&f→ another category")
                .tag(plugin.keyButton(), "next-cat")
                .build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if ("next-cat".equals(Items.readTag(clicked, plugin.keyButton()))) {
            int size = plugin.getCategoryManager().all().size();
            if (size > 0) {
                categoryIndex = (categoryIndex + 1) % size;
                build();
                player.openInventory(inventory);
            }
            return;
        }
        String kitName = Items.readTag(clicked, plugin.keyKit());
        if (kitName != null && plugin.getKitManager().exists(kitName)) {
            confirm.setSelectedKit(kitName);
            confirm.reopen(player);
        }
    }
}

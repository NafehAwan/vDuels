package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.managers.CategoryManager;
import com.vduels.managers.QueueManager;
import com.vduels.model.Kit;
import com.vduels.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The 1v1 queue picker ({@code /queue}). Same fixed template as the duel kit
 * menu - kits flow into the middle two rows, black glass elsewhere, the category
 * arrow pinned to the bottom-right - but each kit shows its live queue/dueling
 * counts and left-clicking joins that kit's queue.
 */
public class QueuePickMenu extends Menu {

    private static final int ARROW_SLOT = 35;
    private static final int[] KIT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    private final VDuels plugin;
    private int categoryIndex = 0;

    public QueuePickMenu(VDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public void build() {
        List<CategoryManager.Category> categories = plugin.getQueueCategoryManager().all();

        List<String> kitNames;
        boolean multi = false;
        if (categories.isEmpty()) {
            create(4, "&7&lQueue");
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
            create(4, "&7&lQueue&r &7" + category.getHeader());
            kitNames = plugin.getQueueCategoryManager().kitsFor(category);
        }

        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }

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
        int queued = plugin.getQueueManager().queued(name);
        int dueling = plugin.getQueueManager().dueling(name);
        Items item = Items.of(kit.getIcon())
                .lore(
                        "&6🗡 &fIN QUEUE: &6" + queued + "&7/&6" + QueueManager.NEEDED,
                        "&6🗡 &fDUELING: &6" + dueling,
                        "",
                        "&7LMB TO &aJOIN")
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
            int size = plugin.getQueueCategoryManager().all().size();
            if (size > 0) {
                categoryIndex = (categoryIndex + 1) % size;
                build();
                player.openInventory(inventory);
            }
            return;
        }
        String kitName = Items.readTag(clicked, plugin.keyKit());
        if (kitName != null && plugin.getKitManager().exists(kitName)) {
            player.closeInventory();
            plugin.getQueueManager().join(player, kitName);
        }
    }
}

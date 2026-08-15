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
 * The kit picker (small chest) opened first by {@code /duel}. When categories
 * exist the title is "DUELS &rarr; &lt;header&gt;" and the arrow cycles between
 * them; with no categories it just shows every kit. Clicking a kit opens DUEL
 * CONFIRM.
 */
public class KitPickMenu extends Menu {

    private static final int ARROW_SLOT = 26;

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
        boolean hasCategories = !categories.isEmpty();

        List<String> kitNames;
        boolean multi = false;
        if (hasCategories) {
            if (categoryIndex >= categories.size()) {
                categoryIndex = 0;
            }
            CategoryManager.Category category = categories.get(categoryIndex);
            create(3, "&8DUELS &8→ &7" + category.getHeader());
            kitNames = plugin.getCategoryManager().kitsFor(category);
            multi = categories.size() > 1;
        } else {
            create(3, "&8DUELS");
            kitNames = new ArrayList<>();
            for (Kit kit : plugin.getKitManager().all()) {
                kitNames.add(kit.getName());
            }
        }

        ItemStack filler = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        int limit = multi ? ARROW_SLOT : 27;
        int slot = 0;
        for (String kitName : kitNames) {
            if (slot >= limit) {
                break;
            }
            ItemStack icon = kitIcon(kitName);
            if (icon != null) {
                inventory.setItem(slot++, icon);
            }
        }

        if (multi) {
            inventory.setItem(ARROW_SLOT, Items.of(Material.ARROW)
                    .name("&eNext Category")
                    .lore("&f→ another category")
                    .tag(plugin.keyButton(), "next-cat")
                    .build());
        }
    }

    private ItemStack kitIcon(String name) {
        Kit kit = plugin.getKitManager().get(name);
        if (kit == null) {
            return null;
        }
        return Items.of(kit.getIcon())
                .name("&e" + kit.getName())
                .lore("", "&fClick to select")
                .tag(plugin.keyKit(), kit.getName())
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

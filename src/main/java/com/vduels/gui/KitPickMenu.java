package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.managers.CategoryManager;
import com.vduels.managers.GuiLayoutManager;
import com.vduels.model.Kit;
import com.vduels.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The kit picker (small chest) opened first by {@code /duel}. When categories
 * exist the title is "DUELS &rarr; &lt;header&gt;" and the arrow cycles between
 * them; with no categories it just shows every kit. Clicking a kit opens DUEL
 * CONFIRM.
 */
public class KitPickMenu extends Menu {

    private static final int ARROW_SLOT = 35;

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

        if (categories.isEmpty()) {
            create(4, "&7DUELS");
            List<String> all = new ArrayList<>();
            for (Kit kit : plugin.getKitManager().all()) {
                all.add(kit.getName());
            }
            // Admins can decorate this default menu with /adminduel.
            if (plugin.getGuiLayoutManager().has(GuiLayoutManager.KIT_MENU)) {
                renderLayout(GuiLayoutManager.KIT_MENU);
            } else {
                grid(all, false);
            }
            return;
        }

        if (categoryIndex >= categories.size()) {
            categoryIndex = 0;
        }
        CategoryManager.Category category = categories.get(categoryIndex);
        boolean multi = categories.size() > 1;
        create(4, "&7DUELS &7→ &7" + category.getHeader());

        String layoutId = GuiLayoutManager.categoryMenuId(category.getId());
        if (plugin.getGuiLayoutManager().has(layoutId)) {
            renderLayout(layoutId);
        } else {
            grid(plugin.getCategoryManager().kitsFor(category), multi);
        }

        // The category arrow is always pinned to the bottom-right corner.
        if (multi) {
            inventory.setItem(ARROW_SLOT, arrowItem());
        }
    }

    /** Renders a saved layout: kit markers become live icons, decoration stays. */
    private void renderLayout(String layoutId) {
        for (Map.Entry<Integer, ItemStack> e : plugin.getGuiLayoutManager().get(layoutId).entrySet()) {
            if (e.getKey() >= 36) {
                continue;
            }
            // The next-category arrow is placed automatically, not from the layout.
            if ("next-cat".equals(Items.readTag(e.getValue(), plugin.keyButton()))) {
                continue;
            }
            String kitName = Items.readTag(e.getValue(), plugin.keyKit());
            if (kitName != null) {
                ItemStack icon = kitIcon(kitName);
                if (icon != null) {
                    inventory.setItem(e.getKey(), icon);
                }
            } else {
                inventory.setItem(e.getKey(), e.getValue());
            }
        }
    }

    /** Default gray-filled grid of kit icons; leaves the arrow slot free when needed. */
    private void grid(List<String> kitNames, boolean reserveArrow) {
        ItemStack filler = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }
        int limit = reserveArrow ? ARROW_SLOT : 36;
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
    }

    private ItemStack arrowItem() {
        return Items.of(Material.ARROW)
                .name("&eNext Category")
                .lore("&f→ another category")
                .tag(plugin.keyButton(), "next-cat")
                .build();
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

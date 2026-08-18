package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.managers.CategoryManager;
import com.vduels.model.Kit;
import com.vduels.model.Party;
import com.vduels.util.Items;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class PartyKitPickMenu extends Menu {

    private static final int[] KIT_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private final VDuels plugin;
    private final Party party;
    private final Runnable onChosen;
    private int categoryIndex = 0;

    public PartyKitPickMenu(VDuels plugin, Party party, Runnable onChosen) {
        this.plugin = plugin;
        this.party = party;
        this.onChosen = onChosen;
    }

    @Override
    public void build() {
        List<CategoryManager.Category> categories = plugin.getCategoryManager().all();

        List<String> kitNames;
        boolean multi = false;
        if (categories.isEmpty()) {
            create(4, "&7PARTY \u2192 KIT");
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
            create(4, "&7PARTY \u2192 KIT \u2192 &7" + category.getHeader());
            kitNames = plugin.getCategoryManager().kitsFor(category);
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
            inventory.setItem(35, arrowItem());
        }
    }

    private ItemStack kitIcon(String name) {
        Kit kit = plugin.getKitManager().get(name);
        if (kit == null) {
            return null;
        }
        Items item = Items.of(kit.getIcon())
                .lore("", "&fClick to select")
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
                .lore("&f\u2192 another category")
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
            party.setSelectedKit(kitName);
            onChosen.run();
        }
    }
}

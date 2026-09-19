/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.ItemStack
 */
package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.DuelConfirmMenu;
import com.meowduels.gui.Menu;
import com.meowduels.managers.CategoryManager;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class KitPickMenu
extends Menu {
    private static final int ARROW_SLOT = 35;
    private static final int[] KIT_SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private final MeowDuels plugin;
    private final DuelConfirmMenu confirm;
    private int categoryIndex = 0;

    public KitPickMenu(MeowDuels plugin, DuelConfirmMenu confirm) {
        this.plugin = plugin;
        this.confirm = confirm;
    }

    @Override
    public void build() {
        int i;
        List<String> kitNames;
        List<CategoryManager.Category> categories = this.plugin.getCategoryManager().all();
        boolean multi = false;
        if (categories.isEmpty()) {
            this.create(4, "&7&lDuel Request");
            kitNames = new ArrayList<String>();
            for (Kit kit : this.plugin.getKitManager().all()) {
                kitNames.add(kit.getName());
            }
        } else {
            // One step past the categories is "all kits".
            //
            // categories.yml is hand-maintained and separate from the QUEUE
            // categories in queuecategories.yml, so a kit that was only ever
            // added to a queue category - or to neither - appeared in no duel
            // category and could not be picked for a duel at all. It was not
            // hidden by a filter, it was simply on no page.
            if (this.categoryIndex > categories.size()) {
                this.categoryIndex = 0;
            }
            multi = true;
            if (this.categoryIndex == categories.size()) {
                this.create(4, "&7&lDuel Request&r &7\u1d00\u029f\u029f \u1d0b\u026a\u1d1b\ua731");
                kitNames = new ArrayList<String>();
                for (Kit kit : this.plugin.getKitManager().all()) {
                    kitNames.add(kit.getName());
                }
            } else {
                CategoryManager.Category category = categories.get(this.categoryIndex);
                this.create(4, "&7&lDuel Request&r &7" + category.getHeader());
                kitNames = this.plugin.getCategoryManager().kitsFor(category);
            }
        }
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (i = 0; i < 36; ++i) {
            this.inventory.setItem(i, filler);
        }
        for (i = 0; i < KIT_SLOTS.length && i < kitNames.size(); ++i) {
            ItemStack icon = this.kitIcon((String)kitNames.get(i));
            if (icon == null) continue;
            this.inventory.setItem(KIT_SLOTS[i], icon);
        }
        if (multi) {
            this.inventory.setItem(35, this.arrowItem());
        }
    }

    private ItemStack kitIcon(String name) {
        Kit kit = this.plugin.getKitManager().get(name);
        if (kit == null) {
            return null;
        }
        Items item = Items.of(kit.getIcon()).hideTooltip().tag(this.plugin.keyKit(), kit.getName());
        if (kit.getDisplayName() != null && !kit.getDisplayName().isEmpty()) {
            item.miniName(kit.getDisplayName());
        } else {
            item.name("&e" + kit.getName());
        }
        return item.build();
    }

    private ItemStack arrowItem() {
        return Items.of(Material.ARROW).name("&eNext Category").lore("&f\u2192 another category").tag(this.plugin.keyButton(), "next-cat").build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if ("next-cat".equals(Items.readTag(clicked, this.plugin.keyButton()))) {
            // +1 for the all-kits step at the end of the cycle.
            int size = this.plugin.getCategoryManager().all().size() + 1;
            if (size > 1) {
                this.categoryIndex = (this.categoryIndex + 1) % size;
                this.build();
                player.openInventory(this.inventory);
            }
            return;
        }
        String kitName = Items.readTag(clicked, this.plugin.keyKit());
        if (kitName != null && this.plugin.getKitManager().exists(kitName)) {
            this.confirm.setSelectedKit(kitName);
            this.confirm.reopen(player);
        }
    }
}


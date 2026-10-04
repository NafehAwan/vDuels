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
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class KitPickMenu
extends Menu {
    /** The name of this screen's file: {@code gui/kitpick.yml}. */
    private static final String MENU = "kitpick";

    private static final int ROWS = 4;
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
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        int size = rows * 9;
        List<CategoryManager.Category> categories = this.plugin.getCategoryManager().all();
        boolean multi = false;
        if (categories.isEmpty()) {
            this.createRaw(rows, gui.titleOf(MENU, "plain",
                    "&7&l\u1d05\u1d1c\u1d07\u029f \u0280\u1d07\ua7af\u1d1c\u1d07\ua731\u1d1b", null));
            kitNames = new ArrayList<String>();
            for (Kit kit : this.plugin.getKitManager().all()) {
                kitNames.add(kit.getName());
            }
        } else {
            if (this.categoryIndex >= categories.size()) {
                this.categoryIndex = 0;
            }
            CategoryManager.Category category = categories.get(this.categoryIndex);
            multi = categories.size() > 1;
            Map<String, String> titleTokens = new HashMap<String, String>();
            titleTokens.put("category", category.getHeader());
            this.createRaw(rows, gui.titleOf(MENU, "category",
                    "&7&l\u1d05\u1d1c\u1d07\u029f \u0280\u1d07\ua7af\u1d1c\u1d07\ua731\u1d1b&r &7{category}", titleTokens));
            kitNames = this.plugin.getCategoryManager().kitsFor(category);
        }
        gui.frame(MENU, this.inventory, rows);
        for (i = 0; i < KIT_SLOTS.length && i < kitNames.size(); ++i) {
            if (KIT_SLOTS[i] >= size) break;
            ItemStack icon = this.kitIcon(gui, (String)kitNames.get(i));
            if (icon == null) continue;
            this.inventory.setItem(KIT_SLOTS[i], icon);
        }
        if (multi) {
            this.inventory.setItem(Math.min(size - 1, gui.slot(MENU, "next-category", ARROW_SLOT, rows)),
                                   this.arrowItem(gui));
        }
    }

    private ItemStack kitIcon(GuiConfigManager gui, String name) {
        Kit kit = this.plugin.getKitManager().get(name);
        if (kit == null) {
            return null;
        }
        HashMap<String, String> tokens = new HashMap<String, String>();
        tokens.put("kit", kit.getName());
        Items item = gui.item(MENU, "kit", tokens, gui.material(MENU, "kit", kit.getIcon()),
                              "&e{kit}")
                .hideTooltip()
                .tag(this.plugin.keyKit(), kit.getName());
        // As on the other kit lists: with no name: in the file, a kit with a
        // display name keeps it as a parsed component.
        if (gui.name(MENU, "kit", tokens) == null
                && kit.getDisplayName() != null && !kit.getDisplayName().isEmpty()) {
            item.miniName(kit.getDisplayName());
        }
        return item.build();
    }

    private ItemStack arrowItem(GuiConfigManager gui) {
        return gui.item(MENU, "next-category", Material.ARROW,
                        "&e\u0274\u1d07x\u1d1b \u1d04\u1d00\u1d1b\u1d07\u0262\u1d0f\u0280\u028f",
                        "&f\u2192 \u1d00\u0274\u1d0f\u1d1b\u029c\u1d07\u0280 \u1d04\u1d00\u1d1b\u1d07\u0262\u1d0f\u0280\u028f")
                .tag(this.plugin.keyButton(), "next-cat").build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if ("next-cat".equals(Items.readTag(clicked, this.plugin.keyButton()))) {
            int size = this.plugin.getCategoryManager().all().size();
            if (size > 0) {
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


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.inventory.ItemStack
 */
package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.managers.CategoryManager;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public class QueuePickMenu
extends Menu {
    private static final Set<QueuePickMenu> OPEN = Collections.newSetFromMap(new ConcurrentHashMap());
    private static final int ARROW_SLOT = 35;
    private static final int[] KIT_SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private final MeowDuels plugin;
    private Player viewer;
    private int categoryIndex = 0;

    public QueuePickMenu(MeowDuels plugin) {
        this.plugin = plugin;
    }

    public static void refreshAll() {
        for (QueuePickMenu menu : OPEN) {
            if (menu.inventory == null) continue;
            menu.populate();
        }
    }

    @Override
    public void open(Player player) {
        this.viewer = player;
        this.build();
        player.openInventory(this.inventory);
        OPEN.add(this);
    }

    @Override
    public void onClose(Player player, InventoryCloseEvent event) {
        OPEN.remove(this);
    }

    @Override
    public void build() {
        List<CategoryManager.Category> categories = this.plugin.getQueueCategoryManager().all();
        if (categories.isEmpty()) {
            this.create(4, "&7Ranked Queue");
        } else {
            if (this.categoryIndex >= categories.size()) {
                this.categoryIndex = 0;
            }
            this.create(4, "&7Ranked Queue \u2192 &7" + categories.get(this.categoryIndex).getHeader());
        }
        this.populate();
    }

    private void populate() {
        int i;
        List<String> kitNames;
        if (this.inventory == null) {
            return;
        }
        List<CategoryManager.Category> categories = this.plugin.getQueueCategoryManager().all();
        boolean multi = false;
        if (categories.isEmpty()) {
            kitNames = new ArrayList<String>();
            for (Kit kit : this.plugin.getKitManager().all()) {
                kitNames.add(kit.getName());
            }
        } else {
            if (this.categoryIndex >= categories.size()) {
                this.categoryIndex = 0;
            }
            multi = categories.size() > 1;
            kitNames = this.plugin.getQueueCategoryManager().kitsFor(categories.get(this.categoryIndex));
        }
        ItemStack filler = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (i = 0; i < 36; ++i) {
            this.inventory.setItem(i, filler);
        }
        for (i = 0; i < KIT_SLOTS.length && i < kitNames.size(); ++i) {
            ItemStack icon = this.kitIcon((String)kitNames.get(i));
            if (icon == null) continue;
            this.inventory.setItem(KIT_SLOTS[i], icon);
        }
        this.inventory.setItem(35, multi ? this.arrowItem() : filler);
    }

    private ItemStack kitIcon(String name) {
        Kit kit = this.plugin.getKitManager().get(name);
        if (kit == null) {
            return null;
        }
        int queued = this.plugin.getQueueManager().queued(name);
        int fights = this.plugin.getQueueManager().dueling(name);
        boolean mine = this.viewer != null && this.plugin.getQueueManager().isQueuedFor(this.viewer.getUniqueId(), name);
        int amount = Math.max(1, Math.min(64, fights));
        String action = mine ? "&7LMB TO &cLEAVE" : "&7LMB TO &aJOIN";
        Items item = Items.of(kit.getIcon(), amount).lore("&6\ud83d\udde1 &fIN QUEUE: &6" + queued + "&7/&62", "&6\ud83d\udde1 &fDUELING: &6" + fights, "", action).hideTooltip().glow(mine).tag(this.plugin.keyKit(), kit.getName());
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
            int size = this.plugin.getQueueCategoryManager().all().size();
            if (size > 0) {
                this.categoryIndex = (this.categoryIndex + 1) % size;
                this.build();
                player.openInventory(this.inventory);
            }
            return;
        }
        String kitName = Items.readTag(clicked, this.plugin.keyKit());
        if (kitName != null && this.plugin.getKitManager().exists(kitName)) {
            this.plugin.getQueueManager().toggle(player, kitName);
            if (this.plugin.getDuelManager().isInDuel(player.getUniqueId())) {
                player.closeInventory();
            } else {
                this.populate();
            }
        }
    }
}


package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.managers.CategoryManager;
import com.vduels.managers.QueueManager;
import com.vduels.model.Kit;
import com.vduels.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The 1v1 queue picker ({@code /queue}). Same fixed template as the duel kit
 * menu, but each kit shows live queue/fight counts, glows when the viewer is
 * queued for it, and is stacked by the number of ongoing fights. Left-clicking a
 * kit toggles that queue. Open menus refresh in place once a second so counts,
 * stacks and glows stay live.
 */
public class QueuePickMenu extends Menu {

    // Every open queue menu, refreshed together on the plugin's 1s tick.
    private static final Set<QueuePickMenu> OPEN =
            Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    private static final int ARROW_SLOT = 35;
    private static final int[] KIT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    private final VDuels plugin;
    private Player viewer;
    private int categoryIndex = 0;

    public QueuePickMenu(VDuels plugin) {
        this.plugin = plugin;
    }

    /** Refresh every open queue menu in place (called each server-second). */
    public static void refreshAll() {
        for (QueuePickMenu menu : OPEN) {
            if (menu.inventory != null) {
                menu.populate();
            }
        }
    }

    @Override
    public void open(Player player) {
        this.viewer = player;
        build();
        player.openInventory(inventory);
        OPEN.add(this);
    }

    @Override
    public void onClose(Player player, InventoryCloseEvent event) {
        OPEN.remove(this);
    }

    @Override
    public void build() {
        List<CategoryManager.Category> categories = plugin.getQueueCategoryManager().all();
        if (categories.isEmpty()) {
            create(4, "&7Queue");
        } else {
            if (categoryIndex >= categories.size()) {
                categoryIndex = 0;
            }
            create(4, "&7Queue → &7" + categories.get(categoryIndex).getHeader());
        }
        populate();
    }

    /** Fills the (already created) inventory - safe to call for a live refresh. */
    private void populate() {
        if (inventory == null) {
            return;
        }
        List<CategoryManager.Category> categories = plugin.getQueueCategoryManager().all();
        List<String> kitNames;
        boolean multi = false;
        if (categories.isEmpty()) {
            kitNames = new ArrayList<>();
            for (Kit kit : plugin.getKitManager().all()) {
                kitNames.add(kit.getName());
            }
        } else {
            if (categoryIndex >= categories.size()) {
                categoryIndex = 0;
            }
            multi = categories.size() > 1;
            kitNames = plugin.getQueueCategoryManager().kitsFor(categories.get(categoryIndex));
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
        inventory.setItem(ARROW_SLOT, multi ? arrowItem() : filler);
    }

    private ItemStack kitIcon(String name) {
        Kit kit = plugin.getKitManager().get(name);
        if (kit == null) {
            return null;
        }
        int queued = plugin.getQueueManager().queued(name);
        int fights = plugin.getQueueManager().dueling(name);
        boolean mine = viewer != null
                && plugin.getQueueManager().isQueuedFor(viewer.getUniqueId(), name);
        // Stack the icon by the number of ongoing fights (min 1 for a valid item).
        int amount = Math.max(1, Math.min(64, fights));
        String action = mine ? "&7LMB TO &cLEAVE" : "&7LMB TO &aJOIN";
        Items item = Items.of(kit.getIcon(), amount)
                .lore(
                        "&6🗡 &fIN QUEUE: &6" + queued + "&7/&6" + QueueManager.NEEDED,
                        "&6🗡 &fDUELING: &6" + fights,
                        "",
                        action)
                .hideTooltip()
                .glow(mine)
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
            plugin.getQueueManager().toggle(player, kitName);
            // A match may have just pulled the player into a duel - close then.
            if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
                player.closeInventory();
            } else {
                populate();
            }
        }
    }
}

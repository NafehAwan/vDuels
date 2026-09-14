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
import com.meowduels.gui.KitItemsEditMenu;
import com.meowduels.gui.Menu;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class KitEditorMenu
extends Menu {
    private static final int[] SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private final MeowDuels plugin;
    /** Personal: each kit opens the player's OWN layout. Global: admins editing
     *  the kit for everyone. */
    private final boolean personal;

    public KitEditorMenu(MeowDuels plugin) {
        this(plugin, false);
    }

    public KitEditorMenu(MeowDuels plugin, boolean personal) {
        this.personal = personal;
        this.plugin = plugin;
    }

    @Override
    public void build() {
        this.create(6, this.personal
                ? "&d&lYour Kits &7- pick a gamemode"
                : "&d&lKit Editor &7- pick a gamemode");
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; ++i) {
            this.inventory.setItem(i, filler);
        }
        List<Kit> kits = this.plugin.getKitManager().all();
        if (kits.isEmpty()) {
            this.inventory.setItem(22, Items.of(Material.BARRIER).name("&cNo kits yet").lore("&7Create one with &e/kitcreate <name>&7.").build());
            return;
        }
        for (int i = 0; i < SLOTS.length && i < kits.size(); ++i) {
            this.inventory.setItem(SLOTS[i], this.icon(kits.get(i)));
        }
    }

    private ItemStack icon(Kit kit) {
        Items item = this.personal
                ? Items.of(kit.getIcon()).lore("", "&7Click to arrange this kit", "&7the way YOU want it.").hideTooltip().tag(this.plugin.keyKit(), kit.getName())
                : Items.of(kit.getIcon()).lore("", "&7Click to edit this gamemode's items,", "&7armor and offhand item.", "&cChanges apply to everyone.").hideTooltip().tag(this.plugin.keyKit(), kit.getName());
        if (kit.getDisplayName() != null && !kit.getDisplayName().isEmpty()) {
            item.miniName(kit.getDisplayName());
        } else {
            item.name("&d" + kit.getName());
        }
        return item.build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String kitName = Items.readTag(event.getCurrentItem(), this.plugin.keyKit());
        if (kitName == null) {
            return;
        }
        Kit kit = this.plugin.getKitManager().get(kitName);
        if (kit != null) {
            new KitItemsEditMenu(this.plugin, kit, this.personal, player.getUniqueId()).open(player);
        }
    }
}


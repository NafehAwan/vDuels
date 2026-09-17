/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package com.meowtags;

import com.meowtags.Gradient;
import com.meowtags.MeowTags;
import com.meowtags.Tag;
import com.meowtags.TagMenuHolder;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class TagMenu {
    public static final int SIZE = 54;
    public static final int MAX_TAGS = 45;
    public static final int REMOVE_SLOT = 49;

    private TagMenu() {
    }

    public static void open(MeowTags plugin, Player player) {
        TagMenuHolder holder = new TagMenuHolder();
        String title = plugin.getConfig().getString("gui-title", "&8Select a Tag").replace('&', '\u00a7');
        Inventory inv = Bukkit.createInventory((InventoryHolder)holder, (int)54, (String)title);
        holder.setInventory(inv);
        ItemStack filler = TagMenu.simple(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; ++i) {
            inv.setItem(i, filler);
        }
        // Only tags this player owns. The menu used to list every tag on the
        // server and grey out the locked ones, which meant someone with access
        // to nothing opened a full wall of things they couldn't use.
        List<Tag> tags = plugin.tags().available(player);
        if (tags.isEmpty()) {
            ArrayList<String> empty = new ArrayList<String>();
            empty.add("");
            empty.add(TagMenu.color("&7Tags are unlocked by rank or by staff."));
            inv.setItem(22, TagMenu.simple(Material.BARRIER,
                    "&c\u0274\u1d0f \u1d1b\u1d00\u0262\ua731 \u028f\u1d07\u1d1b", empty));
        }
        int count = Math.min(tags.size(), 45);
        for (int i = 0; i < count; ++i) {
            Tag t = tags.get(i);
            ArrayList<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add(TagMenu.color("&a\u00bb Click to equip this tag"));
            ItemStack item = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Gradient.name(t.display, t.hex1, t.hex2));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(i, item);
        }
        ArrayList<String> rlore = new ArrayList<String>();
        rlore.add("");
        rlore.add(TagMenu.color("&7\u00bb Click to clear your tag"));
        inv.setItem(49, TagMenu.named(Material.BARRIER, "<!italic><bold><red>\u0280\u1d07\u1d0d\u1d0f\u1d20\u1d07 \u1d1b\u1d00\u0262", rlore));
        player.openInventory(inv);
    }

    private static ItemStack simple(Material mat, String legacyName, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TagMenu.color(legacyName));
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack named(Material mat, String miniName, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Gradient.mini(miniName));
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    static String color(String s) {
        return s.replace('&', '\u00a7');
    }
}


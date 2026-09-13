/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.profile.PlayerProfile
 *  com.destroystokyo.paper.profile.ProfileProperty
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 */
package com.meowduels.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.util.Arrays;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class GoldenHead {
    public static final String KEY_NAME = "golden_head";
    private static final String KEY_VALUE = "1";
    public static final String DISPLAY_NAME = "Golden Head";

    private GoldenHead() {
    }

    public static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, KEY_NAME);
    }

    public static ItemStack create(Plugin plugin, int amount) {
        return GoldenHead.create(plugin, amount, "");
    }

    public static ItemStack create(Plugin plugin, int amount, String textureBase64) {
        int clamped = Math.max(1, Math.min(64, amount));
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, clamped);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(String.valueOf(ChatColor.GOLD) + DISPLAY_NAME);
            meta.setLore(Arrays.asList(String.valueOf(ChatColor.AQUA) + "A powerful competitive UHC healing item."));
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(GoldenHead.key(plugin), PersistentDataType.STRING, (Object)KEY_VALUE);
            GoldenHead.applyTexture(meta, textureBase64);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void applyTexture(ItemMeta meta, String textureBase64) {
        if (textureBase64 == null || textureBase64.trim().isEmpty() || !(meta instanceof SkullMeta)) {
            return;
        }
        try {
            PlayerProfile profile = Bukkit.createProfile((UUID)UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", textureBase64.trim()));
            ((SkullMeta)meta).setPlayerProfile(profile);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public static boolean isGoldenHead(Plugin plugin, ItemStack item) {
        String name;
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        Material type = item.getType();
        if (type != Material.PLAYER_HEAD && type != Material.GOLDEN_APPLE) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        String tag = (String)meta.getPersistentDataContainer().get(GoldenHead.key(plugin), PersistentDataType.STRING);
        if (KEY_VALUE.equals(tag)) {
            return true;
        }
        return meta.hasDisplayName() && (name = ChatColor.stripColor((String)meta.getDisplayName())) != null && name.trim().equalsIgnoreCase(DISPLAY_NAME);
    }
}


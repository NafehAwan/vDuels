/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Registry
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ArmorMeta
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.trim.ArmorTrim
 *  org.bukkit.inventory.meta.trim.TrimMaterial
 *  org.bukkit.inventory.meta.trim.TrimPattern
 */
package com.meowduels.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

public final class Trims {
    public static final String[] PATTERN_KEYS = new String[]{"sentry", "dune", "coast", "wild", "ward", "eye", "vex", "tide", "snout", "rib", "spire", "wayfinder", "shaper", "silence", "raiser", "host", "flow", "bolt"};
    public static final String[] MATERIAL_KEYS = new String[]{"quartz", "iron", "gold", "copper", "netherite", "redstone", "emerald", "diamond", "lapis", "amethyst", "resin"};

    private Trims() {
    }

    public static TrimPattern pattern(String key) {
        try {
            return (TrimPattern)Registry.TRIM_PATTERN.get(NamespacedKey.minecraft((String)key));
        }
        catch (Throwable t) {
            return null;
        }
    }

    public static TrimMaterial material(String key) {
        try {
            return (TrimMaterial)Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft((String)key));
        }
        catch (Throwable t) {
            return null;
        }
    }

    public static boolean available() {
        return Trims.pattern("sentry") != null && Trims.material("quartz") != null;
    }

    public static boolean apply(ItemStack armor, String patternKey, String materialKey) {
        if (armor == null) {
            return false;
        }
        try {
            ItemMeta meta = armor.getItemMeta();
            if (!(meta instanceof ArmorMeta)) {
                return false;
            }
            TrimPattern p = Trims.pattern(patternKey);
            TrimMaterial m = Trims.material(materialKey);
            if (p == null || m == null) {
                return false;
            }
            ((ArmorMeta)meta).setTrim(new ArmorTrim(m, p));
            armor.setItemMeta(meta);
            return true;
        }
        catch (Throwable t) {
            return false;
        }
    }

    public static boolean hasTrim(ItemStack armor) {
        if (armor == null) {
            return false;
        }
        try {
            ItemMeta meta = armor.getItemMeta();
            return meta instanceof ArmorMeta && ((ArmorMeta)meta).hasTrim();
        }
        catch (Throwable t) {
            return false;
        }
    }

    public static void clear(ItemStack armor) {
        if (armor == null) {
            return;
        }
        try {
            ItemMeta meta = armor.getItemMeta();
            if (meta instanceof ArmorMeta) {
                ((ArmorMeta)meta).setTrim(null);
                armor.setItemMeta(meta);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public static String pretty(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(key.charAt(0)) + key.substring(1);
    }
}


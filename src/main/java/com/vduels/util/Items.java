package com.vduels.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fluent-ish builder for GUI items so menu code stays readable.
 */
public final class Items {

    private final ItemStack item;
    private final ItemMeta meta;

    private Items(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }

    public static Items of(Material material) {
        return new Items(material, 1);
    }

    public static Items of(Material material, int amount) {
        return new Items(material, amount);
    }

    public Items name(String name) {
        if (meta != null) {
            meta.setDisplayName(Text.color(name));
        }
        return this;
    }

    public Items lore(String... lines) {
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            for (String line : lines) {
                lore.add(Text.color(line));
            }
            meta.setLore(lore);
        }
        return this;
    }

    public Items lore(List<String> lines) {
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            for (String line : lines) {
                lore.add(Text.color(line));
            }
            meta.setLore(lore);
        }
        return this;
    }

    public Items glow(boolean glow) {
        if (meta != null && glow) {
            // Glint override (1.20.5+) adds the enchant shimmer without a real
            // enchantment, avoiding the version-churn around Enchantment fields.
            meta.setEnchantmentGlintOverride(true);
        }
        return this;
    }

    /** Hides attribute/enchant/extra tooltips so icons show only the name. */
    public Items hideTooltip() {
        if (meta != null) {
            meta.addItemFlags(
                    org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                    org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS,
                    org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP,
                    org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE,
                    org.bukkit.inventory.ItemFlag.HIDE_DYE);
        }
        return this;
    }

    /** Sets a MiniMessage display name (gradients, bold, etc.), non-italic. */
    public Items miniName(String miniMessage) {
        if (meta != null && miniMessage != null) {
            meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize(miniMessage)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        }
        return this;
    }

    public Items tag(NamespacedKey key, String value) {
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Reads a string tag from an item, or null if not present. */
    public static String readTag(ItemStack stack, NamespacedKey key) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        ItemMeta m = stack.getItemMeta();
        return m.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public static List<String> lines(String... lines) {
        return new ArrayList<>(Arrays.asList(lines));
    }
}

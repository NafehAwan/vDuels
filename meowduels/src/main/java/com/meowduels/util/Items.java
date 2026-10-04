/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.format.TextDecoration
 *  net.kyori.adventure.text.minimessage.MiniMessage
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.persistence.PersistentDataType
 */
package com.meowduels.util;

import com.meowduels.util.Colors;
import com.meowduels.util.Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

public final class Items {
    private final ItemStack item;
    private final ItemMeta meta;

    private Items(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = this.item.getItemMeta();
    }

    public static Items of(Material material) {
        return new Items(material, 1);
    }

    public static Items of(Material material, int amount) {
        return new Items(material, amount);
    }

    public Items name(String name) {
        if (this.meta != null) {
            this.meta.setDisplayName(Text.color(name));
        }
        return this;
    }

    public Items lore(String ... lines) {
        if (this.meta != null) {
            ArrayList<String> lore = new ArrayList<String>();
            for (String line : lines) {
                lore.add(Text.color(line));
            }
            this.meta.setLore(lore);
        }
        return this;
    }

    public Items lore(List<String> lines) {
        if (this.meta != null) {
            ArrayList<String> lore = new ArrayList<String>();
            for (String line : lines) {
                lore.add(Text.color(line));
            }
            this.meta.setLore(lore);
        }
        return this;
    }

    /**
     * A display name written out exactly as given.
     *
     * <p>{@link #name} puts every letter through {@link Text#smallCaps}, which is
     * right for a fixed label and wrong for anything variable - a player called
     * "Steve" comes back as "\u1d1b\u1d07\u1d20\u1d07". This one re-cases nothing, so a
     * menu can mix small-caps labels with normal-font names and numbers, and it
     * understands gradients and hex as well.
     */
    public Items rawName(String name) {
        if (this.meta != null) {
            this.meta.setDisplayName(Colors.toSection(name));
        }
        return this;
    }

    /** Lore written out exactly as given - see {@link #rawName}. */
    public Items rawLore(String ... lines) {
        if (this.meta != null) {
            ArrayList<String> lore = new ArrayList<String>();
            for (String line : lines) {
                lore.add(Colors.toSection(line));
            }
            this.meta.setLore(lore);
        }
        return this;
    }

    /** Points a PLAYER_HEAD at someone; a no-op on any other material. */
    public Items skull(OfflinePlayer owner) {
        if (owner != null && this.meta instanceof SkullMeta) {
            try {
                ((SkullMeta)this.meta).setOwningPlayer(owner);
            }
            catch (Throwable throwable) {
                // profile lookups can fail offline; the head just stays blank
            }
        }
        return this;
    }

    /**
     * Adds the enchanted shimmer, and does nothing at all when asked for no
     * shimmer.
     *
     * <p>The asymmetry is deliberate and must stay. A handful of materials
     * glint on their own - an enchanted book used as a kit icon, say - and
     * every caller here passes a "is this one selected" flag, not a styling
     * decision. Turning the override off for the unselected ones would strip
     * a glint those materials are supposed to have.
     *
     * <p>To suppress a glint on purpose, use {@link #glint} with
     * {@link Boolean#FALSE}.
     */
    public Items glow(boolean glow) {
        if (this.meta != null && glow) {
            this.meta.setEnchantmentGlintOverride(Boolean.valueOf(true));
        }
        return this;
    }

    /**
     * The shimmer as three states rather than two: on, off, or untouched.
     *
     * <p>What {@link #glow} cannot express, and what a menu file needs - a
     * {@code glow:} key that is absent has to leave the item exactly as the
     * code built it, while {@code glow: false} has to be able to take a
     * shimmer away. {@code null} is the absent case.
     */
    public Items glint(Boolean glow) {
        if (this.meta != null && glow != null) {
            this.meta.setEnchantmentGlintOverride(glow);
        }
        return this;
    }

    public Items hideTooltip() {
        if (this.meta != null) {
            this.meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE});
        }
        return this;
    }

    public Items miniName(String miniMessage) {
        if (this.meta != null && miniMessage != null) {
            this.meta.displayName(MiniMessage.miniMessage().deserialize((Object)miniMessage).decoration(TextDecoration.ITALIC, false));
        }
        return this;
    }

    public Items tag(NamespacedKey key, String value) {
        if (this.meta != null) {
            this.meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, (Object)value);
        }
        return this;
    }

    public ItemStack build() {
        if (this.meta != null) {
            this.item.setItemMeta(this.meta);
        }
        return this.item;
    }

    public static String readTag(ItemStack stack, NamespacedKey key) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        ItemMeta m = stack.getItemMeta();
        return (String)m.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public static List<String> lines(String ... lines) {
        return new ArrayList<String>(Arrays.asList(lines));
    }
}


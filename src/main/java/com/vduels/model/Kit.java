package com.vduels.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;

/**
 * A saved loadout: main inventory contents plus the four armour slots and the
 * off-hand. Captured straight from a player's inventory and re-applied on duel
 * start. {@link ItemStack} is natively YAML-serialisable so we lean on that.
 */
public class Kit {

    private final String name;
    private ItemStack[] contents;   // 36 main slots
    private ItemStack[] armor;      // boots, leggings, chestplate, helmet
    private ItemStack offhand;
    private Material icon = Material.IRON_SWORD;
    private String displayName;     // optional MiniMessage display name

    public Kit(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Material getIcon() {
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    /** Optional MiniMessage display name (supports gradients, bold, etc.). */
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /** Snapshot the given player's full inventory into this kit. */
    public void captureFrom(Player player) {
        PlayerInventory inv = player.getInventory();
        this.contents = cloneAll(inv.getStorageContents());
        this.armor = cloneAll(inv.getArmorContents());
        this.offhand = inv.getItemInOffHand() == null ? null : inv.getItemInOffHand().clone();
        ItemStack held = inv.getItemInMainHand();
        if (held != null && held.getType() != Material.AIR) {
            this.icon = held.getType();
        }
    }

    /** Clear then apply this kit onto a player. */
    public void applyTo(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        if (contents != null) {
            inv.setStorageContents(cloneAll(contents));
        }
        if (armor != null) {
            inv.setArmorContents(cloneAll(armor));
        }
        inv.setItemInOffHand(offhand == null ? null : offhand.clone());
        player.updateInventory();
    }

    private static ItemStack[] cloneAll(ItemStack[] src) {
        if (src == null) {
            return null;
        }
        ItemStack[] out = new ItemStack[src.length];
        for (int i = 0; i < src.length; i++) {
            out[i] = src[i] == null ? null : src[i].clone();
        }
        return out;
    }

    public void save(ConfigurationSection section) {
        section.set("icon", icon.name());
        section.set("display-name", displayName);
        section.set("contents", contents);
        section.set("armor", armor);
        section.set("offhand", offhand);
    }

    @SuppressWarnings("unchecked")
    public static Kit load(String name, ConfigurationSection section) {
        Kit kit = new Kit(name);
        String iconName = section.getString("icon", "IRON_SWORD");
        Material mat = Material.matchMaterial(iconName);
        kit.icon = mat == null ? Material.IRON_SWORD : mat;
        kit.displayName = section.getString("display-name");

        List<?> contentsList = section.getList("contents");
        if (contentsList != null) {
            kit.contents = contentsList.toArray(new ItemStack[0]);
        }
        List<?> armorList = section.getList("armor");
        if (armorList != null) {
            kit.armor = armorList.toArray(new ItemStack[0]);
        }
        kit.offhand = section.getItemStack("offhand");
        return kit;
    }
}

package com.vduels.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
    private final Set<StartEffect> startEffects = EnumSet.noneOf(StartEffect.class);

    public Kit(String name) {
        this.name = name;
    }

    /** The starting effects this kit grants each round (empty = none). */
    public Set<StartEffect> getStartEffects() {
        return startEffects;
    }

    public boolean hasStartEffect(StartEffect effect) {
        return startEffects.contains(effect);
    }

    public void toggleStartEffect(StartEffect effect) {
        if (!startEffects.remove(effect)) {
            startEffects.add(effect);
        }
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

    /** Re-sets just the off-hand item (used a tick later so it reliably shows). */
    public void applyOffhand(Player player) {
        player.getInventory().setItemInOffHand(offhand == null ? null : offhand.clone());
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
        List<String> effectNames = new ArrayList<>();
        for (StartEffect effect : startEffects) {
            effectNames.add(effect.name());
        }
        section.set("start-effects", effectNames);
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
        for (String effectName : section.getStringList("start-effects")) {
            try {
                kit.startEffects.add(StartEffect.valueOf(effectName));
            } catch (IllegalArgumentException ignored) {
                // Unknown effect name in config - skip it.
            }
        }

        kit.contents = toItemArray(section.getList("contents"));
        kit.armor = toItemArray(section.getList("armor"));
        kit.offhand = section.getItemStack("offhand");
        return kit;
    }

    /**
     * Converts a serialized list into an ItemStack[]. Any entry that isn't an
     * ItemStack (e.g. an item that failed to deserialize on this server version)
     * becomes an empty slot instead of throwing, so one bad item never drops the
     * whole kit.
     */
    private static ItemStack[] toItemArray(List<?> list) {
        if (list == null) {
            return null;
        }
        ItemStack[] out = new ItemStack[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object o = list.get(i);
            out[i] = (o instanceof ItemStack) ? (ItemStack) o : null;
        }
        return out;
    }
}

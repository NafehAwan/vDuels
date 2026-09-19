/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 */
package com.meowduels.model;

import com.meowduels.model.StartEffect;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class Kit {
    private final String name;
    private ItemStack[] contents;
    private ItemStack[] armor;
    private ItemStack offhand;
    private Material icon = Material.IRON_SWORD;
    private String displayName;
    private final Set<StartEffect> startEffects = EnumSet.noneOf(StartEffect.class);

    public Kit(String name) {
        this.name = name;
    }

    public Set<StartEffect> getStartEffects() {
        return this.startEffects;
    }

    public boolean hasStartEffect(StartEffect effect) {
        return this.startEffects.contains((Object)effect);
    }

    public void toggleStartEffect(StartEffect effect) {
        if (!this.startEffects.remove((Object)effect)) {
            this.startEffects.add(effect);
        }
    }

    public String getName() {
        return this.name;
    }

    public Material getIcon() {
        return this.icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void captureFrom(Player player) {
        PlayerInventory inv = player.getInventory();
        this.contents = Kit.cloneAll(inv.getStorageContents());
        this.armor = Kit.cloneAll(inv.getArmorContents());
        this.offhand = inv.getItemInOffHand() == null ? null : inv.getItemInOffHand().clone();
        ItemStack held = inv.getItemInMainHand();
        if (held != null && held.getType() != Material.AIR) {
            this.icon = held.getType();
        }
    }

    public void applyTo(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        if (this.contents != null) {
            inv.setStorageContents(Kit.cloneAll(this.contents));
        }
        if (this.armor != null) {
            inv.setArmorContents(Kit.cloneAll(this.armor));
        }
        inv.setItemInOffHand(this.offhand == null ? null : this.offhand.clone());
        player.updateInventory();
    }

    /**
     * Gives this kit's start effects - the "auto pots".
     *
     * <p>Separate from {@link #applyTo} on purpose, and called when the fight
     * actually opens rather than when the kit is handed out. Applying them with
     * the kit meant a 1:30 strength was already down to about 1:25 by the time
     * the countdown finished - the player paid for the wait out of their own
     * buff. Every mode that starts a fight calls this at its own go signal.
     */
    public void applyStartEffects(Player player) {
        if (player == null) {
            return;
        }
        for (StartEffect effect : this.startEffects) {
            try {
                player.addPotionEffect(effect.toPotionEffect());
            }
            catch (Throwable t) {
                // a effect type this server build does not have
            }
        }
    }

    public void applyOffhand(Player player) {
        player.getInventory().setItemInOffHand(this.offhand == null ? null : this.offhand.clone());
        player.updateInventory();
    }

    public ItemStack[] getContents() {
        return this.contents == null ? new ItemStack[36] : Kit.cloneAll(this.contents);
    }

    public ItemStack[] getArmor() {
        return this.armor == null ? new ItemStack[4] : Kit.cloneAll(this.armor);
    }

    public ItemStack getOffhand() {
        return this.offhand == null ? null : this.offhand.clone();
    }

    public void setContents(ItemStack[] contents) {
        this.contents = Kit.cloneAll(contents);
    }

    public void setArmor(ItemStack[] armor) {
        this.armor = Kit.cloneAll(armor);
    }

    public void setOffhand(ItemStack offhand) {
        this.offhand = offhand == null ? null : offhand.clone();
    }

    public void clearItems() {
        this.contents = null;
        this.armor = null;
        this.offhand = null;
    }

    private static ItemStack[] cloneAll(ItemStack[] src) {
        if (src == null) {
            return null;
        }
        ItemStack[] out = new ItemStack[src.length];
        for (int i = 0; i < src.length; ++i) {
            out[i] = src[i] == null ? null : src[i].clone();
        }
        return out;
    }

    public void save(ConfigurationSection section) {
        section.set("icon", (Object)this.icon.name());
        section.set("display-name", (Object)this.displayName);
        ArrayList<String> effectNames = new ArrayList<String>();
        for (StartEffect effect : this.startEffects) {
            effectNames.add(effect.name());
        }
        section.set("start-effects", effectNames);
        section.set("contents", (Object)this.contents);
        section.set("armor", (Object)this.armor);
        section.set("offhand", (Object)this.offhand);
    }

    public static Kit load(String name, ConfigurationSection section) {
        List<?> armorList;
        Kit kit = new Kit(name);
        String iconName = section.getString("icon", "IRON_SWORD");
        Material mat = Material.matchMaterial((String)iconName);
        kit.icon = mat == null ? Material.IRON_SWORD : mat;
        kit.displayName = section.getString("display-name");
        for (String effectName : section.getStringList("start-effects")) {
            try {
                kit.startEffects.add(StartEffect.valueOf(effectName));
            }
            catch (IllegalArgumentException illegalArgumentException) {}
        }
        List<?> contentsList = section.getList("contents");
        if (contentsList != null) {
            kit.contents = contentsList.toArray(new ItemStack[0]);
        }
        if ((armorList = section.getList("armor")) != null) {
            kit.armor = armorList.toArray(new ItemStack[0]);
        }
        kit.offhand = section.getItemStack("offhand");
        return kit;
    }
}


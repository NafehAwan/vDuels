/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.potion.PotionEffect
 */
package com.meowduels.model;

import com.meowduels.util.GameModeGuard;
import java.util.ArrayList;
import java.util.Collection;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class PlayerSnapshot {
    private final Location location;
    private final GameMode gameMode;
    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final ItemStack offhand;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final int totalExp;
    private final float exp;
    private final int level;
    private final boolean allowFlight;
    private final boolean flying;
    private final Collection<PotionEffect> effects;

    private PlayerSnapshot(Player player) {
        this.location = player.getLocation().clone();
        this.gameMode = player.getGameMode();
        this.contents = (ItemStack[])player.getInventory().getStorageContents().clone();
        this.armor = (ItemStack[])player.getInventory().getArmorContents().clone();
        this.offhand = player.getInventory().getItemInOffHand().clone();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.totalExp = player.getTotalExperience();
        this.exp = player.getExp();
        this.level = player.getLevel();
        this.allowFlight = player.getAllowFlight();
        this.flying = player.isFlying();
        this.effects = new ArrayList<PotionEffect>(player.getActivePotionEffects());
    }

    public static PlayerSnapshot capture(Player player) {
        return new PlayerSnapshot(player);
    }

    public Location getLocation() {
        return this.location;
    }

    public void restore(Player player) {
        player.getInventory().setStorageContents(this.contents);
        player.getInventory().setArmorContents(this.armor);
        player.getInventory().setItemInOffHand(this.offhand);
        GameModeGuard.setFreely(player, this.gameMode);
        double maxHealth = player.getMaxHealth();
        player.setHealth(Math.min(this.health, maxHealth <= 0.0 ? 20.0 : maxHealth));
        player.setFoodLevel(this.foodLevel);
        player.setSaturation(this.saturation);
        player.setFireTicks(0);
        player.setTotalExperience(this.totalExp);
        player.setLevel(this.level);
        player.setExp(this.exp);
        player.setAllowFlight(this.allowFlight);
        player.setFlying(this.flying);
        for (PotionEffect active : player.getActivePotionEffects()) {
            player.removePotionEffect(active.getType());
        }
        for (PotionEffect effect : this.effects) {
            player.addPotionEffect(effect);
        }
        player.updateInventory();
    }
}


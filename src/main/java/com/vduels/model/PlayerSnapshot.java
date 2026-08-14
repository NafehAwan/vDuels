package com.vduels.model;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Captures everything about a player we need to put back the way it was after a
 * duel: inventory, location, mode, vitals, XP and active effects.
 */
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
        this.contents = player.getInventory().getStorageContents().clone();
        this.armor = player.getInventory().getArmorContents().clone();
        this.offhand = player.getInventory().getItemInOffHand().clone();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.totalExp = player.getTotalExperience();
        this.exp = player.getExp();
        this.level = player.getLevel();
        this.allowFlight = player.getAllowFlight();
        this.flying = player.isFlying();
        this.effects = new ArrayList<>(player.getActivePotionEffects());
    }

    public static PlayerSnapshot capture(Player player) {
        return new PlayerSnapshot(player);
    }

    public Location getLocation() {
        return location;
    }

    public void restore(Player player) {
        player.getInventory().setStorageContents(contents);
        player.getInventory().setArmorContents(armor);
        player.getInventory().setItemInOffHand(offhand);
        player.setGameMode(gameMode);

        // getMaxHealth() is deprecated but stable across all 1.21.x versions,
        // unlike the Attribute enum which was renamed mid-cycle.
        double maxHealth = player.getMaxHealth();
        player.setHealth(Math.min(health, maxHealth <= 0 ? 20.0 : maxHealth));
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setFireTicks(0);
        player.setTotalExperience(totalExp);
        player.setLevel(level);
        player.setExp(exp);
        player.setAllowFlight(allowFlight);
        player.setFlying(flying);

        for (PotionEffect active : player.getActivePotionEffects()) {
            player.removePotionEffect(active.getType());
        }
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }
        player.updateInventory();
    }
}

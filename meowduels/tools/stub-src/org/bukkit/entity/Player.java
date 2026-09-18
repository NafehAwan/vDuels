package org.bukkit.entity;

public interface Player extends org.bukkit.OfflinePlayer {
    org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin a0, java.lang.String a1, boolean a2);
    boolean addPotionEffect(org.bukkit.potion.PotionEffect a0);
    boolean addScoreboardTag(java.lang.String a0);
    void closeInventory();
    void damage(double a0);
    boolean equals(java.lang.Object a0);
    double getAbsorptionAmount();
    boolean getAllowFlight();
    float getExp();
    int getFoodLevel();
    org.bukkit.GameMode getGameMode();
    double getHealth();
    org.bukkit.inventory.PlayerInventory getInventory();
    org.bukkit.entity.Player getKiller();
    int getLevel();
    org.bukkit.Location getLocation();
    double getMaxHealth();
    java.lang.String getName();
    int getPing();
    float getSaturation();
    org.bukkit.scoreboard.Scoreboard getScoreboard();
    java.util.Set getScoreboardTags();
    int getStatistic(org.bukkit.Statistic a0);
    int getTotalExperience();
    java.util.UUID getUniqueId();
    boolean hasPermission(java.lang.String a0);
    void hidePlayer(org.bukkit.plugin.Plugin a0, org.bukkit.entity.Player a1);
    void listPlayer(org.bukkit.entity.Player a0);
    void unlistPlayer(org.bukkit.entity.Player a0);
    boolean isFlying();
    boolean isGliding();
    boolean isOnline();
    org.bukkit.inventory.InventoryView openInventory(org.bukkit.inventory.Inventory a0);
    void playSound(org.bukkit.Location a0, java.lang.String a1, float a2, float a3);
    void recalculatePermissions();
    void removeAttachment(org.bukkit.permissions.PermissionAttachment a0);
    void removePotionEffect(org.bukkit.potion.PotionEffectType a0);
    boolean removeScoreboardTag(java.lang.String a0);
    void sendActionBar(net.kyori.adventure.text.Component a0);
    void sendMessage(java.lang.String a0);
    void sendMessage(net.kyori.adventure.text.Component a0);
    void sendPlayerListHeaderAndFooter(net.kyori.adventure.text.Component a0, net.kyori.adventure.text.Component a1);
    void sendTitle(java.lang.String a0, java.lang.String a1, int a2, int a3, int a4);
    void setAbsorptionAmount(double a0);
    void setAllowFlight(boolean a0);
    void setExhaustion(float a0);
    void setExp(float a0);
    void setFallDistance(float a0);
    void setFireTicks(int a0);
    void setFlying(boolean a0);
    void setFoodLevel(int a0);
    void setGameMode(org.bukkit.GameMode a0);
    void setGliding(boolean a0);
    void setGlowing(boolean a0);
    void setHealth(double a0);
    void setLevel(int a0);
    void setSaturation(float a0);
    void setScoreboard(org.bukkit.scoreboard.Scoreboard a0);
    void setTotalExperience(int a0);
    void setVelocity(org.bukkit.util.Vector a0);
    void setWorldBorder(org.bukkit.WorldBorder a0);
    void showPlayer(org.bukkit.plugin.Plugin a0, org.bukkit.entity.Player a1);
    org.bukkit.entity.Player.Spigot spigot();
    boolean teleport(org.bukkit.Location a0);
    void updateCommands();
    void updateInventory();
    java.util.Collection<org.bukkit.potion.PotionEffect> getActivePotionEffects();
    public class Spigot {
        public void respawn() {}
        public void sendMessage(net.md_5.bungee.api.chat.BaseComponent[] a0) {}
    }

}

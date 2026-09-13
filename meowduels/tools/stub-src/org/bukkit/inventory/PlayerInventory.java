package org.bukkit.inventory;

public interface PlayerInventory {
    java.util.HashMap addItem(org.bukkit.inventory.ItemStack[] a0);
    void clear();
    org.bukkit.inventory.ItemStack[] getArmorContents();
    org.bukkit.inventory.ItemStack getItemInMainHand();
    org.bukkit.inventory.ItemStack getItemInOffHand();
    org.bukkit.inventory.ItemStack[] getStorageContents();
    void setArmorContents(org.bukkit.inventory.ItemStack[] a0);
    void setItem(int a0, org.bukkit.inventory.ItemStack a1);
    void setItemInMainHand(org.bukkit.inventory.ItemStack a0);
    void setItemInOffHand(org.bukkit.inventory.ItemStack a0);
    void setStorageContents(org.bukkit.inventory.ItemStack[] a0);
}

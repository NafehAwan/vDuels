package org.bukkit.inventory;

public interface Inventory {
    org.bukkit.inventory.InventoryHolder getHolder();
    org.bukkit.inventory.ItemStack getItem(int a0);
    int getSize();
    void setItem(int a0, org.bukkit.inventory.ItemStack a1);
}

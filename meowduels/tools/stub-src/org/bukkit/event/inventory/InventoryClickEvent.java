package org.bukkit.event.inventory;

public class InventoryClickEvent {
    public org.bukkit.event.inventory.InventoryAction getAction() { return null; }
    public org.bukkit.inventory.ItemStack getCurrentItem() { return null; }
    public org.bukkit.inventory.Inventory getInventory() { return null; }
    public int getRawSlot() { return 0; }
    public org.bukkit.entity.HumanEntity getWhoClicked() { return null; }
    public boolean isRightClick() { return false; }
    public void setCancelled(boolean a0) {}
    public boolean isShiftClick() { return false; }
}

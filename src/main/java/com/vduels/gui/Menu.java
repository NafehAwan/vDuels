package com.vduels.gui;

import com.vduels.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Base class for every vDuels menu. Being the inventory's {@link InventoryHolder}
 * is how {@code GuiListener} recognises our menus and routes clicks back here.
 */
public abstract class Menu implements InventoryHolder {

    protected Inventory inventory;

    protected Inventory create(int rows, String title) {
        this.inventory = Bukkit.createInventory(this, rows * 9, Text.color(title));
        return inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        build();
        player.openInventory(inventory);
    }

    /** (Re)populate the inventory contents. Called on open and on refresh. */
    public abstract void build();

    /** Handle a click. The event is already cancelled before this is called. */
    public abstract void onClick(Player player, InventoryClickEvent event);

    /** Optional hook for menus that need to react to being closed. */
    public void onClose(Player player, InventoryCloseEvent event) {
    }

    /** Whether clicks inside this menu should be cancelled (true for most). */
    public boolean cancelClicks() {
        return true;
    }

    /**
     * Editable menus (the duel-menu editor) let the admin move items freely in a
     * region of the top inventory. When true, {@code GuiListener} only cancels
     * interactions with {@link #isProtectedSlot(int)} slots.
     */
    public boolean isEditable() {
        return false;
    }

    /** For editable menus, whether a raw top-inventory slot is locked. */
    public boolean isProtectedSlot(int rawSlot) {
        return true;
    }
}

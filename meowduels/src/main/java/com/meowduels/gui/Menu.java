/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 */
package com.meowduels.gui;

import com.meowduels.util.Colors;
import com.meowduels.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class Menu
implements InventoryHolder {
    protected Inventory inventory;

    protected Inventory create(int rows, String title) {
        this.inventory = Bukkit.createInventory((InventoryHolder)this, (int)(rows * 9), (String)Text.color(title));
        return this.inventory;
    }

    /**
     * Same as {@link #create}, but the title is taken literally: {@link #create}
     * small-caps every letter, which mangles a player's name in a title.
     */
    protected Inventory createRaw(int rows, String title) {
        this.inventory = Bukkit.createInventory((InventoryHolder)this, (int)(rows * 9), (String)Colors.toSection(title));
        return this.inventory;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public void open(Player player) {
        this.build();
        player.openInventory(this.inventory);
    }

    public abstract void build();

    public abstract void onClick(Player var1, InventoryClickEvent var2);

    public void onClose(Player player, InventoryCloseEvent event) {
    }

    public boolean cancelClicks() {
        return true;
    }

    public boolean isEditable() {
        return false;
    }

    public boolean isProtectedSlot(int rawSlot) {
        return true;
    }
}


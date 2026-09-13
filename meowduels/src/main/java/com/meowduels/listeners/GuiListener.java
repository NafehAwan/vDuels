/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryAction
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 */
package com.meowduels.listeners;

import com.meowduels.gui.Menu;
import com.meowduels.util.Sounds;
import java.util.Iterator;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GuiListener
implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        boolean clickedTop;
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        Inventory top = event.getInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof Menu)) {
            return;
        }
        Menu menu = (Menu)holder;
        int raw = event.getRawSlot();
        boolean bl = clickedTop = raw >= 0 && raw < top.getSize();
        if (!menu.isEditable()) {
            event.setCancelled(true);
            if (clickedTop && event.getCurrentItem() != null) {
                Sounds.click(player);
                menu.onClick(player, event);
            }
            return;
        }
        InventoryAction action = event.getAction();
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY || action == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        if (clickedTop && menu.isProtectedSlot(raw)) {
            event.setCancelled(true);
            menu.onClick(player, event);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getInventory();
        InventoryHolder inventoryHolder = top.getHolder();
        if (!(inventoryHolder instanceof Menu)) {
            return;
        }
        Menu menu = (Menu)inventoryHolder;
        if (!menu.isEditable()) {
            event.setCancelled(true);
            return;
        }
        int size = top.getSize();
        Iterator iterator = event.getRawSlots().iterator();
        while (iterator.hasNext()) {
            int raw = (Integer)iterator.next();
            if (raw >= size || !menu.isProtectedSlot(raw)) continue;
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        HumanEntity humanEntity = event.getPlayer();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        InventoryHolder inventoryHolder = event.getInventory().getHolder();
        if (inventoryHolder instanceof Menu) {
            Menu menu = (Menu)inventoryHolder;
            menu.onClose(player, event);
        }
    }
}


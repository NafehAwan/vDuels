package com.vduels.listeners;

import com.vduels.gui.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Routes inventory events to the {@link Menu} that owns the inventory. Static
 * menus cancel every click; editable menus (the duel editor) only lock their
 * protected slots.
 */
public class GuiListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof Menu menu)) {
            return;
        }
        int raw = event.getRawSlot();
        boolean clickedTop = raw >= 0 && raw < top.getSize();

        if (!menu.isEditable()) {
            event.setCancelled(true);
            if (clickedTop) {
                menu.onClick(player, event);
            }
            return;
        }

        // Editable menu: block anything that could smuggle items into locked
        // slots, but otherwise let the admin arrange items freely.
        InventoryAction action = event.getAction();
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || action == InventoryAction.COLLECT_TO_CURSOR) {
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
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Menu menu)) {
            return;
        }
        if (!menu.isEditable()) {
            event.setCancelled(true);
            return;
        }
        int size = top.getSize();
        for (int raw : event.getRawSlots()) {
            if (raw < size && menu.isProtectedSlot(raw)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (event.getInventory().getHolder() instanceof Menu menu) {
            menu.onClose(player, event);
        }
    }
}

package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.managers.MenuLayoutManager;
import com.vduels.model.Kit;
import com.vduels.util.Items;
import com.vduels.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * The GUI opened by {@code /adminduel}. The top five rows are freely editable:
 * kit icons are pre-placed and the admin can rearrange them and add decoration
 * such as black stained glass. The bottom row holds protected controls. Closing
 * (or clicking Save) persists slots 0-44 as the /duel menu layout.
 */
public class AdminDuelMenu extends Menu {

    private static final int SAVE_SLOT = 49;
    private static final int CANCEL_SLOT = 45;
    private static final int PALETTE_SLOT = 47;

    private final VDuels plugin;
    private boolean cancelled = false;
    private boolean saved = false;

    public AdminDuelMenu(VDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public boolean isProtectedSlot(int rawSlot) {
        // Only the bottom control row is locked.
        return rawSlot >= MenuLayoutManager.EDITABLE_SLOTS && rawSlot < 54;
    }

    @Override
    public void build() {
        create(6, "&8Duel Menu Editor");

        // Pre-fill the editable area.
        if (plugin.getMenuLayoutManager().hasLayout()) {
            for (Map.Entry<Integer, ItemStack> entry : plugin.getMenuLayoutManager().getLayout().entrySet()) {
                inventory.setItem(entry.getKey(), entry.getValue());
            }
        } else {
            int slot = 0;
            for (Kit kit : plugin.getKitManager().all()) {
                if (slot >= MenuLayoutManager.EDITABLE_SLOTS) {
                    break;
                }
                inventory.setItem(slot++, kitIcon(kit));
            }
        }

        // Bottom control row.
        ItemStack lock = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, lock);
        }
        inventory.setItem(CANCEL_SLOT, Items.of(Material.BARRIER)
                .name("&cCancel")
                .lore("&7Close without saving changes.")
                .build());
        inventory.setItem(PALETTE_SLOT, Items.of(Material.BLACK_STAINED_GLASS_PANE)
                .name("&8Decoration")
                .lore("&7Click to get black stained glass",
                        "&7panes to decorate the menu with.")
                .build());
        inventory.setItem(SAVE_SLOT, Items.of(Material.LIME_DYE)
                .name("&a&lSave Layout")
                .lore("&7Save the current arrangement as the",
                        "&7layout players see in &e/duel&7.")
                .build());
    }

    private ItemStack kitIcon(Kit kit) {
        return Items.of(kit.getIcon())
                .name("&e" + kit.getName())
                .lore("&7Kit icon - drag to rearrange.")
                .tag(plugin.keyKit(), kit.getName())
                .build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == SAVE_SLOT) {
            persist();
            saved = true;
            player.sendMessage(Text.prefixed("&aDuel menu layout saved."));
            player.closeInventory();
        } else if (slot == CANCEL_SLOT) {
            cancelled = true;
            player.sendMessage(Text.prefixed("&7Editor closed without saving."));
            player.closeInventory();
        } else if (slot == PALETTE_SLOT) {
            player.getInventory().addItem(Items.of(Material.BLACK_STAINED_GLASS_PANE, 16)
                    .name("&8Decoration").build());
            player.sendMessage(Text.prefixed("&7Added decoration panes to your inventory."));
        }
    }

    @Override
    public void onClose(Player player, InventoryCloseEvent event) {
        if (cancelled || saved) {
            return; // already handled
        }
        persist();
        player.sendMessage(Text.prefixed("&aDuel menu layout saved."));
    }

    private void persist() {
        Map<Integer, ItemStack> layout = new HashMap<>();
        for (int i = 0; i < MenuLayoutManager.EDITABLE_SLOTS; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                layout.put(i, item.clone());
            }
        }
        plugin.getMenuLayoutManager().save(layout);
    }
}

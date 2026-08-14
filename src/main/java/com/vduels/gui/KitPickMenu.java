package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.managers.GuiLayoutManager;
import com.vduels.model.Kit;
import com.vduels.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * The kit picker opened first by {@code /duel}. Kit icons and decoration can be
 * arranged with {@code /editgui kitmenu} (or {@code /adminduel}); the saved
 * layout decides placement. Clicking a kit selects it and opens DUEL CONFIRM.
 */
public class KitPickMenu extends Menu {

    private static final int BACK_SLOT = 49;

    private final VDuels plugin;
    private final DuelConfirmMenu confirm;

    public KitPickMenu(VDuels plugin, DuelConfirmMenu confirm) {
        this.plugin = plugin;
        this.confirm = confirm;
    }

    @Override
    public void build() {
        create(6, "&8Select a Kit");

        if (plugin.getGuiLayoutManager().has(GuiLayoutManager.KIT_MENU)) {
            for (Map.Entry<Integer, ItemStack> e : plugin.getGuiLayoutManager().get(GuiLayoutManager.KIT_MENU).entrySet()) {
                if (e.getKey() >= 45) {
                    continue;
                }
                String kitName = Items.readTag(e.getValue(), plugin.keyKit());
                if (kitName != null) {
                    ItemStack icon = liveKitIcon(kitName);
                    if (icon != null) {
                        inventory.setItem(e.getKey(), icon);
                    }
                } else {
                    inventory.setItem(e.getKey(), e.getValue());
                }
            }
        } else {
            int slot = 0;
            for (Kit kit : plugin.getKitManager().all()) {
                if (slot >= 45) {
                    break;
                }
                inventory.setItem(slot++, liveKitIcon(kit.getName()));
            }
        }

        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(BACK_SLOT, Items.of(Material.ARROW)
                .name("&eBack").lore("&7Close the duel menu.").build());
    }

    private ItemStack liveKitIcon(String name) {
        Kit kit = plugin.getKitManager().get(name);
        if (kit == null) {
            return null;
        }
        return Items.of(kit.getIcon())
                .name("&e" + kit.getName())
                .lore("", "&7Click to select")
                .tag(plugin.keyKit(), kit.getName())
                .build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        if (event.getRawSlot() == BACK_SLOT) {
            if (confirm.getSelectedKit() == null) {
                player.closeInventory();
            } else {
                confirm.reopen(player);
            }
            return;
        }
        String kitName = Items.readTag(event.getCurrentItem(), plugin.keyKit());
        if (kitName != null && plugin.getKitManager().exists(kitName)) {
            confirm.setSelectedKit(kitName);
            confirm.reopen(player);
        }
    }
}

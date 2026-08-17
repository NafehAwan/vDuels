package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.managers.TabEditManager;
import com.vduels.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * {@code /vduelstab}: a small chest to edit the fight tab's title, discord and
 * store lines. Clicking a field starts a chat prompt to type the new value.
 */
public class TabConfigMenu extends Menu {

    private final VDuels plugin;

    public TabConfigMenu(VDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public void build() {
        create(3, "&7&lTab Settings");
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(11, Items.of(Material.BOOK)
                .name("&eTitle")
                .lore("&7Current:", "&f" + plugin.getTabTitle(),
                        "", "&aClick to change &7(MiniMessage / gradients)")
                .tag(plugin.keyButton(), "title")
                .build());
        inventory.setItem(13, Items.of(Material.PAPER)
                .name("&bDiscord")
                .lore("&7Current: &f" + plugin.getTabDiscord(), "", "&aClick to change")
                .tag(plugin.keyButton(), "discord")
                .build());
        inventory.setItem(15, Items.of(Material.GOLDEN_APPLE)
                .name("&6Store")
                .lore("&7Current: &f" + plugin.getTabStore(), "", "&aClick to change")
                .tag(plugin.keyButton(), "store")
                .build());
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String button = Items.readTag(event.getCurrentItem(), plugin.keyButton());
        if (button == null) {
            return;
        }
        TabEditManager.Field field = switch (button) {
            case "title" -> TabEditManager.Field.TITLE;
            case "discord" -> TabEditManager.Field.DISCORD;
            case "store" -> TabEditManager.Field.STORE;
            default -> null;
        };
        if (field != null) {
            player.closeInventory();
            plugin.getTabEditManager().begin(player, field);
        }
    }
}

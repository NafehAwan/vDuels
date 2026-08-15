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
 * The kit picker (small chest) opened first by {@code /duel}. Clicking a kit
 * selects it; the admin-placed &quot;next&quot; arrow advances to DUEL CONFIRM.
 * Layout is editable with {@code /editgui kitmenu}.
 */
public class KitPickMenu extends Menu {

    private final VDuels plugin;
    private final DuelConfirmMenu confirm;

    public KitPickMenu(VDuels plugin, DuelConfirmMenu confirm) {
        this.plugin = plugin;
        this.confirm = confirm;
    }

    public void reopen(Player player) {
        build();
        player.openInventory(inventory);
    }

    @Override
    public void build() {
        create(3, "&8Select a Kit");

        if (plugin.getGuiLayoutManager().has(GuiLayoutManager.KIT_MENU)) {
            for (Map.Entry<Integer, ItemStack> e : plugin.getGuiLayoutManager().get(GuiLayoutManager.KIT_MENU).entrySet()) {
                if (e.getKey() >= 27) {
                    continue;
                }
                String button = Items.readTag(e.getValue(), plugin.keyButton());
                if (button != null) {
                    inventory.setItem(e.getKey(), renderButton(button));
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
                if (slot >= 26) {
                    break;
                }
                inventory.setItem(slot++, liveKitIcon(kit.getName()));
            }
            inventory.setItem(26, renderButton("next"));
        }
    }

    private ItemStack renderButton(String id) {
        if (id.equals("next")) {
            return Items.of(Material.ARROW)
                    .name("&eNext")
                    .lore("&fContinue to the duel menu.")
                    .tag(plugin.keyButton(), "next")
                    .build();
        }
        return Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
    }

    private ItemStack liveKitIcon(String name) {
        Kit kit = plugin.getKitManager().get(name);
        if (kit == null) {
            return null;
        }
        boolean selected = kit.getName().equalsIgnoreCase(confirm.getSelectedKit());
        return Items.of(kit.getIcon())
                .name((selected ? "&a" : "&e") + kit.getName())
                .lore("", selected ? "&aSelected" : "&fClick to select")
                .glow(selected)
                .tag(plugin.keyKit(), kit.getName())
                .build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        String button = Items.readTag(clicked, plugin.keyButton());
        if ("next".equals(button)) {
            if (confirm.getSelectedKit() == null) {
                player.sendMessage(plugin.messages().get("menu.select-kit-first"));
                return;
            }
            confirm.reopen(player);
            return;
        }
        String kitName = Items.readTag(clicked, plugin.keyKit());
        if (kitName != null && plugin.getKitManager().exists(kitName)) {
            confirm.setSelectedKit(kitName);
            reopen(player);
        }
    }
}

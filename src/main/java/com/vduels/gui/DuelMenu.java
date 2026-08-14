package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.model.Kit;
import com.vduels.util.Items;
import com.vduels.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * The GUI opened by {@code /duel <player>}. The player picks a kit (from the
 * admin-customised layout or a default grid), chooses the number of rounds and
 * clicks send. The bottom row holds the live controls.
 */
public class DuelMenu extends Menu {

    private static final int[] ROUND_OPTIONS = {1, 2, 3, 5};

    private final VDuels plugin;
    private final Player target;
    private String selectedKit;
    private int roundsIndex = 1; // default: first to 2

    public DuelMenu(VDuels plugin, Player target) {
        this.plugin = plugin;
        this.target = target;
    }

    @Override
    public void build() {
        create(6, "&8Duel: &b" + target.getName());

        // Top area (0-44): admin layout if present, otherwise a default grid.
        if (plugin.getMenuLayoutManager().hasLayout()) {
            Map<Integer, ItemStack> layout = plugin.getMenuLayoutManager().getLayout();
            for (Map.Entry<Integer, ItemStack> entry : layout.entrySet()) {
                inventory.setItem(entry.getKey(), decorateForSelection(entry.getValue()));
            }
        } else {
            int slot = 0;
            for (Kit kit : plugin.getKitManager().all()) {
                if (slot >= MenuLayoutBounds()) {
                    break;
                }
                inventory.setItem(slot++, kitIcon(kit));
            }
        }

        // Bottom control row (45-53)
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(45, Items.of(Material.BARRIER)
                .name("&cCancel")
                .lore("&7Close without sending.")
                .build());

        inventory.setItem(48, Items.of(Material.COMPARATOR)
                .name("&eRounds: &ffirst to " + currentRounds())
                .lore("&7Click to change.")
                .build());

        if (selectedKit != null) {
            Kit kit = plugin.getKitManager().get(selectedKit);
            inventory.setItem(49, Items.of(kit != null ? kit.getIcon() : Material.PAPER)
                    .name("&aSelected: &f" + selectedKit)
                    .glow(true)
                    .build());
        } else {
            inventory.setItem(49, Items.of(Material.PAPER)
                    .name("&7No kit selected")
                    .lore("&7Click a kit above.")
                    .build());
        }

        inventory.setItem(50, Items.of(selectedKit != null ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(selectedKit != null ? "&a&lSEND CHALLENGE" : "&7Select a kit first")
                .lore("&7Challenge &f" + target.getName() + "&7.")
                .glow(selectedKit != null)
                .build());
    }

    private int MenuLayoutBounds() {
        return com.vduels.managers.MenuLayoutManager.EDITABLE_SLOTS;
    }

    private ItemStack kitIcon(Kit kit) {
        boolean sel = kit.getName().equalsIgnoreCase(selectedKit);
        return Items.of(kit.getIcon())
                .name((sel ? "&a" : "&e") + kit.getName())
                .lore("", sel ? "&aSelected" : "&7Click to select")
                .glow(sel)
                .tag(plugin.keyKit(), kit.getName())
                .build();
    }

    /** If a layout item is a kit icon that is currently selected, make it glow. */
    private ItemStack decorateForSelection(ItemStack layoutItem) {
        if (layoutItem == null) {
            return null;
        }
        String kitName = Items.readTag(layoutItem, plugin.keyKit());
        if (kitName != null && kitName.equalsIgnoreCase(selectedKit)) {
            ItemStack clone = layoutItem.clone();
            org.bukkit.inventory.meta.ItemMeta meta = clone.getItemMeta();
            if (meta != null) {
                meta.setEnchantmentGlintOverride(true);
                clone.setItemMeta(meta);
            }
            return clone;
        }
        return layoutItem;
    }

    private int currentRounds() {
        return ROUND_OPTIONS[roundsIndex];
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == 45) {
            player.closeInventory();
            return;
        }
        if (slot == 48) {
            roundsIndex = (roundsIndex + 1) % ROUND_OPTIONS.length;
            build();
            player.openInventory(inventory);
            return;
        }
        if (slot == 50) {
            if (selectedKit == null) {
                player.sendMessage(Text.prefixed("&cSelect a kit first."));
                return;
            }
            Player online = plugin.getServer().getPlayer(target.getUniqueId());
            if (online == null) {
                player.sendMessage(Text.prefixed("&c" + target.getName() + " is no longer online."));
                player.closeInventory();
                return;
            }
            plugin.getDuelManager().sendRequest(player, online, selectedKit, currentRounds());
            player.closeInventory();
            return;
        }

        // Top area: kit selection via tag.
        if (slot >= 0 && slot < MenuLayoutBounds()) {
            ItemStack clicked = event.getCurrentItem();
            String kitName = Items.readTag(clicked, plugin.keyKit());
            if (kitName != null && plugin.getKitManager().exists(kitName)) {
                selectedKit = kitName;
                build();
                player.openInventory(inventory);
            }
        }
    }
}

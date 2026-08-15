package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.model.Arena;
import com.vduels.model.Kit;
import com.vduels.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Grid of every kit the admins have created. Clicking a kit toggles whether the
 * arena supports it. An empty selection means "all kits allowed".
 */
public class KitSelectMenu extends Menu {

    private final VDuels plugin;
    private final Arena arena;
    private List<Kit> kits;

    public KitSelectMenu(VDuels plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    @Override
    public void build() {
        create(6, "&8Compatible Kits: &b" + arena.getName());
        this.kits = plugin.getKitManager().all();

        if (kits.isEmpty()) {
            inventory.setItem(22, Items.of(Material.BARRIER)
                    .name("&cNo kits created yet")
                    .lore("&7Use &e/kitcreate <name>&7 first.")
                    .build());
        }

        for (int i = 0; i < kits.size() && i < 45; i++) {
            Kit kit = kits.get(i);
            boolean enabled = arena.supportsKitExplicit(kit.getName());
            inventory.setItem(i, Items.of(kit.getIcon())
                    .name((enabled ? "&a" : "&7") + kit.getName())
                    .lore("",
                            enabled ? "&aENABLED &8(click to disable)" : "&cDISABLED &8(click to enable)")
                    .glow(enabled)
                    .build());
        }

        // Bottom bar
        ItemStack filler = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(49, Items.of(Material.ARROW)
                .name("&eBack")
                .lore("&7Return to the arena menu.")
                .build());
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 49) {
            new ArenaMenu(plugin, arena).open(player);
            return;
        }
        if (slot < 0 || slot >= 45 || kits == null || slot >= kits.size()) {
            return;
        }
        Kit kit = kits.get(slot);
        if (arena.supportsKitExplicit(kit.getName())) {
            arena.getKits().removeIf(k -> k.equalsIgnoreCase(kit.getName()));
        } else {
            arena.getKits().add(kit.getName());
        }
        plugin.getArenaManager().save();
        build();
        player.openInventory(inventory);
    }
}

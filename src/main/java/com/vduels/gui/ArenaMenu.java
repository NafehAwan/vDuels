package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.model.Arena;
import com.vduels.util.Items;
import com.vduels.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The GUI opened by {@code /arena <name>}. When the arena is not yet configured
 * it shows a single green "Setup" pane in the middle that launches the chat
 * wizard. Once configured it shows all the toggleable settings plus the kit and
 * duplicator controls.
 */
public class ArenaMenu extends Menu {

    private final VDuels plugin;
    private final Arena arena;

    public ArenaMenu(VDuels plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    @Override
    public void build() {
        create(3, "&8Arena: &b" + arena.getName());

        ItemStack filler = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        if (!arena.isConfigured()) {
            inventory.setItem(13, Items.of(Material.GREEN_STAINED_GLASS_PANE)
                    .name("&a&lSetup")
                    .lore("&7Click to start arena setup.",
                            "&7You'll be asked to walk to each",
                            "&7spawn and corner and type &edone&7.")
                    .build());
            return;
        }

        // Reconfigure (re-run the wizard)
        inventory.setItem(10, Items.of(Material.GREEN_STAINED_GLASS_PANE)
                .name("&a&lSetup")
                .lore("&7Re-run the position setup wizard.")
                .build());

        inventory.setItem(11, toggle("&bAuto-Regenerate",
                arena.isAutoRegenerate(),
                "&7Restore the arena after each fight."));
        inventory.setItem(12, toggle("&bAllow Block Break",
                arena.isAllowBreak(),
                "&7Can players break arena blocks?"));
        inventory.setItem(13, toggle("&bAllow Block Place",
                arena.isAllowPlace(),
                "&7Can players place blocks?"));
        inventory.setItem(14, toggle("&bRemove Own Blocks",
                arena.isAllowRemoveAdded(),
                "&7Can players break blocks they placed",
                "&7during the fight?"));
        inventory.setItem(15, toggle("&bDuplicator (FAWE-style)",
                arena.isDuplicatorEnabled(),
                "&7Enable &e/vduels " + arena.getName() + " copy&7 and",
                "&e/vduels " + arena.getName() + " paste&7."));

        inventory.setItem(16, Items.of(Material.CHEST)
                .name("&e&lCompatible Kits")
                .lore("&7Choose which kits can be used here.",
                        "&7Currently: &f" + arena.getKits().size() + " kit(s)",
                        "&8(none = all kits allowed)")
                .build());

        inventory.setItem(22, Items.of(Material.BOOK)
                .name("&7Arena Info")
                .lore("&7World: &f" + arena.getWorldName(),
                        "&7Region: &f" + describeRegion(),
                        "&8Changes save automatically.")
                .build());
    }

    private String describeRegion() {
        if (arena.getMin() == null) {
            return "not set";
        }
        return arena.getMin().getBlockX() + "," + arena.getMin().getBlockY() + "," + arena.getMin().getBlockZ()
                + " → " + arena.getMax().getBlockX() + "," + arena.getMax().getBlockY() + "," + arena.getMax().getBlockZ();
    }

    private ItemStack toggle(String name, boolean on, String... lore) {
        Material mat = on ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        java.util.List<String> lines = new java.util.ArrayList<>(java.util.Arrays.asList(lore));
        lines.add("");
        lines.add(on ? "&aENABLED &8(click to disable)" : "&cDISABLED &8(click to enable)");
        return Items.of(mat).name(name).lore(lines).build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 27) {
            return;
        }

        if (!arena.isConfigured()) {
            if (slot == 13) {
                player.closeInventory();
                plugin.getSetupManager().begin(player, arena);
            }
            return;
        }

        switch (slot) {
            case 10 -> {
                player.closeInventory();
                plugin.getSetupManager().begin(player, arena);
                return;
            }
            case 11 -> arena.setAutoRegenerate(!arena.isAutoRegenerate());
            case 12 -> arena.setAllowBreak(!arena.isAllowBreak());
            case 13 -> arena.setAllowPlace(!arena.isAllowPlace());
            case 14 -> arena.setAllowRemoveAdded(!arena.isAllowRemoveAdded());
            case 15 -> arena.setDuplicatorEnabled(!arena.isDuplicatorEnabled());
            case 16 -> {
                new KitSelectMenu(plugin, arena).open(player);
                return;
            }
            default -> {
                return;
            }
        }
        plugin.getArenaManager().save();
        build();
        player.openInventory(inventory);
    }
}

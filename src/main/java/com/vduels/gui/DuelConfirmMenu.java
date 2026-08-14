package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.model.Kit;
import com.vduels.util.Items;
import com.vduels.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The GUI opened by {@code /duel <player>}: DUEL CONFIRM. Compact controls -
 * a map (arena), a kit, a clock (rounds) and a green pane to confirm.
 */
public class DuelConfirmMenu extends Menu {

    private static final int[] ROUND_OPTIONS = {1, 2, 3, 5};
    private static final int SLOT_MAP = 10;
    private static final int SLOT_KIT = 12;
    private static final int SLOT_CLOCK = 14;
    private static final int SLOT_CONFIRM = 16;

    private final VDuels plugin;
    private final Player target;
    private String selectedKit;
    private String selectedArena; // null = random / any free
    private int roundsIndex = 1;

    public DuelConfirmMenu(VDuels plugin, Player target) {
        this.plugin = plugin;
        this.target = target;
    }

    public Player getTarget() {
        return target;
    }

    public String getSelectedKit() {
        return selectedKit;
    }

    public void setSelectedKit(String kit) {
        this.selectedKit = kit;
        // A newly chosen kit may not be supported by the previously chosen arena.
        if (selectedArena != null) {
            var arena = plugin.getArenaManager().get(selectedArena);
            if (arena == null || !arena.supportsKit(kit)) {
                selectedArena = null;
            }
        }
    }

    public String getSelectedArena() {
        return selectedArena;
    }

    public void setSelectedArena(String arena) {
        this.selectedArena = arena;
    }

    public void reopen(Player player) {
        build();
        player.openInventory(inventory);
    }

    private int currentRounds() {
        return ROUND_OPTIONS[roundsIndex];
    }

    @Override
    public void build() {
        create(3, "&8DUEL CONFIRM: &b" + target.getName());
        ItemStack filler = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(SLOT_MAP, Items.of(Material.FILLED_MAP)
                .name("&bArena")
                .lore("&7Selected: &f" + (selectedArena == null ? "Random" : selectedArena),
                        "",
                        "&eClick to choose the map.")
                .build());

        Kit kit = selectedKit == null ? null : plugin.getKitManager().get(selectedKit);
        inventory.setItem(SLOT_KIT, Items.of(kit != null ? kit.getIcon() : Material.GOLDEN_APPLE)
                .name("&6Kit")
                .lore("&7Selected: &f" + (selectedKit == null ? "none" : selectedKit),
                        "",
                        "&eClick to choose a kit.")
                .glow(selectedKit != null)
                .build());

        inventory.setItem(SLOT_CLOCK, Items.of(Material.CLOCK)
                .name("&eRounds")
                .lore("&7First to &f" + currentRounds(),
                        "",
                        "&eClick to change.")
                .build());

        boolean ready = selectedKit != null;
        inventory.setItem(SLOT_CONFIRM, Items.of(ready ? Material.GREEN_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE)
                .name(ready ? "&a&lCONFIRM & SEND" : "&7Select a kit first")
                .lore("&7Challenge &f" + target.getName() + "&7.")
                .glow(ready)
                .build());
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        switch (event.getRawSlot()) {
            case SLOT_MAP -> new MapSelectMenu(plugin, this).open(player);
            case SLOT_KIT -> new KitPickMenu(plugin, this).open(player);
            case SLOT_CLOCK -> {
                roundsIndex = (roundsIndex + 1) % ROUND_OPTIONS.length;
                reopen(player);
            }
            case SLOT_CONFIRM -> {
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
                plugin.getDuelManager().sendRequest(player, online, selectedKit, currentRounds(), selectedArena);
                player.closeInventory();
            }
            default -> {
            }
        }
    }
}

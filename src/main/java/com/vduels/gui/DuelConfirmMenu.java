package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.managers.GuiLayoutManager;
import com.vduels.model.Kit;
import com.vduels.util.Items;
import com.vduels.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * The GUI opened after picking a kit: DUEL CONFIRM. Its four buttons (map, kit,
 * clock, confirm) can be rearranged and decorated with {@code /editgui
 * duelconfirm}; the button slots come from the saved layout, the live contents
 * come from here.
 */
public class DuelConfirmMenu extends Menu {

    private static final int[] ROUND_OPTIONS = {1, 2, 3, 5};

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
        create(4, "&8DUEL CONFIRM: &b" + target.getName());
        Map<String, Integer> buttonSlots = new HashMap<>();

        if (plugin.getGuiLayoutManager().has(GuiLayoutManager.DUEL_CONFIRM)) {
            for (Map.Entry<Integer, ItemStack> e : plugin.getGuiLayoutManager().get(GuiLayoutManager.DUEL_CONFIRM).entrySet()) {
                if (e.getKey() >= 36) {
                    continue;
                }
                String id = Items.readTag(e.getValue(), plugin.keyButton());
                if (id != null) {
                    buttonSlots.put(id, e.getKey());
                } else {
                    inventory.setItem(e.getKey(), e.getValue());
                }
            }
        } else {
            ItemStack gray = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
            for (int i = 0; i < 36; i++) {
                inventory.setItem(i, gray);
            }
        }

        inventory.setItem(buttonSlots.getOrDefault("map", 10), renderButton("map"));
        inventory.setItem(buttonSlots.getOrDefault("kit", 12), renderButton("kit"));
        inventory.setItem(buttonSlots.getOrDefault("clock", 14), renderButton("clock"));
        inventory.setItem(buttonSlots.getOrDefault("confirm", 16), renderButton("confirm"));
    }

    private ItemStack renderButton(String id) {
        return switch (id) {
            case "map" -> Items.of(Material.FILLED_MAP)
                    .name("&bArena")
                    .lore("&7Selected: &f" + (selectedArena == null ? "Random" : selectedArena),
                            "", "&eClick to choose the map.")
                    .tag(plugin.keyButton(), "map")
                    .build();
            case "kit" -> {
                Kit kit = selectedKit == null ? null : plugin.getKitManager().get(selectedKit);
                yield Items.of(kit != null ? kit.getIcon() : Material.GOLDEN_APPLE)
                        .name("&6Kit")
                        .lore("&7Selected: &f" + (selectedKit == null ? "none" : selectedKit),
                                "", "&eClick to choose a kit.")
                        .glow(selectedKit != null)
                        .tag(plugin.keyButton(), "kit")
                        .build();
            }
            case "clock" -> Items.of(Material.CLOCK)
                    .name("&eRounds")
                    .lore("&7First to &f" + currentRounds(), "", "&eClick to change.")
                    .tag(plugin.keyButton(), "clock")
                    .build();
            default -> {
                boolean ready = selectedKit != null;
                yield Items.of(ready ? Material.GREEN_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE)
                        .name(ready ? "&a&lCONFIRM & SEND" : "&fSelect a kit first")
                        .lore("&fChallenge &e" + target.getName() + "&f.")
                        .tag(plugin.keyButton(), "confirm")
                        .build();
            }
        };
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String id = Items.readTag(event.getCurrentItem(), plugin.keyButton());
        if (id == null) {
            return;
        }
        switch (id) {
            case "map" -> new MapSelectMenu(plugin, this).open(player);
            case "kit" -> new KitPickMenu(plugin, this).open(player);
            case "clock" -> {
                roundsIndex = (roundsIndex + 1) % ROUND_OPTIONS.length;
                reopen(player);
            }
            case "confirm" -> {
                if (selectedKit == null) {
                    player.sendMessage(plugin.messages().get("menu.select-kit-first"));
                    return;
                }
                Player online = plugin.getServer().getPlayer(target.getUniqueId());
                if (online == null) {
                    player.sendMessage(plugin.messages().get("menu.target-offline", "target", target.getName()));
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

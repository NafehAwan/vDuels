package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.KitPickMenu;
import com.meowduels.gui.MapSelectMenu;
import com.meowduels.gui.Menu;
import com.meowduels.managers.StatsManager;
import com.meowduels.model.Arena;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import com.meowduels.util.Ranks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The duel setup screen.
 *
 * <p>Deliberately not written in the plugin's own house style. Everything else
 * here uses small caps, hex ramps and gradients; this one window uses the
 * default font and the sixteen vanilla colours, because that is the language
 * every other duels plugin speaks and it is the screen people arrive at
 * already knowing how to read.
 *
 * <p>The grammar, once, so the buttons stop inventing their own:
 *
 * <ul>
 * <li>A heading is a symbol, the name, and the same symbol again. The symbol
 *     is the deeper shade of the heading's colour.
 * <li>Aqua opens something or sends something, gold changes a number, red
 *     stops.
 * <li>In the body, yellow is the part that varies - a value, or the key you
 *     press - and white is the words around it.
 * <li>No bold, no gradients, no small caps.
 * </ul>
 */
public class DuelConfirmMenu
extends Menu {
    private static final int[] ROUND_OPTIONS = new int[]{1, 2, 3, 5};

    private static final int ROWS = 3;
    private static final int SLOT_INFO = 10;
    private static final int SLOT_ROUNDS = 11;
    private static final int SLOT_MAP = 12;
    private static final int SLOT_KIT = 13;
    private static final int SLOT_RANKED = 14;
    private static final int SLOT_SEND = 16;
    private static final int SLOT_BACK = 22;

    // The five headings, spelled out rather than built, so the symbols and the
    // two shades stay paired.
    private static final String H_INFO = "<blue>◆ <aqua>Information <blue>◆";
    private static final String H_ROUNDS = "<gold>✦ <yellow>Rounds <gold>✦";
    private static final String H_MAP = "<aqua>⇄ Map Selection ⇄";
    private static final String H_KIT = "<aqua>⇄ Kit Selection ⇄";
    private static final String H_RANKED = "<gold>✦ <yellow>Ranked <gold>✦";
    private static final String H_SEND = "<aqua>✔ Send Duel ✔";
    private static final String H_BACK = "<red>✖ Go Back ✖";

    private final MeowDuels plugin;
    private final Player target;
    private String selectedKit;
    private String selectedArena;
    private int roundsIndex = 1;
    private boolean ranked = false;

    public DuelConfirmMenu(MeowDuels plugin, Player target) {
        this.plugin = plugin;
        this.target = target;
    }

    public Player getTarget() {
        return this.target;
    }

    public String getSelectedKit() {
        return this.selectedKit;
    }

    public void setSelectedKit(String kit) {
        Arena arena;
        this.selectedKit = kit;
        if (!(this.selectedArena == null || (arena = this.plugin.getArenaManager().get(this.selectedArena)) != null && arena.supportsKit(kit))) {
            this.selectedArena = null;
        }
    }

    public String getSelectedArena() {
        return this.selectedArena;
    }

    public void setSelectedArena(String arena) {
        this.selectedArena = arena;
    }

    public void reopen(Player player) {
        this.build();
        player.openInventory(this.inventory);
    }

    private int currentRounds() {
        return ROUND_OPTIONS[this.roundsIndex];
    }

    @Override
    public void build() {
        this.createRaw(ROWS, "Duel Request");
        Style.frame(this.inventory, ROWS);
        HashMap<String, Integer> buttonSlots = new HashMap<String, Integer>();
        if (this.plugin.getGuiLayoutManager().has("duelconfirm")) {
            for (Map.Entry<Integer, ItemStack> e : this.plugin.getGuiLayoutManager().get("duelconfirm").entrySet()) {
                if (e.getKey() >= ROWS * 9) continue;
                String id = Items.readTag(e.getValue(), this.plugin.keyButton());
                if (id != null) {
                    buttonSlots.put(id, e.getKey());
                    continue;
                }
                this.inventory.setItem(e.getKey().intValue(), e.getValue());
            }
        }
        this.inventory.setItem(buttonSlots.getOrDefault("info", SLOT_INFO).intValue(), this.renderButton("info"));
        this.inventory.setItem(buttonSlots.getOrDefault("clock", SLOT_ROUNDS).intValue(), this.renderButton("clock"));
        this.inventory.setItem(buttonSlots.getOrDefault("map", SLOT_MAP).intValue(), this.renderButton("map"));
        this.inventory.setItem(buttonSlots.getOrDefault("kit", SLOT_KIT).intValue(), this.renderButton("kit"));
        this.inventory.setItem(buttonSlots.getOrDefault("ranked", SLOT_RANKED).intValue(), this.renderButton("ranked"));
        this.inventory.setItem(buttonSlots.getOrDefault("confirm", SLOT_SEND).intValue(), this.renderButton("confirm"));
        this.inventory.setItem(buttonSlots.getOrDefault("cancel", SLOT_BACK).intValue(), this.renderButton("cancel"));
    }

    /** A value that has been chosen, or the grey word for not yet. */
    private static String value(String chosen, String whenEmpty) {
        return chosen == null || chosen.isEmpty() ? "<gray>" + whenEmpty : "<yellow>" + chosen;
    }

    private String arenaLabel() {
        return DuelConfirmMenu.value(this.selectedArena, "Random");
    }

    private String kitLabel() {
        Kit kit = this.selectedKit == null ? null : this.plugin.getKitManager().get(this.selectedKit);
        if (kit == null) {
            return "<gray>Not chosen";
        }
        String name = kit.getDisplayName();
        return name == null || name.isEmpty() ? "<yellow>" + kit.getName() : name;
    }

    private ItemStack renderButton(String id) {
        switch (id) {
            case "info": {
                // Everything the request will carry, in one item, so no button
                // below has to repeat it and nothing is decided from memory.
                return Items.of(Material.ITEM_FRAME)
                        .rawName(H_INFO)
                        .rawLore("",
                                 "<white>Kit: " + this.kitLabel(),
                                 "<white>Map: " + this.arenaLabel(),
                                 "<white>Rounds: <yellow>" + this.currentRounds(),
                                 "<white>Mode: " + (this.ranked ? "<yellow>Ranked" : "<gray>Unranked"),
                                 "",
                                 "<white>Opponent: <yellow>" + this.target.getName(),
                                 "<white>Rating: " + this.ratingLabel())
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "info")
                        .build();
            }
            case "clock": {
                return Items.of(Material.CLOCK)
                        .rawName(H_ROUNDS)
                        .rawLore("",
                                 "<yellow>Current <white>" + this.currentRounds() + " Rounds",
                                 "",
                                 "<yellow>LMB <white>Increase Rounds",
                                 "<yellow>RMB <white>Decrease Rounds")
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "clock")
                        .build();
            }
            case "map": {
                return Items.of(Material.PAPER)
                        .rawName(H_MAP)
                        .rawLore("",
                                 "<white>Current: " + this.arenaLabel(),
                                 "",
                                 "<white>Click to Open Menu!")
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "map")
                        .build();
            }
            case "kit": {
                Kit kit = this.selectedKit == null ? null : this.plugin.getKitManager().get(this.selectedKit);
                return Items.of(kit != null ? kit.getIcon() : Material.IRON_SWORD)
                        .rawName(H_KIT)
                        .rawLore("",
                                 "<white>Current: " + this.kitLabel(),
                                 "",
                                 "<white>Click to Open Menu!")
                        .glow(this.selectedKit != null)
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "kit")
                        .build();
            }
            case "ranked": {
                return Items.of(this.ranked ? Material.EXPERIENCE_BOTTLE : Material.GLASS_BOTTLE)
                        .rawName(H_RANKED)
                        .rawLore("",
                                 "<yellow>Current <white>" + (this.ranked ? "Ranked" : "Unranked"),
                                 "",
                                 "<white>" + (this.ranked ? "Winner gains Elo" : "No Elo is exchanged"),
                                 "",
                                 "<yellow>LMB <white>Toggle Ranked")
                        .glow(this.ranked)
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "ranked")
                        .build();
            }
            case "cancel": {
                return Items.of(Material.BARRIER)
                        .rawName(H_BACK)
                        .rawLore("<white>Go Back")
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "cancel")
                        .build();
            }
        }
        // Send. Grey and barrier-free when a kit is still missing: the button
        // stays where it is and says what it wants, rather than vanishing.
        boolean ready = this.selectedKit != null;
        List<String> lore = new ArrayList<String>();
        if (ready) {
            lore.add("<white>Click to Send Duel!");
        } else {
            lore.add("<red>Pick a kit first!");
        }
        return Items.of(ready ? Material.LIME_DYE : Material.GRAY_DYE)
                .rawName(ready ? H_SEND : "<gray>✖ Send Duel ✖")
                .rawLore(lore.toArray(new String[0]))
                .glow(ready)
                .hideTooltip()
                .tag(this.plugin.keyButton(), "confirm")
                .build();
    }

    private String ratingLabel() {
        StatsManager stats = this.plugin.getStatsManager();
        if (stats.isPlaced(this.target.getUniqueId())) {
            return Ranks.mini(stats, this.target.getUniqueId())
                    + " <dark_gray>· <yellow>" + stats.getElo(this.target.getUniqueId());
        }
        return "<gray>Unplaced <dark_gray>· <yellow>"
                + stats.placementsLeft(this.target.getUniqueId()) + " <white>to go";
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String id = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if (id == null) {
            return;
        }
        switch (id) {
            case "info": {
                break;
            }
            case "map": {
                new MapSelectMenu(this.plugin, this).open(player);
                break;
            }
            case "kit": {
                new KitPickMenu(this.plugin, this).open(player);
                break;
            }
            case "clock": {
                // Left goes up, right goes down, exactly as the button says.
                int step = event.isRightClick() ? ROUND_OPTIONS.length - 1 : 1;
                this.roundsIndex = (this.roundsIndex + step) % ROUND_OPTIONS.length;
                this.reopen(player);
                break;
            }
            case "ranked": {
                this.ranked = !this.ranked;
                this.reopen(player);
                break;
            }
            case "cancel": {
                player.closeInventory();
                break;
            }
            case "confirm": {
                if (this.selectedKit == null) {
                    player.sendMessage(this.plugin.messages().get("menu.select-kit-first", new String[0]));
                    return;
                }
                Player online = this.plugin.getServer().getPlayer(this.target.getUniqueId());
                if (online == null) {
                    player.sendMessage(this.plugin.messages().get("menu.target-offline", "target", this.target.getName()));
                    player.closeInventory();
                    return;
                }
                this.plugin.getDuelManager().sendRequest(player, online, this.selectedKit, this.currentRounds(), this.selectedArena, this.ranked);
                player.closeInventory();
                break;
            }
        }
    }
}

package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.KitPickMenu;
import com.meowduels.gui.MapSelectMenu;
import com.meowduels.gui.Menu;
import com.meowduels.model.Arena;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Round Selection: the last screen before a duel request goes out.
 *
 * <p>Not written in this plugin's house style, and not meant to be. The
 * default font, the vanilla colours and five items - it is the layout every
 * practice server uses, which makes it the one screen nobody has to learn.
 *
 * <p>The kit is already decided by the time this opens: {@code /duel <player>}
 * goes through the kit picker first, which is why there is no kit button here
 * and why Go Back returns there. Information reports the choice as Gamemode,
 * the same word the rest of the genre uses for it.
 *
 * <p>The grammar, since every line follows it: a heading is a symbol, the
 * name, and the same symbol again, with the symbol in the deeper shade of the
 * heading's colour. In the body, yellow is the part that varies - a value, or
 * the key you press - and white is the words around it. No bold, no gradient,
 * no small caps.
 */
public class DuelConfirmMenu
extends Menu {
    private static final int[] ROUND_OPTIONS = new int[]{1, 2, 3, 5};

    private static final int ROWS = 3;
    private static final int SLOT_INFO = 10;
    private static final int SLOT_ROUNDS = 11;
    private static final int SLOT_MAP = 12;
    private static final int SLOT_SEND = 14;
    private static final int SLOT_BACK = 22;

    private final MeowDuels plugin;
    private final Player sender;
    private final Player target;
    private String selectedKit;
    private String selectedArena;
    private int roundsIndex = 0;

    public DuelConfirmMenu(MeowDuels plugin, Player sender, Player target) {
        this.plugin = plugin;
        this.sender = sender;
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
        this.createRaw(ROWS, "Round Selection");
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
        this.inventory.setItem(buttonSlots.getOrDefault("confirm", SLOT_SEND).intValue(), this.renderButton("confirm"));
        this.inventory.setItem(buttonSlots.getOrDefault("cancel", SLOT_BACK).intValue(), this.renderButton("cancel"));
    }

    /** The kit, under the name the rest of the genre gives it. */
    private String gamemodeLabel() {
        Kit kit = this.selectedKit == null ? null : this.plugin.getKitManager().get(this.selectedKit);
        if (kit == null) {
            return "<yellow>None";
        }
        String name = kit.getDisplayName();
        return name == null || name.isEmpty() ? "<yellow>" + kit.getName() : name;
    }

    private ItemStack renderButton(String id) {
        switch (id) {
            case "clock": {
                return Items.of(Material.CLOCK)
                        .rawName("<gold>✦ <yellow>Rounds <gold>✦")
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
                        .rawName("<aqua>⇄ Map Selection ⇄")
                        .rawLore("<white>Click to Open Menu!")
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "map")
                        .build();
            }
            case "confirm": {
                return Items.of(Material.LIME_DYE)
                        .rawName("<aqua>✔ Send Duel ✔")
                        .rawLore("<white>Click to Send Duel!")
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "confirm")
                        .build();
            }
            case "cancel": {
                return Items.of(Material.BARRIER)
                        .rawName("<red>✖ Go Back ✖")
                        .rawLore("<white>Go Back")
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "cancel")
                        .build();
            }
        }
        return Items.of(Material.ITEM_FRAME)
                .rawName("<blue>◆ <aqua>Information <blue>◆")
                .rawLore("",
                         "<white>Gamemode: " + this.gamemodeLabel(),
                         "<white>Rounds: <yellow>" + this.currentRounds(),
                         "<white>Opponent: <yellow>" + this.target.getName(),
                         "<white>Sender: <yellow>" + this.sender.getName())
                .hideTooltip()
                .tag(this.plugin.keyButton(), "info")
                .build();
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
            case "clock": {
                // Clamped, not wrapped: the button says increase and decrease,
                // and a button that says increase should never go down.
                int next = this.roundsIndex + (event.isRightClick() ? -1 : 1);
                this.roundsIndex = Math.max(0, Math.min(ROUND_OPTIONS.length - 1, next));
                this.reopen(player);
                break;
            }
            case "cancel": {
                // Back to where the kit was chosen, which is where this window
                // was opened from.
                new KitPickMenu(this.plugin, this).open(player);
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
                this.plugin.getDuelManager().sendRequest(player, online, this.selectedKit, this.currentRounds(), this.selectedArena);
                player.closeInventory();
                break;
            }
        }
    }
}

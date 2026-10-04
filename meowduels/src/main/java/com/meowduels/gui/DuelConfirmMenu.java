package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.KitPickMenu;
import com.meowduels.gui.MapSelectMenu;
import com.meowduels.gui.Menu;
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.managers.GuiLayoutManager;
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

    /** The name of this screen's file: {@code gui/duelconfirm.yml}. */
    private static final String MENU = GuiLayoutManager.DUEL_CONFIRM;

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
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        this.createRaw(rows, gui.title(MENU, "Round Selection"));
        gui.frame(MENU, this.inventory, rows);
        HashMap<String, Integer> buttonSlots = new HashMap<String, Integer>();
        if (this.plugin.getGuiLayoutManager().has(MENU)) {
            for (Map.Entry<Integer, ItemStack> e : this.plugin.getGuiLayoutManager().get(MENU).entrySet()) {
                if (e.getKey() >= rows * 9) continue;
                String id = Items.readTag(e.getValue(), this.plugin.keyButton());
                if (id != null) {
                    buttonSlots.put(id, e.getKey());
                    continue;
                }
                this.inventory.setItem(e.getKey().intValue(), e.getValue());
            }
        }
        // Three sources, in this order: a layout an admin arranged with
        // /editgui wins, then gui/duelconfirm.yml, then the slot below. The
        // editor saving on close - even a close with no edits made - is why
        // a yml slot here can look ignored; /editgui duelconfirm reset clears
        // the stored layout and hands the file back control.
        this.place(buttonSlots, "info", gui.slot(MENU, "info", SLOT_INFO, rows), rows);
        this.place(buttonSlots, "clock", gui.slot(MENU, "clock", SLOT_ROUNDS, rows), rows);
        this.place(buttonSlots, "map", gui.slot(MENU, "map", SLOT_MAP, rows), rows);
        this.place(buttonSlots, "confirm", gui.slot(MENU, "confirm", SLOT_SEND, rows), rows);
        this.place(buttonSlots, "cancel", gui.slot(MENU, "cancel", SLOT_BACK, rows), rows);
    }

    /**
     * Puts a button down, keeping it inside the window.
     *
     * <p>The clamp is not defensive padding. Every default slot here was
     * picked for a three-row window, and {@code rows} is editable, so an owner
     * who shrinks this menu to two rows would otherwise have
     * {@code setItem(22)} throw on an eighteen-slot inventory and take the
     * screen down rather than merely look wrong.
     */
    private void place(Map<String, Integer> editorSlots, String id, int slot, int rows) {
        int at = editorSlots.getOrDefault(id, Integer.valueOf(slot)).intValue();
        if (at < 0 || at >= rows * 9) {
            at = Math.max(0, Math.min(rows * 9 - 1, at));
        }
        this.inventory.setItem(at, this.renderButton(id));
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

    /**
     * The tokens every item on this screen can use, whether it needs them or
     * not.
     *
     * <p>All five get the same map: which placeholders a line may contain is
     * then a property of the file rather than of the code, so an owner can
     * move "Rounds: 3" onto the clock or the opponent's name onto the confirm
     * button without a change here.
     */
    private Map<String, String> tokens() {
        HashMap<String, String> tokens = new HashMap<String, String>();
        tokens.put("rounds", String.valueOf(this.currentRounds()));
        tokens.put("gamemode", this.gamemodeLabel());
        tokens.put("opponent", this.target.getName());
        tokens.put("sender", this.sender.getName());
        return tokens;
    }

    private ItemStack renderButton(String id) {
        GuiConfigManager gui = this.plugin.gui();
        Map<String, String> tokens = this.tokens();
        switch (id) {
            case "clock": {
                return gui.item(MENU, "clock", tokens, Material.CLOCK,
                                "<gold>✦ <yellow>Rounds <gold>✦",
                                "",
                                "<yellow>Current <white>{rounds} Rounds",
                                "",
                                "<yellow>LMB <white>Increase Rounds",
                                "<yellow>RMB <white>Decrease Rounds")
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "clock")
                        .build();
            }
            case "map": {
                return gui.item(MENU, "map", tokens, Material.PAPER,
                                "<aqua>⇄ Map Selection ⇄",
                                "<white>Click to Open Menu!")
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "map")
                        .build();
            }
            case "confirm": {
                return gui.item(MENU, "confirm", tokens, Material.LIME_DYE,
                                "<aqua>✔ Send Duel ✔",
                                "<white>Click to Send Duel!")
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "confirm")
                        .build();
            }
            case "cancel": {
                return gui.item(MENU, "cancel", tokens, Material.BARRIER,
                                "<red>✖ Go Back ✖",
                                "<white>Go Back")
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "cancel")
                        .build();
            }
        }
        return gui.item(MENU, "info", tokens, Material.ITEM_FRAME,
                        "<blue>◆ <aqua>Information <blue>◆",
                        "",
                        "<white>Gamemode: {gamemode}",
                        "<white>Rounds: <yellow>{rounds}",
                        "<white>Opponent: <yellow>{opponent}",
                        "<white>Sender: <yellow>{sender}")
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

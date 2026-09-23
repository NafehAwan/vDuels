package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.KitPickMenu;
import com.meowduels.gui.MapSelectMenu;
import com.meowduels.gui.Menu;
import com.meowduels.gui.Style;
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
 * The duel setup screen: what you are fighting on, with what, for how long, and
 * whether it counts.
 *
 * <p>Four rows inside {@link Style}'s frame. The first interior row is the four
 * things you can change; the second is the two things that end the decision.
 * There is no head: the person you are challenging is already in the window
 * title, and a second copy of their face only pushed the actual choices apart.
 * The whole request is summarised in the confirm button's tooltip, which is the
 * one place you look before you press it.
 *
 * <p>Colour does the work. Green with a tick means it will send, red with a
 * cross means it will not, and everything in between is printed plainly -
 * gradients are spent on the title and the two decision buttons, nowhere else.
 * Names, kit titles and numbers stay in the normal font, because small caps
 * turn "Steve" into "ᴛᴇᴠᴇ".
 */
public class DuelConfirmMenu
extends Menu {
    private static final int[] ROUND_OPTIONS = new int[]{1, 2, 3, 5};

    private static final int ROWS = 4;
    private static final int SLOT_ARENA = 10;
    private static final int SLOT_KIT = 12;
    private static final int SLOT_ROUNDS = 14;
    private static final int SLOT_RANKED = 16;
    private static final int SLOT_CONFIRM = 21;
    private static final int SLOT_CANCEL = 23;

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
        this.createRaw(ROWS, Style.title("#FF2E55", "#FF7FC4",
                "ᴅᴜᴇʟ ʀᴇꞯᴜᴇꜱᴛ", this.target.getName()));
        // Frame first and always. A layout saved back when this menu was a
        // single row only covers slots 0-8, and without this the other three
        // rows would open as holes.
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
        this.inventory.setItem(buttonSlots.getOrDefault("map", SLOT_ARENA).intValue(), this.renderButton("map"));
        this.inventory.setItem(buttonSlots.getOrDefault("kit", SLOT_KIT).intValue(), this.renderButton("kit"));
        this.inventory.setItem(buttonSlots.getOrDefault("clock", SLOT_ROUNDS).intValue(), this.renderButton("clock"));
        this.inventory.setItem(buttonSlots.getOrDefault("ranked", SLOT_RANKED).intValue(), this.renderButton("ranked"));
        this.inventory.setItem(buttonSlots.getOrDefault("confirm", SLOT_CONFIRM).intValue(), this.renderButton("confirm"));
        this.inventory.setItem(buttonSlots.getOrDefault("cancel", SLOT_CANCEL).intValue(), this.renderButton("cancel"));
    }

    private String arenaLabel() {
        if (this.selectedArena == null) {
            return Style.MUTED + "ʀᴀɴᴅᴏᴍ";
        }
        return Style.VALUE + this.selectedArena;
    }

    private String kitLabel() {
        Kit kit = this.selectedKit == null ? null : this.plugin.getKitManager().get(this.selectedKit);
        if (kit == null) {
            return Style.BAD + "ɴᴏᴛ ᴄʜᴏꜱᴇɴ";
        }
        String name = kit.getDisplayName();
        return name == null || name.isEmpty() ? Style.VALUE + kit.getName() : name;
    }

    private String roundsLabel() {
        return Style.VALUE + "ꜰɪʀꜱᴛ ᴛᴏ " + this.currentRounds();
    }

    private ItemStack renderButton(String id) {
        switch (id) {
            case "map": {
                return Items.of(Material.FILLED_MAP)
                        .rawName(Style.VALUE + "ᴀʀᴇɴᴀ")
                        .rawLore("",
                                 Style.LABEL + "ɴᴏᴡ " + Style.SEP + this.arenaLabel(),
                                 "",
                                 Style.HINT + "ᴄʟɪᴄᴋ ᴛᴏ ᴘɪᴄᴋ ᴀ ᴍᴀᴘ")
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "map")
                        .build();
            }
            case "kit": {
                Kit kit = this.selectedKit == null ? null : this.plugin.getKitManager().get(this.selectedKit);
                return Items.of(kit != null ? kit.getIcon() : Material.IRON_SWORD)
                        .rawName(Style.VALUE + "ᴋɪᴛ")
                        .rawLore("",
                                 Style.LABEL + "ɴᴏᴡ " + Style.SEP + this.kitLabel(),
                                 "",
                                 Style.HINT + "ᴄʟɪᴄᴋ ᴛᴏ ᴘɪᴄᴋ ᴀ ᴋɪᴛ")
                        .glow(this.selectedKit != null)
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "kit")
                        .build();
            }
            case "clock": {
                // Every option listed, the live one marked. Cycling a hidden
                // value is the part of the old menu that made people click four
                // times to find out what the choices even were.
                String[] lore = new String[ROUND_OPTIONS.length + 4];
                lore[0] = "";
                for (int i = 0; i < ROUND_OPTIONS.length; ++i) {
                    boolean on = i == this.roundsIndex;
                    lore[i + 1] = on
                            ? Style.GOOD + Style.TICK + Style.VALUE + "ꜰɪʀꜱᴛ ᴛᴏ " + ROUND_OPTIONS[i]
                            : "<dark_gray>  " + Style.MUTED + "ꜰɪʀꜱᴛ ᴛᴏ " + ROUND_OPTIONS[i];
                }
                lore[ROUND_OPTIONS.length + 1] = "";
                lore[ROUND_OPTIONS.length + 2] = Style.HINT + "ᴄʟɪᴄᴋ ᴛᴏ ᴄʜᴀɴɢᴇ";
                lore[ROUND_OPTIONS.length + 3] = Style.HINT + "ʀɪɢʜᴛ-ᴄʟɪᴄᴋ ᴛᴏ ɢᴏ ʙᴀᴄᴋ";
                return Items.of(Material.CLOCK)
                        .rawName(Style.VALUE + "ʀᴏᴜɴᴅꜱ")
                        .rawLore(lore)
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "clock")
                        .build();
            }
            case "ranked": {
                return Items.of(this.ranked ? Material.EXPERIENCE_BOTTLE : Material.GLASS_BOTTLE)
                        .rawName(Style.VALUE + "ʀᴀɴᴋᴇᴅ")
                        .rawLore("",
                                 Style.state(this.ranked,
                                         "ᴡɪɴɴᴇʀ ɢᴀɪɴꜱ ᴇʟᴏ",
                                         "ɴᴏ ᴇʟᴏ ɪꜱ ᴇxᴄʜᴀɴɢᴇᴅ"),
                                 "",
                                 Style.HINT + "ᴄʟɪᴄᴋ ᴛᴏ ᴄʜᴀɴɢᴇ")
                        .glow(this.ranked)
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "ranked")
                        .build();
            }
            case "cancel": {
                return Style.cancel(this.plugin.keyButton(), "cancel",
                        "ᴄᴀɴᴄᴇʟ",
                        "",
                        Style.LABEL + "ᴄʟᴏꜱᴇ ᴡɪᴛʜᴏᴜᴛ ꜱᴇɴᴅɪɴɢ");
            }
        }
        return Style.confirm(this.plugin.keyButton(), "confirm", this.selectedKit != null,
                "ꜱᴇɴᴅ ʀᴇꞯᴜᴇꜱᴛ",
                "ᴘɪᴄᴋ ᴀ ᴋɪᴛ ꜰɪʀꜱᴛ",
                this.summary());
    }

    /**
     * Everything the request will carry, on the button that sends it. This used
     * to live under the target's head; it belongs on the thing you are about to
     * press, where you are already looking.
     */
    private String[] summary() {
        StatsManager stats = this.plugin.getStatsManager();
        String rating = stats.isPlaced(this.target.getUniqueId())
                ? Ranks.mini(stats, this.target.getUniqueId()) + " <dark_gray>· <#FF8A93>"
                  + stats.getElo(this.target.getUniqueId())
                : Style.MUTED + "ᴜɴᴘʟᴀᴄᴇᴅ " + Style.SEP + Style.VALUE
                  + stats.placementsLeft(this.target.getUniqueId()) + " " + Style.LABEL
                  + "ᴛᴏ ɢᴏ";
        List<String> lore = new ArrayList<String>();
        lore.add("");
        lore.add(Style.LABEL + "ᴏᴘᴘᴏɴᴇɴᴛ " + Style.SEP + Style.VALUE + this.target.getName());
        lore.add(Style.LABEL + "ʀᴀᴛɪɴɢ " + Style.SEP + rating);
        lore.add("");
        lore.add(Style.LABEL + "ᴀʀᴇɴᴀ " + Style.SEP + this.arenaLabel());
        lore.add(Style.LABEL + "ᴋɪᴛ " + Style.SEP + this.kitLabel());
        lore.add(Style.LABEL + "ʀᴏᴜɴᴅꜱ " + Style.SEP + this.roundsLabel());
        lore.add(Style.LABEL + "ᴍᴏᴅᴇ " + Style.SEP
                + (this.ranked ? Style.VALUE + "ʀᴀɴᴋᴇᴅ"
                               : Style.MUTED + "ᴜɴʀᴀɴᴋᴇᴅ"));
        if (this.selectedKit == null) {
            lore.add("");
            lore.add(Style.BAD + Style.CROSS + "ᴘɪᴄᴋ ᴀ ᴋɪᴛ ʙᴇꜰᴏʀᴇ ꜱᴇɴᴅɪɴɢ");
        }
        return lore.toArray(new String[0]);
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String id = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if (id == null) {
            return;
        }
        switch (id) {
            case "map": {
                new MapSelectMenu(this.plugin, this).open(player);
                break;
            }
            case "kit": {
                new KitPickMenu(this.plugin, this).open(player);
                break;
            }
            case "clock": {
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

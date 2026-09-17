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
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The duel setup screen: who you are challenging, on what, for how long, and
 * whether it counts.
 *
 * <p>Four rows. The top strip is a frame, the second row holds the four things
 * you can change, the third shows the person you are challenging with every
 * choice summarised under their head, and the last row sends or closes. Nothing
 * is written twice: the buttons carry the "what" and the head carries the
 * "so far", so a glance at the middle of the menu is the whole request.
 *
 * <p>Two fonts, on purpose. Labels are small-caps unicode; anything variable -
 * names, numbers, kit titles - stays in the normal font, because small caps
 * turns "Steve" into "\u1d1b\u1d07\u1d20\u1d07" and "First to 3" loses its digit weight.
 * Nothing is bold: at this size the gradient carries the emphasis, and bold on
 * top of it just muddies the colour ramp.
 */
public class DuelConfirmMenu
extends Menu {
    private static final int[] ROUND_OPTIONS = new int[]{1, 2, 3, 5};

    private static final int ROWS = 4;
    private static final int SIZE = ROWS * 9;
    private static final int SLOT_ARENA = 10;
    private static final int SLOT_KIT = 12;
    private static final int SLOT_ROUNDS = 14;
    private static final int SLOT_RANKED = 16;
    private static final int SLOT_TARGET = 22;
    private static final int SLOT_CONFIRM = 29;
    private static final int SLOT_CANCEL = 33;

    // One palette, used everywhere, so the menu reads as one object instead of
    // five differently-coloured buttons that happen to share a window.
    private static final String ACCENT_A = "#FF2E55";
    private static final String ACCENT_B = "#FF7FC4";
    private static final String VALUE = "<#E6E8EB>";
    private static final String LABEL = "<#8E959D>";
    private static final String MUTED = "<#6B7079>";
    private static final String SEP = "<dark_gray>\u203a ";
    private static final String HINT = "<dark_gray>\u25b8 <#8E959D>";

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

    private static String accent(String text) {
        return "<gradient:" + ACCENT_A + ":" + ACCENT_B + ">" + text + "</gradient>";
    }

    @Override
    public void build() {
        this.createRaw(ROWS, "<dark_gray>\u258f " + DuelConfirmMenu.accent("\u1d05\u1d1c\u1d07\u029f \u0280\u1d07\ua7af\u1d1c\u1d07\ua731\u1d1b") + " " + SEP + VALUE + this.target.getName());
        // Paint the frame first and always. A layout saved back when this menu
        // was a single row only covers slots 0-8, and without this the other
        // three rows would open as holes.
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).rawName(" ").build();
        for (int i = 0; i < SIZE; ++i) {
            this.inventory.setItem(i, filler);
        }
        HashMap<String, Integer> buttonSlots = new HashMap<String, Integer>();
        if (this.plugin.getGuiLayoutManager().has("duelconfirm")) {
            for (Map.Entry<Integer, ItemStack> e : this.plugin.getGuiLayoutManager().get("duelconfirm").entrySet()) {
                if (e.getKey() >= SIZE) continue;
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
        this.inventory.setItem(buttonSlots.getOrDefault("target", SLOT_TARGET).intValue(), this.renderButton("target"));
        this.inventory.setItem(buttonSlots.getOrDefault("confirm", SLOT_CONFIRM).intValue(), this.renderButton("confirm"));
        this.inventory.setItem(buttonSlots.getOrDefault("cancel", SLOT_CANCEL).intValue(), this.renderButton("cancel"));
    }

    private String arenaLabel() {
        if (this.selectedArena == null) {
            return MUTED + "\u0280\u1d00\u0274\u1d05\u1d0f\u1d0d";
        }
        return VALUE + this.selectedArena;
    }

    private String kitLabel() {
        Kit kit = this.selectedKit == null ? null : this.plugin.getKitManager().get(this.selectedKit);
        if (kit == null) {
            return MUTED + "\u0274\u1d0f\u1d1b \u1d04\u029c\u1d0f\ua731\u1d07\u0274";
        }
        String name = kit.getDisplayName();
        return name == null || name.isEmpty() ? VALUE + kit.getName() : name;
    }

    private String modeLabel() {
        return this.ranked ? "<gradient:#5CE1FF:#3E8BFF>\u0280\u1d00\u0274\u1d0b\u1d07\u1d05</gradient>" : MUTED + "\u1d1c\u0274\u0280\u1d00\u0274\u1d0b\u1d07\u1d05";
    }

    private ItemStack renderButton(String id) {
        switch (id) {
            case "map": {
                return Items.of(Material.FILLED_MAP)
                        .rawName(DuelConfirmMenu.accent("\u1d00\u0280\u1d07\u0274\u1d00"))
                        .rawLore("",
                                 LABEL + "\ua731\u1d07\u029f\u1d07\u1d04\u1d1b\u1d07\u1d05 " + SEP + this.arenaLabel(),
                                 "",
                                 HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d18\u026a\u1d04\u1d0b \u1d00 \u1d0d\u1d00\u1d18")
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "map")
                        .build();
            }
            case "kit": {
                Kit kit = this.selectedKit == null ? null : this.plugin.getKitManager().get(this.selectedKit);
                return Items.of(kit != null ? kit.getIcon() : Material.GOLDEN_APPLE)
                        .rawName(DuelConfirmMenu.accent("\u1d0b\u026a\u1d1b"))
                        .rawLore("",
                                 LABEL + "\ua731\u1d07\u029f\u1d07\u1d04\u1d1b\u1d07\u1d05 " + SEP + this.kitLabel(),
                                 "",
                                 HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d18\u026a\u1d04\u1d0b \u1d00 \u1d0b\u026a\u1d1b")
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
                            ? "<#FF7FC4>\u25b8 " + VALUE + "\ua730\u026a\u0280\ua731\u1d1b \u1d1b\u1d0f " + ROUND_OPTIONS[i]
                            : "<dark_gray>  " + MUTED + "\ua730\u026a\u0280\ua731\u1d1b \u1d1b\u1d0f " + ROUND_OPTIONS[i];
                }
                lore[ROUND_OPTIONS.length + 1] = "";
                lore[ROUND_OPTIONS.length + 2] = HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d04\u029c\u1d00\u0274\u0262\u1d07";
                lore[ROUND_OPTIONS.length + 3] = HINT + "\u0280\u026a\u0262\u029c\u1d1b-\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u0262\u1d0f \u0299\u1d00\u1d04\u1d0b";
                return Items.of(Material.CLOCK)
                        .rawName(DuelConfirmMenu.accent("\u0280\u1d0f\u1d1c\u0274\u1d05\ua731"))
                        .rawLore(lore)
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "clock")
                        .build();
            }
            case "ranked": {
                return Items.of(this.ranked ? Material.LIME_DYE : Material.GRAY_DYE)
                        .rawName(this.modeLabel())
                        .rawLore("",
                                 this.ranked ? LABEL + "\u1d21\u026a\u0274\u0274\u1d07\u0280 \u0262\u1d00\u026a\u0274\ua731 \u1d07\u029f\u1d0f" : LABEL + "\u0274\u1d0f \u1d07\u029f\u1d0f \u026a\ua731 \u1d07x\u1d04\u029c\u1d00\u0274\u0262\u1d07\u1d05",
                                 "",
                                 HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d04\u029c\u1d00\u0274\u0262\u1d07")
                        .glow(this.ranked)
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "ranked")
                        .build();
            }
            case "target": {
                return Items.of(Material.PLAYER_HEAD)
                        .skull(this.target)
                        .rawName(VALUE + this.target.getName())
                        .rawLore(this.targetLore())
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "target")
                        .build();
            }
            case "cancel": {
                return Items.of(Material.BARRIER)
                        .rawName("<gradient:#FF6B6B:#A01028>\u1d04\u1d00\u0274\u1d04\u1d07\u029f</gradient>")
                        .rawLore("", LABEL + "\u1d04\u029f\u1d0f\ua731\u1d07 \u1d21\u026a\u1d1b\u029c\u1d0f\u1d1c\u1d1b \ua731\u1d07\u0274\u1d05\u026a\u0274\u0262")
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), "cancel")
                        .build();
            }
        }
        boolean ready = this.selectedKit != null;
        return Items.of(ready ? Material.LIME_DYE : Material.GRAY_DYE)
                .rawName(ready ? "<gradient:#7CFF6B:#1FA32F>\ua731\u1d07\u0274\u1d05 \u0280\u1d07\ua7af\u1d1c\u1d07\ua731\u1d1b</gradient>" : MUTED + "\u1d18\u026a\u1d04\u1d0b \u1d00 \u1d0b\u026a\u1d1b \ua730\u026a\u0280\ua731\u1d1b")
                .rawLore("",
                         ready ? LABEL + "\u1d04\u029c\u1d00\u029f\u029f\u1d07\u0274\u0262\u026a\u0274\u0262 " + SEP + VALUE + this.target.getName()
                               : "<#FF5C5C>\u0280\u1d07\ua7af\u1d1c\u026a\u0280\u1d07\u1d05 " + SEP + LABEL + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d18\u026a\u1d04\u1d0b \u1d00 \u1d0b\u026a\u1d1b")
                .glow(ready)
                .hideTooltip()
                .tag(this.plugin.keyButton(), "confirm")
                .build();
    }

    /** Everything the request will carry, in one place, under their face. */
    private String[] targetLore() {
        StatsManager stats = this.plugin.getStatsManager();
        String rating = stats.isPlaced(this.target.getUniqueId())
                ? Ranks.mini(stats, this.target.getUniqueId()) + " <dark_gray>\u00b7 <#FF8A93>"
                  + stats.getElo(this.target.getUniqueId())
                : MUTED + "\u1d1c\u0274\u1d18\u029f\u1d00\u1d04\u1d07\u1d05 " + SEP + VALUE + stats.placementsLeft(this.target.getUniqueId())
                  + " " + LABEL + "\u1d18\u029f\u1d00\u1d04\u1d07\u1d0d\u1d07\u0274\u1d1b \u1d0d\u1d00\u1d1b\u1d04\u029c\u1d07\ua731 \u029f\u1d07\ua730\u1d1b";
        return new String[]{
            "",
            LABEL + "\u1d0d\u1d00\u1d18 " + SEP + this.arenaLabel(),
            LABEL + "\u1d0b\u026a\u1d1b " + SEP + this.kitLabel(),
            LABEL + "\u0280\u1d0f\u1d1c\u0274\u1d05\ua731 " + SEP + VALUE + "\ua730\u026a\u0280\ua731\u1d1b \u1d1b\u1d0f " + this.currentRounds(),
            LABEL + "\u1d0d\u1d0f\u1d05\u1d07 " + SEP + this.modeLabel(),
            "",
            LABEL + "\u0280\u1d00\u1d1b\u026a\u0274\u0262 " + SEP + rating
        };
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
            case "target": {
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

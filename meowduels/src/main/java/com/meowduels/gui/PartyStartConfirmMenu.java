package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.managers.GuiConfigManager.Face;
import com.meowduels.model.Kit;
import com.meowduels.model.Party;
import com.meowduels.model.PartyMode;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The last screen before a party match starts: green to go, red to go back.
 *
 * <p>Five slots, because it asks one question. The middle slot is not a button -
 * it is the answer sheet, showing the mode, the kit and how many are coming, so
 * the decision is made on what is actually about to happen rather than on
 * remembering what you clicked two windows ago.
 */
public class PartyStartConfirmMenu
extends Menu {
    /** The name of this screen's file: {@code gui/partystartconfirm.yml}. */
    private static final String MENU = "partystartconfirm";

    private static final int ROWS = 3;
    private static final int SLOT_BACK = 18;
    private static final int SLOT_WHAT = 12;
    private static final int SLOT_START = 14;

    private static final String VALUE = "<#E6E8EB>";
    private static final String LABEL = "<#8E959D>";
    private static final String MUTED = "<#6B7079>";
    private static final String SEP = "<dark_gray>\u203a ";

    private final MeowDuels plugin;
    private final PartyMode mode;

    public PartyStartConfirmMenu(MeowDuels plugin, PartyMode mode) {
        this.plugin = plugin;
        this.mode = mode;
    }

    @Override
    public void build() {
    }

    @Override
    public void open(Player player) {
        Party party = this.plugin.getPartyManager().partyOf(player.getUniqueId());
        if (party == null || !party.isLeader(player.getUniqueId())) {
            player.sendMessage(Text.prefixed("&cOnly the party leader can start a match."));
            return;
        }
        // A chest rather than the hopper this used to be: five slots in a row
        // cannot carry a border, and a window with no border next to one with
        // a border reads as a different plugin.
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        int size = rows * 9;
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("mode", this.mode.getLabel());
        tokens.put("description", this.mode.getDescription());
        tokens.put("kit", this.kitLabel(party));
        tokens.put("players", String.valueOf(party.size()));
        this.createRaw(rows, gui.title(MENU,
                "<dark_gray>\u258f <gradient:#7DE2FF:#4B7BFF>\ua731\u1d1b\u1d00\u0280\u1d1b \u1d0d\u1d00\u1d1b\u1d04\u029c</gradient>", tokens));
        gui.frame(MENU, this.inventory, rows);
        this.inventory.setItem(Math.min(size - 1, gui.slot(MENU, "back", SLOT_BACK, rows)),
                gui.back(MENU, "back").tag(this.plugin.keyButton(), "start-back").build());
        this.inventory.setItem(Math.min(size - 1, gui.slot(MENU, "what", SLOT_WHAT, rows)),
                gui.face(MENU, "what", tokens,
                         Face.of(Material.PAPER,
                                 "<gradient:#7DE2FF:#4B7BFF>{mode}</gradient>",
                                 "",
                                 "{label}\u1d0b\u026a\u1d1b {sep}{kit}",
                                 "{label}\u1d18\u029f\u1d00\u028f\u1d07\u0280\ua731 {sep}{value}{players}",
                                 "",
                                 "{label}{description}"))
                        .hideTooltip().build());
        this.inventory.setItem(Math.min(size - 1, gui.slot(MENU, "start", SLOT_START, rows)),
                gui.face(MENU, "start", tokens,
                         Face.of(Material.LIME_DYE,
                                 "<gradient:{go-from}:{go-to}>{tick}\ua731\u1d1b\u1d00\u0280\u1d1b</gradient>",
                                 "",
                                 "{label}\ua731\u1d07\u0274\u1d05 \u1d07\u1d20\u1d07\u0280\u028f\u1d0f\u0274\u1d07 \u026a\u0274 \u1d00\u0274\u1d05 \u0299\u1d07\u0262\u026a\u0274 \u1d1b\u029c\u1d07 \u1d04\u1d0f\u1d1c\u0274\u1d1b\u1d05\u1d0f\u1d21\u0274").glow())
                        .hideTooltip().tag(this.plugin.keyButton(), "start-go").build());
        player.openInventory(this.inventory);
    }

    private String kitLabel(Party party) {
        Kit kit = party.getKit() == null ? null : this.plugin.getKitManager().get(party.getKit());
        if (kit == null) {
            return MUTED + "\u0274\u1d0f\u1d1b \u1d04\u029c\u1d0f\ua731\u1d07\u0274";
        }
        return kit.getDisplayName() == null || kit.getDisplayName().isEmpty()
                ? VALUE + kit.getName() : kit.getDisplayName();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String id = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if ("start-back".equals(id)) {
            new PartyKitMenu(this.plugin, this.mode).open(player);
        } else if ("start-go".equals(id)) {
            player.closeInventory();
            // The mode this window was opened for, not the default. Dropping it
            // here is how a Split start used to run as a free-for-all.
            this.plugin.getPartyManager().startMatch(player, this.mode);
        }
    }
}

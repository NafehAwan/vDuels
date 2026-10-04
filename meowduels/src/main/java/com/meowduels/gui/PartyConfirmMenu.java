package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.managers.GuiConfigManager.Face;
import com.meowduels.model.Party;
import com.meowduels.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Five slots, two answers.
 *
 * <p>Three buttons in the middle of a framed window, the same shape every
 * other confirm in the plugin uses, because a hopper here was the one screen
 * that did not look like the rest. The middle slot spells out which of the two
 * things is about to happen - a leader clicking "leave" ends the party for
 * everyone, and that is not something to find out afterwards.
 */
public class PartyConfirmMenu
extends Menu {
    private static final int ROWS = 3;
    private static final int SLOT_NO = 11;
    private static final int SLOT_WHAT = 13;
    private static final int SLOT_YES = 15;

    private final MeowDuels plugin;
    private final boolean leader;

    public PartyConfirmMenu(MeowDuels plugin, Player player) {
        this.plugin = plugin;
        Party party = plugin.getPartyManager().partyOf(player.getUniqueId());
        this.leader = party != null && party.isLeader(player.getUniqueId());
    }

    /** The name of this screen's file: {@code gui/partyconfirm.yml}. */
    private static final String MENU = "partyconfirm";

    @Override
    public void build() {
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        int size = rows * 9;
        this.createRaw(rows, this.leader
                ? gui.titleOf(MENU, "leader",
                        "<dark_gray>\u258f <gradient:#FF6B6B:#A01028>\u1d05\u026a\ua731\u0299\u1d00\u0274\u1d05 \u1d18\u1d00\u0280\u1d1b\u028f</gradient>", null)
                : gui.titleOf(MENU, "member",
                        "<dark_gray>\u258f <gradient:#FF6B6B:#A01028>\u029f\u1d07\u1d00\u1d20\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f</gradient>", null));
        gui.frame(MENU, this.inventory, rows);
        // The three buttons spell out the two shapes this window takes. A
        // leader clicking through disbands for everyone, which is not
        // something to discover afterwards, so the middle slot says which it
        // is rather than relying on the title.
        this.inventory.setItem(Math.min(size - 1, gui.slot(MENU, "stay", SLOT_NO, rows)),
                gui.face(MENU, "stay", null,
                         Face.of(Material.LIME_DYE,
                                 "<gradient:{go-from}:{go-to}>{tick}\ua731\u1d1b\u1d00\u028f</gradient>",
                                 "",
                                 "{label}\u0274\u1d0f\u1d1b\u029c\u026a\u0274\u0262 \u1d04\u029c\u1d00\u0274\u0262\u1d07\ua731").glow())
                        .tag(this.plugin.keyButton(), "party-no").build());
        int whatSlot = this.leader
                ? gui.slot(MENU, "what-leader", SLOT_WHAT, rows)
                : gui.slot(MENU, "what-member", SLOT_WHAT, rows);
        this.inventory.setItem(Math.min(size - 1, whatSlot),
                (this.leader
                        ? gui.face(MENU, "what-leader", null,
                                   Face.of(Material.PAPER,
                                           "{value}\u1d05\u026a\ua731\u0299\u1d00\u0274\u1d05 \u1d18\u1d00\u0280\u1d1b\u028f",
                                           "",
                                           "<#FF8A93>\u1d07\u1d20\u1d07\u0280\u028f\u1d0f\u0274\u1d07 \u026a\ua731 \u0280\u1d07\u1d0d\u1d0f\u1d20\u1d07\u1d05 \ua730\u0280\u1d0f\u1d0d \u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f"))
                        : gui.face(MENU, "what-member", null,
                                   Face.of(Material.PAPER,
                                           "{value}\u029f\u1d07\u1d00\u1d20\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f",
                                           "",
                                           "{label}\u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f \u1d04\u1d00\u0280\u0280\u026a\u1d07\ua731 \u1d0f\u0274 \u1d21\u026a\u1d1b\u029c\u1d0f\u1d1c\u1d1b \u028f\u1d0f\u1d1c")))
                        .hideTooltip().build());
        int leaveSlot = this.leader
                ? gui.slot(MENU, "leave-leader", SLOT_YES, rows)
                : gui.slot(MENU, "leave-member", SLOT_YES, rows);
        this.inventory.setItem(Math.min(size - 1, leaveSlot),
                (this.leader
                        ? gui.face(MENU, "leave-leader", null,
                                   Face.of(Material.RED_DYE,
                                           "<gradient:{no-from}:{no-to}>{cross}\u1d05\u026a\ua731\u0299\u1d00\u0274\u1d05 \u1d18\u1d00\u0280\u1d1b\u028f</gradient>",
                                           "",
                                           "{label}\u1d1b\u029c\u026a\ua731 \u1d04\u1d00\u0274\u0274\u1d0f\u1d1b \u0299\u1d07 \u1d1c\u0274\u1d05\u1d0f\u0274\u1d07"))
                        : gui.face(MENU, "leave-member", null,
                                   Face.of(Material.RED_DYE,
                                           "<gradient:{no-from}:{no-to}>{cross}\u029f\u1d07\u1d00\u1d20\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f</gradient>",
                                           "",
                                           "{label}\u1d1b\u029c\u026a\ua731 \u1d04\u1d00\u0274\u0274\u1d0f\u1d1b \u0299\u1d07 \u1d1c\u0274\u1d05\u1d0f\u0274\u1d07")))
                        .hideTooltip().tag(this.plugin.keyButton(), "party-yes").build());
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String id = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if ("party-yes".equals(id)) {
            player.closeInventory();
            this.plugin.getPartyManager().leave(player);
        } else if ("party-no".equals(id)) {
            player.closeInventory();
        }
    }
}

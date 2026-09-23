package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
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

    @Override
    public void build() {
        this.createRaw(ROWS, this.leader
                ? "<dark_gray>\u258f <gradient:#FF6B6B:#A01028>\u1d05\u026a\ua731\u0299\u1d00\u0274\u1d05 \u1d18\u1d00\u0280\u1d1b\u028f</gradient>"
                : "<dark_gray>\u258f <gradient:#FF6B6B:#A01028>\u029f\u1d07\u1d00\u1d20\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f</gradient>");
        Style.frame(this.inventory, ROWS);
        this.inventory.setItem(SLOT_NO, Style.confirm(this.plugin.keyButton(), "party-no", true,
                "\ua731\u1d1b\u1d00\u028f", "\ua731\u1d1b\u1d00\u028f",
                "", Style.LABEL + "\u0274\u1d0f\u1d1b\u029c\u026a\u0274\u0262 \u1d04\u029c\u1d00\u0274\u0262\u1d07\ua731"));
        this.inventory.setItem(SLOT_WHAT, Items.of(Material.PAPER)
                .rawName(this.leader ? "<#E6E8EB>\u1d05\u026a\ua731\u0299\u1d00\u0274\u1d05 \u1d18\u1d00\u0280\u1d1b\u028f" : "<#E6E8EB>\u029f\u1d07\u1d00\u1d20\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f")
                .rawLore("", this.leader
                        ? "<#FF8A93>\u1d07\u1d20\u1d07\u0280\u028f\u1d0f\u0274\u1d07 \u026a\ua731 \u0280\u1d07\u1d0d\u1d0f\u1d20\u1d07\u1d05 \ua730\u0280\u1d0f\u1d0d \u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f"
                        : "<#8E959D>\u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f \u1d04\u1d00\u0280\u0280\u026a\u1d07\ua731 \u1d0f\u0274 \u1d21\u026a\u1d1b\u029c\u1d0f\u1d1c\u1d1b \u028f\u1d0f\u1d1c")
                .hideTooltip().build());
        this.inventory.setItem(SLOT_YES, Style.cancel(this.plugin.keyButton(), "party-yes",
                this.leader ? "\u1d05\u026a\ua731\u0299\u1d00\u0274\u1d05 \u1d18\u1d00\u0280\u1d1b\u028f" : "\u029f\u1d07\u1d00\u1d20\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f",
                "", Style.LABEL + "\u1d1b\u029c\u026a\ua731 \u1d04\u1d00\u0274\u0274\u1d0f\u1d1b \u0299\u1d07 \u1d1c\u0274\u1d05\u1d0f\u0274\u1d07"));
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

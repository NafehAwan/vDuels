package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.model.Party;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Who you can invite.
 *
 * <p>Only people who can actually accept: online, not you, and not already in a
 * party of their own. Listing everyone and answering "they're in a party" on
 * click is how a menu ends up feeling broken - the list should be the answer,
 * not the question.
 *
 * <p>Sized to the list. A fixed six rows for four candidates is mostly empty
 * glass, and the window says more about the plugin's layout than about who is
 * online.
 */
public class PartyInviteMenu
extends Menu {
    private static final int MAX_LIST_ROWS = 4;

    private static final String VALUE = "<#E6E8EB>";
    private static final String LABEL = "<#8E959D>";
    private static final String HINT = "<dark_gray>\u25b8 <#8E959D>";

    private final MeowDuels plugin;

    public PartyInviteMenu(MeowDuels plugin) {
        this.plugin = plugin;
    }

    /** Online, not me, not already in a party. */
    private List<Player> candidates(Player viewer) {
        ArrayList<Player> out = new ArrayList<Player>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(viewer.getUniqueId())) {
                continue;
            }
            if (this.plugin.getPartyManager().inParty(online.getUniqueId())) {
                continue;
            }
            out.add(online);
        }
        return out;
    }

    @Override
    public void build() {
        // Filled in open(), which is the only place that knows who is looking.
    }

    @Override
    public void open(Player player) {
        Party party = this.plugin.getPartyManager().partyOf(player.getUniqueId());
        if (party == null || !party.isLeader(player.getUniqueId())) {
            player.sendMessage(Text.prefixed("&cOnly the party leader can invite."));
            return;
        }
        List<Player> people = this.candidates(player);
        int rows = Style.gridRows(people.size(), MAX_LIST_ROWS);
        this.createRaw(rows, Style.title("#FF8AD0", "#B04BD6",
                "\u026a\u0274\u1d20\u026a\u1d1b\u1d07 \u1d1b\u1d0f \u1d18\u1d00\u0280\u1d1b\u028f"));
        Style.frame(this.inventory, rows);
        int limit = (rows - 2) * Style.PER_ROW;
        for (int i = 0; i < people.size() && i < limit; ++i) {
            Player target = people.get(i);
            boolean invited = party.isInvited(target.getUniqueId());
            this.inventory.setItem(Style.gridSlot(i), Items.of(Material.PLAYER_HEAD)
                    .skull(target)
                    .rawName(VALUE + target.getName())
                    .rawLore("", invited ? "<#7CFF6B>\u026a\u0274\u1d20\u026a\u1d1b\u1d07\u1d05 - \u1d21\u1d00\u026a\u1d1b\u026a\u0274\u0262 \ua730\u1d0f\u0280 \u1d1b\u029c\u1d07\u1d0d" : HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u026a\u0274\u1d20\u026a\u1d1b\u1d07")
                    .glow(invited).hideTooltip()
                    .tag(this.plugin.keyButton(), "invite:" + target.getName()).build());
        }
        if (people.isEmpty()) {
            this.inventory.setItem(Style.gridSlot(Style.PER_ROW / 2), Items.of(Material.BARRIER)
                    .rawName("<#FF8A93>\u0274\u1d0f\u0299\u1d0f\u1d05\u028f \u1d1b\u1d0f \u026a\u0274\u1d20\u026a\u1d1b\u1d07")
                    .rawLore("", LABEL + "\u1d07\u1d20\u1d07\u0280\u028f\u1d0f\u0274\u1d07 \u1d0f\u0274\u029f\u026a\u0274\u1d07 \u026a\ua731 \u1d00\u029f\u0280\u1d07\u1d00\u1d05\u028f \u026a\u0274 \u1d00 \u1d18\u1d00\u0280\u1d1b\u028f")
                    .hideTooltip().build());
        }
        this.inventory.setItem(Style.backSlot(rows), Style.back(this.plugin.keyButton(), "party-back"));
        player.openInventory(this.inventory);
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String id = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if (id == null) {
            return;
        }
        if ("party-back".equals(id)) {
            new PartyMenu(this.plugin).openFor(player);
            return;
        }
        if (!id.startsWith("invite:")) {
            return;
        }
        Player target = this.plugin.getServer().getPlayerExact(id.substring(7));
        if (target == null) {
            // They left between the window being drawn and the click. Redraw
            // rather than complain: the list is out of date either way.
            this.open(player);
            return;
        }
        // invite() does every check itself - leader, already in a party, party
        // mid-match - and says which one failed, so there is nothing to repeat
        // here.
        this.plugin.getPartyManager().invite(player, target);
        this.open(player);
    }
}

package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.model.Party;
import com.meowduels.model.PartyMode;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Who to fight in Party Duels.
 *
 * <p>One head per party, wearing its leader's skin, named after the leader,
 * with the roster underneath. A party is identified by who runs it, so the
 * leader's face is the thing you actually recognise across a lobby - a generic
 * icon with a count would make every party look the same.
 *
 * <p>Idle parties only. A party already in a match is not a choice, and
 * offering it just produces a click that fails.
 */
public class PartyOpponentMenu
extends Menu {
    private static final int MAX_LIST_ROWS = 4;

    private static final String VALUE = "<#E6E8EB>";
    private static final String LABEL = "<#8E959D>";
    private static final String MUTED = "<#6B7079>";
    private static final String HINT = "<dark_gray>▸ <#8E959D>";

    private final MeowDuels plugin;

    public PartyOpponentMenu(MeowDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public void build() {
        // Filled in open(): only there do we know whose party is asking.
    }

    @Override
    public void open(Player player) {
        Party party = this.plugin.getPartyManager().partyOf(player.getUniqueId());
        if (party == null || !party.isLeader(player.getUniqueId())) {
            player.sendMessage(Text.prefixed("&cOnly the party leader can start a match."));
            return;
        }
        List<Party> parties = this.plugin.getPartyManager().opponentParties(party);
        int rows = Style.gridRows(parties.size(), MAX_LIST_ROWS);
        this.createRaw(rows, Style.title("#7DE2FF", "#4B7BFF",
                "ᴘᴀʀᴛʏ ᴅᴜᴇʟꜱ")
                + " " + Style.SEP + VALUE + "ᴘɪᴄᴋ ᴀ ᴘᴀʀᴛʏ");
        Style.frame(this.inventory, rows);
        int limit = (rows - 2) * Style.PER_ROW;
        for (int i = 0; i < parties.size() && i < limit; ++i) {
            Party other = parties.get(i);
            this.inventory.setItem(Style.gridSlot(i), this.card(party, other));
        }
        if (parties.isEmpty()) {
            this.inventory.setItem(Style.gridSlot(Style.PER_ROW / 2), Items.of(Material.BARRIER)
                    .rawName("<#FF8A93>ɴᴏ ᴘᴀʀᴛɪᴇꜱ ᴛᴏ ꜰɪɢʜᴛ")
                    .rawLore("",
                             LABEL + "ɴᴏʙᴏᴅʏ ᴇʟꜱᴇ ʜᴀꜱ ᴀ ᴘᴀʀᴛʏ ʀɪɢʜᴛ ɴᴏᴡ",
                             "", MUTED + "ᴛʜᴇʏ ɴᴇᴇᴅ ᴛᴏ ʙᴇ ᴏᴜᴛ ᴏꜰ ᴀ ᴍᴀᴛᴄʜ")
                    .hideTooltip().build());
        }
        this.inventory.setItem(Style.backSlot(rows), Style.back(this.plugin.keyButton(), "opp-back"));
        player.openInventory(this.inventory);
    }

    /** One party: the leader's head, their name, and who is in it. */
    private ItemStack card(Party mine, Party other) {
        UUID leaderId = other.getLeader();
        String leaderName = this.nameOf(leaderId);
        Items item = Items.of(Material.PLAYER_HEAD)
                .skull(Bukkit.getOfflinePlayer((UUID)leaderId))
                .rawName(PartyModeMenu.accent(leaderName) + LABEL + "'ꜱ ᴘᴀʀᴛʏ");
        java.util.ArrayList<String> lore = new java.util.ArrayList<String>();
        lore.add("");
        lore.add(LABEL + "ᴍᴇᴍʙᴇʀꜱ <dark_gray>» " + VALUE + other.size());
        lore.add("");
        int shown = 0;
        for (UUID id : other.getMembers()) {
            if (shown >= 8) {
                lore.add(MUTED + "  +" + (other.size() - shown) + " ᴍᴏʀᴇ");
                break;
            }
            boolean online = Bukkit.getPlayer((UUID)id) != null;
            String mark = id.equals(leaderId) ? "<#FFD65C>★ " : "<dark_gray>• ";
            lore.add("  " + mark + (online ? VALUE : MUTED) + this.nameOf(id));
            ++shown;
        }
        lore.add("");
        boolean picked = leaderId.equals(mine.getDuelTarget());
        lore.add(picked ? "<#7CFF6B>ꜱᴇʟᴇᴄᴛᴇᴅ"
                : HINT + "ᴄʟɪᴄᴋ ᴛᴏ ꜰɪɢʜᴛ ᴛʜᴇᴍ");
        return item.rawLore(lore.toArray(new String[0]))
                .glow(picked).hideTooltip()
                .tag(this.plugin.keyButton(), "opp:" + leaderId).build();
    }

    private String nameOf(UUID id) {
        Player online = Bukkit.getPlayer((UUID)id);
        if (online != null) {
            return online.getName();
        }
        String name = Bukkit.getOfflinePlayer((UUID)id).getName();
        return name == null ? "?" : name;
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String id = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if (id == null) {
            return;
        }
        if ("opp-back".equals(id)) {
            new PartyModeMenu(this.plugin).open(player);
            return;
        }
        if (!id.startsWith("opp:")) {
            return;
        }
        Party party = this.plugin.getPartyManager().partyOf(player.getUniqueId());
        if (party == null || !party.isLeader(player.getUniqueId())) {
            player.closeInventory();
            return;
        }
        UUID leaderId;
        try {
            leaderId = UUID.fromString(id.substring(4));
        }
        catch (IllegalArgumentException e) {
            return;
        }
        // Re-checked rather than trusted: the window was drawn some seconds ago
        // and that party may have started a match of its own since.
        Party target = this.plugin.getPartyManager().partyLedBy(leaderId);
        if (target == null || target == party || target.isFighting()) {
            player.sendMessage(Text.prefixed("&cThat party isn't available any more."));
            this.open(player);
            return;
        }
        party.setDuelTarget(leaderId);
        new PartyKitMenu(this.plugin, PartyMode.DUELS).open(player);
    }
}

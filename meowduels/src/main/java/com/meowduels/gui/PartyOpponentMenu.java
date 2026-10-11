package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.managers.GuiConfigManager.Face;
import com.meowduels.model.Party;
import com.meowduels.model.PartyMode;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /** The name of this screen's file: {@code gui/partyopponent.yml}. */
    private static final String MENU = "partyopponent";

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
            player.sendMessage(this.plugin.messages().get("notice.party.only-party-leader-start-3"));
            return;
        }
        GuiConfigManager gui = this.plugin.gui();
        List<Party> parties = this.plugin.getPartyManager().opponentParties(party);
        // `rows` is the cap, not the height: the window shrinks to the number
        // of parties actually up.
        int rows = Style.gridRows(parties.size(), gui.rows(MENU, MAX_LIST_ROWS));
        this.createRaw(rows, gui.title(MENU,
                "<dark_gray>▏ <gradient:#7DE2FF:#4B7BFF>ᴘᴀʀᴛʏ ᴅᴜᴇʟꜱ</gradient> {sep}{value}ᴘɪᴄᴋ ᴀ ᴘᴀʀᴛʏ"));
        gui.frame(MENU, this.inventory, rows);
        int limit = (rows - 2) * Style.PER_ROW;
        for (int i = 0; i < parties.size() && i < limit; ++i) {
            Party other = parties.get(i);
            this.inventory.setItem(Style.gridSlot(i), this.card(gui, party, other));
        }
        if (parties.isEmpty()) {
            this.inventory.setItem(Style.gridSlot(Style.PER_ROW / 2),
                    gui.face(MENU, "empty", null,
                             Face.of(Material.BARRIER,
                                     "<#FF8A93>ɴᴏ ᴘᴀʀᴛɪᴇꜱ ᴛᴏ ꜰɪɢʜᴛ",
                                     "",
                                     "{label}ɴᴏʙᴏᴅʏ ᴇʟꜱᴇ ʜᴀꜱ ᴀ ᴘᴀʀᴛʏ ʀɪɢʜᴛ ɴᴏᴡ",
                                     "",
                                     "{muted}ᴛʜᴇʏ ɴᴇᴇᴅ ᴛᴏ ʙᴇ ᴏᴜᴛ ᴏꜰ ᴀ ᴍᴀᴛᴄʜ"))
                            .hideTooltip().build());
        }
        this.inventory.setItem(Style.backSlot(rows),
                gui.back(MENU, "back").tag(this.plugin.keyButton(), "opp-back").build());
        player.openInventory(this.inventory);
    }

    /**
     * One party: the leader's head, their name, and who is in it.
     *
     * <p>The roster is a {@code {members}} block rather than lore lines of its
     * own, because how many there are is only known as the card is drawn. An
     * owner can move that block within the card, or drop it, and still have
     * the rest of the card.
     */
    private ItemStack card(GuiConfigManager gui, Party mine, Party other) {
        UUID leaderId = other.getLeader();
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("leader", this.nameOf(leaderId));
        tokens.put("size", String.valueOf(other.size()));
        java.util.ArrayList<String> roster = new java.util.ArrayList<String>();
        int shown = 0;
        for (UUID id : other.getMembers()) {
            if (shown >= 8) {
                Map<String, String> moreTokens = new HashMap<String, String>();
                moreTokens.put("count", String.valueOf(other.size() - shown));
                roster.add(gui.line(MENU, "party", "more-line",
                                    "{muted}  +{count} ᴍᴏʀᴇ", moreTokens));
                break;
            }
            Map<String, String> row = new HashMap<String, String>();
            row.put("mark", id.equals(leaderId) ? "<#FFD65C>★ " : "<dark_gray>• ");
            row.put("colour", Bukkit.getPlayer((UUID)id) != null
                    ? com.meowduels.gui.Style.VALUE : com.meowduels.gui.Style.MUTED);
            row.put("player", this.nameOf(id));
            roster.add(gui.line(MENU, "party", "member-line",
                                "  {mark}{colour}{player}", row));
            ++shown;
        }
        Map<String, java.util.List<String>> blocks = new HashMap<String, java.util.List<String>>();
        blocks.put("members", roster);
        return gui.toggle(MENU, "party", leaderId.equals(mine.getDuelTarget()), tokens, blocks,
                        card("{good}ꜱᴇʟᴇᴄᴛᴇᴅ").glow(),
                        card("{hint}ᴄʟɪᴄᴋ ᴛᴏ ꜰɪɢʜᴛ ᴛʜᴇᴍ"))
                .skull(Bukkit.getOfflinePlayer((UUID)leaderId))
                .hideTooltip()
                .tag(this.plugin.keyButton(), "opp:" + leaderId).build();
    }

    /** The card, which differs between its two states only in the last line. */
    private static Face card(String status) {
        return Face.of(Material.PLAYER_HEAD,
                       "<gradient:#7DE2FF:#4B7BFF>{leader}</gradient>{label}'ꜱ ᴘᴀʀᴛʏ",
                       "",
                       "{label}ᴍᴇᴍʙᴇʀꜱ <dark_gray>» {value}{size}",
                       "",
                       "{members}",
                       "",
                       status);
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
            player.sendMessage(this.plugin.messages().get("notice.party.party-isn-t-available"));
            this.open(player);
            return;
        }
        party.setDuelTarget(leaderId);
        new PartyKitMenu(this.plugin, PartyMode.DUELS).open(player);
    }
}

package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.model.Kit;
import com.meowduels.model.Party;
import com.meowduels.model.PartyMode;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The last screen before Party Duels start: who, what kit, how many rounds.
 *
 * <p>Rounds are the one thing this mode has that Split does not, because every
 * pairing is a real duel and a duel is played to a number. They sit on their own
 * row as three items rather than a click-to-cycle button - cycling hides the
 * other options behind guesswork about how many more clicks are left.
 */
public class PartyDuelsConfirmMenu
extends Menu {
    // Four rows, not five: the fifth was two empty interior rows padding a
    // window that asks one question.
    private static final int ROWS = 4;
    private static final int SLOT_INFO = 4;
    private static final int[] ROUND_SLOTS = new int[]{11, 13, 15};
    private static final int[] ROUND_VALUES = new int[]{1, 3, 5};
    private static final int SLOT_BACK = 27;
    private static final int SLOT_START = 22;

    private static final String VALUE = "<#E6E8EB>";
    private static final String LABEL = "<#8E959D>";
    private static final String MUTED = "<#6B7079>";
    private static final String SEP = "<dark_gray>› ";
    private static final String HINT = "<dark_gray>▸ <#8E959D>";

    private final MeowDuels plugin;

    public PartyDuelsConfirmMenu(MeowDuels plugin) {
        this.plugin = plugin;
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
        Party target = this.plugin.getPartyManager().partyLedBy(party.getDuelTarget());
        if (target == null) {
            player.sendMessage(Text.prefixed("&cThat party is gone - pick another."));
            new PartyOpponentMenu(this.plugin).open(player);
            return;
        }
        this.createRaw(ROWS, Style.title("#7DE2FF", "#4B7BFF",
                "ᴘᴀʀᴛʏ ᴅᴜᴇʟꜱ", this.nameOf(target.getLeader())));
        Style.frame(this.inventory, ROWS);
        int ours = this.onlineCount(party);
        int theirs = this.onlineCount(target);
        this.inventory.setItem(SLOT_INFO, Items.of(Material.PLAYER_HEAD)
                .skull(Bukkit.getOfflinePlayer((UUID)target.getLeader()))
                .rawName(PartyModeMenu.accent(this.nameOf(target.getLeader())) + LABEL + "'ꜱ ᴘᴀʀᴛʏ")
                .rawLore("",
                         LABEL + "ᴋɪᴛ " + SEP + this.kitLabel(party),
                         LABEL + "ꜱɪᴅᴇꜱ " + SEP + VALUE + ours + " " + LABEL + "ᴠꜱ " + VALUE + theirs,
                         LABEL + "ʀᴏᴜɴᴅꜱ " + SEP + VALUE + "ꜰɪʀꜱᴛ ᴛᴏ " + party.getDuelRounds(),
                         "",
                         LABEL + PartyMode.DUELS.getDescription())
                .hideTooltip().build());
        for (int i = 0; i < ROUND_SLOTS.length; ++i) {
            int value = ROUND_VALUES[i];
            boolean on = party.getDuelRounds() == value;
            this.inventory.setItem(ROUND_SLOTS[i], Items.of(Material.PAPER, value)
                    .rawName((on ? "<#7CFF6B>" : VALUE) + "ꜰɪʀꜱᴛ ᴛᴏ " + value)
                    .rawLore("", on ? "<#7CFF6B>ꜱᴇʟᴇᴄᴛᴇᴅ"
                            : HINT + "ᴄʟɪᴄᴋ ᴛᴏ ᴘɪᴄᴋ")
                    .glow(on).hideTooltip()
                    .tag(this.plugin.keyButton(), "pd-rounds:" + value).build());
        }
        this.inventory.setItem(SLOT_BACK, Style.back(this.plugin.keyButton(), "pd-back"));
        boolean ready = ours > 0 && theirs > 0 && party.getKit() != null;
        this.inventory.setItem(SLOT_START, Style.confirm(this.plugin.keyButton(),
                ready ? "pd-go" : "pd-none", ready,
                "ꜱᴛᴀʀᴛ", "ɴᴏᴛ ʀᴇᴀᴅʏ",
                "", ready
                        ? LABEL + "ᴛᴇᴀᴍ ᴠꜱ ᴛᴇᴀᴍ ᴜɴᴛɪʟ ᴏɴᴇ ꜱɪᴅᴇ ɪꜱ ᴏᴜᴛ"
                        : Style.BAD + "ʙᴏᴛʜ ᴘᴀʀᴛɪᴇꜱ ɴᴇᴇᴅ ꜱᴏᴍᴇᴏɴᴇ ᴏɴʟɪɴᴇ"));
        player.openInventory(this.inventory);
    }

    private int onlineCount(Party party) {
        int n = 0;
        for (UUID id : party.getMembers()) {
            if (Bukkit.getPlayer((UUID)id) != null) {
                ++n;
            }
        }
        return n;
    }

    private String kitLabel(Party party) {
        Kit kit = party.getKit() == null ? null : this.plugin.getKitManager().get(party.getKit());
        if (kit == null) {
            return MUTED + "ɴᴏᴛ ᴄʜᴏꜱᴇɴ";
        }
        return kit.getDisplayName() == null || kit.getDisplayName().isEmpty()
                ? VALUE + kit.getName() : kit.getDisplayName();
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
        Party party = this.plugin.getPartyManager().partyOf(player.getUniqueId());
        if (party == null || !party.isLeader(player.getUniqueId())) {
            player.closeInventory();
            return;
        }
        if ("pd-back".equals(id)) {
            new PartyKitMenu(this.plugin, PartyMode.DUELS).open(player);
            return;
        }
        if (id.startsWith("pd-rounds:")) {
            try {
                party.setDuelRounds(Integer.parseInt(id.substring(10)));
            }
            catch (NumberFormatException e) {
                return;
            }
            this.open(player);
            return;
        }
        if ("pd-go".equals(id)) {
            player.closeInventory();
            this.plugin.getPartyManager().challengeDuels(player);
        }
    }
}

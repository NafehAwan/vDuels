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
    /** The name of this screen's file: {@code gui/partyduelsconfirm.yml}. */
    private static final String MENU = "partyduelsconfirm";

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
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        int last = rows * 9 - 1;
        int ours = this.onlineCount(party);
        int theirs = this.onlineCount(target);
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("opponent", this.nameOf(target.getLeader()));
        tokens.put("kit", this.kitLabel(party));
        tokens.put("ours", String.valueOf(ours));
        tokens.put("theirs", String.valueOf(theirs));
        tokens.put("rounds", String.valueOf(party.getDuelRounds()));
        tokens.put("description", PartyMode.DUELS.getDescription());
        this.createRaw(rows, gui.title(MENU,
                "<dark_gray>▏ <gradient:#7DE2FF:#4B7BFF>ᴘᴀʀᴛʏ ᴅᴜᴇʟꜱ</gradient> {sep}{value}{opponent}", tokens));
        gui.frame(MENU, this.inventory, rows);
        this.inventory.setItem(Math.min(last, gui.slot(MENU, "info", SLOT_INFO, rows)),
                gui.face(MENU, "info", tokens,
                         Face.of(Material.PLAYER_HEAD,
                                 "<gradient:#7DE2FF:#4B7BFF>{opponent}</gradient>{label}'ꜱ ᴘᴀʀᴛʏ",
                                 "",
                                 "{label}ᴋɪᴛ {sep}{kit}",
                                 "{label}ꜱɪᴅᴇꜱ {sep}{value}{ours} {label}ᴠꜱ {value}{theirs}",
                                 "{label}ʀᴏᴜɴᴅꜱ {sep}{value}ꜰɪʀꜱᴛ ᴛᴏ {rounds}",
                                 "",
                                 "{label}{description}"))
                        .skull(Bukkit.getOfflinePlayer((UUID)target.getLeader()))
                        .hideTooltip().build());
        // The four round options. Each wears its own count as its stack size,
        // so "first to 3" is a stack of three.
        for (int i = 0; i < ROUND_SLOTS.length; ++i) {
            int value = ROUND_VALUES[i];
            Map<String, String> round = new HashMap<String, String>();
            round.put("rounds", String.valueOf(value));
            this.inventory.setItem(ROUND_SLOTS[i],
                    gui.toggle(MENU, "rounds", party.getDuelRounds() == value, round,
                            Face.of(Material.PAPER,
                                    "{good}ꜰɪʀꜱᴛ ᴛᴏ {rounds}",
                                    "",
                                    "{good}ꜱᴇʟᴇᴄᴛᴇᴅ").amount(value).glow(),
                            Face.of(Material.PAPER,
                                    "{value}ꜰɪʀꜱᴛ ᴛᴏ {rounds}",
                                    "",
                                    "{hint}ᴄʟɪᴄᴋ ᴛᴏ ᴘɪᴄᴋ").amount(value))
                            .hideTooltip()
                            .tag(this.plugin.keyButton(), "pd-rounds:" + value).build());
        }
        this.inventory.setItem(Math.min(last, gui.slot(MENU, "back", SLOT_BACK, rows)),
                gui.back(MENU, "back").tag(this.plugin.keyButton(), "pd-back").build());
        // The tag differs by state on purpose: only the ready button carries
        // the one onClick acts on, so a click on "not ready" does nothing.
        boolean ready = ours > 0 && theirs > 0 && party.getKit() != null;
        this.inventory.setItem(Math.min(last, gui.slot(MENU, "start", SLOT_START, rows)),
                gui.toggle(MENU, "start", ready, tokens,
                        Face.of(Material.LIME_DYE,
                                "<gradient:{go-from}:{go-to}>{tick}ꜱᴛᴀʀᴛ</gradient>",
                                "",
                                "{label}ᴛᴇᴀᴍ ᴠꜱ ᴛᴇᴀᴍ ᴜɴᴛɪʟ ᴏɴᴇ ꜱɪᴅᴇ ɪꜱ ᴏᴜᴛ").glow(),
                        Face.of(Material.GRAY_DYE,
                                "{muted}{cross}ɴᴏᴛ ʀᴇᴀᴅʏ",
                                "",
                                "{bad}ʙᴏᴛʜ ᴘᴀʀᴛɪᴇꜱ ɴᴇᴇᴅ ꜱᴏᴍᴇᴏɴᴇ ᴏɴʟɪɴᴇ"))
                        .hideTooltip()
                        .tag(this.plugin.keyButton(), ready ? "pd-go" : "pd-none").build());
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

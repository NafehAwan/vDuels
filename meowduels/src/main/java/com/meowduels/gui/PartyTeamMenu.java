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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Who is on which side, for a Split match.
 *
 * <p>One window, two halves, a divider down the middle. Aqua on the left, red on
 * the right, a head each, and clicking a head sends it across. Left click or
 * right click do the same thing on purpose: two ways to do one thing is two ways
 * to be surprised.
 *
 * <p>Teams are already shuffled when this opens, so the common case - "just give
 * us two fair sides" - needs no clicks at all. Shuffle re-rolls, start goes.
 *
 * <p>This screen replaces the hopper confirm that FFA uses. It already shows the
 * mode, the kit and every player in the match, which is everything the confirm
 * would have said, so asking twice would be asking for the sake of it.
 */
public class PartyTeamMenu
extends Menu {
    /** The name of this screen's file: {@code gui/partyteam.yml}. */
    private static final String MENU = "partyteam";

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;
    /** Column 4 is the divider; 0-3 are aqua, 5-8 are red. */
    private static final int DIVIDER_COL = 4;
    private static final int[] AQUA_COLS = new int[]{0, 1, 2, 3};
    private static final int[] RED_COLS = new int[]{5, 6, 7, 8};
    private static final int HEAD_ROWS = 4;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_SHUFFLE = 47;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_START = 51;

    private static final String VALUE = "<#E6E8EB>";
    private static final String LABEL = "<#8E959D>";
    private static final String MUTED = "<#6B7079>";
    private static final String SEP = "<dark_gray>\u203a ";
    private static final String HINT = "<dark_gray>\u25b8 <#8E959D>";
    private static final String AQUA = "<gradient:#7DE2FF:#2BB9E8>";
    private static final String RED = "<gradient:#FF8A8A:#D63232>";

    private final MeowDuels plugin;

    public PartyTeamMenu(MeowDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public void build() {
    }

    @Override
    public void open(Player player) {
        Party party = this.plugin.getPartyManager().partyOf(player.getUniqueId());
        if (party == null || !party.isLeader(player.getUniqueId())) {
            player.sendMessage(this.plugin.messages().get("notice.party.only-party-leader-set"));
            return;
        }
        // First open of a match: give them two fair sides rather than an empty
        // board to fill in by hand.
        if (party.getTeams().isEmpty()) {
            party.shuffleTeams();
        }
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        int last = rows * 9 - 1;
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("kit", this.kitLabel(party));
        tokens.put("size", String.valueOf(party.size()));
        tokens.put("mode", PartyMode.SPLIT.getLabel());
        this.createRaw(rows, gui.title(MENU,
                "<dark_gray>\u258f <gradient:#7DE2FF:#4B7BFF>\u1d18\u1d00\u0280\u1d1b\u028f \ua731\u1d18\u029f\u026a\u1d1b - \u1d1b\u1d07\u1d00\u1d0d\ua731</gradient>"));
        gui.frame(MENU, this.inventory, rows);
        // Iron bars, not a pane: the backdrop is panes now, so a pane divider
        // would be a line drawn in the same colour as the thing it divides.
        ItemStack divider = gui.face(MENU, "divider", null,
                                    Face.of(Material.IRON_BARS, " ")).build();
        // Stops above the bottom bar: the bar is shared by both sides, and a
        // divider running through it would be a line the buttons then sit on
        // top of.
        for (int row = 0; row < rows - 1; ++row) {
            this.inventory.setItem(row * 9 + DIVIDER_COL, divider);
        }
        Map<String, String> aquaTokens = new HashMap<String, String>();
        aquaTokens.put("size", String.valueOf(party.teamMembers(Party.Team.AQUA).size()));
        Map<String, String> redTokens = new HashMap<String, String>();
        redTokens.put("size", String.valueOf(party.teamMembers(Party.Team.RED).size()));
        this.inventory.setItem(DIVIDER_COL - 2,
                gui.face(MENU, "banner-aqua", aquaTokens,
                         Face.of(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                                 "<gradient:#7DE2FF:#2BB9E8>\u1d1b\u1d07\u1d00\u1d0d \u1d00\ua7af\u1d1c\u1d00</gradient>",
                                 "",
                                 "{label}\u1d0f\u0274 \u1d1b\u029c\u026a\ua731 \ua731\u026a\u1d05\u1d07 {sep}{value}{size}"))
                        .hideTooltip().build());
        this.inventory.setItem(DIVIDER_COL + 2,
                gui.face(MENU, "banner-red", redTokens,
                         Face.of(Material.RED_STAINED_GLASS_PANE,
                                 "<gradient:#FF8A8A:#D63232>\u1d1b\u1d07\u1d00\u1d0d \u0280\u1d07\u1d05</gradient>",
                                 "",
                                 "{label}\u1d0f\u0274 \u1d1b\u029c\u026a\ua731 \ua731\u026a\u1d05\u1d07 {sep}{value}{size}"))
                        .hideTooltip().build());
        this.fill(party, Party.Team.AQUA, AQUA_COLS);
        this.fill(party, Party.Team.RED, RED_COLS);

        boolean ready = party.aliveOrMembers(Party.Team.AQUA) > 0
                && party.aliveOrMembers(Party.Team.RED) > 0;
        this.inventory.setItem(Math.min(last, gui.slot(MENU, "back", SLOT_BACK, rows)),
                gui.back(MENU, "back").tag(this.plugin.keyButton(), "team-back").build());
        this.inventory.setItem(Math.min(last, gui.slot(MENU, "shuffle", SLOT_SHUFFLE, rows)),
                gui.face(MENU, "shuffle", tokens,
                         Face.of(Material.ENDER_PEARL,
                                 "<gradient:#7DE2FF:#4B7BFF>\ua731\u029c\u1d1c\ua730\ua730\u029f\u1d07</gradient>",
                                 "",
                                 "{label}\ua731\u1d18\u029f\u026a\u1d1b \u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f \u1d00\u1d1b \u0280\u1d00\u0274\u1d05\u1d0f\u1d0d \u1d00\u0262\u1d00\u026a\u0274"))
                        .hideTooltip().tag(this.plugin.keyButton(), "team-shuffle").build());
        this.inventory.setItem(Math.min(last, gui.slot(MENU, "info", SLOT_INFO, rows)),
                gui.face(MENU, "info", tokens,
                         Face.of(Material.PAPER,
                                 "<gradient:#7DE2FF:#4B7BFF>{mode}</gradient>",
                                 "",
                                 "{label}\u1d0b\u026a\u1d1b {sep}{kit}",
                                 "{label}\u1d18\u029f\u1d00\u028f\u1d07\u0280\ua731 {sep}{value}{size}",
                                 "",
                                 "{hint}\u1d04\u029f\u026a\u1d04\u1d0b \u1d00 \u029c\u1d07\u1d00\u1d05 \u1d1b\u1d0f \u1d0d\u1d0f\u1d20\u1d07 \u026a\u1d1b \u1d00\u1d04\u0280\u1d0f\ua731\ua731"))
                        .hideTooltip().build());
        this.inventory.setItem(Math.min(last, gui.slot(MENU, "start", SLOT_START, rows)),
                gui.toggle(MENU, "start", ready, tokens,
                        Face.of(Material.LIME_DYE,
                                "<gradient:{go-from}:{go-to}>\ua731\u1d1b\u1d00\u0280\u1d1b \u1d0d\u1d00\u1d1b\u1d04\u029c</gradient>",
                                "",
                                "{label}\ua731\u1d07\u0274\u1d05 \u0299\u1d0f\u1d1b\u029c \u1d1b\u1d07\u1d00\u1d0d\ua731 \u026a\u0274").glow(),
                        Face.of(Material.GRAY_DYE,
                                "{muted}\ua731\u1d1b\u1d00\u0280\u1d1b \u1d0d\u1d00\u1d1b\u1d04\u029c",
                                "",
                                "<#FF8A93>\u0299\u1d0f\u1d1b\u029c \u1d1b\u1d07\u1d00\u1d0d\ua731 \u0274\u1d07\u1d07\u1d05 \u1d00 \u1d18\u029f\u1d00\u028f\u1d07\u0280"))
                        .hideTooltip().tag(this.plugin.keyButton(), "team-start").build());
        player.openInventory(this.inventory);
    }

    /** Heads down the four columns of one side, filling left to right. */
    private void fill(Party party, Party.Team team, int[] columns) {
        List<UUID> members = party.teamMembers(team);
        for (int i = 0; i < members.size() && i < HEAD_ROWS * columns.length; ++i) {
            int slot = (1 + i / columns.length) * 9 + columns[i % columns.length];
            this.inventory.setItem(slot, this.head(party, members.get(i), team));
        }
    }

    /**
     * One player's head, on whichever side they are on.
     *
     * <p>Two items rather than two faces of one, because the colour runs
     * through the name, the team line and nothing else: a server recolouring
     * one side should not have to pick the right half of a pair of faces.
     */
    private ItemStack head(Party party, UUID id, Party.Team team) {
        GuiConfigManager gui = this.plugin.gui();
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("player", this.nameOf(id));
        tokens.put("state", Bukkit.getPlayer((UUID)id) != null ? ""
                : gui.line(MENU, "head-aqua", "state-offline",
                           "{muted}\u1d0f\ua730\ua730\u029f\u026a\u0274\u1d07", null));
        Items item = team == Party.Team.AQUA
                ? gui.face(MENU, "head-aqua", tokens,
                           Face.of(Material.PLAYER_HEAD,
                                   "<gradient:#7DE2FF:#2BB9E8>{player}</gradient>",
                                   "",
                                   "{label}\u1d1b\u1d07\u1d00\u1d0d {sep}<gradient:#7DE2FF:#2BB9E8>\u1d1b\u1d07\u1d00\u1d0d \u1d00\ua7af\u1d1c\u1d00</gradient>",
                                   "{state}",
                                   "",
                                   "{hint}\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d0d\u1d0f\u1d20\u1d07 \u1d1b\u1d0f \u1d1b\u029c\u1d07 \u1d0f\u1d1b\u029c\u1d07\u0280 \u1d1b\u1d07\u1d00\u1d0d"))
                : gui.face(MENU, "head-red", tokens,
                           Face.of(Material.PLAYER_HEAD,
                                   "<gradient:#FF8A8A:#D63232>{player}</gradient>",
                                   "",
                                   "{label}\u1d1b\u1d07\u1d00\u1d0d {sep}<gradient:#FF8A8A:#D63232>\u1d1b\u1d07\u1d00\u1d0d \u0280\u1d07\u1d05</gradient>",
                                   "{state}",
                                   "",
                                   "{hint}\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d0d\u1d0f\u1d20\u1d07 \u1d1b\u1d0f \u1d1b\u029c\u1d07 \u1d0f\u1d1b\u029c\u1d07\u0280 \u1d1b\u1d07\u1d00\u1d0d"));
        return item.skull(Bukkit.getOfflinePlayer((UUID)id))
                .hideTooltip()
                .tag(this.plugin.keyButton(), "team-move:" + id.toString()).build();
    }

    private String kitLabel(Party party) {
        Kit kit = party.getKit() == null ? null : this.plugin.getKitManager().get(party.getKit());
        if (kit == null) {
            return MUTED + "\u0274\u1d0f\u1d1b \u1d04\u029c\u1d0f\ua731\u1d07\u0274";
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
        if ("team-back".equals(id)) {
            new PartyKitMenu(this.plugin, PartyMode.SPLIT).open(player);
            return;
        }
        if ("team-shuffle".equals(id)) {
            party.shuffleTeams();
            this.open(player);
            return;
        }
        if ("team-start".equals(id)) {
            player.closeInventory();
            this.plugin.getPartyManager().startMatch(player, PartyMode.SPLIT);
            return;
        }
        if (id.startsWith("team-move:")) {
            UUID target;
            try {
                target = UUID.fromString(id.substring(10));
            }
            catch (IllegalArgumentException e) {
                return;
            }
            Party.Team current = party.teamOf(target);
            if (current != null) {
                party.setTeam(target, current == Party.Team.AQUA ? Party.Team.RED : Party.Team.AQUA);
            }
            this.open(player);
        }
    }
}

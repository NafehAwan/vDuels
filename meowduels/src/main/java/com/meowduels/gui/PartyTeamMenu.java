package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.model.Kit;
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
            player.sendMessage(Text.prefixed("&cOnly the party leader can set the teams."));
            return;
        }
        // First open of a match: give them two fair sides rather than an empty
        // board to fill in by hand.
        if (party.getTeams().isEmpty()) {
            party.shuffleTeams();
        }
        this.createRaw(ROWS, "<dark_gray>\u258f " + PartyModeMenu.accent("\u1d18\u1d00\u0280\u1d1b\u028f \ua731\u1d18\u029f\u026a\u1d1b - \u1d1b\u1d07\u1d00\u1d0d\ua731"));
        Style.frame(this.inventory, ROWS);
        // Iron bars, not a pane: the backdrop is panes now, so a pane divider
        // would be a line drawn in the same colour as the thing it divides.
        ItemStack divider = Items.of(Material.IRON_BARS).rawName(" ").build();
        // Stops above the bottom bar: the bar is shared by both sides, and a
        // divider running through it would be a line the buttons then sit on
        // top of.
        for (int row = 0; row < ROWS - 1; ++row) {
            this.inventory.setItem(row * 9 + DIVIDER_COL, divider);
        }
        this.inventory.setItem(DIVIDER_COL - 2, this.banner(party, Party.Team.AQUA));
        this.inventory.setItem(DIVIDER_COL + 2, this.banner(party, Party.Team.RED));
        this.fill(party, Party.Team.AQUA, AQUA_COLS);
        this.fill(party, Party.Team.RED, RED_COLS);

        boolean ready = party.aliveOrMembers(Party.Team.AQUA) > 0
                && party.aliveOrMembers(Party.Team.RED) > 0;
        this.inventory.setItem(SLOT_BACK, Style.back(this.plugin.keyButton(), "team-back"));
        this.inventory.setItem(SLOT_SHUFFLE, Items.of(Material.ENDER_PEARL)
                .rawName(PartyModeMenu.accent("\ua731\u029c\u1d1c\ua730\ua730\u029f\u1d07"))
                .rawLore("", LABEL + "\ua731\u1d18\u029f\u026a\u1d1b \u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f \u1d00\u1d1b \u0280\u1d00\u0274\u1d05\u1d0f\u1d0d \u1d00\u0262\u1d00\u026a\u0274")
                .hideTooltip().tag(this.plugin.keyButton(), "team-shuffle").build());
        this.inventory.setItem(SLOT_INFO, Items.of(Material.PAPER)
                .rawName(PartyModeMenu.accent(PartyMode.SPLIT.getLabel()))
                .rawLore("",
                         LABEL + "\u1d0b\u026a\u1d1b " + SEP + this.kitLabel(party),
                         LABEL + "\u1d18\u029f\u1d00\u028f\u1d07\u0280\ua731 " + SEP + VALUE + party.size(),
                         "",
                         HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d00 \u029c\u1d07\u1d00\u1d05 \u1d1b\u1d0f \u1d0d\u1d0f\u1d20\u1d07 \u026a\u1d1b \u1d00\u1d04\u0280\u1d0f\ua731\ua731")
                .hideTooltip().build());
        this.inventory.setItem(SLOT_START, Items.of(ready ? Material.LIME_DYE : Material.GRAY_DYE)
                .rawName(ready ? "<gradient:#7CFF6B:#1FA32F>\ua731\u1d1b\u1d00\u0280\u1d1b \u1d0d\u1d00\u1d1b\u1d04\u029c</gradient>" : MUTED + "\ua731\u1d1b\u1d00\u0280\u1d1b \u1d0d\u1d00\u1d1b\u1d04\u029c")
                .rawLore("", ready ? LABEL + "\ua731\u1d07\u0274\u1d05 \u0299\u1d0f\u1d1b\u029c \u1d1b\u1d07\u1d00\u1d0d\ua731 \u026a\u0274" : "<#FF8A93>\u0299\u1d0f\u1d1b\u029c \u1d1b\u1d07\u1d00\u1d0d\ua731 \u0274\u1d07\u1d07\u1d05 \u1d00 \u1d18\u029f\u1d00\u028f\u1d07\u0280")
                .glow(ready).hideTooltip()
                .tag(this.plugin.keyButton(), "team-start").build());
        player.openInventory(this.inventory);
    }

    private ItemStack banner(Party party, Party.Team team) {
        boolean aqua = team == Party.Team.AQUA;
        return Items.of(aqua ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                .rawName((aqua ? AQUA : RED) + (aqua ? "\u1d1b\u1d07\u1d00\u1d0d \u1d00\ua7af\u1d1c\u1d00" : "\u1d1b\u1d07\u1d00\u1d0d \u0280\u1d07\u1d05") + "</gradient>")
                .rawLore("", LABEL + "\u1d0f\u0274 \u1d1b\u029c\u026a\ua731 \ua731\u026a\u1d05\u1d07 " + SEP + VALUE + party.teamMembers(team).size())
                .hideTooltip().build();
    }

    /** Heads down the four columns of one side, filling left to right. */
    private void fill(Party party, Party.Team team, int[] columns) {
        List<UUID> members = party.teamMembers(team);
        for (int i = 0; i < members.size() && i < HEAD_ROWS * columns.length; ++i) {
            int slot = (1 + i / columns.length) * 9 + columns[i % columns.length];
            this.inventory.setItem(slot, this.head(party, members.get(i), team));
        }
    }

    private ItemStack head(Party party, UUID id, Party.Team team) {
        boolean aqua = team == Party.Team.AQUA;
        boolean online = Bukkit.getPlayer((UUID)id) != null;
        return Items.of(Material.PLAYER_HEAD)
                .skull(Bukkit.getOfflinePlayer((UUID)id))
                .rawName((aqua ? AQUA : RED) + this.nameOf(id) + "</gradient>")
                .rawLore("",
                         LABEL + "\u1d1b\u1d07\u1d00\u1d0d " + SEP + (aqua ? AQUA + "\u1d1b\u1d07\u1d00\u1d0d \u1d00\ua7af\u1d1c\u1d00</gradient>" : RED + "\u1d1b\u1d07\u1d00\u1d0d \u0280\u1d07\u1d05</gradient>"),
                         online ? "" : MUTED + "\u1d0f\ua730\ua730\u029f\u026a\u0274\u1d07",
                         "", HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d0d\u1d0f\u1d20\u1d07 \u1d1b\u1d0f \u1d1b\u029c\u1d07 \u1d0f\u1d1b\u029c\u1d07\u0280 \u1d1b\u1d07\u1d00\u1d0d")
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

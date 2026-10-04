package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Style;
import com.meowduels.gui.Menu;
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.managers.GuiConfigManager.Face;
import com.meowduels.model.Kit;
import com.meowduels.model.Party;
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
 * The party screen.
 *
 * <p>Four rows, read top to bottom: what the party IS, what the leader can
 * change, who is in it, and what you can do about it. The card at the top is the
 * only place the party's state is written down, so the buttons underneath can
 * each say one thing.
 *
 * <p>The heads are centred on their row. Left-aligning them, which is what this
 * did before, makes a party of two look like a party of nine with seven people
 * missing.
 *
 * <p>Two versions. The leader gets kit, invite, settings and start; a member
 * gets the card, the heads and a leave button in the middle of an otherwise
 * empty action row - not greyed-out copies of the leader's buttons, because a
 * disabled button still reads as something you could have if you clicked harder.
 */
public class PartyMenu
extends Menu {
    private static final int ROWS = 4;
    private static final int SIZE = ROWS * 9;
    private static final int SLOT_CARD = 4;
    private static final int SLOT_KIT = 11;
    private static final int SLOT_INVITE = 13;
    private static final int SLOT_SETTINGS = 15;
    // The interior of the members row, not the whole row: the frame owns
    // columns 0 and 8, and a head sitting in the border makes the window look
    // like the border failed rather than like there are more members.
    private static final int MEMBER_ROW = 19;
    private static final int MEMBER_SLOTS = 7;
    private static final int SLOT_START = 29;
    private static final int SLOT_LEAVE_LEADER = 33;
    private static final int SLOT_LEAVE_MEMBER = 31;

    private static final String VALUE = "<#E6E8EB>";
    private static final String LABEL = "<#8E959D>";
    private static final String MUTED = "<#6B7079>";
    private static final String SEP = "<dark_gray>\u203a ";
    private static final String HINT = "<dark_gray>\u25b8 <#8E959D>";

    private final MeowDuels plugin;

    public PartyMenu(MeowDuels plugin) {
        this.plugin = plugin;
    }

    private static String accent(String text) {
        return "<gradient:#FF8AD0:#B04BD6>" + text + "</gradient>";
    }

    /** The name of this screen's file: {@code gui/party.yml}. */
    private static final String MENU = "party";

    @Override
    public void build() {
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        this.createRaw(rows, gui.title(MENU,
                "<dark_gray>\u258f <gradient:#FF8AD0:#B04BD6>\u1d18\u1d00\u0280\u1d1b\u028f</gradient>"));
        gui.frame(MENU, this.inventory, rows);
    }

    /** Built per viewer: what is in the window depends on who opened it. */
    public void openFor(Player player) {
        Party party = this.plugin.getPartyManager().partyOf(player.getUniqueId());
        if (party == null) {
            player.sendMessage(Text.prefixed("&cYou're not in a party."));
            return;
        }
        this.build();
        boolean leader = party.isLeader(player.getUniqueId());
        this.inventory.setItem(SLOT_CARD, this.card(party));
        this.addMembers(party);
        if (leader) {
            this.addLeaderButtons(party);
        }
        GuiConfigManager gui = this.plugin.gui();
        int rows = this.inventory.getSize() / 9;
        int leaveSlot = leader
                ? gui.slot(MENU, "leave-leader", SLOT_LEAVE_LEADER, rows)
                : gui.slot(MENU, "leave-member", SLOT_LEAVE_MEMBER, rows);
        this.inventory.setItem(Math.min(this.inventory.getSize() - 1, leaveSlot),
                (leader
                        ? gui.face(MENU, "leave-leader", null,
                                   Face.of(Material.RED_DYE,
                                           "<gradient:{no-from}:{no-to}>{cross}\u1d05\u026a\ua731\u0299\u1d00\u0274\u1d05 \u1d18\u1d00\u0280\u1d1b\u028f</gradient>",
                                           "",
                                           "{label}\u1d07\u0274\u1d05\ua731 \u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f \ua730\u1d0f\u0280 \u1d07\u1d20\u1d07\u0280\u028f\u1d0f\u0274\u1d07",
                                           "",
                                           "{hint}\u028f\u1d0f\u1d1c'\u029f\u029f \u0299\u1d07 \u1d00\ua731\u1d0b\u1d07\u1d05 \u1d1b\u1d0f \u1d04\u1d0f\u0274\ua730\u026a\u0280\u1d0d"))
                        : gui.face(MENU, "leave-member", null,
                                   Face.of(Material.RED_DYE,
                                           "<gradient:{no-from}:{no-to}>{cross}\u029f\u1d07\u1d00\u1d20\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f</gradient>",
                                           "",
                                           "{label}\u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f \u1d04\u1d00\u0280\u0280\u026a\u1d07\ua731 \u1d0f\u0274 \u1d21\u026a\u1d1b\u029c\u1d0f\u1d1c\u1d1b \u028f\u1d0f\u1d1c",
                                           "",
                                           "{hint}\u028f\u1d0f\u1d1c'\u029f\u029f \u0299\u1d07 \u1d00\ua731\u1d0b\u1d07\u1d05 \u1d1b\u1d0f \u1d04\u1d0f\u0274\ua730\u026a\u0280\u1d0d")))
                        .hideTooltip().tag(this.plugin.keyButton(), "party-leave-open").build());
        player.openInventory(this.inventory);
    }

    /**
     * The party at a glance, at the top of the window.
     *
     * <p>Everything the party currently is, in one item, so no button below has
     * to repeat it. Which is also why the kit button only has to say "kit".
     */
    private ItemStack card(Party party) {
        GuiConfigManager gui = this.plugin.gui();
        boolean fighting = party.isFighting();
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("leader", this.nameOf(party.getLeader()));
        tokens.put("size", String.valueOf(party.size()));
        tokens.put("kit", this.kitLabel(party));
        tokens.put("alive", String.valueOf(party.getAlive().size()));
        // The mode and status lines each read one way mid-match and another
        // between matches, so each gets a pair of named lines rather than
        // being pasted together here.
        tokens.put("mode", fighting
                ? gui.line(MENU, "card", "mode-fighting", "{value}{mode-label}",
                           java.util.Collections.singletonMap("mode-label", party.getMode().getLabel()))
                : gui.line(MENU, "card", "mode-idle",
                           "{muted}\u0274\u1d0f\u1d1b \u1d18\u026a\u1d04\u1d0b\u1d07\u1d05 \u028f\u1d07\u1d1b", null));
        tokens.put("status", fighting
                ? gui.line(MENU, "card", "status-fighting",
                           "{good}\ua730\u026a\u0262\u029c\u1d1b\u026a\u0274\u0262 <dark_gray>\u00b7 {value}{alive} \ua731\u1d1b\u026a\u029f\u029f \u026a\u0274",
                           java.util.Collections.singletonMap("alive", String.valueOf(party.getAlive().size())))
                : gui.line(MENU, "card", "status-idle",
                           "{muted}\u1d21\u1d00\u026a\u1d1b\u026a\u0274\u0262 \ua730\u1d0f\u0280 \u1d1b\u029c\u1d07 \u029f\u1d07\u1d00\u1d05\u1d07\u0280", null));
        return gui.face(MENU, "card", tokens,
                        Face.of(Material.PAPER,
                                "<gradient:#FF8AD0:#B04BD6>\u1d18\u1d00\u0280\u1d1b\u028f</gradient>",
                                "",
                                "{label}\u029f\u1d07\u1d00\u1d05\u1d07\u0280 {sep}{value}{leader}",
                                "{label}\u1d0d\u1d07\u1d0d\u0299\u1d07\u0280\ua731 {sep}{value}{size}",
                                "{label}\u1d0b\u026a\u1d1b {sep}{kit}",
                                "{label}\u1d0d\u1d0f\u1d05\u1d07 {sep}{mode}",
                                "",
                                "{label}\ua731\u1d1b\u1d00\u1d1b\u1d1c\ua731 {sep}{status}"))
                .hideTooltip().build();
    }

    /** Kit, invite, settings, start - leader only. */
    private void addLeaderButtons(Party party) {
        GuiConfigManager gui = this.plugin.gui();
        int rows = this.inventory.getSize() / 9;
        int last = this.inventory.getSize() - 1;
        Kit kit = this.kitOf(party);
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("kit", this.kitLabel(party));
        tokens.put("size", String.valueOf(party.size()));
        // The icon follows the chosen kit; material-on in the file overrides it.
        Material kitIcon = gui.material(MENU, "kit", kit != null ? kit.getIcon() : Material.GOLDEN_APPLE);
        this.inventory.setItem(Math.min(last, gui.slot(MENU, "kit", SLOT_KIT, rows)),
                gui.toggle(MENU, "kit", kit != null, tokens,
                        Face.of(kitIcon, "{value}\u1d0b\u026a\u1d1b",
                                "",
                                "{label}\ua731\u1d07\u029f\u1d07\u1d04\u1d1b\u1d07\u1d05 {sep}{kit}",
                                "",
                                "{hint}\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d18\u026a\u1d04\u1d0b \u1d00 \u1d0b\u026a\u1d1b").glow(),
                        Face.of(kitIcon, "{value}\u1d0b\u026a\u1d1b",
                                "",
                                "{label}\ua731\u1d07\u029f\u1d07\u1d04\u1d1b\u1d07\u1d05 {sep}{kit}",
                                "",
                                "{hint}\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d18\u026a\u1d04\u1d0b \u1d00 \u1d0b\u026a\u1d1b"))
                        .hideTooltip().tag(this.plugin.keyButton(), "party-kit").build());
        this.inventory.setItem(Math.min(last, gui.slot(MENU, "invite", SLOT_INVITE, rows)),
                gui.face(MENU, "invite", tokens,
                         Face.of(Material.PLAYER_HEAD, "{value}\u026a\u0274\u1d20\u026a\u1d1b\u1d07",
                                 "",
                                 "{label}\u1d00\u1d05\u1d05 \ua731\u1d0f\u1d0d\u1d07\u1d0f\u0274\u1d07 \u1d1b\u1d0f \u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f",
                                 "",
                                 "{hint}\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d18\u026a\u1d04\u1d0b \u1d21\u029c\u1d0f"))
                        .hideTooltip().tag(this.plugin.keyButton(), "party-invite").build());
        this.inventory.setItem(Math.min(last, gui.slot(MENU, "settings", SLOT_SETTINGS, rows)),
                gui.face(MENU, "settings", tokens,
                         Face.of(Material.GRINDSTONE, "{value}\ua731\u1d07\u1d1b\u1d1b\u026a\u0274\u0262\ua731",
                                 "",
                                 "{hint}\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d0f\u1d18\u1d07\u0274"))
                        .hideTooltip().tag(this.plugin.keyButton(), "party-settings-open").build());
        // No kit requirement here any more: the kit is chosen inside the match
        // flow now, so demanding one before the button lights up would block the
        // only route to choosing it.
        tokens.put("why", this.startHint(party));
        this.inventory.setItem(Math.min(last, gui.slot(MENU, "start", SLOT_START, rows)),
                gui.toggle(MENU, "start", !party.isFighting() && party.size() >= 2, tokens,
                        Face.of(Material.LIME_DYE,
                                "<gradient:{go-from}:{go-to}>{tick}\ua731\u1d1b\u1d00\u0280\u1d1b \u1d0d\u1d00\u1d1b\u1d04\u029c</gradient>",
                                "", "{why}").glow(),
                        Face.of(Material.GRAY_DYE,
                                "{muted}{cross}\ua731\u1d1b\u1d00\u0280\u1d1b \u1d0d\u1d00\u1d1b\u1d04\u029c",
                                "", "{why}"))
                        .hideTooltip().tag(this.plugin.keyButton(), "party-start").build());
    }

    /**
     * The member heads, centred on their row.
     *
     * <p>Past nine the row is full and the rest are a count on the card - a
     * second row of heads would push the action row off the window.
     */
    private void addMembers(Party party) {
        int shown = Math.min(MEMBER_SLOTS, party.size());
        int slot = MEMBER_ROW + (MEMBER_SLOTS - shown) / 2;
        int drawn = 0;
        for (UUID id : party.getMembers()) {
            if (drawn == shown) {
                break;
            }
            if (drawn == MEMBER_SLOTS - 1 && party.size() > MEMBER_SLOTS) {
                Map<String, String> more = new HashMap<String, String>();
                more.put("count", String.valueOf(party.size() - MEMBER_SLOTS + 1));
                this.inventory.setItem(slot,
                        this.plugin.gui().face(MENU, "overflow", more,
                                Face.of(Material.PAPER, "{value}+{count}",
                                        "",
                                        "{label}\u1d0d\u1d0f\u0280\u1d07 \u1d0d\u1d07\u1d0d\u0299\u1d07\u0280\ua731 \u026a\u0274 \u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f"))
                                .hideTooltip().build());
                break;
            }
            this.inventory.setItem(slot++, this.head(party, id));
            ++drawn;
        }
    }

    /**
     * One member's head.
     *
     * <p>The leader and an ordinary member are the two faces of one item; the
     * line under the role is a fourth named line, because whether someone is
     * alive, out, offline or simply there is four states and a two-faced item
     * cannot say four things.
     */
    private ItemStack head(Party party, UUID id) {
        GuiConfigManager gui = this.plugin.gui();
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("player", this.nameOf(id));
        tokens.put("state", this.memberState(party, id));
        return gui.toggle(MENU, "member", party.isLeader(id), tokens,
                        Face.of(Material.PLAYER_HEAD,
                                "<#FFD65C>\u2605 {value}{player}",
                                "",
                                "<#FFD65C>\u029f\u1d07\u1d00\u1d05\u1d07\u0280",
                                "{state}").glow(),
                        Face.of(Material.PLAYER_HEAD,
                                "<#C79BFF>\u25c6 {value}{player}",
                                "",
                                "{label}\u1d0d\u1d07\u1d0d\u0299\u1d07\u0280",
                                "{state}"))
                .skull(Bukkit.getOfflinePlayer((UUID)id))
                .hideTooltip().build();
    }

    private String memberState(Party party, UUID id) {
        GuiConfigManager gui = this.plugin.gui();
        if (party.isFighting()) {
            return party.getWatching().contains(id)
                    ? gui.line(MENU, "member", "state-out",
                               "<dark_gray>\u2620 {muted}\u1d07\u029f\u026a\u1d0d\u026a\u0274\u1d00\u1d1b\u1d07\u1d05", null)
                    : gui.line(MENU, "member", "state-alive",
                               "{good}\u26a1 \u1d00\u029f\u026a\u1d20\u1d07", null);
        }
        if (Bukkit.getPlayer((UUID)id) == null) {
            return gui.line(MENU, "member", "state-offline",
                            "{muted}\u1d0f\ua730\ua730\u029f\u026a\u0274\u1d07", null);
        }
        // Nothing to say: they are here and nothing is happening.
        return gui.line(MENU, "member", "state-idle", "", null);
    }

    private Kit kitOf(Party party) {
        return party.getKit() == null ? null : this.plugin.getKitManager().get(party.getKit());
    }

    private String kitLabel(Party party) {
        Kit kit = this.kitOf(party);
        if (kit == null) {
            return MUTED + "\u0274\u1d0f\u1d1b \u1d04\u029c\u1d0f\ua731\u1d07\u0274";
        }
        return kit.getDisplayName() == null || kit.getDisplayName().isEmpty()
                ? VALUE + kit.getName() : kit.getDisplayName();
    }

    /**
     * Why the start button is or is not lit, as one of three named lines.
     *
     * <p>Three reasons rather than one message with the reason spliced in: a
     * server translating this needs each case worded on its own, and "needs at
     * least 2 players" does not translate as a variant of "already fighting".
     */
    private String startHint(Party party) {
        GuiConfigManager gui = this.plugin.gui();
        if (party.isFighting()) {
            return gui.line(MENU, "start", "why-fighting",
                            "{muted}\u1d00\u029f\u0280\u1d07\u1d00\u1d05\u028f \ua730\u026a\u0262\u029c\u1d1b\u026a\u0274\u0262", null);
        }
        if (party.size() < 2) {
            return gui.line(MENU, "start", "why-too-few",
                            "<#FF8A93>\u0274\u1d07\u1d07\u1d05\ua731 \u1d00\u1d1b \u029f\u1d07\u1d00\ua731\u1d1b 2 \u1d18\u029f\u1d00\u028f\u1d07\u0280\ua731", null);
        }
        // Not "free-for-all" any more - the mode is chosen on the next screen.
        return gui.line(MENU, "start", "why-ready",
                        "{label}\ua730\ua730\u1d00, \ua731\u1d18\u029f\u026a\u1d1b \u1d0f\u0280 \u1d05\u1d1c\u1d07\u029f\ua731", null);
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
        if (party == null) {
            player.closeInventory();
            return;
        }
        // Re-checked here as well as when the window was drawn. Leadership can
        // change while a window is open - the leader disbanding is the obvious
        // way - and a stale window must not be a way to act as one.
        if (!"party-leave-open".equals(id) && !party.isLeader(player.getUniqueId())) {
            player.sendMessage(Text.prefixed("&cOnly the party leader can do that."));
            player.closeInventory();
            return;
        }
        if ("party-kit".equals(id)) {
            new PartyModeMenu(this.plugin).open(player);
        } else if ("party-invite".equals(id)) {
            new PartyInviteMenu(this.plugin).open(player);
        } else if ("party-settings-open".equals(id)) {
            new PartySettingsMenu(this.plugin).open(player);
        } else if ("party-start".equals(id)) {
            new PartyModeMenu(this.plugin).open(player);
        } else if ("party-leave-open".equals(id)) {
            new PartyConfirmMenu(this.plugin, player).open(player);
        }
    }
}

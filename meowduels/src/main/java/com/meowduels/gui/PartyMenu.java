package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Style;
import com.meowduels.gui.Menu;
import com.meowduels.model.Kit;
import com.meowduels.model.Party;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
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

    @Override
    public void build() {
        this.createRaw(ROWS, Style.title("#FF8AD0", "#B04BD6", "\u1d18\u1d00\u0280\u1d1b\u028f"));
        Style.frame(this.inventory, ROWS);
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
        this.inventory.setItem(leader ? SLOT_LEAVE_LEADER : SLOT_LEAVE_MEMBER,
                Style.cancel(this.plugin.keyButton(), "party-leave-open",
                        leader ? "\u1d05\u026a\ua731\u0299\u1d00\u0274\u1d05 \u1d18\u1d00\u0280\u1d1b\u028f" : "\u029f\u1d07\u1d00\u1d20\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f",
                        "", LABEL + (leader ? "\u1d07\u0274\u1d05\ua731 \u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f \ua730\u1d0f\u0280 \u1d07\u1d20\u1d07\u0280\u028f\u1d0f\u0274\u1d07" : "\u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f \u1d04\u1d00\u0280\u0280\u026a\u1d07\ua731 \u1d0f\u0274 \u1d21\u026a\u1d1b\u029c\u1d0f\u1d1c\u1d1b \u028f\u1d0f\u1d1c"),
                        "", HINT + "\u028f\u1d0f\u1d1c'\u029f\u029f \u0299\u1d07 \u1d00\ua731\u1d0b\u1d07\u1d05 \u1d1b\u1d0f \u1d04\u1d0f\u0274\ua730\u026a\u0280\u1d0d"));
        player.openInventory(this.inventory);
    }

    /**
     * The party at a glance, at the top of the window.
     *
     * <p>Everything the party currently is, in one item, so no button below has
     * to repeat it. Which is also why the kit button only has to say "kit".
     */
    private ItemStack card(Party party) {
        boolean fighting = party.isFighting();
        return Items.of(Material.PAPER)
                .rawName(PartyMenu.accent("\u1d18\u1d00\u0280\u1d1b\u028f"))
                .rawLore("",
                         LABEL + "\u029f\u1d07\u1d00\u1d05\u1d07\u0280 " + SEP + VALUE + this.nameOf(party.getLeader()),
                         LABEL + "\u1d0d\u1d07\u1d0d\u0299\u1d07\u0280\ua731 " + SEP + VALUE + party.size(),
                         LABEL + "\u1d0b\u026a\u1d1b " + SEP + this.kitLabel(party),
                         LABEL + "\u1d0d\u1d0f\u1d05\u1d07 " + SEP + (fighting
                                 ? VALUE + party.getMode().getLabel()
                                 : MUTED + "\u0274\u1d0f\u1d1b \u1d18\u026a\u1d04\u1d0b\u1d07\u1d05 \u028f\u1d07\u1d1b"),
                         "",
                         LABEL + "\ua731\u1d1b\u1d00\u1d1b\u1d1c\ua731 " + SEP + (fighting
                                 ? "<#7CFF6B>\ua730\u026a\u0262\u029c\u1d1b\u026a\u0274\u0262 <dark_gray>\u00b7 <#E6E8EB>" + party.getAlive().size() + " \ua731\u1d1b\u026a\u029f\u029f \u026a\u0274"
                                 : MUTED + "\u1d21\u1d00\u026a\u1d1b\u026a\u0274\u0262 \ua730\u1d0f\u0280 \u1d1b\u029c\u1d07 \u029f\u1d07\u1d00\u1d05\u1d07\u0280"))
                .hideTooltip().build();
    }

    /** Kit, invite, settings, start - leader only. */
    private void addLeaderButtons(Party party) {
        Kit kit = this.kitOf(party);
        this.inventory.setItem(SLOT_KIT, Items.of(kit != null ? kit.getIcon() : Material.GOLDEN_APPLE)
                .rawName(VALUE + "\u1d0b\u026a\u1d1b")
                .rawLore("", LABEL + "\ua731\u1d07\u029f\u1d07\u1d04\u1d1b\u1d07\u1d05 " + SEP + this.kitLabel(party), "", HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d18\u026a\u1d04\u1d0b \u1d00 \u1d0b\u026a\u1d1b")
                .glow(kit != null).hideTooltip()
                .tag(this.plugin.keyButton(), "party-kit").build());
        this.inventory.setItem(SLOT_INVITE, Items.of(Material.PLAYER_HEAD)
                .rawName(VALUE + "\u026a\u0274\u1d20\u026a\u1d1b\u1d07")
                .rawLore("", LABEL + "\u1d00\u1d05\u1d05 \ua731\u1d0f\u1d0d\u1d07\u1d0f\u0274\u1d07 \u1d1b\u1d0f \u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f", "", HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d18\u026a\u1d04\u1d0b \u1d21\u029c\u1d0f")
                .hideTooltip().tag(this.plugin.keyButton(), "party-invite").build());
        this.inventory.setItem(SLOT_SETTINGS, Items.of(Material.GRINDSTONE)
                .rawName(VALUE + "\ua731\u1d07\u1d1b\u1d1b\u026a\u0274\u0262\ua731")
                .rawLore("", HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d0f\u1d18\u1d07\u0274")
                .hideTooltip().tag(this.plugin.keyButton(), "party-settings-open").build());
        // No kit requirement here any more: the kit is chosen inside the match
        // flow now, so demanding one before the button lights up would block the
        // only route to choosing it.
        boolean ready = !party.isFighting() && party.size() >= 2;
        this.inventory.setItem(SLOT_START, Style.confirm(this.plugin.keyButton(), "party-start", ready,
                "\ua731\u1d1b\u1d00\u0280\u1d1b \u1d0d\u1d00\u1d1b\u1d04\u029c",
                "\ua731\u1d1b\u1d00\u0280\u1d1b \u1d0d\u1d00\u1d1b\u1d04\u029c",
                "", this.startHint(party, kit != null)));
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
                this.inventory.setItem(slot, Items.of(Material.PAPER)
                        .rawName(VALUE + "+" + (party.size() - MEMBER_SLOTS + 1))
                        .rawLore("", LABEL + "\u1d0d\u1d0f\u0280\u1d07 \u1d0d\u1d07\u1d0d\u0299\u1d07\u0280\ua731 \u026a\u0274 \u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f")
                        .hideTooltip().build());
                break;
            }
            this.inventory.setItem(slot++, this.head(party, id));
            ++drawn;
        }
    }

    private ItemStack head(Party party, UUID id) {
        boolean isLeader = party.isLeader(id);
        boolean out = party.isFighting() && party.getWatching().contains(id);
        boolean online = Bukkit.getPlayer((UUID)id) != null;
        String state;
        if (party.isFighting()) {
            state = out ? "<dark_gray>\u2620 " + MUTED + "\u1d07\u029f\u026a\u1d0d\u026a\u0274\u1d00\u1d1b\u1d07\u1d05" : "<#7CFF6B>\u26a1 \u1d00\u029f\u026a\u1d20\u1d07";
        } else if (!online) {
            state = MUTED + "\u1d0f\ua730\ua730\u029f\u026a\u0274\u1d07";
        } else {
            state = "";
        }
        return Items.of(Material.PLAYER_HEAD)
                .skull(Bukkit.getOfflinePlayer((UUID)id))
                .rawName((isLeader ? "<#FFD65C>\u2605 " : "<#C79BFF>\u25c6 ") + VALUE + this.nameOf(id))
                .rawLore("", isLeader ? "<#FFD65C>\u029f\u1d07\u1d00\u1d05\u1d07\u0280" : LABEL + "\u1d0d\u1d07\u1d0d\u0299\u1d07\u0280", state)
                .glow(isLeader).hideTooltip().build();
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

    private String startHint(Party party, boolean hasKit) {
        if (party.isFighting()) {
            return MUTED + "\u1d00\u029f\u0280\u1d07\u1d00\u1d05\u028f \ua730\u026a\u0262\u029c\u1d1b\u026a\u0274\u0262";
        }
        if (party.size() < 2) {
            return "<#FF8A93>\u0274\u1d07\u1d07\u1d05\ua731 \u1d00\u1d1b \u029f\u1d07\u1d00\ua731\u1d1b 2 \u1d18\u029f\u1d00\u028f\u1d07\u0280\ua731";
        }
        // Not "free-for-all" any more - the mode is chosen on the next screen.
        return LABEL + "\ua730\ua730\u1d00, \ua731\u1d18\u029f\u026a\u1d1b \u1d0f\u0280 \u1d05\u1d1c\u1d07\u029f\ua731";
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

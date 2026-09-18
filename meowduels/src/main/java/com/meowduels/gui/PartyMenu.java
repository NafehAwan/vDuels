package com.meowduels.gui;

import com.meowduels.MeowDuels;
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
 * <p>Two versions of the same window. The leader gets the things that change the
 * party - kit, settings, start - and everyone else gets the roster without them.
 * Members are not shown greyed-out copies: a disabled button still reads as
 * something you could have if you clicked hard enough, and the honest version of
 * "you can't" is an empty slot and a line naming who can.
 *
 * <p>Laid out like the duel confirm menu on purpose - options row, subject row,
 * action row - because it answers the same sort of question and there is no
 * reason for two screens in one plugin to disagree about where start lives.
 */
public class PartyMenu
extends Menu {
    private static final int ROWS = 4;
    private static final int SIZE = ROWS * 9;
    private static final int SLOT_KIT = 11;
    private static final int SLOT_INFO = 13;
    private static final int SLOT_SETTINGS = 15;
    private static final int MEMBERS_FROM = 18;
    private static final int MEMBERS_TO = 27;
    private static final int SLOT_START = 29;
    private static final int SLOT_LEAVE = 33;

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
        this.createRaw(ROWS, "<dark_gray>\u258f " + PartyMenu.accent("\u1d18\u1d00\u0280\u1d1b\u028f"));
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).rawName(" ").build();
        for (int i = 0; i < SIZE; ++i) {
            this.inventory.setItem(i, filler);
        }
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
        if (leader) {
            this.addLeaderButtons(party);
        }
        this.addRoster(party, leader);
        this.inventory.setItem(SLOT_LEAVE, Items.of(Material.RED_DYE)
                .rawName("<gradient:#FF6B6B:#A01028>" + (leader ? "\u1d05\u026a\ua731\u0299\u1d00\u0274\u1d05 \u1d18\u1d00\u0280\u1d1b\u028f" : "\u029f\u1d07\u1d00\u1d20\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f") + "</gradient>")
                .rawLore("", LABEL + (leader ? "\u1d07\u0274\u1d05\ua731 \u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f \ua730\u1d0f\u0280 \u1d07\u1d20\u1d07\u0280\u028f\u1d0f\u0274\u1d07" : "\u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f \u1d04\u1d00\u0280\u0280\u026a\u1d07\ua731 \u1d0f\u0274 \u1d21\u026a\u1d1b\u029c\u1d0f\u1d1c\u1d1b \u028f\u1d0f\u1d1c"))
                .hideTooltip().tag(this.plugin.keyButton(), "party-leave-open").build());
        player.openInventory(this.inventory);
    }

    /** Kit, settings and start - leader only. */
    private void addLeaderButtons(Party party) {
        Kit kit = this.kitOf(party);
        this.inventory.setItem(SLOT_KIT, Items.of(kit != null ? kit.getIcon() : Material.GOLDEN_APPLE)
                .rawName(PartyMenu.accent("\u1d0b\u026a\u1d1b"))
                .rawLore("", LABEL + "\ua731\u1d07\u029f\u1d07\u1d04\u1d1b\u1d07\u1d05 " + SEP + this.kitLabel(party), "", HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d18\u026a\u1d04\u1d0b \u1d00 \u1d0b\u026a\u1d1b")
                .glow(kit != null).hideTooltip()
                .tag(this.plugin.keyButton(), "party-kit").build());
        this.inventory.setItem(SLOT_SETTINGS, Items.of(Material.GRINDSTONE)
                .rawName(PartyMenu.accent("\ua731\u1d07\u1d1b\u1d1b\u026a\u0274\u0262\ua731"))
                .rawLore("", HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d0f\u1d18\u1d07\u0274")
                .hideTooltip().tag(this.plugin.keyButton(), "party-settings-open").build());
        boolean ready = !party.isFighting() && party.size() >= 2 && kit != null;
        this.inventory.setItem(SLOT_START, Items.of(ready ? Material.LIME_DYE : Material.GRAY_DYE)
                .rawName(ready ? "<gradient:#7CFF6B:#1FA32F>\ua731\u1d1b\u1d00\u0280\u1d1b \u1d0d\u1d00\u1d1b\u1d04\u029c</gradient>" : MUTED + "\ua731\u1d1b\u1d00\u0280\u1d1b \u1d0d\u1d00\u1d1b\u1d04\u029c")
                .rawLore("", this.startHint(party, kit != null))
                .glow(ready).hideTooltip()
                .tag(this.plugin.keyButton(), "party-start").build());
    }

    /** What the party is, and who is in it - the same for everyone. */
    private void addRoster(Party party, boolean leader) {
        this.inventory.setItem(SLOT_INFO, Items.of(Material.PAPER)
                .rawName(PartyMenu.accent("\u1d18\u1d00\u0280\u1d1b\u028f"))
                .rawLore("",
                         LABEL + "\u029f\u1d07\u1d00\u1d05\u1d07\u0280 " + SEP + VALUE + this.nameOf(party.getLeader()),
                         LABEL + "\u1d0d\u1d07\u1d0d\u0299\u1d07\u0280\ua731 " + SEP + VALUE + party.size(),
                         LABEL + "\u1d0b\u026a\u1d1b " + SEP + this.kitLabel(party),
                         LABEL + "\ua731\u1d1b\u1d00\u1d1b\u1d1c\ua731 " + SEP + (party.isFighting()
                                 ? "<#7CFF6B>\ua730\u026a\u0262\u029c\u1d1b\u026a\u0274\u0262" : MUTED + "\u1d21\u1d00\u026a\u1d1b\u026a\u0274\u0262"),
                         leader ? "" : "",
                         leader ? "" : MUTED + "\u1d0f\u0274\u029f\u028f \u1d1b\u029c\u1d07 \u029f\u1d07\u1d00\u1d05\u1d07\u0280 \u1d04\u1d00\u0274 \u1d04\u029c\u1d00\u0274\u0262\u1d07 \u1d1b\u029c\u1d07\ua731\u1d07")
                .hideTooltip().build());
        int slot = MEMBERS_FROM;
        for (UUID id : party.getMembers()) {
            if (slot >= MEMBERS_TO) {
                break;
            }
            boolean isLeader = party.isLeader(id);
            boolean out = party.isFighting() && party.getWatching().contains(id);
            this.inventory.setItem(slot++, Items.of(Material.PLAYER_HEAD)
                    .skull(Bukkit.getOfflinePlayer((UUID)id))
                    .rawName(VALUE + this.nameOf(id))
                    .rawLore("", isLeader ? "<#FFD65C>\u029f\u1d07\u1d00\u1d05\u1d07\u0280" : LABEL + "\u1d0d\u1d07\u1d0d\u0299\u1d07\u0280",
                             party.isFighting()
                                 ? (out ? MUTED + "\u1d07\u029f\u026a\u1d0d\u026a\u0274\u1d00\u1d1b\u1d07\u1d05" : "<#7CFF6B>\u1d00\u029f\u026a\u1d20\u1d07")
                                 : "")
                    .hideTooltip().build());
        }
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
        if (!hasKit) {
            return "<#FF8A93>\u1d18\u026a\u1d04\u1d0b \u1d00 \u1d0b\u026a\u1d1b \ua730\u026a\u0280\ua731\u1d1b";
        }
        return LABEL + "\ua730\u0280\u1d07\u1d07-\ua730\u1d0f\u0280-\u1d00\u029f\u029f, \u029f\u1d00\ua731\u1d1b \u1d0f\u0274\u1d07 \ua731\u1d1b\u1d00\u0274\u1d05\u026a\u0274\u0262 \u1d21\u026a\u0274\ua731";
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
        if ("party-kit".equals(id) || "party-settings-open".equals(id) || "party-start".equals(id)) {
            if (!party.isLeader(player.getUniqueId())) {
                player.sendMessage(Text.prefixed("&cOnly the party leader can do that."));
                player.closeInventory();
                return;
            }
        }
        if ("party-kit".equals(id)) {
            new PartyKitMenu(this.plugin).open(player);
        } else if ("party-settings-open".equals(id)) {
            new PartySettingsMenu(this.plugin).open(player);
        } else if ("party-start".equals(id)) {
            player.closeInventory();
            this.plugin.getPartyManager().startMatch(player);
        } else if ("party-leave-open".equals(id)) {
            new PartyConfirmMenu(this.plugin, player).open(player);
        }
    }
}

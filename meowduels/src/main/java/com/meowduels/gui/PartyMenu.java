package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.model.Kit;
import com.meowduels.model.Party;
import com.meowduels.util.Items;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The party screen: who is in it, what you are about to fight, and the button
 * that starts it.
 *
 * <p>Laid out like the duel confirm menu on purpose - options row, subject row,
 * action row - because it answers the same question and there is no reason for
 * two screens in the same plugin to disagree about where the start button is.
 *
 * <p>Members are drawn as heads on the middle row rather than a text list: a
 * party is people, and eight names in lore is the least readable way to show
 * eight people.
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

    /** Built per viewer: whether the start button is theirs to press depends on
     *  who is looking at it. */
    public void openFor(Player player) {
        this.build();
        Party party = this.plugin.getPartyManager().partyOf(player.getUniqueId());
        if (party == null) {
            player.sendMessage(com.meowduels.util.Text.prefixed("&cYou're not in a party."));
            return;
        }
        boolean leader = party.isLeader(player.getUniqueId());
        Kit kit = party.getKit() == null ? null : this.plugin.getKitManager().get(party.getKit());
        String kitName = kit == null ? MUTED + "\u0274\u1d0f\u1d1b \u1d04\u029c\u1d0f\ua731\u1d07\u0274"
                : (kit.getDisplayName() == null || kit.getDisplayName().isEmpty()
                   ? VALUE + kit.getName() : kit.getDisplayName());

        this.inventory.setItem(SLOT_KIT, Items.of(kit != null ? kit.getIcon() : Material.GOLDEN_APPLE)
                .rawName(PartyMenu.accent("\u1d0b\u026a\u1d1b"))
                .rawLore("", LABEL + "\ua731\u1d07\u029f\u1d07\u1d04\u1d1b\u1d07\u1d05 " + SEP + kitName, "",
                         leader ? HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d18\u026a\u1d04\u1d0b \u1d00 \u1d0b\u026a\u1d1b" : MUTED + "\u1d0f\u0274\u029f\u028f \u1d1b\u029c\u1d07 \u029f\u1d07\u1d00\u1d05\u1d07\u0280 \u1d04\u1d00\u0274 \u1d04\u029c\u1d00\u0274\u0262\u1d07 \u1d1b\u029c\u026a\ua731")
                .glow(kit != null).hideTooltip()
                .tag(this.plugin.keyButton(), "party-kit").build());

        this.inventory.setItem(SLOT_INFO, Items.of(Material.PAPER)
                .rawName(PartyMenu.accent("\u1d18\u1d00\u0280\u1d1b\u028f"))
                .rawLore("",
                         LABEL + "\u029f\u1d07\u1d00\u1d05\u1d07\u0280 " + SEP + VALUE + this.nameOf(party.getLeader()),
                         LABEL + "\u1d0d\u1d07\u1d0d\u0299\u1d07\u0280\ua731 " + SEP + VALUE + party.size(),
                         LABEL + "\ua731\u1d1b\u1d00\u1d1b\u1d1c\ua731 " + SEP + (party.isFighting()
                                 ? "<#7CFF6B>\ua730\u026a\u0262\u029c\u1d1b\u026a\u0274\u0262" : MUTED + "\u1d21\u1d00\u026a\u1d1b\u026a\u0274\u0262"))
                .hideTooltip().build());

        this.inventory.setItem(SLOT_SETTINGS, Items.of(Material.GRINDSTONE)
                .rawName(PartyMenu.accent("\ua731\u1d07\u1d1b\u1d1b\u026a\u0274\u0262\ua731"))
                .rawLore("", leader ? HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d0f\u1d18\u1d07\u0274" : MUTED + "\u1d0f\u0274\u029f\u028f \u1d1b\u029c\u1d07 \u029f\u1d07\u1d00\u1d05\u1d07\u0280 \u1d04\u1d00\u0274 \u1d04\u029c\u1d00\u0274\u0262\u1d07 \u1d1b\u029c\u026a\ua731")
                .hideTooltip().tag(this.plugin.keyButton(), "party-settings-open").build());

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

        boolean ready = leader && !party.isFighting() && party.size() >= 2 && kit != null;
        this.inventory.setItem(SLOT_START, Items.of(ready ? Material.LIME_DYE : Material.GRAY_DYE)
                .rawName(ready ? "<gradient:#7CFF6B:#1FA32F>\ua731\u1d1b\u1d00\u0280\u1d1b \u1d0d\u1d00\u1d1b\u1d04\u029c</gradient>" : MUTED + "\ua731\u1d1b\u1d00\u0280\u1d1b \u1d0d\u1d00\u1d1b\u1d04\u029c")
                .rawLore("", this.startHint(party, leader, kit != null))
                .glow(ready).hideTooltip()
                .tag(this.plugin.keyButton(), "party-start").build());

        this.inventory.setItem(SLOT_LEAVE, Items.of(Material.RED_DYE)
                .rawName("<gradient:#FF6B6B:#A01028>" + (leader ? "\u1d05\u026a\ua731\u0299\u1d00\u0274\u1d05 \u1d18\u1d00\u0280\u1d1b\u028f" : "\u029f\u1d07\u1d00\u1d20\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f") + "</gradient>")
                .rawLore("", LABEL + (leader ? "\u1d07\u0274\u1d05\ua731 \u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f \ua730\u1d0f\u0280 \u1d07\u1d20\u1d07\u0280\u028f\u1d0f\u0274\u1d07" : "\u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f \u1d04\u1d00\u0280\u0280\u026a\u1d07\ua731 \u1d0f\u0274 \u1d21\u026a\u1d1b\u029c\u1d0f\u1d1c\u1d1b \u028f\u1d0f\u1d1c"))
                .hideTooltip().tag(this.plugin.keyButton(), "party-leave-open").build());

        player.openInventory(this.inventory);
    }

    private String startHint(Party party, boolean leader, boolean hasKit) {
        if (!leader) {
            return MUTED + "\u1d0f\u0274\u029f\u028f \u1d1b\u029c\u1d07 \u029f\u1d07\u1d00\u1d05\u1d07\u0280 \u1d04\u1d00\u0274 \u1d04\u029c\u1d00\u0274\u0262\u1d07 \u1d1b\u029c\u026a\ua731";
        }
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
        boolean leader = party.isLeader(player.getUniqueId());
        if ("party-kit".equals(id)) {
            if (!leader) {
                return;
            }
            new PartyKitMenu(this.plugin).open(player);
        } else if ("party-settings-open".equals(id)) {
            if (!leader) {
                return;
            }
            new PartySettingsMenu(this.plugin).open(player);
        } else if ("party-start".equals(id)) {
            player.closeInventory();
            this.plugin.getPartyManager().startMatch(player);
        } else if ("party-leave-open".equals(id)) {
            new PartyConfirmMenu(this.plugin, player).open(player);
        }
    }
}

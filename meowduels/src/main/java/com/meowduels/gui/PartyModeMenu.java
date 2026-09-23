package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.model.Party;
import com.meowduels.model.PartyMode;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * What kind of party match to play.
 *
 * <p>Three slots across the middle, evenly spaced, with the two unbuilt modes
 * shown greyed and labelled rather than hidden. A picker with one option in it
 * reads as broken; a picker with two greyed options reads as a roadmap.
 *
 * <p>Cool palette on purpose - ice blue through indigo - rather than the crimson
 * the duel menus use. A party is not a duel and should not open looking like
 * one.
 */
public class PartyModeMenu
extends Menu {
    private static final int ROWS = 3;
    private static final int SIZE = ROWS * 9;
    private static final int SLOT_FFA = 11;
    private static final int SLOT_SPLIT = 13;
    private static final int SLOT_DUELS = 15;
    private static final int SLOT_BACK = 18;

    private static final String VALUE = "<#E6E8EB>";
    private static final String LABEL = "<#8E959D>";
    private static final String MUTED = "<#6B7079>";
    private static final String HINT = "<dark_gray>\u25b8 <#8E959D>";

    private final MeowDuels plugin;

    public PartyModeMenu(MeowDuels plugin) {
        this.plugin = plugin;
    }

    /** Ice blue to indigo - the party palette. */
    static String accent(String text) {
        return "<gradient:#7DE2FF:#4B7BFF>" + text + "</gradient>";
    }

    @Override
    public void build() {
    }

    @Override
    public void open(Player player) {
        Party party = this.plugin.getPartyManager().partyOf(player.getUniqueId());
        if (party == null) {
            player.sendMessage(Text.prefixed("&cYou're not in a party."));
            return;
        }
        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage(Text.prefixed("&cOnly the party leader can start a match."));
            return;
        }
        this.createRaw(ROWS, Style.title("#7DE2FF", "#4B7BFF", "\u1d18\u1d00\u0280\u1d1b\u028f \u1d0d\u1d00\u1d1b\u1d04\u029c"));
        Style.frame(this.inventory, ROWS);
        this.inventory.setItem(SLOT_FFA, this.mode(PartyMode.FFA, Material.DIAMOND_SWORD));
        this.inventory.setItem(SLOT_SPLIT, this.mode(PartyMode.SPLIT, Material.SHIELD));
        this.inventory.setItem(SLOT_DUELS, this.mode(PartyMode.DUELS, Material.IRON_SWORD));
        this.inventory.setItem(SLOT_BACK, Style.back(this.plugin.keyButton(), "mode-back"));
        player.openInventory(this.inventory);
    }

    private ItemStack mode(PartyMode mode, Material icon) {
        if (!mode.isReady()) {
            return Items.of(Material.GRAY_DYE)
                    .rawName(MUTED + mode.getLabel())
                    .rawLore("", MUTED + mode.getDescription(), "", "<#8E959D>\u1d04\u1d0f\u1d0d\u026a\u0274\u0262 \ua731\u1d0f\u1d0f\u0274")
                    .hideTooltip().build();
        }
        return Items.of(icon)
                .rawName(PartyModeMenu.accent(mode.getLabel()))
                .rawLore("", LABEL + mode.getDescription(), "", HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d04\u029c\u1d0f\u1d0f\ua731\u1d07")
                .glow(true).hideTooltip()
                .tag(this.plugin.keyButton(), "mode-" + mode.name()).build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String id = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if (id == null) {
            return;
        }
        if ("mode-back".equals(id)) {
            new PartyMenu(this.plugin).openFor(player);
            return;
        }
        if (!id.startsWith("mode-")) {
            return;
        }
        PartyMode mode;
        try {
            mode = PartyMode.valueOf(id.substring(5));
        }
        catch (IllegalArgumentException e) {
            return;
        }
        // Unready modes carry no button tag, so this is belt and braces - but a
        // stale window from a build where one of them WAS tagged should not be
        // a way into a mode that does not exist yet.
        if (!mode.isReady()) {
            return;
        }
        // Party Duels picks its opponent BEFORE its kit: the kit is a property
        // of the match, and there is no match until there are two parties.
        if (mode == PartyMode.DUELS) {
            new PartyOpponentMenu(this.plugin).open(player);
            return;
        }
        new PartyKitMenu(this.plugin, mode).open(player);
    }
}

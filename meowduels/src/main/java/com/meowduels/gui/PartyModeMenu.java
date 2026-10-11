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
import java.util.Map;
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
    /** The name of this screen's file: {@code gui/partymode.yml}. */
    private static final String MENU = "partymode";

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
            player.sendMessage(this.plugin.messages().get("notice.party.re-not-party-2"));
            return;
        }
        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage(this.plugin.messages().get("notice.party.only-party-leader-start-2"));
            return;
        }
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        this.createRaw(rows, gui.title(MENU,
                "<dark_gray>\u258f <gradient:#7DE2FF:#4B7BFF>\u1d18\u1d00\u0280\u1d1b\u028f \u1d0d\u1d00\u1d1b\u1d04\u029c</gradient>"));
        gui.frame(MENU, this.inventory, rows);
        this.put(gui, rows, PartyMode.FFA, gui.slot(MENU, "ffa", SLOT_FFA, rows),
                 gui.toggle(MENU, "ffa", PartyMode.FFA.isReady(), this.tokens(PartyMode.FFA),
                            ready(Material.DIAMOND_SWORD), unready()));
        this.put(gui, rows, PartyMode.SPLIT, gui.slot(MENU, "split", SLOT_SPLIT, rows),
                 gui.toggle(MENU, "split", PartyMode.SPLIT.isReady(), this.tokens(PartyMode.SPLIT),
                            ready(Material.SHIELD), unready()));
        this.put(gui, rows, PartyMode.DUELS, gui.slot(MENU, "duels", SLOT_DUELS, rows),
                 gui.toggle(MENU, "duels", PartyMode.DUELS.isReady(), this.tokens(PartyMode.DUELS),
                            ready(Material.IRON_SWORD), unready()));
        this.inventory.setItem(Math.min(rows * 9 - 1, gui.slot(MENU, "back", SLOT_BACK, rows)),
                gui.back(MENU, "back").tag(this.plugin.keyButton(), "mode-back").build());
        player.openInventory(this.inventory);
    }

    /** A mode that can be played: its own icon, the party gradient, a shimmer. */
    private static Face ready(Material icon) {
        return Face.of(icon,
                       "<gradient:#7DE2FF:#4B7BFF>{mode}</gradient>",
                       "",
                       "{label}{description}",
                       "",
                       "{hint}\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d04\u029c\u1d0f\u1d0f\ua731\u1d07").glow();
    }

    /**
     * A mode that is not built yet - greyed and labelled, not hidden.
     *
     * <p>A picker with one option in it reads as broken; a picker with two
     * greyed options reads as a roadmap.
     */
    private static Face unready() {
        return Face.of(Material.GRAY_DYE,
                       "{muted}{mode}",
                       "",
                       "{muted}{description}",
                       "",
                       "{label}\u1d04\u1d0f\u1d0d\u026a\u0274\u0262 \ua731\u1d0f\u1d0f\u0274");
    }

    private Map<String, String> tokens(PartyMode mode) {
        HashMap<String, String> tokens = new HashMap<String, String>();
        tokens.put("mode", mode.getLabel());
        tokens.put("description", mode.getDescription());
        return tokens;
    }

    /**
     * Places a mode, tagging it only when it can actually be picked.
     *
     * <p>An unready mode carries no button tag at all, which is what stops a
     * click on it reaching a mode that does not exist yet.
     */
    private void put(GuiConfigManager gui, int rows, PartyMode mode, int slot, Items item) {
        item.hideTooltip();
        if (mode.isReady()) {
            item.tag(this.plugin.keyButton(), "mode-" + mode.name());
        }
        this.inventory.setItem(Math.max(0, Math.min(rows * 9 - 1, slot)), item.build());
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

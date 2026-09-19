package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.managers.CategoryManager;
import com.meowduels.model.Kit;
import com.meowduels.model.Party;
import com.meowduels.model.PartyMode;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The party's kit picker, built to match the duel one.
 *
 * <p>Same four rows, same fourteen slots in two inset rows of seven, same
 * black-glass frame, same category arrow in the corner. Only the header differs,
 * and it differs on purpose: it names the mode, so the window says which match
 * is being set up rather than just "kit".
 *
 * <p>Two things the duel picker does not do, both there to stop a kit being
 * unreachable - which is the complaint this menu started from:
 *
 * <p>The category cycle ends on a synthetic "all kits" page. Categories are
 * hand-maintained, so a newly added kit belongs to none of them and would
 * otherwise be in no page at all.
 *
 * <p>And a category holding more than fourteen kits pages rather than truncating
 * at fourteen.
 */
public class PartyKitMenu
extends Menu {
    private static final int ROWS = 4;
    private static final int SIZE = ROWS * 9;
    private static final int[] KIT_SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private static final int SLOT_BACK = 27;
    private static final int SLOT_PREV = 29;
    private static final int SLOT_NEXT = 33;
    private static final int SLOT_CATEGORY = 35;

    private static final String VALUE = "<#E6E8EB>";
    private static final String LABEL = "<#8E959D>";
    private static final String HINT = "<dark_gray>\u25b8 <#8E959D>";

    private final MeowDuels plugin;
    private final PartyMode mode;
    private int categoryIndex = 0;
    private int page = 0;

    public PartyKitMenu(MeowDuels plugin, PartyMode mode) {
        this.plugin = plugin;
        this.mode = mode;
    }

    @Override
    public void build() {
    }

    /** The kits on the current page of the current category. */
    private List<String> currentKits(List<CategoryManager.Category> categories) {
        if (categories.isEmpty() || this.categoryIndex >= categories.size()) {
            // The "all kits" page: everything, in kit order.
            ArrayList<String> all = new ArrayList<String>();
            for (Kit kit : this.plugin.getKitManager().all()) {
                all.add(kit.getName());
            }
            return all;
        }
        return this.plugin.getCategoryManager().kitsFor(categories.get(this.categoryIndex));
    }

    private String headerFor(List<CategoryManager.Category> categories) {
        if (categories.isEmpty()) {
            return "";
        }
        if (this.categoryIndex >= categories.size()) {
            return " <dark_gray>\u1d00\u029f\u029f \u1d0b\u026a\u1d1b\ua731";
        }
        return " <dark_gray>" + categories.get(this.categoryIndex).getHeader();
    }

    @Override
    public void open(Player player) {
        Party party = this.plugin.getPartyManager().partyOf(player.getUniqueId());
        if (party == null || !party.isLeader(player.getUniqueId())) {
            player.sendMessage(Text.prefixed("&cOnly the party leader can pick the kit."));
            return;
        }
        List<CategoryManager.Category> categories = this.plugin.getCategoryManager().all();
        // One extra step in the cycle for "all kits", so every kit is reachable
        // even when it is in no category.
        int steps = categories.isEmpty() ? 1 : categories.size() + 1;
        if (this.categoryIndex >= steps) {
            this.categoryIndex = 0;
        }
        List<String> kits = new ArrayList<String>(new LinkedHashSet<String>(this.currentKits(categories)));
        int pages = Math.max(1, (kits.size() + KIT_SLOTS.length - 1) / KIT_SLOTS.length);
        if (this.page >= pages) {
            this.page = 0;
        }
        int from = this.page * KIT_SLOTS.length;

        this.createRaw(ROWS, "<dark_gray>\u258f " + PartyModeMenu.accent(this.mode.getLabel())
                + this.headerFor(categories)
                + (pages > 1 ? " <dark_gray>" + (this.page + 1) + "/" + pages : ""));
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).rawName(" ").build();
        for (int i = 0; i < SIZE; ++i) {
            this.inventory.setItem(i, filler);
        }
        String chosen = party.getKit();
        for (int i = 0; i < KIT_SLOTS.length && from + i < kits.size(); ++i) {
            ItemStack icon = this.kitIcon(kits.get(from + i), chosen);
            if (icon != null) {
                this.inventory.setItem(KIT_SLOTS[i], icon);
            }
        }
        if (kits.isEmpty()) {
            this.inventory.setItem(KIT_SLOTS[3], Items.of(Material.BARRIER)
                    .rawName("<#FF8A93>\u0274\u1d0f \u1d0b\u026a\u1d1b\ua731 \u029c\u1d07\u0280\u1d07")
                    .rawLore("", LABEL + "\u1d1b\u029c\u026a\ua731 \u1d04\u1d00\u1d1b\u1d07\u0262\u1d0f\u0280\u028f \u029c\u1d00\ua731 \u0274\u1d0f \u1d0b\u026a\u1d1b\ua731")
                    .hideTooltip().build());
        }
        this.inventory.setItem(SLOT_BACK, Items.of(Material.ARROW)
                .rawName(VALUE + "\u0299\u1d00\u1d04\u1d0b")
                .hideTooltip().tag(this.plugin.keyButton(), "kit-back").build());
        if (steps > 1) {
            this.inventory.setItem(SLOT_CATEGORY, Items.of(Material.ENDER_EYE)
                    .rawName(PartyModeMenu.accent("\u0274\u1d07x\u1d1b \u1d04\u1d00\u1d1b\u1d07\u0262\u1d0f\u0280\u028f"))
                    .rawLore("", LABEL + "\u1d04\u028f\u1d04\u029f\u1d07 \u1d1b\u029c\u0280\u1d0f\u1d1c\u0262\u029c \u1d1b\u029c\u1d07 \u1d0b\u026a\u1d1b \u1d04\u1d00\u1d1b\u1d07\u0262\u1d0f\u0280\u026a\u1d07\ua731")
                    .hideTooltip().tag(this.plugin.keyButton(), "kit-cat").build());
        }
        if (this.page > 0) {
            this.inventory.setItem(SLOT_PREV, Items.of(Material.ARROW)
                    .rawName(VALUE + "\u1d18\u0280\u1d07\u1d20\u026a\u1d0f\u1d1c\ua731 \u1d18\u1d00\u0262\u1d07")
                    .hideTooltip().tag(this.plugin.keyButton(), "kit-prev").build());
        }
        if (this.page < pages - 1) {
            this.inventory.setItem(SLOT_NEXT, Items.of(Material.ARROW)
                    .rawName(VALUE + "\u0274\u1d07x\u1d1b \u1d18\u1d00\u0262\u1d07")
                    .hideTooltip().tag(this.plugin.keyButton(), "kit-next").build());
        }
        player.openInventory(this.inventory);
    }

    private ItemStack kitIcon(String name, String chosen) {
        Kit kit = this.plugin.getKitManager().get(name);
        if (kit == null) {
            return null;
        }
        boolean picked = kit.getName().equalsIgnoreCase(chosen);
        Items item = Items.of(kit.getIcon())
                .rawLore("", picked ? "<#7CFF6B>\u25cf \u1d04\u029c\u1d0f\ua731\u1d07\u0274" : HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d18\u026a\u1d04\u1d0b")
                .glow(picked)
                .hideTooltip()
                .tag(this.plugin.keyKit(), kit.getName());
        if (kit.getDisplayName() != null && !kit.getDisplayName().isEmpty()) {
            item.miniName(kit.getDisplayName());
        } else {
            item.rawName(VALUE + kit.getName());
        }
        return item.build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String button = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if ("kit-back".equals(button)) {
            new PartyModeMenu(this.plugin).open(player);
            return;
        }
        if ("kit-cat".equals(button)) {
            List<CategoryManager.Category> categories = this.plugin.getCategoryManager().all();
            int steps = categories.isEmpty() ? 1 : categories.size() + 1;
            this.categoryIndex = (this.categoryIndex + 1) % steps;
            this.page = 0;
            this.open(player);
            return;
        }
        if ("kit-next".equals(button) || "kit-prev".equals(button)) {
            this.page += "kit-next".equals(button) ? 1 : -1;
            if (this.page < 0) {
                this.page = 0;
            }
            this.open(player);
            return;
        }
        String kit = Items.readTag(event.getCurrentItem(), this.plugin.keyKit());
        if (kit == null || !this.plugin.getKitManager().exists(kit)) {
            return;
        }
        Party party = this.plugin.getPartyManager().partyOf(player.getUniqueId());
        if (party == null || !party.isLeader(player.getUniqueId())) {
            player.closeInventory();
            return;
        }
        party.setKit(kit);
        if (this.mode == PartyMode.SPLIT) {
            // Split's team picker already shows the mode, the kit and every
            // player, which is everything a confirm would have said.
            party.getTeams().clear();
            new PartyTeamMenu(this.plugin).open(player);
            return;
        }
        new PartyStartConfirmMenu(this.plugin, this.mode).open(player);
    }
}

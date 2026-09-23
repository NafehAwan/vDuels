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
 * The party's kit picker: the duel picker with a different title.
 *
 * <p>Same four rows, same fourteen slots, same frame, one arrow bottom-right and
 * nothing else. The title is plain small caps in the default colour - the mode,
 * an arrow, the category - because that is what the duel picker's title is, and
 * a coloured gradient here made the two windows look like different features.
 *
 * <p>The one arrow walks EVERYTHING: the next page of this category if it has
 * one, otherwise the next category, and finally a page of every kit there is.
 * That last step exists because categories are hand-maintained - a kit in none
 * of them would otherwise be on no page at all.
 */
public class PartyKitMenu
extends Menu {
    private static final int ROWS = 4;
    private static final int SIZE = ROWS * 9;
    private static final int[] KIT_SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private static final int SLOT_ARROW = 35;

    private final MeowDuels plugin;
    private final PartyMode mode;
    private int step = 0;

    public PartyKitMenu(MeowDuels plugin, PartyMode mode) {
        this.plugin = plugin;
        this.mode = mode;
    }

    /** One screenful: what to call it, and which kits are on it. */
    private static final class Page {
        private final String title;
        private final List<String> kits;

        private Page(String title, List<String> kits) {
            this.title = title;
            this.kits = kits;
        }
    }

    /**
     * Every screenful there is, in the order the arrow walks them.
     *
     * <p>Categories only. A kit in no category does not appear - being in a
     * category is how a kit is offered here, and a catch-all page would undo
     * that by putting every uncategorised kit back on the last screen.
     *
     * <p>The one exception is a server with no categories configured at all,
     * where that rule would leave an empty menu with nothing to fix it from.
     * The duel picker does the same.
     *
     * <p>Flattened to a single list on purpose: with categories and pages within
     * a category, "what does the arrow do next" has one answer instead of two
     * nested ones.
     */
    private List<Page> pages() {
        ArrayList<Page> out = new ArrayList<Page>();
        List<CategoryManager.Category> categories = this.plugin.getCategoryManager().all();
        if (categories.isEmpty()) {
            ArrayList<String> all = new ArrayList<String>();
            for (Kit kit : this.plugin.getKitManager().all()) {
                all.add(kit.getName());
            }
            this.slice(out, "\u1d0b\u026a\u1d1b\ua731", all);
            return out;
        }
        for (CategoryManager.Category category : categories) {
            this.slice(out, PartyKitMenu.smallCaps(category.getId()),
                    this.plugin.getCategoryManager().kitsFor(category));
        }
        return out;
    }

    private void slice(List<Page> out, String title, List<String> kits) {
        ArrayList<String> unique = new ArrayList<String>(new LinkedHashSet<String>(kits));
        if (unique.isEmpty()) {
            // Skipped rather than given a blank screen - the arrow should never
            // land on nothing.
            return;
        }
        for (int i = 0; i < unique.size(); i += KIT_SLOTS.length) {
            out.add(new Page(title, unique.subList(i, Math.min(unique.size(), i + KIT_SLOTS.length))));
        }
    }

    /** Small-caps, so a category id reads like the rest of the title. */
    private static String smallCaps(String text) {
        return Text.smallCaps(text == null ? "" : text.replace('_', ' '));
    }

    @Override
    public void build() {
    }

    @Override
    public void open(Player player) {
        Party party = this.plugin.getPartyManager().partyOf(player.getUniqueId());
        if (party == null || !party.isLeader(player.getUniqueId())) {
            player.sendMessage(Text.prefixed("&cOnly the party leader can pick the kit."));
            return;
        }
        List<Page> pages = this.pages();
        if (pages.isEmpty()) {
            player.sendMessage(Text.prefixed("&cNo kits are in a category yet."));
            player.sendMessage(Text.prefixed("&8Kits outside a category aren't offered - add them with &f/category&8."));
            return;
        }
        if (this.step >= pages.size() || this.step < 0) {
            this.step = 0;
        }
        Page page = pages.get(this.step);
        this.createRaw(ROWS, "<dark_gray>\u258f " + this.mode.getLabel() + " " + Style.SEP + Style.VALUE + page.title);
        Style.frame(this.inventory, ROWS);
        String chosen = party.getKit();
        for (int i = 0; i < KIT_SLOTS.length && i < page.kits.size(); ++i) {
            ItemStack icon = this.kitIcon(page.kits.get(i), chosen);
            if (icon != null) {
                this.inventory.setItem(KIT_SLOTS[i], icon);
            }
        }
        if (pages.size() > 1) {
            this.inventory.setItem(SLOT_ARROW, Items.of(Material.ARROW)
                    .rawName(Style.VALUE + "\u0274\u1d07x\u1d1b \u1d04\u1d00\u1d1b\u1d07\u0262\u1d0f\u0280\u028f")
                    .rawLore("", Style.HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d04\u028f\u1d04\u029f\u1d07")
                    .hideTooltip()
                    .tag(this.plugin.keyButton(), "next-cat").build());
        }
        player.openInventory(this.inventory);
    }

    private ItemStack kitIcon(String name, String chosen) {
        Kit kit = this.plugin.getKitManager().get(name);
        if (kit == null) {
            return null;
        }
        Items item = Items.of(kit.getIcon())
                .glow(kit.getName().equalsIgnoreCase(chosen))
                .hideTooltip()
                .tag(this.plugin.keyKit(), kit.getName());
        if (kit.getDisplayName() != null && !kit.getDisplayName().isEmpty()) {
            item.miniName(kit.getDisplayName());
        } else {
            item.name("&e" + kit.getName());
        }
        return item.build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if ("next-cat".equals(Items.readTag(clicked, this.plugin.keyButton()))) {
            int size = this.pages().size();
            if (size > 0) {
                this.step = (this.step + 1) % size;
                this.open(player);
            }
            return;
        }
        String kit = Items.readTag(clicked, this.plugin.keyKit());
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
            party.getTeams().clear();
            new PartyTeamMenu(this.plugin).open(player);
            return;
        }
        if (this.mode == PartyMode.DUELS) {
            new PartyDuelsConfirmMenu(this.plugin).open(player);
            return;
        }
        new PartyStartConfirmMenu(this.plugin, this.mode).open(player);
    }
}

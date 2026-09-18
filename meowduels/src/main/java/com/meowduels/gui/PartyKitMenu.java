package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.model.Kit;
import com.meowduels.model.Party;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Which kit the party fights with.
 *
 * <p>Framed, centred, and sized to the number of kits. It used to start at slot
 * 0 and run edge to edge, which for four kits meant four icons jammed into the
 * top-left corner of a window with no border and no way back out.
 *
 * <p>Rows hold seven with a margin either side, which reads as a grid rather
 * than a strip - until there are more kits than that can show, at which point
 * the margin is worth less than the space and the rows widen to nine. Every kit
 * stays reachable either way; nothing is hidden behind a page.
 *
 * <p>The kit currently chosen glows and says so. A picker that does not show
 * what is already picked makes you close it to find out.
 */
public class PartyKitMenu
extends Menu {
    private static final int NARROW_PER_ROW = 7;
    private static final int WIDE_PER_ROW = 9;
    private static final int MAX_ROWS = 6;

    private static final String VALUE = "<#E6E8EB>";
    private static final String LABEL = "<#8E959D>";
    private static final String HINT = "<dark_gray>\u25b8 <#8E959D>";

    private final MeowDuels plugin;
    private int page = 0;

    public PartyKitMenu(MeowDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public void build() {
        // Filled in open(), which is the only place that knows whose party it
        // is and therefore which kit to mark as chosen.
    }

    @Override
    public void open(Player player) {
        Party party = this.plugin.getPartyManager().partyOf(player.getUniqueId());
        if (party == null || !party.isLeader(player.getUniqueId())) {
            player.sendMessage(Text.prefixed("&cOnly the party leader can pick the kit."));
            return;
        }
        List<Kit> kits = new ArrayList<Kit>(this.plugin.getKitManager().all());
        int maxContent = MAX_ROWS - 1;
        int perRow = kits.size() > NARROW_PER_ROW * maxContent ? WIDE_PER_ROW : NARROW_PER_ROW;
        int indent = (WIDE_PER_ROW - perRow) / 2;
        int rows = Math.min(MAX_ROWS, Math.max(2, (kits.size() + perRow - 1) / perRow + 1));
        int contentRows = rows - 1;
        int capacity = contentRows * perRow;
        // Pages, rather than showing the first N and quietly losing the rest -
        // a kit you cannot see is a kit you cannot play, which is the whole
        // reason this menu was wrong before.
        int pages = Math.max(1, (kits.size() + capacity - 1) / capacity);
        if (this.page >= pages) {
            this.page = pages - 1;
        }
        if (this.page < 0) {
            this.page = 0;
        }
        int from = this.page * capacity;
        int shown = Math.min(capacity, kits.size() - from);

        this.createRaw(rows, "<dark_gray>\u258f <gradient:#FF8AD0:#B04BD6>{TITLE}</gradient>"
                + (pages > 1 ? " <dark_gray>" + (this.page + 1) + "/" + pages : ""));
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).rawName(" ").build();
        for (int i = 0; i < rows * WIDE_PER_ROW; ++i) {
            this.inventory.setItem(i, filler);
        }

        String chosen = party.getKit();
        for (int i = 0; i < shown; ++i) {
            int row = i / perRow;
            int inRow = i % perRow;
            // The last row is usually short; centring it stops the grid from
            // trailing off to the left.
            int thisRow = Math.min(perRow, shown - row * perRow);
            int pad = (perRow - thisRow) / 2;
            this.inventory.setItem(row * WIDE_PER_ROW + indent + pad + inRow,
                    this.icon(kits.get(from + i), chosen));
        }

        if (kits.isEmpty()) {
            this.inventory.setItem(WIDE_PER_ROW / 2, Items.of(Material.BARRIER)
                    .rawName("<#FF8A93>{NOKITS}")
                    .rawLore("", LABEL + "{NOKITSWHY}")
                    .hideTooltip().build());
        }
        int bar = contentRows * WIDE_PER_ROW;
        this.inventory.setItem(bar + WIDE_PER_ROW / 2, Items.of(Material.ARROW)
                .rawName(VALUE + "{BACK}")
                .hideTooltip().tag(this.plugin.keyButton(), "party-back").build());
        if (this.page > 0) {
            this.inventory.setItem(bar + 2, Items.of(Material.ARROW)
                    .rawName(VALUE + "\u1d18\u0280\u1d07\u1d20\u026a\u1d0f\u1d1c\ua731 \u1d18\u1d00\u0262\u1d07")
                    .hideTooltip().tag(this.plugin.keyButton(), "kit-prev").build());
        }
        if (this.page < pages - 1) {
            this.inventory.setItem(bar + 6, Items.of(Material.ARROW)
                    .rawName(VALUE + "\u0274\u1d07x\u1d1b \u1d18\u1d00\u0262\u1d07")
                    .hideTooltip().tag(this.plugin.keyButton(), "kit-next").build());
        }
        player.openInventory(this.inventory);
    }

    private ItemStack icon(Kit kit, String chosen) {
        boolean picked = kit.getName().equalsIgnoreCase(chosen);
        String name = kit.getDisplayName() == null || kit.getDisplayName().isEmpty()
                ? VALUE + kit.getName() : kit.getDisplayName();
        return Items.of(kit.getIcon())
                .rawName(name)
                .rawLore("", picked ? "<#7CFF6B>\u25cf \u1d04\u029c\u1d0f\ua731\u1d07\u0274" : HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d18\u026a\u1d04\u1d0b")
                .glow(picked)
                .hideTooltip()
                .tag(this.plugin.keyKit(), kit.getName()).build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String button = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if ("party-back".equals(button)) {
            new PartyMenu(this.plugin).openFor(player);
            return;
        }
        if ("kit-next".equals(button) || "kit-prev".equals(button)) {
            this.page += "kit-next".equals(button) ? 1 : -1;
            this.open(player);
            return;
        }
        String kit = Items.readTag(event.getCurrentItem(), this.plugin.keyKit());
        if (kit == null) {
            return;
        }
        Party party = this.plugin.getPartyManager().partyOf(player.getUniqueId());
        if (party == null || !party.isLeader(player.getUniqueId())) {
            player.closeInventory();
            return;
        }
        party.setKit(kit);
        this.plugin.getPartyManager().broadcast(party, "&7Party kit set to &f" + kit + "&7.");
        new PartyMenu(this.plugin).openFor(player);
    }
}

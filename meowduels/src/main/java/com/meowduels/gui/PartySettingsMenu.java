package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.model.Party;
import com.meowduels.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Party settings. Two of them, and both are real decisions rather than filler:
 * whether the party is joinable without an invite, and whether party members can
 * hurt each other outside a match.
 *
 * <p>Neither applies during a match - a free-for-all where you cannot hit each
 * other would have no way to end.
 */
public class PartySettingsMenu
extends Menu {
    private static final int ROWS = 3;
    private static final int SLOT_OPEN = 11;
    private static final int SLOT_FF = 13;
    private static final int SLOT_BACK = 15;

    private static final String LABEL = "<#8E959D>";
    private static final String MUTED = "<#6B7079>";
    private static final String HINT = "<dark_gray>\u25b8 <#8E959D>";

    private final MeowDuels plugin;

    public PartySettingsMenu(MeowDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public void build() {
        this.createRaw(ROWS, Style.title("#FF8AD0", "#B04BD6", "\u1d18\u1d00\u0280\u1d1b\u028f \ua731\u1d07\u1d1b\u1d1b\u026a\u0274\u0262\ua731"));
        Style.frame(this.inventory, ROWS);
    }

    @Override
    public void open(Player player) {
        this.build();
        Party party = this.plugin.getPartyManager().partyOf(player.getUniqueId());
        if (party == null) {
            return;
        }
        // Not just a click guard: the window itself is the leader's. Opening it
        // read-only would show a member two toggles that look like theirs.
        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage(com.meowduels.util.Text.prefixed("&cOnly the party leader can change party settings."));
            return;
        }
        this.inventory.setItem(SLOT_OPEN, this.toggle(party.isOpenToAll(),
                Material.ENDER_EYE, "\u1d0f\u1d18\u1d07\u0274 \u1d1b\u1d0f \u1d00\u029f\u029f", "\u1d00\u0274\u028f\u1d0f\u0274\u1d07 \u1d04\u1d00\u0274 \u1d0a\u1d0f\u026a\u0274 \u1d21\u026a\u1d1b\u029c\u1d0f\u1d1c\u1d1b \u1d00\u0274 \u026a\u0274\u1d20\u026a\u1d1b\u1d07", "party-set-open"));
        this.inventory.setItem(SLOT_FF, this.toggle(party.isFriendlyFire(),
                Material.IRON_SWORD, "\ua730\u0280\u026a\u1d07\u0274\u1d05\u029f\u028f \ua730\u026a\u0280\u1d07", "\u1d0d\u1d07\u1d0d\u0299\u1d07\u0280\ua731 \u1d04\u1d00\u0274 \u029c\u1d1c\u0280\u1d1b \u1d07\u1d00\u1d04\u029c \u1d0f\u1d1b\u029c\u1d07\u0280 \u1d0f\u1d1c\u1d1b\ua731\u026a\u1d05\u1d07 \u1d00 \u1d0d\u1d00\u1d1b\u1d04\u029c", "party-set-ff"));
        this.inventory.setItem(SLOT_BACK, Style.back(this.plugin.keyButton(), "party-back"));
        player.openInventory(this.inventory);
    }

    /** Same shape as the player settings menu, so a toggle is a toggle. */
    private ItemStack toggle(boolean on, Material icon, String name, String why, String tag) {
        return Items.of(icon)
                .rawName(Style.VALUE + name)
                .rawLore("", Style.LABEL + why, "",
                         Style.state(on, "\u1d0f\u0274", "\u1d0f\ua730\ua730"),
                         "", Style.HINT + "\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d1b\u1d0f\u0262\u0262\u029f\u1d07")
                .glow(on).hideTooltip().tag(this.plugin.keyButton(), tag).build();
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
        if ("party-set-open".equals(id)) {
            party.setOpenToAll(!party.isOpenToAll());
            this.open(player);
        } else if ("party-set-ff".equals(id)) {
            party.setFriendlyFire(!party.isFriendlyFire());
            this.open(player);
        } else if ("party-back".equals(id)) {
            new PartyMenu(this.plugin).openFor(player);
        }
    }
}

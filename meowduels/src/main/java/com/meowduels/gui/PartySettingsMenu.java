package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.managers.GuiConfigManager.Face;
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
    private static final int SLOT_BACK = 18;

    private static final String LABEL = "<#8E959D>";
    private static final String MUTED = "<#6B7079>";
    private static final String HINT = "<dark_gray>\u25b8 <#8E959D>";

    private final MeowDuels plugin;

    public PartySettingsMenu(MeowDuels plugin) {
        this.plugin = plugin;
    }

    /** The name of this screen's file: {@code gui/partysettings.yml}. */
    private static final String MENU = "partysettings";

    /** The tail every toggle here shares - state, a gap, then the hint. */
    private static final String ON_STATE = "{good}{tick}\u1d0f\u0274";
    private static final String OFF_STATE = "{bad}{cross}\u1d0f\ua730\ua730";
    private static final String TOGGLE_HINT = "{hint}\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d1b\u1d0f\u0262\u0262\u029f\u1d07";

    @Override
    public void build() {
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        this.createRaw(rows, gui.title(MENU,
                "<dark_gray>\u258f <gradient:#FF8AD0:#B04BD6>\u1d18\u1d00\u0280\u1d1b\u028f \ua731\u1d07\u1d1b\u1d1b\u026a\u0274\u0262\ua731</gradient>"));
        gui.frame(MENU, this.inventory, rows);
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
            player.sendMessage(this.plugin.messages().get("notice.party.only-party-leader-change"));
            return;
        }
        GuiConfigManager gui = this.plugin.gui();
        int rows = this.inventory.getSize() / 9;
        int size = this.inventory.getSize();
        this.inventory.setItem(Math.min(size - 1, gui.slot(MENU, "open", SLOT_OPEN, rows)),
                gui.toggle(MENU, "open", party.isOpenToAll(), null,
                        Face.of(Material.ENDER_EYE,
                                "{value}\u1d0f\u1d18\u1d07\u0274 \u1d1b\u1d0f \u1d00\u029f\u029f",
                                "",
                                "{label}\u1d00\u0274\u028f\u1d0f\u0274\u1d07 \u1d04\u1d00\u0274 \u1d0a\u1d0f\u026a\u0274 \u1d21\u026a\u1d1b\u029c\u1d0f\u1d1c\u1d1b \u1d00\u0274 \u026a\u0274\u1d20\u026a\u1d1b\u1d07",
                                "", ON_STATE, "", TOGGLE_HINT).glow(),
                        Face.of(Material.ENDER_EYE,
                                "{value}\u1d0f\u1d18\u1d07\u0274 \u1d1b\u1d0f \u1d00\u029f\u029f",
                                "",
                                "{label}\u1d00\u0274\u028f\u1d0f\u0274\u1d07 \u1d04\u1d00\u0274 \u1d0a\u1d0f\u026a\u0274 \u1d21\u026a\u1d1b\u029c\u1d0f\u1d1c\u1d1b \u1d00\u0274 \u026a\u0274\u1d20\u026a\u1d1b\u1d07",
                                "", OFF_STATE, "", TOGGLE_HINT))
                        .tag(this.plugin.keyButton(), "party-set-open").build());
        this.inventory.setItem(Math.min(size - 1, gui.slot(MENU, "friendly-fire", SLOT_FF, rows)),
                gui.toggle(MENU, "friendly-fire", party.isFriendlyFire(), null,
                        Face.of(Material.IRON_SWORD,
                                "{value}\ua730\u0280\u026a\u1d07\u0274\u1d05\u029f\u028f \ua730\u026a\u0280\u1d07",
                                "",
                                "{label}\u1d0d\u1d07\u1d0d\u0299\u1d07\u0280\ua731 \u1d04\u1d00\u0274 \u029c\u1d1c\u0280\u1d1b \u1d07\u1d00\u1d04\u029c \u1d0f\u1d1b\u029c\u1d07\u0280 \u1d0f\u1d1c\u1d1b\ua731\u026a\u1d05\u1d07 \u1d00 \u1d0d\u1d00\u1d1b\u1d04\u029c",
                                "", ON_STATE, "", TOGGLE_HINT).glow(),
                        Face.of(Material.IRON_SWORD,
                                "{value}\ua730\u0280\u026a\u1d07\u0274\u1d05\u029f\u028f \ua730\u026a\u0280\u1d07",
                                "",
                                "{label}\u1d0d\u1d07\u1d0d\u0299\u1d07\u0280\ua731 \u1d04\u1d00\u0274 \u029c\u1d1c\u0280\u1d1b \u1d07\u1d00\u1d04\u029c \u1d0f\u1d1b\u029c\u1d07\u0280 \u1d0f\u1d1c\u1d1b\ua731\u026a\u1d05\u1d07 \u1d00 \u1d0d\u1d00\u1d1b\u1d04\u029c",
                                "", OFF_STATE, "", TOGGLE_HINT))
                        .tag(this.plugin.keyButton(), "party-set-ff").build());
        this.inventory.setItem(Math.min(size - 1, gui.slot(MENU, "back", SLOT_BACK, rows)),
                gui.back(MENU, "back").tag(this.plugin.keyButton(), "party-back").build());
        player.openInventory(this.inventory);
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

package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.model.Kit;
import com.meowduels.model.Party;
import com.meowduels.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Which kit the party fights with.
 *
 * <p>Only kits an arena can actually host are offered. A party match needs an
 * arena with an event spawn, so a kit no such arena supports would let the
 * leader pick something that can only fail at the start button - which is the
 * worst place to find out.
 */
public class PartyKitMenu
extends Menu {
    private final MeowDuels plugin;

    public PartyKitMenu(MeowDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public void open(Player player) {
        Party party = this.plugin.getPartyManager().partyOf(player.getUniqueId());
        if (party == null || !party.isLeader(player.getUniqueId())) {
            player.sendMessage(com.meowduels.util.Text.prefixed("&cOnly the party leader can pick the kit."));
            return;
        }
        super.open(player);
    }

    @Override
    public void build() {
        // Every kit, unfiltered. This used to hide any kit no "event-ready"
        // arena supported, which on a server where few arenas have an FFA spawn
        // set meant most kits simply were not in the menu - with nothing to say
        // why. A party match can use any arena now, so the filter was hiding
        // kits that would have worked.
        java.util.List<Kit> kits = new java.util.ArrayList<Kit>(this.plugin.getKitManager().all());
        int rows = Math.max(1, Math.min(6, (kits.size() + 8) / 9));
        this.createRaw(rows, "<dark_gray>\u258f <gradient:#FF8AD0:#B04BD6>\u1d18\u1d00\u0280\u1d1b\u028f \u1d0b\u026a\u1d1b</gradient>");
        int slot = 0;
        for (Kit kit : kits) {
            if (slot >= rows * 9) {
                break;
            }
            this.inventory.setItem(slot++, Items.of(kit.getIcon())
                    .rawName(kit.getDisplayName() == null || kit.getDisplayName().isEmpty()
                             ? "<#E6E8EB>" + kit.getName() : kit.getDisplayName())
                    .rawLore("", "<dark_gray>\u25b8 <#8E959D>\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d18\u026a\u1d04\u1d0b")
                    .hideTooltip().tag(this.plugin.keyKit(), kit.getName()).build());
        }
        if (kits.isEmpty()) {
            this.inventory.setItem(0, Items.of(Material.BARRIER)
                    .rawName("<#FF8A93>\u0274\u1d0f \u1d1c\ua731\u1d00\u0299\u029f\u1d07 \u1d0b\u026a\u1d1b\ua731")
                    .rawLore("", "<#8E959D>\u0274\u1d0f \u1d00\u0280\u1d07\u0274\u1d00 \u1d21\u026a\u1d1b\u029c \u1d00\u0274 \u1d07\u1d20\u1d07\u0274\u1d1b \ua731\u1d18\u1d00\u1d21\u0274 \ua731\u1d1c\u1d18\u1d18\u1d0f\u0280\u1d1b\ua731 \u1d00 \u1d0b\u026a\u1d1b")
                    .hideTooltip().build());
        }
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
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

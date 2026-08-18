package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.gui.Menu;
import com.vduels.model.Party;
import com.vduels.util.Items;
import com.vduels.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class PartyMatchSettingsMenu
extends Menu {
    private final VDuels plugin;
    private final Party party;
    private final String confirmLabel;
    private final Runnable onConfirm;

    public PartyMatchSettingsMenu(VDuels plugin, Party party, String confirmLabel, Runnable onConfirm) {
        this.plugin = plugin;
        this.party = party;
        this.confirmLabel = confirmLabel;
        this.onConfirm = onConfirm;
    }

    public void reopen(Player player) {
        this.build();
        player.openInventory(this.inventory);
    }

    @Override
    public void build() {
        this.create(4, "&7PARTY &7\u2192 &7SETTINGS");
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; ++i) {
            this.inventory.setItem(i, filler);
        }
        this.inventory.setItem(10, this.healthIndicatorItem());
        this.inventory.setItem(12, this.allowDropsItem());
        this.inventory.setItem(14, this.roundsItem());
        this.inventory.setItem(31, this.confirmItem());
    }

    private ItemStack healthIndicatorItem() {
        boolean on = this.party.isHealthIndicator();
        return Items.of(Material.RED_DYE).name("&c&lHEALTH INDICATOR").lore("&7Shows health above players", "&7during the duel.", "", "&7Current: " + (on ? "&aENABLED" : "&cDISABLED"), "", "&aClick to toggle").glow(on).hideTooltip().tag(this.plugin.keyButton(), "party-toggle-health").build();
    }

    private ItemStack allowDropsItem() {
        boolean on = this.party.isAllowDrops();
        return Items.of(Material.HOPPER).name("&6&lALLOW DROPS").lore("&7Whether items can drop", "&7during the match.", "", "&7Current: " + (on ? "&aENABLED" : "&cDISABLED"), "", "&aClick to toggle").glow(on).hideTooltip().tag(this.plugin.keyButton(), "party-toggle-drops").build();
    }

    private ItemStack roundsItem() {
        return Items.of(Material.CLOCK).name("&e&lROUNDS").lore("&7Currently selected: &f" + this.party.getRounds() + " rounds", "", "&aLeft click &7to +1 round", "&cRight click &7to -1 round").hideTooltip().tag(this.plugin.keyButton(), "party-rounds").build();
    }

    private ItemStack confirmItem() {
        boolean ready = this.party.getSelectedKit() != null;
        return Items.of(ready ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE).name((String)(ready ? "&a&l" + this.confirmLabel : "&fSelect a kit first")).lore(ready ? Items.lines("&7Kit: &f" + this.party.getSelectedKit(), "", "&aClick to confirm!") : Items.lines("&7Go back and pick a kit.")).hideTooltip().tag(this.plugin.keyButton(), "party-confirm").build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String tag = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if (tag == null) {
            return;
        }
        switch (tag) {
            case "party-toggle-health": {
                this.party.setHealthIndicator(!this.party.isHealthIndicator());
                this.reopen(player);
                break;
            }
            case "party-toggle-drops": {
                this.party.setAllowDrops(!this.party.isAllowDrops());
                this.reopen(player);
                break;
            }
            case "party-rounds": {
                int current = this.party.getRounds();
                this.party.setRounds(event.isRightClick() ? current - 1 : current + 1);
                this.reopen(player);
                break;
            }
            case "party-confirm": {
                if (this.party.getSelectedKit() == null) {
                    return;
                }
                if (!this.party.isLeader(player.getUniqueId())) {
                    player.sendMessage(Text.color("&cOnly the party leader can do that."));
                    return;
                }
                player.closeInventory();
                this.onConfirm.run();
            }
        }
    }
}


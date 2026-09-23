/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.ItemStack
 */
package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.model.Arena;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class EventMapMenu
extends Menu {
    private final MeowDuels plugin;
    private final Player hoster;
    private final String kit;
    private final int minutes;
    private final int slots;

    public EventMapMenu(MeowDuels plugin, Player hoster, String kit, int minutes, int slots) {
        this.plugin = plugin;
        this.hoster = hoster;
        this.kit = kit;
        this.minutes = minutes;
        this.slots = slots;
    }

    @Override
    public void build() {
        this.create(3, "&d&lEvent \u2192 Pick a map");
        ItemStack filler = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; ++i) {
            this.inventory.setItem(i, filler);
        }
        int slot = 0;
        for (Arena arena : this.plugin.getArenaManager().all()) {
            if (slot >= 27) break;
            if (!arena.isEventReady() || !arena.isEnabled()) continue;
            boolean busy = this.plugin.getDuelManager().isArenaInUse(arena.getName());
            Material icon = busy ? Material.BARRIER : Material.FILLED_MAP;
            String status = busy ? "&cIn use right now" : "&aClick to host here";
            this.inventory.setItem(slot++, Items.of(icon).name("&e" + arena.getName()).lore("&7World: &f" + arena.getWorldName(), "&7Border: &f" + this.borderLabel(arena), "", status).tag(this.plugin.keyButton(), arena.getName()).build());
        }
        if (slot == 0) {
            this.inventory.setItem(13, Items.of(Material.BARRIER).name("&cNo event maps").lore("&7No arena has an event spawn set.", "&7Open an arena in &e/arena&7 and set its", "&7Event Spawn first.").build());
        }
    }

    private String borderLabel(Arena arena) {
        String start = arena.hasBorderRegion() ? String.valueOf((int)arena.getBorderStartSize()) : "auto";
        return start + " \u2192 " + arena.getEventBorderEnd() + " &7(every " + arena.getEventBorderInterval() + "s)";
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String arenaName = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if (arenaName == null) {
            return;
        }
        Arena arena = this.plugin.getArenaManager().get(arenaName);
        if (arena == null) {
            return;
        }
        if (this.plugin.getDuelManager().isArenaInUse(arena.getName())) {
            player.sendMessage(Text.prefixed("&cThat map is in use right now - pick another."));
            return;
        }
        player.closeInventory();
        this.plugin.getEventManager().host(this.hoster, this.kit, this.minutes, this.slots, arena);
    }
}


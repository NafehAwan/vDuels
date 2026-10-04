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
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.model.Arena;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import java.util.HashMap;
import java.util.Map;
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

    /** The name of this screen's file: {@code gui/eventmap.yml}. */
    private static final String MENU = "eventmap";

    private static final int ROWS = 3;

    @Override
    public void build() {
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        this.createRaw(rows, gui.title(MENU, "&d&l\u1d07\u1d20\u1d07\u0274\u1d1b \u2192 \u1d18\u026a\u1d04\u1d0b \u1d00 \u1d0d\u1d00\u1d18"));
        gui.frame(MENU, this.inventory, rows);
        int size = rows * 9;
        int slot = 0;
        for (Arena arena : this.plugin.getArenaManager().all()) {
            if (slot >= size) break;
            if (!arena.isEventReady() || !arena.isEnabled()) continue;
            Map<String, String> tokens = new HashMap<String, String>();
            tokens.put("arena", arena.getName());
            tokens.put("world", arena.getWorldName());
            tokens.put("border", this.borderLabel(arena));
            // Free and busy are two items in the file rather than one with two
            // faces, so a server can give an occupied map its own icon and
            // wording without touching the other.
            Items item = this.plugin.getDuelManager().isArenaInUse(arena.getName())
                    ? gui.item(MENU, "arena-busy", tokens, Material.BARRIER,
                               "&e{arena}",
                               "&7\u1d21\u1d0f\u0280\u029f\u1d05: &f{world}",
                               "&7\u0299\u1d0f\u0280\u1d05\u1d07\u0280: &f{border}",
                               "",
                               "&c\u026a\u0274 \u1d1c\ua731\u1d07 \u0280\u026a\u0262\u029c\u1d1b \u0274\u1d0f\u1d21")
                    : gui.item(MENU, "arena-free", tokens, Material.FILLED_MAP,
                               "&e{arena}",
                               "&7\u1d21\u1d0f\u0280\u029f\u1d05: &f{world}",
                               "&7\u0299\u1d0f\u0280\u1d05\u1d07\u0280: &f{border}",
                               "",
                               "&a\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u029c\u1d0f\ua731\u1d1b \u029c\u1d07\u0280\u1d07");
            this.inventory.setItem(slot++, item.tag(this.plugin.keyButton(), arena.getName()).build());
        }
        if (slot == 0) {
            this.inventory.setItem(Math.min(size - 1, gui.slot(MENU, "empty", 13, rows)),
                    gui.item(MENU, "empty", Material.BARRIER,
                             "&c\u0274\u1d0f \u1d07\u1d20\u1d07\u0274\u1d1b \u1d0d\u1d00\u1d18\ua731",
                             "&7\u0274\u1d0f \u1d00\u0280\u1d07\u0274\u1d00 \u029c\u1d00\ua731 \u1d00\u0274 \u1d07\u1d20\u1d07\u0274\u1d1b \ua731\u1d18\u1d00\u1d21\u0274 \ua731\u1d07\u1d1b.",
                             "&7\u1d0f\u1d18\u1d07\u0274 \u1d00\u0274 \u1d00\u0280\u1d07\u0274\u1d00 \u026a\u0274 &e/\u1d00\u0280\u1d07\u0274\u1d00&7 \u1d00\u0274\u1d05 \ua731\u1d07\u1d1b \u026a\u1d1b\ua731",
                             "&7\u1d07\u1d20\u1d07\u0274\u1d1b \ua731\u1d18\u1d00\u1d21\u0274 \ua730\u026a\u0280\ua731\u1d1b.").build());
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


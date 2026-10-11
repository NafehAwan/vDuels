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
import com.meowduels.gui.ArenaMenu;
import com.meowduels.gui.Menu;
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.managers.GuiConfigManager.Face;
import com.meowduels.model.Arena;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class KitSelectMenu
extends Menu {
    private final MeowDuels plugin;
    private final Arena arena;
    private List<Kit> kits;

    public KitSelectMenu(MeowDuels plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    /** The name of this screen's file: {@code gui/kitselect.yml}. */
    private static final String MENU = "kitselect";

    @Override
    public void build() {
        GuiConfigManager gui = this.plugin.gui();
        Map<String, String> arenaToken = new HashMap<String, String>();
        arenaToken.put("arena", this.arena.getName());
        // Six rows and the slot numbers below are not editable here: a click
        // on this screen is read as an index into the kit list, so a kit that
        // moved would toggle a different kit.
        this.createRaw(6, gui.title(MENU,
                "&8ᴄᴏᴍᴘᴀᴛɪʙʟᴇ ᴋɪᴛꜱ: &d{arena}", arenaToken));
        this.kits = this.plugin.getKitManager().all();
        if (this.kits.isEmpty()) {
            this.inventory.setItem(22,
                    gui.face(MENU, "empty", null,
                             Face.of(Material.BARRIER,
                                     "&cɴᴏ ᴋɪᴛꜱ ᴄʀᴇᴀᴛᴇᴅ ʏᴇᴛ",
                                     "&7ᴜꜱᴇ &e/ᴋɪᴛᴄʀᴇᴀᴛᴇ <ɴᴀᴍᴇ>&7 ꜰɪʀꜱᴛ.")).build());
        }
        for (int i = 0; i < this.kits.size() && i < 45; ++i) {
            Kit kit = this.kits.get(i);
            Map<String, String> tokens = new HashMap<String, String>();
            tokens.put("kit", kit.getName());
            this.inventory.setItem(i,
                    gui.toggle(MENU, "kit", this.arena.supportsKitExplicit(kit.getName()), tokens,
                            Face.of(gui.material(MENU, "kit", kit.getIcon()), "&a{kit}",
                                    "",
                                    "&aᴇɴᴀʙʟᴇᴅ &8(ᴄʟɪᴄᴋ ᴛᴏ ᴅɪꜱᴀʙʟᴇ)").glow(),
                            Face.of(gui.material(MENU, "kit", kit.getIcon()), "&7{kit}",
                                    "",
                                    "&cᴅɪꜱᴀʙʟᴇᴅ &8(ᴄʟɪᴄᴋ ᴛᴏ ᴇɴᴀʙʟᴇ)")).build());
        }
        // Only the bottom bar is floored. The unused grid slots stay empty on
        // purpose - paning them would turn a half-full grid into a wall.
        ItemStack filler = gui.pane(MENU);
        for (int i = 45; i < 54; ++i) {
            this.inventory.setItem(i, filler);
        }
        this.inventory.setItem(49,
                gui.face(MENU, "back", null,
                         Face.of(Material.ARROW,
                                 "&eʙᴀᴄᴋ",
                                 "&7ʀᴇᴛᴜʀɴ ᴛᴏ ᴛʜᴇ ᴀʀᴇɴᴀ ᴍᴇɴᴜ.")).build());
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 49) {
            new ArenaMenu(this.plugin, this.arena).open(player);
            return;
        }
        if (slot < 0 || slot >= 45 || this.kits == null || slot >= this.kits.size()) {
            return;
        }
        Kit kit = this.kits.get(slot);
        if (this.arena.supportsKitExplicit(kit.getName())) {
            this.arena.getKits().removeIf(k -> k.equalsIgnoreCase(kit.getName()));
        } else {
            this.arena.getKits().add(kit.getName());
        }
        this.plugin.getArenaManager().save();
        this.build();
        player.openInventory(this.inventory);
    }
}


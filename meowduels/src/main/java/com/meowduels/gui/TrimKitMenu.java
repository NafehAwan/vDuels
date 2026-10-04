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
import com.meowduels.gui.TrimMenu;
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import java.util.HashMap;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class TrimKitMenu
extends Menu {
    private final MeowDuels plugin;

    public TrimKitMenu(MeowDuels plugin) {
        this.plugin = plugin;
    }

    /** The name of this screen's file: {@code gui/trimkit.yml}. */
    private static final String MENU = "trimkit";

    private static final int ROWS = 6;

    @Override
    public void build() {
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        this.createRaw(rows, gui.title(MENU, "&d&lᴛʀɪᴍꜱ &7- ᴘɪᴄᴋ ᴀ ᴋɪᴛ"));
        gui.frame(MENU, this.inventory, rows);
        int size = rows * 9;
        int slot = 0;
        for (Kit kit : this.plugin.getKitManager().all()) {
            if (slot >= size) break;
            HashMap<String, String> tokens = new HashMap<String, String>();
            tokens.put("kit", kit.getName());
            Items icon = Items.of(gui.material(MENU, "kit", kit.getIcon()),
                                  gui.amount(MENU, "kit", 1))
                    .tag(this.plugin.keyKit(), kit.getName());
            // As on the other kit lists: the file can title a row, but with
            // no name: set the kit keeps deciding, so a display name stays a
            // parsed component rather than becoming a section string.
            String override = gui.name(MENU, "kit", tokens);
            if (override != null) {
                icon.rawName(override);
            } else if (kit.getDisplayName() != null && !kit.getDisplayName().isEmpty()) {
                icon.miniName(kit.getDisplayName());
            } else {
                icon.rawName("&e" + Text.smallCaps(kit.getName()));
            }
            icon.rawLore(gui.lore(MENU, "kit", tokens,
                                  "&7ᴄʟɪᴄᴋ ᴛᴏ ᴇᴅɪᴛ ᴛʜɪꜱ ᴋɪᴛ'ꜱ ᴀʀᴍᴏʀ ᴛʀɪᴍꜱ.").toArray(new String[0]));
            this.inventory.setItem(slot++, icon.build());
        }
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String kitName = Items.readTag(event.getCurrentItem(), this.plugin.keyKit());
        if (kitName != null && this.plugin.getKitManager().exists(kitName)) {
            new TrimMenu(this.plugin, kitName, player.getUniqueId()).open(player);
        }
    }
}


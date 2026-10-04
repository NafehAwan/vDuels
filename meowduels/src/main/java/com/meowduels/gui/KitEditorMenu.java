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
import com.meowduels.gui.KitItemsEditMenu;
import com.meowduels.gui.Menu;
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class KitEditorMenu
extends Menu {
    private static final int[] SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private final MeowDuels plugin;
    /** Personal: each kit opens the player's OWN layout. Global: admins editing
     *  the kit for everyone. */
    private final boolean personal;

    public KitEditorMenu(MeowDuels plugin) {
        this(plugin, false);
    }

    public KitEditorMenu(MeowDuels plugin, boolean personal) {
        this.personal = personal;
        this.plugin = plugin;
    }

    /** The name of this screen's file: {@code gui/kiteditor.yml}. */
    private static final String MENU = "kiteditor";

    private static final int ROWS = 6;

    @Override
    public void build() {
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        this.createRaw(rows, this.personal
                ? gui.titleOf(MENU, "personal", "&d&lʏᴏᴜʀ ᴋɪᴛꜱ &7- ᴘɪᴄᴋ ᴀ ɢᴀᴍᴇᴍᴏᴅᴇ", null)
                : gui.titleOf(MENU, "global", "&d&lᴋɪᴛ ᴇᴅɪᴛᴏʀ &7- ᴘɪᴄᴋ ᴀ ɢᴀᴍᴇᴍᴏᴅᴇ", null));
        gui.frame(MENU, this.inventory, rows);
        int size = rows * 9;
        List<Kit> kits = this.plugin.getKitManager().all();
        if (kits.isEmpty()) {
            this.inventory.setItem(Math.min(size - 1, gui.slot(MENU, "empty", 22, rows)),
                    gui.item(MENU, "empty", Material.BARRIER,
                             "&cɴᴏ ᴋɪᴛꜱ ʏᴇᴛ",
                             "&7ᴄʀᴇᴀᴛᴇ ᴏɴᴇ ᴡɪᴛʜ &e/ᴋɪᴛᴄʀᴇᴀᴛᴇ <ɴᴀᴍᴇ>&7.").build());
            return;
        }
        for (int i = 0; i < SLOTS.length && i < kits.size(); ++i) {
            if (SLOTS[i] >= size) break;
            this.inventory.setItem(SLOTS[i], this.icon(gui, kits.get(i)));
        }
    }

    /**
     * One kit's row, worded for whichever mode the window is in.
     *
     * <p>Two items rather than one with two faces: the personal and global
     * rows are read by different people - a player arranging their own
     * layout, an admin changing the kit for everyone - and a server will want
     * to word the warning on the second one without touching the first.
     */
    private ItemStack icon(GuiConfigManager gui, Kit kit) {
        HashMap<String, String> tokens = new HashMap<String, String>();
        tokens.put("kit", kit.getName());
        Items item = this.personal
                ? gui.item(MENU, "kit-personal", tokens, gui.material(MENU, "kit-personal", kit.getIcon()),
                           "&d{kit}",
                           "",
                           "&7ᴄʟɪᴄᴋ ᴛᴏ ᴀʀʀᴀɴɢᴇ ᴛʜɪꜱ ᴋɪᴛ",
                           "&7ᴛʜᴇ ᴡᴀʏ ʏᴏᴜ ᴡᴀɴᴛ ɪᴛ.")
                : gui.item(MENU, "kit-global", tokens, gui.material(MENU, "kit-global", kit.getIcon()),
                           "&d{kit}",
                           "",
                           "&7ᴄʟɪᴄᴋ ᴛᴏ ᴇᴅɪᴛ ᴛʜɪꜱ ɢᴀᴍᴇᴍᴏᴅᴇ'ꜱ ɪᴛᴇᴍꜱ,",
                           "&7ᴀʀᴍᴏʀ ᴀɴᴅ ᴏꜰꜰʜᴀɴᴅ ɪᴛᴇᴍ.",
                           "&cᴄʜᴀɴɢᴇꜱ ᴀᴘᴘʟʏ ᴛᴏ ᴇᴠᴇʀʏᴏɴᴇ.");
        item.hideTooltip().tag(this.plugin.keyKit(), kit.getName());
        // The file's name: wins; with none set, a kit with a display name keeps
        // it as a parsed component rather than becoming a section string.
        String override = this.personal
                ? gui.name(MENU, "kit-personal", tokens)
                : gui.name(MENU, "kit-global", tokens);
        if (override == null && kit.getDisplayName() != null && !kit.getDisplayName().isEmpty()) {
            item.miniName(kit.getDisplayName());
        }
        return item.build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String kitName = Items.readTag(event.getCurrentItem(), this.plugin.keyKit());
        if (kitName == null) {
            return;
        }
        Kit kit = this.plugin.getKitManager().get(kitName);
        if (kit != null) {
            new KitItemsEditMenu(this.plugin, kit, this.personal, player.getUniqueId()).open(player);
        }
    }
}


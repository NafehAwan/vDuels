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
import com.meowduels.managers.TabEditManager;
import com.meowduels.util.Items;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class TabConfigMenu
extends Menu {
    private final MeowDuels plugin;

    public TabConfigMenu(MeowDuels plugin) {
        this.plugin = plugin;
    }

    /** The name of this screen's file: {@code gui/tabconfig.yml}. */
    private static final String MENU = "tabconfig";

    private static final int ROWS = 3;
    private static final int SLOT_TITLE = 11;
    private static final int SLOT_DISCORD = 13;
    private static final int SLOT_STORE = 15;

    @Override
    public void build() {
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        // The labels are stored already small-capped. This screen used to run
        // them through Items.name(), which small-caps on the way out; the
        // config-driven path deliberately re-cases nothing, so the shipped
        // text is what name() would have produced.
        this.createRaw(rows, gui.title(MENU, "&7&lᴛᴀʙ ꜱᴇᴛᴛɪɴɢꜱ"));
        gui.frame(MENU, this.inventory, rows);
        this.put(rows, "title", gui.slot(MENU, "title", SLOT_TITLE, rows),
                 gui.item(MENU, "title", this.tokens(this.plugin.getTabTitle()), Material.BOOK,
                          "&eᴛɪᴛʟᴇ",
                          "&7ᴄᴜʀʀᴇɴᴛ:",
                          "&f{current}",
                          "",
                          "&aᴄʟɪᴄᴋ ᴛᴏ ᴄʜᴀɴɢᴇ &7(ᴍɪɴɪᴍᴇꜱꜱᴀɢᴇ / ɢʀᴀᴅɪᴇɴᴛꜱ)"));
        this.put(rows, "discord", gui.slot(MENU, "discord", SLOT_DISCORD, rows),
                 gui.item(MENU, "discord", this.tokens(this.plugin.getTabDiscord()), Material.PAPER,
                          "&dᴅɪꜱᴄᴏʀᴅ",
                          "&7ᴄᴜʀʀᴇɴᴛ: &f{current}",
                          "",
                          "&aᴄʟɪᴄᴋ ᴛᴏ ᴄʜᴀɴɢᴇ"));
        this.put(rows, "store", gui.slot(MENU, "store", SLOT_STORE, rows),
                 gui.item(MENU, "store", this.tokens(this.plugin.getTabStore()), Material.GOLDEN_APPLE,
                          "&6ꜱᴛᴏʀᴇ",
                          "&7ᴄᴜʀʀᴇɴᴛ: &f{current}",
                          "",
                          "&aᴄʟɪᴄᴋ ᴛᴏ ᴄʜᴀɴɢᴇ"));
    }

    /**
     * What the field is currently set to.
     *
     * <p>Goes in raw rather than small-capped. The old path put it through
     * the same filter as the label, which turned a tab title of
     * {@code <gold><bold>Name</bold></gold>} into
     * {@code <ɢᴏʟᴅ><ʙᴏʟᴅ>ɴᴀᴍᴇ...} - a preview of a MiniMessage string that
     * could not be read, let alone copied.
     */
    private Map<String, String> tokens(String current) {
        HashMap<String, String> tokens = new HashMap<String, String>();
        tokens.put("current", current == null ? "" : current);
        return tokens;
    }

    private void put(int rows, String id, int slot, Items item) {
        this.inventory.setItem(Math.max(0, Math.min(rows * 9 - 1, slot)),
                               item.tag(this.plugin.keyButton(), id).build());
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        TabEditManager.Field field;
        String button = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if (button == null) {
            return;
        }
        // The decompiler split this switch into a shadow variable that was never
        // read; each branch must assign the `field` the code below actually uses.
        switch (button) {
            case "title": {
                field = TabEditManager.Field.TITLE;
                break;
            }
            case "discord": {
                field = TabEditManager.Field.DISCORD;
                break;
            }
            case "store": {
                field = TabEditManager.Field.STORE;
                break;
            }
            default: {
                field = null;
            }
        }
        if (field != null) {
            player.closeInventory();
            this.plugin.getTabEditManager().begin(player, field);
        }
    }
}


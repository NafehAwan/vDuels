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
import com.meowduels.gui.DuelConfirmMenu;
import com.meowduels.gui.Menu;
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.managers.GuiConfigManager.Face;
import com.meowduels.managers.GuiLayoutManager;
import com.meowduels.model.Arena;
import com.meowduels.util.Items;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class MapSelectMenu
extends Menu {
    private final MeowDuels plugin;
    private final DuelConfirmMenu confirm;

    public MapSelectMenu(MeowDuels plugin, DuelConfirmMenu confirm) {
        this.plugin = plugin;
        this.confirm = confirm;
    }

    /** The name of this screen's file: {@code gui/mapselect.yml}. */
    private static final String MENU = GuiLayoutManager.MAP_SELECT;

    private static final int ROWS = 4;
    private static final int SLOT_RANDOM = 35;

    @Override
    public void build() {
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        int size = rows * 9;
        Map<String, String> titleTokens = new HashMap<String, String>();
        titleTokens.put("target", this.confirm.getTarget().getName());
        this.createRaw(rows, gui.title(MENU, "&8ᴅᴜᴇʟ ᴍᴀᴘ: &d{target}", titleTokens));
        String kit = this.confirm.getSelectedKit();
        if (this.plugin.getGuiLayoutManager().has(MENU)) {
            for (Map.Entry<Integer, ItemStack> e : this.plugin.getGuiLayoutManager().get(MENU).entrySet()) {
                if (e.getKey() >= size) continue;
                if ("random".equals(Items.readTag(e.getValue(), this.plugin.keyButton()))) {
                    this.inventory.setItem(e.getKey().intValue(), this.randomButton());
                    continue;
                }
                String arenaName = Items.readTag(e.getValue(), this.plugin.keyArena());
                if (arenaName != null) {
                    ItemStack icon = this.liveArenaIcon(arenaName, kit);
                    if (icon == null) continue;
                    this.inventory.setItem(e.getKey().intValue(), icon);
                    continue;
                }
                this.inventory.setItem(e.getKey().intValue(), e.getValue());
            }
        } else {
            int rowsNow = this.inventory.getSize() / 9;
            this.plugin.gui().frame(MENU, this.inventory, rowsNow);
            int slot = 0;
            int last = this.inventory.getSize() - 1;
            for (Arena arena : this.plugin.getArenaManager().all()) {
                if (slot >= last) break;
                if (!arena.isConfigured() || kit != null && !arena.supportsKit(kit)) continue;
                this.inventory.setItem(slot++, this.liveArenaIcon(arena.getName(), kit));
            }
            this.inventory.setItem(Math.min(last, this.plugin.gui().slot(MENU, "random", SLOT_RANDOM, rowsNow)),
                                   this.randomButton());
        }
    }

    private ItemStack randomButton() {
        GuiConfigManager gui = this.plugin.gui();
        boolean sel = this.confirm.getSelectedArena() == null;
        return gui.toggle(MENU, "random", sel, null,
                          Face.of(Material.ENDER_PEARL,
                                  "&aʀᴀɴᴅᴏᴍ",
                                  "",
                                  "&aꜱᴇʟᴇᴄᴛᴇᴅ").glow(),
                          Face.of(Material.ENDER_PEARL,
                                  "&eʀᴀɴᴅᴏᴍ",
                                  "",
                                  "&fᴘɪᴄᴋ ᴀɴʏ ꜰʀᴇᴇ ᴄᴏᴍᴘᴀᴛɪʙʟᴇ ᴀʀᴇɴᴀ"))
                .tag(this.plugin.keyButton(), "random").build();
    }

    /**
     * One map's icon, in whichever of its three states applies.
     *
     * <p>Selected, pickable and incompatible are three items in the file
     * rather than one with faces: a two-state toggle cannot say three things,
     * and a server that wants an incompatible map to look plainly unavailable
     * should be able to give it its own icon.
     */
    private ItemStack liveArenaIcon(String name, String kit) {
        Arena arena = this.plugin.getArenaManager().get(name);
        if (arena == null || !arena.isConfigured()) {
            return null;
        }
        GuiConfigManager gui = this.plugin.gui();
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("arena", arena.getName());
        Items item;
        if (name.equalsIgnoreCase(this.confirm.getSelectedArena())) {
            item = gui.face(MENU, "arena-selected", tokens,
                            Face.of(Material.FILLED_MAP,
                                    "&a{arena}",
                                    "",
                                    "&aꜱᴇʟᴇᴄᴛᴇᴅ").glow());
        } else if (kit == null || arena.supportsKit(kit)) {
            item = gui.face(MENU, "arena-free", tokens,
                            Face.of(Material.FILLED_MAP,
                                    "&e{arena}",
                                    "",
                                    "&fᴄʟɪᴄᴋ ᴛᴏ ᴘɪᴄᴋ ᴛʜɪꜱ ᴍᴀᴘ"));
        } else {
            item = gui.face(MENU, "arena-incompatible", tokens,
                            Face.of(Material.FILLED_MAP,
                                    "&c{arena}",
                                    "",
                                    "&cɪɴᴄᴏᴍᴘᴀᴛɪʙʟᴇ ᴡɪᴛʜ ᴛʜɪꜱ ᴋɪᴛ"));
        }
        return item.tag(this.plugin.keyArena(), arena.getName()).build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if ("random".equals(Items.readTag(clicked, this.plugin.keyButton()))) {
            this.confirm.setSelectedArena(null);
            this.confirm.reopen(player);
            return;
        }
        String arenaName = Items.readTag(clicked, this.plugin.keyArena());
        if (arenaName == null) {
            return;
        }
        Arena arena = this.plugin.getArenaManager().get(arenaName);
        String kit = this.confirm.getSelectedKit();
        if (arena == null || !arena.isConfigured()) {
            return;
        }
        if (kit != null && !arena.supportsKit(kit)) {
            player.sendMessage(this.plugin.messages().get("menu.arena-incompatible", new String[0]));
            return;
        }
        this.confirm.setSelectedArena(arena.getName());
        this.confirm.reopen(player);
    }
}


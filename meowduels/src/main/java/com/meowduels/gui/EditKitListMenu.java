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
import com.meowduels.gui.EditKitEffectsMenu;
import com.meowduels.gui.Menu;
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class EditKitListMenu
extends Menu {
    private static final int[] SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private final MeowDuels plugin;

    public EditKitListMenu(MeowDuels plugin) {
        this.plugin = plugin;
    }

    /** The name of this screen's file: {@code gui/editkitlist.yml}. */
    private static final String MENU = "editkitlist";

    private static final int ROWS = 6;

    @Override
    public void build() {
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        this.createRaw(rows, gui.title(MENU, "&7&lᴇᴅɪᴛ ᴋɪᴛ"));
        gui.frame(MENU, this.inventory, rows);
        List<Kit> kits = this.plugin.getKitManager().all();
        int size = rows * 9;
        for (int i = 0; i < SLOTS.length && i < kits.size(); ++i) {
            if (SLOTS[i] >= size) break;
            this.inventory.setItem(SLOTS[i], this.icon(gui, kits.get(i)));
        }
    }

    /**
     * One kit's row.
     *
     * <p>The title is still chosen here rather than in the file. A kit with a
     * display name is titled with {@code miniName}, which parses MiniMessage
     * into a real component; one without gets a small-capped {@code &e<id>}.
     * Those are two different renderers picked per kit, so a single template
     * would restyle every kit on the server. {@code name:} in the file
     * overrides both when an owner wants one.
     */
    private ItemStack icon(GuiConfigManager gui, Kit kit) {
        HashMap<String, String> tokens = new HashMap<String, String>();
        tokens.put("kit", kit.getName());
        tokens.put("effects", String.valueOf(kit.getStartEffects().size()));
        Items item = Items.of(gui.material(MENU, "kit", kit.getIcon()),
                              gui.amount(MENU, "kit", 1))
                .rawLore(gui.lore(MENU, "kit", tokens,
                                  "",
                                  "&7ꜱᴛᴀʀᴛ ᴇꜰꜰᴇᴄᴛꜱ: &f{effects}",
                                  "&aᴄʟɪᴄᴋ ᴛᴏ ᴇᴅɪᴛ.").toArray(new String[0]))
                .hideTooltip()
                .tag(this.plugin.keyKit(), kit.getName());
        String override = gui.name(MENU, "kit", tokens);
        if (override != null) {
            item.rawName(override);
        } else if (kit.getDisplayName() != null && !kit.getDisplayName().isEmpty()) {
            item.miniName(kit.getDisplayName());
        } else {
            item.rawName("&e" + Text.smallCaps(kit.getName()));
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
            new EditKitEffectsMenu(this.plugin, kit).open(player);
        }
    }
}


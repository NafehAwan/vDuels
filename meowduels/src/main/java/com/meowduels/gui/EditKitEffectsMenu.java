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
import com.meowduels.managers.GuiConfigManager.Face;
import com.meowduels.model.Kit;
import com.meowduels.model.StartEffect;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class EditKitEffectsMenu
extends Menu {
    private final MeowDuels plugin;
    private final Kit kit;

    public EditKitEffectsMenu(MeowDuels plugin, Kit kit) {
        this.plugin = plugin;
        this.kit = kit;
    }

    /** The name of this screen's file: {@code gui/editkiteffects.yml}. */
    private static final String MENU = "editkiteffects";

    private static final int ROWS = 3;
    private static final String ON = "&a\u1d07\u0274\u1d00\u0299\u029f\u1d07\u1d05";
    private static final String OFF = "&c\u1d05\u026a\ua731\u1d00\u0299\u029f\u1d07\u1d05";
    private static final String TOGGLE_HINT = "&7\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d1b\u1d0f\u0262\u0262\u029f\u1d07.";

    @Override
    public void build() {
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("kit", this.kit.getName());
        this.createRaw(rows, gui.title(MENU, "&7&l\u1d07\u1d05\u026a\u1d1b \u1d0b\u026a\u1d1b \u2192 &7{kit}", tokens));
        gui.frame(MENU, this.inventory, rows);
        // The four start effects. Each is one item with an on and an off face,
        // so a file can reword or recolour either state on its own.
        this.put(rows, gui.slot(MENU, "strength", 10, rows),
                 gui.toggle(MENU, "strength", this.kit.hasStartEffect(StartEffect.STRENGTH2_90), tokens,
                            Face.of(Material.BLAZE_POWDER, "&c\ua731\u1d1b\u0280\u1d07\u0274\u0262\u1d1b\u029c \u026a\u026a &7(1:30)", "", ON, TOGGLE_HINT).glow(),
                            Face.of(Material.BLAZE_POWDER, "&c\ua731\u1d1b\u0280\u1d07\u0274\u0262\u1d1b\u029c \u026a\u026a &7(1:30)", "", OFF, TOGGLE_HINT)),
                 StartEffect.STRENGTH2_90.name());
        this.put(rows, gui.slot(MENU, "speed", 12, rows),
                 gui.toggle(MENU, "speed", this.kit.hasStartEffect(StartEffect.SPEED2_90), tokens,
                            Face.of(Material.SUGAR, "&d\ua731\u1d18\u1d07\u1d07\u1d05 \u026a\u026a &7(1:30)", "", ON, TOGGLE_HINT).glow(),
                            Face.of(Material.SUGAR, "&d\ua731\u1d18\u1d07\u1d07\u1d05 \u026a\u026a &7(1:30)", "", OFF, TOGGLE_HINT)),
                 StartEffect.SPEED2_90.name());
        this.put(rows, gui.slot(MENU, "speed-infinite", 14, rows),
                 gui.toggle(MENU, "speed-infinite", this.kit.hasStartEffect(StartEffect.SPEED2_INFINITE), tokens,
                            Face.of(Material.FEATHER, "&d\ua731\u1d18\u1d07\u1d07\u1d05 \u026a\u026a &7(\u026a\u0274\ua730\u026a\u0274\u026a\u1d1b\u1d07)", "", ON, TOGGLE_HINT).glow(),
                            Face.of(Material.FEATHER, "&d\ua731\u1d18\u1d07\u1d07\u1d05 \u026a\u026a &7(\u026a\u0274\ua730\u026a\u0274\u026a\u1d1b\u1d07)", "", OFF, TOGGLE_HINT)),
                 StartEffect.SPEED2_INFINITE.name());
        this.put(rows, gui.slot(MENU, "regeneration", 16, rows),
                 gui.toggle(MENU, "regeneration", this.kit.hasStartEffect(StartEffect.REGEN2_90), tokens,
                            Face.of(Material.GHAST_TEAR, "&a\u0280\u1d07\u0262\u1d07\u0274\u1d07\u0280\u1d00\u1d1b\u026a\u1d0f\u0274 \u026a\u026a &7(1:30)", "", ON, TOGGLE_HINT).glow(),
                            Face.of(Material.GHAST_TEAR, "&a\u0280\u1d07\u0262\u1d07\u0274\u1d07\u0280\u1d00\u1d1b\u026a\u1d0f\u0274 \u026a\u026a &7(1:30)", "", OFF, TOGGLE_HINT)),
                 StartEffect.REGEN2_90.name());
        // Admin-only, server-wide actions. Everything above is this kit's start
        // effects; everything here changes what the kit IS for every player.
        this.inventory.setItem(Math.min(rows * 9 - 1, gui.slot(MENU, "edit-items", 21, rows)),
                gui.item(MENU, "edit-items", tokens, Material.CHEST,
                         "&d&l\u1d07\u1d05\u026a\u1d1b \u026a\u1d1b\u1d07\u1d0d\ua731",
                         "&7\u1d0f\u1d18\u1d07\u0274 \u1d1b\u029c\u1d07 \u1d0b\u026a\u1d1b'\ua731 \u026a\u1d1b\u1d07\u1d0d \u0262\u0280\u026a\u1d05.",
                         "",
                         "&c\u1d04\u029c\u1d00\u0274\u0262\u1d07\ua731 \u1d00\u1d18\u1d18\u029f\u028f \u1d1b\u1d0f \u1d07\u1d20\u1d07\u0280\u028f\u1d0f\u0274\u1d07.")
                        .tag(this.plugin.keyButton(), "edit-items").build());
        this.inventory.setItem(Math.min(rows * 9 - 1, gui.slot(MENU, "capture", 23, rows)),
                gui.item(MENU, "capture", tokens, Material.PLAYER_HEAD,
                         "&a&l\u1d04\u029c\u1d00\u0274\u0262\u1d07 \u1d0b\u026a\u1d1b",
                         "&7\u1d0d\u1d00\u1d0b\u1d07 \u1d1b\u029c\u026a\ua731 \u1d0b\u026a\u1d1b \u1d07x\u1d00\u1d04\u1d1b\u029f\u028f \u1d21\u029c\u1d00\u1d1b \u028f\u1d0f\u1d1c",
                         "&7\u1d00\u0280\u1d07 \u029c\u1d0f\u029f\u1d05\u026a\u0274\u0262 \u0280\u026a\u0262\u029c\u1d1b \u0274\u1d0f\u1d21.",
                         "",
                         "&c\u0280\u1d07\u1d18\u029f\u1d00\u1d04\u1d07\ua731 \u1d1b\u029c\u1d07 \u1d0b\u026a\u1d1b \ua730\u1d0f\u0280 \u1d07\u1d20\u1d07\u0280\u028f\u1d0f\u0274\u1d07.",
                         "&e\ua731\u029c\u026a\ua730\u1d1b-\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d04\u1d0f\u0274\ua730\u026a\u0280\u1d0d.")
                        .tag(this.plugin.keyButton(), "capture").build());
    }

    /** A toggle, tagged with the effect name its click handler expects. */
    private void put(int rows, int slot, Items item, String effect) {
        this.inventory.setItem(Math.max(0, Math.min(rows * 9 - 1, slot)),
                               item.hideTooltip().tag(this.plugin.keyButton(), effect).build());
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String button = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if (button == null) {
            return;
        }
        if ("edit-items".equals(button)) {
            player.closeInventory();
            new KitItemsEditMenu(this.plugin, this.kit).open(player); // global mode
            return;
        }
        if ("capture".equals(button)) {
            if (!event.isShiftClick()) {
                player.sendMessage(this.plugin.messages().get("notice.kit.shift-click-replace-kit", "kit", this.kit.getName()));
                return;
            }
            this.kit.captureFrom(player);
            this.plugin.getKitManager().put(this.kit);
            this.plugin.getKitManager().save();
            player.closeInventory();
            player.sendMessage(this.plugin.messages().get("notice.kit.kit-now-inventory-everyone", "kit", this.kit.getName()));
            return;
        }
        try {
            this.kit.toggleStartEffect(StartEffect.valueOf(button));
            this.plugin.getKitManager().save();
            this.build();
            player.openInventory(this.inventory);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            // empty catch block
        }
    }
}


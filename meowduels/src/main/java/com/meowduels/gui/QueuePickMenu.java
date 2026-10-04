/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.inventory.ItemStack
 */
package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.managers.CategoryManager;
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.managers.GuiConfigManager.Face;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public class QueuePickMenu
extends Menu {
    private static final Set<QueuePickMenu> OPEN = Collections.newSetFromMap(new ConcurrentHashMap());
    private static final int ARROW_SLOT = 35;
    private static final int[] KIT_SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private final MeowDuels plugin;
    private Player viewer;
    private int categoryIndex = 0;

    public QueuePickMenu(MeowDuels plugin) {
        this.plugin = plugin;
    }

    public static void refreshAll() {
        for (QueuePickMenu menu : OPEN) {
            if (menu.inventory == null) continue;
            menu.populate();
        }
    }

    @Override
    public void open(Player player) {
        this.viewer = player;
        this.build();
        player.openInventory(this.inventory);
        OPEN.add(this);
    }

    @Override
    public void onClose(Player player, InventoryCloseEvent event) {
        OPEN.remove(this);
    }

    /** The name of this screen's file: {@code gui/queuepick.yml}. */
    private static final String MENU = "queuepick";

    private static final int ROWS = 4;
    private static final int SLOT_ARROW = 35;

    @Override
    public void build() {
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        List<CategoryManager.Category> categories = this.plugin.getQueueCategoryManager().all();
        if (categories.isEmpty()) {
            this.createRaw(rows, gui.titleOf(MENU, "plain",
                    "&7\ua7af\u1d1c\u1d07\u1d1c\u1d07", null));
        } else {
            if (this.categoryIndex >= categories.size()) {
                this.categoryIndex = 0;
            }
            Map<String, String> tokens = new HashMap<String, String>();
            tokens.put("category", categories.get(this.categoryIndex).getHeader());
            this.createRaw(rows, gui.titleOf(MENU, "category",
                    "&7\ua7af\u1d1c\u1d07\u1d1c\u1d07 \u2192 &7{category}", tokens));
        }
        this.populate();
    }

    private void populate() {
        int i;
        List<String> kitNames;
        if (this.inventory == null) {
            return;
        }
        List<CategoryManager.Category> categories = this.plugin.getQueueCategoryManager().all();
        boolean multi = false;
        if (categories.isEmpty()) {
            kitNames = new ArrayList<String>();
            for (Kit kit : this.plugin.getKitManager().all()) {
                kitNames.add(kit.getName());
            }
        } else {
            if (this.categoryIndex >= categories.size()) {
                this.categoryIndex = 0;
            }
            multi = categories.size() > 1;
            kitNames = this.plugin.getQueueCategoryManager().kitsFor(categories.get(this.categoryIndex));
        }
        GuiConfigManager gui = this.plugin.gui();
        int rows = this.inventory.getSize() / 9;
        int last = this.inventory.getSize() - 1;
        gui.frame(MENU, this.inventory, rows);
        for (i = 0; i < KIT_SLOTS.length && i < kitNames.size(); ++i) {
            if (KIT_SLOTS[i] > last) break;
            ItemStack icon = this.kitIcon(gui, (String)kitNames.get(i));
            if (icon == null) continue;
            this.inventory.setItem(KIT_SLOTS[i], icon);
        }
        if (multi) {
            this.inventory.setItem(Math.min(last, gui.slot(MENU, "next-category", SLOT_ARROW, rows)),
                    gui.face(MENU, "next-category", null,
                             Face.of(Material.ARROW,
                                     "&e\u0274\u1d07x\u1d1b \u1d04\u1d00\u1d1b\u1d07\u0262\u1d0f\u0280\u028f",
                                     "&f\u2192 \u1d00\u0274\u1d0f\u1d1b\u029c\u1d07\u0280 \u1d04\u1d00\u1d1b\u1d07\u0262\u1d0f\u0280\u028f"))
                            .tag(this.plugin.keyButton(), "next-cat").build());
        }
    }

    /**
     * One kit's queue tile: how many are waiting, how many are fighting.
     *
     * <p>The stack size is the number of fights running, so a busy kit reads
     * as a tall stack at a glance.
     *
     * <p>The marker is \u2694 (U+2694) rather than the \ud83d\udde1 (U+1F5E1) this used to
     * carry. Minecraft's bundled font only covers the Basic Multilingual
     * Plane, so anything above U+FFFF draws as a white box - which is what
     * those two lines have actually been showing.
     */
    private ItemStack kitIcon(GuiConfigManager gui, String name) {
        Kit kit = this.plugin.getKitManager().get(name);
        if (kit == null) {
            return null;
        }
        int fights = this.plugin.getQueueManager().dueling(name);
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("kit", kit.getName());
        tokens.put("queued", String.valueOf(this.plugin.getQueueManager().queued(name)));
        tokens.put("fights", String.valueOf(fights));
        int amount = Math.max(1, Math.min(64, fights));
        boolean mine = this.viewer != null
                && this.plugin.getQueueManager().isQueuedFor(this.viewer.getUniqueId(), name);
        Items item = gui.toggle(MENU, "kit", mine, tokens,
                        Face.of(gui.material(MENU, "kit", kit.getIcon()), "&e{kit}",
                                "&6\u2694 &f\u026a\u0274 \ua7af\u1d1c\u1d07\u1d1c\u1d07: &6{queued}&7/&62",
                                "&6\u2694 &f\u1d05\u1d1c\u1d07\u029f\u026a\u0274\u0262: &6{fights}",
                                "",
                                "&7\u029f\u1d0d\u0299 \u1d1b\u1d0f &c\u029f\u1d07\u1d00\u1d20\u1d07").amount(amount).glow(),
                        Face.of(gui.material(MENU, "kit", kit.getIcon()), "&e{kit}",
                                "&6\u2694 &f\u026a\u0274 \ua7af\u1d1c\u1d07\u1d1c\u1d07: &6{queued}&7/&62",
                                "&6\u2694 &f\u1d05\u1d1c\u1d07\u029f\u026a\u0274\u0262: &6{fights}",
                                "",
                                "&7\u029f\u1d0d\u0299 \u1d1b\u1d0f &a\u1d0a\u1d0f\u026a\u0274").amount(amount))
                .hideTooltip()
                .tag(this.plugin.keyKit(), kit.getName());
        if (gui.name(MENU, "kit", tokens) == null
                && kit.getDisplayName() != null && !kit.getDisplayName().isEmpty()) {
            item.miniName(kit.getDisplayName());
        }
        return item.build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if ("next-cat".equals(Items.readTag(clicked, this.plugin.keyButton()))) {
            int size = this.plugin.getQueueCategoryManager().all().size();
            if (size > 0) {
                this.categoryIndex = (this.categoryIndex + 1) % size;
                this.build();
                player.openInventory(this.inventory);
            }
            return;
        }
        String kitName = Items.readTag(clicked, this.plugin.keyKit());
        if (kitName != null && this.plugin.getKitManager().exists(kitName)) {
            this.plugin.getQueueManager().toggle(player, kitName);
            if (this.plugin.getDuelManager().isInDuel(player.getUniqueId())) {
                player.closeInventory();
            } else {
                this.populate();
            }
        }
    }
}


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
import com.meowduels.managers.GuiLayoutManager;
import com.meowduels.model.Arena;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public class GuiEditorMenu
extends Menu {
    private static final int CANCEL_SLOT = 45;
    private static final int PALETTE_SLOT = 47;
    private static final int SAVE_SLOT = 49;
    private static final int RESET_SLOT = 51;
    private final MeowDuels plugin;
    private final String menuId;
    private final int editableSize;
    private boolean done = false;

    public GuiEditorMenu(MeowDuels plugin, String menuId) {
        this.plugin = plugin;
        this.menuId = menuId;
        this.editableSize = GuiLayoutManager.editableSize(menuId);
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public boolean isProtectedSlot(int rawSlot) {
        return rawSlot >= this.editableSize;
    }

    @Override
    public void build() {
        this.create(6, "&8Editing: &d" + this.menuId);
        if (this.plugin.getGuiLayoutManager().has(this.menuId)) {
            for (Map.Entry<Integer, ItemStack> entry : this.plugin.getGuiLayoutManager().get(this.menuId).entrySet()) {
                if (entry.getKey() >= this.editableSize) continue;
                this.inventory.setItem(entry.getKey().intValue(), entry.getValue());
            }
        } else {
            this.fillDefaults();
        }
        ItemStack locked = Items.of(Material.GRAY_STAINED_GLASS_PANE).name("&8(outside the menu)").build();
        for (int i = this.editableSize; i < 45; ++i) {
            this.inventory.setItem(i, locked);
        }
        ItemStack bar = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 45; i < 54; ++i) {
            this.inventory.setItem(i, bar);
        }
        this.inventory.setItem(45, Items.of(Material.BARRIER).name("&cCancel").lore("&7Close without saving.").build());
        this.inventory.setItem(47, Items.of(Material.GRAY_STAINED_GLASS_PANE).name("&8Decoration").lore("&7Click for decoration panes to place.").build());
        this.inventory.setItem(49, Items.of(Material.LIME_DYE).name("&a&lSave Layout").lore("&7Save this arrangement.").build());
        this.inventory.setItem(51, Items.of(Material.TNT).name("&cReset to Default").lore("&7Restore the default layout.").build());
    }

    private void fillDefaults() {
        switch (this.menuId) {
            case "duelconfirm": {
                ItemStack gray = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
                for (int i = 0; i < this.editableSize; ++i) {
                    this.inventory.setItem(i, gray);
                }
                this.inventory.setItem(10, this.buttonMarker("map", Material.FILLED_MAP, "&dArena button"));
                this.inventory.setItem(12, this.buttonMarker("kit", Material.GOLDEN_APPLE, "&6Kit button"));
                this.inventory.setItem(14, this.buttonMarker("clock", Material.CLOCK, "&eRounds button"));
                this.inventory.setItem(16, this.buttonMarker("ranked", Material.LIME_DYE, "&bRanked toggle"));
                this.inventory.setItem(22, this.buttonMarker("target", Material.PLAYER_HEAD, "&fOpponent head"));
                this.inventory.setItem(29, this.buttonMarker("confirm", Material.LIME_DYE, "&aConfirm button"));
                this.inventory.setItem(33, this.buttonMarker("cancel", Material.BARRIER, "&cCancel button"));
                break;
            }
            case "mapselect": {
                int slot = 0;
                for (Arena arena : this.plugin.getArenaManager().all()) {
                    if (slot >= this.editableSize - 1) break;
                    if (!arena.isConfigured()) continue;
                    this.inventory.setItem(slot++, Items.of(Material.FILLED_MAP).name("&e" + arena.getName()).lore("&fArena icon - drag to arrange.").tag(this.plugin.keyArena(), arena.getName()).build());
                }
                this.inventory.setItem(this.editableSize - 1, this.buttonMarker("random", Material.ENDER_PEARL, "&eRandom button"));
                break;
            }
        }
    }

    private ItemStack buttonMarker(String id, Material icon, String name) {
        return Items.of(icon).name(name).lore("&7Menu button - drag to arrange.").tag(this.plugin.keyButton(), id).build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        switch (event.getRawSlot()) {
            case 49: {
                this.persist();
                this.done = true;
                player.sendMessage(Text.prefixed("&aSaved the &d" + this.menuId + "&a menu layout."));
                player.closeInventory();
                break;
            }
            case 45: {
                this.done = true;
                player.sendMessage(Text.prefixed("&7Editor closed without saving."));
                player.closeInventory();
                break;
            }
            case 47: {
                player.getInventory().addItem(new ItemStack[]{Items.of(Material.GRAY_STAINED_GLASS_PANE, 16).name("&8Decoration").build()});
                player.sendMessage(Text.prefixed("&7Added decoration panes to your inventory."));
                break;
            }
            case 51: {
                this.plugin.getGuiLayoutManager().clear(this.menuId);
                this.fillDefaultsFresh();
                player.openInventory(this.inventory);
                player.sendMessage(Text.prefixed("&7Reset &d" + this.menuId + "&7 to default."));
            }
        }
    }

    private void fillDefaultsFresh() {
        for (int i = 0; i < this.editableSize; ++i) {
            this.inventory.setItem(i, null);
        }
        this.fillDefaults();
    }

    @Override
    public void onClose(Player player, InventoryCloseEvent event) {
        if (this.done) {
            return;
        }
        this.persist();
        player.sendMessage(Text.prefixed("&aSaved the &d" + this.menuId + "&a menu layout."));
    }

    private void persist() {
        LinkedHashMap<Integer, ItemStack> layout = new LinkedHashMap<Integer, ItemStack>();
        for (int i = 0; i < this.editableSize; ++i) {
            ItemStack item = this.inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            layout.put(i, item.clone());
        }
        switch (this.menuId) {
            case "duelconfirm": {
                this.ensureButton(layout, "map", Material.FILLED_MAP, "&dArena button", 10);
                this.ensureButton(layout, "kit", Material.GOLDEN_APPLE, "&6Kit button", 12);
                this.ensureButton(layout, "clock", Material.CLOCK, "&eRounds button", 14);
                this.ensureButton(layout, "ranked", Material.LIME_DYE, "&bRanked toggle", 16);
                this.ensureButton(layout, "target", Material.PLAYER_HEAD, "&fOpponent head", 22);
                this.ensureButton(layout, "confirm", Material.LIME_DYE, "&aConfirm button", 29);
                this.ensureButton(layout, "cancel", Material.BARRIER, "&cCancel button", 33);
                break;
            }
            case "mapselect": {
                this.ensureButton(layout, "random", Material.ENDER_PEARL, "&eRandom button", this.editableSize - 1);
            }
        }
        this.plugin.getGuiLayoutManager().set(this.menuId, layout);
    }

    private void ensureButton(Map<Integer, ItemStack> layout, String id, Material icon, String name, int defaultSlot) {
        int slot;
        for (ItemStack item : layout.values()) {
            if (!id.equals(Items.readTag(item, this.plugin.keyButton()))) continue;
            return;
        }
        int n = slot = layout.containsKey(defaultSlot) ? this.firstFree(layout) : defaultSlot;
        if (slot >= 0) {
            layout.put(slot, this.buttonMarker(id, icon, name));
        }
    }

    private int firstFree(Map<Integer, ItemStack> layout) {
        for (int i = 0; i < this.editableSize; ++i) {
            if (layout.containsKey(i)) continue;
            return i;
        }
        return -1;
    }
}


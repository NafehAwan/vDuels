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
import com.meowduels.gui.EditKitEffectsMenu;
import com.meowduels.gui.Menu;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public class KitItemsEditMenu
extends Menu {
    private static final int OFFHAND_SLOT = 45;
    private static final int[] ARMOR_SLOTS = new int[]{46, 47, 48, 49};
    private static final int EFFECTS_SLOT = 43;
    private static final int NAME_SLOT = 44;
    private static final int RESET_SLOT = 51;
    private static final int SAVE_SLOT = 52;
    private static final int EXIT_SLOT = 53;
    private final MeowDuels plugin;
    private final Kit kit;

    public KitItemsEditMenu(MeowDuels plugin, Kit kit) {
        this.plugin = plugin;
        this.kit = kit;
    }

    @Override
    public void build() {
        this.create(6, "&d&lEdit Items \u2192 &5" + this.kit.getName());
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 36; i < 54; ++i) {
            this.inventory.setItem(i, filler);
        }
        ItemStack[] contents = this.kit.getContents();
        for (int i = 0; i < 36 && i < contents.length; ++i) {
            this.inventory.setItem(i, contents[i]);
        }
        this.inventory.setItem(45, this.kit.getOffhand());
        ItemStack[] armor = this.kit.getArmor();
        for (int i = 0; i < ARMOR_SLOTS.length && i < armor.length; ++i) {
            this.inventory.setItem(ARMOR_SLOTS[i], armor[i]);
        }
        this.inventory.setItem(43, Items.of(Material.POTION).name("&d&lStart Effects").lore("&7Configure potion effects applied", "&7at the start of the fight.").build());
        this.inventory.setItem(44, Items.of(Material.NAME_TAG).name("&f" + this.kit.getName()).lore("&7Drag items into the grid above,", "&7fill armor + offhand below.").build());
        this.inventory.setItem(51, Items.of(Material.RED_STAINED_GLASS_PANE).name("&c&lReset").lore("&7Restores the kit's saved layout.").build());
        this.inventory.setItem(52, Items.of(Material.LIME_STAINED_GLASS_PANE).name("&a&lSave").lore("&7Save this layout to the kit.").build());
        this.inventory.setItem(53, Items.of(Material.BARRIER).name("&c&lExit").lore("&7Close without saving.").build());
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public boolean isProtectedSlot(int rawSlot) {
        if (rawSlot >= 0 && rawSlot < 36) {
            return false;
        }
        if (rawSlot == 45) {
            return false;
        }
        for (int slot : ARMOR_SLOTS) {
            if (rawSlot != slot) continue;
            return false;
        }
        return true;
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 43) {
            player.closeInventory();
            new EditKitEffectsMenu(this.plugin, this.kit).open(player);
            return;
        }
        if (slot == 51) {
            this.restoreOriginalLayout();
            player.sendMessage(Text.prefixed("&eReset to the kit's saved layout."));
            return;
        }
        if (slot == 52) {
            this.saveToKit();
            player.sendMessage(Text.prefixed("&aSaved &d" + this.kit.getName() + "&a's items."));
            player.closeInventory();
            return;
        }
        if (slot == 53) {
            player.closeInventory();
        }
    }

    @Override
    public void onClose(Player player, InventoryCloseEvent event) {
    }

    private void restoreOriginalLayout() {
        ItemStack[] contents = this.kit.getContents();
        for (int i = 0; i < 36; ++i) {
            this.inventory.setItem(i, contents != null && i < contents.length ? contents[i] : null);
        }
        this.inventory.setItem(45, this.kit.getOffhand());
        ItemStack[] armor = this.kit.getArmor();
        for (int i = 0; i < ARMOR_SLOTS.length; ++i) {
            this.inventory.setItem(ARMOR_SLOTS[i], armor != null && i < armor.length ? armor[i] : null);
        }
    }

    private void saveToKit() {
        ItemStack[] contents = new ItemStack[36];
        for (int i = 0; i < 36; ++i) {
            contents[i] = this.inventory.getItem(i);
        }
        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < ARMOR_SLOTS.length; ++i) {
            armor[i] = this.inventory.getItem(ARMOR_SLOTS[i]);
        }
        this.kit.setContents(contents);
        this.kit.setArmor(armor);
        this.kit.setOffhand(this.inventory.getItem(45));
        this.plugin.getKitManager().save();
    }
}


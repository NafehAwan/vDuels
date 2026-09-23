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
    /** Personal: saves this player's own layout. Global (admins): edits the kit
     *  itself for everyone, and is the only place start effects can be set. */
    private final boolean personal;
    private final java.util.UUID owner;

    public KitItemsEditMenu(MeowDuels plugin, Kit kit) {
        this(plugin, kit, false, null);
    }

    public KitItemsEditMenu(MeowDuels plugin, Kit kit, boolean personal, java.util.UUID owner) {
        this.plugin = plugin;
        this.kit = kit;
        this.personal = personal;
        this.owner = owner;
    }

    @Override
    public void build() {
        this.create(6, this.personal
                ? "&d&lYour Layout \u2192 &5" + this.kit.getName()
                : "&d&lEdit Items \u2192 &5" + this.kit.getName());
        ItemStack filler = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 36; i < 54; ++i) {
            this.inventory.setItem(i, filler);
        }
        this.restoreOriginalLayout();
        if (this.personal) {
            // Start effects are part of the kit itself, not a personal layout,
            // so the button is admin-only and simply absent here.
            this.inventory.setItem(43, Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
            this.inventory.setItem(44, Items.of(Material.NAME_TAG).name("&f" + this.kit.getName()).lore("&7Arrange the kit however you like.", "&7Only YOU get this layout.", "&8Items are set by the server.").build());
            this.inventory.setItem(51, Items.of(Material.RED_STAINED_GLASS_PANE).name("&c&lReset").lore("&7Back to the server's default layout.").build());
            this.inventory.setItem(52, Items.of(Material.LIME_STAINED_GLASS_PANE).name("&a&lSave").lore("&7Save this layout for yourself.").build());
        } else {
            this.inventory.setItem(43, Items.of(Material.POTION).name("&d&lStart Effects").lore("&7Configure potion effects applied", "&7at the start of the fight.").build());
            this.inventory.setItem(44, Items.of(Material.NAME_TAG).name("&f" + this.kit.getName()).lore("&7Drag items into the grid above,", "&7fill armor + offhand below.", "&cThis changes the kit for everyone.").build());
            this.inventory.setItem(51, Items.of(Material.RED_STAINED_GLASS_PANE).name("&c&lReset").lore("&7Restores the kit's saved layout.").build());
            this.inventory.setItem(52, Items.of(Material.LIME_STAINED_GLASS_PANE).name("&a&lSave").lore("&7Save this layout to the kit.").build());
        }
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
            if (this.personal || !player.hasPermission("meowduels.admin")) {
                return; // start effects belong to the kit, and admins own those
            }
            player.closeInventory();
            new EditKitEffectsMenu(this.plugin, this.kit).open(player);
            return;
        }
        if (slot == 51) {
            if (this.personal) {
                this.plugin.getKitLayouts().clear(player.getUniqueId(), this.kit.getName());
                player.sendMessage(Text.prefixed("&eYour &d" + this.kit.getName() + "&e layout was reset to the server default."));
            } else {
                player.sendMessage(Text.prefixed("&eReset to the kit's saved layout."));
            }
            this.restoreOriginalLayout();
            return;
        }
        if (slot == 52) {
            if (this.personal && !this.plugin.getKitLayouts().matchesKit(
                    this.kit, this.grid(), this.gridArmor(), this.inventory.getItem(45))) {
                player.sendMessage(Text.prefixed("&cThat isn't the same set of items. "
                        + "&7You can rearrange the kit, but not change what's in it."));
                return;
            }
            this.saveToKit();
            player.sendMessage(this.personal
                    ? Text.prefixed("&aSaved your own &d" + this.kit.getName() + "&a layout.")
                    : Text.prefixed("&aSaved &d" + this.kit.getName() + "&a's items &7- for everyone."));
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

    /** Fills the grid with the layout being edited: the player's own if this is
     *  a personal edit and they have one, otherwise the kit's default. */
    private void restoreOriginalLayout() {
        java.util.UUID id = this.owner;
        boolean own = this.personal && id != null
                && this.plugin.getKitLayouts().has(id, this.kit.getName());
        ItemStack[] contents = own
                ? this.plugin.getKitLayouts().contents(id, this.kit.getName())
                : this.kit.getContents();
        ItemStack[] armor = own
                ? this.plugin.getKitLayouts().armor(id, this.kit.getName())
                : this.kit.getArmor();
        ItemStack offhand = own
                ? this.plugin.getKitLayouts().offhand(id, this.kit.getName())
                : this.kit.getOffhand();
        for (int i = 0; i < 36; ++i) {
            this.inventory.setItem(i, contents != null && i < contents.length ? contents[i] : null);
        }
        this.inventory.setItem(45, offhand);
        for (int i = 0; i < ARMOR_SLOTS.length; ++i) {
            this.inventory.setItem(ARMOR_SLOTS[i], armor != null && i < armor.length ? armor[i] : null);
        }
    }

    private ItemStack[] grid() {
        ItemStack[] contents = new ItemStack[36];
        for (int i = 0; i < 36; ++i) {
            contents[i] = this.inventory.getItem(i);
        }
        return contents;
    }

    private ItemStack[] gridArmor() {
        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < ARMOR_SLOTS.length; ++i) {
            armor[i] = this.inventory.getItem(ARMOR_SLOTS[i]);
        }
        return armor;
    }

    private void saveToKit() {
        ItemStack[] contents = this.grid();
        ItemStack[] armor = this.gridArmor();
        if (this.personal && this.owner != null) {
            this.plugin.getKitLayouts().store(this.owner, this.kit.getName(),
                    contents, armor, this.inventory.getItem(45));
            return;
        }
        this.kit.setContents(contents);
        this.kit.setArmor(armor);
        this.kit.setOffhand(this.inventory.getItem(45));
        this.plugin.getKitManager().save();
    }
}


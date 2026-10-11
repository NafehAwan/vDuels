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
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.managers.GuiConfigManager.Face;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import java.util.HashMap;
import java.util.Map;
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

    /** The name of this screen's file: {@code gui/kititems.yml}. */
    private static final String MENU = "kititems";

    @Override
    public void build() {
        GuiConfigManager gui = this.plugin.gui();
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("kit", this.kit.getName());
        // Six rows and these slots are fixed: the grid above is the kit itself
        // and the buttons are matched by slot number, so nothing here can move.
        this.createRaw(6, this.personal
                ? gui.titleOf(MENU, "personal",
                        "&d&l\u028f\u1d0f\u1d1c\u0280 \u029f\u1d00\u028f\u1d0f\u1d1c\u1d1b \u2192 &5{kit}", tokens)
                : gui.titleOf(MENU, "global",
                        "&d&l\u1d07\u1d05\u026a\u1d1b \u026a\u1d1b\u1d07\u1d0d\ua731 \u2192 &5{kit}", tokens));
        ItemStack filler = gui.pane(MENU);
        for (int i = 36; i < 54; ++i) {
            this.inventory.setItem(i, filler);
        }
        this.restoreOriginalLayout();
        if (this.personal) {
            // Start effects are part of the kit itself, not a personal layout,
            // so the button is admin-only and simply absent here.
            this.inventory.setItem(43, filler);
            this.inventory.setItem(44, gui.face(MENU, "about-personal", tokens,
                    Face.of(Material.NAME_TAG, "&f{kit}",
                            "&7\u1d00\u0280\u0280\u1d00\u0274\u0262\u1d07 \u1d1b\u029c\u1d07 \u1d0b\u026a\u1d1b \u029c\u1d0f\u1d21\u1d07\u1d20\u1d07\u0280 \u028f\u1d0f\u1d1c \u029f\u026a\u1d0b\u1d07.",
                            "&7\u1d0f\u0274\u029f\u028f \u028f\u1d0f\u1d1c \u0262\u1d07\u1d1b \u1d1b\u029c\u026a\ua731 \u029f\u1d00\u028f\u1d0f\u1d1c\u1d1b.",
                            "&8\u026a\u1d1b\u1d07\u1d0d\ua731 \u1d00\u0280\u1d07 \ua731\u1d07\u1d1b \u0299\u028f \u1d1b\u029c\u1d07 \ua731\u1d07\u0280\u1d20\u1d07\u0280.")).build());
            this.inventory.setItem(51, gui.face(MENU, "reset-personal", tokens,
                    Face.of(Material.RED_STAINED_GLASS_PANE,
                            "&c&l\u0280\u1d07\ua731\u1d07\u1d1b",
                            "&7\u0299\u1d00\u1d04\u1d0b \u1d1b\u1d0f \u1d1b\u029c\u1d07 \ua731\u1d07\u0280\u1d20\u1d07\u0280'\ua731 \u1d05\u1d07\ua730\u1d00\u1d1c\u029f\u1d1b \u029f\u1d00\u028f\u1d0f\u1d1c\u1d1b.")).build());
            this.inventory.setItem(52, gui.face(MENU, "save-personal", tokens,
                    Face.of(Material.LIME_STAINED_GLASS_PANE,
                            "&a&l\ua731\u1d00\u1d20\u1d07",
                            "&7\ua731\u1d00\u1d20\u1d07 \u1d1b\u029c\u026a\ua731 \u029f\u1d00\u028f\u1d0f\u1d1c\u1d1b \ua730\u1d0f\u0280 \u028f\u1d0f\u1d1c\u0280\ua731\u1d07\u029f\ua730.")).build());
        } else {
            this.inventory.setItem(43, gui.face(MENU, "start-effects", tokens,
                    Face.of(Material.POTION,
                            "&d&l\ua731\u1d1b\u1d00\u0280\u1d1b \u1d07\ua730\ua730\u1d07\u1d04\u1d1b\ua731",
                            "&7\u1d04\u1d0f\u0274\ua730\u026a\u0262\u1d1c\u0280\u1d07 \u1d18\u1d0f\u1d1b\u026a\u1d0f\u0274 \u1d07\ua730\ua730\u1d07\u1d04\u1d1b\ua731 \u1d00\u1d18\u1d18\u029f\u026a\u1d07\u1d05",
                            "&7\u1d00\u1d1b \u1d1b\u029c\u1d07 \ua731\u1d1b\u1d00\u0280\u1d1b \u1d0f\ua730 \u1d1b\u029c\u1d07 \ua730\u026a\u0262\u029c\u1d1b.")).build());
            this.inventory.setItem(44, gui.face(MENU, "about-global", tokens,
                    Face.of(Material.NAME_TAG, "&f{kit}",
                            "&7\u1d05\u0280\u1d00\u0262 \u026a\u1d1b\u1d07\u1d0d\ua731 \u026a\u0274\u1d1b\u1d0f \u1d1b\u029c\u1d07 \u0262\u0280\u026a\u1d05 \u1d00\u0299\u1d0f\u1d20\u1d07,",
                            "&7\ua730\u026a\u029f\u029f \u1d00\u0280\u1d0d\u1d0f\u0280 + \u1d0f\ua730\ua730\u029c\u1d00\u0274\u1d05 \u0299\u1d07\u029f\u1d0f\u1d21.",
                            "&c\u1d1b\u029c\u026a\ua731 \u1d04\u029c\u1d00\u0274\u0262\u1d07\ua731 \u1d1b\u029c\u1d07 \u1d0b\u026a\u1d1b \ua730\u1d0f\u0280 \u1d07\u1d20\u1d07\u0280\u028f\u1d0f\u0274\u1d07.")).build());
            this.inventory.setItem(51, gui.face(MENU, "reset-global", tokens,
                    Face.of(Material.RED_STAINED_GLASS_PANE,
                            "&c&l\u0280\u1d07\ua731\u1d07\u1d1b",
                            "&7\u0280\u1d07\ua731\u1d1b\u1d0f\u0280\u1d07\ua731 \u1d1b\u029c\u1d07 \u1d0b\u026a\u1d1b'\ua731 \ua731\u1d00\u1d20\u1d07\u1d05 \u029f\u1d00\u028f\u1d0f\u1d1c\u1d1b.")).build());
            this.inventory.setItem(52, gui.face(MENU, "save-global", tokens,
                    Face.of(Material.LIME_STAINED_GLASS_PANE,
                            "&a&l\ua731\u1d00\u1d20\u1d07",
                            "&7\ua731\u1d00\u1d20\u1d07 \u1d1b\u029c\u026a\ua731 \u029f\u1d00\u028f\u1d0f\u1d1c\u1d1b \u1d1b\u1d0f \u1d1b\u029c\u1d07 \u1d0b\u026a\u1d1b.")).build());
        }
        this.inventory.setItem(53, gui.face(MENU, "exit", tokens,
                Face.of(Material.BARRIER,
                        "&c&l\u1d07x\u026a\u1d1b",
                        "&7\u1d04\u029f\u1d0f\ua731\u1d07 \u1d21\u026a\u1d1b\u029c\u1d0f\u1d1c\u1d1b \ua731\u1d00\u1d20\u026a\u0274\u0262.")).build());
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
                player.sendMessage(this.plugin.messages().get("notice.kit.layout-was-reset-server", "kit", this.kit.getName()));
            } else {
                player.sendMessage(this.plugin.messages().get("notice.kit.reset-kit-s-saved"));
            }
            this.restoreOriginalLayout();
            return;
        }
        if (slot == 52) {
            if (this.personal && !this.plugin.getKitLayouts().matchesKit(
                    this.kit, this.grid(), this.gridArmor(), this.inventory.getItem(45))) {
                player.sendMessage(this.plugin.messages().get("notice.kit.isn-t-same-set"));
                return;
            }
            this.saveToKit();
            player.sendMessage(this.personal
                    ? this.plugin.messages().get("notice.kit.saved-own-layout", "kit", this.kit.getName())
                    : this.plugin.messages().get("notice.kit.saved-s-items-everyone", "kit", this.kit.getName()));
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


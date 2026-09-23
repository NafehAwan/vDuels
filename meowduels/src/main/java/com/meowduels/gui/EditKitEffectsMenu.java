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
import com.meowduels.model.Kit;
import com.meowduels.model.StartEffect;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
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

    @Override
    public void build() {
        this.create(3, "&7&lEdit Kit \u2192 &7" + this.kit.getName());
        ItemStack filler = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; ++i) {
            this.inventory.setItem(i, filler);
        }
        this.inventory.setItem(10, this.toggle(Material.BLAZE_POWDER, "&cStrength II &7(1:30)", StartEffect.STRENGTH2_90));
        this.inventory.setItem(12, this.toggle(Material.SUGAR, "&dSpeed II &7(1:30)", StartEffect.SPEED2_90));
        this.inventory.setItem(14, this.toggle(Material.FEATHER, "&dSpeed II &7(Infinite)", StartEffect.SPEED2_INFINITE));
        this.inventory.setItem(16, this.toggle(Material.GHAST_TEAR, "&aRegeneration II &7(1:30)", StartEffect.REGEN2_90));
        // Admin-only, server-wide actions. Everything above is this kit's start
        // effects; everything here changes what the kit IS for every player.
        this.inventory.setItem(21, Items.of(Material.CHEST)
                .name("&d&lEdit Items")
                .lore("&7Open the kit's item grid.", "", "&cChanges apply to everyone.")
                .tag(this.plugin.keyButton(), "edit-items").build());
        this.inventory.setItem(23, Items.of(Material.PLAYER_HEAD)
                .name("&a&lChange Kit")
                .lore("&7Make this kit exactly what you", "&7are holding right now.", "",
                      "&cReplaces the kit for everyone.", "&eShift-click to confirm.")
                .tag(this.plugin.keyButton(), "capture").build());
    }

    private ItemStack toggle(Material material, String name, StartEffect effect) {
        boolean on = this.kit.hasStartEffect(effect);
        return Items.of(material).name(name).lore("", on ? "&aEnabled" : "&cDisabled", "&7Click to toggle.").glow(on).hideTooltip().tag(this.plugin.keyButton(), effect.name()).build();
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
                player.sendMessage(Text.prefixed("&eShift-click to replace the &d"
                        + this.kit.getName() + "&e kit with your inventory &7- for everyone."));
                return;
            }
            this.kit.captureFrom(player);
            this.plugin.getKitManager().put(this.kit);
            this.plugin.getKitManager().save();
            player.closeInventory();
            player.sendMessage(Text.prefixed("&aThe &d" + this.kit.getName()
                    + "&a kit is now your inventory &7- for everyone."));
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


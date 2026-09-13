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
import com.meowduels.model.Kit;
import com.meowduels.model.StartEffect;
import com.meowduels.util.Items;
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
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; ++i) {
            this.inventory.setItem(i, filler);
        }
        this.inventory.setItem(10, this.toggle(Material.BLAZE_POWDER, "&cStrength II &7(1:30)", StartEffect.STRENGTH2_90));
        this.inventory.setItem(12, this.toggle(Material.SUGAR, "&dSpeed II &7(1:30)", StartEffect.SPEED2_90));
        this.inventory.setItem(14, this.toggle(Material.FEATHER, "&dSpeed II &7(Infinite)", StartEffect.SPEED2_INFINITE));
        this.inventory.setItem(16, this.toggle(Material.GHAST_TEAR, "&aRegeneration II &7(1:30)", StartEffect.REGEN2_90));
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


package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.model.Kit;
import com.vduels.model.StartEffect;
import com.vduels.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Per-kit starting-effect toggles ({@code /editkit <kit>}). Each of the three
 * options can be enabled independently; an enabled effect glows. With none
 * enabled the kit starts a round with no effects (the default).
 */
public class EditKitEffectsMenu extends Menu {

    private final VDuels plugin;
    private final Kit kit;

    public EditKitEffectsMenu(VDuels plugin, Kit kit) {
        this.plugin = plugin;
        this.kit = kit;
    }

    @Override
    public void build() {
        create(3, "&7&lEdit Kit → &7" + kit.getName());
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(11, toggle(Material.BLAZE_POWDER, "&cStrength II &7(1:30)", StartEffect.STRENGTH2_90));
        inventory.setItem(13, toggle(Material.SUGAR, "&bSpeed II &7(1:30)", StartEffect.SPEED2_90));
        inventory.setItem(15, toggle(Material.FEATHER, "&bSpeed II &7(Infinite)", StartEffect.SPEED2_INFINITE));
    }

    private ItemStack toggle(Material material, String name, StartEffect effect) {
        boolean on = kit.hasStartEffect(effect);
        return Items.of(material)
                .name(name)
                .lore("", on ? "&aEnabled" : "&cDisabled", "&7Click to toggle.")
                .glow(on)
                .hideTooltip()
                .tag(plugin.keyButton(), effect.name())
                .build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String button = Items.readTag(event.getCurrentItem(), plugin.keyButton());
        if (button == null) {
            return;
        }
        try {
            kit.toggleStartEffect(StartEffect.valueOf(button));
            plugin.getKitManager().save();
            build();
            player.openInventory(inventory);
        } catch (IllegalArgumentException ignored) {
            // Not an effect button.
        }
    }
}

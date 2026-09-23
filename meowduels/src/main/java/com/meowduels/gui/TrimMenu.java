/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import com.meowduels.util.Trims;
import java.util.ArrayList;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TrimMenu
extends Menu {
    private static final int HELMET_SLOT = 20;
    private static final int CHEST_SLOT = 21;
    private static final int LEGS_SLOT = 22;
    private static final int BOOTS_SLOT = 23;
    private static final int PATTERN_SLOT = 38;
    private static final int MATERIAL_SLOT = 42;
    private static final int APPLY_ALL_SLOT = 48;
    private static final int CLEAR_SLOT = 50;
    private final MeowDuels plugin;
    private final String kitName;
    private final UUID owner;
    private int patternIndex = 0;
    private int materialIndex = 0;

    public TrimMenu(MeowDuels plugin, String kitName, UUID owner) {
        this.plugin = plugin;
        this.kitName = kitName;
        this.owner = owner;
    }

    @Override
    public void build() {
        this.create(6, "&d&lTrims &7- &f" + this.kitName);
        ItemStack filler = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; ++i) {
            this.inventory.setItem(i, filler);
        }
        Kit kit = this.plugin.getKitManager().get(this.kitName);
        if (kit == null) {
            return;
        }
        ItemStack[] armor = kit.getArmor();
        this.inventory.setItem(20, this.armorDisplay(armor, 3, "Helmet"));
        this.inventory.setItem(21, this.armorDisplay(armor, 2, "Chestplate"));
        this.inventory.setItem(22, this.armorDisplay(armor, 1, "Leggings"));
        this.inventory.setItem(23, this.armorDisplay(armor, 0, "Boots"));
        this.inventory.setItem(38, Items.of(Material.PAPER).name("&d&lPattern&7: &f" + Trims.pretty(Trims.PATTERN_KEYS[this.patternIndex])).lore("&7Left-click &8- &fnext", "&7Right-click &8- &fprevious", "", "&8" + (this.patternIndex + 1) + "/" + Trims.PATTERN_KEYS.length).tag(this.plugin.keyButton(), "trim-pattern").build());
        this.inventory.setItem(42, Items.of(this.materialIcon()).name("&d&lMaterial&7: &f" + Trims.pretty(Trims.MATERIAL_KEYS[this.materialIndex])).lore("&7Left-click &8- &fnext", "&7Right-click &8- &fprevious", "", "&8" + (this.materialIndex + 1) + "/" + Trims.MATERIAL_KEYS.length).tag(this.plugin.keyButton(), "trim-material").build());
        this.inventory.setItem(48, Items.of(Material.LIME_DYE).name("&a&lApply to ALL pieces").lore("&7Set the selected trim on every armor piece.").tag(this.plugin.keyButton(), "trim-apply-all").build());
        this.inventory.setItem(50, Items.of(Material.BARRIER).name("&c&lClear all trims").lore("&7Remove trims from every armor piece.").tag(this.plugin.keyButton(), "trim-clear").build());
    }

    private ItemStack armorDisplay(ItemStack[] armor, int idx, String label) {
        ItemStack piece;
        ItemStack itemStack = piece = idx >= 0 && idx < armor.length ? armor[idx] : null;
        if (piece == null || piece.getType() == Material.AIR) {
            return Items.of(Material.GRAY_STAINED_GLASS_PANE).name("&7No " + label).lore("&8This kit has no " + label.toLowerCase() + ".").build();
        }
        ItemStack show = piece.clone();
        String[] saved = this.plugin.getTrimPreferences().get(this.owner, this.kitName, idx);
        if (saved != null) {
            Trims.apply(show, saved[0], saved[1]);
        }
        try {
            ItemMeta meta = show.getItemMeta();
            if (meta != null) {
                ArrayList<String> lore = new ArrayList<String>();
                if (saved != null) {
                    lore.add(Text.color("&7Your trim&8: &f" + Trims.pretty(saved[1]) + " " + Trims.pretty(saved[0])));
                } else {
                    lore.add(Text.color("&8No trim set."));
                }
                lore.add(Text.color("&eClick to apply the selected trim."));
                meta.setLore(lore);
                show.setItemMeta(meta);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return show;
    }

    private Material materialIcon() {
        switch (Trims.MATERIAL_KEYS[this.materialIndex]) {
            case "quartz": {
                return Material.QUARTZ;
            }
            case "iron": {
                return Material.IRON_INGOT;
            }
            case "gold": {
                return Material.GOLD_INGOT;
            }
            case "copper": {
                return Material.COPPER_INGOT;
            }
            case "netherite": {
                return Material.NETHERITE_INGOT;
            }
            case "redstone": {
                return Material.REDSTONE;
            }
            case "emerald": {
                return Material.EMERALD;
            }
            case "diamond": {
                return Material.DIAMOND;
            }
            case "lapis": {
                return Material.LAPIS_LAZULI;
            }
            case "amethyst": {
                return Material.AMETHYST_SHARD;
            }
        }
        return Material.PAPER;
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        String btn = Items.readTag(clicked, this.plugin.keyButton());
        if ("trim-pattern".equals(btn)) {
            int dir = event.isRightClick() ? -1 : 1;
            this.patternIndex = (this.patternIndex + dir + Trims.PATTERN_KEYS.length) % Trims.PATTERN_KEYS.length;
            this.reopen(player);
            return;
        }
        if ("trim-material".equals(btn)) {
            int dir = event.isRightClick() ? -1 : 1;
            this.materialIndex = (this.materialIndex + dir + Trims.MATERIAL_KEYS.length) % Trims.MATERIAL_KEYS.length;
            this.reopen(player);
            return;
        }
        if ("trim-apply-all".equals(btn)) {
            this.applyAll(player);
            return;
        }
        if ("trim-clear".equals(btn)) {
            this.clearAll(player);
            return;
        }
        int armorIdx = this.slotToArmor(event.getRawSlot());
        if (armorIdx >= 0) {
            this.applyOne(player, armorIdx);
        }
    }

    private int slotToArmor(int raw) {
        switch (raw) {
            case 20: {
                return 3;
            }
            case 21: {
                return 2;
            }
            case 22: {
                return 1;
            }
            case 23: {
                return 0;
            }
        }
        return -1;
    }

    private void applyOne(Player player, int armorIdx) {
        Kit kit = this.plugin.getKitManager().get(this.kitName);
        if (kit == null) {
            return;
        }
        ItemStack[] armor = kit.getArmor();
        if (armorIdx >= armor.length || armor[armorIdx] == null || armor[armorIdx].getType() == Material.AIR) {
            player.sendMessage(Text.prefixed("&cThat kit has no armor in that slot."));
            return;
        }
        if (!Trims.available()) {
            player.sendMessage(Text.prefixed("&cCould not apply the trim (this server may not support armor trims)."));
            return;
        }
        this.plugin.getTrimPreferences().set(this.owner, this.kitName, armorIdx, Trims.PATTERN_KEYS[this.patternIndex], Trims.MATERIAL_KEYS[this.materialIndex]);
        player.sendMessage(Text.prefixed("&aApplied &f" + Trims.pretty(Trims.MATERIAL_KEYS[this.materialIndex]) + " " + Trims.pretty(Trims.PATTERN_KEYS[this.patternIndex]) + "&a trim &7(only you will wear it)."));
        this.reopen(player);
    }

    private void applyAll(Player player) {
        Kit kit = this.plugin.getKitManager().get(this.kitName);
        if (kit == null) {
            return;
        }
        if (!Trims.available()) {
            player.sendMessage(Text.prefixed("&cNo armor to trim (or trims unsupported)."));
            return;
        }
        this.plugin.getTrimPreferences().setAll(this.owner, this.kitName, Trims.PATTERN_KEYS[this.patternIndex], Trims.MATERIAL_KEYS[this.materialIndex]);
        player.sendMessage(Text.prefixed("&aApplied the trim to all your armor pieces &7(only you will wear it)."));
        this.reopen(player);
    }

    private void clearAll(Player player) {
        this.plugin.getTrimPreferences().clear(this.owner, this.kitName);
        player.sendMessage(Text.prefixed("&aCleared your trims for &f" + this.kitName + "&a."));
        this.reopen(player);
    }

    private void reopen(Player player) {
        this.build();
        player.openInventory(this.inventory);
    }
}


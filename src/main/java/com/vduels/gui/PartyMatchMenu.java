package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.gui.Menu;
import com.vduels.gui.PartyDuelBrowseMenu;
import com.vduels.gui.PartyFfaMenu;
import com.vduels.gui.PartySplitMenu;
import com.vduels.model.Party;
import com.vduels.util.Items;
import com.vduels.util.Text;
import java.util.List;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

public class PartyMatchMenu
extends Menu {
    private final VDuels plugin;
    private final Party party;

    public PartyMatchMenu(VDuels plugin, Party party) {
        this.plugin = plugin;
        this.party = party;
    }

    @Override
    public void build() {
        this.create(4, "&7PARTY &7\u2192 &7MATCH");
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; ++i) {
            this.inventory.setItem(i, filler);
        }
        this.inventory.setItem(11, this.modeItem(DyeColor.ORANGE, "PARTY FFA", "&7Start a free-for-all match.", "ffa", true));
        this.inventory.setItem(13, this.modeItem(DyeColor.LIGHT_BLUE, "PARTY DUEL", "&7Challenge another party.", "duel", true));
        this.inventory.setItem(15, this.modeItem(DyeColor.PURPLE, "PARTY SPLIT", "&7Split into red vs blue.", "split", true));
    }

    private ItemStack modeItem(DyeColor color, String label, String lore, String tag, boolean ready) {
        ItemStack stack = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta)stack.getItemMeta();
        meta.setColor(color.getColor());
        meta.setDisplayName(Text.color("&6&l" + label));
        meta.setLore(List.of(Text.color(""), Text.color(lore)));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_ENCHANTS);
        String buttonTag = "party-mode-" + tag;
        meta.getPersistentDataContainer().set(plugin.keyButton(), PersistentDataType.STRING, buttonTag);
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String tag = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if (tag == null) {
            return;
        }
        if (tag.equals("party-mode-ffa")) {
            new PartyFfaMenu(this.plugin, this.party, player).open(player);
        } else if (tag.equals("party-mode-duel")) {
            new PartyDuelBrowseMenu(this.plugin, this.party, player).open(player);
        } else if (tag.equals("party-mode-split")) {
            new PartySplitMenu(this.plugin, this.party, player).open(player);
        }
    }
}


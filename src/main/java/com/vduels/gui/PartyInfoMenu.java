package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.gui.Menu;
import com.vduels.model.Party;
import com.vduels.util.Items;
import com.vduels.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class PartyInfoMenu
extends Menu {
    private static final int[] HEAD_SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private final VDuels plugin;
    private final Party party;
    private final UUID viewerId;

    public PartyInfoMenu(VDuels plugin, Party party, Player viewer) {
        this.plugin = plugin;
        this.party = party;
        this.viewerId = viewer.getUniqueId();
    }

    @Override
    public void build() {
        this.create(4, "&7PARTY &7\u2192 &7SETTINGS");
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; ++i) {
            this.inventory.setItem(i, filler);
        }
        ArrayList<UUID> members = new ArrayList<UUID>(this.party.getMembers());
        for (int i = 0; i < HEAD_SLOTS.length && i < members.size(); ++i) {
            this.inventory.setItem(HEAD_SLOTS[i], this.headItem((UUID)members.get(i)));
        }
        boolean leader = this.party.isLeader(this.viewerId);
        this.inventory.setItem(31, leader ? this.disbandItem() : this.leaveItem());
    }

    private ItemStack headItem(UUID id) {
        OfflinePlayer op = Bukkit.getOfflinePlayer((UUID)id);
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta)stack.getItemMeta();
        meta.setOwningPlayer(op);
        boolean isLeader = this.party.isLeader(id);
        boolean isYou = id.equals(this.viewerId);
        String crown = isLeader ? "&e\u2605 " : "";
        String name = op.getName() == null ? "Unknown" : op.getName();
        meta.setDisplayName(Text.color(crown + "&f" + name + (isYou ? " &a(You)" : "")));
        meta.setLore(List.of(Text.color(isLeader ? "&7Party Leader" : "&7Party Member")));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack disbandItem() {
        return Items.of(Material.RED_DYE).name("&c&lDISBAND PARTY").lore("&7Click to disband the party.").hideTooltip().tag(this.plugin.keyButton(), "party-disband").build();
    }

    private ItemStack leaveItem() {
        return Items.of(Material.RED_DYE).name("&c&lLEAVE PARTY").lore("&7Click to leave the party.").hideTooltip().tag(this.plugin.keyButton(), "party-leave-menu").build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String tag = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if ("party-disband".equals(tag)) {
            player.closeInventory();
            this.plugin.getPartyManager().disband(player);
        } else if ("party-leave-menu".equals(tag)) {
            player.closeInventory();
            this.plugin.getPartyManager().leave(player);
        }
    }
}


package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.gui.Menu;
import com.vduels.gui.PartyKitPickMenu;
import com.vduels.gui.PartyMatchSettingsMenu;
import com.vduels.model.Party;
import com.vduels.model.Team;
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
import org.bukkit.persistence.PersistentDataType;

public class PartySplitMenu
extends Menu {
    private static final int[] ROW_SLOTS = new int[]{1, 2, 3, 4, 5, 6, 7};
    private final VDuels plugin;
    private final Party party;
    private final UUID viewerId;

    public PartySplitMenu(VDuels plugin, Party party, Player viewer) {
        this.plugin = plugin;
        this.party = party;
        this.viewerId = viewer.getUniqueId();
    }

    @Override
    public void build() {
        int i;
        this.create(5, "&7PARTY &7\u2192 &7SPLIT");
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i2 = 0; i2 < 45; ++i2) {
            this.inventory.setItem(i2, filler);
        }
        ArrayList<UUID> red = new ArrayList<UUID>();
        ArrayList<UUID> blue = new ArrayList<UUID>();
        ArrayList<UUID> unassigned = new ArrayList<UUID>();
        for (UUID id : this.party.getMembers()) {
            if (Bukkit.getPlayer((UUID)id) == null) continue;
            Team t = this.party.getTeam(id);
            if (t == Team.RED) {
                red.add(id);
                continue;
            }
            if (t == Team.BLUE) {
                blue.add(id);
                continue;
            }
            unassigned.add(id);
        }
        this.inventory.setItem(0, Items.of(Material.RED_STAINED_GLASS_PANE).name("&c&lRED TEAM").lore("&7" + red.size() + " player(s)").hideTooltip().build());
        for (i = 0; i < ROW_SLOTS.length && i < red.size(); ++i) {
            this.inventory.setItem(ROW_SLOTS[i], this.headItem((UUID)red.get(i), Team.RED));
        }
        this.inventory.setItem(9, Items.of(Material.BLUE_STAINED_GLASS_PANE).name("&9&lBLUE TEAM").lore("&7" + blue.size() + " player(s)").hideTooltip().build());
        for (i = 0; i < ROW_SLOTS.length && i < blue.size(); ++i) {
            this.inventory.setItem(9 + ROW_SLOTS[i], this.headItem((UUID)blue.get(i), Team.BLUE));
        }
        this.inventory.setItem(18, Items.of(Material.GRAY_STAINED_GLASS_PANE).name("&7&lUNASSIGNED").lore("&7" + unassigned.size() + " player(s)", "", "&7Won't fight until assigned").hideTooltip().build());
        for (i = 0; i < ROW_SLOTS.length && i < unassigned.size(); ++i) {
            this.inventory.setItem(18 + ROW_SLOTS[i], this.headItem((UUID)unassigned.get(i), Team.NONE));
        }
        this.inventory.setItem(31, this.shuffleItem());
        this.inventory.setItem(40, this.startItem());
    }

    private ItemStack headItem(UUID id, Team team) {
        OfflinePlayer op = Bukkit.getOfflinePlayer((UUID)id);
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta)stack.getItemMeta();
        meta.setOwningPlayer(op);
        boolean isLeader = this.party.isLeader(id);
        boolean isYou = id.equals(this.viewerId);
        String name = op.getName() == null ? "Unknown" : op.getName();
        String crown = isLeader ? "&e\u2605 " : "";
        meta.setDisplayName(Text.color(crown + "&f" + name + (isYou ? " &a(You)" : "")));
        meta.setLore(List.of(Text.color("&7Click to move to " + this.nextTeamLabel(team))));
        String tag = "party-split-member:" + id;
        meta.getPersistentDataContainer().set(this.plugin.keyButton(), PersistentDataType.STRING, tag);
        stack.setItemMeta(meta);
        return stack;
    }

    private String nextTeamLabel(Team current) {
        Team next = current == Team.NONE ? Team.RED : (current == Team.RED ? Team.BLUE : Team.NONE);
        return next == Team.RED ? "&cRED" : (next == Team.BLUE ? "&9BLUE" : "&7UNASSIGNED");
    }

    private ItemStack shuffleItem() {
        return Items.of(Material.COMPASS).name("&e&lSHUFFLE TEAMS").lore("&7Randomly split everyone", "&7into red and blue.", "", "&aClick to shuffle").hideTooltip().tag(this.plugin.keyButton(), "party-shuffle").build();
    }

    private ItemStack startItem() {
        return Items.of(Material.LIME_DYE).name("&a&lSTART SPLIT").lore("", "&7Pick a kit and begin.").hideTooltip().tag(this.plugin.keyButton(), "party-split-start").build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String tag = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if (tag == null) {
            return;
        }
        if (tag.equals("party-shuffle")) {
            if (!this.party.isLeader(player.getUniqueId())) {
                player.sendMessage(Text.color("&cOnly the party leader can shuffle teams."));
                return;
            }
            this.party.shuffleTeams();
            this.build();
            player.openInventory(this.inventory);
            return;
        }
        if (tag.equals("party-split-start")) {
            if (!this.party.isLeader(player.getUniqueId())) {
                player.sendMessage(Text.color("&cOnly the party leader can start a match."));
                return;
            }
            new PartyKitPickMenu(this.plugin, this.party, () -> new PartyMatchSettingsMenu(this.plugin, this.party, "START SPLIT", () -> {
                boolean started = this.plugin.getPartyManager().startSplit(player, this.party);
                if (!started) {
                    player.sendMessage(Text.color("&cCouldn't start - make sure both red and blue have at least one online player and an arena is free."));
                }
            }).open(player)).open(player);
            return;
        }
        if (tag.startsWith("party-split-member:")) {
            UUID id = UUID.fromString(tag.substring("party-split-member:".length()));
            if (!this.party.isLeader(player.getUniqueId()) && !id.equals(player.getUniqueId())) {
                player.sendMessage(Text.color("&cOnly the party leader can move other players between teams."));
                return;
            }
            this.party.cycleTeam(id);
            this.build();
            player.openInventory(this.inventory);
        }
    }
}


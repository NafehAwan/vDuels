package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.gui.Menu;
import com.vduels.gui.PartyKitPickMenu;
import com.vduels.gui.PartyMatchMenu;
import com.vduels.gui.PartyMatchSettingsMenu;
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

public class PartyFfaMenu
extends Menu {
    private static final int[] HEAD_SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private final VDuels plugin;
    private final Party party;
    private final UUID viewerId;

    public PartyFfaMenu(VDuels plugin, Party party, Player viewer) {
        this.plugin = plugin;
        this.party = party;
        this.viewerId = viewer.getUniqueId();
    }

    @Override
    public void build() {
        this.create(4, "&7PARTY &7\u2192 &7FFA");
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; ++i) {
            this.inventory.setItem(i, filler);
        }
        ArrayList<UUID> members = new ArrayList<UUID>(this.party.getMembers());
        for (int i = 0; i < HEAD_SLOTS.length && i < members.size(); ++i) {
            this.inventory.setItem(HEAD_SLOTS[i], this.headItem((UUID)members.get(i)));
        }
        this.inventory.setItem(27, this.backArrow());
        this.inventory.setItem(31, this.startButton());
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
        String suffix = isYou ? " &a(You)" : "";
        meta.setDisplayName(Text.color(crown + "&f" + name + suffix));
        meta.setLore(List.of(Text.color(isLeader ? "&7Party Leader" : "&7Party Member")));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack backArrow() {
        return Items.of(Material.ARROW).name("&7\u2190 Back").lore("&fBack to match selection.").tag(this.plugin.keyButton(), "party-back-match").build();
    }

    private ItemStack startButton() {
        return Items.of(Material.FEATHER).name("&a&lSTART FFA").lore("", "&fClick to choose a kit and start.").hideTooltip().tag(this.plugin.keyButton(), "party-start-ffa").build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String tag = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if (tag == null) {
            return;
        }
        if (tag.equals("party-back-match")) {
            new PartyMatchMenu(this.plugin, this.party).open(player);
        } else if (tag.equals("party-start-ffa")) {
            if (!this.party.isLeader(player.getUniqueId())) {
                player.sendMessage(Text.color("&cOnly the party leader can start a match."));
                return;
            }
            new PartyKitPickMenu(this.plugin, this.party, () -> new PartyMatchSettingsMenu(this.plugin, this.party, "START FFA", () -> {
                boolean started = this.plugin.getPartyManager().startFfa(player, this.party);
                if (!started) {
                    player.sendMessage(Text.color("&cCouldn't start the match - need at least 2 online party members and a free arena for this kit."));
                }
            }).open(player)).open(player);
        }
    }
}


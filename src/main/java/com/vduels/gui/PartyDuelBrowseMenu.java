package com.vduels.gui;

import com.vduels.VDuels;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

public class PartyDuelBrowseMenu extends Menu {

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private final VDuels plugin;
    private final Party party;
    private final UUID viewerId;

    public PartyDuelBrowseMenu(VDuels plugin, Party party, Player viewer) {
        this.plugin = plugin;
        this.party = party;
        this.viewerId = viewer.getUniqueId();
    }

    @Override
    public void build() {
        create(4, "&7PARTY DUELS");
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }
        List<Party> others = new ArrayList<>();
        for (Party p : plugin.getPartyManager().getAllParties()) {
            if (p == party || p.isBusy()) {
                continue;
            }
            others.add(p);
        }
        for (int i = 0; i < SLOTS.length && i < others.size(); i++) {
            inventory.setItem(SLOTS[i], partyIcon(others.get(i)));
        }
    }

    private ItemStack partyIcon(Party other) {
        OfflinePlayer leader = Bukkit.getOfflinePlayer(other.getLeader());
        String leaderName = leader.getName() == null ? "Unknown" : leader.getName();
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        meta.setOwningPlayer(leader);
        meta.setDisplayName(Text.color("&e" + leaderName + "'s Party"));
        List<String> lore = new ArrayList<>();
        lore.add(Text.color(""));
        lore.add(Text.color("&fPARTY LEADER: &e" + leaderName));
        lore.add(Text.color("&fMEMBERS: &e" + other.size()));
        lore.add(Text.color(""));
        lore.add(Text.color("&fMembers:"));
        for (UUID id : other.getMembers()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            lore.add(Text.color("&7- " + (op.getName() == null ? "Unknown" : op.getName())));
        }
        lore.add(Text.color(""));
        lore.add(Text.color("&aClick to challenge"));
        meta.setLore(lore);
        String tag = "party-duel-target:" + other.getLeader();
        meta.getPersistentDataContainer().set(plugin.keyButton(), PersistentDataType.STRING, tag);
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String tag = Items.readTag(event.getCurrentItem(), plugin.keyButton());
        if (tag == null || !tag.startsWith("party-duel-target:")) {
            return;
        }
        if (!party.isLeader(viewerId)) {
            player.sendMessage(Text.color("&cOnly the party leader can send a challenge."));
            return;
        }
        UUID targetLeaderId = UUID.fromString(tag.substring("party-duel-target:".length()));
        new PartyKitPickMenu(plugin, party, () -> new PartyMatchSettingsMenu(plugin, party, "SEND CHALLENGE", () -> {
            boolean sent = plugin.getPartyManager().sendPartyDuelChallenge(player, party, targetLeaderId,
                    party.getSelectedKit(), party.getRounds(), party.isHealthIndicator(), party.isAllowDrops());
            if (!sent) {
                player.sendMessage(Text.color("&cCouldn't send that challenge - the other party may be busy, gone, or offline."));
            }
        }).open(player)).open(player);
    }
}

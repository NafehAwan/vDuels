/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.ItemStack
 */
package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.model.ActiveDuel;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class MatchSummaryMenu
extends Menu {
    private static final String REMATCH = "summary-rematch";
    private final MeowDuels plugin;
    private final ActiveDuel duel;
    private final UUID winnerId;
    private final UUID viewerId;

    public MatchSummaryMenu(MeowDuels plugin, ActiveDuel duel, UUID winnerId, UUID viewerId) {
        this.plugin = plugin;
        this.duel = duel;
        this.winnerId = winnerId;
        this.viewerId = viewerId;
    }

    @Override
    public void build() {
        this.create(3, "\u1d0d\u1d00\u1d1b\u1d04\u029c \u0280\u1d07\u0455\u1d1c\u029f\u1d1b");
        ItemStack filler = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; ++i) {
            this.inventory.setItem(i, filler);
        }
        UUID me = this.viewerId;
        UUID opp = this.duel.getOpponent(me);
        boolean won = me.equals(this.winnerId);
        this.inventory.setItem(11, Items.of(won ? Material.LIME_DYE : Material.RED_DYE).miniName(won ? "<bold><gradient:#8BF58B:#2EB82E>\u1d20\u026a\u1d04\u1d1b\u1d0f\u0280\u028f</gradient></bold>" : "<bold><gradient:#FF6B6B:#8B0000>\u1d05\u1d07\ua730\u1d07\u1d00\u1d1b</gradient></bold>").lore("&7You: &f" + this.name(me), "", "&7Score: &f" + this.duel.getScoreFor(me), "&7Hits: &f" + this.duel.getHits(me), "&7Damage: &f" + MatchSummaryMenu.fmt(this.duel.getDamageDealt(me)) + " &c\u2764", "", "&7Streak: &6" + this.plugin.getStatsManager().getStreak(me), "&7ELO: &b" + this.plugin.getStatsManager().getElo(me) + (this.duel.isRanked() ? " &8(ranked)" : " &8(casual)")).hideTooltip().build());
        this.inventory.setItem(13, Items.of(Material.PAPER).miniName("<gradient:#FFE08A:#E0A13B>\u1d0d\u1d00\u1d1b\u1d04\u029c \u026a\u0274\ua730\u1d0f</gradient>").lore("&7Kit: &f" + this.kitLabel(), "&7Final: &a" + this.duel.getScoreFor(me) + " &7- &c" + this.duel.getScoreFor(opp), "&7Rounds: &f" + this.duel.getRoundsToWin() + " to win", "&7Duration: &f" + this.duration()).hideTooltip().build());
        this.inventory.setItem(15, Items.of(Material.PLAYER_HEAD).miniName("<gradient:#C0C0C0:#6E6E6E>\u1d0f\u1d18\u1d18\u1d0f\u0274\u1d07\u0274\u1d1b</gradient>").lore("&7" + this.name(opp), "", "&7Score: &f" + this.duel.getScoreFor(opp), "&7Hits: &f" + this.duel.getHits(opp), "&7Damage: &f" + MatchSummaryMenu.fmt(this.duel.getDamageDealt(opp)) + " &c\u2764").hideTooltip().build());
        this.inventory.setItem(22, Items.of(Material.NETHER_STAR).miniName("<bold><gradient:#FF5C5C:#8B0000>\u0280\u1d07\u1d0d\u1d00\u1d1b\u1d04\u029c \u2694\ufe0f</gradient></bold>").lore("&7Challenge &f" + this.name(opp) + " &7again", "", "&eClick to send a rematch.").hideTooltip().tag(this.plugin.keyButton(), REMATCH).build());
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String btn = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if (REMATCH.equals(btn)) {
            player.closeInventory();
            this.plugin.getDuelManager().requestRematch(player);
        }
    }

    private String kitLabel() {
        Kit kit = this.plugin.getKitManager().get(this.duel.getKit());
        if (kit != null && kit.getDisplayName() != null && !kit.getDisplayName().isEmpty()) {
            return kit.getDisplayName();
        }
        return this.duel.getKit();
    }

    private String duration() {
        long seconds = Math.max(0L, (System.currentTimeMillis() - this.duel.getStartedAt()) / 1000L);
        return String.format("%02d:%02d", seconds / 60L, seconds % 60L);
    }

    private String name(UUID id) {
        if (id == null) {
            return "?";
        }
        Player online = Bukkit.getPlayer((UUID)id);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer off = Bukkit.getOfflinePlayer((UUID)id);
        return off.getName() == null ? "?" : off.getName();
    }

    private static String fmt(double d) {
        return String.format(Locale.US, "%.1f", d);
    }
}


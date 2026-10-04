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
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.model.ActiveDuel;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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

    /** The name of this screen's file: {@code gui/matchsummary.yml}. */
    private static final String MENU = "matchsummary";

    private static final int ROWS = 3;
    private static final int SLOT_YOU = 11;
    private static final int SLOT_MATCH = 13;
    private static final int SLOT_OPPONENT = 15;
    private static final int SLOT_REMATCH = 22;

    @Override
    public void build() {
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        int size = rows * 9;
        this.createRaw(rows, gui.title(MENU, "\u1d0d\u1d00\u1d1b\u1d04\u029c \u0280\u1d07\u0455\u1d1c\u029f\u1d1b"));
        gui.frame(MENU, this.inventory, rows);
        UUID me = this.viewerId;
        UUID opp = this.duel.getOpponent(me);
        boolean won = me.equals(this.winnerId);
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("you", this.name(me));
        tokens.put("opponent", this.name(opp));
        tokens.put("kit", this.kitLabel());
        tokens.put("score", String.valueOf(this.duel.getScoreFor(me)));
        tokens.put("oppscore", String.valueOf(this.duel.getScoreFor(opp)));
        tokens.put("hits", String.valueOf(this.duel.getHits(me)));
        tokens.put("opphits", String.valueOf(this.duel.getHits(opp)));
        tokens.put("damage", MatchSummaryMenu.fmt(this.duel.getDamageDealt(me)));
        tokens.put("oppdamage", MatchSummaryMenu.fmt(this.duel.getDamageDealt(opp)));
        tokens.put("rounds", String.valueOf(this.duel.getRoundsToWin()));
        tokens.put("duration", this.duration());
        tokens.put("streak", String.valueOf(this.plugin.getStatsManager().getStreak(me)));
        tokens.put("wins", String.valueOf(this.plugin.getStatsManager().getWins(me)));
        // The four panel titles stay MiniMessage rather than becoming section
        // strings: they are gradients, and miniName also clears the italic a
        // component display name carries by default.
        Items you = won
                ? Items.of(gui.material(MENU, "result-won", Material.LIME_DYE))
                        .miniName(gui.miniName(MENU, "result-won",
                                "<bold><gradient:#8BF58B:#2EB82E>\u1d20\u026a\u1d04\u1d1b\u1d0f\u0280\u028f</gradient></bold>", tokens))
                        .rawLore(gui.lore(MENU, "result-won", tokens, RESULT_LORE).toArray(new String[0]))
                : Items.of(gui.material(MENU, "result-lost", Material.RED_DYE))
                        .miniName(gui.miniName(MENU, "result-lost",
                                "<bold><gradient:#FF6B6B:#8B0000>\u1d05\u1d07\ua730\u1d07\u1d00\u1d1b</gradient></bold>", tokens))
                        .rawLore(gui.lore(MENU, "result-lost", tokens, RESULT_LORE).toArray(new String[0]));
        int resultSlot = won
                ? gui.slot(MENU, "result-won", SLOT_YOU, rows)
                : gui.slot(MENU, "result-lost", SLOT_YOU, rows);
        this.inventory.setItem(Math.min(size - 1, resultSlot), you.hideTooltip().build());
        this.inventory.setItem(Math.min(size - 1, gui.slot(MENU, "match", SLOT_MATCH, rows)),
                Items.of(gui.material(MENU, "match", Material.PAPER))
                        .miniName(gui.miniName(MENU, "match",
                                "<gradient:#FFE08A:#E0A13B>\u1d0d\u1d00\u1d1b\u1d04\u029c \u026a\u0274\ua730\u1d0f</gradient>", tokens))
                        .rawLore(gui.lore(MENU, "match", tokens,
                                "&7\u1d0b\u026a\u1d1b: &f{kit}",
                                "&7\ua730\u026a\u0274\u1d00\u029f: &a{score} &7- &c{oppscore}",
                                "&7\u0280\u1d0f\u1d1c\u0274\u1d05\ua731: &f{rounds} \u1d1b\u1d0f \u1d21\u026a\u0274",
                                "&7\u1d05\u1d1c\u0280\u1d00\u1d1b\u026a\u1d0f\u0274: &f{duration}").toArray(new String[0]))
                        .hideTooltip().build());
        this.inventory.setItem(Math.min(size - 1, gui.slot(MENU, "opponent", SLOT_OPPONENT, rows)),
                Items.of(gui.material(MENU, "opponent", Material.PLAYER_HEAD))
                        .miniName(gui.miniName(MENU, "opponent",
                                "<gradient:#C0C0C0:#6E6E6E>\u1d0f\u1d18\u1d18\u1d0f\u0274\u1d07\u0274\u1d1b</gradient>", tokens))
                        .rawLore(gui.lore(MENU, "opponent", tokens,
                                "&7{opponent}",
                                "",
                                "&7\ua731\u1d04\u1d0f\u0280\u1d07: &f{oppscore}",
                                "&7\u029c\u026a\u1d1b\ua731: &f{opphits}",
                                "&7\u1d05\u1d00\u1d0d\u1d00\u0262\u1d07: &f{oppdamage} &c\u2764").toArray(new String[0]))
                        .hideTooltip().build());
        this.inventory.setItem(Math.min(size - 1, gui.slot(MENU, "rematch", SLOT_REMATCH, rows)),
                Items.of(gui.material(MENU, "rematch", Material.NETHER_STAR))
                        .miniName(gui.miniName(MENU, "rematch",
                                "<bold><gradient:#FF5C5C:#8B0000>\u0280\u1d07\u1d0d\u1d00\u1d1b\u1d04\u029c \u2694\ufe0f</gradient></bold>", tokens))
                        .rawLore(gui.lore(MENU, "rematch", tokens,
                                "&7\u1d04\u029c\u1d00\u029f\u029f\u1d07\u0274\u0262\u1d07 &f{opponent} &7\u1d00\u0262\u1d00\u026a\u0274",
                                "",
                                "&e\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \ua731\u1d07\u0274\u1d05 \u1d00 \u0280\u1d07\u1d0d\u1d00\u1d1b\u1d04\u029c.").toArray(new String[0]))
                        .hideTooltip().tag(this.plugin.keyButton(), REMATCH).build());
    }

    /**
     * The viewer's own column, which reads the same whether they won or lost.
     *
     * <p>Shared by both result items so a server rewording it does not have
     * to remember to do it twice and leave the losing panel out of step.
     */
    private static final String[] RESULT_LORE = new String[]{
            "&7\u028f\u1d0f\u1d1c: &f{you}",
            "",
            "&7\ua731\u1d04\u1d0f\u0280\u1d07: &f{score}",
            "&7\u029c\u026a\u1d1b\ua731: &f{hits}",
            "&7\u1d05\u1d00\u1d0d\u1d00\u0262\u1d07: &f{damage} &c\u2764",
            "",
            "&7\ua731\u1d1b\u0280\u1d07\u1d00\u1d0b: &6{streak}",
            "&7\u1d21\u026a\u0274\ua731: &b{wins}"};

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


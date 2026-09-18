/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 */
package com.meowduels.util;

import com.meowduels.MeowDuels;
import com.meowduels.util.Items;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class SpawnItems {
    public static final String QUEUE = "spawn-queue";
    public static final String QUICK = "spawn-quick";
    public static final String LEAVEQUEUE = "spawn-leavequeue";
    public static final String KITEDITOR = "spawn-kiteditor";
    public static final String SETTINGS = "spawn-settings";
    public static final String REMATCH = "spawn-rematch";
    public static final String CREATEPARTY = "spawn-createparty";
    public static final String PARTYMATCH = "party-match";
    public static final String PARTYSPECTATE = "party-spectate";
    public static final String PARTYLEAVE = "party-leave";
    public static final String PARTYSETTINGS = "party-settings";
    private static final int LEAVE_SLOT = 5;
    /** Moved off slot 1 to make room for Create Party; the rematch head is
     *  transient and the party button is always there, so the party button gets
     *  the slot that never changes meaning. */
    private static final int REMATCH_SLOT = 2;

    private SpawnItems() {
    }

    /**
     * The spawn hotbar.
     *
     * <p>There are two of them. Someone in a party gets the party bar instead of
     * the normal one, because every button on the normal bar is something a
     * party member is not allowed to do - handing them a queue sword that always
     * answers "you're in a party" is worse than not handing it over at all.
     */
    public static void give(MeowDuels plugin, Player player) {
        if (player == null) {
            return;
        }
        player.getInventory().clear();
        if (plugin.getPartyManager() != null && plugin.getPartyManager().inParty(player.getUniqueId())) {
            SpawnItems.giveParty(plugin, player);
            return;
        }
        player.getInventory().setItem(0, Items.of(Material.DIAMOND_SWORD).miniName("<gradient:#FF6B6B:#A01028>\u2694 \u0280\u1d00\u0274\u1d0b\u1d07\u1d05 \u01eb\u1d1c\u1d07\u1d1c\u1d07</gradient>").hideTooltip().tag(plugin.keyButton(), QUEUE).build());
        player.getInventory().setItem(1, Items.of(Material.NETHER_STAR).miniName("<gradient:#B98CFF:#6B4BD6>\u271a \u1d04\u0280\u1d07\u1d00\u1d1b\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f</gradient>").rawLore("", "<#8E959D>\u0280\u026a\u0262\u029c\u1d1b-\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \ua731\u1d1b\u1d00\u0280\u1d1b \u1d00 \u1d18\u1d00\u0280\u1d1b\u028f").hideTooltip().tag(plugin.keyButton(), CREATEPARTY).build());
        player.getInventory().setItem(4, Items.of(Material.CHEST).miniName("<gradient:#FFDE8A:#E0872B>\u26a1 \u01eb\u1d1c\u026a\u1d04\u1d0b \u01eb\u1d1c\u1d07\u1d1c\u1d07</gradient>").hideTooltip().tag(plugin.keyButton(), QUICK).build());
        player.getInventory().setItem(7, Items.of(Material.BOOK).miniName("<gradient:#8AD9FF:#3F6BE0>\ud83d\udcd6 \u1d0b\u026a\u1d1b \u1d07\u1d05\u026a\u1d1b\u1d0f\u0280</gradient>").hideTooltip().tag(plugin.keyButton(), KITEDITOR).build());
        player.getInventory().setItem(8, Items.of(Material.GRINDSTONE).miniName("<gradient:#C8C8C8:#707070>\u2699 \u0455\u1d07\u1d1b\u1d1b\u026a\u0274\u0262\u0455</gradient>").hideTooltip().tag(plugin.keyButton(), SETTINGS).build());
        if (plugin.getQueueManager().isQueued(player.getUniqueId())) {
            player.getInventory().setItem(5, SpawnItems.leaveQueueItem(plugin));
        }
        player.updateInventory();
    }

    /** The party hotbar: match, spectate, leave, settings. */
    private static void giveParty(MeowDuels plugin, Player player) {
        boolean leader = plugin.getPartyManager().partyOf(player.getUniqueId()) != null
                && plugin.getPartyManager().partyOf(player.getUniqueId()).isLeader(player.getUniqueId());
        player.getInventory().setItem(0, Items.of(Material.DIAMOND_SWORD)
                .miniName("<gradient:#FF8AD0:#B04BD6>\u2694 \u1d18\u1d00\u0280\u1d1b\u028f \u1d0d\u1d00\u1d1b\u1d04\u029c</gradient>")
                .rawLore("", leader
                        ? "<#8E959D>\u0280\u026a\u0262\u029c\u1d1b-\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \ua731\u1d07\u1d1b \u1d1c\u1d18 \u1d1b\u029c\u1d07 \u1d0d\u1d00\u1d1b\u1d04\u029c"
                        : "<#6B7079>\u1d0f\u0274\u029f\u028f \u1d1b\u029c\u1d07 \u029f\u1d07\u1d00\u1d05\u1d07\u0280 \u1d04\u1d00\u0274 \ua731\u1d1b\u1d00\u0280\u1d1b")
                .hideTooltip().tag(plugin.keyButton(), PARTYMATCH).build());
        player.getInventory().setItem(1, Items.of(Material.ENDER_PEARL)
                .miniName("<gradient:#8AD9FF:#3F6BE0>\u25c9 \u1d18\u1d00\u0280\u1d1b\u028f \ua731\u1d18\u1d07\u1d04\u1d1b\u1d00\u1d1b\u1d07</gradient>")
                .rawLore("", "<#8E959D>\u1d21\u1d00\u1d1b\u1d04\u029c \u028f\u1d0f\u1d1c\u0280 \u1d18\u1d00\u0280\u1d1b\u028f'\ua731 \u1d0d\u1d00\u1d1b\u1d04\u029c",
                          "<dark_gray>\u25b8 <#8E959D>/leave \u1d1b\u1d0f \ua731\u1d1b\u1d0f\u1d18")
                .hideTooltip().tag(plugin.keyButton(), PARTYSPECTATE).build());
        player.getInventory().setItem(7, Items.of(Material.RED_DYE)
                .miniName(leader
                        ? "<gradient:#FF6B6B:#A01028>\u2716 \u1d05\u026a\ua731\u0299\u1d00\u0274\u1d05 \u1d18\u1d00\u0280\u1d1b\u028f</gradient>"
                        : "<gradient:#FF6B6B:#A01028>\u2716 \u029f\u1d07\u1d00\u1d20\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f</gradient>")
                .rawLore("", leader
                        ? "<#8E959D>\u1d1b\u029c\u026a\ua731 \u1d07\u0274\u1d05\ua731 \u1d1b\u029c\u1d07 \u1d18\u1d00\u0280\u1d1b\u028f \ua730\u1d0f\u0280 \u1d07\u1d20\u1d07\u0280\u028f\u1d0f\u0274\u1d07"
                        : "<#8E959D>\u028f\u1d0f\u1d1c \u1d0b\u1d07\u1d07\u1d18 \u1d18\u029f\u1d00\u028f\u026a\u0274\u0262 \u1d00\u029f\u1d0f\u0274\u1d07",
                        "", "<dark_gray>\u25b8 <#8E959D>\u0280\u026a\u0262\u029c\u1d1b-\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d04\u1d0f\u0274\ua730\u026a\u0280\u1d0d")
                .hideTooltip().tag(plugin.keyButton(), PARTYLEAVE).build());
        player.getInventory().setItem(8, Items.of(Material.GRINDSTONE)
                .miniName("<gradient:#C8C8C8:#707070>\u2699 \u1d18\u1d00\u0280\u1d1b\u028f \ua731\u1d07\u1d1b\u1d1b\u026a\u0274\u0262\ua731</gradient>")
                .hideTooltip().tag(plugin.keyButton(), PARTYSETTINGS).build());
        player.updateInventory();
    }

    public static void addRematch(MeowDuels plugin, Player player, UUID opponentId, String opponentName) {
        if (player == null) {
            return;
        }
        ItemStack head = Items.of(Material.PLAYER_HEAD).miniName("<gradient:#FF5C5C:#8B0000><bold>\u0280\u1d07\u1d0d\u1d00\u1d1b\u1d04\u029c \u2694\ufe0f</bold></gradient>").lore("&7Right-click to challenge", "&f" + opponentName + " &7again.").hideTooltip().tag(plugin.keyButton(), REMATCH).build();
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta) {
            try {
                ((SkullMeta)meta).setOwningPlayer(Bukkit.getOfflinePlayer((UUID)opponentId));
                head.setItemMeta(meta);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        player.getInventory().setItem(REMATCH_SLOT, head);
        player.updateInventory();
    }

    public static void setLeaveQueue(MeowDuels plugin, Player player, boolean show) {
        if (player == null) {
            return;
        }
        player.getInventory().setItem(5, show ? SpawnItems.leaveQueueItem(plugin) : null);
        player.updateInventory();
    }

    private static ItemStack leaveQueueItem(MeowDuels plugin) {
        return Items.of(Material.RED_DYE).miniName("<gradient:#FF8A8A:#C0392B>\u2716 \u029f\u1d07\u1d00\u1d20\u1d07 \u01eb\u1d1c\u1d07\u1d1c\u1d07</gradient>").hideTooltip().tag(plugin.keyButton(), LEAVEQUEUE).build();
    }
}


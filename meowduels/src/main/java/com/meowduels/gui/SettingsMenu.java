package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.gui.Style;
import com.meowduels.gui.TrimKitMenu;
import com.meowduels.util.Items;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Your own settings, four rows so the toggles have room to breathe.
 *
 * <p>One row of switches on a framed panel - seven of them, which is exactly
 * how many fit between the frame's two columns. Every switch says what it is,
 * what it currently is, and what happens if you flip it, in that order,
 * because that is the order you read them in when you are deciding.
 */
public class SettingsMenu
extends Menu {
    private static final int ROWS = 4;

    private static final String DUEL = "set-duelreq";
    private static final String BOARD = "set-scoreboard";
    private static final String INVITES = "set-invites";
    private static final String SPECTATORS = "set-spectators";
    private static final String SOUNDS = "set-sounds";
    private static final String CHAT = "set-chat";
    private static final String TRIMS = "set-trims";

    private static final int SLOT_DUEL = 10;
    private static final int SLOT_INVITES = 11;
    private static final int SLOT_SPECTATORS = 12;
    private static final int SLOT_CHAT = 13;
    private static final int SLOT_BOARD = 14;
    private static final int SLOT_SOUNDS = 15;
    private static final int SLOT_TRIMS = 16;

    private final MeowDuels plugin;
    private final UUID owner;

    public SettingsMenu(MeowDuels plugin, Player player) {
        this.plugin = plugin;
        this.owner = player.getUniqueId();
    }

    @Override
    public void build() {
        this.createRaw(ROWS, Style.title("#A0E9FF", "#4C7DF0",
                "ꜱᴇᴛᴛɪɴɢꜱ"));
        Style.frame(this.inventory, ROWS);

        this.inventory.setItem(SLOT_DUEL, this.toggle(Material.IRON_SWORD, DUEL,
                "ᴅᴜᴇʟ ʀᴇǫᴜᴇꜱᴛꜱ",
                this.plugin.getPlayerSettings().isDuelRequests(this.owner),
                "ᴀɴʏᴏɴᴇ ᴄᴀɴ ᴄʜᴀʟʟᴇɴɢᴇ ʏᴏᴜ",
                "ᴄʜᴀʟʟᴇɴɢᴇꜱ ᴀʀᴇ ʙʟᴏᴄᴋᴇᴅ"));

        this.inventory.setItem(SLOT_INVITES, this.toggle(Material.PLAYER_HEAD, INVITES,
                "ᴘᴀʀᴛʏ ɪɴᴠɪᴛᴇꜱ",
                this.plugin.getPlayerSettings().isPartyInvites(this.owner),
                "ᴀɴʏᴏɴᴇ ᴄᴀɴ ɪɴᴠɪᴛᴇ ʏᴏᴜ",
                "ɪɴᴠɪᴛᴇꜱ ᴀʀᴇ ʙʟᴏᴄᴋᴇᴅ"));

        this.inventory.setItem(SLOT_SPECTATORS, this.toggle(Material.ENDER_EYE, SPECTATORS,
                "ꜱᴘᴇᴄᴛᴀᴛᴏʀꜱ",
                this.plugin.getPlayerSettings().isSpectators(this.owner),
                "ᴏᴛʜᴇʀꜱ ᴄᴀɴ ᴡᴀᴛᴄʜ ʏᴏᴜʀ ᴅᴜᴇʟꜱ",
                "ʏᴏᴜʀ ᴅᴜᴇʟꜱ ᴀʀᴇ ᴘʀɪᴠᴀᴛᴇ"));

        this.inventory.setItem(SLOT_CHAT, this.toggle(Material.OAK_SIGN, CHAT,
                "ɪꜱᴏʟᴀᴛᴇᴅ ᴄʜᴀᴛ",
                this.plugin.getPlayerSettings().isIsolatedChat(this.owner),
                "ɪɴ ᴀ ᴍᴀᴛᴄʜ, ᴏɴʟʏ ᴛʜᴇ ᴍᴀᴛᴄʜ ᴛᴀʟᴋꜱ",
                "ʏᴏᴜ ꜱᴇᴇ ᴀɴᴅ ᴊᴏɪɴ ᴛʜᴇ ᴡʜᴏʟᴇ ꜱᴇʀᴠᴇʀ"));

        this.inventory.setItem(SLOT_BOARD, this.toggle(Material.PAPER, BOARD,
                "ꜱᴄᴏʀᴇʙᴏᴀʀᴅ",
                this.plugin.getPlayerSettings().isScoreboard(this.owner),
                "ᴛʜᴇ ꜱɪᴅᴇʙᴀʀ ɪꜱ ᴏɴ",
                "ᴛʜᴇ ꜱɪᴅᴇʙᴀʀ ɪꜱ ʜɪᴅᴅᴇɴ"));

        this.inventory.setItem(SLOT_SOUNDS, this.toggle(Material.NOTE_BLOCK, SOUNDS,
                "ᴍᴇɴᴜ ꜱᴏᴜɴᴅꜱ",
                this.plugin.getPlayerSettings().isSounds(this.owner),
                "ᴄʟɪᴄᴋꜱ, ᴄᴏᴜɴᴛᴅᴏᴡɴꜱ ᴀɴᴅ ᴄᴜᴇꜱ",
                "ᴍᴇᴏᴡᴅᴜᴇʟꜱ ᴍᴀᴋᴇꜱ ɴᴏ ꜱᴏᴜɴᴅ"));

        Player p = Bukkit.getPlayer((UUID)this.owner);
        if (p != null && p.hasPermission("meowduels.trims")) {
            this.inventory.setItem(SLOT_TRIMS, Items.of(Material.LEATHER_CHESTPLATE)
                    .rawName(Style.VALUE + "ᴋɪᴛ ᴛʀɪᴍꜱ")
                    .rawLore("",
                             Style.LABEL + "ꜱᴇᴛ ʏᴏᴜʀ ᴀʀᴍᴏᴜʀ ᴛʀɪᴍ ᴘᴇʀ ᴋɪᴛ",
                             "",
                             Style.HINT + "ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ")
                    .hideTooltip().tag(this.plugin.keyButton(), TRIMS).build());
        }
    }

    /**
     * One switch.
     *
     * <p>Name plain, state in green or red with a tick or a cross, then the
     * hint. No gradient: five gradients in a row is a fruit salad, and the
     * thing that should catch your eye here is which ones are off.
     */
    private ItemStack toggle(Material icon, String id, String name, boolean on,
                             String whenOn, String whenOff) {
        return Items.of(icon)
                .rawName(Style.VALUE + name)
                .rawLore("",
                         Style.state(on, whenOn, whenOff),
                         "",
                         Style.HINT + "ᴄʟɪᴄᴋ ᴛᴏ "
                                 + (on ? "ᴛᴜʀɴ ᴏꜰꜰ" : "ᴛᴜʀɴ ᴏɴ"))
                .glow(on)
                .hideTooltip()
                .tag(this.plugin.keyButton(), id)
                .build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String btn = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if (btn == null) {
            return;
        }
        if (TRIMS.equals(btn)) {
            new TrimKitMenu(this.plugin).open(player);
            return;
        }
        String key = SettingsMenu.keyFor(btn);
        if (key == null) {
            return;
        }
        this.plugin.getPlayerSettings().toggle(key, this.owner);
        if (BOARD.equals(btn)) {
            // The sidebar is drawn from this, so it has to be told at once
            // rather than on the next tick - the menu is still open and the
            // board is visible behind it.
            this.plugin.getScoreboardService().handleJoin(player);
        }
        this.build();
        player.openInventory(this.inventory);
    }

    private static String keyFor(String button) {
        if (DUEL.equals(button)) {
            return "duel-requests";
        }
        if (BOARD.equals(button)) {
            return "scoreboard";
        }
        if (INVITES.equals(button)) {
            return "party-invites";
        }
        if (SPECTATORS.equals(button)) {
            return "spectators";
        }
        if (SOUNDS.equals(button)) {
            return "sounds";
        }
        if (CHAT.equals(button)) {
            return "isolated-chat";
        }
        return null;
    }
}

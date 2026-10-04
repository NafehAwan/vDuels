package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.gui.Style;
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.managers.GuiConfigManager.Face;
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

    /** The name of this screen's file: {@code gui/settings.yml}. */
    private static final String MENU = "settings";

    /**
     * The two lines every switch ends with, as theme placeholders.
     *
     * <p>Written {@code {hint}} rather than with the colour spelled out so a
     * server retinting {@code theme:} in config.yml retints these too - the
     * house style here is composed from the palette, not typed.
     */
    private static final String OFF_HINT = "{hint}ᴄʟɪᴄᴋ ᴛᴏ ᴛᴜʀɴ ᴏɴ";
    private static final String ON_HINT = "{hint}ᴄʟɪᴄᴋ ᴛᴏ ᴛᴜʀɴ ᴏꜰꜰ";

    @Override
    public void build() {
        GuiConfigManager gui = this.plugin.gui();
        int rows = gui.rows(MENU, ROWS);
        int size = rows * 9;
        this.createRaw(rows, gui.title(MENU, "<dark_gray>▏ <gradient:#A0E9FF:#4C7DF0>ꜱᴇᴛᴛɪɴɢꜱ</gradient>"));
        gui.frame(MENU, this.inventory, rows);

        this.put(rows, gui.slot(MENU, "duel-requests", SLOT_DUEL, rows), DUEL,
                 gui.toggle(MENU, "duel-requests",
                         this.plugin.getPlayerSettings().isDuelRequests(this.owner), null,
                         Face.of(Material.IRON_SWORD, "{value}ᴅᴜᴇʟ ʀᴇǫᴜᴇꜱᴛꜱ",
                                 "", "{good}{tick}ᴀɴʏᴏɴᴇ ᴄᴀɴ ᴄʜᴀʟʟᴇɴɢᴇ ʏᴏᴜ", "", ON_HINT).glow(),
                         Face.of(Material.IRON_SWORD, "{value}ᴅᴜᴇʟ ʀᴇǫᴜᴇꜱᴛꜱ",
                                 "", "{bad}{cross}ᴄʜᴀʟʟᴇɴɢᴇꜱ ᴀʀᴇ ʙʟᴏᴄᴋᴇᴅ", "", OFF_HINT)));

        this.put(rows, gui.slot(MENU, "party-invites", SLOT_INVITES, rows), INVITES,
                 gui.toggle(MENU, "party-invites",
                         this.plugin.getPlayerSettings().isPartyInvites(this.owner), null,
                         Face.of(Material.PLAYER_HEAD, "{value}ᴘᴀʀᴛʏ ɪɴᴠɪᴛᴇꜱ",
                                 "", "{good}{tick}ᴀɴʏᴏɴᴇ ᴄᴀɴ ɪɴᴠɪᴛᴇ ʏᴏᴜ", "", ON_HINT).glow(),
                         Face.of(Material.PLAYER_HEAD, "{value}ᴘᴀʀᴛʏ ɪɴᴠɪᴛᴇꜱ",
                                 "", "{bad}{cross}ɪɴᴠɪᴛᴇꜱ ᴀʀᴇ ʙʟᴏᴄᴋᴇᴅ", "", OFF_HINT)));

        this.put(rows, gui.slot(MENU, "spectators", SLOT_SPECTATORS, rows), SPECTATORS,
                 gui.toggle(MENU, "spectators",
                         this.plugin.getPlayerSettings().isSpectators(this.owner), null,
                         Face.of(Material.ENDER_EYE, "{value}ꜱᴘᴇᴄᴛᴀᴛᴏʀꜱ",
                                 "", "{good}{tick}ᴏᴛʜᴇʀꜱ ᴄᴀɴ ᴡᴀᴛᴄʜ ʏᴏᴜʀ ᴅᴜᴇʟꜱ", "", ON_HINT).glow(),
                         Face.of(Material.ENDER_EYE, "{value}ꜱᴘᴇᴄᴛᴀᴛᴏʀꜱ",
                                 "", "{bad}{cross}ʏᴏᴜʀ ᴅᴜᴇʟꜱ ᴀʀᴇ ᴘʀɪᴠᴀᴛᴇ", "", OFF_HINT)));

        this.put(rows, gui.slot(MENU, "isolated-chat", SLOT_CHAT, rows), CHAT,
                 gui.toggle(MENU, "isolated-chat",
                         this.plugin.getPlayerSettings().isIsolatedChat(this.owner), null,
                         Face.of(Material.OAK_SIGN, "{value}ɪꜱᴏʟᴀᴛᴇᴅ ᴄʜᴀᴛ",
                                 "", "{good}{tick}ɪɴ ᴀ ᴍᴀᴛᴄʜ, ᴏɴʟʏ ᴛʜᴇ ᴍᴀᴛᴄʜ ᴛᴀʟᴋꜱ", "", ON_HINT).glow(),
                         Face.of(Material.OAK_SIGN, "{value}ɪꜱᴏʟᴀᴛᴇᴅ ᴄʜᴀᴛ",
                                 "", "{bad}{cross}ʏᴏᴜ ꜱᴇᴇ ᴀɴᴅ ᴊᴏɪɴ ᴛʜᴇ ᴡʜᴏʟᴇ ꜱᴇʀᴠᴇʀ", "", OFF_HINT)));

        this.put(rows, gui.slot(MENU, "scoreboard", SLOT_BOARD, rows), BOARD,
                 gui.toggle(MENU, "scoreboard",
                         this.plugin.getPlayerSettings().isScoreboard(this.owner), null,
                         Face.of(Material.PAPER, "{value}ꜱᴄᴏʀᴇʙᴏᴀʀᴅ",
                                 "", "{good}{tick}ᴛʜᴇ ꜱɪᴅᴇʙᴀʀ ɪꜱ ᴏɴ", "", ON_HINT).glow(),
                         Face.of(Material.PAPER, "{value}ꜱᴄᴏʀᴇʙᴏᴀʀᴅ",
                                 "", "{bad}{cross}ᴛʜᴇ ꜱɪᴅᴇʙᴀʀ ɪꜱ ʜɪᴅᴅᴇɴ", "", OFF_HINT)));

        this.put(rows, gui.slot(MENU, "sounds", SLOT_SOUNDS, rows), SOUNDS,
                 gui.toggle(MENU, "sounds",
                         this.plugin.getPlayerSettings().isSounds(this.owner), null,
                         Face.of(Material.NOTE_BLOCK, "{value}ᴍᴇɴᴜ ꜱᴏᴜɴᴅꜱ",
                                 "", "{good}{tick}ᴄʟɪᴄᴋꜱ, ᴄᴏᴜɴᴛᴅᴏᴡɴꜱ ᴀɴᴅ ᴄᴜᴇꜱ", "", ON_HINT).glow(),
                         Face.of(Material.NOTE_BLOCK, "{value}ᴍᴇɴᴜ ꜱᴏᴜɴᴅꜱ",
                                 "", "{bad}{cross}ᴍᴇᴏᴡᴅᴜᴇʟꜱ ᴍᴀᴋᴇꜱ ɴᴏ ꜱᴏᴜɴᴅ", "", OFF_HINT)));

        Player p = Bukkit.getPlayer((UUID)this.owner);
        if (p != null && p.hasPermission("meowduels.trims")) {
            this.put(rows, gui.slot(MENU, "trims", SLOT_TRIMS, rows), TRIMS,
                     gui.item(MENU, "trims", null, Material.LEATHER_CHESTPLATE,
                              "{value}ᴋɪᴛ ᴛʀɪᴍꜱ",
                              "",
                              "{label}ꜱᴇᴛ ʏᴏᴜʀ ᴀʀᴍᴏᴜʀ ᴛʀɪᴍ ᴘᴇʀ ᴋɪᴛ",
                              "",
                              "{hint}ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ"));
        }
    }

    /**
     * One switch, tagged with the button id its click handler expects.
     *
     * <p>The file's item id and the tag are deliberately different strings:
     * the tags are what {@code onClick} already matches on and renaming them
     * would be a behaviour change, while the ids are what an owner reads, so
     * they say {@code duel-requests} rather than {@code set-duelreq}.
     */
    private void put(int rows, int slot, String tag, Items item) {
        int at = Math.max(0, Math.min(rows * 9 - 1, slot));
        this.inventory.setItem(at, item.hideTooltip().tag(this.plugin.keyButton(), tag).build());
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

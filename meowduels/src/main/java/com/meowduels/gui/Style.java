package com.meowduels.gui;

import com.meowduels.util.Items;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * One look for every menu.
 *
 * <p>The rules, so they stop being decided per menu:
 *
 * <ul>
 * <li><b>Gradient</b> is for the window title and for the two buttons that end
 *     a decision. Nothing else. A gradient on every label is the same as no
 *     gradient at all - nothing stands out because everything does.
 * <li><b>Small caps</b> are for labels and button names, which are the
 *     plugin's own words. Never for content: player names, kit names, arena
 *     names and numbers are printed exactly as they are, because re-casing
 *     somebody's name is a bug, not a style.
 * <li><b>Bold</b> is for nothing. The title carries the weight already.
 * <li><b>Green means go, red means stop</b>, and both say so with a symbol as
 *     well as a colour, for the people who cannot tell them apart.
 * </ul>
 *
 * <p>The frame is a dark grey pane border around a black interior, so a menu
 * reads as a panel rather than a row of floating items.
 */
public final class Style {
    public static final String VALUE = "<#E6E8EB>";
    public static final String LABEL = "<#8E959D>";
    public static final String MUTED = "<#6B7079>";
    public static final String GOOD = "<#7CFF6B>";
    public static final String BAD = "<#FF6B6B>";
    public static final String WARN = "<#FFD65C>";
    public static final String SEP = "<dark_gray>› ";
    public static final String HINT = "<dark_gray>▸ <#8E959D>";
    public static final String TICK = "✔ ";
    public static final String CROSS = "✖ ";

    /** The one gradient: used for a window title and for the confirm button. */
    public static final String GO_A = "#7CFF6B";
    public static final String GO_B = "#1FA32F";
    public static final String NO_A = "#FF8A8A";
    public static final String NO_B = "#C0392B";

    private Style() {
    }

    public static String gradient(String from, String to, String text) {
        return "<gradient:" + from + ":" + to + ">" + text + "</gradient>";
    }

    /** "▏ TITLE" - the leading bar is what makes every window look related. */
    public static String title(String from, String to, String smallCaps) {
        return "<dark_gray>▏ " + Style.gradient(from, to, smallCaps);
    }

    /** Title plus a piece of content, printed as it is. */
    public static String title(String from, String to, String smallCaps, String content) {
        return Style.title(from, to, smallCaps) + " " + SEP + VALUE + content;
    }

    /**
     * The backdrop: dark grey panes, edge to edge.
     *
     * <p>One pane and one colour. A black interior inside a grey border was
     * two greys arguing about which one was the background, and against a
     * dark inventory texture the black half read as a hole rather than as a
     * panel.
     */
    public static void frame(Inventory inventory, int rows) {
        ItemStack pane = Items.of(Material.GRAY_STAINED_GLASS_PANE).rawName(" ").build();
        int size = rows * 9;
        for (int i = 0; i < size; ++i) {
            inventory.setItem(i, pane);
        }
    }

    /**
     * Bottom-left, which is where every window in this plugin puts its way out.
     *
     * <p>Always the same corner, so leaving a menu never needs looking for.
     */
    public static int backSlot(int rows) {
        return (rows - 1) * 9;
    }

    /** A green confirm button, or a grey one that says what is missing. */
    public static ItemStack confirm(org.bukkit.NamespacedKey key, String id, boolean ready,
                                    String readyName, String blockedName, String... lore) {
        return Items.of(ready ? Material.LIME_DYE : Material.GRAY_DYE)
                .rawName(ready ? Style.gradient(GO_A, GO_B, TICK + readyName) : MUTED + CROSS + blockedName)
                .rawLore(lore)
                .glow(ready)
                .hideTooltip()
                .tag(key, id)
                .build();
    }

    public static ItemStack cancel(org.bukkit.NamespacedKey key, String id, String name, String... lore) {
        return Items.of(Material.RED_DYE)
                .rawName(Style.gradient(NO_A, NO_B, CROSS + name))
                .rawLore(lore)
                .hideTooltip()
                .tag(key, id)
                .build();
    }

    /** The way out: an arrow, bottom-left, in every window that has one. */
    public static ItemStack back(org.bukkit.NamespacedKey key, String id) {
        return Items.of(Material.ARROW)
                .rawName(VALUE + "ʙᴀᴄᴋ")
                .rawLore("", HINT + "ᴄʟɪᴄᴋ ᴛᴏ ɢᴏ ʙᴀᴄᴋ")
                .hideTooltip()
                .tag(key, id)
                .build();
    }

    /** How many items fit on one row inside the frame. */
    public static final int PER_ROW = 7;

    /**
     * Where the n-th item of a framed list goes.
     *
     * <p>Lists inside a frame are seven wide, not nine, because columns 0 and 8
     * belong to the border. Everything that lays one out does it through here
     * so a list menu and a grid menu line up with each other.
     */
    public static int gridSlot(int index) {
        return (index / PER_ROW + 1) * 9 + 1 + index % PER_ROW;
    }

    /**
     * Rows a framed list needs to show this many items, borders included.
     *
     * <p>Clamped to six, because that is as tall as a chest goes and asking
     * for a seventh row throws rather than scrolling. Two of the six are the
     * top and bottom border, so four rows of seven - 28 items - is the most a
     * framed list can hold.
     */
    public static int gridRows(int count, int maxListRows) {
        int listRows = Math.max(1, Math.min(maxListRows, (count + PER_ROW - 1) / PER_ROW));
        return Math.min(6, listRows + 2);
    }

    /** on/off line for a toggle, in the two colours that mean those things. */
    public static String state(boolean on, String onText, String offText) {
        return on ? GOOD + TICK + onText : BAD + CROSS + offText;
    }
}

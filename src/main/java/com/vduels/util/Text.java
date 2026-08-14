package com.vduels.util;

import org.bukkit.ChatColor;

/**
 * Small helpers for colouring chat/GUI text. Kept intentionally simple so the
 * rest of the plugin never has to think about legacy colour codes.
 */
public final class Text {

    public static final String PREFIX = color("&8[&bvDuels&8] &r");

    private Text() {
    }

    public static String color(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static String prefixed(String input) {
        return PREFIX + color(input);
    }
}

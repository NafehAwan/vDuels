/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 */
package com.meowduels.util;

import org.bukkit.ChatColor;

public final class Text {
    private static final char[] SMALL = new char[]{'\u1d00', '\u0299', '\u1d04', '\u1d05', '\u1d07', '\ua730', '\u0262', '\u029c', '\u026a', '\u1d0a', '\u1d0b', '\u029f', '\u1d0d', '\u0274', '\u1d0f', '\u1d18', '\ua7af', '\u0280', '\ua731', '\u1d1b', '\u1d1c', '\u1d20', '\u1d21', 'x', '\u028f', '\u1d22'};
    public static final String PREFIX = Text.color("&d\u24d8 ");

    private Text() {
    }

    public static String color(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes((char)'&', (String)Text.smallCaps(input));
    }

    public static String prefixed(String input) {
        return Text.plain(input);
    }

    public static String plain(String input) {
        if (input == null) {
            return "";
        }
        String s = input.replaceAll("(?i)[&\u00a7][0-9a-fk-or]", "");
        s = s.replaceAll("[^\\x20-\\x7e]", "");
        s = s.replaceAll(" {2,}", " ").trim();
        return ChatColor.translateAlternateColorCodes((char)'&', (String)("&7" + s));
    }

    public static String colorNormal(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes((char)'&', (String)input);
    }

    public static String grayEmoji(String input) {
        if (input == null) {
            return "";
        }
        String s = input.replaceAll("(?i)[&\u00a7][0-9a-fk-or]", "");
        s = s.replaceAll(" {2,}", " ").trim();
        return ChatColor.translateAlternateColorCodes((char)'&', (String)("&7" + s));
    }

    public static String smallCaps(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);
            if ((c == '&' || c == '\u00a7') && i + 1 < input.length()) {
                sb.append(c).append(input.charAt(i + 1));
                ++i;
                continue;
            }
            if (c >= 'a' && c <= 'z') {
                sb.append(SMALL[c - 97]);
                continue;
            }
            if (c >= 'A' && c <= 'Z') {
                sb.append(SMALL[c - 65]);
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}


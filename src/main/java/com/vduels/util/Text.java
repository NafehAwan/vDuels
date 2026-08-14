package com.vduels.util;

import org.bukkit.ChatColor;

/**
 * Helpers for colouring chat/GUI text. All display text funnels through
 * {@link #color(String)}, which also converts ASCII letters to the small-caps
 * unicode font so the whole UI shares that look. Bold ({@code &l}) is preserved,
 * so bold text simply renders as bold small-caps. Colour/format codes and any
 * character following {@code &} are left untouched, as are digits and symbols.
 */
public final class Text {

    // a..z -> small-capital unicode letters (x has no small-cap form; kept as-is)
    private static final char[] SMALL = {
            'ᴀ', 'ʙ', 'ᴄ', 'ᴅ', 'ᴇ', 'ꜰ', 'ɢ', 'ʜ', 'ɪ', 'ᴊ', 'ᴋ', 'ʟ', 'ᴍ',
            'ɴ', 'ᴏ', 'ᴘ', 'ꞯ', 'ʀ', 'ꜱ', 'ᴛ', 'ᴜ', 'ᴠ', 'ᴡ', 'x', 'ʏ', 'ᴢ'
    };

    public static final String PREFIX = color("&8[&bvDuels&8] &r");

    private Text() {
    }

    public static String color(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', smallCaps(input));
    }

    public static String prefixed(String input) {
        return PREFIX + color(input);
    }

    /**
     * Replaces ASCII letters with their small-capital unicode equivalents,
     * skipping the character that follows a {@code &} or {@code §} so colour and
     * format codes survive.
     */
    public static String smallCaps(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < input.length()) {
                sb.append(c).append(input.charAt(i + 1));
                i++;
                continue;
            }
            if (c >= 'a' && c <= 'z') {
                sb.append(SMALL[c - 'a']);
            } else if (c >= 'A' && c <= 'Z') {
                sb.append(SMALL[c - 'A']);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

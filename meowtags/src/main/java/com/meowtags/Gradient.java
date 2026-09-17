/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.minimessage.MiniMessage
 */
package com.meowtags;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class Gradient {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String[] SMALL = new String[]{"\u1d00", "\u0299", "\u1d04", "\u1d05", "\u1d07", "\ua730", "\u0262", "\u029c", "\u026a", "\u1d0a", "\u1d0b", "\u029f", "\u1d0d", "\u0274", "\u1d0f", "\u1d18", "\u01eb", "\u0280", "\u0455", "\u1d1b", "\u1d1c", "\u1d20", "\u1d21", "x", "\u028f", "\u1d22"};

    private Gradient() {
    }

    public static String smallCaps(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            char c = Character.toLowerCase(s.charAt(i));
            int idx = LOWER.indexOf(c);
            if (idx >= 0) {
                sb.append(SMALL[idx]);
                continue;
            }
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    public static Component name(String display, String hex1, String hex2) {
        String mm = "<!italic><bold><gradient:#" + Gradient.clean(hex1) + ":#" + Gradient.clean(hex2) + ">" + Gradient.smallCaps(display) + "</gradient></bold>";
        return Gradient.mini(mm);
    }

    public static Component mini(String mm) {
        return MM.deserialize((Object)mm);
    }

    public static String legacySuffix(String display, String hex1, String hex2) {
        int[] a = Gradient.rgb(hex1);
        int[] b = Gradient.rgb(hex2);
        String s = Gradient.smallCaps(display);
        int n = s.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; ++i) {
            double f = n <= 1 ? 0.0 : (double)i / (double)(n - 1);
            int r = (int)Math.round((double)a[0] + (double)(b[0] - a[0]) * f);
            int g = (int)Math.round((double)a[1] + (double)(b[1] - a[1]) * f);
            int bl = (int)Math.round((double)a[2] + (double)(b[2] - a[2]) * f);
            sb.append("&#").append(Gradient.hex2(r)).append(Gradient.hex2(g)).append(Gradient.hex2(bl)).append("&l").append(s.charAt(i));
        }
        return sb.toString();
    }

    private static String clean(String hex) {
        String h = hex.trim();
        if (h.startsWith("#")) {
            h = h.substring(1);
        }
        if (h.length() != 6) {
            return "ffffff";
        }
        return h.toLowerCase();
    }

    private static int[] rgb(String hex) {
        String h = Gradient.clean(hex);
        try {
            return new int[]{Integer.parseInt(h.substring(0, 2), 16), Integer.parseInt(h.substring(2, 4), 16), Integer.parseInt(h.substring(4, 6), 16)};
        }
        catch (NumberFormatException e) {
            return new int[]{255, 255, 255};
        }
    }

    private static String hex2(int v) {
        int x = Math.max(0, Math.min(255, v));
        String s = Integer.toHexString(x);
        return s.length() == 1 ? "0" + s : s;
    }
}


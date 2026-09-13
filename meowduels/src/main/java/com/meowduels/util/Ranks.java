/*
 * Decompiled with CFR 0.152.
 */
package com.meowduels.util;

import com.meowduels.managers.StatsManager;
import com.meowduels.util.Colors;
import java.util.UUID;

public final class Ranks {
    private static final int[] FLOORS = new int[]{900, 1020, 1140, 1260, 1380, 1500};
    private static final String[] NAMES = new String[]{"\u0299\u0280\u1d0f\u0274\u1d22\u1d07", "\u0455\u026a\u029f\u1d20\u1d07\u0280", "\u0262\u1d0f\u029f\u1d05", "\u1d18\u029f\u1d00\u1d1b\u026a\u0274\u1d1c\u1d0d", "\u1d05\u026a\u1d00\u1d0d\u1d0f\u0274\u1d05", "\u1d0d\u1d00\u0455\u1d1b\u1d07\u0280"};
    private static final String[][] COLORS = new String[][]{{"d6975b", "8a5a2b"}, {"eaeaea", "9aa3a8"}, {"ffe27a", "e0a13b"}, {"9ff5e8", "37b9a8"}, {"8ad9ff", "3f8fe0"}, {"ff7ad9", "9b2fd6"}};
    private static final int DIV_WIDTH = 40;

    private Ranks() {
    }

    public static String[] tier(StatsManager stats, UUID id) {
        if (!stats.isPlaced(id)) {
            return new String[]{"\u1d1c\u0274\u0280\u1d00\u0274\u1d0b\u1d07\u1d05", "b8b8b8", "7a7a7a"};
        }
        int e = stats.getElo(id);
        int ti = 0;
        for (int i = 0; i < FLOORS.length; ++i) {
            if (e < FLOORS[i]) continue;
            ti = i;
        }
        int div = (e - FLOORS[ti]) / 40 + 1;
        if (div < 1) {
            div = 1;
        }
        if (div > 3) {
            div = 3;
        }
        return new String[]{NAMES[ti] + " " + div, COLORS[ti][0], COLORS[ti][1]};
    }

    public static int order(StatsManager stats, UUID id) {
        if (!stats.isPlaced(id)) {
            return -1;
        }
        int e = stats.getElo(id);
        int ti = 0;
        for (int i = 0; i < FLOORS.length; ++i) {
            if (e < FLOORS[i]) continue;
            ti = i;
        }
        int div = (e - FLOORS[ti]) / 40 + 1;
        if (div < 1) {
            div = 1;
        }
        if (div > 3) {
            div = 3;
        }
        return ti * 3 + div;
    }

    public static String[] colors(StatsManager stats, UUID id) {
        String[] t = Ranks.tier(stats, id);
        return new String[]{t[1], t[2]};
    }

    public static String label(StatsManager stats, UUID id) {
        return Ranks.tier(stats, id)[0];
    }

    public static String mini(StatsManager stats, UUID id) {
        String[] t = Ranks.tier(stats, id);
        return "<gradient:#" + t[1] + ":#" + t[2] + ">[" + t[0] + "]</gradient>";
    }

    public static String tab(StatsManager stats, UUID id) {
        String[] t = Ranks.tier(stats, id);
        return Ranks.gradientSection("[" + t[0] + "]", t[1], t[2]);
    }

    private static String gradientSection(String text, String h1, String h2) {
        int[] a = Ranks.rgb(h1);
        int[] b = Ranks.rgb(h2);
        int n = text.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; ++i) {
            double f = n <= 1 ? 0.0 : (double)i / (double)(n - 1);
            int r = (int)Math.round((double)a[0] + (double)(b[0] - a[0]) * f);
            int g = (int)Math.round((double)a[1] + (double)(b[1] - a[1]) * f);
            int bl = (int)Math.round((double)a[2] + (double)(b[2] - a[2]) * f);
            sb.append(Colors.hex(r << 16 | g << 8 | bl)).append(text.charAt(i));
        }
        return sb.toString();
    }

    private static int[] rgb(String h) {
        return new int[]{Integer.parseInt(h.substring(0, 2), 16), Integer.parseInt(h.substring(2, 4), 16), Integer.parseInt(h.substring(4, 6), 16)};
    }

    private static String hx(int v) {
        int x = Math.max(0, Math.min(255, v));
        String s = Integer.toHexString(x);
        return s.length() == 1 ? "0" + s : s;
    }
}


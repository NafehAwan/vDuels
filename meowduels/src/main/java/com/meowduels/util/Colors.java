/*
 * Decompiled with CFR 0.152.
 */
package com.meowduels.util;

import java.util.ArrayList;
import java.util.List;

public final class Colors {
    private static final String CODES = "0123456789abcdefklmnor";
    private static final String[] NAMES = new String[]{"black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"};
    private static final int[] RGB = new int[]{0, 170, 43520, 43690, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA, 0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

    private Colors() {
    }

    public static String toSection(String in) {
        if (in == null || in.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(in.length() + 16);
        int n = in.length();
        int i = 0;
        while (i < n) {
            int close;
            int used;
            char c = in.charAt(i);
            if ((c == '&' || c == '\u00a7') && i + 1 < n && (used = Colors.legacyAt(in, i, out)) > 0) {
                i += used;
                continue;
            }
            if (c == '<' && (close = in.indexOf(62, i)) > i) {
                String tag = in.substring(i + 1, close);
                if (tag.regionMatches(true, 0, "gradient", 0, 8) && (tag.length() == 8 || tag.charAt(8) == ':')) {
                    int end = Colors.indexOfIgnoreCase(in, "</gradient>", close + 1);
                    String inner = in.substring(close + 1, end < 0 ? n : end);
                    out.append(Colors.gradient(tag, inner));
                    i = end < 0 ? n : end + 11;
                    continue;
                }
                String code = Colors.tagCode(tag);
                if (code != null) {
                    out.append(code);
                }
                i = close + 1;
                continue;
            }
            out.append(c);
            ++i;
        }
        return out.toString();
    }

    public static String hex(int rgb) {
        String h = String.format("%06x", rgb & 0xFFFFFF);
        StringBuilder sb = new StringBuilder(14).append('\u00a7').append('x');
        for (int i = 0; i < 6; ++i) {
            sb.append('\u00a7').append(h.charAt(i));
        }
        return sb.toString();
    }

    private static int legacyAt(String in, int i, StringBuilder out) {
        if (Character.toLowerCase(in.charAt(i + 1)) == 'x' && i + 13 < in.length()) {
            StringBuilder h = new StringBuilder(6);
            for (int p = i + 2; p < i + 14; p += 2) {
                char marker = in.charAt(p);
                if (marker != '&' && marker != '\u00a7' || Character.digit(in.charAt(p + 1), 16) < 0) {
                    h = null;
                    break;
                }
                h.append(in.charAt(p + 1));
            }
            if (h != null) {
                out.append(Colors.hex(Integer.parseInt(h.toString(), 16)));
                return 14;
            }
        }
        if (in.charAt(i + 1) == '#' && Colors.isHex(in, i + 2, 6)) {
            out.append(Colors.hex(Integer.parseInt(in.substring(i + 2, i + 8), 16)));
            return 8;
        }
        char k = Character.toLowerCase(in.charAt(i + 1));
        if (CODES.indexOf(k) >= 0) {
            out.append('\u00a7').append(k);
            return 2;
        }
        return 0;
    }

    private static String tagCode(String tag) {
        Integer c;
        String t = tag.trim().toLowerCase();
        if (t.startsWith("/")) {
            return null;
        }
        if (t.startsWith("color:") || t.startsWith("colour:") || t.startsWith("c:")) {
            t = t.substring(t.indexOf(58) + 1);
        }
        if ((c = Colors.colorOf(t)) != null) {
            return Colors.hex(c);
        }
        if (t.equals("b") || t.equals("bold")) {
            return "\u00a7l";
        }
        if (t.equals("i") || t.equals("italic") || t.equals("em")) {
            return "\u00a7o";
        }
        if (t.equals("u") || t.equals("underlined")) {
            return "\u00a7n";
        }
        if (t.equals("st") || t.equals("strikethrough")) {
            return "\u00a7m";
        }
        if (t.equals("obf") || t.equals("obfuscated")) {
            return "\u00a7k";
        }
        if (t.equals("reset") || t.equals("r")) {
            return "\u00a7r";
        }
        return null;
    }

    private static Integer colorOf(String t) {
        if (t.length() == 7 && t.charAt(0) == '#' && Colors.isHex(t, 1, 6)) {
            return Integer.parseInt(t.substring(1), 16);
        }
        for (int i = 0; i < NAMES.length; ++i) {
            if (!NAMES[i].equals(t)) continue;
            return RGB[i];
        }
        return null;
    }

    private static String gradient(String tag, String inner) {
        String[] parts = tag.split(":");
        ArrayList<Integer> stops = new ArrayList<Integer>();
        for (int i = 1; i < parts.length; ++i) {
            Integer c = Colors.colorOf(parts[i].trim().toLowerCase());
            if (c == null) continue;
            stops.add(c);
        }
        String flat = Colors.toSection(inner);
        if (stops.size() < 2) {
            return flat;
        }
        StringBuilder style = new StringBuilder();
        StringBuilder body = new StringBuilder();
        int visible = 0;
        for (int i = 0; i < flat.length(); ++i) {
            char ch = flat.charAt(i);
            if (ch == '\u00a7' && i + 1 < flat.length()) {
                char code = Character.toLowerCase(flat.charAt(i + 1));
                if (code == 'x' && i + 13 < flat.length()) {
                    i += 13;
                    continue;
                }
                if ("klmno".indexOf(code) >= 0) {
                    style.append('\u00a7').append(code);
                    ++i;
                    continue;
                }
                if (code == 'r') {
                    style.setLength(0);
                    ++i;
                    continue;
                }
                ++i;
                continue;
            }
            body.append(ch);
            ++visible;
        }
        if (visible == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(visible * 16);
        for (int i = 0; i < visible; ++i) {
            double f = visible <= 1 ? 0.0 : (double)i / (double)(visible - 1);
            out.append(Colors.hex(Colors.lerp(stops, f))).append((CharSequence)style).append(body.charAt(i));
        }
        return out.toString();
    }

    private static int lerp(List<Integer> stops, double f) {
        int segments = stops.size() - 1;
        double scaled = f * (double)segments;
        int seg = (int)Math.floor(scaled);
        if (seg >= segments) {
            seg = segments - 1;
        }
        double t = scaled - (double)seg;
        int a = stops.get(seg);
        int b = stops.get(seg + 1);
        int r = (int)Math.round((double)(a >> 16 & 0xFF) + (double)((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
        int g = (int)Math.round((double)(a >> 8 & 0xFF) + (double)((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
        int bl = (int)Math.round((double)(a & 0xFF) + (double)((b & 0xFF) - (a & 0xFF)) * t);
        return r << 16 | g << 8 | bl;
    }

    private static int indexOfIgnoreCase(String s, String needle, int from) {
        int limit = s.length() - needle.length();
        for (int i = Math.max(0, from); i <= limit; ++i) {
            if (!s.regionMatches(true, i, needle, 0, needle.length())) continue;
            return i;
        }
        return -1;
    }

    private static boolean isHex(String s, int off, int len) {
        if (off + len > s.length()) {
            return false;
        }
        for (int i = off; i < off + len; ++i) {
            if (Character.digit(s.charAt(i), 16) >= 0) continue;
            return false;
        }
        return true;
    }
}


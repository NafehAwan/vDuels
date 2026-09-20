package com.meowduels.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * One place for "you have to wait", and one way of showing it.
 *
 * <p>A cooldown the player can only discover by clicking and reading a line of
 * red text is not a cooldown, it is a guessing game. Every cooldown registered
 * here also drives the vanilla item cooldown - the sweep that wipes across the
 * icon and empties as the timer runs - so the wait is visible on the hotbar the
 * whole time, at the client's frame rate rather than the server's tick rate.
 *
 * <p>The sweep is re-asserted rather than set once. A client that is handed a
 * fresh stack of the same item - a kit reissue between rounds, a pickup - draws
 * it without any cooldown, and the only way to keep the two in step is to tell
 * it again. re-assert is cheap: it is one packet for a player who is actually
 * cooling down, and nothing at all for everyone else.
 */
public final class Cooldowns {
    /** key -> (player -> when it expires). */
    private static final Map<String, Map<UUID, Long>> ENTRIES = new HashMap<String, Map<UUID, Long>>();
    /** key -> (player -> the item whose sweep mirrors it). Per player, because
     *  the same cooldown can sit on a different item for different people - a
     *  golden head is a head on one server and an apple on the next, and both
     *  can be in play at once. */
    private static final Map<String, Map<UUID, Material>> ICONS = new HashMap<String, Map<UUID, Material>>();

    private Cooldowns() {
    }

    /**
     * Starts (or restarts) a cooldown and lights up the item sweep.
     *
     * @param icon the material to draw the sweep on, or null for a cooldown
     *             with no item behind it
     */
    public static void start(Player player, String key, Material icon, long millis) {
        if (player == null || millis <= 0L) {
            return;
        }
        Cooldowns.entries(key).put(player.getUniqueId(), System.currentTimeMillis() + millis);
        if (icon != null) {
            Cooldowns.icons(key).put(player.getUniqueId(), icon);
            Cooldowns.sweep(player, icon, millis);
        }
    }

    /** Milliseconds left, or 0 when the player is free to go. */
    public static long remaining(Player player, String key) {
        if (player == null) {
            return 0L;
        }
        Long until = Cooldowns.entries(key).get(player.getUniqueId());
        if (until == null) {
            return 0L;
        }
        long left = until - System.currentTimeMillis();
        if (left <= 0L) {
            Cooldowns.entries(key).remove(player.getUniqueId());
            return 0L;
        }
        return left;
    }

    public static boolean active(Player player, String key) {
        return Cooldowns.remaining(player, key) > 0L;
    }

    public static void clear(Player player, String key) {
        if (player != null) {
            Cooldowns.entries(key).remove(player.getUniqueId());
        }
    }

    public static void clearAll(UUID id) {
        for (Map<UUID, Long> map : ENTRIES.values()) {
            map.remove(id);
        }
        for (Map<UUID, Material> map : ICONS.values()) {
            map.remove(id);
        }
    }

    /**
     * Keeps every live sweep in step with the clock that owns it, and drops
     * expired entries. Called once a second from the plugin's tick.
     */
    public static void tick(Player player) {
        if (player == null) {
            return;
        }
        UUID id = player.getUniqueId();
        for (Map.Entry<String, Map<UUID, Long>> entry : ENTRIES.entrySet()) {
            Long until = entry.getValue().get(id);
            if (until == null) continue;
            long left = until - System.currentTimeMillis();
            if (left <= 0L) {
                entry.getValue().remove(id);
                Cooldowns.icons(entry.getKey()).remove(id);
                Sounds.refreshed(player);
                continue;
            }
            Material icon = Cooldowns.icons(entry.getKey()).get(id);
            if (icon != null) {
                Cooldowns.sweep(player, icon, left);
            }
        }
    }

    /** A bar of the remaining time, for an action bar that updates every tick. */
    public static String bar(long remainingMs, long totalMs, int width) {
        int filled;
        int n;
        long total = Math.max(1L, totalMs);
        int cells = n = Math.max(1, width);
        int i = filled = (int)Math.round((double)Math.max(0L, remainingMs) / (double)total * (double)cells);
        if (filled > cells) {
            filled = cells;
        }
        StringBuilder out = new StringBuilder(cells);
        for (int j = 0; j < cells; ++j) {
            out.append(j < filled ? '█' : '░');
        }
        return out.toString();
    }

    /** One decimal, so a shrinking number doesn't jitter between widths. */
    public static String seconds(long remainingMs) {
        long tenths = (remainingMs + 99L) / 100L;
        return tenths / 10L + "." + tenths % 10L;
    }

    private static void sweep(Player player, Material icon, long millis) {
        try {
            int ticks = (int)Math.max(1L, (millis + 49L) / 50L);
            if (player.getCooldown(icon) < ticks) {
                player.setCooldown(icon, ticks);
            }
        }
        catch (Throwable throwable) {
            // Not every material accepts a cooldown on every server build. The
            // timer itself is authoritative; the sweep is the nice part.
        }
    }

    private static Map<UUID, Material> icons(String key) {
        Map<UUID, Material> map = ICONS.get(key);
        if (map == null) {
            map = new HashMap<UUID, Material>();
            ICONS.put(key, map);
        }
        return map;
    }

    private static Map<UUID, Long> entries(String key) {
        Map<UUID, Long> map = ENTRIES.get(key);
        if (map == null) {
            map = new HashMap<UUID, Long>();
            ENTRIES.put(key, map);
        }
        return map;
    }
}

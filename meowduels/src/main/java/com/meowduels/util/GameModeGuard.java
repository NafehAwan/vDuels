/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.GameMode
 *  org.bukkit.entity.Player
 */
package com.meowduels.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public final class GameModeGuard {
    private static final Map<UUID, GameMode> PINNED = new HashMap<UUID, GameMode>();
    private static boolean applying = false;

    private GameModeGuard() {
    }

    public static void pin(Player player, GameMode mode) {
        if (player == null || mode == null) {
            return;
        }
        PINNED.put(player.getUniqueId(), mode);
        applying = true;
        try {
            player.setGameMode(mode);
        }
        finally {
            applying = false;
        }
    }

    public static void release(UUID id) {
        if (id != null) {
            PINNED.remove(id);
        }
    }

    public static GameMode required(UUID id) {
        return id == null ? null : PINNED.get(id);
    }

    public static boolean isApplying() {
        return applying;
    }

    public static void setFreely(Player player, GameMode mode) {
        if (player == null || mode == null) {
            return;
        }
        GameModeGuard.release(player.getUniqueId());
        applying = true;
        try {
            player.setGameMode(mode);
        }
        finally {
            applying = false;
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package com.meowduels.util;

import org.bukkit.entity.Player;

public final class Sounds {
    private Sounds() {
    }

    private static void play(Player player, String key, float volume, float pitch) {
        if (player != null) {
            player.playSound(player.getLocation(), key, volume, pitch);
        }
    }

    public static void click(Player player) {
        Sounds.play(player, "ui.button.click", 0.5f, 1.3f);
    }

    public static void request(Player player) {
        Sounds.play(player, "entity.experience_orb.pickup", 1.0f, 1.4f);
    }

    public static void accept(Player player) {
        Sounds.play(player, "block.note_block.chime", 1.0f, 1.2f);
    }

    public static void ready(Player player) {
        Sounds.play(player, "block.note_block.pling", 1.0f, 1.8f);
    }

    public static void matchFound(Player player) {
        Sounds.play(player, "block.note_block.bell", 1.0f, 1.2f);
    }

    public static void countdown(Player player) {
        Sounds.play(player, "block.note_block.pling", 1.0f, 1.0f);
    }

    public static void fight(Player player) {
        Sounds.play(player, "block.note_block.pling", 1.0f, 2.0f);
    }

    public static void roundWon(Player player) {
        Sounds.play(player, "block.note_block.pling", 1.0f, 1.6f);
    }

    public static void roundLost(Player player) {
        Sounds.play(player, "block.note_block.bass", 1.0f, 0.8f);
    }

    public static void victory(Player player) {
        Sounds.play(player, "ui.toast.challenge_complete", 1.0f, 1.0f);
    }

    public static void defeat(Player player) {
        Sounds.play(player, "entity.villager.no", 1.0f, 1.0f);
    }
}


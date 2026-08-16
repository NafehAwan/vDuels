package com.vduels.util;

import org.bukkit.entity.Player;

/**
 * Small sound helper. Uses the string (namespaced-key) form of
 * {@code playSound} so it stays version-independent across the 1.21.x changes to
 * the {@code Sound} registry.
 */
public final class Sounds {

    private Sounds() {
    }

    private static void play(Player player, String key, float volume, float pitch) {
        if (player != null) {
            player.playSound(player.getLocation(), key, volume, pitch);
        }
    }

    /** A soft UI click for menu interactions. */
    public static void click(Player player) {
        play(player, "ui.button.click", 0.5f, 1.3f);
    }

    /** Played to the target when they receive a duel request. */
    public static void request(Player player) {
        play(player, "entity.experience_orb.pickup", 1f, 1.4f);
    }

    /** A countdown tick. */
    public static void countdown(Player player) {
        play(player, "block.note_block.pling", 1f, 1f);
    }

    /** The FIGHT! cue. */
    public static void fight(Player player) {
        play(player, "block.note_block.pling", 1f, 2f);
    }

    public static void roundWon(Player player) {
        play(player, "block.note_block.pling", 1f, 1.6f);
    }

    public static void roundLost(Player player) {
        play(player, "block.note_block.bass", 1f, 0.8f);
    }

    public static void victory(Player player) {
        play(player, "ui.toast.challenge_complete", 1f, 1f);
    }

    public static void defeat(Player player) {
        play(player, "entity.villager.no", 1f, 1f);
    }
}

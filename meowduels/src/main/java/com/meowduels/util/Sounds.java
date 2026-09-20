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

    // --- menus ---------------------------------------------------------------

    /** A menu window opening. Quieter than the click, so browsing is not a drum. */
    public static void open(Player player) {
        Sounds.play(player, "block.barrel.open", 0.35f, 1.6f);
    }

    public static void close(Player player) {
        Sounds.play(player, "block.barrel.close", 0.35f, 1.6f);
    }

    /** A click that did nothing - a locked kit, a button you may not press. */
    public static void deny(Player player) {
        Sounds.play(player, "block.note_block.bass", 0.7f, 0.6f);
    }

    /** A choice that stuck: a kit picked, a mode chosen, a setting flipped. */
    public static void select(Player player) {
        Sounds.play(player, "block.note_block.hat", 0.7f, 1.5f);
    }

    // --- parties and events --------------------------------------------------

    public static void invite(Player player) {
        Sounds.play(player, "block.note_block.chime", 1.0f, 1.6f);
    }

    public static void join(Player player) {
        Sounds.play(player, "entity.experience_orb.pickup", 0.8f, 1.6f);
    }

    public static void leave(Player player) {
        Sounds.play(player, "block.note_block.bass", 0.8f, 1.0f);
    }

    /** You got the kill. Deliberately distinct from winning a round. */
    public static void kill(Player player) {
        Sounds.play(player, "entity.player.attack.crit", 1.0f, 1.2f);
    }

    /** You went down. */
    public static void death(Player player) {
        Sounds.play(player, "entity.wither.spawn", 0.4f, 1.8f);
    }

    /** Someone else went down, heard by everyone still in. */
    public static void eliminated(Player player) {
        Sounds.play(player, "block.note_block.bass", 0.8f, 0.7f);
    }

    /**
     * A countdown pip that climbs as the number falls.
     *
     * <p>One flat note repeated tells you a countdown is running; a rising one
     * tells you how far through it is without reading the number.
     */
    public static void tick(Player player, int secondsLeft, int total) {
        int span = Math.max(1, total);
        float progress = 1.0f - (float)Math.max(0, Math.min(span, secondsLeft)) / (float)span;
        Sounds.play(player, "block.note_block.pling", 1.0f, 0.9f + 0.7f * progress);
    }

    /** Outside the border, once a second, sharpening as the damage ramps. */
    public static void border(Player player, int seconds) {
        float pitch = 0.8f + 0.06f * (float)Math.min(10, seconds);
        Sounds.play(player, "block.note_block.didgeridoo", 0.7f, pitch);
    }

    /** A cooldown refusing the click, and the same cooldown finishing. */
    public static void cooling(Player player) {
        Sounds.play(player, "block.note_block.bass", 0.6f, 1.2f);
    }

    /** A cooldown just ran out - the item is usable again. */
    public static void refreshed(Player player) {
        Sounds.play(player, "block.note_block.bell", 0.5f, 1.9f);
    }
}

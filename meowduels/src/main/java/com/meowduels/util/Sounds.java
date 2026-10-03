/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package com.meowduels.util;

import java.util.function.Predicate;
import org.bukkit.entity.Player;

public final class Sounds {
    /**
     * Who has turned the plugin's sounds off.
     *
     * <p>Every cue in this class is static and has no way back to the plugin,
     * so the one thing it needs to know arrives as a predicate instead: set
     * once at enable, read on every cue. Null until then, which is what makes
     * a sound played during startup audible rather than a crash.
     *
     * <p>volatile because the answer is set on the main thread and cues can be
     * played from a scheduler thread.
     */
    private static volatile Predicate<Player> muted;

    private Sounds() {
    }

    /** Called once, at enable, with the /settings toggle. */
    public static void mutedWhen(Predicate<Player> check) {
        muted = check;
    }

    /** key, volume and pitch for one cue, as read from config. */
    private static final class Cue {
        final String key;
        final float volume;
        final float pitch;
        final float pitchTo;

        Cue(String key, float volume, float pitch, float pitchTo) {
            this.key = key;
            this.volume = volume;
            this.pitch = pitch;
            this.pitchTo = pitchTo;
        }
    }

    private static volatile java.util.Map<String, Cue> cues = java.util.Collections.emptyMap();

    /**
     * Loads the `sounds:` section, once, at enable.
     *
     * <p>Anything missing falls through to the literal the call site passes,
     * so an absent section - or one key of one cue - leaves that cue exactly
     * as it shipped. That is the whole contract: deleting the section cannot
     * change how the plugin sounds.
     */
    public static void load(org.bukkit.configuration.ConfigurationSection section) {
        java.util.Map<String, Cue> built = new java.util.HashMap<String, Cue>();
        if (section != null) {
            for (String name : section.getKeys(false)) {
                org.bukkit.configuration.ConfigurationSection c = section.getConfigurationSection(name);
                if (c == null) continue;
                built.put(name, new Cue(
                        c.getString("key", null),
                        (float)c.getDouble("volume", Double.NaN),
                        (float)c.getDouble("pitch", Double.NaN),
                        (float)c.getDouble("pitch-to", Double.NaN)));
            }
        }
        cues = built;
    }

    /**
     * A pitch that climbs across a countdown.
     *
     * <p>`pitch` is the note at the start and `pitch-to` the note at the end;
     * a server that wants a flat cue sets them equal.
     */
    private static float ramp(String name, float from, float to, float progress) {
        Cue cue = cues.get(name);
        if (cue != null && !Float.isNaN(cue.pitch)) {
            from = cue.pitch;
        }
        if (cue != null && !Float.isNaN(cue.pitchTo)) {
            to = cue.pitchTo;
        }
        return from + (to - from) * Math.max(0.0f, Math.min(1.0f, progress));
    }

    private static void play(Player player, String name, String key, float volume, float pitch) {
        if (player == null) {
            return;
        }
        Predicate<Player> check = muted;
        if (check != null && check.test(player)) {
            return;
        }
        Cue cue = cues.get(name);
        if (cue != null) {
            if (cue.key != null) key = cue.key;
            if (!Float.isNaN(cue.volume)) volume = cue.volume;
            if (!Float.isNaN(cue.pitch)) pitch = cue.pitch;
        }
        if (key == null || key.isEmpty()) {
            return;   // a server that wants one cue silent clears its key
        }
        player.playSound(player.getLocation(), key, volume, pitch);
    }

    public static void click(Player player) {
        Sounds.play(player, "click", "ui.button.click", 0.5f, 1.3f);
    }

    public static void request(Player player) {
        Sounds.play(player, "request", "entity.experience_orb.pickup", 1.0f, 1.4f);
    }

    public static void accept(Player player) {
        Sounds.play(player, "accept", "block.note_block.chime", 1.0f, 1.2f);
    }

    public static void ready(Player player) {
        Sounds.play(player, "ready", "block.note_block.pling", 1.0f, 1.8f);
    }

    public static void matchFound(Player player) {
        Sounds.play(player, "matchFound", "block.note_block.bell", 1.0f, 1.2f);
    }

    public static void countdown(Player player) {
        Sounds.play(player, "countdown", "block.note_block.pling", 1.0f, 1.0f);
    }

    public static void fight(Player player) {
        Sounds.play(player, "fight", "block.note_block.pling", 1.0f, 2.0f);
    }

    public static void roundWon(Player player) {
        Sounds.play(player, "roundWon", "block.note_block.pling", 1.0f, 1.6f);
    }

    public static void roundLost(Player player) {
        Sounds.play(player, "roundLost", "block.note_block.bass", 1.0f, 0.8f);
    }

    public static void victory(Player player) {
        Sounds.play(player, "victory", "ui.toast.challenge_complete", 1.0f, 1.0f);
    }

    public static void defeat(Player player) {
        Sounds.play(player, "defeat", "entity.villager.no", 1.0f, 1.0f);
    }

    // --- menus ---------------------------------------------------------------

    /** A menu window opening. Quieter than the click, so browsing is not a drum. */
    public static void open(Player player) {
        Sounds.play(player, "open", "block.barrel.open", 0.35f, 1.6f);
    }

    public static void close(Player player) {
        Sounds.play(player, "close", "block.barrel.close", 0.35f, 1.6f);
    }

    /** A click that did nothing - a locked kit, a button you may not press. */
    public static void deny(Player player) {
        Sounds.play(player, "deny", "block.note_block.bass", 0.7f, 0.6f);
    }

    /** A choice that stuck: a kit picked, a mode chosen, a setting flipped. */
    public static void select(Player player) {
        Sounds.play(player, "select", "block.note_block.hat", 0.7f, 1.5f);
    }

    // --- parties and events --------------------------------------------------

    public static void invite(Player player) {
        Sounds.play(player, "invite", "block.note_block.chime", 1.0f, 1.6f);
    }

    public static void join(Player player) {
        Sounds.play(player, "join", "entity.experience_orb.pickup", 0.8f, 1.6f);
    }

    public static void leave(Player player) {
        Sounds.play(player, "leave", "block.note_block.bass", 0.8f, 1.0f);
    }

    /** You got the kill. Deliberately distinct from winning a round. */
    public static void kill(Player player) {
        Sounds.play(player, "kill", "entity.player.attack.crit", 1.0f, 1.2f);
    }

    /** You went down. */
    public static void death(Player player) {
        Sounds.play(player, "death", "entity.wither.spawn", 0.4f, 1.8f);
    }

    /** Someone else went down, heard by everyone still in. */
    public static void eliminated(Player player) {
        Sounds.play(player, "eliminated", "block.note_block.bass", 0.8f, 0.7f);
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
        Sounds.play(player, "tick", "block.note_block.pling", 1.0f, Sounds.ramp("tick", 0.9f, 1.6f, progress));
    }

    /** Outside the border, once a second, sharpening as the damage ramps. */
    public static void border(Player player, int seconds) {
        float progress = (float)Math.min(10, seconds) / 10.0f;
        Sounds.play(player, "border", "block.note_block.didgeridoo", 0.7f, Sounds.ramp("border", 0.8f, 1.4f, progress));
    }

    /** Biting a golden head. */
    public static void eat(Player player) {
        Sounds.play(player, "eat", "entity.player.burp", 0.7f, 1.0f);
    }

    /** A cooldown refusing the click, and the same cooldown finishing. */
    public static void cooling(Player player) {
        Sounds.play(player, "cooling", "block.note_block.bass", 0.6f, 1.2f);
    }

    /** A cooldown just ran out - the item is usable again. */
    public static void refreshed(Player player) {
        Sounds.play(player, "refreshed", "block.note_block.bell", 0.5f, 1.9f);
    }
}

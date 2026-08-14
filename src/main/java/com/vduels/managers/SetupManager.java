package com.vduels.managers;

import com.vduels.VDuels;
import com.vduels.model.Arena;
import com.vduels.util.Text;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Drives the chat-based "walk to a spot and type done" arena setup wizard.
 * Each player can have at most one active session.
 */
public class SetupManager {

    public enum Step {
        SPAWN1("&aGo to the &ePlayer 1 spawn&a and type &edone&a in chat."),
        SPAWN2("&aGo to the &ePlayer 2 spawn&a and type &edone&a in chat."),
        CORNER1("&aGo to &ecorner 1 (the lowest corner)&a and type &edone&a."),
        CORNER2("&aGo to &ecorner 2 (the highest corner)&a and type &edone&a."),
        FINISHED("");

        private final String prompt;

        Step(String prompt) {
            this.prompt = prompt;
        }

        public String getPrompt() {
            return prompt;
        }
    }

    private final VDuels plugin;
    private final Map<UUID, Arena> sessionArena = new HashMap<>();
    private final Map<UUID, Step> sessionStep = new HashMap<>();

    public SetupManager(VDuels plugin) {
        this.plugin = plugin;
    }

    public boolean inSetup(UUID player) {
        return sessionStep.containsKey(player);
    }

    public void begin(Player player, Arena arena) {
        sessionArena.put(player.getUniqueId(), arena);
        sessionStep.put(player.getUniqueId(), Step.SPAWN1);
        player.sendMessage(Text.prefixed("&fSetup started for arena &b" + arena.getName() + "&f."));
        player.sendMessage(Text.prefixed("&7Type &ccancel&7 at any time to abort."));
        player.sendMessage(Text.prefixed(Step.SPAWN1.getPrompt()));
    }

    public void cancel(Player player) {
        sessionArena.remove(player.getUniqueId());
        sessionStep.remove(player.getUniqueId());
    }

    /**
     * Handles a chat line for a player in setup. Must be called on the main
     * thread. Returns the now-configured arena if the wizard just finished,
     * otherwise null.
     */
    public Arena handleInput(Player player, String message) {
        UUID id = player.getUniqueId();
        Step step = sessionStep.get(id);
        Arena arena = sessionArena.get(id);
        if (step == null || arena == null) {
            return null;
        }

        if (message.equalsIgnoreCase("cancel")) {
            cancel(player);
            player.sendMessage(Text.prefixed("&cSetup cancelled."));
            return null;
        }

        if (!message.equalsIgnoreCase("done")) {
            player.sendMessage(Text.prefixed("&7Type &edone&7 when you are standing in position, or &ccancel&7."));
            return null;
        }

        switch (step) {
            case SPAWN1 -> {
                arena.setSpawn1(player.getLocation());
                player.sendMessage(Text.prefixed("&aPlayer 1 spawn saved."));
                advance(player, Step.SPAWN2);
            }
            case SPAWN2 -> {
                arena.setSpawn2(player.getLocation());
                player.sendMessage(Text.prefixed("&aPlayer 2 spawn saved."));
                advance(player, Step.CORNER1);
            }
            case CORNER1 -> {
                arena.setCorner1(player.getLocation());
                player.sendMessage(Text.prefixed("&aCorner 1 saved."));
                advance(player, Step.CORNER2);
            }
            case CORNER2 -> {
                arena.setCorner2(player.getLocation());
                player.sendMessage(Text.prefixed("&aCorner 2 saved."));
                finish(player, arena);
                return arena;
            }
            default -> {
            }
        }
        return null;
    }

    private void advance(Player player, Step next) {
        sessionStep.put(player.getUniqueId(), next);
        player.sendMessage(Text.prefixed(next.getPrompt()));
    }

    private void finish(Player player, Arena arena) {
        cancel(player);
        plugin.getArenaManager().save();
        player.sendMessage(Text.prefixed("&aArena &b" + arena.getName() + "&a is now set up!"));
    }
}

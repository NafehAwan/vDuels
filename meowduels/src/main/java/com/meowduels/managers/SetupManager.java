/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package com.meowduels.managers;

import com.meowduels.MeowDuels;
import com.meowduels.model.Arena;
import com.meowduels.util.Text;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public class SetupManager {
    private final MeowDuels plugin;
    private final Map<UUID, Arena> sessionArena = new HashMap<UUID, Arena>();
    private final Map<UUID, Step> sessionStep = new HashMap<UUID, Step>();
    private final Map<UUID, Arena> eventSpawnSession = new HashMap<UUID, Arena>();
    private final Map<UUID, Arena> borderSession = new HashMap<UUID, Arena>();
    private final Map<UUID, Integer> borderStep = new HashMap<UUID, Integer>();

    public SetupManager(MeowDuels plugin) {
        this.plugin = plugin;
    }

    public boolean inSetup(UUID player) {
        return this.sessionStep.containsKey(player);
    }

    public boolean inEventSpawn(UUID player) {
        return this.eventSpawnSession.containsKey(player);
    }

    public void beginEventSpawn(Player player, Arena arena) {
        this.eventSpawnSession.put(player.getUniqueId(), arena);
        player.sendMessage(this.plugin.messages().get("notice.setup.go-event-arena-spawn"));
        player.sendMessage(this.plugin.messages().get("notice.setup.type-cancel-abort-2"));
    }

    public Arena handleEventSpawnInput(Player player, String message) {
        UUID id = player.getUniqueId();
        Arena arena = this.eventSpawnSession.get(id);
        if (arena == null) {
            return null;
        }
        if (message.equalsIgnoreCase("cancel")) {
            this.eventSpawnSession.remove(id);
            player.sendMessage(this.plugin.messages().get("notice.setup.event-spawn-setup-cancelled"));
            return arena;
        }
        if (!message.equalsIgnoreCase("done")) {
            player.sendMessage(this.plugin.messages().get("notice.setup.type-done-where-re-2"));
            return null;
        }
        this.eventSpawnSession.remove(id);
        arena.setEventSpawn(player.getLocation());
        this.plugin.getArenaManager().save();
        player.sendMessage(this.plugin.messages().get("notice.setup.event-spawn-saved", "arena", arena.getName()));
        return arena;
    }

    public boolean inBorder(UUID player) {
        return this.borderSession.containsKey(player);
    }

    public void beginBorder(Player player, Arena arena) {
        this.borderSession.put(player.getUniqueId(), arena);
        this.borderStep.put(player.getUniqueId(), 1);
        player.sendMessage(this.plugin.messages().get("notice.setup.go-first-border-corner"));
        player.sendMessage(this.plugin.messages().get("notice.setup.type-cancel-abort"));
    }

    public Arena handleBorderInput(Player player, String message) {
        UUID id = player.getUniqueId();
        Arena arena = this.borderSession.get(id);
        Integer step = this.borderStep.get(id);
        if (arena == null || step == null) {
            return null;
        }
        if (message.equalsIgnoreCase("cancel")) {
            this.borderSession.remove(id);
            this.borderStep.remove(id);
            player.sendMessage(this.plugin.messages().get("notice.setup.border-setup-cancelled"));
            return arena;
        }
        if (!message.equalsIgnoreCase("done")) {
            player.sendMessage(this.plugin.messages().get("notice.setup.type-done-where-re"));
            return null;
        }
        if (step == 1) {
            arena.setBorderCorner1(player.getLocation());
            this.borderStep.put(id, 2);
            player.sendMessage(this.plugin.messages().get("notice.setup.first-border-corner-saved"));
            player.sendMessage(this.plugin.messages().get("notice.setup.now-go-second-border"));
            return null;
        }
        arena.setBorderCorner2(player.getLocation());
        this.borderSession.remove(id);
        this.borderStep.remove(id);
        this.plugin.getArenaManager().save();
        player.sendMessage(this.plugin.messages().get("notice.setup.event-border-area-saved", "arena", arena.getName()));
        return arena;
    }

    public void begin(Player player, Arena arena) {
        this.sessionArena.put(player.getUniqueId(), arena);
        this.sessionStep.put(player.getUniqueId(), Step.SPAWN1);
        player.sendMessage(this.plugin.messages().get("notice.setup.setup-started-arena", "arena", arena.getName()));
        player.sendMessage(this.plugin.messages().get("notice.setup.type-cancel-at-any"));
        player.sendMessage(this.plugin.messages().get("notice.setup.message-2", "prompt", String.valueOf(Step.SPAWN1.getPrompt())));
    }

    public void cancel(Player player) {
        this.sessionArena.remove(player.getUniqueId());
        this.sessionStep.remove(player.getUniqueId());
    }

    public Arena handleInput(Player player, String message) {
        UUID id = player.getUniqueId();
        Step step = this.sessionStep.get(id);
        Arena arena = this.sessionArena.get(id);
        if (step == null || arena == null) {
            return null;
        }
        if (message.equalsIgnoreCase("cancel")) {
            this.cancel(player);
            player.sendMessage(this.plugin.messages().get("notice.setup.setup-cancelled"));
            return null;
        }
        if (!message.equalsIgnoreCase("done")) {
            player.sendMessage(this.plugin.messages().get("notice.setup.type-done-when-standing"));
            return null;
        }
        switch (step.ordinal()) {
            case 0: {
                arena.setSpawn1(player.getLocation());
                player.sendMessage(this.plugin.messages().get("notice.setup.player-1-spawn-saved"));
                this.advance(player, Step.SPAWN2);
                break;
            }
            case 1: {
                arena.setSpawn2(player.getLocation());
                player.sendMessage(this.plugin.messages().get("notice.setup.player-2-spawn-saved"));
                this.advance(player, Step.CORNER1);
                break;
            }
            case 2: {
                arena.setCorner1(player.getLocation());
                player.sendMessage(this.plugin.messages().get("notice.setup.corner-1-saved"));
                this.advance(player, Step.CORNER2);
                break;
            }
            case 3: {
                arena.setCorner2(player.getLocation());
                player.sendMessage(this.plugin.messages().get("notice.setup.corner-2-saved"));
                this.finish(player, arena);
                return arena;
            }
        }
        return null;
    }

    private void advance(Player player, Step next) {
        this.sessionStep.put(player.getUniqueId(), next);
        player.sendMessage(this.plugin.messages().get("notice.setup.message", "prompt", String.valueOf(next.getPrompt())));
    }

    private void finish(Player player, Arena arena) {
        this.cancel(player);
        this.plugin.getArenaManager().save();
        this.plugin.getArenaManager().snapshotArena(arena);
        player.sendMessage(this.plugin.messages().get("notice.setup.arena-now-set-up", "arena", arena.getName()));
        player.sendMessage(this.plugin.messages().get("notice.setup.tip-after-decorating-arena", "arena", arena.getName()));
    }

    public static enum Step {
        SPAWN1("&aGo to the &ePlayer 1 spawn&a and type &edone&a in chat."),
        SPAWN2("&aGo to the &ePlayer 2 spawn&a and type &edone&a in chat."),
        CORNER1("&aGo to &ecorner 1 (the lowest corner)&a and type &edone&a."),
        CORNER2("&aGo to &ecorner 2 (the highest corner)&a and type &edone&a."),
        FINISHED("");

        private final String prompt;

        private Step(String prompt) {
            this.prompt = prompt;
        }

        public String getPrompt() {
            return this.prompt;
        }
    }
}


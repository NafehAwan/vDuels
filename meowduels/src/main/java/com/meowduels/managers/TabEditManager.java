/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package com.meowduels.managers;

import com.meowduels.MeowDuels;
import com.meowduels.util.Text;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public class TabEditManager {
    private final MeowDuels plugin;
    private final Map<UUID, Field> pending = new HashMap<UUID, Field>();

    public TabEditManager(MeowDuels plugin) {
        this.plugin = plugin;
    }

    public boolean isEditing(UUID id) {
        return this.pending.containsKey(id);
    }

    public void begin(Player player, Field field) {
        this.pending.put(player.getUniqueId(), field);
        player.sendMessage(Text.prefixed("&fType the new &e" + field.getLabel() + "&f in chat."));
        player.sendMessage(Text.prefixed("&7Type &ccancel&7 to abort."));
    }

    public void cancel(Player player) {
        this.pending.remove(player.getUniqueId());
    }

    public void handleInput(Player player, String message) {
        Field field = this.pending.remove(player.getUniqueId());
        if (field == null) {
            return;
        }
        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(Text.prefixed("&cCancelled."));
            return;
        }
        switch (field.ordinal()) {
            case 0: {
                this.plugin.setTabTitle(message);
                break;
            }
            case 1: {
                this.plugin.setTabDiscord(message);
                break;
            }
            case 2: {
                this.plugin.setTabStore(message);
            }
        }
        player.sendMessage(Text.prefixed("&aTab " + field.name().toLowerCase() + " updated."));
    }

    public static enum Field {
        TITLE("tab title (MiniMessage, gradients allowed)"),
        DISCORD("discord line"),
        STORE("store line");

        private final String label;

        private Field(String label) {
            this.label = label;
        }

        public String getLabel() {
            return this.label;
        }
    }
}


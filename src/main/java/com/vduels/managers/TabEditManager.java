package com.vduels.managers;

import com.vduels.VDuels;
import com.vduels.util.Text;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Backs the {@code /vduelstab} GUI: when an admin clicks a field they are asked
 * to type the new value in chat, which this manager captures and saves.
 */
public class TabEditManager {

    public enum Field {
        TITLE("tab title (MiniMessage, gradients allowed)"),
        DISCORD("discord line"),
        STORE("store line");

        private final String label;

        Field(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final VDuels plugin;
    private final Map<UUID, Field> pending = new HashMap<>();

    public TabEditManager(VDuels plugin) {
        this.plugin = plugin;
    }

    public boolean isEditing(UUID id) {
        return pending.containsKey(id);
    }

    public void begin(Player player, Field field) {
        pending.put(player.getUniqueId(), field);
        player.sendMessage(Text.prefixed("&fType the new &e" + field.getLabel() + "&f in chat."));
        player.sendMessage(Text.prefixed("&7Type &ccancel&7 to abort."));
    }

    public void cancel(Player player) {
        pending.remove(player.getUniqueId());
    }

    /** Handle a chat line from an editing admin. Must run on the main thread. */
    public void handleInput(Player player, String message) {
        Field field = pending.remove(player.getUniqueId());
        if (field == null) {
            return;
        }
        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(Text.prefixed("&cCancelled."));
            return;
        }
        switch (field) {
            case TITLE -> plugin.setTabTitle(message);
            case DISCORD -> plugin.setTabDiscord(message);
            case STORE -> plugin.setTabStore(message);
        }
        player.sendMessage(Text.prefixed("&aTab " + field.name().toLowerCase() + " updated."));
    }
}

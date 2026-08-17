package com.vduels.managers;

import com.vduels.VDuels;
import com.vduels.model.ActiveDuel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Lets players spectate others with {@code /spectate <player>} (alias
 * {@code /spec}, chat {@code .leave} to exit). When the target is in a duel the
 * spectator gets that fight's scoreboard and isolated tab; when the fight ends
 * (or the spectator stops) they are returned exactly where they were.
 */
public class SpectateManager {

    private final VDuels plugin;
    private final Map<UUID, Location> returnLocation = new HashMap<>();
    private final Map<UUID, GameMode> returnMode = new HashMap<>();
    private final Map<UUID, UUID> target = new HashMap<>();     // spectator -> watched player
    private final Set<UUID> watchingFight = new HashSet<>();     // spectators mirroring a duel

    public SpectateManager(VDuels plugin) {
        this.plugin = plugin;
    }

    public boolean isSpectating(UUID id) {
        return returnLocation.containsKey(id);
    }

    public void spectate(Player viewer, Player targetPlayer) {
        UUID id = viewer.getUniqueId();
        if (plugin.getDuelManager().isInDuel(id)) {
            viewer.sendMessage(plugin.messages().get("spectate.in-duel"));
            return;
        }
        if (id.equals(targetPlayer.getUniqueId())) {
            viewer.sendMessage(plugin.messages().get("spectate.self"));
            return;
        }
        // Leaving a previous target? Tell them and drop the old fight view.
        UUID previous = target.get(id);
        if (previous != null && !previous.equals(targetPlayer.getUniqueId())) {
            notify(previous, "spectate.stopped-watching", "name", viewer.getName());
            dropFightView(id);
        }
        if (!returnLocation.containsKey(id)) {
            returnLocation.put(id, viewer.getLocation());
            returnMode.put(id, viewer.getGameMode());
        }
        viewer.setGameMode(GameMode.SPECTATOR);
        viewer.teleport(targetPlayer.getLocation());
        target.put(id, targetPlayer.getUniqueId());
        viewer.sendMessage(plugin.messages().get("spectate.now", "target", targetPlayer.getName()));
        notify(targetPlayer.getUniqueId(), "spectate.started-watching", "name", viewer.getName());

        ActiveDuel duel = plugin.getDuelManager().getDuel(targetPlayer.getUniqueId());
        if (duel != null) {
            plugin.getScoreboardService().attachSpectator(viewer, duel, targetPlayer.getUniqueId());
            plugin.getTabService().attachSpectator(viewer, duel);
            watchingFight.add(id);
        }
    }

    public void stop(Player viewer) {
        UUID id = viewer.getUniqueId();
        if (!returnLocation.containsKey(id)) {
            viewer.sendMessage(plugin.messages().get("spectate.not-spectating"));
            return;
        }
        UUID watched = target.get(id);
        if (watched != null) {
            notify(watched, "spectate.stopped-watching", "name", viewer.getName());
        }
        dropFightView(id);
        restore(viewer);
        viewer.sendMessage(plugin.messages().get("spectate.stopped"));
    }

    /** On quit: restore game mode so they don't rejoin stuck in spectator. */
    public void clearOnQuit(Player viewer) {
        UUID id = viewer.getUniqueId();
        if (!returnLocation.containsKey(id)) {
            return;
        }
        UUID watched = target.get(id);
        if (watched != null) {
            notify(watched, "spectate.stopped-watching", "name", viewer.getName());
        }
        dropFightView(id);
        GameMode mode = returnMode.remove(id);
        returnLocation.remove(id);
        target.remove(id);
        viewer.setGameMode(mode == null ? GameMode.SURVIVAL : mode);
    }

    /** Returns spectators to safety once the fight they were watching has ended. */
    public void tick() {
        if (watchingFight.isEmpty()) {
            return;
        }
        for (UUID id : new HashSet<>(watchingFight)) {
            UUID watched = target.get(id);
            Player viewer = org.bukkit.Bukkit.getPlayer(id);
            if (viewer == null) {
                watchingFight.remove(id);
                continue;
            }
            if (watched == null || !plugin.getDuelManager().isInDuel(watched)) {
                restore(viewer);
                dropFightView(id);
                viewer.sendMessage(plugin.messages().get("spectate.fight-ended"));
            }
        }
    }

    private void dropFightView(UUID id) {
        watchingFight.remove(id);
        plugin.getScoreboardService().detach(id);
        plugin.getTabService().detachSpectator(id);
    }

    private void restore(Player viewer) {
        UUID id = viewer.getUniqueId();
        GameMode mode = returnMode.remove(id);
        Location location = returnLocation.remove(id);
        target.remove(id);
        viewer.setGameMode(mode == null ? GameMode.SURVIVAL : mode);
        if (location != null) {
            viewer.teleport(location);
        }
    }

    /** Sends a gray, normal-font notice to a player (only the name varies). */
    private void notify(UUID playerId, String key, String... placeholders) {
        Player player = org.bukkit.Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        String text = plugin.messages().raw(key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            text = text.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>" + text + "</gray>"));
    }
}

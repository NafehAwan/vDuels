package com.vduels.managers;

import com.vduels.VDuels;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lets players spectate others with {@code /spectate <player>} (alias
 * {@code /spec}). Their location and game mode are remembered so {@code /spectate}
 * with no argument returns them exactly where they were.
 */
public class SpectateManager {

    private final VDuels plugin;
    private final Map<UUID, Location> returnLocation = new HashMap<>();
    private final Map<UUID, GameMode> returnMode = new HashMap<>();

    public SpectateManager(VDuels plugin) {
        this.plugin = plugin;
    }

    public boolean isSpectating(UUID id) {
        return returnLocation.containsKey(id);
    }

    public void spectate(Player viewer, Player target) {
        UUID id = viewer.getUniqueId();
        if (plugin.getDuelManager().isInDuel(id)) {
            viewer.sendMessage(plugin.messages().get("spectate.in-duel"));
            return;
        }
        if (viewer.getUniqueId().equals(target.getUniqueId())) {
            viewer.sendMessage(plugin.messages().get("spectate.self"));
            return;
        }
        // Remember where to return only on the first /spectate.
        if (!returnLocation.containsKey(id)) {
            returnLocation.put(id, viewer.getLocation());
            returnMode.put(id, viewer.getGameMode());
        }
        viewer.setGameMode(GameMode.SPECTATOR);
        viewer.teleport(target.getLocation());
        viewer.sendMessage(plugin.messages().get("spectate.now", "target", target.getName()));
    }

    public void stop(Player viewer) {
        UUID id = viewer.getUniqueId();
        if (!returnLocation.containsKey(id)) {
            viewer.sendMessage(plugin.messages().get("spectate.not-spectating"));
            return;
        }
        restore(viewer);
        viewer.sendMessage(plugin.messages().get("spectate.stopped"));
    }

    /** On quit: put the game mode back so they don't rejoin stuck in spectator. */
    public void clearOnQuit(Player viewer) {
        UUID id = viewer.getUniqueId();
        if (returnLocation.containsKey(id)) {
            GameMode mode = returnMode.remove(id);
            returnLocation.remove(id);
            viewer.setGameMode(mode == null ? GameMode.SURVIVAL : mode);
        }
    }

    private void restore(Player viewer) {
        UUID id = viewer.getUniqueId();
        GameMode mode = returnMode.remove(id);
        Location location = returnLocation.remove(id);
        viewer.setGameMode(mode == null ? GameMode.SURVIVAL : mode);
        if (location != null) {
            viewer.teleport(location);
        }
    }
}

package com.vduels.managers;

import com.vduels.VDuels;
import com.vduels.model.ActiveDuel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The 1v1 matchmaking queues, one per kit. A player joins a kit's queue from the
 * {@code /queue} menu; when two players share a queue they are matched into a
 * duel on a free compatible arena. Each player can only be queued for one kit at
 * a time.
 */
public class QueueManager {

    /** Players needed to start a match (1v1). */
    public static final int NEEDED = 2;

    private final VDuels plugin;
    // kit id (lower-case) -> ordered set of queued player ids
    private final Map<String, LinkedHashSet<UUID>> queues = new LinkedHashMap<>();

    public QueueManager(VDuels plugin) {
        this.plugin = plugin;
    }

    /** How many players are waiting in the given kit's queue. */
    public int queued(String kit) {
        LinkedHashSet<UUID> set = queues.get(key(kit));
        return set == null ? 0 : set.size();
    }

    /** How many players are currently in a duel that uses the given kit. */
    public int dueling(String kit) {
        int count = 0;
        for (UUID id : plugin.getDuelManager().duellingPlayers()) {
            ActiveDuel duel = plugin.getDuelManager().getDuel(id);
            if (duel != null && duel.getKit().equalsIgnoreCase(kit)) {
                count++;
            }
        }
        return count;
    }

    public boolean isQueued(UUID id) {
        for (LinkedHashSet<UUID> set : queues.values()) {
            if (set.contains(id)) {
                return true;
            }
        }
        return false;
    }

    /** The kit a player is queued for, or null. */
    public String queuedKit(UUID id) {
        for (Map.Entry<String, LinkedHashSet<UUID>> e : queues.entrySet()) {
            if (e.getValue().contains(id)) {
                return e.getKey();
            }
        }
        return null;
    }

    /** Join (or switch to) a kit's queue, then try to match. */
    public void join(Player player, String kit) {
        UUID id = player.getUniqueId();
        if (plugin.getDuelManager().isInDuel(id)) {
            player.sendMessage(msg("queue.in-duel"));
            return;
        }
        if (!plugin.getKitManager().exists(kit)) {
            player.sendMessage(msg("queue.kit-gone"));
            return;
        }
        String k = key(kit);
        String current = queuedKit(id);
        if (k.equals(current)) {
            player.sendMessage(msg("queue.already", "kit", kit));
            return;
        }
        if (current != null) {
            remove(id); // switching queues
        }
        queues.computeIfAbsent(k, x -> new LinkedHashSet<>()).add(id);
        player.sendMessage(msg("queue.joined", "kit", kit,
                "queued", String.valueOf(queued(kit)), "needed", String.valueOf(NEEDED)));
        tryMatch(k);
    }

    /** Remove a player from whatever queue they are in (no message). */
    public void remove(UUID id) {
        for (Iterator<LinkedHashSet<UUID>> it = queues.values().iterator(); it.hasNext(); ) {
            LinkedHashSet<UUID> set = it.next();
            if (set.remove(id) && set.isEmpty()) {
                it.remove();
            }
        }
    }

    /** {@code /queue leave}: leave the current queue with a message. */
    public void leave(Player player) {
        if (!isQueued(player.getUniqueId())) {
            player.sendMessage(msg("queue.not-queued"));
            return;
        }
        remove(player.getUniqueId());
        player.sendMessage(msg("queue.left"));
    }

    private void tryMatch(String kit) {
        LinkedHashSet<UUID> set = queues.get(kit);
        while (set != null && set.size() >= NEEDED) {
            List<UUID> ids = new ArrayList<>(set);
            Player p1 = Bukkit.getPlayer(ids.get(0));
            Player p2 = Bukkit.getPlayer(ids.get(1));
            // Drop anyone who went offline and retry.
            if (p1 == null) {
                set.remove(ids.get(0));
                continue;
            }
            if (p2 == null) {
                set.remove(ids.get(1));
                continue;
            }
            boolean started = plugin.getDuelManager().startQueuedDuel(p1, p2, kit);
            if (!started) {
                // No free arena right now; leave them queued and stop trying.
                p1.sendMessage(msg("queue.no-arena"));
                p2.sendMessage(msg("queue.no-arena"));
                break;
            }
            set.remove(ids.get(0));
            set.remove(ids.get(1));
        }
        if (set != null && set.isEmpty()) {
            queues.remove(kit);
        }
    }

    private static String key(String kit) {
        return kit.toLowerCase(Locale.ROOT);
    }

    private String msg(String key, String... placeholders) {
        return plugin.messages().get(key, placeholders);
    }
}

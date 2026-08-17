package com.vduels.managers;

import com.vduels.VDuels;
import com.vduels.model.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The 1v1 matchmaking queues, one per kit. A player may sit in several kit
 * queues at once; clicking a kit toggles that queue. When two players share a
 * queue they are matched into a duel on a free compatible arena and pulled out
 * of every queue they were in (a player is only ever in one fight at a time).
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

    /** How many fights (duels) are currently running with the given kit. */
    public int dueling(String kit) {
        return plugin.getDuelManager().fightsWithKit(kit);
    }

    public boolean isQueuedFor(UUID id, String kit) {
        LinkedHashSet<UUID> set = queues.get(key(kit));
        return set != null && set.contains(id);
    }

    public boolean isQueued(UUID id) {
        for (LinkedHashSet<UUID> set : queues.values()) {
            if (set.contains(id)) {
                return true;
            }
        }
        return false;
    }

    /** The set of kit ids (lower-case) a player is currently queued for. */
    public Set<String> queuedKits(UUID id) {
        Set<String> out = new LinkedHashSet<>();
        for (Map.Entry<String, LinkedHashSet<UUID>> e : queues.entrySet()) {
            if (e.getValue().contains(id)) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    /** Clicking a kit: join its queue, or leave it if already queued for it. */
    public void toggle(Player player, String kit) {
        if (isQueuedFor(player.getUniqueId(), kit)) {
            leaveKit(player, kit);
        } else {
            join(player, kit);
        }
    }

    /** Join a kit's queue (in addition to any others), then try to match. */
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
        queues.computeIfAbsent(key(kit), x -> new LinkedHashSet<>()).add(id);
        player.sendMessage(queuedLine("queue.joined-text", kit));
        tryMatch(key(kit));
    }

    /** Leave one kit's queue with a chat notice. */
    public void leaveKit(Player player, String kit) {
        LinkedHashSet<UUID> set = queues.get(key(kit));
        if (set == null || !set.remove(player.getUniqueId())) {
            return;
        }
        if (set.isEmpty()) {
            queues.remove(key(kit));
        }
        player.sendMessage(queuedLine("queue.left-text", kit));
    }

    /** Remove a player from every queue silently (on match start / quit). */
    public void remove(UUID id) {
        for (Iterator<LinkedHashSet<UUID>> it = queues.values().iterator(); it.hasNext(); ) {
            LinkedHashSet<UUID> set = it.next();
            if (set.remove(id) && set.isEmpty()) {
                it.remove();
            }
        }
    }

    /** {@code /queue leave}: leave all queues with a message. */
    public void leaveAll(Player player) {
        if (!isQueued(player.getUniqueId())) {
            player.sendMessage(msg("queue.not-queued"));
            return;
        }
        remove(player.getUniqueId());
        player.sendMessage(msg("queue.left"));
    }

    private void tryMatch(String kit) {
        while (true) {
            LinkedHashSet<UUID> set = queues.get(kit);
            if (set == null || set.size() < NEEDED) {
                return;
            }
            List<UUID> ids = new ArrayList<>(set);
            Player p1 = Bukkit.getPlayer(ids.get(0));
            if (p1 == null) {
                remove(ids.get(0));
                continue;
            }
            Player p2 = Bukkit.getPlayer(ids.get(1));
            if (p2 == null) {
                remove(ids.get(1));
                continue;
            }
            boolean started = plugin.getDuelManager().startQueuedDuel(p1, p2, kit);
            if (!started) {
                // No free arena right now; leave them queued and stop trying.
                p1.sendMessage(msg("queue.no-arena"));
                p2.sendMessage(msg("queue.no-arena"));
                return;
            }
            // startDuel already removed both players from every queue.
        }
    }

    /**
     * A "normal font, gray" line whose only styled part is the kit's display
     * name, e.g. "You have been queued to &lt;gradient&gt;name&lt;/gradient&gt;.".
     * Built as one MiniMessage component so the gray text stays in the vanilla
     * font while the kit name keeps its own colours.
     */
    private Component queuedLine(String textKey, String kit) {
        String prefix = plugin.messages().raw(textKey);
        return MiniMessage.miniMessage().deserialize(
                "<gray>" + prefix + "</gray>" + kitMini(kit) + "<gray>.</gray>");
    }

    /** The kit's MiniMessage display name, or its id wrapped in gray. */
    private String kitMini(String kit) {
        Kit k = plugin.getKitManager().get(kit);
        if (k != null && k.getDisplayName() != null && !k.getDisplayName().isEmpty()) {
            return k.getDisplayName();
        }
        return "<gray>" + kit + "</gray>";
    }

    private static String key(String kit) {
        return kit.toLowerCase(Locale.ROOT);
    }

    private String msg(String key, String... placeholders) {
        return plugin.messages().get(key, placeholders);
    }
}

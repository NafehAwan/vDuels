/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.minimessage.MiniMessage
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.potion.PotionEffect
 */
package com.meowduels.managers;

import com.meowduels.MeowDuels;
import com.meowduels.model.Kit;
import com.meowduels.util.SpawnItems;
import com.meowduels.util.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public class QueueManager {
    public static final int NEEDED = 2;
    private final MeowDuels plugin;
    private final Map<String, LinkedHashSet<UUID>> queues = new LinkedHashMap<String, LinkedHashSet<UUID>>();
    private final Map<UUID, Long> waitingSince = new HashMap<UUID, Long>();

    public QueueManager(MeowDuels plugin) {
        this.plugin = plugin;
    }

    public int queued(String kit) {
        LinkedHashSet<UUID> set = this.queues.get(QueueManager.key(kit));
        return set == null ? 0 : set.size();
    }

    public void quickJoin(Player player) {
        UUID id = player.getUniqueId();
        ArrayList<Kit> candidates = new ArrayList<Kit>();
        for (Kit k : this.plugin.getKitManager().all()) {
            if (!this.plugin.getDuelManager().hasUsableArena(k.getName()) || this.isQueuedFor(id, k.getName())) continue;
            candidates.add(k);
        }
        if (candidates.isEmpty()) {
            player.sendMessage(this.msg("queue.no-arena", new String[0]));
            return;
        }
        Kit target = null;
        for (Kit k : candidates) {
            if (this.queued(k.getName()) <= 0) continue;
            target = k;
            break;
        }
        if (target == null) {
            target = (Kit)candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        }
        this.join(player, target.getName());
    }

    public int dueling(String kit) {
        return this.plugin.getDuelManager().fightsWithKit(kit);
    }

    public boolean isQueuedFor(UUID id, String kit) {
        LinkedHashSet<UUID> set = this.queues.get(QueueManager.key(kit));
        return set != null && set.contains(id);
    }

    public boolean isQueued(UUID id) {
        for (LinkedHashSet<UUID> set : this.queues.values()) {
            if (!set.contains(id)) continue;
            return true;
        }
        return false;
    }

    public Set<String> queuedKits(UUID id) {
        LinkedHashSet<String> out = new LinkedHashSet<String>();
        for (Map.Entry<String, LinkedHashSet<UUID>> e : this.queues.entrySet()) {
            if (!e.getValue().contains(id)) continue;
            out.add(e.getKey());
        }
        return out;
    }

    public void toggle(Player player, String kit) {
        if (this.isQueuedFor(player.getUniqueId(), kit)) {
            this.leaveKit(player, kit);
        } else {
            this.join(player, kit);
        }
    }

    public void join(Player player, String kit) {
        UUID id = player.getUniqueId();
        if (this.plugin.getDuelManager().isInDuel(id)) {
            player.sendMessage(this.msg("queue.in-duel", new String[0]));
            return;
        }
        if (this.plugin.getEventManager().isInvolved(id)) {
            player.sendMessage(Text.prefixed("&cYou can't queue while you're in the event."));
            return;
        }
        if (!this.plugin.getKitManager().exists(kit)) {
            player.sendMessage(this.msg("queue.kit-gone", new String[0]));
            return;
        }
        if (!this.plugin.getDuelManager().hasUsableArena(kit)) {
            player.sendMessage(this.msg("queue.no-arena", new String[0]));
            return;
        }
        for (PotionEffect e : player.getActivePotionEffects()) {
            player.removePotionEffect(e.getType());
        }
        this.queues.computeIfAbsent(QueueManager.key(kit), x -> new LinkedHashSet()).add(id);
        if (!this.waitingSince.containsKey(id)) {
            this.waitingSince.put(id, System.currentTimeMillis());
        }
        player.sendMessage(this.queuedLine("queue.joined-text", kit));
        this.tryMatch(QueueManager.key(kit));
        if (this.isQueued(id) && player.isOnline()) {
            SpawnItems.setLeaveQueue(this.plugin, player, true);
        }
    }

    public void leaveKit(Player player, String kit) {
        LinkedHashSet<UUID> set = this.queues.get(QueueManager.key(kit));
        if (set == null || !set.remove(player.getUniqueId())) {
            return;
        }
        if (set.isEmpty()) {
            this.queues.remove(QueueManager.key(kit));
        }
        player.sendMessage(this.queuedLine("queue.left-text", kit));
        if (!this.isQueued(player.getUniqueId())) {
            this.waitingSince.remove(player.getUniqueId());
            SpawnItems.setLeaveQueue(this.plugin, player, false);
        }
    }

    public void remove(UUID id) {
        Iterator<LinkedHashSet<UUID>> it = this.queues.values().iterator();
        while (it.hasNext()) {
            LinkedHashSet<UUID> set = it.next();
            if (!set.remove(id) || !set.isEmpty()) continue;
            it.remove();
        }
        this.waitingSince.remove(id);
        Player player = Bukkit.getPlayer((UUID)id);
        if (player != null && player.isOnline()) {
            SpawnItems.setLeaveQueue(this.plugin, player, false);
        }
    }

    public long waitingSeconds(UUID id) {
        Long since = this.waitingSince.get(id);
        if (since == null) {
            return 0L;
        }
        return Math.max(0L, (System.currentTimeMillis() - since) / 1000L);
    }

    public void leaveAll(Player player) {
        if (!this.isQueued(player.getUniqueId())) {
            player.sendMessage(this.msg("queue.not-queued", new String[0]));
            return;
        }
        this.remove(player.getUniqueId());
        player.sendMessage(this.msg("queue.left", new String[0]));
    }

    private void tryMatch(String kit) {
        Player p2;
        Player p1;
        while (true) {
            LinkedHashSet<UUID> set;
            if ((set = this.queues.get(kit)) == null || set.size() < 2) {
                return;
            }
            ArrayList<UUID> ids = new ArrayList<UUID>(set);
            p1 = Bukkit.getPlayer((UUID)ids.get(0));
            if (p1 == null || this.plugin.getDuelManager().isInDuel(ids.get(0))) {
                this.remove(ids.get(0));
                continue;
            }
            p2 = Bukkit.getPlayer((UUID)ids.get(1));
            if (p2 == null || this.plugin.getDuelManager().isInDuel(ids.get(1))) {
                this.remove(ids.get(1));
                continue;
            }
            boolean started = this.plugin.getDuelManager().startQueuedDuel(p1, p2, kit);
            if (!started) break;
        }
        p1.sendMessage(this.msg("queue.no-arena", new String[0]));
        p2.sendMessage(this.msg("queue.no-arena", new String[0]));
    }

    private Component queuedLine(String textKey, String kit) {
        String prefix = this.plugin.messages().raw(textKey);
        return MiniMessage.miniMessage().deserialize((Object)("<gray>" + prefix + "</gray>" + this.kitMini(kit) + "<gray>.</gray>"));
    }

    private String kitMini(String kit) {
        Kit k = this.plugin.getKitManager().get(kit);
        if (k != null && k.getDisplayName() != null && !k.getDisplayName().isEmpty()) {
            return k.getDisplayName();
        }
        return "<gray>" + kit + "</gray>";
    }

    private static String key(String kit) {
        return kit.toLowerCase(Locale.ROOT);
    }

    private String msg(String key, String ... placeholders) {
        return this.plugin.messages().get(key, placeholders);
    }
}


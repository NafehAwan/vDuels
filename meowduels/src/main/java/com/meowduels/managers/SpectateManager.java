/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.minimessage.MiniMessage
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 */
package com.meowduels.managers;

import com.meowduels.MeowDuels;
import com.meowduels.model.ActiveDuel;
import com.meowduels.model.Arena;
import com.meowduels.model.Party;
import com.meowduels.util.GameModeGuard;
import com.meowduels.util.Text;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SpectateManager {
    private final MeowDuels plugin;
    // Concurrent: isSpectating()/getWatchedTarget() are read from TAB's thread
    // while the main thread starts and stops spectating.
    private final Map<UUID, Location> returnLocation = new java.util.concurrent.ConcurrentHashMap<UUID, Location>();
    private final Map<UUID, GameMode> returnMode = new HashMap<UUID, GameMode>();
    private final Map<UUID, UUID> target = new java.util.concurrent.ConcurrentHashMap<UUID, UUID>();
    private final Set<UUID> watchingFight = new HashSet<UUID>();

    public SpectateManager(MeowDuels plugin) {
        this.plugin = plugin;
    }

    public boolean isSpectating(UUID id) {
        return this.returnLocation.containsKey(id);
    }

    public int countWatchers(UUID p1, UUID p2) {
        int watching = 0;
        for (UUID watched : this.target.values()) {
            if (watched == null || !watched.equals(p1) && !watched.equals(p2)) continue;
            ++watching;
        }
        return watching;
    }

    public UUID getWatchedTarget(UUID viewer) {
        return this.target.get(viewer);
    }

    public void spectate(Player viewer, Player targetPlayer) {
        UUID id = viewer.getUniqueId();
        if (this.plugin.getDuelManager().isInDuel(id)) {
            viewer.sendMessage(this.plugin.messages().get("spectate.in-duel", new String[0]));
            return;
        }
        if (this.plugin.getEventManager().isInvolved(id)) {
            viewer.sendMessage(Text.prefixed("&cYou can't spectate a duel while you're in the event."));
            return;
        }
        if (id.equals(targetPlayer.getUniqueId())) {
            viewer.sendMessage(this.plugin.messages().get("spectate.self", new String[0]));
            return;
        }
        ActiveDuel duel = this.plugin.getDuelManager().getDuel(targetPlayer.getUniqueId());
        // A party match counts as a fight to watch. It has no ActiveDuel, so
        // everything below that needs one is guarded rather than assumed.
        Party party = duel != null ? null : this.plugin.getPartyManager().partyOf(targetPlayer.getUniqueId());
        if (party != null && !this.plugin.getPartyManager().inPartyMatch(targetPlayer.getUniqueId())) {
            party = null;
        }
        if (duel == null && party == null) {
            viewer.sendMessage(this.plugin.messages().get("spectate.target-not-in-fight", "target", targetPlayer.getName()));
            return;
        }
        if (this.plugin.getPartyManager().inPartyMatch(id)) {
            viewer.sendMessage(Text.prefixed("&cYou can't spectate while you're in a party match."));
            return;
        }
        Arena arena = duel != null ? duel.getArena() : party.getArena();
        if (arena != null && !arena.isAllowSpectators() && !viewer.hasPermission("meowduels.admin")) {
            viewer.sendMessage(this.plugin.messages().get("spectate.not-allowed", new String[0]));
            return;
        }
        // The fighter's own choice, from /settings. Same admin bypass as the
        // arena flag above, and the same silence towards the fighter: they
        // turned spectators off so they would stop hearing about them.
        if (!this.plugin.getPlayerSettings().isSpectators(targetPlayer.getUniqueId()) && !viewer.hasPermission("meowduels.admin")) {
            viewer.sendMessage(Text.prefixed("&c" + targetPlayer.getName() + " has spectators turned off."));
            return;
        }
        UUID previous = this.target.get(id);
        if (previous != null && !previous.equals(targetPlayer.getUniqueId())) {
            this.notify(previous, "spectate.stopped-watching", "name", viewer.getName());
            this.dropFightView(id);
        }
        if (!this.returnLocation.containsKey(id)) {
            this.returnLocation.put(id, viewer.getLocation());
            GameMode current = viewer.getGameMode();
            this.returnMode.put(id, current == GameMode.SPECTATOR ? GameMode.SURVIVAL : current);
        }
        GameModeGuard.pin(viewer, GameMode.SPECTATOR);
        viewer.teleport(targetPlayer.getLocation());
        this.target.put(id, targetPlayer.getUniqueId());
        viewer.sendMessage(this.plugin.messages().get("spectate.now", "target", targetPlayer.getName()));
        this.notify(targetPlayer.getUniqueId(), "spectate.started-watching", "name", viewer.getName());
        if (duel != null) {
            this.plugin.getScoreboardService().attachSpectator(viewer, duel, targetPlayer.getUniqueId());
            this.plugin.getTabService().attachSpectator(viewer, duel);
        } else {
            // Not optional: a party in a match hides its fighters from
            // everyone else's world as well as their tab, so without joining
            // the bubble the spectator arrives in an arena with nobody in it.
            this.plugin.getTabService().attachSpectator(viewer, party);
        }
        this.watchingFight.add(id);
    }

    public void stop(Player viewer) {
        UUID id = viewer.getUniqueId();
        if (!this.returnLocation.containsKey(id)) {
            viewer.sendMessage(this.plugin.messages().get("spectate.not-spectating", new String[0]));
            return;
        }
        UUID watched = this.target.get(id);
        if (watched != null) {
            this.notify(watched, "spectate.stopped-watching", "name", viewer.getName());
        }
        this.dropFightView(id);
        this.restore(viewer);
        viewer.sendMessage(this.plugin.messages().get("spectate.stopped", new String[0]));
    }

    public void clearOnQuit(Player viewer) {
        UUID id = viewer.getUniqueId();
        if (!this.returnLocation.containsKey(id)) {
            return;
        }
        UUID watched = this.target.get(id);
        if (watched != null) {
            this.notify(watched, "spectate.stopped-watching", "name", viewer.getName());
        }
        this.dropFightView(id);
        GameMode mode = this.returnMode.remove(id);
        this.returnLocation.remove(id);
        this.target.remove(id);
        GameModeGuard.setFreely(viewer, mode == null ? GameMode.SURVIVAL : mode);
    }

    /** Pulls every spectator of this match back to the player they are watching.
     *  Called when a round starts: the fighters are teleported to their spawns,
     *  and without this their spectators are left behind at the previous round's
     *  death spot, watching an empty arena. */
    public void followRoundStart(UUID p1, UUID p2) {
        for (Map.Entry<UUID, UUID> e : this.target.entrySet()) {
            UUID watched = e.getValue();
            if (watched == null || (!watched.equals(p1) && !watched.equals(p2))) {
                continue;
            }
            Player viewer = Bukkit.getPlayer((UUID) e.getKey());
            Player subject = Bukkit.getPlayer((UUID) watched);
            if (viewer != null && subject != null) {
                viewer.teleport(subject.getLocation());
            }
        }
    }

    public void tick() {
        if (this.watchingFight.isEmpty()) {
            return;
        }
        for (UUID id : new HashSet<UUID>(this.watchingFight)) {
            UUID watched = this.target.get(id);
            Player viewer = Bukkit.getPlayer((UUID)id);
            if (viewer == null) {
                this.watchingFight.remove(id);
                continue;
            }
            if (watched != null && this.stillFighting(watched)) {
                // Rounds move the fight; a party match can even move arena.
                // Only follow across worlds, so a spectator flying around
                // inside the arena is left where they put themselves.
                Player subject = Bukkit.getPlayer((UUID)watched);
                if (subject != null && subject.getLocation().getWorld() != viewer.getLocation().getWorld()) {
                    viewer.teleport(subject.getLocation());
                }
                continue;
            }
            this.restore(viewer);
            this.dropFightView(id);
            viewer.sendMessage(this.plugin.messages().get("spectate.fight-ended", new String[0]));
        }
    }

    /** In a duel or in a party match - either is something to watch. */
    private boolean stillFighting(UUID id) {
        return this.plugin.getDuelManager().isInDuel(id)
                || this.plugin.getPartyManager().inPartyMatch(id);
    }

    private void dropFightView(UUID id) {
        this.watchingFight.remove(id);
        this.plugin.getScoreboardService().detach(id);
        this.plugin.getTabService().detachSpectator(id);
    }

    private void restore(Player viewer) {
        UUID id = viewer.getUniqueId();
        GameMode mode = this.returnMode.remove(id);
        Location location = this.returnLocation.remove(id);
        this.target.remove(id);
        GameModeGuard.setFreely(viewer, mode == null ? GameMode.SURVIVAL : mode);
        if (location != null) {
            viewer.teleport(location);
        }
    }

    private void notify(UUID playerId, String key, String ... placeholders) {
        Player player = Bukkit.getPlayer((UUID)playerId);
        if (player == null) {
            return;
        }
        String text = this.plugin.messages().raw(key);
        int i = 0;
        while (i + 1 < placeholders.length) {
            text = text.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            i += 2;
        }
        player.sendMessage(MiniMessage.miniMessage().deserialize((Object)("<gray>" + text + "</gray>")));
    }
}


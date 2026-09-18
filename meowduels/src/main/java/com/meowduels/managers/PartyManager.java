package com.meowduels.managers;

import com.meowduels.MeowDuels;
import com.meowduels.model.Arena;
import com.meowduels.model.Kit;
import com.meowduels.model.Party;
import com.meowduels.model.PlayerSnapshot;
import com.meowduels.util.AntiCheatBypass;
import com.meowduels.util.Colors;
import com.meowduels.util.GameModeGuard;
import com.meowduels.util.Sounds;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import com.meowduels.util.SpawnItems;
import com.meowduels.util.Text;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

/**
 * Parties, and the private free-for-all they fight.
 *
 * <p>Two rules shape everything here.
 *
 * <p>First, a party is exclusive. While you are in one you cannot queue, duel,
 * join an event or start an FFA, because every one of those would pull you out
 * of the group the party exists to keep together - and half of them would leave
 * the party holding a snapshot of an inventory you no longer have. {@link #busy}
 * is the single gate the rest of the plugin asks.
 *
 * <p>Second, a party match is over for you when you die, but not finished. You
 * are moved to spectating rather than teleported out, because the interesting
 * part of a party fight is watching how the rest of it goes; /leave is there for
 * anyone who disagrees.
 */
public class PartyManager {
    private final MeowDuels plugin;
    /** Concurrent: party membership is read by the tab/placeholder thread. */
    private final Map<UUID, Party> byPlayer = new ConcurrentHashMap<UUID, Party>();

    public PartyManager(MeowDuels plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- state

    public Party partyOf(UUID id) {
        return id == null ? null : this.byPlayer.get(id);
    }

    public boolean inParty(UUID id) {
        return this.byPlayer.containsKey(id);
    }

    /** True while this player's party is mid-match, whether they are still
     *  alive in it or watching after being knocked out. */
    public boolean inPartyMatch(UUID id) {
        Party party = this.partyOf(id);
        return party != null && party.isFighting() && party.involved().contains(id);
    }

    public boolean isWatching(UUID id) {
        Party party = this.partyOf(id);
        return party != null && party.getWatching().contains(id);
    }

    /**
     * Lowercased names of every arena a party match is currently holding.
     *
     * <p>DuelManager rebuilds its reservation set once a second from what is
     * really in use, and a party match is one of those things - without this it
     * would treat a party's arena as a leak and free it underneath them.
     */
    public Set<String> claimedArenas() {
        HashSet<String> out = new HashSet<String>();
        for (Party party : this.byPlayer.values()) {
            if (party.isFighting() && !party.isFinished() && party.getArena() != null) {
                out.add(party.getArena().getName().toLowerCase(java.util.Locale.ROOT));
            }
        }
        return out;
    }

    /**
     * The leaders whose parties have an outstanding invite for this player.
     *
     * <p>Only used for tab-completing /party accept and /party decline. The
     * invite lives on the party, not on the invitee, so answering "who invited
     * me" means asking every party - which is fine at the scale a party list
     * ever reaches, and beats keeping a second index in sync with the first.
     */
    public List<String> invitersOf(UUID id) {
        ArrayList<String> out = new ArrayList<String>();
        if (id == null) {
            return out;
        }
        for (Party party : new HashSet<Party>(this.byPlayer.values())) {
            if (party.isInvited(id)) {
                Player leader = Bukkit.getPlayer((UUID)party.getLeader());
                if (leader != null) {
                    out.add(leader.getName());
                }
            }
        }
        return out;
    }

    /** Turns an invite down: it stops being outstanding, and the leader is told
     *  rather than left waiting on a yes that is never coming. */
    public void decline(Player player, Player leader) {
        Party party = this.partyOf(leader.getUniqueId());
        if (party == null || !party.getInvited().remove(player.getUniqueId())) {
            player.sendMessage(Text.prefixed("&cYou have no invite from " + leader.getName() + "."));
            return;
        }
        player.sendMessage(this.msg("party.invite-declined", "leader", leader.getName()));
        leader.sendMessage(this.msg("party.invite-declined-by", "player", player.getName()));
    }

    public int partyCount() {
        HashSet<Party> seen = new HashSet<Party>(this.byPlayer.values());
        return seen.size();
    }

    // --------------------------------------------------------- membership

    public void create(Player player) {
        UUID id = player.getUniqueId();
        if (this.inParty(id)) {
            player.sendMessage(Text.prefixed("&cYou're already in a party."));
            return;
        }
        if (this.busyElsewhere(player)) {
            return;
        }
        Party party = new Party(id);
        this.byPlayer.put(id, party);
        player.sendMessage(Text.prefixed("&aParty created. &7Invite someone with &f/party invite <player>&7."));
        this.refreshItems(player);
        this.plugin.getTabService().attachParty(party);
    }

    public void invite(Player leader, Player target) {
        Party party = this.partyOf(leader.getUniqueId());
        if (party == null) {
            leader.sendMessage(Text.prefixed("&cYou're not in a party."));
            return;
        }
        if (!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(Text.prefixed("&cOnly the party leader can invite."));
            return;
        }
        if (party.isFighting()) {
            leader.sendMessage(Text.prefixed("&cYou can't invite during a party match."));
            return;
        }
        UUID targetId = target.getUniqueId();
        if (party.has(targetId)) {
            leader.sendMessage(Text.prefixed("&cThey're already in your party."));
            return;
        }
        if (this.inParty(targetId)) {
            leader.sendMessage(Text.prefixed("&c" + target.getName() + " is already in a party."));
            return;
        }
        party.invite(targetId);
        leader.sendMessage(this.msg("party.invite-sent", "player", target.getName()));
        this.sendInviteCard(target, leader, party);
    }

    public void accept(Player player, Player leader) {
        Party party = this.partyOf(leader.getUniqueId());
        UUID id = player.getUniqueId();
        if (party == null) {
            player.sendMessage(Text.prefixed("&cThat party no longer exists."));
            return;
        }
        if (this.inParty(id)) {
            player.sendMessage(Text.prefixed("&cLeave your current party first."));
            return;
        }
        if (!party.isInvited(id) && !party.isOpenToAll()) {
            player.sendMessage(Text.prefixed("&cYou haven't been invited to that party."));
            return;
        }
        if (party.isFighting()) {
            player.sendMessage(Text.prefixed("&cThat party is mid-match - try again when it ends."));
            return;
        }
        if (this.busyElsewhere(player)) {
            return;
        }
        party.add(id);
        this.byPlayer.put(id, party);
        this.broadcast(party, "&f" + player.getName() + "&a joined the party.");
        // Old spawn items out, party items in - see refreshItems.
        this.refreshItems(player);
        this.plugin.getTabService().attachParty(party);
        player.sendMessage(Text.prefixed("&7The party is run by &f" + leader.getName()
                + "&7 - they pick the kit and start the match."));
    }

    /**
     * The invite, as a card you can click rather than a command to retype.
     *
     * <p>The line it replaced told you the command and left you to type it,
     * which is the one part of accepting an invite a player can get wrong - and
     * the one part that has no reason to exist. Both buttons run the command for
     * you, and hover to say what they will do.
     *
     * <p>Decline is on the card for the same reason: without it the only way to
     * answer no is to ignore the message, which leaves the leader waiting on a
     * yes that is never coming.
     */
    private void sendInviteCard(Player target, Player leader, Party party) {
        target.sendMessage("");
        target.sendMessage(this.msg("party.invite-header"));
        target.sendMessage(this.msg("party.invite-from", "leader", leader.getName()));
        // Members only. The kit was on here and did not belong: at invite time
        // the leader usually has not picked one, and it can change any number of
        // times before a match starts - so it told you nothing you could act on.
        target.sendMessage(this.msg("party.invite-info", "members", String.valueOf(party.size())));
        target.sendMessage("");
        TextComponent accept = new TextComponent(this.msg("party.invite-accept"));
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept " + leader.getName()));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(this.msg("party.invite-accept-hover", "leader", leader.getName())).create()));
        TextComponent gap = new TextComponent(this.msg("party.invite-gap"));
        TextComponent decline = new TextComponent(this.msg("party.invite-decline"));
        decline.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party decline " + leader.getName()));
        decline.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(this.msg("party.invite-decline-hover", "leader", leader.getName())).create()));
        target.spigot().sendMessage(new BaseComponent[]{accept, gap, decline});
        target.sendMessage("");
        Sounds.request(target);
    }

    /**
     * Leaving. The leader leaving disbands - a party is their party, and
     * inventing an heir would surprise everyone still in it.
     */
    public void leave(Player player) {
        UUID id = player.getUniqueId();
        Party party = this.partyOf(id);
        if (party == null) {
            player.sendMessage(Text.prefixed("&cYou're not in a party."));
            return;
        }
        if (party.isLeader(id)) {
            this.disband(party, player.getName() + " disbanded the party.");
            return;
        }
        if (party.isFighting() && party.involved().contains(id)) {
            this.pullOut(party, id);
        }
        party.remove(id);
        this.byPlayer.remove(id);
        this.broadcast(party, "&f" + player.getName() + "&7 left the party.");
        player.sendMessage(Text.prefixed("&7You left the party."));
        this.refreshItems(player);
        this.plugin.getTabService().detach(id);
        this.plugin.getTabService().attachParty(party);
        this.checkWin(party);
    }

    public void disband(Party party, String reason) {
        if (party == null) {
            return;
        }
        if (party.isFighting()) {
            this.endMatch(party, null);
        }
        this.plugin.getTabService().detachParty(party);
        for (UUID id : new ArrayList<UUID>(party.getMembers())) {
            this.byPlayer.remove(id);
            Player p = Bukkit.getPlayer((UUID)id);
            if (p != null) {
                p.sendMessage(Text.prefixed("&c" + reason));
                this.refreshItems(p);
            }
        }
        party.getMembers().clear();
    }

    // -------------------------------------------------------------- match

    /**
     * Starts the party's free-for-all.
     *
     * <p>Every failure gets its own message. "You can't do that" for eight
     * different reasons is the fastest way to make a feature feel broken, and a
     * party match can fail on any of party size, kit, arena or arena setup.
     */
    public void startMatch(Player leader) {
        Party party = this.partyOf(leader.getUniqueId());
        if (party == null || !party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(Text.prefixed("&cOnly the party leader can start a match."));
            return;
        }
        if (party.isFighting()) {
            leader.sendMessage(Text.prefixed("&cYour party is already fighting."));
            return;
        }
        if (party.size() < 2) {
            leader.sendMessage(Text.prefixed("&cYou need at least &f2&c players in the party."));
            return;
        }
        Kit kit = party.getKit() == null ? null : this.plugin.getKitManager().get(party.getKit());
        if (kit == null) {
            leader.sendMessage(Text.prefixed("&cPick a kit first."));
            return;
        }
        List<Player> online = new ArrayList<Player>();
        for (UUID id : party.getMembers()) {
            Player p = Bukkit.getPlayer((UUID)id);
            if (p != null) {
                online.add(p);
            }
        }
        if (online.size() < 2) {
            leader.sendMessage(Text.prefixed("&cNot enough party members are online."));
            return;
        }
        Arena arena = this.plugin.getArenaManager().findFreeArena(
                a -> this.plugin.getDuelManager().isArenaInUse(a.getName())
                     || !a.isEventReady()
                     || !a.supportsKit(kit.getName()));
        if (arena == null) {
            leader.sendMessage(Text.prefixed("&cNo free arena with an event spawn supports that kit."));
            leader.sendMessage(Text.prefixed("&8Set one with the &fEvent Spawn&8 button in &f/arena&8."));
            return;
        }
        party.setArena(arena);
        party.setState(Party.State.FIGHTING);
        party.setFinished(false);
        party.setStartedAt(System.currentTimeMillis());
        party.getAlive().clear();
        party.getWatching().clear();
        party.getSnapshots().clear();
        this.plugin.getDuelManager().markArenaInUse(arena.getName());
        for (Player p : online) {
            this.sendIn(party, p, kit, arena);
        }
        int seconds = Math.max(1, this.plugin.getConfig().getInt("party.countdown-seconds", 5));
        party.setFightStartsAt(System.currentTimeMillis() + (long)seconds * 1000L);
        this.broadcast(party, this.msg("party.match-started", "count", String.valueOf(online.size()),
                "arena", arena.getName()));
        this.countdownTick(party, seconds);
    }

    private void sendIn(Party party, Player player, Kit kit, Arena arena) {
        UUID id = player.getUniqueId();
        party.getAlive().add(id);
        party.getSnapshots().put(id, PlayerSnapshot.capture(player));
        Location spawn = arena.getEventSpawn();
        String world = spawn != null && spawn.getWorld() != null ? spawn.getWorld().getName() : null;
        AntiCheatBypass.grant(this.plugin, player, AntiCheatBypass.worldNodes(this.plugin, world));
        if (spawn != null) {
            player.teleport(spawn);
        }
        for (PotionEffect e : player.getActivePotionEffects()) {
            player.removePotionEffect(e.getType());
        }
        GameModeGuard.pin(player, GameMode.SURVIVAL);
        try {
            player.setHealth(player.getMaxHealth());
        }
        catch (Throwable t) {
            player.setHealth(20.0);
        }
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setExhaustion(0.0f);
        player.setFireTicks(0);
        kit.applyTo(player);
        Kit fixed = kit;
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            Player pl = Bukkit.getPlayer((UUID)id);
            if (pl != null) {
                fixed.applyOffhand(pl);
            }
        }, 1L);
    }

    /**
     * The pre-fight countdown.
     *
     * <p>Guarded on identity, not just state: a party can finish a match and
     * start another one inside the countdown of the first if people are quick,
     * and a stray tick from the old one would then shout FIGHT over the new
     * one's countdown. Comparing fightStartsAt catches exactly that.
     */
    private void countdownTick(Party party, int secondsLeft) {
        if (!party.isFighting() || party.isFinished()) {
            return;
        }
        long deadline = party.getFightStartsAt();
        if (secondsLeft <= 0) {
            for (UUID id : party.getAlive()) {
                Player p = Bukkit.getPlayer((UUID)id);
                if (p != null) {
                    p.sendTitle(this.msg("party.countdown-go"), "", 0, 20, 10);
                    Sounds.fight(p);
                }
            }
            return;
        }
        for (UUID id : party.getAlive()) {
            Player p = Bukkit.getPlayer((UUID)id);
            if (p != null) {
                p.sendTitle(this.msg("party.countdown-title", "seconds", String.valueOf(secondsLeft)),
                        this.msg("party.countdown-subtitle"), 0, 25, 0);
                Sounds.countdown(p);
            }
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (party.getFightStartsAt() == deadline) {
                this.countdownTick(party, secondsLeft - 1);
            }
        }, 20L);
    }

    /**
     * A party member died mid-match.
     *
     * <p>They stay in the match as a spectator rather than being sent home. The
     * inventory snapshot is deliberately NOT restored yet - that happens when
     * the match ends or they /leave, so one code path puts everyone back.
     */
    public void onDeath(UUID id, UUID killerId) {
        Party party = this.partyOf(id);
        if (party == null || !party.isFighting() || !party.getAlive().remove(id)) {
            return;
        }
        if (killerId != null && !killerId.equals(id)) {
            party.addKill(killerId);
        }
        party.getWatching().add(id);
        Player p = Bukkit.getPlayer((UUID)id);
        if (p != null) {
            this.watchFrom(party, p);
            p.sendMessage(Text.prefixed("&7You're out - watching the rest. &f/leave&7 to stop."));
        }
        String left = String.valueOf(party.getAlive().size());
        if (killerId != null && !killerId.equals(id) && party.has(killerId)) {
            this.broadcast(party, this.msg("party.kill-pvp", "victim", this.nameOf(id),
                    "killer", this.nameOf(killerId), "alive", left));
        } else {
            this.broadcast(party, this.msg("party.kill-generic", "victim", this.nameOf(id),
                    "alive", left));
        }
        this.checkWin(party);
    }

    /** Puts a knocked-out member into spectator mode over the arena. */
    private void watchFrom(Party party, Player player) {
        GameModeGuard.release(player.getUniqueId());
        Location where = party.getArena() != null ? party.getArena().getEventSpawn() : null;
        if (where != null) {
            player.teleport(where);
        }
        GameModeGuard.setFreely(player, GameMode.SPECTATOR);
        GameModeGuard.pin(player, GameMode.SPECTATOR);
    }

    /** The spectate button: watch your own party's match from outside it. */
    public void spectate(Player player) {
        Party party = this.partyOf(player.getUniqueId());
        if (party == null) {
            player.sendMessage(Text.prefixed("&cYou're not in a party."));
            return;
        }
        if (!party.isFighting()) {
            player.sendMessage(Text.prefixed("&cYour party isn't fighting right now."));
            return;
        }
        UUID id = player.getUniqueId();
        if (party.getAlive().contains(id)) {
            player.sendMessage(Text.prefixed("&cYou're in the match - you can't spectate it."));
            return;
        }
        if (!party.getWatching().add(id)) {
            player.sendMessage(Text.prefixed("&7You're already watching. &f/leave&7 to stop."));
            return;
        }
        party.getSnapshots().put(id, PlayerSnapshot.capture(player));
        this.watchFrom(party, player);
        player.sendMessage(Text.prefixed("&7Spectating the party match. &f/leave&7 to stop."));
    }

    /** /leave for someone watching a party match: out of the match, still in
     *  the party. */
    public boolean leaveMatch(Player player) {
        UUID id = player.getUniqueId();
        Party party = this.partyOf(id);
        if (party == null || !party.isFighting() || !party.involved().contains(id)) {
            return false;
        }
        this.pullOut(party, id);
        party.getAlive().remove(id);
        party.getWatching().remove(id);
        player.sendMessage(Text.prefixed("&7You left the party match."));
        this.checkWin(party);
        return true;
    }

    /** Restores one player out of a match, leaving the party itself alone. */
    private void pullOut(Party party, UUID id) {
        Player p = Bukkit.getPlayer((UUID)id);
        PlayerSnapshot snap = party.getSnapshots().remove(id);
        if (p == null) {
            return;
        }
        AntiCheatBypass.release(this.plugin, p);
        GameModeGuard.release(id);
        if (snap != null) {
            snap.restore(p);
        }
        Location dest = this.plugin.getDuelSpawn();
        if (dest != null) {
            p.teleport(dest);
        }
        this.refreshItems(p);
    }

    private void checkWin(Party party) {
        if (!party.isFighting() || party.isFinished()) {
            return;
        }
        if (party.getAlive().size() > 1) {
            return;
        }
        UUID winner = party.getAlive().isEmpty() ? null : party.getAlive().iterator().next();
        this.endMatch(party, winner);
    }

    private void endMatch(Party party, UUID winnerId) {
        if (party.isFinished()) {
            return;
        }
        party.setFinished(true);
        Arena arena = party.getArena();
        for (UUID id : new ArrayList<UUID>(party.involved())) {
            this.pullOut(party, id);
        }
        if (winnerId != null) {
            this.broadcast(party, this.msg("party.winner", "winner", this.nameOf(winnerId)));
            Player champ = Bukkit.getPlayer((UUID)winnerId);
            if (champ != null) {
                champ.sendTitle(this.msg("party.win-title"), this.msg("party.win-subtitle"), 5, 40, 10);
                Sounds.victory(champ);
            }
        } else {
            this.broadcast(party, this.msg("party.no-winner"));
        }
        if (arena != null) {
            this.plugin.getDuelManager().freeArena(arena.getName());
            // Honour the arena's own auto-regenerate flag, the way duels do.
            // Regenerating an arena an admin deliberately left persistent is the
            // kind of difference nobody notices until a build is gone.
            if (arena.isAutoRegenerate()) {
                this.plugin.getArenaManager().regenArena(arena);
                this.plugin.getArenaManager().clearDirty(arena.getName());
            }
        }
        party.resetMatch();
    }

    // ------------------------------------------------------------- upkeep

    public void handleQuit(UUID id) {
        Party party = this.partyOf(id);
        if (party == null) {
            return;
        }
        if (party.isLeader(id)) {
            this.disband(party, "The party leader left the server.");
            return;
        }
        if (party.isFighting() && party.involved().contains(id)) {
            party.getAlive().remove(id);
            party.getWatching().remove(id);
            party.getSnapshots().remove(id);
        }
        party.remove(id);
        this.byPlayer.remove(id);
        this.plugin.getTabService().detach(id);
        this.plugin.getTabService().attachParty(party);
        this.broadcast(party, "&f" + this.nameOf(id) + "&7 left the server.");
        this.checkWin(party);
    }

    /**
     * Ends any party match everybody has walked out of.
     *
     * <p>Same reasoning as the abandoned-duel sweep: the arena is reserved and
     * dirty, and no remaining player is going to trigger the code that frees it.
     */
    public void tick() {
        if (this.byPlayer.isEmpty()) {
            return;
        }
        for (Party party : new HashSet<Party>(this.byPlayer.values())) {
            if (!party.isFighting() || party.isFinished()) {
                continue;
            }
            boolean anyone = false;
            for (UUID id : party.involved()) {
                if (Bukkit.getPlayer((UUID)id) != null) {
                    anyone = true;
                    break;
                }
            }
            if (!anyone) {
                this.endMatch(party, null);
            }
        }
    }

    public void shutdown() {
        for (Party party : new HashSet<Party>(this.byPlayer.values())) {
            if (party.isFighting()) {
                // Frees and regenerates the arena inline, which is the part that
                // must not be skipped; the players are about to be disconnected
                // anyway and DuelManager.shutdown restores what it holds.
                this.endMatch(party, null);
            }
        }
        this.byPlayer.clear();
    }

    // ------------------------------------------------------------ helpers

    /**
     * The one gate everything else asks before letting a player into a queue, a
     * duel, an event or an FFA arena.
     *
     * @return true if they were stopped (and told why)
     */
    public boolean busy(Player player) {
        Party party = this.partyOf(player.getUniqueId());
        if (party == null) {
            return false;
        }
        if (party.isFighting()) {
            player.sendMessage(Text.prefixed("&cYou're in a party match - type &f/leave&c first."));
        } else {
            player.sendMessage(Text.prefixed("&cYou're in a party - leave it first to do that."));
        }
        return true;
    }

    /** The mirror of {@link #busy}, for the party side: you can't form or join
     *  a party while you're already committed to something else. */
    private boolean busyElsewhere(Player player) {
        UUID id = player.getUniqueId();
        if (this.plugin.getDuelManager().isInDuel(id)) {
            player.sendMessage(Text.prefixed("&cYou're in a duel - finish it first."));
            return true;
        }
        if (this.plugin.getEventManager().isInvolved(id)) {
            player.sendMessage(Text.prefixed("&cYou're in an event - type &f/leave&c first."));
            return true;
        }
        if (this.plugin.getSpectateManager().isSpectating(id)) {
            player.sendMessage(Text.prefixed("&cYou're spectating - type &f/leave&c first."));
            return true;
        }
        if (this.plugin.getQueueManager().isQueued(id)) {
            this.plugin.getQueueManager().leaveAll(player);
        }
        return false;
    }

    /**
     * Rebuilds a player's hotbar for whatever they are now.
     *
     * <p>Called on every membership change, both directions. SpawnItems.give
     * clears the inventory before it hands anything out, so joining a party
     * takes the old spawn items away in the same breath as giving the party
     * ones - there is never a moment where someone holds a queue sword they are
     * no longer allowed to use, or a party button after leaving.
     *
     * <p>Which bar they get is decided inside SpawnItems from the party state,
     * so this is correct for a leader, a member and someone with no party at
     * all, and callers do not have to know which.
     */
    private void refreshItems(Player player) {
        if (player != null) {
            SpawnItems.give(this.plugin, player);
        }
    }

    /**
     * A line to everyone in the party.
     *
     * <p>Colours survive. This used to go through Text.prefixed, which strips
     * every colour code AND every non-ASCII character before forcing the whole
     * line grey - so the kill messages arrived as grey text with the skull cut
     * out of them, no matter what they were written as. Colors.toSection is
     * idempotent, so a line that arrives already formatted passes through
     * untouched and a raw one gets translated.
     */
    public void broadcast(Party party, String message) {
        if (party == null) {
            return;
        }
        String line = Colors.toSection(message);
        for (UUID id : party.getMembers()) {
            Player p = Bukkit.getPlayer((UUID)id);
            if (p != null) {
                p.sendMessage(line);
            }
        }
    }

    private String msg(String key, String ... placeholders) {
        return this.plugin.messages().get(key, placeholders);
    }

    private String nameOf(UUID id) {
        Player online = Bukkit.getPlayer((UUID)id);
        if (online != null) {
            return online.getName();
        }
        String name = Bukkit.getOfflinePlayer((UUID)id).getName();
        return name == null ? "Someone" : name;
    }
}

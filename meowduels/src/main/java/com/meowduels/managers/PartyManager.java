package com.meowduels.managers;

import com.meowduels.MeowDuels;
import com.meowduels.model.Arena;
import com.meowduels.model.Kit;
import com.meowduels.model.Party;
import com.meowduels.model.PartyMode;
import com.meowduels.model.PlayerSnapshot;
import com.meowduels.util.AntiCheatBypass;
import com.meowduels.util.Cooldowns;
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
import org.bukkit.World;
import org.bukkit.Material;
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
    /** Matches that have been called but whose players are still in the arena
     *  for the end-of-match hold. Identity set: a Party is only ever itself. */
    private final Set<Party> pendingFinish = new HashSet<Party>();
    private static final String INVITE_COOLDOWN = "party-invite";

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
     * <p>Only used for tab-completing /party join and /party decline. The
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
        Sounds.deny(player);
        player.sendMessage(this.msg("party.invite-declined", "leader", leader.getName()));
        leader.sendMessage(this.msg("party.invite-declined-by", "player", player.getName()));
    }

    /**
     * The party match running in this arena, if any.
     *
     * <p>Looks through the parties rather than the online players. Liquid flow
     * asks this on every water tick in an arena, and there are always far fewer
     * parties than players.
     */
    public Party matchInArena(Arena arena) {
        if (arena == null || this.byPlayer.isEmpty()) {
            return null;
        }
        for (Party party : this.byPlayer.values()) {
            if (party.isFighting() && !party.isFinished() && party.getArena() == arena) {
                return party;
            }
        }
        return null;
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
        // You cannot hand out invites from inside a duel, a queue or an event.
        if (this.busyElsewhere(leader)) {
            return;
        }
        if (this.busyTarget(leader, target)) {
            return;
        }
        if (party.isInvited(targetId)) {
            leader.sendMessage(Text.prefixed("&c" + target.getName() + " already has your invite."));
            return;
        }
        long left = Cooldowns.remaining(leader, INVITE_COOLDOWN);
        if (left > 0L) {
            leader.sendMessage(Text.prefixed("&cWait &f" + Cooldowns.seconds(left)
                    + "s&c before inviting again."));
            Sounds.deny(leader);
            return;
        }
        Cooldowns.start(leader, INVITE_COOLDOWN, null,
                (long)(this.plugin.getConfig().getDouble("party.invite-cooldown-seconds", 5.0) * 1000.0));
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
        Sounds.join(player);
        for (UUID other : party.getMembers()) {
            if (other.equals(id)) continue;
            Player op = Bukkit.getPlayer((UUID)other);
            if (op != null) {
                Sounds.join(op);
            }
        }
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
        Sounds.invite(target);
        target.sendMessage(this.msg("party.invite-header"));
        target.sendMessage(this.msg("party.invite-from", "leader", leader.getName()));
        // Members only. The kit was on here and did not belong: at invite time
        // the leader usually has not picked one, and it can change any number of
        // times before a match starts - so it told you nothing you could act on.
        target.sendMessage(this.msg("party.invite-info", "members", String.valueOf(party.size())));
        target.sendMessage("");
        TextComponent accept = new TextComponent(this.msg("party.invite-accept"));
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party join " + leader.getName()));
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
        Sounds.leave(player);
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
        this.startMatch(leader, PartyMode.FFA);
    }

    public void startMatch(Player leader, PartyMode mode) {
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
        // isEventReady is NOT required any more. It meant "has an FFA spawn set",
        // and most arenas do not - which quietly made most kits unplayable in a
        // party, since the kit menu only offered kits some event-ready arena
        // supported. partySpawn falls back to the middle of the arena instead.
        Arena arena = this.plugin.getArenaManager().findFreeArena(
                a -> this.plugin.getDuelManager().isArenaInUse(a.getName())
                     || !a.supportsKit(kit.getName()));
        if (arena == null) {
            leader.sendMessage(Text.prefixed("&cNo free arena supports that kit."));
            leader.sendMessage(Text.prefixed("&8" + this.plugin.getDuelManager().arenaAvailability(kit.getName())));
            return;
        }
        if (mode == PartyMode.SPLIT) {
            // Everyone online must be on a side. A member who joined after the
            // picker was drawn has no team yet, and a team-less player in a team
            // match is a player who can never be eliminated.
            for (Player p : online) {
                if (party.teamOf(p.getUniqueId()) == null) {
                    party.setTeam(p.getUniqueId(), party.teamMembers(Party.Team.AQUA).size()
                            <= party.teamMembers(Party.Team.RED).size()
                            ? Party.Team.AQUA : Party.Team.RED);
                }
            }
            if (this.onlineOn(party, online, Party.Team.AQUA) == 0
                    || this.onlineOn(party, online, Party.Team.RED) == 0) {
                leader.sendMessage(Text.prefixed("&cBoth teams need at least one online player."));
                return;
            }
        }
        party.setMode(mode);
        party.setArena(arena);
        party.setState(Party.State.FIGHTING);
        party.setFinished(false);
        party.setStartedAt(System.currentTimeMillis());
        party.getAlive().clear();
        party.getWatching().clear();
        party.getSnapshots().clear();
        this.plugin.getDuelManager().markArenaInUse(arena.getName());
        // Anything the last fight left lying in this arena goes before this one
        // starts - dropped kits especially, which is how a second match begins
        // with the floor covered in the first match's gear.
        this.plugin.getArenaManager().clearLooseEntities(arena);
        for (Player p : online) {
            this.sendIn(party, p, kit, arena);
        }
        if (mode == PartyMode.SPLIT) {
            this.announceTeams(party);
        }
        // The bubble's hiding mode depends on whether the party is fighting, so
        // it has to be rebuilt now that it is.
        this.plugin.getTabService().refreshParty(party);
        int seconds = Math.max(1, this.plugin.getConfig().getInt("party.countdown-seconds", 5));
        party.setFightStartsAt(System.currentTimeMillis() + (long)seconds * 1000L);
        this.broadcast(party, this.msg("party.match-started", "count", String.valueOf(online.size()),
                "arena", arena.getName()));
        this.countdownTick(party, seconds, seconds * 20);
    }

    /**
     * Where party fighters land.
     *
     * <p>The arena's FFA spawn if one is set, and otherwise the middle of the
     * arena, dropped onto the first solid block under the ceiling. Requiring the
     * spawn was making most arenas - and through the kit menu, most kits -
     * unusable for a party, for a setting that has a perfectly good default.
     *
     * <p>Searching downwards rather than taking the box's centre Y matters: the
     * geometric centre of an arena is usually inside the floor or halfway up the
     * air above it, and neither is somewhere to stand.
     */
    private Location partySpawn(Arena arena) {
        Location set = arena.getEventSpawn();
        if (set != null) {
            return set;
        }
        World world = arena.getWorld();
        Location min = arena.getMin();
        Location max = arena.getMax();
        if (world == null || min == null || max == null) {
            return null;
        }
        int x = (min.getBlockX() + max.getBlockX()) / 2;
        int z = (min.getBlockZ() + max.getBlockZ()) / 2;
        int bottom = Math.min(min.getBlockY(), max.getBlockY());
        for (int y = Math.max(min.getBlockY(), max.getBlockY()); y >= bottom; --y) {
            if (world.getBlockAt(x, y, z).getType() != Material.AIR) {
                return new Location(world, (double)x + 0.5, (double)(y + 1), (double)z + 0.5);
            }
        }
        return new Location(world, (double)x + 0.5, (double)(bottom + 1), (double)z + 0.5);
    }

    private void sendIn(Party party, Player player, Kit kit, Arena arena) {
        UUID id = player.getUniqueId();
        party.getAlive().add(id);
        party.getSnapshots().put(id, PlayerSnapshot.capture(player));
        // Split sends the sides to the arena's two duel spawns, which every
        // configured arena already has - that is what lets Split run in an
        // ordinary duel arena with no FFA spawn set.
        Party.Team team = party.isSplit() ? party.teamOf(player.getUniqueId()) : null;
        Location spawn = team == null ? this.partySpawn(arena)
                : (team == Party.Team.AQUA ? arena.getSpawn1() : arena.getSpawn2());
        if (spawn == null) {
            spawn = this.partySpawn(arena);
        }
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
    /**
     * The party countdown, stepped every half second rather than every second.
     *
     * <p>The number only changes once a second, but the bar under it moves at
     * every step, which is the difference between a countdown that is running
     * and one that looks frozen between beats. The title is re-sent on the
     * second with a stay longer than the gap, so it never blinks out and back.
     */
    private void countdownTick(Party party, int totalSeconds, int remainingTicks) {
        if (!party.isFighting() || party.isFinished()) {
            return;
        }
        long deadline = party.getFightStartsAt();
        if (remainingTicks <= 0) {
            Kit kit = party.getKit() == null ? null : this.plugin.getKitManager().get(party.getKit());
            for (UUID id : party.getAlive()) {
                Player p = Bukkit.getPlayer((UUID)id);
                if (p != null) {
                    // Auto pots land with FIGHT, so their duration is the
                    // fight's rather than the countdown's.
                    if (kit != null) {
                        kit.applyStartEffects(p);
                    }
                    p.sendTitle(this.msg("party.countdown-go"), "", 0, 20, 10);
                    p.sendActionBar(this.deserialize(""));
                    Sounds.fight(p);
                }
            }
            return;
        }
        int secondsLeft = (remainingTicks + 19) / 20;
        boolean onTheSecond = remainingTicks % 20 == 0;
        String bar = "<#6B7079>" + Cooldowns.bar((long)remainingTicks * 50L,
                (long)Math.max(1, totalSeconds) * 1000L, 20);
        for (UUID id : party.getAlive()) {
            Player p = Bukkit.getPlayer((UUID)id);
            if (p == null) continue;
            p.sendActionBar(this.deserialize(bar));
            if (onTheSecond) {
                p.sendTitle(this.msg("party.countdown-title", "seconds", String.valueOf(secondsLeft)),
                        this.countdownSubtitle(party, id), 0, 25, 0);
                Sounds.tick(p, secondsLeft, totalSeconds);
            }
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (party.getFightStartsAt() == deadline) {
                this.countdownTick(party, totalSeconds, remainingTicks - 10);
            }
        }, 10L);
    }

    private net.kyori.adventure.text.Component deserialize(String mini) {
        try {
            return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize((Object)mini);
        }
        catch (Throwable t) {
            return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize((Object)"");
        }
    }

    /**
     * The line under the countdown number.
     *
     * <p>Mode-specific, because "free-for-all, last one standing" under a Split
     * countdown is not a cosmetic slip - it tells the player the wrong rules for
     * the fight they are about to be in.
     */
    private String countdownSubtitle(Party party, UUID id) {
        if (!party.isSplit()) {
            return this.msg("party.countdown-subtitle");
        }
        Party.Team team = party.teamOf(id);
        if (team == null) {
            return this.msg("party.countdown-subtitle-split-noteam");
        }
        return this.msg("party.countdown-subtitle-split", "team", this.teamName(team));
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
        if (p != null) {
            Sounds.death(p);
        }
        // Killer, victim and the survivors each hear something different, so a
        // kill lands without anyone reading chat.
        for (UUID other : party.getAlive()) {
            Player op = Bukkit.getPlayer((UUID)other);
            if (op == null) continue;
            if (other.equals(killerId)) {
                Sounds.kill(op);
            } else {
                Sounds.eliminated(op);
            }
        }
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
        Location where = party.getArena() == null ? null : this.partySpawn(party.getArena());
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
    /**
     * Ends a running party match early, at the leader's word.
     *
     * <p>No winner is declared: a match somebody stopped did not produce one,
     * and announcing whoever happened to still be standing would be a lie.
     */
    public void forceEnd(Player leader) {
        Party party = this.partyOf(leader.getUniqueId());
        if (party == null) {
            leader.sendMessage(Text.prefixed("&cYou're not in a party."));
            return;
        }
        if (!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(Text.prefixed("&cOnly the party leader can end the match."));
            return;
        }
        if (!party.isFighting() || party.isFinished()) {
            leader.sendMessage(Text.prefixed("&cYour party isn't in a match."));
            return;
        }
        this.broadcast(party, this.msg("party.force-ended", "leader", leader.getName()));
        if (party.getMode() == PartyMode.DUELS) {
            // Nothing to tear down here - the pairings are real duels that own
            // their own arenas. This releases the party, and each duel plays
            // out or is left with /leave.
            this.endDuels(party);
            leader.sendMessage(Text.prefixed("&7Any duels already running will finish on their own."));
            return;
        }
        this.endMatch(party, null, false);
    }

    /**
     * Every other party that could be fought right now.
     *
     * <p>Idle only, and never your own. A party already in a match is not a
     * choice, and showing it as one just produces a click that fails.
     */
    public List<Party> opponentParties(Party mine) {
        ArrayList<Party> out = new ArrayList<Party>();
        HashSet<Party> seen = new HashSet<Party>();
        for (Party party : this.byPlayer.values()) {
            if (party == mine || !seen.add(party)) continue;
            if (party.isFighting() || party.size() < 1) continue;
            if (Bukkit.getPlayer((UUID)party.getLeader()) == null) continue;
            out.add(party);
        }
        return out;
    }

    /** The party led by this player, or null. */
    public Party partyLedBy(UUID leaderId) {
        if (leaderId == null) {
            return null;
        }
        Party party = this.partyOf(leaderId);
        return party != null && party.isLeader(leaderId) ? party : null;
    }

    /**
     * Party Duels: pair the two rosters off into ordinary 1v1s.
     *
     * <p>Deliberately thin. Each pair becomes a real duel, so the arena, the
     * countdown, the rounds, the scoreboard and the teardown are all
     * DuelManager's, already built and already correct. The party layer only
     * marks both sides busy so nobody wanders into a queue mid-match, and lets
     * go again when the last duel is over - which {@link #tick} notices,
     * rather than this reaching into DuelManager for a callback.
     *
     * <p>Uneven rosters are allowed. The extra members sit it out in the lobby
     * rather than the whole thing being refused for one odd player.
     */
    public void startDuels(Player leader) {
        Party party = this.partyOf(leader.getUniqueId());
        if (party == null || !party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(Text.prefixed("&cOnly the party leader can start a match."));
            return;
        }
        if (party.isFighting()) {
            leader.sendMessage(Text.prefixed("&cYour party is already fighting."));
            return;
        }
        Party target = this.partyLedBy(party.getDuelTarget());
        if (target == null || target == party) {
            leader.sendMessage(Text.prefixed("&cThat party is gone - pick another."));
            return;
        }
        if (target.isFighting()) {
            leader.sendMessage(Text.prefixed("&cThat party just started a match of their own."));
            return;
        }
        Kit kit = party.getKit() == null ? null : this.plugin.getKitManager().get(party.getKit());
        if (kit == null) {
            leader.sendMessage(Text.prefixed("&cPick a kit first."));
            return;
        }
        List<Player> mine = this.onlineMembers(party);
        List<Player> theirs = this.onlineMembers(target);
        if (mine.isEmpty() || theirs.isEmpty()) {
            leader.sendMessage(Text.prefixed("&cBoth parties need someone online."));
            return;
        }
        int pairs = Math.min(mine.size(), theirs.size());
        int rounds = party.getDuelRounds();
        int started = 0;
        for (int i = 0; i < pairs; ++i) {
            if (this.plugin.getDuelManager().startPartyDuel(mine.get(i), theirs.get(i), kit.getName(), rounds)) {
                ++started;
                continue;
            }
            // Out of arenas. Stop here rather than leaving gaps in the middle.
            break;
        }
        if (started == 0) {
            leader.sendMessage(Text.prefixed("&cNo free arena supports that kit right now."));
            leader.sendMessage(Text.prefixed("&8" + this.plugin.getDuelManager().arenaAvailability(kit.getName())));
            return;
        }
        this.beginDuels(party, target, started, rounds);
        this.beginDuels(target, party, started, rounds);
        if (started < pairs) {
            leader.sendMessage(Text.prefixed("&eOnly &f" + started + "&e of &f" + pairs
                    + "&e pairs could start - the rest are waiting on a free arena."));
        }
    }

    private void beginDuels(Party party, Party against, int pairs, int rounds) {
        party.setMode(PartyMode.DUELS);
        party.setState(Party.State.FIGHTING);
        party.setFinished(false);
        party.setStartedAt(System.currentTimeMillis());
        party.getAlive().clear();
        party.getWatching().clear();
        // No arena and no snapshots on purpose: every member who is fighting is
        // inside a real duel, and DuelManager owns their inventory and their
        // way home. Touching either here would fight it for them.
        this.plugin.getTabService().refreshParty(party);
        this.broadcast(party, this.msg("party.duels-started",
                "party", this.nameOf(against.getLeader()), "pairs", String.valueOf(pairs),
                "rounds", String.valueOf(rounds)));
    }

    /** Ends a Party Duels match: no arena to give back, no inventories to restore. */
    private void endDuels(Party party) {
        party.resetMatch();
        // attachParty, not refreshParty: every member was moved into their own
        // DUEL bubble when their pairing started, and released from it when it
        // finished, so the party bubble has to be built again from scratch
        // rather than reconciled from a membership map that no longer has them.
        this.plugin.getTabService().attachParty(party);
        for (UUID id : party.getMembers()) {
            Player p = Bukkit.getPlayer((UUID)id);
            if (p != null) {
                this.refreshItems(p);
            }
        }
    }

    private boolean anyoneDueling(Party party) {
        for (UUID id : party.getMembers()) {
            if (this.plugin.getDuelManager().isInDuel(id)) {
                return true;
            }
        }
        return false;
    }

    private List<Player> onlineMembers(Party party) {
        ArrayList<Player> out = new ArrayList<Player>();
        for (UUID id : party.getMembers()) {
            Player p = Bukkit.getPlayer((UUID)id);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

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
        if (party.isSplit()) {
            // A side is out when nobody on it is alive. Last side standing wins,
            // however many of them are left - counting heads instead would end a
            // 3v1 the moment it became interesting.
            int aqua = party.aliveOn(Party.Team.AQUA);
            int red = party.aliveOn(Party.Team.RED);
            if (aqua > 0 && red > 0) {
                return;
            }
            this.endSplit(party, aqua > 0 ? Party.Team.AQUA : (red > 0 ? Party.Team.RED : null));
            return;
        }
        if (party.getAlive().size() > 1) {
            return;
        }
        UUID winner = party.getAlive().isEmpty() ? null : party.getAlive().iterator().next();
        this.endMatch(party, winner);
    }

    /** Ends a Split match, announcing the side rather than a person. */
    private void endSplit(Party party, Party.Team winner) {
        if (winner != null) {
            this.broadcast(party, this.msg("party.team-winner", "team", this.teamName(winner)));
            for (UUID id : party.teamMembers(winner)) {
                Player p = Bukkit.getPlayer((UUID)id);
                if (p != null) {
                    p.sendTitle(this.msg("party.win-title"), this.msg("party.win-subtitle-split"), 5, 40, 10);
                    Sounds.victory(p);
                }
            }
        }
        // endMatch does the teardown; passing no individual winner keeps it from
        // announcing one on top of the team line above.
        this.endMatch(party, null, winner == null);
    }

    private String teamName(Party.Team team) {
        return this.msg(team == Party.Team.AQUA ? "party.team-aqua" : "party.team-red");
    }

    private void announceTeams(Party party) {
        this.broadcast(party, this.msg("party.teams-line",
                "aqua", String.valueOf(party.teamMembers(Party.Team.AQUA).size()),
                "red", String.valueOf(party.teamMembers(Party.Team.RED).size())));
    }

    private int onlineOn(Party party, List<Player> online, Party.Team team) {
        int count = 0;
        for (Player p : online) {
            if (party.teamOf(p.getUniqueId()) == team) {
                ++count;
            }
        }
        return count;
    }

    private void endMatch(Party party, UUID winnerId) {
        this.endMatch(party, winnerId, true);
    }

    /**
     * Calls the match, then leaves everyone standing in the arena for a beat.
     *
     * <p>Duels do this and party matches did not: the winning hit and the
     * teleport home landed in the same tick, so the victory title flashed over
     * a lobby you were already standing in. The hold is the moment the match
     * actually reads as over. PvP is already dead during it - every damage
     * guard keys off isFinished - and the arena stays reserved until the
     * players are out of it.
     */
    private void endMatch(Party party, UUID winnerId, boolean announce) {
        if (party.isFinished()) {
            return;
        }
        party.setFinished(true);
        this.pendingFinish.add(party);
        if (!announce) {
            // A Split match already announced its winning side.
        } else if (winnerId != null) {
            this.broadcast(party, this.msg("party.winner", "winner", this.nameOf(winnerId)));
            Player champ = Bukkit.getPlayer((UUID)winnerId);
            if (champ != null) {
                champ.sendTitle(this.msg("party.win-title"), this.msg("party.win-subtitle"), 5, 40, 10);
                Sounds.victory(champ);
            }
        } else {
            this.broadcast(party, this.msg("party.no-winner"));
        }
        double hold = this.plugin.getConfig().getDouble("party.end-seconds", 3.0);
        long ticks = Math.max(1L, Math.round(hold * 20.0));
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.finishMatch(party), ticks);
    }

    /**
     * The teardown half: everyone out, arena regenerated, party back to idle.
     *
     * <p>Guarded by pendingFinish rather than by party state, because shutdown
     * can reach it before the scheduled run does and resetMatch has already
     * cleared everything it could be tested on.
     */
    private void finishMatch(Party party) {
        if (!this.pendingFinish.remove(party)) {
            return;
        }
        Arena arena = party.getArena();
        for (UUID id : new ArrayList<UUID>(party.involved())) {
            this.pullOut(party, id);
        }
        if (arena != null) {
            this.plugin.getDuelManager().freeArena(arena.getName());
            // Honour the arena's own auto-regenerate flag, the way duels do.
            // Regenerating an arena an admin deliberately left persistent is the
            // kind of difference nobody notices until a build is gone.
            if (arena.isAutoRegenerate()) {
                // Same two-step as a duel: paste the snapshot if there is one,
                // and otherwise replay the blocks this match changed. An arena
                // with no snapshot would keep the damage permanently without
                // the second half.
                if (this.plugin.getArenaManager().regenArena(arena) < 0
                        && !party.getChangedBlocks().isEmpty()) {
                    this.plugin.getArenaManager().restoreBlocks(party.getChangedBlocks());
                }
                this.plugin.getArenaManager().clearDirty(arena.getName());
            }
            // Always, regenerate or not: the drops are entities, and no block
            // regen touches them.
            this.plugin.getArenaManager().clearLooseEntities(arena);
        }
        party.resetMatch();
        // Back to the lobby rules: tab-only, so the lobby does not look empty.
        this.plugin.getTabService().refreshParty(party);
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
            if (party.getMode() == PartyMode.DUELS) {
                // Two seconds of grace: the duels are started before the party
                // is flagged, but a member can still be between one round's
                // teardown and the next round's setup.
                if (System.currentTimeMillis() - party.getStartedAt() > 2000L
                        && !this.anyoneDueling(party)) {
                    this.endDuels(party);
                }
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
                // The hold is scheduled, and a scheduled task will not run
                // during shutdown - so do the teardown inline right now.
                this.finishMatch(party);
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
    /** Whether the person being invited is in the middle of something. */
    private boolean busyTarget(Player leader, Player target) {
        UUID id = target.getUniqueId();
        String what = null;
        if (this.plugin.getDuelManager().isInDuel(id)) {
            what = "in a duel";
        } else if (this.plugin.getQueueManager().isQueued(id)) {
            what = "in a queue";
        } else if (this.plugin.getEventManager().isInvolved(id)) {
            what = "in an event";
        }
        if (what == null) {
            return false;
        }
        leader.sendMessage(Text.prefixed("&c" + target.getName() + " is " + what + " - try again after."));
        return true;
    }

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
            // Used to leave the queue for you. Silently cancelling something
            // somebody deliberately joined is worse than refusing: you find out
            // you are not queued when the match you were waiting for goes to
            // somebody else.
            player.sendMessage(Text.prefixed("&cYou're in a queue - type &f/queue leave&c first."));
            return true;
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

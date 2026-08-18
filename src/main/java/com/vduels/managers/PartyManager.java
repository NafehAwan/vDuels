package com.vduels.managers;

import com.vduels.VDuels;
import com.vduels.model.Arena;
import com.vduels.model.Kit;
import com.vduels.model.Party;
import com.vduels.model.PartyDuelRequest;
import com.vduels.model.PartyMatch;
import com.vduels.model.PartyTeamMatch;
import com.vduels.model.PlayerSnapshot;
import com.vduels.model.Team;
import com.vduels.util.Items;
import com.vduels.util.Sounds;
import com.vduels.util.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

public class PartyManager {
    private static final int COUNTDOWN = 3;
    private final VDuels plugin;
    private final Map<UUID, Party> partyByMember = new HashMap<UUID, Party>();
    private final Map<UUID, UUID> invites = new HashMap<UUID, UUID>();
    private final Map<UUID, PlayerSnapshot> matchSnapshots = new HashMap<UUID, PlayerSnapshot>();
    private final Map<UUID, PartyTeamMatch> teamMatchByPlayer = new HashMap<UUID, PartyTeamMatch>();
    private final Map<UUID, Map<UUID, PartyDuelRequest>> partyDuelRequests = new HashMap<UUID, Map<UUID, PartyDuelRequest>>();

    public PartyManager(VDuels plugin) {
        this.plugin = plugin;
    }

    public Party getParty(UUID id) {
        return this.partyByMember.get(id);
    }

    public boolean isInParty(UUID id) {
        return this.partyByMember.containsKey(id);
    }

    public List<Party> getAllParties() {
        return new ArrayList<Party>(new LinkedHashSet<Party>(this.partyByMember.values()));
    }

    public boolean create(Player leader) {
        if (this.isInParty(leader.getUniqueId())) {
            return false;
        }
        Party party = new Party(leader.getUniqueId());
        this.partyByMember.put(leader.getUniqueId(), party);
        this.giveItems(leader);
        return true;
    }

    public boolean invite(Player inviter, Player target) {
        Party party = this.getParty(inviter.getUniqueId());
        if (party == null || !party.isLeader(inviter.getUniqueId())) {
            return false;
        }
        if (this.isInParty(target.getUniqueId())) {
            return false;
        }
        this.invites.put(target.getUniqueId(), inviter.getUniqueId());
        return true;
    }

    public boolean join(Player player, UUID leaderId) {
        boolean allowed;
        Party party = this.partyByMember.get(leaderId);
        if (party == null) {
            return false;
        }
        boolean bl = allowed = party.isPublic() || leaderId.equals(this.invites.get(player.getUniqueId()));
        if (!allowed) {
            return false;
        }
        this.invites.remove(player.getUniqueId());
        party.getMembers().add(player.getUniqueId());
        this.partyByMember.put(player.getUniqueId(), party);
        this.giveItems(player);
        for (UUID id : party.getMembers()) {
            Player member = Bukkit.getPlayer((UUID)id);
            if (member == null || member.getUniqueId().equals(player.getUniqueId())) continue;
            member.sendMessage(this.msg("party.member-joined", "player", player.getName()));
        }
        return true;
    }

    public UUID clearInvite(UUID target) {
        return this.invites.remove(target);
    }

    public UUID getPendingInviter(UUID target) {
        return this.invites.get(target);
    }

    public void leave(Player player) {
        Party party = this.getParty(player.getUniqueId());
        if (party == null) {
            return;
        }
        if (party.isLeader(player.getUniqueId())) {
            this.disband(party);
            return;
        }
        party.getMembers().remove(player.getUniqueId());
        party.setTeam(player.getUniqueId(), Team.NONE);
        this.partyByMember.remove(player.getUniqueId());
        this.removeItems(player);
        player.sendMessage(this.msg("party.you-left", new String[0]));
        for (UUID id : party.getMembers()) {
            Player member = Bukkit.getPlayer((UUID)id);
            if (member == null) continue;
            member.sendMessage(this.msg("party.member-left", "player", player.getName()));
        }
    }

    public boolean kick(Player leader, String targetName) {
        Party party = this.getParty(leader.getUniqueId());
        if (party == null || !party.isLeader(leader.getUniqueId())) {
            return false;
        }
        UUID targetId = null;
        for (UUID id : party.getMembers()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer((UUID)id);
            if (op.getName() == null || !op.getName().equalsIgnoreCase(targetName)) continue;
            targetId = id;
            break;
        }
        if (targetId == null || party.isLeader(targetId)) {
            return false;
        }
        party.getMembers().remove(targetId);
        party.setTeam(targetId, Team.NONE);
        this.partyByMember.remove(targetId);
        Player target = Bukkit.getPlayer((UUID)targetId);
        if (target != null) {
            this.removeItems(target);
            target.sendMessage(this.msg("party.you-were-kicked", new String[0]));
        }
        for (UUID id : party.getMembers()) {
            Player member = Bukkit.getPlayer((UUID)id);
            if (member == null) continue;
            member.sendMessage(this.msg("party.member-kicked", "player", targetName));
        }
        return true;
    }

    public boolean disband(Player leader) {
        Party party = this.getParty(leader.getUniqueId());
        if (party == null || !party.isLeader(leader.getUniqueId())) {
            return false;
        }
        this.disband(party);
        return true;
    }

    private void disband(Party party) {
        PartyTeamMatch match;
        if (party.getActiveMatch() != null) {
            this.endMatch(party.getActiveMatch(), null);
        }
        if (party.isInTeamMatch() && (match = this.findTeamMatch(party)) != null) {
            this.endTeamMatch(match, null);
        }
        for (UUID id : new ArrayList<UUID>(party.getMembers())) {
            this.partyByMember.remove(id);
            Player player = Bukkit.getPlayer((UUID)id);
            if (player == null) continue;
            this.removeItems(player);
            player.sendMessage(this.msg("party.disbanded", new String[0]));
        }
    }

    public boolean transferLeadership(Player leader, String targetName) {
        Party party = this.getParty(leader.getUniqueId());
        if (party == null || !party.isLeader(leader.getUniqueId())) {
            return false;
        }
        UUID targetId = null;
        for (UUID id : party.getMembers()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer((UUID)id);
            if (id.equals(leader.getUniqueId()) || op.getName() == null || !op.getName().equalsIgnoreCase(targetName)) continue;
            targetId = id;
            break;
        }
        if (targetId == null) {
            return false;
        }
        party.setLeader(targetId);
        for (UUID id : party.getMembers()) {
            Player member = Bukkit.getPlayer((UUID)id);
            if (member == null) continue;
            member.sendMessage(this.msg("party.leadership-transferred", "player", targetName));
        }
        return true;
    }

    public boolean setPublic(Player leader, boolean isPublic) {
        Party party = this.getParty(leader.getUniqueId());
        if (party == null || !party.isLeader(leader.getUniqueId())) {
            return false;
        }
        party.setPublic(isPublic);
        return true;
    }

    public boolean forceEnd(Player leader) {
        PartyTeamMatch match;
        Party party = this.getParty(leader.getUniqueId());
        if (party == null || !party.isLeader(leader.getUniqueId())) {
            return false;
        }
        boolean ended = false;
        if (party.getActiveMatch() != null) {
            this.endMatch(party.getActiveMatch(), null);
            ended = true;
        }
        if (party.isInTeamMatch() && (match = this.findTeamMatch(party)) != null) {
            this.endTeamMatch(match, null);
            ended = true;
        }
        return ended;
    }

    private PartyTeamMatch findTeamMatch(Party party) {
        for (UUID id : party.getMembers()) {
            PartyTeamMatch match = this.teamMatchByPlayer.get(id);
            if (match == null) continue;
            return match;
        }
        return null;
    }

    public void handleQuit(UUID id) {
        this.invites.remove(id);
        Party party = this.getParty(id);
        if (party == null) {
            return;
        }
        if (party.getActiveMatch() != null && party.getActiveMatch().getParticipants().contains(id)) {
            this.handleElimination(id, true);
        }
        if (this.teamMatchByPlayer.containsKey(id)) {
            this.handleTeamElimination(id, true);
        }
        if (party.isLeader(id)) {
            this.disband(party);
        } else {
            party.getMembers().remove(id);
            party.setTeam(id, Team.NONE);
            this.partyByMember.remove(id);
        }
    }

    public void giveItems(Player player) {
        player.getInventory().setItem(0, this.leaveItem());
        player.getInventory().setItem(4, this.settingsItem());
        player.getInventory().setItem(8, this.gamemodesItem());
    }

    public void removeItems(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; ++i) {
            String tag;
            ItemStack it = contents[i];
            if (it == null || !"party-leave".equals(tag = Items.readTag(it, this.plugin.keyButton())) && !"party-settings".equals(tag) && !"party-gamemodes".equals(tag)) continue;
            player.getInventory().setItem(i, null);
        }
    }

    private ItemStack leaveItem() {
        return Items.of(Material.RED_DYE).name("&c&lLEAVE PARTY").lore("&7Right-click to leave your party.").hideTooltip().tag(this.plugin.keyButton(), "party-leave").build();
    }

    private ItemStack settingsItem() {
        return Items.of(Material.TOTEM_OF_UNDYING).name("&d&lPARTY SETTINGS").lore("&7Right-click to manage your party.").hideTooltip().tag(this.plugin.keyButton(), "party-settings").build();
    }

    private ItemStack gamemodesItem() {
        return Items.of(Material.LECTERN).name("&e&lGAMEMODES").lore("&7Right-click to start a party match.").hideTooltip().tag(this.plugin.keyButton(), "party-gamemodes").build();
    }

    public boolean startFfa(Player leader, Party party) {
        if (!party.isLeader(leader.getUniqueId()) || party.isBusy() || party.getSelectedKit() == null) {
            return false;
        }
        Kit kit = this.plugin.getKitManager().get(party.getSelectedKit());
        if (kit == null) {
            return false;
        }
        LinkedHashSet<UUID> online = new LinkedHashSet<UUID>();
        for (UUID id : party.getMembers()) {
            if (Bukkit.getPlayer((UUID)id) == null) continue;
            online.add(id);
        }
        if (online.size() < 2) {
            leader.sendMessage(this.msg("party.ffa.need-players", new String[0]));
            return false;
        }
        Arena arena = this.findFreeArena(party.getSelectedKit());
        if (arena == null) {
            leader.sendMessage(this.msg("party.ffa.no-arena", new String[0]));
            return false;
        }
        this.plugin.getDuelManager().reserveArena(arena.getName());
        PartyMatch match = new PartyMatch(party, arena, party.getSelectedKit(), party.getRounds(), party.isHealthIndicator(), party.isAllowDrops(), online);
        party.setActiveMatch(match);
        for (UUID id : online) {
            Player p = Bukkit.getPlayer((UUID)id);
            this.matchSnapshots.put(id, PlayerSnapshot.capture(p));
            p.closeInventory();
        }
        this.startRound(match);
        return true;
    }

    public PartyMatch getMatch(UUID id) {
        Party party = this.getParty(id);
        return party == null ? null : party.getActiveMatch();
    }

    private Arena findFreeArena(String kitName) {
        return this.plugin.getArenaManager().findFreeArena((Arena a) -> this.plugin.getDuelManager().isArenaBusy(a.getName().toLowerCase(Locale.ROOT)) || !a.supportsKit(kitName));
    }

    private void startRound(PartyMatch match) {
        match.setState(PartyMatch.State.STARTING);
        match.resetAlive();
        Kit kit = this.plugin.getKitManager().get(match.getKit());
        List<Player> online = this.prepareTeam(new ArrayList<UUID>(match.getParticipants()), match.getArena().getSpawn1(), kit);
        if (online.size() < 2) {
            this.endMatch(match, online.isEmpty() ? null : online.get(0).getUniqueId());
            return;
        }
        if (match.isHealthIndicator()) {
            match.setHealthScoreboard(this.buildHealthScoreboard(match.getParticipants()));
        }
        for (Player p : online) {
            p.sendMessage(this.msg("party.ffa.round", "round", String.valueOf(match.getCurrentRound()), "roundsToWin", String.valueOf(match.getRoundsToWin())));
        }
        this.runCountdown(match, 3);
    }

    private List<Player> prepareTeam(List<UUID> ids, Location center, Kit kit) {
        ArrayList<Player> list = new ArrayList<Player>();
        for (int i = 0; i < ids.size(); ++i) {
            Player p = Bukkit.getPlayer((UUID)ids.get(i));
            if (p == null) continue;
            Location spot = this.scatter(center, i, ids.size());
            p.teleport(spot);
            p.setGameMode(GameMode.SURVIVAL);
            p.setHealth(20.0);
            p.setFoodLevel(20);
            p.setSaturation(10.0f);
            p.setFireTicks(0);
            p.setFallDistance(0.0f);
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            if (kit != null) {
                kit.applyTo(p);
            }
            list.add(p);
        }
        return list;
    }

    private Location scatter(Location center, int index, int total) {
        double angle = Math.PI * 2 / (double)Math.max(1, total) * (double)index;
        double radius = 4.0;
        Location loc = center.clone();
        loc.setX(center.getX() + radius * Math.cos(angle));
        loc.setZ(center.getZ() + radius * Math.sin(angle));
        return loc;
    }

    private void runCountdown(PartyMatch match, int secondsLeft) {
        if (match.getState() != PartyMatch.State.STARTING) {
            return;
        }
        List<Player> online = this.onlineParticipants(match);
        if (online.isEmpty()) {
            return;
        }
        if (secondsLeft <= 0) {
            match.setState(PartyMatch.State.FIGHTING);
            for (Player p : online) {
                this.sendTitle(p, this.msg("titles.fight.title", new String[0]), this.msg("titles.fight.subtitle", new String[0]));
                Sounds.fight(p);
            }
            return;
        }
        String secs = String.valueOf(secondsLeft);
        String title = this.msg("titles.countdown.title", "seconds", secs);
        String sub = this.msg("titles.countdown.subtitle", "seconds", secs);
        for (Player p : online) {
            this.sendTitle(p, title, sub);
            Sounds.countdown(p);
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.runCountdown(match, secondsLeft - 1), 20L);
    }

    private List<Player> onlineParticipants(PartyMatch match) {
        ArrayList<Player> list = new ArrayList<Player>();
        for (UUID id : match.getParticipants()) {
            Player p = Bukkit.getPlayer((UUID)id);
            if (p == null) continue;
            list.add(p);
        }
        return list;
    }

    public void handleElimination(UUID id, boolean disconnected) {
        Party party = this.getParty(id);
        if (party == null) {
            return;
        }
        PartyMatch match = party.getActiveMatch();
        if (match == null || match.getState() != PartyMatch.State.FIGHTING || !match.getAlive().contains(id)) {
            return;
        }
        match.eliminate(id);
        Player eliminated = Bukkit.getPlayer((UUID)id);
        if (eliminated != null && !disconnected) {
            eliminated.setGameMode(GameMode.SPECTATOR);
            eliminated.teleport(match.getArena().getSpawn1().clone().add(0.0, 3.0, 0.0));
            eliminated.sendMessage(this.msg("party.ffa.eliminated", new String[0]));
        }
        String name = Bukkit.getOfflinePlayer((UUID)id).getName();
        int remaining = match.getAlive().size();
        for (Player p : this.onlineParticipants(match)) {
            if (p.getUniqueId().equals(id)) continue;
            p.sendMessage(this.msg("party.ffa.player-eliminated", "player", name == null ? "A player" : name, "remaining", String.valueOf(remaining)));
        }
        if (match.isRoundOver()) {
            this.finishRound(match);
        }
    }

    private void finishRound(PartyMatch match) {
        UUID matchWinner;
        match.setState(PartyMatch.State.ENDING);
        UUID winner = match.getRoundWinner();
        if (winner != null) {
            match.awardRoundWin(winner);
            Player w = Bukkit.getPlayer((UUID)winner);
            if (w != null) {
                this.sendTitle(w, this.msg("titles.round-won.title", new String[0]), this.msg("titles.round-won.subtitle", "yourScore", String.valueOf(match.getWins(winner)), "theirScore", "-"));
                Sounds.roundWon(w);
            }
        }
        if ((matchWinner = match.getMatchWinner()) != null) {
            this.endMatch(match, matchWinner);
        } else {
            match.nextRound();
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                if (match.getParty().getActiveMatch() == match) {
                    this.startRound(match);
                }
            }, 60L);
        }
    }

    private void endMatch(PartyMatch match, UUID winnerId) {
        match.setState(PartyMatch.State.ENDING);
        this.plugin.getDuelManager().releaseArena(match.getArena().getName());
        match.getParty().setActiveMatch(null);
        String winnerName = winnerId != null ? Bukkit.getOfflinePlayer((UUID)winnerId).getName() : null;
        for (UUID id : match.getParticipants()) {
            Player p = Bukkit.getPlayer((UUID)id);
            PlayerSnapshot snapshot = this.matchSnapshots.remove(id);
            if (p == null) continue;
            if (snapshot != null) {
                snapshot.restore(p);
            }
            p.setGameMode(GameMode.SURVIVAL);
            this.detachScoreboard(p);
            if (winnerId != null && winnerId.equals(id)) {
                p.sendMessage(this.msg("party.ffa.you-won", new String[0]));
                this.sendTitle(p, this.msg("titles.victory.title", new String[0]), this.msg("titles.victory.subtitle", new String[0]));
                Sounds.victory(p);
            } else if (winnerName != null) {
                p.sendMessage(this.msg("party.ffa.match-over", "winner", winnerName));
                this.sendTitle(p, this.msg("titles.defeat.title", new String[0]), this.msg("titles.defeat.subtitle", new String[0]));
                Sounds.defeat(p);
            } else {
                p.sendMessage(this.msg("party.match-force-ended", new String[0]));
            }
            this.giveItems(p);
        }
    }

    public boolean startSplit(Player leader, Party party) {
        if (!party.isLeader(leader.getUniqueId()) || party.isBusy() || party.getSelectedKit() == null) {
            return false;
        }
        HashMap<UUID, Team> teamOf = new HashMap<UUID, Team>();
        for (UUID id : party.getMembers()) {
            Team t;
            if (Bukkit.getPlayer((UUID)id) == null || (t = party.getTeam(id)) == Team.NONE) continue;
            teamOf.put(id, t);
        }
        boolean hasRed = teamOf.containsValue((Object)Team.RED);
        boolean hasBlue = teamOf.containsValue((Object)Team.BLUE);
        if (!hasRed || !hasBlue) {
            leader.sendMessage(this.msg("party.split.need-both-teams", new String[0]));
            return false;
        }
        Kit kit = this.plugin.getKitManager().get(party.getSelectedKit());
        if (kit == null) {
            return false;
        }
        Arena arena = this.findFreeArena(party.getSelectedKit());
        if (arena == null) {
            leader.sendMessage(this.msg("party.ffa.no-arena", new String[0]));
            return false;
        }
        this.plugin.getDuelManager().reserveArena(arena.getName());
        PartyTeamMatch match = new PartyTeamMatch(arena, party.getSelectedKit(), party.getRounds(), party.isHealthIndicator(), party.isAllowDrops(), teamOf, party, party);
        party.setInTeamMatch(true);
        this.beginTeamMatch(match);
        return true;
    }

    public boolean sendPartyDuelChallenge(Player fromLeader, Party fromParty, UUID toLeaderId, String kit, int rounds, boolean healthIndicator, boolean allowDrops) {
        if (!fromParty.isLeader(fromLeader.getUniqueId()) || fromParty.isBusy()) {
            return false;
        }
        Party toParty = this.getParty(toLeaderId);
        Player toLeader = Bukkit.getPlayer((UUID)toLeaderId);
        if (toParty == null || toLeader == null || toParty == fromParty || toParty.isBusy()) {
            return false;
        }
        if (this.plugin.getKitManager().get(kit) == null) {
            return false;
        }
        PartyDuelRequest request = new PartyDuelRequest(fromLeader.getUniqueId(), toLeaderId, kit, rounds, healthIndicator, allowDrops);
        this.partyDuelRequests.computeIfAbsent(toLeaderId, k -> new HashMap()).put(fromLeader.getUniqueId(), request);
        fromLeader.sendMessage(this.msg("party.duel.sent", "target", toLeader.getName()));
        this.sendPartyDuelRequestCard(toLeader, fromLeader, kit, rounds);
        return true;
    }

    private void sendPartyDuelRequestCard(Player target, Player sender, String kit, int rounds) {
        String kitLabel = kit.replace('_', ' ').toUpperCase(Locale.ROOT);
        target.sendMessage("");
        target.sendMessage(this.msg("party.duel.request-header", "sender", sender.getName()));
        target.sendMessage(this.msg("request.kit", "kit", kitLabel));
        target.sendMessage(this.msg("request.rounds", "rounds", String.valueOf(rounds)));
        target.sendMessage("");
        TextComponent click = new TextComponent(this.msg("request.click", new String[0]));
        click.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party duelaccept " + sender.getName()));
        click.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(this.msg("request.click-hover", "sender", sender.getName())).create()));
        target.spigot().sendMessage(new BaseComponent[]{click});
        target.sendMessage("");
        Sounds.request(target);
    }

    public boolean acceptPartyDuelChallenge(Player toLeaderPlayer, String fromLeaderName) {
        UUID toLeaderId = toLeaderPlayer.getUniqueId();
        Map<UUID, PartyDuelRequest> targeted = this.partyDuelRequests.get(toLeaderId);
        PartyDuelRequest found = null;
        if (targeted != null) {
            for (PartyDuelRequest r : targeted.values()) {
                Player fromPlayer = Bukkit.getPlayer((UUID)r.getFromLeader());
                if (fromPlayer == null || !fromPlayer.getName().equalsIgnoreCase(fromLeaderName)) continue;
                found = r;
                break;
            }
        }
        if (found == null || found.isExpired()) {
            toLeaderPlayer.sendMessage(this.msg("accept.expired", new String[0]));
            return false;
        }
        targeted.remove(found.getFromLeader());
        Player fromLeader = Bukkit.getPlayer((UUID)found.getFromLeader());
        Party fromParty = fromLeader == null ? null : this.getParty(fromLeader.getUniqueId());
        Party toParty = this.getParty(toLeaderId);
        if (fromLeader == null || fromParty == null || toParty == null || fromParty.isBusy() || toParty.isBusy()) {
            toLeaderPlayer.sendMessage(this.msg("accept.sender-offline", new String[0]));
            return false;
        }
        Kit kit = this.plugin.getKitManager().get(found.getKit());
        if (kit == null) {
            toLeaderPlayer.sendMessage(this.msg("accept.no-arena", new String[0]));
            return false;
        }
        Arena arena = this.findFreeArena(found.getKit());
        if (arena == null) {
            toLeaderPlayer.sendMessage(this.msg("accept.no-arena", new String[0]));
            fromLeader.sendMessage(this.msg("accept.no-arena", new String[0]));
            return false;
        }
        HashMap<UUID, Team> teamOf = new HashMap<UUID, Team>();
        for (UUID id : fromParty.getMembers()) {
            if (Bukkit.getPlayer((UUID)id) == null) continue;
            teamOf.put(id, Team.RED);
        }
        for (UUID id : toParty.getMembers()) {
            if (Bukkit.getPlayer((UUID)id) == null) continue;
            teamOf.put(id, Team.BLUE);
        }
        this.plugin.getDuelManager().reserveArena(arena.getName());
        PartyTeamMatch match = new PartyTeamMatch(arena, found.getKit(), found.getRounds(), found.isHealthIndicator(), found.isAllowDrops(), teamOf, fromParty, toParty);
        fromParty.setInTeamMatch(true);
        toParty.setInTeamMatch(true);
        this.beginTeamMatch(match);
        return true;
    }

    public boolean declinePartyDuelChallenge(Player toLeaderPlayer, String fromLeaderName) {
        Map<UUID, PartyDuelRequest> targeted = this.partyDuelRequests.get(toLeaderPlayer.getUniqueId());
        if (targeted == null) {
            return false;
        }
        UUID foundId = null;
        for (UUID fromId : targeted.keySet()) {
            Player fromPlayer = Bukkit.getPlayer((UUID)fromId);
            if (fromPlayer == null || !fromPlayer.getName().equalsIgnoreCase(fromLeaderName)) continue;
            foundId = fromId;
            break;
        }
        if (foundId == null) {
            return false;
        }
        targeted.remove(foundId);
        toLeaderPlayer.sendMessage(this.msg("party.duel.declined", new String[0]));
        Player fromLeader = Bukkit.getPlayer(foundId);
        if (fromLeader != null) {
            fromLeader.sendMessage(this.msg("party.duel.was-declined", "player", toLeaderPlayer.getName()));
        }
        return true;
    }

    public PartyTeamMatch getTeamMatch(UUID id) {
        return this.teamMatchByPlayer.get(id);
    }

    private void beginTeamMatch(PartyTeamMatch match) {
        for (UUID id : match.getParticipants()) {
            Player p = Bukkit.getPlayer((UUID)id);
            if (p == null) continue;
            this.matchSnapshots.put(id, PlayerSnapshot.capture(p));
            this.teamMatchByPlayer.put(id, match);
            p.closeInventory();
        }
        this.startTeamRound(match);
    }

    private void startTeamRound(PartyTeamMatch match) {
        match.setState(PartyTeamMatch.State.STARTING);
        match.resetAlive();
        Kit kit = this.plugin.getKitManager().get(match.getKit());
        ArrayList<UUID> redList = new ArrayList<UUID>();
        ArrayList<UUID> blueList = new ArrayList<UUID>();
        for (Map.Entry<UUID, Team> entry : match.getTeamOf().entrySet()) {
            (entry.getValue() == Team.RED ? redList : blueList).add(entry.getKey());
        }
        ArrayList<Player> online = new ArrayList<Player>();
        online.addAll(this.prepareTeam(redList, match.getArena().getSpawn1(), kit));
        online.addAll(this.prepareTeam(blueList, match.getArena().getSpawn2(), kit));
        if (online.size() < 2) {
            this.endTeamMatch(match, null);
            return;
        }
        if (match.isHealthIndicator()) {
            match.setHealthScoreboard(this.buildHealthScoreboard(match.getParticipants()));
        }
        for (Player p : online) {
            p.sendMessage(this.msg("party.team.round", "round", String.valueOf(match.getCurrentRound()), "roundsToWin", String.valueOf(match.getRoundsToWin())));
        }
        this.runTeamCountdown(match, 3);
    }

    private void runTeamCountdown(PartyTeamMatch match, int secondsLeft) {
        if (match.getState() != PartyTeamMatch.State.STARTING) {
            return;
        }
        List<Player> online = this.onlineTeamParticipants(match);
        if (online.isEmpty()) {
            return;
        }
        if (secondsLeft <= 0) {
            match.setState(PartyTeamMatch.State.FIGHTING);
            for (Player p : online) {
                this.sendTitle(p, this.msg("titles.fight.title", new String[0]), this.msg("titles.fight.subtitle", new String[0]));
                Sounds.fight(p);
            }
            return;
        }
        String secs = String.valueOf(secondsLeft);
        String title = this.msg("titles.countdown.title", "seconds", secs);
        String sub = this.msg("titles.countdown.subtitle", "seconds", secs);
        for (Player p : online) {
            this.sendTitle(p, title, sub);
            Sounds.countdown(p);
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.runTeamCountdown(match, secondsLeft - 1), 20L);
    }

    private List<Player> onlineTeamParticipants(PartyTeamMatch match) {
        ArrayList<Player> list = new ArrayList<Player>();
        for (UUID id : match.getParticipants()) {
            Player p = Bukkit.getPlayer((UUID)id);
            if (p == null) continue;
            list.add(p);
        }
        return list;
    }

    public void handleTeamElimination(UUID id, boolean disconnected) {
        PartyTeamMatch match = this.teamMatchByPlayer.get(id);
        if (match == null || match.getState() != PartyTeamMatch.State.FIGHTING || !match.getAlive().contains(id)) {
            return;
        }
        match.eliminate(id);
        Player eliminated = Bukkit.getPlayer((UUID)id);
        if (eliminated != null && !disconnected) {
            eliminated.setGameMode(GameMode.SPECTATOR);
            eliminated.teleport(match.getArena().getSpawn1().clone().add(0.0, 3.0, 0.0));
            eliminated.sendMessage(this.msg("party.team.eliminated", new String[0]));
        }
        String name = Bukkit.getOfflinePlayer((UUID)id).getName();
        for (Player p : this.onlineTeamParticipants(match)) {
            if (p.getUniqueId().equals(id)) continue;
            p.sendMessage(this.msg("party.team.player-eliminated", "player", name == null ? "A player" : name));
        }
        if (match.isRoundOver()) {
            this.finishTeamRound(match);
        }
    }

    private void finishTeamRound(PartyTeamMatch match) {
        Team matchWinner;
        match.setState(PartyTeamMatch.State.ENDING);
        Team winner = match.getWinningTeam();
        if (winner != null) {
            match.awardRoundWin(winner);
            for (Player p : this.onlineTeamParticipants(match)) {
                boolean won = match.getTeam(p.getUniqueId()) == winner;
                this.sendTitle(p, this.msg(won ? "titles.round-won.title" : "titles.round-lost.title", new String[0]), this.msg(won ? "titles.round-won.subtitle" : "titles.round-lost.subtitle", "yourScore", String.valueOf(match.getWins(match.getTeam(p.getUniqueId()))), "theirScore", String.valueOf(match.getWins(won ? this.otherTeam(winner) : winner))));
                if (!won) continue;
                Sounds.roundWon(p);
            }
        }
        if ((matchWinner = match.getMatchWinner()) != null) {
            this.endTeamMatch(match, matchWinner);
        } else {
            match.nextRound();
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.startTeamRound(match), 60L);
        }
    }

    private Team otherTeam(Team team) {
        return team == Team.RED ? Team.BLUE : Team.RED;
    }

    private void endTeamMatch(PartyTeamMatch match, Team winner) {
        match.setState(PartyTeamMatch.State.ENDING);
        this.plugin.getDuelManager().releaseArena(match.getArena().getName());
        if (match.getRedParty() != null) {
            match.getRedParty().setInTeamMatch(false);
        }
        if (match.getBlueParty() != null) {
            match.getBlueParty().setInTeamMatch(false);
        }
        for (UUID id : match.getParticipants()) {
            this.teamMatchByPlayer.remove(id);
            Player p = Bukkit.getPlayer((UUID)id);
            PlayerSnapshot snapshot = this.matchSnapshots.remove(id);
            if (p == null) continue;
            if (snapshot != null) {
                snapshot.restore(p);
            }
            p.setGameMode(GameMode.SURVIVAL);
            this.detachScoreboard(p);
            if (winner == null) {
                p.sendMessage(this.msg("party.match-force-ended", new String[0]));
            } else if (match.getTeam(id) == winner) {
                p.sendMessage(this.msg("party.team.you-won", new String[0]));
                this.sendTitle(p, this.msg("titles.victory.title", new String[0]), this.msg("titles.victory.subtitle", new String[0]));
                Sounds.victory(p);
            } else {
                p.sendMessage(this.msg("party.team.you-lost", new String[0]));
                this.sendTitle(p, this.msg("titles.defeat.title", new String[0]), this.msg("titles.defeat.subtitle", new String[0]));
                Sounds.defeat(p);
            }
            this.giveItems(p);
        }
    }

    private Scoreboard buildHealthScoreboard(Set<UUID> participants) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("party_health", Criteria.HEALTH, Text.color("&c\u2764"));
        objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        objective.setRenderType(RenderType.INTEGER);
        for (UUID id : participants) {
            Player p = Bukkit.getPlayer((UUID)id);
            if (p == null) continue;
            p.setScoreboard(board);
        }
        return board;
    }

    private void detachScoreboard(Player player) {
        if (Bukkit.getScoreboardManager() != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private String msg(String key, String ... placeholders) {
        return this.plugin.messages().get(key, placeholders);
    }

    private void sendTitle(Player player, String title, String subtitle) {
        player.sendTitle(Text.color(title), Text.color(subtitle), 0, 30, 10);
    }
}


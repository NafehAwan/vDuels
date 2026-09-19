/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.WorldBorder
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.ExperienceOrb
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 */
package com.meowduels.managers;

import com.meowduels.MeowDuels;
import com.meowduels.model.Arena;
import com.meowduels.model.Kit;
import com.meowduels.model.PlayerSnapshot;
import com.meowduels.util.AntiCheatBypass;
import com.meowduels.util.GameModeGuard;
import com.meowduels.util.Trims;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

public class EventManager {
    private final MeowDuels plugin;
    private State state = State.NONE;
    private UUID host;
    private String kit;
    private Arena arena;
    private long startAtMs;
    private int maxSlots = 0;
    private final Set<UUID> players = new HashSet<UUID>();
    private final Set<UUID> alive = new HashSet<UUID>();
    private final Set<UUID> spectators = new HashSet<UUID>();
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<UUID, PlayerSnapshot>();
    private final Map<Location, BlockData> changedBlocks = new HashMap<Location, BlockData>();
    /** Kills this event, per player. Reset when an event starts - a leaderboard
     *  that carried over from the last one would be nonsense. */
    private final Map<UUID, Integer> kills = new HashMap<UUID, Integer>();
    private long runningSinceMs = 0L;
    private int borderGen = 0;
    private WorldBorder eventBorder;
    private double borderCenterX;
    private double borderCenterZ;

    public EventManager(MeowDuels plugin) {
        this.plugin = plugin;
    }

    public State getState() {
        return this.state;
    }

    public boolean isWaiting() {
        return this.state == State.WAITING;
    }

    public boolean isRunning() {
        return this.state == State.RUNNING;
    }

    public boolean isPlaying(UUID id) {
        return this.players.contains(id);
    }

    public Arena getArena() {
        return this.arena;
    }

    /** How many are still in it. */
    public int aliveCount() {
        return this.alive.size();
    }

    /** How many are in the event at all - alive plus the ones watching after
     *  being knocked out. */
    public int playerCount() {
        return this.players.size();
    }

    public int spectatorCount() {
        return this.spectators.size();
    }

    public String getKit() {
        return this.kit == null ? "" : this.kit;
    }

    public int killsOf(UUID id) {
        Integer v = this.kills.get(id);
        return v == null ? 0 : v;
    }

    /** Seconds since the fighting actually started, 0 while still waiting. */
    public long runningSeconds() {
        return this.runningSinceMs == 0L ? 0L : Math.max(0L, (System.currentTimeMillis() - this.runningSinceMs) / 1000L);
    }

    /** Everyone the event concerns - fighters and knocked-out spectators. They
     *  all share one nametag team, so this is what feeds it. */
    public Set<UUID> involved() {
        HashSet<UUID> out = new HashSet<UUID>(this.players);
        out.addAll(this.spectators);
        return out;
    }

    public void recordChange(Location loc, BlockData data) {
        if (loc != null && data != null && !this.changedBlocks.containsKey(loc)) {
            this.changedBlocks.put(loc, data);
        }
    }

    public void host(Player hoster, String kitName, int minutes, int slots, Arena chosen) {
        if (this.state != State.NONE) {
            hoster.sendMessage(this.msg("event.already-running", new String[0]));
            return;
        }
        Kit k = this.plugin.getKitManager().get(kitName);
        if (k == null) {
            hoster.sendMessage(this.msg("event.no-kit", new String[0]));
            return;
        }
        if (chosen == null) {
            hoster.sendMessage(this.msg("event.no-arena", new String[0]));
            return;
        }
        if (!chosen.isEventReady()) {
            hoster.sendMessage(this.msg("event.no-event-spawn", "arena", chosen.getName()));
            return;
        }
        if (this.plugin.getDuelManager().isArenaBusy(chosen)) {
            hoster.sendMessage(this.msg("event.arena-busy", "arena", chosen.getName()));
            return;
        }
        this.arena = chosen;
        this.kit = k.getName();
        this.host = hoster.getUniqueId();
        this.state = State.WAITING;
        this.maxSlots = Math.max(0, slots);
        this.players.clear();
        this.alive.clear();
        this.spectators.clear();
        this.snapshots.clear();
        this.plugin.getDuelManager().markArenaInUse(chosen.getName());
        this.startAtMs = System.currentTimeMillis() + (long)Math.max(1, minutes) * 60000L;
        this.startBorder();
        this.joinInternal(hoster);
        this.announceTick();
    }

    public void forceStart(Player who) {
        if (this.state != State.WAITING) {
            who.sendMessage(this.msg("event.none", new String[0]));
            return;
        }
        this.start();
    }

    public void forceEnd(Player who) {
        if (this.state == State.NONE) {
            who.sendMessage(this.msg("event.none", new String[0]));
            return;
        }
        this.broadcast(this.msg("event.force-ended", new String[0]));
        this.end();
    }

    public void join(Player p) {
        UUID id = p.getUniqueId();
        // A party is exclusive - see PartyManager.busy.
        if (this.plugin.getPartyManager().busy(p)) {
            return;
        }
        if (this.state == State.RUNNING) {
            p.sendMessage(this.msg("event.started-cannot-join", new String[0]));
            return;
        }
        if (this.state != State.WAITING) {
            p.sendMessage(this.msg("event.none", new String[0]));
            return;
        }
        if (this.players.contains(id)) {
            p.sendMessage(this.msg("event.already-joined", new String[0]));
            return;
        }
        if (this.maxSlots > 0 && this.players.size() >= this.maxSlots) {
            p.sendMessage(this.msg("event.full", "slots", String.valueOf(this.maxSlots)));
            return;
        }
        if (this.plugin.getDuelManager().isInDuel(id) || this.plugin.getSpectateManager().isSpectating(id) || this.plugin.getQueueManager().isQueued(id)) {
            p.sendMessage(this.msg("event.busy", new String[0]));
            return;
        }
        this.spectators.remove(id);
        this.joinInternal(p);
        p.sendMessage(this.msg("event.joined", new String[0]));
    }

    private Location eventSpawnLoc() {
        if (this.arena == null) {
            return null;
        }
        if (this.arena.getEventSpawn() != null) {
            return this.arena.getEventSpawn();
        }
        return this.arena.getSpawn1() != null ? this.arena.getSpawn1() : this.arena.getSpawn2();
    }

    public boolean isSpectating(UUID id) {
        return this.spectators.contains(id);
    }

    public boolean isInvolved(UUID id) {
        return this.players.contains(id) || this.spectators.contains(id);
    }

    public void spectate(Player p) {
        UUID id = p.getUniqueId();
        if (this.state == State.NONE) {
            p.sendMessage(this.msg("event.none", new String[0]));
            return;
        }
        if (this.players.contains(id)) {
            p.sendMessage(this.msg("event.spectate-playing", new String[0]));
            return;
        }
        if (this.spectators.contains(id)) {
            p.sendMessage(this.msg("event.already-spectating", new String[0]));
            return;
        }
        if (this.plugin.getDuelManager().isInDuel(id) || this.plugin.getSpectateManager().isSpectating(id) || this.plugin.getQueueManager().isQueued(id)) {
            p.sendMessage(this.msg("event.busy", new String[0]));
            return;
        }
        this.snapshots.put(id, PlayerSnapshot.capture(p));
        this.spectators.add(id);
        this.forceSpectator(p, this.eventSpawnLoc());
        this.applyBorderTo(p);
        p.sendMessage(this.msg("event.spectate-start", new String[0]));
    }

    private void forceSpectator(Player p, Location dest) {
        GameModeGuard.pin(p, GameMode.SPECTATOR);
        if (dest != null) {
            p.teleport(dest);
        }
    }

    private boolean stopSpectating(Player p) {
        UUID id = p.getUniqueId();
        if (!this.spectators.remove(id)) {
            return false;
        }
        PlayerSnapshot snap = this.snapshots.remove(id);
        GameModeGuard.release(id);
        if (snap != null) {
            snap.restore(p);
            Location dest = this.plugin.getDuelSpawn() != null ? this.plugin.getDuelSpawn() : snap.getLocation();
            p.teleport(dest);
        }
        return true;
    }

    private void joinInternal(Player p) {
        UUID id = p.getUniqueId();
        this.players.add(id);
        this.alive.add(id);
        this.snapshots.put(id, PlayerSnapshot.capture(p));
        Location acSpawn = this.eventSpawnLoc();
        String eventWorld = acSpawn != null && acSpawn.getWorld() != null ? acSpawn.getWorld().getName() : null;
        AntiCheatBypass.grant(this.plugin, p, AntiCheatBypass.worldNodes(this.plugin, eventWorld));
        Location spawn = this.eventSpawnLoc();
        if (spawn != null) {
            p.teleport(spawn);
        }
        this.applyBorderTo(p);
        for (PotionEffect e : p.getActivePotionEffects()) {
            p.removePotionEffect(e.getType());
        }
        GameModeGuard.pin(p, GameMode.SURVIVAL);
        try {
            p.setHealth(p.getMaxHealth());
        }
        catch (Throwable t) {
            p.setHealth(20.0);
        }
        p.setFoodLevel(20);
        p.setSaturation(20.0f);
        p.setExhaustion(0.0f);
        Kit k = this.plugin.getKitManager().get(this.kit);
        if (k != null) {
            ItemStack[] armor;
            k.applyTo(p);
            for (ItemStack piece : armor = p.getInventory().getArmorContents()) {
                Trims.clear(piece);
            }
            this.plugin.getTrimPreferences().applyToArmor(id, k.getName(), armor);
            p.getInventory().setArmorContents(armor);
            p.updateInventory();
            Kit fixed = k;
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                Player pl = Bukkit.getPlayer((UUID)id);
                if (pl != null) {
                    fixed.applyOffhand(pl);
                }
            }, 1L);
        }
    }

    private void announceTick() {
        if (this.state != State.WAITING) {
            return;
        }
        long remaining = (this.startAtMs - System.currentTimeMillis() + 999L) / 1000L;
        if (remaining <= 0L) {
            this.start();
            return;
        }
        if (remaining % 10L == 0L || remaining <= 10L) {
            Player h = Bukkit.getPlayer((UUID)this.host);
            String hostName = h != null ? h.getName() : "Someone";
            this.broadcast(this.msg("event.announce", "host", hostName, "kit", this.kitLabel(), "time", EventManager.formatTime(remaining), "slots", this.slotsLabel()));
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, this::announceTick, 20L);
    }

    private String slotsLabel() {
        if (this.maxSlots > 0) {
            return this.players.size() + "/" + this.maxSlots;
        }
        return String.valueOf(this.players.size());
    }

    public void announceKill(UUID victimId, UUID killerId) {
        Player killer;
        if (this.state != State.RUNNING) {
            return;
        }
        Player victim = Bukkit.getPlayer((UUID)victimId);
        String victimName = victim != null ? victim.getName() : "A player";
        int left = this.alive.size();
        if (this.alive.contains(victimId)) {
            --left;
        }
        Player player = killer = killerId == null ? null : Bukkit.getPlayer((UUID)killerId);
        if (killerId != null && !killerId.equals(victimId) && this.players.contains(killerId)) {
            this.kills.merge(killerId, 1, Integer::sum);
        }
        if (killer != null && !killerId.equals(victimId)) {
            this.broadcast(this.msg("event.kill.pvp", "killer", killer.getName(), "victim", victimName, "alive", String.valueOf(left)));
        } else {
            this.broadcast(this.msg("event.kill.generic", "victim", victimName, "alive", String.valueOf(left)));
        }
    }

    private void start() {
        if (this.state != State.WAITING) {
            return;
        }
        this.state = State.RUNNING;
        this.runningSinceMs = System.currentTimeMillis();
        this.kills.clear();
        // Auto pots when the event actually starts - players can be sitting in
        // the lobby for minutes before this, and a 1:30 buff handed out at join
        // would be long gone by the time anyone could use it. Covers a forced
        // start too, since that comes through here.
        Kit started = this.plugin.getKitManager().get(this.kit);
        if (started != null) {
            for (UUID id : this.alive) {
                Player p = Bukkit.getPlayer((UUID)id);
                if (p != null) {
                    started.applyStartEffects(p);
                }
            }
        }
        this.broadcast(this.msg("event.started", new String[0]));
        this.startBorder();
        this.beginBorderShrink();
        this.checkWin();
    }

    private void startBorder() {
        double min30;
        if (this.eventBorder != null) {
            return;
        }
        if (this.arena == null || this.arena.getMin() == null || this.arena.getMax() == null) {
            return;
        }
        if (!this.plugin.getConfig().getBoolean("event.border.enabled", true)) {
            return;
        }
        this.borderCenterX = this.arena.getBorderCenterX();
        this.borderCenterZ = this.arena.getBorderCenterZ();
        double start = this.arena.getBorderStartSize();
        if (start < (min30 = (double)Math.max(1, this.arena.getEventBorderEnd()))) {
            start = min30;
        }
        try {
            this.eventBorder = Bukkit.createWorldBorder();
            this.eventBorder.setCenter(this.borderCenterX, this.borderCenterZ);
            this.eventBorder.setWarningDistance(0);
            this.eventBorder.setWarningTime(0);
            this.eventBorder.setSize(start);
        }
        catch (Throwable t) {
            this.eventBorder = null;
            return;
        }
        this.applyBorderToAll();
    }

    private void beginBorderShrink() {
        if (this.eventBorder == null || this.arena == null) {
            return;
        }
        ++this.borderGen;
        this.scheduleBorderShrink(this.borderGen, this.eventBorder.getSize());
    }

    private void applyBorderTo(Player p) {
        if (this.eventBorder != null && p != null) {
            try {
                p.setWorldBorder(this.eventBorder);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
    }

    private void clearBorderFor(Player p) {
        if (p != null) {
            try {
                p.setWorldBorder(null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
    }

    private void applyBorderToAll() {
        if (this.eventBorder == null) {
            return;
        }
        HashSet<UUID> involved = new HashSet<UUID>(this.players);
        involved.addAll(this.spectators);
        for (UUID id : involved) {
            this.applyBorderTo(Bukkit.getPlayer((UUID)id));
        }
    }

    private void scheduleBorderShrink(int gen, double currentTarget) {
        double min30;
        double d = min30 = this.arena != null ? (double)Math.max(1, this.arena.getEventBorderEnd()) : 30.0;
        if (currentTarget <= min30) {
            return;
        }
        int intervalSeconds = this.arena != null ? this.arena.getEventBorderInterval() : 120;
        long intervalTicks = (long)Math.max(1, intervalSeconds) * 20L;
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (this.state != State.RUNNING || gen != this.borderGen || this.eventBorder == null || this.arena == null) {
                return;
            }
            double step = Math.max(1, this.arena.getEventBorderStep());
            double next = Math.max(min30, currentTarget - step);
            long shrinkSeconds = this.plugin.getConfig().getInt("event.border.shrink-seconds", 8);
            this.eventBorder.setSize(next, Math.max(1L, shrinkSeconds));
            this.applyBorderToAll();
            this.scheduleBorderShrink(gen, next);
        }, intervalTicks);
    }

    public void tickBorderDamage() {
        if (this.state != State.RUNNING || this.eventBorder == null || this.alive.isEmpty()) {
            return;
        }
        double half = this.eventBorder.getSize() / 2.0;
        for (UUID id : new HashSet<UUID>(this.alive)) {
            Location loc;
            Player p = Bukkit.getPlayer((UUID)id);
            if (p == null || !(Math.abs((loc = p.getLocation()).getX() - this.borderCenterX) > half) && !(Math.abs(loc.getZ() - this.borderCenterZ) > half)) continue;
            try {
                p.damage(2.0);
            }
            catch (Throwable throwable) {}
        }
    }

    private void resetBorder() {
        ++this.borderGen;
        HashSet<UUID> involved = new HashSet<UUID>(this.players);
        involved.addAll(this.spectators);
        for (UUID id : involved) {
            this.clearBorderFor(Bukkit.getPlayer((UUID)id));
        }
        this.eventBorder = null;
    }

    public void onDeath(UUID id) {
        if (this.state != State.RUNNING || !this.players.contains(id)) {
            return;
        }
        this.players.remove(id);
        this.alive.remove(id);
        this.spectators.add(id);
        Player p = Bukkit.getPlayer((UUID)id);
        if (p != null) {
            Location where = this.eventSpawnLoc();
            this.forceSpectator(p, where != null ? where : p.getLocation());
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.eliminate(id), 40L);
        this.checkWin();
    }

    private void eliminate(UUID id) {
        if (!this.spectators.remove(id)) {
            return;
        }
        this.alive.remove(id);
        this.players.remove(id);
        Player p = Bukkit.getPlayer((UUID)id);
        PlayerSnapshot snap = this.snapshots.remove(id);
        if (p != null && snap != null) {
            AntiCheatBypass.release(this.plugin, p);
            GameModeGuard.release(p.getUniqueId());
            GameModeGuard.release(p.getUniqueId());
            this.clearBorderFor(p);
            snap.restore(p);
            Location dest = this.plugin.getDuelSpawn() != null ? this.plugin.getDuelSpawn() : snap.getLocation();
            p.teleport(dest);
            this.plugin.giveSpawnItems(p);
        }
    }

    public void leave(Player p) {
        UUID id = p.getUniqueId();
        boolean wasAlive = this.alive.remove(id);
        boolean wasPlaying = this.players.remove(id);
        boolean wasSpectating = this.spectators.remove(id);
        if (!wasPlaying && !wasSpectating) {
            return;
        }
        PlayerSnapshot snap = this.snapshots.remove(id);
        AntiCheatBypass.release(this.plugin, p);
        GameModeGuard.release(p.getUniqueId());
        this.clearBorderFor(p);
        if (snap != null) {
            snap.restore(p);
            Location dest = this.plugin.getDuelSpawn() != null ? this.plugin.getDuelSpawn() : snap.getLocation();
            p.teleport(dest);
        }
        this.plugin.giveSpawnItems(p);
        p.sendMessage(this.msg("event.left", new String[0]));
        if (this.state == State.RUNNING && wasAlive) {
            this.checkWin();
        }
    }

    public void handleQuit(UUID id) {
        boolean wasAlive = this.alive.remove(id);
        boolean wasPlaying = this.players.remove(id);
        boolean wasSpectating = this.spectators.remove(id);
        if (!wasPlaying && !wasSpectating) {
            return;
        }
        this.snapshots.remove(id);
        if (this.state == State.RUNNING && wasAlive) {
            this.checkWin();
        }
    }

    private void checkWin() {
        if (this.state != State.RUNNING) {
            return;
        }
        if (this.alive.size() <= 1) {
            UUID winnerId;
            UUID uUID = winnerId = this.alive.isEmpty() ? null : this.alive.iterator().next();
            if (winnerId != null) {
                Player w = Bukkit.getPlayer((UUID)winnerId);
                String name = w != null ? w.getName() : "A player";
                this.broadcast(this.msg("event.win", "winner", name));
            }
            this.end();
        }
    }

    private void end() {
        HashSet<UUID> involved = new HashSet<UUID>(this.players);
        involved.addAll(this.spectators);
        for (UUID id : involved) {
            Player p = Bukkit.getPlayer((UUID)id);
            PlayerSnapshot snap = this.snapshots.remove(id);
            if (p == null || snap == null) continue;
            AntiCheatBypass.release(this.plugin, p);
            GameModeGuard.release(p.getUniqueId());
            GameModeGuard.release(p.getUniqueId());
            p.getInventory().clear();
            snap.restore(p);
            Location dest = this.plugin.getDuelSpawn() != null ? this.plugin.getDuelSpawn() : snap.getLocation();
            p.teleport(dest);
            this.plugin.giveSpawnItems(p);
        }
        this.resetBorder();
        if (this.arena != null) {
            this.clearDroppedItems(this.arena);
            int written = this.plugin.getArenaManager().regenArena(this.arena);
            if (written < 0 && !this.changedBlocks.isEmpty()) {
                this.plugin.getArenaManager().restoreBlocks(this.changedBlocks);
            }
            this.plugin.getDuelManager().freeArena(this.arena.getName());
        }
        this.changedBlocks.clear();
        this.state = State.NONE;
        this.runningSinceMs = 0L;
        this.host = null;
        this.kit = null;
        this.arena = null;
        this.maxSlots = 0;
        this.players.clear();
        this.alive.clear();
        this.spectators.clear();
        this.snapshots.clear();
    }

    private void clearDroppedItems(Arena a) {
        World world = a.getWorld();
        if (world == null || a.getMin() == null || a.getMax() == null) {
            return;
        }
        Location min = a.getMin();
        Location max = a.getMax();
        Location center = new Location(world, (double)(min.getBlockX() + max.getBlockX()) / 2.0 + 0.5, (double)(min.getBlockY() + max.getBlockY()) / 2.0 + 0.5, (double)(min.getBlockZ() + max.getBlockZ()) / 2.0 + 0.5);
        double dx = (double)(max.getBlockX() - min.getBlockX()) / 2.0 + 2.0;
        double dy = (double)(max.getBlockY() - min.getBlockY()) / 2.0 + 2.0;
        double dz = (double)(max.getBlockZ() - min.getBlockZ()) / 2.0 + 2.0;
        for (Entity e : world.getNearbyEntities(center, dx, dy, dz)) {
            if (!(e instanceof Item) && !(e instanceof ExperienceOrb)) continue;
            e.remove();
        }
    }

    public void shutdown() {
        if (this.state != State.NONE) {
            this.end();
        }
    }

    private String kitLabel() {
        Kit k = this.plugin.getKitManager().get(this.kit);
        if (k != null && k.getDisplayName() != null && !k.getDisplayName().isEmpty()) {
            return k.getDisplayName().replaceAll("<[^>]*>", "");
        }
        return this.kit;
    }

    private static String formatTime(long seconds) {
        if (seconds >= 60L) {
            long m = seconds / 60L;
            long s = seconds % 60L;
            return s == 0L ? m + "m" : m + "m " + s + "s";
        }
        return seconds + "s";
    }

    private void broadcast(String message) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(message);
        }
    }

    private String msg(String key, String ... placeholders) {
        return this.plugin.messages().get(key, placeholders);
    }

    public static enum State {
        NONE,
        WAITING,
        RUNNING;

    }
}


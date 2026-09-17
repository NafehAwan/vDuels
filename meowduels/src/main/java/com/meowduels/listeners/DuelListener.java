/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.player.AsyncChatEvent
 *  net.kyori.adventure.audience.Audience
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.minimessage.MiniMessage
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.bukkit.event.entity.EntityResurrectEvent
 *  org.bukkit.event.entity.FoodLevelChangeEvent
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.entity.ProjectileLaunchEvent
 *  org.bukkit.event.player.AsyncPlayerChatEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerGameModeChangeEvent
 *  org.bukkit.event.player.PlayerItemConsumeEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.event.player.PlayerTeleportEvent
 *  org.bukkit.event.player.PlayerToggleSneakEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.projectiles.ProjectileSource
 */
package com.meowduels.listeners;

import com.meowduels.MeowDuels;
import com.meowduels.model.ActiveDuel;
import com.meowduels.model.Arena;
import com.meowduels.util.GameModeGuard;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.ProjectileSource;

public class DuelListener
implements Listener {
    private final MeowDuels plugin;
    private static final double OUT_OF_BOUNDS_MARGIN = 4.0;
    private static final double BOUNDS_INSET = 1.5;
    private static final long BOUNDS_PULL_COOLDOWN_MS = 1000L;
    private final Map<UUID, Long> lastBoundsPull = new HashMap<UUID, Long>();

    public DuelListener(MeowDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled=true)
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        ActiveDuel duel = this.plugin.getDuelManager().getDuel(player.getUniqueId());
        if (duel == null) {
            return;
        }
        if (duel.getState() != ActiveDuel.State.FIGHTING) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && !duel.getArena().isAllowFallDamage()) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            Location spawn;
            event.setCancelled(true);
            Location location = spawn = player.getUniqueId().equals(duel.getPlayer1()) ? duel.getArena().getSpawn1() : duel.getArena().getSpawn2();
            if (spawn != null) {
                player.setFallDistance(0.0f);
                player.teleport(spawn);
            }
            return;
        }
        double effectiveHealth = player.getHealth();
        try {
            effectiveHealth += Math.max(0.0, player.getAbsorptionAmount());
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        if (event.getFinalDamage() >= effectiveHealth && !this.holdingTotem(player)) {
            event.setCancelled(true);
            try {
                player.setHealth(player.getMaxHealth());
            }
            catch (Throwable t) {
                player.setHealth(20.0);
            }
            UUID loserId = player.getUniqueId();
            Location deathLoc = player.getLocation().clone();
            this.plugin.getDuelManager().handleRoundLoss(loserId, deathLoc);
        }
    }

    private boolean holdingTotem(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return main != null && main.getType() == Material.TOTEM_OF_UNDYING || off != null && off.getType() == Material.TOTEM_OF_UNDYING;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        ActiveDuel duel = this.plugin.getDuelManager().getDuel(victim.getUniqueId());
        if (duel == null) {
            if (!this.plugin.getEventManager().isPlaying(victim.getUniqueId())) {
                this.applyNormalDeathMessage(event, victim);
            }
            return;
        }
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);
        UUID loserId = victim.getUniqueId();
        Location deathLoc = victim.getLocation().clone();
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            victim.spigot().respawn();
            this.plugin.getDuelManager().handleRoundLoss(loserId, deathLoc);
        });
    }

    private void applyNormalDeathMessage(PlayerDeathEvent event, Player victim) {
        String message;
        Player killer = victim.getKiller();
        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            int pick = 1 + ThreadLocalRandom.current().nextInt(4);
            message = this.plugin.messages().get("death.normal.pvp." + pick, "killer", killer.getName(), "victim", victim.getName());
        } else {
            message = this.plugin.messages().get("death.normal.generic", "victim", victim.getName());
        }
        event.setDeathMessage(message);
    }

    /**
     * Where a death puts you back.
     *
     * <p>Inside a duel, on your own side of the arena - that part is unchanged.
     * Anywhere else you come back at spawn holding the spawn items, which is the
     * half that was missing: a lobby death used to leave players at whatever bed
     * or world spawn vanilla picked, with an empty hotbar and no way to queue.
     *
     * <p>Only this plugin can tell those two cases apart, which is why the FFA
     * script does not try to. An FFA death lands here first and gets sent to
     * spawn, and the script's own respawn handler pulls the player back into
     * their arena ten ticks later with the arena kit - the same sequence it
     * already ran, just starting from spawn instead of a bed.
     */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        ActiveDuel duel = this.plugin.getDuelManager().getDuel(player.getUniqueId());
        if (duel != null) {
            Location spawn = player.getUniqueId().equals(duel.getPlayer1()) ? duel.getArena().getSpawn1() : duel.getArena().getSpawn2();
            if (spawn != null) {
                event.setRespawnLocation(spawn);
            }
            return;
        }
        if (!this.plugin.getConfig().getBoolean("on-death-spawn", true)
                || this.plugin.getEventManager().isInvolved(player.getUniqueId())) {
            return;
        }
        Location spawn = this.spawnPoint();
        if (spawn != null) {
            event.setRespawnLocation(spawn);
        }
        // A tick later: the inventory is not ours to touch until the respawn has
        // actually happened, and a duel could still have started in between.
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (player.isOnline() && !this.plugin.getDuelManager().isInDuel(player.getUniqueId())) {
                this.plugin.giveSpawnItems(player);
            }
        }, 1L);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onTeleportMonitor(PlayerTeleportEvent event) {
        if (!event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        if (!this.plugin.getDuelManager().isInDuel(player.getUniqueId()) && !this.plugin.getEventManager().isInvolved(player.getUniqueId())) {
            return;
        }
        this.plugin.getLogger().warning("Another plugin CANCELLED a teleport for " + player.getName() + " while they were in a MeowDuels match (cause: " + String.valueOf(event.getCause()) + "). That will strand them - check your world manager's access/gamemode enforcement for non-OP players.");
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (GameModeGuard.isApplying()) {
            return;
        }
        GameMode required = GameModeGuard.required(event.getPlayer().getUniqueId());
        if (required != null && event.getNewGameMode() != required) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onMove(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent) {
            return;
        }
        Player player = event.getPlayer();
        ActiveDuel duel = this.plugin.getDuelManager().getDuel(player.getUniqueId());
        if (duel == null) {
            return;
        }
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (duel.getState() == ActiveDuel.State.STARTING && !duel.isArenaEntered()) {
            Location from = event.getFrom();
            if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
                Location frozen = new Location(from.getWorld(), from.getX(), to.getY(), from.getZ(), to.getYaw(), to.getPitch());
                event.setTo(frozen);
            }
            return;
        }
        if (duel.getState() == ActiveDuel.State.FIGHTING) {
            Location max;
            Arena arena = duel.getArena();
            Location min = arena == null ? null : arena.getMin();
            Location location = max = arena == null ? null : arena.getMax();
            if (min == null || max == null) {
                return;
            }
            double lowX = min.getBlockX();
            double highX = (double)max.getBlockX() + 1.0;
            double lowZ = min.getBlockZ();
            double highZ = (double)max.getBlockZ() + 1.0;
            if (to.getX() >= lowX - 4.0 && to.getX() <= highX + 4.0 && to.getZ() >= lowZ - 4.0 && to.getZ() <= highZ + 4.0) {
                return;
            }
            UUID id = player.getUniqueId();
            long now = System.currentTimeMillis();
            Long last = this.lastBoundsPull.get(id);
            if (last != null && now - last < 1000L) {
                return;
            }
            this.lastBoundsPull.put(id, now);
            double nx = Math.max(lowX + 1.5, Math.min(highX - 1.5, to.getX()));
            double nz = Math.max(lowZ + 1.5, Math.min(highZ - 1.5, to.getZ()));
            Location back = new Location(to.getWorld(), nx, to.getY(), nz, to.getYaw(), to.getPitch());
            player.setFallDistance(0.0f);
            player.teleport(back);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player p = (Player)event.getEntity();
        if (!this.plugin.getDuelManager().isInDuel(p.getUniqueId())) {
            return;
        }
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            if (p.isOnline()) {
                p.updateInventory();
            }
        });
    }

    @EventHandler(ignoreCancelled=true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player p = event.getPlayer();
        if (!this.plugin.getDuelManager().isInDuel(p.getUniqueId())) {
            return;
        }
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            if (p.isOnline()) {
                p.updateInventory();
            }
        });
    }

    @EventHandler(ignoreCancelled=true)
    public void onLaunch(ProjectileLaunchEvent event) {
        ProjectileSource src = event.getEntity().getShooter();
        if (!(src instanceof Player)) {
            return;
        }
        Player p = (Player)src;
        if (!this.plugin.getDuelManager().isInDuel(p.getUniqueId())) {
            return;
        }
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            if (p.isOnline()) {
                p.updateInventory();
            }
        });
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        ProjectileSource src;
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player)event.getEntity();
        ActiveDuel duel = this.plugin.getDuelManager().getDuel(victim.getUniqueId());
        if (duel == null || duel.getState() != ActiveDuel.State.FIGHTING) {
            return;
        }
        Entity damager = event.getDamager();
        Player attacker = null;
        if (damager instanceof Player) {
            attacker = (Player)damager;
        } else if (damager instanceof Projectile && (src = ((Projectile)damager).getShooter()) instanceof Player) {
            attacker = (Player)src;
        }
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        if (!duel.involves(attacker.getUniqueId())) {
            return;
        }
        duel.recordHit(attacker.getUniqueId(), event.getFinalDamage());
    }

    @EventHandler(ignoreCancelled=true)
    public void onHunger(FoodLevelChangeEvent event) {
        HumanEntity humanEntity = event.getEntity();
        if (humanEntity instanceof Player) {
            Player player = (Player)humanEntity;
            ActiveDuel duel = this.plugin.getDuelManager().getDuel(player.getUniqueId());
            if (duel != null && !duel.getArena().isAllowHungerLoss()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onDrop(PlayerDropItemEvent event) {
        if (this.plugin.getDuelManager().isInDuel(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (this.plugin.getDuelManager().isInDuel(event.getPlayer().getUniqueId())) {
            event.setFormat("\u00a77" + event.getPlayer().getName() + "\u00a78: \u00a77%2$s");
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onModernChat(AsyncChatEvent event) {
        UUID senderId = event.getPlayer().getUniqueId();
        ActiveDuel duel = this.plugin.getDuelManager().getDuel(senderId);
        if (duel == null) {
            return;
        }
        String name = event.getPlayer().getName();
        boolean blue = duel.isAqua(senderId);
        String teamTag = blue ? "<green>" : "<red>";
        NamedTextColor teamColor = blue ? NamedTextColor.GREEN : NamedTextColor.RED;
        event.renderer((source, displayName, message, viewer) -> {
            boolean inFight = this.seesDuelChat(viewer, duel);
            String tag = inFight ? teamTag : "<gray>";
            NamedTextColor msgColor = inFight ? teamColor : NamedTextColor.GRAY;
            return DuelListener.mm(tag + name + "<dark_gray>: ").append(message.colorIfAbsent((TextColor)msgColor));
        });
    }

    private boolean seesDuelChat(Audience viewer, ActiveDuel duel) {
        if (!(viewer instanceof Player)) {
            return false;
        }
        UUID vid = ((Player)viewer).getUniqueId();
        if (this.plugin.getDuelManager().getDuel(vid) == duel) {
            return true;
        }
        UUID watched = this.plugin.getSpectateManager().getWatchedTarget(vid);
        return watched != null && (watched.equals(duel.getPlayer1()) || watched.equals(duel.getPlayer2()));
    }

    private static Component mm(String miniMessage) {
        return MiniMessage.miniMessage().deserialize((Object)miniMessage);
    }

    @EventHandler(ignoreCancelled=true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            this.plugin.getDuelManager().markReady(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.plugin.getTabService().onPlayerJoin(player);
        this.plugin.getScoreboardService().handleJoin(player);
        if (this.plugin.getConfig().getBoolean("on-join-reset", true)) {
            for (PotionEffect e : player.getActivePotionEffects()) {
                player.removePotionEffect(e.getType());
            }
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
        }
        this.sendToSpawn(player);
    }

    /**
     * Everyone lands at spawn holding the spawn items, every join.
     *
     * <p>This used to sit inside the {@code on-join-reset} block, so turning that
     * off - or never running {@code /meowduelssetspawn}, which left the location
     * null - silently dropped both. Neither is optional now: a duels server whose
     * players arrive somewhere random with an empty hotbar has no way in.
     *
     * <p>The items are given twice and the teleport checked twice, one second
     * apart, because other plugins also act on join. A world manager restoring a
     * last-known position, or anything that reinstates a saved inventory, runs
     * after us on the same tick; the second pass runs after all of them. It only
     * re-teleports if the player is in the wrong world, so someone who walked off
     * on their own is left alone.
     */
    private void sendToSpawn(Player player) {
        Location dest = this.spawnPoint();
        if (dest == null) {
            this.plugin.giveSpawnItems(player);
            return;
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!player.isOnline() || this.plugin.getDuelManager().isInDuel(player.getUniqueId())) {
                return;
            }
            player.teleport(dest);
            this.plugin.giveSpawnItems(player);
        }, 2L);
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!player.isOnline() || this.plugin.getDuelManager().isInDuel(player.getUniqueId())
                    || this.plugin.getEventManager().isInvolved(player.getUniqueId())
                    || this.plugin.getSpectateManager().isSpectating(player.getUniqueId())) {
                return;
            }
            if (player.getLocation().getWorld() != dest.getWorld()) {
                player.teleport(dest);
            }
            this.plugin.giveSpawnItems(player);
        }, 22L);
    }

    /**
     * Where spawn is: the configured duel spawn, or the main world's spawn if
     * nobody has set one. Falling back beats doing nothing - an unset spawn is
     * the single most likely reason a join lands the player in the wrong place.
     */
    private Location spawnPoint() {
        Location dest = this.plugin.getDuelSpawn();
        if (dest != null) {
            return dest;
        }
        if (Bukkit.getWorlds().isEmpty()) {
            return null;
        }
        return Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.plugin.getSetupManager().cancel(player);
        this.plugin.getSpectateManager().clearOnQuit(player);
        this.plugin.getQueueManager().remove(player.getUniqueId());
        if (this.plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            this.plugin.getDuelManager().handleDisconnect(player.getUniqueId());
        }
        GameModeGuard.release(player.getUniqueId());
    }
}


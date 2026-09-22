package com.meowduels.listeners;

import com.meowduels.MeowDuels;
import com.meowduels.managers.EventManager;
import com.meowduels.model.ActiveDuel;
import com.meowduels.model.Party;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Gives MeowDuels the last word on whether two fighters may hit each other.
 *
 * <p>A duel, a party match and an FFA round all take place inside somebody
 * else's region. If a region denies PvP - or denies it only above a certain
 * height, which is what a region drawn with a low Y ceiling amounts to - the
 * fight simply stops working partway up the arena, with no message and nothing
 * in the log. That is not a rule anyone set on purpose; it is the region's Y
 * bounds leaking into the match.
 *
 * <p>So this runs at HIGH, after the region plugins have had their say, and
 * un-cancels the hit when MeowDuels' own rules allow it. It does not blindly
 * revive every cancelled hit: it re-derives legality from the match itself, so
 * the countdown, Split's friendly fire and a stopped event all still hold.
 * Height is never part of that decision.
 */
public class PvpOverrideListener
implements Listener {
    private final MeowDuels plugin;

    public PvpOverrideListener(MeowDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onPvp(EntityDamageByEntityEvent event) {
        this.revive(event);
    }

    // There is deliberately no second pass at MONITOR. It would run after
    // DuelListener's own HIGHEST handler, which cancels a lethal hit ON PURPOSE
    // to end the round without killing anyone - reviving that would turn every
    // round-ending hit into a real death. HIGH is the last slot that beats the
    // region plugins without stepping on MeowDuels' own decisions.

    private void revive(EntityDamageByEntityEvent event) {
        if (!event.isCancelled()) {
            return;
        }
        if (!this.plugin.getConfig().getBoolean("pvp.force-in-match", true)) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player)event.getEntity();
        Player attacker = PvpOverrideListener.shooter(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        if (this.allowed(attacker.getUniqueId(), victim.getUniqueId())) {
            event.setCancelled(false);
        }
    }

    /** The player behind the damage, whether they swung or shot. */
    private static Player shooter(Entity damager) {
        if (damager instanceof Player) {
            return (Player)damager;
        }
        if (damager instanceof Projectile) {
            ProjectileSource src = ((Projectile)damager).getShooter();
            if (src instanceof Player) {
                return (Player)src;
            }
        }
        return null;
    }

    private boolean allowed(UUID attacker, UUID victim) {
        ActiveDuel duel = this.plugin.getDuelManager().getDuel(victim);
        if (duel != null) {
            return duel.getState() == ActiveDuel.State.FIGHTING && duel.involves(attacker);
        }
        Party party = this.plugin.getPartyManager().partyOf(victim);
        if (party != null && party.isFighting() && !party.isFinished()) {
            return this.allowedInParty(party, attacker, victim);
        }
        EventManager ev = this.plugin.getEventManager();
        return ev.isRunning() && ev.isPlaying(victim) && ev.isPlaying(attacker);
    }

    private boolean allowedInParty(Party party, UUID attacker, UUID victim) {
        if (party.isCountingDown()) {
            return false;
        }
        if (!party.getAlive().contains(attacker) || !party.getAlive().contains(victim)) {
            return false;
        }
        if (!party.isTeamMode()) {
            return true;
        }
        Party.Team mine = party.teamOf(attacker);
        return mine == null || mine != party.teamOf(victim);
    }
}

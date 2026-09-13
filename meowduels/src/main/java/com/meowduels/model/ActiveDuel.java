/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.block.data.BlockData
 */
package com.meowduels.model;

import com.meowduels.model.Arena;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

public class ActiveDuel {
    private final UUID player1;
    private final UUID player2;
    private final Arena arena;
    private final String kit;
    private final int roundsToWin;
    private int score1;
    private int score2;
    private int currentRound = 1;
    private int gameNumber = 0;
    private boolean arenaEntered = false;
    private boolean ranked = false;
    private boolean finished = false;
    private long fightStartedAt = 0L;
    private final Set<UUID> ready = new HashSet<UUID>();
    private State state = State.STARTING;
    private final long startedAt = System.currentTimeMillis();
    private final Map<Location, BlockData> changedBlocks = new HashMap<Location, BlockData>();
    private final Map<UUID, Double> damageDealt = new HashMap<UUID, Double>();
    private final Map<UUID, Integer> hits = new HashMap<UUID, Integer>();

    public void recordHit(UUID attacker, double amount) {
        if (attacker == null) {
            return;
        }
        Double d = this.damageDealt.get(attacker);
        this.damageDealt.put(attacker, (d == null ? 0.0 : d) + Math.max(0.0, amount));
        Integer h = this.hits.get(attacker);
        this.hits.put(attacker, (h == null ? 0 : h) + 1);
    }

    public double getDamageDealt(UUID attacker) {
        Double d = this.damageDealt.get(attacker);
        return d == null ? 0.0 : d;
    }

    public int getHits(UUID attacker) {
        Integer h = this.hits.get(attacker);
        return h == null ? 0 : h;
    }

    public ActiveDuel(UUID player1, UUID player2, Arena arena, String kit, int roundsToWin) {
        this.player1 = player1;
        this.player2 = player2;
        this.arena = arena;
        this.kit = kit;
        this.roundsToWin = roundsToWin;
    }

    public UUID getPlayer1() {
        return this.player1;
    }

    public UUID getPlayer2() {
        return this.player2;
    }

    public UUID getOpponent(UUID player) {
        return player.equals(this.player1) ? this.player2 : this.player1;
    }

    public boolean involves(UUID player) {
        return this.player1.equals(player) || this.player2.equals(player);
    }

    public Arena getArena() {
        return this.arena;
    }

    public String getKit() {
        return this.kit;
    }

    public int getRoundsToWin() {
        return this.roundsToWin;
    }

    public boolean isRanked() {
        return this.ranked;
    }

    public void setRanked(boolean ranked) {
        this.ranked = ranked;
    }

    public int getScore1() {
        return this.score1;
    }

    public int getScore2() {
        return this.score2;
    }

    public int getCurrentRound() {
        return this.currentRound;
    }

    public void nextRound() {
        ++this.currentRound;
    }

    public long getStartedAt() {
        return this.startedAt;
    }

    public int getGameNumber() {
        return this.gameNumber;
    }

    public void setGameNumber(int gameNumber) {
        this.gameNumber = gameNumber;
    }

    public boolean isArenaEntered() {
        return this.arenaEntered;
    }

    public void setArenaEntered(boolean arenaEntered) {
        this.arenaEntered = arenaEntered;
    }

    public boolean addReady(UUID player) {
        return this.ready.add(player);
    }

    public int getReadyCount() {
        return this.ready.size();
    }

    public void clearReady() {
        this.ready.clear();
    }

    public void markFightStart() {
        this.fightStartedAt = System.currentTimeMillis();
    }

    public long sinceFightStart() {
        return this.fightStartedAt == 0L ? Long.MAX_VALUE : System.currentTimeMillis() - this.fightStartedAt;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean isAqua(UUID player) {
        return player.equals(this.player1);
    }

    public int getScoreFor(UUID player) {
        return player.equals(this.player1) ? this.score1 : this.score2;
    }

    public int getScoreAgainst(UUID player) {
        return player.equals(this.player1) ? this.score2 : this.score1;
    }

    public State getState() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean awardRound(UUID winner) {
        if (winner.equals(this.player1)) {
            ++this.score1;
        } else {
            ++this.score2;
        }
        return this.score1 >= this.roundsToWin || this.score2 >= this.roundsToWin;
    }

    public UUID getMatchWinner() {
        if (this.score1 >= this.roundsToWin) {
            return this.player1;
        }
        if (this.score2 >= this.roundsToWin) {
            return this.player2;
        }
        return null;
    }

    public void recordChange(Location loc, BlockData original) {
        Location key = new Location(loc.getWorld(), (double)loc.getBlockX(), (double)loc.getBlockY(), (double)loc.getBlockZ());
        this.changedBlocks.putIfAbsent(key, original);
    }

    public Map<Location, BlockData> getChangedBlocks() {
        return this.changedBlocks;
    }

    public static enum State {
        STARTING,
        FIGHTING,
        ENDING;

    }
}


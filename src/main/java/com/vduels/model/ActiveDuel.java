package com.vduels.model;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * State for a duel that is currently being played. Tracks the round score and,
 * importantly, a log of every block that changed during the fight so the arena
 * can be restored to its pristine state when the match ends.
 */
public class ActiveDuel {

    public enum State {
        STARTING,
        FIGHTING,
        ENDING
    }

    private final UUID player1;
    private final UUID player2;
    private final Arena arena;
    private final String kit;
    private final int roundsToWin;

    private int score1;
    private int score2;
    private int currentRound = 1;
    private State state = State.STARTING;
    private final long startedAt = System.currentTimeMillis();

    // Blocks changed during the fight -> their ORIGINAL data, for regeneration.
    private final Map<Location, BlockData> changedBlocks = new HashMap<>();

    public ActiveDuel(UUID player1, UUID player2, Arena arena, String kit, int roundsToWin) {
        this.player1 = player1;
        this.player2 = player2;
        this.arena = arena;
        this.kit = kit;
        this.roundsToWin = roundsToWin;
    }

    public UUID getPlayer1() {
        return player1;
    }

    public UUID getPlayer2() {
        return player2;
    }

    public UUID getOpponent(UUID player) {
        return player.equals(player1) ? player2 : player1;
    }

    public boolean involves(UUID player) {
        return player1.equals(player) || player2.equals(player);
    }

    public Arena getArena() {
        return arena;
    }

    public String getKit() {
        return kit;
    }

    public int getRoundsToWin() {
        return roundsToWin;
    }

    public int getScore1() {
        return score1;
    }

    public int getScore2() {
        return score2;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void nextRound() {
        currentRound++;
    }

    public long getStartedAt() {
        return startedAt;
    }

    /** BLUE for player 1, RED for player 2 (used by the scoreboard). */
    public boolean isBlue(UUID player) {
        return player.equals(player1);
    }

    /** This player's own score first, then the opponent's. */
    public int getScoreFor(UUID player) {
        return player.equals(player1) ? score1 : score2;
    }

    public int getScoreAgainst(UUID player) {
        return player.equals(player1) ? score2 : score1;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    /** Records a win for the given player; returns true if they won the match. */
    public boolean awardRound(UUID winner) {
        if (winner.equals(player1)) {
            score1++;
        } else {
            score2++;
        }
        return score1 >= roundsToWin || score2 >= roundsToWin;
    }

    public UUID getMatchWinner() {
        if (score1 >= roundsToWin) {
            return player1;
        }
        if (score2 >= roundsToWin) {
            return player2;
        }
        return null;
    }

    /**
     * Records the original block data at a location, but only the FIRST time it
     * changes, so we always restore the pre-duel state.
     */
    public void recordChange(Location loc, BlockData original) {
        Location key = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        changedBlocks.putIfAbsent(key, original);
    }

    public Map<Location, BlockData> getChangedBlocks() {
        return changedBlocks;
    }
}

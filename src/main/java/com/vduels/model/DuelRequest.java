package com.vduels.model;

import java.util.UUID;

/**
 * A pending challenge from one player to another. Requests expire after a
 * configurable window (see DuelManager).
 */
public class DuelRequest {

    private final UUID sender;
    private final UUID target;
    private final String kit;
    private final int rounds;
    private final String arena; // chosen arena name, or null for "random / any free"
    private final long createdAt;

    public DuelRequest(UUID sender, UUID target, String kit, int rounds, String arena) {
        this.sender = sender;
        this.target = target;
        this.kit = kit;
        this.rounds = rounds;
        this.arena = arena;
        this.createdAt = System.currentTimeMillis();
    }

    public String getArena() {
        return arena;
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getTarget() {
        return target;
    }

    public String getKit() {
        return kit;
    }

    public int getRounds() {
        return rounds;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired(long ttlMillis) {
        return System.currentTimeMillis() - createdAt > ttlMillis;
    }
}

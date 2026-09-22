package com.meowduels.model;

import java.util.UUID;

/**
 * One party challenging another to Party Duels.
 *
 * <p>The kit and the rounds are copied in rather than read back off the
 * challenging party when it is accepted. Between sending and accepting the
 * challenger can open the kit picker again, and a match that quietly becomes a
 * different match after you agreed to it is the kind of thing people stop
 * trusting a menu over.
 */
public class PartyDuelRequest {
    private final UUID from;
    private final UUID to;
    private final String kit;
    private final int rounds;
    private final long createdAt = System.currentTimeMillis();

    public PartyDuelRequest(UUID fromLeader, UUID toLeader, String kit, int rounds) {
        this.from = fromLeader;
        this.to = toLeader;
        this.kit = kit;
        this.rounds = rounds;
    }

    public UUID getFrom() {
        return this.from;
    }

    public UUID getTo() {
        return this.to;
    }

    public String getKit() {
        return this.kit;
    }

    public int getRounds() {
        return this.rounds;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

    public boolean isExpired(long ttlMs) {
        return System.currentTimeMillis() - this.createdAt > ttlMs;
    }
}

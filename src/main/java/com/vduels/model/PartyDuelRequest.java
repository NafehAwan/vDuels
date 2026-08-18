package com.vduels.model;

import java.util.UUID;

public class PartyDuelRequest {
    private final UUID fromLeader;
    private final UUID toLeader;
    private final String kit;
    private final int rounds;
    private final boolean healthIndicator;
    private final boolean allowDrops;
    private final long createdAt = System.currentTimeMillis();

    public PartyDuelRequest(UUID fromLeader, UUID toLeader, String kit, int rounds, boolean healthIndicator, boolean allowDrops) {
        this.fromLeader = fromLeader;
        this.toLeader = toLeader;
        this.kit = kit;
        this.rounds = rounds;
        this.healthIndicator = healthIndicator;
        this.allowDrops = allowDrops;
    }

    public UUID getFromLeader() {
        return this.fromLeader;
    }

    public UUID getToLeader() {
        return this.toLeader;
    }

    public String getKit() {
        return this.kit;
    }

    public int getRounds() {
        return this.rounds;
    }

    public boolean isHealthIndicator() {
        return this.healthIndicator;
    }

    public boolean isAllowDrops() {
        return this.allowDrops;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - this.createdAt > 60000L;
    }
}


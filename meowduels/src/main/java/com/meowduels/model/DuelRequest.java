/*
 * Decompiled with CFR 0.152.
 */
package com.meowduels.model;

import java.util.UUID;

public class DuelRequest {
    private final UUID sender;
    private final UUID target;
    private final String kit;
    private final int rounds;
    private final String arena;
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
        return this.arena;
    }

    public UUID getSender() {
        return this.sender;
    }

    public UUID getTarget() {
        return this.target;
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

    public boolean isExpired(long ttlMillis) {
        return System.currentTimeMillis() - this.createdAt > ttlMillis;
    }
}


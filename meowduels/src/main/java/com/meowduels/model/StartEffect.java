/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
package com.meowduels.model;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public enum StartEffect {
    STRENGTH2_90(PotionEffectType.STRENGTH, 1, 1800, "Strength II (1:30)"),
    SPEED2_90(PotionEffectType.SPEED, 1, 1800, "Speed II (1:30)"),
    SPEED2_INFINITE(PotionEffectType.SPEED, 1, -1, "Speed II (infinite)"),
    REGEN2_90(PotionEffectType.REGENERATION, 1, 1800, "Regeneration II (1:30)");

    private final PotionEffectType type;
    private final int amplifier;
    private final int durationTicks;
    private final String label;

    private StartEffect(PotionEffectType type, int amplifier, int durationTicks, String label) {
        this.type = type;
        this.amplifier = amplifier;
        this.durationTicks = durationTicks;
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }

    public PotionEffect toPotionEffect() {
        return new PotionEffect(this.type, this.durationTicks, this.amplifier);
    }
}


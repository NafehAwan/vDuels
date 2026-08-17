package com.vduels.model;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Optional potion effects a kit can grant at the start of every round. Admins
 * toggle any combination through {@code /editkit}; with none set a kit starts
 * with no effects (the default).
 */
public enum StartEffect {

    STRENGTH2_90(PotionEffectType.STRENGTH, 1, 1800, "Strength II (1:30)"),
    SPEED2_90(PotionEffectType.SPEED, 1, 1800, "Speed II (1:30)"),
    SPEED2_INFINITE(PotionEffectType.SPEED, 1, -1, "Speed II (infinite)");

    private final PotionEffectType type;
    private final int amplifier;      // 0-indexed: level II == 1
    private final int durationTicks;  // -1 == infinite
    private final String label;

    StartEffect(PotionEffectType type, int amplifier, int durationTicks, String label) {
        this.type = type;
        this.amplifier = amplifier;
        this.durationTicks = durationTicks;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public PotionEffect toPotionEffect() {
        return new PotionEffect(type, durationTicks, amplifier);
    }
}

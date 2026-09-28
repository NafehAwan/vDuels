package com.meowduels.util;

import java.util.Locale;

/**
 * How a health number is written, in the one place that decides it.
 *
 * <p>It used to live twice - once in the sidebar, once in the PlaceholderAPI
 * hook - with the same magic number in both, which is how a threshold gets
 * changed in one of them and not the other.
 */
public final class Health {
    /**
     * Below this, the number is written with decimals.
     *
     * <p>Full health is a round twenty and reads better as one. Anything less
     * is the number you are actually watching in a fight: whether they are on
     * 19.35 or 18.8 decides whether the next hit kills, and a rounded "19" for
     * both is exactly the information you needed.
     */
    private static final double EXACT_BELOW = 20.0;

    private Health() {
    }

    /**
     * How the number reads above a head: always one decimal, 20.0 included.
     *
     * <p>Different from {@link #text} on purpose. The sidebar prints a round
     * 20 because a full bar is not information; a nametag is read mid-combo,
     * where a fixed width matters more - a number that grows and shrinks a
     * character as you hit somebody is harder to read than one that does not.
     * Two decimals would be that same fixed width and still too much to take
     * in at a glance.
     */
    public static String nametag(double hp) {
        if (hp < 0.0) {
            hp = 0.0;
        }
        return String.format(Locale.US, "%.1f", hp);
    }

    public static String text(double hp) {
        if (hp < 0.0) {
            hp = 0.0;
        }
        if (hp < EXACT_BELOW) {
            return String.format(Locale.US, "%.2f", hp);
        }
        return String.valueOf((int)Math.round(hp));
    }
}

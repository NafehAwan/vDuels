package com.meowduels.util;

import java.util.Map;

/**
 * {@code {name}} substitution, shared by the scoreboard and the menus.
 *
 * <p>Lifted out of {@code ScoreboardService}, which had it private and which
 * is still its heaviest user: a sidebar re-renders every tick, so the
 * early return on a line with no brace in it is load-bearing rather than
 * tidiness.
 *
 * <p>Deliberately not a regex. A menu label can legitimately contain a brace
 * that is not a placeholder, and a pass that only knows the tokens it was
 * handed will leave such a brace alone instead of swallowing it.
 */
public final class Tokens {
    private Tokens() {
    }

    /**
     * Replaces every {@code {key}} in {@code s} with its value.
     *
     * <p>Returns {@code s} itself when there is nothing to do, so a caller on
     * a hot path pays one {@code indexOf} rather than a map walk.
     */
    public static String apply(String s, Map<String, String> tokens) {
        if (s == null || tokens == null || tokens.isEmpty() || s.indexOf(123) < 0) {
            return s;
        }
        for (Map.Entry<String, String> e : tokens.entrySet()) {
            String value = e.getValue();
            s = s.replace("{" + e.getKey() + "}", value == null ? "" : value);
        }
        return s;
    }
}

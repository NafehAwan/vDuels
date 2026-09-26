package com.meowduels.util;

/**
 * The marker in front of a fighter's name, in the one place that decides it.
 *
 * <p>It used to be a bolt, written out as a section-code literal in eight
 * different files, which is how six of them ended up aqua and the other two
 * did not.
 *
 * <p>A flag, and two colours: blue for one side, red for the other. Plain
 * blue and plain red, not aqua and not the salmon the menus use - at tab-list
 * size the pale pair reads as one colour.
 */
public final class Marks {
    /** U+2691, BLACK FLAG. Solid, so it survives being one character tall. */
    public static final String FLAG = "⚑";
    /** U+2620, SKULL AND CROSSBONES - somebody knocked out of a match. */
    public static final String SKULL = "☠";

    public static final char BLUE = '9';
    public static final char RED = 'c';
    public static final char EVENT = 'e';
    public static final char ALIVE = 'a';
    public static final char OUT = '8';

    private Marks() {
    }

    /**
     * Tab-list prefix: a space, the flag, a space, then the name.
     *
     * <p>The leading space is the point. A row that starts hard against the
     * left edge and a row that starts with a marker do not line up, and a tab
     * list is read as a column.
     */
    public static String tab(char color) {
        return "§" + color + " " + FLAG + " §" + color;
    }

    /**
     * Above-the-head prefix: the same marker without the leading space.
     *
     * <p>A nametag is centred on the player, so padding it on one side only
     * shifts the whole thing off their head.
     */
    public static String tag(char color) {
        return "§" + color + FLAG + " §" + color;
    }

    /** Knocked out, in either place: grey, and a skull rather than a flag. */
    public static String tabOut() {
        return "§" + OUT + " " + SKULL + " §" + OUT;
    }

    public static String tagOut() {
        return "§" + OUT + SKULL + " §" + OUT;
    }
}

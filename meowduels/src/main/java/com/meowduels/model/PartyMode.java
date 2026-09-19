package com.meowduels.model;

/**
 * The shapes a party match can take.
 *
 * <p>Two of them are not built yet and say so in the menu rather than being
 * absent from it - a mode that is coming is worth knowing about, and an empty
 * picker with one option in it looks like something is broken.
 */
public enum PartyMode {
    FFA("\u1d18\u1d00\u0280\u1d1b\u028f \ua730\ua730\u1d00", "\u1d07\u1d20\u1d07\u0280\u028f\u1d0f\u0274\u1d07 \ua730\u1d0f\u0280 \u1d1b\u029c\u1d07\u1d0d\ua731\u1d07\u029f\u1d20\u1d07\ua731, \u029f\u1d00\ua731\u1d1b \u1d0f\u0274\u1d07 \ua731\u1d1b\u1d00\u0274\u1d05\u026a\u0274\u0262", true),
    SPLIT("\u1d18\u1d00\u0280\u1d1b\u028f \ua731\u1d18\u029f\u026a\u1d1b", "\u1d1b\u1d21\u1d0f \u1d1b\u1d07\u1d00\u1d0d\ua731, \u1d00\ua7af\u1d1c\u1d00 \u1d00\u0262\u1d00\u026a\u0274\ua731\u1d1b \u0280\u1d07\u1d05", true),
    DUELS("\u1d18\u1d00\u0280\u1d1b\u028f \u1d05\u1d1c\u1d07\u029f\ua731", "\u1d18\u1d00\u026a\u0280\u1d07\u1d05 \u1d0f\ua730\ua730, \u1d0f\u0274\u1d07 \u1d0f\u0274 \u1d0f\u0274\u1d07", false);

    private final String label;
    private final String description;
    private final boolean ready;

    private PartyMode(String label, String description, boolean ready) {
        this.label = label;
        this.description = description;
        this.ready = ready;
    }

    public String getLabel() {
        return this.label;
    }

    public String getDescription() {
        return this.description;
    }

    /** False while the mode is still "soon" - the picker greys it and refuses
     *  the click, and nothing downstream has to know it exists. */
    public boolean isReady() {
        return this.ready;
    }
}

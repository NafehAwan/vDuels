/*
 * Decompiled with CFR 0.152.
 */
package com.meowtags;

public class Tag {
    public final String id;
    public final String display;
    public final String hex1;
    public final String hex2;
    public final boolean custom;

    public Tag(String id, String display, String hex1, String hex2, boolean custom) {
        this.id = id;
        this.display = display;
        this.hex1 = hex1;
        this.hex2 = hex2;
        this.custom = custom;
    }

    public String permission() {
        return "meowtags." + this.id;
    }
}


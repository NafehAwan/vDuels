/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package com.meowduels.hook;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class TabHook {
    private final boolean available;
    private Object api;
    private Method getPlayer;
    private Object tablistManager;
    private Method tablistSetPrefix;
    private Object nametagManager;
    private Method nametagSetPrefix;
    private Method nametagSetSuffix;

    public TabHook() {
        boolean ok = false;
        try {
            Class<?> apiClass = Class.forName("me.neznamy.tab.api.TabAPI");
            this.api = apiClass.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
            Class<?> tabPlayerClass = Class.forName("me.neznamy.tab.api.TabPlayer");
            this.getPlayer = apiClass.getMethod("getPlayer", UUID.class);
            if (this.api != null) {
                this.tablistManager = this.tryGet(apiClass, "getTabListFormatManager");
                this.tablistSetPrefix = this.resolveMethod(this.tablistManager, "setPrefix", tabPlayerClass);
                this.nametagManager = this.tryGet(apiClass, "getNameTagManager");
                this.nametagSetPrefix = this.resolveMethod(this.nametagManager, "setPrefix", tabPlayerClass);
                this.nametagSetSuffix = this.resolveMethod(this.nametagManager, "setSuffix", tabPlayerClass);
                ok = this.getPlayer != null && (this.tablistSetPrefix != null || this.nametagSetPrefix != null);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        this.available = ok;
    }

    private Object tryGet(Class<?> apiClass, String method) {
        try {
            return apiClass.getMethod(method, new Class[0]).invoke(this.api, new Object[0]);
        }
        catch (Throwable t) {
            return null;
        }
    }

    private Method resolveMethod(Object manager, String name, Class<?> tabPlayerClass) {
        if (manager == null) {
            return null;
        }
        try {
            Method m = manager.getClass().getMethod(name, tabPlayerClass, String.class);
            m.setAccessible(true);
            return m;
        }
        catch (Throwable t) {
            return null;
        }
    }

    public boolean isAvailable() {
        return this.available;
    }

    private Object tabPlayer(Player player) {
        try {
            return this.getPlayer.invoke(this.api, player.getUniqueId());
        }
        catch (Throwable t) {
            return null;
        }
    }

    public void hideRank(Player player) {
        this.setPrefix(player, "");
    }

    public void setRankPrefix(Player player, String value) {
        if (!this.available || this.nametagSetPrefix == null || player == null) {
            return;
        }
        Object tp = this.tabPlayer(player);
        if (tp == null) {
            return;
        }
        try {
            this.nametagSetPrefix.invoke(this.nametagManager, tp, value);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public void setTablistPrefix(Player player, String value) {
        if (!this.available || this.tablistSetPrefix == null || player == null) {
            return;
        }
        Object tp = this.tabPlayer(player);
        if (tp == null) {
            return;
        }
        try {
            this.tablistSetPrefix.invoke(this.tablistManager, tp, value);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public void applyDuel(Player player, String prefix, String suffix) {
        this.setPrefix(player, prefix);
        this.setNametagSuffix(player, suffix);
    }

    public void clearDuel(Player player) {
        this.setPrefix(player, null);
        this.setNametagSuffix(player, null);
    }

    public void restoreRank(Player player) {
        this.setPrefix(player, null);
    }

    private void setPrefix(Player player, String value) {
        if (!this.available || player == null) {
            return;
        }
        Object tp = this.tabPlayer(player);
        if (tp == null) {
            return;
        }
        if (this.tablistSetPrefix != null) {
            try {
                this.tablistSetPrefix.invoke(this.tablistManager, tp, value);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        if (this.nametagSetPrefix != null) {
            try {
                this.nametagSetPrefix.invoke(this.nametagManager, tp, value);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
    }

    public void setNametagSuffix(Player player, String value) {
        if (!this.available || this.nametagSetSuffix == null || player == null) {
            return;
        }
        Object tp = this.tabPlayer(player);
        if (tp == null) {
            return;
        }
        try {
            this.nametagSetSuffix.invoke(this.nametagManager, tp, value);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }
}


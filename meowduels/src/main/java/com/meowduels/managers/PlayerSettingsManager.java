/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package com.meowduels.managers;

import com.meowduels.MeowDuels;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class PlayerSettingsManager {
    private final MeowDuels plugin;
    private final File file;
    /** key -> (player -> value). A row exists only once somebody has changed
     *  that toggle, so the default below is what everyone else gets and a new
     *  toggle needs no migration. */
    private final Map<String, Map<UUID, Boolean>> flags = new HashMap<String, Map<UUID, Boolean>>();
    private static final String[] KEYS = new String[]{
        "duel-requests", "scoreboard", "party-invites", "spectators", "sounds", "isolated-chat"};

    /**
     * The toggles that start OFF. Everything else starts on.
     *
     * <p>Isolated chat is here because it takes something away: a player who
     * has never opened /settings should not silently stop seeing half the
     * server. It is opt-in, and the tips say it exists.
     */
    private static final java.util.Set<String> OFF_BY_DEFAULT =
            new java.util.HashSet<String>(java.util.Arrays.asList("isolated-chat"));

    public PlayerSettingsManager(MeowDuels plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "settings.yml");
        this.load();
    }

    private void load() {
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration((File)this.file);
        for (String key : KEYS) {
            this.loadSection(cfg.getConfigurationSection(key), this.map(key));
        }
    }

    private void loadSection(ConfigurationSection root, Map<UUID, Boolean> into) {
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            try {
                into.put(UUID.fromString(key), root.getBoolean(key));
            }
            catch (Exception exception) {}
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (String key : KEYS) {
            for (Map.Entry<UUID, Boolean> e : this.map(key).entrySet()) {
                cfg.set(key + "." + String.valueOf(e.getKey()), (Object)e.getValue());
            }
        }
        try {
            cfg.save(this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("Failed to save settings.yml: " + ex.getMessage());
        }
    }

    private Map<UUID, Boolean> map(String key) {
        Map<UUID, Boolean> found = this.flags.get(key);
        if (found == null) {
            found = new HashMap<UUID, Boolean>();
            this.flags.put(key, found);
        }
        return found;
    }

    /** What this toggle is for a player who has never touched it. */
    public static boolean defaultOf(String key) {
        return !OFF_BY_DEFAULT.contains(key);
    }

    public boolean is(String key, UUID id) {
        Boolean v = this.map(key).get(id);
        return v == null ? PlayerSettingsManager.defaultOf(key) : v.booleanValue();
    }

    public void toggle(String key, UUID id) {
        this.map(key).put(id, !this.is(key, id));
        this.save();
    }

    public boolean isDuelRequests(UUID id) {
        return this.is("duel-requests", id);
    }

    public boolean isScoreboard(UUID id) {
        return this.is("scoreboard", id);
    }

    public boolean isPartyInvites(UUID id) {
        return this.is("party-invites", id);
    }

    public boolean isSpectators(UUID id) {
        return this.is("spectators", id);
    }

    public boolean isSounds(UUID id) {
        return this.is("sounds", id);
    }

    public boolean isIsolatedChat(UUID id) {
        return this.is("isolated-chat", id);
    }

    public void toggleDuelRequests(UUID id) {
        this.toggle("duel-requests", id);
    }

    public void toggleScoreboard(UUID id) {
        this.toggle("scoreboard", id);
    }
}


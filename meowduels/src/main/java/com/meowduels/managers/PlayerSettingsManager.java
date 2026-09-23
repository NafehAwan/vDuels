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
    /** key -> (player -> value). Every toggle defaults to ON when absent, so a
     *  new one needs no migration and an unknown player needs no row. */
    private final Map<String, Map<UUID, Boolean>> flags = new HashMap<String, Map<UUID, Boolean>>();
    private static final String[] KEYS = new String[]{
        "duel-requests", "scoreboard", "party-invites", "spectators", "sounds"};

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

    /** Every toggle is on unless the player turned it off. */
    public boolean is(String key, UUID id) {
        Boolean v = this.map(key).get(id);
        return v == null || v.booleanValue();
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

    public void toggleDuelRequests(UUID id) {
        this.toggle("duel-requests", id);
    }

    public void toggleScoreboard(UUID id) {
        this.toggle("scoreboard", id);
    }
}


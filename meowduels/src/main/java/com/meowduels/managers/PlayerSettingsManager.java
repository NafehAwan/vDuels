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
    private final Map<UUID, Boolean> duelRequests = new HashMap<UUID, Boolean>();
    private final Map<UUID, Boolean> scoreboard = new HashMap<UUID, Boolean>();

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
        this.loadSection(cfg.getConfigurationSection("duel-requests"), this.duelRequests);
        this.loadSection(cfg.getConfigurationSection("scoreboard"), this.scoreboard);
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
        for (Map.Entry<UUID, Boolean> e : this.duelRequests.entrySet()) {
            cfg.set("duel-requests." + String.valueOf(e.getKey()), (Object)e.getValue());
        }
        for (Map.Entry<UUID, Boolean> e : this.scoreboard.entrySet()) {
            cfg.set("scoreboard." + String.valueOf(e.getKey()), (Object)e.getValue());
        }
        try {
            cfg.save(this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("Failed to save settings.yml: " + ex.getMessage());
        }
    }

    public boolean isDuelRequests(UUID id) {
        Boolean v = this.duelRequests.get(id);
        return v == null || v != false;
    }

    public boolean isScoreboard(UUID id) {
        Boolean v = this.scoreboard.get(id);
        return v == null || v != false;
    }

    public void toggleDuelRequests(UUID id) {
        this.duelRequests.put(id, !this.isDuelRequests(id));
        this.save();
    }

    public void toggleScoreboard(UUID id) {
        this.scoreboard.put(id, !this.isScoreboard(id));
        this.save();
    }
}


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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class StatsManager {
    private final MeowDuels plugin;
    private final File file;
    private final Map<UUID, Integer> streak = new java.util.concurrent.ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Integer> wins = new java.util.concurrent.ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Integer> losses = new java.util.concurrent.ConcurrentHashMap<UUID, Integer>();
    // Concurrent: read on TAB's thread while matches on the main thread write.
    private final Map<UUID, Integer> played = new java.util.concurrent.ConcurrentHashMap<UUID, Integer>();

    public StatsManager(MeowDuels plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        this.load();
    }

    private void load() {
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration((File)this.file);
        this.loadSection(cfg.getConfigurationSection("streak"), this.streak);
        this.loadSection(cfg.getConfigurationSection("wins"), this.wins);
        this.loadSection(cfg.getConfigurationSection("losses"), this.losses);
        this.loadSection(cfg.getConfigurationSection("played"), this.played);
    }

    private void loadSection(ConfigurationSection root, Map<UUID, Integer> into) {
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            try {
                into.put(UUID.fromString(key), root.getInt(key, 0));
            }
            catch (Exception exception) {}
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, Integer> e : this.streak.entrySet()) {
            cfg.set("streak." + String.valueOf(e.getKey()), (Object)e.getValue());
        }
        for (Map.Entry<UUID, Integer> e : this.wins.entrySet()) {
            cfg.set("wins." + String.valueOf(e.getKey()), (Object)e.getValue());
        }
        for (Map.Entry<UUID, Integer> e : this.losses.entrySet()) {
            cfg.set("losses." + String.valueOf(e.getKey()), (Object)e.getValue());
        }
        for (Map.Entry<UUID, Integer> e : this.played.entrySet()) {
            cfg.set("played." + String.valueOf(e.getKey()), (Object)e.getValue());
        }
        try {
            cfg.save(this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("Failed to save stats.yml: " + ex.getMessage());
        }
    }

    public void addWin(UUID id) {
        this.streak.merge(id, 1, Integer::sum);
    }

    public void resetStreak(UUID id) {
        this.streak.put(id, 0);
    }

    public int getStreak(UUID id) {
        Integer v = this.streak.get(id);
        return v == null ? 0 : v;
    }

    public void addDuelWin(UUID id) {
        this.wins.merge(id, 1, Integer::sum);
    }

    public void addDuelLoss(UUID id) {
        this.losses.merge(id, 1, Integer::sum);
    }

    public int getWins(UUID id) {
        Integer v = this.wins.get(id);
        return v == null ? 0 : v;
    }

    public int getLosses(UUID id) {
        Integer v = this.losses.get(id);
        return v == null ? 0 : v;
    }

    public int getPlayed(UUID id) {
        Integer v = this.played.get(id);
        return v == null ? 0 : v;
    }

    public void addPlayed(UUID id) {
        this.played.merge(id, 1, Integer::sum);
    }

    /**
     * The leaderboard, ranked on duels won.
     *
     * <p>It used to be ranked on Elo. With Elo gone there is no hidden number
     * left to sort by, which is arguably the honest version: the board now
     * says what it counts.
     */
    public List<Map.Entry<UUID, Integer>> topWins(int limit) {
        ArrayList<Map.Entry<UUID, Integer>> out = new ArrayList<Map.Entry<UUID, Integer>>();
        for (Map.Entry<UUID, Integer> e : this.wins.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            out.add(e);
        }
        Collections.sort(out, new Comparator<Map.Entry<UUID, Integer>>(){

            @Override
            public int compare(Map.Entry<UUID, Integer> a, Map.Entry<UUID, Integer> b) {
                return b.getValue() - a.getValue();
            }
        });
        return out.size() > limit ? out.subList(0, limit) : out;
    }
}

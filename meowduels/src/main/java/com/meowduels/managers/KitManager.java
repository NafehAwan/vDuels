/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package com.meowduels.managers;

import com.meowduels.MeowDuels;
import com.meowduels.model.Kit;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class KitManager {
    private final MeowDuels plugin;
    private final File file;
    private final Map<String, Kit> kits = new LinkedHashMap<String, Kit>();

    public KitManager(MeowDuels plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kits.yml");
        if (!this.file.exists()) {
            plugin.saveResource("kits.yml", false);
        }
        this.load();
    }

    public void load() {
        this.kits.clear();
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration((File)this.file);
        ConfigurationSection root = config.getConfigurationSection("kits");
        if (root == null) {
            return;
        }
        for (String name : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(name);
            if (section == null) continue;
            this.kits.put(name.toLowerCase(Locale.ROOT), Kit.load(name, section));
        }
        this.plugin.getLogger().info("Loaded " + this.kits.size() + " kit(s).");
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Kit kit : this.kits.values()) {
            kit.save(config.createSection("kits." + kit.getName()));
        }
        try {
            config.save(this.file);
        }
        catch (IOException e) {
            this.plugin.getLogger().severe("Failed to save kits.yml: " + e.getMessage());
        }
    }

    public Kit get(String name) {
        return name == null ? null : this.kits.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String name) {
        return this.get(name) != null;
    }

    public void put(Kit kit) {
        this.kits.put(kit.getName().toLowerCase(Locale.ROOT), kit);
        this.save();
    }

    public void delete(String name) {
        this.kits.remove(name.toLowerCase(Locale.ROOT));
        this.save();
    }

    public List<Kit> all() {
        return new ArrayList<Kit>(this.kits.values());
    }

    public boolean isEmpty() {
        return this.kits.isEmpty();
    }
}


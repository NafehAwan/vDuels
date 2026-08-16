package com.vduels.managers;

import com.vduels.VDuels;
import com.vduels.model.Kit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Owns every kit: persistence, lookup and CRUD.
 */
public class KitManager {

    private final VDuels plugin;
    private final File file;
    private final Map<String, Kit> kits = new LinkedHashMap<>();

    public KitManager(VDuels plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kits.yml");
        if (!file.exists()) {
            plugin.saveResource("kits.yml", false); // ship the default kits
        }
        load();
    }

    public void load() {
        kits.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("kits");
        if (root == null) {
            return;
        }
        for (String name : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(name);
            if (section != null) {
                kits.put(name.toLowerCase(Locale.ROOT), Kit.load(name, section));
            }
        }
        plugin.getLogger().info("Loaded " + kits.size() + " kit(s).");
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Kit kit : kits.values()) {
            kit.save(config.createSection("kits." + kit.getName()));
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save kits.yml: " + e.getMessage());
        }
    }

    public Kit get(String name) {
        return name == null ? null : kits.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String name) {
        return get(name) != null;
    }

    public void put(Kit kit) {
        kits.put(kit.getName().toLowerCase(Locale.ROOT), kit);
        save();
    }

    public void delete(String name) {
        kits.remove(name.toLowerCase(Locale.ROOT));
        save();
    }

    public List<Kit> all() {
        return new ArrayList<>(kits.values());
    }

    public boolean isEmpty() {
        return kits.isEmpty();
    }
}

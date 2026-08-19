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
        // Fast path: try to parse the whole file at once.
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = null;
        try {
            config.load(file);
            root = config.getConfigurationSection("kits");
        } catch (Throwable t) {
            plugin.getLogger().warning("kits.yml couldn't be parsed in one pass ("
                    + t + "); loading each kit individually.");
        }
        if (root != null) {
            int failed = loadFromSection(root);
            plugin.getLogger().info("Loaded " + kits.size() + " kit(s)."
                    + (failed > 0 ? " (" + failed + " skipped)" : ""));
            return;
        }
        // Fallback: a single bad item can make Bukkit reject the entire file, so
        // load each kit in isolation and only skip the ones that truly fail.
        loadIndividually();
    }

    private int loadFromSection(ConfigurationSection root) {
        int failed = 0;
        for (String name : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(name);
            if (section == null) {
                continue;
            }
            try {
                kits.put(name.toLowerCase(Locale.ROOT), Kit.load(name, section));
            } catch (Exception e) {
                failed++;
                plugin.getLogger().warning("Skipping kit '" + name + "' - failed to load: " + e);
            }
        }
        return failed;
    }

    /** Parses the raw YAML and loads each kit through its own YamlConfiguration. */
    private void loadIndividually() {
        int failed = 0;
        try (java.io.Reader reader = new java.io.InputStreamReader(
                new java.io.FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8)) {
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Object top = yaml.load(reader);
            Object kitsObj = (top instanceof Map) ? ((Map<?, ?>) top).get("kits") : null;
            if (!(kitsObj instanceof Map)) {
                plugin.getLogger().severe("kits.yml has no 'kits' section - no kits loaded.");
                return;
            }
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) kitsObj).entrySet()) {
                String name = String.valueOf(entry.getKey());
                try {
                    Map<String, Object> single = new LinkedHashMap<>();
                    single.put(name, entry.getValue());
                    YamlConfiguration one = new YamlConfiguration();
                    one.loadFromString(yaml.dump(single));
                    ConfigurationSection sec = one.getConfigurationSection(name);
                    if (sec != null) {
                        kits.put(name.toLowerCase(Locale.ROOT), Kit.load(name, sec));
                    }
                } catch (Exception e) {
                    failed++;
                    plugin.getLogger().warning("Skipping kit '" + name + "' - failed to load: " + e);
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().severe("Could not read kits.yml: " + t);
        }
        plugin.getLogger().info("Loaded " + kits.size() + " kit(s) individually."
                + (failed > 0 ? " (" + failed + " skipped)" : ""));
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

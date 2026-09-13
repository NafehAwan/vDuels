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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class CategoryManager {
    private final MeowDuels plugin;
    private final File file;
    private final String resourceName;
    private final Map<String, Category> categories = new LinkedHashMap<String, Category>();

    public CategoryManager(MeowDuels plugin) {
        this(plugin, "categories.yml");
    }

    public CategoryManager(MeowDuels plugin, String resourceName) {
        this.plugin = plugin;
        this.resourceName = resourceName;
        this.file = new File(plugin.getDataFolder(), resourceName);
        if (!this.file.exists()) {
            plugin.saveResource(resourceName, false);
        }
        this.load();
    }

    private static String safeId(String id) {
        String cleaned = id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        return cleaned.isEmpty() ? "category" : cleaned;
    }

    public void load() {
        YamlConfiguration config;
        ConfigurationSection root;
        this.categories.clear();
        if (this.file.exists() && (root = (config = YamlConfiguration.loadConfiguration((File)this.file)).getConfigurationSection("categories")) != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection sec = root.getConfigurationSection(id);
                if (sec == null) continue;
                Category cat = new Category(id, sec.getString("header", id.toUpperCase(Locale.ROOT)));
                cat.getKits().addAll(sec.getStringList("kits"));
                this.categories.put(id.toLowerCase(Locale.ROOT), cat);
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Category cat : this.categories.values()) {
            ConfigurationSection sec = config.createSection("categories." + cat.getId());
            sec.set("header", (Object)cat.getHeader());
            sec.set("kits", new ArrayList<String>(cat.getKits()));
        }
        try {
            config.save(this.file);
        }
        catch (IOException e) {
            this.plugin.getLogger().severe("Failed to save categories.yml: " + e.getMessage());
        }
    }

    public List<Category> all() {
        return new ArrayList<Category>(this.categories.values());
    }

    public Category get(String id) {
        return id == null ? null : this.categories.get(CategoryManager.safeId(id));
    }

    public boolean create(String id, String header) {
        String key = CategoryManager.safeId(id);
        if (this.categories.containsKey(key)) {
            return false;
        }
        this.categories.put(key, new Category(key, header == null ? id.toUpperCase(Locale.ROOT) : header));
        this.save();
        return true;
    }

    public void delete(String id) {
        this.categories.remove(CategoryManager.safeId(id));
        this.save();
    }

    public List<String> kitsFor(Category category) {
        ArrayList<String> out = new ArrayList<String>();
        for (String name : category.getKits()) {
            if (!this.plugin.getKitManager().exists(name)) continue;
            out.add(name);
        }
        return out;
    }

    public static final class Category {
        private final String id;
        private String header;
        private final List<String> kits = new ArrayList<String>();

        Category(String id, String header) {
            this.id = id;
            this.header = header;
        }

        public String getId() {
            return this.id;
        }

        public String getHeader() {
            return this.header;
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public List<String> getKits() {
            return this.kits;
        }
    }
}


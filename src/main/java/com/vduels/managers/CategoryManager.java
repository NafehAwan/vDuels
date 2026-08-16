package com.vduels.managers;

import com.vduels.VDuels;
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
 * Kit-menu categories. Each category has a display header (shown as
 * "DUELS &rarr; &lt;header&gt;") and a list of kits; the kit menu shows one
 * category at a time and the arrow cycles to the next. An empty kit list means
 * "all kits". There is always at least one category (a default is seeded).
 */
public class CategoryManager {

    public static final class Category {
        private final String id;
        private String header;
        private final List<String> kits = new ArrayList<>();

        Category(String id, String header) {
            this.id = id;
            this.header = header;
        }

        public String getId() {
            return id;
        }

        public String getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public List<String> getKits() {
            return kits;
        }
    }

    private final VDuels plugin;
    private final File file;
    private final Map<String, Category> categories = new LinkedHashMap<>();

    public CategoryManager(VDuels plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "categories.yml");
        if (!file.exists()) {
            plugin.saveResource("categories.yml", false); // ship the default category
        }
        load();
    }

    /**
     * Category ids are used as YAML keys, so they must not contain '.' (Bukkit's
     * path separator) or spaces - those would silently split the section.
     */
    private static String safeId(String id) {
        String cleaned = id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        return cleaned.isEmpty() ? "category" : cleaned;
    }

    public void load() {
        categories.clear();
        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection root = config.getConfigurationSection("categories");
            if (root != null) {
                for (String id : root.getKeys(false)) {
                    ConfigurationSection sec = root.getConfigurationSection(id);
                    if (sec == null) {
                        continue;
                    }
                    Category cat = new Category(id, sec.getString("header", id.toUpperCase(Locale.ROOT)));
                    cat.getKits().addAll(sec.getStringList("kits"));
                    categories.put(id.toLowerCase(Locale.ROOT), cat);
                }
            }
        }
        // No default category is seeded - admins create their own.
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Category cat : categories.values()) {
            ConfigurationSection sec = config.createSection("categories." + cat.getId());
            sec.set("header", cat.getHeader());
            sec.set("kits", new ArrayList<>(cat.getKits()));
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save categories.yml: " + e.getMessage());
        }
    }

    public List<Category> all() {
        return new ArrayList<>(categories.values());
    }

    public Category get(String id) {
        return id == null ? null : categories.get(safeId(id));
    }

    public boolean create(String id, String header) {
        String key = safeId(id);
        if (categories.containsKey(key)) {
            return false;
        }
        categories.put(key, new Category(key, header == null ? id.toUpperCase(Locale.ROOT) : header));
        save();
        return true;
    }

    public void delete(String id) {
        categories.remove(safeId(id));
        save();
    }

    /** The kit names to show for a category: only the ones added to it. */
    public List<String> kitsFor(Category category) {
        List<String> out = new ArrayList<>();
        for (String name : category.getKits()) {
            if (plugin.getKitManager().exists(name)) {
                out.add(name);
            }
        }
        return out;
    }
}

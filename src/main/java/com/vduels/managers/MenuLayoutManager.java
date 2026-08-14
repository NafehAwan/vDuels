package com.vduels.managers;

import com.vduels.VDuels;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores the admin-customised layout for the /duel menu's top area (slots
 * 0-44): kit icons and decorative items such as black stained glass. The bottom
 * row is always reserved for live controls and is never persisted here.
 */
public class MenuLayoutManager {

    public static final int EDITABLE_SLOTS = 45; // rows 1-5

    private final VDuels plugin;
    private final File file;
    private final Map<Integer, ItemStack> layout = new HashMap<>();

    public MenuLayoutManager(VDuels plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "duel-menu.yml");
        load();
    }

    public void load() {
        layout.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("layout");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                ItemStack item = root.getItemStack(key);
                if (item != null && slot >= 0 && slot < EDITABLE_SLOTS) {
                    layout.put(slot, item);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public void save(Map<Integer, ItemStack> newLayout) {
        layout.clear();
        layout.putAll(newLayout);
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection("layout");
        for (Map.Entry<Integer, ItemStack> entry : layout.entrySet()) {
            if (entry.getValue() != null) {
                root.set(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save duel-menu.yml: " + e.getMessage());
        }
    }

    public boolean hasLayout() {
        return !layout.isEmpty();
    }

    public Map<Integer, ItemStack> getLayout() {
        return new HashMap<>(layout);
    }
}

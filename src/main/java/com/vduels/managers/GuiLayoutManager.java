package com.vduels.managers;

import com.vduels.VDuels;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores the admin-customised layout (slot -> item) for each editable menu.
 * A layout mixes decoration items with "marker" items whose persistent-data
 * tags tell the live menu where to render its buttons / kit icons / arena
 * icons. Everything is one YAML file: {@code gui-layouts.yml}.
 */
public class GuiLayoutManager {

    public static final String DUEL_CONFIRM = "duelconfirm";
    public static final String MAP_SELECT = "mapselect";
    public static final String KIT_MENU = "kitmenu";

    private final VDuels plugin;
    private final File file;
    private final Map<String, Map<Integer, ItemStack>> layouts = new HashMap<>();

    public GuiLayoutManager(VDuels plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "gui-layouts.yml");
        load();
    }

    /** Player-facing rows for a menu (the editor is always 6 rows). */
    public static int rows(String menuId) {
        return menuId.equals(DUEL_CONFIRM) ? 3 : 6;
    }

    /** Editable slot count in the editor (the last row holds controls). */
    public static int editableSize(String menuId) {
        return menuId.equals(DUEL_CONFIRM) ? 27 : 45;
    }

    public static boolean isValidMenu(String menuId) {
        return menuId.equals(DUEL_CONFIRM) || menuId.equals(MAP_SELECT) || menuId.equals(KIT_MENU);
    }

    public void load() {
        layouts.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("layouts");
        if (root == null) {
            return;
        }
        for (String menuId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(menuId);
            if (section == null) {
                continue;
            }
            Map<Integer, ItemStack> layout = new LinkedHashMap<>();
            for (String key : section.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ItemStack item = section.getItemStack(key);
                    if (item != null) {
                        layout.put(slot, item);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            if (!layout.isEmpty()) {
                layouts.put(menuId, layout);
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Map<Integer, ItemStack>> entry : layouts.entrySet()) {
            ConfigurationSection section = config.createSection("layouts." + entry.getKey());
            for (Map.Entry<Integer, ItemStack> slot : entry.getValue().entrySet()) {
                section.set(String.valueOf(slot.getKey()), slot.getValue());
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save gui-layouts.yml: " + e.getMessage());
        }
    }

    public boolean has(String menuId) {
        Map<Integer, ItemStack> layout = layouts.get(menuId);
        return layout != null && !layout.isEmpty();
    }

    public Map<Integer, ItemStack> get(String menuId) {
        Map<Integer, ItemStack> layout = layouts.get(menuId);
        return layout == null ? new LinkedHashMap<>() : new LinkedHashMap<>(layout);
    }

    public void set(String menuId, Map<Integer, ItemStack> layout) {
        layouts.put(menuId, new LinkedHashMap<>(layout));
        save();
    }

    public void clear(String menuId) {
        layouts.remove(menuId);
        save();
    }
}

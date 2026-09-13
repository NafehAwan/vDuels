/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.inventory.ItemStack
 */
package com.meowduels.managers;

import com.meowduels.MeowDuels;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class GuiLayoutManager {
    public static final String DUEL_CONFIRM = "duelconfirm";
    public static final String MAP_SELECT = "mapselect";
    public static final String KIT_MENU = "kitmenu";
    private final MeowDuels plugin;
    private final File file;
    private final Map<String, Map<Integer, ItemStack>> layouts = new HashMap<String, Map<Integer, ItemStack>>();

    public GuiLayoutManager(MeowDuels plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "gui-layouts.yml");
        this.load();
    }

    public static int rows(String menuId) {
        return 4;
    }

    public static int editableSize(String menuId) {
        return 36;
    }

    public static boolean isValidMenu(String menuId) {
        return menuId.equals(DUEL_CONFIRM) || menuId.equals(MAP_SELECT);
    }

    public static String categoryMenuId(String categoryId) {
        return "category:" + categoryId.toLowerCase(Locale.ROOT);
    }

    public static boolean isCategoryMenu(String menuId) {
        return menuId.startsWith("category:");
    }

    public void load() {
        this.layouts.clear();
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration((File)this.file);
        ConfigurationSection root = config.getConfigurationSection("layouts");
        if (root == null) {
            return;
        }
        for (String menuId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(menuId);
            if (section == null) continue;
            LinkedHashMap<Integer, ItemStack> layout = new LinkedHashMap<Integer, ItemStack>();
            for (String key : section.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ItemStack item = section.getItemStack(key);
                    if (item == null) continue;
                    layout.put(slot, item);
                }
                catch (NumberFormatException numberFormatException) {}
            }
            if (layout.isEmpty()) continue;
            this.layouts.put(menuId, layout);
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Map<Integer, ItemStack>> entry : this.layouts.entrySet()) {
            ConfigurationSection section = config.createSection("layouts." + entry.getKey());
            for (Map.Entry<Integer, ItemStack> slot : entry.getValue().entrySet()) {
                section.set(String.valueOf(slot.getKey()), (Object)slot.getValue());
            }
        }
        try {
            config.save(this.file);
        }
        catch (IOException e) {
            this.plugin.getLogger().severe("Failed to save gui-layouts.yml: " + e.getMessage());
        }
    }

    public boolean has(String menuId) {
        Map<Integer, ItemStack> layout = this.layouts.get(menuId);
        return layout != null && !layout.isEmpty();
    }

    public Map<Integer, ItemStack> get(String menuId) {
        Map<Integer, ItemStack> layout = this.layouts.get(menuId);
        return layout == null ? new LinkedHashMap<Integer, ItemStack>() : new LinkedHashMap<Integer, ItemStack>(layout);
    }

    public void set(String menuId, Map<Integer, ItemStack> layout) {
        this.layouts.put(menuId, new LinkedHashMap<Integer, ItemStack>(layout));
        this.save();
    }

    public void clear(String menuId) {
        this.layouts.remove(menuId);
        this.save();
    }
}


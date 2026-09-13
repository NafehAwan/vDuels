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
import com.meowduels.util.Trims;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class TrimPreferenceManager {
    private final MeowDuels plugin;
    private final File file;
    private final Map<UUID, Map<String, String[]>> prefs = new HashMap<UUID, Map<String, String[]>>();

    public TrimPreferenceManager(MeowDuels plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "trims.yml");
        this.load();
    }

    private void load() {
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration((File)this.file);
        for (String uuidKey : cfg.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(uuidKey);
            }
            catch (Exception ex) {
                continue;
            }
            ConfigurationSection kits = cfg.getConfigurationSection(uuidKey);
            if (kits == null) continue;
            for (String kit : kits.getKeys(false)) {
                ConfigurationSection slots = kits.getConfigurationSection(kit);
                if (slots == null) continue;
                String[] arr = new String[4];
                for (String slotKey : slots.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(slotKey);
                        if (slot < 0 || slot >= 4) continue;
                        arr[slot] = slots.getString(slotKey);
                    }
                    catch (NumberFormatException numberFormatException) {}
                }
                this.byKit(id).put(kit.toLowerCase(Locale.ROOT), arr);
            }
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, String[]>> byPlayer : this.prefs.entrySet()) {
            for (Map.Entry<String, String[]> byKit : byPlayer.getValue().entrySet()) {
                String[] arr = byKit.getValue();
                for (int slot = 0; slot < 4; ++slot) {
                    if (arr[slot] == null) continue;
                    cfg.set(String.valueOf(byPlayer.getKey()) + "." + byKit.getKey() + "." + slot, (Object)arr[slot]);
                }
            }
        }
        try {
            cfg.save(this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("Failed to save trims.yml: " + ex.getMessage());
        }
    }

    private Map<String, String[]> byKit(UUID id) {
        return this.prefs.computeIfAbsent(id, k -> new HashMap());
    }

    private String[] slotsFor(UUID id, String kit, boolean create) {
        String[] arr;
        Map<String, String[]> kits = this.prefs.get(id);
        String key = kit.toLowerCase(Locale.ROOT);
        if (kits == null) {
            if (!create) {
                return null;
            }
            kits = this.byKit(id);
        }
        if ((arr = kits.get(key)) == null && create) {
            arr = new String[4];
            kits.put(key, arr);
        }
        return arr;
    }

    public void set(UUID id, String kit, int slot, String pattern, String material) {
        if (slot < 0 || slot > 3) {
            return;
        }
        this.slotsFor((UUID)id, (String)kit, (boolean)true)[slot] = pattern + ":" + material;
        this.save();
    }

    public void setAll(UUID id, String kit, String pattern, String material) {
        String[] arr = this.slotsFor(id, kit, true);
        String val = pattern + ":" + material;
        for (int i = 0; i < 4; ++i) {
            arr[i] = val;
        }
        this.save();
    }

    public void clear(UUID id, String kit) {
        Map<String, String[]> kits = this.prefs.get(id);
        if (kits != null) {
            kits.remove(kit.toLowerCase(Locale.ROOT));
            this.save();
        }
    }

    public String[] get(UUID id, String kit, int slot) {
        String[] arr = this.slotsFor(id, kit, false);
        if (arr == null || slot < 0 || slot > 3 || arr[slot] == null) {
            return null;
        }
        int sep = arr[slot].indexOf(58);
        if (sep < 0) {
            return null;
        }
        return new String[]{arr[slot].substring(0, sep), arr[slot].substring(sep + 1)};
    }

    public void applyToArmor(UUID id, String kit, ItemStack[] armor) {
        String[] arr = this.slotsFor(id, kit, false);
        if (arr == null || armor == null) {
            return;
        }
        for (int slot = 0; slot < 4 && slot < armor.length; ++slot) {
            int sep;
            if (arr[slot] == null || armor[slot] == null || (sep = arr[slot].indexOf(58)) < 0) continue;
            Trims.apply(armor[slot], arr[slot].substring(0, sep), arr[slot].substring(sep + 1));
        }
    }
}


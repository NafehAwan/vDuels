/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 */
package com.meowtags;

import com.meowtags.Gradient;
import com.meowtags.MeowTags;
import com.meowtags.Tag;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class TagManager {
    private static final String[][] DEFAULTS = new String[][]{{"demon", "ff3131", "7a0000"}, {"goat", "ffd700", "ff8c00"}, {"beast", "ff6a00", "b30000"}, {"savage", "ff0080", "7928ca"}, {"legend", "ffd700", "ff7a00"}, {"sweat", "00c6ff", "0047ff"}, {"toxic", "aaff00", "00a300"}, {"king", "ffe259", "b8860b"}, {"queen", "ff9ae0", "c724b1"}, {"ghost", "ffffff", "8a8a8a"}, {"reaper", "9d4edd", "240046"}, {"shadow", "9e9e9e", "1a1a1a"}, {"venom", "39ff14", "00a300"}, {"blaze", "ff7a00", "ff0000"}, {"frost", "a5f3ff", "2b8cff"}, {"storm", "cfd9df", "4a6fa5"}, {"viper", "76ff7a", "1e5631"}, {"wolf", "e0e0e0", "708090"}, {"dragon", "ff4e00", "ec9f05"}, {"phoenix", "ff512f", "f09819"}, {"titan", "e0e0e0", "4b6cb7"}, {"ninja", "6d6d6d", "0a0a0a"}, {"samurai", "ff4b47", "8b0000"}, {"assassin", "b06cff", "4a00e0"}, {"hunter", "8bd94f", "1d4e0a"}, {"slayer", "ff3b3b", "6b0000"}, {"warrior", "ffd200", "f7971e"}, {"immortal", "ffe259", "ffa751"}, {"god", "fffacd", "ffcf00"}, {"angel", "ffffff", "ffe08a"}, {"devil", "ff1a1a", "7a0000"}, {"rebel", "ff4b2b", "b31217"}, {"rogue", "8a8ab5", "2b2b4a"}, {"elite", "00f5a0", "00b8d9"}, {"pro", "ff4fa3", "9b1fd6"}, {"menace", "ff7b54", "dd2476"}, {"chaos", "ff2ec4", "6a1b9a"}, {"inferno", "ffb300", "ff2200"}, {"glitch", "00ffff", "ff00e5"}, {"cyber", "22e5ff", "ff2bd6"}, {"nova", "ffcf5c", "ff3d3d"}, {"omega", "e07bff", "8a2be2"}};
    private final MeowTags plugin;
    private final Map<String, Tag> tags = new LinkedHashMap<String, Tag>();
    private final Map<UUID, String> selection = new LinkedHashMap<UUID, String>();
    private final Map<UUID, Boolean> shown = new LinkedHashMap<UUID, Boolean>();
    private final File tagsFile;
    private final File dataFile;
    private int priority = 10000;
    private String duelTag = "meowduel";

    public TagManager(MeowTags plugin) {
        this.plugin = plugin;
        this.tagsFile = new File(plugin.getDataFolder(), "tags.yml");
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        this.priority = this.plugin.getConfig().getInt("suffix-priority", 10000);
        this.duelTag = this.plugin.getConfig().getString("duel-scoreboard-tag", "meowduel");
        this.loadTags();
        this.loadData();
    }

    private void loadTags() {
        this.tags.clear();
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration((File)this.tagsFile);
        ConfigurationSection sec = cfg.getConfigurationSection("tags");
        if (sec == null || sec.getKeys(false).isEmpty()) {
            for (String[] d : DEFAULTS) {
                cfg.set("tags." + d[0] + ".display", (Object)d[0]);
                cfg.set("tags." + d[0] + ".from", (Object)d[1]);
                cfg.set("tags." + d[0] + ".to", (Object)d[2]);
            }
            try {
                cfg.save(this.tagsFile);
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("Could not write tags.yml: " + e.getMessage());
            }
            sec = cfg.getConfigurationSection("tags");
        }
        HashSet<String> defaultIds = new HashSet<String>();
        for (String[] d : DEFAULTS) {
            defaultIds.add(d[0]);
        }
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                String disp = sec.getString(id + ".display", id);
                String from = sec.getString(id + ".from", "ffffff");
                String to = sec.getString(id + ".to", "888888");
                boolean custom = !defaultIds.contains(id);
                this.tags.put(id.toLowerCase(), new Tag(id.toLowerCase(), disp, from, to, custom));
            }
        }
    }

    private void loadData() {
        this.selection.clear();
        this.shown.clear();
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration((File)this.dataFile);
        ConfigurationSection sec = cfg.getConfigurationSection("players");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                String id = sec.getString(key);
                if (id == null || !this.tags.containsKey(id.toLowerCase())) continue;
                try {
                    this.selection.put(UUID.fromString(key), id.toLowerCase());
                }
                catch (IllegalArgumentException illegalArgumentException) {}
            }
        }
    }

    public void saveData() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, String> e : this.selection.entrySet()) {
            cfg.set("players." + e.getKey().toString(), (Object)e.getValue());
        }
        try {
            cfg.save(this.dataFile);
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("Could not write data.yml: " + e.getMessage());
        }
    }

    public List<Tag> ordered() {
        return new ArrayList<Tag>(this.tags.values());
    }

    /** The tags this player actually owns, in menu order.
     *
     *  <p>The menu and the click handler MUST both use this: a slot number is
     *  just an index into the list, so if the menu were filtered and the click
     *  handler still read the full list, clicking a tag would equip a different
     *  one. */
    public List<Tag> available(Player player) {
        ArrayList<Tag> out = new ArrayList<Tag>();
        if (player == null) {
            return out;
        }
        for (Tag tag : this.tags.values()) {
            if (player.hasPermission(tag.permission())) {
                out.add(tag);
            }
        }
        return out;
    }

    public Tag get(String id) {
        return id == null ? null : this.tags.get(id.toLowerCase());
    }

    public String selected(Player p) {
        return this.selection.get(p.getUniqueId());
    }

    public boolean isInDuel(Player p) {
        try {
            return p.getScoreboardTags().contains(this.duelTag);
        }
        catch (Throwable t) {
            return false;
        }
    }

    public boolean addCustom(String id, String display, String from, String to) {
        String key = id.toLowerCase();
        this.tags.put(key, new Tag(key, display, from, to, true));
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration((File)this.tagsFile);
        cfg.set("tags." + key + ".display", (Object)display);
        cfg.set("tags." + key + ".from", (Object)from);
        cfg.set("tags." + key + ".to", (Object)to);
        try {
            cfg.save(this.tagsFile);
            return true;
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("Could not save custom tag: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteCustom(String id) {
        String key = id.toLowerCase();
        Tag t = this.tags.get(key);
        if (t == null || !t.custom) {
            return false;
        }
        this.tags.remove(key);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration((File)this.tagsFile);
        cfg.set("tags." + key, null);
        try {
            cfg.save(this.tagsFile);
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("Could not delete custom tag: " + e.getMessage());
        }
        return true;
    }

    public void apply(Player p, String id) {
        this.selection.put(p.getUniqueId(), id.toLowerCase());
        this.shown.remove(p.getUniqueId());
        this.refresh(p);
        this.saveData();
    }

    public void clear(Player p) {
        this.selection.remove(p.getUniqueId());
        this.shown.remove(p.getUniqueId());
        this.lp("lp user " + String.valueOf(p.getUniqueId()) + " meta removesuffix " + this.priority);
        this.saveData();
    }

    public void refresh(Player p) {
        UUID u = p.getUniqueId();
        if (!this.selection.containsKey(u)) {
            return;
        }
        boolean shouldShow = !this.isInDuel(p);
        Boolean cur = this.shown.get(u);
        if (shouldShow) {
            if (!Boolean.TRUE.equals(cur)) {
                this.show(p);
            }
        } else if (!Boolean.FALSE.equals(cur)) {
            this.hide(p);
        }
    }

    private void show(Player p) {
        Tag t = this.tags.get(this.selection.get(p.getUniqueId()));
        if (t == null) {
            return;
        }
        String uuid = p.getUniqueId().toString();
        String val = " " + Gradient.legacySuffix(t.display, t.hex1, t.hex2);
        this.lp("lp user " + uuid + " meta removesuffix " + this.priority);
        this.lp("lp user " + uuid + " meta setsuffix " + this.priority + " \"" + val + "\"");
        this.shown.put(p.getUniqueId(), Boolean.TRUE);
    }

    private void hide(Player p) {
        this.lp("lp user " + String.valueOf(p.getUniqueId()) + " meta removesuffix " + this.priority);
        this.shown.put(p.getUniqueId(), Boolean.FALSE);
    }

    private void lp(String cmd) {
        try {
            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)cmd);
        }
        catch (Throwable t) {
            this.plugin.getLogger().warning("LuckPerms command failed: " + cmd);
        }
    }
}


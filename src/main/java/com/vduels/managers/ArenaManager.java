package com.vduels.managers;

import com.vduels.VDuels;
import com.vduels.model.Arena;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
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
 * Owns every arena: persistence, lookup, CRUD and the region operations
 * (regenerate / copy / paste) that back the "duplicator" feature.
 */
public class ArenaManager {

    private final VDuels plugin;
    private final File file;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();

    // Simple per-player clipboard for the arena duplicator.
    private final Map<java.util.UUID, RegionClipboard> clipboards = new java.util.HashMap<>();

    public ArenaManager(VDuels plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
        load();
    }

    public void load() {
        arenas.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("arenas");
        if (root == null) {
            return;
        }
        for (String name : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(name);
            if (section != null) {
                arenas.put(name.toLowerCase(Locale.ROOT), Arena.load(name, section));
            }
        }
        plugin.getLogger().info("Loaded " + arenas.size() + " arena(s).");
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Arena arena : arenas.values()) {
            arena.save(config.createSection("arenas." + arena.getName()));
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save arenas.yml: " + e.getMessage());
        }
    }

    public Arena get(String name) {
        return name == null ? null : arenas.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String name) {
        return get(name) != null;
    }

    public Arena create(String name) {
        Arena arena = new Arena(name);
        arenas.put(name.toLowerCase(Locale.ROOT), arena);
        save();
        return arena;
    }

    public void delete(String name) {
        arenas.remove(name.toLowerCase(Locale.ROOT));
        save();
    }

    public List<Arena> all() {
        return new ArrayList<>(arenas.values());
    }

    /** Returns any configured arena not currently in use, or null. */
    public Arena findFreeArena(java.util.function.Predicate<Arena> inUse) {
        for (Arena arena : arenas.values()) {
            if (arena.isConfigured() && !inUse.test(arena)) {
                return arena;
            }
        }
        return null;
    }

    public Arena findArenaAt(Location loc) {
        for (Arena arena : arenas.values()) {
            if (arena.contains(loc)) {
                return arena;
            }
        }
        return null;
    }

    // --- region operations ------------------------------------------------

    /**
     * Restores a set of blocks to previously-captured data. Used for the fast
     * post-duel regeneration path (only blocks that actually changed).
     */
    public void restoreBlocks(Map<Location, BlockData> originals) {
        for (Map.Entry<Location, BlockData> entry : originals.entrySet()) {
            Location loc = entry.getKey();
            if (loc.getWorld() == null) {
                continue;
            }
            loc.getBlock().setBlockData(entry.getValue(), false);
        }
    }

    /** Copies the whole arena region into a player's clipboard. */
    public boolean copyToClipboard(java.util.UUID player, Arena arena) {
        if (!arena.isConfigured()) {
            return false;
        }
        clipboards.put(player, RegionClipboard.capture(arena));
        return true;
    }

    public boolean hasClipboard(java.util.UUID player) {
        return clipboards.containsKey(player);
    }

    /**
     * Pastes a player's clipboard so its minimum corner lands at {@code target}.
     * Returns the number of blocks written, or -1 if there is no clipboard.
     */
    public int pasteClipboard(java.util.UUID player, Location target) {
        RegionClipboard clip = clipboards.get(player);
        if (clip == null) {
            return -1;
        }
        return clip.pasteAt(target);
    }

    /**
     * Lightweight in-memory clipboard of a cuboid region. Kept as strings so it
     * is fully version independent.
     */
    public static final class RegionClipboard {
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;
        private final String[] data; // flattened blockdata strings

        private RegionClipboard(int sizeX, int sizeY, int sizeZ, String[] data) {
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.data = data;
        }

        static RegionClipboard capture(Arena arena) {
            World world = arena.getWorld();
            Location min = arena.getMin();
            Location max = arena.getMax();
            int sx = max.getBlockX() - min.getBlockX() + 1;
            int sy = max.getBlockY() - min.getBlockY() + 1;
            int sz = max.getBlockZ() - min.getBlockZ() + 1;
            String[] data = new String[sx * sy * sz];
            int i = 0;
            for (int y = 0; y < sy; y++) {
                for (int x = 0; x < sx; x++) {
                    for (int z = 0; z < sz; z++) {
                        Block b = world.getBlockAt(min.getBlockX() + x, min.getBlockY() + y, min.getBlockZ() + z);
                        data[i++] = b.getBlockData().getAsString();
                    }
                }
            }
            return new RegionClipboard(sx, sy, sz, data);
        }

        int pasteAt(Location target) {
            World world = target.getWorld();
            int baseX = target.getBlockX();
            int baseY = target.getBlockY();
            int baseZ = target.getBlockZ();
            int written = 0;
            int i = 0;
            for (int y = 0; y < sizeY; y++) {
                for (int x = 0; x < sizeX; x++) {
                    for (int z = 0; z < sizeZ; z++) {
                        String str = data[i++];
                        if (str == null) {
                            continue;
                        }
                        BlockData bd;
                        try {
                            bd = org.bukkit.Bukkit.createBlockData(str);
                        } catch (IllegalArgumentException ex) {
                            bd = Material.AIR.createBlockData();
                        }
                        world.getBlockAt(baseX + x, baseY + y, baseZ + z).setBlockData(bd, false);
                        written++;
                    }
                }
            }
            return written;
        }
    }
}

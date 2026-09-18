/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package com.meowduels.managers;

import com.meowduels.MeowDuels;
import com.meowduels.model.Arena;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Item;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Entity;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class ArenaManager {
    private static final Pattern TRAILING_NUMBER = Pattern.compile("^(.*?)(\\d+)$");
    private final MeowDuels plugin;
    private final File file;
    private final File snapshotDir;
    /** Arenas a fight has touched since they were last regenerated. Written to
     *  disk, because the whole point is to survive a server that stops without
     *  running any of our shutdown code. */
    private final Set<String> dirty = new HashSet<String>();
    private final File dirtyFile;
    private final Map<String, Arena> arenas = new LinkedHashMap<String, Arena>();
    private final Map<UUID, RegionClipboard> clipboards = new HashMap<UUID, RegionClipboard>();
    private final Map<String, RegionClipboard> snapshots = new HashMap<String, RegionClipboard>();
    private volatile Set<String> arenaWorlds = new HashSet<String>();

    public ArenaManager(MeowDuels plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
        this.snapshotDir = new File(plugin.getDataFolder(), "arena-snapshots");
        this.dirtyFile = new File(plugin.getDataFolder(), "arenas-dirty.yml");
        if (!this.snapshotDir.exists()) {
            this.snapshotDir.mkdirs();
        }
        this.load();
        this.loadDirty();
    }

    /**
     * Notes that an arena is in use and its blocks may have been changed.
     *
     * <p>Cheap - the file only holds arena names, and only changes when a match
     * starts or an arena is restored.
     */
    /**
     * Removes the loose entities a fight leaves behind - dropped items, arrows,
     * primed TNT, xp orbs.
     *
     * <p>Block regeneration does not touch entities, so without this the next
     * match in an arena starts on a floor covered in the last one's gear. Lives
     * here rather than in one manager because duels, events and party matches
     * all need exactly the same sweep.
     */
    public void clearLooseEntities(Arena arena) {
        if (arena == null) {
            return;
        }
        World world = arena.getWorld();
        Location min = arena.getMin();
        Location max = arena.getMax();
        if (world == null || min == null || max == null) {
            return;
        }
        Location center = new Location(world,
                (double)(min.getBlockX() + max.getBlockX()) / 2.0 + 0.5,
                (double)(min.getBlockY() + max.getBlockY()) / 2.0 + 0.5,
                (double)(min.getBlockZ() + max.getBlockZ()) / 2.0 + 0.5);
        double dx = (double)Math.abs(max.getBlockX() - min.getBlockX()) / 2.0 + 2.0;
        double dy = (double)Math.abs(max.getBlockY() - min.getBlockY()) / 2.0 + 2.0;
        double dz = (double)Math.abs(max.getBlockZ() - min.getBlockZ()) / 2.0 + 2.0;
        for (Entity entity : world.getNearbyEntities(center, dx, dy, dz)) {
            if (!(entity instanceof Projectile) && !(entity instanceof Item)
                    && !(entity instanceof TNTPrimed) && !(entity instanceof ExperienceOrb)) continue;
            entity.remove();
        }
    }

    public void markDirty(String arenaName) {
        if (arenaName != null && this.dirty.add(arenaName.toLowerCase(Locale.ROOT))) {
            this.saveDirty();
        }
    }

    /** The arena has been regenerated; it no longer needs recovering. */
    public void clearDirty(String arenaName) {
        if (arenaName != null && this.dirty.remove(arenaName.toLowerCase(Locale.ROOT))) {
            this.saveDirty();
        }
    }

    /**
     * Restores every arena that was mid-fight when the server last stopped.
     *
     * <p>The ordinary teardown paths regenerate an arena when a match ends, and
     * shutdown now does the same for matches still running. Neither helps if the
     * process is killed outright, and nothing else would ever notice - an arena
     * with a hole in it looks exactly like an arena that was built that way.
     * This runs once on startup and closes that last gap.
     */
    public void recoverDirtyArenas() {
        if (this.dirty.isEmpty()) {
            return;
        }
        List<String> pending = new ArrayList<String>(this.dirty);
        int done = 0;
        for (String name : pending) {
            Arena arena = this.get(name);
            if (arena == null || !arena.isAutoRegenerate()) {
                this.dirty.remove(name);
                continue;
            }
            if (this.regenArena(arena) >= 0) {
                done++;
            }
            this.dirty.remove(name);
        }
        this.saveDirty();
        if (done > 0) {
            this.plugin.getLogger().info("Regenerated " + done
                    + " arena(s) left dirty by the last shutdown.");
        }
    }

    private void loadDirty() {
        if (!this.dirtyFile.exists()) {
            return;
        }
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration((File)this.dirtyFile);
            for (String name : cfg.getStringList("dirty")) {
                if (name != null && !name.isEmpty()) {
                    this.dirty.add(name.toLowerCase(Locale.ROOT));
                }
            }
        }
        catch (Throwable t) {
            this.plugin.getLogger().warning("Could not read arenas-dirty.yml: " + t);
        }
    }

    private void saveDirty() {
        try {
            YamlConfiguration cfg = new YamlConfiguration();
            cfg.set("dirty", (Object)new ArrayList<String>(this.dirty));
            cfg.save(this.dirtyFile);
        }
        catch (Throwable t) {
            this.plugin.getLogger().warning("Could not write arenas-dirty.yml: " + t);
        }
    }

    public void load() {
        this.arenas.clear();
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration((File)this.file);
        ConfigurationSection root = config.getConfigurationSection("arenas");
        if (root == null) {
            return;
        }
        for (String name : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(name);
            if (section == null) continue;
            this.arenas.put(name.toLowerCase(Locale.ROOT), Arena.load(name, section));
        }
        this.rebuildWorldIndex();
        this.plugin.getLogger().info("Loaded " + this.arenas.size() + " arena(s).");
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Arena arena : this.arenas.values()) {
            arena.save(config.createSection("arenas." + arena.getName()));
        }
        try {
            config.save(this.file);
        }
        catch (IOException e) {
            this.plugin.getLogger().severe("Failed to save arenas.yml: " + e.getMessage());
        }
        this.rebuildWorldIndex();
    }

    private void rebuildWorldIndex() {
        HashSet<String> worlds = new HashSet<String>();
        for (Arena arena : this.arenas.values()) {
            String w = arena.getWorldName();
            if (w == null) continue;
            worlds.add(w.toLowerCase(Locale.ROOT));
        }
        this.arenaWorlds = worlds;
    }

    public boolean hasArenaInWorld(World world) {
        return world != null && this.arenaWorlds.contains(world.getName().toLowerCase(Locale.ROOT));
    }

    public Arena get(String name) {
        return name == null ? null : this.arenas.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String name) {
        return this.get(name) != null;
    }

    public Arena create(String name) {
        Arena arena = new Arena(name);
        this.arenas.put(name.toLowerCase(Locale.ROOT), arena);
        this.save();
        return arena;
    }

    public void delete(String name) {
        this.arenas.remove(name.toLowerCase(Locale.ROOT));
        this.snapshots.remove(name.toLowerCase(Locale.ROOT));
        this.snapshotFile(name).delete();
        this.save();
    }

    public List<Arena> all() {
        return new ArrayList<Arena>(this.arenas.values());
    }

    public Arena findFreeArena(Predicate<Arena> inUse) {
        for (Arena arena : this.arenas.values()) {
            if (!arena.isConfigured() || !arena.isEnabled() || inUse.test(arena)) continue;
            return arena;
        }
        return null;
    }

    public Arena findArenaAt(Location loc) {
        for (Arena arena : this.arenas.values()) {
            if (!arena.contains(loc)) continue;
            return arena;
        }
        return null;
    }

    public void restoreBlocks(Map<Location, BlockData> originals) {
        for (Map.Entry<Location, BlockData> entry : originals.entrySet()) {
            Location loc = entry.getKey();
            if (loc.getWorld() == null) continue;
            loc.getBlock().setBlockData(entry.getValue(), false);
        }
    }

    private File snapshotFile(String arenaName) {
        return new File(this.snapshotDir, arenaName.toLowerCase(Locale.ROOT) + ".snapshot.gz");
    }

    public boolean hasSnapshot(Arena arena) {
        return this.snapshots.containsKey(arena.getName().toLowerCase(Locale.ROOT)) || this.snapshotFile(arena.getName()).exists();
    }

    public boolean snapshotArena(Arena arena) {
        if (!arena.isConfigured() || arena.getWorld() == null) {
            return false;
        }
        RegionClipboard clip = RegionClipboard.capture(arena.getWorld(), arena.getMin(), arena.getMax());
        this.snapshots.put(arena.getName().toLowerCase(Locale.ROOT), clip);
        this.writeSnapshotToDisk(arena.getName(), clip);
        return true;
    }

    public int regenArena(Arena arena) {
        if (!arena.isConfigured() || arena.getWorld() == null) {
            return -1;
        }
        RegionClipboard clip = this.snapshots.get(arena.getName().toLowerCase(Locale.ROOT));
        if (clip == null && (clip = this.readSnapshotFromDisk(arena.getName())) != null) {
            this.snapshots.put(arena.getName().toLowerCase(Locale.ROOT), clip);
        }
        if (clip == null) {
            return -1;
        }
        return clip.pasteAt(arena.getWorld(), arena.getMin());
    }

    private void writeSnapshotToDisk(String arenaName, RegionClipboard clip) {
        File out = this.snapshotFile(arenaName);
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(out))));){
            w.write(clip.sizeX + " " + clip.sizeY + " " + clip.sizeZ);
            w.newLine();
            for (String s : clip.data) {
                w.write(s == null ? "" : s);
                w.newLine();
            }
        }
        catch (IOException e) {
            this.plugin.getLogger().severe("Failed to write snapshot for arena " + arenaName + ": " + e.getMessage());
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private RegionClipboard readSnapshotFromDisk(String arenaName) {
        File in = this.snapshotFile(arenaName);
        if (!in.exists()) {
            return null;
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(in))));){
            RegionClipboard regionClipboard;
            String header = r.readLine();
            if (header == null) {
                RegionClipboard regionClipboard2;
                RegionClipboard regionClipboard3 = regionClipboard2 = null;
                return regionClipboard3;
            }
            String[] parts = header.split(" ");
            int sx = Integer.parseInt(parts[0]);
            int sy = Integer.parseInt(parts[1]);
            int sz = Integer.parseInt(parts[2]);
            String[] data = new String[sx * sy * sz];
            for (int i = 0; i < data.length; ++i) {
                String line = r.readLine();
                data[i] = line == null || line.isEmpty() ? null : line;
            }
            RegionClipboard regionClipboard4 = regionClipboard = new RegionClipboard(sx, sy, sz, data);
            return regionClipboard4;
        }
        catch (IOException e) {
            this.plugin.getLogger().severe("Failed to read snapshot for arena " + arenaName + ": " + e.getMessage());
            return null;
        }
    }

    public boolean copyToClipboard(UUID player, Arena arena) {
        if (!arena.isConfigured()) {
            return false;
        }
        this.clipboards.put(player, RegionClipboard.captureArena(arena));
        return true;
    }

    public boolean hasClipboard(UUID player) {
        return this.clipboards.containsKey(player);
    }

    public PasteResult pasteAsNewArena(UUID player, Location target) {
        RegionClipboard clip = this.clipboards.get(player);
        if (clip == null) {
            return null;
        }
        String newName = this.nextCloneName(clip.sourceName);
        World world = target.getWorld();
        int baseY = target.getBlockY();
        try {
            int top = world.getMaxHeight() - 1;
            int bottom = world.getMinHeight();
            if (baseY + clip.sizeY - 1 > top) {
                baseY = top - (clip.sizeY - 1);
            }
            if (baseY < bottom) {
                baseY = bottom;
            }
        }
        catch (Throwable top) {
            // empty catch block
        }
        Location dest = new Location(world, (double)target.getBlockX(), (double)baseY, (double)target.getBlockZ());
        int written = clip.pasteAt(world, dest, true);
        Arena source = this.get(clip.sourceName);
        Arena clone = new Arena(newName);
        int dx = dest.getBlockX() - clip.originX;
        int dy = dest.getBlockY() - clip.originY;
        int dz = dest.getBlockZ() - clip.originZ;
        clone.setCorner1(ArenaManager.offset(clip.corner1, target.getWorld(), dx, dy, dz));
        clone.setCorner2(ArenaManager.offset(clip.corner2, target.getWorld(), dx, dy, dz));
        clone.setSpawn1(ArenaManager.offsetWithRotation(clip.spawn1, target.getWorld(), dx, dy, dz));
        clone.setSpawn2(ArenaManager.offsetWithRotation(clip.spawn2, target.getWorld(), dx, dy, dz));
        if (clip.eventSpawn != null) {
            clone.setEventSpawn(ArenaManager.offsetWithRotation(clip.eventSpawn, target.getWorld(), dx, dy, dz));
        }
        if (clip.borderCorner1 != null) {
            clone.setBorderCorner1(ArenaManager.offset(clip.borderCorner1, target.getWorld(), dx, dy, dz));
        }
        if (clip.borderCorner2 != null) {
            clone.setBorderCorner2(ArenaManager.offset(clip.borderCorner2, target.getWorld(), dx, dy, dz));
        }
        if (source != null) {
            clone.copySettingsFrom(source);
        }
        this.arenas.put(newName.toLowerCase(Locale.ROOT), clone);
        this.snapshotArena(clone);
        this.save();
        return new PasteResult(newName, written);
    }

    private static Location offset(Location base, World world, int dx, int dy, int dz) {
        return new Location(world, (double)(base.getBlockX() + dx), (double)(base.getBlockY() + dy), (double)(base.getBlockZ() + dz));
    }

    private static Location offsetWithRotation(Location base, World world, int dx, int dy, int dz) {
        Location loc = new Location(world, (double)(base.getBlockX() + dx) + 0.5, (double)(base.getBlockY() + dy), (double)(base.getBlockZ() + dz) + 0.5);
        loc.setYaw(base.getYaw());
        loc.setPitch(base.getPitch());
        return loc;
    }

    private String nextCloneName(String sourceName) {
        String base = sourceName;
        Matcher m = TRAILING_NUMBER.matcher(sourceName);
        if (m.matches() && !m.group(1).isEmpty()) {
            base = m.group(1);
        }
        int n = 2;
        while (this.exists(base + n)) {
            ++n;
        }
        return base + n;
    }

    public static final class RegionClipboard {
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;
        private final String[] data;
        private String sourceName;
        private int originX;
        private int originY;
        private int originZ;
        private Location corner1;
        private Location corner2;
        private Location spawn1;
        private Location spawn2;
        private Location eventSpawn;
        private Location borderCorner1;
        private Location borderCorner2;

        private RegionClipboard(int sizeX, int sizeY, int sizeZ, String[] data) {
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.data = data;
        }

        static RegionClipboard capture(World world, Location min, Location max) {
            int sx = max.getBlockX() - min.getBlockX() + 1;
            int sy = max.getBlockY() - min.getBlockY() + 1;
            int sz = max.getBlockZ() - min.getBlockZ() + 1;
            String[] data = new String[sx * sy * sz];
            int i = 0;
            for (int y = 0; y < sy; ++y) {
                for (int x = 0; x < sx; ++x) {
                    for (int z = 0; z < sz; ++z) {
                        Block b = world.getBlockAt(min.getBlockX() + x, min.getBlockY() + y, min.getBlockZ() + z);
                        data[i++] = b.getBlockData().getAsString();
                    }
                }
            }
            return new RegionClipboard(sx, sy, sz, data);
        }

        static RegionClipboard captureArena(Arena arena) {
            RegionClipboard clip = RegionClipboard.capture(arena.getWorld(), arena.getMin(), arena.getMax());
            clip.sourceName = arena.getName();
            Location min = arena.getMin();
            clip.originX = min.getBlockX();
            clip.originY = min.getBlockY();
            clip.originZ = min.getBlockZ();
            clip.corner1 = arena.getCorner1().clone();
            clip.corner2 = arena.getCorner2().clone();
            clip.spawn1 = arena.getSpawn1().clone();
            clip.spawn2 = arena.getSpawn2().clone();
            clip.eventSpawn = arena.getEventSpawn() == null ? null : arena.getEventSpawn().clone();
            clip.borderCorner1 = arena.getBorderCorner1() == null ? null : arena.getBorderCorner1().clone();
            clip.borderCorner2 = arena.getBorderCorner2() == null ? null : arena.getBorderCorner2().clone();
            return clip;
        }

        int pasteAt(World world, Location target) {
            return this.pasteAt(world, target, false);
        }

        int pasteAt(World world, Location target, boolean skipCompare) {
            int baseX = target.getBlockX();
            int baseY = target.getBlockY();
            int baseZ = target.getBlockZ();
            int written = 0;
            int i = 0;
            HashMap<String, BlockData> cache = new HashMap<String, BlockData>();
            for (int y = 0; y < this.sizeY; ++y) {
                for (int x = 0; x < this.sizeX; ++x) {
                    for (int z = 0; z < this.sizeZ; ++z) {
                        String str;
                        if ((str = this.data[i++]) == null) continue;
                        Block block = world.getBlockAt(baseX + x, baseY + y, baseZ + z);
                        if (!skipCompare && str.equals(block.getBlockData().getAsString())) continue;
                        BlockData bd = (BlockData)cache.get(str);
                        if (bd == null) {
                            try {
                                bd = Bukkit.createBlockData((String)str);
                            }
                            catch (IllegalArgumentException ex) {
                                bd = Material.AIR.createBlockData();
                            }
                            cache.put(str, bd);
                        }
                        block.setBlockData(bd, false);
                        ++written;
                    }
                }
            }
            return written;
        }
    }

    public static final class PasteResult {
        public final String arenaName;
        public final int blocksWritten;

        PasteResult(String arenaName, int blocksWritten) {
            this.arenaName = arenaName;
            this.blocksWritten = blocksWritten;
        }
    }
}


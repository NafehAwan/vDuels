package com.vduels.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A duel arena: two spawns, a bounding region (two corners) and a bag of
 * behaviour settings. Everything is GUI-configurable; this class only knows how
 * to hold the data and read/write itself to a YAML section.
 */
public class Arena {

    private final String name;

    private String worldName;
    private Location spawn1;
    private Location spawn2;
    private Location corner1; // stored as-is, min/max computed on demand
    private Location corner2;

    // Settings (all GUI toggleable)
    private boolean autoRegenerate = true;
    private boolean allowBreak = false;
    private boolean allowPlace = false;
    private boolean allowRemoveAdded = true; // may break blocks you placed this round
    private boolean duplicatorEnabled = false;

    private final Set<String> kits = new LinkedHashSet<>();

    public Arena(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** An arena is only playable once both spawns and both corners are set. */
    public boolean isConfigured() {
        return worldName != null && spawn1 != null && spawn2 != null
                && corner1 != null && corner2 != null;
    }

    public World getWorld() {
        return worldName == null ? null : Bukkit.getWorld(worldName);
    }

    public String getWorldName() {
        return worldName;
    }

    public Location getSpawn1() {
        return spawn1;
    }

    public Location getSpawn2() {
        return spawn2;
    }

    public Location getCorner1() {
        return corner1;
    }

    public Location getCorner2() {
        return corner2;
    }

    public void setSpawn1(Location loc) {
        this.spawn1 = loc.clone();
        this.worldName = loc.getWorld().getName();
    }

    public void setSpawn2(Location loc) {
        this.spawn2 = loc.clone();
        this.worldName = loc.getWorld().getName();
    }

    public void setCorner1(Location loc) {
        this.corner1 = loc.clone();
        this.worldName = loc.getWorld().getName();
    }

    public void setCorner2(Location loc) {
        this.corner2 = loc.clone();
        this.worldName = loc.getWorld().getName();
    }

    public Location getMin() {
        if (corner1 == null || corner2 == null) {
            return null;
        }
        World w = getWorld();
        return new Location(w,
                Math.min(corner1.getBlockX(), corner2.getBlockX()),
                Math.min(corner1.getBlockY(), corner2.getBlockY()),
                Math.min(corner1.getBlockZ(), corner2.getBlockZ()));
    }

    public Location getMax() {
        if (corner1 == null || corner2 == null) {
            return null;
        }
        World w = getWorld();
        return new Location(w,
                Math.max(corner1.getBlockX(), corner2.getBlockX()),
                Math.max(corner1.getBlockY(), corner2.getBlockY()),
                Math.max(corner1.getBlockZ(), corner2.getBlockZ()));
    }

    /** True if the block coordinates fall inside this arena's region. */
    public boolean contains(Location loc) {
        if (!isConfigured() || loc.getWorld() == null
                || !loc.getWorld().getName().equals(worldName)) {
            return false;
        }
        Location min = getMin();
        Location max = getMax();
        return loc.getBlockX() >= min.getBlockX() && loc.getBlockX() <= max.getBlockX()
                && loc.getBlockY() >= min.getBlockY() && loc.getBlockY() <= max.getBlockY()
                && loc.getBlockZ() >= min.getBlockZ() && loc.getBlockZ() <= max.getBlockZ();
    }

    // --- settings ---------------------------------------------------------

    public boolean isAutoRegenerate() {
        return autoRegenerate;
    }

    public void setAutoRegenerate(boolean v) {
        this.autoRegenerate = v;
    }

    public boolean isAllowBreak() {
        return allowBreak;
    }

    public void setAllowBreak(boolean v) {
        this.allowBreak = v;
    }

    public boolean isAllowPlace() {
        return allowPlace;
    }

    public void setAllowPlace(boolean v) {
        this.allowPlace = v;
    }

    public boolean isAllowRemoveAdded() {
        return allowRemoveAdded;
    }

    public void setAllowRemoveAdded(boolean v) {
        this.allowRemoveAdded = v;
    }

    public boolean isDuplicatorEnabled() {
        return duplicatorEnabled;
    }

    public void setDuplicatorEnabled(boolean v) {
        this.duplicatorEnabled = v;
    }

    public Set<String> getKits() {
        return kits;
    }

    public boolean supportsKit(String kitName) {
        // No restriction configured means the arena accepts every kit.
        if (kits.isEmpty()) {
            return true;
        }
        return supportsKitExplicit(kitName);
    }

    /** True only if the kit is explicitly in this arena's allow-list. */
    public boolean supportsKitExplicit(String kitName) {
        return kits.stream().anyMatch(k -> k.equalsIgnoreCase(kitName));
    }

    // --- persistence ------------------------------------------------------

    public void save(ConfigurationSection section) {
        section.set("world", worldName);
        writeLoc(section, "spawn1", spawn1, true);
        writeLoc(section, "spawn2", spawn2, true);
        writeLoc(section, "corner1", corner1, false);
        writeLoc(section, "corner2", corner2, false);
        section.set("settings.auto-regenerate", autoRegenerate);
        section.set("settings.allow-break", allowBreak);
        section.set("settings.allow-place", allowPlace);
        section.set("settings.allow-remove-added", allowRemoveAdded);
        section.set("settings.duplicator", duplicatorEnabled);
        section.set("kits", new java.util.ArrayList<>(kits));
    }

    public static Arena load(String name, ConfigurationSection section) {
        Arena arena = new Arena(name);
        arena.worldName = section.getString("world");
        arena.spawn1 = readLoc(section, "spawn1", arena.worldName, true);
        arena.spawn2 = readLoc(section, "spawn2", arena.worldName, true);
        arena.corner1 = readLoc(section, "corner1", arena.worldName, false);
        arena.corner2 = readLoc(section, "corner2", arena.worldName, false);
        arena.autoRegenerate = section.getBoolean("settings.auto-regenerate", true);
        arena.allowBreak = section.getBoolean("settings.allow-break", false);
        arena.allowPlace = section.getBoolean("settings.allow-place", false);
        arena.allowRemoveAdded = section.getBoolean("settings.allow-remove-added", true);
        arena.duplicatorEnabled = section.getBoolean("settings.duplicator", false);
        arena.kits.addAll(section.getStringList("kits"));
        return arena;
    }

    private static void writeLoc(ConfigurationSection section, String path, Location loc, boolean withRotation) {
        if (loc == null) {
            section.set(path, null);
            return;
        }
        section.set(path + ".x", withRotation ? loc.getX() : loc.getBlockX());
        section.set(path + ".y", withRotation ? loc.getY() : loc.getBlockY());
        section.set(path + ".z", withRotation ? loc.getZ() : loc.getBlockZ());
        if (withRotation) {
            section.set(path + ".yaw", loc.getYaw());
            section.set(path + ".pitch", loc.getPitch());
        }
    }

    private static Location readLoc(ConfigurationSection section, String path, String world, boolean withRotation) {
        if (!section.isConfigurationSection(path)) {
            return null;
        }
        World w = world == null ? null : Bukkit.getWorld(world);
        Location loc = new Location(w,
                section.getDouble(path + ".x"),
                section.getDouble(path + ".y"),
                section.getDouble(path + ".z"));
        if (withRotation) {
            loc.setYaw((float) section.getDouble(path + ".yaw"));
            loc.setPitch((float) section.getDouble(path + ".pitch"));
        }
        return loc;
    }
}

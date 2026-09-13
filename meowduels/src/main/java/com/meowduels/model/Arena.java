/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.configuration.ConfigurationSection
 */
package com.meowduels.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class Arena {
    private final String name;
    private String worldName;
    private Location spawn1;
    private Location spawn2;
    private Location corner1;
    private Location corner2;
    private Location eventSpawn;
    private Location borderCorner1;
    private Location borderCorner2;
    private int eventBorderEnd = 30;
    private int eventBorderInterval = 120;
    private int eventBorderStep = 20;
    private boolean autoRegenerate = true;
    private int regenDelayTicks = 0;
    private boolean allowBuild = false;
    private boolean allowBreak = false;
    private boolean allowBreakInner = false;
    private boolean allowBreakWalls = false;
    private boolean allowBreakRoof = false;
    private boolean allowPlaceInner = false;
    private boolean allowPlaceWalls = false;
    private boolean allowPlaceRoof = false;
    private boolean allowRemoveAdded = true;
    private int roofMargin = 1;
    private int wallMargin = 0;
    private boolean allowExplosions = false;
    private boolean allowFireSpread = false;
    private boolean allowLiquidFlow = false;
    private boolean allowMobSpawns = false;
    private boolean allowFallDamage = true;
    private boolean allowHungerLoss = false;
    private boolean lockClearWeather = false;
    private boolean lockDayTime = false;
    private boolean voidKillEnabled = false;
    private int voidKillY = -64;
    private boolean borderParticles = false;
    private boolean glowingOpponent = false;
    private boolean enabled = true;
    private boolean cloningEnabled = true;
    private boolean allowSpectators = true;
    private int countdownOverride = -1;
    private String description = "";
    private final Set<String> kits = new LinkedHashSet<String>();

    public Arena(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean isConfigured() {
        return this.worldName != null && this.spawn1 != null && this.spawn2 != null && this.corner1 != null && this.corner2 != null;
    }

    public World getWorld() {
        return this.worldName == null ? null : Bukkit.getWorld((String)this.worldName);
    }

    public String getWorldName() {
        return this.worldName;
    }

    public Location getSpawn1() {
        return this.spawn1;
    }

    public Location getSpawn2() {
        return this.spawn2;
    }

    public Location getEventSpawn() {
        return this.eventSpawn;
    }

    public void setEventSpawn(Location loc) {
        this.eventSpawn = loc == null ? null : loc.clone();
    }

    public boolean isEventReady() {
        return this.isConfigured() && this.eventSpawn != null;
    }

    public Location getBorderCorner1() {
        return this.borderCorner1;
    }

    public Location getBorderCorner2() {
        return this.borderCorner2;
    }

    public void setBorderCorner1(Location loc) {
        this.borderCorner1 = loc == null ? null : loc.clone();
    }

    public void setBorderCorner2(Location loc) {
        this.borderCorner2 = loc == null ? null : loc.clone();
    }

    public boolean hasBorderRegion() {
        return this.borderCorner1 != null && this.borderCorner2 != null;
    }

    public double getBorderCenterX() {
        if (this.hasBorderRegion()) {
            return (double)(this.borderCorner1.getBlockX() + this.borderCorner2.getBlockX()) / 2.0 + 0.5;
        }
        Location min = this.getMin();
        Location max = this.getMax();
        return (double)(min.getBlockX() + max.getBlockX()) / 2.0 + 0.5;
    }

    public double getBorderCenterZ() {
        if (this.hasBorderRegion()) {
            return (double)(this.borderCorner1.getBlockZ() + this.borderCorner2.getBlockZ()) / 2.0 + 0.5;
        }
        Location min = this.getMin();
        Location max = this.getMax();
        return (double)(min.getBlockZ() + max.getBlockZ()) / 2.0 + 0.5;
    }

    public double getBorderStartSize() {
        if (this.hasBorderRegion()) {
            double dx = Math.abs(this.borderCorner1.getBlockX() - this.borderCorner2.getBlockX()) + 1;
            double dz = Math.abs(this.borderCorner1.getBlockZ() - this.borderCorner2.getBlockZ()) + 1;
            return Math.max(dx, dz);
        }
        Location min = this.getMin();
        Location max = this.getMax();
        double dx = Math.abs(max.getBlockX() - min.getBlockX()) + 1;
        double dz = Math.abs(max.getBlockZ() - min.getBlockZ()) + 1;
        return Math.max(dx, dz);
    }

    public int getEventBorderEnd() {
        return this.eventBorderEnd;
    }

    public void setEventBorderEnd(int v) {
        this.eventBorderEnd = Math.max(1, v);
    }

    public int getEventBorderInterval() {
        return this.eventBorderInterval;
    }

    public void setEventBorderInterval(int v) {
        this.eventBorderInterval = Math.max(5, v);
    }

    public int getEventBorderStep() {
        return this.eventBorderStep;
    }

    public void setEventBorderStep(int v) {
        this.eventBorderStep = Math.max(1, v);
    }

    public boolean overlaps(Arena other) {
        if (other == null || other == this || this.worldName == null || !this.worldName.equals(other.worldName)) {
            return false;
        }
        Location aMin = this.getMin();
        Location aMax = this.getMax();
        Location bMin = other.getMin();
        Location bMax = other.getMax();
        if (aMin == null || aMax == null || bMin == null || bMax == null) {
            return false;
        }
        return aMin.getBlockX() < bMax.getBlockX() && aMax.getBlockX() > bMin.getBlockX() && aMin.getBlockY() < bMax.getBlockY() && aMax.getBlockY() > bMin.getBlockY() && aMin.getBlockZ() < bMax.getBlockZ() && aMax.getBlockZ() > bMin.getBlockZ();
    }

    public Location getCorner1() {
        return this.corner1;
    }

    public Location getCorner2() {
        return this.corner2;
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
        if (this.corner1 == null || this.corner2 == null) {
            return null;
        }
        World w = this.getWorld();
        return new Location(w, (double)Math.min(this.corner1.getBlockX(), this.corner2.getBlockX()), (double)Math.min(this.corner1.getBlockY(), this.corner2.getBlockY()), (double)Math.min(this.corner1.getBlockZ(), this.corner2.getBlockZ()));
    }

    public Location getMax() {
        if (this.corner1 == null || this.corner2 == null) {
            return null;
        }
        World w = this.getWorld();
        return new Location(w, (double)Math.max(this.corner1.getBlockX(), this.corner2.getBlockX()), (double)Math.max(this.corner1.getBlockY(), this.corner2.getBlockY()), (double)Math.max(this.corner1.getBlockZ(), this.corner2.getBlockZ()));
    }

    public boolean contains(Location loc) {
        if (!this.isConfigured() || loc.getWorld() == null || !loc.getWorld().getName().equals(this.worldName)) {
            return false;
        }
        Location min = this.getMin();
        Location max = this.getMax();
        return loc.getBlockX() >= min.getBlockX() && loc.getBlockX() <= max.getBlockX() && loc.getBlockY() >= min.getBlockY() && loc.getBlockY() <= max.getBlockY() && loc.getBlockZ() >= min.getBlockZ() && loc.getBlockZ() <= max.getBlockZ();
    }

    public Zone classify(Location loc) {
        Location min = this.getMin();
        Location max = this.getMax();
        if (min == null || max == null) {
            return Zone.INNER;
        }
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        int roofFrom = max.getBlockY() - Math.max(0, this.roofMargin) + 1;
        if (y >= roofFrom) {
            return Zone.ROOF;
        }
        int margin = Math.max(0, this.wallMargin);
        boolean onWall = x <= min.getBlockX() + margin || x >= max.getBlockX() - margin || z <= min.getBlockZ() + margin || z >= max.getBlockZ() - margin;
        boolean bl = onWall;
        if (onWall) {
            return Zone.WALL;
        }
        return Zone.INNER;
    }

    public boolean canBreak(Location loc) {
        return this.allowBreak;
    }

    public boolean canPlace(Location loc) {
        return this.allowBuild;
    }

    public boolean isAllowBuild() {
        return this.allowBuild;
    }

    public void setAllowBuild(boolean v) {
        this.allowBuild = v;
    }

    public boolean isAllowBreak() {
        return this.allowBreak;
    }

    public void setAllowBreak(boolean v) {
        this.allowBreak = v;
    }

    public void copySettingsFrom(Arena other) {
        this.autoRegenerate = other.autoRegenerate;
        this.regenDelayTicks = other.regenDelayTicks;
        this.eventBorderEnd = other.eventBorderEnd;
        this.eventBorderInterval = other.eventBorderInterval;
        this.eventBorderStep = other.eventBorderStep;
        this.allowBuild = other.allowBuild;
        this.allowBreak = other.allowBreak;
        this.allowBreakInner = other.allowBreakInner;
        this.allowBreakWalls = other.allowBreakWalls;
        this.allowBreakRoof = other.allowBreakRoof;
        this.allowPlaceInner = other.allowPlaceInner;
        this.allowPlaceWalls = other.allowPlaceWalls;
        this.allowPlaceRoof = other.allowPlaceRoof;
        this.allowRemoveAdded = other.allowRemoveAdded;
        this.roofMargin = other.roofMargin;
        this.wallMargin = other.wallMargin;
        this.allowExplosions = other.allowExplosions;
        this.allowFireSpread = other.allowFireSpread;
        this.allowLiquidFlow = other.allowLiquidFlow;
        this.allowMobSpawns = other.allowMobSpawns;
        this.allowFallDamage = other.allowFallDamage;
        this.allowHungerLoss = other.allowHungerLoss;
        this.lockClearWeather = other.lockClearWeather;
        this.lockDayTime = other.lockDayTime;
        this.voidKillEnabled = other.voidKillEnabled;
        this.voidKillY = other.voidKillY;
        this.borderParticles = other.borderParticles;
        this.glowingOpponent = other.glowingOpponent;
        this.enabled = other.enabled;
        this.cloningEnabled = other.cloningEnabled;
        this.allowSpectators = other.allowSpectators;
        this.countdownOverride = other.countdownOverride;
        this.description = other.description;
        this.kits.clear();
        this.kits.addAll(other.kits);
    }

    public boolean isAutoRegenerate() {
        return this.autoRegenerate;
    }

    public void setAutoRegenerate(boolean v) {
        this.autoRegenerate = v;
    }

    public int getRegenDelayTicks() {
        return this.regenDelayTicks;
    }

    public void setRegenDelayTicks(int v) {
        this.regenDelayTicks = Math.max(0, v);
    }

    public boolean isAllowBreakInner() {
        return this.allowBreakInner;
    }

    public void setAllowBreakInner(boolean v) {
        this.allowBreakInner = v;
    }

    public boolean isAllowBreakWalls() {
        return this.allowBreakWalls;
    }

    public void setAllowBreakWalls(boolean v) {
        this.allowBreakWalls = v;
    }

    public boolean isAllowBreakRoof() {
        return this.allowBreakRoof;
    }

    public void setAllowBreakRoof(boolean v) {
        this.allowBreakRoof = v;
    }

    public boolean isAllowPlaceInner() {
        return this.allowPlaceInner;
    }

    public void setAllowPlaceInner(boolean v) {
        this.allowPlaceInner = v;
    }

    public boolean isAllowPlaceWalls() {
        return this.allowPlaceWalls;
    }

    public void setAllowPlaceWalls(boolean v) {
        this.allowPlaceWalls = v;
    }

    public boolean isAllowPlaceRoof() {
        return this.allowPlaceRoof;
    }

    public void setAllowPlaceRoof(boolean v) {
        this.allowPlaceRoof = v;
    }

    public boolean isAllowRemoveAdded() {
        return this.allowRemoveAdded;
    }

    public void setAllowRemoveAdded(boolean v) {
        this.allowRemoveAdded = v;
    }

    public int getRoofMargin() {
        return this.roofMargin;
    }

    public void setRoofMargin(int v) {
        this.roofMargin = Math.max(0, v);
    }

    public int getWallMargin() {
        return this.wallMargin;
    }

    public void setWallMargin(int v) {
        this.wallMargin = Math.max(0, v);
    }

    public boolean isAllowExplosions() {
        return this.allowExplosions;
    }

    public void setAllowExplosions(boolean v) {
        this.allowExplosions = v;
    }

    public boolean isAllowFireSpread() {
        return this.allowFireSpread;
    }

    public void setAllowFireSpread(boolean v) {
        this.allowFireSpread = v;
    }

    public boolean isAllowLiquidFlow() {
        return this.allowLiquidFlow;
    }

    public void setAllowLiquidFlow(boolean v) {
        this.allowLiquidFlow = v;
    }

    public boolean isAllowMobSpawns() {
        return this.allowMobSpawns;
    }

    public void setAllowMobSpawns(boolean v) {
        this.allowMobSpawns = v;
    }

    public boolean isAllowFallDamage() {
        return this.allowFallDamage;
    }

    public void setAllowFallDamage(boolean v) {
        this.allowFallDamage = v;
    }

    public boolean isAllowHungerLoss() {
        return this.allowHungerLoss;
    }

    public void setAllowHungerLoss(boolean v) {
        this.allowHungerLoss = v;
    }

    public boolean isLockClearWeather() {
        return this.lockClearWeather;
    }

    public void setLockClearWeather(boolean v) {
        this.lockClearWeather = v;
    }

    public boolean isLockDayTime() {
        return this.lockDayTime;
    }

    public void setLockDayTime(boolean v) {
        this.lockDayTime = v;
    }

    public boolean isVoidKillEnabled() {
        return this.voidKillEnabled;
    }

    public void setVoidKillEnabled(boolean v) {
        this.voidKillEnabled = v;
    }

    public int getVoidKillY() {
        return this.voidKillY;
    }

    public void setVoidKillY(int v) {
        this.voidKillY = v;
    }

    public boolean isBorderParticles() {
        return this.borderParticles;
    }

    public void setBorderParticles(boolean v) {
        this.borderParticles = v;
    }

    public boolean isGlowingOpponent() {
        return this.glowingOpponent;
    }

    public void setGlowingOpponent(boolean v) {
        this.glowingOpponent = v;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean v) {
        this.enabled = v;
    }

    public boolean isCloningEnabled() {
        return this.cloningEnabled;
    }

    public void setCloningEnabled(boolean v) {
        this.cloningEnabled = v;
    }

    public boolean isAllowSpectators() {
        return this.allowSpectators;
    }

    public void setAllowSpectators(boolean v) {
        this.allowSpectators = v;
    }

    public int getCountdownOverride() {
        return this.countdownOverride;
    }

    public void setCountdownOverride(int v) {
        this.countdownOverride = v;
    }

    public String getDescription() {
        return this.description == null ? "" : this.description;
    }

    public void setDescription(String v) {
        this.description = v;
    }

    public Set<String> getKits() {
        return this.kits;
    }

    public boolean supportsKit(String kitName) {
        if (this.kits.isEmpty()) {
            return true;
        }
        return this.supportsKitExplicit(kitName);
    }

    public boolean supportsKitExplicit(String kitName) {
        return this.kits.stream().anyMatch(k -> k.equalsIgnoreCase(kitName));
    }

    public void save(ConfigurationSection section) {
        section.set("world", (Object)this.worldName);
        Arena.writeLoc(section, "spawn1", this.spawn1, true);
        Arena.writeLoc(section, "spawn2", this.spawn2, true);
        Arena.writeLoc(section, "corner1", this.corner1, false);
        Arena.writeLoc(section, "corner2", this.corner2, false);
        Arena.writeLoc(section, "event-spawn", this.eventSpawn, true);
        Arena.writeLoc(section, "border-corner1", this.borderCorner1, false);
        Arena.writeLoc(section, "border-corner2", this.borderCorner2, false);
        section.set("settings.event-border-end", (Object)this.eventBorderEnd);
        section.set("settings.event-border-interval", (Object)this.eventBorderInterval);
        section.set("settings.event-border-step", (Object)this.eventBorderStep);
        section.set("settings.auto-regenerate", (Object)this.autoRegenerate);
        section.set("settings.regen-delay-ticks", (Object)this.regenDelayTicks);
        section.set("settings.allow-build", (Object)this.allowBuild);
        section.set("settings.allow-break-all", (Object)this.allowBreak);
        section.set("settings.allow-break-inner", (Object)this.allowBreakInner);
        section.set("settings.allow-break-walls", (Object)this.allowBreakWalls);
        section.set("settings.allow-break-roof", (Object)this.allowBreakRoof);
        section.set("settings.allow-place-inner", (Object)this.allowPlaceInner);
        section.set("settings.allow-place-walls", (Object)this.allowPlaceWalls);
        section.set("settings.allow-place-roof", (Object)this.allowPlaceRoof);
        section.set("settings.allow-remove-added", (Object)this.allowRemoveAdded);
        section.set("settings.roof-margin", (Object)this.roofMargin);
        section.set("settings.wall-margin", (Object)this.wallMargin);
        section.set("settings.allow-explosions", (Object)this.allowExplosions);
        section.set("settings.allow-fire-spread", (Object)this.allowFireSpread);
        section.set("settings.allow-liquid-flow", (Object)this.allowLiquidFlow);
        section.set("settings.allow-mob-spawns", (Object)this.allowMobSpawns);
        section.set("settings.allow-fall-damage", (Object)this.allowFallDamage);
        section.set("settings.allow-hunger-loss", (Object)this.allowHungerLoss);
        section.set("settings.lock-clear-weather", (Object)this.lockClearWeather);
        section.set("settings.lock-day-time", (Object)this.lockDayTime);
        section.set("settings.void-kill-enabled", (Object)this.voidKillEnabled);
        section.set("settings.void-kill-y", (Object)this.voidKillY);
        section.set("settings.border-particles", (Object)this.borderParticles);
        section.set("settings.glowing-opponent", (Object)this.glowingOpponent);
        section.set("settings.enabled", (Object)this.enabled);
        section.set("settings.cloning-enabled", (Object)this.cloningEnabled);
        section.set("settings.allow-spectators", (Object)this.allowSpectators);
        section.set("settings.countdown-override", (Object)this.countdownOverride);
        section.set("settings.description", (Object)this.description);
        section.set("kits", new ArrayList<String>(this.kits));
    }

    public static Arena load(String name, ConfigurationSection section) {
        Arena arena = new Arena(name);
        arena.worldName = section.getString("world");
        arena.spawn1 = Arena.readLoc(section, "spawn1", arena.worldName, true);
        arena.spawn2 = Arena.readLoc(section, "spawn2", arena.worldName, true);
        arena.corner1 = Arena.readLoc(section, "corner1", arena.worldName, false);
        arena.corner2 = Arena.readLoc(section, "corner2", arena.worldName, false);
        arena.eventSpawn = Arena.readLoc(section, "event-spawn", arena.worldName, true);
        arena.borderCorner1 = Arena.readLoc(section, "border-corner1", arena.worldName, false);
        arena.borderCorner2 = Arena.readLoc(section, "border-corner2", arena.worldName, false);
        arena.eventBorderEnd = section.getInt("settings.event-border-end", 30);
        arena.eventBorderInterval = section.getInt("settings.event-border-interval", 120);
        arena.eventBorderStep = section.getInt("settings.event-border-step", 20);
        arena.autoRegenerate = section.getBoolean("settings.auto-regenerate", true);
        arena.regenDelayTicks = section.getInt("settings.regen-delay-ticks", 0);
        boolean legacyPlace = section.getBoolean("settings.allow-place", false);
        arena.allowBreakInner = section.getBoolean("settings.allow-break-inner", false);
        arena.allowBreakWalls = section.getBoolean("settings.allow-break-walls", false);
        arena.allowBreakRoof = section.getBoolean("settings.allow-break-roof", false);
        arena.allowPlaceInner = section.getBoolean("settings.allow-place-inner", legacyPlace);
        arena.allowPlaceWalls = section.getBoolean("settings.allow-place-walls", false);
        arena.allowPlaceRoof = section.getBoolean("settings.allow-place-roof", false);
        arena.allowRemoveAdded = section.getBoolean("settings.allow-remove-added", true);
        boolean legacyAnyBreak = arena.allowBreakInner || arena.allowBreakWalls || arena.allowBreakRoof || section.getBoolean("settings.allow-break", false);
        boolean legacyAnyPlace = arena.allowPlaceInner || arena.allowPlaceWalls || arena.allowPlaceRoof || legacyPlace;
        arena.allowBreak = section.getBoolean("settings.allow-break-all", legacyAnyBreak);
        arena.allowBuild = section.getBoolean("settings.allow-build", legacyAnyPlace);
        arena.roofMargin = section.getInt("settings.roof-margin", 1);
        arena.wallMargin = section.getInt("settings.wall-margin", 0);
        arena.allowExplosions = section.getBoolean("settings.allow-explosions", false);
        arena.allowFireSpread = section.getBoolean("settings.allow-fire-spread", false);
        arena.allowLiquidFlow = section.getBoolean("settings.allow-liquid-flow", false);
        arena.allowMobSpawns = section.getBoolean("settings.allow-mob-spawns", false);
        arena.allowFallDamage = section.getBoolean("settings.allow-fall-damage", true);
        arena.allowHungerLoss = section.getBoolean("settings.allow-hunger-loss", false);
        arena.lockClearWeather = section.getBoolean("settings.lock-clear-weather", false);
        arena.lockDayTime = section.getBoolean("settings.lock-day-time", false);
        arena.voidKillEnabled = section.getBoolean("settings.void-kill-enabled", false);
        arena.voidKillY = section.getInt("settings.void-kill-y", -64);
        arena.borderParticles = section.getBoolean("settings.border-particles", false);
        arena.glowingOpponent = section.getBoolean("settings.glowing-opponent", false);
        arena.enabled = section.getBoolean("settings.enabled", true);
        boolean legacyDuplicator = section.getBoolean("settings.duplicator", true);
        arena.cloningEnabled = section.getBoolean("settings.cloning-enabled", legacyDuplicator);
        arena.allowSpectators = section.getBoolean("settings.allow-spectators", true);
        arena.countdownOverride = section.getInt("settings.countdown-override", -1);
        arena.description = section.getString("settings.description", "");
        arena.kits.addAll(section.getStringList("kits"));
        return arena;
    }

    private static void writeLoc(ConfigurationSection section, String path, Location loc, boolean withRotation) {
        if (loc == null) {
            section.set(path, null);
            return;
        }
        section.set(path + ".x", (Object)(withRotation ? loc.getX() : (double)loc.getBlockX()));
        section.set(path + ".y", (Object)(withRotation ? loc.getY() : (double)loc.getBlockY()));
        section.set(path + ".z", (Object)(withRotation ? loc.getZ() : (double)loc.getBlockZ()));
        if (withRotation) {
            section.set(path + ".yaw", (Object)Float.valueOf(loc.getYaw()));
            section.set(path + ".pitch", (Object)Float.valueOf(loc.getPitch()));
        }
    }

    private static Location readLoc(ConfigurationSection section, String path, String world, boolean withRotation) {
        if (!section.isConfigurationSection(path)) {
            return null;
        }
        World w = world == null ? null : Bukkit.getWorld((String)world);
        Location loc = new Location(w, section.getDouble(path + ".x"), section.getDouble(path + ".y"), section.getDouble(path + ".z"));
        if (withRotation) {
            loc.setYaw((float)section.getDouble(path + ".yaw"));
            loc.setPitch((float)section.getDouble(path + ".pitch"));
        }
        return loc;
    }

    public static enum Zone {
        ROOF,
        WALL,
        INNER;

    }
}


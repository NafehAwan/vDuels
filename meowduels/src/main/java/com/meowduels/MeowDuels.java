/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.NamespacedKey
 *  org.bukkit.World
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.meowduels;

import com.meowduels.commands.LeaderboardCommand;
import com.meowduels.commands.MeowDuelsCommand;
import com.meowduels.gui.QueuePickMenu;
import com.meowduels.hook.MeowDuelsPlaceholders;
import com.meowduels.hook.TabHook;
import com.meowduels.listeners.ArenaProtectionListener;
import com.meowduels.listeners.DuelListener;
import com.meowduels.listeners.EventListener;
import com.meowduels.listeners.GoldenHeadListener;
import com.meowduels.listeners.GuiListener;
import com.meowduels.listeners.SetupChatListener;
import com.meowduels.listeners.SpawnItemsListener;
import com.meowduels.managers.ArenaManager;
import com.meowduels.managers.CategoryManager;
import com.meowduels.managers.DuelManager;
import com.meowduels.managers.EventManager;
import com.meowduels.managers.GuiLayoutManager;
import com.meowduels.managers.KitManager;
import com.meowduels.managers.MessageManager;
import com.meowduels.managers.PlayerSettingsManager;
import com.meowduels.managers.PartyManager;
import com.meowduels.managers.QueueManager;
import com.meowduels.managers.ScoreboardService;
import com.meowduels.managers.SetupManager;
import com.meowduels.managers.SpectateManager;
import com.meowduels.managers.StatsManager;
import com.meowduels.managers.TabEditManager;
import com.meowduels.managers.TabService;
import com.meowduels.managers.KitLayoutManager;
import com.meowduels.managers.TrimPreferenceManager;
import com.meowduels.util.SpawnItems;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class MeowDuels
extends JavaPlugin {
    private ArenaManager arenaManager;
    private KitManager kitManager;
    private DuelManager duelManager;
    private SetupManager setupManager;
    private GuiLayoutManager guiLayoutManager;
    private ScoreboardService scoreboardService;
    private MessageManager messageManager;
    private CategoryManager categoryManager;
    private CategoryManager queueCategoryManager;
    private QueueManager queueManager;
    private TabService tabService;
    private TabEditManager tabEditManager;
    private SpectateManager spectateManager;
    private StatsManager statsManager;
    private EventManager eventManager;
    private TrimPreferenceManager trimPreferenceManager;
    private KitLayoutManager kitLayoutManager;
    private PlayerSettingsManager playerSettingsManager;
    private TabHook tabHook;
    private PartyManager partyManager;
    private NamespacedKey keyKit;
    private NamespacedKey keyButton;
    private NamespacedKey keyArena;
    private String scoreboardIp = "play.example.net";
    private String serverName = "server";
    private String tabTitle;
    private String tabDiscord;
    private String tabStore;
    private Location duelSpawn;

    public void onEnable() {
        this.saveDefaultConfig();
        this.mergeConfigDefaults();
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdirs();
        }
        this.keyKit = new NamespacedKey((Plugin)this, "kit");
        this.keyButton = new NamespacedKey((Plugin)this, "button");
        this.keyArena = new NamespacedKey((Plugin)this, "arena");
        this.scoreboardIp = this.getConfig().getString("scoreboard-ip", "play.example.net");
        this.serverName = this.getConfig().getString("server-name", "server");
        this.tabTitle = this.getConfig().getString("tab.title", "<gold><bold>ServerName</bold></gold>");
        this.tabDiscord = this.getConfig().getString("tab.discord", "<aqua>discord.example.net</aqua>");
        this.tabStore = this.getConfig().getString("tab.store", "<yellow>store.example.net</yellow>");
        this.loadDuelSpawn();
        this.messageManager = new MessageManager(this);
        this.arenaManager = new ArenaManager(this);
        // Restores any arena the last shutdown left mid-fight. Has to happen
        // before anything can claim one for a new match.
        this.arenaManager.recoverDirtyArenas();
        this.kitManager = new KitManager(this);
        this.categoryManager = new CategoryManager(this);
        this.queueCategoryManager = new CategoryManager(this, "queuecategories.yml");
        this.guiLayoutManager = new GuiLayoutManager(this);
        this.setupManager = new SetupManager(this);
        this.scoreboardService = new ScoreboardService(this);
        this.duelManager = new DuelManager(this);
        this.queueManager = new QueueManager(this);
        this.partyManager = new PartyManager(this);
        this.tabService = new TabService(this);
        this.tabEditManager = new TabEditManager(this);
        this.spectateManager = new SpectateManager(this);
        this.statsManager = new StatsManager(this);
        this.eventManager = new EventManager(this);
        this.trimPreferenceManager = new TrimPreferenceManager(this);
        this.kitLayoutManager = new KitLayoutManager(this);
        this.playerSettingsManager = new PlayerSettingsManager(this);
        this.tabHook = new TabHook();
        if (this.tabHook.isAvailable()) {
            this.getLogger().info("Hooked into TAB - ranks hidden and health shown on nametags during duels.");
        } else {
            this.getLogger().warning("TAB API not found - if you use TAB, the in-duel tab/nametag override won't work. Check the TAB version.");
        }
        this.registerCommands();
        this.registerListeners();
        this.hookPlaceholderAPI();
        this.getServer().getScheduler().runTaskTimer((Plugin)this, () -> {
            this.scoreboardService.tick();
            this.tabService.tick();
            this.spectateManager.tick();
            this.duelManager.tickWorldLocks();
            this.duelManager.tickArenaSafety();
            this.duelManager.tickCounts();
            this.duelManager.tickArenaReservations();
            this.queueManager.tickMatch();
            this.partyManager.tick();
            if (this.eventManager.isRunning() && this.eventManager.getArena() != null) {
                DuelManager.applyWorldLocks(this.eventManager.getArena());
                this.eventManager.tickBorderDamage();
            }
            QueuePickMenu.refreshAll();
        }, 20L, 20L);
        this.getServer().getScheduler().runTaskTimer((Plugin)this, () -> this.scoreboardService.updateDuelHealthTags(), 2L, 2L);
        this.getServer().getScheduler().runTaskTimer((Plugin)this, () -> this.scoreboardService.updateRankBelowName(), 20L, 20L);
        this.getLogger().info("MeowDuels enabled.");
    }

    public void onDisable() {
        if (this.eventManager != null) {
            this.eventManager.shutdown();
        }
        if (this.partyManager != null) {
            this.partyManager.shutdown();
        }
        if (this.duelManager != null) {
            this.duelManager.shutdown();
        }
        if (this.arenaManager != null) {
            this.arenaManager.save();
        }
        if (this.kitManager != null) {
            this.kitManager.save();
        }
        if (this.statsManager != null) {
            this.statsManager.save();
        }
    }

    private void registerCommands() {
        MeowDuelsCommand handler = new MeowDuelsCommand(this);
        PluginCommand leaderboard = this.getCommand("leaderboard");
        if (leaderboard != null) {
            leaderboard.setExecutor((CommandExecutor)new LeaderboardCommand(this));
        } else {
            this.getLogger().warning("Command 'leaderboard' is missing from plugin.yml.");
        }
        for (String name : new String[]{"meowduels", "createarena", "arena", "deletearena", "kitcreate", "deletekit", "kiticon", "kitdisplayname", "editgui", "category", "categoryqueue", "scoreboardip", "duel", "leave", "queue", "meowduelstab", "editkit", "spectate", "meowduelssetspawn", "kiteditor", "party", "givegoldenhead", "resetconfig", "meowduelsserver", "meowduelstrims", "ff", "event", "eventspec", "eventleave", "meowduelsspawnitems"}) {
            PluginCommand command = this.getCommand(name);
            if (command != null) {
                command.setExecutor((CommandExecutor)handler);
                command.setTabCompleter((TabCompleter)handler);
                continue;
            }
            this.getLogger().warning("Command '" + name + "' is missing from plugin.yml.");
        }
    }

    private void hookPlaceholderAPI() {
        if (this.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            new MeowDuelsPlaceholders(this).register();
            this.getLogger().info("Hooked into PlaceholderAPI - %meowduels_...% placeholders registered.");
        }
        catch (Throwable t) {
            this.getLogger().warning("Could not register PlaceholderAPI expansion: " + String.valueOf(t));
        }
    }

    private void mergeConfigDefaults() {
        InputStream in = this.getResource("config.yml");
        if (in == null) {
            return;
        }
        File file = new File(this.getDataFolder(), "config.yml");
        YamlConfiguration user = YamlConfiguration.loadConfiguration((File)file);
        YamlConfiguration defaults = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(in, StandardCharsets.UTF_8));
        boolean changed = false;
        for (String key : defaults.getKeys(true)) {
            if (user.contains(key)) continue;
            user.set(key, defaults.get(key));
            changed = true;
        }
        if (changed) {
            try {
                user.save(file);
                this.getLogger().info("config.yml updated with new options (existing settings kept).");
            }
            catch (IOException e) {
                this.getLogger().warning("Could not merge config.yml defaults: " + e.getMessage());
            }
        }
    }

    private void registerListeners() {
        this.getServer().getPluginManager().registerEvents((Listener)new GuiListener(), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new SetupChatListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new ArenaProtectionListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new DuelListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new EventListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new GoldenHeadListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new SpawnItemsListener(this), (Plugin)this);
    }

    public ArenaManager getArenaManager() {
        return this.arenaManager;
    }

    public KitManager getKitManager() {
        return this.kitManager;
    }

    public EventManager getEventManager() {
        return this.eventManager;
    }

    public KitLayoutManager getKitLayouts() {
        return this.kitLayoutManager;
    }

    public TrimPreferenceManager getTrimPreferences() {
        return this.trimPreferenceManager;
    }

    public PlayerSettingsManager getPlayerSettings() {
        return this.playerSettingsManager;
    }

    /** The marker shown after a fighter's name in the tab list. Configurable
     *  because the default is an emoji, which only renders with a resource pack
     *  that provides the glyph - "duel-marker" in config.yml takes any text. */
    public String getDuelMarker() {
        String marker = this.getConfig().getString("duel-marker", "\ud83d\udde1");
        return marker == null ? "" : marker;
    }

    public void giveSpawnItems(Player player) {
        SpawnItems.give(this, player);
    }

    public DuelManager getDuelManager() {
        return this.duelManager;
    }

    public SetupManager getSetupManager() {
        return this.setupManager;
    }

    public GuiLayoutManager getGuiLayoutManager() {
        return this.guiLayoutManager;
    }

    public ScoreboardService getScoreboardService() {
        return this.scoreboardService;
    }

    public MessageManager messages() {
        return this.messageManager;
    }

    public CategoryManager getCategoryManager() {
        return this.categoryManager;
    }

    public CategoryManager getQueueCategoryManager() {
        return this.queueCategoryManager;
    }

    public QueueManager getQueueManager() {
        return this.queueManager;
    }

    public TabService getTabService() {
        return this.tabService;
    }

    public TabEditManager getTabEditManager() {
        return this.tabEditManager;
    }

    public StatsManager getStatsManager() {
        return this.statsManager;
    }

    public TabHook getTabHook() {
        return this.tabHook;
    }

    public void reloadAll() {
        this.reloadConfig();
        this.mergeConfigDefaults();
        this.reloadConfig();
        this.scoreboardIp = this.getConfig().getString("scoreboard-ip", "play.example.net");
        this.serverName = this.getConfig().getString("server-name", "server");
        this.tabTitle = this.getConfig().getString("tab.title", "<gold><bold>ServerName</bold></gold>");
        this.tabDiscord = this.getConfig().getString("tab.discord", "<aqua>discord.example.net</aqua>");
        this.tabStore = this.getConfig().getString("tab.store", "<yellow>store.example.net</yellow>");
        this.loadDuelSpawn();
        if (this.messageManager != null) {
            this.messageManager.reload();
        }
        if (this.scoreboardService != null) {
            this.scoreboardService.reload();
        }
    }

    public SpectateManager getSpectateManager() {
        return this.spectateManager;
    }

    public String getTabTitle() {
        return this.tabTitle;
    }

    public String getTabDiscord() {
        return this.tabDiscord;
    }

    public String getTabStore() {
        return this.tabStore;
    }

    public void setTabTitle(String value) {
        this.tabTitle = value;
        this.getConfig().set("tab.title", (Object)value);
        this.saveConfig();
    }

    public void setTabDiscord(String value) {
        this.tabDiscord = value;
        this.getConfig().set("tab.discord", (Object)value);
        this.saveConfig();
    }

    public void setTabStore(String value) {
        this.tabStore = value;
        this.getConfig().set("tab.store", (Object)value);
        this.saveConfig();
    }

    public PartyManager getPartyManager() {
        return this.partyManager;
    }

    public NamespacedKey keyKit() {
        return this.keyKit;
    }

    public NamespacedKey keyButton() {
        return this.keyButton;
    }

    public NamespacedKey keyArena() {
        return this.keyArena;
    }

    public String getScoreboardIp() {
        return this.scoreboardIp;
    }

    public void setScoreboardIp(String ip) {
        this.scoreboardIp = ip;
        this.getConfig().set("scoreboard-ip", (Object)ip);
        this.saveConfig();
    }

    public String getServerName() {
        return this.serverName;
    }

    public void setServerName(String name) {
        this.serverName = name;
        this.getConfig().set("server-name", (Object)name);
        this.saveConfig();
    }

    public Location getDuelSpawn() {
        return this.duelSpawn;
    }

    public void setDuelSpawn(Location loc) {
        this.duelSpawn = loc.clone();
        this.getConfig().set("duel-spawn.world", (Object)loc.getWorld().getName());
        this.getConfig().set("duel-spawn.x", (Object)loc.getX());
        this.getConfig().set("duel-spawn.y", (Object)loc.getY());
        this.getConfig().set("duel-spawn.z", (Object)loc.getZ());
        this.getConfig().set("duel-spawn.yaw", (Object)Float.valueOf(loc.getYaw()));
        this.getConfig().set("duel-spawn.pitch", (Object)Float.valueOf(loc.getPitch()));
        this.saveConfig();
    }

    private void loadDuelSpawn() {
        if (!this.getConfig().isSet("duel-spawn.world")) {
            this.duelSpawn = null;
            return;
        }
        String worldName = this.getConfig().getString("duel-spawn.world");
        World world = worldName == null ? null : Bukkit.getWorld((String)worldName);
        World world2 = world;
        if (world == null) {
            this.duelSpawn = null;
            return;
        }
        double x = this.getConfig().getDouble("duel-spawn.x");
        double y = this.getConfig().getDouble("duel-spawn.y");
        double z = this.getConfig().getDouble("duel-spawn.z");
        float yaw = (float)this.getConfig().getDouble("duel-spawn.yaw");
        float pitch = (float)this.getConfig().getDouble("duel-spawn.pitch");
        this.duelSpawn = new Location(world, x, y, z, yaw, pitch);
    }
}


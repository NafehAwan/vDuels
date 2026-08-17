package com.vduels;

import com.vduels.commands.VDuelsCommand;
import com.vduels.listeners.ArenaProtectionListener;
import com.vduels.listeners.DuelListener;
import com.vduels.listeners.GuiListener;
import com.vduels.listeners.SetupChatListener;
import com.vduels.managers.ArenaManager;
import com.vduels.managers.DuelManager;
import com.vduels.managers.CategoryManager;
import com.vduels.managers.GuiLayoutManager;
import com.vduels.managers.KitManager;
import com.vduels.managers.MessageManager;
import com.vduels.managers.QueueManager;
import com.vduels.managers.ScoreboardService;
import com.vduels.managers.SetupManager;
import com.vduels.managers.SpectateManager;
import com.vduels.managers.TabEditManager;
import com.vduels.managers.TabService;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * vDuels entry point. Owns the managers, registers commands/listeners and
 * exposes the {@link NamespacedKey}s used to tag GUI items.
 */
public final class VDuels extends JavaPlugin {

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

    private NamespacedKey keyKit;
    private NamespacedKey keyButton;
    private NamespacedKey keyArena;
    private String scoreboardIp = "play.example.net";
    private String tabTitle;
    private String tabDiscord;
    private String tabStore;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        this.keyKit = new NamespacedKey(this, "kit");
        this.keyButton = new NamespacedKey(this, "button");
        this.keyArena = new NamespacedKey(this, "arena");
        this.scoreboardIp = getConfig().getString("scoreboard-ip", "play.example.net");
        this.tabTitle = getConfig().getString("tab.title", "<gold><bold>ServerName</bold></gold>");
        this.tabDiscord = getConfig().getString("tab.discord", "<aqua>discord.example.net</aqua>");
        this.tabStore = getConfig().getString("tab.store", "<yellow>store.example.net</yellow>");

        this.messageManager = new MessageManager(this);
        this.arenaManager = new ArenaManager(this);
        this.kitManager = new KitManager(this);
        this.categoryManager = new CategoryManager(this);
        this.queueCategoryManager = new CategoryManager(this, "queuecategories.yml");
        this.guiLayoutManager = new GuiLayoutManager(this);
        this.setupManager = new SetupManager(this);
        this.scoreboardService = new ScoreboardService(this);
        this.duelManager = new DuelManager(this);
        this.queueManager = new QueueManager(this);
        this.tabService = new TabService(this);
        this.tabEditManager = new TabEditManager(this);
        this.spectateManager = new SpectateManager(this);

        registerCommands();
        registerListeners();

        // Refresh in-duel scoreboards, tab counts and open queue menus per second.
        getServer().getScheduler().runTaskTimer(this, () -> {
            scoreboardService.tick();
            tabService.tick();
            spectateManager.tick();
            com.vduels.gui.QueuePickMenu.refreshAll();
        }, 20L, 20L);

        getLogger().info("vDuels enabled.");
    }

    @Override
    public void onDisable() {
        if (duelManager != null) {
            duelManager.shutdown();
        }
        if (arenaManager != null) {
            arenaManager.save();
        }
        if (kitManager != null) {
            kitManager.save();
        }
    }

    private void registerCommands() {
        VDuelsCommand handler = new VDuelsCommand(this);
        for (String name : new String[]{"vduels", "createarena", "arena", "deletearena",
                "kitcreate", "deletekit", "kiticon", "kitdisplayname", "editgui", "category",
                "categoryqueue", "scoreboardip", "duel", "leave", "queue", "vduelstab",
                "editkit", "changekit", "spectate"}) {
            PluginCommand command = getCommand(name);
            if (command != null) {
                command.setExecutor(handler);
                command.setTabCompleter(handler);
            } else {
                getLogger().warning("Command '" + name + "' is missing from plugin.yml.");
            }
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(new SetupChatListener(this), this);
        getServer().getPluginManager().registerEvents(new ArenaProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new DuelListener(this), this);
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }

    public SetupManager getSetupManager() {
        return setupManager;
    }

    public GuiLayoutManager getGuiLayoutManager() {
        return guiLayoutManager;
    }

    public ScoreboardService getScoreboardService() {
        return scoreboardService;
    }

    public MessageManager messages() {
        return messageManager;
    }

    public CategoryManager getCategoryManager() {
        return categoryManager;
    }

    public CategoryManager getQueueCategoryManager() {
        return queueCategoryManager;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public TabService getTabService() {
        return tabService;
    }

    public TabEditManager getTabEditManager() {
        return tabEditManager;
    }

    public SpectateManager getSpectateManager() {
        return spectateManager;
    }

    public String getTabTitle() {
        return tabTitle;
    }

    public String getTabDiscord() {
        return tabDiscord;
    }

    public String getTabStore() {
        return tabStore;
    }

    public void setTabTitle(String value) {
        this.tabTitle = value;
        getConfig().set("tab.title", value);
        saveConfig();
    }

    public void setTabDiscord(String value) {
        this.tabDiscord = value;
        getConfig().set("tab.discord", value);
        saveConfig();
    }

    public void setTabStore(String value) {
        this.tabStore = value;
        getConfig().set("tab.store", value);
        saveConfig();
    }

    public NamespacedKey keyKit() {
        return keyKit;
    }

    public NamespacedKey keyButton() {
        return keyButton;
    }

    public NamespacedKey keyArena() {
        return keyArena;
    }

    public String getScoreboardIp() {
        return scoreboardIp;
    }

    public void setScoreboardIp(String ip) {
        this.scoreboardIp = ip;
        getConfig().set("scoreboard-ip", ip);
        saveConfig();
    }
}

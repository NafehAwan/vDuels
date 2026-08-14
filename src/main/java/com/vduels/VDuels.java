package com.vduels;

import com.vduels.commands.VDuelsCommand;
import com.vduels.listeners.ArenaProtectionListener;
import com.vduels.listeners.DuelListener;
import com.vduels.listeners.GuiListener;
import com.vduels.listeners.SetupChatListener;
import com.vduels.managers.ArenaManager;
import com.vduels.managers.DuelManager;
import com.vduels.managers.KitManager;
import com.vduels.managers.MenuLayoutManager;
import com.vduels.managers.SetupManager;
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
    private MenuLayoutManager menuLayoutManager;

    private NamespacedKey keyKit;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        this.keyKit = new NamespacedKey(this, "kit");

        this.arenaManager = new ArenaManager(this);
        this.kitManager = new KitManager(this);
        this.menuLayoutManager = new MenuLayoutManager(this);
        this.setupManager = new SetupManager(this);
        this.duelManager = new DuelManager(this);

        registerCommands();
        registerListeners();

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
                "kitcreate", "deletekit", "adminduel", "duel"}) {
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

    public MenuLayoutManager getMenuLayoutManager() {
        return menuLayoutManager;
    }

    public NamespacedKey keyKit() {
        return keyKit;
    }
}

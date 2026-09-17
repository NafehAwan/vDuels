/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.meowtags;

import com.meowtags.Gradient;
import com.meowtags.MenuListener;
import com.meowtags.TagManager;
import com.meowtags.TagsCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MeowTags
extends JavaPlugin {
    private static final String PREFIX = "<#ff6ad5><bold>\u1d1b\u1d00\u0262\ua731</bold></#ff6ad5> <dark_gray>\u00bb</dark_gray> ";
    private TagManager tags;

    public void onEnable() {
        this.saveDefaultConfig();
        this.tags = new TagManager(this);
        this.tags.load();
        TagsCommand cmd = new TagsCommand(this);
        if (this.getCommand("tags") != null) {
            this.getCommand("tags").setExecutor((CommandExecutor)cmd);
            this.getCommand("tags").setTabCompleter((TabCompleter)cmd);
        }
        this.getServer().getPluginManager().registerEvents((Listener)new MenuListener(this), (Plugin)this);
        int interval = this.getConfig().getInt("reconcile-interval-ticks", 40);
        if (interval < 20) {
            interval = 20;
        }
        int period = interval;
        Bukkit.getScheduler().runTaskTimer((Plugin)this, new Runnable(){

            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    MeowTags.this.tags.refresh(p);
                }
            }
        }, (long)period, (long)period);
        if (this.getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            this.getLogger().warning("LuckPerms not found - tags won't display until it's installed.");
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            this.tags.refresh(p);
        }
        this.getLogger().info("MeowTags enabled with " + this.tags.ordered().size() + " tags.");
    }

    public void onDisable() {
        if (this.tags != null) {
            this.tags.saveData();
        }
    }

    public TagManager tags() {
        return this.tags;
    }

    public Component prefixed(String miniMessage) {
        return Gradient.mini(PREFIX + miniMessage);
    }
}


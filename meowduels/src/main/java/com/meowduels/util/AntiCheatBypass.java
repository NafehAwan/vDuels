/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.permissions.PermissionAttachment
 *  org.bukkit.plugin.Plugin
 */
package com.meowduels.util;

import com.meowduels.MeowDuels;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

public final class AntiCheatBypass {
    private static final List<String> DEFAULT_WORLD_NODES = Arrays.asList("worldguard.region.bypass.{world}");
    private static final List<String> DEFAULT_NODES = Arrays.asList("grim.exempt", "grim.nosetback", "vulcan.bypass", "spartan.bypass", "matrix.bypass", "nocheatplus.bypass", "themis.bypass", "polar.bypass", "aac.bypass");
    private static final Map<UUID, List<PermissionAttachment>> ACTIVE = new HashMap<UUID, List<PermissionAttachment>>();

    private AntiCheatBypass() {
    }

    public static void grant(MeowDuels plugin, Player player, String ... extra) {
        if (player == null) {
            return;
        }
        AntiCheatBypass.release(plugin, player);
        ArrayList<PermissionAttachment> list = new ArrayList<PermissionAttachment>();
        for (String node : AntiCheatBypass.nodes(plugin)) {
            AntiCheatBypass.attach(list, plugin, player, node);
        }
        if (extra != null) {
            for (String node : extra) {
                AntiCheatBypass.attach(list, plugin, player, node);
            }
        }
        if (!list.isEmpty()) {
            ACTIVE.put(player.getUniqueId(), list);
        }
        try {
            player.recalculatePermissions();
            player.updateCommands();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        AntiCheatBypass.runHooks(plugin, player, "anticheat.commands-on-start");
    }

    public static void release(MeowDuels plugin, Player player) {
        if (player == null) {
            return;
        }
        List<PermissionAttachment> list = ACTIVE.remove(player.getUniqueId());
        if (list == null) {
            return;
        }
        for (PermissionAttachment att : list) {
            try {
                player.removeAttachment(att);
            }
            catch (Throwable throwable) {}
        }
        try {
            player.recalculatePermissions();
            player.updateCommands();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        AntiCheatBypass.runHooks(plugin, player, "anticheat.commands-on-end");
    }

    private static void attach(List<PermissionAttachment> list, MeowDuels plugin, Player player, String node) {
        if (node == null || node.trim().isEmpty()) {
            return;
        }
        try {
            list.add(player.addAttachment((Plugin)plugin, node.trim(), true));
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public static String[] worldNodes(MeowDuels plugin, String world) {
        if (world == null || world.isEmpty()) {
            return new String[0];
        }
        List<String> templates = plugin.getConfig().getStringList("anticheat.world-bypass-nodes");
        if (templates == null || templates.isEmpty()) {
            templates = DEFAULT_WORLD_NODES;
        }
        ArrayList<String> out = new ArrayList<String>();
        for (String template : templates) {
            if (template == null || template.trim().isEmpty()) continue;
            out.add(template.trim().replace("{world}", world));
        }
        return out.toArray(new String[0]);
    }

    private static List<String> nodes(MeowDuels plugin) {
        List<String> configured = plugin.getConfig().getStringList("anticheat.bypass-nodes");
        return configured == null || configured.isEmpty() ? DEFAULT_NODES : configured;
    }

    private static void runHooks(MeowDuels plugin, Player player, String path) {
        List<String> commands = plugin.getConfig().getStringList(path);
        if (commands == null || commands.isEmpty()) {
            return;
        }
        for (String raw : commands) {
            if (raw == null || raw.trim().isEmpty()) continue;
            String command = raw.replace("{player}", player.getName()).replace("%player%", player.getName());
            try {
                Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)command);
            }
            catch (Throwable t) {
                plugin.getLogger().warning("Anti-cheat hook failed: " + command + " (" + String.valueOf(t) + ")");
            }
        }
    }
}


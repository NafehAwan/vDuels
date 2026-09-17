/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 */
package com.meowtags;

import com.meowtags.Gradient;
import com.meowtags.MeowTags;
import com.meowtags.TagMenu;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class TagsCommand
implements CommandExecutor,
TabCompleter {
    private final MeowTags plugin;

    public TagsCommand(MeowTags plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub;
        String string = sub = args.length > 0 ? args[0].toLowerCase() : "";
        if (sub.equals("add")) {
            if (!sender.hasPermission("meowtags.admin")) {
                sender.sendMessage(this.plugin.prefixed("<red>You can't manage tags."));
                return true;
            }
            if (args.length < 4) {
                sender.sendMessage(this.plugin.prefixed("<gray>Usage: <white>/tags add <id> <hex1> <hex2> [display]"));
                return true;
            }
            String id = args[1].toLowerCase();
            String from = args[2];
            String to = args[3];
            String display = args.length >= 5 ? TagsCommand.join(args, 4) : id;
            this.plugin.tags().addCustom(id, display, from, to);
            sender.sendMessage(this.plugin.prefixed("<green>Added tag <white>" + id + "<green> \u2014 <bold><gradient:#" + TagsCommand.strip(from) + ":#" + TagsCommand.strip(to) + ">" + Gradient.smallCaps(display) + "</gradient></bold>"));
            sender.sendMessage(this.plugin.prefixed("<gray>Grant it with <white>meowtags." + id));
            return true;
        }
        if (sub.equals("delete") || sub.equals("remove")) {
            if (!sender.hasPermission("meowtags.admin")) {
                sender.sendMessage(this.plugin.prefixed("<red>You can't manage tags."));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(this.plugin.prefixed("<gray>Usage: <white>/tags delete <id>"));
                return true;
            }
            String id = args[1].toLowerCase();
            if (this.plugin.tags().deleteCustom(id)) {
                sender.sendMessage(this.plugin.prefixed("<green>Deleted custom tag <white>" + id));
            } else {
                sender.sendMessage(this.plugin.prefixed("<red>No custom tag <white>" + id + "<red> (defaults can't be deleted)."));
            }
            return true;
        }
        if (sub.equals("reload")) {
            if (!sender.hasPermission("meowtags.admin")) {
                sender.sendMessage(this.plugin.prefixed("<red>You can't manage tags."));
                return true;
            }
            this.plugin.reloadConfig();
            this.plugin.tags().load();
            sender.sendMessage(this.plugin.prefixed("<green>Reloaded config and tags."));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.plugin.prefixed("<red>Only players can use the tag menu."));
            return true;
        }
        Player player = (Player)sender;
        if (sub.equals("off") || sub.equals("clear") || sub.equals("none")) {
            this.plugin.tags().clear(player);
            player.sendMessage(this.plugin.prefixed("<gray>Your tag has been removed."));
            return true;
        }
        if (this.plugin.tags().isInDuel(player)) {
            player.sendMessage(this.plugin.prefixed("<red>You can't change your tag during a duel."));
            return true;
        }
        TagMenu.open(this.plugin, player);
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> out = new ArrayList<String>();
        if (args.length == 1) {
            out.add("off");
            if (sender.hasPermission("meowtags.admin")) {
                out.add("add");
                out.add("delete");
                out.add("reload");
            }
        }
        return out;
    }

    private static String join(String[] a, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < a.length; ++i) {
            if (i > from) {
                sb.append(' ');
            }
            sb.append(a[i]);
        }
        return sb.toString();
    }

    private static String strip(String hex) {
        String h = hex.trim();
        return h.startsWith("#") ? h.substring(1) : h;
    }
}


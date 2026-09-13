package org.bukkit.command;

public interface TabCompleter {
    java.util.List<String> onTabComplete(org.bukkit.command.CommandSender a0, org.bukkit.command.Command a1, String a2, String[] a3);
}

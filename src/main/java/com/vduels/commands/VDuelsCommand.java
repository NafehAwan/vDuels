package com.vduels.commands;

import com.vduels.VDuels;
import com.vduels.gui.AdminDuelMenu;
import com.vduels.gui.ArenaMenu;
import com.vduels.gui.DuelConfirmMenu;
import com.vduels.gui.KitPickMenu;
import com.vduels.model.Arena;
import com.vduels.model.DuelRequest;
import com.vduels.model.Kit;
import com.vduels.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Single executor for every vDuels command. Which command was run is decided by
 * {@link Command#getName()}, keeping registration in plugin.yml simple.
 */
public class VDuelsCommand implements CommandExecutor, TabCompleter {

    private final VDuels plugin;

    public VDuelsCommand(VDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "createarena" -> createArena(sender, args);
            case "arena" -> openArena(sender, args);
            case "deletearena" -> deleteArena(sender, args);
            case "kitcreate" -> createKit(sender, args);
            case "deletekit" -> deleteKit(sender, args);
            case "adminduel" -> adminDuel(sender);
            case "duel" -> duel(sender, args);
            case "scoreboardip" -> scoreboardIp(sender, args);
            case "vduels" -> root(sender, args);
            default -> {
                return false;
            }
        }
        return true;
    }

    // --- admin: arenas ----------------------------------------------------

    private void createArena(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(Text.prefixed("&cUsage: /createarena <name>"));
            return;
        }
        String name = args[0];
        if (plugin.getArenaManager().exists(name)) {
            sender.sendMessage(Text.prefixed("&cAn arena named &f" + name + "&c already exists."));
            return;
        }
        plugin.getArenaManager().create(name);
        sender.sendMessage(Text.prefixed("&aArena &b" + name + "&a created. Open &e/arena " + name + "&a to set it up."));
    }

    private void openArena(CommandSender sender, String[] args) {
        if (!requireAdmin(sender) || !requirePlayer(sender)) {
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(Text.prefixed("&cUsage: /arena <name>"));
            return;
        }
        Arena arena = plugin.getArenaManager().get(args[0]);
        if (arena == null) {
            sender.sendMessage(Text.prefixed("&cNo arena named &f" + args[0] + "&c."));
            return;
        }
        new ArenaMenu(plugin, arena).open((Player) sender);
    }

    private void deleteArena(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(Text.prefixed("&cUsage: /deletearena <name>"));
            return;
        }
        if (!plugin.getArenaManager().exists(args[0])) {
            sender.sendMessage(Text.prefixed("&cNo arena named &f" + args[0] + "&c."));
            return;
        }
        plugin.getArenaManager().delete(args[0]);
        sender.sendMessage(Text.prefixed("&aArena &b" + args[0] + "&a deleted."));
    }

    // --- admin: kits ------------------------------------------------------

    private void createKit(CommandSender sender, String[] args) {
        if (!requireAdmin(sender) || !requirePlayer(sender)) {
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(Text.prefixed("&cUsage: /kitcreate <name>"));
            return;
        }
        String name = args[0];
        if (plugin.getKitManager().exists(name)) {
            sender.sendMessage(Text.prefixed("&cA kit named &f" + name + "&c already exists."));
            return;
        }
        Kit kit = new Kit(name);
        kit.captureFrom((Player) sender);
        plugin.getKitManager().put(kit);
        sender.sendMessage(Text.prefixed("&aKit &b" + name + "&a saved from your current inventory."));
    }

    private void deleteKit(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(Text.prefixed("&cUsage: /deletekit <name>"));
            return;
        }
        if (!plugin.getKitManager().exists(args[0])) {
            sender.sendMessage(Text.prefixed("&cNo kit named &f" + args[0] + "&c."));
            return;
        }
        plugin.getKitManager().delete(args[0]);
        sender.sendMessage(Text.prefixed("&aKit &b" + args[0] + "&a deleted."));
    }

    private void adminDuel(CommandSender sender) {
        if (!requireAdmin(sender) || !requirePlayer(sender)) {
            return;
        }
        new AdminDuelMenu(plugin).open((Player) sender);
    }

    private void scoreboardIp(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(Text.prefixed("&7Current scoreboard IP: &f" + plugin.getScoreboardIp()));
            sender.sendMessage(Text.prefixed("&cUsage: /vduels:scoreboardip <ip>"));
            return;
        }
        plugin.setScoreboardIp(args[0]);
        sender.sendMessage(Text.prefixed("&aScoreboard IP set to &f" + args[0] + "&a."));
    }

    // --- player: duel -----------------------------------------------------

    private void duel(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            sender.sendMessage(Text.prefixed("&cUsage: /duel <player>"));
            return;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            handleAccept(player, args);
            return;
        }

        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            sender.sendMessage(Text.prefixed("&cYou are already in a duel."));
            return;
        }
        if (plugin.getKitManager().isEmpty()) {
            sender.sendMessage(Text.prefixed("&cNo kits have been created yet."));
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Text.prefixed("&cPlayer &f" + args[0] + "&c is not online."));
            return;
        }
        if (target.equals(player)) {
            sender.sendMessage(Text.prefixed("&cYou cannot duel yourself."));
            return;
        }
        // Kit selection comes first; picking a kit opens the DUEL CONFIRM menu.
        DuelConfirmMenu confirm = new DuelConfirmMenu(plugin, target);
        new KitPickMenu(plugin, confirm).open(player);
    }

    private void handleAccept(Player player, String[] args) {
        UUID senderId;
        if (args.length >= 2) {
            Player challenger = plugin.getServer().getPlayerExact(args[1]);
            if (challenger == null) {
                player.sendMessage(Text.prefixed("&cThat player is not online."));
                return;
            }
            senderId = challenger.getUniqueId();
        } else {
            DuelRequest recent = plugin.getDuelManager().getMostRecentRequest(player);
            if (recent == null) {
                player.sendMessage(Text.prefixed("&cYou have no pending duel requests."));
                return;
            }
            senderId = recent.getSender();
        }
        plugin.getDuelManager().acceptRequest(player, senderId);
    }

    // --- root: copy/paste + help -----------------------------------------

    private void root(CommandSender sender, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("scoreboardip")) {
            scoreboardIp(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        if (args.length >= 2 && (args[1].equalsIgnoreCase("copy") || args[1].equalsIgnoreCase("paste"))) {
            duplicator(sender, args[0], args[1]);
            return;
        }
        sendHelp(sender);
    }

    private void duplicator(CommandSender sender, String arenaName, String action) {
        if (!requireAdmin(sender) || !requirePlayer(sender)) {
            return;
        }
        Player player = (Player) sender;
        Arena arena = plugin.getArenaManager().get(arenaName);
        if (arena == null || !arena.isConfigured()) {
            sender.sendMessage(Text.prefixed("&cNo configured arena named &f" + arenaName + "&c."));
            return;
        }
        if (!arena.isDuplicatorEnabled()) {
            sender.sendMessage(Text.prefixed("&cThe duplicator is disabled for this arena. Enable it in &e/arena " + arenaName + "&c."));
            return;
        }
        if (action.equalsIgnoreCase("copy")) {
            plugin.getArenaManager().copyToClipboard(player.getUniqueId(), arena);
            sender.sendMessage(Text.prefixed("&aCopied arena &b" + arena.getName() + "&a to your clipboard."));
        } else {
            if (!plugin.getArenaManager().hasClipboard(player.getUniqueId())) {
                sender.sendMessage(Text.prefixed("&cYour clipboard is empty. Run &e/vduels " + arenaName + " copy&c first."));
                return;
            }
            int blocks = plugin.getArenaManager().pasteClipboard(player.getUniqueId(), player.getLocation());
            sender.sendMessage(Text.prefixed("&aPasted &f" + blocks + "&a blocks at your location."));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Text.color("&8&m----------------------------------------"));
        sender.sendMessage(Text.color("&b&lvDuels &7- commands"));
        sender.sendMessage(Text.color("&e/duel <player> &7- challenge a player"));
        if (sender.hasPermission("vduels.admin")) {
            sender.sendMessage(Text.color("&e/createarena <name> &7- create an arena"));
            sender.sendMessage(Text.color("&e/arena <name> &7- setup / settings GUI"));
            sender.sendMessage(Text.color("&e/deletearena <name> &7- delete an arena"));
            sender.sendMessage(Text.color("&e/kitcreate <name> &7- save a kit from inventory"));
            sender.sendMessage(Text.color("&e/deletekit <name> &7- delete a kit"));
            sender.sendMessage(Text.color("&e/adminduel &7- edit the duel menu layout"));
            sender.sendMessage(Text.color("&e/vduels <arena> copy|paste &7- duplicator"));
            sender.sendMessage(Text.color("&e/vduels:scoreboardip <ip> &7- set scoreboard IP"));
        }
        sender.sendMessage(Text.color("&8&m----------------------------------------"));
    }

    // --- helpers ----------------------------------------------------------

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("vduels.admin")) {
            sender.sendMessage(Text.prefixed("&cYou don't have permission to do that."));
            return false;
        }
        return true;
    }

    private boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Text.prefixed("&cThis command can only be used by a player."));
            return false;
        }
        return true;
    }

    // --- tab completion ---------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();

        if ((name.equals("arena") || name.equals("deletearena")) && args.length == 1) {
            for (Arena arena : plugin.getArenaManager().all()) {
                if (startsWith(arena.getName(), args[0])) {
                    out.add(arena.getName());
                }
            }
        } else if (name.equals("deletekit") && args.length == 1) {
            for (Kit kit : plugin.getKitManager().all()) {
                if (startsWith(kit.getName(), args[0])) {
                    out.add(kit.getName());
                }
            }
        } else if (name.equals("duel") && args.length == 1) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (startsWith(p.getName(), args[0])) {
                    out.add(p.getName());
                }
            }
        } else if (name.equals("vduels")) {
            if (args.length == 1) {
                for (Arena arena : plugin.getArenaManager().all()) {
                    if (startsWith(arena.getName(), args[0])) {
                        out.add(arena.getName());
                    }
                }
            } else if (args.length == 2) {
                for (String sub : List.of("copy", "paste")) {
                    if (startsWith(sub, args[1])) {
                        out.add(sub);
                    }
                }
            }
        }
        return out;
    }

    private boolean startsWith(String value, String prefix) {
        return value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }
}

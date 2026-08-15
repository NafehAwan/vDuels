package com.vduels.commands;

import com.vduels.VDuels;
import com.vduels.gui.ArenaMenu;
import com.vduels.gui.DuelConfirmMenu;
import com.vduels.gui.GuiEditorMenu;
import com.vduels.gui.KitPickMenu;
import com.vduels.managers.CategoryManager;
import com.vduels.managers.GuiLayoutManager;
import com.vduels.model.Arena;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
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
            case "kiticon" -> kitIcon(sender, args);
            case "adminduel" -> editGui(sender, new String[]{GuiLayoutManager.DUEL_CONFIRM});
            case "editgui" -> editGui(sender, args);
            case "category" -> category(sender, args);
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

    private void kitIcon(CommandSender sender, String[] args) {
        if (!requireAdmin(sender) || !requirePlayer(sender)) {
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(Text.prefixed("&cUsage: /kiticon <name>"));
            return;
        }
        Kit kit = plugin.getKitManager().get(args[0]);
        if (kit == null) {
            sender.sendMessage(Text.prefixed("&cNo kit named &f" + args[0] + "&c."));
            return;
        }
        ItemStack held = ((Player) sender).getInventory().getItemInMainHand();
        if (held == null || held.getType() == Material.AIR) {
            sender.sendMessage(Text.prefixed("&cHold the item you want to use as the icon."));
            return;
        }
        kit.setIcon(held.getType());
        plugin.getKitManager().save();
        sender.sendMessage(Text.prefixed("&aKit &b" + kit.getName() + "&a icon set to &f" + held.getType().name() + "&a."));
    }

    private void editGui(CommandSender sender, String[] args) {
        if (!requireAdmin(sender) || !requirePlayer(sender)) {
            return;
        }
        if (args.length < 1 || !GuiLayoutManager.isValidMenu(args[0].toLowerCase(Locale.ROOT))) {
            sender.sendMessage(Text.prefixed("&cUsage: /editgui <duelconfirm|mapselect>"));
            return;
        }
        new GuiEditorMenu(plugin, args[0].toLowerCase(Locale.ROOT)).open((Player) sender);
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
            sender.sendMessage(msg("duel.usage"));
            return;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            handleAccept(player, args);
            return;
        }

        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            sender.sendMessage(msg("duel.already-in-duel"));
            return;
        }
        if (plugin.getKitManager().isEmpty()) {
            sender.sendMessage(msg("duel.no-kits"));
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(msg("duel.target-offline", "name", args[0]));
            return;
        }
        if (target.equals(player)) {
            sender.sendMessage(msg("duel.cannot-duel-self"));
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
                player.sendMessage(msg("accept.target-offline"));
                return;
            }
            senderId = challenger.getUniqueId();
        } else {
            DuelRequest recent = plugin.getDuelManager().getMostRecentRequest(player);
            if (recent == null) {
                player.sendMessage(msg("accept.no-requests"));
                return;
            }
            senderId = recent.getSender();
        }
        plugin.getDuelManager().acceptRequest(player, senderId);
    }

    // --- kit-menu categories ---------------------------------------------

    private void category(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        // args[0] = sub, args[1] = id, args[2..] = rest
        String sub = args.length >= 1 ? args[0].toLowerCase(Locale.ROOT) : "list";
        CategoryManager cats = plugin.getCategoryManager();

        switch (sub) {
            case "list" -> {
                if (cats.all().isEmpty()) {
                    sender.sendMessage(Text.prefixed("&7No categories yet. Create one with &e/category create <id>&7."));
                    return;
                }
                sender.sendMessage(Text.color("&bKit categories:"));
                for (CategoryManager.Category c : cats.all()) {
                    sender.sendMessage(Text.color("&e" + c.getId() + " &8- header &f" + c.getHeader()
                            + " &8- kits: &f" + (c.getKits().isEmpty() ? "all" : String.join(", ", c.getKits()))));
                }
            }
            case "create" -> {
                if (args.length < 2) {
                    sender.sendMessage(Text.prefixed("&cUsage: /category create <id> [header]"));
                    return;
                }
                String header = args.length >= 3 ? joinFrom(args, 2) : args[1].toUpperCase(Locale.ROOT);
                if (cats.create(args[1], header)) {
                    sender.sendMessage(Text.prefixed("&aCreated category &b" + args[1] + "&a."));
                } else {
                    sender.sendMessage(Text.prefixed("&cA category with that id already exists."));
                }
            }
            case "delete" -> {
                if (args.length < 2) {
                    sender.sendMessage(Text.prefixed("&cUsage: /category delete <id>"));
                    return;
                }
                cats.delete(args[1]);
                sender.sendMessage(Text.prefixed("&aDeleted category &b" + args[1] + "&a."));
            }
            case "header" -> {
                if (args.length < 3) {
                    sender.sendMessage(Text.prefixed("&cUsage: /category header <id> <text>"));
                    return;
                }
                CategoryManager.Category c = cats.get(args[1]);
                if (c == null) {
                    sender.sendMessage(Text.prefixed("&cNo category named &f" + args[1] + "&c."));
                    return;
                }
                c.setHeader(joinFrom(args, 2));
                cats.save();
                sender.sendMessage(Text.prefixed("&aHeader for &b" + c.getId() + "&a set to &f" + c.getHeader() + "&a."));
            }
            case "addkit", "removekit" -> {
                if (args.length < 3) {
                    sender.sendMessage(Text.prefixed("&cUsage: /category " + sub + " <id> <kit>"));
                    return;
                }
                CategoryManager.Category c = cats.get(args[1]);
                if (c == null) {
                    sender.sendMessage(Text.prefixed("&cNo category named &f" + args[1] + "&c."));
                    return;
                }
                if (sub.equals("addkit")) {
                    if (!plugin.getKitManager().exists(args[2])) {
                        sender.sendMessage(Text.prefixed("&cNo kit named &f" + args[2] + "&c."));
                        return;
                    }
                    c.getKits().add(args[2]);
                    sender.sendMessage(Text.prefixed("&aAdded &f" + args[2] + "&a to &b" + c.getId() + "&a."));
                } else {
                    c.getKits().removeIf(k -> k.equalsIgnoreCase(args[2]));
                    sender.sendMessage(Text.prefixed("&aRemoved &f" + args[2] + "&a from &b" + c.getId() + "&a."));
                }
                cats.save();
            }
            default -> sender.sendMessage(Text.prefixed("&cSub-commands: list, create, delete, header, addkit, removekit"));
        }
    }

    private String joinFrom(String[] args, int from) {
        return String.join(" ", java.util.Arrays.copyOfRange(args, from, args.length));
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
            sender.sendMessage(Text.color("&e/kiticon <name> &7- set a kit's icon to your held item"));
            sender.sendMessage(Text.color("&e/editgui <menu> &7- customise a GUI layout"));
            sender.sendMessage(Text.color("&e/vduels:category ... &7- manage kit-menu categories"));
            sender.sendMessage(Text.color("&e/vduels <arena> copy|paste &7- duplicator"));
            sender.sendMessage(Text.color("&e/vduels:scoreboardip <ip> &7- set scoreboard IP"));
        }
        sender.sendMessage(Text.color("&8&m----------------------------------------"));
    }

    // --- helpers ----------------------------------------------------------

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("vduels.admin")) {
            sender.sendMessage(msg("general.no-permission"));
            return false;
        }
        return true;
    }

    private boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg("general.players-only"));
            return false;
        }
        return true;
    }

    private String msg(String key, String... placeholders) {
        return plugin.messages().get(key, placeholders);
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
        } else if ((name.equals("deletekit") || name.equals("kiticon")) && args.length == 1) {
            for (Kit kit : plugin.getKitManager().all()) {
                if (startsWith(kit.getName(), args[0])) {
                    out.add(kit.getName());
                }
            }
        } else if (name.equals("editgui") && args.length == 1) {
            for (String menu : List.of("duelconfirm", "mapselect")) {
                if (startsWith(menu, args[0])) {
                    out.add(menu);
                }
            }
        } else if (name.equals("category")) {
            if (args.length == 1) {
                for (String sub : List.of("list", "create", "delete", "header", "addkit", "removekit")) {
                    if (startsWith(sub, args[0])) {
                        out.add(sub);
                    }
                }
            } else if (args.length == 2 && !args[0].equalsIgnoreCase("create")) {
                for (CategoryManager.Category c : plugin.getCategoryManager().all()) {
                    if (startsWith(c.getId(), args[1])) {
                        out.add(c.getId());
                    }
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

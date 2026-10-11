/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package com.meowduels.commands;

import com.meowduels.MeowDuels;
import com.meowduels.gui.ArenaMenu;
import com.meowduels.gui.DuelConfirmMenu;
import com.meowduels.gui.PartyMenu;
import com.meowduels.gui.EditKitEffectsMenu;
import com.meowduels.gui.EditKitListMenu;
import com.meowduels.gui.EventMapMenu;
import com.meowduels.gui.GuiEditorMenu;
import com.meowduels.gui.KitEditorMenu;
import com.meowduels.gui.KitPickMenu;
import com.meowduels.gui.QueuePickMenu;
import com.meowduels.gui.TabConfigMenu;
import com.meowduels.gui.TrimKitMenu;
import com.meowduels.gui.TrimMenu;
import com.meowduels.managers.ArenaManager;
import com.meowduels.managers.CategoryManager;
import com.meowduels.managers.EventManager;
import com.meowduels.managers.GuiLayoutManager;
import com.meowduels.model.Arena;
import com.meowduels.model.DuelRequest;
import com.meowduels.model.Kit;
import com.meowduels.util.GoldenHead;
import com.meowduels.util.Text;
import com.meowduels.util.Trims;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class MeowDuelsCommand
implements CommandExecutor,
TabCompleter {
    private final MeowDuels plugin;

    public MeowDuelsCommand(MeowDuels plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "createarena": {
                this.createArena(sender, args);
                break;
            }
            case "arena": {
                this.openArena(sender, args);
                break;
            }
            case "deletearena": {
                this.deleteArena(sender, args);
                break;
            }
            case "kitcreate": {
                this.createKit(sender, args);
                break;
            }
            case "deletekit": {
                this.deleteKit(sender, args);
                break;
            }
            case "kiticon": {
                this.kitIcon(sender, args);
                break;
            }
            case "kitdisplayname": {
                this.kitDisplayName(sender, args);
                break;
            }
            case "editgui": {
                this.editGui(sender, args);
                break;
            }
            case "category": {
                this.categoryCommand(sender, args, this.plugin.getCategoryManager(), "category");
                break;
            }
            case "categoryqueue": {
                this.categoryCommand(sender, args, this.plugin.getQueueCategoryManager(), "categoryqueue");
                break;
            }
            case "queue": {
                this.queue(sender, args);
                break;
            }
            case "meowduelstab": {
                this.vduelsTab(sender);
                break;
            }
            case "editkit": {
                this.editKit(sender, args);
                break;
            }
            case "spectate": {
                this.spectate(sender, args);
                break;
            }
            case "duel": {
                this.duel(sender, args);
                break;
            }
            case "leave": {
                this.leave(sender);
                break;
            }
            case "party": {
                this.party(sender, args);
                break;
            }
            case "meowduelsspawnitems": {
                if (!this.requirePlayer(sender)) break;
                this.plugin.giveSpawnItems((Player)sender);
                break;
            }
            case "ff": {
                this.forfeit(sender);
                break;
            }
            case "event": {
                this.event(sender, args);
                break;
            }
            case "eventspec": {
                this.eventSpectate(sender);
                break;
            }
            case "eventleave": {
                this.eventLeave(sender);
                break;
            }
            case "scoreboardip": {
                this.scoreboardIp(sender, args);
                break;
            }
            case "meowduels": {
                this.root(sender, args);
                break;
            }
            case "meowduelssetspawn": {
                this.setDuelSpawn(sender);
                break;
            }
            case "kiteditor": {
                this.openKitEditor(sender);
                break;
            }
            case "givegoldenhead": {
                this.giveGoldenHead(sender, args);
                break;
            }
            case "resetconfig": {
                this.resetConfig(sender);
                break;
            }
            case "meowduelsserver": {
                this.setServerName(sender, args);
                break;
            }
            case "meowduelstrims": {
                this.openTrims(sender, args);
                break;
            }
            default: {
                return false;
            }
        }
        return true;
    }

    private void createArena(CommandSender sender, String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.usage-createarena-name"));
            return;
        }
        String name = args[0];
        if (this.plugin.getArenaManager().exists(name)) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.arena-named-already-exists", "name", name));
            return;
        }
        this.plugin.getArenaManager().create(name);
        sender.sendMessage(this.plugin.messages().get("notice.admin.arena-created-open-arena", "name", name, "name2", name));
    }

    private void openArena(CommandSender sender, String[] args) {
        Arena arena;
        if (!this.requireAdmin(sender) || !this.requirePlayer(sender)) {
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.usage-arena-name-tp"));
            return;
        }
        if (args.length >= 2) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("copy") || sub.equals("paste")) {
                this.arenaClone(sender, args[0], sub);
                return;
            }
            if (sub.equals("regen")) {
                this.arenaRegen(sender, args[0]);
                return;
            }
            if (sub.equals("snapshot")) {
                this.arenaSnapshot(sender, args[0]);
                return;
            }
            if (sub.equals("tp") || sub.equals("teleport")) {
                this.arenaTeleport(sender, args[0], args.length >= 3 ? args[2] : "1");
                return;
            }
        }
        if ((arena = this.plugin.getArenaManager().get(args[0])) == null) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.no-arena-named-2", "name", args[0]));
            return;
        }
        new ArenaMenu(this.plugin, arena).open((Player)sender);
    }

    private void arenaClone(CommandSender sender, String arenaName, String action) {
        if (!this.requireAdmin(sender) || !this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        Arena arena = this.plugin.getArenaManager().get(arenaName);
        if (arena == null || !arena.isConfigured()) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.no-configured-arena-named-4", "arenaname", arenaName));
            return;
        }
        if (!arena.isCloningEnabled()) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.cloning-disabled-arena-enable", "arenaname", arenaName));
            return;
        }
        if (action.equalsIgnoreCase("copy")) {
            this.plugin.getArenaManager().copyToClipboard(player.getUniqueId(), arena);
            sender.sendMessage(this.plugin.messages().get("notice.admin.copied-arena-clipboard-walk", "arena", arena.getName(), "arenaname", arenaName));
        } else {
            if (!this.plugin.getArenaManager().hasClipboard(player.getUniqueId())) {
                sender.sendMessage(this.plugin.messages().get("notice.admin.clipboard-empty-run-arena-2", "arenaname", arenaName));
                return;
            }
            ArenaManager.PasteResult result = this.plugin.getArenaManager().pasteAsNewArena(player.getUniqueId(), player.getLocation());
            if (result == null) {
                sender.sendMessage(this.plugin.messages().get("notice.admin.clipboard-empty-run-arena", "arenaname", arenaName));
                return;
            }
            sender.sendMessage(this.plugin.messages().get("notice.admin.pasted-block-s-registered", "value", String.valueOf(result.blocksWritten), "value2", String.valueOf(result.arenaName)));
        }
    }

    private void arenaRegen(CommandSender sender, String arenaName) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        Arena arena = this.plugin.getArenaManager().get(arenaName);
        if (arena == null || !arena.isConfigured()) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.no-configured-arena-named-3", "arenaname", arenaName));
            return;
        }
        int written = this.plugin.getArenaManager().regenArena(arena);
        if (written < 0) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.no-snapshot-saved-yet", "arenaname", arenaName, "arenaname2", arenaName));
        } else {
            sender.sendMessage(this.plugin.messages().get("notice.admin.regenerated-block-s", "written", String.valueOf(written), "arenaname", arenaName));
        }
    }

    private void arenaSnapshot(CommandSender sender, String arenaName) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        Arena arena = this.plugin.getArenaManager().get(arenaName);
        if (arena == null || !arena.isConfigured()) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.no-configured-arena-named-2", "arenaname", arenaName));
            return;
        }
        this.plugin.getArenaManager().snapshotArena(arena);
        sender.sendMessage(this.plugin.messages().get("notice.admin.snapshot-saved-now-regen", "arenaname", arenaName));
    }

    private void arenaTeleport(CommandSender sender, String arenaName, String which) {
        String label;
        Location dest;
        if (!this.requireAdmin(sender) || !this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        Arena arena = this.plugin.getArenaManager().get(arenaName);
        if (arena == null || !arena.isConfigured()) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.no-configured-arena-named", "arenaname", arenaName));
            return;
        }
        if (which.equalsIgnoreCase("2")) {
            dest = arena.getSpawn2();
            label = "Player 2 spawn";
        } else if (which.equalsIgnoreCase("event")) {
            dest = arena.getEventSpawn();
            label = "event spawn";
        } else {
            dest = arena.getSpawn1();
            label = "Player 1 spawn";
        }
        if (dest == null) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.isn-t-set", "label", label, "arenaname", arenaName));
            return;
        }
        player.teleport(dest);
        sender.sendMessage(this.plugin.messages().get("notice.admin.teleported", "label", label, "arenaname", arenaName));
    }

    private void setDuelSpawn(CommandSender sender) {
        if (!this.requireAdmin(sender) || !this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        this.plugin.setDuelSpawn(player.getLocation());
        sender.sendMessage(this.plugin.messages().get("notice.admin.duel-spawn-set-current"));
    }

    /** Everyone's personal kit editor: rearrange the kits you get in a duel.
     *  The kits themselves are admin territory - see /editkit. */
    private void openKitEditor(CommandSender sender) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        new KitEditorMenu(this.plugin, true).open((Player)sender);
    }

    private void deleteArena(CommandSender sender, String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.usage-deletearena-name"));
            return;
        }
        if (!this.plugin.getArenaManager().exists(args[0])) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.no-arena-named", "name", args[0]));
            return;
        }
        this.plugin.getArenaManager().delete(args[0]);
        sender.sendMessage(this.plugin.messages().get("notice.admin.arena-deleted", "name", args[0]));
    }

    private void createKit(CommandSender sender, String[] args) {
        if (!this.requireAdmin(sender) || !this.requirePlayer(sender)) {
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.usage-kitcreate-name"));
            return;
        }
        String name = args[0];
        if (this.plugin.getKitManager().exists(name)) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.kit-named-already-exists", "name", name));
            return;
        }
        Kit kit = new Kit(name);
        kit.captureFrom((Player)sender);
        this.plugin.getKitManager().put(kit);
        sender.sendMessage(this.plugin.messages().get("notice.admin.kit-saved-from-current", "name", name));
    }

    private void deleteKit(CommandSender sender, String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.usage-deletekit-name"));
            return;
        }
        if (!this.plugin.getKitManager().exists(args[0])) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.no-kit-named-6", "name", args[0]));
            return;
        }
        this.plugin.getKitManager().delete(args[0]);
        sender.sendMessage(this.plugin.messages().get("notice.admin.kit-deleted", "name", args[0]));
    }

    private void kitIcon(CommandSender sender, String[] args) {
        if (!this.requireAdmin(sender) || !this.requirePlayer(sender)) {
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.usage-kiticon-name"));
            return;
        }
        Kit kit = this.plugin.getKitManager().get(args[0]);
        if (kit == null) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.no-kit-named-5", "name", args[0]));
            return;
        }
        ItemStack held = ((Player)sender).getInventory().getItemInMainHand();
        if (held == null || held.getType() == Material.AIR) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.hold-item-want-use"));
            return;
        }
        kit.setIcon(held.getType());
        this.plugin.getKitManager().save();
        sender.sendMessage(this.plugin.messages().get("notice.admin.kit-icon-set", "kit", kit.getName(), "value", String.valueOf(held.getType().name())));
    }

    private void kitDisplayName(CommandSender sender, String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.usage-kitdisplayname-name-display"));
            return;
        }
        Kit kit = this.plugin.getKitManager().get(args[0]);
        if (kit == null) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.no-kit-named-4", "name", args[0]));
            return;
        }
        String display = this.joinFrom(args, 1);
        kit.setDisplayName(display);
        this.plugin.getKitManager().save();
        sender.sendMessage(this.plugin.messages().get("notice.admin.kit-display-name-set", "kit", kit.getName()));
    }

    private void editGui(CommandSender sender, String[] args) {
        if (!this.requireAdmin(sender) || !this.requirePlayer(sender)) {
            return;
        }
        if (args.length < 1 || !GuiLayoutManager.isValidMenu(args[0].toLowerCase(Locale.ROOT))) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.usage-editgui-duelconfirm-mapselect"));
            return;
        }
        String menuId = args[0].toLowerCase(Locale.ROOT);
        // A layout saved here wins over the slot values in gui/<menu>.yml, and
        // the editor saves on close even when nothing was moved - so opening it
        // once is enough to pin every slot and make edits to the file look
        // ignored. This is the way back out.
        if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
            if (!this.plugin.getGuiLayoutManager().has(menuId)) {
                sender.sendMessage(this.plugin.messages().get("notice.admin.no-saved-layout-gui", "menuid", menuId, "menuid2", menuId));
                return;
            }
            this.plugin.getGuiLayoutManager().clear(menuId);
            sender.sendMessage(this.plugin.messages().get("notice.admin.cleared-saved-layout-slots", "menuid", menuId, "menuid2", menuId));
            return;
        }
        new GuiEditorMenu(this.plugin, menuId).open((Player)sender);
    }

    private void scoreboardIp(CommandSender sender, String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.current-scoreboard-ip", "scoreboardip", String.valueOf(this.plugin.getScoreboardIp())));
            sender.sendMessage(this.plugin.messages().get("notice.admin.usage-scoreboardip-ip-color"));
            sender.sendMessage(this.plugin.messages().get("notice.admin.color-minimessage-e-g"));
            return;
        }
        this.plugin.setScoreboardIp(args[0]);
        if (args.length >= 2) {
            StringBuilder color = new StringBuilder();
            for (int i = 1; i < args.length; ++i) {
                if (i > 1) {
                    color.append(' ');
                }
                color.append(args[i]);
            }
            this.plugin.getConfig().set("scoreboard-ip-color", (Object)color.toString());
            this.plugin.saveConfig();
            this.plugin.getScoreboardService().reload();
            sender.sendMessage(this.plugin.messages().get("notice.admin.scoreboard-ip-set-2", "name", args[0], "value", String.valueOf(color)));
        } else {
            sender.sendMessage(this.plugin.messages().get("notice.admin.scoreboard-ip-set", "name", args[0]));
        }
    }

    private void openTrims(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (!player.hasPermission("meowduels.trims")) {
            player.sendMessage(this.plugin.messages().get("notice.admin.don-t-have-permission-2"));
            return;
        }
        if (!Trims.available()) {
            player.sendMessage(this.plugin.messages().get("notice.admin.armor-trims-aren-t"));
            return;
        }
        if (args.length >= 1) {
            if (!this.plugin.getKitManager().exists(args[0])) {
                player.sendMessage(this.plugin.messages().get("notice.admin.no-kit-named-3", "name", args[0]));
                return;
            }
            new TrimMenu(this.plugin, this.plugin.getKitManager().get(args[0]).getName(), player.getUniqueId()).open(player);
            return;
        }
        new TrimKitMenu(this.plugin).open(player);
    }

    private void setServerName(CommandSender sender, String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.current-scoreboard-server-name", "servername", String.valueOf(this.plugin.getServerName())));
            sender.sendMessage(this.plugin.messages().get("notice.admin.usage-meowduelsserver-name"));
            return;
        }
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < args.length; ++i) {
            if (i > 0) {
                name.append(' ');
            }
            name.append(args[i]);
        }
        this.plugin.setServerName(name.toString());
        this.plugin.getScoreboardService().reload();
        sender.sendMessage(this.plugin.messages().get("notice.admin.scoreboard-server-name-set", "value", String.valueOf(name)));
    }

    private void duel(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (args.length == 0) {
            sender.sendMessage(this.msg("duel.usage", new String[0]));
            return;
        }
        if (this.checkBusy(player, true, true, true)) {
            return;
        }
        if (args[0].equalsIgnoreCase("accept")) {
            this.handleAccept(player, args);
            return;
        }
        if (this.plugin.getKitManager().isEmpty()) {
            sender.sendMessage(this.msg("duel.no-kits", new String[0]));
            return;
        }
        Player target = this.plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(this.msg("duel.target-offline", "name", args[0]));
            return;
        }
        if (target.equals((Object)player)) {
            sender.sendMessage(this.msg("duel.cannot-duel-self", new String[0]));
            return;
        }
        if (args.length >= 2) {
            Kit kit = this.plugin.getKitManager().get(args[1]);
            if (kit == null) {
                sender.sendMessage(this.msg("duel.kit-gone", new String[0]));
                return;
            }
            int rounds = Math.max(1, this.plugin.getConfig().getInt("duel.default-rounds", 1));
            if (args.length >= 3) {
                try {
                    rounds = Integer.parseInt(args[2]);
                }
                catch (NumberFormatException ex) {
                    sender.sendMessage(this.plugin.messages().get("notice.admin.rounds-must-number-e", "target", target.getName(), "kit", kit.getName()));
                    return;
                }
                if (rounds < 1) {
                    rounds = 1;
                }
                if (rounds > 99) {
                    rounds = 99;
                }
            }
            this.plugin.getDuelManager().sendRequest(player, target, kit.getName(), rounds, null);
            return;
        }
        DuelConfirmMenu confirm = new DuelConfirmMenu(this.plugin, player, target);
        new KitPickMenu(this.plugin, confirm).open(player);
    }

    private boolean checkBusy(Player player, boolean spectate, boolean queue, boolean duel) {
        UUID id = player.getUniqueId();
        // A party is exclusive: while you're in one, duelling, queueing,
        // spectating and events are all off the table.
        if (this.plugin.getPartyManager().busy(player)) {
            return true;
        }
        if (this.plugin.getEventManager().isInvolved(id)) {
            player.sendMessage(this.plugin.messages().get("notice.admin.re-event-type-leave"));
            return true;
        }
        if (spectate && this.plugin.getSpectateManager().isSpectating(id)) {
            player.sendMessage(this.plugin.messages().get("notice.admin.re-spectating-type-leave"));
            return true;
        }
        if (queue && this.plugin.getQueueManager().isQueued(id)) {
            player.sendMessage(this.plugin.messages().get("notice.admin.re-queue-type-leave-2"));
            return true;
        }
        if (duel && this.plugin.getDuelManager().isInDuel(id)) {
            player.sendMessage(this.plugin.messages().get("notice.admin.re-duel-type-leave-2"));
            return true;
        }
        return false;
    }

    private void event(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (args.length == 0) {
            this.plugin.getEventManager().join(player);
            return;
        }
        if (args[0].equalsIgnoreCase("host")) {
            int minutes;
            if (!player.hasPermission("meowduels.event.host")) {
                player.sendMessage(this.msg("event.no-permission", new String[0]));
                return;
            }
            if (args.length >= 2 && args[1].equalsIgnoreCase("force_start")) {
                this.plugin.getEventManager().forceStart(player);
                return;
            }
            if (args.length >= 2 && args[1].equalsIgnoreCase("force_end")) {
                this.plugin.getEventManager().forceEnd(player);
                return;
            }
            if (args.length < 3) {
                player.sendMessage(this.msg("event.host-usage", new String[0]));
                return;
            }
            if (!this.plugin.getKitManager().exists(args[1])) {
                player.sendMessage(this.msg("event.no-kit", new String[0]));
                return;
            }
            try {
                minutes = Integer.parseInt(args[2]);
            }
            catch (NumberFormatException ex) {
                player.sendMessage(this.plugin.messages().get("notice.admin.minutes-must-number-e", "name", args[1]));
                return;
            }
            if (minutes < 1) {
                minutes = 1;
            }
            int slots = 0;
            if (args.length >= 4) {
                try {
                    slots = Integer.parseInt(args[3]);
                }
                catch (NumberFormatException ex) {
                    player.sendMessage(this.plugin.messages().get("notice.admin.slots-must-number-e", "name", args[1]));
                    return;
                }
                if (slots < 0) {
                    slots = 0;
                }
            }
            if (this.plugin.getEventManager().getState() != EventManager.State.NONE) {
                player.sendMessage(this.msg("event.already-running", new String[0]));
                return;
            }
            new EventMapMenu(this.plugin, player, args[1], minutes, slots).open(player);
            return;
        }
        this.plugin.getEventManager().join(player);
    }

    private void eventSpectate(CommandSender sender) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        UUID id = player.getUniqueId();
        if (this.plugin.getDuelManager().isInDuel(id)) {
            player.sendMessage(this.plugin.messages().get("notice.admin.re-duel-type-leave"));
            return;
        }
        if (this.plugin.getSpectateManager().isSpectating(id)) {
            player.sendMessage(this.plugin.messages().get("notice.admin.re-already-spectating-type"));
            return;
        }
        if (this.plugin.getQueueManager().isQueued(id)) {
            player.sendMessage(this.plugin.messages().get("notice.admin.re-queue-type-leave"));
            return;
        }
        this.plugin.getEventManager().spectate(player);
    }

    private void eventLeave(CommandSender sender) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (this.plugin.getEventManager().isInvolved(player.getUniqueId())) {
            this.plugin.getEventManager().leave(player);
        } else {
            player.sendMessage(this.plugin.messages().get("notice.admin.aren-t-event"));
        }
    }

    private void forfeit(CommandSender sender) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (this.plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            this.plugin.getDuelManager().leave(player);
        } else {
            player.sendMessage(this.plugin.messages().get("notice.admin.aren-t-duel"));
        }
    }

    /**
     * /party - the text door to everything the party items do.
     *
     * <p>Invite and accept only exist here: they need a player name, and a menu
     * that lists every online player would be a worse way to type one.
     */
    /**
     * Completions for /party.
     *
     * <p>The lists are filtered to what would actually work: invite offers only
     * people who can be invited, and accept/decline only the leaders who have
     * actually invited you. A completion that suggests something the command
     * then refuses is worse than no completion, because it looks like the
     * command is broken rather than the choice.
     *
     * @return null when this is not /party, so the caller falls through
     */
    private List<String> partyComplete(CommandSender sender, Command command, String[] args) {
        if (!command.getName().equalsIgnoreCase("party")) {
            return null;
        }
        ArrayList<String> out = new ArrayList<String>();
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0];
            for (String sub : new String[]{"create", "invite", "join", "decline", "duel", "spectate", "force_end", "leave", "disband"}) {
                if (this.startsWith(sub, prefix)) {
                    out.add(sub);
                }
            }
            return out;
        }
        if (!(sender instanceof Player)) {
            return out;
        }
        Player player = (Player)sender;
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("duel")) {
            if (args.length == 2) {
                for (String action : new String[]{"accept", "decline"}) {
                    if (this.startsWith(action, args[1])) {
                        out.add(action);
                    }
                }
            } else if (args.length == 3) {
                for (String leader : this.plugin.getPartyManager().duelChallengers(player.getUniqueId())) {
                    if (this.startsWith(leader, args[2])) {
                        out.add(leader);
                    }
                }
            }
            return out;
        }
        if (args.length != 2) {
            return out;
        }
        if (sub.equals("invite")) {
            for (Player online : this.plugin.getServer().getOnlinePlayers()) {
                if (online.getUniqueId().equals(player.getUniqueId())) continue;
                if (this.plugin.getPartyManager().inParty(online.getUniqueId())) continue;
                if (!this.startsWith(online.getName(), args[1])) continue;
                out.add(online.getName());
            }
        } else if (sub.equals("join") || sub.equals("decline")) {
            for (String leader : this.plugin.getPartyManager().invitersOf(player.getUniqueId())) {
                if (this.startsWith(leader, args[1])) {
                    out.add(leader);
                }
            }
        }
        return out;
    }

    private void party(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        if (sub.equals("create")) {
            this.plugin.getPartyManager().create(player);
            return;
        }
        if (sub.equals("leave") || sub.equals("disband")) {
            this.plugin.getPartyManager().leave(player);
            return;
        }
        // /party duel accept|decline <leader> - answering another party's
        // challenge. Deliberately its own subcommand rather than sharing
        // "join": joining a party and agreeing to fight one are different
        // enough that one word for both would be a trap.
        if (sub.equals("duel")) {
            String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
            if (args.length < 3 || !action.equals("accept") && !action.equals("decline")) {
                player.sendMessage(this.plugin.messages().get("notice.admin.usage-party-duel-accept"));
                return;
            }
            Player from = this.plugin.getServer().getPlayerExact(args[2]);
            if (from == null) {
                player.sendMessage(this.plugin.messages().get("notice.admin.isn-t-online-2", "name", args[2]));
                return;
            }
            if (action.equals("accept")) {
                this.plugin.getPartyManager().acceptDuels(player, from);
            } else {
                this.plugin.getPartyManager().declineDuels(player, from);
            }
            return;
        }
        if (sub.equals("force_end") || sub.equals("forceend")) {
            this.plugin.getPartyManager().forceEnd(player);
            return;
        }
        // "accept" still works: it is in every invite message anyone has ever
        // been sent, and those are already in people's chat history.
        if (sub.equals("invite") || sub.equals("join") || sub.equals("accept") || sub.equals("decline")) {
            if (args.length < 2) {
                player.sendMessage(this.plugin.messages().get("notice.admin.usage-party-player", "sub", sub));
                return;
            }
            Player target = this.plugin.getServer().getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(this.plugin.messages().get("notice.admin.isn-t-online", "name", args[1]));
                return;
            }
            if (sub.equals("invite")) {
                this.plugin.getPartyManager().invite(player, target);
            } else if (sub.equals("decline")) {
                this.plugin.getPartyManager().decline(player, target);
            } else {
                this.plugin.getPartyManager().accept(player, target);
            }
            return;
        }
        if (sub.equals("spectate")) {
            this.plugin.getPartyManager().spectate(player);
            return;
        }
        if (this.plugin.getPartyManager().inParty(player.getUniqueId())) {
            new PartyMenu(this.plugin).openFor(player);
            return;
        }
        player.sendMessage(this.plugin.messages().get("notice.admin.aren-t-party-party"));
    }

    private void leave(CommandSender sender) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        UUID id = player.getUniqueId();
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (player.isOnline() && !this.plugin.getDuelManager().isInDuel(player.getUniqueId())) {
                this.plugin.giveSpawnItems(player);
            }
        }, 2L);
        // Party match first: someone knocked out and watching is ALSO not in a
        // duel, not in an event and not a SpectateManager spectator, so every
        // check below would fall through and tell them they aren't in anything.
        if (this.plugin.getPartyManager().leaveMatch(player)) {
            return;
        }
        if (this.plugin.getEventManager().isInvolved(id)) {
            this.plugin.getEventManager().leave(player);
            return;
        }
        if (this.plugin.getDuelManager().isInDuel(id)) {
            this.plugin.getDuelManager().leave(player);
            return;
        }
        if (this.plugin.getSpectateManager().isSpectating(id)) {
            this.plugin.getSpectateManager().stop(player);
            return;
        }
        if (this.plugin.getQueueManager().isQueued(id)) {
            this.plugin.getQueueManager().leaveAll(player);
            return;
        }
        if (this.plugin.getPartyManager().inParty(id)) {
            player.sendMessage(this.plugin.messages().get("notice.admin.re-party-use-leave"));
            return;
        }
        player.sendMessage(this.plugin.messages().get("notice.admin.aren-t-event-duel"));
    }

    private void vduelsTab(CommandSender sender) {
        if (!this.requireAdmin(sender) || !this.requirePlayer(sender)) {
            return;
        }
        new TabConfigMenu(this.plugin).open((Player)sender);
    }

    private void editKit(CommandSender sender, String[] args) {
        if (!this.requireAdmin(sender) || !this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (args.length >= 1) {
            Kit kit = this.plugin.getKitManager().get(args[0]);
            if (kit == null) {
                sender.sendMessage(this.plugin.messages().get("notice.admin.no-kit-named-2", "name", args[0]));
                return;
            }
            new EditKitEffectsMenu(this.plugin, kit).open(player);
            return;
        }
        if (this.plugin.getKitManager().isEmpty()) {
            sender.sendMessage(this.msg("duel.no-kits", new String[0]));
            return;
        }
        new EditKitListMenu(this.plugin).open(player);
    }

    private void spectate(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (args.length == 0) {
            this.plugin.getSpectateManager().stop(player);
            return;
        }
        if (this.checkBusy(player, false, true, true)) {
            return;
        }
        Player target = this.plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(this.msg("spectate.not-found", "name", args[0]));
            return;
        }
        this.plugin.getSpectateManager().spectate(player, target);
    }

    private void queue(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (args.length >= 1 && args[0].equalsIgnoreCase("leave")) {
            this.plugin.getQueueManager().leaveAll(player);
            return;
        }
        if (this.checkBusy(player, true, false, true)) {
            return;
        }
        if (args.length >= 1) {
            Kit kit = this.plugin.getKitManager().get(args[0]);
            if (kit == null) {
                sender.sendMessage(this.msg("queue.kit-gone", new String[0]));
                return;
            }
            this.plugin.getQueueManager().join(player, kit.getName());
            return;
        }
        if (this.plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            sender.sendMessage(this.msg("duel.already-in-duel", new String[0]));
            return;
        }
        if (this.plugin.getKitManager().isEmpty()) {
            sender.sendMessage(this.msg("duel.no-kits", new String[0]));
            return;
        }
        new QueuePickMenu(this.plugin).open(player);
    }

    private void handleAccept(Player player, String[] args) {
        UUID senderId;
        if (args.length >= 2) {
            Player challenger = this.plugin.getServer().getPlayerExact(args[1]);
            if (challenger == null) {
                player.sendMessage(this.msg("accept.target-offline", new String[0]));
                return;
            }
            senderId = challenger.getUniqueId();
        } else {
            DuelRequest recent = this.plugin.getDuelManager().getMostRecentRequest(player);
            if (recent == null) {
                player.sendMessage(this.msg("accept.no-requests", new String[0]));
                return;
            }
            senderId = recent.getSender();
        }
        this.plugin.getDuelManager().acceptRequest(player, senderId);
    }

    private void categoryCommand(CommandSender sender, String[] args, CategoryManager cats, String cmd) {
        String sub;
        if (!this.requireAdmin(sender)) {
            return;
        }
        switch (sub = args.length >= 1 ? args[0].toLowerCase(Locale.ROOT) : "list") {
            case "list": {
                if (cats.all().isEmpty()) {
                    sender.sendMessage(this.plugin.messages().get("notice.admin.no-categories-yet-create", "cmd", cmd));
                    return;
                }
                sender.sendMessage(Text.color("&dKit categories:"));
                for (CategoryManager.Category c : cats.all()) {
                    sender.sendMessage(Text.color("&e" + c.getId() + " &8- header &f" + c.getHeader() + " &8- kits: &f" + (c.getKits().isEmpty() ? "all" : String.join((CharSequence)", ", c.getKits()))));
                }
                break;
            }
            case "create": {
                String header;
                if (args.length < 2) {
                    sender.sendMessage(this.plugin.messages().get("notice.admin.usage-create-id-header", "cmd", cmd));
                    return;
                }
                String string = header = args.length >= 3 ? this.joinFrom(args, 2) : args[1].toUpperCase(Locale.ROOT);
                if (cats.create(args[1], header)) {
                    sender.sendMessage(this.plugin.messages().get("notice.admin.created-category", "name", args[1]));
                    break;
                }
                sender.sendMessage(this.plugin.messages().get("notice.admin.category-id-already-exists"));
                break;
            }
            case "delete": {
                if (args.length < 2) {
                    sender.sendMessage(this.plugin.messages().get("notice.admin.usage-delete-id", "cmd", cmd));
                    return;
                }
                cats.delete(args[1]);
                sender.sendMessage(this.plugin.messages().get("notice.admin.deleted-category", "name", args[1]));
                break;
            }
            case "header": {
                if (args.length < 3) {
                    sender.sendMessage(this.plugin.messages().get("notice.admin.usage-header-id-text", "cmd", cmd));
                    return;
                }
                CategoryManager.Category c = cats.get(args[1]);
                if (c == null) {
                    sender.sendMessage(this.plugin.messages().get("notice.admin.no-category-named-2", "name", args[1]));
                    return;
                }
                c.setHeader(this.joinFrom(args, 2));
                cats.save();
                sender.sendMessage(this.plugin.messages().get("notice.admin.header-set", "id", String.valueOf(c.getId()), "header", c.getHeader()));
                break;
            }
            case "addkit": 
            case "removekit": {
                if (args.length < 3) {
                    sender.sendMessage(this.plugin.messages().get("notice.admin.usage-id-kit", "cmd", cmd, "sub", sub));
                    return;
                }
                CategoryManager.Category c = cats.get(args[1]);
                if (c == null) {
                    sender.sendMessage(this.plugin.messages().get("notice.admin.no-category-named", "name", args[1]));
                    return;
                }
                boolean adding = sub.equals("addkit");
                if (adding) {
                    if (!this.plugin.getKitManager().exists(args[2])) {
                        sender.sendMessage(this.plugin.messages().get("notice.admin.no-kit-named", "name", args[2]));
                        return;
                    }
                    this.addKitDedup(c, args[2]);
                    sender.sendMessage(this.plugin.messages().get("notice.admin.added", "name", args[2], "id", String.valueOf(c.getId())));
                } else {
                    c.getKits().removeIf(k -> k.equalsIgnoreCase(args[2]));
                    sender.sendMessage(this.plugin.messages().get("notice.admin.removed-from", "name", args[2], "id", String.valueOf(c.getId())));
                }
                cats.save();
                CategoryManager other = cats == this.plugin.getCategoryManager() ? this.plugin.getQueueCategoryManager() : this.plugin.getCategoryManager();
                this.syncKit(other, c.getId(), c.getHeader(), args[2], adding);
                break;
            }
            default: {
                sender.sendMessage(this.plugin.messages().get("notice.admin.sub-commands-list-create"));
            }
        }
    }

    private void addKitDedup(CategoryManager.Category category, String kit) {
        if (category.getKits().stream().noneMatch(k -> k.equalsIgnoreCase(kit))) {
            category.getKits().add(kit);
        }
    }

    private void syncKit(CategoryManager other, String catId, String header, String kit, boolean adding) {
        CategoryManager.Category oc = other.get(catId);
        if (oc == null) {
            if (!adding) {
                return;
            }
            other.create(catId, header);
            oc = other.get(catId);
            if (oc == null) {
                return;
            }
        }
        if (adding) {
            this.addKitDedup(oc, kit);
        } else {
            oc.getKits().removeIf(k -> k.equalsIgnoreCase(kit));
        }
        other.save();
    }

    private String joinFrom(String[] args, int from) {
        return String.join((CharSequence)" ", Arrays.copyOfRange(args, from, args.length));
    }

    private void root(CommandSender sender, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!this.requireAdmin(sender)) {
                return;
            }
            this.plugin.reloadAll();
            sender.sendMessage(this.msg("general.reloaded", new String[0]));
            return;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("scoreboardip")) {
            this.scoreboardIp(sender, Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        if (args.length >= 2 && (args[1].equalsIgnoreCase("copy") || args[1].equalsIgnoreCase("paste"))) {
            this.arenaClone(sender, args[0], args[1]);
            return;
        }
        this.sendHelp(sender);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Text.color("&8&m----------------------------------------"));
        sender.sendMessage(Text.color("&d&lMeowDuels &7- commands"));
        sender.sendMessage(Text.color("&d/duel <player> &7- challenge a player"));
        sender.sendMessage(Text.color("&d/leave &7- forfeit your current duel"));
        sender.sendMessage(Text.color("&d/queue &7- open the 1v1 queue menu"));
        if (sender.hasPermission("meowduels.admin")) {
            sender.sendMessage(Text.color("&d/createarena <name> &7- create an arena"));
            sender.sendMessage(Text.color("&d/arena <name> &7- setup / settings GUI"));
            sender.sendMessage(Text.color("&d/arena <name> copy|paste &7- clone the arena; the copy"));
            sender.sendMessage(Text.color("  &7auto-registers itself, ready to use immediately"));
            sender.sendMessage(Text.color("&d/arena <name> regen|snapshot &7- manual regen tools"));
            sender.sendMessage(Text.color("&d/deletearena <name> &7- delete an arena"));
            sender.sendMessage(Text.color("&d/meowduelssetspawn &7- set where duels send players when they end"));
            sender.sendMessage(Text.color("&d/kitcreate <name> &7- save a kit from inventory"));
            sender.sendMessage(Text.color("&d/kiteditor &7- GUI item editor for every kit/gamemode"));
            sender.sendMessage(Text.color("&d/deletekit <name> &7- delete a kit"));
            sender.sendMessage(Text.color("&d/kiticon <name> &7- set a kit's icon to your held item"));
            sender.sendMessage(Text.color("&d/editgui <menu> [reset] &7- arrange a GUI, or clear it back to gui/<menu>.yml"));
            sender.sendMessage(Text.color("&d/category ... &7- manage kit-menu categories"));
            sender.sendMessage(Text.color("&d/scoreboardip <ip> &7- set scoreboard IP"));
        }
        sender.sendMessage(Text.color("&8&m----------------------------------------"));
    }

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("meowduels.admin")) {
            sender.sendMessage(this.msg("general.no-permission", new String[0]));
            return false;
        }
        return true;
    }

    private boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.msg("general.players-only", new String[0]));
            return false;
        }
        return true;
    }

    private String msg(String key, String ... placeholders) {
        return this.plugin.messages().get(key, placeholders);
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Handled before the block below, which is one long decompiled chain
        // this has no business being threaded into.
        List<String> party = this.partyComplete(sender, command, args);
        if (party != null) {
            return party;
        }
        ArrayList<String> out;
        block39: {
            String name;
            block44: {
                block42: {
                    CategoryManager cats;
                    block43: {
                        block41: {
                            block40: {
                                block38: {
                                    String cmd = command.getName().toLowerCase(Locale.ROOT);
                                    if (cmd.equals("queue") && args.length == 1) {
                                        ArrayList<String> o = new ArrayList<String>();
                                        if (this.startsWith("leave", args[0])) {
                                            o.add("leave");
                                        }
                                        for (Kit kit : this.plugin.getKitManager().all()) {
                                            if (!this.startsWith(kit.getName(), args[0])) continue;
                                            o.add(kit.getName());
                                        }
                                        return o;
                                    }
                                    if (cmd.equals("event")) {
                                        ArrayList<String> o = new ArrayList<String>();
                                        if (args.length == 1) {
                                            if (this.startsWith("host", args[0])) {
                                                o.add("host");
                                            }
                                        } else if (args.length == 2 && args[0].equalsIgnoreCase("host")) {
                                            if (this.startsWith("force_start", args[1])) {
                                                o.add("force_start");
                                            }
                                            if (this.startsWith("force_end", args[1])) {
                                                o.add("force_end");
                                            }
                                            for (Kit kit : this.plugin.getKitManager().all()) {
                                                if (!this.startsWith(kit.getName(), args[1])) continue;
                                                o.add(kit.getName());
                                            }
                                        } else if (args.length == 3 && args[0].equalsIgnoreCase("host")) {
                                            for (String m : List.of("1", "3", "5", "10")) {
                                                if (!m.startsWith(args[2])) continue;
                                                o.add(m);
                                            }
                                        }
                                        return o;
                                    }
                                    if (cmd.equals("meowduelstrims") && args.length == 1) {
                                        ArrayList<String> o = new ArrayList<String>();
                                        for (Kit kit : this.plugin.getKitManager().all()) {
                                            if (!this.startsWith(kit.getName(), args[0])) continue;
                                            o.add(kit.getName());
                                        }
                                        return o;
                                    }
                                    if (cmd.equals("givegoldenhead") && args.length == 1) {
                                        ArrayList<String> o = new ArrayList<String>();
                                        for (Player p : this.plugin.getServer().getOnlinePlayers()) {
                                            if (!this.startsWith(p.getName(), args[0])) continue;
                                            o.add(p.getName());
                                        }
                                        return o;
                                    }
                                    if (cmd.equals("givegoldenhead") && args.length == 2) {
                                        ArrayList<String> o = new ArrayList<String>();
                                        for (String amt : List.of("1", "8", "16", "32", "64")) {
                                            if (!amt.startsWith(args[1])) continue;
                                            o.add(amt);
                                        }
                                        return o;
                                    }
                                    if (cmd.equals("duel") && args.length == 2) {
                                        ArrayList<String> o = new ArrayList<String>();
                                        for (Kit kit : this.plugin.getKitManager().all()) {
                                            if (!this.startsWith(kit.getName(), args[1])) continue;
                                            o.add(kit.getName());
                                        }
                                        return o;
                                    }
                                    if (cmd.equals("duel") && args.length == 3) {
                                        ArrayList<String> o = new ArrayList<String>();
                                        for (String r : List.of("1", "3", "5")) {
                                            if (!r.startsWith(args[2])) continue;
                                            o.add(r);
                                        }
                                        return o;
                                    }
                                    name = command.getName().toLowerCase(Locale.ROOT);
                                    out = new ArrayList<String>();
                                    if (!name.equals("arena") && !name.equals("deletearena") || args.length != 1) break block38;
                                    for (Arena arena : this.plugin.getArenaManager().all()) {
                                        if (!this.startsWith(arena.getName(), args[0])) continue;
                                        out.add(arena.getName());
                                    }
                                    break block39;
                                }
                                if (!name.equals("deletekit") && !name.equals("kiticon") && !name.equals("kitdisplayname") && !name.equals("editkit") || args.length != 1) break block40;
                                for (Kit kit : this.plugin.getKitManager().all()) {
                                    if (!this.startsWith(kit.getName(), args[0])) continue;
                                    out.add(kit.getName());
                                }
                                break block39;
                            }
                            if (!name.equals("editgui")) break block41;
                            if (args.length == 1) {
                                for (String menu : List.of("duelconfirm", "mapselect")) {
                                    if (!this.startsWith(menu, args[0])) continue;
                                    out.add(menu);
                                }
                                break block39;
                            }
                            if (args.length == 2 && this.startsWith("reset", args[1])) {
                                out.add("reset");
                            }
                            break block39;
                        }
                        if (!name.equals("category") && !name.equals("categoryqueue")) break block42;
                        cats = name.equals("categoryqueue") ? this.plugin.getQueueCategoryManager() : this.plugin.getCategoryManager();
                        CategoryManager categoryManager = cats;
                        if (args.length != 1) break block43;
                        for (String sub : List.of("list", "create", "delete", "header", "addkit", "removekit")) {
                            if (!this.startsWith(sub, args[0])) continue;
                            out.add(sub);
                        }
                        break block39;
                    }
                    if (args.length != 2 || args[0].equalsIgnoreCase("create")) break block39;
                    for (CategoryManager.Category c : cats.all()) {
                        if (!this.startsWith(c.getId(), args[1])) continue;
                        out.add(c.getId());
                    }
                    break block39;
                }
                if (!name.equals("queue") || args.length != 1) break block44;
                if (!this.startsWith("leave", args[0])) break block39;
                out.add("leave");
                break block39;
            }
            if ((name.equals("duel") || name.equals("spectate")) && args.length == 1) {
                for (Player p : this.plugin.getServer().getOnlinePlayers()) {
                    if (!this.startsWith(p.getName(), args[0])) continue;
                    out.add(p.getName());
                }
            } else if (name.equals("meowduels")) {
                if (args.length == 1) {
                    for (Arena arena : this.plugin.getArenaManager().all()) {
                        if (!this.startsWith(arena.getName(), args[0])) continue;
                        out.add(arena.getName());
                    }
                } else if (args.length == 2) {
                    for (String sub : List.of("copy", "paste")) {
                        if (!this.startsWith(sub, args[1])) continue;
                        out.add(sub);
                    }
                }
            }
        }
        return out;
    }

    private void resetConfig(CommandSender sender) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        File dataFolder = this.plugin.getDataFolder();
        File configFile = new File(dataFolder, "config.yml");
        if (configFile.exists()) {
            configFile.delete();
        }
        this.plugin.saveResource("config.yml", true);
        File arenaFile = new File(dataFolder, "arenas.yml");
        if (arenaFile.exists()) {
            arenaFile.delete();
        }
        this.plugin.getArenaManager().load();
        this.plugin.reloadAll();
        sender.sendMessage(this.plugin.messages().get("notice.admin.config-reset-defaults-all", "value", String.valueOf(this.plugin.getArenaManager().all().size())));
    }

    private void giveGoldenHead(CommandSender sender, String[] args) {
        int stack;
        Player target;
        if (!sender.hasPermission("uhc.admin.givehead")) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.don-t-have-permission"));
            return;
        }
        if (args.length >= 1) {
            target = this.plugin.getServer().getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(this.plugin.messages().get("notice.admin.player-not-online", "name", args[0]));
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player)sender;
        } else {
            sender.sendMessage(this.plugin.messages().get("notice.admin.console-must-specify-player"));
            return;
        }
        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException e) {
                sender.sendMessage(this.plugin.messages().get("notice.admin.invalid-amount", "name", args[1]));
                return;
            }
            if (amount < 1) {
                sender.sendMessage(this.plugin.messages().get("notice.admin.amount-must-at-least"));
                return;
            }
        }
        String texture = this.plugin.getConfig().getString("golden-head.texture", "");
        for (int remaining = amount; remaining > 0; remaining -= stack) {
            stack = Math.min(64, remaining);
            target.getInventory().addItem(new ItemStack[]{GoldenHead.create((Plugin)this.plugin, stack, texture)});
        }
        String plural = amount == 1 ? "" : "s";
        target.sendMessage(this.plugin.messages().get("notice.admin.received-golden-head", "amount", String.valueOf(amount), "plural", plural));
        if (sender != target) {
            sender.sendMessage(this.plugin.messages().get("notice.admin.gave-golden-head", "amount", String.valueOf(amount), "plural", plural, "target", target.getName()));
        }
    }

    private boolean startsWith(String value, String prefix) {
        return value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }
}


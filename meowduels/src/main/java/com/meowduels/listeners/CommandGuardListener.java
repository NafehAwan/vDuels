package com.meowduels.listeners;

import com.meowduels.MeowDuels;
import com.meowduels.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * No commands while you are fighting, bar the ones that get you out.
 *
 * <p>A command mid-match is either an escape hatch or an exploit: /home, /tp
 * and /kit all end the fight in a way the plugin never sees, leaving an arena
 * booked and an opponent standing in it. Blocking everything and naming the
 * exceptions is the only version of this that stays correct as a server adds
 * plugins, because the dangerous list is open-ended and the safe one is not.
 *
 * <p>Entries in the allow list are matched on the longest one that fits, so
 * "party" and "party leave" can disagree: the first blocks the whole command,
 * the second permits exactly that subcommand. Aliases are resolved against the
 * plugin's own commands so /meowduels:leave is not a way round it.
 */
public class CommandGuardListener
implements Listener {
    private static final String BYPASS = "meowduels.bypass.commands";

    private final MeowDuels plugin;

    public CommandGuardListener(MeowDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!this.plugin.getConfig().getBoolean("commands.block-in-match", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS)) {
            return;
        }
        String what = this.matchKind(player);
        if (what == null) {
            return;
        }
        String typed = CommandGuardListener.normalise(event.getMessage());
        if (typed.isEmpty() || this.allowed(typed)) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage(Text.prefixed("&cYou can't use commands " + what + "."));
        player.sendMessage(Text.prefixed("&7Use &f" + this.exitHint(player) + "&7 to get out first."));
    }

    /** What the player is in the middle of, or null if they are free. */
    private String matchKind(Player player) {
        UUID id = player.getUniqueId();
        if (this.plugin.getDuelManager().isInDuel(id)) {
            return "during a duel";
        }
        if (this.plugin.getPartyManager().inPartyMatch(id)) {
            return "during a party match";
        }
        if (this.plugin.getEventManager().isPlaying(id)) {
            return "during an event";
        }
        // Spectators are deliberately not blocked: they are not in the fight,
        // and /spectate is how they stop.
        return null;
    }

    private String exitHint(Player player) {
        return this.plugin.getEventManager().isPlaying(player.getUniqueId()) ? "/eventleave" : "/leave";
    }

    /**
     * "/Meowduels:Party  Leave x" -> "party leave x".
     *
     * <p>The namespace prefix goes because /meowduels:leave and /leave are the
     * same command, and a guard that only knows one of them is not a guard.
     */
    private static String normalise(String message) {
        String line = message == null ? "" : message.trim();
        if (line.startsWith("/")) {
            line = line.substring(1);
        }
        line = line.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        int colon = line.indexOf(58);
        int firstSpace = line.indexOf(32);
        if (colon > 0 && (firstSpace < 0 || colon < firstSpace)) {
            line = line.substring(colon + 1);
        }
        return line;
    }

    private boolean allowed(String typed) {
        for (String entry : this.allowList()) {
            String rule = CommandGuardListener.normalise(entry);
            if (rule.isEmpty()) continue;
            // Whole words only: "leave" must not clear "leaveserver", and
            // "party leave" must not clear "party leavealltheway".
            if (typed.equals(rule) || typed.startsWith(rule + " ")) {
                return true;
            }
        }
        return false;
    }

    private List<String> allowList() {
        List<String> configured = this.plugin.getConfig().getStringList("commands.allowed-in-match");
        if (configured != null && !configured.isEmpty()) {
            return configured;
        }
        ArrayList<String> fallback = new ArrayList<String>();
        fallback.add("leave");
        fallback.add("party leave");
        fallback.add("party force_end");
        fallback.add("ff");
        fallback.add("eventleave");
        return fallback;
    }
}

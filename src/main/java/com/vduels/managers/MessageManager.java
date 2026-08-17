package com.vduels.managers;

import com.vduels.VDuels;
import com.vduels.util.Text;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads player-facing messages and titles from {@code messages.yml} so admins
 * can reword them without a rebuild. Every message has a built-in default (the
 * shipped wording), so a missing or deleted key still works. Supports a
 * {@code {prefix}} token and {@code {name}} style placeholders.
 */
public class MessageManager {

    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put("prefix", "&fⓘ ");

        DEFAULTS.put("general.no-permission", "{prefix}&cYou don't have permission to do that.");
        DEFAULTS.put("general.players-only", "{prefix}&cThis command can only be used by a player.");

        DEFAULTS.put("duel.usage", "{prefix}&cUsage: /duel <player>");
        DEFAULTS.put("duel.cannot-duel-self", "{prefix}&cYou cannot duel yourself.");
        DEFAULTS.put("duel.already-in-duel", "{prefix}&cYou are already in a duel.");
        DEFAULTS.put("duel.target-in-duel", "{prefix}&c{target} is already in a duel.");
        DEFAULTS.put("duel.kit-gone", "{prefix}&cThat kit no longer exists.");
        DEFAULTS.put("duel.no-kits", "{prefix}&cNo kits have been created yet.");
        DEFAULTS.put("duel.target-offline", "{prefix}&cPlayer {name} is not online.");
        DEFAULTS.put("duel.sent", "{prefix}&aChallenge sent to &e{target} &a(&e{kit}&a, first to &e{rounds}&a).");
        DEFAULTS.put("duel.starting", "{prefix}&aDuel starting against &e{opponent}&a!");
        DEFAULTS.put("duel.round", "{prefix}&fRound &b{round} &f- first to {roundsToWin}");

        // The "Duel:" card shown to both players when a match begins.
        DEFAULTS.put("duel.start.header", "&f&lDuel:");
        DEFAULTS.put("duel.start.opponent", "&a⚔ &7Opponent: &f{opponent}");
        DEFAULTS.put("duel.start.kit", "&a✎ &7Kit: &d{kit}");
        DEFAULTS.put("duel.start.rounds", "&a★ &7Rounds: &f{rounds}");
        DEFAULTS.put("duel.start.ranked", "&a☾ &7Ranked: &f{ranked}");
        DEFAULTS.put("duel.start.leave", "{prefix}&7Use &a/leave &7to leave the duel.");

        DEFAULTS.put("leave.not-in-duel", "{prefix}&cYou are not in a duel.");

        DEFAULTS.put("queue.in-duel", "{prefix}&cYou are already in a duel.");
        DEFAULTS.put("queue.kit-gone", "{prefix}&cThat kit no longer exists.");
        DEFAULTS.put("queue.already", "{prefix}&eYou are already queued for &f{kit}&e.");
        DEFAULTS.put("queue.joined", "{prefix}&aJoined the &e{kit} &aqueue (&e{queued}&7/&e{needed}&a).");
        DEFAULTS.put("queue.left", "{prefix}&aYou left the queue.");
        DEFAULTS.put("queue.not-queued", "{prefix}&cYou are not in a queue.");
        DEFAULTS.put("queue.no-arena", "{prefix}&cNo free arena is available for that kit right now.");
        DEFAULTS.put("duel.victory", "{prefix}&a&lVICTORY! &fYou won the duel (&e{yourScore} &f- &e{theirScore}&f).");
        DEFAULTS.put("duel.defeat", "{prefix}&c&lDEFEAT. &f{winner} won the duel.");

        DEFAULTS.put("accept.no-requests", "{prefix}&cYou have no pending duel requests.");
        DEFAULTS.put("accept.target-offline", "{prefix}&cThat player is not online.");
        DEFAULTS.put("accept.expired", "{prefix}&cThat duel request has expired.");
        DEFAULTS.put("accept.sender-offline", "{prefix}&cThat player is no longer online.");
        DEFAULTS.put("accept.one-in-duel", "{prefix}&cOne of you is already in a duel.");
        DEFAULTS.put("accept.no-arena", "{prefix}&cNo free arena is available for that kit right now.");

        DEFAULTS.put("request.header", "&6DUEL REQUEST FROM &e&l{sender}");
        DEFAULTS.put("request.kit", "&eKit: &e&l{kit}");
        DEFAULTS.put("request.rounds", "&eRounds: &f{rounds}");
        DEFAULTS.put("request.ranked", "&eRanked: &c&lDISABLED");
        DEFAULTS.put("request.click", "&6&l[CLICK HERE]");
        DEFAULTS.put("request.click-hover", "&aClick to accept the duel from &f{sender}");

        DEFAULTS.put("menu.select-kit-first", "{prefix}&cSelect a kit first.");
        DEFAULTS.put("menu.target-offline", "{prefix}&c{target} is no longer online.");
        DEFAULTS.put("menu.arena-incompatible", "{prefix}&cThat arena doesn't support the selected kit.");

        DEFAULTS.put("arena.cannot-edit", "{prefix}&cYou cannot edit an arena here.");

        DEFAULTS.put("titles.countdown.title", "&e{seconds}");
        DEFAULTS.put("titles.countdown.subtitle", "");
        DEFAULTS.put("titles.fight.title", "&eFIGHT ⚔ !");
        DEFAULTS.put("titles.fight.subtitle", "");
        DEFAULTS.put("titles.round-won.title", "&aRound won!");
        DEFAULTS.put("titles.round-won.subtitle", "&e{yourScore} &f- &e{theirScore}");
        DEFAULTS.put("titles.round-lost.title", "&cRound lost");
        DEFAULTS.put("titles.round-lost.subtitle", "&e{yourScore} &f- &e{theirScore}");
        DEFAULTS.put("titles.victory.title", "&a&lVICTORY");
        DEFAULTS.put("titles.victory.subtitle", "");
        DEFAULTS.put("titles.defeat.title", "&c&lDEFEAT");
        DEFAULTS.put("titles.defeat.subtitle", "");
    }

    private final VDuels plugin;
    private final File file;
    private YamlConfiguration config;
    private String prefix;

    public MessageManager(VDuels plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            writeDefaults();
        }
        config = YamlConfiguration.loadConfiguration(file);
        prefix = raw("prefix");
    }

    /** Writes all defaults to messages.yml so admins have a file to edit. */
    private void writeDefaults() {
        YamlConfiguration out = new YamlConfiguration();
        for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
            out.set(entry.getKey(), entry.getValue());
        }
        try {
            out.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write messages.yml: " + e.getMessage());
        }
    }

    private String raw(String key) {
        String value = config == null ? null : config.getString(key);
        if (value == null) {
            value = DEFAULTS.get(key);
        }
        return value == null ? "&cmissing message: " + key : value;
    }

    /**
     * Returns a fully coloured message. Placeholders are passed as alternating
     * name/value pairs, e.g. {@code get("duel.sent", "target", name)}.
     */
    public String get(String key, String... placeholders) {
        String value = raw(key).replace("{prefix}", prefix == null ? "" : prefix);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            value = value.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return Text.color(value);
    }
}

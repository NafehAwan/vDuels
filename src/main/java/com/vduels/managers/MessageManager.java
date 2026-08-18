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

        DEFAULTS.put("spectate.not-found", "{prefix}&cPlayer {name} is not online.");
        DEFAULTS.put("spectate.in-duel", "{prefix}&cYou can't spectate while in a duel.");
        DEFAULTS.put("spectate.self", "{prefix}&cYou can't spectate yourself.");
        DEFAULTS.put("spectate.now", "{prefix}&aNow spectating &e{target}&a. Type &e/spectate&a to stop.");
        DEFAULTS.put("spectate.stopped", "{prefix}&aStopped spectating.");
        DEFAULTS.put("spectate.not-spectating", "{prefix}&cYou are not spectating anyone.");
        DEFAULTS.put("spectate.fight-ended", "{prefix}&eThe fight ended - returning you back.");
        // Normal-font gray notices (only the name varies); rendered as-is.
        DEFAULTS.put("spectate.started-watching", "{name} has started spectating you.");
        DEFAULTS.put("spectate.stopped-watching", "{name} has stopped spectating you.");

        DEFAULTS.put("queue.in-duel", "{prefix}&cYou are already in a duel.");
        DEFAULTS.put("queue.kit-gone", "{prefix}&cThat kit no longer exists.");
        DEFAULTS.put("queue.left", "{prefix}&aYou left every queue.");
        DEFAULTS.put("queue.not-queued", "{prefix}&cYou are not in a queue.");
        DEFAULTS.put("queue.no-arena", "{prefix}&cNo free arena is available for that kit right now.");
        // Normal-font, gray lead-ins; only the kit's display name is styled.
        DEFAULTS.put("queue.joined-text", "You have been queued to ");
        DEFAULTS.put("queue.left-text", "You have left the queue for ");
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


        DEFAULTS.put("party.already-in-party", "{prefix}&cYou're already in a party.");
        DEFAULTS.put("party.created", "{prefix}&aParty created! Use &f/party invite <player> &ato add people.");
        DEFAULTS.put("party.usage-invite", "{prefix}&cUsage: /party invite <player>");
        DEFAULTS.put("party.invite-failed", "{prefix}&cCouldn't invite {target} - they may already be in a party.");
        DEFAULTS.put("party.invite-sent", "{prefix}&aInvite sent to {target}.");
        DEFAULTS.put("party.invite-received", "{prefix}&e{inviter} &finvited you to their party! &aType /party join {inviter} &fto accept.");
        DEFAULTS.put("party.usage-join", "{prefix}&cUsage: /party join <leader>");
        DEFAULTS.put("party.no-pending-invite", "{prefix}&cYou don't have a pending invite from that player.");
        DEFAULTS.put("party.joined", "{prefix}&aYou joined the party!");
        DEFAULTS.put("party.declined", "{prefix}&7You declined the invite.");
        DEFAULTS.put("party.invite-was-declined", "{prefix}&e{player} &fdeclined your party invite.");
        DEFAULTS.put("party.not-in-party", "{prefix}&cYou're not in a party.");
        DEFAULTS.put("party.usage-kick", "{prefix}&cUsage: /party kick <player>");
        DEFAULTS.put("party.kick-failed", "{prefix}&cCouldn't kick {target}.");
        DEFAULTS.put("party.not-leader", "{prefix}&cOnly the party leader can do that.");
        DEFAULTS.put("party.member-joined", "{prefix}&e{player} &fjoined the party.");
        DEFAULTS.put("party.member-left", "{prefix}&e{player} &fleft the party.");
        DEFAULTS.put("party.you-left", "{prefix}&7You left the party.");
        DEFAULTS.put("party.you-were-kicked", "{prefix}&cYou were kicked from the party.");
        DEFAULTS.put("party.member-kicked", "{prefix}&e{player} &fwas kicked from the party.");
        DEFAULTS.put("party.disbanded", "{prefix}&cThe party was disbanded.");
        DEFAULTS.put("party.ffa.need-players", "{prefix}&cYou need at least 2 online party members to start a FFA.");
        DEFAULTS.put("party.ffa.no-arena", "{prefix}&cNo free arena supports this kit right now.");
        DEFAULTS.put("party.ffa.round", "{prefix}&fRound &e{round}&f/&e{roundsToWin} &f- last one standing wins!");
        DEFAULTS.put("party.ffa.eliminated", "{prefix}&cYou were eliminated! Spectating until the round ends.");
        DEFAULTS.put("party.ffa.player-eliminated", "{prefix}&e{player} &fwas eliminated! &e{remaining} &fplayer(s) remaining.");
        DEFAULTS.put("party.ffa.you-won", "{prefix}&a&lYOU WON THE PARTY FFA!");
        DEFAULTS.put("party.ffa.match-over", "{prefix}&fThe party FFA is over. &e{winner} &fwon!");
        DEFAULTS.put("party.match-force-ended", "{prefix}&7The match was force-ended by the party leader.");
        DEFAULTS.put("party.now-public", "{prefix}&aYour party is now public - anyone can /party join you.");
        DEFAULTS.put("party.now-private", "{prefix}&7Your party is now private - invite only.");
        DEFAULTS.put("party.usage-transfer", "{prefix}&cUsage: /party transfer <player>");
        DEFAULTS.put("party.transfer-failed", "{prefix}&cCouldn't transfer leadership to {target} - are they in your party?");
        DEFAULTS.put("party.leadership-transferred", "{prefix}&e{player} &fis now the party leader.");
        DEFAULTS.put("party.nothing-to-end", "{prefix}&cYour party isn't in a match right now.");
        DEFAULTS.put("party.split.need-both-teams", "{prefix}&cAssign at least one online player to each of red and blue first.");
        DEFAULTS.put("party.team.round", "{prefix}&fRound &e{round}&f/&e{roundsToWin} &f- eliminate the other team!");
        DEFAULTS.put("party.team.eliminated", "{prefix}&cYou were eliminated! Spectating until the round ends.");
        DEFAULTS.put("party.team.player-eliminated", "{prefix}&e{player} &fwas eliminated!");
        DEFAULTS.put("party.team.you-won", "{prefix}&a&lYOUR TEAM WON!");
        DEFAULTS.put("party.team.you-lost", "{prefix}&c&lYOUR TEAM LOST.");
        DEFAULTS.put("party.duel.sent", "{prefix}&aChallenge sent to {target}'s party!");
        DEFAULTS.put("party.duel.request-header", "&e{sender}&f's party has challenged your party to a duel!");
        DEFAULTS.put("party.duel.declined", "{prefix}&7You declined the party challenge.");
        DEFAULTS.put("party.duel.was-declined", "{prefix}&e{player}&f's party declined your challenge.");
        DEFAULTS.put("party.usage-duelaccept", "{prefix}&cUsage: /party duelaccept <leader>");

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

    /** The stored/default text for a key, with no colour or small-caps applied. */
    public String raw(String key) {
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

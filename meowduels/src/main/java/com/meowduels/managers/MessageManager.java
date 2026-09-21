/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package com.meowduels.managers;

import com.meowduels.MeowDuels;
import com.meowduels.util.Colors;
import com.meowduels.util.Text;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

public class MessageManager {
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<String, String>();
    private final MeowDuels plugin;
    private final File file;
    private YamlConfiguration config;
    private String prefix;

    public MessageManager(MeowDuels plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        this.reload();
    }

    public void reload() {
        if (!this.file.exists()) {
            this.writeDefaults();
        }
        this.config = YamlConfiguration.loadConfiguration((File)this.file);
        this.fillMissing();
        this.prefix = this.raw("prefix");
    }

    private void writeDefaults() {
        YamlConfiguration out = new YamlConfiguration();
        for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
            out.set(entry.getKey(), (Object)entry.getValue());
        }
        try {
            out.save(this.file);
        }
        catch (IOException e) {
            this.plugin.getLogger().severe("Failed to write messages.yml: " + e.getMessage());
        }
    }

    /**
     * Adds keys this build introduced to an existing messages.yml.
     *
     * <p>writeDefaults only runs when the file is absent, so on an updated
     * server every new message would live in DEFAULTS and never be editable.
     * Existing values are never touched - only genuinely missing keys are
     * written, and the file is only saved when something was added.
     */
    private void fillMissing() {
        if (this.config == null) {
            return;
        }
        boolean added = false;
        for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
            if (this.config.contains(entry.getKey())) continue;
            this.config.set(entry.getKey(), (Object)entry.getValue());
            added = true;
        }
        if (!added) {
            return;
        }
        try {
            this.config.save(this.file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to update messages.yml: " + e.getMessage());
        }
    }

    public String raw(String key) {
        String value;
        String string = value = this.config == null ? null : this.config.getString(key);
        if (value == null) {
            value = DEFAULTS.get(key);
        }
        return value == null ? "&cmissing message: " + key : value;
    }

    public String get(String key, String ... placeholders) {
        String value = this.raw(key).replace("{prefix}", this.prefix == null ? "" : this.prefix);
        int i = 0;
        while (i + 1 < placeholders.length) {
            value = value.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            i += 2;
        }
        if (key.startsWith("request.") || key.startsWith("titles.")) {
            return Text.color(value);
        }
        if (key.startsWith("party.") || key.startsWith("event.")) {
            // Colors.toSection rather than colorNormal: these lines carry hex
            // and gradients, which the &-only translator leaves as literal text.
            return Colors.toSection(value);
        }
        if (key.startsWith("match.") || key.startsWith("round.") || key.startsWith("death.")) {
            return Text.colorNormal(value);
        }
        // Everything else is deliberately flattened to grey - see Text.grayEmoji.
        // Anything that wants its own colours needs a branch above, or it loses
        // them here without a word.
        return Text.grayEmoji(value);
    }

    static {
        DEFAULTS.put("prefix", "&d\u24d8 ");
        DEFAULTS.put("general.no-permission", "{prefix}&cYou don't have permission to do that.");
        DEFAULTS.put("general.players-only", "{prefix}&cThis command can only be used by a player.");
        DEFAULTS.put("duel.usage", "{prefix}&cUsage: /duel <player>");
        DEFAULTS.put("duel.cannot-duel-self", "{prefix}&cYou cannot duel yourself.");
        DEFAULTS.put("duel.already-in-duel", "{prefix}&cYou are already in a duel.");
        DEFAULTS.put("duel.target-in-duel", "{prefix}&c{target} is already in a duel.");
        DEFAULTS.put("duel.kit-gone", "{prefix}&cThat kit no longer exists.");
        DEFAULTS.put("duel.arena-booked", "{prefix}&cSorry, the Arena was booked.");
        DEFAULTS.put("duel.requests-off", "{prefix}&c{target} has duel requests turned off.");
        DEFAULTS.put("duel.no-arenas", "{prefix}&cNo free arena is available right now - try again shortly.");
        DEFAULTS.put("duel.no-kits", "{prefix}&cNo kits have been created yet.");
        DEFAULTS.put("duel.target-offline", "{prefix}&cPlayer {name} is not online.");
        DEFAULTS.put("duel.sent", "{prefix}&aChallenge sent to &e{target} &a(&e{kit}&a, first to &e{rounds}&a).");
        DEFAULTS.put("duel.starting", "{prefix}&aDuel starting against &e{opponent}&a!");
        DEFAULTS.put("duel.round", "{prefix}&fRound &d{round} &f- first to {roundsToWin}");
        DEFAULTS.put("duel.start.header", "&f&lDuel:");
        DEFAULTS.put("duel.start.opponent", "&a\u2694 &7Opponent: &f{opponent}");
        DEFAULTS.put("duel.start.kit", "&a\u270e &7Kit: &d{kit}");
        DEFAULTS.put("duel.start.rounds", "&a\u2605 &7Rounds: &f{rounds}");
        DEFAULTS.put("duel.start.ranked", "&a\u263e &7Ranked: &f{ranked}");
        DEFAULTS.put("duel.start.leave", "{prefix}&7Use &a/leave &7to leave the duel.");
        DEFAULTS.put("leave.not-in-duel", "{prefix}&cYou are not in a duel.");
        DEFAULTS.put("spectate.not-found", "{prefix}&cPlayer {name} is not online.");
        DEFAULTS.put("spectate.in-duel", "{prefix}&cYou can't spectate while in a duel.");
        DEFAULTS.put("spectate.self", "{prefix}&cYou can't spectate yourself.");
        DEFAULTS.put("spectate.target-not-in-fight", "{prefix}&c{target} is not in a fight right now.");
        DEFAULTS.put("spectate.not-allowed", "{prefix}&cSpectating is disabled in this arena.");
        DEFAULTS.put("spectate.now", "{prefix}&aNow spectating &e{target}&a. Type &e/spectate&a to stop.");
        DEFAULTS.put("spectate.stopped", "{prefix}&aStopped spectating.");
        DEFAULTS.put("spectate.not-spectating", "{prefix}&cYou are not spectating anyone.");
        DEFAULTS.put("spectate.fight-ended", "{prefix}&eThe fight ended - returning you back.");
        DEFAULTS.put("spectate.started-watching", "{name} has started spectating you.");
        DEFAULTS.put("spectate.stopped-watching", "{name} has stopped spectating you.");
        DEFAULTS.put("queue.in-duel", "{prefix}&cYou are already in a duel.");
        DEFAULTS.put("queue.kit-gone", "{prefix}&cThat kit no longer exists.");
        DEFAULTS.put("queue.left", "{prefix}&aYou left every queue.");
        DEFAULTS.put("queue.not-queued", "{prefix}&cYou are not in a queue.");
        DEFAULTS.put("queue.no-arena", "{prefix}&cNo free arena is available for that kit right now.");
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
        DEFAULTS.put("arena.cannot-edit", "{prefix}&cYou cannot edit an arena here.");
        DEFAULTS.put("titles.match-found.title", "&cMATCH FOUND!");
        DEFAULTS.put("titles.match-found.subtitle", "&7Preparing your arena...");
        DEFAULTS.put("titles.countdown.title", "&e{seconds}");
        DEFAULTS.put("titles.countdown.subtitle", "");
        DEFAULTS.put("titles.fight.title", "&eFIGHT \u2694 !");
        DEFAULTS.put("titles.fight.subtitle", "");
        DEFAULTS.put("titles.round-won.title", "&a&lVICTORY");
        DEFAULTS.put("titles.round-won.subtitle", "&7Score &8\u00bb &a{yourScore} &7- &c{theirScore}");
        DEFAULTS.put("titles.round-lost.title", "&c&lDEFEAT");
        DEFAULTS.put("titles.round-lost.subtitle", "&7Score &8\u00bb &a{yourScore} &7- &c{theirScore}");
        DEFAULTS.put("round.won-chat", "&6\u2604 &aRound {round} won &8\u00bb &f{score} &7- &f{opponent_score}");
        DEFAULTS.put("round.lost-chat", "&4\u2620 &cRound {round} lost &8\u00bb &f{score} &7- &f{opponent_score}");
        DEFAULTS.put("event.announce", "&a{host} is hosting an event of &e{kit}&a. Do &f/event &ato join. &7Players: &f{slots} &8| &7Time Left: &c{time}");
        DEFAULTS.put("event.started", "&a&lThe Event has started!");
        DEFAULTS.put("event.full", "&cThe event is full ({slots} slots).");
        DEFAULTS.put("event.spectate-start", "&7You are now spectating the event. &f/eventleave &7to stop.");
        DEFAULTS.put("event.spectate-playing", "&cYou're playing in the event - you can't spectate it.");
        DEFAULTS.put("event.already-spectating", "&cYou're already spectating the event.");
        DEFAULTS.put("party.invite-header", "  &d\u2605 &f&lPARTY INVITE");
        DEFAULTS.put("party.invite-from", "  &7{leader} &7wants you in their party.");
        DEFAULTS.put("party.invite-info", "  &8Members: &f{members}");
        DEFAULTS.put("party.invite-accept", "  &a&l[ JOIN ]");
        DEFAULTS.put("party.invite-gap", "   ");
        DEFAULTS.put("party.invite-decline", "&c&l[ DECLINE ]");
        DEFAULTS.put("party.invite-accept-hover", "&aJoin {leader}'s party");
        DEFAULTS.put("party.invite-decline-hover", "&cTurn down {leader}'s invite");
        DEFAULTS.put("party.invite-sent", "&aInvited &f{player}&a - waiting for them to accept.");
        DEFAULTS.put("party.invite-declined", "&7You turned down &f{leader}&7's invite.");
        DEFAULTS.put("party.invite-declined-by", "&7{player} &7turned down your party invite.");
        DEFAULTS.put("party.match-started", "&aParty match started &8\u2022 &f{count} &7fighting on &f{arena}");
        DEFAULTS.put("party.countdown-title", "&d{seconds}");
        DEFAULTS.put("party.countdown-subtitle", "&7Free-for-all \u2022 last one standing");
        DEFAULTS.put("party.countdown-go", "&a&lFIGHT!");
        DEFAULTS.put("party.countdown-subtitle-split", "<#8E959D>\u1d1b\u1d07\u1d00\u1d0d \u1d20\ua731 \u1d1b\u1d07\u1d00\u1d0d <dark_gray>\u2022 <#8E959D>\u028f\u1d0f\u1d1c'\u0280\u1d07 \u1d0f\u0274 {team}");
        DEFAULTS.put("party.countdown-subtitle-split-noteam", "<#8E959D>\u1d1b\u1d07\u1d00\u1d0d \u1d20\ua731 \u1d1b\u1d07\u1d00\u1d0d <dark_gray>\u2022 <#8E959D>\u1d21\u026a\u1d18\u1d07 \u1d1b\u029c\u1d07 \u1d0f\u1d1b\u029c\u1d07\u0280 \ua731\u026a\u1d05\u1d07");
        DEFAULTS.put("party.kill-pvp", "<#FF3B57>\u2620 <#FF8A93>{victim} <#6B7079>was killed by <#7CFF6B>{killer} <dark_gray>\u2022 <#E6E8EB>{alive} <#6B7079>left");
        DEFAULTS.put("party.kill-generic", "<#FF3B57>\u2620 <#FF8A93>{victim} <#6B7079>died <dark_gray>\u2022 <#E6E8EB>{alive} <#6B7079>left");
        DEFAULTS.put("party.team-aqua", "<#7DE2FF>\u1d1b\u1d07\u1d00\u1d0d \u1d00\ua7af\u1d1c\u1d00");
        DEFAULTS.put("party.team-red", "<#FF8A8A>\u1d1b\u1d07\u1d00\u1d0d \u0280\u1d07\u1d05");
        DEFAULTS.put("party.teams-line", "<#8E959D>\u1d1b\u1d07\u1d00\u1d0d\ua731 <dark_gray>\u00bb <#7DE2FF>{aqua} <dark_gray>\u1d20\ua731 <#FF8A8A>{red}");
        DEFAULTS.put("party.team-winner", "<#FFD65C>\u2605 {team} <#6B7079>won the party match!");
        DEFAULTS.put("party.winner", "<#FFD65C>\u2605 <gradient:#FFE9A3:#FFB02E>{winner}</gradient> <#6B7079>won the party match!");
        DEFAULTS.put("party.no-winner", "&7The party match ended with no winner.");
        DEFAULTS.put("party.force-ended", "<#FF8A93>\u2716 <#6B7079>{leader} ended the party match.");
        DEFAULTS.put("party.duels-started", "<#7DE2FF>\u2694 <#6B7079>ᴘᴀʀᴛʏ ᴅᴜᴇʟꜱ ᴠꜱ <#E6E8EB>{party} <dark_gray>\u2022 <#E6E8EB>{pairs} <#6B7079>ᴘᴀɪʀꜱ <dark_gray>\u2022 <#6B7079>ꜰɪʀꜱᴛ ᴛᴏ <#E6E8EB>{rounds}");
        DEFAULTS.put("party.win-title", "&6&lVICTORY");
        DEFAULTS.put("party.win-subtitle", "&7Last one standing");
        DEFAULTS.put("party.win-subtitle-split", "<#8E959D>\u028f\u1d0f\u1d1c\u0280 \u1d1b\u1d07\u1d00\u1d0d \u1d21\u026a\u1d18\u1d07\u1d05 \u1d1b\u029c\u1d07\u1d0d \u1d0f\u1d1c\u1d1b");
        DEFAULTS.put("event.kill.pvp", "<#FF3B57>\u2620 <#FF8A93>{victim} <#6B7079>was eliminated by <#7CFF6B>{killer} <dark_gray>\u2022 <#E6E8EB>{alive} <#6B7079>left");
        DEFAULTS.put("event.kill.generic", "<#FF3B57>\u2620 <#FF8A93>{victim} <#6B7079>was eliminated <dark_gray>\u2022 <#E6E8EB>{alive} <#6B7079>left");
        DEFAULTS.put("event.win", "&a&l{winner} has won the event! &4\u2620");
        DEFAULTS.put("event.joined", "&aYou joined the event! Wait for it to start.");
        DEFAULTS.put("event.left", "&7You left the event.");
        DEFAULTS.put("event.no-permission", "&cYou don't have permission to host events.");
        DEFAULTS.put("event.already-running", "&cAn event is already running.");
        DEFAULTS.put("event.no-kit", "&cNo kit named that exists.");
        DEFAULTS.put("event.no-arena", "&cNo free arena is available to host an event right now.");
        DEFAULTS.put("event.no-event-spawn", "&cArena {arena} has no event spawn set - set it in /arena first.");
        DEFAULTS.put("event.arena-busy", "&cArena {arena} is in use right now.");
        DEFAULTS.put("event.none", "&cThere's no event to join right now.");
        DEFAULTS.put("event.already-joined", "&cYou already joined the event.");
        DEFAULTS.put("event.busy", "&cLeave your duel, queue, or spectate first (&f/leave&c) to join the event.");
        DEFAULTS.put("event.started-cannot-join", "&cThe event already started - you can't join now.");
        DEFAULTS.put("event.host-usage", "&cUsage: &f/event host <kit> <minutes> [slots]&c, &f/event host force_start&c or &f/event host force_end");
        DEFAULTS.put("event.force-ended", "&cThe event has been ended by a host.");
        DEFAULTS.put("event.countdown-title", "<gradient:#FFD65C:#FF8A2E>{seconds}</gradient>");
        DEFAULTS.put("event.countdown-subtitle", "<#8E959D>\u1d1b\u029c\u1d07 \u1d07\u1d20\u1d07\u0274\u1d1b \u026a\ua731 \u1d00\u0299\u1d0f\u1d1c\u1d1b \u1d1b\u1d0f \ua731\u1d1b\u1d00\u0280\u1d1b");
        DEFAULTS.put("event.start-title", "<gradient:#7CFF6B:#1FA32F>\ua730\u026a\u0262\u029c\u1d1b</gradient>");
        DEFAULTS.put("event.start-subtitle", "<#8E959D>\u029f\u1d00\ua731\u1d1b \u1d0f\u0274\u1d07 \ua731\u1d1b\u1d00\u0274\u1d05\u026a\u0274\u0262 \u1d21\u026a\u0274\ua731");
        DEFAULTS.put("event.win-title", "<gradient:#FFE9A3:#FFB02E>\u1d20\u026a\u1d04\u1d1b\u1d0f\u0280\u028f</gradient>");
        DEFAULTS.put("event.win-subtitle", "<#8E959D>\u029f\u1d00\ua731\u1d1b \u1d0f\u0274\u1d07 \ua731\u1d1b\u1d00\u0274\u1d05\u026a\u0274\u0262");
        DEFAULTS.put("event.out-title", "<gradient:#FF8A8A:#C0392B>\u1d07\u029f\u026a\u1d0d\u026a\u0274\u1d00\u1d1b\u1d07\u1d05</gradient>");
        DEFAULTS.put("event.out-subtitle", "<#8E959D>\u028f\u1d0f\u1d1c\u0280 \u1d0b\u026a\u029f\u029f\ua731 <dark_gray>\u00bb <#E6E8EB>{kills}");
        DEFAULTS.put("countdown.player-ready", "&7{player} is Ready !");
        DEFAULTS.put("countdown.forced", "&7\u267b Duel Started Forcefully !");
        DEFAULTS.put("duel.forfeit-broadcast", "&7Someone got ragebaited! &4\u2620");
        DEFAULTS.put("match.toxic.1", "&6\u2604 &a{winner} &7obliterated &c{loser} &4\u2620 &8\u2022 &f{winner_score}&7-&f{loser_score}");
        DEFAULTS.put("match.toxic.2", "&4\u2620 &c{loser} &7got sent to spawn by &6\u2604 &a{winner} &8(&f{winner_score}&7-&f{loser_score}&8)");
        DEFAULTS.put("match.toxic.3", "&6\u2604 &a{winner} &7ended &c{loser}&7's whole career &4\u2620 &8\u2022 &f{winner_score}&7-&f{loser_score}");
        DEFAULTS.put("match.toxic.4", "&4\u2620 &c{loser} &7just got ratio'd by &6\u2604 &a{winner} &8[&f{winner_score}&7-&f{loser_score}&8]");
        DEFAULTS.put("match.toxic.5", "&6\u2604 &a{winner} &7made &c{loser} &4\u2620 &7hit the respawn button &8\u2022 &f{winner_score}&7-&f{loser_score}");
        DEFAULTS.put("match.toxic.6", "&4\u2620 &c{loser} &7got absolutely clapped by &6\u2604 &a{winner} &8(&f{winner_score}&7-&f{loser_score}&8)");
        DEFAULTS.put("match.toxic.7", "&6\u2604 &a{winner} &7turned &c{loser} &4\u2620 &7into a corpse &8\u2022 &f{winner_score}&7-&f{loser_score}");
        DEFAULTS.put("match.toxic.8", "&4\u2620 &c{loser} &7should uninstall \u2014 &6\u2604 &a{winner} &7said gg &8[&f{winner_score}&7-&f{loser_score}&8]");
        DEFAULTS.put("match.toxic.9", "&6\u2604 &a{winner} &7dropped &c{loser} &4\u2620 &7like a bad habit &8\u2022 &f{winner_score}&7-&f{loser_score}");
        DEFAULTS.put("match.toxic.10", "&4\u2620 &c{loser} &7got humbled by &6\u2604 &a{winner} &8(&f{winner_score}&7-&f{loser_score}&8)");
        DEFAULTS.put("death.duel.1", "&4\u2620 &c{victim} &7was killed by &a{killer} &8\u2022 &f{killer_score}&7-&f{victim_score}");
        DEFAULTS.put("death.duel.2", "&4\u2620 &c{victim} &7got dropped by &a{killer} &8\u2022 &f{killer_score}&7-&f{victim_score}");
        DEFAULTS.put("death.duel.3", "&a\u2694 &a{killer} &7cut down &c{victim} &8\u2022 &f{killer_score}&7-&f{victim_score}");
        DEFAULTS.put("death.duel.4", "&4\u2620 &c{victim} &7got sent flying by &a{killer} &8\u2022 &f{killer_score}&7-&f{victim_score}");
        DEFAULTS.put("death.duel.5", "&a\u2694 &a{killer} &7clapped &c{victim} &8\u2022 &f{killer_score}&7-&f{victim_score}");
        DEFAULTS.put("death.normal.pvp.1", "&4\u2620 &c{victim} &7was slain by &a{killer}");
        DEFAULTS.put("death.normal.pvp.2", "&4\u2620 &c{victim} &7got dropped by &a{killer}");
        DEFAULTS.put("death.normal.pvp.3", "&a\u2694 &a{killer} &7ended &c{victim}");
        DEFAULTS.put("death.normal.pvp.4", "&4\u2620 &c{victim} &7couldn't handle &a{killer}");
        DEFAULTS.put("death.normal.generic", "&4\u2620 &c{victim} &7died");
        DEFAULTS.put("titles.victory.title", "&a&lVICTORY");
        DEFAULTS.put("titles.victory.subtitle", "&7Score &8\u00bb &a{yourScore} &7- &c{theirScore}");
        DEFAULTS.put("titles.defeat.title", "&c&lDEFEAT");
        DEFAULTS.put("titles.defeat.subtitle", "&7Score &8\u00bb &a{yourScore} &7- &c{theirScore}");
    }
}


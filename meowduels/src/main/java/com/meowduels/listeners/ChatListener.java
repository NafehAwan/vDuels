package com.meowduels.listeners;

import com.meowduels.MeowDuels;
import com.meowduels.model.ActiveDuel;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Chat inside a match stays inside the match.
 *
 * <p>A fight is a conversation between the people in it. Lobby chatter running
 * through the middle of it is noise to them, and their callouts are noise to
 * the lobby, so each match gets its own room: the people fighting, plus anyone
 * watching them.
 *
 * <p>This is done by removing viewers, not by rewriting the line. Whatever
 * permissions or chat plugin the server runs still formats the message - a
 * fighter keeps their rank, their prefix and their colour, exactly as they
 * look everywhere else. The plugin's only opinion is about who may read it.
 *
 * <p>Both directions, from one rule. A message is delivered to a viewer only
 * when neither end objects: a sender inside a match with isolation on reaches
 * only their own match, and a viewer inside a match with isolation on hears
 * only their own match. Turning the setting off puts that player back in the
 * shared room in both directions, which is the whole point of it being theirs
 * to set.
 *
 * <p>Nobody is left wondering why they were ignored: an outsider who types a
 * fighter's name is told, once, that the fighter is in a match and cannot see
 * it.
 */
public class ChatListener
implements Listener {
    /** At most this many "they can't see you" lines for one message. */
    private static final int MAX_NOTICES = 3;

    private final MeowDuels plugin;

    public ChatListener(MeowDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onChat(AsyncChatEvent event) {
        if (!this.plugin.getConfig().getBoolean("chat.isolated", true)) {
            return;
        }
        Player sender = event.getPlayer();
        Object senderMatch = this.matchOf(sender.getUniqueId());
        Set<Audience> viewers = event.viewers();
        if (viewers != null) {
            try {
                Iterator<Audience> it = viewers.iterator();
                while (it.hasNext()) {
                    if (this.canRead(sender, senderMatch, it.next())) continue;
                    it.remove();
                }
            }
            catch (UnsupportedOperationException e) {
                // A chat plugin ahead of us handed back a fixed audience. Its
                // routing wins; ours would only half-apply.
                return;
            }
        }
        this.noticeMentions(sender, senderMatch, event.message());
    }

    /**
     * Whether this viewer is in the same room as the sender.
     *
     * <p>Console and anything else that is not a player keeps everything: log
     * files and relays are not in a match and should not have holes in them.
     */
    private boolean canRead(Player sender, Object senderMatch, Audience viewer) {
        if (!(viewer instanceof Player)) {
            return true;
        }
        Player reader = (Player)viewer;
        if (reader.getUniqueId().equals(sender.getUniqueId())) {
            // You always hear yourself. Dropping the sender from their own
            // audience is how a message looks like it silently failed.
            return true;
        }
        Object readerMatch = this.matchOf(reader.getUniqueId());
        if (senderMatch != null && readerMatch != senderMatch && this.isolated(sender.getUniqueId())) {
            return false;
        }
        return readerMatch == null || readerMatch == senderMatch || !this.isolated(reader.getUniqueId());
    }

    /**
     * The match this player belongs to for chat purposes, or null.
     *
     * <p>Identity, not equality: a duel is its ActiveDuel and a party match is
     * its host party, and two people are in the same room when they come back
     * with the same object. A spectator borrows the room of whoever they are
     * watching, which is what makes spectator chat land with the fight instead
     * of with the lobby.
     */
    private Object matchOf(UUID id) {
        ActiveDuel duel = this.plugin.getDuelManager().getDuel(id);
        if (duel != null) {
            return duel;
        }
        Object party = this.plugin.getPartyManager().matchOf(id);
        if (party != null) {
            return party;
        }
        UUID watched = this.plugin.getSpectateManager().getWatchedTarget(id);
        if (watched == null) {
            return null;
        }
        ActiveDuel theirs = this.plugin.getDuelManager().getDuel(watched);
        return theirs != null ? theirs : this.plugin.getPartyManager().matchOf(watched);
    }

    private boolean isolated(UUID id) {
        return this.plugin.getPlayerSettings().isIsolatedChat(id);
    }

    /**
     * Tells the sender about anyone they named who cannot read it.
     *
     * <p>Only for people in a different room, and only for people whose own
     * setting is what is hiding the message - if they left isolation off, they
     * saw it, and there is nothing to report.
     */
    private void noticeMentions(Player sender, Object senderMatch, net.kyori.adventure.text.Component message) {
        if (message == null) {
            return;
        }
        // MiniMessage rather than the plain-text serializer: this codebase
        // already proves MiniMessage's interface kind at runtime, and the only
        // difference here is that styling arrives as <tags>. A tag cannot
        // appear inside a player name, and it reads as a word boundary, which
        // is what the matcher below wants anyway.
        String text = MiniMessage.miniMessage().serialize(message);
        if (text == null || text.isEmpty()) {
            return;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        ArrayList<String> lines = new ArrayList<String>();
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (lines.size() >= MAX_NOTICES) {
                break;
            }
            UUID id = other.getUniqueId();
            if (id.equals(sender.getUniqueId()) || !this.isolated(id)) continue;
            Object theirMatch = this.matchOf(id);
            if (theirMatch == null || theirMatch == senderMatch) continue;
            if (!ChatListener.mentions(lower, other.getName().toLowerCase(Locale.ROOT))) continue;
            lines.add(this.plugin.messages().get("chat.isolated-notice",
                    "target", other.getName(),
                    "kind", this.plugin.messages().raw(theirMatch instanceof ActiveDuel
                            ? "chat.kind-duel" : "chat.kind-party")));
        }
        if (lines.isEmpty()) {
            return;
        }
        // Next tick, on the main thread: the notice belongs after the message
        // it is about, and this runs on the chat thread where it would land
        // before it.
        List<String> out = lines;
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            if (!sender.isOnline()) {
                return;
            }
            for (String line : out) {
                sender.sendMessage(line);
            }
        });
    }

    /**
     * Whether this text names that player, ignoring case.
     *
     * <p>Whole word only, so "Nafeh" does not match inside "Nafehawan", while
     * "@Nafeh", "Nafeh," and "nafeh?" all do. Anything that could be part of a
     * Minecraft name - letters, digits and underscore - counts as still being
     * inside the word.
     */
    private static boolean mentions(String text, String name) {
        if (name.isEmpty()) {
            return false;
        }
        int from = 0;
        while (true) {
            int at = text.indexOf(name, from);
            if (at < 0) {
                return false;
            }
            int end = at + name.length();
            boolean left = at == 0 || !ChatListener.isNameChar(text.charAt(at - 1));
            boolean right = end >= text.length() || !ChatListener.isNameChar(text.charAt(end));
            if (left && right) {
                return true;
            }
            from = at + 1;
        }
    }

    private static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}

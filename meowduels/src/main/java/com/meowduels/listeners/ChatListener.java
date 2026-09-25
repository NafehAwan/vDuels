package com.meowduels.listeners;

import com.meowduels.MeowDuels;
import com.meowduels.model.ActiveDuel;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
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
 * <p>A full name is a way through. Anyone whose name appears in the message,
 * spelled out in full, is delivered it whichever room they are in - and only
 * them, because everyone else on the far side of the wall is still removed. It
 * is how you get one line to somebody mid-fight without shouting it at the
 * lobby, and it works in both directions.
 *
 * <p>Nobody is left wondering. Type a fighter's name and you are told, once,
 * that they are in a match, that they got this line because you named them,
 * and that /msg is the way to have an actual conversation.
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
        // Worked out before anything is removed, because being named is what
        // decides whether a viewer survives the filter.
        Set<UUID> named = this.namedIn(event.message(), sender);
        Set<Audience> viewers = event.viewers();
        if (viewers != null) {
            try {
                Iterator<Audience> it = viewers.iterator();
                while (it.hasNext()) {
                    if (this.canRead(sender, senderMatch, named, it.next())) continue;
                    it.remove();
                }
            }
            catch (UnsupportedOperationException e) {
                // A chat plugin ahead of us handed back a fixed audience. Its
                // routing wins; ours would only half-apply.
                return;
            }
        }
        this.noticeMentions(sender, senderMatch, named);
    }

    /**
     * Whether this viewer is in the same room as the sender.
     *
     * <p>Console and anything else that is not a player keeps everything: log
     * files and relays are not in a match and should not have holes in them.
     */
    private boolean canRead(Player sender, Object senderMatch, Set<UUID> named, Audience viewer) {
        if (!(viewer instanceof Player)) {
            return true;
        }
        Player reader = (Player)viewer;
        if (reader.getUniqueId().equals(sender.getUniqueId())) {
            // You always hear yourself. Dropping the sender from their own
            // audience is how a message looks like it silently failed.
            return true;
        }
        if (named.contains(reader.getUniqueId())) {
            // Named in full, so the wall does not apply to them. Everyone else
            // on the far side of it is still removed below, which is what
            // makes this reach one person rather than broadcast past the wall.
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
     * Everyone named in full in this message, sender excluded.
     *
     * <p>Worked out once per message rather than per viewer: it is a scan of
     * the online list, and the audience of a busy server is much longer than
     * the online list is.
     */
    private Set<UUID> namedIn(net.kyori.adventure.text.Component message, Player sender) {
        if (message == null) {
            return java.util.Collections.emptySet();
        }
        // MiniMessage rather than the plain-text serializer: this codebase
        // already proves MiniMessage's interface kind at runtime, and the only
        // difference here is that styling arrives as <tags>. A tag cannot
        // appear inside a player name, and it reads as a word boundary, which
        // is what the matcher below wants anyway.
        String text = MiniMessage.miniMessage().serialize(message);
        if (text == null || text.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        HashSet<UUID> out = new HashSet<UUID>();
        for (Player other : Bukkit.getOnlinePlayers()) {
            UUID id = other.getUniqueId();
            if (id.equals(sender.getUniqueId())) continue;
            if (!ChatListener.mentions(lower, other.getName().toLowerCase(Locale.ROOT))) continue;
            out.add(id);
        }
        return out;
    }

    /**
     * Tells the sender which of the people they named are mid-match.
     *
     * <p>They did receive the line - naming them is what got it through - so
     * this is not an apology, it is a heads-up that a conversation will not
     * work this way and that /msg is the thing that does. Nothing is said
     * about someone who left isolation off, because for them nothing happened.
     */
    private void noticeMentions(Player sender, Object senderMatch, Set<UUID> named) {
        if (named.isEmpty()) {
            return;
        }
        ArrayList<String> lines = new ArrayList<String>();
        for (UUID id : named) {
            if (lines.size() >= MAX_NOTICES) {
                break;
            }
            Player other = Bukkit.getPlayer(id);
            if (other == null || !this.isolated(id)) continue;
            Object theirMatch = this.matchOf(id);
            if (theirMatch == null || theirMatch == senderMatch) continue;
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

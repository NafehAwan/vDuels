#!/usr/bin/env python3
"""Writes src/main/resources/gui/<menu>.yml from the defaults in the Java.

The shipped files have to say exactly what the code says, or the promise the
whole design rests on - *delete the gui/ folder and the plugin looks as it
shipped* - quietly stops being true. Typing twenty-seven files by hand and
then checking them is the slow way to find that out; generating them from the
one source means there is nothing to drift.

What is not generated is the prose. Each menu gets a short description, a list
of the placeholders its items accept, and a comment per item, all from NOTES
below - a server owner opening party.yml should not have to guess what
"opponent" is.

  ./gen_gui_yaml.py            writes every menu that has NOTES
  ./gen_gui_yaml.py party      writes just that one

tools/verify_gui.py then proves the result matches the Java.
"""

import os
import sys

import gui_defaults as G

OUT = os.path.join(G.ROOT, "src", "main", "resources", "gui")

COMMON = """\
# Every value here is the one the plugin uses when the key is missing, so you
# can delete any line, any item, or this whole file: the menu goes back to
# looking exactly as it ships. There is no reset command because deleting IS
# the reset.
#
# Each item takes:
#   slot      0-based, counting left to right then down (row 1 is 0-8)
#   material  any Minecraft item id, e.g. CLOCK, PAPER, LIME_DYE
#   amount    stack size shown on the icon, 1-64
#   name      the item's title
#   lore      the lines under it; "" is a blank spacer line
#   glow      true to force the enchanted shimmer, false to force it off\n#\n# An item with lore-on and lore-off has two faces - one for when the thing\n# it controls is on, one for when it is off. name-on/name-off and\n# material-on/material-off work the same way, and a plain name:/material:\n# covers both states.
#
# Theme placeholders, usable in any name or lore in any menu. These follow\n# theme: in config.yml, so one edit there restyles every screen:\n#   {value} {label} {muted} {good} {bad} {warn}   colours\n#   {tick} {cross} {sep} {hint}                   the house symbols\n# Write a colour out instead if you want one line to ignore the theme.\n#\n# Colours: <yellow>, <gold>, <#FF2E55> for a custom colour, or
# <gradient:#7CFF6B:#1FA32F>text</gradient>. Legacy &e codes work too.\
"""

FIXED_SLOT_NOTE = """\
#
# THIS MENU HAS NO slot: KEYS, ON PURPOSE. It decides what a click did by
# which slot was clicked, so moving an item here would run the wrong action.
# Material, name and lore are all still yours to change.\
"""

EDITGUI_NOTE = """\
#
# NOTE ON SLOTS: /editgui can also arrange this menu, and a layout saved there
# wins over the slot values below. The editor saves when you close it even if
# you changed nothing, so opening it once will pin these slots and make edits
# here look ignored. Run `/editgui {menu} reset` to clear that stored
# layout and hand control back to this file.\
"""

SMALLCAPS_NOTE = """\
#
# The text below is written in small capitals (ᴛʜɪꜱ ꜱᴛʏʟᴇ). That is just what
# the characters are - type ordinary letters if you would rather have them.\
"""

# menu -> (one-line description, {placeholder: meaning}, {item: comment})
NOTES = {
    "duelconfirm": (
        "Round Selection - the last screen before a duel request goes out.",
        {"rounds": "the number of rounds currently selected",
         "gamemode": "the chosen kit's display name",
         "opponent": "who the duel is being sent to",
         "sender": "who is sending it"},
        {"info": "The read-out: what is about to be sent, and to whom.",
         "clock": "Left-click raises the round count, right-click lowers it.\n"
                  "# Clamped, not wrapped - the options are 1, 2, 3 and 5.",
         "map": "Opens the map picker. Left unset, a random compatible arena is used.",
         "confirm": "Sends the request.",
         "cancel": "Back to the kit picker this screen was opened from."},
    ),
    "editkiteffects": (
        "/editkit <kit> - the potion effects a kit starts a match with, and the\n"
        "two admin actions that redefine the kit itself.",
        {"kit": "the kit's id"},
        {"strength": "Strength II for the first 90 seconds.",
         "speed": "Speed II for the first 90 seconds.",
         "speed-infinite": "Speed II for the whole match.",
         "regeneration": "Regeneration II for the first 90 seconds.",
         "edit-items": "Opens the kit's item grid. Changes apply to every player.",
         "capture": "Replaces the kit with whatever the admin is holding.\n"
                    "# Needs a shift-click, since it cannot be undone."},
    ),
    "eventmap": (
        "/event - pick the arena an event will be hosted in.",
        {"arena": "the arena's name", "world": "the world it is in",
         "border": "its shrinking-border setting"},
        {"arena-free": "One row per event-ready arena that is free. These fill the\n"
                       "# window in order, so they take no slot of their own.",
         "arena-busy": "The same row for an arena already in use. Not clickable.",
         "empty": "Shown instead when no arena has an event spawn set yet."},
    ),
    "kiteditor": (
        "/kiteditor and /mykits - pick a kit to edit. Opened in two modes: a\n"
        "player arranging their own layout, or an admin changing the kit for\n"
        "everyone, each with its own title and row wording.",
        {"kit": "the kit's id"},
        {"kit-personal": "One row per kit, as a player sees it.",
         "kit-global": "The same row as an admin sees it - this one carries the\n"
                       "# warning that a change hits every player.",
         "empty": "Shown instead when no kits exist yet."},
    ),
    "mapselect": (
        "The map picker, opened from the duel confirm screen.",
        {"arena": "the map's name", "target": "who the duel is being sent to"},
        {"arena-selected": "The map currently chosen.",
         "arena-free": "A map that can be picked. These fill the window in order,\n"
                       "# so they take no slot of their own.",
         "arena-incompatible": "A map the chosen kit cannot be played on.",
         "random": "Leaves the choice open, and any free compatible map is used.\n"
                   "# Its on face is the one shown while no map is picked."},
    ),
    "kitpick": (
        "The kit picker, the first screen of /duel <player>.",
        {"kit": "the kit's id", "category": "the category being shown"},
        {"kit": "One row per kit. With no name: set, a kit with a display name\n"
                "# keeps it and one without is titled by its id.",
         "next-category": "Pages to the next category. Only shown when more than\n"
                          "# one category exists."},
    ),
    "matchsummary": (
        "The scoreboard shown to both players when a duel ends.",
        {"you": "the viewer's name", "opponent": "the other player",
         "kit": "the kit played", "score": "the viewer's rounds won",
         "oppscore": "the opponent's rounds won", "hits": "the viewer's hits",
         "opphits": "the opponent's hits", "damage": "damage the viewer dealt",
         "oppdamage": "damage the opponent dealt", "rounds": "rounds needed to win",
         "duration": "how long the match ran", "streak": "the viewer's win streak",
         "wins": "the viewer's total wins"},
        {"result-won": "The viewer's own column, when they won.",
         "result-lost": "The same column, when they lost. Its lore is shared with\n"
                        "# result-won in the code, so both ship the same lines.",
         "match": "The match itself: kit, final score, length.",
         "opponent": "The other player's column.",
         "rematch": "Sends a fresh duel request to the same opponent.\n"
                    "# These four names are MiniMessage, so gradients work."},
    ),
    "settings": (
        "/settings - each player's own switches.",
        {},
        {"duel-requests": "Whether anyone may send this player a duel.",
         "party-invites": "Whether anyone may invite them to a party.",
         "spectators": "Whether their duels can be watched.",
         "isolated-chat": "On, a match's chat is private to the match. Off by\n"
                          "# default.",
         "scoreboard": "Their sidebar.",
         "sounds": "Every cue this plugin plays.",
         "trims": "Opens the per-kit armour trim picker. Only shown to players\n"
                  "# with meowduels.trims."},
    ),
    "partyconfirm": (
        "The confirm shown by /party leave. It takes two shapes: a leader\n"
        "leaving disbands the party for everyone, a member only leaves.",
        {},
        {"stay": "Closes the window and changes nothing.",
         "what-leader": "The middle slot as a leader sees it - the warning that\n"
                        "# this ends the party for everyone.",
         "what-member": "The middle slot as a member sees it.",
         "leave-leader": "Goes through with disbanding.",
         "leave-member": "Goes through with leaving."},
    ),
    "partysettings": (
        "/party settings - the leader's two switches. Neither applies during a\n"
        "match; a free-for-all where you cannot hit each other could not end.",
        {},
        {"open": "Whether anyone may join without an invite.",
         "friendly-fire": "Whether members can hurt each other outside a match.",
         "back": "Returns to the party menu."},
    ),
    "partymode": (
        "/party start - what kind of party match to play. The two unbuilt modes\n"
        "are shown greyed rather than hidden: a picker with one option reads as\n"
        "broken, one with two greyed options reads as a roadmap.",
        {"mode": "the mode's name", "description": "its one-line description"},
        {"ffa": "Free-for-all. Its off face is what shows if the mode is ever\n"
                "# marked unavailable.",
         "split": "Two teams.",
         "duels": "Party versus party, one pair at a time.",
         "back": "Returns to the party menu."},
    ),
    "partystartconfirm": (
        "The last screen before a party match starts. The middle slot is not a\n"
        "button - it is the answer sheet, so the decision is made on what is\n"
        "about to happen rather than on what was clicked two windows ago.",
        {"mode": "the chosen mode", "description": "the mode's description",
         "kit": "the chosen kit", "players": "how many are coming"},
        {"back": "Returns to the kit picker.",
         "what": "The read-out: mode, kit, headcount.",
         "start": "Starts the countdown for everyone."},
    ),
    "partyinvite": (
        "/party invite - everyone online who could be invited.\n"
        "On this screen `rows` is the tallest the window may get; it still\n"
        "shrinks to however many people are actually online.",
        {"player": "the candidate's name"},
        {"player": "One head per invitable player. Its on face is someone who has\n"
                   "# already been invited and has not answered.",
         "empty": "Shown when everyone online is already in a party.",
         "back": "Returns to the party menu."},
    ),
    "partyopponent": (
        "/party start > Party Duels - the other parties you could fight.\n"
        "On this screen `rows` is the tallest the window may get.",
        {"leader": "the other party's leader", "size": "how many are in it",
         "count": "used by more-line", "mark": "used by member-line",
         "colour": "used by member-line"},
        {"party": "One card per party. {members} on a line of its own expands to\n"
                  "# the roster, so you can move it inside the card or drop it.\n"
                  "# member-line is one person in that roster and more-line is the\n"
                  "# '+3 more' that caps it at eight.",
         "empty": "Shown when no other party is up and out of a match.",
         "back": "Returns to the mode picker."},
    ),
    "editkitlist": (
        "/editkit - pick a kit to edit its start-of-match potion effects.",
        {"kit": "the kit's id", "effects": "how many start effects it has"},
        {"kit": "One row per kit. With no name: set, a kit with a display name keeps\n"
                "# it and one without is titled by its id."},
    ),
    "trimkit": (
        "/trims - pick a kit to edit its armour trims.",
        {"kit": "the kit's id"},
        {"kit": "One row per kit, filling the window from the top left."},
    ),
    "tabconfig": (
        "/tabconfig - the three lines shown in the player list header.",
        {"current": "what that field is set to right now"},
        {"title": "The header's top line. Accepts MiniMessage, so gradients work.",
         "discord": "Your Discord invite.",
         "store": "Your store link."},
    ),
}


def scalar(value):
    """A YAML double-quoted scalar. Everything is quoted, including "" ."""
    out = value.replace("\\", "\\\\").replace('"', '\\"')
    return '"%s"' % out


def render(menu):
    code = G.reads.get(menu)
    if code is None:
        return None, "%s: the Java has no gui.* calls for this menu yet" % menu
    if menu not in NOTES:
        return None, "%s: no NOTES entry, so it has no prose yet" % menu
    description, placeholders, comments = NOTES[menu]

    smallcaps = any(
        any(ord(c) > 0x250 for c in [code.get("title") or ""][0])
        for _ in [0]
    ) or any(
        ord(c) > 0x250
        for item in code["items"].values()
        for c in (item.get("name") or "") + "".join(item.get("lore") or [])
    )

    lines = []
    lines.append("# " + "-" * 73)
    for part in description.split("\n"):
        lines.append("# " + part)
    lines.append("#")
    lines.append(COMMON)
    if placeholders:
        lines.append("#")
        lines.append("# Placeholders usable in any name or lore on this screen:")
        width = max(len(p) for p in placeholders)
        for name in placeholders:
            lines.append("#   {%s}%s  %s"
                         % (name, " " * (width - len(name)), placeholders[name]))
    if smallcaps:
        lines.append(SMALLCAPS_NOTE)
    if menu in G.FIXED:
        lines.append(FIXED_SLOT_NOTE)
    if menu in ("duelconfirm", "mapselect", "kitmenu"):
        lines.append(EDITGUI_NOTE.replace("{menu}", menu))
    lines.append("# " + "-" * 73)
    lines.append("")

    for key in sorted(k for k in code if k == "title" or k.startswith("title-")):
        lines.append("%s: %s" % (key, scalar(code[key])))
    if "rows" in code:
        lines.append("rows: %d" % code["rows"])
    lines.append("")
    lines.append("# Uncomment to give this one screen a different frame from the rest of the")
    lines.append("# plugin. Leave it out and it follows theme.filler in config.yml.")
    lines.append('#filler: "GRAY_STAINED_GLASS_PANE"')
    lines.append('#filler-name: " "')
    lines.append("")
    lines.append("items:")

    for item_id, spec in code["items"].items():
        lines.append("")
        for part in (comments.get(item_id) or "").split("\n"):
            if part.strip():
                lines.append("  # " + part.lstrip("# ") if not part.startswith("#")
                             else "  " + part)
        lines.append("  %s:" % item_id)
        if "slot" in spec and menu not in G.FIXED:
            lines.append("    slot: %d" % spec["slot"])
        for key in ("material", "material-on", "material-off"):
            if key in spec:
                lines.append("    %s: %s" % (key, spec[key]))
        if "material" not in spec and "material-on" not in spec \
                and "material" in spec.get("dynamic", []):
            lines.append("    # material: STONE   # each row uses its own icon unless you set this")
        if "amount" in spec:
            lines.append("    amount: %d" % spec["amount"])
        for key in ("name", "name-on", "name-off"):
            if spec.get(key) is not None:
                lines.append("    %s: %s" % (key, scalar(spec[key])))
        if "name" not in spec and "name-on" not in spec and "name" in spec.get("dynamic", []):
            lines.append('    # name: "&e{kit}"   # each row is titled by the kit unless you set this')
        for key in ("glow", "glow-on", "glow-off"):
            # Only a true is shipped. Items.glow(false) has always been a
            # no-op, so a shipped false would start forcing the shimmer off on
            # materials that glint by themselves.
            if spec.get(key) is True:
                lines.append("    %s: true" % key)
        for key, value in sorted(spec.get("lines", {}).items()):
            lines.append("    %s: %s" % (key, scalar(value)))
        for key in ("lore", "lore-on", "lore-off"):
            if not spec.get(key):
                continue
            lines.append("    %s:" % key)
            for line in spec[key]:
                lines.append("      - %s" % scalar(line))
    return "\n".join(lines) + "\n", None


if __name__ == "__main__":
    if G.problems:
        print("the Java could not be parsed:")
        for p in G.problems:
            print("  - %s" % p)
        sys.exit(1)
    wanted = sys.argv[1:] or sorted(NOTES)
    if not os.path.isdir(OUT):
        os.makedirs(OUT)
    skipped = []
    for menu in wanted:
        text, err = render(menu)
        if err:
            skipped.append(err)
            continue
        with open(os.path.join(OUT, menu + ".yml"), "w", encoding="utf-8") as fh:
            fh.write(text)
        print("wrote gui/%s.yml" % menu)
    for s in skipped:
        print("  skipped %s" % s)

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
#   glow      true to force the enchanted shimmer, false to force it off
#
# Colours: <yellow>, <gold>, <#FF2E55> for a custom colour, or
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

    if "title" in code:
        lines.append("title: %s" % scalar(code["title"]))
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
        if "material" in spec:
            lines.append("    material: %s" % spec["material"])
        if spec.get("name") is not None:
            lines.append("    name: %s" % scalar(spec["name"]))
        if spec.get("lore"):
            lines.append("    lore:")
            for line in spec["lore"]:
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

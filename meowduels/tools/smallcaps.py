#!/usr/bin/env python3
"""Text.smallCaps, in Python, reading its alphabet out of Text.java.

Nineteen of the menus were written with Items.name()/lore() and Menu.create(),
all of which run their argument through Text.smallCaps before showing it.
Making those menus editable means moving them onto rawName()/rawLore(), which
re-case nothing - so their defaults have to be written out already
small-capped, or the conversion would quietly restyle every label.

Hand-typing those escapes is how a previous pass shipped an eleven-letter
word with ten codepoints in it. This applies the real mapping instead, taken
from the SMALL array in Text.java so the two cannot drift.

  ./smallcaps.py 'Tab Settings'          -> the converted text
  ./smallcaps.py --java 'Tab Settings'   -> ...as a Java literal
"""

import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
TEXT = os.path.join(HERE, os.pardir, "src", "main", "java", "com",
                    "meowduels", "util", "Text.java")

with open(TEXT, encoding="utf-8") as fh:
    src = fh.read()

m = re.search(r"SMALL\s*=\s*new char\[\]\{(.*?)\};", src, re.S)
if not m:
    sys.exit("could not find the SMALL array in Text.java")

SMALL = []
for part in m.group(1).split(","):
    part = part.strip()
    esc = re.fullmatch(r"'\\u([0-9a-fA-F]{4})'", part)
    if esc:
        SMALL.append(chr(int(esc.group(1), 16)))
    else:
        SMALL.append(re.fullmatch(r"'(.)'", part).group(1))

if len(SMALL) != 26:
    sys.exit("SMALL has %d entries, expected 26" % len(SMALL))


def small_caps(text, keep_placeholders=True):
    """Exactly Text.smallCaps: every letter mapped, &x pairs skipped.

    With keep_placeholders, a {token} is copied through untouched. The
    original code small-capped only its literal halves - the runtime value
    was concatenated in afterwards - so converting a line that already has
    the placeholder spliced in would turn {current} into {ᴄᴜʀʀᴇɴᴛ} and leave
    a placeholder that never matches anything.
    """
    out = []
    i = 0
    while i < len(text):
        c = text[i]
        if keep_placeholders and c == "{":
            end = text.find("}", i)
            if end > i:
                out.append(text[i:end + 1])
                i = end + 1
                continue
        if c in "&§" and i + 1 < len(text):
            out.append(c)
            out.append(text[i + 1])
            i += 2
            continue
        if "a" <= c <= "z":
            out.append(SMALL[ord(c) - 97])
        elif "A" <= c <= "Z":
            out.append(SMALL[ord(c) - 65])
        else:
            out.append(c)
        i += 1
    return "".join(out)


def as_java(text):
    out = []
    for c in text:
        if c == '"':
            out.append('\\"')
        elif c == "\\":
            out.append("\\\\")
        elif ord(c) < 128:
            out.append(c)
        else:
            out.append("\\u%04x" % ord(c))
    return '"%s"' % "".join(out)


if __name__ == "__main__":
    args = sys.argv[1:]
    java = False
    if args and args[0] == "--java":
        java = True
        args = args[1:]
    for text in args:
        converted = small_caps(text)
        print(as_java(converted) if java else converted)

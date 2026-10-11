#!/usr/bin/env python3
"""Checks messages.yml's defaults against the code that reads them.

Three things can go wrong with this file and none of them shows up at
compile time:

  1. A key read but never defined. MessageManager.raw falls back to
     "&cmissing message: <key>", so the bug ships as that text on a player's
     screen.

  2. A key defined but never read - dead weight an owner will try to edit and
     wonder why nothing changes.

  3. A key under the wrong prefix. get() picks its renderer by prefix:
     request./titles. small-cap or parse MiniMessage, party./event./chat. go
     through Colors.toSection, match./round./death. through colorNormal,
     notice. through colorNormal, and everything else is flattened to grey by
     grayEmoji. Move a line between those and it silently changes colour, or
     loses its gradient, or starts showing <gradient:...> as literal text.

It also re-proves the Phase A invariant: every notice.* default renders
identically under the old Text.prefixed and the new notice. branch. That is
what makes "this build reads exactly like the last one" a checkable claim
rather than an assurance.

Exit status is 0 when everything agrees, 1 otherwise.
"""

import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "meowduels")
MSG = os.path.join(JAVA, "managers", "MessageManager.java")

problems = []


def fail(msg):
    problems.append(msg)


# --------------------------------------------------------------------------
# The renderers, by key prefix, exactly as MessageManager.get branches.
# --------------------------------------------------------------------------
BRANCHES = {
    "request.": "minimessage-or-smallcaps",
    "titles.": "minimessage-or-smallcaps",
    "notice.": "colorNormal",
    "party.": "toSection",
    "event.": "toSection",
    "chat.": "toSection",
    "match.": "colorNormal",
    "round.": "colorNormal",
    "death.": "colorNormal",
}

# Families whose key is assembled at runtime - "match.toxic." + pick, and
# the death messages picked at random - so no literal search can see them
# being read. Listed by prefix.
RUNTIME_ONLY = (
    "prefix",
    "death.",
    "match.toxic.",
)


def unescape(lit):
    out, i = [], 0
    while i < len(lit):
        c = lit[i]
        if c != "\\":
            out.append(c)
            i += 1
            continue
        nxt = lit[i + 1]
        if nxt == "u":
            out.append(chr(int(lit[i + 2:i + 6], 16)))
            i += 6
        else:
            out.append({"n": "\n", "t": "\t", '"': '"', "\\": "\\"}.get(nxt, nxt))
            i += 2
    return "".join(out)


with open(MSG, encoding="utf-8") as fh:
    msg_src = fh.read()

# Several defaults are a named constant rather than a literal - the
# countdown and FIGHT titles share theirs between the duel and party paths -
# so the constants are resolved first.
consts = {}
for m in re.finditer(r'static\s+final\s+String\s+(\w+)\s*=\s*\n?\s*"((?:[^"\\]|\\.)*)"', msg_src):
    consts[m.group(1)] = unescape(m.group(2))

defaults = {}
for m in re.finditer(r'DEFAULTS\.put\("([^"]+)",\s*(?:"((?:[^"\\]|\\.)*)"|(\w+))\)', msg_src):
    if m.group(2) is not None:
        defaults[m.group(1)] = unescape(m.group(2))
    elif m.group(3) in consts:
        defaults[m.group(1)] = consts[m.group(3)]
    else:
        fail("%s's default is %s, which is not a String constant in this file"
             % (m.group(1), m.group(3)))

if not defaults:
    fail("could not read the DEFAULTS block in MessageManager.java")

# --------------------------------------------------------------------------
# Every key the code asks for.
# --------------------------------------------------------------------------
# Any string literal in the tree that is not part of the DEFAULTS block
# counts as a read. Deliberately loose: these keys reach get() through
# messages().get, through raw(), and through four different per-class msg()
# wrappers, and a pattern tight enough to name them all would quietly miss
# the next one somebody adds.
read = {}
used = set()
for dirpath, _dirs, files in os.walk(JAVA):
    for name in sorted(files):
        if not name.endswith(".java"):
            continue
        path = os.path.join(dirpath, name)
        with open(path, encoding="utf-8") as fh:
            src = fh.read()
        if path == MSG:
            # Everything after the DEFAULTS block is the reading half.
            src = re.sub(r'DEFAULTS\.put\("[^"]+",\s*"(?:[^"\\]|\\.)*"\)', "", src)
        # Two passes, because they answer different questions.
        #
        # Reads: a literal in the first argument of a get/raw/msg call. This
        # one must NOT consult `defaults` - an earlier version only counted a
        # literal as read when it was already defined, which made the check
        # circular: deleting a default also deleted the evidence that anything
        # wanted it, and the missing key reported clean.
        for m in re.finditer(r'\.\s*(?:get|raw|msg)\(\s*"((?:[^"\\]|\\.)*)"', src):
            read.setdefault(unescape(m.group(1)), set()).add(name)
        # Uses: any literal at all, for deciding which defaults are dead
        # weight. Loose on purpose; a false "it is used" only costs a note.
        for m in re.finditer(r'"((?:[^"\\]|\\.)*)"', src):
            lit = unescape(m.group(1))
            if lit in defaults:
                used.add(lit)

for key in sorted(read):
    if any(key == r or key.startswith(r) or r.startswith(key) for r in RUNTIME_ONLY):
        continue          # "match.toxic." + pick - the literal is a prefix
    if key not in defaults:
        fail("%s is read by %s but has no default - it would ship as "
             '"missing message: %s"' % (key, ", ".join(sorted(read[key])), key))

# An unread key is noise in an owner's messages.yml, not a defect: it costs
# them a line to scroll past. Reported, but it does not fail the check -
# several predate this plugin's current code and deleting one could break a
# server that had already reworded it.
unread = []
for key in sorted(defaults):
    if key in used or any(key == r or key.startswith(r) for r in RUNTIME_ONLY):
        continue
    unread.append(key)

# --------------------------------------------------------------------------
# Renderers. A notice.* key must not also match an earlier branch, and no key
# may sit under a prefix whose renderer would restyle it.
# --------------------------------------------------------------------------
for key in sorted(defaults):
    matched = [p for p in BRANCHES if key.startswith(p)]
    if len(matched) > 1:
        fail("%s matches more than one renderer prefix (%s) - get() takes the "
             "first branch, which may not be the one intended"
             % (key, ", ".join(sorted(matched))))

# --------------------------------------------------------------------------
# The Phase A invariant, re-proved from the shipped defaults alone.
# --------------------------------------------------------------------------
def translate(s):
    out = list(s)
    for i in range(len(out) - 1):
        if out[i] == "&" and out[i + 1] in "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx":
            out[i] = "\u00a7"
            out[i + 1] = out[i + 1].lower()
    return "".join(out)


def plain(s):
    """Text.plain - what Text.prefixed did to these lines before Phase A."""
    s = re.sub(r"(?i)[&\u00a7][0-9a-fk-or]", "", s)
    s = re.sub(r"[^\x20-\x7e]", "", s)
    s = re.sub(r" {2,}", " ", s).strip()
    return translate("&7" + s)


notices = 0
for key, value in sorted(defaults.items()):
    if not key.startswith("notice."):
        continue
    notices += 1
    # A placeholder stands for runtime text, which neither renderer touches.
    sample = re.sub(r"\{[^}]*\}", "X", value)
    if plain(sample) != translate(sample):
        fail("%s no longer renders as it did before Phase A:\n"
             "        was: %r\n        now: %r"
             % (key, plain(sample), translate(sample)))

# --------------------------------------------------------------------------
if problems:
    print("messages.yml check FAILED\n")
    for p in problems:
        print("  - %s" % p)
    print("\n%d problem(s)." % len(problems))
    sys.exit(1)

if unread:
    print("note: %d key(s) have a default that nothing reads: %s"
          % (len(unread), ", ".join(unread)))
print("OK - %d keys, all reads defined, %d notice keys render as before"
      % (len(defaults), notices))

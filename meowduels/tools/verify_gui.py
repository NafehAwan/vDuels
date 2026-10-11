#!/usr/bin/env python3
"""Proves the shipped gui/*.yml files say exactly what the Java says.

The menus are config-driven, and every lookup takes the literal the menu
would have used anyway as its default:

    gui.item("duelconfirm", "clock", tokens, Material.CLOCK,
             "<gold>... Rounds ...", "", "<yellow>Current <white>{rounds}...")

That design has one rule holding it up - *delete the gui/ folder and the
plugin looks exactly as it shipped* - and one way to break it silently:
let a shipped YAML value drift from the Java default beside it. Nobody would
notice, because both paths render something plausible. A server owner would
just find that editing the file "changes" a menu that was never what the
screenshots showed.

So this walks the Java, pulls out every default, and compares. It also:

  * fails when a menu in GuiConfigManager.MENUS has no shipped file,
  * fails when a shipped file carries an item or key the Java never reads,
  * fails when one of the five slot-dispatched menus ships a `slot:`, which
    would move an item without moving the click handler that reads its
    number, and
  * checks the `theme:` section the same way, because Style.load reads it
    through a section parameter that tools/verify_config.py cannot see, and
    so is exempt there.

Exit status is 0 when everything agrees, 1 otherwise.
"""

import os
import re
import sys

import gui_defaults as G

# The Java-side parsing lives in gui_defaults so that this checker and
# tools/gen_gui_yaml.py read the defaults from one place.
ROOT = G.ROOT
GUI_SRC = G.GUI_SRC
RESOURCES = G.RESOURCES
GUI_RES = G.GUI_RES
MENUS = G.MENUS
FIXED = G.FIXED
reads = G.reads
item_count = G.item_count
decode = G.decode
problems = list(G.problems)


def fail(msg):
    problems.append(msg)


# --------------------------------------------------------------------------
# The shipped YAML. Parsed by hand: the repo has no PyYAML and the shape
# here is small and fixed.
# --------------------------------------------------------------------------
def strip_trailing_comment(v):
    """Drops a trailing `# note`, but not a # inside quotes.

    Needed because every colour in this config is a hex literal - treating
    the first # on the line as a comment turns "<#FF2E55>" into '"<'.
    """
    quote = None
    for i, c in enumerate(v):
        if quote:
            if c == "\\":
                continue
            if c == quote:
                quote = None
        elif c in "\"'":
            quote = c
        elif c == "#" and (i == 0 or v[i - 1] in " \t"):
            return v[:i]
    return v


def unquote(v):
    v = v.strip()
    if len(v) >= 2 and v[0] == v[-1] and v[0] in "\"'":
        body = v[1:-1]
        if v[0] == '"':
            body = re.sub(r"\\u([0-9a-fA-F]{4})", lambda m: chr(int(m.group(1), 16)), body)
            body = body.replace('\\"', '"').replace("\\\\", "\\")
        return body
    return v


def load_yaml(path):
    """title/rows/filler at the root, plus items.<id>.<key>."""
    doc = {"items": {}}
    item = None
    lore_key = None
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            line = line.rstrip("\n")
            if not line.strip() or line.lstrip().startswith("#"):
                continue
            indent = len(line) - len(len(line.lstrip()) and line.lstrip() or "")
            stripped = line.strip()
            if lore_key is not None and stripped.startswith("- "):
                item[lore_key].append(unquote(stripped[2:]))
                continue
            lore_key = None
            if indent == 0:
                if stripped.endswith(":") and stripped[:-1] == "items":
                    item = None
                    continue
                if ":" in stripped:
                    k, _, v = stripped.partition(":")
                    doc[k.strip()] = unquote(v)
                continue
            if indent == 2 and stripped.endswith(":"):
                item = doc["items"].setdefault(stripped[:-1].strip(), {})
                continue
            if indent >= 4 and item is not None and ":" in stripped:
                k, _, v = stripped.partition(":")
                k = k.strip()
                if v.strip() == "":
                    item[k] = []
                    lore_key = k
                else:
                    item[k] = unquote(v)
    return doc


for menu in MENUS:
    path = os.path.join(GUI_RES, menu + ".yml")
    if not os.path.exists(path):
        fail("gui/%s.yml is listed in GuiConfigManager.MENUS but not shipped" % menu)

checked_menus = 0
for menu in sorted(reads):
    path = os.path.join(GUI_RES, menu + ".yml")
    if menu not in MENUS:
        fail("%s reads menu '%s', which is not in GuiConfigManager.MENUS"
             % (reads[menu]["file"], menu))
        continue
    if not os.path.exists(path):
        continue
    doc = load_yaml(path)
    code = reads[menu]
    checked_menus += 1

    for key in sorted(k for k in code if k == "title" or k.startswith("title-")):
        if doc.get(key) is None:
            fail("gui/%s.yml: no %s, but the code has one (%r)" % (menu, key, code[key]))
        elif doc[key] != code[key]:
            fail("gui/%s.yml: %s is %r, the code default is %r"
                 % (menu, key, doc[key], code[key]))
    if menu in FIXED and "rows" in doc:
        fail("gui/%s.yml: rows is shipped, but this menu dispatches clicks by "
             "slot number - its height is counted into those numbers, so the "
             "key would do nothing" % menu)
    if "rows" in code:
        if doc.get("rows") is None:
            fail("gui/%s.yml: no rows, but the code has %d" % (menu, code["rows"]))
        elif int(doc["rows"]) != code["rows"]:
            fail("gui/%s.yml: rows is %s, the code default is %d"
                 % (menu, doc["rows"], code["rows"]))

    for item_id, expect in sorted(code["items"].items()):
        got = doc["items"].get(item_id)
        if got is None:
            fail("gui/%s.yml: no item '%s', which the code reads" % (menu, item_id))
            continue
        for key, want in sorted(expect.get("lines", {}).items()):
            if key not in got:
                fail("gui/%s.yml: item '%s' has no %s, the code default is %r"
                     % (menu, item_id, key, want))
            elif got[key] != want:
                fail("gui/%s.yml: item '%s' %s is %r, the code default is %r"
                     % (menu, item_id, key, got[key], want))
        for key in ("slot", "material", "material-on", "material-off",
                    "name", "name-on", "name-off",
                    "lore", "lore-on", "lore-off",
                    "glow", "glow-on", "glow-off", "amount"):
            if key not in expect:
                continue
            want = expect[key]
            if key == "slot" and menu in FIXED:
                if key in got:
                    fail("gui/%s.yml: item '%s' has a slot, but this menu dispatches "
                         "clicks by slot number - moving it would fire the wrong "
                         "handler, so the key must not be shipped" % (menu, item_id))
                continue
            if key in ("glow", "glow-on", "glow-off") and want is False:
                # A face that does not glow carries no key: Items.glow(false)
                # has always been a no-op, and shipping `glow: false` would
                # start forcing the shimmer off on materials that glint by
                # themselves.
                if key in got:
                    fail("gui/%s.yml: item '%s' ships %s: false, but the code "
                         "leaves that state's shimmer alone - the key would "
                         "change what an enchanted icon looks like"
                         % (menu, item_id, key))
                continue
            if key not in got:
                fail("gui/%s.yml: item '%s' has no %s, the code default is %r"
                     % (menu, item_id, key, want))
                continue
            have = got[key]
            if key.startswith("lore"):
                if list(have) != list(want):
                    fail("gui/%s.yml: item '%s' %s differs from the code default:\n"
                         "      shipped: %r\n         code: %r"
                         % (menu, item_id, key, list(have), list(want)))
                continue
            if key in ("slot", "amount"):
                if int(have) != int(want):
                    fail("gui/%s.yml: item '%s' %s is %s, the code default is %s"
                         % (menu, item_id, key, have, want))
                continue
            if key.startswith("glow"):
                if str(have).lower() != str(want).lower():
                    fail("gui/%s.yml: item '%s' %s is %s, the code default is %s"
                         % (menu, item_id, key, have, want))
                continue
            if have != want:
                fail("gui/%s.yml: item '%s' %s is %r, the code default is %r"
                     % (menu, item_id, key, have, want))

    for item_id in sorted(doc["items"]):
        if item_id not in code["items"]:
            fail("gui/%s.yml: item '%s' is shipped but the code never reads it"
                 % (menu, item_id))

# --------------------------------------------------------------------------
# The theme: section. tools/verify_config.py exempts it (Style.load reads it
# through a section parameter its regex cannot follow), so it is checked here
# against Style's own field initialisers.
# --------------------------------------------------------------------------
with open(os.path.join(GUI_SRC, "Style.java"), encoding="utf-8") as fh:
    style_src = fh.read()

fields = {}
for m in re.finditer(r"public\s+static\s+String\s+(\w+)\s*=\s*\"((?:[^\"\\]|\\.)*)\"", style_src):
    fields[m.group(1)] = decode(m.group(2))

load = re.search(r"public static void load\(.*?\n    \}", style_src, re.S)
theme_reads = {}
if load:
    for m in re.finditer(r"(\w+)\s*=\s*t\.getString\(\"([^\"]+)\",\s*(\w+)\)", load.group(0)):
        theme_reads[m.group(2)] = m.group(3)
else:
    fail("could not find Style.load - the theme section is going unchecked")

config_path = os.path.join(RESOURCES, "config.yml")
shipped_theme = {}
with open(config_path, encoding="utf-8") as fh:
    in_theme = False
    for line in fh:
        if re.match(r"^theme:\s*$", line):
            in_theme = True
            continue
        if in_theme:
            if line.strip() and not line.startswith(("  ", "\t")):
                break
            if line.strip().startswith("#") or ":" not in line:
                continue
            k, _, v = line.strip().partition(":")
            shipped_theme[k.strip()] = unquote(strip_trailing_comment(v))

for key, field in sorted(theme_reads.items()):
    if field not in fields:
        fail("theme.%s falls back to Style.%s, which is not a String field" % (key, field))
        continue
    if key not in shipped_theme:
        fail("theme.%s is read by Style.load but not shipped in config.yml" % key)
    elif shipped_theme[key] != fields[field]:
        fail("config.yml theme.%s is %r, but Style.%s defaults to %r"
             % (key, shipped_theme[key], field, fields[field]))

for key in sorted(shipped_theme):
    if key not in theme_reads:
        fail("config.yml ships theme.%s, which Style.load never reads" % key)

# --------------------------------------------------------------------------
if problems:
    print("gui/ fidelity check FAILED\n")
    for p in problems:
        print("  - %s" % p)
    print("\n%d problem(s)." % len(problems))
    sys.exit(1)

print("OK - %d menus, %d items, %d theme keys, all shipped, all defaults agree"
      % (checked_menus, item_count, len(theme_reads)))

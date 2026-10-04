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

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "meowduels")
GUI_SRC = os.path.join(JAVA, "gui")
RESOURCES = os.path.join(ROOT, "src", "main", "resources")
GUI_RES = os.path.join(RESOURCES, "gui")

problems = []


def fail(msg):
    problems.append(msg)


# --------------------------------------------------------------------------
# A Java string literal, decoded the way javac decodes it.
#
# Written out rather than leaning on ast.literal_eval or codecs: the source
# is full of \uXXXX escapes for small-caps letters and box symbols, and a
# decoder that gets one of those wrong would report a false mismatch against
# a YAML file that is actually correct - which is worse than no checker,
# because it trains you to ignore the output.
# --------------------------------------------------------------------------
SIMPLE = {"n": "\n", "t": "\t", "r": "\r", "b": "\b", "f": "\f",
          "0": "\0", '"': '"', "'": "'", "\\": "\\"}


def decode(literal):
    out = []
    i = 0
    while i < len(literal):
        c = literal[i]
        if c != "\\":
            out.append(c)
            i += 1
            continue
        nxt = literal[i + 1]
        if nxt == "u":
            j = i + 2
            while literal[j] == "u":          # \uuXXXX is legal Java
                j += 1
            out.append(chr(int(literal[j:j + 4], 16)))
            i = j + 4
        elif nxt in SIMPLE:
            out.append(SIMPLE[nxt])
            i += 2
        else:
            out.append(nxt)
            i += 2
    return "".join(out)


def split_args(text):
    """Top-level comma split of an argument list, honouring strings/nesting."""
    args, depth, cur, i = [], 0, [], 0
    while i < len(text):
        c = text[i]
        if c == '"':
            j = i + 1
            while True:
                if text[j] == "\\":
                    j += 2
                    continue
                if text[j] == '"':
                    break
                j += 1
            cur.append(text[i:j + 1])
            i = j + 1
            continue
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        if c == "," and depth == 0:
            args.append("".join(cur).strip())
            cur = []
            i += 1
            continue
        cur.append(c)
        i += 1
    if "".join(cur).strip():
        args.append("".join(cur).strip())
    return args


def balanced(src, open_paren):
    """Index just past the ) matching the ( at open_paren."""
    depth, i = 0, open_paren
    while i < len(src):
        c = src[i]
        if c == '"':
            i += 1
            while True:
                if src[i] == "\\":
                    i += 2
                    continue
                if src[i] == '"':
                    break
                i += 1
        elif c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def strip_comments(src):
    """Blank out // and /* */ so a commented-out call is not parsed."""
    out, i, n = [], 0, len(src)
    while i < n:
        c = src[i]
        if c == '"':
            j = i + 1
            while True:
                if src[j] == "\\":
                    j += 2
                    continue
                if src[j] == '"':
                    break
                j += 1
            out.append(src[i:j + 1])
            i = j + 1
            continue
        if src.startswith("//", i):
            j = src.find("\n", i)
            j = n if j < 0 else j
            out.append(" " * (j - i))
            i = j
            continue
        if src.startswith("/*", i):
            j = src.find("*/", i)
            j = n if j < 0 else j + 2
            out.append(" ".join("" for _ in range(0)) + " " * (j - i))
            i = j
            continue
        out.append(c)
        i += 1
    return "".join(out)


# --------------------------------------------------------------------------
# The menu list and the fixed-slot list, read out of the Java rather than
# repeated here - a checker that keeps its own copy of the thing it checks
# drifts from it.
# --------------------------------------------------------------------------
def java_string_list(src, field):
    m = re.search(re.escape(field) + r"\s*=\s*Arrays\.asList\(", src)
    if not m:
        return []
    end = balanced(src, src.index("(", m.end() - 1))
    body = src[src.index("(", m.end() - 1) + 1:end]
    return [decode(a[1:-1]) for a in split_args(body) if a.startswith('"')]


gcm_path = os.path.join(JAVA, "managers", "GuiConfigManager.java")
with open(gcm_path, encoding="utf-8") as fh:
    gcm = fh.read()

MENUS = java_string_list(gcm, "MENUS")
FIXED = java_string_list(gcm, "FIXED_SLOTS")
if not MENUS:
    fail("could not read GuiConfigManager.MENUS - has the field been renamed?")
if not FIXED:
    fail("could not read GuiConfigManager.FIXED_SLOTS")


# --------------------------------------------------------------------------
# Constants, so `gui.slot(MENU, "cancel", SLOT_BACK, rows)` can be resolved.
# --------------------------------------------------------------------------
def constants(src):
    out = {}
    for m in re.finditer(r"static\s+final\s+String\s+(\w+)\s*=\s*\"((?:[^\"\\]|\\.)*)\"", src):
        out[m.group(1)] = decode(m.group(2))
    for m in re.finditer(r"static\s+final\s+String\s+(\w+)\s*=\s*GuiLayoutManager\.(\w+)", src):
        out[m.group(1)] = ("@gl", m.group(2))
    for m in re.finditer(r"static\s+final\s+int\s+(\w+)\s*=\s*(-?\d+)", src):
        out[m.group(1)] = int(m.group(2))
    return out


with open(os.path.join(JAVA, "managers", "GuiLayoutManager.java"), encoding="utf-8") as fh:
    glm_consts = constants(fh.read())


def resolve(arg, consts):
    """A Java expression to a Python value, or None when not a literal."""
    arg = arg.strip()
    if arg.startswith('"') and arg.endswith('"'):
        return decode(arg[1:-1])
    if re.fullmatch(r"-?\d+", arg):
        return int(arg)
    if arg.startswith("Material."):
        return arg.split(".", 1)[1]
    if arg in consts:
        value = consts[arg]
        if isinstance(value, tuple) and value[0] == "@gl":
            return glm_consts.get(value[1])
        return value
    return None


# --------------------------------------------------------------------------
# Every gui.<call>(...) in the menu sources.
# --------------------------------------------------------------------------
CALL = re.compile(r"\bgui\.(item|title|rows|slot|frame)\s*\(")

reads = {}          # menuId -> {"title":str, "rows":int, "items":{id:{...}}}
item_count = 0

for name in sorted(os.listdir(GUI_SRC)):
    if not name.endswith(".java"):
        continue
    with open(os.path.join(GUI_SRC, name), encoding="utf-8") as fh:
        raw = fh.read()
    src = strip_comments(raw)
    consts = constants(raw)
    for m in CALL.finditer(src):
        kind = m.group(1)
        open_paren = src.index("(", m.end() - 1)
        end = balanced(src, open_paren)
        if end < 0:
            fail("%s: unbalanced gui.%s(" % (name, kind))
            continue
        args = split_args(src[open_paren + 1:end])
        if not args:
            continue
        menu = resolve(args[0], consts)
        if not isinstance(menu, str):
            fail("%s: gui.%s's menu id is not a literal (%s)" % (name, kind, args[0]))
            continue
        entry = reads.setdefault(menu, {"items": {}, "file": name})
        if kind == "frame":
            continue
        if kind == "title":
            value = resolve(args[1], consts)
            if isinstance(value, str):
                entry["title"] = value
            continue
        if kind == "rows":
            value = resolve(args[1], consts)
            if isinstance(value, int):
                entry["rows"] = value
            continue
        item_id = resolve(args[1], consts)
        if not isinstance(item_id, str):
            fail("%s: gui.%s's item id is not a literal (%s)" % (name, kind, args[1]))
            continue
        slot_entry = entry["items"].setdefault(item_id, {})
        if kind == "slot":
            value = resolve(args[2], consts)
            if isinstance(value, int):
                slot_entry["slot"] = value
            continue
        # gui.item: [menu, id, (on)?, (tokens)?, Material, name, lore...]
        rest = args[2:]
        while rest and resolve(rest[0], consts) is None and not rest[0].startswith("Material."):
            rest = rest[1:]          # skips the boolean and the tokens map
        if not rest:
            fail("%s: gui.item(%s) has no material argument" % (name, item_id))
            continue
        material = resolve(rest[0], consts)
        values = [resolve(a, consts) for a in rest[1:]]
        if any(v is None for v in values):
            # A name assembled at runtime cannot be compared, and must not
            # be silently treated as absent either.
            fail("%s: gui.item(%s) has a non-literal name or lore line; pass it "
                 "as a {token} instead so the default stays checkable" % (name, item_id))
            continue
        slot_entry["material"] = material
        if values:
            slot_entry["name"] = values[0]
            slot_entry["lore"] = values[1:]
        item_count += 1

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

    if "title" in code:
        if doc.get("title") is None:
            fail("gui/%s.yml: no title, but the code has one (%r)" % (menu, code["title"]))
        elif doc["title"] != code["title"]:
            fail("gui/%s.yml: title is %r, the code default is %r"
                 % (menu, doc["title"], code["title"]))
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
        if "slot" in expect:
            if menu in FIXED:
                if "slot" in got:
                    fail("gui/%s.yml: item '%s' has a slot, but this menu dispatches "
                         "clicks by slot number - moving it would fire the wrong "
                         "handler, so the key must not be shipped" % (menu, item_id))
            elif "slot" not in got:
                fail("gui/%s.yml: item '%s' has no slot, the code default is %d"
                     % (menu, item_id, expect["slot"]))
            elif int(got["slot"]) != expect["slot"]:
                fail("gui/%s.yml: item '%s' slot is %s, the code default is %d"
                     % (menu, item_id, got["slot"], expect["slot"]))
        if "material" in expect:
            have = got.get("material") or got.get("material-on")
            if have is None:
                fail("gui/%s.yml: item '%s' has no material, the code default is %s"
                     % (menu, item_id, expect["material"]))
            elif have != expect["material"] and got.get("material-on") != expect["material"] \
                    and got.get("material-off") != expect["material"]:
                fail("gui/%s.yml: item '%s' material is %s, the code default is %s"
                     % (menu, item_id, have, expect["material"]))
        if "name" in expect:
            have = got.get("name", got.get("name-on"))
            if have is None:
                fail("gui/%s.yml: item '%s' has no name, the code default is %r"
                     % (menu, item_id, expect["name"]))
            elif have != expect["name"] and got.get("name-on") != expect["name"] \
                    and got.get("name-off") != expect["name"]:
                fail("gui/%s.yml: item '%s' name is %r, the code default is %r"
                     % (menu, item_id, have, expect["name"]))
        if expect.get("lore"):
            have = got.get("lore", got.get("lore-on"))
            if have is None:
                fail("gui/%s.yml: item '%s' has no lore, the code has %d line(s)"
                     % (menu, item_id, len(expect["lore"])))
            elif list(have) != list(expect["lore"]) \
                    and list(got.get("lore-on") or []) != list(expect["lore"]) \
                    and list(got.get("lore-off") or []) != list(expect["lore"]):
                fail("gui/%s.yml: item '%s' lore differs from the code default:\n"
                     "      shipped: %r\n         code: %r"
                     % (menu, item_id, list(have), list(expect["lore"])))

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

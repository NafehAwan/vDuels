#!/usr/bin/env python3
"""The menu defaults, read out of the Java source.

Shared by tools/verify_gui.py, which checks the shipped gui/*.yml against
these, and tools/gen_gui_yaml.py, which writes those files from them. Both
import it so a default has exactly one definition: a checker that keeps its
own copy of the thing it checks drifts from it, and a generator that keeps a
third copy is worse again.

Exports:
    MENUS, FIXED   the menu list and the slot-dispatched ones
    reads          menuId -> {"title", "rows", "items": {id: {...}}, "file"}
    item_count     how many gui.item() calls were parsed
    problems       anything that could not be parsed
    decode(), as_yaml_scalar()
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

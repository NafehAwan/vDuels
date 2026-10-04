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
def face_helpers(src):
    """Static helpers whose whole body is `return Face.of(...)`.

    The party screens share a handful of item shapes between several items -
    three modes that each read the same way when playable and the same way
    when not - and writing the Face out three times invites the three copies
    to drift. A helper keeps them to one, and this expands the call so the
    defaults stay readable here.

    Only the single-parameter form is understood, which is all the code uses;
    anything more involved is reported rather than guessed at.
    """
    out = {}
    pattern = re.compile(
        r"(?:private|static|\s)+Face\s+(\w+)\(([^)]*)\)\s*\{\s*return\s+(Face\.of\()",
        re.S)
    for m in pattern.finditer(src):
        name = m.group(1)
        params = [pp.strip().split()[-1] for pp in m.group(2).split(",") if pp.strip()]
        open_paren = src.index("(", m.start(3))
        close = balanced(src, open_paren)
        if close < 0:
            continue
        tail_end = src.index(";", close)
        out[name] = {
            "args": src[open_paren + 1:close],
            "glow": ".glow()" in src[close:tail_end],
            "params": params,
        }
    return out


def constants(src):
    out = {}
    for m in re.finditer(r"static\s+final\s+String\s+(\w+)\s*=\s*\"((?:[^\"\\]|\\.)*)\"", src):
        out[m.group(1)] = decode(m.group(2))
    for m in re.finditer(r"static\s+final\s+String\s+(\w+)\s*=\s*GuiLayoutManager\.(\w+)", src):
        out[m.group(1)] = ("@gl", m.group(2))
    for m in re.finditer(r"static\s+final\s+int\s+(\w+)\s*=\s*(-?\d+)", src):
        out[m.group(1)] = int(m.group(2))
    # String[] constants, so a lore block shared between two items can be
    # named once in the Java and still be read here.
    for m in re.finditer(r"static\s+final\s+String\[\]\s+(\w+)\s*=\s*new String\[\]\{", src):
        open_brace = src.index("{", m.end() - 1)
        depth, i = 0, open_brace
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
            elif c == "{":
                depth += 1
            elif c == "}":
                depth -= 1
                if depth == 0:
                    break
            i += 1
        parts = split_args(src[open_brace + 1:i])
        out[m.group(1)] = ("@arr", [decode(a[1:-1]) for a in parts if a.startswith('"')])
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
        if isinstance(value, tuple) and value[0] == "@arr":
            return list(value[1])
        return value
    return None


# --------------------------------------------------------------------------
# Every gui.<call>(...) in the menu sources.
# --------------------------------------------------------------------------
GCM_CONSTS = constants(gcm)
BACK_NAME = GCM_CONSTS.get("BACK_NAME")
BACK_LORE = GCM_CONSTS.get("BACK_LORE")
if isinstance(BACK_LORE, tuple) and BACK_LORE[0] == "@arr":
    BACK_LORE = list(BACK_LORE[1])

CALL = re.compile(r"\bgui\.(item|title|rows|slot|frame|name|lore|material|amount|toggle|face|titleOf|miniName|back)\s*\(")

reads = {}          # menuId -> {"title":str, "rows":int, "items":{id:{...}}}
item_count = 0

for name in sorted(os.listdir(GUI_SRC)):
    if not name.endswith(".java"):
        continue
    with open(os.path.join(GUI_SRC, name), encoding="utf-8") as fh:
        raw = fh.read()
    src = strip_comments(raw)
    consts = constants(raw)
    helpers = face_helpers(src)
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
        if kind == "titleOf":
            variant = resolve(args[1], consts)
            value = resolve(args[2], consts)
            if isinstance(variant, str) and isinstance(value, str):
                entry["title-" + variant] = value
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
        if kind == "back":
            # gui.back(menu, id) - one fixed appearance, defined once in
            # GuiConfigManager and read from there.
            slot_entry["material"] = "ARROW"
            slot_entry["name"] = BACK_NAME
            slot_entry["lore"] = list(BACK_LORE or [])
            item_count += 1
            continue
        if kind == "miniName":
            # gui.miniName(menu, id, default, tokens) - the default IS a
            # literal, so it is checked like any other name.
            value = resolve(args[2], consts)
            if not isinstance(value, str):
                fail("%s: gui.miniName(%s) default is not a literal" % (name, item_id))
                continue
            slot_entry["name"] = value
            item_count += 1
            continue
        if kind in ("name", "material", "amount"):
            # These are read on their own where the code decides the rest of
            # the item. The default may still be a literal - gui.material with
            # Material.LIME_DYE - and where it is, it is checked like any
            # other; only a value worked out at runtime is left unchecked.
            default = resolve(args[2], consts) if len(args) > 2 else None
            if default is None:
                slot_entry.setdefault("dynamic", []).append(kind)
            elif not (kind == "amount" and default == 1):
                # An amount of 1 is what an item is anyway, so shipping it
                # would put `amount: 1` in every file for nothing.
                slot_entry[kind] = default
            continue
        if kind == "lore":
            # gui.lore(menu, id, tokens, line...) - the lines are the default.
            rest = args[2:]
            while rest and resolve(rest[0], consts) is None:
                rest = rest[1:]
            values = []
            broken = False
            for a in rest:
                r = resolve(a, consts)
                if r is None:
                    fail("%s: gui.lore(%s) has a non-literal line; pass it as a "
                         "{token} so the default stays checkable" % (name, item_id))
                    broken = True
                    break
                values.extend(r) if isinstance(r, list) else values.append(r)
            if broken:
                continue
            if values:
                slot_entry["lore"] = values
            continue
        if kind == "slot":
            value = resolve(args[2], consts)
            if isinstance(value, int):
                slot_entry["slot"] = value
            continue
        if kind in ("toggle", "face"):
            # gui.toggle(menu, id, on, tokens, Face.of(..).glow(), Face.of(..))
            # gui.face(menu, id, tokens, Face.of(..))
            faces = []
            for a in args[2:]:
                a = a.lstrip()
                if a.startswith("Face.of("):
                    faces.append(a)
                    continue
                call = re.match(r"(\w+)\((.*)\)$", a, re.S)
                if call and call.group(1) in helpers:
                    helper = helpers[call.group(1)]
                    body = helper["args"]
                    actual = split_args(call.group(2))
                    if len(actual) != len(helper["params"]):
                        fail("%s: %s(...) passes %d argument(s), the helper takes %d"
                             % (name, call.group(1), len(actual), len(helper["params"])))
                        continue
                    for param, value in zip(helper["params"], actual):
                        body = re.sub(r"\b%s\b" % re.escape(param), value, body)
                    faces.append("Face.of(" + body + ")"
                                 + (".glow()" if helper["glow"] else ""))
                    continue
            if not faces or (kind == "toggle" and len(faces) != 2):
                fail("%s: gui.%s(%s) does not have the Face argument(s) it needs"
                     % (name, kind, item_id))
                continue
            parsed = []
            for face in faces:
                inner_open = face.index("(")
                inner_close = balanced(face, inner_open)
                parts = split_args(face[inner_open + 1:inner_close])
                values = [resolve(a, consts) for a in parts]
                if not values or any(v is None for v in values):
                    fail("%s: gui.%s(%s) has a non-literal Face argument; pass it "
                         "as a {token} so the default stays checkable"
                         % (name, kind, item_id))
                    parsed = None
                    break
                parsed.append({"material": values[0], "name": values[1],
                               "lore": values[2:],
                               "glow": ".glow()" in face[inner_close:]})
            if parsed is None:
                continue
            if kind == "face":
                slot_entry["material"] = parsed[0]["material"]
                slot_entry["name"] = parsed[0]["name"]
                if parsed[0]["lore"]:
                    slot_entry["lore"] = parsed[0]["lore"]
                if parsed[0]["glow"]:
                    slot_entry["glow"] = True
            else:
                on, off = parsed
                # Only the keys that actually differ between the two states
                # are shipped per state; the rest stay single.
                if on["material"] == off["material"]:
                    slot_entry["material"] = on["material"]
                else:
                    slot_entry["material-on"] = on["material"]
                    slot_entry["material-off"] = off["material"]
                if on["name"] == off["name"]:
                    slot_entry["name"] = on["name"]
                else:
                    slot_entry["name-on"] = on["name"]
                    slot_entry["name-off"] = off["name"]
                if on["lore"] == off["lore"]:
                    if on["lore"]:
                        slot_entry["lore"] = on["lore"]
                else:
                    slot_entry["lore-on"] = on["lore"]
                    slot_entry["lore-off"] = off["lore"]
                if on["glow"] != off["glow"]:
                    slot_entry["glow-on"] = on["glow"]
                    slot_entry["glow-off"] = off["glow"]
                elif on["glow"]:
                    slot_entry["glow"] = True
            item_count += 1
            continue
        if kind == "legacy-toggle":
            # gui.toggle(menu, id, on, tokens, Material, name,
            #            new String[]{on face}, new String[]{off face})
            rest = args[2:]
            while rest and not rest[0].startswith("Material."):
                rest = rest[1:]
            if len(rest) < 4:
                fail("%s: gui.toggle(%s) does not have a material, a name and "
                     "two lore arrays" % (name, item_id))
                continue
            faces = []
            for arr in rest[2:4]:
                inner = arr[arr.index("{") + 1:arr.rindex("}")]
                values = [resolve(a, consts) for a in split_args(inner)]
                if any(v is None for v in values):
                    fail("%s: gui.toggle(%s) has a non-literal lore line"
                         % (name, item_id))
                    values = None
                faces.append(values)
            if any(f is None for f in faces):
                continue
            slot_entry["material"] = resolve(rest[0], consts)
            slot_entry["name"] = resolve(rest[1], consts)
            slot_entry["lore-on"], slot_entry["lore-off"] = faces
            item_count += 1
            continue
        # gui.item: [menu, id, (tokens)?, material, name, lore...]
        #
        # The material is found by position - the argument just before the
        # first string literal - rather than by trying to resolve it. It is
        # often an expression (gui.material(...) for a row that uses the
        # kit's own icon), and a scan that skipped anything unresolvable
        # walked straight past it and read the name as the material.
        first_string = None
        for i, a in enumerate(args[2:], start=2):
            if a.startswith('"'):
                first_string = i
                break
        if first_string is None or first_string < 3:
            fail("%s: gui.item(%s) has no material followed by a name" % (name, item_id))
            continue
        material = resolve(args[first_string - 1], consts)
        if material is None:
            # Worked out at runtime, so there is no default to ship. Recorded
            # so the generator can offer the key as a comment.
            slot_entry.setdefault("dynamic", []).append("material")
        values = [resolve(a, consts) for a in args[first_string:]]
        if any(v is None for v in values):
            # A name assembled at runtime cannot be compared, and must not
            # be silently treated as absent either.
            fail("%s: gui.item(%s) has a non-literal name or lore line; pass it "
                 "as a {token} instead so the default stays checkable" % (name, item_id))
            continue
        if material is not None:
            slot_entry["material"] = material
        if values:
            slot_entry["name"] = values[0]
            if values[1:]:
                slot_entry["lore"] = values[1:]
        item_count += 1

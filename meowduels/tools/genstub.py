#!/usr/bin/env python3
"""Generate a compile-only stub of the server API this plugin uses.

The PaperMC maven repo is blocked by this environment's network policy, so the
plugin can't be built against the real paper-api. Rather than hand-write a stub
and guess at descriptors, this reads them straight out of a known-good
MeowDuels.jar constant pool: every member it emits is exactly what the working
jar already links against, so a rebuild links identically. extern_refs.sh
verifies that afterwards.

Bytecode gives us member names, descriptors, static-ness (invokestatic/getstatic)
and interface-ness (invokeinterface). The one thing it can't give us is the type
hierarchy - that lives in stub-hierarchy.txt, which is short and hand-kept.

Usage: genstub.py <javap-dump> <plugin-classes-dir> <out-src-dir> [hierarchy]
"""
import os, re, sys, collections

EXTERNAL = ("org/bukkit/", "net/kyori/", "io/papermc/", "net/md_5/",
            "me/clip/", "org/spigotmc/", "com/destroystokyo/paper/")
PRIMS = {"V": "void", "Z": "boolean", "B": "byte", "C": "char", "S": "short",
         "I": "int", "J": "long", "F": "float", "D": "double"}


def is_external(name):
    return any(name.startswith(p) for p in EXTERNAL)


def read_type(desc, i):
    arr = 0
    while desc[i] == "[":
        arr, i = arr + 1, i + 1
    c = desc[i]
    if c in PRIMS:
        t, i = PRIMS[c], i + 1
    elif c == "L":
        end = desc.index(";", i)
        t, i = desc[i + 1:end].replace("/", ".").replace("$", "."), end + 1
    else:
        raise ValueError("bad descriptor %r at %d" % (desc, i))
    return t + "[]" * arr, i


def parse_method(desc):
    i, params = 1, []
    while desc[i] != ")":
        t, i = read_type(desc, i)
        params.append(t)
    return params, read_type(desc, i + 1)[0]


def default_for(t):
    if t == "void":
        return None
    if t == "boolean":
        return "false"
    if t in ("byte", "char", "short", "int"):
        return "0"
    return {"long": "0L", "float": "0.0f", "double": "0.0"}.get(t, "null")


api = collections.defaultdict(
    lambda: {"iface": False, "methods": {}, "fields": {}, "ctors": set()})
types = set()
REF = re.compile(r"\b(invokestatic|invokevirtual|invokeinterface|invokespecial|"
                 r"getstatic|putstatic|getfield|putfield)\b.*?"
                 r"//\s*(?:InterfaceMethod|Method|Field)\s+([\w/$]+)\.\"?([\w$<>]+)\"?:(\S+)")


def note_descriptor(desc):
    """Records every class named in a raw descriptor, in internal form."""
    for m in re.finditer(r"L([\w/$]+);", desc):
        if is_external(m.group(1)):
            types.add(m.group(1))


def collect(javap_path, classes_dir):
    for line in open(javap_path, encoding="utf-8", errors="replace"):
        m = REF.search(line)
        if m:
            op, owner, name, desc = m.groups()
            if is_external(owner):
                e = api[owner]
                types.add(owner)
                if op == "invokeinterface":
                    e["iface"] = True
                note_descriptor(desc)
                if op in ("getstatic", "putstatic", "getfield", "putfield"):
                    t = read_type(desc, 0)[0]
                    e["fields"][name] = (t, op in ("getstatic", "putstatic"))
                else:
                    params, ret = parse_method(desc)
                    if name == "<init>":
                        e["ctors"].add(tuple(params))
                    else:
                        e["methods"][(name, tuple(params))] = (ret, op == "invokestatic")
        for cm in re.finditer(r"//\s*class\s+([\w/$]+)", line):
            if is_external(cm.group(1)):
                types.add(cm.group(1))
    # Supertypes and any other API name the plugin's own class files mention.
    for root, _, files in os.walk(classes_dir):
        for f in files:
            if f.endswith(".class"):
                blob = open(os.path.join(root, f), "rb").read()
                for m in re.finditer(
                        rb"(?:org/bukkit|net/kyori|io/papermc|net/md_5|me/clip|org/spigotmc"
                        rb"|com/destroystokyo/paper)[\w/$]*",
                        blob):
                    name = m.group(0).decode()
                    if name.count("/") >= 2:
                        types.add(name)
    for t in types:
        api[t]


def load_shapes(javap_path, path):
    """Which API types are interfaces / annotations.

    Interfaces are detected automatically: anything the plugin's own classes
    declare in an `implements` clause must be one. Annotations can't be inferred
    from bytecode the same way, so they are listed in stub-shape.txt.
    """
    shape = {}
    header = re.compile(r"^(?:public |final |abstract )*(?:class|interface) \S+"
                        r"(?: extends [\w.,$ ]+?)?(?: implements ([\w.,$ ]+?))? ?\{")
    for line in open(javap_path, encoding="utf-8", errors="replace"):
        m = header.match(line.strip())
        if m and m.group(1):
            for name in m.group(1).split(","):
                internal = name.strip().replace(".", "/")
                if is_external(internal):
                    shape[internal] = "interface"
    if path and os.path.exists(path):
        for raw in open(path, encoding="utf-8"):
            line = raw.split("#", 1)[0].strip()
            if line:
                name, kind = line.split()
                shape[name] = kind
    return shape


def load_members(path):
    """Extra members that bytecode can't reveal: methods the plugin OVERRIDES
    but never calls, so they appear in no constant pool. Format per line:
        org/bukkit/plugin/java/JavaPlugin | public void onEnable() {}
    """
    extra = collections.defaultdict(list)
    if path and os.path.exists(path):
        for raw in open(path, encoding="utf-8"):
            line = raw.split("#", 1)[0].strip()
            if line and "|" in line:
                owner, decl = line.split("|", 1)
                extra[owner.strip()].append(decl.strip())
    return extra


def load_hierarchy(path):
    rel = {}
    if path and os.path.exists(path):
        for raw in open(path, encoding="utf-8"):
            line = raw.split("#", 1)[0].strip()
            if line:
                child, kw, parent = line.split()
                rel.setdefault(child, []).append((kw, parent))
    return rel


def render(internal, hier, nested, indent="", shapes=None, extra=None):
    shapes = shapes or {}
    extra = extra or {}
    e = api[internal]
    simple = internal.rpartition("/")[2].rpartition("$")[2]
    kind = shapes.get(internal)
    if kind == "enum":
        # Real enum-ness matters where Java demands it: annotation element types
        # and switch subjects. Constants come from the getstatic references.
        consts = sorted(n for n, (_, st) in e["fields"].items() if st)
        for decl in extra.get(internal, []):
            if decl not in consts:
                consts.append(decl)
        out = ["%spublic enum %s {" % (indent, simple),
               "%s    %s;" % (indent, ", ".join(consts) if consts else "")]
        for (name, params), (ret, static) in sorted(e["methods"].items()):
            args = ", ".join("%s a%d" % (p, i) for i, p in enumerate(params))
            d = default_for(ret)
            body = "{}" if d is None else "{ return %s; }" % d
            out.append("%s    public %s%s %s(%s) %s" % (
                indent, "static " if static else "", ret, name, args, body))
        out.append("%s}" % indent)
        return "\n".join(out) + "\n"
    if kind == "annotation":
        # RUNTIME retention is not optional here. Java's default is CLASS, which
        # javac writes into RuntimeInvisibleAnnotations - and a framework that
        # scans for annotations at runtime (Bukkit looking for @EventHandler)
        # then finds nothing, silently registering no listeners at all. Any
        # annotation worth stubbing is one somebody reads at runtime.
        out = ["%s@java.lang.annotation.Retention("
               "java.lang.annotation.RetentionPolicy.RUNTIME)" % indent,
               "%spublic @interface %s {" % (indent, simple)]
        for decl in extra.get(internal, []):
            out.append("%s    %s" % (indent, decl))
        out.append("%s}" % indent)
        return "\n".join(out) + "\n"
    iface = e["iface"] or kind == "interface"
    decl = []
    ext = [p for k, p in hier.get(internal, []) if k == "extends"]
    impl = [p for k, p in hier.get(internal, []) if k == "implements"]
    if ext:
        decl.append("extends " + ", ".join(x.replace("/", ".") for x in ext))
    if impl:
        decl.append(("extends " if iface else "implements ")
                    + ", ".join(x.replace("/", ".") for x in impl))
    head = "%spublic %s%s %s %s{" % (
        indent, "" if iface else "", "interface" if iface else "class",
        simple, (" ".join(decl) + " ") if decl else "")
    out = [head]
    pad = indent + "    "
    for name, (t, static) in sorted(e["fields"].items()):
        if iface:
            out.append("%s%s %s = %s;" % (pad, t, name, default_for(t)))
        else:
            out.append("%spublic %s%s %s;" % (pad, "static " if static else "", t, name))
    for params in sorted(e["ctors"]):
        if not iface:
            args = ", ".join("%s a%d" % (p, i) for i, p in enumerate(params))
            out.append("%spublic %s(%s) {}" % (pad, simple, args))
    overridden = set()
    for decl in extra.get(internal, []):
        m = re.search(r"(\w+)\s*\(([^)]*)\)", decl)
        if m:
            arity = 0 if not m.group(2).strip() else m.group(2).count(",") + 1
            overridden.add((m.group(1), arity))
    for (name, params), (ret, static) in sorted(e["methods"].items()):
        if (name, len(params)) in overridden:
            continue
        args = ", ".join("%s a%d" % (p, i) for i, p in enumerate(params))
        d = default_for(ret)
        if iface and not static:
            out.append("%s%s %s(%s);" % (pad, ret, name, args))
        else:
            body = "{}" if d is None else "{ return %s; }" % d
            mods = "public static " if static else ("static " if iface else "public ")
            out.append("%s%s%s %s(%s) %s" % (pad, mods, ret, name, args, body))
    for decl in extra.get(internal, []):
        out.append("%s%s" % (pad, decl))
    for child in sorted(nested):
        out.append(render(child, hier, [], pad, shapes, extra))
    out.append("%s}" % indent)
    return "\n".join(out) + "\n"


def main():
    javap_path, classes_dir, out_dir = sys.argv[1:4]
    hier_path = sys.argv[4] if len(sys.argv) > 4 else None
    collect(javap_path, classes_dir)
    base = os.path.dirname(os.path.abspath(hier_path or "."))
    shapes = load_shapes(javap_path, os.path.join(base, "stub-shape.txt"))
    extra = load_members(os.path.join(base, "stub-members.txt"))
    for owner in list(shapes) + list(extra):
        api[owner]
    hier = load_hierarchy(hier_path)
    for child in hier:
        api[child]
        for _, parent in hier[child]:
            api[parent]
    nested = collections.defaultdict(list)
    for t in list(api):
        if "$" in t:
            outer = t.split("$", 1)[0]
            nested[outer].append(t)
            api[outer]
    count = 0
    for internal in sorted(api):
        if "$" in internal:
            continue
        pkg, _, simple = internal.rpartition("/")
        d = os.path.join(out_dir, pkg)
        os.makedirs(d, exist_ok=True)
        with open(os.path.join(d, simple + ".java"), "w", encoding="utf-8") as fh:
            fh.write("package %s;\n\n%s" % (
                pkg.replace("/", "."),
                render(internal, hier, nested.get(internal, []), "", shapes, extra)))
        count += 1
    print("stub types written:", count)


if __name__ == "__main__":
    main()

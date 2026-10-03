#!/usr/bin/env python3
"""Checks the shipped config against what the Java actually reads.

Two failures matter and neither is visible to the compiler:

  * a key Java reads that the shipped config never mentions - the buyer
    cannot discover the setting exists;
  * a shipped key whose value differs from the Java fallback - which breaks
    the one promise this plugin makes about its own config, that deleting a
    key leaves behaviour unchanged.

Section-based reads (getConfigurationSection + a loader) cannot be seen by a
regex, so paths under KNOWN_SECTIONS are exempt from the "nothing reads it"
half rather than reported as false alarms.
"""
import re, sys, pathlib, yaml

ROOT = pathlib.Path(__file__).resolve().parent.parent
KNOWN_SECTIONS = ("anticheat",  # read through AntiCheatBypass.runHooks(name)
                  "scoreboard.global", "scoreboard.ffa", "scoreboard.duel",
                  "tab.global", "tab.duel", "tab.party",
                  "duel-spawn", "sounds", "theme", "tips")

CALL = re.compile(r'getConfig\(\)\.get(Boolean|Int|Double|String|StringList|Long)\('
                  r'\s*"([^"]+)"\s*(?:,\s*([^)]+))?\)')
# ScoreboardService reads its own block through a `sb` section variable.
SB = re.compile(r'\bsb\.get(Boolean|Int|Double|String|StringList)\('
                r'\s*"([^"]+)"\s*(?:,\s*([^)]+))?\)')
# A local `cfg` holding getConfig(), used where one method reads several keys.
CFG = re.compile(r'\bcfg\.get(Boolean|Int|Double|String|StringList|Long)\('
                 r'\s*"([^"]+)"\s*(?:,\s*([^)]+))?\)')
# Written by /meowduelssetspawn at runtime - state, not a setting a buyer authors.
RUNTIME_ONLY = ("duel-spawn",)

def unescape(lit):
    """A Java backslash-u escape is the same character the YAML writes literally."""
    return re.sub(r'\\u([0-9a-fA-F]{4})', lambda m: chr(int(m.group(1), 16)), lit)

def leaves(node, prefix=""):
    if isinstance(node, dict):
        for k, v in node.items():
            yield from leaves(v, f"{prefix}.{k}" if prefix else k)
    else:
        yield prefix, node

def main():
    cfg = yaml.safe_load((ROOT / "src/main/resources/config.yml").read_text(encoding="utf-8"))
    have = dict(leaves(cfg))
    reads = {}
    for f in (ROOT / "src/main/java").rglob("*.java"):
        text = f.read_text(encoding="utf-8")
        for kind, path, default in CALL.findall(text):
            reads.setdefault(path, (kind, (default or "").strip(), f.name))
        for kind, path, default in SB.findall(text):
            reads.setdefault("scoreboard." + path, (kind, (default or "").strip(), f.name))
        # ...but only where `cfg` actually is the plugin config. ArenaManager
        # uses the same name for arenas-dirty.yml.
        if re.search(r'cfg\s*=\s*(?:this\.)?(?:plugin\.)?getConfig\(\)', text):
            for kind, path, default in CFG.findall(text):
                reads.setdefault(path, (kind, (default or "").strip(), f.name))

    missing, drift = [], []
    for path, (kind, default, where) in sorted(reads.items()):
        if path not in have:
            if any(path == r or path.startswith(r + ".") for r in RUNTIME_ONLY):
                continue
            missing.append(f"  {path:42} read in {where} (default {default or '-'})")
            continue
        if not default:
            continue
        want, got = unescape(default.strip('"')), have[path]
        if kind == "Boolean":
            if str(got).lower() != want.lower():
                drift.append(f"  {path:42} config={got!r} java={want!r}")
        elif kind in ("Int", "Long", "Double"):
            try:
                if abs(float(got) - float(want.rstrip("Lf"))) > 1e-9:
                    drift.append(f"  {path:42} config={got!r} java={want!r}")
            except (TypeError, ValueError):
                pass
        elif kind == "String" and want != str(got):
            drift.append(f"  {path:42} config={got!r} java={want!r}")

    unread = [p for p in have
              if p not in reads
              and not any(p == s or p.startswith(s + ".") for s in KNOWN_SECTIONS)
              and not any(p.startswith(r + ".") or r.startswith(p + ".") for r in reads)]

    bad = False
    if missing:
        bad = True
        print(f"READ BY JAVA, NOT IN config.yml ({len(missing)}):"); print("\n".join(missing))
    if drift:
        bad = True
        print(f"\nSHIPPED VALUE != JAVA FALLBACK ({len(drift)}):"); print("\n".join(drift))
    if unread:
        print(f"\nIN config.yml, NOTHING READS IT ({len(unread)}):")
        for p in sorted(unread):
            print(f"  {p}")
    if not bad:
        print(f"OK - {len(reads)} keys read, all shipped, all defaults agree")
    return 1 if bad else 0

if __name__ == "__main__":
    sys.exit(main())

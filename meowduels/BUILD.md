# MeowDuels — building

Paper 1.21.11 duels/FFA plugin (`com.meowduels`). This directory is the complete
source; `mvn package` produces `target/MeowDuels-1.0.0.jar`.

## Quick start

```bash
cd meowduels
tools/install-stub.sh     # once per machine
mvn package
```

## Why there is a stub instead of the real paper-api

`repo.papermc.io` is blocked by the build environment's network policy, so
`io.papermc.paper:paper-api` cannot be downloaded. `tools/stub-src/` is a
**compile-only** stand-in: 118 types containing exactly the members this plugin
uses, and nothing else. It is never shipped — the dependency is `provided`, so
the real Paper classes are used at runtime.

The stub is committed, so a fresh checkout builds with no extra steps.

## The part that matters: descriptors must match real Paper

A stub whose signature differs from the real API compiles fine and then throws
`NoSuchMethodError` on the server. Two things guard against that:

1. **The stub is generated from bytecode, not written by hand.** `genstub.py`
   reads a known-good MeowDuels.jar's constant pool, so every member it emits is
   exactly what a working build already linked against. Generic erasure traps
   (`MiniMessage.deserialize(Object)`, not `(String)`) come out right by
   construction.

2. **`tools/extern_refs.sh` verifies it.** It dumps every API member a jar links
   against. Run it on a reference jar and on your rebuild; the output must be
   identical:

   ```bash
   tools/extern_refs.sh /path/to/reference.jar /tmp/a.txt
   tools/extern_refs.sh target/MeowDuels-1.0.0.jar /tmp/b.txt
   diff /tmp/a.txt /tmp/b.txt      # must be empty
   ```

## Verifying a rebuild

```bash
tools/verify_build.sh <known-good.jar> target/MeowDuels-1.0.0.jar
```

Checks the two things that compile perfectly and then fail silently on a server:

- **API descriptors.** A wrong signature throws `NoSuchMethodError` at runtime.
- **`@EventHandler` retention.** Java annotations default to `CLASS` retention,
  which javac writes into `RuntimeInvisibleAnnotations`. Bukkit only scans the
  *visible* ones, so a stub annotation missing `@Retention(RUNTIME)` registers
  **no listeners at all** - the plugin enables, commands work, and every event
  handler is dead. `genstub.py` now emits `@Retention(RUNTIME)` on every
  annotation it generates, and this check fails the build if the count drops
  below the reference jar's.

## Regenerating the stub (only when the API surface changes)

Adding a call to a Paper method the stub lacks is a compile error. Add the
member by hand to `tools/stub-src/`, or regenerate from a jar that already uses
it:

```bash
cd tools
mkdir -p classes && unzip -o -q <reference.jar> 'com/meowduels/*' -d classes
javap -p -c -classpath classes $(cd classes && find com -name '*.class' \
    | sed 's/\.class$//' | tr '/' '.') > javap.txt
python3 genstub.py javap.txt classes stub-src stub-hierarchy.txt
./install-stub.sh
```

Three things bytecode cannot express, kept as short hand-maintained files:

| file | holds |
|---|---|
| `stub-hierarchy.txt` | `extends` / `implements` links (e.g. `PlayerTeleportEvent extends PlayerMoveEvent`) |
| `stub-shape.txt` | which types are interfaces, enums or annotations |
| `stub-members.txt` | members the plugin overrides but never calls (they appear in no constant pool), and generic signatures erased from descriptors |

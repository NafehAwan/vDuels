# MeowTags — building

Cosmetic suffix tags (`com.meowtags`), Paper 1.21.11. Writes the chosen tag as a
LuckPerms suffix and hides it while the player is in a MeowDuels duel.

```bash
../meowduels/tools/install-stub.sh   # shared compile-only paper-api stub
mvn package
```

The stub in `meowduels/tools/stub-src` covers **both** plugins - it is generated
from the union of their bytecode, so one install serves both. See
`meowduels/BUILD.md` for why a stub exists and how to regenerate it.

Verify a rebuild against a known-good jar the same way:

```bash
PKG='com/meowtags/*' ../meowduels/tools/verify_build.sh <known-good.jar> target/MeowTags-1.0.0.jar
```

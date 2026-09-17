#!/usr/bin/env bash
# Dumps every server-API member a jar links against, as KIND owner.name:descriptor.
# Comparing this between the reference jar and a rebuild proves the compile-only
# stub didn't quietly change a signature - the failure mode that produces a
# NoSuchMethodError at runtime instead of a compile error.
#
# The KIND (Method / InterfaceMethod / Field) is part of the output and not an
# ornament. It USED to be stripped here, which is how a stub that declared
# io.papermc.paper.scoreboard.numbers.NumberFormat as a class instead of an
# interface got shipped: the descriptors matched perfectly, so this check passed,
# and every scoreboard tick threw IncompatibleClassChangeError because the JVM
# demands an InterfaceMethodref where javac had written a Methodref.
# Usage: extern_refs.sh <jar> <out.txt>   (absolute paths)
set -e
PKG="${PKG:-com/meowduels/*}"
JAR="$1"; OUT="$2"
TMP=$(mktemp -d)
unzip -o -q "$JAR" "$PKG" -d "$TMP"
CLASSES=$(cd "$TMP" && find com -name '*.class' | sed 's/\.class$//' | tr '/' '.')
javap -p -c -classpath "$TMP" $CLASSES 2>/dev/null \
  | grep -oE '// (InterfaceMethod|Method|Field) [^ ]+' \
  | sed -E 's#// ##' \
  | grep -E '^(InterfaceMethod|Method|Field) (org/bukkit|net/kyori|io/papermc|me/clip|net/md_5|com/destroystokyo)' \
  | sort -u > "$OUT"
rm -rf "$TMP"
wc -l < "$OUT"

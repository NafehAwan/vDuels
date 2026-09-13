#!/usr/bin/env bash
# Dumps every server-API member a jar links against, as owner.name:descriptor.
# Comparing this between the reference jar and a rebuild proves the compile-only
# stub didn't quietly change a signature - the failure mode that produces a
# NoSuchMethodError at runtime instead of a compile error.
# Usage: extern_refs.sh <jar> <out.txt>   (absolute paths)
set -e
JAR="$1"; OUT="$2"
TMP=$(mktemp -d)
unzip -o -q "$JAR" 'com/meowduels/*' -d "$TMP"
CLASSES=$(cd "$TMP" && find com -name '*.class' | sed 's/\.class$//' | tr '/' '.')
javap -p -c -classpath "$TMP" $CLASSES 2>/dev/null \
  | grep -oE '// (InterfaceMethod|Method|Field) [^ ]+' \
  | sed -E 's#// (InterfaceMethod|Method|Field) ##' \
  | grep -E '^(org/bukkit|net/kyori|io/papermc|me/clip|net/md_5|com/destroystokyo)' \
  | sort -u > "$OUT"
rm -rf "$TMP"
wc -l < "$OUT"

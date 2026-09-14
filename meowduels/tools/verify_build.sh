#!/usr/bin/env bash
# Compares a rebuild against a known-good reference jar on the two things that
# break silently at runtime while compiling perfectly:
#   1. API descriptors  - a wrong one throws NoSuchMethodError
#   2. @EventHandler retention - CLASS instead of RUNTIME registers NO listeners
# Usage: verify_build.sh <reference.jar> <rebuilt.jar>
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
REF="$1"; NEW="$2"; FAIL=0

"$HERE/extern_refs.sh" "$REF" /tmp/_vr.txt >/dev/null
"$HERE/extern_refs.sh" "$NEW" /tmp/_vn.txt >/dev/null
echo "== API references =="
if diff /tmp/_vr.txt /tmp/_vn.txt > /tmp/_vd.txt; then
  echo "   identical ($(wc -l < /tmp/_vn.txt) refs)"
else
  echo "   CHANGED:"; sed 's/^/     /' /tmp/_vd.txt
fi

count_ann() {
  local T; T=$(mktemp -d)
  unzip -o -q "$1" 'com/meowduels/*' -d "$T"
  local C; C=$(cd "$T" && find com -name '*.class' | sed 's/\.class$//' | tr '/' '.')
  javap -v -p -classpath "$T" $C 2>/dev/null | grep -c "$2" || true
  rm -rf "$T"
}
echo "== @EventHandler retention =="
RV_REF=$(count_ann "$REF" RuntimeVisibleAnnotations)
RV_NEW=$(count_ann "$NEW" RuntimeVisibleAnnotations)
RI_NEW=$(count_ann "$NEW" RuntimeInvisibleAnnotations)
echo "   reference runtime-visible: $RV_REF"
echo "   rebuild   runtime-visible: $RV_NEW   (invisible: $RI_NEW)"
if [ "$RV_NEW" -lt "$RV_REF" ]; then
  echo "   FAIL - listeners would not register. Stub annotation needs @Retention(RUNTIME)."
  FAIL=1
else
  echo "   OK"
fi
exit $FAIL

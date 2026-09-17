#!/usr/bin/env bash
# Checks a built jar against paper-refs.txt: the API members the original,
# real-paper-api build linked against, with their constant-pool reference kind.
#
# This is the check that catches what descriptor comparison cannot. A stub type
# declared as a class where real Paper has an interface produces identical
# descriptors and a perfectly clean compile; the JVM then refuses the call site
# at runtime with IncompatibleClassChangeError, because a static method on an
# interface has to be an InterfaceMethodref and javac wrote a Methodref.
#
# Members added since that build aren't in the baseline and are reported
# separately - they need a human to confirm the kind (constructors are always
# Method; anything called on an interface type is InterfaceMethod).
#
# Usage: verify_kinds.sh <jar>
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
JAR="$1"
"$HERE/extern_refs.sh" "$JAR" /tmp/_vk_new.txt >/dev/null
grep -v '^#' "$HERE/paper-refs.txt" | grep -v '^$' | sort > /tmp/_vk_ref.txt
sort -u /tmp/_vk_new.txt > /tmp/_vk_have.txt

# A member in the baseline whose kind changed: same signature, different kind.
cut -d' ' -f2- /tmp/_vk_ref.txt  | sort > /tmp/_vk_ref_sigs.txt
cut -d' ' -f2- /tmp/_vk_have.txt | sort > /tmp/_vk_have_sigs.txt
comm -12 /tmp/_vk_ref_sigs.txt /tmp/_vk_have_sigs.txt > /tmp/_vk_shared.txt

FAIL=0
while read -r sig; do
  R=$(grep -F -m1 " $sig" /tmp/_vk_ref.txt  | cut -d' ' -f1)
  N=$(grep -F -m1 " $sig" /tmp/_vk_have.txt | cut -d' ' -f1)
  if [ "$R" != "$N" ]; then
    echo "   MISMATCH $sig   real=$R  built=$N"
    FAIL=1
  fi
done < /tmp/_vk_shared.txt

echo "== reference kinds vs real paper-api =="
echo "   checked:  $(wc -l < /tmp/_vk_shared.txt) members shared with the baseline"
if [ "$FAIL" = 0 ]; then echo "   OK - every kind matches"; else echo "   FAIL"; fi
NEW=$(comm -13 /tmp/_vk_ref_sigs.txt /tmp/_vk_have_sigs.txt | wc -l)
echo "   not in the baseline (added since, confirm by hand): $NEW"
comm -13 /tmp/_vk_ref_sigs.txt /tmp/_vk_have_sigs.txt | sed 's/^/     /'
exit $FAIL

#!/usr/bin/env bash
# Compiles the committed compile-only paper-api stub and installs it into the
# local maven repo, so `mvn package` in meowduels/ works on a fresh checkout.
set -e
cd "$(dirname "$0")"
rm -rf stub-out && mkdir -p stub-out
find stub-src -name '*.java' > stub-files.txt
javac -nowarn -d stub-out @stub-files.txt
jar cf paper-api-stub.jar -C stub-out .
mvn -q install:install-file -Dfile=paper-api-stub.jar \
    -DgroupId=io.papermc.paper -DartifactId=paper-api \
    -Dversion=1.21.11-R0.1-SNAPSHOT -Dpackaging=jar
echo "paper-api stub installed"

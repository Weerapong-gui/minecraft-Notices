#!/usr/bin/env bash
# Build Notices.jar against a Paper server install (no Gradle needed), running unit tests first.
# Usage: ./build.sh /path/to/paper-server-dir   (the folder containing libraries/ and versions/)
set -euo pipefail
SERVER="${1:?usage: ./build.sh /path/to/paper-server-dir}"
cd "$(dirname "$0")"
CP="$(find "$SERVER/libraries" "$SERVER/versions" -name '*.jar' | tr '\n' ':')"

JUNIT_VER=1.11.4
JUNIT=lib/junit-platform-console-standalone-$JUNIT_VER.jar
JUNIT_URL=https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/$JUNIT_VER/junit-platform-console-standalone-$JUNIT_VER.jar
if [ ! -f "$JUNIT" ]; then
  mkdir -p lib
  curl -fsSL -o "$JUNIT.tmp" "$JUNIT_URL"
  EXPECT="$(curl -fsSL "$JUNIT_URL.sha1" | tr -d ' \n' | cut -c1-40)"
  if command -v sha1sum >/dev/null; then ACTUAL="$(sha1sum "$JUNIT.tmp" | cut -d' ' -f1)"; else ACTUAL="$(shasum -a 1 "$JUNIT.tmp" | cut -d' ' -f1)"; fi
  if [ "$EXPECT" != "$ACTUAL" ]; then
    rm -f "$JUNIT.tmp"; echo "junit checksum mismatch ($EXPECT != $ACTUAL)" >&2; exit 1
  fi
  mv "$JUNIT.tmp" "$JUNIT"
fi

rm -rf build && mkdir -p build/classes build/test-classes
javac --release 25 -nowarn -cp "$CP" -d build/classes $(find src/main/java -name '*.java')
if [ -d src/test/java ]; then
  javac --release 25 -nowarn -cp "build/classes:$JUNIT:$CP" -d build/test-classes $(find src/test/java -name '*.java')
  java -jar "$JUNIT" execute -cp "build/classes:build/test-classes:$CP" --scan-classpath build/test-classes --disable-banner --details=summary
fi
cp src/main/resources/plugin.yml src/main/resources/config.yml build/classes/
jar cf build/Notices.jar -C build/classes .
echo "built build/Notices.jar"

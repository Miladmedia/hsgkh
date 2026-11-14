#!/usr/bin/env sh
# Lightweight Gradle 8.4 bootstrap for environments where the standard wrapper JAR is unavailable.
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_USER_HOME=${GRADLE_USER_HOME:-"$HOME/.gradle"}
DIST="$GRADLE_USER_HOME/wrapper/dists/gradle-8.4-bin/local/gradle-8.4"
if [ ! -x "$DIST/bin/gradle" ]; then
  BASE="$GRADLE_USER_HOME/wrapper/dists/gradle-8.4-bin/local"
  mkdir -p "$BASE"
  ZIP="$BASE/gradle-8.4-bin.zip"
  if command -v curl >/dev/null 2>&1; then curl -fL --retry 3 https://services.gradle.org/distributions/gradle-8.4-bin.zip -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then wget -O "$ZIP" https://services.gradle.org/distributions/gradle-8.4-bin.zip
  else echo "curl or wget is required to download Gradle 8.4" >&2; exit 1; fi
  command -v unzip >/dev/null 2>&1 || { echo "unzip is required" >&2; exit 1; }
  unzip -q -o "$ZIP" -d "$BASE"
  rm -f "$ZIP"
fi
exec "$DIST/bin/gradle" -p "$APP_HOME" "$@"

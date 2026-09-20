#!/bin/sh
set -e
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
URL="https://raw.githubusercontent.com/gradle/gradle/v9.6.0/gradle/wrapper/gradle-wrapper.jar"
SHA="497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7"
if [ ! -f "$JAR" ]; then
  echo "Bootstrapping Gradle wrapper..."
  if command -v curl >/dev/null 2>&1; then curl -fsSL "$URL" -o "$JAR";
  elif command -v wget >/dev/null 2>&1; then wget -q "$URL" -O "$JAR";
  else echo "curl or wget is required once to bootstrap the wrapper." >&2; exit 1; fi
fi
if command -v sha256sum >/dev/null 2>&1; then
  ACTUAL=$(sha256sum "$JAR" | awk '{print $1}')
  [ "$ACTUAL" = "$SHA" ] || { echo "Gradle wrapper checksum mismatch" >&2; rm -f "$JAR"; exit 1; }
fi
if [ -n "$JAVA_HOME" ]; then JAVA_EXE="$JAVA_HOME/bin/java"; else JAVA_EXE=java; fi
exec "$JAVA_EXE" ${JAVA_OPTS:-} -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"

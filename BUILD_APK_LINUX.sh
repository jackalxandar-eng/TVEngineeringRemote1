#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
./gradlew --no-daemon assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk TV-Engineering-Remote-debug.apk
echo "APK: $(pwd)/TV-Engineering-Remote-debug.apk"

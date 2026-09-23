#!/usr/bin/env bash
# Build the app and install it on the connected Android phone (update-in-place; keeps data).
set -euo pipefail
cd "$(dirname "$0")/.." || exit 1

export JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null)"
[ -n "${JAVA_HOME:-}" ] || { echo "JDK 17 not found — run scripts/preflight.sh"; exit 1; }
SDK="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
ADB="$SDK/platform-tools/adb"
[ -x "$ADB" ] || { echo "Android SDK not found — run scripts/preflight.sh"; exit 1; }

serial=$("$ADB" devices | awk 'NR>1 && $2=="device"{print $1; exit}')
[ -n "$serial" ] || { echo "No authorized phone connected — run scripts/preflight.sh"; exit 1; }
export ANDROID_SERIAL="$serial"

./gradlew :app:installDebug
"$ADB" -s "$serial" shell monkey -p com.workout.tracker -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
echo "Installed & launched on $serial."

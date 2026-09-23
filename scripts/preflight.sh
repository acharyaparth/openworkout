#!/usr/bin/env bash
# Preflight: verify (and where possible auto-install) the toolchain to build this Android app.
# Safe to run repeatedly. Tested on macOS. ANDROID ONLY.
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1

SDK_DEFAULT="/opt/homebrew/share/android-commandlinetools"
MISSING=0
echo "== OpenWorkout preflight (Android only) =="

case "$(uname -s)" in
  Darwin) : ;;
  *) echo "!!  Tested on macOS. On $(uname -s), install JDK 17 + Android SDK (platform 35) via your package manager, then set ANDROID_HOME." ;;
esac

# --- JDK 17 (needs the user's password, so we cannot auto-install it) ---
if /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
  echo "ok  JDK 17"
else
  echo "!!  JDK 17 missing. Install it yourself (asks for your password):"
  echo "      brew install --cask temurin@17"
  MISSING=1
fi

# --- Android SDK (auto-install via Homebrew if absent) ---
SDK=""
for p in "${ANDROID_HOME:-}" "$SDK_DEFAULT" "$HOME/Library/Android/sdk"; do
  [ -n "$p" ] && [ -x "$p/platform-tools/adb" ] && SDK="$p" && break
done
if [ -z "$SDK" ]; then
  if command -v brew >/dev/null 2>&1; then
    echo ".. installing Android SDK via Homebrew (no password needed)…"
    brew install --cask android-commandlinetools >/dev/null 2>&1
    SDK="$SDK_DEFAULT"
    yes | sdkmanager --sdk_root="$SDK" --licenses >/dev/null 2>&1
    sdkmanager --sdk_root="$SDK" "platform-tools" "platforms;android-35" "build-tools;35.0.0" >/dev/null 2>&1
  else
    echo "!!  Homebrew not found — install the Android SDK manually (platform-tools, platform 35, build-tools 35)."
    MISSING=1
  fi
fi
if [ -n "$SDK" ] && [ -x "$SDK/platform-tools/adb" ]; then
  echo "ok  Android SDK ($SDK)"
  printf 'sdk.dir=%s\n' "$SDK" > local.properties
  echo "ok  wrote local.properties"
  ADB="$SDK/platform-tools/adb"
  dev=$("$ADB" devices | awk 'NR>1 && $2=="device"{print $1}')
  if [ -n "$dev" ]; then
    echo "ok  phone connected: $dev"
  else
    echo "!!  No authorized Android phone. Enable Developer Options + USB debugging, plug in via USB,"
    echo "    tap 'Allow' on the phone, then rerun this script."
    MISSING=1
  fi
else
  MISSING=1
fi

if [ "$MISSING" -ne 0 ]; then
  echo "== resolve the !! items above, then rerun scripts/preflight.sh =="
  exit 1
fi
echo "== preflight OK — ready to build =="

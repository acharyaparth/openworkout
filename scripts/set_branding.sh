#!/usr/bin/env bash
# Apply the user's branding.  Usage: scripts/set_branding.sh "App Name" "#RRGGBB"
set -euo pipefail
cd "$(dirname "$0")/.." || exit 1
NAME="${1:?usage: set_branding.sh \"App Name\" \"#RRGGBB\"}"
HEX="${2:?usage: set_branding.sh \"App Name\" \"#RRGGBB\"}"

# app name -> strings.xml (escape XML & and the apostrophe)
esc=$(printf '%s' "$NAME" | sed "s/&/\&amp;/g; s/'/\\\\'/g")
perl -0pi -e "s|(<string name=\"app_name\">).*?(</string>)|\${1}${esc}\${2}|s" \
  app/src/main/res/values/strings.xml

# accent color -> Theme.kt (AppGreen) + a ~70% darker AppGreenDim
argb=$(printf '%s' "$HEX" | sed 's/#//' | tr 'a-f' 'A-F')
[ ${#argb} -eq 6 ] || { echo "Color must be #RRGGBB"; exit 1; }
r=$((16#${argb:0:2})); g=$((16#${argb:2:2})); b=$((16#${argb:4:2}))
dim=$(printf '%02X%02X%02X' $((r*7/10)) $((g*7/10)) $((b*7/10)))
theme=app/src/main/java/com/workout/tracker/ui/theme/Theme.kt
perl -pi -e "s/val AppGreen = Color\(0x[0-9A-Fa-f]{8}\)/val AppGreen = Color(0xFF$argb)/" "$theme"
perl -pi -e "s/val AppGreenDim = Color\(0x[0-9A-Fa-f]{8}\)/val AppGreenDim = Color(0xFF$dim)/" "$theme"

echo "Set app name '$NAME' and accent #$argb (dim #$dim)."

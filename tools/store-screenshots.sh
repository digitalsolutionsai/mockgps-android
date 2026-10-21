#!/bin/bash
# Captures Play Store phone screenshots (1080x2160, within Play's 2:1 limit) for each app locale.
# Usage: tools/store-screenshots.sh [locale ...]   (needs a running emulator/device, app installed and set up)
set -euo pipefail
cd "$(dirname "$0")/.."
PKG=vn.dsai.mockgps
OUT=docs/play-store/graphics/screenshots
LOCALES=("$@")
[ ${#LOCALES[@]} -eq 0 ] && LOCALES=(en-US vi-VN es-ES pt-BR in-ID hi-IN ja-JP ko-KR zh-CN zh-TW ru-RU de-DE fr-FR tr-TR th-TH)

adb shell wm size 1080x2160
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
trap 'adb shell wm size reset; adb shell settings put global window_animation_scale 1; adb shell settings put global transition_animation_scale 1' EXIT

shot() { adb exec-out screencap -p > "$1"; }

# Opens the overflow menu and taps its Nth item, whatever the language (the dump only holds the popup).
menu_item() {
  for _ in 1 2 3 4 5; do
    adb shell input tap 985 158; sleep 1.5
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 || true
    xy=$(adb shell cat /sdcard/ui.xml | python3 -c '
import re, sys
n = int(sys.argv[1])
items = sorted((int(y1), (int(x1) + int(x2)) // 2, (int(y1) + int(y2)) // 2)
               for x1, y1, x2, y2 in re.findall(r"text=\"[^\"]+\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", sys.stdin.read()))
if len(items) == 4: print(items[n - 1][1], items[n - 1][2])' "$1")
    if [ -n "$xy" ]; then adb shell input tap $xy; return 0; fi
    adb shell input keyevent KEYCODE_BACK; sleep 1
  done
  echo "menu not found" >&2; return 1
}

for L in "${LOCALES[@]}"; do
  dir="$OUT/$L"; mkdir -p "$dir"
  echo "== $L"
  adb shell cmd locale set-app-locales $PKG --locales "$L" >/dev/null
  tools/mockgps stop >/dev/null 2>&1 || true
  adb shell am force-stop $PKG
  tools/mockgps joystick off >/dev/null 2>&1 || true

  # 1. Fixed point near Ben Thanh market, running.
  tools/mockgps set 10.772500 106.698000
  adb shell am start -n $PKG/.MainActivity >/dev/null; sleep 7
  adb shell input swipe 330 760 330 760 900; sleep 4      # long-press: drop a pin next to it
  shot "$dir/1-point.png"

  # 2. Route playing on real roads.
  tools/mockgps route 10.779783,106.699018 10.772450,106.698060 10.767700,106.705300 --travel motorbike --speed 30
  sleep 3
  adb shell input tap 540 1700; sleep 1                    # "Route" tab (middle segment)
  sleep 8
  shot "$dir/2-route.png"

  # 3. Route options (sheet dragged up).
  adb shell input swipe 540 1600 540 400 900; sleep 3
  shot "$dir/3-route-options.png"
  adb shell input swipe 540 760 540 2000 600; sleep 2

  # 4. Floating joystick over the map (default position sits above the sheet).
  adb shell input tap 240 1694; sleep 1                    # "Point" tab
  tools/mockgps set 10.772500 106.698000
  tools/mockgps joystick on; sleep 4
  shot "$dir/4-joystick.png"
  tools/mockgps joystick off

  # 5. adb control screen (third menu item).
  menu_item 3; sleep 2
  shot "$dir/5-adb.png"
  adb shell input keyevent KEYCODE_BACK; sleep 1
  tools/mockgps stop
done
adb shell cmd locale set-app-locales $PKG --locales "" >/dev/null || true

# 256-colour palette (libimagequant via Pillow): about 3x smaller, visually identical.
python3 - "$OUT" "${LOCALES[@]}" <<'PY'
import pathlib, sys
from PIL import Image
out = pathlib.Path(sys.argv[1])
for locale in sys.argv[2:]:
    for f in (out / locale).glob("*.png"):
        Image.open(f).convert("RGB").quantize(256, method=Image.Quantize.LIBIMAGEQUANT).save(f, optimize=True)
PY
echo "Saved to $OUT"

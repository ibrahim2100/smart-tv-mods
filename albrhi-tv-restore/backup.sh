#!/bin/bash
# Pulls the TV's layout copy (app order, hidden apps, HDMI names) into ./backups/.
# restore.sh pushes backups/layout-latest.json back when it reinstalls the launcher.
cd "$(dirname "$0")"
. ./config.sh
DEV="${1:-$DEV_DEFAULT}"
SRC="/sdcard/Android/data/$APP_PKG/files/layout.json"

adb connect "$DEV" >/dev/null 2>&1
adb -s "$DEV" get-state >/dev/null 2>&1 || { echo "TV not reachable at $DEV"; exit 1; }
mkdir -p backups
STAMP="$(date +%Y%m%d-%H%M%S)"
if adb -s "$DEV" pull "$SRC" "backups/layout-$STAMP.json" >/dev/null 2>&1; then
  cp "backups/layout-$STAMP.json" backups/layout-latest.json
  # keep the 20 newest dated copies
  ls -1t backups/layout-2*.json 2>/dev/null | tail -n +21 | xargs -I{} rm -f "{}"
  echo "saved backups/layout-$STAMP.json"
else
  echo "no layout file on the TV yet (open Albrhi Home once, or move/hide an app)"; exit 1
fi

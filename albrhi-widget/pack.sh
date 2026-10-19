#!/bin/bash
# Refreshes ../albrhi-tv-restore with the latest APK and source. Run after every build.
set -e
cd "$(dirname "$0")"
OUT=../albrhi-tv-restore
mkdir -p "$OUT/source"
cp albrhi-widget.apk "$OUT/"
rm -rf "$OUT/source/src" "$OUT/source/res"
cp -R src res AndroidManifest.xml build.sh pack.sh debug.keystore "$OUT/source/"
echo "pack updated: $(ls -la "$OUT/albrhi-widget.apk" | awk '{print $5}') bytes APK"

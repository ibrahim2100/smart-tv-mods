#!/bin/bash
# Installs Albrhi Home on a TCL Google TV the safe way: BESIDE the stock launcher.
#   ./tcl-install.sh <ip:port> [--layout path/to/layout.json] [--accessibility]
#
# It takes a restore-point backup first, installs the app, and gives it the permissions it asks for.
# It does NOT disable any app, does NOT make Albrhi Home the default Home and does NOT touch the screensaver or any
# other setting. It also does NOT give the app the WRITE_SECURE_SETTINGS permission: with that permission the app turns
# its own accessibility service on the first time it opens (that is how the custom remote buttons keep working), so
# add --accessibility only when you want that. Switching Home over is a separate, deliberate step
# (and ./tcl-restore.sh revert undoes everything).
cd "$(dirname "$0")" || exit 1
DEV="${1:-${ADB_DEVICE:-}}"
LAYOUT=""; ACCESS=0
[ "${2:-}" = "--layout" ] && LAYOUT="${3:-}"
for a in "$@"; do [ "$a" = "--accessibility" ] && ACCESS=1; done
[ -n "$DEV" ] || { sed -n '2,12p' "$0" | sed 's/^# \{0,1\}//'; exit 2; }
APP=com.albrhi.tvwidget
APK=albrhi-widget.apk
# use the newest build we can find, so a stale copy is never installed by mistake
[ -f ../albrhi-widget/albrhi-widget.apk ] && { [ ! -f "$APK" ] || [ ../albrhi-widget/albrhi-widget.apk -nt "$APK" ]; } && APK=../albrhi-widget/albrhi-widget.apk
[ -f "$APK" ] || { echo "no albrhi-widget.apk (build it: cd ../albrhi-widget && ./build.sh)"; exit 1; }
A() { adb -s "$DEV" shell "$@" | tr -d '\r'; }

adb connect "$DEV" >/dev/null 2>&1
[ "$(A getprop ro.product.manufacturer)" = "TCL" ] || { echo "Only for TCL TVs. Nothing was changed."; exit 1; }

echo "==> restore-point backup"
./tcl-restore.sh backup "$DEV" | sed 's/^/    /'
[ -f backups/tcl-original.txt ] || { cp backups/tcl-latest-state.txt backups/tcl-original.txt && echo "    saved this state as backups/tcl-original.txt (the 'before' picture)"; }

echo "==> installing"
adb -s "$DEV" install -r "$APK" | tail -1
for p in ACCESS_FINE_LOCATION POST_NOTIFICATIONS DUMP PACKAGE_USAGE_STATS; do A "pm grant $APP android.permission.$p" >/dev/null 2>&1; done
A "appops set $APP SYSTEM_ALERT_WINDOW allow"
A "appops set $APP REQUEST_INSTALL_PACKAGES allow"
A "appops set $APP GET_USAGE_STATS allow"
A "appops set $APP SCHEDULE_EXACT_ALARM allow" >/dev/null 2>&1
[ "$ACCESS" = 1 ] && A "pm grant $APP android.permission.WRITE_SECURE_SETTINGS" >/dev/null 2>&1 && echo "    accessibility permission granted (the app will switch its remote-button service on by itself)"

if [ -n "$LAYOUT" ] && [ -f "$LAYOUT" ]; then
  # the app must create its own hand-over folder (a folder made by adb is invisible to it on this Android), so open it
  # once, wait until the folder exists, then push the layout and open the app again
  A "am start -n $APP/.HomeActivity" >/dev/null
  # (A pipes through tr, so its exit status says nothing: look at what the TV prints)
  for i in $(seq 1 40); do [ "$(A "[ -d /sdcard/Android/data/$APP/files/import ] && echo yes")" = "yes" ] && break; sleep 1; done
  if adb -s "$DEV" push "$LAYOUT" "/sdcard/Android/data/$APP/files/import/layout.json" >/dev/null 2>&1; then
    A "am force-stop $APP"; sleep 1
    A "am start -n $APP/.HomeActivity" >/dev/null; sleep 9
    # check by looking at what the app now holds, not just that the file went away
    adb -s "$DEV" pull "/sdcard/Android/data/$APP/files/layout.json" /tmp/tcl-after.$$ >/dev/null 2>&1
    if python3 - "$LAYOUT" /tmp/tcl-after.$$ <<'PY'
import json,sys
want=json.load(open(sys.argv[1])).get('ui',{}); have=json.load(open(sys.argv[2])).get('ui',{})
sys.exit(0 if all(have.get(k)==v for k,v in want.items()) else 1)
PY
    then echo "    layout applied (checked)"; else echo "    WARNING: the layout was not picked up; set the look from Quick settings instead"; fi
    rm -f /tmp/tcl-after.$$
  else
    echo "    WARNING: could not hand over the layout (the app's folder was not ready); set the look from Quick settings instead"
  fi
fi

echo "==> done. Open it with: adb -s $DEV shell am start -n $APP/.HomeActivity"
echo "    The TV's own Home is unchanged. To undo everything: ./tcl-restore.sh revert $DEV"

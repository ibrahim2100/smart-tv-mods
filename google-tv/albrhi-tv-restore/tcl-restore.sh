#!/bin/bash
# Restore point for a TCL Google TV that has Albrhi Home installed.
#
#   ./tcl-restore.sh backup <ip:port>   record the TV's current state (kept in backups/tcl-*), plus our own layout
#   ./tcl-restore.sh revert <ip:port>   put the TV back exactly as it was before Albrhi Home, then remove Albrhi Home
#   ./tcl-restore.sh verify <ip:port>   compare the TV with that original state (changes nothing)
#
#   add --lock to `revert` to also switch off Developer options and USB debugging at the very end
#   (the connection ends then; turn them on again from the TV's own settings if you need adb)
#
# The address can also come from the ADB_DEVICE environment variable. Nothing here is specific to one TV's address.
# It refuses to run on anything that is not a TCL, and it only ever: re-enables packages, puts back the handful of
# settings listed below, and uninstalls com.albrhi.tvwidget. It never deletes a TV app or its data.
cd "$(dirname "$0")" || exit 1

APP=com.albrhi.tvwidget
STOCK="com.google.android.apps.tv.launcherx com.google.android.tungsten.setupwraith"
SECURE_KEYS="screensaver_components screensaver_enabled enabled_accessibility_services accessibility_enabled location_mode"
ORIGINAL=backups/tcl-original.txt

CMD="${1:-}"
DEV="${2:-${ADB_DEVICE:-}}"
LOCK=0
for a in "$@"; do [ "$a" = "--lock" ] && LOCK=1; done
[ "${DEV#--}" != "$DEV" ] && DEV="${ADB_DEVICE:-}"
if [ -z "$CMD" ] || [ -z "$DEV" ]; then
  sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'
  exit 2
fi

A() { adb -s "$DEV" shell "$@" | tr -d '\r'; }

adb connect "$DEV" >/dev/null 2>&1
[ "$(adb -s "$DEV" get-state 2>/dev/null)" = "device" ] || { echo "TV not reachable at $DEV (is USB/Wireless debugging on, and was the prompt on the screen accepted?)"; exit 1; }
BRAND="$(A getprop ro.product.manufacturer)"
[ "$BRAND" = "TCL" ] || { echo "This restore point is only for TCL TVs (this one says '$BRAND'). Nothing was changed."; exit 1; }

# One line per fact we care about. Everything else on the TV is never touched.
read_state() {
  echo "brand=$BRAND"
  echo "model=$(A getprop ro.product.model)"
  echo "android=$(A getprop ro.build.version.release)"
  echo "home=$(A cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1 | cut -d/ -f1)"
  echo "disabled=$(A pm list packages -d | sed 's/^package://' | sort | tr '\n' ' ' | sed 's/ $//')"
  for k in $SECURE_KEYS; do
    v="$(A settings get secure "$k")"; [ "$v" = "null" ] && v=""
    echo "secure.$k=$v"
  done
  v="$(A settings get system screen_off_timeout)"; [ "$v" = "null" ] && v=""
  echo "system.screen_off_timeout=$v"
  echo "dev.development_settings_enabled=$(A settings get global development_settings_enabled)"
  echo "dev.adb_enabled=$(A settings get global adb_enabled)"
  echo "ours=$(A pm list packages "$APP" | grep -c "^package:$APP\$")"
}

# the part of the state that Albrhi Home could ever have changed
comparable() { grep -vE '^(ours|dev\.|android=|model=)' "$1"; }

get() { grep -m1 "^$1=" "$2" | cut -d= -f2-; }

case "$CMD" in
backup)
  mkdir -p backups
  D="backups/tcl-$(date +%Y%m%d-%H%M%S)"
  mkdir -p "$D"
  read_state > "$D/state.txt"
  A pm list packages -3 | sed 's/^package://' | sort > "$D/user-apps.txt"
  A "appops get $APP" > "$D/appops.txt" 2>/dev/null
  adb -s "$DEV" pull "/sdcard/Android/data/$APP/files/layout.json" "$D/layout.json" >/dev/null 2>&1 && echo "saved our layout too"
  cp "$D/state.txt" backups/tcl-latest-state.txt
  # keep the fullest layout as "latest": a freshly installed app has an empty one that must not overwrite your real one
  if [ -f "$D/layout.json" ]; then
    if [ ! -f backups/tcl-latest-layout.json ] || [ "$(wc -c < "$D/layout.json")" -ge "$(wc -c < backups/tcl-latest-layout.json)" ]; then cp "$D/layout.json" backups/tcl-latest-layout.json; fi
  fi
  echo "backup written to $D"
  [ -f "$ORIGINAL" ] || echo "note: $ORIGINAL (the state from before Albrhi Home) does not exist yet; \`revert\` needs it. Create it with: cp $D/state.txt $ORIGINAL   (only if Albrhi Home had not changed any setting yet)"
  ;;

verify)
  [ -f "$ORIGINAL" ] || { echo "no $ORIGINAL to compare with"; exit 1; }
  read_state > /tmp/tcl-now.$$
  if diff <(comparable "$ORIGINAL") <(comparable /tmp/tcl-now.$$); then
    echo "OK: the TV matches its original state (Albrhi Home installed: $(get ours /tmp/tcl-now.$$))"
    rm -f /tmp/tcl-now.$$
  else
    echo "DIFFERENT from the original (lines above: < original, > now)"
    rm -f /tmp/tcl-now.$$
    exit 1
  fi
  ;;

revert)
  [ -f "$ORIGINAL" ] || { echo "no $ORIGINAL: refusing to guess what 'original' is."; exit 1; }
  echo "==> taking a safety backup first"
  "$0" backup "$DEV" | sed 's/^/    /'

  echo "==> 1/5 switching the stock Google TV home and setup back on (harmless if already on)"
  for p in $STOCK; do A "pm enable $p" | sed 's/^/    /'; done

  echo "==> 2/5 re-enabling anything that is switched off now but was not at the start"
  ORIG_DIS=" $(get disabled "$ORIGINAL") "
  for p in $(A pm list packages -d | sed 's/^package://'); do
    case "$ORIG_DIS" in *" $p "*) ;; *) echo "    enabling $p"; A "pm enable $p" >/dev/null ;; esac
  done

  echo "==> 3/5 putting the settings back"
  for k in $SECURE_KEYS; do
    v="$(get secure.$k "$ORIGINAL")"
    if [ -z "$v" ]; then A "settings delete secure $k" >/dev/null; else A "settings put secure $k $v" >/dev/null; fi
  done
  v="$(get system.screen_off_timeout "$ORIGINAL")"; [ -n "$v" ] && A "settings put system screen_off_timeout $v" >/dev/null

  echo "==> 4/5 removing Albrhi Home (its settings, permissions and files go with it)"
  A "pm clear-package-preferred-activities $APP" >/dev/null 2>&1
  A "pm uninstall $APP" | sed 's/^/    /'
  A "input keyevent KEYCODE_HOME"

  echo "==> 5/5 checking"
  sleep 3
  "$0" verify "$DEV"
  RC=$?
  if [ "$LOCK" = 1 ] && [ $RC = 0 ]; then
    echo "==> --lock: switching Developer options and USB debugging off (the connection ends now)"
    A "settings put global development_settings_enabled 0" >/dev/null
    A "settings put global adb_enabled 0" >/dev/null
  fi
  exit $RC
  ;;

*)
  sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'
  exit 2
  ;;
esac

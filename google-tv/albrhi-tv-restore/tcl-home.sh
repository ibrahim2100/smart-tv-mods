#!/bin/bash
# Makes Albrhi Home the Home of a TCL Google TV, or gives Home back to Google TV.
#   ./tcl-home.sh <ip:port> on     disable Google TV's launcher (pm disable-user: reversible), make ours the Home
#   ./tcl-home.sh <ip:port> off    switch Google TV's launcher back on and make it the Home again (ours stays installed)
#
# Only `pm disable-user` / `pm enable` on the packages named below, plus the default-Home choice. Nothing is deleted.
# The TCL tab and the TCL Channel rows of Google TV's own home go away while it is disabled.
# ./tcl-restore.sh revert undoes this too (and uninstalls Albrhi Home).
cd "$(dirname "$0")" || exit 1
DEV="${1:-${ADB_DEVICE:-}}"
MODE="${2:-}"
[ -n "$DEV" ] && { [ "$MODE" = on ] || [ "$MODE" = off ]; } || { sed -n '2,8p' "$0" | sed 's/^# \{0,1\}//'; exit 2; }
APP=com.albrhi.tvwidget
GTV=com.google.android.apps.tv.launcherx
SETUP=com.google.android.tungsten.setupwraith
A() { adb -s "$DEV" shell "$@" | tr -d '\r'; }
home() { A cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1 | cut -d/ -f1; }

adb connect "$DEV" >/dev/null 2>&1
[ "$(A getprop ro.product.manufacturer)" = "TCL" ] || { echo "Only for TCL TVs. Nothing was changed."; exit 1; }
[ -f backups/tcl-original.txt ] || { echo "no backups/tcl-original.txt: run ./tcl-install.sh first (it records the original state)"; exit 1; }

if [ "$MODE" = on ]; then
  [ "$(A pm list packages $APP | grep -c "^package:$APP\$")" = 1 ] || { echo "Albrhi Home is not installed on this TV"; exit 1; }
  ./tcl-restore.sh backup "$DEV" | sed 's/^/    /'
  echo "==> disabling Google TV's launcher (reversible)"
  A "pm disable-user --user 0 $GTV" | sed 's/^/    /'
  A "cmd package set-home-activity $APP/.HomeActivity" | sed 's/^/    /'
  A "input keyevent KEYCODE_HOME"; sleep 2
  if [ "$(home)" != "$APP" ]; then
    echo "==> another screen still answers the Home button ($(home)); disabling Google TV's setup screen too (reversible)"
    A "pm disable-user --user 0 $SETUP" | sed 's/^/    /'
    A "cmd package set-home-activity $APP/.HomeActivity" >/dev/null
    A "input keyevent KEYCODE_HOME"; sleep 2
  fi
else
  echo "==> giving Home back to Google TV"
  A "pm enable $GTV" | sed 's/^/    /'
  A "pm enable $SETUP" | sed 's/^/    /'
  A "pm clear-package-preferred-activities $APP" >/dev/null 2>&1
  A "input keyevent KEYCODE_HOME"; sleep 2
fi

H="$(home)"
echo "Home is now: $H"
if [ "$MODE" = on ]; then [ "$H" = "$APP" ] && echo "OK: Albrhi Home answers the Home button." || { echo "NOT OK: Home did not move to Albrhi Home. Undo with: ./tcl-home.sh $DEV off"; exit 1; }
else [ "$H" = "$GTV" ] && echo "OK: Google TV is the Home again." || { echo "NOT OK: Google TV is not the Home; run ./tcl-restore.sh revert $DEV"; exit 1; }; fi

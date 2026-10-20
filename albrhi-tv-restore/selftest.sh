#!/bin/bash
# selftest - one command that tells you whether the TV setup is healthy.
#   ./selftest.sh            checks, then measures smoothness with a scripted walk through the home screen
#   ./selftest.sh --quick    checks only (no key presses on the TV)
cd "$(dirname "$0")"
. ./config.sh
DEV="${TV:-$DEV_DEFAULT}"
A() { adb -s "$DEV" shell "$@" 2>/dev/null | tr -d '\r'; }
PASS=0; FAIL=0
ok()   { printf "  [ok]    %s\n" "$1"; PASS=$((PASS+1)); }
bad()  { printf "  [FAIL]  %s\n" "$1"; FAIL=$((FAIL+1)); }
note() { printf "  [info]  %s\n" "$1"; }

adb connect "$DEV" >/dev/null 2>&1
adb -s "$DEV" get-state >/dev/null 2>&1 || { echo "TV not reachable at $DEV"; exit 2; }

echo "== System"
[ "$(A 'cmd role get-role-holders --user 0 android.app.role.HOME' | tr -d '\n')" = "$APP_PKG" ] && ok "Albrhi Home is the default home" || bad "Albrhi Home is NOT the default home"
A 'pm list packages -d --user 0' | grep -q "$GTV_LAUNCHER" && ok "Google TV launcher disabled" || bad "Google TV launcher is enabled (HOME may go to it)"
A 'settings get secure enabled_accessibility_services' | grep -q ControlService && ok "remote-control service enabled" || bad "remote-control service off (run ./restore.sh)"
A "dumpsys activity services $APP_PKG" | grep -q "isForeground=true" && ok "background service running (alerts + phone)" || bad "background service not running"
curl -s -m 4 "http://${DEV%%:*}:8765/api/ping" | grep -q '"ok":true' && ok "phone server answering" || bad "phone server not answering"

echo "== Memory and storage"
AV=$(A "grep MemAvailable /proc/meminfo" | awk '{print int($2/1024)}')
[ "${AV:-0}" -ge 300 ] && ok "free RAM ${AV} MB" || bad "free RAM low: ${AV} MB (use 'Clean memory')"
PSS=$(A "dumpsys meminfo $APP_PKG" | awk '/TOTAL PSS:/{print int($3/1024)}')
[ "${PSS:-999}" -le 120 ] && ok "launcher uses ${PSS} MB" || bad "launcher uses ${PSS} MB (more than expected)"
FREE=$(A "df /data | tail -1" | awk '{print $4}')
note "free storage: $FREE"

echo "== Stability"
CR=$(A "logcat -d *:E" | grep -c "FATAL EXCEPTION.*\|Process: $APP_PKG")
[ "$CR" -eq 0 ] && ok "no crash of the launcher in the log" || bad "$CR crash line(s) in the log (adb logcat -d *:E | grep -A8 'FATAL EXCEPTION')"
for p in $DISABLE_PKGS; do A 'pm list packages -d --user 0' | grep -q "$p" || { bad "$p got re-enabled (run ./restore.sh)"; BAD_PKG=1; }; done
[ -z "$BAD_PKG" ] && ok "Haier / MediaTek apps still disabled"
python3 - "$(ls -1t backups/layout-2*.json 2>/dev/null | head -1)" <<'PY' 2>/dev/null
import json,sys
f=sys.argv[1]
if f and f!='':
    d=json.load(open(f)); n=len([x for x in d['order'].split(',') if x]); print("  [info]  layout backup has %d apps in order, %d hidden" % (n, len([x for x in d['hidden'].split(',') if x])))
PY

if [ "$1" != "--quick" ]; then
  echo "== Smoothness (walks left/right on the home screen)"
  A "am start -n $HOME_ACT" >/dev/null; sleep 2
  A "dumpsys gfxinfo $APP_PKG reset" >/dev/null
  A 'for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do input keyevent KEYCODE_DPAD_RIGHT; input keyevent KEYCODE_DPAD_LEFT; done' >/dev/null
  sleep 1
  G=$(A "dumpsys gfxinfo $APP_PKG")
  P90=$(echo "$G" | awk '/90th percentile/{gsub("ms","",$3); print $3; exit}')
  FR=$(echo "$G" | awk '/Total frames rendered/{print $4; exit}')
  JK=$(echo "$G" | awk '/^Janky frames:/{print $4; exit}')
  note "frames: ${FR}  ·  90th percentile: ${P90} ms  ·  janky: ${JK}"
  [ "${P90:-99}" -le 34 ] && ok "smooth (90% of frames under ${P90} ms; this TV composes 4K, so ~25 ms is its floor)" || bad "slow frames: ${P90} ms at the 90th percentile"
fi

echo
echo "result: $PASS ok, $FAIL failed"
[ "$FAIL" -eq 0 ]

#!/bin/bash
# Prints whether each customisation is currently in place.
cd "$(dirname "$0")"
. ./config.sh
DEV="${1:-$DEV_DEFAULT}"
A() { adb -s "$DEV" shell "$@" 2>/dev/null; }
ok()  { printf "  [ok]   %s\n" "$1"; }
bad() { printf "  [MISSING] %s\n" "$1"; }

adb connect "$DEV" | tail -1
adb -s "$DEV" get-state >/dev/null 2>&1 || { echo "TV not reachable"; exit 1; }
DIS="$(A 'pm list packages -d --user 0')"
ALL="$(A 'pm list packages --user 0')"

A "pm list packages $APP_PKG" | grep -q "$APP_PKG" && ok "Albrhi Home installed" || bad "Albrhi Home installed"
[ "$(A 'cmd role get-role-holders --user 0 android.app.role.HOME' | tr -d '\r\n')" = "$APP_PKG" ] && ok "Albrhi Home holds the HOME role" || bad "HOME role"
echo "$DIS" | grep -q "$GTV_LAUNCHER" && ok "Google TV launcher disabled" || bad "Google TV launcher disabled"
echo "$DIS" | grep -q "$GTV_SETUP"    && ok "setupwraith disabled"           || bad "setupwraith disabled"
for p in $DISABLE_PKGS; do echo "$DIS" | grep -q "$p" && ok "disabled $p" || bad "disabled $p"; done
for p in $REMOVE_SYSTEM_PKGS $REMOVE_USER_PKGS; do echo "$ALL" | grep -q "package:$p\$" && bad "removed $p" || ok "removed $p"; done
[ "$(A 'settings get secure screensaver_components' | tr -d '\r\n')" = "$DREAM" ] && ok "screensaver is Albrhi Barca" || bad "screensaver"
[ "$(A 'settings get secure location_mode' | tr -d '\r\n')" = "3" ] && ok "location on (Wi-Fi name visible)" || bad "location on (Wi-Fi name hidden)"
A "appops get $APP_PKG SYSTEM_ALERT_WINDOW" | grep -q "SYSTEM_ALERT_WINDOW: allow" && ok "alert banner permission" || bad "alert banner permission"
A "appops get $APP_PKG REQUEST_INSTALL_PACKAGES" | grep -q "allow" && ok "phone install permission" || bad "phone install permission"
A "settings get secure enabled_accessibility_services" | grep -q "ControlService" && ok "phone remote-control service enabled" || bad "phone remote-control service enabled"
curl -s -m 4 "http://${DEV%%:*}:8765/api/ping" | grep -q '"ok":true' && ok "phone install server answering" || bad "phone install server answering"
[ -f backups/layout-latest.json ] && ok "layout backup exists ($(ls -1 backups/layout-2*.json 2>/dev/null | wc -l | tr -d ' ') copies)" || bad "layout backup (run ./backup.sh)"
echo
echo "current HOME: $(A 'dumpsys window | grep mFocusedApp' | head -1 | sed 's/^ *//')"

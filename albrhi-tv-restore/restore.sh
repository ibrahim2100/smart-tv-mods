#!/bin/bash
# Re-applies every Albrhi TV customisation. Safe to run again after a system update.
#   ./restore.sh                 -> uses the default address in config.sh
#   ./restore.sh 192.168.1.50:5555
cd "$(dirname "$0")"
. ./config.sh
DEV="${1:-$DEV_DEFAULT}"
A() { adb -s "$DEV" shell "$@"; }

echo "==> connecting to $DEV"
adb connect "$DEV" | tail -1
adb -s "$DEV" get-state >/dev/null 2>&1 || { echo "TV not reachable (enable Wireless debugging / check the port: adb mdns services)"; exit 1; }

echo "==> 1/6 installing Albrhi Home (launcher + widget + screensaver)"
adb -s "$DEV" install -r albrhi-widget.apk || exit 1
# runtime permissions the launcher uses for Wi-Fi name (harmless if already granted)
A "pm grant $APP_PKG android.permission.ACCESS_FINE_LOCATION" 2>/dev/null
A "pm grant $APP_PKG android.permission.NEARBY_WIFI_DEVICES" 2>/dev/null
# match alerts: floating banner, exact alarms, foreground-service notification
A "pm grant $APP_PKG android.permission.POST_NOTIFICATIONS" 2>/dev/null
A "appops set $APP_PKG SYSTEM_ALERT_WINDOW allow"
A "appops set $APP_PKG SCHEDULE_EXACT_ALARM allow"
# phone install/uninstall (the TV still asks you to confirm each one with the remote)
A "appops set $APP_PKG REQUEST_INSTALL_PACKAGES allow"
# phone remote control (accessibility actions: focus, OK, back, home, typing)
A "settings put secure enabled_accessibility_services $APP_PKG/$APP_PKG.ControlService"
A "settings put secure accessibility_enabled 1"
# put back the saved app order / hidden apps / HDMI names (made by backup.sh)
if [ -f backups/layout-latest.json ]; then
  A "mkdir -p /sdcard/Android/data/$APP_PKG/files/import"
  adb -s "$DEV" push backups/layout-latest.json "/sdcard/Android/data/$APP_PKG/files/import/layout.json" >/dev/null && echo "layout backup queued for import"
fi
# Android only reveals the Wi-Fi name when Location services are on (was off = 0 originally)
A "settings put secure location_mode 3"

echo "==> 2/6 making Albrhi Home the default home"
A "cmd package set-home-activity $HOME_ACT"
A "am start -n $HOME_ACT" >/dev/null
sleep 2

echo "==> 3/6 switching off Google TV home (reversible: ./revert.sh)"
A "pm disable-user --user 0 $GTV_LAUNCHER"
A "pm disable-user --user 0 $GTV_SETUP"

echo "==> 4/6 disabling Haier / MediaTek apps"
for p in $DISABLE_PKGS; do A "pm disable-user --user 0 $p" | tail -1; done

echo "==> 5/6 removing unneeded apps"
for p in $REMOVE_SYSTEM_PKGS; do echo -n "$p: "; A "pm uninstall --user 0 $p" | tail -1; done
for p in $REMOVE_USER_PKGS; do echo -n "$p: "; A "pm uninstall $p" 2>&1 | tail -1; done

echo "==> 6/6 screensaver"
A "settings put secure screensaver_components $DREAM"
A "settings put secure screensaver_enabled 1"

echo
echo "done. Run ./check.sh to verify."

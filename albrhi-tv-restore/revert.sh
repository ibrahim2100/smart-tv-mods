#!/bin/bash
# Puts the TV back to Google TV as it was. Albrhi Home stays installed (harmless);
# add --full to uninstall it as well.
cd "$(dirname "$0")"
. ./config.sh
DEV="${1:-$DEV_DEFAULT}"
FULL=0; [ "$1" = "--full" ] && { FULL=1; DEV="$DEV_DEFAULT"; }; [ "$2" = "--full" ] && FULL=1
A() { adb -s "$DEV" shell "$@"; }

adb connect "$DEV" | tail -1
echo "==> re-enabling Google TV home"
A "pm enable $GTV_LAUNCHER"
A "pm enable $GTV_SETUP"
A "cmd package set-home-activity $GTV_HOME_ACT"
echo "==> re-enabling Haier / MediaTek apps"
for p in $DISABLE_PKGS; do A "pm enable $p" | tail -1; done
echo "==> bringing back removed system apps"
for p in $REMOVE_SYSTEM_PKGS; do A "pm install-existing --user 0 $p" | tail -1; done
echo "==> screensaver back to Google's"
A "settings put secure screensaver_components $GTV_DREAM"
[ "$FULL" = 1 ] && { echo "==> uninstalling Albrhi Home"; A "pm uninstall $APP_PKG" | tail -1; }
A "settings put secure location_mode 0"   # original value
A "input keyevent KEYCODE_HOME"
echo
echo "done. Note: 'Apps only mode' was switched ON in Google TV (Settings > Accounts & Profiles > your profile);"
echo "turn it off there if you want Google's recommendation rows back."
echo "Apps you installed yourself and we removed (${REMOVE_USER_PKGS}) must be reinstalled from Play Store."

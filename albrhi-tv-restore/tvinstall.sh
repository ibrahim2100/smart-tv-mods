#!/bin/bash
# tvinstall - send apps to the TV from this Mac over ADB.
#
#   ./tvinstall.sh install <file.apk | folder-with-split-apks | file.xapk | https://link-to.apk>
#   ./tvinstall.sh list                 apps installed by you (not system apps)
#   ./tvinstall.sh remove <package>     uninstall one of them
#   ./tvinstall.sh launch <package>     open an app on the TV
#   ./tvinstall.sh connect              just (re)connect
#
# The TV address comes from config.sh (override with TV=ip:port ./tvinstall.sh ...).
cd "$(dirname "$0")"
. ./config.sh
DEV="${TV:-$DEV_DEFAULT}"
A() { adb -s "$DEV" shell "$@" 2>&1 | grep -v "Init wrapper"; }

connect() {
  adb connect "$DEV" >/dev/null 2>&1
  adb -s "$DEV" get-state >/dev/null 2>&1 || { echo "TV not reachable at $DEV (check Wireless debugging / run: adb mdns services)"; exit 1; }
}

is_zip() { [ "$(head -c 2 "$1" 2>/dev/null)" = "PK" ]; }

install_one() {
  local src="$1" tmp=""
  case "$src" in
    http://*|https://*)
      tmp="$(mktemp -d)"; echo "downloading..."
      curl -fL --progress-bar -o "$tmp/download" "$src" || { echo "download failed"; rm -rf "$tmp"; exit 1; }
      is_zip "$tmp/download" || { echo "that link is not an APK/zip file"; rm -rf "$tmp"; exit 1; }
      case "$src" in *.xapk*|*.apks*) mv "$tmp/download" "$tmp/app.xapk"; src="$tmp/app.xapk" ;; *) mv "$tmp/download" "$tmp/app.apk"; src="$tmp/app.apk" ;; esac ;;
  esac

  if [ -d "$src" ]; then
    echo "installing split APKs from $src"
    adb -s "$DEV" install-multiple -r "$src"/*.apk
  else
    [ -f "$src" ] || { echo "no such file: $src"; exit 1; }
    case "$src" in
      *.xapk|*.apks)
        local work; work="$(mktemp -d)"; unzip -q "$src" -d "$work" || exit 1
        echo "installing bundle ($(ls "$work"/*.apk | wc -l | tr -d ' ') APKs)"
        adb -s "$DEV" install-multiple -r "$work"/*.apk
        # expansion files, if the bundle has them: Android/obb/<package>/
        if [ -d "$work/Android/obb" ]; then adb -s "$DEV" push "$work/Android/obb/." /sdcard/Android/obb/ ; fi
        rm -rf "$work" ;;
      *)
        is_zip "$src" || { echo "$src does not look like an APK"; exit 1; }
        adb -s "$DEV" install -r "$src" ;;
    esac
  fi
  [ -n "$tmp" ] && rm -rf "$tmp"
  # Android switches an accessibility service off when its app is replaced: put ours back (and the permissions it needs)
  if adb -s "$DEV" shell "pm list packages $APP_PKG" 2>/dev/null | grep -q "$APP_PKG"; then
    A "pm grant $APP_PKG android.permission.ACCESS_FINE_LOCATION" >/dev/null
    A "pm grant $APP_PKG android.permission.NEARBY_WIFI_DEVICES" >/dev/null
    A "pm grant $APP_PKG android.permission.POST_NOTIFICATIONS" >/dev/null
    A "settings put secure enabled_accessibility_services $APP_PKG/$APP_PKG.ControlService" >/dev/null
    A "settings put secure accessibility_enabled 1" >/dev/null
  fi
  echo "done. Open Albrhi Home on the TV - the app appears at the end of the grid (move it with a long press)."
}

case "$1" in
  install) [ -n "$2" ] || { echo "usage: $0 install <file.apk|folder|file.xapk|url>"; exit 1; }; connect; install_one "$2" ;;
  list)    connect; A 'pm list packages -3' | sed 's/package://' | sort ;;
  remove)  [ -n "$2" ] || { echo "usage: $0 remove <package>"; exit 1; }; connect; echo -n "remove $2 ? [y/N] "; read -r a; [ "$a" = y ] && A "pm uninstall $2" ;;
  launch)  [ -n "$2" ] || { echo "usage: $0 launch <package>"; exit 1; }; connect; A "monkey -p $2 -c android.intent.category.LEANBACK_LAUNCHER 1" | tail -1 ;;
  connect) connect; echo "connected to $DEV" ;;
  *) sed -n '2,12p' "$0" ;;
esac

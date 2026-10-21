#!/bin/bash
# uninstall-custom-home - removes our installed app from the TV entirely.
# Run revert-home.sh FIRST if the home-swap is active, otherwise the TV may be left
# without a working home screen until you reinstall something.
cd "$(dirname "$0")"
. ./config.sh
APP_ID="com.albrhi.tvhome"

echo "TV: $TV_IP"
echo -n "Uninstall '$APP_ID' from the TV? [y/N] "
read -r a
[ "$a" = "y" ] || { echo "cancelled"; exit 0; }

tv_sh "luna-send -n 1 -f luna://com.webos.appInstallService/remove '{\"id\":\"$APP_ID\"}'"
echo "done - check the app list with check.sh"

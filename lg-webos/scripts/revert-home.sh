#!/bin/bash
# revert-home - undo the "replace LG's home" swap and go back to the stock LG launcher.
# Safe to run even if the swap was never done (it just finds nothing to undo).
# Does NOT remove root or Homebrew Channel - it only restores the home screen and the
# settings it changes. To remove our app entirely too, run uninstall-custom-home.sh first.
cd "$(dirname "$0")"
. ./config.sh

echo "TV: $TV_IP"
echo -n "This restores the ORIGINAL LG Home screen and boot behaviour. Continue? [y/N] "
read -r a
[ "$a" = "y" ] || { echo "cancelled"; exit 0; }

echo "1) removing the home-swap boot hook..."
tv_sh "rm -f /var/lib/webosbrew/init.d/40-tvhome-as-home" >/dev/null
tv_sh "umount /media/developer/apps/usr/palm/applications/com.albrhi.tvhome/appinfo.json 2>/dev/null" >/dev/null

echo "2) restoring the settings as they were before the swap (backups/pre-home-swap-20261021.txt)..."
tv_sh "luna-send -n 1 luna://com.webos.settingsservice/setSystemSettings '{\"category\":\"general\",\"settings\":{\"lastInputApp\":\"com.webos.app.livetv\",\"physicalLastInputApp\":\"com.webos.app.livetv\",\"homeAutoLaunch\":\"on\",\"homePromotion\":\"on\",\"screenSaverEnabled\":\"on\",\"screenSaverAd\":\"on\"}}'" >/dev/null
tv_sh "rm -f /var/lib/webosbrew/tvhome-last-input.orig" >/dev/null

echo "3) rebooting the TV..."
tv_sh "reboot" >/dev/null

echo "done. The TV will restart and come back on the stock LG Home screen."
echo "Root and Homebrew Channel are untouched - 'Homebrew Channel' will still be in the app list."

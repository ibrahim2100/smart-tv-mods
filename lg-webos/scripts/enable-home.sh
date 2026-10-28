#!/bin/bash
# enable-home - make the app the TV's real home screen (Home button, Back from apps, start-up) and turn off LG's home promotions
# and screensaver (and its ads). Reversible: revert-home.sh puts every setting back. Read docs/SAFETY first.
cd "$(dirname "$0")"
. ./config.sh
SVC=/media/developer/apps/usr/palm/services/com.albrhi.tvhome.service
echo "TV: $TV_IP"; echo -n "Make Aljalsa TV the home screen? [y/N] "; read -r a; [ "$a" = "y" ] || { echo cancelled; exit 0; }
./backup.sh >/dev/null 2>&1 && echo "snapshot saved in scripts/backups/"
tv_sh "luna-send -n 1 luna://com.webos.settingsservice/setSystemSettings '{\"category\":\"general\",\"settings\":{\"homePromotion\":\"off\",\"screenSaverEnabled\":\"off\",\"screenSaverAd\":\"off\"}}'" >/dev/null
tv_sh "ln -sf $SVC/as-home.sh /var/lib/webosbrew/init.d/40-tvhome-as-home; nohup sh $SVC/as-home.sh >/dev/null 2>&1 &" >/dev/null
echo "done. webOS restarts its app manager: the screen may stay blank for up to ~90 seconds, then your home appears."
echo "If anything looks wrong: ./revert-home.sh"

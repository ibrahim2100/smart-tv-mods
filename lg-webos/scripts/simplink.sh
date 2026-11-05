#!/bin/bash
# simplink - the TV's SIMPLINK (HDMI-CEC) switch. With it ON, a console or box on HDMI that powers up can pull the TV's
# input over to itself by force (a PlayStation on HDMI1 does exactly that). Turning it OFF stops that.
#   ./simplink.sh status    show the two settings
#   ./simplink.sh off       SIMPLINK off (the TV no longer obeys HDMI devices; the TV remote no longer drives them either)
#   ./simplink.sh on        back to LG's default (both on)
# Only the `other` settings category keys simplinkEnable / simplinkAutoPowerOn are read or written. Nothing else is touched.
cd "$(dirname "$0")"
. ./config.sh
get() { tv_sh "luna-send -n 1 -f luna://com.webos.settingsservice/getSystemSettings '{\"category\":\"other\",\"keys\":[\"simplinkEnable\",\"simplinkAutoPowerOn\"]}' 2>&1" | grep -E 'simplink|returnValue' | tr -s ' '; }
case "${1:-status}" in
  status) get ;;
  off) tv_sh "luna-send -n 1 -f luna://com.webos.settingsservice/setSystemSettings '{\"category\":\"other\",\"settings\":{\"simplinkEnable\":\"off\"}}' 2>&1" | grep -E 'returnValue|errorText'; get ;;
  on)  tv_sh "luna-send -n 1 -f luna://com.webos.settingsservice/setSystemSettings '{\"category\":\"other\",\"settings\":{\"simplinkEnable\":\"on\",\"simplinkAutoPowerOn\":\"on\"}}' 2>&1" | grep -E 'returnValue|errorText'; get ;;
  *) sed -n '2,9p' "$0" | sed 's/^# \{0,1\}//'; exit 2 ;;
esac

#!/bin/bash
# check - what state is the TV in right now, compared to the original baseline.
cd "$(dirname "$0")"
. ./config.sh
PASS=0; WARN=0
ok()   { printf "  [ok]    %s\n" "$1"; PASS=$((PASS+1)); }
warn() { printf "  [!]     %s\n" "$1"; WARN=$((WARN+1)); }
note() { printf "  [info]  %s\n" "$1"; }

echo "TV: $TV_IP"
nc -z -G 2 "$TV_IP" 23 2>/dev/null && ok "telnet root shell reachable (port 23)" || { echo "TV not reachable - stop"; exit 2; }

echo "== Root / Homebrew Channel"
CFG=$(tv_sh "luna-send -n 1 -f luna://org.webosbrew.hbchannel.service/getConfiguration '{}' 2>&1")
echo "$CFG" | grep -q '"root": *true'    && ok "root: ok"                      || warn "root status not confirmed (Homebrew Channel app may be missing)"
echo "$CFG" | grep -q '"sshdEnabled": *true' && ok "SSH enabled in Homebrew Channel" || note "SSH not enabled (telnet still works on port 23)"
echo "$CFG" | grep -q '"failsafe": *false'   && ok "not in failsafe mode"       || warn "Homebrew Channel reports failsafe mode"

echo "== Home screen (is it still stock LG, or swapped to our app?)"
tv_sh "ls /var/lib/webosbrew/init.d/ 2>/dev/null" | grep -q "tvhome-as-home" \
  && note "home-swap is ACTIVE: our app is the TV's home (expected after enable-home.sh; revert-home.sh undoes it)" \
  || note "home-swap is not active: LG's own home is still the launcher (run enable-home.sh to switch)"

echo "== General settings vs. original baseline"
SET=$(tv_sh "luna-send -n 1 -f luna://com.webos.settingsservice/getSystemSettings '{\"category\":\"general\",\"keys\":[\"lastInputApp\",\"physicalLastInputApp\",\"homeAutoLaunch\"]}' 2>&1")
echo "$SET" | grep -q '"homeAutoLaunch": *"on"' && ok "homeAutoLaunch: on (LG default)" || note "homeAutoLaunch is off (expected while our app is the home)"
note "current lastInputApp / physicalLastInputApp:"
echo "$SET" | grep -E 'lastInputApp|physicalLastInputApp' | sed 's/^/    /'

echo "== Our custom app"
tv_sh "ls /media/cryptofs/apps/usr/palm/applications/ /media/developer/apps/usr/palm/applications/ 2>/dev/null" | grep -qi "albrhi" \
  && note "custom app (albrhi tv home) is installed" \
  || note "custom app not installed yet"

echo "== Helper service and alert card"
tv_sh "wget -qO- -T 5 --post-data '{}' http://127.0.0.1:45620/alertsGet" | grep -q '"returnValue":true' && ok "root service answers (match alerts on)" || warn "root service is not answering on 127.0.0.1:45620 (see docs/LESSONS)"
tv_sh "ls /media/developer/apps/usr/palm/applications/" | grep -q "com.albrhi.alert" && ok "alert card app installed" || warn "alert card app missing: run install-alert-app.sh"
echo
echo "result: $PASS ok, $WARN warning(s)"
[ "$WARN" -eq 0 ]

#!/bin/bash
# install-alert-app - one-time install of the match-alert card app (com.albrhi.alert) on the TV. After that, deploy.sh keeps its files up to date.
cd "$(dirname "$0")"
. ./config.sh
T=$(mktemp -d)
../node_modules/.bin/ares-package ../alert -o "$T" --no-minify >/dev/null 2>&1 || { echo "packaging failed (run: npm install in the project root)"; exit 1; }
IPK=$(ls "$T"/*.ipk | head -1)
tv_push "$IPK" /tmp/alert.ipk
tv_sh "luna-send -n 1 luna://com.webos.appInstallService/dev/install '{\"id\":\"com.albrhi.alert\",\"ipkUrl\":\"/tmp/alert.ipk\",\"subscribe\":true}' >/dev/null 2>&1 &" >/dev/null
echo "installing... (about 20 seconds)"; sleep 25
tv_sh "ls /media/developer/apps/usr/palm/applications | grep -c com.albrhi.alert" | tr -d '\r' | tail -1
rm -rf "$T"

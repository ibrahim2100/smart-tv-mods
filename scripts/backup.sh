#!/bin/bash
# backup - snapshot the TV's current app list and key settings into backups/.
# Run this any time before a risky change, same idea as the Haier tools.
cd "$(dirname "$0")"
. ./config.sh
TS=$(date +%Y%m%d-%H%M%S)
mkdir -p backups

echo "TV: $TV_IP"
echo "pulling app list..."
tv_sh "luna-send -n 1 -f luna://com.webos.applicationManager/listApps '{}' > /tmp/apps.json" >/dev/null
tv_pull /tmp/apps.json "backups/apps-$TS.json"

echo "pulling settings..."
tv_sh "luna-send -n 1 -f luna://com.webos.settingsservice/getSystemSettings '{\"category\":\"general\",\"keys\":[\"lastInputApp\",\"physicalLastInputApp\",\"homeAutoLaunch\"]}'" > "backups/settings-$TS.txt"
tv_sh "luna-send -n 1 -f luna://org.webosbrew.hbchannel.service/getConfiguration '{}'" >> "backups/settings-$TS.txt"
tv_sh "ls /var/lib/webosbrew/init.d/ 2>/dev/null" >> "backups/settings-$TS.txt"

echo "saved:"
ls -la "backups/apps-$TS.json" "backups/settings-$TS.txt" 2>/dev/null

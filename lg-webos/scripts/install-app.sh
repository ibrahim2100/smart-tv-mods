#!/bin/bash
# install-app - FIRST-TIME install of the home app + its root service (com.albrhi.tvhome) on a rooted TV.
# After this, use deploy.sh for updates. Same mechanism install-alert-app.sh uses for the alert card.
# Needs: npm install (once, in the project root), and the TV rooted with Homebrew Channel (telnet root shell, see docs/INSTALL).
cd "$(dirname "$0")"
. ./config.sh
T=$(mktemp -d)
../node_modules/.bin/ares-package ../app ../service -o "$T" --no-minify >/dev/null 2>&1 || { echo "packaging failed (run: npm install in the project root)"; exit 1; }
IPK=$(ls "$T"/com.albrhi.tvhome_*.ipk | head -1)
[ -n "$PACKAGE_ONLY" ] && { echo "packaged: $IPK (PACKAGE_ONLY set, not installing)"; ls -la "$IPK"; exit 0; }
tv_push "$IPK" /tmp/tvhome.ipk
tv_sh "luna-send -n 1 luna://com.webos.appInstallService/dev/install '{\"id\":\"com.albrhi.tvhome\",\"ipkUrl\":\"/tmp/tvhome.ipk\",\"subscribe\":true}' >/dev/null 2>&1 &" >/dev/null
echo "installing... (about 30 seconds)"; sleep 35
tv_sh "ls /media/developer/apps/usr/palm/applications | grep -c com.albrhi.tvhome; ls /media/developer/apps/usr/palm/services | grep -c com.albrhi.tvhome.service" | tr -d '\r' | tail -2
echo "next: ./install-alert-app.sh, then ./enable-home.sh (makes it the TV's home screen)"
rm -rf "$T"

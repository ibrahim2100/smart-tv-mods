#!/bin/bash
# deploy - push the current app/, service/ and alert/ folders to the TV and restart what changed. No TV restart needed.
# (TV Home runs from a RAM copy made at boot by as-home.sh; this refreshes that copy too and the page reloads itself.)
cd "$(dirname "$0")"
. ./config.sh
APP=/media/developer/apps/usr/palm/applications/com.albrhi.tvhome
SVC=/media/developer/apps/usr/palm/services/com.albrhi.tvhome.service
ALERT=/media/developer/apps/usr/palm/applications/com.albrhi.alert   # the match-alert card app (installed once: install-alert-app.sh)
T=$(mktemp -d)
echo "TV: $TV_IP"
( cd ../app && COPYFILE_DISABLE=1 tar czf "$T/app.tgz" index.html app.js style.css config.js perf.js bg.jpg fonts icons )
( cd ../service && COPYFILE_DISABLE=1 tar czf "$T/svc.tgz" service.js net.js uefa.js as-home.sh autostart.sh package.json services.json )
( cd ../alert && COPYFILE_DISABLE=1 tar czf "$T/alert.tgz" appinfo.json index.html icon.png fonts crests )
tv_push "$T/app.tgz" /tmp/app.tgz
tv_push "$T/alert.tgz" /tmp/alert.tgz
tv_push "$T/svc.tgz" /tmp/svc.tgz
tv_sh "cd $APP && rm -rf icons fonts && tar xzf /tmp/app.tgz && chmod -R a+rX . && cd $SVC && tar xzf /tmp/svc.tgz && chmod a+rx *.sh && chmod a+r *.js *.json && rm -f $SVC/alerts.js /tmp/app.tgz /tmp/svc.tgz; if [ -d $ALERT ]; then cd $ALERT && rm -rf fonts crests && tar xzf /tmp/alert.tgz && chmod -R a+rX . && echo alert-app-updated; else echo 'ALERT APP NOT INSTALLED: run install-alert-app.sh'; fi; rm -f /tmp/alert.tgz" | tr -d '\r' | grep -E 'alert-app|NOT INSTALLED'
rm -rf "$T"
# TV Home runs from a RAM copy made at boot (as-home.sh): refresh that copy too (never its appinfo.json), restart the
# root service. The running page notices the change (service 'build') and reloads itself. No TV restart needed.
# DON'T kill the home page's process or close/launch it by hand: the system then stops bringing it to the front.
tv_sh "R=/tmp/tvhome-as-home; if [ -d \$R ]; then cd $APP && cp index.html app.js style.css config.js perf.js bg.jpg \$R/ && rm -rf \$R/icons \$R/fonts && cp -r icons fonts \$R/ && echo ram-copy-updated; fi; nohup sh $SVC/autostart.sh </dev/null >/dev/null 2>&1 &" | tr -d '\r' | tail -2
echo "done. TV Home reloads itself within ~15 seconds. Never kill its process: the system then loses track of the home app (needs a TV restart)."

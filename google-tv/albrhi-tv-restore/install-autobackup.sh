#!/bin/bash
# OPTIONAL: makes this Mac pull the TV's layout copy every day at 12:00 (launchd agent).
#   ./install-autobackup.sh          install
#   ./install-autobackup.sh remove   uninstall
cd "$(dirname "$0")"
DIR="$(pwd)"
PLIST="$HOME/Library/LaunchAgents/com.albrhi.tvbackup.plist"
if [ "$1" = "remove" ]; then
  launchctl unload "$PLIST" 2>/dev/null; rm -f "$PLIST"; echo "removed"; exit 0
fi
cat > "$PLIST" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>Label</key><string>com.albrhi.tvbackup</string>
  <key>ProgramArguments</key><array><string>/bin/bash</string><string>$DIR/backup.sh</string></array>
  <key>StartCalendarInterval</key><dict><key>Hour</key><integer>12</integer><key>Minute</key><integer>0</integer></dict>
  <key>EnvironmentVariables</key><dict><key>PATH</key><string>/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin</string></dict>
  <key>StandardOutPath</key><string>$DIR/backups/autobackup.log</string>
  <key>StandardErrorPath</key><string>$DIR/backups/autobackup.log</string>
</dict></plist>
EOF
mkdir -p "$DIR/backups"
launchctl unload "$PLIST" 2>/dev/null; launchctl load "$PLIST"
echo "installed: daily backup at 12:00 -> $DIR/backups  (undo: ./install-autobackup.sh remove)"

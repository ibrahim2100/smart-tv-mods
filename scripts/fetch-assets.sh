#!/bin/bash
# fetch-assets - rebuild the third-party artwork that is NOT stored in git (trademarked): app icons, club crests, PlayStation/beIN/HDMI logos.
# See docs/ASSETS. Output goes to app/ and alert/ unless OUT is set (OUT=/some/dir ./fetch-assets.sh to try it without touching the project).
cd "$(dirname "$0")"
. ./config.sh
ROOT="$(cd .. && pwd)"; OUT="${OUT:-$ROOT}"
mkdir -p "$OUT/app/icons/apps" "$OUT/app/icons/logos" "$OUT/app/icons/crests" "$OUT/alert/crests"

echo "1/3 app icons: copied from the TV itself (needs the TV on and rooted)"
tv_sh "luna-send -n 1 -w 8000 luna://com.webos.applicationManager/listLaunchPoints '{}' > /tmp/lp.json" >/dev/null
tv_pull /tmp/lp.json "$OUT/lp.json"
python3 - "$OUT/lp.json" <<'PY' > "$OUT/pull.list"
import json, sys
for p in json.load(open(sys.argv[1]))["launchPoints"]:
    icon = p.get("largeIcon") or p.get("icon")
    if icon and not p.get("hidden"): print(p["id"], icon)
PY
while read -u 3 aid src; do tv_pull "$src" "$OUT/app/icons/apps/$aid.png" </dev/null; done 3< "$OUT/pull.list"
rm -f "$OUT/lp.json" "$OUT/pull.list"

echo "2/3 club crests: ESPN's public image server (same source the match cards use)"
for pair in "83 barcelona" "86 madrid" "929 hilal" "817 nassr"; do set -- $pair
  curl -fsSL -o "$OUT/app/icons/crests/$2.png" "https://a.espncdn.com/i/teamlogos/soccer/500/$1.png" && cp "$OUT/app/icons/crests/$2.png" "$OUT/alert/crests/$2.png"
  command -v sips >/dev/null && { sips -Z 220 "$OUT/app/icons/crests/$2.png" >/dev/null 2>&1; sips -Z 220 "$OUT/alert/crests/$2.png" >/dev/null 2>&1; }
done

echo "3/3 PlayStation / beIN / HDMI logos: SVGs from Wikimedia Commons, rendered to PNG with headless Chrome (macOS path below)"
CH="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
if [ -x "$CH" ]; then
  T=$(mktemp -d)
  dl() { curl -fsSL -o "$T/$1.svg" "$2"; }
  render() { # name w h [invert-to-white]
    [ -n "$4" ] && sed -i '' 's/fill:#000000/fill:#ffffff/g; s/<svg /<svg fill="#ffffff" /' "$T/$1.svg"
    printf '<!doctype html><style>html,body{margin:0;background:transparent}img{display:block;width:%spx;height:%spx}</style><img src="%s.svg">' "$2" "$3" "$1" > "$T/$1.html"
    "$CH" --headless=new --disable-gpu --hide-scrollbars --default-background-color=00000000 --window-size="$2,$3" --force-device-scale-factor=1 --screenshot="$OUT/app/icons/logos/$1.png" "file://$T/$1.html" >/dev/null 2>&1; }
  dl playstation "https://upload.wikimedia.org/wikipedia/commons/5/5c/PlayStation_logo_and_wordmark.svg"; render playstation 1000 181 white
  dl bein "https://upload.wikimedia.org/wikipedia/commons/d/d4/BeIN_Sports_logo_%28horizontal_version%29.svg"; render bein 1024 182
  dl hdmi "https://upload.wikimedia.org/wikipedia/commons/5/58/High_Definition_Multimedia_Interface_Logo.svg"
  sed -i '' 's/viewBox="0 0 338.667 78.834"/viewBox="0 0 338.667 60"/; s/height="297.957"/height="226.7"/' "$T/hdmi.svg"; render hdmi 1000 226 white   # white, tagline cropped off
  rm -rf "$T"
else echo "   Chrome not found: skipped (see docs/ASSETS for the manual steps)"; fi
echo "done. Club crests and logos are trademarks of their owners; personal use only."

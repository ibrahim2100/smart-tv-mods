# AGENTS.md — guide for AI coding agents / دليل لوكلاء الذكاء الاصطناعي

> **AR:** هذا الملف مكتوب لأي نموذج ذكاء اصطناعي (Claude، GPT، Gemini، Codex، Cursor…) يقرأ المستودع. بعد قراءته تقدر **تفهم المشروعين كاملين، وتنفذهما على تلفزيون المستخدم، أو تبني نسخة خاصة بتلفزيونه**. الملف بالإنجليزية لأنه أدق للنماذج؛ README بالعربي والإنجليزي للبشر.
>
> **EN:** Written for any AI model reading this repo. After reading it you can **understand both projects end to end, run them on the user's TV, or build a version of your own for the user's TV.**

Authorship note: this repository was written by an AI agent (Anthropic's Claude, via Claude Code) under the direction and hardware testing of its owner, Ibrahim Al-Rahn. Treat the code as AI-written and human-tested on the real devices below.

---

## 0. TL;DR

| | `lg-webos/` | `google-tv/` |
|---|---|---|
| Device tested | LG webOS 7.5.8 TV (Chrome 87, 4K, 1.2 GB RAM) | Haier MatrixTV BE, Google TV / Android 14 (4K panel, UI at 1920×1080) |
| What it is | Web app that replaces the TV's home screen + root Node service + overlay alert app | Native Android launcher (Java, no Gradle) + restore/revert script kit |
| Access needed | **Root** (Homebrew Channel) + telnet :23. **NOT** LG Developer Mode (remove that app before rooting) | **Developer options → USB / Wireless debugging** (adb); no root |
| Driver | `lg-webos/scripts/*.sh` over telnet | `google-tv/albrhi-tv-restore/*.sh` over adb |
| Test without a TV | `cd lg-webos && npm test`; open `app/index.html` or `alert/index.html?demo=goal` in a browser | `google-tv/albrhi-widget/build.sh` (compiles), no emulator needed for a syntax check |
| Reads first | `lg-webos/CLAUDE.md`, `lg-webos/docs/LESSONS.md`, `lg-webos/docs/ARCHITECTURE.md` | `google-tv/albrhi-tv-restore/README.md` (Arabic, exhaustive), `google-tv/README.md` |

The two projects share **no code and no scripts**. Never run adb scripts against the LG or telnet scripts against the Google TV. Work inside the folder of the project the user means; if unclear which TV, ask.

## 1. Ground rules for an agent

1. **Ask before touching hardware.** Every script in `scripts/` or `albrhi-tv-restore/` talks to a real TV. Get the TV's IP from the user and explicit consent before running anything that connects. Local edits, tests and builds need no consent.
2. **Back up first**, always: `lg-webos/scripts/backup.sh` / `google-tv/albrhi-tv-restore/backup.sh` (and `check.sh`). Know the revert command before you change anything (see §5).
3. **Never kill the webOS home process** or close/launch it by hand (black screen until reboot). Use `deploy.sh`, which updates files and lets the page reload itself.
4. **Never commit private data** (§7). This repo is public.
5. **Do not invent APIs.** Everything the code calls exists on the tested firmware; if you port to another firmware, verify each call on that device first (e.g. `luna-send` a read-only method, `adb shell cmd package ...`).
6. Keep edits small and verifiable: run the test/build, then deploy, then run `check.sh`.

## 2. Prerequisites the *user* must do on the TV (agents cannot do these)

### LG webOS (needs root — not Developer Mode)
Do **not** use the LG *Developer Mode* app for this project: it gives no root, needs renewal every ~1000 h, and is not enough for the home swap, bind mounts, the root service and telnet. **Do not keep it installed while rooting or after**: on the owner's TV it was rejected / broke the root. If the TV has it, remove it first. Path:
1. Check rootability of the exact model + firmware at <https://cani.rootmy.tv>. Methods are firmware dependent and get patched by updates: **don't update the TV first.**
2. Root using the matching method from <https://www.webosbrew.org/rooting/>. On the owner's webOS 7.5.8 TV the launcher <https://e.nya.je/getroot/> (jsbro / dangbro / wtfbro exploits; you enter firmware version and TV IP and follow its steps) was used. This installs **Homebrew Channel**. The page is only a launcher; the user must do these steps on the TV, an agent cannot.
3. Confirm a root shell on the LAN: `nc <tv-ip> 23` gives a `#` prompt (telnet root shell from Homebrew Channel). The project's scripts use exactly this.
4. Computer and TV on the same LAN. Write the IP to `lg-webos/scripts/tv.conf` (`TV_IP=...`).
Risk: root may vanish after a firmware update; telnet :23 is an unauthenticated root shell for the whole LAN, never expose it to the internet (see `lg-webos/docs/SAFETY.md`).

### Google TV / Android TV (no root)
1. Settings → System → About → tap **Build** 7 times to enable Developer options.
2. Developer options → turn on **USB debugging** (and **Wireless debugging** on Android 11+ TVs; some TVs call it "Network debugging", port 5555 by default or the one shown on screen; `adb mdns services` lists it).
3. On your computer: `adb connect <tv-ip>:5555`; **accept the "Allow USB debugging?" prompt on the TV** (tick "Always allow"). `adb devices` must show `device`, not `unauthorized`.
4. Put the address in `google-tv/albrhi-tv-restore/device.conf` (one line `ip:port`) or export `ADB_DEVICE`.
Optional: to install apps from the phone, the launcher needs the `REQUEST_INSTALL_PACKAGES` app-op (granted by `restore.sh`).

## 3. Run the LG project as-is (`lg-webos/`)

```bash
cd lg-webos
npm install                                  # webOS CLI (ares-package)
cp scripts/tv.conf.example scripts/tv.conf   # set TV_IP
npm test                                     # alert logic vs recorded UEFA data — must print "all checks pass"
scripts/backup.sh                            # snapshot apps + settings → scripts/backups/ (git-ignored)
scripts/install-app.sh                       # package + install app and root service (first time)
scripts/install-alert-app.sh                 # install the overlay alert app (first time)
scripts/fetch-assets.sh                      # trademarked icons/crests are NOT in git; pulled locally
scripts/deploy.sh                            # push app/ service/ alert/ and restart the service
scripts/enable-home.sh                       # OPTIONAL: make it the real home (asks y/N, snapshots first)
scripts/check.sh                             # health check
```
Verify: `check.sh` reports root shell, service on `127.0.0.1:45620`, alert app installed. A screenshot: `luna-send -n 1 luna://com.webos.service.capture/executeOneShot '{"path":"/tmp/s.jpg","method":"DISPLAY","width":960,"height":540,"format":"JPG"}'`, then pull the file. Revert: `scripts/revert-home.sh` (reboots), remove: `scripts/uninstall-custom-home.sh`.

### How it works (LG)
- `app/` is a plain web app (no build). On boot `service/as-home.sh` (an init.d hook in `/var/lib/webosbrew/init.d/40-tvhome-as-home`) copies the installed app to `/tmp/tvhome-as-home`, rewrites its id to `com.webos.app.home` and **bind-mounts it over** `/usr/palm/applications/com.webos.app.home`, restarts `sam`, starts the service and sets `homeAutoLaunch` off.
- Luna calls made from the home app's identity never answer, so the page talks to the root service (`service/service.js`, app id `com.albrhi.tvhome.service`) over **HTTP `127.0.0.1:45620`** (POST `/<method>`, JSON). API: getApps, getIcon, getInputs, getAv, setPicture, setSound, screenOff, screenOn, launch, sys, volume, mem, cleanMemory, build, log, uninstall, alertsGet, alertsSet, alertsTest.
- Alerts: `service/uefa.js` polls UEFA's public match API (`https://match.uefa.com/v5/matches?competitionId=1&status=LIVE`, `/matches/{id}/events`), diffs state, queues cards 7 s apart and launches app `com.albrhi.alert` (overlay window, `PalmSystem.launchParams`) via `luna://com.webos.applicationManager/launch`.
- Self-update: the page polls the service `build` id every 15 s and `location.reload()`s after a deploy.
- Full detail: `lg-webos/docs/ARCHITECTURE.md`, pitfalls: `lg-webos/docs/LESSONS.md`.

### LG hard constraints (learn from these)
No `?.`/`??` in service code (Node 12). Animate only `transform`/`opacity` (no blur filters, animated shadows). New file under `app/` must be added to the file list in `scripts/deploy.sh`. Large files over telnet `nc` stall at ~25 MB: send 4 MB chunks and verify sizes. Do not run deploy mid-match (service restarts).

## 4. Run the Google TV project as-is (`google-tv/`)

```bash
# 1) build the APK (needs JDK 17 + Android SDK build-tools 34.0.0 + platform android-34; paths in build.sh assume macOS — adjust)
cd google-tv/albrhi-widget && ./build.sh && ./pack.sh     # → albrhi-widget.apk copied to ../albrhi-tv-restore/
# 2) point at the TV and apply
cd ../albrhi-tv-restore
cp device.conf.example device.conf                           # put ip:port
./backup.sh                                                  # layout/hidden/names → backups/ (git-ignored)
./check.sh                                                   # what is applied
./restore.sh                                                 # install + permissions + home role + disables + screensaver
./selftest.sh --quick                                        # sanity check
```
Revert: `./revert.sh` (add `--full` for a deeper revert) puts back Google TV launcher and settings; no-computer revert: the launcher's own "back to Google TV" card.

### How it works (Android)
- App package `com.albrhi.tvwidget`; `HomeActivity` declares the HOME category. `restore.sh` runs `cmd package set-home-activity`, then **disables** the stock launcher `com.google.android.apps.tv.launcherx` and `com.google.android.tungsten.setupwraith` (`pm disable-user --user 0`, reversible) so the HOME button lands on ours.
- Screensaver: `BarcaDream` set via `settings put secure screensaver_components`. `PreviewActivity` shows it as a normal screen (`adb shell am start -n com.albrhi.tvwidget/.PreviewActivity`) — useful for screenshots.
- Phone control: `RemoteServer` (HTTP, port 8765, 6-digit PIN pairing → token) + `ControlService` (accessibility service for keys/typing). Installs go through `Installer`/`InstallReceiver`; the TV still asks for confirmation.
- Matches/weather: `Fixtures.java` (TheSportsDB; `TEAM_ID`), `Weather.java` (Open-Meteo, IP geolocation), `Alerts.java` + `AlarmReceiver` (exact alarms, overlay banner).
- Layout persistence: `LayoutBackup.java` ⇄ `backup.sh` / `restore.sh` import path `/sdcard/Android/data/com.albrhi.tvwidget/files/import/layout.json`.
- `albrhi-widget/` is the source of truth; `albrhi-tv-restore/source/` is a copy refreshed by `pack.sh`. Keep the two folders side by side.

### Android hard constraints
Needs app-ops granted by adb (`SYSTEM_ALERT_WINDOW`, `SCHEDULE_EXACT_ALARM`, `REQUEST_INSTALL_PACKAGES`) — `restore.sh` does it. The accessibility service is enabled through `settings put secure enabled_accessibility_services`. The signing key (`debug.keystore`) is generated locally by `build.sh`, git-ignored: if lost, the app can't update in place (uninstall + reinstall).

## 5. Make your OWN version for a different TV or owner

Follow this loop: **inspect device → change variation points → verify locally → back up → deploy → check → keep the revert ready.**

### LG webOS — variation points
| Want | Change |
|---|---|
| Your apps/inputs/teams/branding | `lg-webos/app/config.js` (`APPS`, `SYSTEM`, `INPUTS`, `TEAMS`, `BRAND`, `CITY_AR`…); icons by `scripts/fetch-assets.sh` or put files in `app/icons/` |
| Other competitions / event types for alerts | `ALERTS.competitions` (UEFA ids: 1 UCL, 14 UEL, 2001 UECL) and the event switches |
| Your identity | app ids `com.albrhi.*` in `app/appinfo.json`, `service/services.json`, `alert/appinfo.json`, `scripts/*.sh`, `service/*.sh` — rename consistently (grep `com.albrhi`) |
| Another language | strings in `app/index.html`, `app/app.js`, `alert/index.html`, `service/uefa.js` (`*_AR` maps) |
| Look | `app/style.css`, wallpaper via `tools/wallpaper.html` → `app/bg.jpg` |
| Different webOS version | check: Chrome version (`app/` targets Chrome 87), Node version on the TV (`node -v`), home app id (`ls /usr/palm/applications | grep home`), `luna-send` availability of each method used in `service/service.js`, init.d path under `/var/lib/webosbrew/init.d/`. Test on the device with `check.sh`; keep `revert-home.sh` ready. |
| Not a sports fan | set `ALERTS.enabled: false` and drop `TEAMS` |

### Google TV — variation points
| Want | Change |
|---|---|
| Your identity | package `com.albrhi.tvwidget` appears in `AndroidManifest.xml`, `src/com/albrhi/tvwidget/` (rename the directory + `package` lines), `res/xml`, and `albrhi-tv-restore/config.sh` (`APP_PKG`, `HOME_ACT`, `DREAM`) |
| Your team/fixtures | `Fixtures.java` `TEAM_ID` (TheSportsDB team id) and its labels |
| Which vendor apps to disable | `config.sh` lists `DISABLE_PKGS`, `REMOVE_SYSTEM_PKGS`, `REMOVE_USER_PKGS`. **Build your own from the device**: `adb shell pm list packages -s` (system), `-3` (user), `adb shell dumpsys package <pkg> | head`. Only disable what you recognise; `pm disable-user --user 0 <pkg>` is reversible with `pm enable`. |
| Different stock launcher | find it with `adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME`. Android TV (not Google TV) uses `com.google.android.tvlauncher`; Google TV uses `com.google.android.apps.tv.launcherx`. Set `GTV_LAUNCHER`/`GTV_HOME_ACT` accordingly. |
| SDK level | `build.sh` uses `--min-sdk-version 26 --target-sdk-version 34`; match your TV (`adb shell getprop ro.build.version.sdk`). |
| Language / UI | `res/values`, `res/font`, string literals in `src/`. The UI is hand-drawn views (no XML layouts), so most visuals live in Java (`WidgetView`, `HomeActivity`, `StatusBar`, `TopPills`, `QuickSettings`). |

### Porting to a *different platform* (Tizen, Fire TV, Roku, tvOS…)
These projects do not run there. Reuse the *ideas* (rows of apps + inputs, match card, UEFA alert pipeline in `lg-webos/service/uefa.js` + `lg-webos/tools/uefa-test.js` which are platform-independent Node code), and re-implement the shell for the target platform's APIs. Do not copy the root/home-swap tricks to platforms where they don't apply.

## 6. Verification checklist (do this after any change)
- LG: `npm test` passes; open `app/index.html` in a browser (off-TV the system calls are skipped); `alert/index.html?demo=goal|red|var|penalty|cancel|kickoff|full`; then `deploy.sh` → `check.sh` → screenshot.
- Android: `./build.sh` succeeds → `./restore.sh` → `./check.sh` → `adb shell dumpsys activity activities | grep topResumedActivity` shows `com.albrhi.tvwidget/.HomeActivity` after pressing Home.
- Always state plainly what you verified and what you could not (e.g. "VAR/red-card alerts were never seen on a live match").

## 7. Public-repo hygiene (mandatory)
Never commit: real IP addresses (use the `*.example` files), `*.keystore`/`*.jks`/`*.pem`, `backups/`, `*.apk`, tokens, Wi-Fi names, home city/coordinates, personal emails (commit with a GitHub noreply address), screenshots that show a Wi-Fi SSID, city, or personal apps (redact first). Trademarked logos/icons are not stored in the repo. Before every push:
```bash
git ls-files | grep -iE 'keystore|\.apk|backups|tv\.conf|device\.conf'     # must print nothing
git grep -nIE '192\.168\.[0-9]+\.[0-9]+' -- . ':!*.example' ':!*.md'          # must print nothing real
git log --format='%ae' | sort -u                                             # must be a noreply address
```

## 8. Where to read next
`README.md` (humans) · `lg-webos/docs/{INSTALL,USAGE,CONFIG,ARCHITECTURE,ALERTS,LESSONS,SAFETY,ASSETS,ROADMAP}.md` · `google-tv/README.md` and `google-tv/albrhi-tv-restore/README.md` · `llms.txt` (index).

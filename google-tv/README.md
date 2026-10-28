# 🟢 Google TV (Haier MatrixTV BE · Android 14)

لانشر Android خاص (Albrhi Home) وشاشة انتظار (Albrhi Barca) مع حزمة استرجاع كاملة.
A custom Android launcher (Albrhi Home) and screensaver (Albrhi Barca) with a full restore kit.

| المجلد · Folder | الدور · Role |
|---|---|
| [`albrhi-widget/`](albrhi-widget) | المصدر وبناء الـAPK (`build.sh` ← `pack.sh`) · source and APK build |
| [`albrhi-tv-restore/`](albrhi-tv-restore) | حزمة الاسترجاع: `restore.sh` `revert.sh` `check.sh` `backup.sh` `tvinstall.sh` + الشرح الكامل في [README](albrhi-tv-restore/README.md) · restore kit and full docs |

- التلفزيون · TV: `192.168.1.50:5555` عبر `adb` (قابل للتعديل في `albrhi-tv-restore/config.sh`).
- `albrhi-widget/pack.sh` يحدّث `../albrhi-tv-restore` (الاثنان إخوة في هذا المجلد، لا تفصلهما).
  *`pack.sh` refreshes the sibling `../albrhi-tv-restore`: keep the two folders together.*
- هذا غير مشروع LG webOS في `../lg-webos/`. · This is not the LG webOS project in `../lg-webos/`.

# التثبيت · Installation

## العربي

### المتطلبات
1. **تلفزيون LG webOS عليه روت** مع [Homebrew Channel](https://github.com/webosbrew/webos-homebrew-channel). دليل الروت: <https://e.nya.je/getroot/> (على مسؤوليتك؛ قد يلغي الضمان، وتحديث النظام ممكن يشيل الروت).
2. **وصول لشِل الروت من الكمبيوتر.** السكربتات تستخدم **telnet على المنفذ 23** (`nc`)، وهذا اللي توفره طريقة الروت المستخدمة هنا. لو عندك SSH بدله، عدّل دالتي `tv_sh` و`tv_push`/`tv_pull` في [`scripts/config.sh`](../scripts/config.sh).
3. الكمبيوتر والتلفزيون على نفس الشبكة. macOS أو Linux (السكربتات bash). `node` و`npm`. لسحب الشعارات: Google Chrome (على macOS).
4. اختياري: وضع المطوّر (Developer Mode) غير مطلوب؛ نثبّت بخدمة `appInstallService` من الشِل الجذري.

### الخطوات (أول مرة)
```bash
git clone <your-repo> aljalsa-tv && cd aljalsa-tv
npm install                                   # أدوات webOS CLI (ares-package)
cp scripts/tv.conf.example scripts/tv.conf    # حط TV_IP=عنوان تلفزيونك
scripts/check.sh                              # يتأكد إن الروت والاتصال شغّالين (للقراءة فقط)
scripts/backup.sh                             # لقطة لقائمة التطبيقات والإعدادات (قبل أي تغيير)
scripts/install-app.sh                        # يغلّف التطبيق + الخدمة ويثبتهم (com.albrhi.tvhome)
scripts/install-alert-app.sh                  # يثبت بطاقة التنبيه (com.albrhi.alert) مرة وحدة
scripts/fetch-assets.sh                       # يسحب أيقونات التطبيقات من التلفزيون + الشعارات (غير مخزنة بـgit)
scripts/deploy.sh                             # يرفع كل الملفات ويشغّل الخدمة
scripts/enable-home.sh                        # (اختياري) يجعله الهوم الرئيسي ويوقف إعلانات LG وشاشة توقفها
```
> **صدق عن الاختبار:** `install-alert-app.sh` جُرّب فعلياً على التلفزيون. `install-app.sh` يستخدم نفس الآلية وجُرّب تغليفه (`PACKAGE_ONLY=1 scripts/install-app.sh`)، لكن تثبيته الكامل من الصفر ما أُعيد على تلفزيون نظيف بعد إعادة ترتيب المشروع. التطبيق الحالي كان مثبّتاً بنفس الطريقة (ipk ثم تحديث الملفات بـ`deploy.sh`).

### التحديث
عدّل الملفات ثم `scripts/deploy.sh`. الصفحة تعيد تحميل نفسها خلال ~15 ثانية. ما تحتاج إعادة تشغيل للتلفزيون.
**لا تقتل عملية صفحة الهوم ولا تغلقها وتفتحها بالأوامر** (انظر [LESSONS](LESSONS.md)).

### الإزالة والرجوع
- `scripts/revert-home.sh`: يرجّع واجهة LG وإعداداتها (بما فيها الإعلانات وشاشة التوقف) ويعيد التشغيل.
- `scripts/uninstall-custom-home.sh`: يحذف التطبيق من التلفزيون.
- الروت نفسه ما يُلغى بسكربت: الطريقة المضمونة «Reset to Initial Settings» من إعدادات التلفزيون.

---

## English

### Requirements
1. **A rooted LG webOS TV** with [Homebrew Channel](https://github.com/webosbrew/webos-homebrew-channel). Rooting guide: <https://e.nya.je/getroot/> (at your own risk; may void warranty; a firmware update may remove root).
2. **A root shell reachable from your computer.** The scripts use **telnet on port 23** (via `nc`), which is what the rooting method used here provides. If you have SSH instead, adapt `tv_sh` / `tv_push` / `tv_pull` in [`scripts/config.sh`](../scripts/config.sh).
3. Computer and TV on the same network. macOS or Linux (bash scripts), `node` and `npm`. Google Chrome (macOS) is needed only to render logos.
4. Developer Mode is not required: everything is installed through `appInstallService` from the root shell.

### First-time steps
```bash
git clone <your-repo> aljalsa-tv && cd aljalsa-tv
npm install                                   # webOS CLI tools (ares-package)
cp scripts/tv.conf.example scripts/tv.conf    # put TV_IP=your TV's address
scripts/check.sh                              # read-only: confirms root and connection
scripts/backup.sh                             # snapshot of app list and settings (before any change)
scripts/install-app.sh                        # packages and installs app + service (com.albrhi.tvhome)
scripts/install-alert-app.sh                  # installs the alert card (com.albrhi.alert), once
scripts/fetch-assets.sh                       # pulls app icons from the TV + logos (not stored in git)
scripts/deploy.sh                             # pushes every file and restarts the service
scripts/enable-home.sh                        # (optional) makes it the real home, turns off LG ads/screensaver
```
> **Honest testing note:** `install-alert-app.sh` was run for real on the TV. `install-app.sh` uses the same mechanism and its packaging was tested (`PACKAGE_ONLY=1 scripts/install-app.sh`), but a full from-scratch install on a clean TV was not repeated after the project was reorganised. The running app was installed the same way originally (ipk, then files refreshed with `deploy.sh`).

### Updating
Edit files, then `scripts/deploy.sh`. The page reloads itself within ~15 s. No TV restart needed.
**Never kill the home page's process or close/launch it by hand** (see [LESSONS](LESSONS.md)).

### Removing / reverting
- `scripts/revert-home.sh`: puts LG's home and settings back (including ads and screensaver) and reboots.
- `scripts/uninstall-custom-home.sh`: removes the app from the TV.
- Root itself can't be undone by a script: the sure way is "Reset to Initial Settings" in the TV's own menu.

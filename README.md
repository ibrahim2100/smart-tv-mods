# WebOS — تعديلات الجلسة · Aljalsa mods for LG webOS

**شاشة رئيسية مخصصة لتلفزيونات LG webOS (بعد الروت)** — تطبيقاتك، المدخلات، المباريات، تنبيهات دوري الأبطال، وشاشة توقف بهوية خاصة.
**A custom home screen for rooted LG webOS TVs** — your apps, HDMI inputs, match cards, Champions League alerts and a branded screensaver.

![home](docs/screenshots/home.jpg)

| شاشة التوقف · Screensaver | تنبيه هدف · Goal alert (over any app) |
|:---:|:---:|
| ![saver](docs/screenshots/saver.jpg) | ![alert](docs/screenshots/alert.jpg) |

> ⚠️ **مشروع غير رسمي · Unofficial.** يحتاج تلفزيون LG عليه روت (Homebrew Channel). غير LG وغير مرتبط فيها. اقرأ [docs/SAFETY.md](docs/SAFETY.md) قبل أي تثبيت.
> Needs a rooted LG TV (Homebrew Channel). Not affiliated with LG. Read [docs/SAFETY.md](docs/SAFETY.md) before installing.

---

## العربي

### وش هو؟
واجهة ويب (HTML/CSS/JS بدون أي بناء) تشتغل كشاشة رئيسية للتلفزيون، ومعها خدمة جذرية صغيرة (Node.js) وتطبيق نافذة صغير لعرض بطاقات التنبيه فوق أي تطبيق.
جُرّبت على LG webOS 7.5 (Chrome 87، شاشة 4K، رام 1.2 جيجا)، وكل التصميم مبني على قيود هذا الجهاز الضعيف (انظر [docs/LESSONS.md](docs/LESSONS.md)).

### المزايا
- **تطبيقاتك** مفصولة عن **تطبيقات النظام الأساسية** و**المدخلات** (شعار PlayStation وbeIN وHDMI الرسمية). تطبيق جديد تنزّله يظهر لحاله.
- **حركات اختيار ناعمة** (رفع وحلقة ذهبية)، وفتح التطبيق بحركة لون، بأداء 55–60 إطار/ثانية.
- **ضغط مطوّل (OK لنصف ثانية):** نقل، إخفاء، حذف التطبيق فعلياً (بتأكيد). يشتغل بالمؤشر كذلك.
- **بطاقة المباراة القادمة** لكل فريق تختاره (ESPN)، بعدّاد `HH:MM:SS` ونتيجة مباشرة، وتحديث كل ساعة مع زر تحديث يدوي.
- **تنبيهات دوري أبطال أوروبا** من مصدر UEFA الرسمي: بداية، هدف (مع اسم اللاعب)، طرد، ركلة جزاء، VAR، استراحة، نهاية. تظهر **فوق أي تطبيق** ببطاقة بتصميمنا.
- **شاشة توقف** بهوية الجلسة (شعار، ساعة كبيرة، طقس، المباراة القادمة) تحل محل شاشة LG (وإعلاناتها).
- **تنظيف الذاكرة** (يدوي وتلقائي) لأن التلفزيون ما عنده إلا ~350 ميجا متاحة.
- **لوحة إعدادات سريعة:** شبكة، بلوتوث، مخرج الصوت، الصوت، وضع الصورة، إطفاء الشاشة مع بقاء الصوت.
- يوقف **إعلانات LG** ويستبدل الهوم الأصلي (ويرجع بسكربت واحد).

### ابدأ بسرعة
```bash
npm install                       # أدوات webOS CLI (مرة وحدة)
cp scripts/tv.conf.example scripts/tv.conf   # حط عنوان التلفزيون
scripts/install-app.sh            # تثبيت التطبيق + الخدمة (أول مرة)
scripts/install-alert-app.sh      # تثبيت بطاقة التنبيه (أول مرة)
scripts/fetch-assets.sh           # سحب الأيقونات والشعارات (غير مخزّنة بـgit)
scripts/deploy.sh                 # رفع الملفات
scripts/enable-home.sh            # جعله الهوم الرئيسي (اختياري)
```
التفاصيل الكاملة: [docs/INSTALL.md](docs/INSTALL.md) · الاستخدام: [docs/USAGE.md](docs/USAGE.md) · الإعدادات: [docs/CONFIG.md](docs/CONFIG.md).

### الوثائق
| الملف | المحتوى |
|---|---|
| [docs/INSTALL.md](docs/INSTALL.md) | المتطلبات والتثبيت خطوة بخطوة والتحديث والإزالة |
| [docs/USAGE.md](docs/USAGE.md) | كيف تستخدم الواجهة بالريموت والمؤشر |
| [docs/CONFIG.md](docs/CONFIG.md) | كل خيار في `config.js` |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | كيف يشتغل: التطبيق، الخدمة، البطاقة، الاستبدال، الواجهة البرمجية |
| [docs/ALERTS.md](docs/ALERTS.md) | تنبيهات المباريات ومصدر UEFA ومقارنة المصادر |
| [docs/LESSONS.md](docs/LESSONS.md) | الأخطاء اللي تعلمناها (مهم قبل ما تعدّل) + استكشاف الأعطال |
| [docs/SAFETY.md](docs/SAFETY.md) | وش يتغير على التلفزيون وكيف ترجّعه |
| [docs/ASSETS.md](docs/ASSETS.md) | الأيقونات والشعارات والخطوط وتراخيصها |
| [docs/ROADMAP.md](docs/ROADMAP.md) | أفكار لاحقة |
| [CLAUDE.md](CLAUDE.md) | ملخص للمطوّر (أو Claude) عشان تكمل بسرعة |

---

## English

### What is it?
A plain web app (HTML/CSS/JS, no build step) that runs as the TV's home screen, plus a small root helper service (Node.js) and a tiny overlay app that draws alert cards over whatever is on screen.
Built and tested on LG webOS 7.5 (Chrome 87, 4K panel, 1.2 GB RAM). Every design choice follows from that weak hardware (see [docs/LESSONS.md](docs/LESSONS.md)).

### Features
- **Your apps** separated from **essential system apps** and **HDMI inputs** (official PlayStation / beIN / HDMI logos). A newly installed app appears by itself.
- **Smooth selection animation** (lift + gold ring) and a colour-flood launch animation, at 55–60 fps on the TV.
- **Long-press OK (0.5 s):** move, hide, or really uninstall an app (with confirmation). Works with the Magic Remote pointer too.
- **Next-match card** for each team you pick (ESPN), with an `HH:MM:SS` countdown and live score, hourly refresh plus a manual refresh button.
- **Champions League alerts** from UEFA's own API: kick-off, goals (with scorer), red cards, penalties, VAR, half time, full time — shown **over any app** as a styled card.
- **Branded screensaver** (mark, big clock, weather, next match) replacing LG's (and its ads).
- **Memory cleaner** (manual and automatic): the TV only has ~350 MB free.
- **Quick settings panel:** network, Bluetooth, sound output, volume, picture mode, screen-off with audio.
- Turns off **LG's home promotions/ads** and replaces the stock home (one script puts everything back).

### Quick start
```bash
npm install                       # webOS CLI tools (once)
cp scripts/tv.conf.example scripts/tv.conf   # put your TV's address in
scripts/install-app.sh            # first-time install of app + service
scripts/install-alert-app.sh      # first-time install of the alert card
scripts/fetch-assets.sh           # pull icons/logos (not stored in git)
scripts/deploy.sh                 # push the files
scripts/enable-home.sh            # make it the real home screen (optional)
```
Full details: [docs/INSTALL.md](docs/INSTALL.md) · usage: [docs/USAGE.md](docs/USAGE.md) · settings: [docs/CONFIG.md](docs/CONFIG.md).

### Repository layout
```
app/        the home screen (what the TV shows): index.html, app.js, style.css, config.js, fonts, icons
service/    root helper service (Node): launch/uninstall/volume/picture, memory cleaner, match alerts (uefa.js), home swap scripts
alert/      overlay app that draws the alert card over any app
scripts/    run from your computer: install, deploy, enable/revert home, backup, check, fetch-assets
tools/      wallpaper generators, `tv` remote-control script, tests with recorded UEFA data
docs/       documentation in Arabic and English
```

### Tests
```bash
npm test      # alert logic against recorded UEFA data (no TV needed)
```

### Credits
- Home-swap idea, boot script and the first helper service: [zzeppieri/webos-custom-home](https://github.com/zzeppieri/webos-custom-home) (MIT).
- [webosbrew](https://github.com/webosbrew) (Homebrew Channel, root tooling). Rooting guide: <https://e.nya.je/getroot/>.
- Fonts: IBM Plex Sans Arabic and Amiri (SIL OFL 1.1). Data: UEFA match API, ESPN public scoreboard/schedule, Open-Meteo, ipwho.is.
- Logos and crests are trademarks of their owners; they are not stored in git (see [docs/ASSETS.md](docs/ASSETS.md)).

### License
MIT for the code and the original artwork. Third-party assets keep their own terms.

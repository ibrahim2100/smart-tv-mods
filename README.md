# Smart TV Mods · تعديلات التلفزيونات الذكية

**مشروعان مفتوحا المصدر لتخصيص تلفزيونين: واجهة رئيسية لـ LG webOS، ولانشر لـ Google TV (Android).**
**Two open-source projects to customise two TVs: a home screen for LG webOS and a launcher for Google TV (Android).**

![LG webOS home](lg-webos/docs/screenshots/home.jpg)

| | 🔴 [`lg-webos/`](lg-webos) | 🟢 [`google-tv/`](google-tv) |
|---|---|---|
| **التلفزيون · TV** | LG webOS 7.5، مروّت · rooted (Homebrew Channel) | Haier MatrixTV BE — Google TV / Android 14 |
| **ما هو · What** | شاشة رئيسية ويب (HTML/JS) + خدمة جذرية + بطاقات تنبيه دوري الأبطال فوق أي تطبيق + شاشة توقف | لانشر Android (Albrhi Home) + شاشة انتظار + ربط الجوال + حزمة استرجاع |
| **اللغة · Stack** | JavaScript (بدون بناء · no build), Node.js | Java (Android SDK بدون Gradle · without Gradle) |
| **الأدوات · Tooling** | telnet بورت 23، `scripts/*.sh`، `npm test` | `adb`، `build.sh`، `restore.sh` |
| **الشرح · Docs** | [lg-webos/README.md](lg-webos/README.md) + [`docs/`](lg-webos/docs) | [google-tv/README.md](google-tv/README.md) |
| **الحالة · Status** | قيد التطوير · active | مستقر · stable |

> ⚠️ **مشاريع غير رسمية · Unofficial.** غير مرتبطة بـLG أو Google أو Haier. تعديل التلفزيون على مسؤوليتك؛ اقرأ قسم الأمان في كل مشروع قبل التثبيت.
> Not affiliated with LG, Google or Haier. You modify your TV at your own risk; read each project's safety section first.

**الفهرس · Contents:** [العربي](#العربي) · [English](#english)

---

## العربي

### وش في هذا المستودع؟
مستودع واحد يجمع مشروعين **منفصلين تماماً** (لا كود مشترك، ولا سكربتات مشتركة):

1. **[`lg-webos/`](lg-webos) — الجلسة (WebOS):** يستبدل الشاشة الرئيسية لتلفزيون LG المروّت. تطبيقاتك ومدخلات HDMI والنظام في صفوف مرتبة بحركات ناعمة، بطاقة المباراة القادمة، **تنبيهات دوري أبطال أوروبا** (من UEFA) تظهر فوق أي تطبيق حتى Netflix، شاشة توقف بهوية خاصة، تنظيف ذاكرة، ويوقف إعلانات LG. مجرّب على webOS 7.5.
2. **[`google-tv/`](google-tv) — Albrhi Home:** لانشر Android لتلفزيون Google TV (Haier MatrixTV BE): واجهة بأسلوب Apple TV، مباريات وتنبيهات، شاشة انتظار، **ريموت وتثبيت تطبيقات من الجوال**، وحزمة سكربتات تعيد كل التعديلات أو ترجع التلفزيون للأصل.

### كيف أختار؟
- عندك **LG** (webOS) ← ادخل [`lg-webos/`](lg-webos).
- عندك **Google TV / Android TV** ← ادخل [`google-tv/`](google-tv).
- شغّل أي سكربت من داخل مجلد مشروعه فقط؛ سكربتات `adb` لـGoogle TV وسكربتات telnet لـLG.

### هيكل المستودع
```
.
├── lg-webos/                 🔴 LG webOS
│   ├── app/                  الواجهة · the home screen
│   ├── service/              الخدمة الجذرية وتنبيهات UEFA · root service + UEFA alerts
│   ├── alert/                تطبيق بطاقة التنبيه · alert overlay app
│   ├── scripts/              تثبيت ورفع وتراجع · install, deploy, revert
│   ├── tools/                مولّدات الخلفية والاختبارات · wallpaper generators, tests
│   └── docs/                 وثائق مفصلة ثنائية اللغة · detailed bilingual docs
├── google-tv/                🟢 Google TV
│   ├── albrhi-widget/        مصدر التطبيق وبناؤه · app source + build
│   └── albrhi-tv-restore/    حزمة الاسترجاع والأدوات · restore kit + tools
├── README.md · CLAUDE.md · LICENSE
```

### ابدأ
```bash
git clone https://github.com/ibrahim2100/smart-tv-mods.git
cd smart-tv-mods
```
ثم:
- **LG:** `cd lg-webos && npm install && cp scripts/tv.conf.example scripts/tv.conf` وكمّل من [`lg-webos/docs/INSTALL.md`](lg-webos/docs/INSTALL.md).
- **Google TV:** `cd google-tv/albrhi-widget && ./build.sh && ./pack.sh` ثم `cd ../albrhi-tv-restore && cp device.conf.example device.conf && ./restore.sh` (التفاصيل في [`google-tv/README.md`](google-tv/README.md)).

### الخصوصية والأمان في المستودع
- **لا شي خاص مرفوع:** لا عناوين IP حقيقية (أمثلة فقط: `tv.conf.example` و`device.conf.example`)، لا مفاتيح توقيع (`*.keystore` محجوبة)، لا نسخ احتياطية للتلفزيون، لا ملفات APK، ولا إيميلات (الالتزامات بعنوان GitHub noreply).
- الشعارات والأيقونات (علامات تجارية) **غير مخزّنة**؛ تُسحب بـ`lg-webos/scripts/fetch-assets.sh` عندك محلياً.
- ملفات إعدادك المحلية (`tv.conf`، `device.conf`) في `.gitignore`.

### المساهمة
- افتح Issue أو Pull Request. اذكر أي تلفزيون وإصدار النظام.
- قبل أي تعديل في LG: `cd lg-webos && npm test`، واقرأ [`lg-webos/docs/LESSONS.md`](lg-webos/docs/LESSONS.md) (قواعد مهمة، مثل: لا تقتل عملية الهوم).
- ملاحظات للمطوّر ولـClaude: [`CLAUDE.md`](CLAUDE.md).

### الرخصة والشكر
MIT (انظر [`LICENSE`](LICENSE) و[`lg-webos/LICENSE`](lg-webos/LICENSE)). الخطوط: IBM Plex Sans Arabic وAmiri (SIL OFL 1.1). فكرة استبدال الهوم في webOS مأخوذة من [zzeppieri/webos-custom-home](https://github.com/zzeppieri/webos-custom-home) (MIT)؛ الروت عبر [webosbrew](https://github.com/webosbrew) ودليل <https://e.nya.je/getroot/>. البيانات: UEFA وESPN وOpen-Meteo وipwho.is. الشعارات علامات تجارية لأصحابها.

---

## English

### What's in this repository?
One repository with two **completely separate** projects (no shared code, no shared scripts):

1. **[`lg-webos/`](lg-webos) — Aljalsa (WebOS):** replaces the home screen of a rooted LG TV. Apps, HDMI inputs and system apps in tidy rows with smooth animation, a next-match card, **UEFA Champions League alerts** shown over any app (even Netflix), a branded screensaver, a memory cleaner, and LG ads switched off. Tested on webOS 7.5.
2. **[`google-tv/`](google-tv) — Albrhi Home:** an Android launcher for a Google TV (Haier MatrixTV BE): Apple-TV-style home, matches and alerts, a screensaver, **phone remote and app installs**, plus a script kit that re-applies every change or reverts the TV to stock.

### Which one do I need?
- You own an **LG** (webOS) TV → go to [`lg-webos/`](lg-webos).
- You own a **Google TV / Android TV** → go to [`google-tv/`](google-tv).
- Run scripts only from inside their own project: `adb` scripts are for Google TV, telnet scripts for LG.

### Repository layout
(see the tree above.)

### Getting started
```bash
git clone https://github.com/ibrahim2100/smart-tv-mods.git
cd smart-tv-mods
```
Then:
- **LG:** `cd lg-webos && npm install && cp scripts/tv.conf.example scripts/tv.conf`, then continue with [`lg-webos/docs/INSTALL.md`](lg-webos/docs/INSTALL.md).
- **Google TV:** `cd google-tv/albrhi-widget && ./build.sh && ./pack.sh`, then `cd ../albrhi-tv-restore && cp device.conf.example device.conf && ./restore.sh` (details in [`google-tv/README.md`](google-tv/README.md)).

### Privacy and safety of this repo
- **Nothing private is committed:** no real IP addresses (examples only: `tv.conf.example`, `device.conf.example`), no signing keys (`*.keystore` ignored), no TV backups, no APKs, no email addresses (commits use a GitHub noreply address).
- Logos and icons (trademarks) are **not stored**; pull them locally with `lg-webos/scripts/fetch-assets.sh`.
- Your local config files (`tv.conf`, `device.conf`) are git-ignored.

### Contributing
- Open an issue or pull request; say which TV and OS version you use.
- Before changing the LG project: `cd lg-webos && npm test`, and read [`lg-webos/docs/LESSONS.md`](lg-webos/docs/LESSONS.md) (hard rules such as never killing the home process).
- Developer / Claude notes: [`CLAUDE.md`](CLAUDE.md).

### License and credits
MIT (see [`LICENSE`](LICENSE) and [`lg-webos/LICENSE`](lg-webos/LICENSE)). Fonts: IBM Plex Sans Arabic and Amiri (SIL OFL 1.1). The webOS home-swap idea comes from [zzeppieri/webos-custom-home](https://github.com/zzeppieri/webos-custom-home) (MIT); rooting via [webosbrew](https://github.com/webosbrew) and <https://e.nya.je/getroot/>. Data: UEFA, ESPN, Open-Meteo, ipwho.is. Logos are trademarks of their owners.

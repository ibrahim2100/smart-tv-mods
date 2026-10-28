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

## لقطات · Screenshots

| 🔴 LG webOS | |
|:---:|:---:|
| ![LG home](lg-webos/docs/screenshots/home.jpg) | ![LG screensaver](lg-webos/docs/screenshots/saver.jpg) |
| الشاشة الرئيسية · Home | شاشة التوقف · Screensaver |
| ![LG goal alert](lg-webos/docs/screenshots/alert.jpg) | ![LG alert kinds](lg-webos/docs/screenshots/alert-kinds.jpg) |
| تنبيه هدف فوق أي تطبيق · Goal alert over any app | أنواع التنبيهات · Alert kinds |

| 🟢 Google TV | |
|:---:|:---:|
| ![Google TV home](google-tv/docs/screenshots/home.jpg) | ![Google TV screensaver](google-tv/docs/screenshots/saver.jpg) |
| الشاشة الرئيسية · Home | شاشة الانتظار · Screensaver |

*(حجبنا اسم الشبكة والمدينة وبعض التطبيقات من اللقطات حفاظاً على الخصوصية · Wi-Fi name, city and a few apps are blurred for privacy.)*

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
├── README.md · AGENTS.md · llms.txt · CLAUDE.md · LICENSE
```

### ابدأ
```bash
git clone https://github.com/ibrahim2100/smart-tv-mods.git
cd smart-tv-mods
```
ثم:
- **LG:** `cd lg-webos && npm install && cp scripts/tv.conf.example scripts/tv.conf` وكمّل من [`lg-webos/docs/INSTALL.md`](lg-webos/docs/INSTALL.md).
- **Google TV:** `cd google-tv/albrhi-widget && ./build.sh && ./pack.sh` ثم `cd ../albrhi-tv-restore && cp device.conf.example device.conf && ./restore.sh` (التفاصيل في [`google-tv/README.md`](google-tv/README.md)).

### المتطلبات: الروت (LG) / USB Debugging (Android)

لا يشتغل المشروعان على تلفزيون «على حالته». لازم تفتح صلاحيات الوصول أول (روت لـLG، وUSB debugging لـAndroid):

**🔴 LG webOS (يحتاج روت، وليس وضع المطور)**
1. **لا تستخدم تطبيق Developer Mode لهذا المشروع.** وضع المطور (Developer Mode) طريق ثاني محدود: بدون روت، ويحتاج تمديد كل ~1000 ساعة، ولا يكفي للاستبدال والخدمة الجذرية هنا. والأهم: **لا تبقيه مثبّتاً أثناء الروت وبعده** — عندنا التلفزيون رفض/تعطّل معه، وإذا كان محمّلاً يتعطل الروت. لو عندك التطبيق، احذفه قبل الروت.
2. اعرف هل تلفزيونك قابل للروت: ادخل <https://cani.rootmy.tv> واكتب موديل التلفزيون وإصدار النظام (Settings ← General ← About). الطرق تعتمد على الإصدار ومعظمها يُرقَّع مع التحديثات، فلا تحدّث النظام قبل الروت.
3. نفّذ الروت بالطريقة المناسبة من [webosbrew.org/rooting](https://www.webosbrew.org/rooting/) (على تلفزيوننا webOS 7.5 استُخدمت أداة <https://e.nya.je/getroot/>، وهي مُطلِق لثغرات jsbro/dangbro/wtfbro: تضع إصدار النظام وIP التلفزيون وتتبع التعليمات). تثبّت الأداة **Homebrew Channel** وتعطيك روت. المحاولة الفاشلة آمنة غالباً، لكن لا تجرّب تغييرات عشوائية على النظام.
4. تأكد من الاتصال: `nc <ip-التلفزيون> 23` يعطيك `#` (طرفية روت telnet من Homebrew Channel). السكربتات تستخدم هذا المنفذ.
5. الكمبيوتر والتلفزيون على نفس الشبكة، وضع العنوان في `lg-webos/scripts/tv.conf`.
> ⚠️ الروت قد يزول بعد تحديث النظام. وبورت 23 روت بلا كلمة سر لكل من على شبكتك، لا تفتحه للإنترنت.

**🟢 Google TV / Android TV (بدون روت)**
1. الإعدادات ← النظام ← حول ← اضغط **Build** 7 مرات لتفعيل خيارات المطور.
2. خيارات المطور ← فعّل **USB debugging** (وفي Android 11+ فعّل أيضاً **Wireless debugging**).
3. من الكمبيوتر: `adb connect <ip-التلفزيون>:5555` ثم **اقبل رسالة «Allow USB debugging?» على شاشة التلفزيون** (فعّل «Always allow»). `adb devices` لازم يعرض `device` لا `unauthorized`.
4. ضع العنوان في `google-tv/albrhi-tv-restore/device.conf`.

### 🤖 صُنع بوكيل ذكاء اصطناعي
هذا المستودع **كتبه وكيل ذكاء اصطناعي**: [Claude](https://claude.com/claude-code) من Anthropic (Claude Code، النموذج Claude Sonnet 5.5) بتوجيه **إبراهيم الراهن** الذي حدد المطلوب وجرّب كل شي على تلفزيونيه الحقيقيين وصحّح المسار. الفضل في كتابة الكود والتوثيق وتحليل الأعطال يعود للوكيل (Claude)، والقرار والتجربة والملكية لصاحب المشروع. ما كُتب هنا يُظهر أن وكيلاً يقدر يبني ويختبر ويوثق ويرفع مشروعاً كاملاً على أجهزة حقيقية إذا توفرت له الصلاحيات الصحيحة.

### 🧠 لأي نموذج ذكاء اصطناعي يقرأ المستودع
المستودع مصمم بحيث يقدر أي نموذج (Claude، GPT، Gemini، Codex، Cursor…) **يفهمه كاملاً، ينفذه على تلفزيونك، أو يبني نسخته الخاصة**:
- [`AGENTS.md`](AGENTS.md): الدليل الكامل (المتطلبات، التشغيل خطوة بخطوة، نقاط التخصيص لكل مشروع، قواعد الأمان، قائمة التحقق).
- [`llms.txt`](llms.txt): فهرس مختصر للنماذج. و`CLAUDE.md` يحيل إلى `AGENTS.md`.

جرّب هذا الطلب مع أي وكيل (مثل Claude Code):
```text
اقرأ AGENTS.md في هذا المستودع. تلفزيوني [LG webOS / Google TV] عنوانه [IP]، وفعّلت [الروت / USB debugging].
ثبّت المشروع المناسب على تلفزيوني، وخذ نسخة احتياطية أولاً، واسألني قبل أي أمر يتصل بالتلفزيون.
```
أو لبناء نسختك: «اقرأ AGENTS.md ثم اصنع نسخة خاصة بي: فريقي [X]، تطبيقاتي [..]، لغتي [..]، وتلفزيوني [الموديل].»

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

### Requirements: root (LG) / USB debugging (Android)

Neither project works on a TV "as shipped". You must unlock access first (root for LG, USB debugging for Android):

**🔴 LG webOS (root required, NOT Developer Mode)**
1. **Do not use the LG Developer Mode app for this project.** Developer Mode is a different, limited path: no root, it must be renewed about every 1000 h, and it is not enough for the home swap and root service. More importantly, **don't keep it installed while rooting or afterwards**: on our TV it was rejected/broke the root. If you have the app, remove it before rooting.
2. Check that your TV can be rooted: open <https://cani.rootmy.tv> with your model and firmware version (Settings → General → About). Methods depend on the firmware and most get patched by updates, so **don't update the TV before rooting**.
3. Root with the method that fits from [webosbrew.org/rooting](https://www.webosbrew.org/rooting/). (On our webOS 7.5 TV the tool <https://e.nya.je/getroot/> was used: it is a launcher for the jsbro/dangbro/wtfbro exploits where you enter the firmware version and the TV's IP and follow its steps.) It installs **Homebrew Channel** and gives root. A failed attempt is generally safe, but never make random changes to the system.
4. Check the connection: `nc <tv-ip> 23` should give a `#` prompt (the telnet root shell provided by Homebrew Channel). The scripts use this port.
5. Computer and TV on the same network; put the address in `lg-webos/scripts/tv.conf`.
> ⚠️ Root may be lost after a firmware update. Port 23 is an unauthenticated root shell for everyone on your LAN: never expose it to the internet.

**🟢 Google TV / Android TV (no root)**
1. Settings → System → About → tap **Build** 7 times to unlock Developer options.
2. Developer options → turn on **USB debugging** (on Android 11+ also **Wireless debugging**).
3. On your computer: `adb connect <tv-ip>:5555`, then **accept the "Allow USB debugging?" prompt on the TV screen** (tick "Always allow"). `adb devices` must show `device`, not `unauthorized`.
4. Put the address in `google-tv/albrhi-tv-restore/device.conf`.

### 🤖 Built by an AI agent
This repository was **written by an AI agent**: [Claude](https://claude.com/claude-code) by Anthropic (Claude Code, model Claude Sonnet 5.5), directed by **Ibrahim Al-Rahn**, who set the goals, tested everything on his two real TVs and steered the work. The credit for writing the code and documentation and for diagnosing the failures goes to the agent (Claude); the decisions, testing and ownership are the project owner's. It shows what an agent can build, test, document and publish on real devices when given the right access.

### 🧠 For any AI model reading this repo
The repository is structured so that any model (Claude, GPT, Gemini, Codex, Cursor…) can **understand it fully, run it on your TV, or build its own version**:
- [`AGENTS.md`](AGENTS.md): the full guide (prerequisites, step-by-step run, customisation points per project, safety rules, verification checklist).
- [`llms.txt`](llms.txt): a compact index for models; `CLAUDE.md` points to `AGENTS.md`.

Try this prompt with any agent (e.g. Claude Code):
```text
Read AGENTS.md in this repository. My TV is a [LG webOS / Google TV] at [IP] and I enabled [root / USB debugging].
Install the matching project on my TV: back up first and ask me before any command that connects to the TV.
```
Or to make your own: "Read AGENTS.md, then build me my own version: my team is [X], my apps are [..], my language is [..], my TV is [model]."

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

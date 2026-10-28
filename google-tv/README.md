# 🟢 Google TV (Haier MatrixTV BE · Android 14)

**لانشر Android مخصص (Albrhi Home) وشاشة انتظار (Albrhi Barca) مع حزمة استرجاع كاملة** لتلفزيون Google TV.
**A custom Android launcher (Albrhi Home) and screensaver (Albrhi Barca) with a full restore kit** for a Google TV.

> هذا المجلد مشروع منفصل عن [`../lg-webos`](../lg-webos). · This folder is separate from [`../lg-webos`](../lg-webos).
> مشروع مستقر: غير مرتبط بأي خدمة سحابية، ولا يجمع بيانات. · Stable; no cloud service, no data collection.

| الشاشة الرئيسية · Home | شاشة الانتظار · Screensaver |
|:---:|:---:|
| ![home](docs/screenshots/home.jpg) | ![screensaver](docs/screenshots/saver.jpg) |

---

## العربي

### وش هو؟
تطبيق Android (Java بدون Gradle) يحل محل واجهة Google TV الرئيسية على تلفزيون Haier MatrixTV BE (Android 14). يتحكم بزر **Home**، ويضيف شاشة انتظار، وأدوات تحكم من الجوال، وحزمة سكربتات تعيد كل التعديلات لو نزل تحديث ورجّع شي، أو ترجع التلفزيون للأصل.

### المزايا
| الميزة | الشرح |
|---|---|
| **واجهة رئيسية** | بطل كبير بأسلوب Apple TV: اسم التطبيق المحدد وصورته والخلفية تتلوّن بلونه. صفوف: تطبيقاتك، أدوات التلفزيون، المدخلات (HDMI) مع حالة الشبكة. |
| **الشريط العلوي** | ساعة، تاريخ ميلادي وهجري، طقس (موقع تلقائي من الـIP، Open-Meteo)، وشارة المباراة القادمة. |
| **المباريات** | قائمة مباريات الفرق، عدّ تنازلي، وتنبيهات: قبل المباراة بـ15 دقيقة، عند الانطلاق، عند الانتهاء، وعند تغيّر الموعد. بانر فوق أي تطبيق. |
| **ضغط مطوّل على OK** | نقل، إعادة تسمية، إخفاء، حذف (أو معلومات التطبيق لتطبيقات النظام). الترتيب محفوظ. |
| **الإعدادات السريعة** | الشبكة، الصورة، الصوت، التطبيقات، الموقع، الكيبورد، حواف الشاشة (للتلفزيونات التي تقص الأطراف)، تنظيف الذاكرة، تجربة التنبيه، رجوع لواجهة Google TV. |
| **ربط الجوال** | صفحة ويب على شبكة البيت: ريموت كامل (أسهم، OK، صوت، كتابة، تشغيل رابط فيديو، إطفاء الشاشة)، تثبيت وحذف وفتح التطبيقات، ورفع APK. رمز ربط من 6 أرقام لكل جهاز، وكل تثبيت يحتاج تأكيد على التلفزيون. |
| **شاشة الانتظار** | Albrhi Barca بدل شاشة Google الأصلية. |
| **تنظيف الذاكرة** | يدوي أو تلقائي قبل فتح تطبيق لو الرام الحرة أقل من 300 ميجا (يغلق الخلفية فقط). |
| **ثبات** | شاشة طوارئ لو انهار اللانشر 3 مرات خلال دقيقتين، ونسخة احتياطية يومية اختيارية للترتيب والأسماء. |
| **تقليل الإعلانات والتطبيقات الزائدة** | يعطّل تطبيقات Haier/MediaTek الزائدة وبعض تطبيقات Google عن المستخدم الحالي. |

### المتطلبات
- تلفزيون Android/Google TV مع تفعيل **Developer options ← Wireless debugging / USB debugging**.
- `adb` على جهازك (Android platform-tools).
- للبناء من المصدر: JDK 17 وAndroid SDK (build-tools 34.0.0 وplatform android-34).

### ابدأ بسرعة
```bash
cd albrhi-tv-restore
cp device.conf.example device.conf   # ضع عنوان التلفزيون، مثل 192.168.1.50:5555
./check.sh        # يفحص وش مطبّق وش لا
./restore.sh      # يثبّت اللانشر ويطبّق كل التعديلات
./revert.sh       # يرجّع التلفزيون لـGoogle TV الأصلي
```
> ملف `albrhi-widget.apk` غير مرفوع على GitHub: ابنِه أولاً (الأسفل) ثم انسخه لمجلد `albrhi-tv-restore/` (`pack.sh` يسويها).

### البناء من المصدر
```bash
cd albrhi-widget
./build.sh        # aapt2 ← javac ← d8 ← zipalign ← apksigner  (ينتج albrhi-widget.apk)
./pack.sh         # ينسخ الـAPK والمصدر إلى ../albrhi-tv-restore
```
- `build.sh` ينشئ مفتاح توقيع محلي `debug.keystore` أول مرة. **لا يُرفع على git (محجوب في `.gitignore`)، احتفظ بنسخة خاصة منه**: لو ضاع لا تقدر تحدّث التطبيق فوق نفسه.
- المسارات في `build.sh` لـmacOS (Homebrew JDK وSDK في `~/Library/Android/sdk`): عدّلها لنظامك.
- `albrhi-tv-restore/source/` نسخة مطابقة من المصدر تُحدَّث بـ`pack.sh`؛ المصدر الرئيسي في `albrhi-widget/`.

### الأدوات (`albrhi-tv-restore/`)
| الملف | الوظيفة |
|---|---|
| `restore.sh` / `revert.sh` / `check.sh` | إعادة التطبيق / الرجوع للأصل / الفحص |
| `backup.sh` | يسحب ترتيب التطبيقات والمخفية وأسماء HDMI إلى `backups/` (غير مرفوعة) |
| `install-autobackup.sh` | (اختياري) نسخة احتياطية يومية 12 ظهراً عبر launchd |
| `tvinstall.sh` | تثبيت تطبيق من الماك (APK أو مجلد splits أو xapk أو رابط)، `list`, `remove`, `launch` |
| `selftest.sh` | فحص ذاتي: النظام، الذاكرة، الاستقرار، السلاسة |
| `config.sh` | قوائم الحزم والعنوان (`device.conf` أو `ADB_DEVICE`) |

الشرح الكامل بالتفصيل (كل التعديلات على التلفزيون، الأداء، ربط الجوال وواجهته، الرجوع بدون كمبيوتر): [albrhi-tv-restore/README.md](albrhi-tv-restore/README.md).

### الأمان
- اتصال ربط الجوال HTTP داخل شبكة البيت فقط (يرفض أي عنوان خارجي) بدون تشفير؛ التوكن لا يُرسل خارج الشبكة. قرار التشفير مؤجّل ومشروح في README الحزمة.
- خدمة الإتاحة `Albrhi Remote Control` لا تقرأ الشاشة ولا تخزّن شي؛ تتحرك فقط عند طلب جوال مربوط.
- لا مفاتيح أو توكنات أو نسخ احتياطية على git. عنوان التلفزيون في `device.conf` المحلي.

---

## English

### What is it?
An Android app (plain Java, no Gradle) that replaces the Google TV home screen on a Haier MatrixTV BE (Android 14). It takes over the **Home** button, adds a screensaver and phone-based controls, and ships a script kit that re-applies every change after a system update, or reverts the TV to stock.

### Features
| Feature | Details |
|---|---|
| **Home screen** | Apple-TV-style hero: the selected app's name and artwork, background tinted with its colour. Rows: your apps, TV tools, inputs (HDMI) with network status. |
| **Top bar** | Clock, Gregorian and Hijri date, weather (location from IP, Open-Meteo), next-match pill. |
| **Matches** | Team fixtures list, countdown, and alerts: 15 min before, at kick-off, at full time, and when a fixture moves. Banner over any app. |
| **Long-press OK** | Move, rename, hide, delete (or app info for system apps). Order is saved. |
| **Quick settings** | Network, picture, sound, apps, location, keyboard, screen margins (for TVs that crop edges), memory cleaner, alert test, back to the stock Google TV launcher. |
| **Phone pairing** | A web page on your home network: full remote (arrows, OK, volume, typing, play a video link, screen off), install/uninstall/open apps, APK upload. A 6-digit pairing code per device, and every install needs confirmation on the TV. |
| **Screensaver** | Albrhi Barca replaces Google's. |
| **Memory cleaner** | Manual, or automatic before launching an app when free RAM < 300 MB (background apps only). |
| **Resilience** | Emergency screen if the launcher crashes 3 times in 2 minutes; optional daily backup of layout and names. |
| **Less bloat** | Disables extra Haier/MediaTek apps and some Google apps for the current user. |

### Requirements
- An Android/Google TV with **Developer options → Wireless/USB debugging** on.
- `adb` on your computer (Android platform-tools).
- To build from source: JDK 17 and the Android SDK (build-tools 34.0.0, platform android-34).

### Quick start
```bash
cd albrhi-tv-restore
cp device.conf.example device.conf   # put your TV's address in, e.g. 192.168.1.50:5555
./check.sh        # what is applied, what is not
./restore.sh      # installs the launcher and applies every customisation
./revert.sh       # puts the TV back to stock Google TV
```
> `albrhi-widget.apk` is not stored on GitHub: build it first (below) and copy it into `albrhi-tv-restore/` (`pack.sh` does that).

### Build from source
```bash
cd albrhi-widget
./build.sh        # aapt2 → javac → d8 → zipalign → apksigner  (produces albrhi-widget.apk)
./pack.sh         # copies the APK and source into ../albrhi-tv-restore
```
- `build.sh` generates a local signing key `debug.keystore` the first time. **It is git-ignored; keep a private copy**: without it you can't update the app in place.
- Paths in `build.sh` assume macOS (Homebrew JDK, SDK in `~/Library/Android/sdk`): adjust for your system.
- `albrhi-tv-restore/source/` is an identical copy of the source refreshed by `pack.sh`; the main source is `albrhi-widget/`.

### Tools (`albrhi-tv-restore/`)
| File | Purpose |
|---|---|
| `restore.sh` / `revert.sh` / `check.sh` | re-apply / revert to stock / health check |
| `backup.sh` | pulls app order, hidden apps and HDMI names into `backups/` (not committed) |
| `install-autobackup.sh` | (optional) daily 12:00 backup via launchd |
| `tvinstall.sh` | install an app from your Mac (APK, splits folder, xapk or URL); `list`, `remove`, `launch` |
| `selftest.sh` | self-test: system, memory, stability, smoothness |
| `config.sh` | package lists and address (`device.conf` or `ADB_DEVICE`) |

The full Arabic reference (every change made to the TV, performance notes, phone pairing and its API, no-computer revert paths): [albrhi-tv-restore/README.md](albrhi-tv-restore/README.md).

### Security
- Phone pairing is plain HTTP on your home network only (it rejects any outside address), unencrypted; the token never leaves the LAN. The encryption decision is deferred and explained in the kit's README.
- The `Albrhi Remote Control` accessibility service doesn't read or store the screen; it acts only on requests from a paired phone.
- No keys, tokens or backups in git. The TV address lives in your local `device.conf`.

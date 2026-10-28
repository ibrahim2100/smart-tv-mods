# مشروعان لتلفزيونين مختلفين · Two projects, two different TVs

هذا المجلد يحوي **مشروعين منفصلين تماماً** لتلفزيونين مختلفين. لا تخلط بينهما.
This folder holds **two completely separate projects** for two different TVs. Do not mix them.

| | 🟢 Google TV (Haier) | 🔴 LG webOS |
|---|---|---|
| **التلفزيون · TV** | Haier MatrixTV BE — Google TV / Android 14 | LG webOS 7.5 (مروّت بـHomebrew Channel · rooted) |
| **المجلدات · Folders** | `albrhi-widget/` (المصدر · source) و`albrhi-tv-restore/` (حزمة الاسترجاع · restore kit) | `aljalsa-tv/` |
| **ما هو · What** | لانشر Android (Albrhi Home) + شاشة انتظار، APK مبني بـ`build.sh` | واجهة ويب (HTML/JS) تحل محل هوم LG + خدمة جذرية + بطاقات تنبيه |
| **الأدوات · Tooling** | `adb` (بورت 5555)، سكربتات `restore.sh` `check.sh` `backup.sh` | telnet بورت 23 (روت)، سكربتات `scripts/deploy.sh` `check.sh` |
| **عنوان التلفزيون · Address** | `192.168.1.50:5555` (في `albrhi-tv-restore/config.sh`) | في `aljalsa-tv/scripts/tv.conf` (غير مرفوع · not committed) |
| **Git** | مستودع هذا المجلد (محلي) · this folder's repo (local) | مستودع مستقل داخل `aljalsa-tv/` ← [github.com/ibrahim2100/WebOS](https://github.com/ibrahim2100/WebOS) |
| **التوثيق · Docs** | `albrhi-tv-restore/README.md` | `aljalsa-tv/README.md` و`aljalsa-tv/docs/` |

## قواعد الفصل · Separation rules
1. **لا ملفات مشتركة ولا سكربتات مشتركة.** كل مشروع يشغَّل من داخل مجلده فقط.
2. سكربتات Google TV (`adb`) لا تمس LG، وسكربتات LG (`telnet`) لا تمس Google TV. تأكد أي تلفزيون تقصد قبل أي رفع.
3. `aljalsa-tv/` له مستودع git خاص به؛ المستودع الخارجي يتجاهله (`.gitignore`). التزامات أحدهما لا تدخل الآخر.
4. الاسم «Albrhi TV» قديماً كان يعني لانشر Google TV. مشروع LG اسمه الآن **الجلسة / WebOS**.
5. مشروع Google TV مستقر: لا تعدّله إلا بطلب صريح.

*Separate tooling, separate git history, separate TVs. Run each project only from its own folder; the outer repo ignores `aljalsa-tv/`. The Google TV project is stable: change it only on explicit request.*

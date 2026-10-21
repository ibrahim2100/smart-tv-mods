# السكربتات · Scripts

تشتغل من جهازك وتتصل بالتلفزيون عبر telnet (المنفذ 23، روت بلا كلمة سر). عدّل عنوان التلفزيون في `scripts/tv.conf` (انسخه من `tv.conf.example`) أو مرّر `TV_IP=...`.
*Run from your computer; they reach the TV over telnet (port 23, unauthenticated root). Set the address in `scripts/tv.conf` or `TV_IP=...`.*

| السكربت · Script | الوظيفة · What it does |
|---|---|
| `config.sh` | مشترك: `TV_IP` وأدوات `tv_sh` / `tv_push` / `tv_pull` · shared helpers |
| `install-app.sh` | يحزّم ويثبّت التطبيق والخدمة (`PACKAGE_ONLY=1` للحزم فقط) · package + install app/service |
| `install-alert-app.sh` | يثبّت تطبيق بطاقة التنبيه · install the alert overlay app |
| `fetch-assets.sh` | يسحب الأيقونات والشعارات (غير مخزنة) · pull third-party artwork |
| `deploy.sh` | يرفع `app/` و`service/` و`alert/` ويعيد تشغيل الخدمة، لا يقتل الصفحة · push files, restart service, never kills the page |
| `enable-home.sh` | يجعل واجهتنا الهوم ويطفئ إعلانات LG (يحفظ لقطة أولاً) · make it the home, turn off LG ads |
| `revert-home.sh` | يرجع هوم LG وكل الإعدادات ويعيد التشغيل · restore stock home + settings, reboots |
| `uninstall-custom-home.sh` | يحذف تطبيقنا (ارجع للهوم الأصلي أولاً) · remove our app |
| `backup.sh` | لقطة من قائمة التطبيقات والإعدادات إلى `backups/` · snapshot |
| `check.sh` | فحص الحالة: الروت، الاستبدال، الخدمة، تطبيق التنبيه · health check |

## استخدام · Usage
```bash
scripts/check.sh               # وضع التلفزيون الآن
scripts/backup.sh              # نسخة قبل أي تعديل كبير
scripts/deploy.sh              # رفع آخر تعديل
scripts/revert-home.sh         # تراجع كامل عن الاستبدال
```

> ⚠️ لا تشغّلها أثناء مشاهدة مباراة؛ بعضها يعيد تشغيل الخدمة أو التلفزيون.
> *Don't run them mid-match; some restart the service or the TV.*

لو بغيت **إرجاع التلفزيون بالكامل**: Factory Reset من إعدادات التلفزيون (يمسح كل شي بما فيه الروت). انظر [docs/SAFETY.md](../docs/SAFETY.md).
